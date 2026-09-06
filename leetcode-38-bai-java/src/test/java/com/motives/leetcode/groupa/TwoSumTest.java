package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class TwoSumTest {

    private final TwoSum solution = new TwoSum();

    @Test
    void returnsIndicesOfTheTwoNumbersThatAddUpToTarget() {
        assertArrayEquals(new int[] {0, 1}, solution.twoSum(new int[] {2, 7, 11, 15}, 9));
    }
}
