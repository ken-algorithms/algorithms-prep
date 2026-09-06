package com.motives.leetcode.groupc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LeetCode #350 - Intersection of Two Arrays II (Nhom C, bai 34).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class IntersectionOfTwoArrays {

    public int[] intersect(int[] nums1, int[] nums2) {
        Map<Integer, Integer> remainingCount = new HashMap<>();
        for (int num : nums1) {
            remainingCount.merge(num, 1, Integer::sum);
        }

        List<Integer> intersection = new ArrayList<>();
        for (int num : nums2) {
            int available = remainingCount.getOrDefault(num, 0);
            if (available > 0) {
                intersection.add(num);
                remainingCount.put(num, available - 1);
            }
        }
        return intersection.stream().mapToInt(Integer::intValue).toArray();
    }
}
