package com.motives.leetcode.groupc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IsomorphicStringsTest {

    private final IsomorphicStrings solution = new IsomorphicStrings();

    @Test
    void acceptsAConsistentOneToOneCharacterMapping() {
        assertTrue(solution.isIsomorphic("egg", "add"));
    }

    @Test
    void rejectsAMappingThatIsNotOneToOne() {
        assertFalse(solution.isIsomorphic("foo", "bar"));
    }
}
