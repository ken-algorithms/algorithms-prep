package com.prep.onbai.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import com.prep.onbai.ledger.Ledger.InsufficientFunds;
import com.prep.onbai.ledger.Ledger.Leg;
import com.prep.onbai.ledger.Ledger.Request;
import com.prep.onbai.testsupport.Pg;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

/** Bai 05 — tranh chap that tren Postgres that: khong am, khong mat tien, khong deadlock. */
class LedgerConcurrencyTest {

    static final LocalDate D = LocalDate.of(2026, 10, 18);
    static final long CASH = 1;

    DataSource ds;
    LedgerService ledger;
    JdbcTemplate jdbc;

    @BeforeEach
    void fresh() {
        ds = Pg.freshDatabase("b05_concurrency", 32, "ledger.sql");
        ledger = new LedgerService(ds);
        jdbc = new JdbcTemplate(ds);
        ledger.open(CASH, "cash", true, false);
    }

    void openWallet(long id, String amount) {
        ledger.open(id, "wallet:" + id, false, false);
        if (new BigDecimal(amount).signum() == 0) {
            return;
        }
        ledger.post(Request.of("seed:" + id, "DEPOSIT", D, new Leg(CASH, new BigDecimal(amount).negate()),
                new Leg(id, new BigDecimal(amount))));
    }

    @Test
    void fiftyConcurrentWithdrawalsNeverOverdraw() throws Exception {
        openWallet(10, "100");
        openWallet(11, "0");
        int n = 50;
        CyclicBarrier start = new CyclicBarrier(n);
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
        try (ExecutorService pool = Executors.newFixedThreadPool(n)) {
            for (int i = 0; i < n; i++) {
                int k = i;
                pool.submit(() -> {
                    start.await();
                    try {
                        ledger.transfer("w:" + k, 10, 11, new BigDecimal("10"), D);
                        ok.incrementAndGet();
                    } catch (InsufficientFunds e) {
                        rejected.incrementAndGet();
                    }
                    return null;
                });
            }
        }
        System.out.printf("[LAB] 50 lenh rut 10 cung luc tu vi 100: %d thanh cong, %d bi tu choi%n", ok.get(), rejected.get());
        assertThat(ok.get()).isEqualTo(10);
        assertThat(rejected.get()).isEqualTo(40);
        assertThat(jdbc.queryForObject("SELECT balance FROM account WHERE id = 10", BigDecimal.class)).isEqualByComparingTo("0");
        assertThat(jdbc.queryForObject("SELECT min(balance_after) FROM posting WHERE account_id = 10", BigDecimal.class))
                .isGreaterThanOrEqualTo(BigDecimal.ZERO); // khong luc nao am, ke ca giua chung
        assertThat(new LedgerReconciler(ds).run().clean()).isTrue();
    }

    @Test
    void randomTransfersInBothDirectionsNeverDeadlockAndConserveMoney() throws Exception {
        long[] wallets = {10, 11, 12, 13, 14};
        for (long w : wallets) {
            openWallet(w, "1000");
        }
        int threads = 16, perThread = 150;
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (int t = 0; t < threads; t++) {
                int tt = t;
                futures.add(pool.submit(() -> {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int i = 0; i < perThread; i++) {
                        long from = wallets[rnd.nextInt(wallets.length)];
                        long to = wallets[rnd.nextInt(wallets.length)];
                        if (from == to) {
                            continue;
                        }
                        try {
                            ledger.transfer("r:" + tt + ":" + i, from, to, BigDecimal.valueOf(rnd.nextInt(1, 300)), D);
                            ok.incrementAndGet();
                        } catch (InsufficientFunds e) {
                            rejected.incrementAndGet();
                        }
                    }
                    return null;
                }));
            }
        }
        for (Future<?> f : futures) {
            f.get(); // deadlock hay loi khac se no o day
        }
        BigDecimal total = jdbc.queryForObject("SELECT sum(balance) FROM account WHERE id BETWEEN 10 AND 14", BigDecimal.class);
        LedgerReconciler.Report report = new LedgerReconciler(ds).run();
        System.out.printf("[LAB] %d chuyen khoan ngau nhien hai chieu, 16 luong: %d ok, %d thieu tien, tong 5 vi = %s, doi soat sach = %s%n",
                ok.get() + rejected.get(), ok.get(), rejected.get(), total.stripTrailingZeros().toPlainString(), report.clean());
        assertThat(total).isEqualByComparingTo("5000");
        assertThat(report.clean()).isTrue();
    }

    @Test
    void lockingInRequestOrderDeadlocks() throws Exception {
        openWallet(10, "100");
        openWallet(11, "100");
        // Anti-pattern: khoa theo thu tu (from, to) cua request. A->B va B->A cung luc = vong cho.
        CyclicBarrier bothHoldFirstLock = new CyclicBarrier(2);
        List<String> outcomes = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            Future<String> ab = pool.submit(() -> naiveTransfer(10, 11, bothHoldFirstLock));
            Future<String> ba = pool.submit(() -> naiveTransfer(11, 10, bothHoldFirstLock));
            outcomes.add(ab.get());
            outcomes.add(ba.get());
        }
        System.out.println("[LAB] khoa theo thu tu request: " + outcomes);
        assertThat(outcomes).containsExactlyInAnyOrder("commit", "40P01"); // Postgres giet mot ben sau deadlock_timeout
    }

    private String naiveTransfer(long from, long to, CyclicBarrier barrier) throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                s.executeUpdate("UPDATE account SET balance = balance - 10 WHERE id = " + from);
                barrier.await(); // ca hai da giu khoa dau tien
                s.executeUpdate("UPDATE account SET balance = balance + 10 WHERE id = " + to);
                c.rollback(); // chi minh hoa khoa, khong ghi that
                return "commit";
            } catch (SQLException e) {
                c.rollback();
                return e.getSQLState();
            }
        }
    }
}
