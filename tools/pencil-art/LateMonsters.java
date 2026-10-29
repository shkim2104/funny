import java.awt.*;

import static java.lang.Math.*;

/** Monsters of dungeons 3~7, on the same 700x700 canvas and conventions as {@link Monsters}. */
final class LateMonsters {
    private static final double G = Monsters.G;
    private static final Color RED = Monsters.RED, ORANGE = Monsters.ORANGE, ICE = Monsters.ICE, GOLD = Monsters.GOLD,
            PURPLE = Monsters.PURPLE, GREEN = Monsters.GREEN, YELLOW = Monsters.YELLOW, BROWN = Monsters.BROWN;

    private LateMonsters() {}

    private static Shape blade(double hx, double hy, double tx, double ty, double w) {
        double dx = tx - hx, dy = ty - hy, len = hypot(dx, dy), nx = -dy / len * w / 2, ny = dx / len * w / 2;
        return Pencil.poly(hx + nx, hy + ny, tx + nx * 0.6 - dx * 0.06, ty + ny * 0.6 - dy * 0.06, tx, ty,
                tx - nx * 0.6 - dx * 0.06, ty - ny * 0.6 - dy * 0.06, hx - nx, hy - ny);
    }

    /** A sword held at (hx, hy) pointing to (tx, ty): blade, crossguard and grip. */
    private static void sword(Pencil p, double hx, double hy, double tx, double ty, double w, Color edge) {
        p.part(blade(hx, hy, tx, ty, w), 0.12);
        if (edge != null) p.colorLine(2.5, edge, hx, hy, tx, ty);
        double dx = tx - hx, dy = ty - hy, len = hypot(dx, dy), nx = -dy / len, ny = dx / len;
        p.part(Pencil.limb(w * 0.55, hx + nx * w * 1.3, hy + ny * w * 1.3, hx - nx * w * 1.3, hy - ny * w * 1.3), 0.75);
        p.part(Pencil.limb(w * 0.5, hx, hy, hx - dx / len * w * 1.6, hy - dy / len * w * 1.6), 0.85);
    }

    /** A feathered or leathery wing from the shoulder (sx, sy) spreading to one side (side = -1 left, 1 right). */
    private static Shape wing(double sx, double sy, int side, double s) {
        return Pencil.blob(sx, sy, sx + side * 80 * s, sy - 170 * s, sx + side * 230 * s, sy - 260 * s,
                sx + side * 300 * s, sy - 200 * s, sx + side * 270 * s, sy - 60 * s, sx + side * 200 * s, sy + 60 * s,
                sx + side * 90 * s, sy + 90 * s);
    }

    private static void feathers(Pencil p, double sx, double sy, int side, double s) {
        for (int i = 0; i < 5; i++) {
            double a = toRadians(-70 + i * 30);
            p.ink(sx + side * 40 * s, sy, sx + side * (40 + cos(a) * 220) * s, sy + sin(a) * 200 * s, 2.2);
        }
    }

    // ================= 3 : 잊혀진 폐허 =================

    static void skeletonSoldier(Pencil p) {
        p.part(Pencil.limb(18, 322, 452, 312, 560, 318, 668), 0.08);
        p.part(Pencil.limb(18, 378, 452, 392, 560, 386, 668), 0.08);
        p.part(Pencil.ell(312, 560, 15, 15), 0.1);
        p.part(Pencil.ell(392, 560, 15, 15), 0.1);
        p.part(Pencil.ell(306, 672, 32, 11), 0.2);
        p.part(Pencil.ell(394, 672, 32, 11), 0.2);
        p.part(Pencil.blob(298, 440, 402, 440, 392, 478, 350, 486, 308, 478), 0.15);
        p.ink(350, 296, 350, 446, 6);
        for (int i = 0; i < 4; i++) {
            double y = 312 + i * 30, w = 62 - i * 7;
            p.curve(4.5, 350 - w, y + 14, 350 - w * 0.6, y - 4, 350, y + 2, 350 + w * 0.6, y - 4, 350 + w, y + 14);
        }
        p.part(Pencil.limb(15, 412, 306, 452, 382, 474, 336), 0.08);
        sword(p, 474, 336, 540, 120, 22, null);
        p.part(Pencil.limb(15, 288, 306, 252, 380, 244, 436), 0.08);
        p.part(Pencil.ell(350, 228, 64, 68), 0.06);
        p.part(Pencil.ell(326, 226, 17, 19), 0.95);
        p.part(Pencil.ell(374, 226, 17, 19), 0.95);
        p.dot(326, 228, 5, RED);
        p.dot(374, 228, 5, RED);
        p.part(Pencil.poly(344, 250, 356, 250, 350, 264), 0.9);
        p.part(Pencil.blob(318, 272, 382, 272, 374, 300, 326, 300), 0.08);
        for (int i = 0; i < 5; i++) p.ink(330 + i * 10, 276, 330 + i * 10, 296, 2);
        p.part(Pencil.ell(236, 446, 82, 94), 0.5, BROWN, 0.7);
        p.part(Pencil.ell(236, 446, 18, 18), 0.2, GOLD, 0.8);
        p.ink(236, 360, 236, 532, 3.5);
        p.ink(160, 446, 312, 446, 3.5);
    }

    static void wraith(Pencil p) {
        Shape arm = Pencil.limb(30, 262, 330, 180, 400, 120, 372);
        p.part(arm, 0.15, ICE, 0.5);
        p.part(Pencil.mirror(arm, 350), 0.15, ICE, 0.5);
        p.part(Pencil.blob(350, 118, 452, 178, 480, 330, 468, 480, 510, 640, 452, 592, 420, 664, 380, 604,
                340, 676, 300, 604, 258, 656, 236, 586, 196, 626, 228, 470, 220, 330, 250, 178), 0.2, ICE, 0.55);
        p.part(Pencil.ell(350, 250, 70, 82), 0.95);
        p.smudge(Pencil.ell(350, 246, 46, 30), ICE, 60);
        p.dot(326, 244, 10, ICE);
        p.dot(374, 244, 10, ICE);
        for (double hx : new double[]{120, 580}) {
            for (int c = -1; c <= 1; c++) p.ink(hx, 372, hx + (hx < 350 ? -24 : 24), 372 + c * 16, 2.6);
        }
    }

    static void zombie(Pencil p) {
        p.part(Pencil.limb(42, 362, 452, 342, 560, 330, 666), 0.5);
        p.part(Pencil.limb(42, 418, 452, 442, 560, 452, 666), 0.55);
        p.part(Pencil.ell(318, 670, 40, 14), 0.7);
        p.part(Pencil.ell(462, 670, 40, 14), 0.7);
        p.part(Pencil.limb(36, 362, 312, 252, 352, 170, 372), 0.3, GREEN, 0.7);
        p.part(Pencil.blob(296, 282, 424, 270, 452, 440, 410, 470, 380, 446, 350, 478, 330, 452), 0.45, BROWN, 0.4);
        p.part(Pencil.limb(36, 318, 298, 220, 318, 130, 328), 0.3, GREEN, 0.7);
        p.part(Pencil.ell(126, 330, 22, 20), 0.3, GREEN, 0.7);
        p.part(Pencil.ell(166, 374, 22, 20), 0.3, GREEN, 0.7);
        p.part(Pencil.ell(298, 222, 62, 68), 0.3, GREEN, 0.8);
        p.dot(276, 212, 11, YELLOW);
        p.dot(318, 220, 7, YELLOW);
        p.part(Pencil.blob(272, 254, 312, 250, 306, 276, 280, 278), 0.9);
        p.ink(270, 180, 330, 192, 2.5);
        for (int i = 0; i < 4; i++) p.ink(278 + i * 14, 176 + i * 3, 280 + i * 14, 194 + i * 3, 2);
        for (int i = 0; i < 5; i++) p.ink(262 + i * 18, 160, 256 + i * 18, 140, 2.5);
    }

    static void ruinBandit(Pencil p) {
        p.part(Pencil.blob(262, 240, 438, 240, 480, 420, 470, 600, 350, 620, 230, 600, 220, 420), 0.85);
        double[] f = Monsters.figure(p, 350, 470, 1.0, 0.7, 0.6, 0.6, false, 252, 470, 462, 436);
        p.part(blade(252, 470, 200, 560, 16), 0.1);
        p.part(blade(462, 436, 540, 380, 16), 0.1);
        double hx = f[0], hy = f[1] + 4, hr = f[2] * 1.2;
        p.part(Pencil.blob(hx - hr * 1.15, hy + hr * 0.5, hx - hr * 1.05, hy - hr * 0.8, hx, hy - hr * 1.5,
                hx + hr * 1.05, hy - hr * 0.8, hx + hr * 1.15, hy + hr * 0.5, hx, hy + hr * 0.9), 0.78);
        p.part(Pencil.ell(hx, hy + hr * 0.05, hr * 0.7, hr * 0.68), 0.97);
        p.dot(hx - hr * 0.28, hy, 7, YELLOW);
        p.dot(hx + hr * 0.28, hy, 7, YELLOW);
        p.part(Pencil.limb(30, hx - hr * 1.0, hy + hr * 0.85, hx + hr * 1.0, hy + hr * 0.85), 0.35, RED, 0.8);
        p.part(Pencil.limb(22, hx + hr * 0.7, hy + hr * 0.9, hx + hr * 1.2, hy + hr * 1.8), 0.35, RED, 0.8);
    }

    static void lich(Pencil p) {
        p.smudge(Pencil.ell(548, 136, 90, 90), PURPLE, 40);
        p.part(Pencil.limb(14, 526, 662, 548, 170), 0.6, BROWN, 0.6);
        p.part(Pencil.blob(250, 160, 450, 160, 470, 330, 230, 330), 0.85);   // hood behind
        double[] f = Monsters.figure(p, 350, 540, 1.2, 0.8, 0.78, 0.7, true, 236, 520, 530, 360, PURPLE);
        p.part(Pencil.limb(26, 300, f[3] + 6, 400, f[3] + 6), 0.2, GOLD, 0.8);
        double hx = f[0], hy = f[1] + 8, hr = f[2] * 0.95;
        p.part(Pencil.ell(hx, hy, hr, hr * 1.05), 0.06);
        p.part(Pencil.ell(hx - hr * 0.38, hy - hr * 0.05, hr * 0.26, hr * 0.3), 0.97);
        p.part(Pencil.ell(hx + hr * 0.38, hy - hr * 0.05, hr * 0.26, hr * 0.3), 0.97);
        p.dot(hx - hr * 0.38, hy - hr * 0.03, 6, PURPLE);
        p.dot(hx + hr * 0.38, hy - hr * 0.03, 6, PURPLE);
        for (int i = 0; i < 5; i++) p.ink(hx - hr * 0.4 + i * hr * 0.2, hy + hr * 0.6, hx - hr * 0.4 + i * hr * 0.2, hy + hr * 0.85, 2);
        p.part(Pencil.poly(hx - hr * 0.9, hy - hr * 0.75, hx - hr * 0.8, hy - hr * 1.45, hx - hr * 0.4, hy - hr * 1.0,
                hx, hy - hr * 1.6, hx + hr * 0.4, hy - hr * 1.0, hx + hr * 0.8, hy - hr * 1.45, hx + hr * 0.9, hy - hr * 0.75), 0.15, GOLD, 0.9);
        p.part(Pencil.ell(548, 136, 40, 40), 0.1, PURPLE, 1.0);
    }

    // ================= 4 : 마왕성 =================

    static void darkKnight(Pencil p) {
        double[] f = Monsters.figure(p, 350, 520, 1.25, 0.85, 0.85, 0.85, false, 236, 470, 464, 470);
        double px = f[0], py = f[1] + 6, pr = f[2] * 1.05;
        p.part(Pencil.limb(28, px, py - pr * 0.9, px + pr * 0.9, py - pr * 1.6, px + pr * 1.7, py - pr * 1.2,
                px + pr * 1.9, py - pr * 0.2), 0.3, RED, 0.8);   // plume, sweeping back
        sword(p, 464, 470, 560, 190, 30, null);
        p.part(Pencil.ell(244, f[3] + 16, 58, 40), 0.9);
        p.part(Pencil.ell(456, f[3] + 16, 58, 40), 0.9);
        p.ink(350, f[3] + 20, 350, f[4] - 10, 3);
        p.part(Pencil.limb(22, 270, f[4] - 4, 430, f[4] - 4), 0.95);
        double hx = f[0], hy = f[1] + 6, hr = f[2] * 1.05;
        p.part(Pencil.blob(hx - hr, hy + hr * 0.9, hx - hr * 1.05, hy - hr * 0.4, hx, hy - hr * 1.1,
                hx + hr * 1.05, hy - hr * 0.4, hx + hr, hy + hr * 0.9), 0.88);
        p.part(Pencil.limb(10, hx - hr * 0.7, hy, hx + hr * 0.7, hy), 1.0);
        p.dot(hx - hr * 0.3, hy, 6, RED);
        p.dot(hx + hr * 0.3, hy, 6, RED);
        p.part(Pencil.poly(176, 380, 304, 380, 304, 520, 240, 610, 176, 520), 0.7);
        p.part(Pencil.poly(220, 420, 260, 420, 260, 520, 240, 550, 220, 520), 0.1, RED, 0.9);
    }

    static void fallenMage(Pencil p) {
        p.part(Pencil.limb(12, 470, 650, 494, 200), 0.6, BROWN, 0.6);
        p.smudge(Pencil.ell(496, 150, 70, 80), PURPLE, 40);
        p.part(Pencil.flame(496, 206, 64, 120, 12), 0.1, PURPLE, 1.0);
        double[] f = Monsters.figure(p, 340, 500, 1.0, 0.65, 0.65, 0.6, true, 240, 500, 474, 420, PURPLE);
        double hx = f[0], hy = f[1] + 8, hr = f[2] * 1.05;
        p.part(Pencil.blob(hx - hr * 0.8, hy + hr * 0.4, hx + hr * 0.8, hy + hr * 0.4, hx + hr * 0.4, hy + hr * 1.9,
                hx, hy + hr * 2.3, hx - hr * 0.4, hy + hr * 1.9), 0.04);   // beard
        p.part(Pencil.ell(hx, hy, hr * 0.85, hr * 0.8), 0.92);
        p.dot(hx - hr * 0.3, hy, 7, PURPLE);
        p.dot(hx + hr * 0.3, hy, 7, PURPLE);
        p.part(Pencil.ell(hx, hy - hr * 0.7, hr * 1.9, hr * 0.38), 0.8);
        p.part(Pencil.blob(hx - hr * 1.05, hy - hr * 0.8, hx + hr * 1.05, hy - hr * 0.8, hx + hr * 0.5, hy - hr * 1.8,
                hx + hr * 1.3, hy - hr * 3.0, hx - hr * 0.1, hy - hr * 2.0), 0.75, PURPLE, 0.5);
    }

    static void hellhound(Pencil p) {
        double[] b = Monsters.beast(p, 330, 0.8, RED, 0.35, false);
        p.part(Pencil.flame(636, 446, 60, 110, 30), 0.05, ORANGE, 1.0);   // flaming tail tip
        for (int i = 0; i < 6; i++) {
            double x = 260 + i * 46, y = b[1] + 8 + abs(i - 2.5) * 6;
            p.part(Pencil.poly(x - 16, y + 14, x + 6, y - 46, x + 18, y + 14), 0.9);
        }
        for (int i = 0; i < 4; i++) p.part(Pencil.flame(150 + i * 34, b[1] + 50, 46, 100, 18), 0.05, ORANGE, 1.0);
        Monsters.dogHead(p, 200, b[1] + 18, 1.1, 0.8, ORANGE, RED);
    }

    static void demonArcher(Pencil p) {
        Shape w = Pencil.poly(330, 300, 250, 190, 150, 170, 190, 230, 200, 300, 240, 290, 260, 350);
        p.part(Pencil.mirror(w, 350), 0.65, RED, 0.3);
        p.part(Pencil.limb(14, 380, 450, 470, 520, 520, 470, 560, 520), 0.7, RED, 0.4);   // tail
        p.part(Pencil.poly(548, 500, 590, 520, 560, 546), 0.8);
        double[] f = Monsters.figure(p, 360, 470, 1.0, 0.6, 0.55, 0.5, false, 218, 360, 330, 352, RED);
        p.outlineOpen(Pencil.spline(222, 226, 176, 300, 172, 360, 178, 420, 222, 494), 7);
        p.ink(222, 228, 330, 352, 1.8);
        p.ink(330, 352, 222, 492, 1.8);
        p.ink(336, 354, 128, 362, 3);
        p.part(Pencil.poly(128, 352, 98, 362, 128, 372), 0.9);
        double hx = f[0], hy = f[1] + 6, hr = f[2] * 1.05;
        Shape horn = Pencil.blob(hx - hr * 0.6, hy - hr * 0.6, hx - hr * 0.9, hy - hr * 1.4, hx - hr * 0.7, hy - hr * 2.0,
                hx - hr * 0.5, hy - hr * 1.3, hx - hr * 0.25, hy - hr * 0.8);
        p.part(horn, 0.85);
        p.part(Pencil.mirror(horn, hx), 0.85);
        p.part(Pencil.ell(hx, hy, hr, hr * 0.95), 0.35, RED, 0.6);
        p.part(Pencil.poly(hx - hr * 0.9, hy - hr * 0.1, hx - hr * 1.5, hy - hr * 0.4, hx - hr * 0.9, hy + hr * 0.3), 0.35, RED, 0.6);
        p.dot(hx - hr * 0.35, hy - hr * 0.05, 8, YELLOW);
        p.dot(hx + hr * 0.35, hy - hr * 0.05, 8, YELLOW);
        p.curve(3, hx - hr * 0.4, hy + hr * 0.5, hx, hy + hr * 0.4, hx + hr * 0.4, hy + hr * 0.5);
    }

    static void demonLord(Pencil p) {
        p.part(Pencil.blob(210, 230, 490, 230, 640, 660, 350, 690, 60, 660), 0.85, RED, 0.6);   // cape
        double[] f = Monsters.figure(p, 350, 610, 1.4, 0.8, 0.75, 0.7, false, 216, 520, 524, 420);
        sword(p, 524, 420, 610, 40, 40, RED);
        for (double sx : new double[]{230, 470}) {
            p.part(Pencil.ell(sx, f[3] + 18, 62, 44), 0.9);
            for (int i = -1; i <= 1; i++) p.part(Pencil.poly(sx + i * 34 - 12, f[3], sx + i * 34, f[3] - 50, sx + i * 34 + 12, f[3]), 0.85);
        }
        p.part(Pencil.blob(270, f[3] + 30, 430, f[3] + 30, 420, f[4] - 30, 280, f[4] - 30), 0.6, RED, 0.5);
        double hx = f[0], hy = f[1] + 4, hr = f[2] * 1.0;
        Shape horn = Pencil.blob(hx - hr * 0.7, hy - hr * 0.6, hx - hr * 1.7, hy - hr * 1.0, hx - hr * 2.1, hy - hr * 2.1,
                hx - hr * 1.5, hy - hr * 1.2, hx - hr * 0.6, hy - hr * 0.95);
        p.part(horn, 0.9);
        p.part(Pencil.mirror(horn, hx), 0.9);
        p.part(Pencil.ell(hx, hy, hr, hr), 0.7, RED, 0.5);
        p.part(Pencil.poly(hx - hr * 0.7, hy - hr * 0.8, hx - hr * 0.55, hy - hr * 1.5, hx - hr * 0.25, hy - hr * 1.0,
                hx, hy - hr * 1.7, hx + hr * 0.25, hy - hr * 1.0, hx + hr * 0.55, hy - hr * 1.5, hx + hr * 0.7, hy - hr * 0.8), 0.15, GOLD, 0.9);
        p.smudge(Pencil.ell(hx, hy, hr * 0.8, hr * 0.35), RED, 60);
        p.dot(hx - hr * 0.35, hy, 10, RED);
        p.dot(hx + hr * 0.35, hy, 10, RED);
        p.part(Pencil.poly(hx - hr * 0.35, hy + hr * 0.45, hx - hr * 0.27, hy + hr * 0.7, hx - hr * 0.19, hy + hr * 0.45), 0);
        p.part(Pencil.poly(hx + hr * 0.35, hy + hr * 0.45, hx + hr * 0.27, hy + hr * 0.7, hx + hr * 0.19, hy + hr * 0.45), 0);
    }

    // ================= 5 : 얼음 협곡 =================

    static void frostWolf(Pencil p) {
        double[] b = Monsters.beast(p, 320, 0.22, ICE, 0.5, true);
        p.part(Pencil.blob(150, b[1] - 10, 230, b[1] - 70, 300, b[1] - 20, 320, b[1] + 80, 260, b[1] + 160,
                170, b[1] + 120), 0.12, ICE, 0.6);   // frosty mane
        for (int i = 0; i < 5; i++) {
            double x = 300 + i * 44;
            p.part(Pencil.poly(x - 12, b[1] + 22, x + 4, b[1] - 30 - i % 2 * 20, x + 14, b[1] + 22), 0.05, ICE, 1.0);
        }
        Monsters.dogHead(p, 200, b[1] + 16, 1.05, 0.22, ICE, ICE);
    }

    static void iceSpirit(Pencil p) {
        p.smudge(Pencil.ell(350, 380, 220, 240), ICE, 30);
        double[][] shards = {{180, 300, 40, 90}, {520, 260, 46, 100}, {210, 500, 34, 70}, {500, 520, 36, 76}};
        for (double[] s : shards) {
            p.part(Pencil.poly(s[0], s[1] - s[3], s[0] + s[2], s[1], s[0], s[1] + s[3], s[0] - s[2], s[1]), 0.08, ICE, 0.9);
        }
        p.part(Pencil.poly(350, 120, 460, 360, 350, 640, 240, 360), 0.1, ICE, 1.0);
        p.ink(350, 120, 350, 640, 2.2);
        p.ink(240, 360, 460, 360, 2.2);
        p.ink(300, 240, 400, 480, 1.6);
        p.dot(322, 330, 9, Pencil.LEAD);
        p.dot(378, 330, 9, Pencil.LEAD);
        p.curve(2.5, 330, 392, 350, 402, 370, 392);
        for (double[] s : new double[][]{{150, 180}, {560, 420}, {270, 600}, {460, 150}}) {
            p.colorLine(2.5, ICE, s[0] - 14, s[1], s[0] + 14, s[1]);
            p.colorLine(2.5, ICE, s[0], s[1] - 14, s[0], s[1] + 14);
        }
    }

    static void snowGolem(Pencil p) {
        p.part(Pencil.rock(p.r, 170, 410, 74, 64, 8), 0.6);
        p.part(Pencil.rock(p.r, 530, 410, 74, 64, 8), 0.6);
        p.part(Pencil.limb(26, 200, 380, 260, 390), 0.6, BROWN, 0.7);
        p.part(Pencil.limb(26, 500, 380, 440, 390), 0.6, BROWN, 0.7);
        p.part(Pencil.ell(350, 570, 172, 110), 0.05, ICE, 0.35);
        p.part(Pencil.ell(350, 410, 130, 100), 0.05, ICE, 0.35);
        p.part(Pencil.ell(350, 262, 92, 82), 0.05, ICE, 0.35);
        for (int i = 0; i < 3; i++) p.dot(350, 370 + i * 40, 10, Pencil.LEAD);
        p.dot(318, 246, 12, Pencil.LEAD);
        p.dot(382, 246, 12, Pencil.LEAD);
        p.part(Pencil.poly(352, 268, 262, 290, 352, 290), 0.1, ORANGE, 1.0);
        for (int i = 0; i < 5; i++) p.dot(312 + i * 19, 314 + abs(i - 2) * -5, 5, Pencil.LEAD);
        for (int i = 0; i < 4; i++) {
            double x = 296 + i * 36;
            p.part(Pencil.poly(x - 12, 192 + abs(i - 1.5) * 6, x, 140 - i % 2 * 24, x + 12, 192 + abs(i - 1.5) * 6), 0.05, ICE, 1.0);
        }
        p.ink(250, 520, 300, 560, 2.5);
        p.ink(300, 560, 290, 610, 2.5);
    }

    static void frostQueen(Pencil p) {
        p.part(Pencil.blob(270, 140, 430, 140, 450, 300, 440, 420, 260, 420, 250, 300), 0.5, ICE, 0.3);   // hair
        p.part(Pencil.limb(12, 506, 660, 530, 170), 0.15, ICE, 0.7);
        double[] f = Monsters.figure(p, 350, 560, 0.95, 0.2, 0.2, 0.12, true, 240, 470, 514, 400, ICE);
        double hx = f[0], hy = f[1] + 6, hr = f[2] * 0.95;
        p.part(Pencil.ell(hx, hy, hr * 0.85, hr), 0.04);
        p.dot(hx - hr * 0.32, hy - hr * 0.05, 6, ICE);
        p.dot(hx + hr * 0.32, hy - hr * 0.05, 6, ICE);
        p.curve(2.5, hx - hr * 0.2, hy + hr * 0.5, hx, hy + hr * 0.55, hx + hr * 0.2, hy + hr * 0.5);
        for (int i = 0; i < 5; i++) {
            double x = hx - hr * 0.8 + i * hr * 0.4, top = hy - hr * (1.5 + (i == 2 ? 0.8 : i % 2 == 0 ? 0.3 : 0.5));
            p.part(Pencil.poly(x - hr * 0.16, hy - hr * 0.8, x, top, x + hr * 0.16, hy - hr * 0.8), 0.05, ICE, 1.0);
        }
        double sx = 530, sy = 150;
        for (int i = 0; i < 3; i++) {
            double a = PI / 3 * i;
            p.colorLine(4, ICE, sx - cos(a) * 46, sy - sin(a) * 46, sx + cos(a) * 46, sy + sin(a) * 46);
        }
        p.dot(sx, sy, 10, ICE);
    }

    // ================= 6 : 불지옥 화산 =================

    static void fireSalamander(Pencil p) {
        p.part(Pencil.limb(46, 500, 560, 600, 520, 664, 580, 624, 648), 0.5, ORANGE, 0.5);
        for (double[] l : new double[][]{{230, 600, 196, 640, 176, 674}, {440, 600, 470, 640, 492, 674}}) p.part(Pencil.limb(26, l), 0.6);
        p.part(Pencil.blob(140, 560, 300, 500, 500, 512, 566, 580, 450, 632, 250, 634), 0.5, ORANGE, 0.55);
        for (double[] l : new double[][]{{260, 610, 236, 646, 220, 676}, {470, 610, 500, 644, 520, 676}}) p.part(Pencil.limb(28, l), 0.55);
        for (int i = 0; i < 6; i++) p.part(Pencil.flame(230 + i * 48, 512 - abs(i - 2.5) * 4, 40, 70 + i % 2 * 30, 8), 0.04, ORANGE, 1.0);
        p.part(Pencil.blob(40, 560, 110, 516, 200, 530, 210, 590, 120, 608, 50, 590), 0.5, ORANGE, 0.55);
        p.dot(130, 548, 10, YELLOW);
        p.dot(56, 556, 4, Pencil.LEAD);
        p.curve(3, 48, 586, 110, 590, 160, 580);
    }

    static void lavaGolem(Pencil p) {
        p.part(Pencil.poly(276, 500, 336, 500, 330, 676, 262, 676), 0.8);
        p.part(Pencil.poly(370, 500, 430, 500, 442, 676, 372, 676), 0.8);
        p.part(Pencil.rock(p.r, 170, 480, 76, 70, 7), 0.82);
        p.part(Pencil.rock(p.r, 540, 470, 80, 74, 7), 0.82);
        p.part(Pencil.limb(70, 230, 300, 180, 420), 0.8);
        p.part(Pencil.limb(70, 470, 300, 530, 410), 0.8);
        p.part(Pencil.poly(222, 260, 480, 240, 524, 420, 452, 526, 252, 526, 190, 420), 0.8);
        p.part(Pencil.rock(p.r, 350, 196, 66, 56, 7), 0.82);
        p.dot(328, 196, 9, ORANGE);
        p.dot(372, 196, 9, ORANGE);
        p.colorLine(5, ORANGE, 250, 300, 300, 360, 280, 420, 340, 480);
        p.colorLine(5, ORANGE, 440, 280, 400, 350, 450, 410);
        p.colorLine(4, ORANGE, 160, 440, 190, 490, 170, 530);
        p.colorLine(4, ORANGE, 520, 430, 560, 470, 540, 520);
        p.colorLine(4, ORANGE, 300, 520, 296, 600, 310, 660);
        p.smudge(Pencil.ell(350, 380, 80, 70), ORANGE, 30);
    }

    static void flameWraith(Pencil p) {
        p.smudge(Pencil.ell(350, 400, 230, 270), ORANGE, 28);
        p.part(Pencil.flame(200, 470, 90, 180, -60), 0.05, ORANGE, 1.0);
        p.part(Pencil.flame(500, 470, 90, 180, 60), 0.05, ORANGE, 1.0);
        p.part(Pencil.flame(350, 660, 300, 560, -20), 0.08, ORANGE, 1.0);
        p.part(Pencil.flame(350, 640, 170, 380, 10), 0.03, YELLOW, 1.0);
        p.part(Pencil.ell(320, 390, 22, 28), 0.95);
        p.part(Pencil.ell(384, 390, 22, 28), 0.95);
        p.part(Pencil.blob(318, 456, 386, 456, 372, 500, 330, 500), 0.95);
    }

    static void volcanoOgre(Pencil p) {
        p.part(Pencil.limb(62, 520, 380, 600, 120), 0.65, BROWN, 0.6);
        for (int i = 0; i < 4; i++) {
            double y = 150 + i * 50, x = 600 - (y - 120) * 0.31;
            p.part(Pencil.poly(x + 24, y - 10, x + 58, y, x + 24, y + 12), 0.2);
        }
        double[] f = Monsters.figure(p, 350, 560, 1.5, 0.55, 0.45, 0.45, false, 210, 560, 520, 380, ORANGE);
        p.part(Pencil.ell(350, 440, 132, 116), 0.3, ORANGE, 0.4);
        p.part(Pencil.poly(250, f[4] + 10, 450, f[4] + 10, 420, f[4] + 110, 280, f[4] + 110), 0.6, BROWN, 0.8);
        double hx = f[0], hy = f[1] + 14, hr = f[2] * 1.1;
        p.part(Pencil.ell(hx, hy, hr, hr * 0.9), 0.45, ORANGE, 0.35);
        p.part(Pencil.poly(hx - 10, hy - hr * 0.8, hx + 4, hy - hr * 1.6, hx + 22, hy - hr * 0.8), 0.85);
        p.dot(hx - hr * 0.38, hy - hr * 0.1, 9, ORANGE);
        p.dot(hx + hr * 0.38, hy - hr * 0.1, 9, ORANGE);
        p.part(Pencil.blob(hx - hr * 0.7, hy + hr * 0.3, hx + hr * 0.7, hy + hr * 0.3, hx + hr * 0.6, hy + hr * 0.75,
                hx - hr * 0.6, hy + hr * 0.75), 0.6);
        p.part(Pencil.poly(hx - hr * 0.5, hy + hr * 0.35, hx - hr * 0.42, hy + hr * 0.0, hx - hr * 0.32, hy + hr * 0.35), 0);
        p.part(Pencil.poly(hx + hr * 0.5, hy + hr * 0.35, hx + hr * 0.42, hy + hr * 0.0, hx + hr * 0.32, hy + hr * 0.35), 0);
    }

    static void flameLord(Pencil p) {
        for (int i = 0; i < 7; i++) p.part(Pencil.flame(150 + i * 66, 640, 120, 320 + (i % 3) * 60, (i - 3) * 20), 0.05, ORANGE, 1.0);
        double[] f = Monsters.figure(p, 350, 610, 1.35, 0.85, 0.82, 0.75, false, 224, 520, 526, 430, RED);
        sword(p, 526, 430, 612, 60, 38, ORANGE);
        for (int i = 0; i < 4; i++) p.part(Pencil.flame(560 + i * 14, 330 - i * 70, 40, 80, 14), 0.04, ORANGE, 1.0);
        p.colorLine(5, ORANGE, 300, f[3] + 40, 340, f[3] + 100, 310, f[4] - 20);
        p.colorLine(5, ORANGE, 410, f[3] + 30, 380, f[3] + 120, 420, f[4] - 30);
        double hx = f[0], hy = f[1] + 6, hr = f[2] * 1.0;
        for (int i = 0; i < 5; i++) {
            p.part(Pencil.flame(hx - hr * 0.8 + i * hr * 0.4, hy - hr * 0.6, hr * 0.5, hr * (1.1 + (i == 2 ? 0.7 : 0.2 * (i % 2))), 0),
                    0.04, i % 2 == 0 ? ORANGE : YELLOW, 1.0);
        }
        p.part(Pencil.blob(hx - hr, hy + hr * 0.9, hx - hr * 1.05, hy - hr * 0.5, hx, hy - hr * 1.0,
                hx + hr * 1.05, hy - hr * 0.5, hx + hr, hy + hr * 0.9), 0.88);
        p.dot(hx - hr * 0.32, hy, 9, YELLOW);
        p.dot(hx + hr * 0.32, hy, 9, YELLOW);
    }

    // ================= 7 : 천공의 성채 =================

    static void stormGriffin(Pencil p) {
        p.part(wing(330, 360, 1, 1.0), 0.25, GOLD, 0.3);
        feathers(p, 330, 360, 1, 1.0);
        double[] b = Monsters.beast(p, 320, 0.35, GOLD, 0.35, false);
        p.part(wing(300, 380, 1, 0.85), 0.15, GOLD, 0.3);
        feathers(p, 300, 380, 1, 0.85);
        double hx = 190, hy = b[1];
        p.part(Pencil.blob(hx + 90, hy + 60, hx + 70, hy - 40, hx, hy - 80, hx - 70, hy - 40, hx - 80, hy + 30,
                hx - 20, hy + 90, hx + 60, hy + 120), 0.08);
        p.part(Pencil.blob(hx - 70, hy - 20, hx - 140, hy, hx - 150, hy + 50, hx - 110, hy + 30, hx - 70, hy + 30), 0.2, GOLD, 1.0);
        p.dot(hx - 30, hy - 26, 10, YELLOW);
        p.ink(hx - 50, hy - 46, hx - 10, hy - 40, 3);
        p.colorLine(5, YELLOW, 560, 90, 520, 170, 570, 190, 520, 280);
    }

    static void skyKnight(Pencil p) {
        p.part(Pencil.blob(260, 250, 440, 250, 500, 640, 200, 640), 0.2, ICE, 0.4);   // cape
        p.part(blade(300, 600, 120, 60, 34), 0.25, GOLD, 0.4);   // lance
        p.part(Pencil.poly(150, 140, 230, 130, 170, 200), 0.1, ICE, 0.9);   // pennant
        double[] f = Monsters.figure(p, 350, 520, 1.15, 0.35, 0.3, 0.3, false, 250, 440, 470, 500, GOLD);
        p.part(Pencil.ell(486, 500, 74, 74), 0.25, GOLD, 0.6);
        for (int i = 0; i < 8; i++) {
            double a = PI / 4 * i;
            p.ink(486 + cos(a) * 18, 500 + sin(a) * 18, 486 + cos(a) * 52, 500 + sin(a) * 52, 2.5);
        }
        double hx = f[0], hy = f[1] + 6, hr = f[2] * 1.05;
        Shape plume = Pencil.blob(hx - hr * 0.8, hy - hr * 0.2, hx - hr * 1.6, hy - hr * 1.0, hx - hr * 1.5, hy - hr * 0.4,
                hx - hr * 1.7, hy - hr * 0.1, hx - hr * 0.9, hy + hr * 0.2);
        p.part(plume, 0.04);
        p.part(Pencil.mirror(plume, hx), 0.04);
        p.part(Pencil.ell(hx, hy, hr, hr), 0.3, GOLD, 0.5);
        p.part(Pencil.limb(9, hx - hr * 0.6, hy, hx + hr * 0.6, hy), 0.95);
        p.dot(hx - hr * 0.28, hy, 5, ICE);
        p.dot(hx + hr * 0.28, hy, 5, ICE);
    }

    static void thunderGolem(Pencil p) {
        p.part(Pencil.poly(270, 500, 336, 500, 336, 676, 256, 676), 0.55);
        p.part(Pencil.poly(370, 500, 436, 500, 450, 676, 370, 676), 0.55);
        p.part(Pencil.poly(150, 290, 230, 280, 236, 440, 156, 450), 0.55);
        p.part(Pencil.poly(470, 280, 550, 290, 546, 450, 466, 440), 0.55);
        p.part(Pencil.poly(140, 440, 246, 432, 250, 530, 136, 536), 0.6);
        p.part(Pencil.poly(456, 432, 562, 440, 566, 536, 452, 530), 0.6);
        p.part(Pencil.poly(220, 250, 480, 250, 460, 510, 240, 510), 0.5);
        p.part(Pencil.poly(300, 150, 400, 150, 410, 246, 290, 246), 0.55);
        p.smudge(Pencil.ell(350, 360, 70, 70), YELLOW, 50);
        p.dot(350, 360, 30, YELLOW);
        p.dot(324, 196, 8, YELLOW);
        p.dot(376, 196, 8, YELLOW);
        p.colorLine(4, YELLOW, 350, 390, 320, 440, 360, 460, 330, 510);
        p.colorLine(4, YELLOW, 230, 300, 200, 350, 230, 380, 190, 430);
        p.colorLine(4, YELLOW, 470, 300, 510, 340, 480, 380, 520, 420);
        for (double[] s : new double[][]{{120, 230}, {590, 230}, {600, 150}}) p.part(Pencil.rock(p.r, s[0], s[1], 26, 22, 6), 0.55);
    }

    static void fallenAngel(Pencil p) {
        p.part(wing(330, 330, -1, 1.0), 0.82);
        p.part(wing(370, 330, 1, 1.0), 0.82);
        feathers(p, 330, 330, -1, 1.0);
        feathers(p, 370, 330, 1, 1.0);
        double[] f = Monsters.figure(p, 350, 520, 1.0, 0.3, 0.25, 0.2, true, 244, 470, 470, 420);
        sword(p, 470, 420, 540, 190, 24, null);
        double hx = f[0], hy = f[1] + 6, hr = f[2] * 0.95;
        p.part(Pencil.blob(hx - hr * 1.1, hy + hr * 1.2, hx - hr * 1.1, hy - hr * 0.6, hx, hy - hr * 1.2,
                hx + hr * 1.1, hy - hr * 0.6, hx + hr * 1.1, hy + hr * 1.2), 0.75);
        p.part(Pencil.ell(hx, hy + hr * 0.1, hr * 0.78, hr * 0.85), 0.04);
        p.dot(hx - hr * 0.3, hy + hr * 0.05, 6, RED);
        p.dot(hx + hr * 0.3, hy + hr * 0.05, 6, RED);
        p.colorLine(4, GOLD, hx - hr * 0.9, hy - hr * 1.6, hx - hr * 0.5, hy - hr * 1.9, hx, hy - hr * 1.95, hx + hr * 0.3, hy - hr * 1.88);
        p.colorLine(4, GOLD, hx + hr * 0.6, hy - hr * 1.8, hx + hr * 0.9, hy - hr * 1.55);
    }

    static void fallenKing(Pencil p) {
        for (int k = 2; k >= 0; k--) {
            double s = 0.75 + k * 0.17, y = 300 + k * 30;
            p.part(wing(330, y, -1, s), 0.9 - k * 0.08);
            p.part(wing(370, y, 1, s), 0.9 - k * 0.08);
            feathers(p, 330, y, -1, s);
            feathers(p, 370, y, 1, s);
        }
        double[] f = Monsters.figure(p, 350, 610, 1.3, 0.55, 0.5, 0.45, false, 226, 540, 524, 420, GOLD);
        sword(p, 524, 420, 600, 50, 40, GOLD);
        for (double sx : new double[]{238, 462}) p.part(Pencil.ell(sx, f[3] + 18, 60, 42), 0.6, GOLD, 0.5);
        double hx = f[0], hy = f[1] + 4, hr = f[2] * 1.0;
        Shape halo = Pencil.ell(hx, hy - hr * 1.3, hr * 1.4, hr * 0.45);
        p.outline(halo, 5);
        p.smudge(halo, GOLD, 40);
        p.part(Pencil.ell(hx, hy, hr, hr), 0.8);
        p.part(Pencil.limb(10, hx - hr * 0.6, hy, hx + hr * 0.6, hy), 1.0);
        p.dot(hx - hr * 0.3, hy, 7, RED);
        p.dot(hx + hr * 0.3, hy, 7, RED);
        p.part(Pencil.poly(hx - hr * 0.95, hy - hr * 0.6, hx - hr * 0.75, hy - hr * 1.5, hx - hr * 0.4, hy - hr * 0.9,
                hx, hy - hr * 1.75, hx + hr * 0.4, hy - hr * 0.9, hx + hr * 0.75, hy - hr * 1.5, hx + hr * 0.95, hy - hr * 0.6), 0.15, GOLD, 1.0);
    }
}
