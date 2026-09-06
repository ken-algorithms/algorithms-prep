package com.motives.leetcode.groupa;

import com.motives.leetcode.common.ListNode;

/**
 * LeetCode #19 - Remove Nth Node From End of List (Nhom A, bai 15).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class RemoveNthNodeFromEndOfList {

    public ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode dummy = new ListNode(0, head);
        ListNode fast = dummy;
        ListNode slow = dummy;

        for (int i = 0; i < n; i++) {
            fast = fast.getNext();
        }
        while (fast.getNext() != null) {
            fast = fast.getNext();
            slow = slow.getNext();
        }
        slow.setNext(slow.getNext().getNext());
        return dummy.getNext();
    }
}
