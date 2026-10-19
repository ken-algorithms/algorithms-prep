package com.prep.cs.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import com.prep.cs.Infra;
import com.prep.cs.ledger.db.BalanceReads;
import com.prep.cs.ledger.db.LedgerDb;

/**
 * Buoc 5: tai khoan co 1 trieu posting (moi 20 giay mot posting tu 01/01/2026). Do bon cach doc so du va hai
 * cach phan trang sao ke, moi cach chay 7 lan lay trung vi. Kem EXPLAIN de thay Postgres lam gi.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BalanceReadTest {

    static final long ACC = 7, N = 1_000_000;
    LedgerDb db;

    @BeforeAll
    void seed() throws Exception {
        DataSource ds = Infra.requirePostgres();
        db = new LedgerDb(ds);
        db.reset();
        try (Connection c = db.connect(); Statement st = c.createStatement()) {
            long t0 = System.nanoTime();
            st.execute("INSERT INTO l_account (id) VALUES (7)");
            st.execute("INSERT INTO l_account (id, allow_negative) VALUES (9, true)");   // doi ung cua but toan lon
            st.execute("INSERT INTO l_journal (idem_key, request_hash) VALUES ('bulk', 'bulk')");
            // 1 trieu posting cho tai khoan 7 + 1 trieu dong doi ung cho tai khoan 9: mot journal can bang
            st.execute("""
                    INSERT INTO l_posting (journal_id, account_id, seq, amount, balance_after, created_at)
                    SELECT 1, a.id, g, a.sign * x.amt, a.sign * sum(x.amt) OVER (PARTITION BY a.id ORDER BY g),
                           timestamptz '2026-01-01 00:00:00+00' + g * interval '20 seconds'
                      FROM generate_series(1, 1000000) g
                      CROSS JOIN LATERAL (SELECT CASE WHEN g % 3 = 0 THEN -40 ELSE 50 END::bigint AS amt) x
                      CROSS JOIN (VALUES (7::bigint, 1), (9::bigint, -1)) a(id, sign)""");
            st.execute("""
                    UPDATE l_account a SET balance = s.total, last_seq = s.n
                      FROM (SELECT account_id, sum(amount) total, count(*) n FROM l_posting GROUP BY account_id) s
                     WHERE s.account_id = a.id""");
            st.execute("ANALYZE l_posting");
            System.out.printf("[LAB] nap 2 trieu posting trong %.1f s%n", (System.nanoTime() - t0) / 1e9);
        }
    }

    interface Q {
        Object run() throws Exception;
    }

    static String time(String label, Q q) throws Exception {
        long[] ns = new long[7];
        Object v = null;
        for (int i = 0; i < ns.length; i++) {
            long t = System.nanoTime();
            v = q.run();
            ns[i] = System.nanoTime() - t;
        }
        Arrays.sort(ns);
        return String.format("%-44s %9.3f ms   -> %s", label, ns[3] / 1e6, v);
    }

    @Test
    void fourWaysToReadABalance() throws Exception {
        try (Connection c = db.connect()) {
            BalanceReads r = new BalanceReads(c);
            Instant mid = Instant.parse("2026-01-01T00:00:00Z").plusSeconds(360_000L * 20);   // dung luc posting 360.000
            System.out.println("[LAB] " + time("sum: SUM(amount) 1 trieu dong", () -> r.sum(ACC)));
            System.out.println("[LAB] " + time("running: cot balance cua tai khoan", () -> r.running(ACC)));
            System.out.println("[LAB] " + time("at(" + mid + "): posting cuoi <= t", () -> r.at(ACC, mid)));
            System.out.println("[LAB] " + time("sao ke keyset: 50 dong sau seq 900.000", () -> r.statement(ACC, 900_000, 50).size()));
            System.out.println("[LAB] " + time("sao ke OFFSET 900.000 LIMIT 50", () -> offsetPage(c, 900_000)));
            assertThat(r.sum(ACC)).isEqualTo(r.running(ACC));
            long expectedAtMid = 0;
            for (long g = 1; g <= 360_000; g++) expectedAtMid += g % 3 == 0 ? -40 : 50;
            assertThat(r.at(ACC, mid)).isEqualTo(expectedAtMid);
            explain(c, "SELECT coalesce(sum(amount), 0) FROM l_posting WHERE account_id = 7");
            explain(c, """
                    SELECT balance_after FROM l_posting WHERE account_id = 7 AND created_at <= '2026-03-25T08:00:00Z'
                    ORDER BY created_at DESC, seq DESC LIMIT 1""");
        }
    }

    static int offsetPage(Connection c, int offset) throws Exception {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT seq, amount, balance_after FROM l_posting WHERE account_id = 7 "
                     + "ORDER BY seq OFFSET " + offset + " LIMIT 50")) {
            int n = 0;
            while (rs.next()) n++;
            return n;
        }
    }

    static void explain(Connection c, String sql) throws Exception {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("EXPLAIN (ANALYZE, BUFFERS, COSTS OFF, TIMING OFF) " + sql)) {
            System.out.println("[LAB] EXPLAIN " + sql.replaceAll("\\s+", " ").trim());
            while (rs.next()) System.out.println("        " + rs.getString(1));
        }
    }

    @Test
    void reconcileStillCleanAfterBulkLoad() throws Exception {
        List<String> bad = db.reconcile();
        assertThat(bad).isEmpty();
    }
}
