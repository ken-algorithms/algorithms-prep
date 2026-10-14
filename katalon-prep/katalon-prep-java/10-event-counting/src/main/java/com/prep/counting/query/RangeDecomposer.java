package com.prep.counting.query;

import static com.prep.counting.domain.Times.aligned;
import static com.prep.counting.domain.Times.floor;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Splits any half-open range [from, to) into the fewest pieces, each read from one tier.
 *
 * <p>Greedy, one step at a time: at the current instant take the coarsest bucket that starts here and still
 * fits, then merge it with the previous piece if that piece has the same tier. Edges shorter than a minute,
 * and everything after {@code sealedUntil}, come from raw.
 *
 * <p>Bound: at most 2 raw pieces, 118 minute buckets, 46 hour buckets, plus one bucket per whole day.
 */
public final class RangeDecomposer {

    public record Part(Tier tier, Instant from, Instant to) {}

    private static final List<Tier> ROLLUPS = List.of(Tier.DAY, Tier.HOUR, Tier.MINUTE);
    private static final Duration MINUTE = Duration.ofMinutes(1);

    private RangeDecomposer() {}

    public static List<Part> decompose(Instant from, Instant to, Instant sealedUntil) {
        List<Part> parts = new ArrayList<>();
        if (!from.isBefore(to)) {
            return parts;
        }
        Instant rolledEnd = clamp(sealedUntil, from, to); // buckets are only trusted before the seal
        Instant cur = from;
        while (cur.isBefore(rolledEnd)) {
            Tier tier = coarsestFitting(cur, rolledEnd);
            Instant next = tier != null
                    ? cur.plus(tier.step)
                    : min(rolledEnd, floor(cur, MINUTE).plus(MINUTE)); // < 1 minute edge: raw
            add(parts, tier != null ? tier : Tier.RAW, cur, next);
            cur = next;
        }
        if (rolledEnd.isBefore(to)) {
            add(parts, Tier.RAW, rolledEnd, to); // not sealed yet: still filling, read it live
        }
        return parts;
    }

    private static Tier coarsestFitting(Instant cur, Instant end) {
        for (Tier t : ROLLUPS) {
            if (aligned(cur, t.step) && !cur.plus(t.step).isAfter(end)) {
                return t;
            }
        }
        return null;
    }

    private static void add(List<Part> parts, Tier tier, Instant from, Instant to) {
        if (!parts.isEmpty()) {
            Part last = parts.get(parts.size() - 1);
            if (last.tier() == tier && last.to().equals(from)) {
                parts.set(parts.size() - 1, new Part(tier, last.from(), to));
                return;
            }
        }
        parts.add(new Part(tier, from, to));
    }

    private static Instant clamp(Instant t, Instant lo, Instant hi) {
        return t.isBefore(lo) ? lo : (t.isAfter(hi) ? hi : t);
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
