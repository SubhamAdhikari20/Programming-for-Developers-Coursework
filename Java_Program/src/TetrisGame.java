import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.*;
import java.util.*;

public class TetrisGame extends JFrame {
    // Game Constants
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int CELL_SIZE = 35;
    private static final Color[] COLORS = {
            new Color(30, 30, 30),     // Background
            new Color(255, 89, 94),    // Red
            new Color(255, 202, 58),   // Yellow
            new Color(138, 201, 38),   // Green
            new Color(25, 130, 196),   // Blue
            new Color(106, 76, 147)    // Purple
    };

    // Game State
    private Stack<int[]> boardStack;          // Stack representing the board (top row = index 0)
    private Queue<Tetromino> pieceQueue = new LinkedList<>();
    private Tetromino currentPiece;
    private int score = 0;
    private int level = 1;
    private boolean gameOver = false;

    // Animation parameters for Game Over
    private Timer gameOverAnimationTimer;
    private float gameOverAlpha = 1.0f;      // Alpha value for fade (0.0 to 1.0)
    private boolean alphaIncreasing = false; // Oscillate alpha
    private double gameOverScale = 1.0;      // Scale factor for pulsating effect
    private boolean scaleIncreasing = true;

    // GUI Components
    private GameCanvas gameCanvas;
    private JPanel previewPanel;
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private Timer gameTimer;

    public TetrisGame() {
        initUI();
        initGame();
        setupControls();
        setupKeyboardControls();
        startGame();
    }

    // ----------------------------
    //        UI Initialization
    // ----------------------------
    private void initUI() {
        setTitle("Modern Tetris");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // Main Container
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(COLORS[0]);

        // Game Board Canvas
        gameCanvas = new GameCanvas();
        mainPanel.add(gameCanvas, BorderLayout.CENTER);

        // Side Panel
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(COLORS[0]);

        // Score and Level
        scoreLabel = createStatusLabel("Score: 0", 24);
        levelLabel = createStatusLabel("Level: 1", 20);

        // Next Piece Preview
        previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (!pieceQueue.isEmpty()) {
                    pieceQueue.peek().drawPreview((Graphics2D) g,
                            getWidth() / 2, getHeight() / 2, CELL_SIZE / 2);
                }
            }
        };
        previewPanel.setPreferredSize(new Dimension(120, 120));
        previewPanel.setBackground(COLORS[0].darker());

        // Control Buttons
        JPanel controlPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        String[] controls = {"◀", "▼", "↻", "▶"};
        for (String cmd : controls) {
            GradientButton btn = new GradientButton(cmd);
            btn.addActionListener(this::handleControl);
            controlPanel.add(btn);
        }

        // Assemble Side Panel
        sidePanel.add(scoreLabel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(previewPanel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(levelLabel);
        sidePanel.add(Box.createVerticalStrut(20));
        sidePanel.add(controlPanel);

        mainPanel.add(sidePanel, BorderLayout.EAST);
        add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private JLabel createStatusLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, size));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(CENTER_ALIGNMENT);
        return label;
    }

    // ----------------------------
    //    Game Initialization
    // ----------------------------
    private void initGame() {
        boardStack = new Stack<>();
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            int[] row = new int[BOARD_WIDTH];
            Arrays.fill(row, 0);
            boardStack.add(0, row); // index 0 = top
        }

        // Initialize piece queue
        pieceQueue.clear();
        for (int i = 0; i < 3; i++) {
            pieceQueue.add(Tetromino.random());
        }
        currentPiece = pieceQueue.poll();
    }

    private void startGame() {
        gameTimer = new Timer(getFallSpeed(), e -> gameUpdate());
        gameTimer.start();
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
        gameCanvas.repaint();
        previewPanel.repaint();
    }

    // Merge current piece into board
    private void lockPiece() {
        for (int y = 0; y < currentPiece.matrix.length; y++) {
            for (int x = 0; x < currentPiece.matrix[y].length; x++) {
                if (currentPiece.matrix[y][x] != 0) {
                    int boardX = currentPiece.x + x;
                    int boardY = currentPiece.y + y;
                    if (boardY >= 0 && boardY < BOARD_HEIGHT &&
                            boardX >= 0 && boardX < BOARD_WIDTH) {
                        boardStack.get(boardY)[boardX] = currentPiece.colorIndex;
                    }
                }
            }
        }
    }

    private void clearCompletedRows() {
        int linesCleared = 0;
        for (int i = BOARD_HEIGHT - 1; i >= 0; i--) {
            if (isRowFull(boardStack.get(i))) {
                boardStack.remove(i);
                int[] emptyRow = new int[BOARD_WIDTH];
                Arrays.fill(emptyRow, 0);
                boardStack.add(0, emptyRow);
                linesCleared++;
                i++;
            }
        }
        if (linesCleared > 0) {
            score += linesCleared * 100 * level;
            level = 1 + score / 1000;
            scoreLabel.setText(String.format("Score: %,d", score));
            levelLabel.setText("Level: " + level);
            gameTimer.setDelay(getFallSpeed());
        }
    }

    private boolean isRowFull(int[] row) {
        for (int cell : row) {
            if (cell == 0) return false;
        }
        return true;
    }

    private void spawnPiece() {
        currentPiece = pieceQueue.poll();
        pieceQueue.add(Tetromino.random());

        // Check game over
        if (collision(currentPiece, 0, 0) || isTopRowFilled()) {
            triggerGameOver();
        }
    }

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
    private boolean tryMove(Tetromino piece, int dx, int dy) {
        if (!collision(piece, dx, dy)) {
            piece.move(dx, dy);
            return true;
        }
        return false;
    }

    private boolean collision(Tetromino piece, int dx, int dy) {
        for (int y = 0; y < piece.matrix.length; y++) {
            for (int x = 0; x < piece.matrix[y].length; x++) {
                if (piece.matrix[y][x] != 0) {
                    int nextX = piece.x + x + dx;
                    int nextY = piece.y + y + dy;
                    if (nextX < 0 || nextX >= BOARD_WIDTH ||
                            nextY >= BOARD_HEIGHT) {
                        return true;
                    }
                    if (nextY >= 0 && boardStack.get(nextY)[nextX] != 0) {
                        return true;
                    }
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
        // Reset animation parameters
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
        gameCanvas.repaint();
    }

    private void resetGame() {
        gameOver = false;
        score = 0;
        level = 1;
        initGame();
        gameTimer.setDelay(getFallSpeed());
        gameTimer.start();
        updateUI();
    }

    // ----------------------------
    //    Utility & Controls
    // ----------------------------
    private void updateUI() {
        scoreLabel.setText(String.format("Score: %,d", score));
        levelLabel.setText("Level: " + level);
        gameCanvas.repaint();
        previewPanel.repaint();
    }

    private void setupKeyboardControls() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

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


    private void setupControls() {
        // Keyboard controls
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // Add a key for 'R' to restart after game over
        im.put(KeyStroke.getKeyStroke("released R"), "restart");
        am.put("restart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameOver) {
                    if (gameOverAnimationTimer != null) {
                        gameOverAnimationTimer.stop();
                    }
                    resetGame();
                }
            }
        });

        String[] keys = {"LEFT", "RIGHT", "UP", "DOWN"};
        String[] cmds = {"left", "right", "rotate", "drop"};
        for (int i = 0; i < keys.length; i++) {
            im.put(KeyStroke.getKeyStroke(keys[i]), cmds[i]);
            am.put(cmds[i], new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (gameOver) return;
                    switch (e.getActionCommand()) {
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
                    gameCanvas.repaint();
                    previewPanel.repaint();
                }
            });
        }
    }

    // Handle the control button clicks (left, down, rotate, right)
    private void handleControl(ActionEvent e) {
        if (gameOver) return;
        String cmd = ((JButton) e.getSource()).getText();
        switch (cmd) {
            case "◀":
                tryMove(currentPiece, -1, 0);
                break;
            case "▼":
                while (tryMove(currentPiece, 0, 1)) { }
                break;
            case "↻":
                rotatePiece();
                break;
            case "▶":
                tryMove(currentPiece, 1, 0);
                break;
        }
        gameCanvas.repaint();
        previewPanel.repaint();
    }

    // Rotate the current piece if possible
    private void rotatePiece() {
        int[][] rotated = new int[currentPiece.matrix[0].length][currentPiece.matrix.length];
        for (int y = 0; y < currentPiece.matrix.length; y++) {
            for (int x = 0; x < currentPiece.matrix[y].length; x++) {
                rotated[x][currentPiece.matrix.length - 1 - y] = currentPiece.matrix[y][x];
            }
        }
        Tetromino test = new Tetromino(rotated, currentPiece.colorIndex);
        test.x = currentPiece.x;
        test.y = currentPiece.y;
        if (!collision(test, 0, 0)) {
            currentPiece.matrix = rotated;
        }
    }

    private int getFallSpeed() {
        return Math.max(50, 1000 - (level * 75));
    }

    // ----------------------------
    //  Inner Class: Game Canvas
    // ----------------------------
    class GameCanvas extends JPanel {
        public GameCanvas() {
            setPreferredSize(new Dimension(
                    BOARD_WIDTH * CELL_SIZE,
                    BOARD_HEIGHT * CELL_SIZE
            ));
            setBackground(COLORS[0]);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            drawBoard(g2);
            drawCurrentPiece(g2);
            if (gameOver) {
                drawGameOver(g2);
            }
        }

        private void drawBoard(Graphics2D g2) {
            for (int y = 0; y < boardStack.size(); y++) {
                int[] row = boardStack.get(y);
                for (int x = 0; x < BOARD_WIDTH; x++) {
                    drawCell(g2, x, y, row[x]);
                    g2.setColor(Color.gray);
                    g2.drawRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        private void drawCurrentPiece(Graphics2D g2) {
            currentPiece.draw(g2, CELL_SIZE);
        }

        private void drawCell(Graphics2D g2, int x, int y, int colorIdx) {
            if (colorIdx == 0) return;
            int xPos = x * CELL_SIZE;
            int yPos = y * CELL_SIZE;
            Color color = COLORS[colorIdx];

            // Cell shadow
            g2.setColor(color.darker().darker());
            g2.fillRoundRect(xPos + 2, yPos + 2, CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);

            // Cell body
            g2.setColor(color);
            g2.fillRoundRect(xPos, yPos, CELL_SIZE - 4, CELL_SIZE - 4, 8, 8);

            // Highlight
            g2.setColor(color.brighter());
            g2.drawLine(xPos + 2, yPos + 2, xPos + CELL_SIZE - 6, yPos + 2);
            g2.drawLine(xPos + 2, yPos + 2, xPos + 2, yPos + CELL_SIZE - 6);
        }

        // Animated game-over overlay with pixel-style gradient text
        private void drawGameOver(Graphics2D g2) {
            // Semi-transparent black overlay
            g2.setColor(new Color(0, 0, 0, 200));
            g2.fillRect(0, 0, getWidth(), getHeight());

            // We’ll use a pixel-like gradient for the text:
            // top: bright orange, bottom: purple
            // We'll do an outline approach so we can fill with a gradient
            String text = "GAME OVER";
            Font baseFont = new Font("Monospaced", Font.BOLD, 75);

            // Save old transform
            AffineTransform old = g2.getTransform();

            // Center transform + scale
            AffineTransform at = new AffineTransform();
            at.translate(getWidth() / 1.3, getHeight() / 1.5);
            at.scale(gameOverScale, gameOverScale);
            g2.setTransform(at);

            // Apply alpha composite
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, gameOverAlpha));

            // Create a TextLayout so we can get an outline
            FontRenderContext frc = g2.getFontRenderContext();
            TextLayout layout = new TextLayout(text, baseFont, frc);
            Shape textShape = layout.getOutline(null);

            // Calculate the text bounds for centering
            Rectangle2D textBounds = textShape.getBounds2D();
            double textWidth = textBounds.getWidth();
            double textHeight = textBounds.getHeight();

            // Shift the text shape so that its center is at (0,0)
            AffineTransform centerText = AffineTransform.getTranslateInstance(
                    -textWidth / 2.0,
                    textHeight / 2.0
            );
            textShape = centerText.createTransformedShape(textShape);

            // Create a vertical gradient paint for the text
            float topY = (float) (textBounds.getY());
            float bottomY = (float) (textBounds.getY() + textBounds.getHeight());
            GradientPaint textGradient = new GradientPaint(
                    0, topY,
                    new Color(255, 165, 0),  // bright orange
                    0, bottomY,
                    new Color(160, 50, 200)  // purple
            );

            g2.setPaint(textGradient);
            g2.fill(textShape);

            // Optional stroke around the text (pixel-like effect)
            g2.setStroke(new BasicStroke(2.0f));
            g2.setColor(Color.black);
            g2.draw(textShape);

            // Reset transform and alpha
            g2.setTransform(old);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            // Draw a smaller line: "Press R to restart"
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

    // ----------------------------
    //   Tetromino & Main
    // ----------------------------
    static class Tetromino {
        int[][] matrix;
        int colorIndex;
        int x, y;

        Tetromino(int[][] matrix, int colorIndex) {
            this.matrix = matrix;
            this.colorIndex = colorIndex;
            x = BOARD_WIDTH / 2 - matrix[0].length / 2;
            y = -matrix.length; // Start above the board
        }

        static Tetromino random() {
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
            return new Tetromino(
                    shapes[rand.nextInt(shapes.length)],
                    rand.nextInt(COLORS.length - 1) + 1
            );
        }

        void move(int dx, int dy) {
            x += dx;
            y += dy;
        }

        void draw(Graphics2D g2, int cellSize) {
            g2.setColor(COLORS[colorIndex]);
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

        void drawPreview(Graphics2D g2, int cx, int cy, int size) {
            g2.setColor(COLORS[colorIndex]);
            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0; j < matrix[i].length; j++) {
                    if (matrix[i][j] != 0) {
                        int x = cx - (matrix[i].length * size) / 2 + j * size;
                        int y = cy - (matrix.length * size) / 2 + i * size;
                        g2.fillRoundRect(x, y, size - 2, size - 2, 4, 4);
                    }
                }
            }
        }
    }

    static class GradientButton extends JButton {
        GradientButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 18));
            setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            // Gradient background
            Color start = new Color(60, 60, 60);
            Color end = new Color(40, 40, 40);
            GradientPaint gp = new GradientPaint(
                    0, 0, start, 0, getHeight(), end
            );
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

            // Button text
            FontMetrics fm = g2.getFontMetrics();
            Rectangle2D bounds = fm.getStringBounds(getText(), g2);
            int x = (int) ((getWidth() - bounds.getWidth()) / 2);
            int y = (int) ((getHeight() - bounds.getHeight()) / 2 + fm.getAscent());
            g2.drawString(getText(), x, y);

            // Border
            g2.setColor(new Color(80, 80, 80));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> new TetrisGame().setVisible(true));
    }
}
