package com.motives.leetcode.groupb;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * LeetCode #347 - Top K Frequent Elements (Nhom B, bai 25).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class TopKFrequentElements {

    public int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> frequencyByValue = new HashMap<>();
        for (int num : nums) {
            frequencyByValue.merge(num, 1, Integer::sum);
        }

        PriorityQueue<Integer> leastFrequentOfTopK =
                new PriorityQueue<>((a, b) -> frequencyByValue.get(a) - frequencyByValue.get(b));
        for (int value : frequencyByValue.keySet()) {
            leastFrequentOfTopK.add(value);
            if (leastFrequentOfTopK.size() > k) {
                leastFrequentOfTopK.poll();
            }
        }

        int[] result = new int[k];
        for (int i = k - 1; i >= 0; i--) {
            result[i] = leastFrequentOfTopK.poll();
        }
        return result;
    }
}
