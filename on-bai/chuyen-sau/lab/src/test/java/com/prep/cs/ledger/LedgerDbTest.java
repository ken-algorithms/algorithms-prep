package com.prep.cs.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.prep.cs.Infra;
import com.prep.cs.ledger.db.LedgerDb;
import com.prep.cs.ledger.db.Ledgers;

/** Buoc 2-4 tren Postgres that: V1 sai the nao, V2 deadlock the nao, V3 dung va nhanh bao nhieu. */
class LedgerDbTest {

    LedgerDb db;

    @BeforeEach
    void setUp() throws Exception {
        DataSource ds = Infra.requirePostgres();
        db = new LedgerDb(ds);
        db.reset();
    }

    /** n luong, moi luong mot connection, cung bam "chay" mot luc. */
    interface Job {
        void run(Connection c, int thread) throws Exception;
    }

    void concurrently(int n, Job job) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        List<Thread> ts = new ArrayList<>();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < n; i++) {
            int t = i;
            Thread th = new Thread(() -> {
                try (Connection c = db.connect()) {
                    go.await();
                    job.run(c, t);
                } catch (Throwable e) {
                    unexpected.add(e);
                }
            });
            ts.add(th);
            th.start();
        }
        go.countDown();
        for (Thread t : ts) t.join();
        assertThat(unexpected).isEmpty();
    }

    @Test
    void v1ReadCheckWriteCreatesMoney() throws Exception {
        db.open(1, 100);
        db.open(2, 0);
        Ledgers.Ledger v1 = new Ledgers.ReadCheckWrite();
        AtomicInteger ok = new AtomicInteger();
        concurrently(50, (c, t) -> {
            if (!v1.transfer(c, "w-" + t, 1, 2, 10).rejected()) ok.incrementAndGet();
        });
        List<String> bad = db.reconcile();
        System.out.printf("[LAB] V1: 50 lenh chuyen 10 tu tai khoan co 100 -> thanh cong %d; so du A = %d, B = %d "
                        + "(A + B = %d, dung phai la 100)%n  doi soat: %s%n", ok.get(), db.balance(1), db.balance(2),
                db.balance(1) + db.balance(2), summary(bad));
        assertThat(db.balance(1) + db.balance(2)).isNotEqualTo(100);
        assertThat(bad).anyMatch(s -> s.startsWith("balance <> tong posting"));
    }

    @Test
    void v2DeadlocksWhenTwoTransfersGoOppositeWays() throws Exception {
        db.open(1, 1_000_000);
        db.open(2, 1_000_000);
        Ledgers.Ledger v2 = new Ledgers.AtomicUnordered();
        AtomicInteger ok = new AtomicInteger(), deadlocks = new AtomicInteger();
        ConcurrentLinkedQueue<String> sample = new ConcurrentLinkedQueue<>();
        concurrently(8, (c, t) -> {
            for (int i = 0; i < 25; i++) {
                long from = t % 2 == 0 ? 1 : 2, to = 3 - from;            // nua A->B, nua B->A
                try {
                    v2.transfer(c, "v2-" + t + "-" + i, from, to, 1);
                    ok.incrementAndGet();
                } catch (SQLException e) {
                    if (!"40P01".equals(e.getSQLState())) throw e;
                    deadlocks.incrementAndGet();
                    if (sample.isEmpty()) sample.add(e.getMessage());
                }
            }
        });
        System.out.printf("[LAB] V2: 200 lenh A<->B nguoc chieu -> %d thanh cong, %d bi Postgres giet vi deadlock (40P01)%n"
                + "  %s%n", ok.get(), deadlocks.get(), sample.peek() == null ? "" : sample.peek().replace("\n", "\n  "));
        assertThat(deadlocks.get()).isPositive();
        assertThat(db.reconcile()).isEmpty();   // transaction bi giet da rollback sach: so cai van dung
    }

    @Test
    void v3HasNoDeadlockNoOverdraftAndRunsEachKeyOnce() throws Exception {
        db.open(1, 1_000_000);
        db.open(2, 1_000_000);
        db.open(3, 100);
        db.open(4, 0);
        Ledgers.Ledger v3 = new Ledgers.Final();
        AtomicInteger ok = new AtomicInteger();
        long t0 = System.nanoTime();
        concurrently(8, (c, t) -> {
            for (int i = 0; i < 100; i++) {
                long from = t % 2 == 0 ? 1 : 2;
                v3.transfer(c, "v3-" + t + "-" + i, from, 3 - from, 1);
                ok.incrementAndGet();
            }
        });
        double sec = (System.nanoTime() - t0) / 1e9;
        AtomicInteger withdrawOk = new AtomicInteger();
        concurrently(50, (c, t) -> {
            if (!v3.transfer(c, "rut-" + t, 3, 4, 10).rejected()) withdrawOk.incrementAndGet();
        });
        AtomicInteger executed = new AtomicInteger(), replayed = new AtomicInteger();
        concurrently(20, (c, t) -> {
            Ledgers.Receipt r = v3.transfer(c, "bam-hai-lan", 1, 4, 7);
            (r.replayed() ? replayed : executed).incrementAndGet();
        });
        System.out.printf("[LAB] V3: 800 lenh A<->B nguoc chieu: %d thanh cong, 0 deadlock, %.0f lenh/s (8 luong)%n",
                ok.get(), ok.get() / sec);
        System.out.printf("[LAB] V3: 50 lenh rut 10 tu 100 cung luc -> %d thanh cong, so du %d%n",
                withdrawOk.get(), db.balance(3));
        System.out.printf("[LAB] V3: 20 request cung idempotency key -> chay that %d, phat lai %d; doi soat: %s%n",
                executed.get(), replayed.get(), db.reconcile());
        assertThat(withdrawOk.get()).isEqualTo(10);
        assertThat(db.balance(3)).isZero();
        assertThat(executed.get()).isEqualTo(1);
        assertThat(db.reconcile()).isEmpty();
    }

    /** Gom vi pham theo loai: {loai = so dong}. */
    static java.util.Map<String, Long> summary(List<String> bad) {
        return bad.stream().collect(java.util.stream.Collectors.groupingBy(
                s -> s.substring(0, s.indexOf(':')), java.util.TreeMap::new, java.util.stream.Collectors.counting()));
    }

    @Test
    void sameKeyDifferentContentIsRejected() throws Exception {
        db.open(1, 1_000);
        db.open(2, 0);
        Ledgers.Ledger v3 = new Ledgers.Final();
        try (Connection c = db.connect()) {
            v3.transfer(c, "k-1", 1, 2, 100);
            assertThatThrownBy(() -> v3.transfer(c, "k-1", 1, 2, 999))
                    .hasMessageContaining("lenh KHAC");
        }
    }
}
