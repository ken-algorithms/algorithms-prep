package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SearchInRotatedSortedArrayTest {

    private final SearchInRotatedSortedArray solution = new SearchInRotatedSortedArray();

    @Test
    void findsTheTargetInARotatedSortedArray() {
        assertEquals(4, solution.search(new int[] {4, 5, 6, 7, 0, 1, 2}, 0));
    }

    @Test
    void returnsMinusOneWhenTargetIsAbsent() {
        assertEquals(-1, solution.search(new int[] {4, 5, 6, 7, 0, 1, 2}, 3));
    }
}
