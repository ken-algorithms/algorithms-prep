package com.prep.onbai.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.prep.onbai.ledger.Ledger.InsufficientFunds;
import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Receipt;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.testsupport.Pg;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

/** Bai 05 — doc so du real-time: snapshot + duoi, account nong, read-your-writes. */
class BalanceReadTest {

    static final LocalDate D = LocalDate.of(2026, 10, 18);
    static final long CASH = 1, HOT_CASH = 2, ALICE = 10, BOB = 11;

    DataSource ds;
    LedgerService ledger;
    JdbcTemplate jdbc;

    @BeforeEach
    void fresh() {
        ds = Pg.freshDatabase("b05_read", 32, "ledger.sql");
        ledger = new LedgerService(ds);
        jdbc = new JdbcTemplate(ds);
        ledger.open(CASH, "cash", true, false);
        ledger.open(HOT_CASH, "cash:hot", true, true);
        ledger.open(ALICE, "wallet:alice", false, false);
        ledger.open(BOB, "wallet:bob", false, false);
    }

    Receipt deposit(String key, long from, long to, String amount) {
        return ledger.post(Request.of(key, "DEPOSIT", D, new Leg(from, new BigDecimal(amount).negate()),
                new Leg(to, new BigDecimal(amount))));
    }

    @Test
    void readCostIsBoundedByRefreshNotByHistory() {
        SnapshotBalances snap = new SnapshotBalances(ds);
        for (int i = 0; i < 500; i++) {
            deposit("d:" + i, CASH, ALICE, "1");
        }
        assertThat(snap.read(ALICE).tailRows()).isEqualTo(500); // chua co snapshot: cong ca lich su
        snap.refresh(ALICE);
        for (int i = 0; i < 7; i++) {
            deposit("e:" + i, CASH, ALICE, "1");
        }
        SnapshotBalances.Read r = snap.read(ALICE);
        System.out.printf("[LAB] 507 posting, sau refresh doc chi cong %d dong duoi, so du %s @seq %d%n",
                r.tailRows(), r.balance().stripTrailingZeros().toPlainString(), r.asOfSeq());
        assertThat(r.tailRows()).isEqualTo(7);
        assertThat(r.balance()).isEqualByComparingTo("507");
        assertThat(r.asOfSeq()).isEqualTo(507);
    }

    @Test
    void snapshotPlusTailIsAlwaysConsistentWhileWritersAndRefreshersRun() throws Exception {
        long[] wallets = {10, 11, 12, 13};
        for (long w : wallets) {
            if (w > 11) {
                ledger.open(w, "wallet:" + w, false, false);
            }
            deposit("seed:" + w, CASH, w, "1000");
        }
        SnapshotBalances snap = new SnapshotBalances(ds);
        BalanceQueries q = new BalanceQueries(ds);
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicLong checks = new AtomicLong();
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(12)) {
            for (int t = 0; t < 8; t++) { // 8 luong ghi
                int tt = t;
                futures.add(pool.submit(() -> {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int i = 0; i < 120; i++) {
                        long from = wallets[rnd.nextInt(4)], to = wallets[rnd.nextInt(4)];
                        if (from != to) {
                            try {
                                ledger.transfer("t:" + tt + ":" + i, from, to, BigDecimal.valueOf(rnd.nextInt(1, 200)), D);
                            } catch (InsufficientFunds ignored) {
                                // binh thuong
                            }
                        }
                    }
                    return null;
                }));
            }
            for (int t = 0; t < 2; t++) { // 2 luong refresh song song, khong khoa gi
                futures.add(pool.submit(() -> {
                    while (running.get()) {
                        snap.refresh(wallets[ThreadLocalRandom.current().nextInt(4)]);
                    }
                    return null;
                }));
            }
            for (int t = 0; t < 2; t++) { // 2 luong doc: (so du, seq) phai khop dung so du chay tai seq do
                futures.add(pool.submit(() -> {
                    while (running.get()) {
                        long w = wallets[ThreadLocalRandom.current().nextInt(4)];
                        SnapshotBalances.Read r = snap.read(w);
                        assertThat(r.balance()).isEqualByComparingTo(q.asOfSeq(w, r.asOfSeq()));
                        checks.incrementAndGet();
                    }
                    return null;
                }));
            }
            for (int i = 0; i < 8; i++) {
                futures.get(i).get();
            }
            running.set(false);
            for (Future<?> f : futures) {
                f.get();
            }
        }
        for (long w : wallets) {
            snap.refresh(w);
            assertThat(snap.read(w).balance()).isEqualByComparingTo(q.current(w).balance());
        }
        System.out.println("[LAB] snapshot+duoi khop so du chay o " + checks.get() + " lan doc giua luc dang ghi");
        assertThat(new LedgerReconciler(ds).run().clean()).isTrue();
    }

    @Test
    void hotAccountWatermarkByIdMissesALateCommitButXminWatermarkDoesNot() throws Exception {
        HotAccountBalances hot = new HotAccountBalances(ds);
        CountDownLatch inserted = new CountDownLatch(1), release = new CountDownLatch(1);
        TransactionTemplate outer = new TransactionTemplate(new DataSourceTransactionManager(ds));
        try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
            // T1: ghi posting len account nong (lay id NHO), roi chua commit
            Future<?> slow = pool.submit(() -> outer.executeWithoutResult(s -> {
                deposit("dep:slow", HOT_CASH, ALICE, "100");
                inserted.countDown();
                await(release);
            }));
            assertThat(inserted.await(10, TimeUnit.SECONDS)).isTrue();
            // T2: ghi sau (id LON hon) va commit truoc
            deposit("dep:fast", HOT_CASH, BOB, "30");
            hot.refreshByIdNaive(HOT_CASH); // moc = id cua T2, T1 chua thay
            hot.refresh(HOT_CASH);          // moc = xid cua T1 (con dang chay)
            release.countDown();
            slow.get();
        }
        hot.refreshByIdNaive(HOT_CASH);
        System.out.printf("[LAB] account nong: su that %s, chot theo id %s (sot vinh vien), chot theo xmin %s%n",
                plain(hot.truth(HOT_CASH)), plain(hot.readByIdNaive(HOT_CASH)), plain(hot.read(HOT_CASH)));
        assertThat(hot.truth(HOT_CASH)).isEqualByComparingTo("-130");
        assertThat(hot.readByIdNaive(HOT_CASH)).isEqualByComparingTo("-30");
        assertThat(hot.read(HOT_CASH)).isEqualByComparingTo("-130");
        hot.refresh(HOT_CASH);
        assertThat(hot.read(HOT_CASH)).isEqualByComparingTo("-130");
        assertThat(jdbc.queryForObject("SELECT balance FROM hot_balance WHERE account_id = ?", BigDecimal.class, HOT_CASH))
                .isEqualByComparingTo("-130");
    }

    @Test
    void aLongTransactionHoldsTheXminHorizonBackButReadsStayCorrect() throws Exception {
        HotAccountBalances hot = new HotAccountBalances(ds);
        deposit("dep:1", HOT_CASH, ALICE, "10");
        hot.refresh(HOT_CASH);
        try (Connection idle = ds.getConnection(); Statement s = idle.createStatement()) {
            idle.setAutoCommit(false);
            s.execute("SELECT pg_current_xact_id()"); // transaction mo, co xid, "quen" commit
            for (int i = 0; i < 20; i++) {
                deposit("dep:x" + i, HOT_CASH, BOB, "1");
            }
            hot.refresh(HOT_CASH);
            long tail = jdbc.queryForObject("""
                    SELECT count(*) FROM posting p JOIN hot_balance hb ON hb.account_id = p.account_id
                     WHERE p.account_id = ? AND p.tx_id >= hb.up_to_xid""", Long.class, HOT_CASH);
            System.out.println("[LAB] mot transaction mo giu moc xmin: phan duoi phai cong = " + tail + " dong");
            assertThat(tail).isEqualTo(20);                                  // moc dung yen
            assertThat(hot.read(HOT_CASH)).isEqualByComparingTo("-30");      // nhung doc van dung
            idle.rollback();
        }
        hot.refresh(HOT_CASH);
        assertThat(jdbc.queryForObject("SELECT balance FROM hot_balance WHERE account_id = ?", BigDecimal.class, HOT_CASH))
                .isEqualByComparingTo("-30");
    }

    @Test
    void readYourWritesFallsBackToPrimaryWhenTheReplicaLags() throws Exception {
        deposit("dep:1", CASH, ALICE, "100");
        // "Replica tre": mot transaction REPEATABLE READ mo truoc -> snapshot cua no dung yen o qua khu
        try (Connection lagging = ds.getConnection()) {
            lagging.setAutoCommit(false);
            lagging.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try (Statement s = lagging.createStatement()) {
                s.executeQuery("SELECT 1").close();
            }
            ReadYourWrites ryw = new ReadYourWrites(ds, new SingleConnectionDataSource(lagging, true));

            Receipt paid = ledger.transfer("tr:1", ALICE, BOB, new BigDecimal("30"), D); // khach vua tra 30
            long token = paid.on(ALICE).seq();

            ReadYourWrites.Read blind = ryw.balance(ALICE, 0);    // khong gui token
            ReadYourWrites.Read withToken = ryw.balance(ALICE, token);
            System.out.printf("[LAB] khong token: %s tu %s | token seq=%d: %s tu %s%n",
                    plain(blind.balance().balance()), blind.servedBy(), token,
                    plain(withToken.balance().balance()), withToken.servedBy());
            assertThat(blind.servedBy()).isEqualTo("replica");
            assertThat(blind.balance().balance()).isEqualByComparingTo("100"); // so CU: khach tuong chua tru tien
            assertThat(withToken.servedBy()).isEqualTo("primary");
            assertThat(withToken.balance().balance()).isEqualByComparingTo("70");
            lagging.rollback();
        }
    }

    private static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("het gio cho");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
