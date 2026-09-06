package com.motives.leetcode.groupb;

import java.util.Arrays;

/**
 * LeetCode #322 - Coin Change (Nhom B, bai 23).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class CoinChange {

    private static final int UNREACHABLE = Integer.MAX_VALUE;

    public int coinChange(int[] coins, int amount) {
        int[] minCoinsForAmount = new int[amount + 1];
        Arrays.fill(minCoinsForAmount, UNREACHABLE);
        minCoinsForAmount[0] = 0;

        for (int currentAmount = 1; currentAmount <= amount; currentAmount++) {
            for (int coin : coins) {
                if (coin <= currentAmount && minCoinsForAmount[currentAmount - coin] != UNREACHABLE) {
                    minCoinsForAmount[currentAmount] =
                            Math.min(minCoinsForAmount[currentAmount], minCoinsForAmount[currentAmount - coin] + 1);
                }
            }
        }
        return minCoinsForAmount[amount] == UNREACHABLE ? -1 : minCoinsForAmount[amount];
    }
}
