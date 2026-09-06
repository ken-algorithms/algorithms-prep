package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TopKFrequentElementsTest {

    private final TopKFrequentElements solution = new TopKFrequentElements();

    @Test
    void returnsTheKMostFrequentValuesRegardlessOfOrder() {
        int[] result = solution.topKFrequent(new int[] {1, 1, 1, 2, 2, 3}, 2);

        assertEquals(Set.of(1, 2), Set.of(Arrays.stream(result).boxed().toArray(Integer[]::new)));
    }
}
