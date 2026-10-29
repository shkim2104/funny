import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pencil-on-notebook drawing tools: wobbly hand-drawn outlines (two overlapping passes that overshoot a
 * little), hatching for shade (denser and cross-hatched as it gets darker), graphite smudges and a
 * colored-pencil accent. Everything is seeded, so re-running the generator redraws the same picture.
 *
 * Widths and spacings are in "pencil units" multiplied by {@code scale}, so the same drawing code reads
 * the same whether the canvas is a 700px monster or a 1672px backdrop.
 */
final class Pencil {
    static final Color LEAD = new Color(50, 48, 58);
    static final Color PAPER = new Color(247, 243, 230);

    final Graphics2D g;
    final Random r;
    final double scale;
    /** When false, parts are not backed with paper (backdrops: the page itself is already there). */
    boolean paperBacked = true;

    Pencil(BufferedImage img, long seed, double scale) {
        this.g = img.createGraphics();
        this.r = new Random(seed);
        this.scale = scale;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    void dispose() {
        g.dispose();
    }

    // ---------- the main verbs ----------

    /** A solid drawn object: paper underneath (so it hides what's behind), shading, then its outline. */
    void part(Shape s, double tone) {
        if (paperBacked) {
            g.setColor(PAPER);
            g.fill(s);
        }
        shade(s, tone);
        outline(s, 3.0);
    }

    /** Like part(), plus a colored-pencil layer over the graphite. */
    void part(Shape s, double tone, Color accent, double strength) {
        if (paperBacked) {
            g.setColor(PAPER);
            g.fill(s);
        }
        if (accent != null && strength > 0) tint(s, accent, strength);
        shade(s, tone);
        outline(s, 3.0);
    }

    /**
     * Graphite value inside a shape: a soft smudge, one or two layers of hatching, and an extra
     * cross-hatch on the side away from the light (bottom-right), so things read as round.
     */
    void shade(Shape s, double tone) {
        if (tone <= 0.02) return;
        g.setColor(alpha(LEAD, (int) (tone * 55)));
        g.fill(s);
        double a = 38 + r.nextDouble() * 14;
        hatch(s, Math.min(1, tone * 0.9), a, LEAD);
        if (tone > 0.5) hatch(s, (tone - 0.5) * 1.4, a - 78, LEAD);
        Rectangle2D b = s.getBounds2D();
        Area shadow = new Area(s);
        shadow.subtract(new Area(AffineTransform.getTranslateInstance(-b.getWidth() * 0.18, -b.getHeight() * 0.2)
                .createTransformedShape(s)));
        hatch(shadow, Math.min(1, tone * 0.6 + 0.3), a - 70, LEAD);
    }

    /** Colored pencil: a light wash of the color and a loose hatch in it. */
    void tint(Shape s, Color c, double strength) {
        g.setColor(alpha(c, (int) (60 * strength)));
        g.fill(s);
        hatch(s, 0.45 * strength + 0.2, -20 + r.nextDouble() * 20, c);
    }

    /** Parallel wobbly strokes clipped to the shape, spaced tighter as tone rises. */
    void hatch(Shape s, double tone, double angleDeg, Color c) {
        if (tone <= 0) return;
        Shape oldClip = g.getClip();
        g.clip(s);
        Rectangle2D b = s.getBounds2D();
        double spacing = (17 - 11 * Math.min(1, tone)) * scale;
        double ang = Math.toRadians(angleDeg);
        double dx = Math.cos(ang), dy = Math.sin(ang);
        double nx = -dy, ny = dx;
        double cx = b.getCenterX(), cy = b.getCenterY();
        double half = Math.hypot(b.getWidth(), b.getHeight()) / 2 + 4;
        int alpha = (int) Math.min(220, 50 + 140 * tone);
        for (double t = -half; t <= half; t += spacing * (0.8 + 0.4 * r.nextDouble())) {
            // Each stroke starts and stops a little randomly, like a quick hand.
            double s0 = -half + r.nextDouble() * 6 * scale, s1 = half - r.nextDouble() * 6 * scale;
            line(cx + nx * t + dx * s0, cy + ny * t + dy * s0, cx + nx * t + dx * s1, cy + ny * t + dy * s1,
                    1.5, alpha(c, alpha));
        }
        g.setClip(oldClip);
    }

    /** A hand-drawn outline: one firm pass and one lighter pass, each slightly off the true edge. */
    void outline(Shape s, double width) {
        for (List<Point2D> sub : subpaths(s, 7 * scale)) {
            boolean closed = sub.size() > 2 && sub.get(0).distance(sub.get(sub.size() - 1)) < 2 * scale;
            List<Point2D> pts = new ArrayList<>(sub);
            if (closed && pts.size() > 3) {
                // Overshoot past the start so the closing joint looks drawn, not computed.
                pts.add(pts.get(1));
                pts.add(pts.get(2));
            }
            stroke(pts, width, alpha(LEAD, 215), 1.3);
            stroke(pts, width * 0.55, alpha(LEAD, 120), 2.2);
        }
    }

    /** A single wobbly pencil line. */
    void line(double x1, double y1, double x2, double y2, double width, Color c) {
        double len = Math.hypot(x2 - x1, y2 - y1);
        int n = Math.max(2, (int) (len / (14 * scale)));
        List<Point2D> pts = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double t = i / (double) n;
            pts.add(new Point2D.Double(x1 + (x2 - x1) * t, y1 + (y2 - y1) * t));
        }
        stroke(pts, width, c, 0.9);
    }

    /** A firm graphite line (details: cracks, bones, strings). */
    void ink(double x1, double y1, double x2, double y2, double width) {
        line(x1, y1, x2, y2, width, alpha(LEAD, 210));
        line(x1, y1, x2, y2, width * 0.5, alpha(LEAD, 110));
    }

    /** An open smooth curve through the points, drawn as a pencil line. */
    void curve(double width, double... xy) {
        outlineOpen(spline(xy), width);
    }

    void outlineOpen(Shape s, double width) {
        for (List<Point2D> sub : subpaths(s, 7 * scale)) {
            stroke(sub, width, alpha(LEAD, 215), 1.2);
            stroke(sub, width * 0.5, alpha(LEAD, 110), 2.0);
        }
    }

    /** A colored dot (eyes, glints), with a dark rim. */
    void dot(double x, double y, double rad, Color c) {
        Shape e = ell(x, y, rad, rad);
        g.setColor(c);
        g.fill(e);
        outline(e, 1.8);
    }

    /** A colored-pencil line (lava cracks, lightning, glows). */
    void colorLine(double width, Color c, double... xy) {
        List<Point2D> pts = new ArrayList<>();
        for (int i = 0; i + 1 < xy.length; i += 2) pts.add(new Point2D.Double(xy[i], xy[i + 1]));
        List<Point2D> dense = resample(pts, 10 * scale);
        stroke(dense, width * 1.8, alpha(c, 90), 1.0);
        stroke(dense, width, alpha(c, 230), 0.8);
    }

    /** Hand-lettered text: written twice slightly offset (pressed pencil), optionally tilted. */
    void text(String s, double x, double y, float size, Color c, double tiltDeg) {
        Graphics2D t = (Graphics2D) g.create();
        t.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        t.rotate(Math.toRadians(tiltDeg), x, y);
        t.setFont(new Font("맑은 고딕", Font.BOLD, Math.round(size)));
        t.setColor(alpha(c, 120));
        t.drawString(s, (float) x + 1.2f, (float) y + 0.8f);
        t.setColor(alpha(c, 225));
        t.drawString(s, (float) x, (float) y);
        t.dispose();
    }

    /** Text width for the same font text() uses. */
    double textWidth(String s, float size) {
        return g.getFontMetrics(new Font("맑은 고딕", Font.BOLD, Math.round(size))).stringWidth(s);
    }

    /** A dotted trail through the points (map routes). */
    void dots(double spacing, double rad, Color c, double... xy) {
        List<Point2D> pts = new ArrayList<>();
        for (int i = 0; i + 1 < xy.length; i += 2) pts.add(new Point2D.Double(xy[i], xy[i + 1]));
        for (List<Point2D> sub : subpaths(spline(xy), spacing)) {
            for (Point2D q : sub) {
                g.setColor(alpha(c, 200));
                g.fill(ell(q.getX(), q.getY(), rad, rad));
            }
        }
    }

    /** Soft graphite rubbed into the page around a shape (shadows, glows). */
    void smudge(Shape s, Color c, int alpha) {
        g.setColor(alpha(c, alpha));
        g.fill(s);
    }

    // ---------- stroke engine ----------

    private void stroke(List<Point2D> pts, double width, Color c, double wobble) {
        if (pts.size() < 2) return;
        double amp = wobble * scale;
        double phase = r.nextDouble() * 100, freq = 0.25 + r.nextDouble() * 0.2;
        double driftX = (r.nextDouble() - 0.5) * 2 * amp, driftY = (r.nextDouble() - 0.5) * 2 * amp;
        Path2D path = new Path2D.Double();
        for (int i = 0; i < pts.size(); i++) {
            Point2D p = pts.get(i);
            double ox = driftX + amp * Math.sin(i * freq + phase) + (r.nextDouble() - 0.5) * amp * 0.5;
            double oy = driftY + amp * Math.cos(i * freq * 1.3 + phase) + (r.nextDouble() - 0.5) * amp * 0.5;
            if (i == 0) path.moveTo(p.getX() + ox, p.getY() + oy);
            else path.lineTo(p.getX() + ox, p.getY() + oy);
        }
        g.setStroke(new BasicStroke((float) (width * scale * (0.85 + 0.3 * r.nextDouble())),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(c);
        g.draw(path);
    }

    private static List<List<Point2D>> subpaths(Shape s, double step) {
        List<List<Point2D>> out = new ArrayList<>();
        List<Point2D> cur = null;
        double[] c = new double[6];
        for (PathIterator it = new FlatteningPathIterator(s.getPathIterator(null), 0.6); !it.isDone(); it.next()) {
            int type = it.currentSegment(c);
            if (type == PathIterator.SEG_MOVETO) {
                if (cur != null && cur.size() > 1) out.add(resample(cur, step));
                cur = new ArrayList<>();
                cur.add(new Point2D.Double(c[0], c[1]));
            } else if (type == PathIterator.SEG_LINETO) {
                if (cur == null) cur = new ArrayList<>();
                cur.add(new Point2D.Double(c[0], c[1]));
            } else if (type == PathIterator.SEG_CLOSE && cur != null && !cur.isEmpty()) {
                cur.add(cur.get(0));
            }
        }
        if (cur != null && cur.size() > 1) out.add(resample(cur, step));
        return out;
    }

    private static List<Point2D> resample(List<Point2D> pts, double step) {
        List<Point2D> out = new ArrayList<>();
        out.add(pts.get(0));
        double carry = 0;
        for (int i = 1; i < pts.size(); i++) {
            Point2D a = pts.get(i - 1), b = pts.get(i);
            double len = a.distance(b);
            double t = step - carry;
            while (t < len) {
                out.add(new Point2D.Double(a.getX() + (b.getX() - a.getX()) * t / len, a.getY() + (b.getY() - a.getY()) * t / len));
                t += step;
            }
            carry = len - (t - step);
        }
        out.add(pts.get(pts.size() - 1));
        return out;
    }

    // ---------- shapes ----------

    static Shape ell(double cx, double cy, double rx, double ry) {
        return new Ellipse2D.Double(cx - rx, cy - ry, rx * 2, ry * 2);
    }

    static Shape poly(double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i + 1 < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        p.closePath();
        return p;
    }

    /** A smooth closed shape through the points (Catmull-Rom). */
    static Shape blob(double... xy) {
        int n = xy.length / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 0; i < n; i++) {
            int i0 = (i - 1 + n) % n, i1 = i, i2 = (i + 1) % n, i3 = (i + 2) % n;
            p.curveTo(xy[i1 * 2] + (xy[i2 * 2] - xy[i0 * 2]) / 6, xy[i1 * 2 + 1] + (xy[i2 * 2 + 1] - xy[i0 * 2 + 1]) / 6,
                    xy[i2 * 2] - (xy[i3 * 2] - xy[i1 * 2]) / 6, xy[i2 * 2 + 1] - (xy[i3 * 2 + 1] - xy[i1 * 2 + 1]) / 6,
                    xy[i2 * 2], xy[i2 * 2 + 1]);
        }
        p.closePath();
        return p;
    }

    /** A smooth open curve through the points. */
    static Shape spline(double... xy) {
        int n = xy.length / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 0; i < n - 1; i++) {
            int i0 = Math.max(0, i - 1), i1 = i, i2 = i + 1, i3 = Math.min(n - 1, i + 2);
            p.curveTo(xy[i1 * 2] + (xy[i2 * 2] - xy[i0 * 2]) / 6, xy[i1 * 2 + 1] + (xy[i2 * 2 + 1] - xy[i0 * 2 + 1]) / 6,
                    xy[i2 * 2] - (xy[i3 * 2] - xy[i1 * 2]) / 6, xy[i2 * 2 + 1] - (xy[i3 * 2 + 1] - xy[i1 * 2 + 1]) / 6,
                    xy[i2 * 2], xy[i2 * 2 + 1]);
        }
        return p;
    }

    /** An arm, leg or tail: a fat rounded stroke along the points. */
    static Shape limb(double width, double... xy) {
        Path2D p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i + 1 < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
        return new BasicStroke((float) width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND).createStrokedShape(p);
    }

    /** A lumpy rock: a blob with n jittered points around (cx, cy). */
    static Shape rock(Random r, double cx, double cy, double rx, double ry, int n) {
        double[] xy = new double[n * 2];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n, k = 0.75 + r.nextDouble() * 0.35;
            xy[i * 2] = cx + Math.cos(a) * rx * k;
            xy[i * 2 + 1] = cy + Math.sin(a) * ry * k * (Math.sin(a) > 0 ? 0.7 : 1);
        }
        return blob(xy);
    }

    /** A flame / crystal spike: a pointed teardrop from (bx,by) up to the tip. */
    static Shape flame(double bx, double by, double w, double h, double lean) {
        return blob(bx - w / 2, by, bx - w * 0.35 + lean * 0.3, by - h * 0.5, bx + lean, by - h,
                bx + w * 0.35 + lean * 0.3, by - h * 0.5, bx + w / 2, by, bx, by + w * 0.25);
    }

    /** The shape mirrored across the vertical line x = cx. */
    static Shape mirror(Shape s, double cx) {
        AffineTransform t = new AffineTransform(-1, 0, 0, 1, 2 * cx, 0);
        return t.createTransformedShape(s);
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
}
