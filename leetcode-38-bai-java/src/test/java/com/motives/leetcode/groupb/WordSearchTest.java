package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WordSearchTest {

    private final WordSearch solution = new WordSearch();

    private char[][] sampleBoard() {
        return new char[][] {
            {'A', 'B', 'C', 'E'},
            {'S', 'F', 'C', 'S'},
            {'A', 'D', 'E', 'E'},
        };
    }

    @Test
    void findsAWordThatFollowsAdjacentCells() {
        assertTrue(solution.exist(sampleBoard(), "ABCCED"));
    }

    @Test
    void findsAWordThatChangesDirection() {
        assertTrue(solution.exist(sampleBoard(), "SEE"));
    }

    @Test
    void rejectsAWordThatWouldReuseACell() {
        assertFalse(solution.exist(sampleBoard(), "ABCB"));
    }
}
