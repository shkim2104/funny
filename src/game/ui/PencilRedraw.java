package game.ui;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Redraws the rubbed-out part of a picture the way a pencil would. It finds what is missing by comparing
 * the graphite picture with the rubbed-out one, plans a pen path over that area (first along its dark
 * lines, then back and forth across the rest), and uncovers the picture only under the pen as it moves.
 */
final class PencilRedraw {
    private static final int SAMPLE = 2, LIFT_DISTANCE = 6, HATCH_GAP = 4, DARK_LINE = 125;
    private static final float LINE_WIDTH = 3.5f, HATCH_WIDTH = 6.5f;
    /** Share of the time spent on the lines; the hatching takes the rest. */
    private static final double LINE_SHARE = 0.55;

    /** One stop of the pen. */
    private static final class Stop {
        final float x, y, width;
        final boolean penDown;

        Stop(float x, float y, float width, boolean penDown) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.penDown = penDown;
        }
    }

    private final BufferedImage gray;
    private final BufferedImage mask;
    private final BufferedImage drawn;
    private final List<Stop> stops = new ArrayList<>();
    private final int lineStops;
    private int reached;

    PencilRedraw(Image grayPicture, Image rubbedPicture) {
        gray = copy(grayPicture);
        BufferedImage rubbed = copy(rubbedPicture);
        int w = gray.getWidth(), h = gray.getHeight();
        mask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        drawn = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        boolean[][] missing = new boolean[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int a = gray.getRGB(x, y) >>> 24;
                missing[x][y] = a > 40 && (rubbed.getRGB(x, y) >>> 24) < a / 2;
            }
        }
        planLines(missing);
        lineStops = stops.size();
        planHatching(missing);
    }

    private static BufferedImage copy(Image picture) {
        BufferedImage out = new BufferedImage(picture.getWidth(null), picture.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(picture, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Dark missing pixels, walked nearest-first, so the pen follows each line and lifts between them. */
    private void planLines(boolean[][] missing) {
        List<Point> left = new ArrayList<>();
        for (int x = 0; x < mask.getWidth(); x += SAMPLE) {
            for (int y = 0; y < mask.getHeight(); y += SAMPLE) {
                if (missing[x][y] && (gray.getRGB(x, y) & 255) < DARK_LINE) left.add(new Point(x, y));
            }
        }
        Point at = null;
        while (!left.isEmpty()) {
            Point next = left.get(0);
            if (at != null) {
                double best = Double.MAX_VALUE;
                for (Point p : left) {
                    double d = p.distanceSq(at);
                    if (d < best) {
                        best = d;
                        next = p;
                    }
                }
            }
            left.remove(next);
            boolean down = at != null && next.distance(at) <= LIFT_DISTANCE;
            stops.add(new Stop(next.x, next.y, LINE_WIDTH, down));
            at = next;
        }
    }

    /** Diagonal strokes across everything missing, each one going the opposite way to the last. */
    private void planHatching(boolean[][] missing) {
        int w = mask.getWidth(), h = mask.getHeight();
        boolean forward = true;
        for (int c = 0; c < w + h; c += HATCH_GAP) {
            List<Stop> stroke = new ArrayList<>();
            for (int x = Math.max(0, c - h + 1); x < w && x <= c; x += SAMPLE) {
                int y = c - x;
                if (y < h && missing[x][y]) stroke.add(new Stop(x, y, HATCH_WIDTH, !stroke.isEmpty()));
            }
            if (stroke.isEmpty()) continue;
            if (!forward) {
                List<Stop> back = new ArrayList<>();
                for (int i = stroke.size() - 1; i >= 0; i--) {
                    Stop s = stroke.get(i);
                    back.add(new Stop(s.x, s.y, s.width, i != stroke.size() - 1));
                }
                stroke = back;
            }
            stops.addAll(stroke);
            forward = !forward;
        }
    }

    /** Moves the pen to this point of the drawing (0~1), uncovering what it passes. */
    void advance(double progress) {
        int target;
        if (progress >= 1) target = stops.size();
        else if (progress < LINE_SHARE) target = (int) (lineStops * progress / LINE_SHARE);
        else target = lineStops + (int) ((stops.size() - lineStops) * (progress - LINE_SHARE) / (1 - LINE_SHARE));
        if (target <= reached) return;

        Graphics2D g = mask.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        for (int i = Math.max(1, reached); i < target; i++) {
            Stop a = stops.get(i - 1), b = stops.get(i);
            g.setStroke(new BasicStroke(b.width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(b.penDown ? new Line2D.Float(a.x, a.y, b.x, b.y) : new Line2D.Float(b.x, b.y, b.x, b.y));
        }
        g.dispose();
        reached = target;
    }

    /** The picture as far as the pen has drawn it. */
    Image drawn() {
        Graphics2D g = drawn.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(gray, 0, 0, null);
        g.setComposite(AlphaComposite.DstIn);
        g.drawImage(mask, 0, 0, null);
        g.dispose();
        return drawn;
    }

    /** Where the pen is, or null with nothing to draw. */
    Point2D tip() {
        if (stops.isEmpty()) return null;
        Stop s = stops.get(Math.min(reached, stops.size() - 1));
        return new Point2D.Float(s.x, s.y);
    }
}
