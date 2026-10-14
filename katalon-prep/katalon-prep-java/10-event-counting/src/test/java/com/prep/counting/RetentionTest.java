package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.prep.counting.query.Stats;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RetentionTest {

    @Test
    void droppingOldRawPartitionsKeepsTheRollups() {
        Fixture f = new Fixture("retention", Instant.parse("2026-08-29T00:00:30Z"));
        Random rnd = new Random(3);
        for (int day = 20; day <= 28; day++) {
            Instant ts = Instant.parse("2026-08-" + day + "T12:00:00Z");
            f.clock.set(ts.plusSeconds(60)); // each day's batch arrives on its day
            f.ingest.ingest(Fixture.batch("t1", ts, rnd));
        }
        f.clock.set(Instant.parse("2026-08-29T00:00:30Z"));
        f.rollup.refresh(Instant.parse("2026-08-20T00:00:00Z"), Instant.parse("2026-08-29T00:00:00Z"));
        f.rollup.refreshRecent();

        List<String> dropped = f.partitions.dropBefore(LocalDate.parse("2026-08-22"));
        System.out.println("=== dropped " + dropped + "; left " + f.partitions.partitions());
        assertEquals(List.of("raw_events_20260820", "raw_events_20260821"), dropped);
        assertEquals(7 * 20, f.rawCount());

        Stats oldDay = f.stats.query("t1", Instant.parse("2026-08-20T00:00:00Z"), Instant.parse("2026-08-21T00:00:00Z"));
        assertEquals(20, oldDay.trueCount() + oldDay.falseCount() + oldDay.fakeKey() + oldDay.malformed());
    }
}
