package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SubarraySumEqualsKTest {

    private final SubarraySumEqualsK solution = new SubarraySumEqualsK();

    @Test
    void countsContiguousSubarraysThatSumToK() {
        assertEquals(2, solution.subarraySum(new int[] {1, 1, 1}, 2));
    }
}
