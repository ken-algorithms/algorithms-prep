package com.motives.leetcode.groupc;

import java.util.HashSet;
import java.util.Set;

/**
 * LeetCode #217 - Contains Duplicate (Nhom C, bai 29).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ContainsDuplicate {

    public boolean containsDuplicate(int[] nums) {
        Set<Integer> seen = new HashSet<>();
        for (int num : nums) {
            if (!seen.add(num)) {
                return true;
            }
        }
        return false;
    }
}
