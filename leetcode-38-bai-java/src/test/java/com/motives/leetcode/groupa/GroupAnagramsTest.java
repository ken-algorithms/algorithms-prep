package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class GroupAnagramsTest {

    private final GroupAnagrams solution = new GroupAnagrams();

    @Test
    void groupsWordsThatAreAnagramsOfEachOther() {
        String[] input = {"eat", "tea", "tan", "ate", "nat", "bat"};

        List<List<String>> expected = List.of(
                List.of("eat", "tea", "ate"),
                List.of("tan", "nat"),
                List.of("bat"));

        assertEquals(expected, solution.groupAnagrams(input));
    }
}
