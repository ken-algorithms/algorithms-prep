package com.motives.leetcode.groupc;

import java.util.ArrayList;
import java.util.List;

/**
 * LeetCode #706 - Design HashMap (Nhom C, bai 38).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 *
 * <p>Separate chaining: moi bucket la mot list cac Entry (key, value).
 */
public class MyHashMap {

    private static final int BUCKET_COUNT = 1000;

    private final List<List<Entry>> buckets;

    public MyHashMap() {
        buckets = new ArrayList<>(BUCKET_COUNT);
        for (int i = 0; i < BUCKET_COUNT; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    public void put(int key, int value) {
        List<Entry> bucket = bucketFor(key);
        for (int i = 0; i < bucket.size(); i++) {
            if (bucket.get(i).key() == key) {
                bucket.set(i, new Entry(key, value));
                return;
            }
        }
        bucket.add(new Entry(key, value));
    }

    public int get(int key) {
        for (Entry entry : bucketFor(key)) {
            if (entry.key() == key) {
                return entry.value();
            }
        }
        return -1;
    }

    public void remove(int key) {
        List<Entry> bucket = bucketFor(key);
        bucket.removeIf(entry -> entry.key() == key);
    }

    private List<Entry> bucketFor(int key) {
        int index = Math.floorMod(key, BUCKET_COUNT);
        return buckets.get(index);
    }

    private record Entry(int key, int value) {
    }
}
