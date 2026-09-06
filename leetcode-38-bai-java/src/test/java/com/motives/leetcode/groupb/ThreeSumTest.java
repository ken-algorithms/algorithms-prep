package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class ThreeSumTest {

    private final ThreeSum solution = new ThreeSum();

    @Test
    void findsAllUniqueTripletsThatSumToZero() {
        int[] input = {-1, 0, 1, 2, -1, -4};

        List<List<Integer>> expected = List.of(List.of(-1, -1, 2), List.of(-1, 0, 1));
        assertEquals(expected, solution.threeSum(input));
    }
}
