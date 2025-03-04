package tetrisgame;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.util.*;

public class TetrisGameArea extends JPanel {
    private int gridColumns;
    private int gridRows;
    private int cellSize;

    // Board as a stack (top row at index 0)
    private Stack<int[]> boardStack;
    private Queue<TetrisBlock> pieceQueue;
    private TetrisBlock currentPiece;
    private int score = 0;
    private int level = 1;
    private boolean gameOver = false;
    
    private Timer gameTimer;
    
    // Game Over animation parameters
    private Timer gameOverAnimationTimer;
    private float gameOverAlpha = 1.0f; 
    private boolean alphaIncreasing = false;
    private double gameOverScale = 1.0; 
    private boolean scaleIncreasing = true;
    
    // Reference to side panel and preview panel
    private TetrisSidePanel sidePanel;
    private TetrisPreview previewPanel;
    
    public TetrisGameArea(int gridColumns, int gridRows, int cellSize, Queue<TetrisBlock> pieceQueue) {
        this.gridColumns = gridColumns;
        this.gridRows = gridRows;
        this.cellSize = cellSize;
        this.pieceQueue = pieceQueue;
        setBackground(TetrisColors.COLORS[0]);
        setPreferredSize(new Dimension(cellSize * gridColumns, cellSize * gridRows));
        
        initGame();
        setupKeyboardControls();
        startGame();
    }
    
    public void setSidePanel(TetrisSidePanel sp) {
        this.sidePanel = sp;
        this.previewPanel = sp.getPreviewPanel();
    }
    
    private void initGame() {
        boardStack = new Stack<>();
        for (int i = 0; i < gridRows; i++) {
            int[] row = new int[gridColumns];
            Arrays.fill(row, 0);
            boardStack.add(0, row);
        }
        pieceQueue.clear();
        // Enqueue three tetrominoes
        for (int i = 0; i < 3; i++) {
            pieceQueue.add(TetrisBlock.random(gridColumns));
        }
        currentPiece = pieceQueue.poll();
    }
    
    private void startGame() {
        gameTimer = new Timer(getFallSpeed(), e -> gameUpdate());
        gameTimer.start();
    }
    
    
    private int getFallSpeed() {
        return Math.max(50, 1000 - (level * 75));
    }
    
    private void setupKeyboardControls() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        
        im.put(KeyStroke.getKeyStroke("LEFT"), "left");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "right");
        im.put(KeyStroke.getKeyStroke("UP"), "rotate");
        im.put(KeyStroke.getKeyStroke("DOWN"), "drop");
        im.put(KeyStroke.getKeyStroke("R"), "restart");
        
        am.put("left", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { processControl("left"); }
        });
        am.put("right", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { processControl("right"); }
        });
        am.put("rotate", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { processControl("rotate"); }
        });
        am.put("drop", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { processControl("drop"); }
        });
        am.put("restart", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (gameOver) resetGame();
            }
        });
    }
    
    // Unified control processing (used by both keyboard and button actions)
    public void processControl(String cmd) {
        if (gameOver) return;
        switch (cmd) {
            case "left":
                tryMove(currentPiece, -1, 0);
                break;
            case "right":
                tryMove(currentPiece, 1, 0);
                break;
            case "rotate":
                rotatePiece();
                break;
            case "drop":
                while (tryMove(currentPiece, 0, 1)) { }
                break;
        }
        repaint();
    }
    
    private void rotatePiece() {
        int rows = currentPiece.matrix.length;
        int cols = currentPiece.matrix[0].length;
        int[][] rotated = new int[cols][rows];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                rotated[x][rows - 1 - y] = currentPiece.matrix[y][x];
            }
        }
        TetrisBlock testPiece = new TetrisBlock(rotated, currentPiece.colorIndex);
        testPiece.x = currentPiece.x;
        testPiece.y = currentPiece.y;
        if (!collision(testPiece, 0, 0)) {
            currentPiece.matrix = rotated;
        }
    }
    
    // ----------------------------
    //    Main Game Loop
    // ----------------------------
    private void gameUpdate() {
        if (!tryMove(currentPiece, 0, 1)) {
            lockPiece();
            clearCompletedRows();
            spawnPiece();
        }
        repaint();
        previewPanel.repaint();
//        if (previewPanel != null) previewPanel.repaint();
    }
    
    private void lockPiece() {
        for (int y = 0; y < currentPiece.matrix.length; y++) {
            for (int x = 0; x < currentPiece.matrix[y].length; x++) {
                if (currentPiece.matrix[y][x] != 0) {
                    int boardX = currentPiece.x + x;
                    int boardY = currentPiece.y + y;
                    if (boardY >= 0 && boardY < gridRows && boardX >= 0 && boardX < gridColumns) {
                        boardStack.get(boardY)[boardX] = currentPiece.colorIndex;
                    }
                }
            }
        }
    }
    
    private void clearCompletedRows() {
        int linesCleared = 0;
        for (int i = gridRows - 1; i >= 0; i--) {
            boolean full = true;
            for (int cell : boardStack.get(i)) {
                if (cell == 0) { full = false; break; }
            }
            if (full) {
                boardStack.remove(i);
                int[] emptyRow = new int[gridColumns];
                Arrays.fill(emptyRow, 0);
                boardStack.add(0, emptyRow);
                linesCleared++;
                i++; // recheck same index after shift
            }
        }
        if (linesCleared > 0) {
            score += linesCleared * 100 * level;
            level = 1 + score / 1000;
            gameTimer.setDelay(getFallSpeed());
            if (sidePanel != null) {
                sidePanel.updateStatus(score, level);
            }
        }
    }
    
    private void spawnPiece() {
        currentPiece = pieceQueue.poll();
        pieceQueue.add(TetrisBlock.random(gridColumns));
        if (previewPanel != null) previewPanel.repaint();
        if (collision(currentPiece, 0, 0) || isTopRowFilled()) {
            triggerGameOver();
        }
    }
    
//    private boolean isRowFull(int[] row) {
//        for (int cell : row) {
//            if (cell == 0) return false;
//        }
//        return true;
//    }
    
     private boolean isTopRowFilled() {
        int[] topRow = boardStack.get(0);
        for (int cell : topRow) {
            if (cell != 0) return true;
        }
        return false;
    }
    
     
     // ----------------------------
    //    Collision & Movement
    // ----------------------------
    private boolean tryMove(TetrisBlock piece, int dx, int dy) {
        if (!collision(piece, dx, dy)) {
            piece.move(dx, dy);
            return true;
        }
        return false;
    }
    
    private boolean collision(TetrisBlock piece, int dx, int dy) {
        for (int y = 0; y < piece.matrix.length; y++) {
            for (int x = 0; x < piece.matrix[y].length; x++) {
                if (piece.matrix[y][x] != 0) {
                    int newX = piece.x + x + dx;
                    int newY = piece.y + y + dy;
                    if (newX < 0 || newX >= gridColumns || newY >= gridRows) return true;
                    if (newY >= 0 && boardStack.get(newY)[newX] != 0) return true;
                }
            }
        }
        return false;
    }
    
    // ----------------------------
    //   Game Over & Animation
    // ----------------------------
    private void triggerGameOver() {
        gameTimer.stop();
        gameOver = true;
        startGameOverAnimation();
    }
    
    private void startGameOverAnimation() {
        gameOverAlpha = 1.0f;
        alphaIncreasing = false;
        gameOverScale = 1.0;
        scaleIncreasing = true;
        gameOverAnimationTimer = new Timer(50, e -> updateGameOverAnimation());
        gameOverAnimationTimer.start();
    }
    
    private void updateGameOverAnimation() {
        // Oscillate alpha between 0.5 and 1.0
        if (alphaIncreasing) {
            gameOverAlpha += 0.05f;
            if (gameOverAlpha >= 1.0f) {
                gameOverAlpha = 1.0f;
                alphaIncreasing = false;
            }
        } else {
            gameOverAlpha -= 0.05f;
            if (gameOverAlpha <= 0.5f) {
                gameOverAlpha = 0.5f;
                alphaIncreasing = true;
            }
        }
        // Oscillate scale between 1.0 and 1.2
        if (scaleIncreasing) {
            gameOverScale += 0.01;
            if (gameOverScale >= 1.2) {
                gameOverScale = 1.2;
                scaleIncreasing = false;
            }
        } else {
            gameOverScale -= 0.01;
            if (gameOverScale <= 1.0) {
                gameOverScale = 1.0;
                scaleIncreasing = true;
            }
        }
        this.repaint();
    }
    
    private void resetGame() {
        gameOver = false;
        score = 0;
        level = 1;
        initGame();
        gameTimer.setDelay(getFallSpeed());
        gameTimer.start();
        if (sidePanel != null) sidePanel.updateResetStatus(score, level, this);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBoard(g2);
        drawCurrentPiece(g2);
        if (gameOver) drawGameOver(g2);
    }
    
    private void drawBoard(Graphics2D g2) {
        for (int y = 0; y < boardStack.size(); y++) {
            int[] row = boardStack.get(y);
            for (int x = 0; x < gridColumns; x++) {
                drawCell(g2, x, y, row[x]);
                g2.setColor(Color.gray);
                g2.drawRect(x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }
    }
    
    private void drawCurrentPiece(Graphics2D g2) {
        currentPiece.draw(g2, cellSize);
    }
    
    private void drawCell(Graphics2D g2, int x, int y, int colorIdx) {
        if (colorIdx == 0) return;
        int xPos = x * cellSize;
        int yPos = y * cellSize;
        Color color = TetrisColors.COLORS[colorIdx];
        // Draw cell shadow.
        g2.setColor(color.darker().darker());
        g2.fillRoundRect(xPos + 2, yPos + 2, cellSize - 4, cellSize - 4, 8, 8);
        // Draw cell body.
        g2.setColor(color);
        g2.fillRoundRect(xPos, yPos, cellSize - 4, cellSize - 4, 8, 8);
        // Draw highlight.
        g2.setColor(color.brighter());
        g2.drawLine(xPos + 2, yPos + 2, xPos + cellSize - 6, yPos + 2);
        g2.drawLine(xPos + 2, yPos + 2, xPos + 2, yPos + cellSize - 6);
    }
    
    private void drawGameOver(Graphics2D g2) {
        // Draw semi-transparent overlay.
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, getWidth(), getHeight());
        String text = "GAME OVER";
        Font baseFont = new Font("Monospaced", Font.BOLD, 75);
        AffineTransform old = g2.getTransform();
        AffineTransform at = new AffineTransform();
        at.translate(getWidth() / 1.3, getHeight() / 1.5);
        at.scale(gameOverScale, gameOverScale);
        g2.setTransform(at);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, gameOverAlpha));
        FontRenderContext frc = g2.getFontRenderContext();
        TextLayout layout = new TextLayout(text, baseFont, frc);
        Shape textShape = layout.getOutline(null);
        Rectangle2D textBounds = textShape.getBounds2D();
        double textWidth = textBounds.getWidth();
        double textHeight = textBounds.getHeight();
        AffineTransform centerText = AffineTransform.getTranslateInstance(-textWidth / 2.0, textHeight / 2.0);
        textShape = centerText.createTransformedShape(textShape);
        float topY = (float) textBounds.getY();
        float bottomY = (float) (textBounds.getY() + textBounds.getHeight());
        GradientPaint textGradient = new GradientPaint(
            0, topY,
            new Color(255, 165, 0),
            0, bottomY,
            new Color(160, 50, 200)
        );
        g2.setPaint(textGradient);
        g2.fill(textShape);
        g2.setStroke(new BasicStroke(2.0f));
        g2.setColor(Color.black);
        g2.draw(textShape);
        g2.setTransform(old);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        String restartText = "Press R to restart";
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(restartText);
        int x = (getWidth() - textW) / 2;
        int y = (int) (getHeight() / 2.0 + 80);
        g2.setColor(Color.WHITE);
        g2.drawString(restartText, x, y);
    }
}
