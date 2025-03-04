package tetrisgame;

import javax.swing.*;
import java.awt.*;
import java.util.Queue;

public class TetrisSidePanel extends JPanel {

    private TetrisPreview previewPanel;
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private ControlPanel controlPanel;
    

    public TetrisSidePanel(Queue<TetrisBlock> pieceQueue, int gridRows, int cellSize, TetrisGameArea gameArea) {
        setPreferredSize(new Dimension(240, gridRows * cellSize));
        setBackground(TetrisColors.COLORS[0]);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        scoreLabel = createStatusLabel("Score: 0", 24);
        add(scoreLabel);
        add(Box.createVerticalStrut(20));

        previewPanel = new TetrisPreview(pieceQueue, cellSize);
        add(previewPanel);
        add(Box.createVerticalStrut(20));

        levelLabel = createStatusLabel("Level: 1", 20);
        add(levelLabel);
        add(Box.createVerticalStrut(20));

        controlPanel = new ControlPanel(gameArea);
        add(controlPanel);
    }

    private JLabel createStatusLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, size));
        label.setForeground(Color.WHITE);
        label.setAlignmentX(CENTER_ALIGNMENT);
        return label;
    }

    public void updateStatus(int score, int level) {
        scoreLabel.setText(String.format("Score: %,d", score));
        levelLabel.setText("Level: " + level);
    }
    
    public void updateResetStatus(int score, int level, TetrisGameArea gameArea) {
        scoreLabel.setText(String.format("Score: %,d", score));
        levelLabel.setText("Level: " + level);
        gameArea.repaint();
        previewPanel.repaint();
    }

    public TetrisPreview getPreviewPanel() {
        return previewPanel;
    }
}

class TetrisPreview extends JPanel {
    private Queue<TetrisBlock> pieceQueue;
    private int cellSize;
    
    public TetrisPreview(Queue<TetrisBlock> pieceQueue, int cellSize) {
        setBackground(TetrisColors.COLORS[0].darker());
        setPreferredSize(new Dimension(240, 120));
        this.pieceQueue = pieceQueue;
        this.cellSize = cellSize;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!pieceQueue.isEmpty()) {
            pieceQueue.peek().drawPreview((Graphics2D) g, getWidth() / 2, getHeight() / 2, cellSize / 2);
        }
    }
}

class ControlPanel extends JPanel {
    private TetrisCustomButton leftButton;
    private TetrisCustomButton rightButton;
    private TetrisCustomButton downButton;
    private TetrisCustomButton rotateButton;
    
    private TetrisGameArea gameArea;
    
    public ControlPanel(TetrisGameArea gameArea) {
        this.gameArea = gameArea;
        setPreferredSize(new Dimension(240, 120));
        setBackground(TetrisColors.COLORS[0].darker());
        setLayout(new GridLayout(3, 3, 10, 10));
        
        rotateButton = new TetrisCustomButton("./images/rotate.png");
        leftButton = new TetrisCustomButton("./images/left.png");
        rightButton = new TetrisCustomButton("./images/right.png");
        downButton = new TetrisCustomButton("./images/down.png");
        
        // Both keyboard and mouse controls call processControl.
        rotateButton.addActionListener(e -> gameArea.processControl("rotate"));
        leftButton.addActionListener(e -> gameArea.processControl("left"));
        downButton.addActionListener(e -> gameArea.processControl("drop"));
        rightButton.addActionListener(e -> gameArea.processControl("right"));
        
        add(rotateButton);
        add(downButton);
        add(leftButton);
        add(rightButton);
    }
}