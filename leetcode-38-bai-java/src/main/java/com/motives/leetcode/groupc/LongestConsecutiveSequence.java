package com.motives.leetcode.groupc;

import java.util.HashSet;
import java.util.Set;

/**
 * LeetCode #128 - Longest Consecutive Sequence (Nhom C, bai 32).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LongestConsecutiveSequence {

    public int longestConsecutive(int[] nums) {
        Set<Integer> values = new HashSet<>();
        for (int num : nums) {
            values.add(num);
        }

        int longest = 0;
        for (int value : values) {
            boolean isSequenceStart = !values.contains(value - 1);
            if (isSequenceStart) {
                longest = Math.max(longest, sequenceLengthFrom(values, value));
            }
        }
        return longest;
    }

    private int sequenceLengthFrom(Set<Integer> values, int start) {
        int length = 1;
        while (values.contains(start + length)) {
            length++;
        }
        return length;
    }
}
