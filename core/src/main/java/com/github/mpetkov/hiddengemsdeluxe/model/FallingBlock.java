package com.github.mpetkov.hiddengemsdeluxe.model;

import com.github.mpetkov.hiddengemsdeluxe.util.GameConstants;

import java.util.Random;

public class FallingBlock {
    private int fallingRow;
    private int fallingCol;
    private int[] fallingColors;
    private int[] nextColors;
    private Random random;
    private GridManager gridManager;

    public FallingBlock(Random random, GridManager gridManager, int[] initialNextColors) {
        this.random = random;
        this.gridManager = gridManager;
        this.fallingColors = new int[3];
        this.nextColors = initialNextColors;
        generateNewBlock(random);
    }

    public int getFallingRow() {
        return fallingRow;
    }

    public int getFallingCol() {
        return fallingCol;
    }

    public int[] getFallingColors() {
        return fallingColors;
    }

    public int[] getNextColors() {
        return nextColors;
    }

    public void generateNewBlock(Random random) {
        System.arraycopy(nextColors, 0, fallingColors, 0, 3);
        fallingRow = GameConstants.ROWS - 1;
        fallingCol = 2 + random.nextInt(2);
        for (int i = 0; i < 3; i++) {
            nextColors[i] = random.nextInt(4);
        }
    }

    public boolean canRise() {
        return fallingRow >= 3 &&
            gridManager.getGridCell(fallingRow - 1, fallingCol) == -1 &&
            gridManager.getGridCell(fallingRow - 2, fallingCol) == -1 &&
            gridManager.getGridCell(fallingRow - 3, fallingCol) == -1;
    }

    public boolean canMove(int dir) {
        int newCol = fallingCol + dir;
        if (newCol < 0 || newCol >= GameConstants.COLS) return false;
        for (int i = 0; i < 3; i++) {
            int row = fallingRow - i;
            if (row >= 0 && gridManager.getGridCell(row, newCol) != -1) return false;
        }
        return true;
    }

    public void moveDown() {
        fallingRow--;
    }

    public void moveHorizontal(int dir) {
        fallingCol += dir;
    }

    public void rotateBlock() {
        int top = fallingColors[0];
        fallingColors[0] = fallingColors[1];
        fallingColors[1] = fallingColors[2];
        fallingColors[2] = top;
    }
}
