package com.prep.cs.counting;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.time.Instant;
import java.util.SplittableRandom;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.prep.cs.Infra;

/** Postgres that: commit co fencing offset, va truy van khoang bat ky khop voi dem thu cong. */
class CountStoreTest {

    CountStore store;

    @BeforeEach
    void setUp() throws Exception {
        DataSource ds = Infra.requirePostgres();
        store = new CountStore(ds);
        store.createSchema();
        store.truncate();
    }

    static ByteBuffer block(long clientId, int epoch, long fromSeq, int n, long tsSec, int[] counts) {
        ByteBuffer b = ByteBuffer.allocate(4 + n * RequestRecord.SIZE);
        b.putInt(n);
        Decoded d = new Decoded();
        for (int i = 0; i < n; i++) {
            d.clientId = clientId;
            d.epoch = epoch;
            d.seq = fromSeq + i;
            d.tsMillis = (tsSec + i) * 1000;
            System.arraycopy(counts, 0, d.counts, 0, 4);
            RequestRecord.write(b, d);
        }
        return b.flip();
    }

    @Test
    void zombieAggregatorCannotCommitAfterTheNewOwnerDid() throws Exception {
        try (Connection c = store.connect()) {
            long start = store.loadOffset(c, "t", 0);
            PartitionAggregator zombie = new PartitionAggregator(0, start);
            PartitionAggregator owner = new PartitionAggregator(0, start);
            int[] one = {1, 0, 0, 0};
            // ca hai cung doc offset 0..1 (zombie bi GC pause, group da giao partition cho owner)
            zombie.onBlock(0, 0, block(7, 1, 0, 10, 1_792_390_000L, one));
            owner.onBlock(0, 0, block(7, 1, 0, 10, 1_792_390_000L, one));
            owner.onBlock(1, 0, block(7, 1, 10, 10, 1_792_390_010L, one));
            assertThat(owner.commit(store, c, "t")).isTrue();            // 0 -> 2
            boolean zombieOk = zombie.commit(store, c, "t");             // muon doi 0 -> 1: offset da la 2
            Stats s = store.query(1_792_390_000L, 1_792_390_020L);
            System.out.printf("[LAB] owner commit ok, zombie commit %s; tong TRUE = %d (dung: 20)%n",
                    zombieOk ? "OK (SAI!)" : "bi tu choi", s.trueCount());
            assertThat(zombieOk).isFalse();
            assertThat(s.trueCount()).isEqualTo(20);
        }
    }

    @Test
    void ownerResyncsWhenZombieCommittedFirst() throws Exception {
        try (Connection c = store.connect()) {
            long start = store.loadOffset(c, "t", 0);
            PartitionAggregator zombie = new PartitionAggregator(0, start);
            PartitionAggregator owner = new PartitionAggregator(0, start);
            int[] one = {1, 0, 0, 0};
            // zombie da doc offset 0 truoc khi bi GC dung; owner moi nhan partition, doc offset 0 va 1
            zombie.onBlock(0, 0, block(7, 1, 0, 10, 1_792_390_000L, one));
            owner.onBlock(0, 0, block(7, 1, 0, 10, 1_792_390_000L, one));
            owner.onBlock(1, 0, block(7, 1, 10, 10, 1_792_390_010L, one));
            assertThat(zombie.commit(store, c, "t")).isTrue();            // xac song tinh day, commit TRUOC: 0 -> 1
            assertThat(owner.commit(store, c, "t")).isFalse();            // chu that muon 0 -> 2: bi tu choi
            // lam dung nhu Aggregator.commitAll: nap lai offset tu Postgres, doc lai de nap state, roi dem tiep
            long reloaded = store.loadOffset(c, "t", 0);
            PartitionAggregator resynced = new PartitionAggregator(0, reloaded);
            resynced.onBlock(0, 0, block(7, 1, 0, 10, 1_792_390_000L, one));     // < 1: chi nap state
            resynced.onBlock(1, 0, block(7, 1, 10, 10, 1_792_390_010L, one));    // >= 1: dem
            assertThat(resynced.commit(store, c, "t")).isTrue();
            Stats s = store.query(1_792_390_000L, 1_792_390_020L);
            System.out.printf("[LAB] xac song commit truoc (0 -> 1), chu that bi tu choi, nap lai tu offset %d: "
                    + "replay %d, dem %d; tong TRUE = %d (dung: 20)%n", reloaded, resynced.replayed, resynced.counted,
                    s.trueCount());
            assertThat(s.trueCount()).isEqualTo(20);
        }
    }

    @Test
    void replayRebuildsDedupStateWithoutCountingTwice() throws Exception {
        try (Connection c = store.connect()) {
            int[] one = {0, 1, 0, 0};
            PartitionAggregator first = new PartitionAggregator(0, store.loadOffset(c, "t", 0));
            first.onBlock(0, 0, block(9, 1, 0, 50, 1_792_390_000L, one));
            assertThat(first.commit(store, c, "t")).isTrue();
            // khoi dong lai: offset trong DB = 1. Doc lai offset 0 chi de nap state, roi offset 1 la retry cua seq 40..49
            PartitionAggregator restarted = new PartitionAggregator(0, store.loadOffset(c, "t", 0));
            restarted.onBlock(0, 0, block(9, 1, 0, 50, 1_792_390_000L, one));      // replay
            restarted.onBlock(1, 0, block(9, 1, 40, 20, 1_792_390_040L, one));     // 40..49 trung, 50..59 moi
            assertThat(restarted.commit(store, c, "t")).isTrue();
            System.out.printf("[LAB] sau restart: replay %d, dem moi %d, bo trung %d%n",
                    restarted.replayed, restarted.counted, restarted.duplicates);
            assertThat(restarted.replayed).isEqualTo(50);
            assertThat(restarted.counted).isEqualTo(10);
            assertThat(restarted.duplicates).isEqualTo(10);
            assertThat(store.query(1_792_390_000L, 1_792_390_100L).falseCount()).isEqualTo(60);
        }
    }

    @Test
    void anyRangeMatchesBruteForceOverThreeDaysOfData() throws Exception {
        long base = Instant.parse("2026-10-16T21:37:00Z").getEpochSecond();
        int seconds = 3 * 86_400;
        long[][] truth = new long[seconds][4];
        SplittableRandom r = new SplittableRandom(11);
        try (Connection c = store.connect()) {
            PartitionAggregator pa = new PartitionAggregator(0, store.loadOffset(c, "t", 0));
            long seq = 0, offset = 0;
            for (int s = 0; s < seconds; s += 1 + r.nextInt(40)) {      // mot request moi 1..40 giay
                int[] cnt = {r.nextInt(15), r.nextInt(8), r.nextInt(4), r.nextInt(2)};
                pa.onBlock(offset++, 0, block(1, 1, seq++, 1, base + s, cnt));
                for (int k = 0; k < 4; k++) truth[s][k] += cnt[k];
            }
            assertThat(pa.commit(store, c, "t")).isTrue();
        }
        long maxBuckets = 0;
        for (int i = 0; i < 300; i++) {
            int a = r.nextInt(seconds), b = a + r.nextInt(seconds - a + 1);
            long[] want = new long[4];
            for (int s = a; s < b; s++) for (int k = 0; k < 4; k++) want[k] += truth[s][k];
            Stats got = store.query(base + a, base + b);
            assertThat(got.sameCounts(want)).as("[%d, %d)", a, b).isTrue();
            maxBuckets = Math.max(maxBuckets, got.bucketsRead());
        }
        Stats all = store.query(base, base + seconds);
        System.out.printf("[LAB] 300 khoang ngau nhien trong 3 ngay: deu khop dem thu cong; doc toi da %d dong; "
                + "ca 3 ngay: %s%n", maxBuckets, all);
    }
}
