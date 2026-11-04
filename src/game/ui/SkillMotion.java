package game.ui;

import game.ui.CharacterArt.Part;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * Signature skill motions: a pencil close-up of the hero playing a skill, drawn on a W x H page over the
 * dungeon's backdrop and the monster, which BattleStage lays over the battle. Each scene is a table of
 * key poses (eased between) plus what it draws around them, and the moments its hits land.
 * One per job class for a 1차 skill, and a showier one for a 4차 skill (see Job, Skill.motion).
 */
final class SkillMotion {
    static final int W = 800, H = 430, GROUND = H - 28, HERO_X = 170, HERO_H = 198;
    static final int MON_X = 640, MON_GROUND = 270, MON_H = 170, HIT_Y = MON_GROUND - 75;
    static final double STAND_H = 650, REACH = 196, SIDE_ON = 70;
    /** The far ground is higher on the page: a hero who walks toward the monster rises this much per pixel. */
    static final double RISE = 0.22;

    // Key columns: time, right arm, left arm, prop angle, lean, legs, left hand on the prop (0..1),
    // prop drawn (0..1), hero x, zoom, focus (0 hero .. 1 target), how far up the prop the left hand holds.
    static final int MS = 0, ARM_R = 1, ARM_L = 2, PROP = 3, LEAN = 4, LEGS = 5, GRIP = 6, AMOUNT = 7, X = 8, ZOOM = 9, FOCUS = 10, GRIP_DIST = 11;

    private SkillMotion() {}

    /** The scene for a skill's motion key, or null for an unknown key. */
    static Scene scene(String key) {
        if (key == null) return null;
        switch (key) {
            case "slash": return new Slash();
            case "fireball": return new Fireball();
            case "emerald_arrow": return new EmeraldArrow();
            case "silver_stars": return new Stars(false);
            case "twin_pistols": return new TwinPistols();
            case "raging_blow": return new RagingBlow();
            case "meteor": return new Meteor();
            case "storm_song": return new StormSong();
            case "night_stars": return new Stars(true);
            case "broadside": return new Broadside();
            default: return null;
        }
    }

    /** One camera's view of the page. */
    static final class Cam {
        final AffineTransform t;
        final double zoom;

        Cam(AffineTransform t, double zoom) {
            this.t = t;
            this.zoom = zoom;
        }

        Point2D at(double x, double y) {
            return t.transform(new Point2D.Double(x, y), null);
        }
    }

    abstract static class Scene {
        final Color color;
        final double[][] keys;
        final double end, closeUpEnd;
        final double[] hits;
        /** How far above the feet the close-up centres. */
        double lookUp = 175;
        /** Focus lines turn this color just before the move lets loose. */
        Color focusLate = Sketch.RED_PENCIL;
        /** Where the hands were in the last frame drawn, in page coordinates. */
        double handX, handY, handLX, handLY;
        /** The forged weapon's drawing, when the hero holds one and the scene swings a blade. */
        BufferedImage heldDrawing;

        Scene(Color color, double end, double closeUpEnd, double[] hits, double[][] keys) {
            this.color = color;
            this.end = end;
            this.closeUpEnd = closeUpEnd;
            this.hits = hits;
            this.keys = keys;
        }

        /** Drawn in the right hand: (0, 0) at the hand, +y along the arm, on the figure's scale. */
        void held(Graphics2D g, double[] p, double ms) {}

        /** The same for the left hand. */
        void heldLeft(Graphics2D g, double[] p, double ms) {}

        /** Drawn behind the hero, in page coordinates. */
        void under(Graphics2D g, Cam cam, double[] p, double ms) {}

        /** Drawn over everything, in page coordinates. */
        void over(Graphics2D g, Cam cam, double[] p, double ms) {}

        /** Tint laid over the backdrop (night, a burning sky), or null. */
        Color sky(double ms) {
            return null;
        }

        /** How hard the page shakes and the monster is knocked back, from the hits so far. */
        double knock(double ms) {
            double k = 0;
            for (double h : hits) if (ms >= h) k = Math.max(k, Math.pow(Math.max(0, 1 - (ms - h) / 380), 2));
            return k;
        }

        double shake(double ms) {
            double s = 0;
            for (double h : hits) if (ms >= h) s = Math.max(s, Math.pow(Math.max(0, 1 - (ms - h) / 280), 2));
            return s;
        }

        /**
         * One frame at ms, onto a W x H page. The monster is drawn monH tall with its feet (footFrac of the
         * picture down) on its ground, mirrored if it faces left.
         */
        void render(Graphics2D g, CharacterArt art, BufferedImage bg, BufferedImage mon, boolean monFlip, double monH, double footFrac, double ms) {
            double[] p = at(keys, ms);
            double knock = knock(ms), shake = shake(ms);
            double heroX = p[X], zoom = p[ZOOM];
            double feetY = GROUND - (heroX - HERO_X) * RISE;
            double fx = lerp(heroX, (heroX + MON_X) / 2 + 90, p[FOCUS]), fy = lerp(feetY - lookUp, MON_GROUND - 50, p[FOCUS]);
            fx = clamp(fx, W / 2.0 / zoom, W - W / 2.0 / zoom);
            fy = clamp(fy, H / 2.0 / zoom, H - H / 2.0 / zoom);
            AffineTransform t = new AffineTransform();
            t.translate(W / 2.0 + Math.sin(ms * 0.09) * 12 * shake, H / 2.0 + Math.sin(ms * 0.13) * 8 * shake);
            t.scale(zoom, zoom);
            t.translate(-fx, -fy);
            Cam cam = new Cam(t, zoom);

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Sketch.PAPER);
            g.fillRect(0, 0, W, H);
            Graphics2D gw = (Graphics2D) g.create();
            gw.transform(t);
            if (bg != null) {
                double scale = Math.max(W / (double) bg.getWidth(), H / (double) bg.getHeight());
                gw.drawImage(bg, 0, 0, (int) (bg.getWidth() * scale), (int) (bg.getHeight() * scale), null);
            }
            Color sky = sky(ms);
            if (sky != null) {
                gw.setColor(sky);
                gw.fillRect(-W, -H, W * 3, H * 3);
            }
            if (mon != null) {
                int mh = (int) Math.round(monH), mw = (int) Math.round(monH * mon.getWidth() / mon.getHeight());
                int mx = (int) (MON_X + knock * 26 - mw / 2.0), my = (int) Math.round(MON_GROUND - mh * footFrac);
                if (monFlip) gw.drawImage(mon, mx + mw, my, -mw, mh, null);
                else gw.drawImage(mon, mx, my, mw, mh, null);
            }
            gw.dispose();

            double close = ms < closeUpEnd ? clamp((zoom - 1) / 1.1, 0, 1) : 0;
            if (close > 0) {
                g.setColor(new Color(247, 243, 230, (int) (150 * close)));
                g.fillRect(0, 0, W, H);
                focusLines(g, close, (int) (ms / 66), ms > closeUpEnd - 350 ? focusLate : Sketch.INK);
            }
            under(g, cam, p, ms);
            Point2D feet = cam.at(heroX, feetY);
            drawHero(g, art, feet.getX(), feet.getY(), HERO_H * zoom, p, ms);
            over(g, cam, p, ms);
            for (double h : hits) {
                if (ms >= h && ms < h + 110) {
                    g.setColor(new Color(255, 252, 240, (int) (120 * (1 - (ms - h) / 110))));
                    g.fillRect(0, 0, W, H);
                }
            }
        }

        void drawHero(Graphics2D g, CharacterArt art, double cx, double ground, double height, double[] p, double ms) {
            double s = height / STAND_H;
            int pad = (int) Math.ceil(620 * s);
            int bw = (int) Math.ceil(CharacterArt.FIG_W * s) + pad * 2, bh = (int) Math.ceil(CharacterArt.FIG_H * s) + pad * 2;
            BufferedImage buf = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gb = buf.createGraphics();
            gb.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gb.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gb.translate(pad, pad);
            gb.scale(s, s);
            gb.rotate(Math.toRadians(p[LEAN]), CharacterArt.FIG_W / 2.0, CharacterArt.FOOT_Y);
            double armR = p[ARM_R] + tremble(ms), side = p[GRIP] * SIDE_ON;
            limb(gb, art, Part.LEG, false, p[LEGS], 0, 1, null, ms);
            limb(gb, art, Part.LEG, true, p[LEGS] * 0.7, 0, 1, null, ms);
            Point o = CharacterArt.origin(Part.BODY, false);
            gb.drawImage(art.image(Part.BODY), o.x, o.y, null);

            // A hand on the prop: the left arm is aimed at its spot and stretched just enough to arrive.
            Point ra = CharacterArt.origin(Part.ARM, true), la = CharacterArt.origin(Part.ARM, false);
            double px = ra.x + Part.ARM.w / 2.0, py = ra.y + 8;
            double hx = px + Math.sin(Math.toRadians(armR)) * REACH, hy = py + Math.cos(Math.toRadians(armR)) * REACH;
            double gx = hx - Math.sin(Math.toRadians(p[PROP])) * p[GRIP_DIST], gy = hy - Math.cos(Math.toRadians(p[PROP])) * p[GRIP_DIST];
            double lx = la.x + Part.ARM.w / 2.0 + side, ly = la.y + 8;
            double aim = -Math.toDegrees(Math.atan2(gx - lx, gy - ly));
            double armL = lerp(p[ARM_L], aim, p[GRIP]), stretch = lerp(1, Math.hypot(gx - lx, gy - ly) / REACH, p[GRIP]);
            limb(gb, art, Part.ARM, false, armL, side, stretch, p, ms);
            double[] pr = p.clone();
            pr[ARM_R] = armR;
            limb(gb, art, Part.ARM, true, armR, 0, 1, pr, ms);
            o = CharacterArt.origin(Part.HEAD, false);
            gb.drawImage(art.image(Part.HEAD), o.x, o.y, null);
            gb.dispose();

            // The paper halo that keeps the pencil readable over the backdrop: four offset copies are enough
            // for an outline, and the battle draws these frames live.
            BufferedImage halo = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gh = halo.createGraphics();
            gh.drawImage(buf, 0, 0, null);
            gh.setComposite(AlphaComposite.SrcIn);
            gh.setColor(Sketch.PAPER);
            gh.fillRect(0, 0, bw, bh);
            gh.dispose();
            int x = (int) Math.round(cx - CharacterArt.FIG_W * s / 2) - pad, y = (int) Math.round(ground - CharacterArt.FOOT_Y * s) - pad;
            int ring = (int) Math.max(3, Math.round(height / 60));
            for (int i = 0; i < 4; i++) {
                double a = Math.PI / 4 + Math.PI * 2 * i / 4;
                g.drawImage(halo, x + (int) Math.round(Math.cos(a) * ring), y + (int) Math.round(Math.sin(a) * ring), null);
            }
            g.drawImage(buf, x, y, null);
            handX = cx - CharacterArt.FIG_W * s / 2 + hx * s;
            handY = ground - (CharacterArt.FOOT_Y - hy) * s;
            handLX = cx - CharacterArt.FIG_W * s / 2 + (lx - Math.sin(Math.toRadians(armL)) * REACH * stretch) * s;
            handLY = ground - (CharacterArt.FOOT_Y - (ly + Math.cos(Math.toRadians(armL)) * REACH * stretch)) * s;
        }

        /** A shiver in the raised arm while power gathers (none by default). */
        double tremble(double ms) {
            return 0;
        }

        void limb(Graphics2D g, CharacterArt art, Part p, boolean right, double angle, double shift, double stretch, double[] pose, double ms) {
            Point o = CharacterArt.origin(p, right);
            Graphics2D gl = (Graphics2D) g.create();
            gl.translate(shift, 0);
            gl.rotate(Math.toRadians(right ? -angle : angle), o.x + p.w / 2.0, o.y + 8);
            gl.translate(0, o.y + 8);
            gl.scale(1, stretch);
            gl.translate(0, -(o.y + 8));
            BufferedImage img = art.image(p);
            if (right) gl.drawImage(img, o.x + p.w, o.y, -p.w, p.h, null);
            else gl.drawImage(img, o.x, o.y, null);
            if (pose != null && pose[AMOUNT] > 0) {
                gl.translate(o.x + p.w / 2.0, o.y + p.baseH - 16);
                if (right) held(gl, pose, ms);
                else heldLeft(gl, pose, ms);
            }
            gl.dispose();
        }
    }

    static double[] at(double[][] keys, double ms) {
        for (int i = 0; i + 1 < keys.length; i++) {
            double[] a = keys[i], b = keys[i + 1];
            if (ms <= b[MS]) {
                double u = (ms - a[MS]) / (b[MS] - a[MS]);
                u = u * u * (3 - 2 * u);
                double[] out = new double[a.length];
                for (int k = 0; k < a.length; k++) out[k] = lerp(a[k], b[k], u);
                return out;
            }
        }
        return keys[keys.length - 1].clone();
    }

    // =====================================================================================
    // 1차
    // =====================================================================================

    /** 파워 스트라이크: close in, a two-handed sword pencilled into the hands, lifted, then one dash and cut. */
    static class Slash extends Scene {
        static final double CUT = 1160;
        final Random sparks = new Random(4);
        final double[] angles = new double[16], speeds = new double[16];

        Slash() {
            this(new Color(0xFF7A3C), 2300, new double[]{CUT}, new double[][]{
                    {0, 6, 6, 150, 0, 0, 0, 0, HERO_X, 1, 0, 58},
                    {300, 62, 6, 150, 2, 6, 1, 0.35, HERO_X, 2.5, 0, 58},
                    {620, 68, 6, 150, 3, 8, 1, 1, HERO_X, 2.45, 0, 58},
                    {860, 166, 6, 208, -9, 12, 1, 1, HERO_X - 8, 1.75, 0, 58},
                    {1040, 170, 6, 212, -10, 12, 1, 1, HERO_X - 8, 1.85, 0, 58},
                    {1160, 44, 6, 62, 18, 24, 1, 1, MON_X - 150, 1.45, 0.72, 58},
                    {1320, 42, 6, 60, 18, 24, 1, 1, MON_X - 150, 1.45, 0.72, 58},
                    {1600, 40, 6, 60, 12, 16, 1, 0.6, MON_X - 170, 1.25, 0.6, 58},
                    {1900, 6, 6, 150, 0, 0, 0, 0, HERO_X, 1, 0, 58},
            });
        }

        Slash(Color color, double end, double[] hits, double[][] keys) {
            super(color, end, hits[0], hits, keys);
            for (int i = 0; i < 16; i++) {
                angles[i] = sparks.nextDouble() * Math.PI * 2;
                speeds[i] = 0.6 + sparks.nextDouble() * 0.6;
            }
        }

        @Override double tremble(double ms) {
            return ms > 860 && ms < 1040 ? Math.sin(ms * 0.2) * 2 : 0;
        }

        @Override void held(Graphics2D g, double[] p, double ms) {
            g.rotate(Math.toRadians(p[ARM_R] - p[PROP]));
            blade(g, p[AMOUNT], color, heldDrawing);
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            if (ms > 980 && ms < 1060) {   // a glint runs up the raised blade
                double a = 1 - Math.abs(ms - 1020) / 40;
                g.setColor(new Color(255, 250, 200, (int) (200 * a)));
                g.fill(spark(W / 2.0 + 40, 26, 34 * a + 4));
            }
            double since = ms - CUT;
            if (since >= 0 && since < 700) {
                Point2D hit = cam.at(MON_X + knock(ms) * 26, MON_GROUND - MON_H * 0.45);
                cut(g, since / 700, hit.getX(), hit.getY(), cam.zoom, 150, 34, 38);
            }
        }

        /** One big straight pencil cut through the target, drawn at once, then rubbed out from its tail. */
        void cut(Graphics2D g2, double t, double cx, double cy, double zoom, double length, double thick, double tilt) {
            double half = length * zoom, width = thick * zoom;
            double head = t < 0.12 ? t / 0.12 : 1, tail = t < 0.45 ? 0 : (t - 0.45) / 0.55;
            Graphics2D g = (Graphics2D) g2.create();
            g.translate(cx, cy);
            g.rotate(Math.toRadians(tilt));
            for (int i = 0; i < 44; i++) {
                double u = i / 44.0;
                if (u < tail || u > head) continue;
                double x = -half + u * half * 2, h = width * Math.sin(Math.PI * u) / 2;
                Sketch.line(g, x - 5, -h, x + 7, h, i, (float) (2.2 * zoom), color);
            }
            double from = -half + tail * half * 2, to = -half + head * half * 2;
            for (int side = -1; side <= 1; side += 2) {
                Path2D.Double edge = new Path2D.Double();
                boolean started = false;
                for (int i = 0; i <= 44; i++) {
                    double u = i / 44.0, x = -half + u * half * 2;
                    if (x < from || x > to) continue;
                    double y = side * width * Math.sin(Math.PI * u) / 2 + Math.sin(i * 1.7 + side) * 1.2;
                    if (started) edge.lineTo(x, y);
                    else edge.moveTo(x, y);
                    started = true;
                }
                g.setColor(Sketch.INK);
                g.setStroke(new BasicStroke((float) (2.8 * zoom), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(edge);
            }
            g.dispose();
            double st = t / 0.5;
            if (st < 1) {
                for (int i = 0; i < angles.length; i++) {
                    double d0 = (18 + st * 95 * speeds[i]) * zoom, len = (26 * (1 - st) + 8) * zoom;
                    Sketch.line(g2, cx + Math.cos(angles[i]) * d0, cy + Math.sin(angles[i]) * d0, cx + Math.cos(angles[i]) * (d0 + len),
                            cy + Math.sin(angles[i]) * (d0 + len), i + 3, (float) (2.4 * zoom), i % 3 == 0 ? Sketch.INK : color);
                }
            }
        }
    }

    /**
     * A sword from the grip along +y, pencilled stroke by stroke as amount goes to 1; or the player's own
     * forged weapon, fading in, held where they drew the hand.
     */
    static void blade(Graphics2D g, double amount, Color tint, BufferedImage drawing) {
        if (drawing != null) {
            Graphics2D gd = (Graphics2D) g.create();
            gd.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) clamp(amount, 0, 1)));
            gd.rotate(Math.PI);
            gd.drawImage(drawing, -GearArt.GRIP_X, -GearArt.GRIP_Y, null);
            gd.dispose();
            return;
        }
        double w = 25, tip = 400, shoulder = 340, grip = -70;
        double[][] strokes = {{0, grip, 0, 0}, {-w * 2.2, 18, w * 2.2, 18}, {-w, 18, -w, shoulder}, {-w, shoulder, 0, tip}, {w, 18, w, shoulder}, {w, shoulder, 0, tip}};
        if (amount > 0.6) {
            Path2D.Double edge = new Path2D.Double();
            edge.moveTo(-w, 18);
            edge.lineTo(-w, shoulder);
            edge.lineTo(0, tip);
            edge.lineTo(w, shoulder);
            edge.lineTo(w, 18);
            edge.closePath();
            Sketch.hatch(g, edge, 41, tint, (int) (Math.min(1, (amount - 0.6) / 0.4) * 210), 15, 62);
        }
        double shown = amount * strokes.length;
        for (int i = 0; i < strokes.length && shown > 0; i++) {
            double[] s = strokes[i];
            double part = Math.min(1, shown);
            Sketch.line(g, s[0], s[1], lerp(s[0], s[2], part), lerp(s[1], s[3], part), 41 + i, 10f, Sketch.INK);
            shown -= 1;
        }
    }

    /** 에너지 볼트 (불·독 마법사): fire gathers over the raised hands, then a real fireball is thrown. */
    static class Fireball extends Scene {
        static final double THROW = 1400, HIT = 1800;

        Fireball() {
            super(new Color(0xE0283C), 2750, THROW, new double[]{HIT}, new double[][]{
                    {0, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                    {320, 110, 110, 0, 0, 10, 0, 0, HERO_X, 1.5, 0, 0},
                    {800, 168, 168, 0, -3, 14, 0, 0, HERO_X, 1.3, 0, 0},
                    {1250, 172, 172, 0, -6, 14, 0, 0, HERO_X, 1.32, 0, 0},
                    {1330, 178, 178, 0, -11, 16, 0, 0, HERO_X - 8, 1.34, 0, 0},
                    {1440, 84, 60, 0, 13, 22, 0, 0, HERO_X + 12, 1.22, 0.75, 0},
                    {2200, 95, 30, 0, 10, 18, 0, 0, HERO_X, 1.18, 1, 0},
                    {2600, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
            });
            lookUp = 150;
        }

        @Override void under(Graphics2D g, Cam cam, double[] p, double ms) {
            magicCircle(g, cam.at(p[X], GROUND - 2), 118 * cam.zoom, 30 * cam.zoom, clamp((ms - 220) / 650, 0, 1),
                    clamp(1 - (ms - 1500) / 400, 0, 1), color, cam.zoom, 5);
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            double above = GROUND - 252;
            double grow = clamp((ms - 560) / 720, 0, 1);
            if (grow > 0 && ms < THROW) {
                Point2D c = cam.at(p[X], above);
                fire(g, c.getX(), c.getY(), (10 + 34 * grow) * cam.zoom, -Math.PI / 2, 0.8, ms);
            }
            if (ms >= THROW && ms < HIT) {
                double u = Math.pow((ms - THROW) / (HIT - THROW), 1.25);
                double wx = lerp(HERO_X + 40, MON_X, u), wy = lerp(above, HIT_Y, u) - Math.sin(u * Math.PI) * 26;
                Point2D c = cam.at(wx, wy), to = cam.at(MON_X, HIT_Y);
                double heading = Math.atan2(to.getY() - c.getY(), to.getX() - c.getX());
                embers(g, c.getX(), c.getY(), heading + Math.PI, 46 * cam.zoom, ms);
                fire(g, c.getX(), c.getY(), 42 * cam.zoom, heading + Math.PI, 2.3, ms);
            }
            if (ms >= HIT && ms < HIT + 650) explosion(g, cam.at(MON_X, HIT_Y + 20), cam.zoom, (ms - HIT) / 650, color, new Color(238, 134, 40), 1);
        }
    }

    /** 크리티컬 샷: the bow drawn in a long close-up, a held breath, then one emerald arrow straight through. */
    static class EmeraldArrow extends Scene {
        static final double RELEASE = 1180, HIT = 1480;

        EmeraldArrow() {
            super(new Color(0x0FBFA0), 2500, RELEASE, new double[]{HIT}, new double[][]{
                    {0, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 40},
                    {300, 90, -40, 92, 0, 10, 1, 0.4, HERO_X, 1.75, 0, 40},
                    {560, 92, -40, 92, 0, 12, 1, 1, HERO_X, 2.0, 0, 44},
                    {1000, 92, -40, 92, -5, 14, 1, 1, HERO_X, 2.25, 0, 190},
                    {1170, 92, -40, 92, -5, 14, 1, 1, HERO_X, 2.3, 0, 192},
                    {1230, 96, 40, 92, 4, 14, 0.2, 1, HERO_X - 6, 1.15, 0.95, 40},
                    {2050, 92, 20, 92, 2, 10, 0, 0.6, HERO_X, 1.12, 0.9, 40},
                    {2400, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 40},
            });
        }

        @Override void held(Graphics2D g, double[] p, double ms) {
            bow(g, p[AMOUNT], ms < RELEASE ? p[GRIP_DIST] : 0, ms < RELEASE, color);
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            gatherLeaves(g, cam, ms, 380, RELEASE, handX + 44 * cam.zoom, handY, color, 80, 6);
            if (ms > 1000 && ms < RELEASE) {
                double a = 1 - Math.abs(ms - 1090) / 90;
                g.setColor(new Color(170, 248, 228, (int) (235 * Math.max(0, a))));
                g.fill(spark(handX + 150, handY, 44 * Math.max(0, a) + 2));
            }
            Point2D from = cam.at(HERO_X + 70, GROUND - 130), to = cam.at(MON_X, HIT_Y);
            for (int i = 0; i < 5; i++) {
                double born = RELEASE + (HIT - RELEASE) * i / 5.0, age = (ms - born) / 800;
                if (age < 0 || age >= 1) continue;
                double u = i / 5.0;
                double x = lerp(from.getX(), to.getX(), u) + Math.sin(age * 5 + i) * 12 * cam.zoom;
                double y = lerp(from.getY(), to.getY(), u) + age * 62 * cam.zoom + (i % 2 == 0 ? -12 : 14) * cam.zoom;
                leaf(g, x, y, 6 * cam.zoom * (1 - age * 0.45), age * 4 + i, i, color);
            }
            if (ms >= HIT && ms < HIT + 900) leafBurst(g, to, cam.zoom, (ms - HIT) / 900, color, 8, 1);
            if (ms >= RELEASE && ms < HIT) arrow(g, from, to, (ms - RELEASE) / (HIT - RELEASE), 1.25 * cam.zoom, color, ms);
            if (ms >= HIT && ms < HIT + 620) beam(g, from, to, (ms - HIT) / 620, color, 15, cam.zoom);
        }
    }

    /**
     * Throwing stars, two hands in turn from a low stance: two for 럭키 세븐; for 쿼드러플 스로우 (night) four,
     * under a night sky and a crescent moon, thrown beside a shadow of the hero, each bursting in shadow.
     */
    static class Stars extends Scene {
        static final Color SILVER = new Color(150, 160, 178), SHADOW = new Color(28, 14, 40);
        final double[] throwsAt;
        final boolean night;
        static final double FLIGHT = 190;

        Stars(boolean night) {
            super(new Color(night ? 0x7A3CC8 : 0x6A2C9A), night ? 2900 : 2100, 820, night ? new double[]{1010, 1120, 1230, 1340} : new double[]{1010, 1120},
                    night ? new double[][]{
                            {0, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                            {300, 40, -40, 92, 6, 10, 0, 1, HERO_X, 2.0, 0, 0},
                            {640, 34, -34, 92, 8, 12, 0, 1, HERO_X, 2.1, 0, 0},
                            {790, 30, -30, 92, 9, 12, 0, 1, HERO_X, 2.1, 0, 0},
                            {835, 104, -22, 92, 12, 16, 0, 1, HERO_X + 14, 1.35, 0.55, 0},
                            {945, 22, -104, 92, 12, 16, 0, 1, HERO_X + 18, 1.3, 0.58, 0},
                            {1055, 104, -22, 92, 13, 18, 0, 1, HERO_X + 22, 1.27, 0.6, 0},
                            {1165, 22, -104, 92, 13, 18, 0, 1, HERO_X + 26, 1.24, 0.62, 0},
                            {1900, 40, -40, 92, 6, 12, 0, 0, HERO_X + 24, 1.15, 0.6, 0},
                            {2700, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                    } : new double[][]{
                            {0, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                            {300, 40, -40, 92, 6, 10, 0, 1, HERO_X, 2.0, 0, 0},
                            {640, 34, -34, 92, 8, 12, 0, 1, HERO_X, 2.1, 0, 0},
                            {790, 30, -30, 92, 9, 12, 0, 1, HERO_X, 2.1, 0, 0},
                            {835, 104, -22, 92, 12, 16, 0, 1, HERO_X + 14, 1.35, 0.55, 0},
                            {945, 22, -104, 92, 12, 16, 0, 1, HERO_X + 18, 1.3, 0.58, 0},
                            {1500, 40, -40, 92, 6, 12, 0, 0, HERO_X + 20, 1.15, 0.6, 0},
                            {2000, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                    });
            this.night = night;
            this.throwsAt = night ? new double[]{820, 930, 1040, 1150} : new double[]{820, 930};
            lookUp = 140;
            focusLate = color;
        }

        @Override Color sky(double ms) {
            if (!night) return null;
            double dark = clamp(ms / 500, 0, 1) * clamp((end - 250 - ms) / 400, 0, 1);
            return new Color(24, 10, 44, (int) (150 * dark));
        }

        @Override void held(Graphics2D g, double[] p, double ms) {
            if (holding(true, ms)) throwingStar(g, 0, 30, 62 * p[AMOUNT], ms * 0.002, SILVER);
        }

        @Override void heldLeft(Graphics2D g, double[] p, double ms) {
            if (holding(false, ms)) throwingStar(g, 0, 30, 62 * p[AMOUNT], -ms * 0.002, SILVER);
        }

        boolean holding(boolean right, double ms) {
            double last = -1;
            boolean more = false;
            for (int i = right ? 0 : 1; i < throwsAt.length; i += 2) {
                if (ms >= throwsAt[i]) last = throwsAt[i];
                else more = true;
            }
            return last < 0 || (more && ms - last > 60);
        }

        @Override void under(Graphics2D g, Cam cam, double[] p, double ms) {
            if (!night) return;
            double show = clamp(ms / 600, 0, 1) * clamp((end - 250 - ms) / 400, 0, 1);
            if (show <= 0) return;
            // A crescent moon over the target, and the hero's shadow throwing beside them.
            Point2D m = cam.at(MON_X - 40, 70);
            double r = 70 * cam.zoom;
            Area moon = new Area(new Ellipse2D.Double(m.getX() - r, m.getY() - r, r * 2, r * 2));
            moon.subtract(new Area(new Ellipse2D.Double(m.getX() - r * 0.55, m.getY() - r * 1.15, r * 2, r * 2)));
            g.setColor(new Color(240, 232, 200, (int) (230 * show)));
            g.fill(moon);
            Sketch.hatch(g, moon, 77, new Color(190, 170, 230), (int) (150 * show), 6, 40);
            g.setColor(alpha(Sketch.INK, (int) (220 * show)));
            g.setStroke(new BasicStroke(2.6f));
            g.draw(moon);
            Random r2 = new Random(5);
            for (int i = 0; i < 16; i++) {
                double x = r2.nextDouble() * W, y = r2.nextDouble() * H * 0.45, tw = 0.5 + 0.5 * Math.sin(ms * 0.01 + i);
                g.setColor(new Color(230, 220, 255, (int) (200 * show * tw)));
                g.fill(spark(x, y, 4 + 4 * tw));
            }
        }

        @Override void drawHero(Graphics2D g, CharacterArt art, double cx, double ground, double height, double[] p, double ms) {
            if (night && ms > 300 && ms < end - 500) {
                // The shadow double: the same throw, a beat behind, dark and half seen.
                double[] q = at(keys, Math.max(0, ms - 90));
                BufferedImage ghost = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gg = ghost.createGraphics();
                double hx = handX, hy = handY, lx = handLX, ly = handLY;
                super.drawHero(gg, art, cx - 70 * height / HERO_H, ground - 6 * height / HERO_H, height * 0.96, q, ms);
                handX = hx;
                handY = hy;
                handLX = lx;
                handLY = ly;
                gg.setComposite(AlphaComposite.SrcAtop);
                gg.setColor(new Color(60, 24, 96));
                gg.fillRect(0, 0, W, H);
                gg.dispose();
                Graphics2D gs = (Graphics2D) g.create();
                gs.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                gs.drawImage(ghost, 0, 0, null);
                gs.dispose();
            }
            super.drawHero(g, art, cx, ground, height, p, ms);
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            if (ms > 250 && ms < throwsAt[throwsAt.length - 1]) {   // a dark aura flickers up from both hands
                double grow = clamp((ms - 250) / 400, 0, 1);
                int tongues = night ? 18 : 12;
                for (int k = 0; k < tongues; k++) {
                    boolean right = k < tongues / 2;
                    double hx0 = right ? handX : handLX, hy0 = right ? handY : handLY;
                    double x0 = hx0 + (k % (tongues / 2) - tongues / 4.0) * 7 * cam.zoom, len = (34 + 16 * Math.sin(ms * 0.012 + k * 1.7)) * cam.zoom * grow * (night ? 1.4 : 1);
                    Path2D.Double tongue = new Path2D.Double();
                    for (int i = 0; i <= 10; i++) {
                        double t = i / 10.0;
                        double x = x0 + Math.sin(t * 4 + ms * 0.015 + k) * 5 * cam.zoom * t, y = hy0 + 6 * cam.zoom - len * t;
                        if (i == 0) tongue.moveTo(x, y);
                        else tongue.lineTo(x, y);
                    }
                    g.setColor(alpha(k % 2 == 0 ? color : SHADOW, 210));
                    g.setStroke(new BasicStroke((float) ((k % 3 == 0 ? 3.2 : 2) * cam.zoom / 1.6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(tongue);
                }
            }
            Point2D to = cam.at(MON_X, HIT_Y);
            double size = night ? 1.35 : 1;
            for (int i = 0; i < throwsAt.length; i++) {
                double u = (ms - throwsAt[i]) / FLIGHT;
                double spot = (i - (throwsAt.length - 1) / 2.0) * 15 * cam.zoom;
                if (u >= 0 && u < 1) {
                    Point2D from = cam.at(HERO_X + (i % 2 == 0 ? 82 : 66) + i * 6, GROUND - (i % 2 == 0 ? 140 : 128));
                    double x = lerp(from.getX(), to.getX(), u), y = lerp(from.getY(), to.getY() + spot, u) - Math.sin(u * Math.PI) * 26;
                    streak(g, from, to, spot, u, cam.zoom * size, color, SHADOW);
                    throwingStar(g, x, y, 21 * cam.zoom * size, ms * 0.04, SILVER);
                }
                double since = ms - hits[i];
                if (since >= 0) {
                    double fade = clamp(1 - (ms - (end - 500)) / 300, 0, 1);
                    if (since < 420) shadowBurst(g, to.getX() + spot, to.getY() + spot, cam.zoom * size, since / 420, i, color, SHADOW);
                    if (fade > 0) throwingStar(g, to.getX() + spot * 1.5, to.getY() + spot, 15 * cam.zoom * fade * size, i * 0.7, SILVER);
                    if (since < 400) ticks(g, to.getX() + spot, to.getY() + spot, since / 400, cam.zoom * 0.75, i + 11, color);
                }
            }
            if (night) {   // the finish: every star bursts at once into a dark star
                double fin = ms - hits[hits.length - 1] - 260;
                if (fin >= 0 && fin < 700) {
                    double u = fin / 700, grow = 1 - Math.pow(1 - clamp(u / 0.3, 0, 1), 3), keep = clamp(1 - (u - 0.35) / 0.65, 0, 1);
                    Shape s = spark(to.getX(), to.getY(), 150 * cam.zoom * grow);
                    g.setColor(alpha(SHADOW, (int) (200 * keep)));
                    g.fill(s);
                    Sketch.hatch(g, s, 91, color, (int) (230 * keep), 6, 45);
                    g.setColor(alpha(SILVER, (int) (240 * keep)));
                    g.setStroke(new BasicStroke(3f));
                    g.draw(s);
                    ticks(g, to.getX(), to.getY(), u, cam.zoom * 1.5, 31, color);
                }
            }
        }
    }

    /** 더블 파이어: a pistol in each hand, aimed in close-up, one shot from each hand in turn. */
    static class TwinPistols extends Scene {
        static final double[] SHOTS = {840, 1060};

        TwinPistols(Color color, double end, double closeUpEnd, double[] hits, double[][] keys) {
            super(color, end, closeUpEnd, hits, keys);
        }

        TwinPistols() {
            super(new Color(0xFFB040), 2150, SHOTS[0], new double[]{890, 1110}, new double[][]{
                    {0, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                    {300, 104, -72, 0, 0, 10, 0, 0.8, HERO_X, 2.0, 0, 0},
                    {700, 106, -72, 0, 0, 12, 0, 1, HERO_X, 2.25, 0, 0},
                    {835, 106, -72, 0, 0, 12, 0, 1, HERO_X, 2.3, 0, 0},
                    {885, 138, -72, 0, -7, 12, 0, 1, HERO_X - 12, 1.3, 0.55, 0},
                    {1020, 106, -72, 0, 0, 12, 0, 1, HERO_X - 4, 1.28, 0.55, 0},
                    {1055, 106, -72, 0, 0, 12, 0, 1, HERO_X - 4, 1.28, 0.55, 0},
                    {1105, 106, -104, 0, -7, 12, 0, 1, HERO_X - 12, 1.25, 0.58, 0},
                    {1280, 106, -72, 0, 0, 12, 0, 1, HERO_X - 2, 1.22, 0.55, 0},
                    {1700, 106, -72, 0, 0, 8, 0, 0.6, HERO_X, 1.15, 0.5, 0},
                    {2050, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
            });
        }

        @Override void held(Graphics2D g, double[] p, double ms) {
            pistol(g, p[AMOUNT], color);
        }

        @Override void heldLeft(Graphics2D g, double[] p, double ms) {
            pistol(g, p[AMOUNT], color);
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            Point2D to = cam.at(MON_X, HIT_Y);
            for (int i = 0; i < SHOTS.length; i++) {
                shot(g, cam, p, ms - SHOTS[i], i == 0, to, i == 0 ? -10 : 12, color);
                double after = ms - hits[i];
                if (after >= 0 && after < 420) ticks(g, to.getX(), to.getY() + (i == 0 ? -10 : 12), after / 420, cam.zoom, i + 3, color);
            }
        }

        /** A muzzle flash and a tracer from one hand's pistol, since ms after it fired. */
        void shot(Graphics2D g, Cam cam, double[] p, double since, boolean right, Point2D to, double spot, Color c) {
            if (since < 0) return;
            double barrel = 180 * HERO_H * cam.zoom / STAND_H, tilt = Math.toRadians(right ? p[ARM_R] : -p[ARM_L]);
            double hx = (right ? handX : handLX) + Math.sin(tilt) * barrel, hy = (right ? handY : handLY) + Math.cos(tilt) * barrel;
            if (since < 150) {
                double u = since / 150;
                g.setColor(new Color(255, 214, 90, (int) (235 * (1 - u))));
                Shape burst = spark(hx + 12, hy, 54 * (1 - u) + 12);
                g.fill(burst);
                g.setColor(alpha(Sketch.INK, (int) (230 * (1 - u))));
                g.setStroke(new BasicStroke(3f));
                g.draw(burst);
            }
            if (since < 90) {
                Sketch.line(g, hx + 14, hy, to.getX(), to.getY() + spot, (long) since, 3f, c);
                Sketch.line(g, hx + 14, hy - 3, to.getX(), to.getY() + spot - 3, (long) since + 2, 1.6f, Sketch.INK);
            }
        }
    }

    static void pistol(Graphics2D g, double a, Color c) {
        Rectangle body = new Rectangle(-24, -20, 48, (int) (200 * a));
        Sketch.hatch(g, body, 3, c, 200, 14, 60);
        Sketch.box(g, -24, -20, 48, 200 * a, 3, 10f, Sketch.INK);
        if (a > 0.5) Sketch.box(g, -24 - 84 * (a - 0.5) * 2, -8, 84 * (a - 0.5) * 2, 48, 4, 10f, Sketch.INK);
    }

    // =====================================================================================
    // 4차: the finishers, bigger in every way
    // =====================================================================================

    /**
     * 레이징 블로우: the page goes red, flames climb the hero and the raised sword shakes with power; one dash,
     * and a huge crescent of fire splits the target, the page cracks and shock rings roll out.
     */
    static final class RagingBlow extends Slash {
        static final double STRIKE = 1500;

        RagingBlow() {
            super(new Color(0xFF2E3E), 3000, new double[]{STRIKE}, new double[][]{
                    {0, 6, 6, 150, 0, 0, 0, 0, HERO_X, 1, 0, 58},
                    {320, 62, 6, 150, 2, 6, 1, 0.4, HERO_X, 2.6, 0, 58},
                    {700, 70, 6, 150, 3, 8, 1, 1, HERO_X, 2.5, 0, 58},
                    {1000, 168, 6, 210, -10, 14, 1, 1, HERO_X - 10, 1.9, 0, 58},
                    {1380, 172, 6, 214, -12, 14, 1, 1, HERO_X - 10, 2.05, 0, 58},
                    {1500, 40, 6, 58, 20, 26, 1, 1, MON_X - 150, 1.5, 0.75, 58},
                    {1800, 38, 6, 56, 20, 26, 1, 1, MON_X - 150, 1.42, 0.75, 58},
                    {2300, 40, 6, 60, 12, 16, 1, 0.6, MON_X - 170, 1.2, 0.6, 58},
                    {2800, 6, 6, 150, 0, 0, 0, 0, HERO_X, 1, 0, 58},
            });
            focusLate = color;
        }

        @Override double tremble(double ms) {
            return ms > 1000 && ms < STRIKE ? Math.sin(ms * 0.28) * 3.2 : 0;
        }

        @Override Color sky(double ms) {
            double red = clamp((ms - 600) / 500, 0, 1) * clamp((STRIKE + 900 - ms) / 500, 0, 1);
            return new Color(150, 20, 20, (int) (90 * red));
        }

        @Override void under(Graphics2D g, Cam cam, double[] p, double ms) {
            if (ms < 500 || ms > STRIKE) return;
            double grow = clamp((ms - 500) / 600, 0, 1);
            Point2D feet = cam.at(p[X], GROUND - (p[X] - HERO_X) * RISE);
            // A ring of power at the feet, and flames licking up the whole body.
            double r = (70 + 40 * grow) * cam.zoom;
            ellipse(g, feet, r, r * 0.26, 1, (float) (3.4 * cam.zoom / 1.5), alpha(color, (int) (230 * grow)), 13);
            ellipse(g, feet, r * 1.25, r * 0.32, grow, (float) (2 * cam.zoom / 1.5), alpha(new Color(255, 150, 40), (int) (200 * grow)), 17);
            for (int k = 0; k < 16; k++) {
                double x0 = feet.getX() + (k - 7.5) * 9 * cam.zoom, len = (110 + 60 * Math.sin(ms * 0.011 + k * 1.3)) * cam.zoom * grow;
                Path2D.Double tongue = new Path2D.Double();
                for (int i = 0; i <= 12; i++) {
                    double t = i / 12.0;
                    double x = x0 + Math.sin(t * 5 + ms * 0.02 + k) * 8 * cam.zoom * t, y = feet.getY() - len * t;
                    if (i == 0) tongue.moveTo(x, y);
                    else tongue.lineTo(x, y);
                }
                g.setColor(alpha(k % 3 == 0 ? new Color(255, 200, 60) : k % 3 == 1 ? color : new Color(255, 120, 30), 200));
                g.setStroke(new BasicStroke((float) ((k % 2 == 0 ? 4 : 2.4) * cam.zoom / 1.6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(tongue);
            }
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            if (ms > 900 && ms < STRIKE) embers(g, handX, handY, -Math.PI / 2, 30 * cam.zoom, ms);
            if (ms > 1280 && ms < 1400) {   // the blade flashes once at the top
                double a = 1 - Math.abs(ms - 1340) / 60;
                g.setColor(new Color(255, 245, 200, (int) (230 * a)));
                g.fill(spark(W / 2.0 + 30, 30, 60 * a + 6));
            }
            double since = ms - STRIKE;
            if (since < 0 || since > 1300) return;
            Point2D hit = cam.at(MON_X + knock(ms) * 26, MON_GROUND - MON_H * 0.45);
            // The fire crescent: a fat moon-shaped swath drawn across the target, then burning away.
            double u = since / 900;
            if (u < 1) {
                double head = clamp(u / 0.18, 0, 1), keep = clamp(1 - (u - 0.4) / 0.6, 0, 1);
                double r = 210 * cam.zoom;
                Graphics2D gc = (Graphics2D) g.create();
                gc.translate(hit.getX() - 40 * cam.zoom, hit.getY());
                gc.rotate(Math.toRadians(-28));
                Path2D.Double crescent = new Path2D.Double();
                int steps = 40;
                double sweep = Math.PI * 1.1 * head;
                for (int i = 0; i <= steps; i++) {
                    double a = -Math.PI * 0.55 + sweep * i / steps;
                    double rr = r * (1 + 0.04 * Math.sin(i * 1.7));
                    if (i == 0) crescent.moveTo(Math.cos(a) * rr, Math.sin(a) * rr * 0.62);
                    else crescent.lineTo(Math.cos(a) * rr, Math.sin(a) * rr * 0.62);
                }
                for (int i = steps; i >= 0; i--) {
                    double a = -Math.PI * 0.55 + sweep * i / steps, mid = Math.sin(Math.PI * i / steps);
                    double rr = r * (0.72 - 0.16 * mid);
                    crescent.lineTo(Math.cos(a) * rr + 40 * cam.zoom * mid, Math.sin(a) * rr * 0.62);
                }
                crescent.closePath();
                gc.setColor(new Color(255, 230, 150, (int) (230 * keep)));
                gc.fill(crescent);
                Sketch.hatch(gc, crescent, 61, new Color(255, 120, 30), (int) (240 * keep), 7, 30);
                Sketch.hatch(gc, crescent, 62, color, (int) (200 * keep), 11, -40);
                gc.setColor(alpha(Sketch.INK, (int) (240 * keep)));
                gc.setStroke(new BasicStroke((float) (4 * cam.zoom / 1.4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                gc.draw(crescent);
                gc.dispose();
            }
            cut(g, Math.min(1, since / 700), hit.getX(), hit.getY(), cam.zoom, 230, 52, 38);
            // The page cracks from the point of impact, and shock rings roll out.
            double crack = clamp(since / 160, 0, 1), fade = clamp(1 - (since - 700) / 500, 0, 1);
            Random r = new Random(23);
            g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < 9; i++) {
                double a = r.nextDouble() * Math.PI * 2, len = (180 + r.nextDouble() * 220) * cam.zoom * crack;
                Path2D.Double line = new Path2D.Double();
                line.moveTo(hit.getX(), hit.getY());
                double x = hit.getX(), y = hit.getY();
                for (int k = 1; k <= 5; k++) {
                    x += Math.cos(a + (r.nextDouble() - 0.5) * 0.8) * len / 5;
                    y += Math.sin(a + (r.nextDouble() - 0.5) * 0.8) * len / 5;
                    line.lineTo(x, y);
                }
                g.setColor(alpha(Sketch.INK, (int) (230 * fade)));
                g.draw(line);
            }
            for (int ring = 0; ring < 3; ring++) {
                double v = clamp((since - ring * 110) / 650, 0, 1);
                if (v <= 0 || v >= 1) continue;
                double rr = (40 + 320 * (1 - Math.pow(1 - v, 3))) * cam.zoom;
                ellipse(g, hit, rr, rr * 0.55, 1, (float) (5 * (1 - v) + 1), alpha(ring == 1 ? new Color(255, 160, 40) : color, (int) (230 * (1 - v))), 31 + ring);
            }
            if (since < 500) embers(g, hit.getX(), hit.getY(), -Math.PI / 2 + Math.sin(since * 0.01), 70 * cam.zoom, ms);
        }
    }

    /**
     * 메테오: the hands go up over a magic circle, a second giant circle opens in a burning sky over the
     * target, and a huge flaming rock falls through it: a blast, a shock ring along the ground, flying rocks.
     */
    static final class Meteor extends Scene {
        static final double FALL = 1500, HIT = 2150;

        Meteor() {
            super(new Color(0xE0283C), 3300, 1250, new double[]{HIT}, new double[][]{
                    {0, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                    {320, 110, 110, 0, 0, 10, 0, 0, HERO_X, 1.6, 0, 0},
                    {800, 168, 168, 0, -3, 14, 0, 0, HERO_X, 1.45, 0, 0},
                    {1250, 176, 176, 0, -8, 16, 0, 0, HERO_X, 1.5, 0, 0},
                    {1500, 176, 176, 0, -8, 16, 0, 0, HERO_X, 1.0, 0.55, 0},
                    {2150, 150, 150, 0, -5, 14, 0, 0, HERO_X, 1.0, 0.6, 0},
                    {2700, 100, 100, 0, -2, 10, 0, 0, HERO_X, 1.05, 0.6, 0},
                    {3150, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
            });
            lookUp = 150;
        }

        @Override Color sky(double ms) {
            double burn = clamp((ms - 900) / 600, 0, 1) * clamp((HIT + 1000 - ms) / 600, 0, 1);
            return new Color(120, 20, 14, (int) (120 * burn));
        }

        @Override double shake(double ms) {
            double s = super.shake(ms);
            if (ms > FALL && ms < HIT) s = Math.max(s, 0.25 * (ms - FALL) / (HIT - FALL));
            return ms >= HIT ? Math.max(s, Math.pow(Math.max(0, 1 - (ms - HIT) / 700), 2)) : s;
        }

        @Override void under(Graphics2D g, Cam cam, double[] p, double ms) {
            magicCircle(g, cam.at(p[X], GROUND - 2), 130 * cam.zoom, 34 * cam.zoom, clamp((ms - 220) / 650, 0, 1),
                    clamp(1 - (ms - 2300) / 400, 0, 1), color, cam.zoom, 5);
            // The sky circle, turning slowly as the rock comes through it.
            double open = clamp((ms - 900) / 600, 0, 1), fade = clamp(1 - (ms - HIT) / 500, 0, 1);
            if (open > 0 && fade > 0) {
                Point2D c = cam.at(MON_X - 80, 46);
                double turn = ms * 0.0012;
                for (int ring = 0; ring < 3; ring++) {
                    double rr = (150 - ring * 32) * cam.zoom;
                    ellipse(g, c, rr, rr * 0.3, open, (float) (3.4 - ring), alpha(ring == 1 ? new Color(255, 170, 40) : color, (int) (235 * fade)), 40 + ring);
                }
                for (int i = 0; i < 8; i++) {   // runes around it
                    double a = turn + i * Math.PI / 4, rr = 132 * cam.zoom;
                    double x = c.getX() + Math.cos(a) * rr, y = c.getY() + Math.sin(a) * rr * 0.3;
                    g.setColor(alpha(new Color(255, 200, 80), (int) (230 * fade * open)));
                    g.fill(spark(x, y, 9 * cam.zoom));
                }
            }
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            double grow = clamp((ms - 500) / 800, 0, 1);
            if (grow > 0 && ms < FALL) {
                Point2D c = cam.at(p[X], GROUND - 262);
                fire(g, c.getX(), c.getY(), (10 + 30 * grow) * cam.zoom, -Math.PI / 2, 0.9, ms);
            }
            if (ms >= FALL && ms < HIT) {
                double u = Math.pow((ms - FALL) / (HIT - FALL), 1.6);
                Point2D from = cam.at(MON_X - 220, -160), to = cam.at(MON_X, HIT_Y);
                double x = lerp(from.getX(), to.getX(), u), y = lerp(from.getY(), to.getY(), u);
                double heading = Math.atan2(to.getY() - from.getY(), to.getX() - from.getX());
                double r = (58 + 30 * u) * cam.zoom;
                embers(g, x, y, heading + Math.PI, r * 1.1, ms);
                fire(g, x, y, r * 1.45, heading + Math.PI, 3.2, ms);
                Path2D.Double rock = blob(x, y, r * 0.78, 77);
                g.setColor(new Color(80, 50, 40));
                g.fill(rock);
                Sketch.hatch(g, rock, 71, new Color(40, 24, 20), 220, 6, 50);
                g.setColor(new Color(255, 150, 40, 200));
                g.setStroke(new BasicStroke((float) (3 * cam.zoom)));
                g.draw(rock);
            }
            if (ms >= HIT && ms < HIT + 1100) {
                double u = (ms - HIT) / 1100;
                Point2D c = cam.at(MON_X, HIT_Y + 20);
                explosion(g, c, cam.zoom, Math.min(1, u * 1.15), color, new Color(238, 134, 40), 1.7);
                double ring = clamp(u / 0.7, 0, 1);
                if (ring < 1) {
                    double rr = (60 + 460 * (1 - Math.pow(1 - ring, 3))) * cam.zoom;
                    Point2D ground = cam.at(MON_X, MON_GROUND);
                    ellipse(g, ground, rr, rr * 0.18, 1, (float) (6 * (1 - ring) + 1), alpha(new Color(255, 170, 60), (int) (240 * (1 - ring))), 51);
                }
                Random r = new Random(9);
                for (int i = 0; i < 12; i++) {   // rocks thrown up and falling back
                    double a = -Math.PI * (0.1 + 0.8 * r.nextDouble()), sp = (160 + r.nextDouble() * 220) * cam.zoom;
                    double x = c.getX() + Math.cos(a) * sp * u * 1.6, y = c.getY() + Math.sin(a) * sp * u * 1.6 + 420 * cam.zoom * u * u;
                    Path2D.Double chip = blob(x, y, (5 + r.nextDouble() * 7) * cam.zoom * (1 - u * 0.6), i);
                    g.setColor(alpha(new Color(70, 46, 36), (int) (240 * (1 - u))));
                    g.fill(chip);
                }
            }
        }
    }

    /**
     * 폭풍의 시: a whirlwind of emerald wind and leaves winds around the hero as the bow is drawn, then four
     * arrows fly in a row and a fifth, huge, rides a tornado of leaves into the target.
     */
    static final class StormSong extends Scene {
        static final double[] RELEASES = {1150, 1260, 1370, 1480, 1700};
        static final double FLIGHT = 240;

        StormSong() {
            super(new Color(0x2FD0A8), 3100, 1150, new double[]{1390, 1500, 1610, 1720, 1980}, new double[][]{
                    {0, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 40},
                    {300, 90, -40, 92, 0, 10, 1, 0.4, HERO_X, 1.8, 0, 40},
                    {560, 92, -40, 92, 0, 12, 1, 1, HERO_X, 2.0, 0, 60},
                    {1000, 92, -40, 92, -5, 14, 1, 1, HERO_X, 2.25, 0, 190},
                    {1140, 92, -40, 92, -5, 14, 1, 1, HERO_X, 2.3, 0, 192},
                    {1200, 94, -40, 92, 0, 14, 1, 1, HERO_X - 4, 1.2, 0.85, 120},
                    {1650, 94, -40, 92, -6, 14, 1, 1, HERO_X - 6, 1.15, 0.85, 200},
                    {1730, 96, 40, 92, 5, 16, 0.2, 1, HERO_X - 10, 1.1, 0.9, 40},
                    {2500, 92, 20, 92, 2, 10, 0, 0.6, HERO_X, 1.08, 0.85, 40},
                    {2950, 6, 6, 92, 0, 0, 0, 0, HERO_X, 1, 0, 40},
            });
        }

        @Override Color sky(double ms) {
            double wind = clamp((ms - 300) / 500, 0, 1) * clamp((end - 300 - ms) / 500, 0, 1);
            return new Color(20, 90, 80, (int) (55 * wind));
        }

        @Override void held(Graphics2D g, double[] p, double ms) {
            boolean nocked = true;
            for (double r : RELEASES) if (ms >= r && ms < r + 50) nocked = false;
            nocked &= ms < RELEASES[RELEASES.length - 1];
            bow(g, p[AMOUNT], nocked ? p[GRIP_DIST] : 0, nocked, color);
        }

        @Override void under(Graphics2D g, Cam cam, double[] p, double ms) {
            // The whirlwind: rings of wind spinning around the hero, tilting like a funnel.
            double grow = clamp((ms - 250) / 700, 0, 1), fade = clamp((2000 - ms) / 400, 0, 1);
            if (grow <= 0 || fade <= 0) return;
            Point2D feet = cam.at(p[X], GROUND);
            for (int ring = 0; ring < 5; ring++) {
                double y = feet.getY() - ring * 48 * cam.zoom, rr = (60 + ring * 22) * cam.zoom * grow;
                double start = ms * (0.008 + ring * 0.002), sweep = 4.2;
                Path2D.Double arc = new Path2D.Double();
                for (int i = 0; i <= 30; i++) {
                    double a = start + sweep * i / 30;
                    double x = feet.getX() + Math.cos(a) * rr, yy = y + Math.sin(a) * rr * 0.25;
                    if (i == 0) arc.moveTo(x, yy);
                    else arc.lineTo(x, yy);
                }
                g.setColor(alpha(ring % 2 == 0 ? color : new Color(160, 240, 210), (int) (200 * fade)));
                g.setStroke(new BasicStroke((float) (2.6 * cam.zoom / 1.6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(arc);
            }
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            gatherLeaves(g, cam, ms, 300, RELEASES[0], handX + 44 * cam.zoom, handY, color, 110, 10);
            Point2D from = cam.at(HERO_X + 70, GROUND - 130), to = cam.at(MON_X, HIT_Y);
            for (int i = 0; i < RELEASES.length; i++) {
                boolean big = i == RELEASES.length - 1;
                double spot = big ? 0 : (i - 1.5) * 14 * cam.zoom;
                Point2D aim = new Point2D.Double(to.getX(), to.getY() + spot);
                double u = (ms - RELEASES[i]) / FLIGHT;
                if (u >= 0 && u < 1) arrow(g, from, aim, u, (big ? 2.6 : 1.15) * cam.zoom, color, ms + i * 40);
                double since = ms - hits[i];
                if (since >= 0 && since < 400) ticks(g, aim.getX(), aim.getY(), since / 400, cam.zoom * (big ? 1.4 : 0.8), i + 5, color);
                if (big && since >= 0 && since < 1000) {
                    leafBurst(g, to, cam.zoom, since / 1000, color, 18, 1.8);
                    beam(g, from, to, Math.min(1, since / 700), color, 26, cam.zoom);
                    // The tornado standing on the target for a moment.
                    double v = since / 1000, keep = clamp(1 - (v - 0.5) / 0.5, 0, 1);
                    for (int ring = 0; ring < 6; ring++) {
                        double rr = (30 + ring * 20) * cam.zoom * (0.6 + 0.4 * v);
                        Point2D c = new Point2D.Double(to.getX(), to.getY() + 60 * cam.zoom - ring * 34 * cam.zoom);
                        ellipse(g, c, rr, rr * 0.28, 1, 2.6f, alpha(ring % 2 == 0 ? color : new Color(160, 240, 210), (int) (220 * keep)), 60 + ring);
                    }
                }
            }
        }
    }

    /**
     * 브로드사이드 (캡틴): the hero signals with a raised hand, a pirate ship sails in on the page behind and
     * fires three cannonballs in a row, then the hero finishes with both pistols at once.
     */
    static final class Broadside extends TwinPistols {
        static final double[] CANNONS = {1150, 1330, 1510};
        static final double CANNON_FLIGHT = 420, VOLLEY = 2120;

        Broadside() {
            super(new Color(0xFFB040), 3200, 1000, new double[]{1570, 1750, 1930, 2150}, new double[][]{
                    {0, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
                    {300, 160, 6, 0, -4, 8, 0, 0.8, HERO_X, 1.9, 0, 0},
                    {700, 168, 6, 0, -5, 10, 0, 1, HERO_X, 1.6, 0, 0},
                    {1000, 150, 6, 0, -3, 10, 0, 1, HERO_X, 1.0, 0.45, 0},
                    {1800, 120, 6, 0, -2, 10, 0, 1, HERO_X, 1.0, 0.45, 0},
                    {1960, 106, -72, 0, 0, 12, 0, 1, HERO_X, 1.5, 0.4, 0},
                    {2080, 106, -72, 0, 0, 12, 0, 1, HERO_X, 1.55, 0.45, 0},
                    {2130, 138, -104, 0, -8, 12, 0, 1, HERO_X - 14, 1.3, 0.55, 0},
                    {2400, 106, -72, 0, 0, 12, 0, 1, HERO_X - 2, 1.22, 0.55, 0},
                    {2800, 106, -72, 0, 0, 8, 0, 0.6, HERO_X, 1.15, 0.5, 0},
                    {3150, 6, 6, 0, 0, 0, 0, 0, HERO_X, 1, 0, 0},
            });
        }

        @Override Color sky(double ms) {
            double dusk = clamp((ms - 600) / 500, 0, 1) * clamp((end - 300 - ms) / 500, 0, 1);
            return new Color(90, 60, 20, (int) (60 * dusk));
        }

        @Override void under(Graphics2D g, Cam cam, double[] p, double ms) {
            double in = clamp((ms - 500) / 600, 0, 1), out = clamp((end - 400 - ms) / 500, 0, 1);
            if (in <= 0 || out <= 0) return;
            // The ship: hull, mast and sails in pencil, gliding in from the left on the far waves behind.
            double x0 = shipX(ms), y0 = SHIP_Y;
            Graphics2D gs = (Graphics2D) g.create();
            gs.transform(cam.t);
            gs.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) out));
            double bob = Math.sin(ms * 0.004) * 4;
            gs.translate(x0, y0 + bob);
            gs.scale(SHIP_SCALE, SHIP_SCALE);
            Path2D.Double hull = new Path2D.Double();
            hull.moveTo(0, 0);
            hull.lineTo(300, 0);
            hull.lineTo(260, 60);
            hull.lineTo(40, 60);
            hull.closePath();
            Sketch.hatch(gs, hull, 81, new Color(122, 78, 44), 230, 6, 40);
            Sketch.box(gs, 0, -2, 300, 4, 82, 3f, Sketch.INK);
            gs.setColor(Sketch.INK);
            gs.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gs.draw(hull);
            for (int i = 0; i < 3; i++) {   // gun ports, flashing when they fire
                double px = 80 + i * 70;
                double since = ms - CANNONS[i];
                gs.setColor(since >= 0 && since < 140 ? new Color(255, 210, 90) : Sketch.INK);
                gs.fill(new Ellipse2D.Double(px - 9, 18, 18, 18));
            }
            Sketch.line(gs, 150, 0, 150, -210, 83, 5f, Sketch.INK);
            Path2D.Double sail = new Path2D.Double();
            sail.moveTo(150, -200);
            sail.quadTo(230, -140, 150, -60);
            sail.closePath();
            gs.setColor(new Color(240, 232, 214));
            gs.fill(sail);
            Sketch.hatch(gs, sail, 84, Sketch.INK_SOFT, 120, 9, 70);
            gs.setColor(Sketch.INK);
            gs.draw(sail);
            gs.setColor(Sketch.INK);
            gs.fill(new Ellipse2D.Double(170, -150, 20, 18));   // a skull on the sail
            gs.setColor(new Color(240, 232, 214));
            gs.fill(new Ellipse2D.Double(174, -146, 5, 5));
            gs.fill(new Ellipse2D.Double(181, -146, 5, 5));
            Path2D.Double flag = new Path2D.Double();
            flag.moveTo(150, -210);
            flag.lineTo(195, -200 + Math.sin(ms * 0.01) * 4);
            flag.lineTo(150, -190);
            flag.closePath();
            gs.setColor(color);
            gs.fill(flag);
            gs.setColor(Sketch.INK);
            gs.draw(flag);
            for (int w = 0; w < 4; w++) {   // waves under the hull
                double wx = -40 + w * 100 + Math.sin(ms * 0.003 + w) * 10;
                Sketch.line(gs, wx, 66, wx + 60, 64, 85 + w, 2.6f, new Color(52, 120, 214));
            }
            gs.dispose();
        }

        static final double SHIP_Y = 150, SHIP_SCALE = 0.62;

        static double shipX(double ms) {
            return lerp(-260, 250, 1 - Math.pow(1 - clamp((ms - 500) / 600, 0, 1), 3));
        }

        @Override void over(Graphics2D g, Cam cam, double[] p, double ms) {
            Point2D to = cam.at(MON_X, HIT_Y);
            for (int i = 0; i < CANNONS.length; i++) {
                double u = (ms - CANNONS[i]) / CANNON_FLIGHT;
                Point2D port = cam.at(shipX(ms) + (80 + i * 70) * SHIP_SCALE, SHIP_Y + 27 * SHIP_SCALE);
                if (u >= 0 && u < 0.3) {   // smoke at the port
                    double s = u / 0.3;
                    Path2D.Double puff = blob(port.getX() + 10 * s * cam.zoom, port.getY() - 8 * s * cam.zoom, (12 + 26 * s) * cam.zoom, i + 3);
                    g.setColor(new Color(200, 196, 190, (int) (200 * (1 - s))));
                    g.fill(puff);
                    g.setColor(alpha(Sketch.INK_SOFT, (int) (200 * (1 - s))));
                    g.draw(puff);
                }
                if (u >= 0 && u < 1) {
                    double spot = (i - 1) * 16 * cam.zoom;
                    double x = lerp(port.getX(), to.getX(), u), y = lerp(port.getY(), to.getY() + spot, u) - Math.sin(u * Math.PI) * 120 * cam.zoom;
                    for (int k = 1; k <= 5; k++) {   // a dotted trail
                        double v = Math.max(0, u - k * 0.05);
                        double tx = lerp(port.getX(), to.getX(), v), ty = lerp(port.getY(), to.getY() + spot, v) - Math.sin(v * Math.PI) * 120 * cam.zoom;
                        g.setColor(alpha(Sketch.INK_SOFT, 160 - k * 25));
                        g.fill(new Ellipse2D.Double(tx - 3, ty - 3, 6, 6));
                    }
                    g.setColor(Sketch.INK);
                    g.fill(new Ellipse2D.Double(x - 13 * cam.zoom, y - 13 * cam.zoom, 26 * cam.zoom, 26 * cam.zoom));
                    g.setColor(new Color(255, 255, 255, 160));
                    g.fill(new Ellipse2D.Double(x - 7 * cam.zoom, y - 8 * cam.zoom, 6 * cam.zoom, 6 * cam.zoom));
                }
                double since = ms - hits[i];
                if (since >= 0 && since < 650) explosion(g, new Point2D.Double(to.getX(), to.getY() + (i - 1) * 16 * cam.zoom), cam.zoom * 0.7, since / 650, color, new Color(238, 134, 40), 0.8);
            }
            // The finishing volley: both pistols at once.
            if (ms >= VOLLEY - 40) {
                shot(g, cam, p, ms - VOLLEY, true, to, -8, color);
                shot(g, cam, p, ms - VOLLEY - 30, false, to, 10, color);
                double after = ms - hits[3];
                if (after >= 0 && after < 500) {
                    ticks(g, to.getX(), to.getY(), after / 500, cam.zoom * 1.3, 44, color);
                    Shape s = spark(to.getX(), to.getY(), 90 * cam.zoom * (1 - after / 500) + 10);
                    g.setColor(new Color(255, 214, 90, (int) (220 * (1 - after / 500))));
                    g.fill(s);
                }
            }
        }

        @Override void held(Graphics2D g, double[] p, double ms) {
            if (ms > 1850) pistol(g, p[AMOUNT], color);
        }

        @Override void heldLeft(Graphics2D g, double[] p, double ms) {
            if (ms > 1850) pistol(g, p[AMOUNT], color);
        }
    }

    // =====================================================================================
    // pencil pieces shared by the scenes
    // =====================================================================================

    /** A magic circle pencilled on the ground: two rings and a five-point star drawn as `drawn` goes to 1. */
    static void magicCircle(Graphics2D g, Point2D c, double rx, double ry, double drawn, double fade, Color color, double zoom, int points) {
        if (drawn <= 0 || fade <= 0) return;
        Color line = alpha(color, (int) (235 * fade));
        ellipse(g, c, rx, ry, drawn, (float) (3.2 * zoom / 1.5), line, 3);
        ellipse(g, c, rx * 0.76, ry * 0.76, drawn, (float) (2 * zoom / 1.5), line, 9);
        double shown = drawn * points;
        for (int i = 0; i < points && shown > 0; i++) {
            double a = -Math.PI / 2 + i * 2 * 2 * Math.PI / points, b = -Math.PI / 2 + (i + 1) * 2 * 2 * Math.PI / points;
            double part = Math.min(1, shown);
            double ax = c.getX() + Math.cos(a) * rx * 0.76, ay = c.getY() + Math.sin(a) * ry * 0.76;
            double bx = c.getX() + Math.cos(b) * rx * 0.76, by = c.getY() + Math.sin(b) * ry * 0.76;
            Sketch.line(g, ax, ay, lerp(ax, bx, part), lerp(ay, by, part), i + 21, 2f, line);
            shown -= 1;
        }
    }

    /** A ball of fire: layers of flickering flame around a bright core, with tongues trailing toward `tail`. */
    static void fire(Graphics2D g, double x, double y, double r, double tail, double reach, double ms) {
        long step = (long) (ms / 50);
        Color[] layers = {new Color(205, 42, 30), new Color(240, 112, 28), new Color(250, 190, 44), new Color(255, 240, 170)};
        double[] sizes = {1, 0.76, 0.52, 0.28};
        for (int i = 0; i < layers.length; i++) {
            Path2D.Double flame = flameShape(x, y, r * sizes[i], tail, reach * (1 - i * 0.2), step + i * 5, i);
            g.setColor(alpha(layers[i], i == 0 ? 225 : 245));
            g.fill(flame);
            if (i < 2) Sketch.hatch(g, flame, step + i, i == 0 ? new Color(120, 20, 18) : new Color(205, 42, 30), 150, 6, 55);
            if (i == 0) {
                g.setColor(Sketch.INK);
                g.setStroke(new BasicStroke((float) Math.max(2.2, r / 18), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(flame);
            }
        }
    }

    static Path2D.Double flameShape(double x, double y, double r, double tail, double reach, long seed, int layer) {
        Random rnd = new Random(seed * 37 + layer);
        int tongues = 7;
        double[] phase = new double[tongues], height = new double[tongues];
        for (int i = 0; i < tongues; i++) {
            phase[i] = (i + (rnd.nextDouble() - 0.5) * 0.5) / tongues * Math.PI * 2;
            height[i] = 0.45 + rnd.nextDouble() * 0.6;
        }
        Path2D.Double p = new Path2D.Double();
        int steps = 84;
        for (int i = 0; i <= steps; i++) {
            double a = Math.PI * 2 * i / steps, toTail = Math.cos(a - tail);
            double behind = Math.max(0, toTail);
            double lick = 0;
            for (int k = 0; k < tongues; k++) {
                double d = Math.abs(Math.atan2(Math.sin(a - phase[k]), Math.cos(a - phase[k])));
                lick = Math.max(lick, height[k] * Math.max(0, 1 - d / 0.42));
            }
            double d = r * (0.82 + (0.1 + 0.38 * behind) * lick + reach * Math.pow(behind, 3.2) * (0.25 + lick));
            double px = x + Math.cos(a) * d, py = y + Math.sin(a) * d;
            if (i == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }
        p.closePath();
        return p;
    }

    /** Sparks left behind a flying fire. */
    static void embers(Graphics2D g, double x, double y, double tail, double r, double ms) {
        Random rnd = new Random((long) (ms / 50));
        for (int i = 0; i < 14; i++) {
            double d = r * (1.4 + rnd.nextDouble() * 2.8), a = tail + (rnd.nextDouble() - 0.5) * 0.7, s = 2 + rnd.nextDouble() * 3.5;
            g.setColor(i % 3 == 0 ? new Color(205, 42, 30) : new Color(250, 170, 40));
            g.fill(new Ellipse2D.Double(x + Math.cos(a) * d - s, y + Math.sin(a) * d - s, s * 2, s * 2));
        }
    }

    /** A bow in the hand, drawn in as amount goes to 1, its string pulled back `pull`, an arrow on it if nocked. */
    static void bow(Graphics2D g, double a, double pull, boolean nocked, Color color) {
        double[][] curve = new double[17][];
        for (int i = 0; i <= 16; i++) {
            double x = -190 + 380 * i / 16.0;
            curve[i] = new double[]{x, 52 - 90 * Math.pow(x / 190, 2)};
        }
        int shown = (int) Math.round(a * 16);
        for (int i = 0; i < shown; i++) Sketch.line(g, curve[i][0], curve[i][1], curve[i + 1][0], curve[i + 1][1], i, 12f, Sketch.INK);
        if (a < 1) return;
        Sketch.line(g, -190, -38, 0, -pull, 3, 4f, Sketch.INK_SOFT);
        Sketch.line(g, 190, -38, 0, -pull, 4, 4f, Sketch.INK_SOFT);
        if (nocked) {
            double tip = 340 - pull;
            Sketch.line(g, 0, -pull, 0, tip, 5, 12f, color);
            Sketch.line(g, -22, tip - 46, 0, tip, 6, 11f, Sketch.INK);
            Sketch.line(g, 22, tip - 46, 0, tip, 7, 11f, Sketch.INK);
        }
    }

    /** One arrow on its way from `from` to `to` (u 0..1), with speed lines and wind wound around its path. */
    static void arrow(Graphics2D g, Point2D from, Point2D to, double u, double size, Color color, double ms) {
        double x = lerp(from.getX(), to.getX(), u), y = lerp(from.getY(), to.getY(), u);
        Graphics2D ga = (Graphics2D) g.create();
        ga.translate(x, y);
        ga.rotate(Math.atan2(to.getY() - from.getY(), to.getX() - from.getX()));
        for (int i = 0; i < 6; i++) {
            double off = (i - 2.5) * 9 * size;
            Sketch.line(ga, -150 * size - i * 34 * (i % 2 == 0 ? 1 : 0.4), off, -62 * size, off * 0.4, i + (long) (ms / 40), 2.4f, i % 2 == 0 ? color : Sketch.INK_SOFT);
        }
        for (int side = -1; side <= 1; side += 2) {
            Path2D.Double wind = new Path2D.Double();
            for (int i = 0; i <= 40; i++) {
                double wx = -250 * size + 250 * size * i / 40.0;
                double wy = side * Math.sin(wx * 0.045 / size + ms * 0.03) * 19 * size * (0.35 + 0.65 * i / 40.0);
                if (i == 0) wind.moveTo(wx, wy);
                else wind.lineTo(wx, wy);
            }
            ga.setColor(alpha(color, 210));
            ga.setStroke(new BasicStroke((float) (2.6 * size), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            ga.draw(wind);
        }
        ga.setColor(color);
        ga.setStroke(new BasicStroke((float) (6 * size), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        ga.draw(new Line2D.Double(-74 * size, 0, 0, 0));
        for (int side = -1; side <= 1; side += 2) {
            ga.setStroke(new BasicStroke((float) (4.5 * size), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            ga.draw(new Line2D.Double(-58 * size, 0, -78 * size, side * 13 * size));
            ga.draw(new Line2D.Double(-44 * size, 0, -64 * size, side * 13 * size));
        }
        Path2D.Double head = new Path2D.Double();
        head.moveTo(24 * size, 0);
        head.lineTo(-6 * size, -12 * size);
        head.lineTo(0, 0);
        head.lineTo(-6 * size, 12 * size);
        head.closePath();
        ga.fill(head);
        ga.setColor(Sketch.INK);
        ga.setStroke(new BasicStroke((float) (2.4 * size), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        ga.draw(head);
        ga.dispose();
    }

    /** The arrow's wake through the target: a hatched wedge that thins out (u 0..1). */
    static void beam(Graphics2D g, Point2D from, Point2D to, double u, Color color, double width, double zoom) {
        double keep = clamp(1 - (u - 0.45) / 0.55, 0, 1);
        double dx = to.getX() - from.getX(), dy = to.getY() - from.getY(), len = Math.hypot(dx, dy);
        double ex = to.getX() + dx / len * 340, ey = to.getY() + dy / len * 340;
        double w = width * keep * zoom / 1.2;
        Path2D.Double wedge = new Path2D.Double();
        wedge.moveTo(to.getX() - dx / len * 130, to.getY() - dy / len * 130);
        wedge.lineTo(ex - dy / len * w, ey + dx / len * w);
        wedge.lineTo(ex + dy / len * w, ey - dx / len * w);
        wedge.closePath();
        Sketch.hatch(g, wedge, 3, color, (int) (230 * keep), 6, 60);
        g.setColor(alpha(color, (int) (245 * keep)));
        g.setStroke(new BasicStroke(3f));
        g.draw(wedge);
        ticks(g, to.getX(), to.getY(), u / 0.6, zoom, 7, color);
    }

    /** Leaves circling a point on the wind and closing in as the shot is readied. */
    static void gatherLeaves(Graphics2D g, Cam cam, double ms, double from, double until, double cx, double cy, Color color, double start, int count) {
        double u = clamp((ms - from) / (until - from), 0, 1);
        if (u <= 0 || ms >= until) return;
        double radius = lerp(start, 26, u * u) * cam.zoom;
        for (int ring = 0; ring < 2; ring++) {
            double a0 = ms * 0.006 + ring * 2.4, sweep = 2.2 + u * 1.5, rr = radius * (1 - ring * 0.25);
            Path2D.Double arc = new Path2D.Double();
            for (int i = 0; i <= 26; i++) {
                double a = a0 + sweep * i / 26;
                if (i == 0) arc.moveTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr * 0.62);
                else arc.lineTo(cx + Math.cos(a) * rr, cy + Math.sin(a) * rr * 0.62);
            }
            g.setColor(alpha(color, (int) (190 * Math.min(1, u * 3))));
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(arc);
        }
        for (int i = 0; i < count; i++) {
            if (u * 2.2 < i / (double) count) continue;
            double a = ms * 0.007 + i * Math.PI * 2 / count;
            leaf(g, cx + Math.cos(a) * radius, cy + Math.sin(a) * radius * 0.62, 6.5 * cam.zoom, a + 1.4, i, color);
        }
    }

    /** The hit: a ring of wind and a burst of leaves that spread, then flutter down. */
    static void leafBurst(Graphics2D g, Point2D c, double zoom, double u, Color color, int count, double size) {
        double ring = clamp(u / 0.4, 0, 1);
        if (ring < 1) {
            double r = (24 + 80 * size * (1 - Math.pow(1 - ring, 3))) * zoom;
            g.setColor(alpha(color, (int) (230 * (1 - ring))));
            g.setStroke(new BasicStroke((float) (4.5 * (1 - ring) + 1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Ellipse2D.Double(c.getX() - r, c.getY() - r * 0.6, r * 2, r * 1.2));
        }
        Random rnd = new Random(29);
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, speed = (45 + rnd.nextDouble() * 60) * size, spin = rnd.nextDouble() * 6;
            double out = 1 - Math.pow(1 - Math.min(1, u * 1.7), 3);
            double x = c.getX() + Math.cos(a) * speed * out * zoom + Math.sin(u * 9 + i) * 7 * zoom * u;
            double y = c.getY() + Math.sin(a) * speed * 0.62 * out * zoom + u * u * 75 * zoom;
            double s = (5 + rnd.nextDouble() * 3) * zoom * (u < 0.7 ? 1 : (1 - u) / 0.3);
            leaf(g, x, y, s, spin + u * 7, i, color);
        }
    }

    /** One small pencil leaf; every third is lighter. */
    static void leaf(Graphics2D g, double x, double y, double size, double turn, int index, Color color) {
        if (size < 1.5) return;
        Graphics2D gl = (Graphics2D) g.create();
        gl.translate(x, y);
        gl.rotate(turn);
        Path2D.Double blade = new Path2D.Double();
        blade.moveTo(-size * 1.3, 0);
        blade.quadTo(0, -size * 1.05, size * 1.3, 0);
        blade.quadTo(0, size * 1.05, -size * 1.3, 0);
        gl.setColor(index % 3 == 0 ? new Color(120, 228, 200) : color);
        gl.fill(blade);
        gl.setColor(Sketch.INK);
        gl.setStroke(new BasicStroke((float) Math.max(1.2, size / 6), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gl.draw(blade);
        gl.draw(new Line2D.Double(-size * 0.9, 0, size * 0.9, 0));
        gl.dispose();
    }

    /** A thrown star's tapered streak along its arc: dark outside, colored inside. */
    static void streak(Graphics2D g, Point2D from, Point2D to, double spot, double u, double zoom, Color inside, Color outside) {
        double back = Math.max(0, u - 0.32);
        int steps = 12;
        double[][] pts = new double[steps + 1][];
        for (int k = 0; k <= steps; k++) {
            double v = lerp(back, u, k / (double) steps);
            pts[k] = new double[]{lerp(from.getX(), to.getX(), v), lerp(from.getY(), to.getY() + spot, v) - Math.sin(v * Math.PI) * 26};
        }
        for (int pass = 0; pass < 2; pass++) {
            Path2D.Double band = new Path2D.Double();
            double width = (pass == 0 ? 15 : 7) * zoom;
            for (int k = 0; k <= steps; k++) {
                double w = width * k / steps;
                double[] a = pts[Math.max(0, k - 1)], b = pts[Math.min(steps, k + 1)];
                double dx = b[0] - a[0], dy = b[1] - a[1], len = Math.max(1e-6, Math.hypot(dx, dy));
                double px = pts[k][0] - dy / len * w, py = pts[k][1] + dx / len * w;
                if (k == 0) band.moveTo(px, py);
                else band.lineTo(px, py);
            }
            for (int k = steps; k >= 0; k--) {
                double w = width * k / steps;
                double[] a = pts[Math.max(0, k - 1)], b = pts[Math.min(steps, k + 1)];
                double dx = b[0] - a[0], dy = b[1] - a[1], len = Math.max(1e-6, Math.hypot(dx, dy));
                band.lineTo(pts[k][0] + dy / len * w, pts[k][1] - dx / len * w);
            }
            band.closePath();
            g.setColor(alpha(pass == 0 ? outside : inside, pass == 0 ? 200 : 235));
            g.fill(band);
        }
    }

    /** Where a star bites: a jagged splash that spreads and thins out. */
    static void shadowBurst(Graphics2D g, double x, double y, double zoom, double u, int seed, Color inside, Color outside) {
        double grow = 1 - Math.pow(1 - clamp(u / 0.35, 0, 1), 3), keep = clamp(1 - (u - 0.3) / 0.7, 0, 1);
        double r = 52 * zoom * grow;
        Path2D.Double outer = spiky(x, y, r, r * 0.45, 9, seed + 40), inner = spiky(x, y, r * 0.55, r * 0.25, 7, seed + 50);
        g.setColor(alpha(outside, (int) (200 * keep)));
        g.fill(outer);
        g.setColor(alpha(inside, (int) (230 * keep)));
        g.fill(inner);
        g.setColor(alpha(Sketch.INK, (int) (230 * keep)));
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(outer);
    }

    static void focusLines(Graphics2D g, double amount, int seed, Color c) {
        Random r = new Random(seed * 31L + 7);
        double cx = W * 0.5, cy = H * 0.5, far = Math.hypot(W, H) * 0.6;
        for (int i = 0; i < 64; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double near = lerp(far, far * (0.62 + r.nextDouble() * 0.26), amount);
            Sketch.line(g, cx + Math.cos(a) * near, cy + Math.sin(a) * near * 0.72, cx + Math.cos(a) * far, cy + Math.sin(a) * far * 0.72,
                    i + seed, i % 5 == 0 ? 2.6f : 1.4f, c);
        }
    }

    /** Pencil ticks bursting out; u runs 0..1. */
    static void ticks(Graphics2D g, double cx, double cy, double u, double zoom, long seed, Color c) {
        if (u < 0 || u >= 1) return;
        Random r = new Random(seed);
        for (int i = 0; i < 14; i++) {
            double a = r.nextDouble() * Math.PI * 2, speed = 0.6 + r.nextDouble() * 0.6;
            double d0 = (14 + u * 90 * speed) * zoom, len = (24 * (1 - u) + 7) * zoom;
            Sketch.line(g, cx + Math.cos(a) * d0, cy + Math.sin(a) * d0, cx + Math.cos(a) * (d0 + len), cy + Math.sin(a) * (d0 + len), i, 2.6f, i % 3 == 0 ? Sketch.INK : c);
        }
    }

    /** A scribbled blast that spreads, then rubs away; u runs 0..1. */
    static void explosion(Graphics2D g, Point2D c, double zoom, double u, Color inner, Color outer, double size) {
        double grow = 1 - Math.pow(1 - clamp(u / 0.4, 0, 1), 3), keep = clamp(1 - (u - 0.45) / 0.55, 0, 1);
        double r = 190 * zoom * grow * size;
        Path2D.Double big = spiky(c.getX(), c.getY(), r, r * 0.68, 13, 3), small = spiky(c.getX(), c.getY(), r * 0.6, r * 0.38, 11, 9);
        Sketch.hatch(g, big, 4, outer, (int) (230 * keep), 7, 35);
        Sketch.hatch(g, small, 5, inner, (int) (240 * keep), 6, -40);
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(alpha(Sketch.INK, (int) (240 * keep)));
        g.draw(big);
        g.setColor(alpha(inner, (int) (240 * keep)));
        g.draw(small);
        ticks(g, c.getX(), c.getY(), u, zoom * 1.7 * size, 5, outer);
    }

    static Path2D.Double spiky(double cx, double cy, double out, double in, int points, long seed) {
        Random r = new Random(seed);
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < points * 2; i++) {
            double a = Math.PI * i / points, d = (i % 2 == 0 ? out : in) * (0.85 + r.nextDouble() * 0.3);
            double x = cx + Math.cos(a) * d, y = cy + Math.sin(a) * d * 0.8;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        p.closePath();
        return p;
    }

    static Path2D.Double blob(double cx, double cy, double r, long seed) {
        Random rnd = new Random(seed);
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12, d = r * (0.82 + rnd.nextDouble() * 0.28);
            if (i == 0) p.moveTo(cx + Math.cos(a) * d, cy + Math.sin(a) * d);
            else p.lineTo(cx + Math.cos(a) * d, cy + Math.sin(a) * d);
        }
        p.closePath();
        return p;
    }

    static void ellipse(Graphics2D g, Point2D c, double rx, double ry, double amount, float stroke, Color color, long seed) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i <= 48 * amount; i++) {
            double a = -Math.PI / 2 + Math.PI * 2 * i / 48, w = 1 + Math.sin(i * 1.3 + seed) * 0.012;
            double x = c.getX() + Math.cos(a) * rx * w, y = c.getY() + Math.sin(a) * ry * w;
            if (i == 0) p.moveTo(x, y);
            else p.lineTo(x, y);
        }
        g.setColor(color);
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(p);
    }

    static void throwingStar(Graphics2D g, double x, double y, double r, double turn, Color c) {
        if (r < 1) return;
        Graphics2D gs = (Graphics2D) g.create();
        gs.translate(x, y);
        gs.rotate(turn);
        Path2D.Double s = new Path2D.Double();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * i / 4, d = i % 2 == 0 ? r : r * 0.32;
            if (i == 0) s.moveTo(Math.cos(a) * d, Math.sin(a) * d);
            else s.lineTo(Math.cos(a) * d, Math.sin(a) * d);
        }
        s.closePath();
        gs.setColor(Sketch.PAPER);
        gs.fill(s);
        Sketch.hatch(gs, s, 3, c, 235, Math.max(3, r / 5), 45);
        gs.setColor(Sketch.INK);
        gs.setStroke(new BasicStroke((float) Math.max(2, r / 9), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gs.draw(s);
        gs.dispose();
    }

    /** An eight-point sparkle. */
    static Shape spark(double x, double y, double r) {
        Path2D.Double p = new Path2D.Double();
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * i / 8, d = i % 2 == 0 ? r : r * 0.4;
            if (i == 0) p.moveTo(x + Math.cos(a) * d, y + Math.sin(a) * d);
            else p.lineTo(x + Math.cos(a) * d, y + Math.sin(a) * d);
        }
        p.closePath();
        return p;
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static double lerp(double a, double b, double u) {
        return a + (b - a) * u;
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
