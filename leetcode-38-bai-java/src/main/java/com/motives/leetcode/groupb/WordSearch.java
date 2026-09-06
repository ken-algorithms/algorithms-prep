package com.motives.leetcode.groupb;

/**
 * LeetCode #79 - Word Search (Nhom B, bai 27).
 * Mo ta huong giai bang loi: xem leetcode-38-bai-phong-van-vietnam.md
 */
public class WordSearch {

    private static final char VISITED_MARKER = '#';
    private static final int[][] DIRECTIONS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public boolean exist(char[][] board, String word) {
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                if (searchFrom(board, word, row, col, 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean searchFrom(char[][] board, String word, int row, int col, int wordIndex) {
        if (wordIndex == word.length()) {
            return true;
        }
        if (!isMatchingCell(board, word, row, col, wordIndex)) {
            return false;
        }

        char original = board[row][col];
        board[row][col] = VISITED_MARKER;

        boolean found = false;
        for (int[] direction : DIRECTIONS) {
            if (searchFrom(board, word, row + direction[0], col + direction[1], wordIndex + 1)) {
                found = true;
                break;
            }
        }

        board[row][col] = original;
        return found;
    }

    private boolean isMatchingCell(char[][] board, String word, int row, int col, int wordIndex) {
        return row >= 0 && row < board.length
                && col >= 0 && col < board[row].length
                && board[row][col] == word.charAt(wordIndex);
    }
}
