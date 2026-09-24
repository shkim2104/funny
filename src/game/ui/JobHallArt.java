package game.ui;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Draws the job advancement hall (전직소) onto the town artwork, in the same colored-pencil
 * notebook style: wobbly doubled ink outlines, hatched fills and a boxed hand-lettered label.
 * Coordinates are in town.png pixels; the result is baked into the image once at startup.
 */
final class JobHallArt {
    /** Clickable area around the hall and its label, in town.png pixels. */
    static final Rectangle AREA = new Rectangle(290, 1060, 310, 225);

    private static final Color INK = new Color(42, 48, 98);
    private static final Color WALL = new Color(232, 218, 190);
    private static final Color ROOF = new Color(128, 92, 176);
    private static final Color ROOF_DARK = new Color(88, 60, 130);
    private static final Color DOOR = new Color(150, 96, 56);
    private static final Color GLASS = new Color(150, 192, 230);
    private static final Color FLAG = new Color(214, 72, 60);
    private static final Color GOLD = new Color(230, 186, 70);

    private JobHallArt() {}

    /** Returns a copy of the town artwork with the hall drawn in. */
    static BufferedImage paintOnto(Image town) {
        if (town == null) return null;
        BufferedImage img = new BufferedImage(town.getWidth(null), town.getHeight(null), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(town, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        draw(g, new Random(7));   // fixed seed: the wobble looks hand-drawn but never changes
        g.dispose();
        return img;
    }

    private static void draw(Graphics2D g, Random r) {
        // Soft grass patch and shadow the building sits on.
        g.setColor(new Color(150, 190, 110, 90));
        g.fill(new Ellipse2D.Double(300, 1238, 260, 40));
        hatch(g, new Ellipse2D.Double(300, 1238, 260, 40), new Color(110, 160, 80, 110), 7, r);

        // Walls.
        Shape walls = new Rectangle2D.Double(335, 1168, 190, 92);
        fill(g, walls, WALL, new Color(190, 170, 140, 120), 9, r);
        // Stone courses.
        g.setColor(new Color(160, 138, 108, 150));
        g.setStroke(new BasicStroke(1.4f));
        for (int y = 1186; y < 1258; y += 16) {
            wobble(g, 337, y, 523, y, r, 1.2);
            for (int x = 345 + ((y / 16) % 2) * 14; x < 520; x += 30) wobble(g, x, y, x, y + 16, r, 0.8);
        }
        outline(g, walls, r);

        // Hip roof.
        Path2D roof = new Path2D.Double();
        roof.moveTo(318, 1172);
        roof.lineTo(542, 1172);
        roof.lineTo(494, 1112);
        roof.lineTo(366, 1112);
        roof.closePath();
        fill(g, roof, ROOF, ROOF_DARK, 7, r);
        // Shingle rows.
        g.setColor(new Color(70, 48, 110, 140));
        g.setStroke(new BasicStroke(1.3f));
        for (int y = 1126; y < 1170; y += 14) {
            double t = (y - 1112) / 60.0;
            wobble(g, 366 - 48 * t + 4, y, 494 + 48 * t - 4, y, r, 1.0);
        }
        outline(g, roof, r);

        // Flag on the ridge.
        g.setColor(INK);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        wobble(g, 430, 1112, 430, 1068, r, 0.8);
        Path2D flag = new Path2D.Double();
        flag.moveTo(431, 1069);
        flag.curveTo(448, 1066, 456, 1078, 472, 1074);
        flag.lineTo(466, 1086);
        flag.curveTo(452, 1090, 446, 1080, 431, 1084);
        flag.closePath();
        fill(g, flag, FLAG, new Color(160, 40, 34), 5, r);
        outline(g, flag, r);

        // Emblem over the door: a shield with crossed swords.
        Shape shield = shieldShape(430, 1178, 30, 34);
        fill(g, shield, GOLD, new Color(190, 140, 40), 5, r);
        outline(g, shield, r);
        g.setColor(INK);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        wobble(g, 420, 1184, 440, 1204, r, 0.6);
        wobble(g, 440, 1184, 420, 1204, r, 0.6);

        // Arched door.
        Path2D door = new Path2D.Double();
        door.moveTo(412, 1260);
        door.lineTo(412, 1228);
        door.quadTo(430, 1206, 448, 1228);
        door.lineTo(448, 1260);
        door.closePath();
        fill(g, door, DOOR, new Color(110, 66, 36), 5, r);
        outline(g, door, r);
        g.setColor(INK);
        g.fill(new Ellipse2D.Double(440, 1240, 4, 4));

        // Windows with cross bars.
        for (int wx : new int[] {356, 476}) {
            Shape win = new RoundRectangle2D.Double(wx, 1196, 30, 32, 10, 10);
            fill(g, win, GLASS, new Color(100, 150, 200), 6, r);
            outline(g, win, r);
            g.setColor(INK);
            g.setStroke(new BasicStroke(1.4f));
            wobble(g, wx + 15, 1197, wx + 15, 1227, r, 0.5);
            wobble(g, wx + 1, 1212, wx + 29, 1212, r, 0.5);
        }

        // Steps.
        g.setColor(new Color(200, 190, 175));
        g.fill(new Rectangle2D.Double(404, 1260, 52, 8));
        outline(g, new Rectangle2D.Double(404, 1260, 52, 8), r);

        drawLabel(g, r);
    }

    /** The boxed, hand-lettered "전직소" sign with an arrow, matching the other labels on the map. */
    private static void drawLabel(Graphics2D g, Random r) {
        Font font = new Font("맑은 고딕", Font.BOLD, 30);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        String text = "전직소";
        int tx = 488, ty = 1104;
        int tw = fm.stringWidth(text);
        Rectangle2D box = new Rectangle2D.Double(tx - 12, ty - fm.getAscent() - 4, tw + 24, fm.getAscent() + 16);
        g.setColor(new Color(246, 240, 226, 230));
        g.fill(box);
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(INK);
        outline(g, box, r);
        // Second, slightly offset box like the doubled pen strokes on the other signs.
        g.setStroke(new BasicStroke(1.2f));
        wobble(g, box.getMinX() + 4, box.getMaxY() + 3, box.getMaxX() + 3, box.getMaxY() + 3, r, 0.8);
        wobble(g, box.getMaxX() + 3, box.getMinY() + 4, box.getMaxX() + 3, box.getMaxY() + 3, r, 0.8);
        g.setColor(INK);
        g.drawString(text, tx, ty);
        // Arrow pointing down-left at the roof.
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(box.getMinX() + 10, box.getMaxY() + 2);
        arrow.quadTo(box.getMinX() - 6, box.getMaxY() + 12, box.getMinX() - 16, box.getMaxY() + 26);
        g.draw(arrow);
        double ax = box.getMinX() - 16, ay = box.getMaxY() + 26;
        wobble(g, ax, ay, ax + 1, ay - 11, r, 0.4);
        wobble(g, ax, ay, ax + 11, ay - 3, r, 0.4);
    }

    private static Shape shieldShape(double cx, double top, double w, double h) {
        Path2D p = new Path2D.Double();
        p.moveTo(cx - w / 2, top);
        p.lineTo(cx + w / 2, top);
        p.lineTo(cx + w / 2, top + h * 0.45);
        p.quadTo(cx + w / 2, top + h * 0.85, cx, top + h);
        p.quadTo(cx - w / 2, top + h * 0.85, cx - w / 2, top + h * 0.45);
        p.closePath();
        return p;
    }

    /** Flat colored-pencil fill plus diagonal hatching in a darker tone. */
    private static void fill(Graphics2D g, Shape s, Color base, Color hatchColor, int spacing, Random r) {
        g.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 225));
        g.fill(s);
        hatch(g, s, hatchColor, spacing, r);
    }

    private static void hatch(Graphics2D g, Shape s, Color c, int spacing, Random r) {
        Shape oldClip = g.getClip();
        g.clip(s);
        Rectangle b = s.getBounds();
        g.setColor(c);
        g.setStroke(new BasicStroke(1.1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int x = b.x - b.height; x < b.x + b.width; x += spacing) {
            wobble(g, x, b.y + b.height, x + b.height, b.y, r, 1.2);
        }
        g.setClip(oldClip);
    }

    /** Ink outline drawn twice with a little jitter, like a quick pen sketch. */
    private static void outline(Graphics2D g, Shape s, Random r) {
        g.setColor(INK);
        for (int pass = 0; pass < 2; pass++) {
            g.setStroke(new BasicStroke(pass == 0 ? 2.2f : 1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double dx = (r.nextDouble() - 0.5) * 2.2, dy = (r.nextDouble() - 0.5) * 2.2;
            g.draw(AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(s));
        }
    }

    /** A slightly bowed line so strokes don't look ruler-straight. */
    private static void wobble(Graphics2D g, double x1, double y1, double x2, double y2, Random r, double amount) {
        double mx = (x1 + x2) / 2 + (r.nextDouble() - 0.5) * 2 * amount;
        double my = (y1 + y2) / 2 + (r.nextDouble() - 0.5) * 2 * amount;
        g.draw(new QuadCurve2D.Double(x1, y1, mx, my, x2, y2));
    }
}
