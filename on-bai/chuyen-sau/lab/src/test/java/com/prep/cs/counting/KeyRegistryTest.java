package com.prep.cs.counting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class KeyRegistryTest {

    @Test
    void findsEveryKeyAndNoFakeOneAmongAMillionProbes() {
        Hash128 h = new Hash128(12345, 67890);
        List<String> keys = IntStream.range(0, 1_000_000).mapToObj(i -> "feature_" + i).toList();
        KeyRegistry reg = KeyRegistry.of(keys, h, 1);
        assertThat(reg.size()).isEqualTo(1_000_000);
        assertThat(keys).allMatch(reg::contains);
        SplittableRandom r = new SplittableRandom(1);
        long falsePositive = IntStream.range(0, 1_000_000)
                .filter(i -> reg.contains("fake_" + Long.toHexString(r.nextLong()))).count();
        System.out.printf("[LAB] registry 1.000.000 key: %d o (tai %.2f), %.1f MB cho hai mang fp; "
                        + "1.000.000 key gia -> %d bi nhan nham%n", reg.capacity(), (double) reg.size() / reg.capacity(),
                reg.capacity() * 16 / 1e6, falsePositive);
        assertThat(falsePositive).isZero();
    }

    @Test
    void differentSeedsGiveDifferentLayouts() {
        long[] a = new long[2], b = new long[2];
        new Hash128(1, 2).hash("checkout_v2", a);
        new Hash128(3, 4).hash("checkout_v2", b);
        assertThat(a).isNotEqualTo(b);   // ke tan cong khong biet seed thi khong tinh truoc duoc va cham
    }
}
