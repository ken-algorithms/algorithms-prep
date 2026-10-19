package com.prep.cs.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.prep.cs.Infra;
import com.prep.cs.ledger.db.Holds;
import com.prep.cs.ledger.db.LedgerDb;
import com.prep.cs.ledger.db.Ledgers;

/** Buoc 6 (tam giu tien: so du kha dung) va buoc 7 (tai khoan nong: mot dong bi moi giao dich tranh nhau). */
class HoldsAndHotAccountTest {

    LedgerDb db;

    @BeforeEach
    void setUp() throws Exception {
        db = new LedgerDb(Infra.requirePostgres());
        db.reset();
    }

    @Test
    void authorizeCaptureReleaseKeepLedgerAndAvailableBalanceRight() throws Exception {
        db.open(1, 100);
        db.open(2, 0);
        Holds holds = new Holds();
        try (Connection c = db.connect()) {
            long h1 = holds.authorize(c, 1, 40, 600), h2 = holds.authorize(c, 1, 40, 600);
            long h3 = holds.authorize(c, 1, 40, 600);                 // chi con kha dung 20
            System.out.printf("[LAB] so du 100: giu 40 -> hold %d, giu 40 -> hold %d, giu 40 -> %s; so du %d, kha dung %d%n",
                    h1, h2, h3 < 0 ? "TU CHOI" : "hold " + h3, db.balance(1), Holds.available(c, 1));
            assertThat(h3).isEqualTo(-1);
            assertThat(holds.capture(c, h1, 2)).isTrue();              // chuyen that 40 cho merchant
            assertThat(holds.release(c, h2)).isTrue();                 // huy giao dich: nha 40
            assertThat(holds.capture(c, h2, 2)).isFalse();             // da nha thi khong capture duoc nua
            System.out.printf("[LAB] capture hold %d, release hold %d: so du %d, kha dung %d, merchant %d; doi soat %s%n",
                    h1, h2, db.balance(1), Holds.available(c, 1), db.balance(2), db.reconcile());
            assertThat(db.balance(1)).isEqualTo(60);
            assertThat(Holds.available(c, 1)).isEqualTo(60);
        }
        // 50 lenh giu 10 cung luc tren kha dung 60: dung 6 lenh duoc giu
        AtomicInteger ok = new AtomicInteger();
        List<Thread> ts = new ArrayList<>();
        CountDownLatch go = new CountDownLatch(1);
        for (int i = 0; i < 50; i++) {
            Thread t = new Thread(() -> {
                try (Connection c = db.connect()) {
                    go.await();
                    if (holds.authorize(c, 1, 10, 600) > 0) ok.incrementAndGet();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            ts.add(t);
            t.start();
        }
        go.countDown();
        for (Thread t : ts) t.join();
        try (Connection c = db.connect()) {
            System.out.printf("[LAB] 50 lenh giu 10 cung luc tren kha dung 60 -> %d duoc giu, kha dung con %d%n",
                    ok.get(), Holds.available(c, 1));
            assertThat(ok.get()).isEqualTo(6);
            assertThat(Holds.available(c, 1)).isZero();
        }
        assertThat(db.reconcile()).isEmpty();
    }

    /**
     * 16 luong, moi luong chuyen tu tai khoan khach CUA RIENG NO vao merchant trong 5 giay.
     * syncCommit=false: COMMIT khong cho WAL xuong dia — khoa dong duoc nha som hon (chi de DO, khong dung cho tien).
     */
    long transfersPerSecond(int shards, boolean syncCommit) throws Exception {
        return transfersPerSecond(shards, syncCommit, new Ledgers.Final());
    }

    long transfersPerSecond(int shards, boolean syncCommit, Ledgers.Ledger ledger) throws Exception {
        db.reset();
        int threads = 16;
        for (int t = 0; t < threads; t++) db.open(100 + t, 1_000_000_000L);
        for (int s = 0; s < shards; s++) db.open(1_000 + s, 0);
        AtomicLong done = new AtomicLong();
        long until = System.currentTimeMillis() + 5_000;
        List<Thread> ts = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            int me = t;
            Thread th = new Thread(() -> {
                SplittableRandom r = new SplittableRandom(me);
                try (Connection c = db.connect()) {
                    if (!syncCommit) {
                        try (var st = c.createStatement()) {
                            st.execute("SET synchronous_commit = off");
                        }
                    }
                    for (int i = 0; System.currentTimeMillis() < until; i++) {
                        long shard = 1_000 + r.nextInt(shards);       // cong vao mot o ngau nhien cua merchant
                        ledger.transfer(c, "pay-" + me + "-" + i, 100 + me, shard, 1);
                        done.incrementAndGet();
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            });
            ts.add(th);
            th.start();
        }
        for (Thread t : ts) t.join();
        assertThat(db.reconcile()).isEmpty();
        return done.get() / 5;
    }

    @Test
    void hotMerchantRowCapsThroughputAndShardingLiftsIt() throws Exception {
        long oneAsync = transfersPerSecond(1, false), sixteenAsync = transfersPerSecond(16, false);
        long one = transfersPerSecond(1, true), sixteen = transfersPerSecond(16, true);
        long merchantTotal;
        try (Connection c = db.connect();
             PreparedStatement ps = c.prepareStatement("SELECT sum(balance) FROM l_account WHERE id >= 1000");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            merchantTotal = rs.getLong(1);                            // so du merchant = tong cac o
        }
        System.out.printf("[LAB] 16 luong tra tien vao MOT dong merchant: %,d giao dich/s; vao 16 o merchant: %,d giao dich/s "
                + "(x%.1f); so du merchant = SUM 16 o = %,d%n", one, sixteen, (double) sixteen / one, merchantTotal);
        System.out.printf("[LAB] cung hai tai do, synchronous_commit = off (COMMIT khong cho fsync WAL): MOT dong %,d/s, "
                + "16 o %,d/s%n", oneAsync, sixteenAsync);
        long oneServer = transfersPerSecond(1, true, new Ledgers.ServerSide());
        long sixteenServer = transfersPerSecond(16, true, new Ledgers.ServerSide());
        System.out.printf("[LAB] ham l_transfer (1 round trip): MOT dong %,d/s, 16 o %,d/s%n", oneServer, sixteenServer);
        assertThat(sixteen).isGreaterThan(one);
    }
}
