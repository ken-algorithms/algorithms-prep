package com.prep.onbai.pgconcurrency;

import static com.prep.onbai.pgconcurrency.Tx.READ_COMMITTED;
import static com.prep.onbai.pgconcurrency.Tx.REPEATABLE_READ;
import static com.prep.onbai.pgconcurrency.Tx.SERIALIZABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.prep.onbai.testsupport.Locks;
import com.prep.onbai.testsupport.Pg;
import java.sql.SQLException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Bai 04 — moi di thuong duoc dung lai bang hai transaction that, xen ke theo thu tu co dinh.
 * Khong sleep doan mo: lenh nao bi chan thi chay async va test CHO thay no trong pg_stat_activity.
 */
class IsolationAnomaliesTest {

    static final String READ = "SELECT balance FROM counter_account WHERE id = 1";

    DataSource ds;
    JdbcTemplate jdbc;

    @BeforeEach
    void fresh() {
        ds = Pg.freshDatabase("b04_anomalies", "pgconcurrency.sql");
        jdbc = new JdbcTemplate(ds);
        jdbc.update("INSERT INTO counter_account (id, balance) VALUES (1, 100)");
        jdbc.update("INSERT INTO household VALUES (1)");
        jdbc.update("INSERT INTO member_account VALUES (1, 1, 50), (2, 1, 50)");
    }

    int balance() {
        return jdbc.queryForObject(READ, Integer.class);
    }

    int householdTotal() {
        return jdbc.queryForObject("SELECT sum(balance) FROM member_account WHERE household = 1", Integer.class);
    }

    // ── Lost update va 4 cach sua ──────────────────────────────────────────────────────────────

    @Test
    void lostUpdateAtReadCommitted() throws Exception {
        try (Tx t1 = Tx.begin(ds, READ_COMMITTED); Tx t2 = Tx.begin(ds, READ_COMMITTED)) {
            int a = t1.queryInt(READ);                                               // T1 doc 100
            int b = t2.queryInt(READ);                                               // T2 doc 100
            t1.update("UPDATE counter_account SET balance = ? WHERE id = 1", a + 10);
            t1.commit();                                                             // 110
            t2.update("UPDATE counter_account SET balance = ? WHERE id = 1", b + 10); // ghi 110 tu so CU
            t2.commit();
        }
        System.out.println("[LAB] lost update o READ COMMITTED: 100 +10 +10 = " + balance());
        assertThat(balance()).isEqualTo(110); // mat mot lan +10, khong loi nao
    }

    @Test
    void fix1AtomicUpdateLetsTheDatabaseDoTheReadModifyWrite() throws Exception {
        try (Tx t1 = Tx.begin(ds, READ_COMMITTED); Tx t2 = Tx.begin(ds, READ_COMMITTED)) {
            t1.update("UPDATE counter_account SET balance = balance + 10 WHERE id = 1");
            Future<Integer> f = t2.async(() -> t2.update("UPDATE counter_account SET balance = balance + 10 WHERE id = 1"));
            Locks.awaitWaiters(ds, 1); // T2 cho khoa dong cua T1
            t1.commit();
            f.get(5, TimeUnit.SECONDS); // T2 doc LAI dong moi nhat (110) roi cong
            t2.commit();
        }
        assertThat(balance()).isEqualTo(120);
    }

    @Test
    void fix2SelectForUpdateSerializesTheReadModifyWrite() throws Exception {
        try (Tx t1 = Tx.begin(ds, READ_COMMITTED); Tx t2 = Tx.begin(ds, READ_COMMITTED)) {
            int a = t1.queryInt(READ + " FOR UPDATE");
            Future<Integer> fb = t2.async(() -> t2.queryInt(READ + " FOR UPDATE"));
            Locks.awaitWaiters(ds, 1);
            t1.update("UPDATE counter_account SET balance = ? WHERE id = 1", a + 10);
            t1.commit();
            int b = fb.get(5, TimeUnit.SECONDS); // 110: T2 doc SAU khi T1 commit
            t2.update("UPDATE counter_account SET balance = ? WHERE id = 1", b + 10);
            t2.commit();
            assertThat(b).isEqualTo(110);
        }
        assertThat(balance()).isEqualTo(120);
    }

    @Test
    void fix3OptimisticVersionDetectsTheConflictAndTheLoserRetries() throws Exception {
        String cas = "UPDATE counter_account SET balance = ?, version = version + 1 WHERE id = 1 AND version = ?";
        try (Tx t1 = Tx.begin(ds, READ_COMMITTED); Tx t2 = Tx.begin(ds, READ_COMMITTED)) {
            int[] r1 = t1.queryRow("SELECT balance, version FROM counter_account WHERE id = 1");
            int[] r2 = t2.queryRow("SELECT balance, version FROM counter_account WHERE id = 1");
            assertThat(t1.update(cas, r1[0] + 10, r1[1])).isEqualTo(1);
            t1.commit();
            assertThat(t2.update(cas, r2[0] + 10, r2[1])).isZero(); // version da doi -> 0 dong: XUNG DOT
            t2.rollback();
        }
        try (Tx retry = Tx.begin(ds, READ_COMMITTED)) { // retry = transaction MOI, doc lai tu dau
            int[] r = retry.queryRow("SELECT balance, version FROM counter_account WHERE id = 1");
            assertThat(retry.update(cas, r[0] + 10, r[1])).isEqualTo(1);
            retry.commit();
        }
        assertThat(balance()).isEqualTo(120);
    }

    @Test
    void fix4RepeatableReadAbortsTheLoserWith40001() throws Exception {
        try (Tx t1 = Tx.begin(ds, REPEATABLE_READ); Tx t2 = Tx.begin(ds, REPEATABLE_READ)) {
            int a = t1.queryInt(READ);
            int b = t2.queryInt(READ);
            t1.update("UPDATE counter_account SET balance = ? WHERE id = 1", a + 10);
            t1.commit();
            SQLException e = assertThrows(SQLException.class,
                    () -> t2.update("UPDATE counter_account SET balance = ? WHERE id = 1", b + 10));
            System.out.println("[LAB] REPEATABLE READ: " + e.getSQLState() + " " + firstLine(e));
            assertThat(e.getSQLState()).isEqualTo("40001");
            t2.rollback();
        }
        try (Tx retry = Tx.begin(ds, REPEATABLE_READ)) {
            retry.update("UPDATE counter_account SET balance = ? WHERE id = 1", retry.queryInt(READ) + 10);
            retry.commit();
        }
        assertThat(balance()).isEqualTo(120);
    }

    // ── Write skew: quy tac tren NHIEU dong ────────────────────────────────────────────────────

    static final String HOUSEHOLD_SUM = "SELECT sum(balance) FROM member_account WHERE household = 1";
    static final String WITHDRAW_90 = "UPDATE member_account SET balance = balance - 90 WHERE id = ?";

    @ParameterizedTest(name = "isolation {0}")
    @ValueSource(ints = {READ_COMMITTED, REPEATABLE_READ})
    void writeSkewPassesReadCommittedAndRepeatableRead(int isolation) throws Exception {
        try (Tx t1 = Tx.begin(ds, isolation); Tx t2 = Tx.begin(ds, isolation)) {
            assertThat(t1.queryInt(HOUSEHOLD_SUM)).isEqualTo(100); // 100 - 90 >= 0: OK
            assertThat(t2.queryInt(HOUSEHOLD_SUM)).isEqualTo(100); // OK
            t1.update(WITHDRAW_90, 1);                             // hai ben ghi HAI DONG KHAC NHAU
            t2.update(WITHDRAW_90, 2);                             // -> khong ai cho ai, khong xung dot
            t1.commit();
            t2.commit();
        }
        String name = isolation == READ_COMMITTED ? "READ COMMITTED" : "REPEATABLE READ";
        System.out.println("[LAB] write skew o " + name + ": tong cua ho = " + householdTotal());
        assertThat(householdTotal()).isEqualTo(-80);
    }

    @Test
    void serializableCatchesWriteSkewOnlyIfBothSidesAreSerializable() throws Exception {
        String outcome;
        try (Tx t1 = Tx.begin(ds, SERIALIZABLE); Tx t2 = Tx.begin(ds, SERIALIZABLE)) {
            t1.queryInt(HOUSEHOLD_SUM);
            t2.queryInt(HOUSEHOLD_SUM);
            t1.update(WITHDRAW_90, 1);
            t2.update(WITHDRAW_90, 2);
            t1.commit();
            SQLException e = assertThrows(SQLException.class, t2::commit);
            outcome = e.getSQLState() + " " + firstLine(e);
            assertThat(e.getSQLState()).isEqualTo("40001");
        }
        assertThat(householdTotal()).isEqualTo(10); // chi mot ben qua

        jdbc.update("UPDATE member_account SET balance = 50");
        try (Tx t1 = Tx.begin(ds, SERIALIZABLE); Tx t2 = Tx.begin(ds, READ_COMMITTED)) { // mot ben quen SERIALIZABLE
            t1.queryInt(HOUSEHOLD_SUM);
            t2.queryInt(HOUSEHOLD_SUM);
            t1.update(WITHDRAW_90, 1);
            t2.update(WITHDRAW_90, 2);
            t1.commit();
            t2.commit(); // SSI chi theo doi cac transaction SERIALIZABLE
        }
        System.out.println("[LAB] SERIALIZABLE ca hai: " + outcome + " | mot ben READ COMMITTED: tong = " + householdTotal());
        assertThat(householdTotal()).isEqualTo(-80);
    }

    @Test
    void writeSkewFixedByLockingTheRowThatRepresentsTheRule() throws Exception {
        String lockHousehold = "SELECT id FROM household WHERE id = 1 FOR UPDATE"; // "vat chi diem" cho quy tac
        try (Tx t1 = Tx.begin(ds, READ_COMMITTED); Tx t2 = Tx.begin(ds, READ_COMMITTED)) {
            t1.queryInt(lockHousehold);
            Future<Integer> t2Sum = t2.async(() -> {
                t2.queryInt(lockHousehold);       // cho T1
                return t2.queryInt(HOUSEHOLD_SUM); // doc SAU khi T1 commit
            });
            Locks.awaitWaiters(ds, 1);
            assertThat(t1.queryInt(HOUSEHOLD_SUM)).isEqualTo(100);
            t1.update(WITHDRAW_90, 1);
            t1.commit();
            int seen = t2Sum.get(5, TimeUnit.SECONDS);
            assertThat(seen).isEqualTo(10);   // 10 - 90 < 0 -> T2 tu choi
            t2.rollback();
        }
        assertThat(householdTotal()).isEqualTo(10);
    }

    // ── Khong cho mai: lock_timeout, NOWAIT, SKIP LOCKED ──────────────────────────────────────

    @Test
    void lockTimeoutNowaitAndSkipLockedNeverWaitForever() throws Exception {
        try (Tx holder = Tx.begin(ds, READ_COMMITTED); Tx a = Tx.begin(ds, READ_COMMITTED);
             Tx b = Tx.begin(ds, READ_COMMITTED); Tx c = Tx.begin(ds, READ_COMMITTED)) {
            holder.queryInt(READ + " FOR UPDATE");

            a.execute("SET LOCAL lock_timeout = '200ms'");
            long t0 = System.nanoTime();
            SQLException timeout = assertThrows(SQLException.class, () -> a.queryInt(READ + " FOR UPDATE"));
            long waited = (System.nanoTime() - t0) / 1_000_000;

            SQLException nowait = assertThrows(SQLException.class, () -> b.queryInt(READ + " FOR UPDATE NOWAIT"));
            int skipped = c.queryInt(READ + " FOR UPDATE SKIP LOCKED");

            System.out.printf("[LAB] lock_timeout: %s sau %d ms | NOWAIT: %s | SKIP LOCKED: %s%n",
                    timeout.getSQLState(), waited, nowait.getSQLState(), skipped == Integer.MIN_VALUE ? "0 dong" : skipped);
            assertThat(timeout.getSQLState()).isEqualTo("55P03");
            assertThat(waited).isBetween(150L, 3000L);
            assertThat(nowait.getSQLState()).isEqualTo("55P03");
            assertThat(skipped).isEqualTo(Integer.MIN_VALUE);
        }
    }

    private static String firstLine(SQLException e) {
        return e.getMessage().lines().findFirst().orElse("");
    }
}
