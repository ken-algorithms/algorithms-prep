package com.motives.leetcode.groupa;

import com.motives.leetcode.common.TreeNode;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.StringJoiner;

/**
 * LeetCode #297 - Serialize and Deserialize Binary Tree (Nhom A, bai 10).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class BinaryTreeCodec {

    private static final String NULL_MARKER = "#";
    private static final String DELIMITER = ",";

    public String serialize(TreeNode root) {
        StringJoiner joiner = new StringJoiner(DELIMITER);
        appendPreorder(root, joiner);
        return joiner.toString();
    }

    public TreeNode deserialize(String data) {
        Deque<String> tokens = new ArrayDeque<>(Arrays.asList(data.split(DELIMITER)));
        return buildFromPreorder(tokens);
    }

    private void appendPreorder(TreeNode node, StringJoiner joiner) {
        if (node == null) {
            joiner.add(NULL_MARKER);
            return;
        }
        joiner.add(String.valueOf(node.val()));
        appendPreorder(node.left(), joiner);
        appendPreorder(node.right(), joiner);
    }

    private TreeNode buildFromPreorder(Deque<String> tokens) {
        String token = tokens.poll();
        if (token == null || NULL_MARKER.equals(token)) {
            return null;
        }
        TreeNode left = buildFromPreorder(tokens);
        TreeNode right = buildFromPreorder(tokens);
        return new TreeNode(Integer.parseInt(token), left, right);
    }
}
