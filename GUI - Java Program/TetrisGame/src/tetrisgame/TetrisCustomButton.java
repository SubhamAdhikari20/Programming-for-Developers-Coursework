package tetrisgame;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

public class TetrisCustomButton extends JButton {
    private Image image;
    
    public TetrisCustomButton(String imagePath) {
        super("");
        Image tempImage = new ImageIcon(getClass().getResource(imagePath)).getImage();
        image = tempImage.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setForeground(Color.WHITE);
        setFont(new Font("Segoe UI", Font.BOLD, 18));
        setBorderPainted(false);
        setIcon(new ImageIcon(image));
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int diameter = Math.min(getWidth(), getHeight());
        Ellipse2D circle = new Ellipse2D.Float(0, 0, diameter, diameter);
        g2.setClip(circle);
        GradientPaint gp = new GradientPaint(0, 0, new Color(60, 60, 60), 0, getHeight(), new Color(40, 40, 40));
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        if (image != null) {
            int imgWidth = image.getWidth(null);
            int imgHeight = image.getHeight(null);
            int x = (diameter - imgWidth) / 2;
            int y = (diameter - imgHeight) / 2;
            g2.drawImage(image, x, y, this);
        }
        g2.setClip(null);
        g2.setColor(new Color(80, 80, 80));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
        g2.dispose();
    }
    
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(60, 60);
    }
}
