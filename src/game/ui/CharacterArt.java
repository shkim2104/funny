package game.ui;

import game.save.SaveManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;

/**
 * A character the player drew themselves, part by part: head, body, one arm and one leg (the other
 * arm and leg are the same drawing mirrored). Parts are assembled on a FIG_W x FIG_H figure with the
 * feet on FOOT_Y, and arms/legs swing around their shoulder/hip so the battle can animate them.
 * Saved as PNGs next to the character's save file.
 */
public final class CharacterArt {
    public enum Part {
        HEAD("머리", 200, 200, "얼굴까지 그려 주세요!"),
        BODY("몸통", 200, 240, "옷이나 갑옷을 그려도 좋아요."),
        ARM("팔", 90, 220, "왼팔만 그리면 오른팔은 똑같이 붙어요."),
        LEG("다리", 100, 220, "왼다리만 그리면 오른다리는 똑같이 붙어요.");

        public final String label;
        public final int w, h;
        public final String tip;

        Part(String label, int w, int h, String tip) {
            this.label = label;
            this.w = w;
            this.h = h;
            this.tip = tip;
        }

        /** Arms and legs come in mirrored pairs. */
        public boolean paired() {
            return this == ARM || this == LEG;
        }
    }

    public static final int FIG_W = 400, FIG_H = 660, FOOT_Y = 650;
    private static final int SHOULDER_X = 105, SHOULDER_Y = 228, HIP_X = 155, HIP_Y = 430;

    private final BufferedImage[] parts = new BufferedImage[Part.values().length];
    private BufferedImage flat;

    private CharacterArt() {
        for (Part p : Part.values()) parts[p.ordinal()] = new BufferedImage(p.w, p.h, BufferedImage.TYPE_INT_ARGB);
    }

    public static CharacterArt blank() {
        return new CharacterArt();
    }

    public BufferedImage image(Part p) {
        return parts[p.ordinal()];
    }

    /** Call after drawing into a part so the assembled preview is rebuilt. */
    public void changed() {
        flat = null;
        shrunkCache = null;
    }

    public boolean isEmpty(Part p) {
        BufferedImage img = image(p);
        for (int y = 0; y < img.getHeight(); y += 2) {
            for (int x = 0; x < img.getWidth(); x += 2) if ((img.getRGB(x, y) >>> 24) > 30) return false;
        }
        return true;
    }

    public CharacterArt copy() {
        CharacterArt c = new CharacterArt();
        for (Part p : Part.values()) {
            Graphics2D g = c.image(p).createGraphics();
            g.drawImage(image(p), 0, 0, null);
            g.dispose();
        }
        return c;
    }

    /** Fills any part left empty with the default doodle's, so a character is never missing a limb. */
    public void fillEmptyFromDefault() {
        CharacterArt d = defaultDoodle();
        for (Part p : Part.values()) {
            if (!isEmpty(p)) continue;
            Graphics2D g = image(p).createGraphics();
            g.drawImage(d.image(p), 0, 0, null);
            g.dispose();
        }
        changed();
    }

    // ---------- layout ----------

    /** Top-left of a part on the figure; right = the mirrored copy of an arm or leg. */
    public static Point origin(Part p, boolean right) {
        switch (p) {
            case HEAD: return new Point((FIG_W - p.w) / 2, 10);
            case BODY: return new Point((FIG_W - p.w) / 2, 210);
            case ARM: {
                int x = SHOULDER_X - p.w / 2;
                return new Point(right ? FIG_W - x - p.w : x, SHOULDER_Y - 14);
            }
            default: {
                int x = HIP_X - p.w / 2;
                return new Point(right ? FIG_W - x - p.w : x, HIP_Y);
            }
        }
    }

    /**
     * Paints the assembled figure in figure coordinates (caller sets the transform). Angles in degrees:
     * positive swings a limb outward/forward.
     */
    public void paintFigure(Graphics2D g, double armL, double armR, double legL, double legR, float[] alphaPerPart) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        limb(g2, Part.LEG, false, legL, alpha(alphaPerPart, Part.LEG));
        limb(g2, Part.LEG, true, legR, alpha(alphaPerPart, Part.LEG));
        place(g2, Part.BODY, alpha(alphaPerPart, Part.BODY));
        limb(g2, Part.ARM, false, armL, alpha(alphaPerPart, Part.ARM));
        limb(g2, Part.ARM, true, armR, alpha(alphaPerPart, Part.ARM));
        place(g2, Part.HEAD, alpha(alphaPerPart, Part.HEAD));
        g2.dispose();
    }

    private static float alpha(float[] a, Part p) {
        return a == null ? 1f : a[p.ordinal()];
    }

    private void place(Graphics2D g, Part p, float alpha) {
        if (alpha <= 0) return;
        Point o = origin(p, false);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g.drawImage(image(p), o.x, o.y, null);
        g.setComposite(old);
    }

    private void limb(Graphics2D g, Part p, boolean right, double angle, float alpha) {
        if (alpha <= 0) return;
        Point o = origin(p, right);
        Graphics2D gl = (Graphics2D) g.create();
        gl.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        // Swing around the top-center joint; the left limb swings outward with -angle, the right with +angle.
        gl.rotate(Math.toRadians(right ? -angle : angle), o.x + p.w / 2.0, o.y + 8);
        if (right) gl.drawImage(image(p), o.x + p.w, o.y, -p.w, p.h, null);
        else gl.drawImage(image(p), o.x, o.y, null);
        gl.dispose();
    }

    /** The figure standing still, cached (character cards). */
    public BufferedImage flat() {
        if (flat == null) {
            flat = new BufferedImage(FIG_W, FIG_H, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = flat.createGraphics();
            paintFigure(g, 0, 0, 0, 0, null);
            g.dispose();
        }
        return flat;
    }

    /** Draws the figure standing on (cx, groundY), height px tall, scaled smoothly. */
    public void drawStill(Graphics2D g, int cx, int groundY, int height) {
        double s = height / (double) FOOT_Y;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(shrunk(height), (int) Math.round(cx - FIG_W * s / 2), (int) Math.round(groundY - FOOT_Y * s), null);
        g2.dispose();
    }

    private BufferedImage shrunkCache;

    private BufferedImage shrunk(int height) {
        int w = (int) Math.round(FIG_W * height / (double) FOOT_Y), h = (int) Math.round(FIG_H * height / (double) FOOT_Y);
        if (shrunkCache != null && shrunkCache.getHeight() == h) return shrunkCache;
        BufferedImage cur = flat();
        while (cur.getHeight() / 2 >= h) cur = scale(cur, cur.getWidth() / 2, cur.getHeight() / 2);
        shrunkCache = scale(cur, w, h);
        return shrunkCache;
    }

    private static BufferedImage scale(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(Math.max(1, w), Math.max(1, h), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    /**
     * Battle pose: arms sway and the legs shuffle a little while idle; on a lunge (attack 0..1) the arm
     * on the monster's side swings up and the legs stride.
     */
    public void drawPosed(Graphics2D g, int cx, int groundY, int height, double t, double attack) {
        double s = height / (double) FOOT_Y;
        double sway = Math.sin(t * 2.4) * 4;
        double armR = 6 + sway + attack * 95;
        double armL = 6 - sway + attack * 15;
        double legL = attack * 14, legR = -attack * 10;

        // Pose into a buffer, then lay it on a paper-colored halo so thin pencil lines stay readable
        // over the hatched backdrops (like a doodle cut out and stuck onto the page).
        int pad = (int) Math.ceil(130 * s);
        int bw = (int) Math.ceil(FIG_W * s) + pad * 2, bh = (int) Math.ceil(FIG_H * s) + pad * 2;
        BufferedImage buf = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gb = buf.createGraphics();
        gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gb.translate(pad, pad);
        gb.scale(s, s);
        paintFigure(gb, armL, armR, legL, legR, null);
        gb.dispose();
        BufferedImage halo = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gh = halo.createGraphics();
        gh.drawImage(buf, 0, 0, null);
        gh.setComposite(AlphaComposite.SrcIn);
        gh.setColor(Sketch.PAPER);
        gh.fillRect(0, 0, bw, bh);
        gh.dispose();

        int x = (int) Math.round(cx - FIG_W * s / 2) - pad, y = (int) Math.round(groundY - FOOT_Y * s) - pad;
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            g.drawImage(halo, x + (int) Math.round(Math.cos(a) * 3), y + (int) Math.round(Math.sin(a) * 3), null);
        }
        // A second, slightly darker pass doubles each stroke, like pressing the pencil harder.
        g.drawImage(buf, x + 1, y, null);
        g.drawImage(buf, x, y, null);
    }

    // ---------- save / load ----------

    private static String fileKey(Part p) {
        return p.name().toLowerCase();
    }

    public void save(String id) {
        for (Part p : Part.values()) {
            try {
                ImageIO.write(image(p), "png", SaveManager.drawingFile(id, fileKey(p)));
            } catch (Exception e) {
                System.out.println("캐릭터 그림을 저장하지 못했습니다: " + e.getMessage());
            }
        }
    }

    /** The character's drawing, or the default doodle if they have none yet (or it can't be read). */
    public static CharacterArt load(String id) {
        CharacterArt art = new CharacterArt();
        boolean any = false;
        for (Part p : Part.values()) {
            File f = SaveManager.drawingFile(id, fileKey(p));
            if (!f.exists()) continue;
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) continue;
                Graphics2D g = art.image(p).createGraphics();
                g.drawImage(img, 0, 0, p.w, p.h, null);
                g.dispose();
                any = true;
            } catch (Exception ignored) {
                // unreadable part: falls back to the doodle below
            }
        }
        if (!any) return defaultDoodle();
        art.fillEmptyFromDefault();
        return art;
    }

    // ---------- the default doodle ----------

    private static final Color LEAD = new Color(48, 46, 56);

    /** A simple notebook stick-buddy, for characters that haven't been drawn yet. */
    public static CharacterArt defaultDoodle() {
        CharacterArt a = new CharacterArt();
        Graphics2D g = pen(a.image(Part.HEAD));
        doodleOval(g, 100, 110, 72, 70, 11);
        g.fillOval(70, 100, 12, 14);
        g.fillOval(118, 100, 12, 14);
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawArc(78, 120, 44, 26, 200, 140);
        for (int i = 0; i < 5; i++) g.drawLine(70 + i * 15, 46, 64 + i * 17, 26 + (i % 2) * 8);   // spiky hair
        g.dispose();

        g = pen(a.image(Part.BODY));
        doodleRect(g, 52, 6, 96, 222, 21);
        g.drawLine(100, 30, 100, 210);
        for (int y = 70; y <= 170; y += 50) g.fillOval(92, y, 10, 10);
        g.dispose();

        g = pen(a.image(Part.ARM));
        doodleRect(g, 30, 6, 30, 170, 31);
        doodleOval(g, 45, 192, 18, 18, 32);
        g.dispose();

        g = pen(a.image(Part.LEG));
        doodleRect(g, 32, 0, 36, 180, 41);
        doodleRect(g, 22, 184, 60, 26, 42);
        g.dispose();
        return a;
    }

    private static Graphics2D pen(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(LEAD);
        g.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        return g;
    }

    private static void doodleOval(Graphics2D g, double cx, double cy, double rx, double ry, long seed) {
        Random r = new Random(seed);
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        int n = 40;
        for (int i = 0; i <= n + 3; i++) {
            double a = Math.PI * 2 * i / n, k = 1 + (r.nextDouble() - 0.5) * 0.05;
            double x = cx + Math.cos(a) * rx * k, y = cy + Math.sin(a) * ry * k;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        g.draw(p);
    }

    private static void doodleRect(Graphics2D g, double x, double y, double w, double h, long seed) {
        Sketch.box(g, x, y, w, h, seed, 4.5f, LEAD);
    }

    /** For drawing tools: a part's image transformed onto the figure (mirrored for the right copy). */
    public static AffineTransform toFigure(Part p, boolean right) {
        Point o = origin(p, right);
        if (!right) return AffineTransform.getTranslateInstance(o.x, o.y);
        AffineTransform t = AffineTransform.getTranslateInstance(o.x + p.w, o.y);
        t.scale(-1, 1);
        return t;
    }
}
