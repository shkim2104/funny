package game.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * Chunky colored battle command, in the spirit of the handheld Pokémon touch menus: a glossy colored
 * slab with a dark rim, white outlined label, optional second line (e.g. "MP 12"), and a ▶ cursor plus
 * white rim when it has keyboard focus. "Dimmed" keeps it clickable but washed out (e.g. not enough MP).
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
        setFont(Theme.dosFont(Font.BOLD, 18f));
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

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        boolean pressed = getModel().isPressed();
        boolean focused = isFocusOwner() && isEnabled();

        Color base;
        if (!isEnabled()) base = new Color(96, 98, 106);
        else if (dimmed) base = mix(color, new Color(120, 120, 126), 0.65f);
        else base = color;
        if (isEnabled() && (hover || focused) && !pressed) base = mix(base, Color.WHITE, 0.15f);
        if (pressed) base = mix(base, Color.BLACK, 0.2f);
        int dy = pressed ? 2 : 0;

        // Dark underside, then the slab sitting on it (pressing pushes the slab down onto it).
        g2.setColor(new Color(28, 30, 36));
        g2.fill(new RoundRectangle2D.Float(1, 3, w - 2, h - 4, 16, 16));
        RoundRectangle2D slab = new RoundRectangle2D.Float(1, 1 + dy, w - 2, h - 5, 16, 16);
        g2.setPaint(new GradientPaint(0, 0, mix(base, Color.WHITE, 0.25f), 0, h, mix(base, Color.BLACK, 0.12f)));
        g2.fill(slab);
        // Glossy top band.
        g2.setColor(new Color(255, 255, 255, isEnabled() ? 55 : 25));
        g2.fill(new RoundRectangle2D.Float(5, 4 + dy, w - 10, (h - 5) * 0.38f, 12, 12));
        g2.setStroke(new BasicStroke(focused ? 3f : 2f));
        g2.setColor(focused ? Color.WHITE : new Color(28, 30, 36));
        g2.draw(new RoundRectangle2D.Float(focused ? 2.5f : 1.5f, (focused ? 2.5f : 1.5f) + dy,
                w - (focused ? 5 : 3), h - (focused ? 8 : 6), 15, 15));

        Color textColor = isEnabled() ? (dimmed ? new Color(225, 225, 228) : Color.WHITE) : new Color(176, 178, 184);
        Font titleFont = getFont();
        FontMetrics fm = g2.getFontMetrics(titleFont);
        String title = getText();
        int titleW = fm.stringWidth(title);
        int textBlockH = subtitle == null ? fm.getAscent() : fm.getAscent() + 16;
        int ty = (h - 4 - textBlockH) / 2 + fm.getAscent() + dy;
        int tx = (w - titleW) / 2;
        drawOutlined(g2, title, titleFont, tx, ty, textColor);
        if (subtitle != null) {
            Font sub = Theme.dosFont(Font.BOLD, 12f);
            int sw = g2.getFontMetrics(sub).stringWidth(subtitle);
            drawOutlined(g2, subtitle, sub, (w - sw) / 2f, ty + 16, isEnabled() ? new Color(255, 244, 200) : textColor);
        }

        if (focused) {
            // ▶ cursor, like the selection arrow in the handheld menus.
            int cx = Math.max(8, tx - 18), cy = (h - 4) / 2 + dy;
            Polygon arrow = new Polygon(new int[]{cx, cx + 9, cx}, new int[]{cy - 7, cy, cy + 7}, 3);
            g2.setColor(new Color(28, 30, 36));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(arrow);
            g2.setColor(Color.WHITE);
            g2.fill(arrow);
        }
        g2.dispose();
    }

    private static void drawOutlined(Graphics2D g2, String text, Font font, float x, float y, Color fill) {
        Shape s = font.createGlyphVector(g2.getFontRenderContext(), text).getOutline(x, y);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(28, 30, 36, 220));
        g2.draw(s);
        g2.setColor(fill);
        g2.fill(s);
    }

    private static Color mix(Color a, Color b, float t) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
}
