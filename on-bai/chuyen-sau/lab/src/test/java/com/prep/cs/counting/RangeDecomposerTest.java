package com.prep.cs.counting;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

class RangeDecomposerTest {

    static long sec(String iso) {
        return Instant.parse(iso).getEpochSecond();
    }

    @Test
    void example() {
        List<RangeDecomposer.Range> rs = RangeDecomposer.decompose(sec("2026-10-19T02:59:58Z"), sec("2026-10-19T05:00:03Z"));
        rs.forEach(r -> System.out.printf("[LAB] %-6s [%s, %s) %d bucket%n", r.tier(),
                Instant.ofEpochSecond(r.fromBucket()), Instant.ofEpochSecond(r.toBucket()), r.buckets()));
        assertThat(rs).extracting(RangeDecomposer.Range::tier)
                .containsExactly(Tier.SECOND, Tier.HOUR, Tier.SECOND);
        assertThat(RangeDecomposer.totalBuckets(rs)).isEqualTo(2 + 2 + 3);
    }

    @Test
    void worstCaseIsBoundedByTierSizesNotByData() {
        // khoang xau nhat cua mot nam: lech 1 giay ca hai dau
        long from = sec("2025-10-19T00:00:01Z"), to = sec("2026-10-18T23:59:59Z");
        List<RangeDecomposer.Range> rs = RangeDecomposer.decompose(from, to);
        long buckets = RangeDecomposer.totalBuckets(rs);
        System.out.printf("[LAB] mot nam lech 1 giay hai dau: %d khoang, %d bucket%n", rs.size(), buckets);
        assertThat(buckets).isLessThanOrEqualTo(282 + 365);
    }

    @Test
    void randomRangesCoverExactlyTheSameSeconds() {
        SplittableRandom r = new SplittableRandom(3);
        long base = sec("2026-10-17T00:00:00Z");
        for (int i = 0; i < 2_000; i++) {
            long from = base + r.nextLong(3 * 86_400), to = from + r.nextLong(2 * 86_400);
            long covered = 0, prevEnd = from;
            for (RangeDecomposer.Range x : RangeDecomposer.decompose(from, to)) {
                assertThat(x.fromBucket()).isEqualTo(prevEnd);              // lien mach, khong ho, khong chong
                assertThat(Math.floorMod(x.fromBucket(), x.tier().seconds)).isZero();
                covered += x.toBucket() - x.fromBucket();
                prevEnd = x.toBucket();
            }
            assertThat(covered).isEqualTo(to - from);
            assertThat(RangeDecomposer.totalBuckets(RangeDecomposer.decompose(from, to)))
                    .isLessThanOrEqualTo(282 + (to - from) / 86_400);
        }
    }
}
