package com.motives.leetcode.groupa;

import static com.motives.leetcode.support.ListNodes.of;
import static com.motives.leetcode.support.ListNodes.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.motives.leetcode.common.ListNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class RemoveNthNodeFromEndOfListTest {

    private final RemoveNthNodeFromEndOfList solution = new RemoveNthNodeFromEndOfList();

    @Test
    void removesTheNthNodeCountedFromTheEnd() {
        ListNode head = of(1, 2, 3, 4, 5);

        ListNode result = solution.removeNthFromEnd(head, 2);

        assertEquals(List.of(1, 2, 3, 5), toList(result));
    }
}
