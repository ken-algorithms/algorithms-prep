package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KthLargestElementTest {

    private final KthLargestElement solution = new KthLargestElement();

    @Test
    void findsTheKthLargestValueInUnsortedInput() {
        assertEquals(5, solution.findKthLargest(new int[] {3, 2, 1, 5, 6, 4}, 2));
    }
}
