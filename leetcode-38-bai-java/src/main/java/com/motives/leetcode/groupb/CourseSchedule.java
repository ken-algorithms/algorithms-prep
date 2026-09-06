package com.motives.leetcode.groupb;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * LeetCode #207 - Course Schedule (Nhom B, bai 21).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class CourseSchedule {

    public boolean canFinish(int numCourses, int[][] prerequisites) {
        List<List<Integer>> dependentCourses = buildAdjacencyList(numCourses, prerequisites);
        int[] inDegree = computeInDegrees(numCourses, prerequisites);

        Deque<Integer> readyToTake = new ArrayDeque<>();
        for (int course = 0; course < numCourses; course++) {
            if (inDegree[course] == 0) {
                readyToTake.add(course);
            }
        }

        int coursesTaken = 0;
        while (!readyToTake.isEmpty()) {
            int course = readyToTake.poll();
            coursesTaken++;
            for (int dependent : dependentCourses.get(course)) {
                if (--inDegree[dependent] == 0) {
                    readyToTake.add(dependent);
                }
            }
        }
        return coursesTaken == numCourses;
    }

    private List<List<Integer>> buildAdjacencyList(int numCourses, int[][] prerequisites) {
        List<List<Integer>> adjacency = new ArrayList<>(numCourses);
        for (int i = 0; i < numCourses; i++) {
            adjacency.add(new ArrayList<>());
        }
        for (int[] prerequisite : prerequisites) {
            int course = prerequisite[0];
            int mustTakeFirst = prerequisite[1];
            adjacency.get(mustTakeFirst).add(course);
        }
        return adjacency;
    }

    private int[] computeInDegrees(int numCourses, int[][] prerequisites) {
        int[] inDegree = new int[numCourses];
        for (int[] prerequisite : prerequisites) {
            inDegree[prerequisite[0]]++;
        }
        return inDegree;
    }
}
