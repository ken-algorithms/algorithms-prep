package com.motives.leetcode.common;

/**
 * Immutable binary tree node shared by every tree-based problem.
 * A record fits naturally here: every algorithm in this project either
 * reads an existing tree or builds one bottom-up (children before parent),
 * so no in-place mutation of left/right is ever required.
 */
public record TreeNode(int val, TreeNode left, TreeNode right) {

    public TreeNode(int val) {
        this(val, null, null);
    }
}
