package com.motives.leetcode.groupc;

/**
 * LeetCode #242 - Valid Anagram (Nhom C, bai 30).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ValidAnagram {

    private static final int LOWERCASE_LETTER_COUNT = 26;

    public boolean isAnagram(String s, String t) {
        if (s.length() != t.length()) {
            return false;
        }

        int[] letterCounts = new int[LOWERCASE_LETTER_COUNT];
        for (int i = 0; i < s.length(); i++) {
            letterCounts[s.charAt(i) - 'a']++;
            letterCounts[t.charAt(i) - 'a']--;
        }

        for (int count : letterCounts) {
            if (count != 0) {
                return false;
            }
        }
        return true;
    }
}
