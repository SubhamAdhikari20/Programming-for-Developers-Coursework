package tetrisgame;

import java.awt.*;
import java.util.*;

public class TetrisBlock {
    public int[][] matrix;
    public int colorIndex;
    public int x, y;
    
    public TetrisBlock(int[][] matrix, int colorIndex) {
        this.matrix = matrix;
        this.colorIndex = colorIndex;
        x = Tetris.BOARD_WIDTH / 2 - matrix[0].length / 2;
        y = -matrix.length; // start above board
    }
    
    public static TetrisBlock random(int gridColumns) {
        int[][][] shapes = {
            {{1, 1}, {1, 1}},             // O
            {{1, 1, 1, 1}},               // I
            {{1, 1, 1}, {0, 1, 0}},       // T
            {{1, 1, 0}, {0, 1, 1}},       // S
            {{0, 1, 1}, {1, 1, 0}},       // Z
            {{1, 0}, {1, 0}, {1, 1}},     // L
            {{0, 1}, {0, 1}, {1, 1}}      // J
        };
        Random rand = new Random();
        return new TetrisBlock(
            shapes[rand.nextInt(shapes.length)],
            rand.nextInt(TetrisColors.COLORS.length - 1) + 1
        );
    }
    
    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }
    
    public void draw(Graphics2D g2, int cellSize) {
        g2.setColor(TetrisColors.COLORS[colorIndex]);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] != 0) {
                    int xPos = (x + j) * cellSize;
                    int yPos = (y + i) * cellSize;
                    g2.fillRoundRect(xPos, yPos, cellSize - 4, cellSize - 4, 8, 8);
                }
            }
        }
    }
    
    public void drawPreview(Graphics2D g2, int cx, int cy, int size) {
        g2.setColor(TetrisColors.COLORS[colorIndex]);
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] != 0) {
                    int xPos = cx - (matrix[i].length * size) / 2 + j * size;
                    int yPos = cy - (matrix.length * size) / 2 + i * size;
                    g2.fillRoundRect(xPos, yPos, size - 2, size - 2, 4, 4);
                }
            }
        }
    }
}