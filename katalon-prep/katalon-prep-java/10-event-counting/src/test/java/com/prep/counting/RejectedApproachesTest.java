package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.domain.Outcome;
import com.prep.counting.query.Stats;
import com.prep.counting.rejected.LockedTotalCounter;
import com.prep.counting.rejected.SyncMinuteCounter;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Each rejected approach, run against the same batches as the accepted one, failing in the way the video says. */
class RejectedApproachesTest {

    private static final Instant NINE = Instant.parse("2026-08-29T09:00:00Z");

    private static long sum(Map<Outcome, Long> m) {
        return m.values().stream().mapToLong(Long::longValue).sum();
    }

    @Test
    void approach1LockedTotalLosesTimeAndCountsRetriesTwice() {
        Fixture f = new Fixture("rejected_1", NINE.plusSeconds(7200));
        LockedTotalCounter naive = new LockedTotalCounter(f.jdbc, f.tx, f.classifier);
        EventBatch at9 = Fixture.batch("t1", NINE, new Random(1));
        EventBatch at10 = Fixture.batch("t1", NINE.plusSeconds(3600), new Random(2));
        naive.ingest(at9);
        naive.ingest(at10);
        // "between 09:00 and 10:00?" — the last record only knows 40 events since the beginning
        assertEquals(40, sum(naive.lastRecord("t1")));

        naive.ingest(at10); // the client retries after a timeout
        Map<Outcome, Long> afterRetry = naive.lastRecord("t1");
        System.out.println("=== approach 1 after one retry: " + afterRetry + " (true answer: 40 events)");
        assertEquals(60, sum(afterRetry)); // counted twice, and no raw row exists to prove it
    }

    @Test
    void approach3SyncMinuteCountersAnswerRangesButNotRetries() {
        Fixture f = new Fixture("rejected_3", NINE.plusSeconds(7200));
        SyncMinuteCounter sync = new SyncMinuteCounter(f.jdbc, f.classifier);
        Random rnd = new Random(3);
        EventBatch last = null;
        for (int i = 0; i < 120; i++) {
            last = Fixture.batch("t1", NINE.plusSeconds(i * 60L), rnd);
            sync.ingest(last);
            f.ingest.ingest(last);
        }
        Instant from = NINE.plusSeconds(1800);
        Instant to = NINE.plusSeconds(5400);
        // fixes approach 1: a minute-aligned range is answered correctly
        assertArrayEquals(f.oracle.count("t1", from, to), sync.count("t1", from, to));

        sync.ingest(last); // one retry
        f.ingest.ingest(last);
        long[] syncAll = sync.count("t1", NINE, NINE.plusSeconds(7200));
        long[] truth = f.oracle.count("t1", NINE, NINE.plusSeconds(7200));
        long syncTotal = syncAll[0] + syncAll[1] + syncAll[2] + syncAll[3];
        long trueTotal = truth[0] + truth[1] + truth[2] + truth[3];
        System.out.println("=== approach 3 after one retry: " + syncTotal + " events, truth " + trueTotal);
        assertEquals(trueTotal + 20, syncTotal);
    }

    /** Approach 2 is always right, so the tests use it as the oracle; the cost is how many rows it reads. */
    @Test
    void approach2CountsEveryRowTheBestReadsBuckets() {
        Fixture f = new Fixture("rejected_2", NINE);
        Random rnd = new Random(4);
        for (int i = 0; i < 6 * 60; i++) {
            Instant ts = NINE.plusSeconds(i * 60L);
            f.clock.set(ts.plusSeconds(1));
            for (int j = 0; j < 5; j++) {
                f.ingest.ingest(Fixture.batch("t1", ts.plusSeconds(j * 10L), rnd));
            }
        }
        f.clock.set(NINE.plus(Duration.ofHours(6)).plusSeconds(30));
        f.rollup.refresh(NINE, NINE.plus(Duration.ofHours(6)));
        f.rollup.refreshRecent();
        Instant from = NINE.plusSeconds(17 * 60 + 30);
        Instant to = NINE.plus(Duration.ofHours(5)).plusSeconds(42 * 60 + 10);
        long[] raw = f.oracle.count("t1", from, to);
        Stats best = f.stats.query("t1", from, to);
        long rawRows = raw[0] + raw[1] + raw[2] + raw[3];
        System.out.println("=== approach 2 counted " + rawRows + " raw rows; the accepted design read "
                + best.bucketRowsRead() + " bucket rows + " + best.rawRowsRead() + " raw rows in " + best.parts()
                + " pieces");
        assertEquals(rawRows, best.trueCount() + best.falseCount() + best.fakeKey() + best.malformed());
        assertTrue(best.bucketRowsRead() + best.rawRowsRead() < rawRows / 20);
    }
}
