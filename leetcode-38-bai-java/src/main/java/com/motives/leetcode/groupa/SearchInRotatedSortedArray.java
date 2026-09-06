package com.motives.leetcode.groupa;

/**
 * LeetCode #33 - Search in Rotated Sorted Array (Nhom A, bai 18).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class SearchInRotatedSortedArray {

    public int search(int[] nums, int target) {
        int left = 0;
        int right = nums.length - 1;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) {
                return mid;
            }

            boolean leftHalfIsSorted = nums[left] <= nums[mid];
            if (leftHalfIsSorted) {
                if (nums[left] <= target && target < nums[mid]) {
                    right = mid - 1;
                } else {
                    left = mid + 1;
                }
            } else {
                if (nums[mid] < target && target <= nums[right]) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }
        }
        return -1;
    }
}
