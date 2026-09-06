package com.motives.leetcode.groupa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LeetCode #49 - Group Anagrams (Nhom A, bai 11).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class GroupAnagrams {

    public List<List<String>> groupAnagrams(String[] strs) {
        Map<String, List<String>> anagramsByKey = new LinkedHashMap<>();
        for (String word : strs) {
            String key = sortedKey(word);
            anagramsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(word);
        }
        return new ArrayList<>(anagramsByKey.values());
    }

    private String sortedKey(String word) {
        char[] letters = word.toCharArray();
        Arrays.sort(letters);
        return new String(letters);
    }
}
