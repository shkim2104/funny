package game.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * The handheld-style message window: a cream panel with a dark double rim. Used as a container
 * (the battle log) or, via setText, as the "무엇을 할까?" prompt that wraps its text to fit.
 */
public class MessageBox extends JPanel {
    private String text = "";

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
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(BattleHud.BOX_EDGE);
        g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 18, 18));
        g2.setColor(new Color(96, 168, 176));   // teal inner rim, like the classic text box frame
        g2.fill(new RoundRectangle2D.Float(3, 3, w - 6, h - 6, 15, 15));
        g2.setColor(BattleHud.BOX_FILL);
        g2.fill(new RoundRectangle2D.Float(7, 7, w - 14, h - 14, 11, 11));

        if (!text.isEmpty()) {
            g2.setFont(Theme.dosFont(Font.PLAIN, 17f));
            g2.setColor(BattleHud.INK);
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
