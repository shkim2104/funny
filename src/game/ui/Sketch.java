package game.ui;

import game.model.Item;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.IntConsumer;

/**
 * Pencil-on-notebook UI kit, matching the pencil dungeon art: ruled paper pages, hand-drawn wobbly
 * boxes and lines, hatched button fills, a highlighter for selection. Wobble is seeded per component,
 * so a box keeps the same shape on every repaint instead of shimmering.
 */
public final class Sketch {
    public static final Color PAPER = new Color(247, 243, 230);
    public static final Color INK = new Color(50, 48, 58);
    public static final Color INK_SOFT = new Color(118, 112, 106);
    public static final Color RED_PENCIL = new Color(190, 62, 52);
    public static final Color BLUE_PENCIL = new Color(52, 96, 170);
    private static final Color RULE = new Color(110, 150, 205, 70);
    private static final Color MARGIN = new Color(214, 92, 92, 110);
    private static final Color MARKER = new Color(250, 220, 70, 120);

    /** A bundled fonts/handwriting.ttf wins if present; otherwise the system Korean UI font. */
    private static final Font HAND_BASE = loadHand();

    private Sketch() {}

    private static Font loadHand() {
        try (InputStream in = Sketch.class.getResourceAsStream("fonts/handwriting.ttf")) {
            if (in != null) return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception ignored) {
            // fall through to the system font
        }
        return new Font("맑은 고딕", Font.PLAIN, 12);
    }

    public static Font hand(int style, float size) {
        return HAND_BASE.deriveFont(style, size);
    }

    /** Grade colors re-inked for paper: the dark-theme hexes are too pale on cream. */
    public static Color gradeInk(Item.Grade grade) {
        switch (grade) {
            case UNCOMMON: return new Color(54, 134, 58);
            case RARE: return new Color(44, 96, 180);
            case EPIC: return new Color(128, 64, 176);
            case LEGENDARY: return new Color(186, 124, 16);
            case MYTHIC: return new Color(196, 52, 44);
            default: return new Color(100, 100, 106);
        }
    }

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    // ---------- drawing ----------

    /** Ruled notebook paper over the whole area, with a red margin line at marginX (none if < 0). */
    public static void paper(Graphics2D g, int w, int h, int marginX) {
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(RULE);
        for (int y = 34; y < h; y += 30) g.drawLine(0, y, w, y);
        if (marginX >= 0) {
            g.setColor(MARGIN);
            g.drawLine(marginX, 0, marginX, h);
            g.drawLine(marginX + 4, 0, marginX + 4, h);
        }
    }

    /** A wobbly pencil line: two passes, the second lighter and a little off. */
    public static void line(Graphics2D g, double x1, double y1, double x2, double y2, long seed, float width, Color c) {
        Random r = new Random(seed);
        for (int pass = 0; pass < 2; pass++) {
            g.setStroke(new BasicStroke(pass == 0 ? width : width * 0.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(c, pass == 0 ? 220 : 110));
            g.draw(wobble(new double[]{x1, y1, x2, y2}, r, 1.2, false));
        }
    }

    /** A hand-drawn rectangle whose corners overshoot a little, like a quick pencil box. */
    public static void box(Graphics2D g, double x, double y, double w, double h, long seed, float width, Color c) {
        Random r = new Random(seed);
        for (int pass = 0; pass < 2; pass++) {
            double o = pass == 0 ? 0 : 1.5;
            g.setStroke(new BasicStroke(pass == 0 ? width : width * 0.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(c, pass == 0 ? 225 : 105));
            double ov = 4 + r.nextDouble() * 3;
            g.draw(wobble(new double[]{x - ov + o, y + o, x + w + ov, y + o}, r, 1.0, false));
            g.draw(wobble(new double[]{x + w - o, y - ov, x + w - o, y + h + ov}, r, 1.0, false));
            g.draw(wobble(new double[]{x + w + ov - o, y + h - o, x - ov, y + h - o}, r, 1.0, false));
            g.draw(wobble(new double[]{x + o, y + h + ov, x + o, y - ov}, r, 1.0, false));
        }
    }

    /** A hand-drawn circle: two passes around, each a little off and overshooting its start. */
    public static void circle(Graphics2D g, double cx, double cy, double r, long seed, float width, Color c) {
        Random rnd = new Random(seed);
        for (int pass = 0; pass < 2; pass++) {
            Path2D p = new Path2D.Double();
            int n = Math.max(24, (int) (r / 3));
            double phase = rnd.nextDouble() * Math.PI * 2, wob = 1.2 + pass * 0.8;
            double start = rnd.nextDouble() * Math.PI * 2;
            for (int i = 0; i <= n + 3; i++) {
                double a = start + Math.PI * 2 * i / n;
                double rr = r + Math.sin(phase + i * 0.9) * wob * 0.6 + (rnd.nextDouble() - 0.5) * wob * 0.4;
                double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
                if (i == 0) p.moveTo(x, y);
                else p.lineTo(x, y);
            }
            g.setStroke(new BasicStroke(pass == 0 ? width : width * 0.55f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(withAlpha(c, pass == 0 ? 225 : 105));
            g.draw(p);
        }
    }

    /** The outline of box() as one closed shape, for clipping hatch fills inside it. */
    public static Shape boxShape(double x, double y, double w, double h) {
        return new Rectangle2D.Double(x, y, w, h);
    }

    /** Parallel pencil strokes clipped to the shape. */
    public static void hatch(Graphics2D g, Shape s, long seed, Color c, int alpha, double spacing, double angleDeg) {
        Random r = new Random(seed);
        Shape oldClip = g.getClip();
        g.clip(s);
        Rectangle2D b = s.getBounds2D();
        double a = Math.toRadians(angleDeg), dx = Math.cos(a), dy = Math.sin(a), nx = -dy, ny = dx;
        double cx = b.getCenterX(), cy = b.getCenterY(), half = Math.hypot(b.getWidth(), b.getHeight()) / 2 + 4;
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(withAlpha(c, alpha));
        for (double t = -half; t <= half; t += spacing * (0.8 + 0.4 * r.nextDouble())) {
            g.draw(wobble(new double[]{cx + nx * t - dx * half, cy + ny * t - dy * half, cx + nx * t + dx * half, cy + ny * t + dy * half},
                    r, 0.8, false));
        }
        g.setClip(oldClip);
    }

    /** A highlighter swipe across the rectangle (selection). */
    public static void marker(Graphics2D g, double x, double y, double w, double h, long seed) {
        Random r = new Random(seed);
        g.setColor(MARKER);
        g.setStroke(new BasicStroke((float) h, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double my = y + h / 2;
        g.draw(wobble(new double[]{x + h / 2, my + r.nextDouble() * 2 - 1, x + w - h / 2, my + r.nextDouble() * 2 - 1}, r, 1.5, false));
    }

    private static Shape wobble(double[] seg, Random r, double amp, boolean closed) {
        double x1 = seg[0], y1 = seg[1], x2 = seg[2], y2 = seg[3];
        double len = Math.hypot(x2 - x1, y2 - y1);
        int n = Math.max(2, (int) (len / 14));
        double nx = -(y2 - y1) / Math.max(1, len), ny = (x2 - x1) / Math.max(1, len);
        double phase = r.nextDouble() * 10, drift = (r.nextDouble() - 0.5) * amp;
        Path2D p = new Path2D.Double();
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            double off = drift + amp * Math.sin(phase + i * 0.7) * 0.8 + (r.nextDouble() - 0.5) * amp * 0.4;
            double px = x1 + (x2 - x1) * t + nx * off, py = y1 + (y2 - y1) * t + ny * off;
            if (i == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }
        if (closed) p.closePath();
        return p;
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a * c.getAlpha() / 255)));
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ---------- components ----------

    /** A notebook page: the dialog's background. */
    public static class Page extends JPanel {
        private final int marginX;

        public Page(LayoutManager lm, int marginX) {
            super(lm);
            this.marginX = marginX;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            paper(g2, getWidth(), getHeight(), marginX);
            g2.dispose();
        }
    }

    /** A transparent panel framed by a pencil box. */
    public static class Box extends JPanel {
        private final long seed;

        public Box(LayoutManager lm, long seed) {
            super(lm);
            this.seed = seed;
            setOpaque(false);
            setBorder(new EmptyBorder(14, 16, 14, 16));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            box(g2, 5, 5, getWidth() - 10, getHeight() - 10, seed, 2.2f, INK);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * A scrap of notebook paper stuck onto a picture: paper fill with a faint shadow and a pencil box,
     * so pencil text and buttons stay readable over hatched artwork.
     */
    public static class Note extends JPanel {
        private final long seed;

        public Note(LayoutManager lm, long seed) {
            super(lm);
            this.seed = seed;
            setOpaque(false);
            setBorder(new EmptyBorder(16, 22, 16, 22));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(new Color(0, 0, 0, 45));
            g2.fillRect(9, 9, w - 12, h - 12);
            g2.setColor(PAPER);
            g2.fillRect(5, 5, w - 12, h - 12);
            g2.setColor(RULE);
            g2.setStroke(new BasicStroke(1.2f));
            for (int y = 30; y < h - 10; y += 26) g2.drawLine(6, y, w - 8, y);
            box(g2, 5, 5, w - 12, h - 12, seed, 2.2f, INK);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** A pencil-boxed button; hover hatches it in, primary ones are drawn in red pencil. */
    public static class Button extends JButton {
        private final boolean primary;
        private final long seed;
        private boolean hover;

        public Button(String text, boolean primary) {
            super(text);
            this.primary = primary;
            this.seed = text.hashCode() * 31L + (primary ? 7 : 3);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(hand(Font.BOLD, 15f));
            setForeground(primary ? RED_PENCIL : INK);
            setBorder(new EmptyBorder(10, 20, 10, 20));
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
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            Color ink = isEnabled() ? (primary ? RED_PENCIL : INK) : new Color(INK_SOFT.getRed(), INK_SOFT.getGreen(), INK_SOFT.getBlue(), 120);
            Shape inside = boxShape(5, 5, w - 10, h - 10);
            if (isEnabled() && (hover || getModel().isPressed())) {
                hatch(g2, inside, seed, ink, getModel().isPressed() ? 110 : 70, 6, 35);
            } else if (primary && isEnabled()) {
                hatch(g2, inside, seed, ink, 40, 9, 35);
            }
            if (isFocusOwner() && isEnabled()) marker(g2, 10, h - 14, w - 20, 8, seed + 1);
            box(g2, 5, 5, w - 10, h - 10, seed, primary ? 2.4f : 2.0f, ink);
            g2.dispose();
            setForeground(ink);
            super.paintComponent(g);
        }
    }

    /** Notebook index tabs over a pencil box; only the selected tab's content shows. */
    public static class Tabs extends JPanel {
        private final JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        private final CardLayout cards = new CardLayout();
        private final JPanel body;
        private final List<Tab> tabs = new ArrayList<>();
        private IntConsumer onChange = i -> { };
        private int selected;

        public Tabs(long seed) {
            super(new BorderLayout());
            setOpaque(false);
            header.setOpaque(false);
            header.setBorder(new EmptyBorder(0, 10, 0, 0));
            body = new Box(cards, seed);
            body.setBorder(new EmptyBorder(10, 10, 10, 10));
            add(header, BorderLayout.NORTH);
            add(body, BorderLayout.CENTER);
        }

        public void addTab(String title, Component content) {
            int index = tabs.size();
            Tab tab = new Tab(title, index);
            tab.addActionListener(e -> setSelected(index));
            tabs.add(tab);
            header.add(tab);
            body.add(content, String.valueOf(index));
            if (index == 0) setSelected(0);
        }

        public void setSelected(int index) {
            selected = index;
            cards.show(body, String.valueOf(index));
            for (Tab t : tabs) t.repaint();
            onChange.accept(index);
        }

        public int getSelected() {
            return selected;
        }

        public void onChange(IntConsumer listener) {
            onChange = listener;
        }

        private final class Tab extends JButton {
            private final int index;

            Tab(String title, int index) {
                super(title);
                this.index = index;
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setOpaque(false);
                setFont(hand(Font.BOLD, 15f));
                setBorder(new EmptyBorder(8, 18, 10, 18));
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                boolean on = selected == index;
                int w = getWidth(), h = getHeight();
                if (on) marker(g2, 8, h / 2 - 2, w - 16, 14, index * 17L + 5);
                long s = index * 101L + 9;
                line(g2, 4, h, 6, 4, s, on ? 2.2f : 1.6f, on ? INK : INK_SOFT);
                line(g2, 6, 4, w - 6, 3, s + 1, on ? 2.2f : 1.6f, on ? INK : INK_SOFT);
                line(g2, w - 6, 3, w - 4, h, s + 2, on ? 2.2f : 1.6f, on ? INK : INK_SOFT);
                g2.dispose();
                setForeground(on ? INK : INK_SOFT);
                super.paintComponent(g);
            }
        }
    }

    /** A see-through scroll pane with a penciled scrollbar thumb, so the ruled paper shows behind the list. */
    public static JScrollPane scroll(JComponent view) {
        view.setOpaque(false);
        JScrollPane sp = new JScrollPane(view);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setViewportBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getVerticalScrollBar().setOpaque(false);
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        sp.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected JButton createDecreaseButton(int o) { return zero(); }
            @Override protected JButton createIncreaseButton(int o) { return zero(); }

            private JButton zero() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                return b;
            }

            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
                if (r.isEmpty()) return;
                Graphics2D g2 = smooth(g);
                hatch(g2, new Rectangle(r.x + 3, r.y + 2, r.width - 6, r.height - 4), 77, INK, 120, 4, 50);
                box(g2, r.x + 3, r.y + 2, r.width - 6, r.height - 4, 78, 1.4f, INK);
                g2.dispose();
            }
        });
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        return sp;
    }

    /**
     * A list row on paper: icon, grade tag over the name, a value on the right; selection is a
     * highlighter swipe, rows are separated by a faint pencil line.
     */
    public static class Row extends JPanel {
        private final JLabel icon = new JLabel();
        private final JLabel tag = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel value = new JLabel();
        private boolean selected;
        private long seed;

        public Row() {
            super(new BorderLayout(10, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(0, 10, 0, 14));
            // GridLayout, not BoxLayout: BoxLayout caches sizes, and a reused renderer then clips the first row's name.
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            tag.setFont(hand(Font.BOLD, 11f));
            name.setFont(hand(Font.BOLD, 15f));
            name.setForeground(INK);
            text.add(tag);
            text.add(name);
            JPanel west = new JPanel(new BorderLayout(10, 0));
            west.setOpaque(false);
            west.add(icon, BorderLayout.WEST);
            west.add(text, BorderLayout.CENTER);
            add(west, BorderLayout.WEST);
            value.setFont(hand(Font.BOLD, 14f));
            value.setForeground(BLUE_PENCIL);
            add(value, BorderLayout.EAST);
        }

        public Row set(Item item, String nameText, String valueText, boolean selected, int index) {
            icon.setIcon(ItemArt.item(item, 44));
            tag.setText(item.getGrade().getLabel());
            tag.setForeground(gradeInk(item.getGrade()));
            name.setText(nameText);
            value.setText(valueText);
            this.selected = selected;
            this.seed = index * 131L + nameText.hashCode();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            if (selected) marker(g2, 6, 8, getWidth() - 12, getHeight() - 16, seed);
            line(g2, 8, getHeight() - 2, getWidth() - 8, getHeight() - 2, seed + 3, 1.1f, new Color(118, 112, 106, 120));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Detail-pane text for an item on paper: grade tag, type, stat, then special effects in red pencil. */
    public static String itemDetailHtml(Item item) {
        StringBuilder sb = new StringBuilder("<html><body style='width:210px'>")
                .append("<font color='").append(hex(gradeInk(item.getGrade()))).append("'><b>[")
                .append(item.getGrade().getLabel()).append("]</b></font> ").append(item.getTypeLabel())
                .append(item.isGachaOnly() ? " <font color='" + hex(INK_SOFT) + "'>· 보물 뽑기 전용</font>" : "")
                .append(item.getGrade() == Item.Grade.MYTHIC ? " <font color='" + hex(INK_SOFT) + "'>· 보스 전용</font>" : "")
                .append("<br><font color='").append(hex(BLUE_PENCIL)).append("'><b>").append(item.getStatText()).append("</b></font>");
        for (String line : item.getBonus().lines()) {
            sb.append("<br><font color='").append(hex(RED_PENCIL)).append("'>✎ ").append(line).append("</font>");
        }
        return sb.append("</body></html>").toString();
    }

    /** A penciled gauge: a wobbly box, hatched in color up to the fraction, with "value/max" written over it. */
    public static class Bar extends JComponent {
        private final Color color;
        private final long seed;
        private double fraction;
        private String text = "";

        public Bar(Color color, long seed) {
            this.color = color;
            this.seed = seed;
            setPreferredSize(new Dimension(180, 34));
            setFont(hand(Font.BOLD, 13f));
        }

        public void set(int value, int max) {
            fraction = max <= 0 ? 0 : Math.max(0, Math.min(1, value / (double) max));
            text = value + " / " + max;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = smooth(g);
            int w = getWidth(), h = getHeight();
            double fill = (w - 12) * fraction;
            if (fill > 0) {
                Shape s = new Rectangle2D.Double(6, 5, fill, h - 10);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 45));
                g2.fill(s);
                hatch(g2, s, seed, color, 170, 4.5, 60);
            }
            box(g2, 6, 5, w - 12, h - 10, seed + 1, 1.8f, INK);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(text)) / 2, ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.setColor(PAPER);
            g2.drawString(text, tx + 1, ty + 1);
            g2.setColor(INK);
            g2.drawString(text, tx, ty);
            g2.dispose();
        }
    }

    /** A small hand-written label. */
    public static JLabel label(String text, int style, float size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(hand(style, size));
        l.setForeground(color);
        return l;
    }

    /** A page title with a scribbled underline. */
    public static JLabel title(String text) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = smooth(g);
                FontMetrics fm = getFontMetrics(getFont());
                int w = Math.min(getWidth() - 4, fm.stringWidth(getText()) + 10);
                line(g2, 0, getHeight() - 4, w, getHeight() - 6, getText().hashCode(), 2.2f, RED_PENCIL);
                line(g2, 6, getHeight() - 1, w - 10, getHeight() - 2, getText().hashCode() + 1, 1.4f, RED_PENCIL);
                g2.dispose();
            }
        };
        l.setFont(hand(Font.BOLD, 24f));
        l.setForeground(INK);
        l.setBorder(new EmptyBorder(0, 0, 8, 0));
        return l;
    }
}
