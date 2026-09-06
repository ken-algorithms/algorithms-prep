package com.motives.leetcode.groupb;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CourseScheduleTest {

    private final CourseSchedule solution = new CourseSchedule();

    @Test
    void allowsFinishingWhenPrerequisitesFormNoCycle() {
        assertTrue(solution.canFinish(2, new int[][] {{1, 0}}));
    }

    @Test
    void rejectsACircularPrerequisiteChain() {
        assertFalse(solution.canFinish(2, new int[][] {{1, 0}, {0, 1}}));
    }
}
