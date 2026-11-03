import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

import static java.lang.Math.*;

/**
 * Battle backdrops: a scene penciled onto a sheet of ruled notebook paper, W x H like dungeon1/bg.png.
 * In battle the art is cropped to a wide band around y ~190..820 and the monster stands at y ~590 on
 * the right, the player at the bottom-left, so each scene keeps its floor open around those spots.
 */
final class Backdrops {
    static final int W = 1672, H = 941;
    /** Where the back of the floor meets the scene; the monster's feet land a little below this. */
    static final double HORIZON = 560;
    static final double VX = 836, VY = 470;   // floor vanishing point

    private Backdrops() {}

    /** Plain notebook paper: cream page with a faint grain. Ruled lines go on last (see rule()). */
    static BufferedImage paper(long seed) {
        return paper(W, H, seed);
    }

    static BufferedImage paper(int w, int h, long seed) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Pencil.PAPER);
        g.fillRect(0, 0, w, h);
        Random r = new Random(seed);
        for (int i = 0; i < w * h / 175; i++) {
            int x = r.nextInt(w), y = r.nextInt(h);
            g.setColor(new Color(120, 110, 90, 10 + r.nextInt(16)));
            g.fillRect(x, y, 1 + r.nextInt(3), 1);
        }
        g.dispose();
        return img;
    }

    /** Blue ruled lines and the red margin, drawn over the scene so the page shows through the drawing. */
    static void rule(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.2f));
        int w = img.getWidth(), h = img.getHeight();
        g.setColor(new Color(110, 150, 205, 70));
        for (int y = 96; y < h; y += 44) g.drawLine(0, y, w, y);
        g.setColor(new Color(214, 92, 92, 90));
        g.drawLine(150, 0, 150, h);
        g.drawLine(156, 0, 156, h);
        g.dispose();
    }

    /** A floor from the horizon down: light shading, perspective cracks toward the vanishing point. */
    static void floor(Pencil p, double tone, double wave) {
        p.part(Pencil.blob(-60, HORIZON + 10, 400, HORIZON - wave, 836, HORIZON + wave * 0.5, 1250, HORIZON - wave,
                W + 60, HORIZON + 10, W + 60, H + 60, -60, H + 60), tone);
        for (int i = 0; i < 11; i++) {
            double bx = -200 + i * 207 + p.r.nextDouble() * 40;
            double t = 0.12 + p.r.nextDouble() * 0.1;
            p.line(VX + (bx - VX) * t, HORIZON + 20 + (H - HORIZON) * t * 0.6, bx, H + 20, 1.6,
                    Pencil.alpha(Pencil.LEAD, 70));
        }
        for (int i = 0; i < 5; i++) {
            double y = HORIZON + 40 + i * i * 22;
            p.line(-20, y, W + 20, y + p.r.nextDouble() * 10, 1.4, Pencil.alpha(Pencil.LEAD, 50));
        }
    }

    static void pebbles(Pencil p, int n, double tone) {
        for (int i = 0; i < n; i++) {
            double x = p.r.nextDouble() * W, y = HORIZON + 60 + p.r.nextDouble() * (H - HORIZON - 60);
            double s = 8 + (y - HORIZON) * 0.06 + p.r.nextDouble() * 10;
            p.part(Pencil.rock(p.r, x, y, s * 1.4, s * 0.9, 7), tone);
        }
    }

    // ================= 1 : 초원 =================

    static void tree(Pencil p, double x, double baseY, double size) {
        p.part(Pencil.limb(26 * size, x, baseY, x + 4, baseY - 150 * size), 0.55, Monsters.BROWN, 0.6);
        java.awt.geom.Area crown = new java.awt.geom.Area();
        double[][] puffs = {{0, -200, 90}, {-70, -160, 70}, {70, -165, 72}, {-40, -250, 70}, {45, -245, 66}};
        for (double[] q : puffs) {
            crown.add(new java.awt.geom.Area(Pencil.ell(x + q[0] * size, baseY + q[1] * size, q[2] * size, q[2] * size * 0.85)));
        }
        p.part(crown, 0.3, Monsters.GREEN, 0.7);
    }

    static void meadow(Pencil p) {
        // Sun with rays, a couple of clouds.
        p.part(Pencil.ell(1320, 120, 62, 62), 0.0, Monsters.YELLOW, 0.8);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            p.colorLine(3, Monsters.YELLOW, 1320 + Math.cos(a) * 82, 120 + Math.sin(a) * 82, 1320 + Math.cos(a) * 112, 120 + Math.sin(a) * 112);
        }
        cloud(p, 470, 140, 300, 0.03);
        cloud(p, 980, 90, 220, 0.03);
        // Rolling hills, far then near.
        p.part(Pencil.blob(-80, HORIZON, 160, 360, 460, 410, 760, 330, 1080, 400, 1380, 340, W + 80, 420, W + 80, HORIZON + 20,
                -80, HORIZON + 20), 0.12, Monsters.GREEN, 0.35);
        p.part(Pencil.blob(-80, HORIZON + 10, 300, 450, 640, 490, 1000, 450, 1360, 480, W + 80, 450, W + 80, HORIZON + 30,
                -80, HORIZON + 30), 0.18, Monsters.GREEN, 0.45);
        // The meadow itself: a light green floor with a dirt path winding toward the hills.
        p.part(Pencil.poly(-20, HORIZON + 10, W + 20, HORIZON + 10, W + 20, H + 20, -20, H + 20), 0.06, Monsters.GREEN, 0.4);
        p.part(Pencil.blob(760, HORIZON + 12, 880, HORIZON + 12, 960, 700, 1100, 820, 1180, H + 20, 600, H + 20, 680, 800, 740, 690),
                0.12, Monsters.BROWN, 0.45);
        tree(p, 230, HORIZON + 40, 1.25);
        tree(p, 520, HORIZON - 8, 0.7);
        tree(p, 1560, HORIZON + 60, 1.35);
        for (double[] bsh : new double[][]{{380, HORIZON + 30}, {1100, HORIZON + 20}, {1440, HORIZON + 110}}) {
            p.part(Pencil.rock(p.r, bsh[0], bsh[1], 70, 40, 9), 0.3, Monsters.GREEN, 0.7);
        }
        // Grass tufts and little flowers.
        for (int i = 0; i < 70; i++) {
            double x = p.r.nextDouble() * W, y = HORIZON + 40 + p.r.nextDouble() * (H - HORIZON - 40);
            double s = 6 + (y - HORIZON) * 0.04;
            p.ink(x, y, x - s * 0.6, y - s * 1.6, 1.6);
            p.ink(x, y, x + 1, y - s * 2, 1.6);
            p.ink(x, y, x + s * 0.7, y - s * 1.5, 1.6);
        }
        for (int i = 0; i < 28; i++) {
            double x = p.r.nextDouble() * W, y = HORIZON + 50 + p.r.nextDouble() * (H - HORIZON - 60);
            Color petal = i % 3 == 0 ? Monsters.RED : i % 3 == 1 ? Monsters.YELLOW : Monsters.PURPLE;
            for (int k = 0; k < 5; k++) {   // five soft colored-pencil petals around a yellow middle
                double a = Math.PI * 2 * k / 5;
                p.smudge(Pencil.ell(x + Math.cos(a) * 6, y + Math.sin(a) * 4, 4.5, 3.5), petal, 200);
            }
            p.smudge(Pencil.ell(x, y, 2.6, 2.2), Monsters.YELLOW, 240);
        }
        // A few birds.
        for (int i = 0; i < 4; i++) {
            double x = 700 + i * 90, y = 220 + (i % 2) * 40;
            p.curve(2.4, x - 18, y, x - 8, y - 10, x, y);
            p.curve(2.4, x, y, x + 8, y - 10, x + 18, y);
        }
    }

    // ================= 2 : 어두운 동굴 =================

    static void cave(Pencil p) {
        // Back wall of the cavern.
        p.part(Pencil.poly(-20, -20, W + 20, -20, W + 20, HORIZON + 20, -20, HORIZON + 20), 0.42);
        // Side walls bulging in.
        p.part(Pencil.blob(-80, -40, 260, 80, 330, 300, 240, 520, 360, 700, -80, 760), 0.75);
        p.part(Pencil.blob(W + 80, -40, W - 250, 60, W - 340, 260, W - 220, 470, W - 300, 640, W + 80, 700), 0.75);
        // A glowing crystal cluster at the back.
        for (int i = 0; i < 4; i++) {
            p.part(Pencil.flame(640 + i * 34, HORIZON - 10, 40, 120 + i % 2 * 60, -10 + i * 8), 0.1, Monsters.ICE, 0.9);
        }
        p.smudge(Pencil.ell(690, HORIZON - 70, 160, 110), Monsters.ICE, 22);
        floor(p, 0.12, 24);
        // Stalactites from the ceiling.
        for (int i = 0; i < 14; i++) {
            double x = 160 + i * 105 + p.r.nextDouble() * 40;
            double len = 90 + p.r.nextDouble() * (i % 3 == 0 ? 230 : 120);
            double w = 34 + p.r.nextDouble() * 30;
            p.part(Pencil.poly(x - w / 2, -10, x + w / 2, -10, x + w * 0.1, len), 0.6);
        }
        // Stalagmites at the sides of the floor, clear of where the fighters stand.
        double[][] mites = {{380, 560, 50, 150}, {460, 580, 34, 90}, {1080, 570, 46, 130}, {1580, 640, 70, 210}};
        for (double[] m : mites) p.part(Pencil.poly(m[0] - m[2], m[1], m[0] + m[2], m[1], m[0] + 4, m[1] - m[3]), 0.55);
        pebbles(p, 16, 0.45);
    }

    // ================= 3 : 잊혀진 폐허 =================

    /** A fluted column standing on the floor at x, broken off at a jagged top. */
    static void column(Pencil p, double x, double baseY, double w, double h, boolean broken, double tone) {
        double top = baseY - h;
        if (broken) {
            p.part(Pencil.poly(x - w / 2, baseY, x + w / 2, baseY, x + w / 2, top + 30, x + w * 0.2, top + 60,
                    x - w * 0.05, top, x - w * 0.3, top + 40, x - w / 2, top + 20), tone);
        } else {
            p.part(Pencil.poly(x - w / 2, baseY, x + w / 2, baseY, x + w / 2, top, x - w / 2, top), tone);
            p.part(Pencil.poly(x - w * 0.7, top, x + w * 0.7, top, x + w * 0.6, top - 26, x - w * 0.6, top - 26), tone + 0.1);
        }
        for (int i = 1; i < 4; i++) p.ink(x - w / 2 + w * i / 4, baseY - 6, x - w / 2 + w * i / 4, top + (broken ? 70 : 6), 1.6);
        p.part(Pencil.poly(x - w * 0.7, baseY + 4, x + w * 0.7, baseY + 4, x + w * 0.6, baseY - 22, x - w * 0.6, baseY - 22), tone + 0.1);
    }

    static void vine(Pencil p, double x, double y, double len) {
        double[] pts = new double[12];
        for (int i = 0; i < 6; i++) {
            pts[i * 2] = x + sin(i * 1.3) * 26;
            pts[i * 2 + 1] = y + len * i / 5;
        }
        p.curve(2.4, pts);
        for (int i = 1; i < 6; i++) {
            double lx = pts[i * 2], ly = pts[i * 2 + 1];
            p.part(Pencil.blob(lx, ly, lx + 26, ly - 14, lx + 40, ly + 4, lx + 18, ly + 14), 0.15, Monsters.GREEN, 0.8);
        }
    }

    static void ruins(Pencil p) {
        p.part(Pencil.blob(-60, HORIZON, 200, 430, 520, 470, 820, 400, 1150, 460, 1450, 410, W + 60, HORIZON, W + 60,
                HORIZON + 40, -60, HORIZON + 40), 0.12);   // distant hills
        // A broken arch at the back.
        column(p, 640, HORIZON, 70, 300, false, 0.3);
        column(p, 900, HORIZON, 70, 300, false, 0.3);
        java.awt.geom.Area arch = new java.awt.geom.Area(Pencil.ell(770, HORIZON - 326, 200, 130));
        arch.subtract(new java.awt.geom.Area(Pencil.ell(770, HORIZON - 326, 130, 80)));
        arch.subtract(new java.awt.geom.Area(new Rectangle(500, (int) (HORIZON - 326), 600, 300)));
        arch.subtract(new java.awt.geom.Area(new Rectangle(830, 0, 200, 400)));   // the right half has fallen
        p.part(arch, 0.35);
        floor(p, 0.1, 14);
        column(p, 300, HORIZON + 40, 96, 420, false, 0.35);
        column(p, 470, HORIZON + 10, 80, 170, true, 0.35);
        column(p, 1100, HORIZON + 6, 70, 260, true, 0.35);
        column(p, 1590, HORIZON + 90, 110, 540, false, 0.4);
        vine(p, 300, HORIZON - 410, 300);
        vine(p, 1590, HORIZON - 440, 380);
        // A fallen drum of a column and rubble.
        p.part(Pencil.blob(560, 770, 760, 740, 790, 800, 590, 830), 0.35);
        p.part(Pencil.ell(780, 772, 26, 32), 0.2);
        for (int i = 0; i < 10; i++) {
            double x = 380 + p.r.nextDouble() * 1250, y = HORIZON + 50 + p.r.nextDouble() * 300;
            p.part(Pencil.poly(x, y, x + 50, y - 6, x + 56, y + 26, x + 4, y + 30), 0.3);
        }
    }

    // ================= 4 : 마왕성 =================

    static void tower(Pencil p, double x, double w, double top, double tone) {
        p.part(Pencil.poly(x - w / 2, HORIZON + 10, x + w / 2, HORIZON + 10, x + w / 2, top, x - w / 2, top), tone);
        p.part(Pencil.poly(x - w * 0.62, top, x + w * 0.62, top, x, top - w * 1.5), min(1, tone + 0.25));
        p.part(Pencil.poly(x - 1, top - w * 1.5, x - 1, top - w * 1.5 - 70, x + 60, top - w * 1.5 - 50, x - 1, top - w * 1.5 - 32),
                0.2, Monsters.RED, 0.9);
        for (int i = 0; i < 3; i++) {
            double wy = top + 60 + i * 110;
            p.part(Pencil.blob(x - 12, wy + 34, x - 12, wy, x, wy - 12, x + 12, wy, x + 12, wy + 34), 0.1, Monsters.ORANGE, 1.0);
        }
    }

    static void torch(Pencil p, double x, double y) {
        p.smudge(Pencil.ell(x, y - 30, 70, 70), Monsters.ORANGE, 30);
        p.part(Pencil.limb(12, x, y, x, y + 70), 0.7);
        p.part(Pencil.flame(x, y, 34, 70, 6), 0.03, Monsters.ORANGE, 1.0);
    }

    static void castle(Pencil p) {
        p.part(Pencil.poly(-20, -20, W + 20, -20, W + 20, HORIZON, -20, HORIZON), 0.3);   // night sky
        p.part(Pencil.ell(1120, 130, 62, 62), 0.0);   // moon
        tower(p, 836, 150, 120, 0.7);   // keep behind the wall
        // Curtain wall with merlons and brick lines.
        double wallTop = 300;
        p.part(Pencil.poly(-20, HORIZON + 10, W + 20, HORIZON + 10, W + 20, wallTop, -20, wallTop), 0.5);
        for (int x = -10; x < W; x += 90) p.part(Pencil.poly(x, wallTop, x + 50, wallTop, x + 50, wallTop - 40, x, wallTop - 40), 0.55);
        for (double y = wallTop + 40; y < HORIZON; y += 40) {
            p.line(-20, y, W + 20, y, 1.4, Pencil.alpha(Pencil.LEAD, 80));
            for (double x = ((int) (y / 40) % 2) * 45; x < W; x += 90) p.line(x, y, x, y + 40, 1.4, Pencil.alpha(Pencil.LEAD, 70));
        }
        tower(p, 260, 160, 160, 0.62);
        tower(p, 1440, 160, 180, 0.62);
        for (double bx : new double[]{560, 1110}) {
            p.part(Pencil.poly(bx - 50, wallTop + 10, bx + 50, wallTop + 10, bx + 50, wallTop + 190, bx, wallTop + 150,
                    bx - 50, wallTop + 190), 0.2, Monsters.RED, 0.9);
            p.part(Pencil.ell(bx, wallTop + 80, 22, 22), 0.6);
        }
        for (double tx : new double[]{420, 700, 980, 1260}) torch(p, tx, 400);
        floor(p, 0.22, 6);
        for (double y = HORIZON + 60; y < H; y += 70 + (y - HORIZON) * 0.3) {
            p.line(-20, y, W + 20, y, 1.6, Pencil.alpha(Pencil.LEAD, 70));
        }
    }

    // ================= 5 : 얼음 협곡 =================

    static void iceCanyon(Pencil p) {
        p.part(Pencil.poly(-20, HORIZON, 140, 260, 300, 380, 470, 180, 640, 330, 820, 150, 1000, 320, 1160, 200,
                1340, 340, 1500, 230, W + 20, 380, W + 20, HORIZON), 0.1, Monsters.ICE, 0.25);   // far peaks
        for (double[] s : new double[][]{{470, 180}, {820, 150}, {1160, 200}}) {
            p.part(Pencil.poly(s[0] - 50, s[1] + 70, s[0], s[1], s[0] + 50, s[1] + 70, s[0] + 20, s[1] + 56, s[0], s[1] + 80,
                    s[0] - 20, s[1] + 56), 0.0);
        }
        // Ice cliffs on both sides, faceted.
        Shape left = Pencil.poly(-20, -20, 300, -20, 380, 200, 300, 420, 420, 640, -20, 700);
        Shape right = Pencil.poly(W + 20, -20, W - 280, -20, W - 360, 220, W - 270, 440, W - 380, 620, W + 20, 680);
        p.part(left, 0.3, Monsters.ICE, 0.7);
        p.part(right, 0.3, Monsters.ICE, 0.7);
        p.ink(300, -20, 180, 300, 2);
        p.ink(180, 300, 300, 420, 2);
        p.ink(W - 280, -20, W - 160, 320, 2);
        p.ink(W - 160, 320, W - 270, 440, 2);
        for (int i = 0; i < 6; i++) {
            double x = 400 + i * 160;
            p.part(Pencil.poly(x - 18, -10, x + 18, -10, x + 2, 60 + (i % 3) * 50), 0.05, Monsters.ICE, 0.9);
        }
        floor(p, 0.05, 30);
        // Snow drifts: low humps sitting on the floor.
        for (double[] m : new double[][]{{420, 660, 220, 50}, {1120, 610, 150, 30}, {1520, 790, 240, 70}}) {
            double x = m[0], y = m[1], w = m[2], h = m[3];
            p.part(Pencil.blob(x - w, y, x - w * 0.5, y - h * 0.8, x, y - h, x + w * 0.5, y - h * 0.7, x + w, y,
                    x, y + h * 0.12), 0.04, Monsters.ICE, 0.25);
        }
        for (int i = 0; i < 40; i++) {
            double x = p.r.nextDouble() * W, y = p.r.nextDouble() * H, s = 6 + p.r.nextDouble() * 6;
            p.colorLine(1.6, Monsters.ICE, x - s, y, x + s, y);
            p.colorLine(1.6, Monsters.ICE, x, y - s, x, y + s);
        }
    }

    // ================= 6 : 불지옥 화산 =================

    static void volcano(Pencil p) {
        p.part(Pencil.poly(-20, -20, W + 20, -20, W + 20, HORIZON, -20, HORIZON), 0.42);   // smoky sky
        for (int i = 0; i < 6; i++) {
            p.part(Pencil.ell(760 + i * 40 - (i % 2) * 90, 120 - i * 18, 90 + i * 12, 60 + i * 6), 0.35);
        }
        p.part(Pencil.poly(380, HORIZON + 10, 740, 170, 900, 170, 1290, HORIZON + 10), 0.7);
        p.part(Pencil.ell(820, 172, 82, 20), 0.1, Monsters.ORANGE, 1.0);
        p.colorLine(9, Monsters.ORANGE, 780, 180, 760, 260, 700, 340, 680, 430, 600, 540);
        p.colorLine(7, Monsters.ORANGE, 870, 180, 900, 280, 960, 380, 1010, 470, 1080, 560);
        p.smudge(Pencil.ell(820, 190, 160, 80), Monsters.ORANGE, 40);
        p.part(Pencil.poly(-20, HORIZON + 10, 120, 330, 220, 360, 330, HORIZON + 10), 0.75);
        p.part(Pencil.poly(W - 340, HORIZON + 10, W - 210, 300, W - 100, 350, W + 20, HORIZON + 10), 0.75);
        floor(p, 0.45, 10);
        p.colorLine(6, Monsters.ORANGE, 0, 900, 220, 840, 360, 860, 520, 790, 660, 800);
        p.colorLine(5, Monsters.ORANGE, 980, 920, 1120, 830, 1260, 850, 1400, 760, 1680, 740);
        p.colorLine(4, Monsters.ORANGE, 420, 640, 560, 660, 700, 620);
        pebbles(p, 12, 0.75);
        for (int i = 0; i < 40; i++) {
            double x = p.r.nextDouble() * W, y = 160 + p.r.nextDouble() * (HORIZON - 160);
            p.colorLine(2.2, Monsters.ORANGE, x, y, x + 4, y - 10);
        }
    }

    // ================= 7 : 천공의 성채 =================

    static void cloud(Pencil p, double cx, double cy, double w, double tone) {
        java.awt.geom.Area a = new java.awt.geom.Area();
        int n = 5;
        for (int i = 0; i < n; i++) {
            double x = cx - w / 2 + w * i / (n - 1), rr = w * (0.16 + 0.06 * sin(i * 2.1 + cx));
            a.add(new java.awt.geom.Area(Pencil.ell(x, cy - rr * 0.4, rr, rr * 0.8)));
        }
        a.add(new java.awt.geom.Area(Pencil.ell(cx, cy, w * 0.55, w * 0.12)));
        p.part(a, tone, Monsters.ICE, 0.15);
    }

    static void skyCastle(Pencil p) {
        for (int i = 0; i < 9; i++) {
            double a = toRadians(100 + i * 9);
            p.line(1560, -40, 1560 + cos(a) * 1400, -40 + sin(a) * 1400, 1.6, Pencil.alpha(Monsters.GOLD, 70));
        }
        cloud(p, 300, 200, 360, 0.04);
        cloud(p, 1250, 140, 300, 0.04);
        // The floating citadel: an upside-down rock with towers on top.
        p.part(Pencil.poly(560, 380, 1110, 380, 980, 470, 860, 560, 760, 480), 0.55);
        for (double[] t : new double[][]{{640, 90, 170}, {760, 110, 120}, {900, 100, 200}, {1040, 80, 150}}) {
            double x = t[0], w = t[1], top = 380 - t[2] - 80;
            p.part(Pencil.poly(x - w / 2, 384, x + w / 2, 384, x + w / 2, top, x - w / 2, top), 0.2, Monsters.GOLD, 0.3);
            p.part(Pencil.poly(x - w * 0.6, top, x + w * 0.6, top, x, top - w * 0.9), 0.5, Monsters.ICE, 0.4);
        }
        p.part(Pencil.poly(180, 470, 340, 470, 290, 520, 240, 540), 0.55);
        p.part(Pencil.poly(1350, 430, 1520, 430, 1470, 490, 1400, 500), 0.55);
        // A sea of cloud as the floor.
        java.awt.geom.Area sea = new java.awt.geom.Area(new Rectangle(-20, (int) HORIZON + 10, W + 40, H));
        for (int i = 0; i < 12; i++) sea.add(new java.awt.geom.Area(Pencil.ell(i * 150, HORIZON + 10, 110, 50)));
        p.part(sea, 0.06, Monsters.ICE, 0.2);
        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 8; i++) {
                double x = i * 230 + row * 90, y = HORIZON + 110 + row * 110;
                p.curve(2, x, y, x + 50, y - 30, x + 100, y - 20, x + 150, y);
            }
        }
        column(p, 380, HORIZON + 60, 70, 300, false, 0.15);
        column(p, 1600, HORIZON + 120, 100, 520, false, 0.15);
        for (int i = 0; i < 5; i++) {
            double x = 300 + i * 230, y = 80 + (i % 2) * 70;
            p.curve(2.4, x - 18, y, x - 8, y - 10, x, y);
            p.curve(2.4, x, y, x + 8, y - 10, x + 18, y);
        }
    }

    // ================= 최종장 : 침공당한 마을 =================

    /** The deep night of the other world, the fill inside rifts and the cosmic god's body. */
    static final Color NIGHT = new Color(40, 30, 68);

    /** A shape filled with the other world: deep night, purple hatching, nebula smears and stars, then outlined. */
    static void starfield(Pencil p, Shape s, int n, double size) {
        p.g.setColor(NIGHT);
        p.g.fill(s);
        p.tint(s, Monsters.PURPLE, 1.0);
        Shape old = p.g.getClip();
        p.g.clip(s);
        java.awt.geom.Rectangle2D b = s.getBounds2D();
        for (int i = 0; i < 4; i++) {
            double x = b.getX() + p.r.nextDouble() * b.getWidth(), y = b.getY() + p.r.nextDouble() * b.getHeight();
            p.smudge(Pencil.ell(x, y, b.getWidth() * 0.3, b.getHeight() * 0.22), i % 2 == 0 ? Monsters.PURPLE : Monsters.ICE, 45);
        }
        for (int i = 0; i < n; i++) {
            double x = b.getX() + p.r.nextDouble() * b.getWidth(), y = b.getY() + p.r.nextDouble() * b.getHeight();
            double k = p.r.nextDouble();
            if (k < 0.14) {
                double l = size * (2.6 + k * 8);
                Color c = i % 3 == 0 ? Monsters.YELLOW : i % 3 == 1 ? Monsters.ICE : Pencil.PAPER;
                p.colorLine(1.6, c, x - l, y, x + l, y);
                p.colorLine(1.6, c, x, y - l, x, y + l);
            } else {
                p.smudge(Pencil.ell(x, y, size * (0.5 + k), size * (0.5 + k)), Pencil.PAPER, 150 + (int) (k * 100));
            }
        }
        p.g.setClip(old);
        p.outline(s, 3.0);
    }

    /**
     * A crack torn in the sky along the spine points: widest in the middle, jagged edges, the other world's
     * stars showing through, a purple glow around it and hairline cracks running off its edges.
     */
    static void rift(Pencil p, double maxW, int stars, double... spine) {
        int n = spine.length / 2;
        java.util.List<double[]> pts = new java.util.ArrayList<>();
        for (int i = 0; i < n - 1; i++) {
            for (int k = 0; k < 3; k++) {
                double t = k / 3.0;
                pts.add(new double[]{spine[i * 2] + (spine[i * 2 + 2] - spine[i * 2]) * t,
                        spine[i * 2 + 1] + (spine[i * 2 + 3] - spine[i * 2 + 1]) * t});
            }
        }
        pts.add(new double[]{spine[n * 2 - 2], spine[n * 2 - 1]});
        int m = pts.size();
        double[] upper = new double[m * 2], lower = new double[m * 2];
        for (int i = 0; i < m; i++) {
            double[] a = pts.get(max(0, i - 1)), b = pts.get(min(m - 1, i + 1)), c = pts.get(i);
            double dx = b[0] - a[0], dy = b[1] - a[1], len = hypot(dx, dy);
            double nx = -dy / len, ny = dx / len;
            double w = maxW * 0.5 * pow(sin(PI * i / (m - 1)), 0.7) * (i % 2 == 0 ? 1.0 : 0.55 + p.r.nextDouble() * 0.25);
            upper[i * 2] = c[0] + nx * w;
            upper[i * 2 + 1] = c[1] + ny * w;
            lower[i * 2] = c[0] - nx * w * (0.7 + p.r.nextDouble() * 0.5);
            lower[i * 2 + 1] = c[1] - ny * w * (0.7 + p.r.nextDouble() * 0.5);
        }
        double[] xy = new double[m * 4];
        for (int i = 0; i < m; i++) {
            xy[i * 2] = upper[i * 2];
            xy[i * 2 + 1] = upper[i * 2 + 1];
            xy[(2 * m - 1 - i) * 2] = lower[i * 2];
            xy[(2 * m - 1 - i) * 2 + 1] = lower[i * 2 + 1];
        }
        Shape crack = Pencil.poly(xy);
        p.smudge(Pencil.limb(maxW * 3.2, spine), Monsters.PURPLE, 34);
        p.smudge(Pencil.limb(maxW * 1.8, spine), Monsters.PURPLE, 40);
        starfield(p, crack, stars, 2.2 * p.scale);
        for (int i = 2; i < m - 2; i += 2) {
            boolean up = p.r.nextBoolean();
            double[] e = up ? upper : lower;
            double x = e[i * 2], y = e[i * 2 + 1], c = pts.get(i)[0], cy = pts.get(i)[1];
            double ox = x - c, oy = y - cy, ol = max(1, hypot(ox, oy)), l = maxW * (0.4 + p.r.nextDouble() * 0.7);
            double ux = ox / ol, uy = oy / ol;
            p.colorLine(1.8, Monsters.PURPLE, x, y, x + ux * l * 0.5 + uy * l * 0.25, y + uy * l * 0.5 - ux * l * 0.25,
                    x + ux * l, y + uy * l);
        }
    }

    /** A falling star: a rock with a glowing tail streaming back toward (fromX, fromY). */
    static void meteor(Pencil p, double x, double y, double fromX, double fromY, double size) {
        double dx = x - fromX, dy = y - fromY, len = hypot(dx, dy), ux = dx / len, uy = dy / len;
        p.smudge(Pencil.limb(size * 2.6, fromX + dx * 0.3, fromY + dy * 0.3, x, y), Monsters.PURPLE, 55);
        p.colorLine(size * 0.55, Monsters.PURPLE, fromX + dx * 0.2, fromY + dy * 0.2, x - ux * size, y - uy * size);
        p.colorLine(size * 0.22, Monsters.YELLOW, fromX + dx * 0.5, fromY + dy * 0.5, x - ux * size, y - uy * size);
        p.smudge(Pencil.ell(x, y, size * 2, size * 2), Monsters.PURPLE, 60);
        p.part(Pencil.rock(p.r, x, y, size, size * 1.15, 7), 0.6, Monsters.PURPLE, 0.8);
        p.colorLine(2.4, Monsters.YELLOW, x - size * 0.3, y - size * 0.2, x + size * 0.25, y + size * 0.3);
    }

    /** Where a star came down: a heap of glowing rock with cracks running out over the ground and a sparkle on top. */
    static void fallenStar(Pencil p, double x, double y, double r) {
        p.smudge(Pencil.ell(x, y, r * 3, r * 1.5), Monsters.PURPLE, 60);
        for (int k = 0; k < 3; k++) {
            double a = PI * (0.15 + 0.35 * k) + p.r.nextDouble() * 0.3, l = r * (2.2 + p.r.nextDouble());
            double mx = x + cos(a + 0.2) * l * 0.6, my = y + sin(a + 0.2) * l * 0.3 + r * 0.3;
            p.colorLine(2, Monsters.PURPLE, x + cos(a) * r * 0.6, y + r * 0.3, mx, my, x + cos(a) * l, y + sin(a) * l * 0.5 + r * 0.3);
        }
        p.part(Pencil.rock(p.r, x - r * 0.9, y + r * 0.1, r * 0.6, r * 0.5, 6), 0.55);
        p.part(Pencil.rock(p.r, x + r * 0.9, y + r * 0.2, r * 0.55, r * 0.45, 6), 0.55);
        p.part(Pencil.rock(p.r, x, y - r * 0.3, r, r * 0.85, 7), 0.6, Monsters.PURPLE, 0.9);
        p.colorLine(2, Monsters.YELLOW, x - r * 0.4, y - r * 0.5, x + r * 0.15, y - r * 0.1);
        double sx = x + r * 0.6, sy = y - r * 1.4, s = r * 0.6;
        p.colorLine(2, Monsters.YELLOW, sx - s, sy, sx + s, sy);
        p.colorLine(2, Monsters.YELLOW, sx, sy - s, sx, sy + s);
    }

    /** A house with a bite torn out of one top corner: roof and wall broken off, beams sticking out, rubble below. */
    static void brokenHouse(Pencil p, double x, double baseY, double w, double h, Color roof, boolean bittenRight) {
        double top = baseY - h;
        double bx = bittenRight ? x + w * 1.15 : x - w * 0.15, dir = bittenRight ? -1 : 1;
        java.awt.geom.Area bite = new java.awt.geom.Area(Pencil.poly(bx, top - h * 0.8, bx + dir * w * 0.62, top - h * 0.8,
                bx + dir * w * 0.5, top - h * 0.3, bx + dir * w * 0.58, top + h * 0.05, bx + dir * w * 0.36, top + h * 0.2,
                bx + dir * w * 0.42, top + h * 0.42, bx + dir * w * 0.2, top + h * 0.5, bx, top + h * 0.6));
        java.awt.geom.Area wall = new java.awt.geom.Area(Pencil.poly(x, baseY, x + w, baseY, x + w, top, x, top));
        wall.subtract(bite);
        java.awt.geom.Area attic = new java.awt.geom.Area(Pencil.poly(x - w * 0.1, top + 4, x + w / 2, top - h * 0.6, x + w * 1.1, top + 4));
        attic.subtract(bite);
        // Beams poking out of the break.
        for (int i = 0; i < 3; i++) {
            double by = top - h * 0.1 + i * h * 0.16, ex = bx + dir * w * (0.38 + i * 0.04);
            p.part(Pencil.limb(9, ex + dir * w * 0.12, by, ex - dir * w * 0.06, by - 14 + i * 6), 0.6, Monsters.BROWN, 0.7);
        }
        p.part(wall, 0.16, Monsters.BROWN, 0.35);
        p.part(attic, 0.4, roof, 0.8);
        double wx = bittenRight ? x + w * 0.14 : x + w * 0.62;
        p.part(Pencil.poly(wx, top + h * 0.28, wx + w * 0.22, top + h * 0.28, wx + w * 0.22, top + h * 0.52, wx, top + h * 0.52), 0.85);
        p.ink(wx + w * 0.11, top + h * 0.28, wx + w * 0.11, top + h * 0.52, 1.8);
        double dx = x + w * 0.4;
        p.part(Pencil.poly(dx, baseY, dx, baseY - h * 0.4, dx + w * 0.2, baseY - h * 0.4, dx + w * 0.2, baseY), 0.65, Monsters.BROWN, 0.6);
        p.ink(x + w * 0.5, top + h * 0.5, x + w * 0.56, top + h * 0.66, 2);
        p.ink(x + w * 0.56, top + h * 0.66, x + w * 0.5, top + h * 0.8, 2);
        for (int i = 0; i < 5; i++) {
            double rx = x + (bittenRight ? w * 0.8 : -w * 0.15) + p.r.nextDouble() * w * 0.4, s = w * (0.05 + p.r.nextDouble() * 0.05);
            p.part(Pencil.rock(p.r, rx, baseY + 6, s * 1.4, s, 7), 0.45);
        }
    }

    static void invadedTown(Pencil p) {
        // A sky gone dark, stars where the daylight was.
        p.part(Pencil.poly(-20, -20, W + 20, -20, W + 20, HORIZON + 10, -20, HORIZON + 10), 0.5, Monsters.PURPLE, 0.55);
        for (int i = 0; i < 70; i++) {
            double x = p.r.nextDouble() * W, y = p.r.nextDouble() * (HORIZON - 120);
            if (i % 7 == 0) {
                p.colorLine(1.8, Monsters.YELLOW, x - 9, y, x + 9, y);
                p.colorLine(1.8, Monsters.YELLOW, x, y - 9, x, y + 9);
            } else {
                p.smudge(Pencil.ell(x, y, 2.6, 2.6), Pencil.PAPER, 220);
            }
        }
        rift(p, 120, 90, -40, 170, 180, 90, 420, 180, 640, 60, 860, 170, 1060, 100, 1260, 200, 1460, 110, W + 40, 190);
        meteor(p, 330, 350, 470, 180, 18);
        meteor(p, 1010, 320, 900, 170, 15);
        meteor(p, 1300, 400, 1380, 170, 20);
        meteor(p, 680, 420, 640, 130, 12);
        // The far rooftops, two of them burning, then the broken houses at both sides of the plaza.
        for (double fx : new double[]{540, 1100}) p.smudge(Pencil.ell(fx, HORIZON - 110, 130, 80), Monsters.ORANGE, 45);
        for (int i = 0; i < 8; i++) {
            double x = 330 + i * 140, hgt = 70 + (i % 3) * 22;
            p.part(Pencil.poly(x, HORIZON + 4, x + 110, HORIZON + 4, x + 110, HORIZON - hgt, x + 55, HORIZON - hgt - 50 + (i % 2) * 26,
                    x + (i % 2 == 0 ? 20 : 70), HORIZON - hgt - 14, x, HORIZON - hgt), 0.62);
        }
        for (double[] f : new double[][]{{525, HORIZON - 104}, {560, HORIZON - 96}, {1090, HORIZON - 120}, {1124, HORIZON - 104}}) {
            p.part(Pencil.flame(f[0], f[1], 34, 60 + (f[0] % 3) * 10, 6), 0.03, Monsters.ORANGE, 1.0);
            p.part(Pencil.flame(f[0], f[1] - 2, 16, 30, 3), 0.0, Monsters.YELLOW, 1.0);
        }
        p.smudge(Pencil.ell(836, HORIZON - 40, 420, 60), Monsters.PURPLE, 30);
        brokenHouse(p, 30, HORIZON + 40, 250, 210, Monsters.RED, true);
        brokenHouse(p, 1400, HORIZON + 34, 230, 190, Monsters.ORANGE, false);
        // The plaza: cobbles, a toppled fountain at the back, glowing cracks and fallen stars.
        floor(p, 0.14, 8);
        for (int ring = 1; ring <= 3; ring++) p.outline(Pencil.ell(W / 2.0, 800, 260 * ring, 60 * ring), 1.4);
        p.part(Pencil.ell(780, HORIZON + 30, 120, 26), 0.35);
        p.part(Pencil.ell(780, HORIZON + 26, 96, 16), 0.1, Monsters.ICE, 0.6);
        p.part(Pencil.poly(744, HORIZON + 20, 756, HORIZON - 70, 772, HORIZON - 40, 790, HORIZON - 92, 800, HORIZON + 20), 0.3);
        p.part(Pencil.limb(26, 840, HORIZON + 46, 920, HORIZON + 30), 0.3);
        p.colorLine(4, Monsters.PURPLE, 560, 700, 640, 740, 610, 800, 700, 860, 680, 940);
        p.colorLine(3.4, Monsters.PURPLE, 1000, 640, 1060, 700, 1040, 760, 1120, 810);
        p.colorLine(3, Monsters.PURPLE, 200, 820, 290, 800, 330, 850, 420, 830);
        for (double[] c : new double[][]{{640, 740, 28}, {1060, 700, 22}, {180, 690, 20}, {1540, 860, 26}}) fallenStar(p, c[0], c[1], c[2]);
        pebbles(p, 8, 0.45);
        // A lamp post bent over, and the town sign knocked to the ground.
        p.part(Pencil.limb(12, 470, HORIZON + 120, 476, HORIZON - 40, 530, HORIZON - 90), 0.7);
        p.part(Pencil.poly(518, HORIZON - 96, 548, HORIZON - 84, 538, HORIZON - 60, 512, HORIZON - 70), 0.08, Monsters.YELLOW, 0.4);
        p.part(Pencil.poly(820, 856, 960, 846, 966, 888, 826, 896), 0.1, Monsters.BROWN, 0.5);
        p.text("우리 마을", 842, 886, 28, new Color(52, 86, 170), -4);
    }
}
