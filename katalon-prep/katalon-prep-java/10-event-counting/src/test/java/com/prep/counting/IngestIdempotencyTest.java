package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.prep.counting.domain.EventBatch;
import com.prep.counting.ingest.IngestResult;
import com.prep.counting.ingest.TooLateException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Test;

class IngestIdempotencyTest {

    private static final Instant NOW = Instant.parse("2026-08-29T10:00:00Z");
    private final Fixture f = new Fixture("ingest_idem", NOW);

    @Test
    void aRetriedBatchIsStoredOnce() {
        EventBatch b = Fixture.batch("t1", NOW.minusSeconds(30), new Random(1));
        IngestResult first = f.ingest.ingest(b);
        IngestResult retry = f.ingest.ingest(b); // client timed out and sent the same batch again
        System.out.println("=== first " + first + ", retry " + retry);
        assertEquals(new IngestResult(20, 20, 0), first);
        assertEquals(new IngestResult(20, 0, 20), retry);
        assertEquals(20, f.rawCount());
    }

    @Test
    void twoDifferentRequestsWithTheSameContentAreBothCounted() {
        EventBatch b = Fixture.batch("t1", NOW.minusSeconds(30), new Random(2));
        f.ingest.ingest(b);
        f.ingest.ingest(new EventBatch("t1", b.batchId() + "-other", b.ts(), b.items()));
        assertEquals(40, f.rawCount());
    }

    @Test
    void tooOldIsRefusedAndCountedNeverDroppedSilently() {
        EventBatch b = Fixture.batch("t1", NOW.minus(Duration.ofDays(8)), new Random(3));
        assertThrows(TooLateException.class, () -> f.ingest.ingest(b));
        assertEquals(20, f.ingest.rejectedTooLate());
    }

    @Test
    void futureTimestampsAndHugeBatchesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> f.ingest.ingest(Fixture.batch("t1", NOW.plus(Duration.ofHours(1)), new Random(4))));
        EventBatch b = Fixture.batch("t1", NOW, new Random(5));
        java.util.List<EventBatch.Item> many = new java.util.ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            many.add(b.items().get(0));
        }
        assertThrows(IllegalArgumentException.class, () -> f.ingest.ingest(new EventBatch("t1", "big", NOW, many)));
    }
}
