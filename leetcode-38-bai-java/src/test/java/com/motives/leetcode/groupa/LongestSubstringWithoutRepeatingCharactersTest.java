package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LongestSubstringWithoutRepeatingCharactersTest {

    private final LongestSubstringWithoutRepeatingCharacters solution =
            new LongestSubstringWithoutRepeatingCharacters();

    @Test
    void findsTheLongestRunOfDistinctCharacters() {
        assertEquals(3, solution.lengthOfLongestSubstring("abcabcbb"));
    }
}
