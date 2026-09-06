package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #560 - Subarray Sum Equals K (Nhom C, bai 33).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class SubarraySumEqualsK {

    public int subarraySum(int[] nums, int k) {
        Map<Integer, Integer> countByPrefixSum = new HashMap<>();
        countByPrefixSum.put(0, 1);

        int prefixSum = 0;
        int matchingSubarrays = 0;
        for (int num : nums) {
            prefixSum += num;
            matchingSubarrays += countByPrefixSum.getOrDefault(prefixSum - k, 0);
            countByPrefixSum.merge(prefixSum, 1, Integer::sum);
        }
        return matchingSubarrays;
    }
}
