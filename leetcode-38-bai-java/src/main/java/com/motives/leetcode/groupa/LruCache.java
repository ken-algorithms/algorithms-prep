package com.motives.leetcode.groupa;

import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #146 - LRU Cache (Nhom A, bai 3).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 *
 * <p>HashMap cho tra cuu O(1) + doubly linked list cho thu tu su dung O(1).
 * Node moi duoc dua vao cuoi (most-recently-used); khi vuot capacity,
 * node dau (least-recently-used) bi loai.
 */
public class LruCache {

    private final int capacity;
    private final Map<Integer, Node> cache;
    private final Node head;
    private final Node tail;

    public LruCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.head = new Node(0, 0);
        this.tail = new Node(0, 0);
        head.next = tail;
        tail.prev = head;
    }

    public int get(int key) {
        Node node = cache.get(key);
        if (node == null) {
            return -1;
        }
        moveToMostRecentlyUsed(node);
        return node.value;
    }

    public void put(int key, int value) {
        Node existing = cache.get(key);
        if (existing != null) {
            existing.value = value;
            moveToMostRecentlyUsed(existing);
            return;
        }

        Node created = new Node(key, value);
        cache.put(key, created);
        addToMostRecentlyUsed(created);

        if (cache.size() > capacity) {
            Node leastRecentlyUsed = head.next;
            removeNode(leastRecentlyUsed);
            cache.remove(leastRecentlyUsed.key);
        }
    }

    private void moveToMostRecentlyUsed(Node node) {
        removeNode(node);
        addToMostRecentlyUsed(node);
    }

    private void addToMostRecentlyUsed(Node node) {
        Node previousTail = tail.prev;
        previousTail.next = node;
        node.prev = previousTail;
        node.next = tail;
        tail.prev = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private static final class Node {
        private final int key;
        private int value;
        private Node prev;
        private Node next;

        private Node(int key, int value) {
            this.key = key;
            this.value = value;
        }
    }
}
