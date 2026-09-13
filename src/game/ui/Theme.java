package game.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public final class Theme {
    public static final Color BG = new Color(15, 16, 20);
    public static final Color PANEL = new Color(24, 26, 31);
    public static final Color SURFACE = new Color(32, 34, 40);
    public static final Color BORDER = new Color(48, 51, 59);
    public static final Color ACCENT = new Color(199, 168, 106);
    public static final Color ACCENT_HOVER = new Color(215, 187, 132);
    public static final Color ACCENT_PRESSED = new Color(165, 137, 82);
    public static final Color TEXT = new Color(230, 230, 235);
    public static final Color TEXT_DIM = new Color(140, 143, 154);
    public static final Color HP = new Color(200, 96, 96);
    public static final Color MP = new Color(96, 140, 202);

    public static final int RADIUS = 14;

    public static final Font TITLE_FONT = new Font("맑은 고딕", Font.BOLD, 23);
    public static final Font HEADER_FONT = new Font("맑은 고딕", Font.BOLD, 14);
    public static final Font BODY_FONT = new Font("맑은 고딕", Font.PLAIN, 13);
    public static final Font SMALL_FONT = new Font("맑은 고딕", Font.PLAIN, 12);
    public static final Font MONO_FONT = new Font("맑은 고딕", Font.PLAIN, 13);

    private Theme() {}

    public static void apply(JFrame frame) {
        frame.getContentPane().setBackground(BG);
    }

    // ---------- rounded panel ----------
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;

        public RoundedPanel(LayoutManager lm, Color fill, int radius) {
            super(lm);
            this.fill = fill;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.setColor(BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static RoundedPanel panel(LayoutManager lm) {
        return new RoundedPanel(lm, PANEL, RADIUS);
    }

    public static RoundedPanel surface(LayoutManager lm) {
        return new RoundedPanel(lm, SURFACE, RADIUS - 4);
    }

    // ---------- labels ----------
    public static JLabel title(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE_FONT);
        l.setForeground(ACCENT);
        return l;
    }

    public static JLabel header(String text) {
        JLabel l = new JLabel(text);
        l.setFont(HEADER_FONT);
        l.setForeground(TEXT);
        return l;
    }

    public static JLabel body(String text) {
        JLabel l = new JLabel(text);
        l.setFont(BODY_FONT);
        l.setForeground(TEXT_DIM);
        return l;
    }

    // ---------- buttons ----------
    public static class FlatButton extends JButton {
        private boolean hover = false;
        private final boolean primary;
        private final boolean ghost;

        public FlatButton(String text, boolean primary) {
            this(text, primary, false);
        }

        /** ghost = see-through button (for overlaying on artwork): no solid box, just a faint tint + text. */
        public FlatButton(String text, boolean primary, boolean ghost) {
            super(text);
            this.primary = primary;
            this.ghost = ghost;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(ghost ? (primary ? ACCENT_HOVER : TEXT) : (primary ? BG : TEXT));
            setFont(HEADER_FONT);
            setBorder(new EmptyBorder(11, 18, 11, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (ghost) {
                Color fill;
                if (!isEnabled()) fill = null;
                else if (getModel().isPressed()) fill = new Color(8, 9, 12, 150);
                else if (hover) fill = new Color(8, 9, 12, 110);
                else fill = new Color(8, 9, 12, 60);

                if (fill != null) {
                    g2.setColor(fill);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS - 2, RADIUS - 2));
                }
                g2.setColor(primary ? new Color(199, 168, 106, 180) : new Color(255, 255, 255, 60));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, RADIUS - 2, RADIUS - 2));
            } else {
                Color fill;
                if (!isEnabled()) fill = SURFACE;
                else if (getModel().isPressed()) fill = primary ? ACCENT_PRESSED : new Color(54, 58, 67);
                else if (hover) fill = primary ? ACCENT_HOVER : new Color(42, 45, 52);
                else fill = primary ? ACCENT : SURFACE;

                g2.setColor(fill);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), RADIUS - 2, RADIUS - 2));

                if (!primary) {
                    g2.setColor(BORDER);
                    g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, RADIUS - 2, RADIUS - 2));
                }
            }

            if (isFocusOwner() && isEnabled()) {
                g2.setColor(ghost ? ACCENT_HOVER : (primary ? Color.WHITE : ACCENT));
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, getWidth() - 3, getHeight() - 3, RADIUS - 3, RADIUS - 3));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton button(String text) {
        return new FlatButton(text, false);
    }

    public static JButton primaryButton(String text) {
        return new FlatButton(text, true);
    }

    public static JButton ghostButton(String text) {
        return new FlatButton(text, false, true);
    }

    public static JButton ghostPrimaryButton(String text) {
        return new FlatButton(text, true, true);
    }

    // ---------- arrow-key + enter navigation ----------
    /** Wires Up/Left to focus the previous button, Down/Right the next, and Enter to click the focused one. */
    public static void arrowNav(JButton... buttons) {
        for (int i = 0; i < buttons.length; i++) {
            JButton btn = buttons[i];
            int idx = i;
            InputMap im = btn.getInputMap(JComponent.WHEN_FOCUSED);
            ActionMap am = btn.getActionMap();
            im.put(KeyStroke.getKeyStroke("UP"), "nav-prev");
            im.put(KeyStroke.getKeyStroke("LEFT"), "nav-prev");
            im.put(KeyStroke.getKeyStroke("DOWN"), "nav-next");
            im.put(KeyStroke.getKeyStroke("RIGHT"), "nav-next");
            im.put(KeyStroke.getKeyStroke("ENTER"), "nav-enter");
            am.put("nav-prev", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { focusStep(buttons, idx, -1); }
            });
            am.put("nav-next", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { focusStep(buttons, idx, 1); }
            });
            am.put("nav-enter", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { btn.doClick(); }
            });
        }
    }

    private static void focusStep(JButton[] buttons, int from, int dir) {
        int n = buttons.length;
        int i = from;
        for (int step = 0; step < n; step++) {
            i = ((i + dir) % n + n) % n;
            if (buttons[i].isEnabled() && buttons[i].isShowing()) {
                buttons[i].requestFocusInWindow();
                return;
            }
        }
    }

    /** Requests focus on the first enabled button, once it is realized on screen. */
    public static void focusFirst(JButton... buttons) {
        for (JButton b : buttons) {
            if (b.isEnabled()) {
                SwingUtilities.invokeLater(b::requestFocusInWindow);
                return;
            }
        }
    }

    // ---------- meter (hp/mp bar) ----------
    public static class Meter extends JComponent {
        private int max = 100;
        private int value = 100;
        private final Color color;

        public Meter(Color color) {
            this.color = color;
            setPreferredSize(new Dimension(200, 22));
            setOpaque(false);
        }

        public void setMaximum(int max) { this.max = Math.max(1, max); repaint(); }
        public void setValue(int value) { this.value = value; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int h = getHeight();
            int w = getWidth();
            int arc = h;

            g2.setColor(SURFACE);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

            float ratio = Math.max(0f, Math.min(1f, value / (float) max));
            int fillW = Math.round(w * ratio);
            if (fillW > 0) {
                g2.setColor(color);
                g2.fill(new RoundRectangle2D.Float(0, 0, fillW, h, arc, arc));
            }

            g2.setColor(BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, h - 1, arc, arc));

            String txt = value + " / " + max;
            g2.setFont(SMALL_FONT);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(txt)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(TEXT);
            g2.drawString(txt, tx, ty);
            g2.dispose();
        }
    }

    public static Meter meter(Color color) {
        return new Meter(color);
    }

    // legacy alias kept for compatibility with older call sites
    public static Meter bar(Color color) {
        return meter(color);
    }

    // ---------- list ----------
    public static void styleList(JList<String> list) {
        list.setBackground(SURFACE);
        list.setForeground(TEXT);
        list.setFont(BODY_FONT);
        list.setSelectionBackground(ACCENT);
        list.setSelectionForeground(BG);
        list.setFixedCellHeight(30);
        list.setBorder(new EmptyBorder(6, 10, 6, 10));
    }

    // ---------- scroll ----------
    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.getViewport().setBackground(SURFACE);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ---------- text field ----------
    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setBackground(SURFACE);
        field.setForeground(TEXT);
        field.setCaretColor(TEXT);
        field.setFont(BODY_FONT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        return field;
    }

    public static void styleDialog(JDialog d) {
        d.getContentPane().setBackground(BG);
    }
}
