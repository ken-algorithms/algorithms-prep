package com.motives.leetcode.groupb;

import static com.motives.leetcode.support.TreeNodes.fromLevelOrder;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidateBinarySearchTreeTest {

    private final ValidateBinarySearchTree solution = new ValidateBinarySearchTree();

    @Test
    void acceptsATreeThatSatisfiesTheBstProperty() {
        assertTrue(solution.isValidBST(fromLevelOrder(2, 1, 3)));
    }

    @Test
    void rejectsATreeWhereARightChildIsSmallerThanTheRoot() {
        assertFalse(solution.isValidBST(fromLevelOrder(5, 1, 4, null, null, 3, 6)));
    }
}
