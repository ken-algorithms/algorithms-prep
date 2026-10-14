package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.counting.rollup.Reconciler;
import java.time.Instant;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ReconcilerTest {

    @Test
    void driftIsFoundAndRepaired() {
        Fixture f = new Fixture("reconcile", Instant.parse("2026-08-29T11:00:30Z"));
        Random rnd = new Random(5);
        for (int i = 0; i < 60; i++) {
            f.ingest.ingest(Fixture.batch("t1", Instant.parse("2026-08-29T10:00:00Z").plusSeconds(i * 60L), rnd));
        }
        f.rollup.refreshRecent();
        f.rollup.refresh(Instant.parse("2026-08-29T10:00:00Z"), Instant.parse("2026-08-29T11:00:00Z"));
        assertTrue(f.reconciler.reconcileLastHours(2).found().isEmpty(), "clean before the bug");

        // simulate a bug that double-counted 5 events in one minute bucket
        f.jdbc.update("UPDATE agg_1m SET cnt = cnt + 5 WHERE bucket = '2026-08-29 10:17:00+00' AND outcome = 1");

        Reconciler.Report r = f.reconciler.reconcileLastHours(2);
        System.out.println("=== found " + r.found() + " → after repair " + r.afterRepair());
        assertEquals(1, r.found().size());
        assertEquals(-5, r.found().get(0).diff());
        assertTrue(r.afterRepair().isEmpty());
    }
}
