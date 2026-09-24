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
    private static final Color LOCKED_FILL = new Color(20, 20, 30, 85);

    private static final class Hotspot {
        final String label;
        final Rectangle area;
        final Runnable onClick;
        final BooleanSupplier locked;

        Hotspot(String label, Rectangle area, Runnable onClick, BooleanSupplier locked) {
            this.label = label;
            this.area = area;
            this.onClick = onClick;
            this.locked = locked;
        }
    }

    private final Image art;
    private final List<Hotspot> hotspots = new ArrayList<>();
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
        setBackground(Theme.BG);

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

    /** Adds a clickable area that is drawn dimmed while locked returns true (it still receives clicks). */
    public void addHotspot(String label, int x, int y, int w, int h, Runnable onClick, BooleanSupplier locked) {
        hotspots.add(new Hotspot(label, new Rectangle(x, y, w, h), onClick, locked));
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

    private int offsetY() {
        return art == null ? 0 : (int) Math.round((getHeight() - art.getHeight(null) * scale()) / 2);
    }

    private Hotspot hotspotAt(Point p) {
        double s = scale();
        double ix = (p.x - offsetX()) / s;
        double iy = (p.y - offsetY()) / s;
        for (Hotspot h : hotspots) {
            if (h.area.contains(ix, iy)) return h;
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
            if (!h.locked.getAsBoolean()) continue;
            Rectangle r = toScreen(h.area);
            g2.setColor(LOCKED_FILL);
            g2.fill(new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 24, 24));
            drawChainedLock(g2, r, s);
        }

        if (hovered != null) {
            Rectangle r = toScreen(hovered.area);
            RoundRectangle2D shape = new RoundRectangle2D.Float(r.x, r.y, r.width, r.height, 24, 24);
            g2.setColor(HOVER_FILL);
            g2.fill(shape);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    10f, new float[] {10f, 7f}, 0f));
            g2.setColor(HOVER_LINE);
            g2.draw(shape);
            if (mouse != null) {
                String label = hovered.locked.getAsBoolean() ? hovered.label + " (잠김)" : hovered.label;
                drawPill(g2, label, mouse.x + 16, mouse.y + 28, 1f, 15f);
            }
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

    /**
     * Two sagging chains crossed in an X over the region, as if wrapped around it, with a padlock
     * hanging where they meet. Sizes follow the map scale so every region's chains look the same.
     */
    private static void drawChainedLock(Graphics2D g2, Rectangle r, double s) {
        float link = (float) Math.max(7, 22 * s);
        float insetX = r.width * 0.04f, insetY = r.height * 0.06f;
        float left = r.x + insetX, right = r.x + r.width - insetX;
        float top = r.y + insetY, bottom = r.y + r.height - insetY;
        float sag = r.height * 0.14f;
        float cx = r.x + r.width / 2f, cy = r.y + r.height / 2f;

        // Both curves share the control point, so they cross exactly at their midpoints (t = 0.5).
        java.awt.geom.QuadCurve2D.Float a = new java.awt.geom.QuadCurve2D.Float(left, top, cx, cy + sag, right, bottom);
        java.awt.geom.QuadCurve2D.Float b = new java.awt.geom.QuadCurve2D.Float(right, top, cx, cy + sag, left, bottom);
        drawChain(g2, a, link);
        drawChain(g2, b, link);

        float lockX = cx, lockY = cy + sag / 2f;
        drawPadlock(g2, lockX, lockY, (float) Math.max(12, 34 * s));
    }

    /** Lays chain links along a curve, alternating face-on rings and edge-on links like a real chain. */
    private static void drawChain(Graphics2D g2, Shape path, float link) {
        // Flatten the curve into a polyline and walk it by arc length.
        List<float[]> pts = new ArrayList<>();
        java.awt.geom.PathIterator it = path.getPathIterator(null, 0.5);
        float[] c = new float[6];
        while (!it.isDone()) {
            it.currentSegment(c);
            pts.add(new float[] {c[0], c[1]});
            it.next();
        }
        float step = link * 0.82f;
        float carried = 0;
        int index = 0;
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = 1; i < pts.size(); i++) {
            float x0 = pts.get(i - 1)[0], y0 = pts.get(i - 1)[1];
            float dx = pts.get(i)[0] - x0, dy = pts.get(i)[1] - y0;
            float seg = (float) Math.hypot(dx, dy);
            if (seg == 0) continue;
            double angle = Math.atan2(dy, dx);
            float d = step - carried;
            while (d <= seg) {
                drawLink(g, x0 + dx * d / seg, y0 + dy * d / seg, angle, link, index++ % 2 == 0);
                d += step;
            }
            carried = seg - (d - step);
        }
        g.dispose();
    }

    private static void drawLink(Graphics2D g2, float x, float y, double angle, float link, boolean faceOn) {
        Graphics2D g = (Graphics2D) g2.create();
        g.translate(x, y);
        g.rotate(angle);
        Color ink = new Color(34, 30, 28);
        float len = link * 1.25f;
        if (faceOn) {
            // Ring seen from the front: an oval band with a hole.
            float h = link * 0.78f;
            float band = Math.max(1.8f, link * 0.2f);
            java.awt.geom.Ellipse2D.Float ring = new java.awt.geom.Ellipse2D.Float(-len / 2, -h / 2, len, h);
            g.setStroke(new BasicStroke(band + 2.4f));
            g.setColor(ink);
            g.draw(ring);
            g.setStroke(new BasicStroke(band));
            g.setPaint(new GradientPaint(0, -h / 2, new Color(196, 200, 206), 0, h / 2, new Color(96, 100, 110)));
            g.draw(ring);
        } else {
            // Ring seen edge-on: a thin rounded bar.
            float t = Math.max(3f, link * 0.3f);
            RoundRectangle2D bar = new RoundRectangle2D.Float(-len / 2, -t / 2, len, t, t, t);
            g.setPaint(new GradientPaint(0, -t / 2, new Color(210, 214, 220), 0, t / 2, new Color(90, 94, 104)));
            g.fill(bar);
            g.setStroke(new BasicStroke(1.2f));
            g.setColor(ink);
            g.draw(bar);
        }
        g.dispose();
    }

    /**
     * An iron padlock centered at (cx, cy); u is the unit size (body half-width). Outlined in dark
     * ink with slightly heavy strokes so it sits naturally on the hand-drawn map.
     */
    private static void drawPadlock(Graphics2D g2, float cx, float cy, float u) {
        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color ink = new Color(40, 32, 26);
        float bodyW = u * 2f, bodyH = u * 1.6f;
        float bodyX = cx - u, bodyY = cy - bodyH * 0.2f;
        float line = Math.max(2f, u * 0.14f);

        // Soft drop shadow so the lock lifts off the dimmed map.
        g.setColor(new Color(0, 0, 0, 90));
        g.fill(new RoundRectangle2D.Float(bodyX + line, bodyY + line * 1.5f, bodyW, bodyH, u * 0.5f, u * 0.5f));

        // Shackle: a thick U above the body, drawn as an outlined band.
        float shW = u * 1.3f, shTop = bodyY - u * 1.25f;
        java.awt.geom.Path2D.Float shackle = new java.awt.geom.Path2D.Float();
        shackle.moveTo(cx - shW / 2, bodyY + line);
        shackle.lineTo(cx - shW / 2, shTop + shW / 2);
        shackle.append(new java.awt.geom.Arc2D.Float(cx - shW / 2, shTop, shW, shW, 180, -180, java.awt.geom.Arc2D.OPEN), true);
        shackle.lineTo(cx + shW / 2, bodyY + line);
        g.setStroke(new BasicStroke(u * 0.42f + line * 2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(ink);
        g.draw(shackle);
        g.setStroke(new BasicStroke(u * 0.42f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(176, 182, 190));
        g.draw(shackle);

        // Body: brass plate with a vertical gradient.
        RoundRectangle2D body = new RoundRectangle2D.Float(bodyX, bodyY, bodyW, bodyH, u * 0.5f, u * 0.5f);
        g.setPaint(new GradientPaint(0, bodyY, new Color(236, 196, 92), 0, bodyY + bodyH, new Color(168, 116, 34)));
        g.fill(body);
        g.setStroke(new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(ink);
        g.draw(body);
        // Top-edge highlight.
        g.setColor(new Color(255, 240, 190, 170));
        g.fill(new RoundRectangle2D.Float(bodyX + line * 1.5f, bodyY + line * 1.2f, bodyW - line * 3f, u * 0.22f, u * 0.2f, u * 0.2f));

        // Keyhole: a circle plus a tapered slot.
        float kr = u * 0.26f, ky = bodyY + bodyH * 0.42f;
        g.setColor(ink);
        g.fill(new java.awt.geom.Ellipse2D.Float(cx - kr, ky - kr, kr * 2, kr * 2));
        java.awt.geom.Path2D.Float slot = new java.awt.geom.Path2D.Float();
        slot.moveTo(cx - kr * 0.45f, ky);
        slot.lineTo(cx + kr * 0.45f, ky);
        slot.lineTo(cx + kr * 0.8f, ky + bodyH * 0.36f);
        slot.lineTo(cx - kr * 0.8f, ky + bodyH * 0.36f);
        slot.closePath();
        g.fill(slot);
        g.dispose();
    }

    /** Dark translucent rounded label with white text; (x, y) is the pill's top-left. */
    private void drawPill(Graphics2D g2, String text, int x, int y, float alpha, float size) {
        Font font = Theme.dosFont(Font.BOLD, size);
        FontMetrics fm = g2.getFontMetrics(font);
        int padX = 18, padY = 10;
        int w = fm.stringWidth(text) + padX * 2;
        int h = fm.getHeight() + padY * 2;
        x = Math.max(8, Math.min(x, getWidth() - w - 8));
        y = Math.max(8, Math.min(y, getHeight() - h - 8));
        Graphics2D gp = (Graphics2D) g2.create();
        gp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        gp.setColor(new Color(16, 14, 12, 200));
        gp.fill(new RoundRectangle2D.Float(x, y, w, h, 16, 16));
        gp.setFont(font);
        gp.setColor(Color.WHITE);
        gp.drawString(text, x + padX, y + padY + fm.getAscent());
        gp.dispose();
    }
}
