package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MinimumWindowSubstringTest {

    private final MinimumWindowSubstring solution = new MinimumWindowSubstring();

    @Test
    void findsTheShortestWindowContainingAllCharactersOfTheTarget() {
        assertEquals("BANC", solution.minWindow("ADOBECODEBANC", "ABC"));
    }
}
