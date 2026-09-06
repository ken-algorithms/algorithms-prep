package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class WordBreakTest {

    private final WordBreak solution = new WordBreak();

    @Test
    void confirmsTheStringCanBeSegmentedUsingDictionaryWords() {
        assertTrue(solution.wordBreak("leetcode", List.of("leet", "code")));
    }
}
