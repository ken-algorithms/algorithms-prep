package com.motives.leetcode.groupc;

import java.util.HashSet;
import java.util.Set;

/**
 * LeetCode #202 - Happy Number (Nhom C, bai 35).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class HappyNumber {

    public boolean isHappy(int n) {
        Set<Integer> seen = new HashSet<>();
        int current = n;
        while (current != 1 && seen.add(current)) {
            current = sumOfSquaredDigits(current);
        }
        return current == 1;
    }

    private int sumOfSquaredDigits(int number) {
        int sum = 0;
        while (number > 0) {
            int digit = number % 10;
            sum += digit * digit;
            number /= 10;
        }
        return sum;
    }
}
