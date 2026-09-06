package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

class LongestPalindromicSubstringTest {

    private final LongestPalindromicSubstring solution = new LongestPalindromicSubstring();

    @Test
    void findsTheLongestPalindromicSubstring() {
        assertTrue(Set.of("bab", "aba").contains(solution.longestPalindrome("babad")));
    }
}
