package com.motives.leetcode.groupb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * LeetCode #15 - 3Sum (Nhom B, bai 19).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ThreeSum {

    public List<List<Integer>> threeSum(int[] nums) {
        int[] sorted = nums.clone();
        Arrays.sort(sorted);

        List<List<Integer>> triplets = new ArrayList<>();
        for (int i = 0; i < sorted.length - 2; i++) {
            if (i > 0 && sorted[i] == sorted[i - 1]) {
                continue;
            }
            findPairsWithTargetSum(sorted, i, triplets);
        }
        return triplets;
    }

    private void findPairsWithTargetSum(int[] sorted, int fixedIndex, List<List<Integer>> triplets) {
        int left = fixedIndex + 1;
        int right = sorted.length - 1;

        while (left < right) {
            int sum = sorted[fixedIndex] + sorted[left] + sorted[right];
            if (sum < 0) {
                left++;
            } else if (sum > 0) {
                right--;
            } else {
                triplets.add(List.of(sorted[fixedIndex], sorted[left], sorted[right]));
                left++;
                right--;
                while (left < right && sorted[left] == sorted[left - 1]) {
                    left++;
                }
                while (left < right && sorted[right] == sorted[right + 1]) {
                    right--;
                }
            }
        }
    }
}
