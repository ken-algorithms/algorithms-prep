package com.motives.leetcode.groupa;

/**
 * LeetCode #200 - Number of Islands (Nhom A, bai 6).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class NumberOfIslands {

    private static final char LAND = '1';
    private static final char WATER = '0';
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public int numIslands(char[][] grid) {
        int islandCount = 0;
        for (int row = 0; row < grid.length; row++) {
            for (int col = 0; col < grid[row].length; col++) {
                if (grid[row][col] == LAND) {
                    islandCount++;
                    sinkIsland(grid, row, col);
                }
            }
        }
        return islandCount;
    }

    private void sinkIsland(char[][] grid, int row, int col) {
        if (!isLand(grid, row, col)) {
            return;
        }
        grid[row][col] = WATER;
        for (int[] direction : DIRECTIONS) {
            sinkIsland(grid, row + direction[0], col + direction[1]);
        }
    }

    private boolean isLand(char[][] grid, int row, int col) {
        return row >= 0 && row < grid.length
                && col >= 0 && col < grid[row].length
                && grid[row][col] == LAND;
    }
}
