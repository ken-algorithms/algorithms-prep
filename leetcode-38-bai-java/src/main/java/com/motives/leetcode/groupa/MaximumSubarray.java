package com.motives.leetcode.groupa;

/**
 * LeetCode #53 - Maximum Subarray (Nhom A, bai 12).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MaximumSubarray {

    public int maxSubArray(int[] nums) {
        int best = nums[0];
        int currentSum = nums[0];

        for (int i = 1; i < nums.length; i++) {
            currentSum = Math.max(nums[i], currentSum + nums[i]);
            best = Math.max(best, currentSum);
        }
        return best;
    }
}
