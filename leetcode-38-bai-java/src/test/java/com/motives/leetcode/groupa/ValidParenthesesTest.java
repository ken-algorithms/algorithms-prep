package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ValidParenthesesTest {

    private final ValidParentheses solution = new ValidParentheses();

    @Test
    void acceptsProperlyNestedAndOrderedBrackets() {
        assertTrue(solution.isValid("()[]{}"));
    }

    @Test
    void rejectsMismatchedBrackets() {
        assertFalse(solution.isValid("(]"));
    }
}
