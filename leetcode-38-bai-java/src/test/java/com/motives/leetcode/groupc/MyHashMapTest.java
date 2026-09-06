package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MyHashMapTest {

    @Test
    void storesUpdatesAndRemovesKeyValuePairs() {
        MyHashMap map = new MyHashMap();

        map.put(1, 1);
        map.put(2, 2);
        assertEquals(1, map.get(1));
        assertEquals(-1, map.get(3), "key 3 was never inserted");

        map.put(2, 1);
        assertEquals(1, map.get(2), "put on an existing key should overwrite its value");

        map.remove(2);
        assertEquals(-1, map.get(2), "key 2 should be gone after remove");
    }
}
