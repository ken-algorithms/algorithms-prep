package com.prep.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * LAB 03 — MVCC / bloat / locking.
 *
 * <p>Hai chu de, ca hai deu la cau hoi phong van kinh dien:
 *
 * <ol>
 *   <li><b>MVCC & bloat:</b> vi sao {@code UPDATE} lam bang PHINH ra, va vi sao {@code VACUUM}
 *       KHONG tra lai disk. Day la cau hoi loc rat tot giua "biet dung Postgres" va "hieu Postgres".
 *   <li><b>{@code FOR UPDATE SKIP LOCKED}:</b> pattern queue chuan de giao test cho worker, cuc ky
 *       on-domain voi Katalon (distributed test execution).
 * </ol>
 */
class Lab03MvccLockingTest {

    @BeforeAll
    static void setUp() {
        Pg.exec(
                "DROP SCHEMA IF EXISTS lab03 CASCADE",
                "CREATE SCHEMA lab03",
                """
                CREATE TABLE lab03.results (
                    id          bigserial PRIMARY KEY,
                    status      text NOT NULL,
                    payload     text NOT NULL
                )
                """,
                """
                INSERT INTO lab03.results (status, payload)
                SELECT 'PENDING', repeat('x', 200)
                FROM generate_series(1, 100000)
                """,
                // autovacuum TAT tren bang nay de quan sat bloat mot cach tuong minh.
                // (Trong production KHONG BAO GIO tat autovacuum.)
                "ALTER TABLE lab03.results SET (autovacuum_enabled = false)",
                "VACUUM ANALYZE lab03.results");
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    @DisplayName("MVCC & bloat")
    class MvccAndBloat {

        @Test
        @Order(1)
        @DisplayName("UPDATE 100k dong lam bang PHINH GAN GAP DOI (update = insert moi + danh dau cu la dead)")
        void updateDoublesTableSize() {
            long before = Pg.tableSizeBytes("lab03.results");

            // UPDATE tat ca 100k dong, KHONG doi do dai payload -> ve logic khong them du lieu nao.
            Pg.exec("UPDATE lab03.results SET status = 'RUNNING'");

            long after = Pg.tableSizeBytes("lab03.results");
            long dead = deadTuples();

            System.out.printf(
                    "=== [1] UPDATE 100k: size %s -> %s (+%.0f%%), dead_tup=%d%n",
                    Pg.humanSize(before), Pg.humanSize(after),
                    (after - before) * 100.0 / before, dead);

            // Postgres KHONG sua dong tai cho. No ghi mot phien ban MOI va danh dau phien ban cu
            // la dead (de cac transaction dang chay van doc duoc ban cu — do la MVCC).
            // Ket qua: bang gan gap doi du so dong khong doi.
            assertTrue(after > before * 3 / 2, "before=%d after=%d".formatted(before, after));
            assertEquals(100_000L, dead, "moi dong cu tro thanh mot dead tuple");
            assertEquals(100_000L, Pg.count("lab03.results"), "so dong LOGIC khong doi");
        }

        @Test
        @Order(2)
        @DisplayName("VACUUM don dead tuple nhung KHONG tra disk lai cho OS - chi tai su dung ben trong")
        void vacuumReclaimsSpaceForReuseButDoesNotShrinkTheFile() {
            long beforeVacuum = Pg.tableSizeBytes("lab03.results");

            Pg.exec("VACUUM lab03.results");

            long afterVacuum = Pg.tableSizeBytes("lab03.results");
            long dead = deadTuples();

            System.out.printf(
                    "=== [2] VACUUM: size %s -> %s (KHONG giam), dead_tup=%d%n",
                    Pg.humanSize(beforeVacuum), Pg.humanSize(afterVacuum), dead);

            // dead tuple da duoc GIAI PHONG (khong gian trong bang duoc tai su dung cho INSERT sau),
            // NHUNG file tren disk khong nho lai.
            assertEquals(0L, dead, "VACUUM da don dead tuple");
            assertEquals(beforeVacuum, afterVacuum, "VACUUM thuong KHONG tra disk lai cho OS");

            // Day chinh la cau tra loi cho "toi DELETE ca trieu dong ma disk khong giam":
            // VACUUM chi danh dau khong gian de TAI SU DUNG. Muon tra disk phai VACUUM FULL
            // (khoa ACCESS EXCLUSIVE, khong dung duoc o production) hoac pg_repack (online).
            // Hoac tot nhat: dung PARTITION roi DROP PARTITION — xem Lab 02.
        }

        @Test
        @Order(3)
        @DisplayName("VACUUM FULL tra disk lai - nhung KHOA bang, nen khong dung duoc o production")
        void vacuumFullShrinksButLocks() {
            long before = Pg.tableSizeBytes("lab03.results");
            Pg.exec("VACUUM FULL lab03.results");
            long after = Pg.tableSizeBytes("lab03.results");

            System.out.printf("=== [3] VACUUM FULL: size %s -> %s%n", Pg.humanSize(before), Pg.humanSize(after));

            // VACUUM FULL ghi lai ca bang -> file nho lai gan bang kich thuoc "sach".
            assertTrue(after < before * 3 / 4, "before=%d after=%d".formatted(before, after));

            // Danh doi: no lay ACCESS EXCLUSIVE LOCK — moi SELECT/INSERT/UPDATE deu bi chan cho
            // den khi xong. Voi bang 500GB thi la nhieu gio downtime. Do la ly do cau tra loi dung
            // cho cau hoi "lam sao thu hoi disk" o production KHONG phai VACUUM FULL, ma la
            // pg_repack (online, khong khoa) hoac thiet ke partition tu dau.
        }

        private long deadTuples() {
            return Pg.scalar(
                    "SELECT n_dead_tup FROM pg_stat_user_tables "
                            + "WHERE schemaname = 'lab03' AND relname = 'results'",
                    Long.class);
        }
    }

    @Nested
    @DisplayName("FOR UPDATE SKIP LOCKED - queue giao test cho worker")
    class WorkerQueue {

        @BeforeAll
        static void resetQueue() {
            Pg.exec(
                    "DROP TABLE IF EXISTS lab03.queue",
                    """
                    CREATE TABLE lab03.queue (
                        id       bigserial PRIMARY KEY,
                        status   text NOT NULL DEFAULT 'PENDING',
                        claimed_by text
                    )
                    """,
                    "INSERT INTO lab03.queue (status) SELECT 'PENDING' FROM generate_series(1, 100)");
        }

        /** Mot worker "nhan viec": lay N job dang PENDING, bo qua job worker khac dang giu. */
        private static List<Long> claim(Connection conn, String workerId, int batchSize) throws SQLException {
            var claimed = new ArrayList<Long>();
            String sql =
                    """
                    SELECT id FROM lab03.queue
                    WHERE status = 'PENDING'
                    ORDER BY id
                    LIMIT %d
                    FOR UPDATE SKIP LOCKED
                    """
                            .formatted(batchSize);
            try (Statement st = conn.createStatement();
                    ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    claimed.add(rs.getLong(1));
                }
            }
            if (!claimed.isEmpty()) {
                String ids = claimed.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElseThrow();
                try (Statement st = conn.createStatement()) {
                    st.executeUpdate(
                            "UPDATE lab03.queue SET status = 'CLAIMED', claimed_by = '%s' WHERE id IN (%s)"
                                    .formatted(workerId, ids));
                }
            }
            return claimed;
        }

        @Test
        @DisplayName("hai worker chay dong thoi nhan duoc cac job KHONG TRUNG NHAU, khong ai phai cho")
        void twoWorkersClaimDisjointJobs() throws SQLException {
            try (Connection worker1 = Pg.connection();
                    Connection worker2 = Pg.connection()) {
                worker1.setAutoCommit(false);
                worker2.setAutoCommit(false);

                // worker1 giu 10 job dau (chua commit).
                List<Long> batch1 = claim(worker1, "w1", 10);

                // worker2 chay NGAY LUC DO. SKIP LOCKED lam no BO QUA 10 dong worker1 dang giu
                // va lay 10 dong tiep theo — khong cho, khong deadlock.
                List<Long> batch2 = claim(worker2, "w2", 10);

                worker1.commit();
                worker2.commit();

                System.out.println("=== w1 claimed: " + batch1);
                System.out.println("=== w2 claimed: " + batch2);

                assertEquals(10, batch1.size());
                assertEquals(10, batch2.size());

                // Khong co job nao bi giao cho ca hai worker — day la tinh chat quan trong nhat.
                var overlap = new HashSet<>(batch1);
                overlap.retainAll(new HashSet<>(batch2));
                assertTrue(overlap.isEmpty(), "job bi giao trung: " + overlap);

                assertEquals(20L, Pg.count("lab03.queue WHERE status = 'CLAIMED'"));
            }
        }

        @Test
        @DisplayName("KHONG co SKIP LOCKED -> worker thu hai bi CHAN (chung minh bang lock_timeout)")
        void withoutSkipLockedTheSecondWorkerBlocks() throws SQLException {
            Pg.exec("UPDATE lab03.queue SET status = 'PENDING', claimed_by = NULL");

            try (Connection worker1 = Pg.connection();
                    Connection worker2 = Pg.connection()) {
                worker1.setAutoCommit(false);
                worker2.setAutoCommit(false);

                // worker1 giu row id=1.
                try (Statement st = worker1.createStatement()) {
                    st.executeQuery("SELECT id FROM lab03.queue WHERE id = 1 FOR UPDATE").close();
                }

                // worker2 xin dung row do, KHONG co SKIP LOCKED.
                // Dat lock_timeout de test khong treo mai mai — va chinh viec no timeout la BANG CHUNG
                // rang no dang bi chan.
                try (Statement st = worker2.createStatement()) {
                    st.execute("SET lock_timeout = '500ms'");
                    var ex = assertThrows(
                            SQLException.class,
                            () -> st.executeQuery("SELECT id FROM lab03.queue WHERE id = 1 FOR UPDATE"));
                    System.out.println("=== [khong SKIP LOCKED] worker2 bi chan: " + ex.getMessage());
                    assertTrue(
                            ex.getMessage().toLowerCase().contains("lock timeout")
                                    || ex.getMessage().toLowerCase().contains("canceling statement"),
                            ex.getMessage());
                }

                worker1.rollback();
                worker2.rollback();
            }

            // BAI HOC: mot queue viet bang `FOR UPDATE` thuong se bien N worker thanh 1 worker —
            // tat ca xep hang sau nhau tren cung nhung dong dau tien. Throughput khong tang du ban
            // scale worker len 50. `SKIP LOCKED` la thu bien no thanh queue that su song song.
        }

        @Test
        @DisplayName("job dang duoc giu KHONG bi worker khac lay lai (khong xu ly 2 lan)")
        void claimedJobsAreNotHandedOutTwice() throws SQLException {
            Pg.exec("UPDATE lab03.queue SET status = 'PENDING', claimed_by = NULL");

            try (Connection worker1 = Pg.connection()) {
                worker1.setAutoCommit(false);
                List<Long> first = claim(worker1, "w1", 100);
                worker1.commit();
                assertEquals(100, first.size());
            }

            // Het job PENDING -> worker tiep theo nhan duoc list rong (khong nem, khong cho).
            try (Connection worker2 = Pg.connection()) {
                worker2.setAutoCommit(false);
                List<Long> second = claim(worker2, "w2", 10);
                worker2.commit();
                assertTrue(second.isEmpty(), "khong con job nao de nhan");
            }

            assertEquals(100L, Pg.count("lab03.queue WHERE claimed_by = 'w1'"));
            assertEquals(0L, Pg.count("lab03.queue WHERE claimed_by = 'w2'"));
        }
    }
}
