package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContinuousSubarraySumTest {

    private final ContinuousSubarraySum solution = new ContinuousSubarraySum();

    @Test
    void findsASubarrayOfAtLeastTwoElementsWhoseSumIsAMultipleOfK() {
        assertTrue(solution.checkSubarraySum(new int[] {23, 2, 4, 6, 7}, 6));
    }
}
