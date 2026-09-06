package com.motives.leetcode.groupa;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #1 - Two Sum (Nhom A, bai 1).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class TwoSum {

    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> valueToIndex = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            Integer complementIndex = valueToIndex.get(complement);
            if (complementIndex != null) {
                return new int[] {complementIndex, i};
            }
            valueToIndex.put(nums[i], i);
        }
        throw new IllegalArgumentException("No two sum solution exists for the given input");
    }
}
