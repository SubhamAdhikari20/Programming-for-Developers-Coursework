package tetrisgame;

import javax.swing.*;
import java.awt.*;
import java.util.*;


public class Tetris extends JFrame {
    // Game Constants
    public static final int BOARD_WIDTH = 10;
    public static final int BOARD_HEIGHT = 20;
    public static final int CELL_SIZE = 35;

    // Piece queue
    private Queue<TetrisBlock> pieceQueue = new LinkedList<>();

    private TetrisMainPanel mainPanel;

    public Tetris() {
        initUI();
    }

    private void initUI() {
        setTitle("Tetris Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        mainPanel = new TetrisMainPanel(BOARD_WIDTH, BOARD_HEIGHT, CELL_SIZE, pieceQueue);
        add(mainPanel);

        pack();
        setLocationRelativeTo(null);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new Tetris().setVisible(true));
    }
}

class TetrisMainPanel extends JPanel {
    private TetrisGameArea gameArea;
    private TetrisSidePanel sidePanel;

    public TetrisMainPanel(int gridColumns, int gridRows, int cellSize, Queue<TetrisBlock> pieceQueue) {
        setBackground(TetrisColors.COLORS[0]);
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        gameArea = new TetrisGameArea(gridColumns, gridRows, cellSize, pieceQueue);
        add(gameArea, BorderLayout.CENTER);

        sidePanel = new TetrisSidePanel(pieceQueue, gridRows, cellSize, gameArea);
        add(sidePanel, BorderLayout.EAST);

        // Let the game area update the side panel (score, level, preview)
        gameArea.setSidePanel(sidePanel);
    }
    
    public TetrisGameArea getGameArea() {
        return gameArea;
    }
}
