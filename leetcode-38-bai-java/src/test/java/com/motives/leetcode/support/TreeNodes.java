package com.motives.leetcode.support;

import com.motives.leetcode.common.TreeNode;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Test-only helper to build a {@link TreeNode} tree from a LeetCode-style
 * level-order array (nulls mark missing children), e.g. {3, 9, 20, null, null, 15, 7}.
 *
 * <p>The immutable {@link TreeNode} record cannot be filled in level by level
 * (a node's children are only known after it is dequeued), so this builds a
 * mutable scratch tree first and converts it to immutable nodes bottom-up.
 */
public final class TreeNodes {

    private TreeNodes() {
    }

    public static TreeNode fromLevelOrder(Integer... values) {
        if (values.length == 0 || values[0] == null) {
            return null;
        }

        MutableNode root = new MutableNode(values[0]);
        Deque<MutableNode> queue = new ArrayDeque<>();
        queue.add(root);

        int i = 1;
        while (!queue.isEmpty() && i < values.length) {
            MutableNode current = queue.poll();
            if (i < values.length && values[i] != null) {
                current.left = new MutableNode(values[i]);
                queue.add(current.left);
            }
            i++;
            if (i < values.length && values[i] != null) {
                current.right = new MutableNode(values[i]);
                queue.add(current.right);
            }
            i++;
        }
        return toImmutable(root);
    }

    private static TreeNode toImmutable(MutableNode node) {
        if (node == null) {
            return null;
        }
        return new TreeNode(node.value, toImmutable(node.left), toImmutable(node.right));
    }

    private static final class MutableNode {
        private final int value;
        private MutableNode left;
        private MutableNode right;

        private MutableNode(int value) {
            this.value = value;
        }
    }
}
