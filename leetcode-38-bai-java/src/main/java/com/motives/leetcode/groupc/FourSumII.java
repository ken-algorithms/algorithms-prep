package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #454 - 4Sum II (Nhom C, bai 36).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class FourSumII {

    public int fourSumCount(int[] nums1, int[] nums2, int[] nums3, int[] nums4) {
        Map<Integer, Integer> countByPairSum = new HashMap<>();
        for (int a : nums1) {
            for (int b : nums2) {
                countByPairSum.merge(a + b, 1, Integer::sum);
            }
        }

        int totalQuadruplets = 0;
        for (int c : nums3) {
            for (int d : nums4) {
                totalQuadruplets += countByPairSum.getOrDefault(-(c + d), 0);
            }
        }
        return totalQuadruplets;
    }
}
