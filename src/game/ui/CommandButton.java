package game.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A battle command drawn in pencil on notebook paper, hatched in its own colored pencil (공격 red,
 * 아이템 yellow, 도망 blue, skills their effect color), with an optional second line (e.g. "MP 12").
 * "Dimmed" keeps it clickable but washed out (e.g. not enough MP).
 */
public class CommandButton extends JButton {
    private Color color;
    private String subtitle;
    private boolean dimmed;
    private boolean hover;

    public CommandButton(String text, Color color) {
        super(text);
        this.color = color;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setBorder(new EmptyBorder(8, 14, 8, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(Sketch.hand(Font.BOLD, 18f));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
            @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
        });
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    public void setColor(Color color) {
        this.color = color;
        repaint();
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
        repaint();
    }

    public void setDimmed(boolean dimmed) {
        this.dimmed = dimmed;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(120, subtitle == null ? 46 : 56);
    }

    /**
     * Pencil style: a paper slab hatched in the command's colored pencil, framed with a wobbly graphite
     * box. Hover/press hatch denser, focus adds a highlighter swipe and a ▶ pencil arrow, dimmed
     * (e.g. not enough MP) keeps only a faint wash. Wobble is seeded by the label, so it doesn't shimmer.
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Sketch.smooth(g);
        int w = getWidth(), h = getHeight();
        boolean pressed = getModel().isPressed();
        boolean focused = isFocusOwner() && isEnabled();
        int dy = pressed ? 2 : 0;
        long seed = getText().hashCode() * 17L + 3;

        Shape slab = new java.awt.geom.Rectangle2D.Double(4, 4 + dy, w - 8, h - 9);
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fill(new java.awt.geom.Rectangle2D.Double(7, 8, w - 8, h - 9));
        g2.setColor(Sketch.PAPER);
        g2.fill(slab);
        if (isEnabled()) {
            Color pencil = dimmed ? mix(color, new Color(150, 150, 156), 0.6f) : color;
            g2.setColor(new Color(pencil.getRed(), pencil.getGreen(), pencil.getBlue(), dimmed ? 30 : 60));
            g2.fill(slab);
            int alpha = dimmed ? 70 : pressed ? 210 : (hover || focused) ? 180 : 130;
            Sketch.hatch(g2, slab, seed, pencil, alpha, (hover || focused) && !dimmed ? 4.5 : 6.5, 35);
        }
        if (focused) Sketch.marker(g2, 10, h - 16, w - 20, 9, seed + 5);
        Color ink = isEnabled() ? (dimmed ? Sketch.INK_SOFT : Sketch.INK) : new Color(150, 146, 140);
        Sketch.box(g2, 4, 4 + dy, w - 8, h - 9, seed + 1, focused ? 2.8f : 2.1f, ink);

        Font titleFont = getFont();
        FontMetrics fm = g2.getFontMetrics(titleFont);
        String title = getText();
        int titleW = fm.stringWidth(title);
        int textBlockH = subtitle == null ? fm.getAscent() : fm.getAscent() + 16;
        int ty = (h - 4 - textBlockH) / 2 + fm.getAscent() + dy;
        int tx = (w - titleW) / 2;
        drawOnPaper(g2, title, titleFont, tx, ty, ink);
        if (subtitle != null) {
            Font sub = Sketch.hand(Font.BOLD, 12f);
            int sw = g2.getFontMetrics(sub).stringWidth(subtitle);
            drawOnPaper(g2, subtitle, sub, (w - sw) / 2f, ty + 16, isEnabled() ? Sketch.BLUE_PENCIL : ink);
        }

        if (focused) {
            int cx = Math.max(10, tx - 20), cy = (h - 4) / 2 + dy;
            Sketch.line(g2, cx, cy - 7, cx + 10, cy, seed + 6, 2.4f, Sketch.RED_PENCIL);
            Sketch.line(g2, cx + 10, cy, cx, cy + 7, seed + 7, 2.4f, Sketch.RED_PENCIL);
        }
        g2.dispose();
    }

    /** Text written on the hatched slab: a thin paper halo first so the hatching never runs through it. */
    private static void drawOnPaper(Graphics2D g2, String text, Font font, float x, float y, Color ink) {
        Shape s = font.createGlyphVector(g2.getFontRenderContext(), text).getOutline(x, y);
        g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(247, 243, 230, 230));
        g2.draw(s);
        g2.setColor(ink);
        g2.fill(s);
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
