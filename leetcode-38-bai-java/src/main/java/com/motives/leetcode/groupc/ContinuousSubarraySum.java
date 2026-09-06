package com.motives.leetcode.groupc;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #523 - Continuous Subarray Sum (Nhom C, bai 37).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ContinuousSubarraySum {

    public boolean checkSubarraySum(int[] nums, int k) {
        Map<Integer, Integer> firstIndexByRemainder = new HashMap<>();
        firstIndexByRemainder.put(0, -1);

        int prefixSum = 0;
        for (int i = 0; i < nums.length; i++) {
            prefixSum += nums[i];
            int remainder = prefixSum % k;

            Integer firstIndex = firstIndexByRemainder.get(remainder);
            if (firstIndex != null) {
                if (i - firstIndex > 1) {
                    return true;
                }
            } else {
                firstIndexByRemainder.put(remainder, i);
            }
        }
        return false;
    }
}
