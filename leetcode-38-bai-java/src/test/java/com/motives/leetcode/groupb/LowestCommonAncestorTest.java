package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.motives.leetcode.common.TreeNode;
import org.junit.jupiter.api.Test;

class LowestCommonAncestorTest {

    private final LowestCommonAncestor solution = new LowestCommonAncestor();

    @Test
    void findsTheDeepestSharedAncestorOfTwoNodes() {
        TreeNode nodeFour = new TreeNode(4);
        TreeNode nodeSeven = new TreeNode(7);
        TreeNode nodeTwo = new TreeNode(2, nodeSeven, nodeFour);
        TreeNode nodeSix = new TreeNode(6);
        TreeNode nodeFive = new TreeNode(5, nodeSix, nodeTwo);
        TreeNode nodeZero = new TreeNode(0);
        TreeNode nodeEight = new TreeNode(8);
        TreeNode nodeOne = new TreeNode(1, nodeZero, nodeEight);
        TreeNode root = new TreeNode(3, nodeFive, nodeOne);

        assertEquals(root, solution.lowestCommonAncestor(root, nodeFive, nodeOne));
    }
}
