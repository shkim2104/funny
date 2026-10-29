import java.awt.*;

import static java.lang.Math.*;

/**
 * Monster drawings on a 700x700 transparent canvas, feet on y = G, roughly centered on x = 350 and facing
 * the player (who stands bottom-left). Shapes go back to front; each part is backed with paper so the
 * monster reads as a pencil drawing on the page, not a see-through sketch.
 */
final class Monsters {
    static final int SIZE = 700;
    static final double G = 680;

    static final Color RED = new Color(206, 52, 48);
    static final Color ORANGE = new Color(238, 128, 36);
    static final Color ICE = new Color(86, 168, 228);
    static final Color GOLD = new Color(214, 170, 44);
    static final Color PURPLE = new Color(138, 82, 196);
    static final Color GREEN = new Color(96, 156, 72);
    static final Color YELLOW = new Color(240, 206, 50);
    static final Color BROWN = new Color(150, 98, 56);

    private Monsters() {}

    interface Drawing {
        void draw(Pencil p);
    }

    // ================= shared bodies =================

    /**
     * A standing humanoid; draws legs (or a robe), torso and arms with hands at the given points and
     * returns {headX, headY, headR, shoulderY, hipY, torsoHalfW} for the caller's head and props.
     */
    static double[] figure(Pencil p, double cx, double h, double girth, double legTone, double bodyTone,
                           double armTone, boolean robe, double lhx, double lhy, double rhx, double rhy) {
        return figure(p, cx, h, girth, legTone, bodyTone, armTone, robe, lhx, lhy, rhx, rhy, null);
    }

    /** As above, with a colored-pencil accent on the torso/robe (null for plain graphite). */
    static double[] figure(Pencil p, double cx, double h, double girth, double legTone, double bodyTone,
                           double armTone, boolean robe, double lhx, double lhy, double rhx, double rhy, Color accent) {
        double headR = h * 0.105;
        double headY = G - h + headR;
        double shoulderY = headY + headR + h * 0.05;
        double hipY = G - h * 0.44;
        double tw = h * 0.16 * girth;
        double limbW = h * 0.075 * girth;
        if (!robe) {
            p.part(Pencil.limb(limbW * 1.15, cx - tw * 0.5, hipY, cx - tw * 0.62, G - limbW * 0.7), legTone);
            p.part(Pencil.limb(limbW * 1.15, cx + tw * 0.5, hipY, cx + tw * 0.62, G - limbW * 0.7), legTone);
            p.part(Pencil.ell(cx - tw * 0.7, G - limbW * 0.35, limbW * 0.85, limbW * 0.4), min(1, legTone + 0.25));
            p.part(Pencil.ell(cx + tw * 0.7, G - limbW * 0.35, limbW * 0.85, limbW * 0.4), min(1, legTone + 0.25));
        }
        // Arms behind the torso first, so shoulders tuck in.
        double sxL = cx - tw * 0.85, sxR = cx + tw * 0.85, sy = shoulderY + limbW * 0.4;
        p.part(Pencil.limb(limbW, sxL, sy, (sxL + lhx) / 2 - limbW * 0.6, (sy + lhy) / 2 + limbW * 0.3, lhx, lhy), armTone);
        p.part(Pencil.limb(limbW, sxR, sy, (sxR + rhx) / 2 + limbW * 0.6, (sy + rhy) / 2 + limbW * 0.3, rhx, rhy), armTone);
        Shape torso = robe
                ? Pencil.blob(cx - tw, shoulderY, cx + tw, shoulderY, cx + tw * 1.25, (shoulderY + G) / 2,
                        cx + tw * 1.7, G - 4, cx, G + 4, cx - tw * 1.7, G - 4, cx - tw * 1.25, (shoulderY + G) / 2)
                : Pencil.blob(cx - tw, shoulderY, cx, shoulderY - limbW * 0.2, cx + tw, shoulderY,
                        cx + tw * 0.85, hipY + limbW * 0.2, cx, hipY + limbW * 0.4, cx - tw * 0.85, hipY + limbW * 0.2);
        if (accent != null) p.part(torso, bodyTone, accent, 0.6);
        else p.part(torso, bodyTone);
        p.part(Pencil.ell(lhx, lhy, limbW * 0.62, limbW * 0.62), armTone * 0.8);
        p.part(Pencil.ell(rhx, rhy, limbW * 0.62, limbW * 0.62), armTone * 0.8);
        return new double[]{cx, headY, headR, shoulderY, hipY, tw};
    }

    /** A four-legged beast facing left: body, legs, tail. Returns {headX, headY, size}. */
    static double[] beast(Pencil p, double size, double tone, Color accent, double accentStrength, boolean bushyTail) {
        double bodyY = G - size * 0.62;
        double legW = size * 0.15;
        // Far legs (lighter, behind), then tail, body, near legs.
        p.part(Pencil.limb(legW, 250, bodyY + 10, 262, G - legW * 0.4), min(1, tone + 0.15));
        p.part(Pencil.limb(legW, 470, bodyY + 10, 486, G - legW * 0.4), min(1, tone + 0.15));
        if (bushyTail) {
            p.part(Pencil.blob(540, bodyY - 30, 620, bodyY - 90, 668, bodyY - 60, 640, bodyY + 10, 560, bodyY + 30), tone);
        } else {
            p.part(Pencil.limb(legW * 0.5, 540, bodyY - 10, 610, bodyY - 60, 650, bodyY - 30), tone);
        }
        Shape body = Pencil.blob(190, bodyY - size * 0.18, 330, bodyY - size * 0.3, 470, bodyY - size * 0.26,
                565, bodyY - size * 0.05, 520, bodyY + size * 0.22, 330, bodyY + size * 0.26, 210, bodyY + size * 0.2);
        if (accent != null) p.part(body, tone, accent, accentStrength);
        else p.part(body, tone);
        p.part(Pencil.limb(legW, 220, bodyY + 30, 205, G - legW * 0.4), tone);
        p.part(Pencil.limb(legW, 500, bodyY + 30, 520, G - legW * 0.4), tone);
        for (double fx : new double[]{205, 520}) p.part(Pencil.ell(fx - 8, G - legW * 0.3, legW * 0.75, legW * 0.35), tone);
        return new double[]{170, bodyY - size * 0.3, size};
    }

    /** A canine head facing left at (hx, hy); open jaw when snarling. */
    static void dogHead(Pencil p, double hx, double hy, double s, double tone, Color eye, Color accent) {
        p.part(Pencil.poly(hx + 40 * s, hy - 40 * s, hx + 62 * s, hy - 110 * s, hx + 82 * s, hy - 38 * s), tone);   // ear
        Shape head = Pencil.blob(hx + 90 * s, hy - 40 * s, hx + 30 * s, hy - 62 * s, hx - 40 * s, hy - 30 * s,
                hx - 120 * s, hy - 6 * s, hx - 118 * s, hy + 22 * s, hx - 40 * s, hy + 34 * s, hx + 80 * s, hy + 40 * s);
        if (accent != null) p.part(head, tone, accent, 0.6);
        else p.part(head, tone);
        p.part(Pencil.blob(hx - 110 * s, hy + 24 * s, hx - 40 * s, hy + 30 * s, hx - 30 * s, hy + 60 * s,
                hx - 90 * s, hy + 52 * s), min(1, tone + 0.1));   // lower jaw
        for (int i = 0; i < 3; i++) {
            double tx = hx - 100 * s + i * 22 * s;
            p.part(Pencil.poly(tx, hy + 22 * s, tx + 8 * s, hy + 40 * s, tx + 14 * s, hy + 22 * s), 0);
        }
        p.dot(hx - 116 * s, hy - 2 * s, 9 * s, Pencil.LEAD);   // nose
        p.dot(hx - 22 * s, hy - 22 * s, 9 * s, eye);
        p.ink(hx - 40 * s, hy - 36 * s, hx - 6 * s, hy - 30 * s, 2.4);   // brow
    }

    /** Bat (used by the cave and, frosted, the ice canyon). */
    static void bat(Pencil p, double tone, Color accent, Color eye) {
        Shape wing = Pencil.poly(318, 352, 250, 250, 140, 214, 34, 252, 72, 300, 96, 362, 150, 330, 186, 410,
                236, 372, 292, 440);
        for (Shape w : new Shape[]{wing, Pencil.mirror(wing, 350)}) {
            if (accent != null) p.part(w, tone, accent, 0.7);
            else p.part(w, tone);
        }
        for (int side = -1; side <= 1; side += 2) {
            double sx = 350 + side * 32;
            p.ink(sx, 352, 350 + side * 210, 214, 2.6);
            p.ink(sx, 352, 350 + side * 254, 362, 2.4);
            p.ink(sx, 352, 350 + side * 164, 410, 2.4);
        }
        p.part(Pencil.poly(314, 280, 300, 186, 340, 258), tone + 0.1);
        p.part(Pencil.mirror(Pencil.poly(314, 280, 300, 186, 340, 258), 350), tone + 0.1);
        p.part(Pencil.ell(350, 410, 62, 86), min(1, tone + 0.15));
        p.part(Pencil.ell(350, 300, 58, 52), min(1, tone + 0.1));
        p.dot(328, 296, 9, eye);
        p.dot(372, 296, 9, eye);
        p.part(Pencil.poly(334, 326, 341, 350, 348, 326), 0);
        p.part(Pencil.poly(352, 326, 359, 350, 366, 326), 0);
        p.ink(330, 492, 322, 520, 3);
        p.ink(370, 492, 378, 520, 3);
    }

    // ================= 1 : 초원 =================

    static void rat(Pencil p) {
        p.part(Pencil.limb(12, 470, 600, 560, 560, 640, 600, 670, 560), 0.3, BROWN, 0.4);   // tail
        p.part(Pencil.limb(26, 280, 620, 270, 672), 0.55);
        p.part(Pencil.limb(26, 430, 620, 446, 672), 0.55);
        p.part(Pencil.blob(200, 580, 300, 500, 430, 500, 500, 560, 470, 640, 300, 650, 210, 630), 0.42, BROWN, 0.5);
        p.part(Pencil.limb(26, 250, 630, 236, 674), 0.5);
        p.part(Pencil.limb(26, 450, 630, 470, 674), 0.5);
        p.part(Pencil.ell(250, 500, 44, 44), 0.35, BROWN, 0.4);   // ear
        p.part(Pencil.ell(250, 500, 24, 24), 0.05, RED, 0.3);
        p.part(Pencil.blob(250, 540, 190, 540, 120, 580, 130, 600, 200, 612, 270, 600), 0.4, BROWN, 0.5);   // snout
        p.dot(126, 588, 9, Pencil.LEAD);
        p.dot(196, 562, 8, Pencil.LEAD);
        for (int i = -1; i <= 1; i++) p.ink(150, 594, 90, 584 + i * 16, 1.8);
        p.part(Pencil.poly(150, 604, 156, 622, 162, 604), 0);
    }

    static void boar(Pencil p) {
        double[] b = beast(p, 330, 0.62, BROWN, 0.6, false);
        for (int i = 0; i < 9; i++) {
            double x = 230 + i * 30, y = b[1] - 4 + abs(i - 4) * 4;
            p.ink(x, y + 16, x + 6, y - 22, 3);   // bristles
        }
        double hx = 170, hy = b[1] + 70;
        p.part(Pencil.poly(hx + 30, hy - 60, hx + 52, hy - 110, hx + 74, hy - 54), 0.6, BROWN, 0.5);
        p.part(Pencil.blob(hx + 90, hy - 50, hx + 10, hy - 70, hx - 70, hy - 30, hx - 100, hy + 20, hx - 60, hy + 70,
                hx + 60, hy + 70, hx + 100, hy + 20), 0.6, BROWN, 0.6);
        p.part(Pencil.ell(hx - 100, hy + 20, 26, 34), 0.3, BROWN, 0.5);   // snout disc
        p.dot(hx - 106, hy + 12, 5, Pencil.LEAD);
        p.dot(hx - 94, hy + 28, 5, Pencil.LEAD);
        p.dot(hx - 20, hy - 22, 9, RED);
        Shape tusk = Pencil.blob(hx - 70, hy + 46, hx - 96, hy + 30, hx - 104, hy - 4, hx - 84, hy + 22, hx - 58, hy + 36);
        p.part(tusk, 0);
        p.part(Pencil.mirror(tusk, hx - 50), 0);
    }

    static void dog(Pencil p) {
        double[] b = beast(p, 320, 0.45, BROWN, 0.45, false);
        dogHead(p, 196, b[1] + 16, 1.0, 0.45, YELLOW, BROWN);
        p.part(Pencil.limb(18, 210, b[1] + 70, 290, b[1] + 80), 0.2, RED, 0.8);   // tattered collar
    }

    static void raccoon(Pencil p) {
        double[] b = beast(p, 290, 0.35, BROWN, 0.35, true);
        // Ringed tail: dark bands across the bushy tail.
        for (int i = 0; i < 3; i++) {
            double x = 580 + i * 26;
            p.part(Pencil.blob(x - 8, b[1] + 20 - i * 18, x + 10, b[1] + 6 - i * 20, x + 20, b[1] + 40 - i * 22, x, b[1] + 50 - i * 18), 0.9);
        }
        double hx = 170, hy = b[1] + 40;
        p.part(Pencil.poly(hx - 10, hy - 50, hx + 6, hy - 104, hx + 34, hy - 52), 0.5);
        p.part(Pencil.poly(hx + 50, hy - 46, hx + 76, hy - 96, hx + 92, hy - 40), 0.5);
        p.part(Pencil.blob(hx + 110, hy - 10, hx + 40, hy - 60, hx - 40, hy - 40, hx - 100, hy + 10, hx - 60, hy + 60,
                hx + 40, hy + 60), 0.25, BROWN, 0.35);
        p.part(Pencil.blob(hx - 80, hy - 6, hx - 20, hy - 30, hx + 60, hy - 22, hx + 70, hy + 4, hx - 10, hy + 6, hx - 70, hy + 14), 0.95);   // mask
        p.dot(hx - 30, hy - 10, 9, YELLOW);
        p.dot(hx + 34, hy - 8, 9, YELLOW);
        p.part(Pencil.blob(hx - 100, hy + 10, hx - 60, hy + 20, hx - 50, hy + 50, hx - 90, hy + 44), 0.05);   // white muzzle
        p.dot(hx - 100, hy + 14, 8, Pencil.LEAD);
    }

    static void alphaWolf(Pencil p) {
        double[] b = beast(p, 390, 0.55, null, 0, true);
        p.part(Pencil.blob(120, b[1] - 10, 220, b[1] - 90, 320, b[1] - 40, 340, b[1] + 80, 260, b[1] + 180,
                150, b[1] + 140), 0.7);   // heavy mane
        for (int i = 0; i < 6; i++) {
            double x = 330 + i * 40;
            p.part(Pencil.poly(x - 12, b[1] + 26, x + 6, b[1] - 24 - i % 2 * 16, x + 16, b[1] + 26), 0.75);
        }
        dogHead(p, 200, b[1] + 20, 1.3, 0.6, RED, null);
        p.colorLine(3, RED, 146, b[1] - 20, 166, b[1] + 20);   // battle scar
        p.colorLine(3, RED, 160, b[1] - 24, 180, b[1] + 14);
    }

    // ================= 2 : 어두운 동굴 =================

    static void goblin(Pencil p) {
        double[] f = figure(p, 350, 430, 1.0, 0.55, 0.6, 0.3, false, 262, 520, 456, 470);
        // Rusty dagger held up in the right hand.
        p.part(Pencil.poly(450, 470, 470, 462, 520, 330, 506, 326), 0.15);
        p.part(Pencil.limb(12, 438, 482, 476, 458), 0.8);
        double hx = f[0], hy = f[1] + 6, hr = f[2] * 1.5;
        Shape ear = Pencil.poly(hx - hr * 0.8, hy - 6, hx - hr * 2.0, hy - hr * 0.9, hx - hr * 0.75, hy + hr * 0.35);
        p.part(ear, 0.3, GREEN, 0.7);
        p.part(Pencil.mirror(ear, hx), 0.3, GREEN, 0.7);
        p.part(Pencil.ell(hx, hy, hr, hr * 0.88), 0.25, GREEN, 0.8);
        p.dot(hx - hr * 0.38, hy - hr * 0.12, 10, YELLOW);
        p.dot(hx + hr * 0.38, hy - hr * 0.12, 10, YELLOW);
        p.ink(hx - hr * 0.6, hy - hr * 0.42, hx - hr * 0.15, hy - hr * 0.26, 3);
        p.ink(hx + hr * 0.6, hy - hr * 0.42, hx + hr * 0.15, hy - hr * 0.26, 3);
        p.part(Pencil.poly(hx - 6, hy, hx + 12, hy + hr * 0.3, hx - 14, hy + hr * 0.28), 0.4);
        p.curve(3, hx - hr * 0.45, hy + hr * 0.5, hx, hy + hr * 0.62, hx + hr * 0.45, hy + hr * 0.48);
        p.part(Pencil.poly(hx - hr * 0.3, hy + hr * 0.53, hx - hr * 0.22, hy + hr * 0.72, hx - hr * 0.14, hy + hr * 0.57), 0);
    }

    static void caveSpider(Pencil p) {
        double[][] farLegs = {{300, 520, 210, 400, 120, 676}, {330, 510, 290, 360, 230, 676}};
        double[][] backLegs = {{470, 470, 590, 330, 664, 676}, {450, 480, 520, 360, 560, 676}};
        double[][] nearLegs = {{290, 540, 160, 440, 60, 676}, {320, 548, 250, 450, 180, 676},
                {470, 520, 620, 400, 690, 676}, {440, 540, 470, 430, 440, 676}};
        for (double[] l : farLegs) p.part(Pencil.limb(20, l), 0.85);
        for (double[] l : backLegs) p.part(Pencil.limb(20, l), 0.85);
        p.part(Pencil.ell(470, 450, 165, 130), 0.85);
        p.part(Pencil.blob(470, 370, 500, 430, 470, 520, 440, 430), 0.1, RED, 0.6);   // hourglass mark
        for (double[] l : nearLegs) p.part(Pencil.limb(24, l), 0.75);
        p.part(Pencil.ell(270, 520, 95, 78), 0.7);
        for (int i = 0; i < 4; i++) p.dot(225 + i * 22, 498 - abs(i - 1.5) * 6, 7, RED);
        p.dot(240, 530, 6, RED);
        p.dot(290, 530, 6, RED);
        p.part(Pencil.poly(220, 570, 232, 610, 246, 572), 0.9);
        p.part(Pencil.poly(280, 572, 292, 612, 304, 574), 0.9);
    }

    static void caveBear(Pencil p) {
        double[] b = beast(p, 360, 0.6, BROWN, 0.6, false);
        // Shaggy shoulder hump over the beast body.
        p.part(Pencil.blob(240, 380, 330, 300, 420, 330, 380, 420, 260, 440), 0.65, BROWN, 0.6);
        double hx = b[0] + 10, hy = b[1] + 40;
        p.part(Pencil.ell(hx + 50, hy - 78, 30, 30), 0.65, BROWN, 0.6);
        p.part(Pencil.ell(hx + 128, hy - 64, 28, 28), 0.65, BROWN, 0.6);
        p.part(Pencil.blob(hx + 160, hy - 40, hx + 90, hy - 90, hx + 10, hy - 70, hx - 60, hy - 10, hx - 80, hy + 40,
                hx - 10, hy + 70, hx + 120, hy + 60), 0.55, BROWN, 0.6);
        p.part(Pencil.ell(hx - 58, hy + 30, 46, 36), 0.2);
        p.dot(hx - 92, hy + 18, 12, Pencil.LEAD);
        p.dot(hx + 10, hy - 18, 9, RED);
        p.curve(3, hx - 92, hy + 50, hx - 60, hy + 58, hx - 30, hy + 50);
        for (double fx : new double[]{197, 512}) {
            for (int c = 0; c < 3; c++) p.ink(fx - 30 + c * 12, G - 10, fx - 40 + c * 12, G + 2, 3);
        }
    }

    static void orcChief(Pencil p) {
        // Axe behind the right hand.
        p.part(Pencil.limb(16, 560, 640, 590, 170), 0.6);
        p.part(Pencil.blob(586, 190, 640, 140, 690, 120, 672, 230, 694, 340, 640, 316, 588, 270), 0.7);
        p.ink(600, 196, 600, 262, 3);
        double[] f = figure(p, 360, 560, 1.35, 0.7, 0.5, 0.45, false, 230, 560, 580, 410);
        double hy = f[4] - 40;
        p.part(Pencil.blob(250, f[3] + 10, 470, f[3] + 10, 450, hy, 270, hy), 0.8);   // chest plate
        p.part(Pencil.limb(26, 250, f[4] - 6, 470, f[4] - 6), 0.9);   // belt
        p.part(Pencil.ell(360, f[4] - 6, 22, 20), 0.1, GOLD, 0.8);
        p.part(Pencil.ell(240, f[3] + 20, 62, 44), 0.85);   // shoulder pads
        p.part(Pencil.ell(480, f[3] + 20, 62, 44), 0.85);
        double hx = f[0], hy2 = f[1] + 10, hr = f[2] * 1.05;
        p.part(Pencil.ell(hx, hy2, hr, hr * 0.95), 0.4, GREEN, 0.75);
        p.part(Pencil.blob(hx - hr * 1.05, hy2 - hr * 0.1, hx - hr * 0.8, hy2 - hr * 1.05, hx + hr * 0.8, hy2 - hr * 1.05,
                hx + hr * 1.05, hy2 - hr * 0.1, hx, hy2 - hr * 0.35), 0.85);   // helmet
        Shape horn = Pencil.blob(hx - hr * 0.9, hy2 - hr * 0.55, hx - hr * 1.6, hy2 - hr * 1.0, hx - hr * 1.7,
                hy2 - hr * 1.9, hx - hr * 1.3, hy2 - hr * 1.1, hx - hr * 0.7, hy2 - hr * 0.85);
        p.part(horn, 0.15);
        p.part(Pencil.mirror(horn, hx), 0.15);
        p.dot(hx - hr * 0.38, hy2 + hr * 0.05, 10, RED);
        p.dot(hx + hr * 0.38, hy2 + hr * 0.05, 10, RED);
        p.curve(3.5, hx - hr * 0.5, hy2 + hr * 0.5, hx, hy2 + hr * 0.42, hx + hr * 0.5, hy2 + hr * 0.5);
        p.part(Pencil.poly(hx - hr * 0.42, hy2 + hr * 0.5, hx - hr * 0.36, hy2 + hr * 0.12, hx - hr * 0.26, hy2 + hr * 0.48), 0);
        p.part(Pencil.poly(hx + hr * 0.42, hy2 + hr * 0.5, hx + hr * 0.36, hy2 + hr * 0.12, hx + hr * 0.26, hy2 + hr * 0.48), 0);
    }
}
