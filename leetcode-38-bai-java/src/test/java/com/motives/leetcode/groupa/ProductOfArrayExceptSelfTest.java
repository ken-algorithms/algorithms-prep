package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class ProductOfArrayExceptSelfTest {

    private final ProductOfArrayExceptSelf solution = new ProductOfArrayExceptSelf();

    @Test
    void computesProductOfAllOtherElementsWithoutDivision() {
        assertArrayEquals(new int[] {24, 12, 8, 6}, solution.productExceptSelf(new int[] {1, 2, 3, 4}));
    }
}
