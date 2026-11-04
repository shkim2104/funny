import java.awt.*;
import java.awt.geom.Area;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The tutorial's town folk, on 256px icon canvases: a full figure for the town map (feet at the bottom
 * centre, drawn at about 80px there, so bold and simple) and a head-and-shoulders portrait for the
 * dialogue window. images/npc/{key}.png and {key}_face.png.
 */
final class Townsfolk {
    private Townsfolk() {}

    private static final double C = 128;
    private static final Color SKIN = new Color(244, 214, 186), WHITE = new Color(246, 244, 238), GREY = new Color(150, 150, 160),
            DARK = new Color(60, 56, 66), BLUE = new Color(70, 110, 190), NAVY = new Color(48, 60, 120), PINK = new Color(236, 146, 176),
            CREAM = new Color(236, 222, 186), RED = Monsters.RED, GOLD = Monsters.GOLD, BROWN = Monsters.BROWN,
            GREEN = Monsters.GREEN, PURPLE = Monsters.PURPLE, YELLOW = Monsters.YELLOW, ICE = Monsters.ICE;

    /** What a person looks like: skin, clothes, trousers, hair (null = bald). */
    private static final class Look {
        final Color cloth, legs, hair;
        final boolean robe;
        final double size, lift;

        Look(Color cloth, Color legs, Color hair, boolean robe, double size) {
            this(cloth, legs, hair, robe, size, 0);
        }

        /** lift raises the whole figure off the ground (the fairy hovers). */
        Look(Color cloth, Color legs, Color hair, boolean robe, double size, double lift) {
            this.lift = lift;
            this.cloth = cloth;
            this.legs = legs;
            this.hair = hair;
            this.robe = robe;
            this.size = size;
        }
    }

    /** A filled colored shape with graphite shade and a pencil outline. */
    private static void solid(Pencil p, Shape s, Color c, double tone) {
        p.g.setColor(c);
        p.g.fill(s);
        p.shade(s, tone);
        p.outline(s, 3.0);
    }

    /** A bright filled five-point star (Maps.star is a faint map sparkle, too dark here). */
    private static void star(Pencil p, double x, double y, double r, Color c) {
        double[] xy = new double[20];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * 0.45;
            xy[i * 2] = x + Math.cos(a) * rr;
            xy[i * 2 + 1] = y + Math.sin(a) * rr;
        }
        Shape st = Pencil.poly(xy);
        p.g.setColor(c);
        p.g.fill(st);
        p.outline(st, r < 10 ? 1.6 : 2.4);
    }

    /** Light hair (blond, grey) goes muddy under the full graphite shade. */
    private static double hairTone(Color hair) {
        float[] hsb = Color.RGBtoHSB(hair.getRed(), hair.getGreen(), hair.getBlue(), null);
        return hsb[2] > 0.75f ? 0.12 : 0.35;
    }

    // ================= the full figure =================

    /** Head centre and radius of the standing figure, for the extras to work from. */
    private static double headY(Look k) {
        return 246 - k.lift - 174 * k.size;
    }

    private static double headR(Look k) {
        return 34 * k.size;
    }

    private static void figure(Pencil p, Look k) {
        double s = k.size, feet = 246 - k.lift, hy = headY(k), hr = headR(k);
        p.smudge(Pencil.ell(C, 246, 46 * s, 9), Pencil.LEAD, 70);
        double hip = feet - 50 * s, shoulder = hy + hr + 10 * s;
        if (k.robe) {
            solid(p, Pencil.blob(C - 30 * s, shoulder, C - 50 * s, feet - 30 * s, C - 54 * s, feet - 2, C, feet + 2, C + 54 * s, feet - 2,
                    C + 50 * s, feet - 30 * s, C + 30 * s, shoulder, C, shoulder - 6 * s), k.cloth, 0.3);
        } else {
            solid(p, Pencil.limb(17 * s, C - 13 * s, hip, C - 16 * s, feet - 8), k.legs, 0.35);
            solid(p, Pencil.limb(17 * s, C + 13 * s, hip, C + 16 * s, feet - 8), k.legs, 0.35);
            solid(p, Pencil.ell(C - 20 * s, feet - 5, 13 * s, 7 * s), DARK, 0.5);
            solid(p, Pencil.ell(C + 20 * s, feet - 5, 13 * s, 7 * s), DARK, 0.5);
            solid(p, Pencil.blob(C - 38 * s, hip + 6 * s, C - 42 * s, (hip + shoulder) / 2, C - 28 * s, shoulder, C, shoulder - 5 * s,
                    C + 28 * s, shoulder, C + 42 * s, (hip + shoulder) / 2, C + 38 * s, hip + 6 * s, C, hip + 12 * s), k.cloth, 0.3);
        }
        solid(p, Pencil.limb(15 * s, C - 30 * s, shoulder + 6 * s, C - 50 * s, shoulder + 56 * s), k.cloth, 0.35);
        solid(p, Pencil.limb(15 * s, C + 30 * s, shoulder + 6 * s, C + 50 * s, shoulder + 56 * s), k.cloth, 0.35);
        solid(p, Pencil.ell(C - 51 * s, shoulder + 60 * s, 8 * s, 8 * s), SKIN, 0.08);
        solid(p, Pencil.ell(C + 51 * s, shoulder + 60 * s, 8 * s, 8 * s), SKIN, 0.08);
        if (k.hair != null) solid(p, Pencil.ell(C, hy - hr * 0.25, hr * 1.06, hr * 0.85), k.hair, hairTone(k.hair));
        solid(p, Pencil.ell(C, hy, hr, hr), SKIN, 0.08);
        if (k.hair != null) solid(p, Pencil.blob(C - hr * 1.02, hy - hr * 0.1, C - hr * 0.7, hy - hr * 0.95, C, hy - hr * 1.12,
                C + hr * 0.7, hy - hr * 0.95, C + hr * 1.02, hy - hr * 0.1, C, hy - hr * 0.55), k.hair, hairTone(k.hair));
        p.dot(C - hr * 0.33, hy + hr * 0.05, 3.4 * s, Pencil.LEAD);
        p.dot(C + hr * 0.33, hy + hr * 0.05, 3.4 * s, Pencil.LEAD);
        p.curve(2.2, C - hr * 0.3, hy + hr * 0.42, C, hy + hr * 0.58, C + hr * 0.3, hy + hr * 0.42);
        p.smudge(Pencil.ell(C - hr * 0.58, hy + hr * 0.35, hr * 0.16, hr * 0.1), PINK, 120);
        p.smudge(Pencil.ell(C + hr * 0.58, hy + hr * 0.35, hr * 0.16, hr * 0.1), PINK, 120);
    }

    /** Where the right hand (viewer's right) is, for props. */
    private static double handX(Look k) {
        return C + 51 * k.size;
    }

    private static double handY(Look k) {
        return headY(k) + headR(k) + 70 * k.size;
    }

    private static void mayor(Pencil p) {
        Look k = new Look(new Color(120, 92, 150), BROWN, null, false, 1);
        // The stubby pencil walking stick, behind the hand.
        double x = handX(k) + 8, top = handY(k) - 40;
        solid(p, Pencil.poly(x - 7, top + 12, x + 7, top + 12, x + 7, 238, x - 7, 238), YELLOW, 0.1);
        solid(p, Pencil.poly(x - 7, top + 12, x + 7, top + 12, x + 7, top, x - 7, top), PINK, 0.1);
        solid(p, Pencil.poly(x - 7, 238, x + 7, 238, x, 250), CREAM, 0.15);
        figure(p, k);
        double hy = headY(k), hr = headR(k);
        solid(p, Pencil.blob(C - hr * 0.8, hy + hr * 0.3, C - hr * 0.5, hy + hr * 1.5, C, hy + hr * 1.9, C + hr * 0.5, hy + hr * 1.5,
                C + hr * 0.8, hy + hr * 0.3, C, hy + hr * 0.62), WHITE, 0.08);
        solid(p, Pencil.ell(C - hr * 1.0, hy - hr * 0.2, hr * 0.22, hr * 0.3), WHITE, 0.08);
        solid(p, Pencil.ell(C + hr * 1.0, hy - hr * 0.2, hr * 0.22, hr * 0.3), WHITE, 0.08);
        p.outline(Pencil.ell(C - hr * 0.33, hy + hr * 0.05, hr * 0.24, hr * 0.24), 2.2);
        p.outline(Pencil.ell(C + hr * 0.33, hy + hr * 0.05, hr * 0.24, hr * 0.24), 2.2);
        p.ink(C - hr * 0.1, hy + hr * 0.02, C + hr * 0.1, hy + hr * 0.02, 1.6);
    }

    private static void innkeeper(Pencil p) {
        Look k = new Look(new Color(150, 120, 170), GREY, new Color(200, 200, 206), false, 0.95);
        figure(p, k);
        double hy = headY(k), hr = headR(k);
        // Sleepy: eyes as closed arcs over the dots, a droopy nightcap with a pompom.
        p.curve(2.4, C - hr * 0.48, hy + hr * 0.02, C - hr * 0.33, hy + hr * 0.12, C - hr * 0.18, hy + hr * 0.02);
        p.curve(2.4, C + hr * 0.18, hy + hr * 0.02, C + hr * 0.33, hy + hr * 0.12, C + hr * 0.48, hy + hr * 0.02);
        solid(p, Pencil.blob(C - hr * 1.05, hy - hr * 0.45, C - hr * 0.2, hy - hr * 1.5, C + hr * 1.3, hy - hr * 1.2, C + hr * 1.6, hy - hr * 0.2,
                C + hr * 1.05, hy - hr * 0.5, C, hy - hr * 0.75), BLUE, 0.25);
        solid(p, Pencil.ell(C + hr * 1.6, hy - hr * 0.1, hr * 0.25, hr * 0.25), WHITE, 0.05);
        p.text("z", C + hr * 1.4, hy - hr * 1.3, 26, BLUE, 10);
        solid(p, Pencil.blob(handX(k) - 20, handY(k) - 4, handX(k) + 12, handY(k) - 4, handX(k) + 10, handY(k) + 14, handX(k) - 18, handY(k) + 14),
                WHITE, 0.1);
        p.ink(handX(k) + 10, handY(k) + 2, handX(k) + 18, handY(k) + 6, 2);
    }

    private static void merchant(Pencil p) {
        Look k = new Look(new Color(214, 120, 90), BROWN, new Color(120, 76, 44), false, 1);
        figure(p, k);
        double hy = headY(k), hr = headR(k);
        solid(p, Pencil.ell(C, hy - hr * 1.15, hr * 0.42, hr * 0.34), new Color(120, 76, 44), 0.35);
        double sh = hy + hr + 10;
        solid(p, Pencil.blob(C - 28, sh + 18, C + 28, sh + 18, C + 32, sh + 92, C, sh + 98, C - 32, sh + 92), WHITE, 0.05);
        p.ink(C - 30, sh + 20, C - 24, sh + 4, 1.6);
        p.ink(C + 30, sh + 20, C + 24, sh + 4, 1.6);
        solid(p, Pencil.blob(C + 30, sh + 60, C + 52, sh + 58, C + 54, sh + 82, C + 42, sh + 88, C + 30, sh + 82), GOLD, 0.15);
        p.text("G", C + 35, sh + 80, 20, BROWN, 0);
    }

    private static void smith(Pencil p) {
        Look k = new Look(new Color(110, 100, 96), new Color(80, 74, 70), DARK, false, 1.08);
        figure(p, k);
        double hy = headY(k), hr = headR(k), sh = hy + hr + 11;
        solid(p, Pencil.blob(C - 32, sh + 16, C + 32, sh + 16, C + 36, sh + 100, C, sh + 108, C - 36, sh + 100), BROWN, 0.35);
        // The hammer, held up in front of the hand.
        double hx = handX(k), hyy = handY(k);
        solid(p, Pencil.limb(8, hx, hyy + 4, hx + 22, hyy - 56), BROWN, 0.3);
        solid(p, Pencil.poly(hx + 4, hyy - 74, hx + 44, hyy - 60, hx + 36, hyy - 40, hx - 4, hyy - 54), GREY, 0.55);
        solid(p, Pencil.ell(hx, hyy + 2, 9, 9), SKIN, 0.08);
        p.smudge(Pencil.ell(C - hr * 0.55, hy + hr * 0.4, hr * 0.22, hr * 0.12), Pencil.LEAD, 110);
        p.smudge(Pencil.ell(C + hr * 0.5, hy - hr * 0.35, hr * 0.18, hr * 0.1), Pencil.LEAD, 90);
        p.ink(C - hr * 0.5, hy - hr * 0.3, C - hr * 0.15, hy - hr * 0.2, 2.6);
        p.ink(C + hr * 0.15, hy - hr * 0.2, C + hr * 0.5, hy - hr * 0.3, 2.6);
    }

    private static void questClerk(Pencil p) {
        Look k = new Look(new Color(90, 140, 110), NAVY, new Color(70, 50, 40), false, 0.95);
        figure(p, k);
        double hy = headY(k), hr = headR(k);
        solid(p, Pencil.blob(C - hr * 1.05, hy - hr * 0.45, C, hy - hr * 1.15, C + hr * 1.05, hy - hr * 0.45, C + hr * 1.5, hy - hr * 0.35,
                C, hy - hr * 0.5), NAVY, 0.3);
        double hx = handX(k), hyy = handY(k);
        solid(p, Pencil.poly(hx - 26, hyy - 30, hx + 8, hyy - 30, hx + 8, hyy + 14, hx - 26, hyy + 14), CREAM, 0.1);
        for (int i = 0; i < 3; i++) p.ink(hx - 20, hyy - 20 + i * 10, hx + 2, hyy - 20 + i * 10, 1.4);
        solid(p, Pencil.poly(hx - 4, hyy - 32, hx + 4, hyy - 32, hx + 4, hyy - 2, hx, hyy - 8, hx - 4, hyy - 2), RED, 0.1);
    }

    private static void gachaFairy(Pencil p) {
        Look k = new Look(PINK, PINK, YELLOW, true, 0.72, 30);
        double hy = headY(k);
        solid(p, Pencil.ell(C - 44, hy + 64, 34, 52), new Color(200, 230, 246), 0.05);
        solid(p, Pencil.ell(C + 44, hy + 64, 34, 52), new Color(200, 230, 246), 0.05);
        figure(p, k);
        double hx = handX(k), hyy = handY(k);
        p.ink(hx, hyy, hx + 18, hyy - 52, 2.6);
        star(p, hx + 20, hyy - 60, 16, YELLOW);
        for (double[] s : new double[][]{{50, 60}, {200, 40}, {70, 180}, {196, 150}}) star(p, s[0], s[1], 7, GOLD);
    }

    private static void jobMaster(Pencil p) {
        Look k = new Look(NAVY, NAVY, DARK, true, 1.05);
        // The great brush, standing beside the hand.
        double hx = handX(k) + 6;
        solid(p, Pencil.limb(9, hx, 60, hx, 230), BROWN, 0.3);
        solid(p, Pencil.flame(hx, 246, 22, 30, 0), DARK, 0.7);
        figure(p, k);
        double hy = headY(k), hr = headR(k);
        solid(p, Pencil.blob(C - hr * 0.5, hy + hr * 0.4, C - hr * 0.2, hy + hr * 1.7, C, hy + hr * 2.1, C + hr * 0.2, hy + hr * 1.7,
                C + hr * 0.5, hy + hr * 0.4, C, hy + hr * 0.62), GREY, 0.2);
        solid(p, Pencil.ell(C, hy - hr * 1.15, hr * 0.32, hr * 0.28), DARK, 0.6);
        p.ink(C - hr * 0.5, hy - hr * 0.25, C - hr * 0.15, hy - hr * 0.15, 2.6);
        p.ink(C + hr * 0.15, hy - hr * 0.15, C + hr * 0.5, hy - hr * 0.25, 2.6);
        solid(p, Pencil.poly(C - 8, hy + hr + 30, C + 8, hy + hr + 30, C + 8, 236, C - 8, 236), GOLD, 0.15);
    }

    private static void curator(Pencil p) {
        Look k = new Look(BROWN, DARK, new Color(190, 190, 196), false, 1);
        solid(p, Pencil.poly(C - 90, 246, C - 40, 246, C - 40, 228, C - 90, 228), RED, 0.25);
        solid(p, Pencil.poly(C - 86, 228, C - 44, 228, C - 44, 212, C - 86, 212), BLUE, 0.25);
        solid(p, Pencil.poly(C - 92, 212, C - 48, 212, C - 48, 198, C - 92, 198), GREEN, 0.25);
        figure(p, k);
        double hy = headY(k), hr = headR(k);
        for (int i = 0; i < 5; i++) solid(p, Pencil.ell(C - hr + i * hr * 0.5, hy - hr * 1.0 - (i % 2) * 6, hr * 0.28, hr * 0.22), new Color(190, 190, 196), 0.3);
        double hx = handX(k), hyy = handY(k);
        p.ink(hx, hyy, hx + 14, hyy - 24, 3);
        Area lens = new Area(Pencil.ell(hx + 22, hyy - 38, 17, 17));
        lens.subtract(new Area(Pencil.ell(hx + 22, hyy - 38, 12, 12)));
        solid(p, lens, GOLD, 0.2);
        p.smudge(Pencil.ell(hx + 22, hyy - 38, 12, 12), ICE, 70);
        for (double[] d : new double[][]{{40, 50}, {210, 70}, {60, 120}, {220, 140}, {30, 170}}) p.dot(d[0], d[1], 2.4, GREY);
    }

    private static void gatekeeper(Pencil p) {
        Look k = new Look(new Color(140, 146, 160), DARK, null, false, 1.05);
        double hx = handX(k);
        solid(p, Pencil.limb(7, hx + 4, 20, hx + 4, 244), BROWN, 0.3);
        solid(p, Pencil.poly(hx - 6, 30, hx + 14, 30, hx + 4, 4), GREY, 0.5);
        figure(p, k);
        double hy = headY(k), hr = headR(k);
        Area helmet = new Area(Pencil.ell(C, hy - hr * 0.15, hr * 1.12, hr * 1.02));
        helmet.subtract(new Area(Pencil.poly(0, hy - hr * 0.2, 256, hy - hr * 0.2, 256, 256, 0, 256)));
        solid(p, helmet, GREY, 0.5);
        solid(p, Pencil.blob(C - 6, hy - hr * 1.1, C, hy - hr * 1.7, C + 18, hy - hr * 1.6, C + 8, hy - hr * 1.0), RED, 0.2);
        double sx = C - 54, sy = handY(k) - 20;
        solid(p, Pencil.ell(sx, sy, 30, 36), BLUE, 0.35);
        star(p, sx, sy, 12, GOLD);
    }

    // ================= the portraits =================

    /** Head and shoulders: shoulders in the cloth color, a big head, the face. */
    private static void face(Pencil p, Color cloth, Color hair, boolean bald) {
        solid(p, Pencil.blob(-10, 270, 10, 212, 70, 188, C, 182, 186, 188, 246, 212, 266, 270), cloth, 0.3);
        solid(p, Pencil.poly(C - 22, 160, C + 22, 160, C + 20, 196, C - 20, 196), SKIN, 0.1);
        if (hair != null && !bald) solid(p, Pencil.ell(C, 100, 82, 66), hair, hairTone(hair));
        solid(p, Pencil.ell(C, 118, 70, 72), SKIN, 0.08);
        if (hair != null && !bald) solid(p, Pencil.blob(C - 72, 110, C - 50, 52, C, 40, C + 50, 52, C + 72, 110, C, 72), hair, hairTone(hair));
        p.dot(C - 24, 118, 7, Pencil.LEAD);
        p.dot(C + 24, 118, 7, Pencil.LEAD);
        p.dot(C - 22, 115, 2.2, WHITE);
        p.dot(C + 26, 115, 2.2, WHITE);
        p.curve(3, C - 20, 150, C, 162, C + 20, 150);
        p.smudge(Pencil.ell(C - 42, 140, 12, 8), PINK, 120);
        p.smudge(Pencil.ell(C + 42, 140, 12, 8), PINK, 120);
    }

    private static void mayorFace(Pencil p) {
        face(p, new Color(120, 92, 150), null, true);
        solid(p, Pencil.ell(C - 72, 104, 16, 22), WHITE, 0.08);
        solid(p, Pencil.ell(C + 72, 104, 16, 22), WHITE, 0.08);
        solid(p, Pencil.blob(C - 56, 140, C - 36, 214, C, 244, C + 36, 214, C + 56, 140, C, 164), WHITE, 0.08);
        p.curve(3, C - 18, 150, C, 158, C + 18, 150);
        p.outline(Pencil.ell(C - 24, 118, 17, 17), 2.6);
        p.outline(Pencil.ell(C + 24, 118, 17, 17), 2.6);
        p.ink(C - 7, 116, C + 7, 116, 2);
        p.ink(C - 40, 92, C - 14, 96, 3);
        p.ink(C + 14, 96, C + 40, 92, 3);
    }

    private static void innkeeperFace(Pencil p) {
        face(p, new Color(150, 120, 170), new Color(200, 200, 206), false);
        solid(p, Pencil.ell(C - 24, 117, 10, 8), SKIN, 0.05);
        solid(p, Pencil.ell(C + 24, 117, 10, 8), SKIN, 0.05);
        p.curve(3, C - 34, 116, C - 24, 124, C - 14, 116);
        p.curve(3, C + 14, 116, C + 24, 124, C + 34, 116);
        solid(p, Pencil.blob(C - 80, 84, C - 20, 6, C + 80, 20, C + 110, 92, C + 78, 80, C, 56), BLUE, 0.25);
        solid(p, Pencil.ell(C + 112, 98, 16, 16), WHITE, 0.05);
        p.text("z z", C + 66, 40, 30, BLUE, 10);
    }

    private static void merchantFace(Pencil p) {
        face(p, new Color(214, 120, 90), new Color(120, 76, 44), false);
        solid(p, Pencil.ell(C, 34, 30, 24), new Color(120, 76, 44), 0.35);
        solid(p, Pencil.blob(C - 40, 200, C + 40, 200, C + 44, 270, C - 44, 270), WHITE, 0.05);
        p.curve(3, C - 24, 148, C, 166, C + 24, 148);
    }

    private static void smithFace(Pencil p) {
        face(p, new Color(110, 100, 96), DARK, false);
        solid(p, Pencil.blob(C - 46, 196, C + 46, 196, C + 54, 270, C - 54, 270), BROWN, 0.35);
        p.smudge(Pencil.ell(C - 40, 142, 16, 9), Pencil.LEAD, 120);
        p.smudge(Pencil.ell(C + 34, 92, 12, 7), Pencil.LEAD, 100);
        p.ink(C - 44, 98, C - 12, 104, 4);
        p.ink(C + 12, 104, C + 44, 98, 4);
        solid(p, Pencil.blob(C - 34, 150, C - 10, 140, C, 146, C + 10, 140, C + 34, 150, C, 156), DARK, 0.6);
    }

    private static void questClerkFace(Pencil p) {
        face(p, new Color(90, 140, 110), new Color(70, 50, 40), false);
        solid(p, Pencil.blob(C - 76, 86, C, 26, C + 76, 86, C + 116, 92, C, 78), NAVY, 0.3);
        solid(p, Pencil.poly(C + 66, 190, C + 80, 190, C + 80, 250, C + 73, 240, C + 66, 250), RED, 0.1);
    }

    private static void gachaFairyFace(Pencil p) {
        solid(p, Pencil.ell(C - 90, 150, 40, 70), new Color(200, 230, 246), 0.05);
        solid(p, Pencil.ell(C + 90, 150, 40, 70), new Color(200, 230, 246), 0.05);
        face(p, PINK, YELLOW, false);
        star(p, C + 70, 50, 18, YELLOW);
        for (double[] s : new double[][]{{40, 40}, {220, 120}, {30, 200}}) star(p, s[0], s[1], 9, GOLD);
    }

    private static void jobMasterFace(Pencil p) {
        face(p, NAVY, DARK, false);
        solid(p, Pencil.blob(C - 36, 150, C - 20, 224, C, 252, C + 20, 224, C + 36, 150, C, 166), GREY, 0.2);
        p.curve(3, C - 16, 152, C, 158, C + 16, 152);
        solid(p, Pencil.ell(C, 30, 22, 18), DARK, 0.6);
        p.ink(C - 44, 98, C - 12, 104, 4);
        p.ink(C + 12, 104, C + 44, 98, 4);
    }

    private static void curatorFace(Pencil p) {
        face(p, BROWN, new Color(190, 190, 196), false);
        for (int i = 0; i < 6; i++) solid(p, Pencil.ell(C - 70 + i * 28, 46 + (i % 2) * 10, 20, 16), new Color(190, 190, 196), 0.3);
        Area lens = new Area(Pencil.ell(C + 24, 118, 26, 26));
        lens.subtract(new Area(Pencil.ell(C + 24, 118, 19, 19)));
        solid(p, lens, GOLD, 0.2);
        p.smudge(Pencil.ell(C + 24, 118, 19, 19), ICE, 60);
        p.ink(C + 44, 138, C + 70, 176, 5);
        for (double[] d : new double[][]{{40, 70}, {210, 60}, {30, 150}, {226, 170}}) p.dot(d[0], d[1], 3, GREY);
    }

    private static void gatekeeperFace(Pencil p) {
        face(p, new Color(140, 146, 160), null, true);
        Area helmet = new Area(Pencil.ell(C, 104, 82, 80));
        helmet.subtract(new Area(Pencil.poly(0, 96, 256, 96, 256, 256, 0, 256)));
        solid(p, helmet, GREY, 0.5);
        solid(p, Pencil.blob(C - 10, 28, C, -10, C + 40, 0, C + 14, 34), RED, 0.2);
        p.ink(C - 44, 104, C - 12, 108, 4);
        p.ink(C + 12, 108, C + 44, 104, 4);
    }

    static Map<String, Monsters.Drawing> all() {
        Map<String, Monsters.Drawing> m = new LinkedHashMap<>();
        m.put("mayor", Townsfolk::mayor);
        m.put("mayor_face", Townsfolk::mayorFace);
        m.put("innkeeper", Townsfolk::innkeeper);
        m.put("innkeeper_face", Townsfolk::innkeeperFace);
        m.put("merchant", Townsfolk::merchant);
        m.put("merchant_face", Townsfolk::merchantFace);
        m.put("smith", Townsfolk::smith);
        m.put("smith_face", Townsfolk::smithFace);
        m.put("quest_clerk", Townsfolk::questClerk);
        m.put("quest_clerk_face", Townsfolk::questClerkFace);
        m.put("gacha_fairy", Townsfolk::gachaFairy);
        m.put("gacha_fairy_face", Townsfolk::gachaFairyFace);
        m.put("job_master", Townsfolk::jobMaster);
        m.put("job_master_face", Townsfolk::jobMasterFace);
        m.put("curator", Townsfolk::curator);
        m.put("curator_face", Townsfolk::curatorFace);
        m.put("gatekeeper", Townsfolk::gatekeeper);
        m.put("gatekeeper_face", Townsfolk::gatekeeperFace);
        return m;
    }
}
