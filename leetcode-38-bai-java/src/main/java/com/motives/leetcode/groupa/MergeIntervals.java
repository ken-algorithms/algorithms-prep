package com.motives.leetcode.groupa;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Comparator;

/**
 * LeetCode #56 - Merge Intervals (Nhom A, bai 2).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MergeIntervals {

    public int[][] merge(int[][] intervals) {
        int[][] sorted = intervals.clone();
        Arrays.sort(sorted, Comparator.comparingInt(interval -> interval[0]));

        Deque<int[]> merged = new ArrayDeque<>();
        for (int[] interval : sorted) {
            int[] last = merged.peekLast();
            if (last != null && interval[0] <= last[1]) {
                last[1] = Math.max(last[1], interval[1]);
            } else {
                merged.addLast(interval);
            }
        }
        return merged.toArray(new int[0][]);
    }
}
