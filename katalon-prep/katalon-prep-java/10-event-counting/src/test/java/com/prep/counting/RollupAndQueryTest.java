package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.query.Stats;
import com.prep.counting.rollup.Reconciler;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** The accepted design, end to end, against COUNT(*) on raw as the oracle. */
class RollupAndQueryTest {

    private static final Instant START = Instant.parse("2026-08-29T06:00:00Z");

    private static long[] arr(Stats s) {
        return new long[] {s.falseCount(), s.trueCount(), s.fakeKey(), s.malformed()};
    }

    /**
     * Three hours of traffic arriving in event-time order, with the rollup running every minute as in production
     * and 10% of batches arriving 0–5 minutes late. Then 500 random ranges must equal COUNT(*) on raw.
     */
    @Test
    void anyRangeEqualsCountOnRaw() {
        Fixture f = new Fixture("rollup_query", START);
        Random rnd = new Random(7);
        int batches = 0;
        for (int minute = 0; minute < 180; minute++) {
            Instant minuteStart = START.plus(Duration.ofMinutes(minute));
            for (int i = 0; i < 20; i++) {
                f.clock.set(minuteStart.plusSeconds(i * 3L));
                Instant ts = f.clock.instant();
                if (rnd.nextInt(10) == 0) {
                    ts = ts.minusSeconds(rnd.nextInt(300)); // late, but inside the 30-minute window
                }
                f.ingest.ingest(Fixture.batch("t1", ts, rnd));
                batches++;
            }
            f.clock.set(minuteStart.plus(Duration.ofMinutes(1)).plusSeconds(1));
            f.rollup.refreshRecent();
        }
        System.out.println("=== " + batches + " batches, " + f.rawCount() + " raw events; sealed until "
                + f.rollup.sealedUntil());

        long maxBucketRows = 0;
        for (int q = 0; q < 500; q++) {
            Instant from = START.minusSeconds(600).plusSeconds(rnd.nextInt(3 * 3600 + 600));
            Instant to = from.plusSeconds(1 + rnd.nextInt(4 * 3600));
            Stats s = f.stats.query("t1", from, to);
            assertArrayEquals(f.oracle.count("t1", from, to), arr(s), from + " → " + to);
            maxBucketRows = Math.max(maxBucketRows, s.bucketRowsRead());
        }
        Stats all = f.stats.query("t1", START, START.plus(Duration.ofHours(3)));
        System.out.println("=== whole 3 hours: " + all);
        System.out.println("=== 500 random ranges equal COUNT(*) on raw; most bucket rows read by one query: "
                + maxBucketRows);
        assertArrayEquals(f.oracle.count("t1", START, START.plus(Duration.ofHours(3))), arr(all));
        assertTrue(all.rawRowsRead() == 0, "a sealed range never touches raw");
    }

    @Test
    void aLateEventInsideTheWindowMovesToItsBucketOnTheNextRun() {
        Fixture f = new Fixture("late_inside", Instant.parse("2026-08-29T10:05:30Z"));
        f.rollup.refreshRecent(); // sealed until 10:05
        Instant ts = Instant.parse("2026-08-29T09:58:10Z");
        EventBatch late = Fixture.batch("t1", ts, new Random(9)); // happened 09:58, arrives 10:05:30
        f.ingest.ingest(late);
        Instant from = Instant.parse("2026-08-29T09:58:00Z");
        Instant to = Instant.parse("2026-08-29T09:59:00Z");

        long before = total(f.stats.query("t1", from, to));
        f.clock.set(Instant.parse("2026-08-29T10:06:30Z"));
        f.rollup.refreshRecent();
        long after = total(f.stats.query("t1", from, to));
        System.out.println("=== bucket 09:58 before the next run: " + before + ", after: " + after);
        assertEquals(0, before); // stale for at most one rollup period: the 202 promised exactly this
        assertEquals(20, after); // counted by EVENT time, in the 09:58 bucket, not 10:05
    }

    @Test
    void aBatchLaterThanTheWindowIsRepairedByTheHourlyReconcile() {
        Fixture f = new Fixture("late_beyond", Instant.parse("2026-08-29T12:00:30Z"));
        Random rnd = new Random(11);
        for (int i = 0; i < 50; i++) {
            Instant ts = Instant.parse("2026-08-29T09:00:00Z").plusSeconds(i * 60L);
            f.clock.set(ts.plusSeconds(5)); // on time
            f.ingest.ingest(Fixture.batch("t1", ts, rnd));
        }
        f.clock.set(Instant.parse("2026-08-29T12:00:30Z"));
        f.rollup.refresh(Instant.parse("2026-08-29T09:00:00Z"), Instant.parse("2026-08-29T12:00:00Z"));
        f.rollup.refreshRecent();

        // a mobile client was offline for three hours: event time 09:10, arrives at 12:00:30
        f.ingest.ingest(Fixture.batch("t1", Instant.parse("2026-08-29T09:10:00Z"), rnd));
        assertEquals(20, f.ingest.lateBeyondWindow());
        Instant from = Instant.parse("2026-08-29T09:00:00Z");
        Instant to = Instant.parse("2026-08-29T10:00:00Z");
        long wrong = total(f.stats.query("t1", from, to));

        Reconciler.Report r = f.reconciler.reconcileLastHours(24);
        long right = total(f.stats.query("t1", from, to));
        System.out.println("=== 09:00–10:00 before reconcile " + wrong + ", drift rows found " + r.found()
                + ", after repair " + right);
        assertEquals(1000, wrong);
        assertEquals(1020, right);
        assertTrue(r.afterRepair().isEmpty());
    }

    private static long total(Stats s) {
        return s.trueCount() + s.falseCount() + s.fakeKey() + s.malformed();
    }
}
