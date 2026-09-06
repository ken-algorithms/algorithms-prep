package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CoinChangeTest {

    private final CoinChange solution = new CoinChange();

    @Test
    void findsTheFewestCoinsThatAddUpToTheAmount() {
        assertEquals(3, solution.coinChange(new int[] {1, 2, 5}, 11));
    }

    @Test
    void returnsMinusOneWhenTheAmountIsUnreachable() {
        assertEquals(-1, solution.coinChange(new int[] {2}, 3));
    }
}
