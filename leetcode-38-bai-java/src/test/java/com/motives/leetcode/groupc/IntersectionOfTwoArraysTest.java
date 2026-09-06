package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class IntersectionOfTwoArraysTest {

    private final IntersectionOfTwoArrays solution = new IntersectionOfTwoArrays();

    @Test
    void keepsSharedElementsWithTheirMatchingMultiplicity() {
        assertArrayEquals(new int[] {2, 2}, solution.intersect(new int[] {1, 2, 2, 1}, new int[] {2, 2}));
    }
}
