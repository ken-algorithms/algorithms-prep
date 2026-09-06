package com.motives.leetcode.support;

import com.motives.leetcode.common.ListNode;
import java.util.ArrayList;
import java.util.List;

/** Test-only helpers to build/read a {@link ListNode} chain from plain values. */
public final class ListNodes {

    private ListNodes() {
    }

    public static ListNode of(int... values) {
        ListNode dummy = new ListNode(0);
        ListNode tail = dummy;
        for (int value : values) {
            ListNode node = new ListNode(value);
            tail.setNext(node);
            tail = node;
        }
        return dummy.getNext();
    }

    public static List<Integer> toList(ListNode head) {
        List<Integer> values = new ArrayList<>();
        for (ListNode current = head; current != null; current = current.getNext()) {
            values.add(current.getVal());
        }
        return values;
    }
}
