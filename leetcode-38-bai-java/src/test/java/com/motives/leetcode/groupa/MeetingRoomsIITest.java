package com.motives.leetcode.groupa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MeetingRoomsIITest {

    private final MeetingRoomsII solution = new MeetingRoomsII();

    @Test
    void countsTheMinimumRoomsNeededForOverlappingMeetings() {
        int[][] intervals = {{0, 30}, {5, 10}, {15, 20}};

        assertEquals(2, solution.minMeetingRooms(intervals));
    }
}
