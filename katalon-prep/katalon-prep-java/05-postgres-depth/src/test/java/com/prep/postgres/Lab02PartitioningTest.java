package com.prep.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * LAB 02 — PARTITIONING theo thoi gian.
 *
 * <p>Day la ky thuat bat buoc cho bang test_results o quy mo Katalon: hang ty dong, giu 30 ngay
 * hot + archive, va phai xoa du lieu cu MOI NGAY. Neu khong partition, viec xoa du lieu cu se giet
 * chet database bang bloat va autovacuum.
 *
 * <p>Ba thu can chung minh: (1) partition pruning, (2) DROP PARTITION vs DELETE, (3) bay
 * default partition.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Lab02PartitioningTest {

    private static final int ROWS_PER_MONTH = 50_000;

    @BeforeAll
    static void createPartitionedTable() {
        Pg.exec(
                "DROP SCHEMA IF EXISTS lab02 CASCADE",
                "CREATE SCHEMA lab02",
                // Chu y: PRIMARY KEY cua bang partitioned PHAI chua partition key.
                // Day la gioi han that cua Postgres, va la thu de quen khi migrate bang cu sang partition.
                """
                CREATE TABLE lab02.test_results (
                    id          bigserial,
                    run_id      bigint      NOT NULL,
                    status      text        NOT NULL,
                    created_at  timestamptz NOT NULL,
                    PRIMARY KEY (id, created_at)
                ) PARTITION BY RANGE (created_at)
                """,
                "CREATE TABLE lab02.results_2026_01 PARTITION OF lab02.test_results "
                        + "FOR VALUES FROM ('2026-01-01') TO ('2026-02-01')",
                "CREATE TABLE lab02.results_2026_02 PARTITION OF lab02.test_results "
                        + "FOR VALUES FROM ('2026-02-01') TO ('2026-03-01')",
                "CREATE TABLE lab02.results_2026_03 PARTITION OF lab02.test_results "
                        + "FOR VALUES FROM ('2026-03-01') TO ('2026-04-01')",
                "CREATE TABLE lab02.results_2026_04 PARTITION OF lab02.test_results "
                        + "FOR VALUES FROM ('2026-04-01') TO ('2026-05-01')");

        for (int month = 1; month <= 4; month++) {
            Pg.exec(
                    """
                    INSERT INTO lab02.test_results (run_id, status, created_at)
                    SELECT (i %% 1000) + 1,
                           CASE WHEN i %% 20 = 0 THEN 'FAILED' ELSE 'PASSED' END,
                           timestamptz '2026-%02d-01' + (i %% 27) * interval '1 day'
                    FROM generate_series(1, %d) AS s(i)
                    """
                            .formatted(month, ROWS_PER_MONTH));
        }
        // Index tao tren bang cha se duoc tao tu dong tren MOI partition.
        Pg.exec(
                "CREATE INDEX idx_lab02_run ON lab02.test_results (run_id)",
                "ANALYZE lab02.test_results");
    }

    @Test
    @Order(1)
    @DisplayName("4 partition, moi cai 50k dong, tong 200k")
    void dataIsSpreadAcrossPartitions() {
        assertEquals(200_000L, Pg.count("lab02.test_results"));
        assertEquals(50_000L, Pg.count("lab02.results_2026_01"));
        assertEquals(50_000L, Pg.count("lab02.results_2026_03"));

        List<String> partitions = Pg.column(
                """
                SELECT c.relname FROM pg_class c
                JOIN pg_inherits i ON i.inhrelid = c.oid
                JOIN pg_class p ON p.oid = i.inhparent
                WHERE p.relname = 'test_results' ORDER BY c.relname
                """);
        assertEquals(
                List.of("results_2026_01", "results_2026_02", "results_2026_03", "results_2026_04"),
                partitions);
    }

    @Test
    @Order(2)
    @DisplayName("PARTITION PRUNING: filter theo thang -> chi quet 1 partition, bo qua 3 cai kia")
    void partitionPruningSkipsIrrelevantPartitions() {
        String plan = Pg.explain(
                """
                SELECT count(*) FROM lab02.test_results
                WHERE created_at >= '2026-03-01' AND created_at < '2026-04-01'
                """);
        System.out.println("=== [2] partition pruning ===\n" + plan);

        // Chi partition thang 3 duoc nhac den.
        assertTrue(plan.contains("results_2026_03"), plan);
        assertFalse(plan.contains("results_2026_01"), "partition thang 1 phai bi loai:\n" + plan);
        assertFalse(plan.contains("results_2026_04"), "partition thang 4 phai bi loai:\n" + plan);
    }

    @Test
    @Order(3)
    @DisplayName("BAY: query KHONG co partition key -> quet TAT CA partition (mat het loi ich)")
    void queryWithoutPartitionKeyScansEverything() {
        // Filter theo run_id thay vi created_at -> planner khong biet bo partition nao.
        String plan = Pg.explain("SELECT count(*) FROM lab02.test_results WHERE run_id = 42");
        System.out.println("=== [3] khong co partition key ===\n" + plan);

        // Cả 4 partition đều bị quét.
        long partitionsTouched = plan.lines().filter(l -> l.contains("results_2026_")).count();
        assertTrue(partitionsTouched >= 4, "mong doi quet ca 4 partition, thay %d:\n%s".formatted(partitionsTouched, plan));

        // BAI HOC: partition key phai chon theo CACH QUERY, khong phai theo cach du lieu den.
        // Neu 90% query cua ban filter theo run_id chu khong theo thoi gian, thi partition theo
        // thoi gian chi giup viec XOA du lieu cu, khong giup doc. Do van co the la ly do du (xem test 4),
        // nhung phai noi ro trade-off do khi thiet ke.
    }

    @Test
    @Order(4)
    @DisplayName("DROP PARTITION gan nhu tuc thoi va KHONG sinh dead tuple; DELETE thi nguoc lai")
    void dropPartitionVersusDelete() {
        // --- Cach 1: DELETE 50k dong tu partition thang 2 ---
        long sizeBefore = Pg.tableSizeBytes("lab02.results_2026_02");
        long startDelete = System.nanoTime();
        Pg.exec("DELETE FROM lab02.test_results WHERE created_at >= '2026-02-01' AND created_at < '2026-03-01'");
        long deleteMs = (System.nanoTime() - startDelete) / 1_000_000;

        long deadTuples = Pg.scalar(
                """
                SELECT n_dead_tup FROM pg_stat_user_tables
                WHERE schemaname = 'lab02' AND relname = 'results_2026_02'
                """,
                Long.class);
        long sizeAfterDelete = Pg.tableSizeBytes("lab02.results_2026_02");

        System.out.printf(
                "=== [4] DELETE 50k: %d ms, dead_tup=%d, size %s -> %s (KHONG giam)%n",
                deleteMs, deadTuples, Pg.humanSize(sizeBefore), Pg.humanSize(sizeAfterDelete));

        assertEquals(0L, Pg.count("lab02.results_2026_02"), "dong da bi xoa logic");
        // DIEM QUAN TRONG: DELETE khong giai phong disk. Dong chi duoc danh dau dead;
        // dung luong chi tra lai HE DIEU HANH sau VACUUM FULL (khoa bang) hoac pg_repack.
        assertTrue(sizeAfterDelete >= sizeBefore * 9 / 10, "size khong giam dang ke sau DELETE");
        assertTrue(deadTuples > 0, "DELETE sinh ra dead tuple, autovacuum phai don sau");

        // --- Cach 2: DROP PARTITION thang 1 ---
        long startDrop = System.nanoTime();
        Pg.exec("DROP TABLE lab02.results_2026_01");
        long dropMs = (System.nanoTime() - startDrop) / 1_000_000;

        System.out.printf("=== [4] DROP PARTITION 50k: %d ms, khong dead tuple, disk tra lai NGAY%n", dropMs);

        assertEquals(100_000L, Pg.count("lab02.test_results"), "con lai thang 3 va 4");

        // Day la ly do THAT SU de partition: retention policy tro thanh mot lenh DDL o(1)
        // thay vi mot job DELETE chay hang gio, sinh bloat, va lam autovacuum bao.
    }

    @Test
    @Order(5)
    @DisplayName("BAY: insert ngoai moi range -> NEM. DEFAULT partition la cai bay tiep theo")
    void missingPartitionThrowsAndDefaultPartitionIsATrap() {
        // Khong co partition cho 2026-06 -> insert that bai.
        var ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> Pg.exec(
                        "INSERT INTO lab02.test_results (run_id, status, created_at) "
                                + "VALUES (1, 'PASSED', '2026-06-15')"));
        assertTrue(ex.getMessage().contains("no partition"), ex.getMessage());

        // "Giai phap" thuong thay: tao DEFAULT partition de khong bao gio loi nua.
        Pg.exec("CREATE TABLE lab02.results_default PARTITION OF lab02.test_results DEFAULT");
        Pg.exec("INSERT INTO lab02.test_results (run_id, status, created_at) VALUES (1, 'PASSED', '2026-06-15')");
        assertEquals(1L, Pg.count("lab02.results_default"));

        // NHUNG day la bay: default partition phinh dan im lang, va tu do
        // KHONG THE tao partition moi cho range da co du lieu trong default ma khong khoa bang
        // (Postgres phai quet default partition de kiem tra). Bang cang lon, khoa cang lau.
        var conflict = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> Pg.exec(
                        "CREATE TABLE lab02.results_2026_06 PARTITION OF lab02.test_results "
                                + "FOR VALUES FROM ('2026-06-01') TO ('2026-07-01')"));
        System.out.println("=== [5] default partition chan viec tao partition moi ===\n" + conflict.getMessage());

        // Cach dung: co JOB TU DONG tao partition truoc han (vi du pg_partman, hoac cron tu viet),
        // va dung default partition CHI de bat loi + canh bao, khong de no phinh.
    }
}
