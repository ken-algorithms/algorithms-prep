package com.motives.leetcode.groupa;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #3 - Longest Substring Without Repeating Characters (Nhom A, bai 7).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LongestSubstringWithoutRepeatingCharacters {

    public int lengthOfLongestSubstring(String s) {
        Map<Character, Integer> lastSeenIndex = new HashMap<>();
        int windowStart = 0;
        int longest = 0;

        for (int windowEnd = 0; windowEnd < s.length(); windowEnd++) {
            char current = s.charAt(windowEnd);
            Integer previousIndex = lastSeenIndex.get(current);
            if (previousIndex != null && previousIndex >= windowStart) {
                windowStart = previousIndex + 1;
            }
            lastSeenIndex.put(current, windowEnd);
            longest = Math.max(longest, windowEnd - windowStart + 1);
        }
        return longest;
    }
}
