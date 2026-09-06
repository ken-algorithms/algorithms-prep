package com.motives.leetcode.groupa;

import static com.motives.leetcode.support.TreeNodes.fromLevelOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.motives.leetcode.common.TreeNode;
import java.util.List;
import org.junit.jupiter.api.Test;

class BinaryTreeLevelOrderTraversalTest {

    private final BinaryTreeLevelOrderTraversal solution = new BinaryTreeLevelOrderTraversal();

    @Test
    void groupsNodeValuesByDepth() {
        TreeNode root = fromLevelOrder(3, 9, 20, null, null, 15, 7);

        List<List<Integer>> expected = List.of(List.of(3), List.of(9, 20), List.of(15, 7));
        assertEquals(expected, solution.levelOrder(root));
    }
}
