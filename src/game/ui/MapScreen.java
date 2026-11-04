package game.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * A clickable illustrated map: the artwork is fit inside the panel (letterboxed, never cropped),
 * and rectangular hotspots defined in the artwork's own pixel coordinates react to hover and
 * click. Short notices are shown as a fading toast instead of a modal dialog.
 */
public class MapScreen extends JPanel {
    private static final long TOAST_DURATION = 1800;
    private static final Color HOVER_FILL = new Color(255, 236, 160, 60);
    private static final Color HOVER_LINE = new Color(255, 214, 90, 230);

    private static final class Hotspot {
        final String label;
        final Rectangle area;
        final Runnable onClick;
        final BooleanSupplier locked;
        final BooleanSupplier shown;

        Hotspot(String label, Rectangle area, Runnable onClick, BooleanSupplier locked, BooleanSupplier shown) {
            this.label = label;
            this.area = area;
            this.onClick = onClick;
            this.locked = locked;
            this.shown = shown;
        }
    }

    private Image art;
    private final List<Hotspot> hotspots = new ArrayList<>();

    /** A person standing on the map: a picture in an area of the artwork (a penciled stand-in until drawn). */
    private static final class Figure {
        final Image image;
        final Rectangle area;
        final String name;

        Figure(Image image, Rectangle area, String name) {
            this.image = image;
            this.area = area;
            this.name = name;
        }
    }

    private final List<Figure> figures = new ArrayList<>();
    /** "!" marks over places where something finished waits (artwork pixels, shown while the test holds). */
    private final List<Object[]> badges = new ArrayList<>();

    /** Puts a "!" over the top of an area (artwork pixels) while `shown` is true. */
    public void addBadge(Rectangle area, BooleanSupplier shown) {
        badges.add(new Object[]{new Rectangle(area), shown});
    }
    /** The tutorial's pointer: a blinking pencil circle on this area, and the line over the map. */
    private Rectangle guide;
    private String guideText;
    private Timer guideTimer;
    private Hotspot hovered;
    private Point mouse;

    // Scaled artwork is cached per panel size; rescaling a ~1200px image on every hover repaint is wasteful.
    private BufferedImage scaled;
    private int scaledForW = -1, scaledForH = -1;

    private String toastText;
    private long toastStart;
    private Timer toastTimer;

    public MapScreen(Image art) {
        this.art = art;
        setOpaque(true);
        setBackground(Sketch.PAPER);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouse = e.getPoint();
                Hotspot h = hotspotAt(e.getPoint());
                if (h != hovered) {
                    hovered = h;
                    setCursor(h != null ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                }
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = null;
                mouse = null;
                setCursor(Cursor.getDefaultCursor());
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                Hotspot h = hotspotAt(e.getPoint());
                if (h != null) h.onClick.run();
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    /** Adds a clickable area, given in the artwork's pixel coordinates. */
    public void addHotspot(String label, int x, int y, int w, int h, Runnable onClick) {
        addHotspot(label, x, y, w, h, onClick, () -> false);
    }

    /** Adds a clickable area that is crossed out with a pencil X while locked returns true (it still receives clicks). */
    public void addHotspot(String label, int x, int y, int w, int h, Runnable onClick, BooleanSupplier locked) {
        hotspots.add(new Hotspot(label, new Rectangle(x, y, w, h), onClick, locked, () -> true));
    }

    /** Adds a clickable area that only exists while shown returns true (e.g. during the invasion). */
    public void addShownHotspot(String label, int x, int y, int w, int h, Runnable onClick, BooleanSupplier shown) {
        hotspots.add(new Hotspot(label, new Rectangle(x, y, w, h), onClick, () -> false, shown));
    }

    /**
     * Puts a person on the map, drawn in the area (artwork pixels), with a click area that wins over the
     * buildings underneath. A null picture draws a penciled stand-in.
     */
    public void addFigure(String label, Image image, Rectangle area, Runnable onClick) {
        figures.add(new Figure(image, area, label));
        hotspots.add(0, new Hotspot(label, new Rectangle(area), onClick, () -> false, () -> true));
    }

    /** Points the tutorial at an area with a blinking pencil circle and a line of text; null clears it. */
    public void setGuide(Rectangle area, String text) {
        guide = area;
        guideText = text;
        if (area != null && guideTimer == null) {
            guideTimer = new Timer(40, e -> repaint());
            guideTimer.start();
        } else if (area == null && guideTimer != null) {
            guideTimer.stop();
            guideTimer = null;
        }
        repaint();
    }

    /** Swaps the artwork (same size, so the hotspots still fit). A null picture keeps the current one. */
    public void setArt(Image next) {
        if (next == null || next == art) return;
        art = next;
        scaled = null;
        repaint();
    }

    /** Shows a short message over the map that fades out on its own. */
    public void toast(String text) {
        toastText = text;
        toastStart = System.currentTimeMillis();
        if (toastTimer == null) {
            toastTimer = new Timer(30, e -> {
                if (System.currentTimeMillis() - toastStart >= TOAST_DURATION) {
                    toastText = null;
                    toastTimer.stop();
                    toastTimer = null;
                }
                repaint();
            });
            toastTimer.start();
        }
        repaint();
    }

    private double scale() {
        if (art == null) return 1;
        return Math.min(getWidth() / (double) art.getWidth(null), getHeight() / (double) art.getHeight(null));
    }

    private int offsetX() {
        return art == null ? 0 : (int) Math.round((getWidth() - art.getWidth(null) * scale()) / 2);
    }

    /**
     * The map art's ruled lines (tools/pencil-art Backdrops.rule): from y=96, every 44 art pixels, light blue,
     * 2.2 wide. Screen coordinates of the first line at or below the top, the gap and the pen width, so the
     * side menu can rule its page on the very same lines.
     */
    static final int ART_RULE_TOP = 96, ART_RULE_GAP = 44;
    static final Color RULE_INK = new Color(110, 150, 205, 70);

    public double ruleGap() {
        return ART_RULE_GAP * scale();
    }

    public double ruleStart() {
        double gap = ruleGap(), y = offsetY() + ART_RULE_TOP * scale();
        return gap <= 0 ? 0 : ((y % gap) + gap) % gap;
    }

    public float ruleWidth() {
        return (float) Math.max(1, 2.2 * scale());
    }

    /**
     * A strip of the notebook page from x0 to x1: the paper with its faint grain and the ruled lines from
     * `start`, every `gap`. The map paints its margins with it and the side menu its whole width.
     */
    public static void paintPage(Graphics2D g, int x0, int x1, int h, double start, double gap, float width) {
        g.setColor(Sketch.PAPER);
        g.fillRect(x0, 0, x1 - x0, h);
        java.util.Random r = new java.util.Random(4242);
        int n = (x1 - x0) * h / 175;
        for (int i = 0; i < n; i++) {
            g.setColor(new Color(120, 110, 90, 10 + r.nextInt(16)));
            g.fillRect(x0 + r.nextInt(Math.max(1, x1 - x0)), r.nextInt(Math.max(1, h)), 1 + r.nextInt(3), 1);
        }
        if (gap < 4) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(width));
        g.setColor(RULE_INK);
        for (double y = start; y < h; y += gap) g.draw(new java.awt.geom.Line2D.Double(x0, y, x1, y));
    }

    private int offsetY() {
        return art == null ? 0 : (int) Math.round((getHeight() - art.getHeight(null) * scale()) / 2);
    }

    private Hotspot hotspotAt(Point p) {
        double s = scale();
        double ix = (p.x - offsetX()) / s;
        double iy = (p.y - offsetY()) / s;
        for (Hotspot h : hotspots) {
            if (h.shown.getAsBoolean() && h.area.contains(ix, iy)) return h;
        }
        return null;
    }

    private Rectangle toScreen(Rectangle r) {
        double s = scale();
        return new Rectangle(offsetX() + (int) Math.round(r.x * s), offsetY() + (int) Math.round(r.y * s),
                (int) Math.round(r.width * s), (int) Math.round(r.height * s));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Around the map art, the same page goes on: its ruled lines continue at the art's own height and gap.
        Graphics2D paper = (Graphics2D) g.create();
        paintPage(paper, 0, getWidth(), getHeight(), ruleStart(), ruleGap(), ruleWidth());
        paper.dispose();
        if (art == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        double s = scale();
        int dw = (int) Math.round(art.getWidth(null) * s);
        int dh = (int) Math.round(art.getHeight(null) * s);
        if (dw <= 0 || dh <= 0) {
            g2.dispose();
            return;
        }
        if (scaled == null || scaledForW != getWidth() || scaledForH != getHeight()) {
            scaled = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gs = scaled.createGraphics();
            gs.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            gs.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            gs.drawImage(art, 0, 0, dw, dh, null);
            gs.dispose();
            scaledForW = getWidth();
            scaledForH = getHeight();
        }
        g2.drawImage(scaled, offsetX(), offsetY(), null);

        for (Hotspot h : hotspots) {
            if (!h.shown.getAsBoolean() || !h.locked.getAsBoolean()) continue;
            Rectangle r = toScreen(h.area);
            drawCrossOut(g2, r, s);
        }

        for (Figure f : figures) drawFigure(g2, f);
        if (guide != null) drawGuide(g2, s);

        if (hovered != null && !hovered.shown.getAsBoolean()) hovered = null;
        if (hovered != null) {
            Rectangle r = toScreen(hovered.area);
            RoundRectangle2D shape = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 24, 24);
            g2.setColor(HOVER_FILL);
            g2.fill(shape);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[] {10f, 7f}, 0f));
            g2.setColor(HOVER_LINE);
            g2.draw(shape);
            if (mouse != null && game.save.Settings.isBuildingLabels()) {
                String label = hovered.locked.getAsBoolean() ? hovered.label + " (잠김)" : hovered.label;
                drawPill(g2, label, mouse.x + 16, mouse.y + 28, 1f, 15f);
            }
        }

        for (Object[] b : badges) {
            if (!((BooleanSupplier) b[1]).getAsBoolean()) continue;
            Rectangle r = toScreen((Rectangle) b[0]);
            double rad = Math.max(12, 20 * scale() * 1.6);
            QuestJournalDialog.paintBang(g2, r.getCenterX(), Math.max(rad + 2, r.y - rad * 0.4), rad, r.x * 31L + r.y);
        }

        if (toastText != null) {
            long elapsed = System.currentTimeMillis() - toastStart;
            float t = elapsed / (float) TOAST_DURATION;
            float alpha = t < 0.1f ? t / 0.1f : (t > 0.7f ? Math.max(0f, 1f - (t - 0.7f) / 0.3f) : 1f);
            Font font = Theme.dosFont(Font.BOLD, 20f);
            int tw = g2.getFontMetrics(font).stringWidth(toastText);
            drawPill(g2, toastText, (getWidth() - tw) / 2 - 18, getHeight() - 90, alpha, 20f);
        }
        g2.dispose();
    }

    private void drawFigure(Graphics2D g2, Figure f) {
        Rectangle r = toScreen(f.area);
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (f.image != null) {
            // Square art with the feet at the bottom: fit its height to the area, centered.
            int side = r.height;
            g.drawImage(f.image, r.x + (r.width - side) / 2, r.y, side, side, null);
        } else {
            double cx = r.getCenterX(), top = r.y + r.height * 0.1, head = r.width * 0.22;
            Sketch.circle(g, cx, top + head, head, f.name.hashCode(), 2f, Sketch.INK);
            Sketch.line(g, cx, top + head * 2, cx, r.y + r.height * 0.72, 1, 2f, Sketch.INK);
            Sketch.line(g, cx - r.width * 0.25, r.y + r.height * 0.45, cx + r.width * 0.25, r.y + r.height * 0.45, 2, 2f, Sketch.INK);
            Sketch.line(g, cx, r.y + r.height * 0.72, cx - r.width * 0.2, r.y + r.height, 3, 2f, Sketch.INK);
            Sketch.line(g, cx, r.y + r.height * 0.72, cx + r.width * 0.2, r.y + r.height, 4, 2f, Sketch.INK);
        }
        g.dispose();
    }

    /** The tutorial pointer: a red pencil ring that breathes in and out, an arrow above it, the goal on top. */
    private void drawGuide(Graphics2D g2, double s) {
        Rectangle r = toScreen(guide);
        double t = System.currentTimeMillis() / 1000.0, pulse = 1 + Math.sin(t * 4) * 0.05;
        double cx = r.getCenterX(), cy = r.getCenterY(), rx = r.width * 0.62 * pulse, ry = r.height * 0.62 * pulse;
        Graphics2D g = (Graphics2D) g2.create();
        g.setColor(new Color(255, 236, 160, 70));
        g.fill(new java.awt.geom.Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
        g.setColor(Sketch.RED_PENCIL);
        g.setStroke(new BasicStroke((float) Math.max(3, 6 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2));
        g.setStroke(new BasicStroke((float) Math.max(2, 3 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new java.awt.geom.Ellipse2D.Double(cx - rx * 1.04 + 3, cy - ry * 0.97, rx * 2.02, ry * 1.98));
        double bob = Math.sin(t * 5) * 8, ay = cy - ry - 18 + bob, size = Math.max(16, 34 * s);
        java.awt.geom.Path2D.Double arrow = new java.awt.geom.Path2D.Double();
        arrow.moveTo(cx, ay);
        arrow.lineTo(cx - size * 0.6, ay - size * 0.8);
        arrow.lineTo(cx - size * 0.22, ay - size * 0.8);
        arrow.lineTo(cx - size * 0.22, ay - size * 1.7);
        arrow.lineTo(cx + size * 0.22, ay - size * 1.7);
        arrow.lineTo(cx + size * 0.22, ay - size * 0.8);
        arrow.lineTo(cx + size * 0.6, ay - size * 0.8);
        arrow.closePath();
        g.setColor(new Color(244, 200, 50));
        g.fill(arrow);
        g.setColor(Sketch.INK);
        g.setStroke(new BasicStroke((float) Math.max(2, 3 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(arrow);
        g.dispose();
        if (guideText != null && !guideText.isEmpty()) {
            Font font = Theme.dosFont(Font.BOLD, 20f);
            int tw = g2.getFontMetrics(font).stringWidth(guideText);
            drawPill(g2, guideText, (getWidth() - tw) / 2 - 18, 16, 1f, 20f);
        }
    }

    /**
     * A locked region is crossed out on the page: a big penciled X from corner to corner, two quick
     * strokes each (one firm, one lighter), the way you'd strike something off a notebook map.
     * Seeded by the region, so the X keeps its exact shape on every repaint.
     */
    private static void drawCrossOut(Graphics2D g2, Rectangle r, double s) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double w = Math.max(4, 12 * s);
        double ix = r.width * 0.08, iy = r.height * 0.1;
        double x0 = r.x + ix, x1 = r.x + r.width - ix, y0 = r.y + iy, y1 = r.y + r.height - iy;
        java.util.Random rnd = new java.util.Random(r.x * 31L + r.y);
        graphite(g, x0, y0, x1, y1, w, rnd);
        graphite(g, x1, y0, x0, y1, w, rnd);
        g.dispose();
    }

    /**
     * One soft graphite stroke: several thin strands laid side by side, each broken into short
     * segments of uneven darkness, so it has the grain of a pencil dragged across paper rather than
     * a solid line. Strands start and stop a little apart, which tapers the ends.
     */
    private static void graphite(Graphics2D g, double x0, double y0, double x1, double y1, double w, java.util.Random rnd) {
        double len = Math.hypot(x1 - x0, y1 - y0);
        double dx = (x1 - x0) / len, dy = (y1 - y0) / len, nx = -dy, ny = dx;
        int strands = 7;
        double step = Math.max(3, w * 0.55);
        for (int k = 0; k < strands; k++) {
            double off = (k / (double) (strands - 1) - 0.5) * w + (rnd.nextDouble() - 0.5) * w * 0.25;
            double from = len * rnd.nextDouble() * 0.06, to = len * (1 - rnd.nextDouble() * 0.06);
            double phase = rnd.nextDouble() * 10, wob = w * 0.12;
            float thick = (float) (w * (0.2 + rnd.nextDouble() * 0.12));
            // Strands near the middle of the stroke are pressed harder than the edges.
            int base = (int) (125 - Math.abs(off) / w * 70);
            g.setStroke(new BasicStroke(thick, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double px = x0 + dx * from + nx * off, py = y0 + dy * from + ny * off;
            for (double t = from + step; t <= to; t += step) {
                double o = off + Math.sin(phase + t * 0.03) * wob;
                double qx = x0 + dx * t + nx * o, qy = y0 + dy * t + ny * o;
                int a = Math.max(24, Math.min(180, base + (int) ((rnd.nextDouble() - 0.5) * 70)));
                g.setColor(new Color(92, 90, 100, a));
                g.draw(new java.awt.geom.Line2D.Double(px, py, qx, qy));
                px = qx;
                py = qy;
            }
        }
    }
    /** A yellow sticky note in pencil with the text written on it; (x, y) is the note's top-left. */
    private void drawPill(Graphics2D g2, String text, int x, int y, float alpha, float size) {
        Font font = Sketch.hand(Font.BOLD, size);
        FontMetrics fm = g2.getFontMetrics(font);
        int padX = 18, padY = 10;
        int w = fm.stringWidth(text) + padX * 2;
        int h = fm.getHeight() + padY * 2;
        x = Math.max(8, Math.min(x, getWidth() - w - 8));
        y = Math.max(8, Math.min(y, getHeight() - h - 8));
        Graphics2D gp = (Graphics2D) g2.create();
        gp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        gp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gp.setColor(new Color(255, 244, 178, 245));
        gp.fill(new RoundRectangle2D.Float(x, y, w, h, 6, 6));
        Sketch.box(gp, x, y, w, h, text.hashCode(), 2.0f, Sketch.INK);
        gp.setFont(font);
        gp.setColor(Sketch.INK);
        gp.drawString(text, x + padX, y + padY + fm.getAscent());
        gp.dispose();
    }
}
