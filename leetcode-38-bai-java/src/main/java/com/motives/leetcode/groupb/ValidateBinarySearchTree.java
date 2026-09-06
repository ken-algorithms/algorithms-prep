package com.motives.leetcode.groupb;

import com.motives.leetcode.common.TreeNode;

/**
 * LeetCode #98 - Validate Binary Search Tree (Nhom B, bai 28).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ValidateBinarySearchTree {

    public boolean isValidBST(TreeNode root) {
        return isWithinBounds(root, Long.MIN_VALUE, Long.MAX_VALUE);
    }

    private boolean isWithinBounds(TreeNode node, long lowerBound, long upperBound) {
        if (node == null) {
            return true;
        }
        if (node.val() <= lowerBound || node.val() >= upperBound) {
            return false;
        }
        return isWithinBounds(node.left(), lowerBound, node.val())
                && isWithinBounds(node.right(), node.val(), upperBound);
    }
}
