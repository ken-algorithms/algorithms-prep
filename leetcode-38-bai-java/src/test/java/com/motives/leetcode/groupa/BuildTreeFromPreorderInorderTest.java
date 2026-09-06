package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.motives.leetcode.common.TreeNode;
import org.junit.jupiter.api.Test;

class BuildTreeFromPreorderInorderTest {

    private final BuildTreeFromPreorderInorder solution = new BuildTreeFromPreorderInorder();

    @Test
    void rebuildsTheSameTreeFromItsPreorderAndInorderTraversals() {
        TreeNode tree = solution.buildTree(
                new int[] {3, 9, 20, 15, 7},
                new int[] {9, 3, 15, 20, 7});

        assertEquals(3, tree.val());
        assertEquals(9, tree.left().val());
        assertEquals(20, tree.right().val());
        assertEquals(15, tree.right().left().val());
        assertEquals(7, tree.right().right().val());
    }
}
