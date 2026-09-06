package com.motives.leetcode.groupa;

/**
 * LeetCode #5 - Longest Palindromic Substring (Nhom A, bai 13).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LongestPalindromicSubstring {

    public String longestPalindrome(String s) {
        if (s.isEmpty()) {
            return "";
        }

        int bestStart = 0;
        int bestEnd = 0;
        for (int center = 0; center < s.length(); center++) {
            int[] oddBounds = expandAroundCenter(s, center, center);
            if (oddBounds[1] - oddBounds[0] > bestEnd - bestStart) {
                bestStart = oddBounds[0];
                bestEnd = oddBounds[1];
            }

            int[] evenBounds = expandAroundCenter(s, center, center + 1);
            if (evenBounds[1] - evenBounds[0] > bestEnd - bestStart) {
                bestStart = evenBounds[0];
                bestEnd = evenBounds[1];
            }
        }
        return s.substring(bestStart, bestEnd + 1);
    }

    private int[] expandAroundCenter(String s, int left, int right) {
        while (left >= 0 && right < s.length() && s.charAt(left) == s.charAt(right)) {
            left--;
            right++;
        }
        return new int[] {left + 1, right - 1};
    }
}
