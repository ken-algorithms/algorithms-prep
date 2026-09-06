package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FourSumIITest {

    private final FourSumII solution = new FourSumII();

    @Test
    void countsQuadrupletsAcrossFourArraysThatSumToZero() {
        int[] nums1 = {1, 2};
        int[] nums2 = {-2, -1};
        int[] nums3 = {-1, 2};
        int[] nums4 = {0, 2};

        assertEquals(2, solution.fourSumCount(nums1, nums2, nums3, nums4));
    }
}
