package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidAnagramTest {

    private final ValidAnagram solution = new ValidAnagram();

    @Test
    void recognizesTwoStringsWithTheSameLetterCounts() {
        assertTrue(solution.isAnagram("anagram", "nagaram"));
    }
}
