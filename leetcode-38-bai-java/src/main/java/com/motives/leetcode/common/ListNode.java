package com.motives.leetcode.common;

/**
 * Singly linked list node shared by every linked-list problem.
 * Unlike {@link TreeNode}, algorithms such as "Remove Nth Node From End of
 * List" relinken nodes in place, so {@code next} must stay mutable.
 * Encapsulated behind getters/setters instead of public fields.
 */
public final class ListNode {

    private int val;
    private ListNode next;

    public ListNode(int val) {
        this.val = val;
    }

    public ListNode(int val, ListNode next) {
        this.val = val;
        this.next = next;
    }

    public int getVal() {
        return val;
    }

    public void setVal(int val) {
        this.val = val;
    }

    public ListNode getNext() {
        return next;
    }

    public void setNext(ListNode next) {
        this.next = next;
    }
}
