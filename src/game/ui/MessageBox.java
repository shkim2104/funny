package game.ui;

import javax.swing.*;
import java.awt.*;

/**
 * The battle's message window, as a scrap of ruled notebook paper framed in pencil. Used as a container
 * (the battle log) or, via setText, as the "무엇을 할까?" prompt that wraps its text to fit.
 */
public class MessageBox extends JPanel {
    private static final Color RULE = new Color(110, 150, 205, 70);
    private String text = "";
    private final long seed = System.identityHashCode(this);

    public MessageBox(LayoutManager layout) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Sketch.smooth(g);
        int w = getWidth(), h = getHeight();
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRect(8, 8, w - 10, h - 10);
        g2.setColor(Sketch.PAPER);
        g2.fillRect(4, 4, w - 10, h - 10);
        g2.setColor(RULE);
        g2.setStroke(new BasicStroke(1.2f));
        for (int y = 34; y < h - 8; y += 24) g2.drawLine(6, y, w - 8, y);
        g2.setColor(new Color(214, 92, 92, 80));
        g2.drawLine(14, 6, 14, h - 8);
        Sketch.box(g2, 4, 4, w - 10, h - 10, seed, 2.2f, Sketch.INK);

        if (!text.isEmpty()) {
            g2.setFont(Sketch.hand(Font.BOLD, 16f));
            g2.setColor(Sketch.INK);
            FontMetrics fm = g2.getFontMetrics();
            Insets in = getInsets();
            int maxW = w - in.left - in.right;
            int y = in.top + fm.getAscent();
            for (String paragraph : text.split("\n")) {
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.split(" ")) {
                    String candidate = line.length() == 0 ? word : line + " " + word;
                    if (fm.stringWidth(candidate) > maxW && line.length() > 0) {
                        g2.drawString(line.toString(), in.left, y);
                        y += fm.getHeight() + 2;
                        line = new StringBuilder(word);
                    } else {
                        line = new StringBuilder(candidate);
                    }
                }
                g2.drawString(line.toString(), in.left, y);
                y += fm.getHeight() + 2;
            }
        }
        g2.dispose();
    }
}
