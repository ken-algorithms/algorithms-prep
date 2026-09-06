package com.motives.leetcode.groupa;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * LeetCode #20 - Valid Parentheses (Nhom A, bai 5).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class ValidParentheses {

    private static final Map<Character, Character> CLOSING_TO_OPENING =
            Map.of(')', '(', ']', '[', '}', '{');

    public boolean isValid(String s) {
        Deque<Character> stack = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            Character expectedOpening = CLOSING_TO_OPENING.get(c);
            if (expectedOpening == null) {
                stack.push(c);
            } else if (stack.isEmpty() || !stack.pop().equals(expectedOpening)) {
                return false;
            }
        }
        return stack.isEmpty();
    }
}
