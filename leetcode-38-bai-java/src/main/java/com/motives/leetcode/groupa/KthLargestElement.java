package com.motives.leetcode.groupa;

import java.util.PriorityQueue;

/**
 * LeetCode #215 - Kth Largest Element in an Array (Nhom A, bai 17).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class KthLargestElement {

    public int findKthLargest(int[] nums, int k) {
        PriorityQueue<Integer> smallestOfTopK = new PriorityQueue<>(k);
        for (int num : nums) {
            smallestOfTopK.add(num);
            if (smallestOfTopK.size() > k) {
                smallestOfTopK.poll();
            }
        }
        return smallestOfTopK.peek();
    }
}
