package com.github.mpetkov.hiddengemsdeluxe.model;
import com.github.mpetkov.hiddengemsdeluxe.model.GridManager;


import java.util.Arrays;

public class GridManager {
    private int[][] grid;
    private final int rows;
    private final int cols;

    public GridManager(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new int[rows][cols];
        for (int[] row : grid) Arrays.fill(row, -1);
    }

    public int getGridCell(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            return -2;
        }
        return grid[row][col];
    }

    public void setGridCell(int row, int col, int value) {
        if (row >= 0 && row < rows && col >= 0 && col < cols) {
            grid[row][col] = value;
        }
    }

    public int[][] getGrid() {
        return grid;
    }

    public boolean[][] findMatches() {
        boolean[][] toRemove = new boolean[rows][cols];
        boolean anyMatches = false;

        // Хоризонтално
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col <= cols - 3; col++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row][col + 1] &&
                    color == grid[row][col + 2]) {
                    toRemove[row][col] = true;
                    toRemove[row][col + 1] = true;
                    toRemove[row][col + 2] = true;
                    anyMatches = true;
                }
            }
        }

        // Вертикално
        for (int col = 0; col < cols; col++) {
            for (int row = 0; row <= rows - 3; row++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row + 1][col] &&
                    color == grid[row + 2][col]) {
                    toRemove[row][col] = true;
                    toRemove[row + 1][col] = true;
                    toRemove[row + 2][col] = true;
                    anyMatches = true;
                }
            }
        }

        // Диагонали ↘
        for (int row = 0; row <= rows - 3; row++) {
            for (int col = 0; col <= cols - 3; col++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row + 1][col + 1] &&
                    color == grid[row + 2][col + 2]) {
                    toRemove[row][col] = true;
                    toRemove[row + 1][col + 1] = true;
                    toRemove[row + 2][col + 2] = true;
                    anyMatches = true;
                }
            }
        }

        // Диагонали ↙
        for (int row = 0; row <= rows - 3; row++) {
            for (int col = 2; col < cols; col++) {
                int color = grid[row][col];
                if (color != -1 &&
                    color == grid[row + 1][col - 1] &&
                    color == grid[row + 2][col - 2]) {
                    toRemove[row][col] = true;
                    toRemove[row + 1][col - 1] = true;
                    toRemove[row + 2][col - 2] = true;
                    anyMatches = true;
                }
            }
        }
        return toRemove;
    }

    public void applyGravity() {
        for (int col = 0; col < cols; col++) {
            for (int row = 1; row < rows; row++) {
                if (grid[row][col] != -1 && grid[row - 1][col] == -1) {
                    int currentRow = row;
                    while (currentRow > 0 && grid[currentRow - 1][col] == -1) {
                        grid[currentRow - 1][col] = grid[currentRow][col];
                        grid[currentRow][col] = -1;
                        currentRow--;
                    }
                }
            }
        }
    }
}
