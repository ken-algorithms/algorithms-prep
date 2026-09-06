package com.motives.leetcode.groupb;

import java.util.List;
import java.util.Set;

/**
 * LeetCode #139 - Word Break (Nhom B, bai 22).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class WordBreak {

    public boolean wordBreak(String s, List<String> wordDict) {
        Set<String> dictionary = Set.copyOf(wordDict);
        int n = s.length();

        boolean[] breakable = new boolean[n + 1];
        breakable[0] = true;

        for (int end = 1; end <= n; end++) {
            for (int start = 0; start < end; start++) {
                if (breakable[start] && dictionary.contains(s.substring(start, end))) {
                    breakable[end] = true;
                    break;
                }
            }
        }
        return breakable[n];
    }
}
