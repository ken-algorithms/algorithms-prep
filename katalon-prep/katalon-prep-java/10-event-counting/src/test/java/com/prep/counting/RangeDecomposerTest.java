package com.prep.counting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.prep.counting.domain.Times;
import com.prep.counting.query.RangeDecomposer;
import com.prep.counting.query.RangeDecomposer.Part;
import com.prep.counting.query.Tier;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class RangeDecomposerTest {

    private static final Instant FAR_FUTURE = Instant.parse("2100-01-01T00:00:00Z");

    private static long buckets(List<Part> parts, Tier tier) {
        return parts.stream().filter(p -> p.tier() == tier)
                .mapToLong(p -> Duration.between(p.from(), p.to()).dividedBy(tier.step)).sum();
    }

    @Test
    void theWorkedExampleFromDoc04() {
        List<Part> parts = RangeDecomposer.decompose(Instant.parse("2026-08-26T03:17:30Z"),
                Instant.parse("2026-08-29T11:42:10Z"), FAR_FUTURE);
        System.out.println("=== 03:17:30 → three days later 11:42:10");
        parts.forEach(p -> System.out.println("    " + p.tier() + " " + p.from() + " → " + p.to()));
        assertEquals(List.of(
                new Part(Tier.RAW, Instant.parse("2026-08-26T03:17:30Z"), Instant.parse("2026-08-26T03:18:00Z")),
                new Part(Tier.MINUTE, Instant.parse("2026-08-26T03:18:00Z"), Instant.parse("2026-08-26T04:00:00Z")),
                new Part(Tier.HOUR, Instant.parse("2026-08-26T04:00:00Z"), Instant.parse("2026-08-27T00:00:00Z")),
                new Part(Tier.DAY, Instant.parse("2026-08-27T00:00:00Z"), Instant.parse("2026-08-29T00:00:00Z")),
                new Part(Tier.HOUR, Instant.parse("2026-08-29T00:00:00Z"), Instant.parse("2026-08-29T11:00:00Z")),
                new Part(Tier.MINUTE, Instant.parse("2026-08-29T11:00:00Z"), Instant.parse("2026-08-29T11:42:00Z")),
                new Part(Tier.RAW, Instant.parse("2026-08-29T11:42:00Z"), Instant.parse("2026-08-29T11:42:10Z"))),
                parts);
        assertEquals(84, buckets(parts, Tier.MINUTE));
        assertEquals(31, buckets(parts, Tier.HOUR));
        assertEquals(2, buckets(parts, Tier.DAY));
    }

    @Test
    void everythingAfterTheSealIsReadFromRaw() {
        List<Part> parts = RangeDecomposer.decompose(Instant.parse("2026-08-29T08:00:00Z"),
                Instant.parse("2026-08-29T10:30:00Z"), Instant.parse("2026-08-29T10:28:00Z"));
        assertEquals(new Part(Tier.HOUR, Instant.parse("2026-08-29T08:00:00Z"), Instant.parse("2026-08-29T10:00:00Z")),
                parts.get(0));
        assertEquals(new Part(Tier.RAW, Instant.parse("2026-08-29T10:28:00Z"), Instant.parse("2026-08-29T10:30:00Z")),
                parts.get(parts.size() - 1));
    }

    /** 20,000 random ranges up to a year long: no gap, no overlap, aligned buckets, and the stated bound. */
    @Test
    void randomRangesAreCoveredExactlyAndBounded() {
        Random rnd = new Random(42);
        Instant base = Instant.parse("2026-01-01T00:00:00Z");
        long maxBuckets = 0;
        for (int i = 0; i < 20_000; i++) {
            Instant from = base.plusSeconds(rnd.nextInt(200 * 86_400));
            Instant to = from.plusSeconds(1 + rnd.nextInt(365 * 86_400));
            Instant sealed = rnd.nextBoolean() ? FAR_FUTURE : to.minusSeconds(rnd.nextInt(180)).truncatedTo(
                    java.time.temporal.ChronoUnit.MINUTES);
            List<Part> parts = RangeDecomposer.decompose(from, to, sealed);
            Instant cur = from;
            for (Part p : parts) {
                assertEquals(cur, p.from(), "gap or overlap");
                assertTrue(p.from().isBefore(p.to()));
                if (p.tier() != Tier.RAW) {
                    assertTrue(Times.aligned(p.from(), p.tier().step) && Times.aligned(p.to(), p.tier().step));
                }
                cur = p.to();
            }
            assertEquals(to, cur);
            assertTrue(parts.stream().filter(p -> p.tier() == Tier.RAW).count() <= 2);
            assertTrue(buckets(parts, Tier.MINUTE) <= 118);
            assertTrue(buckets(parts, Tier.HOUR) <= 46);
            maxBuckets = Math.max(maxBuckets, buckets(parts, Tier.MINUTE) + buckets(parts, Tier.HOUR)
                    + buckets(parts, Tier.DAY));
        }
        System.out.println("=== 20,000 random ranges up to one year: at most " + maxBuckets + " buckets per query");
        assertTrue(maxBuckets <= 118 + 46 + 366);
    }
}
