package com.motives.leetcode.groupa;

/**
 * LeetCode #121 - Best Time to Buy and Sell Stock (Nhom A, bai 8).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class BestTimeToBuySellStock {

    public int maxProfit(int[] prices) {
        int minPriceSoFar = Integer.MAX_VALUE;
        int maxProfitSoFar = 0;

        for (int price : prices) {
            minPriceSoFar = Math.min(minPriceSoFar, price);
            maxProfitSoFar = Math.max(maxProfitSoFar, price - minPriceSoFar);
        }
        return maxProfitSoFar;
    }
}
