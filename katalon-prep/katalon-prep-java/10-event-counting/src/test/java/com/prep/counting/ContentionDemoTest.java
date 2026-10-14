package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.domain.Outcome;
import com.prep.counting.rejected.LockedTotalCounter;
import com.prep.counting.rejected.SyncMinuteCounter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Same 4,000 batches (80,000 events), 16 writer threads, three write paths. Prints batches per second. It asserts
 * only correctness: speed depends on the machine, so read the printed numbers as one run on one machine.
 */
class ContentionDemoTest {

    private static final int THREADS = 16;
    private static final int BATCHES = 4000;

    @Test
    void hotRowVersusAppend() throws Exception {
        Instant now = Instant.parse("2026-08-29T10:00:00Z");
        Fixture f = new Fixture("contention", now);
        Random rnd = new Random(8);
        List<EventBatch> batches = new ArrayList<>();
        for (int i = 0; i < BATCHES; i++) {
            batches.add(Fixture.batch("t1", now.minusSeconds(rnd.nextInt(60)), rnd)); // one tenant, one minute
        }
        LockedTotalCounter locked = new LockedTotalCounter(f.jdbc, f.tx, f.classifier);
        SyncMinuteCounter sync = new SyncMinuteCounter(f.jdbc, f.classifier);
        f.partitions.ensureFor(now);

        run("warm-up", batches.subList(0, 200), f.ingest::ingest);
        f.jdbc.execute("TRUNCATE raw_events");
        double a1 = run("approach 1: lock + running total", batches, locked::ingest);
        double a3 = run("approach 3: sync minute counter", batches, sync::ingest);
        double best = run("accepted: append raw (rollup later)", batches, f.ingest::ingest);

        Map<Outcome, Long> totals = locked.lastRecord("t1");
        assertEquals(BATCHES * 20L, totals.values().stream().mapToLong(Long::longValue).sum());
        assertEquals(BATCHES * 20L, f.rawCount());
        System.out.printf("=== batches/s: approach 1 %.0f, approach 3 %.0f, accepted %.0f%n", a1, a3, best);
    }

    private static double run(String name, List<EventBatch> batches, Consumer<EventBatch> write) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        long t0 = System.nanoTime();
        List<Future<?>> futures = new ArrayList<>();
        for (EventBatch b : batches) {
            futures.add(pool.submit(() -> write.accept(b)));
        }
        for (Future<?> fu : futures) {
            fu.get();
        }
        double secs = (System.nanoTime() - t0) / 1e9;
        pool.shutdown();
        double rate = batches.size() / secs;
        System.out.printf("=== %-40s %5d batches in %.2f s = %6.0f batches/s%n", name, batches.size(), secs, rate);
        return rate;
    }
}
