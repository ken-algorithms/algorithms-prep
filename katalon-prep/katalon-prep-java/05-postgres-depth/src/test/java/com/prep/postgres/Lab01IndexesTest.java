package com.prep.postgres;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * LAB 01 — INDEX: chung minh bang query plan THAT, khong phai ly thuyet.
 *
 * <p>Bang {@code test_results} mo phong dung du lieu Katalon: hang tram trieu ket qua test, doc
 * nhieu, filter theo run/status/thoi gian.
 *
 * <p>Test o day co thu tu ({@code @Order}) vi chung xay dan len nhau: do plan truoc khi co index,
 * roi tao index, roi do lai. Do la dung cach ban se lam khi tuning that.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class Lab01IndexesTest {

    /** 300k dong: du lon de planner chon Seq Scan khi khong co index, du nho de test nhanh. */
    private static final int ROWS = 300_000;

    @BeforeAll
    static void loadData() {
        Pg.exec(
                "DROP SCHEMA IF EXISTS lab01 CASCADE",
                "CREATE SCHEMA lab01",
                """
                CREATE TABLE lab01.test_results (
                    id           bigserial PRIMARY KEY,
                    run_id       bigint      NOT NULL,
                    test_name    text        NOT NULL,
                    status       text        NOT NULL,
                    duration_ms  int         NOT NULL,
                    created_at   timestamptz NOT NULL,
                    metadata     jsonb       NOT NULL DEFAULT '{}'::jsonb
                )
                """,
                // generate_series: cach nhanh nhat de sinh du lieu test lon trong Postgres.
                // status lech co y: 94%% PASSED, 5%% FAILED, 1%% SKIPPED -> giong production,
                // va la dieu kien de partial index phat huy tac dung.
                """
                INSERT INTO lab01.test_results (run_id, test_name, status, duration_ms, created_at, metadata)
                SELECT
                    (i %% 5000) + 1,
                    'suite' || (i %% 50) || '.test_' || (i %% 997),
                    CASE WHEN i %% 100 < 94 THEN 'PASSED'
                         WHEN i %% 100 < 99 THEN 'FAILED'
                         ELSE 'SKIPPED' END,
                    (random() * 5000)::int,
                    timestamptz '2026-01-01' + (i %% 200) * interval '1 day',
                    jsonb_build_object('browser', CASE WHEN i %% 3 = 0 THEN 'chrome' ELSE 'firefox' END)
                FROM generate_series(1, %d) AS s(i)
                """
                        .formatted(ROWS),
                // ANALYZE la BUOC BAT BUOC sau khi load du lieu lon. Khong ANALYZE thi planner
                // dung statistics cu (hoac rong) va chon plan sai. Day la nguyen nhan that cua
                // rat nhieu "query cham sau khi migrate du lieu".
                "ANALYZE lab01.test_results");
    }

    @Test
    @Order(1)
    @DisplayName("khong index -> Seq Scan quet ca bang, du chi lay 1 run")
    void withoutIndexPlannerMustScanEverything() {
        String plan = Pg.explain("SELECT * FROM lab01.test_results WHERE run_id = 42");

        assertTrue(Pg.usesSeqScan(plan), plan);
        assertFalse(Pg.usesIndex(plan), plan);
        System.out.println("=== [1] khong index ===\n" + plan);
    }

    @Test
    @Order(2)
    @DisplayName("B-tree tren run_id -> doi sang Index Scan")
    void btreeIndexChangesThePlan() {
        Pg.exec(
                "CREATE INDEX idx_results_run ON lab01.test_results (run_id)",
                "ANALYZE lab01.test_results");

        String plan = Pg.explain("SELECT * FROM lab01.test_results WHERE run_id = 42");

        assertTrue(Pg.usesIndex(plan), plan);
        assertTrue(Pg.indexesUsed(plan).contains("idx_results_run"), Pg.indexesUsed(plan).toString());
        System.out.println("=== [2] co B-tree ===\n" + plan);
    }

    @Test
    @Order(3)
    @DisplayName("COMPOSITE INDEX: quy tac leftmost prefix - thu tu cot quyet dinh index co dung duoc")
    void compositeIndexLeftmostPrefixRule() {
        Pg.exec(
                "CREATE INDEX idx_results_run_status ON lab01.test_results (run_id, status)",
                "ANALYZE lab01.test_results");

        // (a) Filter ca hai cot -> dung duoc index.
        String both = Pg.explainPlanOnly(
                "SELECT * FROM lab01.test_results WHERE run_id = 42 AND status = 'FAILED'");
        assertTrue(Pg.usesIndex(both), both);

        // (b) Filter chi cot DAU (run_id) -> VAN dung duoc (leftmost prefix).
        String leftOnly = Pg.explainPlanOnly("SELECT * FROM lab01.test_results WHERE run_id = 42");
        assertTrue(Pg.usesIndex(leftOnly), leftOnly);

        // (c) Filter chi cot THU HAI (status) -> KHONG dung duoc index composite nay.
        //     Day la loi thiet ke index pho bien nhat: nguoi ta tao (a,b) roi tuong query theo b
        //     cung nhanh. Khong. Phai co index rieng cho b, hoac dao thu tu.
        String rightOnly = Pg.explainPlanOnly(
                "SELECT count(*) FROM lab01.test_results WHERE status = 'SKIPPED'");
        System.out.println("=== [3c] chi filter cot thu hai ===\n" + rightOnly);
        assertFalse(
                Pg.indexesUsed(rightOnly).contains("idx_results_run_status"),
                "index (run_id, status) khong the phuc vu query chi co status: " + rightOnly);
    }

    @Test
    @Order(4)
    @DisplayName("PARTIAL INDEX: chi index 5% dong FAILED -> nho hon B-tree day hang chuc lan")
    void partialIndexIsDramaticallySmaller() {
        Pg.exec(
                "CREATE INDEX idx_results_status_full ON lab01.test_results (status)",
                """
                CREATE INDEX idx_results_failed_partial ON lab01.test_results (run_id)
                WHERE status = 'FAILED'
                """,
                "ANALYZE lab01.test_results");

        long full = Pg.indexSizeBytes("lab01.idx_results_status_full");
        long partial = Pg.indexSizeBytes("lab01.idx_results_failed_partial");

        System.out.printf(
                "=== [4] index size: full=%s  partial=%s  (nho hon %.1fx)%n",
                Pg.humanSize(full), Pg.humanSize(partial), (double) full / partial);

        // Chi 5% dong la FAILED -> partial index nho hon nhieu lan.
        assertTrue(partial * 5 < full, "partial=%d full=%d".formatted(partial, full));

        // Va no VAN duoc dung cho query co dung dieu kien WHERE do.
        String plan = Pg.explain(
                "SELECT * FROM lab01.test_results WHERE status = 'FAILED' AND run_id = 42");
        assertTrue(Pg.usesIndex(plan), plan);
        System.out.println("=== [4] partial index plan ===\n" + plan);
    }

    @Test
    @Order(5)
    @DisplayName("COVERING INDEX (INCLUDE) -> Index Only Scan, khong cham heap")
    void coveringIndexEnablesIndexOnlyScan() {
        // Query chi can run_id + duration_ms. Neu index chua ca hai thi Postgres khong can doc heap.
        Pg.exec(
                """
                CREATE INDEX idx_results_covering ON lab01.test_results (run_id) INCLUDE (duration_ms)
                """,
                // VACUUM la BAT BUOC de Index Only Scan hoat dong: no cap nhat visibility map.
                // Khong VACUUM thi Postgres van phai cham heap de kiem tra visibility -> mat tac dung.
                "VACUUM ANALYZE lab01.test_results");

        String plan = Pg.explain(
                "SELECT run_id, duration_ms FROM lab01.test_results WHERE run_id BETWEEN 100 AND 110");

        System.out.println("=== [5] covering index ===\n" + plan);
        assertTrue(Pg.usesIndexOnlyScan(plan), "mong doi Index Only Scan:\n" + plan);
    }

    @Test
    @Order(6)
    @DisplayName("BRIN cho cot thoi gian append-only -> nho hon B-tree hang tram lan")
    void brinIsTinyForTimeOrderedData() {
        Pg.exec(
                "CREATE INDEX idx_results_created_btree ON lab01.test_results (created_at)",
                "CREATE INDEX idx_results_created_brin ON lab01.test_results USING brin (created_at)",
                "ANALYZE lab01.test_results");

        long btree = Pg.indexSizeBytes("lab01.idx_results_created_btree");
        long brin = Pg.indexSizeBytes("lab01.idx_results_created_brin");

        System.out.printf(
                "=== [6] created_at: btree=%s  brin=%s  (BRIN nho hon %.0fx)%n",
                Pg.humanSize(btree), Pg.humanSize(brin), (double) btree / brin);

        // BRIN chi luu min/max moi 128 page -> nho hon hang tram lan.
        // Danh doi: BRIN chi hieu qua khi du lieu SAP XEP VAT LY theo cot do (append-only theo
        // thoi gian). Voi du lieu ngau nhien thi BRIN vo dung. Day la trade-off phai noi duoc.
        assertTrue(brin * 50 < btree, "btree=%d brin=%d".formatted(btree, brin));
    }

    @Test
    @Order(7)
    @DisplayName("GIN cho jsonb -> query theo field trong JSON dung duoc index")
    void ginIndexForJsonb() {
        Pg.exec(
                "CREATE INDEX idx_results_metadata_gin ON lab01.test_results USING gin (metadata)",
                "ANALYZE lab01.test_results");

        String plan = Pg.explain(
                "SELECT count(*) FROM lab01.test_results WHERE metadata @> '{\"browser\":\"chrome\"}'");

        System.out.println("=== [7] GIN jsonb ===\n" + plan);
        assertTrue(Pg.usesIndex(plan), plan);
    }

    @Test
    @Order(8)
    @DisplayName("uoc luong vs thuc te: rows= lech nhieu = statistics cu -> phai ANALYZE")
    void plannerEstimateVersusActual() {
        // Them 100k dong MOI ma KHONG ANALYZE -> statistics lac hau.
        Pg.exec(
                """
                INSERT INTO lab01.test_results (run_id, test_name, status, duration_ms, created_at)
                SELECT 99999, 'new.test_' || i, 'PASSED', 10,
                       timestamptz '2026-07-01' + (i % 10) * interval '1 day'
                FROM generate_series(1, 100000) AS s(i)
                """);

        String stale = Pg.explain("SELECT * FROM lab01.test_results WHERE run_id = 99999");
        long actualBefore = Pg.actualRows(stale);
        System.out.println("=== [8a] TRUOC ANALYZE (statistics cu) ===\n" + stale);

        Pg.exec("ANALYZE lab01.test_results");
        String fresh = Pg.explain("SELECT * FROM lab01.test_results WHERE run_id = 99999");
        System.out.println("=== [8b] SAU ANALYZE ===\n" + fresh);

        // Thuc te co 100k dong run_id=99999.
        assertEquals(100_000L, Pg.count("lab01.test_results WHERE run_id = 99999"));
        assertTrue(actualBefore > 0, "phai doc duoc actual rows tu plan");

        // Bai hoc: `rows=` trong EXPLAIN la UOC LUONG. Khi uoc luong lech xa actual, planner chon
        // sai kieu join / sai thu tu -> query cham gap hang chuc lan. Viec dau tien khi tuning
        // KHONG phai them index, ma la kiem tra statistics con dung khong.
    }
}
