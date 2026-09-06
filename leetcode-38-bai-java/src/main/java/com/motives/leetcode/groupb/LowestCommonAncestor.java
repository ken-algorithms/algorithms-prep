package com.motives.leetcode.groupb;

import com.motives.leetcode.common.TreeNode;

/**
 * LeetCode #236 - Lowest Common Ancestor of a Binary Tree (Nhom B, bai 26).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class LowestCommonAncestor {

    public TreeNode lowestCommonAncestor(TreeNode root, TreeNode p, TreeNode q) {
        if (root == null || root == p || root == q) {
            return root;
        }

        TreeNode fromLeft = lowestCommonAncestor(root.left(), p, q);
        TreeNode fromRight = lowestCommonAncestor(root.right(), p, q);

        if (fromLeft != null && fromRight != null) {
            return root;
        }
        return fromLeft != null ? fromLeft : fromRight;
    }
}
