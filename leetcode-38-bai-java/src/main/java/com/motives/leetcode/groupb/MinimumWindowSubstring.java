package com.motives.leetcode.groupb;

/**
 * LeetCode #76 - Minimum Window Substring (Nhom B, bai 24).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class MinimumWindowSubstring {

    private static final int ASCII_SIZE = 128;

    public String minWindow(String s, String t) {
        if (s.isEmpty() || t.isEmpty()) {
            return "";
        }

        int[] remainingNeeded = new int[ASCII_SIZE];
        for (char c : t.toCharArray()) {
            remainingNeeded[c]++;
        }

        int missingCount = t.length();
        int windowStart = 0;
        int bestStart = 0;
        int bestEnd = 0;

        for (int windowEnd = 1; windowEnd <= s.length(); windowEnd++) {
            char enteringChar = s.charAt(windowEnd - 1);
            if (remainingNeeded[enteringChar] > 0) {
                missingCount--;
            }
            remainingNeeded[enteringChar]--;

            if (missingCount == 0) {
                windowStart = shrinkWindow(s, remainingNeeded, windowStart);
                if (bestEnd == 0 || windowEnd - windowStart < bestEnd - bestStart) {
                    bestStart = windowStart;
                    bestEnd = windowEnd;
                }
                remainingNeeded[s.charAt(windowStart)]++;
                missingCount++;
                windowStart++;
            }
        }
        return s.substring(bestStart, bestEnd);
    }

    private int shrinkWindow(String s, int[] remainingNeeded, int windowStart) {
        while (remainingNeeded[s.charAt(windowStart)] < 0) {
            remainingNeeded[s.charAt(windowStart)]++;
            windowStart++;
        }
        return windowStart;
    }
}
