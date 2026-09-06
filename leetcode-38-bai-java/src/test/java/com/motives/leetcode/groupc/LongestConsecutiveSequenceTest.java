package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LongestConsecutiveSequenceTest {

    private final LongestConsecutiveSequence solution = new LongestConsecutiveSequence();

    @Test
    void findsTheLongestRunOfConsecutiveIntegersRegardlessOfOrder() {
        assertEquals(4, solution.longestConsecutive(new int[] {100, 4, 200, 1, 3, 2}));
    }
}
