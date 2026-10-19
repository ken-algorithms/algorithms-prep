package com.prep.cs.counting;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.SplittableRandom;

import org.junit.jupiter.api.Test;

class LongObjectMapTest {

    @Test
    void behavesLikeHashMapUnderRandomPutsAndRemoves() {
        LongObjectMap<Long> m = new LongObjectMap<>(4);
        Map<Long, Long> ref = new HashMap<>();
        SplittableRandom r = new SplittableRandom(9);
        for (int round = 0; round < 50; round++) {
            for (int i = 0; i < 2_000; i++) {
                long k = r.nextLong(5_000) - 2_500;
                m.put(k, k * 3);
                ref.put(k, k * 3);
            }
            long cut = r.nextLong(5_000) - 2_500;
            int removed = m.removeIf(v -> v / 3 < cut);
            int before = ref.size();
            ref.values().removeIf(v -> v / 3 < cut);
            assertThat(removed).isEqualTo(before - ref.size());
            assertThat(m.size()).isEqualTo(ref.size());
            for (long k = -2_500; k < 2_500; k++) assertThat(m.get(k)).isEqualTo(ref.get(k));
        }
    }
}
