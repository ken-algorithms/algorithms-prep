package com.motives.leetcode.groupa;

import com.motives.leetcode.common.TreeNode;
import java.util.HashMap;
import java.util.Map;

/**
 * LeetCode #105 - Construct Binary Tree from Preorder and Inorder Traversal (Nhom A, bai 16).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class BuildTreeFromPreorderInorder {

    public TreeNode buildTree(int[] preorder, int[] inorder) {
        return new TreeBuilder(preorder, inorder).build();
    }

    /** Encapsulates the recursion state so the recursive helper needs only two parameters. */
    private static final class TreeBuilder {
        private final int[] preorder;
        private final Map<Integer, Integer> inorderIndexByValue;
        private int preorderCursor;

        private TreeBuilder(int[] preorder, int[] inorder) {
            this.preorder = preorder;
            this.inorderIndexByValue = new HashMap<>();
            for (int i = 0; i < inorder.length; i++) {
                inorderIndexByValue.put(inorder[i], i);
            }
        }

        private TreeNode build() {
            return build(0, preorder.length - 1);
        }

        private TreeNode build(int inorderLeft, int inorderRight) {
            if (inorderLeft > inorderRight) {
                return null;
            }

            int rootValue = preorder[preorderCursor++];
            int rootInorderIndex = inorderIndexByValue.get(rootValue);

            TreeNode left = build(inorderLeft, rootInorderIndex - 1);
            TreeNode right = build(rootInorderIndex + 1, inorderRight);
            return new TreeNode(rootValue, left, right);
        }
    }
}
