package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BestTimeToBuySellStockTest {

    private final BestTimeToBuySellStock solution = new BestTimeToBuySellStock();

    @Test
    void findsTheMaximumProfitFromOneBuyAndOneSell() {
        assertEquals(5, solution.maxProfit(new int[] {7, 1, 5, 3, 6, 4}));
    }
}
