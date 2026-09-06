package com.motives.leetcode.groupa;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * LeetCode #253 - Meeting Rooms II (Nhom A, bai 14).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MeetingRoomsII {

    public int minMeetingRooms(int[][] intervals) {
        if (intervals.length == 0) {
            return 0;
        }

        int[][] sorted = intervals.clone();
        Arrays.sort(sorted, Comparator.comparingInt(interval -> interval[0]));

        PriorityQueue<Integer> endTimesInUse = new PriorityQueue<>();
        for (int[] meeting : sorted) {
            int start = meeting[0];
            int end = meeting[1];
            if (!endTimesInUse.isEmpty() && endTimesInUse.peek() <= start) {
                endTimesInUse.poll();
            }
            endTimesInUse.add(end);
        }
        return endTimesInUse.size();
    }
}
