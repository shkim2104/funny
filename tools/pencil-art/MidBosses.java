import java.awt.*;

import static java.lang.Math.*;

/**
 * The 중간 보스 of each dungeon (stage 10), on the same 700x700 canvas and conventions as {@link Monsters}:
 * bigger and busier than the dungeon's regular monsters, smaller than its boss.
 */
final class MidBosses {
    private static final double G = Monsters.G;
    private static final Color RED = Monsters.RED, ORANGE = Monsters.ORANGE, ICE = Monsters.ICE, GOLD = Monsters.GOLD,
            PURPLE = Monsters.PURPLE, GREEN = Monsters.GREEN, YELLOW = Monsters.YELLOW, BROWN = Monsters.BROWN;

    private MidBosses() {}

    /** A curled horn from the root (x, y) sweeping out to one side (side = -1 left, 1 right). */
    private static Shape horn(double x, double y, int side, double s) {
        return Pencil.blob(x, y - 12 * s, x + side * 46 * s, y - 34 * s, x + side * 92 * s, y - 20 * s, x + side * 110 * s, y - 70 * s,
                x + side * 84 * s, y + 4 * s, x + side * 40 * s, y + 12 * s, x, y + 12 * s);
    }

    /** A skull at (hx, hy) with radius r. */
    private static void skull(Pencil p, double hx, double hy, double r, Color eye) {
        p.part(Pencil.blob(hx - r, hy + r * 0.2, hx - r * 0.9, hy - r * 0.8, hx, hy - r * 1.05, hx + r * 0.9, hy - r * 0.8,
                hx + r, hy + r * 0.2, hx + r * 0.55, hy + r * 0.75, hx - r * 0.55, hy + r * 0.75), 0.04);
        p.part(Pencil.ell(hx - r * 0.38, hy - r * 0.05, r * 0.26, r * 0.3), 0.95);
        p.part(Pencil.ell(hx + r * 0.38, hy - r * 0.05, r * 0.26, r * 0.3), 0.95);
        p.dot(hx - r * 0.38, hy - r * 0.02, r * 0.1, eye);
        p.dot(hx + r * 0.38, hy - r * 0.02, r * 0.1, eye);
        p.part(Pencil.poly(hx, hy + r * 0.2, hx - r * 0.12, hy + r * 0.42, hx + r * 0.12, hy + r * 0.42), 0.9);
        for (int i = 0; i < 5; i++) p.ink(hx - r * 0.4 + i * r * 0.2, hy + r * 0.55, hx - r * 0.4 + i * r * 0.2, hy + r * 0.78, 2.2);
    }

    /** A zigzag lightning bolt from (x, y) downwards, h tall. */
    private static void bolt(Pencil p, double x, double y, double h, double w, Color c) {
        p.colorLine(w, c, x, y, x - h * 0.18, y + h * 0.38, x + h * 0.08, y + h * 0.42, x - h * 0.12, y + h);
    }

    // ================= 1 : 초원 — 성난 들소 =================

    static void angryBison(Pencil p) {
        double[] b = Monsters.beast(p, 380, 0.72, BROWN, 0.75, false);
        // A shaggy hump over the shoulders and a beard.
        p.part(Pencil.blob(190, b[1] + 60, 210, b[1] - 70, 300, b[1] - 120, 400, b[1] - 60, 400, b[1] + 40, 300, b[1] + 90), 0.82, BROWN, 0.6);
        for (int i = 0; i < 10; i++) {
            double x = 220 + i * 18, y = b[1] - 80 + abs(i - 5) * 9;
            p.ink(x, y + 30, x + 8, y - 12, 3);
        }
        double hx = 170, hy = b[1] + 90;
        p.part(Pencil.blob(hx + 80, hy - 70, hx - 10, hy - 90, hx - 90, hy - 40, hx - 112, hy + 30, hx - 70, hy + 80,
                hx + 40, hy + 76, hx + 96, hy + 20), 0.78, BROWN, 0.6);
        p.part(Pencil.blob(hx - 60, hy + 60, hx - 20, hy + 70, hx - 30, hy + 150, hx - 60, hy + 120), 0.88);   // beard
        p.part(horn(hx - 20, hy - 74, -1, 0.9), 0.08);
        p.part(horn(hx + 40, hy - 78, 1, 0.75), 0.1);
        p.part(Pencil.ell(hx - 100, hy + 30, 26, 30), 0.45, BROWN, 0.5);
        p.dot(hx - 108, hy + 22, 6, Pencil.LEAD);
        p.dot(hx - 94, hy + 40, 6, Pencil.LEAD);
        p.dot(hx - 40, hy - 26, 11, RED);
        p.ink(hx - 70, hy - 50, hx - 18, hy - 36, 4);   // angry brow
        // Snorting steam.
        for (int i = 0; i < 3; i++) p.part(Pencil.ell(hx - 150 - i * 34, hy + 10 - i * 18, 16 + i * 6, 12 + i * 5), 0.02);
        // Dust kicked up by a stamping hoof.
        for (int i = 0; i < 4; i++) p.part(Pencil.ell(560 + i * 30, G - 18 - (i % 2) * 14, 22, 12), 0.15, BROWN, 0.3);
    }

    // ================= 2 : 어두운 동굴 — 고블린 주술사 =================

    static void goblinShaman(Pencil p) {
        // A crooked staff with a skull and a poison-green orb.
        p.part(Pencil.limb(14, 520, 660, 548, 470, 530, 200), 0.7, BROWN, 0.8);
        skull(p, 530, 190, 34, GREEN);
        p.part(Pencil.ell(530, 120, 34, 34), 0.05, GREEN, 1.0);
        for (int i = 0; i < 5; i++) p.part(Pencil.ell(470 + i * 26, 90 - (i % 2) * 30 - i * 8, 7 + i % 3 * 3, 7 + i % 3 * 3), 0.02, GREEN, 0.9);
        double[] f = Monsters.figure(p, 340, 470, 1.15, 0.5, 0.55, 0.3, true, 236, 470, 520, 440, PURPLE);
        // Bone necklace.
        for (int i = 0; i < 6; i++) {
            double a = toRadians(200 + i * 28);
            p.part(Pencil.ell(340 + cos(a) * 70, f[3] + 30 - sin(a) * 30, 9, 14), 0.02);
        }
        double hx = f[0], hy = f[1] + 8, hr = f[2] * 1.5;
        Shape ear = Pencil.poly(hx - hr * 0.8, hy - 6, hx - hr * 2.1, hy - hr, hx - hr * 0.75, hy + hr * 0.35);
        p.part(ear, 0.3, GREEN, 0.7);
        p.part(Pencil.mirror(ear, hx), 0.3, GREEN, 0.7);
        p.part(Pencil.ell(hx, hy, hr, hr * 0.88), 0.25, GREEN, 0.8);
        // A tall pointed shaman hat with a feather.
        p.part(Pencil.blob(hx - hr * 1.15, hy - hr * 0.55, hx, hy - hr * 2.6, hx + hr * 0.4, hy - hr * 2.4, hx + hr * 1.15, hy - hr * 0.55), 0.7, PURPLE, 0.8);
        p.part(Pencil.blob(hx + hr * 0.3, hy - hr * 1.6, hx + hr * 1.2, hy - hr * 2.6, hx + hr * 1.4, hy - hr * 2.4, hx + hr * 0.5, hy - hr * 1.3), 0.1, RED, 0.9);
        p.dot(hx - hr * 0.38, hy - hr * 0.08, 10, GREEN);
        p.dot(hx + hr * 0.38, hy - hr * 0.08, 10, GREEN);
        p.part(Pencil.poly(hx - 6, hy + 2, hx + 12, hy + hr * 0.32, hx - 14, hy + hr * 0.3), 0.4);
        p.curve(3, hx - hr * 0.42, hy + hr * 0.52, hx, hy + hr * 0.42, hx + hr * 0.42, hy + hr * 0.52);
        // Bubbling poison in the left hand.
        for (int i = 0; i < 4; i++) p.part(Pencil.ell(236 - 10 + i * 8, 440 - i * 28, 10 + i * 2, 10 + i * 2), 0.02, GREEN, 0.9);
    }

    // ================= 3 : 잊혀진 폐허 — 해골 기사 =================

    static void skeletonKnight(Pencil p) {
        // Tattered cape behind.
        p.part(Pencil.blob(270, 250, 430, 250, 470, 470, 500, 640, 440, 610, 390, 650, 330, 610, 270, 650, 220, 620, 230, 470), 0.78, RED, 0.35);
        double[] f = Monsters.figure(p, 350, 560, 1.1, 0.25, 0.45, 0.2, false, 218, 470, 520, 440);
        // Rusty breastplate over the ribs.
        p.part(Pencil.blob(f[0] - f[5] * 0.9, f[3] + 10, f[0] + f[5] * 0.9, f[3] + 10, f[0] + f[5] * 0.8, f[4] - 10, f[0] - f[5] * 0.8, f[4] - 10), 0.5, BROWN, 0.5);
        for (int i = 0; i < 3; i++) p.ink(f[0] - f[5] * 0.6, f[3] + 50 + i * 36, f[0] + f[5] * 0.6, f[3] + 50 + i * 36, 2.4);
        // A notched sword up in the right hand.
        p.part(Pencil.poly(512, 440, 532, 440, 570, 110, 548, 92, 520, 160, 534, 200, 510, 260), 0.12);
        p.part(Pencil.limb(14, 490, 450, 556, 432), 0.75);
        // A round rusty shield in the left.
        p.part(Pencil.ell(218, 470, 104, 116), 0.55, BROWN, 0.6);
        p.part(Pencil.ell(218, 470, 30, 32), 0.2, GOLD, 0.7);
        for (int i = 0; i < 4; i++) p.ink(218 + cos(i * PI / 2) * 40, 470 + sin(i * PI / 2) * 44, 218 + cos(i * PI / 2) * 96, 470 + sin(i * PI / 2) * 106, 2.2);
        double hx = f[0], hy = f[1] + 10, hr = f[2] * 1.15;
        skull(p, hx, hy, hr, PURPLE);
        // Broken helmet with a plume.
        p.part(Pencil.blob(hx - hr * 1.05, hy - hr * 0.3, hx - hr * 0.9, hy - hr * 1.1, hx, hy - hr * 1.3, hx + hr * 0.9, hy - hr * 1.1, hx + hr * 1.05, hy - hr * 0.3,
                hx, hy - hr * 0.6), 0.6, BROWN, 0.4);
        p.part(Pencil.blob(hx - 6, hy - hr * 1.25, hx + hr * 0.6, hy - hr * 2.2, hx + hr * 1.4, hy - hr * 1.9, hx + hr * 0.5, hy - hr * 1.15), 0.3, RED, 0.9);
    }

    // ================= 4 : 마왕성 — 마왕의 근위대장 =================

    static void demonCaptain(Pencil p) {
        // A long halberd behind the right arm.
        p.part(Pencil.limb(14, 540, 670, 560, 70), 0.75, BROWN, 0.6);
        p.part(Pencil.blob(560, 70, 650, 110, 660, 200, 562, 180), 0.15, PURPLE, 0.5);
        p.part(Pencil.poly(548, 120, 520, 40, 576, 100), 0.1);
        // A deep red cape.
        p.part(Pencil.blob(250, 250, 450, 250, 500, 470, 520, 660, 180, 660, 200, 470), 0.82, RED, 0.6);
        double[] f = Monsters.figure(p, 350, 580, 1.35, 0.85, 0.88, 0.8, false, 232, 480, 548, 420, PURPLE);
        // Spiked pauldrons.
        for (int side = -1; side <= 1; side += 2) {
            double sx = f[0] + side * f[5] * 1.0, sy = f[3] + 18;
            p.part(Pencil.ell(sx, sy, 52, 40), 0.85, PURPLE, 0.5);
            for (int i = 0; i < 3; i++) p.part(Pencil.poly(sx - 14 + i * 14, sy - 30, sx - 6 + i * 14, sy - 70, sx + 2 + i * 14, sy - 30), 0.1);
        }
        p.part(Pencil.poly(f[0] - 30, f[3] + 60, f[0], f[3] + 120, f[0] + 30, f[3] + 60), 0.1, RED, 1.0);   // emblem
        double hx = f[0], hy = f[1] + 10, hr = f[2] * 1.05;
        // Closed horned helm with a glowing slit.
        p.part(horn(hx - hr * 0.7, hy - hr * 0.5, -1, 0.8), 0.15);
        p.part(horn(hx + hr * 0.7, hy - hr * 0.5, 1, 0.8), 0.15);
        p.part(Pencil.blob(hx - hr, hy + hr * 0.8, hx - hr * 1.05, hy - hr * 0.5, hx, hy - hr * 1.1, hx + hr * 1.05, hy - hr * 0.5,
                hx + hr, hy + hr * 0.8, hx, hy + hr * 1.05), 0.9);
        p.colorLine(6, RED, hx - hr * 0.65, hy - hr * 0.05, hx + hr * 0.65, hy - hr * 0.05);
        p.dot(hx - hr * 0.32, hy - hr * 0.05, 8, YELLOW);
        p.dot(hx + hr * 0.32, hy - hr * 0.05, 8, YELLOW);
    }

    // ================= 5 : 얼음 협곡 — 설인 =================

    static void yeti(Pencil p) {
        // A big snowball hoisted overhead.
        double[] f = Monsters.figure(p, 320, 540, 1.65, 0.12, 0.1, 0.12, false, 186, 520, 520, 210, ICE);
        p.part(Pencil.ell(568, 128, 86, 80), 0.03, ICE, 0.35);
        p.ink(520, 110, 566, 92, 2.2);
        p.ink(576, 160, 616, 140, 2.2);
        p.part(Pencil.ell(520, 210, 30, 30), 0.1, ICE, 0.3);   // the hand over the snowball
        // Fur tufts all over the body and arms.
        for (int i = 0; i < 14; i++) {
            double x = 230 + (i % 7) * 30, y = f[3] + 60 + (i / 7) * 120 + (i % 2) * 20;
            p.ink(x, y, x - 10, y + 24, 2.4);
            p.ink(x + 8, y, x, y + 26, 2.4);
        }
        p.part(Pencil.ell(320, (f[3] + f[4]) / 2 + 30, 90, 110), 0.25, ICE, 0.3);   // pale belly
        double hx = f[0], hy = f[1] + 22, hr = f[2] * 1.2;
        p.part(Pencil.blob(hx - hr * 1.1, hy + hr * 0.4, hx - hr, hy - hr * 0.7, hx, hy - hr * 1.15, hx + hr, hy - hr * 0.7,
                hx + hr * 1.1, hy + hr * 0.4, hx, hy + hr * 1.0), 0.1, ICE, 0.3);
        p.part(Pencil.ell(hx, hy + hr * 0.15, hr * 0.62, hr * 0.55), 0.55, ICE, 0.6);   // face
        p.dot(hx - hr * 0.28, hy, 9, Pencil.LEAD);
        p.dot(hx + hr * 0.28, hy, 9, Pencil.LEAD);
        p.ink(hx - hr * 0.5, hy - hr * 0.25, hx - hr * 0.12, hy - hr * 0.12, 3.4);
        p.ink(hx + hr * 0.5, hy - hr * 0.25, hx + hr * 0.12, hy - hr * 0.12, 3.4);
        p.part(Pencil.blob(hx - hr * 0.4, hy + hr * 0.38, hx + hr * 0.4, hy + hr * 0.38, hx + hr * 0.3, hy + hr * 0.62, hx - hr * 0.3, hy + hr * 0.62), 0.9);
        p.part(Pencil.poly(hx - hr * 0.3, hy + hr * 0.38, hx - hr * 0.22, hy + hr * 0.56, hx - hr * 0.14, hy + hr * 0.38), 0);
        p.part(Pencil.poly(hx + hr * 0.3, hy + hr * 0.38, hx + hr * 0.22, hy + hr * 0.56, hx + hr * 0.14, hy + hr * 0.38), 0);
        // Icicles on the brow ridge of the canyon floor.
        for (int i = 0; i < 5; i++) p.part(Pencil.poly(120 + i * 28, G - 4, 132 + i * 28, G - 50 - (i % 2) * 22, 144 + i * 28, G - 4), 0.05, ICE, 1.0);
    }

    // ================= 6 : 불지옥 화산 — 화염 와이번 =================

    static void fireWyvern(Pencil p) {
        // Far wing.
        p.part(Pencil.blob(380, 330, 450, 130, 560, 40, 640, 80, 620, 200, 660, 300, 560, 300, 520, 380), 0.55, RED, 0.5);
        for (int i = 0; i < 4; i++) p.ink(400, 330, 470 + i * 50, 70 + i * 50, 2.4);
        double[] b = Monsters.beast(p, 300, 0.6, RED, 0.6, false);
        // Near wing over the back.
        p.part(Pencil.blob(330, 360, 330, 160, 400, 60, 470, 90, 480, 200, 540, 260, 450, 300, 400, 380), 0.45, ORANGE, 0.55);
        for (int i = 0; i < 3; i++) p.ink(340, 350, 390 + i * 40, 90 + i * 60, 2.2);
        // A long neck and horned head reaching to the player.
        p.part(Pencil.limb(70, 240, b[1] + 60, 210, b[1] - 30, 180, b[1] - 80), 0.6, RED, 0.6);
        double hx = 170, hy = b[1] - 90;
        p.part(Pencil.blob(hx + 60, hy - 30, hx, hy - 50, hx - 70, hy - 20, hx - 110, hy + 10, hx - 70, hy + 34, hx + 50, hy + 40), 0.6, RED, 0.7);
        p.part(Pencil.poly(hx + 30, hy - 40, hx + 90, hy - 110, hx + 60, hy - 30), 0.15);
        p.part(Pencil.poly(hx + 10, hy - 44, hx + 50, hy - 116, hx + 36, hy - 38), 0.15);
        p.dot(hx - 20, hy - 18, 9, YELLOW);
        for (int i = 0; i < 3; i++) p.part(Pencil.poly(hx - 96 + i * 18, hy + 18, hx - 90 + i * 18, hy + 34, hx - 84 + i * 18, hy + 18), 0);
        // A puff of fire from the jaws.
        for (int i = 0; i < 3; i++) p.part(Pencil.ell(hx - 128 - i * 28, hy + 14 + i * 10, 30 - i * 6, 22 - i * 4), 0.03, i == 1 ? YELLOW : ORANGE, 1.0);
        // Spikes down the back and tail.
        for (int i = 0; i < 6; i++) {
            double x = 250 + i * 50, y = b[1] - 80 + i * 8;
            p.part(Pencil.poly(x - 12, y + 24, x, y - 10, x + 12, y + 24), 0.2, ORANGE, 0.8);
        }
    }

    // ================= 7 : 천공의 성채 — 폭풍의 수호자 =================

    static void stormWarden(Pencil p) {
        // A storm cloud it floats on instead of legs.
        for (int i = 0; i < 6; i++) p.part(Pencil.ell(200 + i * 60, G - 50 - (i % 2) * 24, 80, 50), 0.5, ICE, 0.3);
        bolt(p, 230, 560, 120, 6, YELLOW);
        bolt(p, 480, 570, 110, 6, YELLOW);
        // A round shield of sky-steel and a spear crackling with lightning.
        p.part(Pencil.limb(14, 540, 640, 560, 90), 0.2, GOLD, 0.6);
        p.part(Pencil.poly(560, 90, 538, 160, 584, 160), 0.1, GOLD, 0.8);
        bolt(p, 600, 80, 160, 5, YELLOW);
        double[] f = Monsters.figure(p, 350, 560, 1.25, 0.3, 0.2, 0.2, true, 222, 440, 548, 400, GOLD);
        p.part(Pencil.ell(222, 440, 96, 108), 0.1, GOLD, 0.7);
        p.part(Pencil.ell(222, 440, 40, 44), 0.05, ICE, 0.8);
        bolt(p, 232, 400, 80, 5, YELLOW);
        // Winged helm and glowing eyes.
        double hx = f[0], hy = f[1] + 8, hr = f[2] * 1.05;
        for (int side = -1; side <= 1; side += 2) {
            p.part(Pencil.blob(hx + side * hr * 0.8, hy - hr * 0.3, hx + side * hr * 1.8, hy - hr * 1.6, hx + side * hr * 2.1, hy - hr * 1.2,
                    hx + side * hr * 1.4, hy - hr * 0.2), 0.04, ICE, 0.5);
        }
        p.part(Pencil.blob(hx - hr, hy + hr * 0.8, hx - hr * 1.05, hy - hr * 0.5, hx, hy - hr * 1.1, hx + hr * 1.05, hy - hr * 0.5,
                hx + hr, hy + hr * 0.8, hx, hy + hr * 1.0), 0.3, GOLD, 0.6);
        p.part(Pencil.blob(hx - hr * 0.7, hy - hr * 0.2, hx + hr * 0.7, hy - hr * 0.2, hx + hr * 0.5, hy + hr * 0.6, hx - hr * 0.5, hy + hr * 0.6), 0.85);
        p.dot(hx - hr * 0.3, hy + hr * 0.08, 10, ICE);
        p.dot(hx + hr * 0.3, hy + hr * 0.08, 10, ICE);
        // Sparks around.
        for (int i = 0; i < 6; i++) {
            double a = toRadians(200 + i * 26);
            p.colorLine(3, YELLOW, 350 + cos(a) * 250, 330 + sin(a) * 220, 350 + cos(a) * 280, 330 + sin(a) * 250);
        }
    }
}
