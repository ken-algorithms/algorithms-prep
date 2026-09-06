package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HappyNumberTest {

    private final HappyNumber solution = new HappyNumber();

    @Test
    void recognizesANumberThatEventuallyReachesOne() {
        assertTrue(solution.isHappy(19));
    }

    @Test
    void recognizesANumberThatFallsIntoAnInfiniteCycle() {
        assertFalse(solution.isHappy(2));
    }
}
