import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.*;

/**
 * Pencil icons on a 256x256 transparent canvas: weapons and armor (images/items/*.png, file names match
 * ItemCatalog's icon ids), potions, and the augment category icons (images/augments/*.png).
 * Weapons are drawn upright and turned 45 degrees, tip to the top-right, like inventory icons.
 */
final class Icons {
    static final int SIZE = 256;
    private static final double C = SIZE / 2.0;
    private static final Color RED = Monsters.RED, ORANGE = Monsters.ORANGE, ICE = Monsters.ICE, GOLD = Monsters.GOLD,
            PURPLE = Monsters.PURPLE, GREEN = Monsters.GREEN, YELLOW = Monsters.YELLOW, BROWN = Monsters.BROWN;

    private Icons() {}

    /** Decorations shared by weapons and armor. */
    private static final int FLAME = 1, CRACKS = 2, WINGS = 4, SPIKES = 8, GLOW = 16, BONES = 32, CORE = 64, CHIPS = 128;

    // ================= weapons =================

    private static final AffineTransform TILT = AffineTransform.getRotateInstance(toRadians(45), C, C);

    private static Shape t(Shape s) {
        return TILT.createTransformedShape(s);
    }

    private static double[] tp(double x, double y) {
        double[] pt = {x, y};
        TILT.transform(pt, 0, pt, 0, 1);
        return pt;
    }

    /** A sword standing up (tip at the top): blade width w, blade from y=top to the guard at y=guardY. */
    private static void sword(Pencil p, double w, double top, double guardY, double guardW, double bladeTone,
                              Color accent, Color guardColor, Color gem, int deco) {
        if ((deco & GLOW) != 0) p.smudge(t(Pencil.ell(C, (top + guardY) / 2, w * 1.6, (guardY - top) * 0.7)), accent, 45);
        Shape blade = Pencil.poly(C - w / 2, guardY, C - w / 2, top + w * 1.2, C, top, C + w / 2, top + w * 1.2, C + w / 2, guardY);
        if ((deco & CHIPS) != 0) {
            Area a = new Area(blade);
            for (int i = 0; i < 3; i++) a.subtract(new Area(Pencil.ell(C + (i % 2 == 0 ? -w / 2 : w / 2), top + 50 + i * 34, 5, 6)));
            blade = a;
        }
        p.part(t(blade), bladeTone, accent, accent == null ? 0 : 0.6);
        double[] a0 = tp(C, guardY - 6), a1 = tp(C, top + w * 1.4);
        p.ink(a0[0], a0[1], a1[0], a1[1], 1.6);   // fuller
        if ((deco & FLAME) != 0) {
            for (int i = 0; i < 3; i++) p.part(t(Pencil.flame(C + w / 2 + 4, guardY - 30 - i * 40, 18, 34, 10)), 0.03, ORANGE, 1.0);
        }
        if ((deco & CRACKS) != 0) {
            double[] c0 = tp(C - 4, guardY - 20), c1 = tp(C + 5, guardY - 60), c2 = tp(C - 3, guardY - 100), c3 = tp(C + 4, top + 40);
            p.colorLine(3, ORANGE, c0[0], c0[1], c1[0], c1[1], c2[0], c2[1], c3[0], c3[1]);
        }
        p.part(t(Pencil.blob(C - guardW / 2, guardY + 4, C - guardW * 0.3, guardY - 8, C + guardW * 0.3, guardY - 8,
                C + guardW / 2, guardY + 4, C + guardW * 0.3, guardY + 14, C - guardW * 0.3, guardY + 14)), 0.4,
                guardColor, guardColor == null ? 0 : 0.8);
        if ((deco & WINGS) != 0) {
            Shape wing = Pencil.blob(C - guardW / 2, guardY, C - guardW * 0.9, guardY - 40, C - guardW * 0.7, guardY - 10);
            p.part(t(wing), 0.05, GOLD, 0.5);
            p.part(t(Pencil.mirror(wing, C)), 0.05, GOLD, 0.5);
        }
        p.part(t(Pencil.limb(14, C, guardY + 14, C, 226)), 0.7, BROWN, 0.6);
        p.part(t(Pencil.ell(C, 234, 12, 12)), 0.3, gem != null ? gem : GOLD, 0.9);
    }

    private static void dagger(Pencil p, double cx, double tilt, double tone, Color accent, int deco, boolean fang) {
        AffineTransform r = AffineTransform.getRotateInstance(toRadians(tilt), cx, C);
        Shape blade = fang
                ? Pencil.blob(cx - 14, 170, cx - 16, 110, cx - 4, 60, cx + 10, 40, cx + 6, 90, cx + 14, 170)
                : Pencil.poly(cx - 13, 170, cx - 13, 80, cx, 52, cx + 13, 80, cx + 13, 170);
        p.part(r.createTransformedShape(blade), tone, accent, accent == null ? 0 : 0.7);
        if ((deco & FLAME) != 0) p.part(r.createTransformedShape(Pencil.flame(cx + 18, 120, 16, 40, 8)), 0.03, ORANGE, 1.0);
        p.part(r.createTransformedShape(Pencil.limb(12, cx - 30, 176, cx + 30, 176)), 0.6);
        p.part(r.createTransformedShape(Pencil.limb(13, cx, 184, cx, 222)), 0.75, BROWN, 0.6);
        p.part(r.createTransformedShape(Pencil.ell(cx, 228, 10, 10)), 0.3, accent != null ? accent : GOLD, 0.8);
    }

    private static void axe(Pencil p) {
        p.part(t(Pencil.limb(16, C, 40, C, 236)), 0.6, BROWN, 0.7);
        p.part(t(Pencil.blob(C + 6, 52, C + 60, 24, C + 96, 40, C + 80, 96, C + 100, 150, C + 50, 130, C + 6, 112)), 0.65);
        p.part(t(Pencil.poly(C - 6, 66, C - 54, 80, C - 6, 100)), 0.7);
        double[] e0 = tp(C + 70, 36), e1 = tp(C + 94, 148);
        p.curve(2.5, e0[0], e0[1], tp(C + 82, 92)[0], tp(C + 82, 92)[1], e1[0], e1[1]);
        p.part(t(Pencil.limb(20, C, 150, C, 176)), 0.85);   // wrap
    }

    private static void scythe(Pencil p) {
        p.part(t(Pencil.limb(14, C, 30, C + 6, 238)), 0.65, BROWN, 0.6);
        p.smudge(t(Pencil.ell(C - 40, 60, 70, 40)), PURPLE, 40);
        p.part(t(Pencil.blob(C + 4, 32, C - 40, 20, C - 100, 40, C - 120, 90, C - 70, 60, C - 20, 56, C + 6, 60)), 0.25, PURPLE, 0.8);
        p.part(t(Pencil.ell(C + 2, 46, 14, 14)), 0.9);
        p.dot(tp(C + 2, 46)[0], tp(C + 2, 46)[1], 5, PURPLE);
    }

    private static void staff(Pencil p, Color gem) {
        p.part(t(Pencil.limb(14, C, 70, C - 4, 240)), 0.6, BROWN, 0.7);
        p.smudge(t(Pencil.ell(C, 52, 52, 52)), gem, 45);
        p.part(t(Pencil.blob(C - 26, 80, C - 32, 50, C - 12, 30, C + 12, 30, C + 32, 50, C + 26, 80)), 0.6, BROWN, 0.5);
        p.part(t(Pencil.poly(C, 8, C + 20, 46, C, 84, C - 20, 46)), 0.05, gem, 1.0);
        double[] a = tp(C, 8), b = tp(C, 84);
        p.ink(a[0], a[1], b[0], b[1], 1.5);
    }

    private static void spear(Pencil p, Color accent, int deco) {
        p.part(t(Pencil.limb(12, C, 70, C, 244)), 0.5, accent, 0.4);
        p.part(t(Pencil.blob(C, 6, C + 18, 50, C + 8, 80, C - 8, 80, C - 18, 50)), 0.06, accent, 0.9);
        p.part(t(Pencil.limb(14, C - 26, 86, C + 26, 86)), 0.6);
        p.part(t(Pencil.poly(C + 4, 92, C + 44, 106, C + 20, 116, C + 40, 132, C + 4, 122)), 0.1, accent, 0.9);
        if ((deco & GLOW) != 0) {
            double[] a = tp(C - 30, 20), b = tp(C - 16, 44), c = tp(C - 34, 54), d = tp(C - 18, 80);
            p.colorLine(3, YELLOW, a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1]);
        }
    }

    private static void bow(Pencil p, Color accent) {
        p.outlineOpen(Pencil.spline(170, 20, 110, 60, 80, 128, 110, 196, 170, 236), 9);
        p.outlineOpen(Pencil.spline(170, 20, 110, 60, 80, 128, 110, 196, 170, 236), 4);
        p.colorLine(3, accent, 120, 52, 92, 128, 120, 204);
        p.ink(170, 20, 170, 236, 1.6);
        p.ink(40, 128, 210, 128, 2.4);
        p.part(Pencil.poly(210, 120, 236, 128, 210, 136), 0.2, accent, 0.9);
        p.part(Pencil.poly(40, 128, 26, 116, 30, 128, 26, 140), 0.1);
        p.part(Pencil.ell(84, 128, 12, 20), 0.6, BROWN, 0.6);
    }

    private static void hammer(Pencil p, Color accent) {
        p.part(t(Pencil.limb(16, C, 80, C, 238)), 0.6, BROWN, 0.7);
        p.part(t(Pencil.poly(C - 70, 30, C + 70, 30, C + 76, 96, C - 76, 96)), 0.55, accent, 0.4);
        p.part(t(Pencil.poly(C - 70, 40, C - 90, 46, C - 90, 80, C - 70, 86)), 0.7);
        p.part(t(Pencil.poly(C + 70, 40, C + 90, 46, C + 90, 80, C + 70, 86)), 0.7);
        double[] a = tp(C - 40, 20), b = tp(C - 20, 50), c = tp(C - 44, 60), d = tp(C - 22, 100);
        p.colorLine(3.5, YELLOW, a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1]);
    }

    // ================= armor =================

    private static final int PLATE = 0, LEATHER = 1, SCALE = 2, CLOAK = 3, ROBE = 4;

    private static void armor(Pencil p, int kind, double tone, Color accent, Color trim, int deco) {
        if ((deco & WINGS) != 0) {
            for (int side = -1; side <= 1; side += 2) {
                Shape wing = Pencil.blob(C + side * 50, 90, C + side * 110, 30, C + side * 126, 80, C + side * 118, 150, C + side * 70, 160);
                p.part(wing, 0.7);
                for (int i = 0; i < 3; i++) p.ink(C + side * 60, 100 + i * 20, C + side * (112 - i * 6), 60 + i * 34, 1.6);
            }
        }
        if (kind == CLOAK) {
            p.part(Pencil.blob(C - 40, 40, C + 40, 40, C + 90, 150, C + 110, 238, C, 230, C - 110, 238, C - 90, 150), tone, accent, 0.7);
            p.part(Pencil.blob(C - 46, 70, C - 40, 26, C, 12, C + 40, 26, C + 46, 70, C, 84), min(1, tone + 0.15), accent, 0.7);
            p.part(Pencil.ell(C, 76, 34, 22), 0.95);
            for (int i = -2; i <= 2; i++) p.curve(1.8, C + i * 26, 110, C + i * 34, 170, C + i * 40, 226);
            p.dot(C, 96, 9, trim != null ? trim : GOLD);
            if ((deco & FLAME) != 0) for (int i = 0; i < 5; i++) p.part(Pencil.flame(C - 90 + i * 45, 238, 34, 50, 0), 0.03, ORANGE, 1.0);
            return;
        }
        if (kind == ROBE) {
            p.part(Pencil.limb(40, C - 60, 70, C - 100, 160, C - 96, 200), tone, accent, 0.6);
            p.part(Pencil.limb(40, C + 60, 70, C + 100, 160, C + 96, 200), tone, accent, 0.6);
            p.part(Pencil.blob(C - 62, 50, C, 40, C + 62, 50, C + 76, 160, C + 92, 240, C, 236, C - 92, 240, C - 76, 160), tone, accent, 0.7);
            p.part(Pencil.poly(C - 26, 46, C, 92, C + 26, 46), 0.9);
            p.part(Pencil.limb(18, C - 64, 130, C + 64, 130), 0.2, trim != null ? trim : GOLD, 0.8);
            p.ink(C, 140, C, 230, 2);
            if ((deco & BONES) != 0) for (int i = 0; i < 3; i++) p.curve(2.2, C - 30, 160 + i * 20, C, 154 + i * 20, C + 30, 160 + i * 20);
            return;
        }
        Shape body = Pencil.poly(64, 54, 100, 42, C, 66, 156, 42, 192, 54, 228, 92, 198, 122, 190, 226, 66, 226, 58, 122, 28, 92);
        p.part(body, tone, accent, accent == null ? 0 : 0.6);
        if (kind == SCALE) {
            for (int row = 0; row < 6; row++) {
                for (int i = 0; i < 6; i++) {
                    double x = 74 + i * 22 + (row % 2) * 11, y = 92 + row * 22;
                    p.curve(1.6, x - 10, y, x, y + 10, x + 10, y);
                }
            }
        }
        if (kind == LEATHER) {
            for (int y = 80; y < 220; y += 14) p.ink(C - 3, y, C + 3, y + 6, 1.4);
            p.part(Pencil.limb(14, 70, 196, 186, 196), 0.6, BROWN, 0.7);
        } else {
            p.ink(C, 72, C, 220, 2);
            p.curve(1.8, 72, 150, C, 162, 184, 150);
            p.part(Pencil.ell(50, 88, 34, 26), min(1, tone + 0.1), trim, trim == null ? 0 : 0.6);
            p.part(Pencil.ell(206, 88, 34, 26), min(1, tone + 0.1), trim, trim == null ? 0 : 0.6);
            p.part(Pencil.limb(16, 70, 196, 186, 196), 0.75, trim, trim == null ? 0 : 0.7);
        }
        if ((deco & SPIKES) != 0) {
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 3; i++) {
                    double x = C + side * (64 + i * 16);
                    p.part(Pencil.poly(x - 7, 74, x + side * 4, 40 - i * 4, x + 7, 74), 0.85);
                }
            }
        }
        if ((deco & BONES) != 0) for (int i = 0; i < 4; i++) p.curve(2.4, 86, 96 + i * 22, C, 88 + i * 22, 170, 96 + i * 22);
        if ((deco & CORE) != 0) {
            p.smudge(Pencil.ell(C, 120, 40, 40), ORANGE, 60);
            p.dot(C, 120, 16, ORANGE);
        }
        if ((deco & CRACKS) != 0) {
            p.colorLine(3, ORANGE, 80, 110, 100, 140, 88, 170, 108, 204);
            p.colorLine(3, ORANGE, 176, 104, 160, 140, 172, 180);
        }
        if ((deco & GLOW) != 0) p.smudge(Pencil.ell(C, 130, 110, 100), accent != null ? accent : GOLD, 30);
    }

    // ================= potions =================

    private static void potion(Pencil p, Color liquid, double size, boolean star) {
        double r = 70 * size;
        p.part(Pencil.ell(C, 160, r, r), 0.05);
        p.smudge(Pencil.ell(C, 172, r * 0.9, r * 0.8), liquid, 140);
        p.tint(Pencil.ell(C, 172, r * 0.9, r * 0.8), liquid, 1.0);
        p.outline(Pencil.ell(C, 160, r, r), 3);
        p.part(Pencil.poly(C - 18, 160 - r + 6, C - 18, 160 - r - 40, C + 18, 160 - r - 40, C + 18, 160 - r + 6), 0.05);
        p.part(Pencil.ell(C, 160 - r - 46, 22, 10), 0.6, BROWN, 0.8);
        p.curve(2.2, C - r * 0.55, 140, C - r * 0.4, 118, C - r * 0.15, 108);
        if (star) p.part(Pencil.poly(C, 140, C + 8, 158, C + 26, 160, C + 12, 172, C + 16, 190, C, 180, C - 16, 190, C - 12, 172,
                C - 26, 160, C - 8, 158), 0.02, YELLOW, 1.0);
    }

    // ================= town icons: treasure chests, quest scroll, inn bed =================

    private static void chest(Pencil p, double tone, Color wood, Color band, boolean sparkle) {
        if (sparkle) p.smudge(Pencil.ell(C, 140, 120, 100), GOLD, 50);
        p.part(Pencil.poly(36, 130, 220, 130, 214, 226, 42, 226), tone, wood, 0.8);   // body
        p.part(Pencil.blob(36, 132, 50, 86, 128, 62, 206, 86, 220, 132), tone + 0.1, wood, 0.8);   // lid
        for (double x : new double[]{78, 178}) p.part(Pencil.limb(18, x, 72, x, 224), 0.2, band, 0.9);
        p.part(Pencil.limb(14, 38, 132, 218, 132), 0.2, band, 0.9);
        p.part(Pencil.blob(112, 120, 144, 120, 148, 158, 128, 170, 108, 158), 0.15, GOLD, 1.0);   // lock plate
        p.dot(128, 146, 6, Pencil.LEAD);
        if (sparkle) {
            for (double[] s : new double[][]{{40, 50}, {214, 40}, {230, 110}}) {
                p.colorLine(3, YELLOW, s[0] - 12, s[1], s[0] + 12, s[1]);
                p.colorLine(3, YELLOW, s[0], s[1] - 12, s[0], s[1] + 12);
            }
        }
    }

    private static void scroll(Pencil p) {
        p.part(Pencil.poly(58, 50, 198, 50, 198, 210, 58, 210), 0.05, new Color(214, 184, 120), 0.5);
        p.part(Pencil.ell(128, 48, 82, 16), 0.3, BROWN, 0.6);
        p.part(Pencil.ell(128, 212, 82, 16), 0.3, BROWN, 0.6);
        for (int i = 0; i < 5; i++) p.ink(78, 84 + i * 22, 178 - (i % 2) * 30, 84 + i * 22, 2.2);
        p.part(Pencil.ell(170, 186, 18, 18), 0.2, RED, 1.0);   // wax seal
        p.part(Pencil.limb(8, 166, 202, 156, 236), 0.2, RED, 0.9);
    }

    private static void bed(Pencil p) {
        p.part(Pencil.poly(30, 100, 60, 100, 60, 220, 30, 220), 0.55, BROWN, 0.7);   // headboard
        p.part(Pencil.poly(40, 170, 226, 170, 226, 210, 40, 210), 0.3, BROWN, 0.6);   // frame
        p.part(Pencil.blob(56, 170, 60, 150, 216, 146, 222, 170), 0.04);   // mattress
        p.part(Pencil.ell(88, 144, 30, 16), 0.03);   // pillow
        p.part(Pencil.blob(110, 172, 116, 134, 222, 132, 226, 172), 0.15, ICE, 0.9);   // blanket
        p.part(Pencil.limb(10, 220, 210, 220, 230), 0.6);
        Area moon = new Area(Pencil.ell(186, 62, 34, 34));
        moon.subtract(new Area(Pencil.ell(202, 52, 30, 30)));
        p.part(moon, 0.0, YELLOW, 1.0);
        p.text("z", 110, 96, 30, Pencil.LEAD, 0);
        p.text("Z", 136, 70, 40, Pencil.LEAD, 0);
    }

    // ================= augment category icons =================

    private static void heart(Pencil p, Color c) {
        Area h = new Area(Pencil.ell(C - 40, 100, 52, 52));
        h.add(new Area(Pencil.ell(C + 40, 100, 52, 52)));
        h.add(new Area(Pencil.poly(C - 90, 116, C, 226, C + 90, 116, C, 110)));
        p.part(h, 0.15, c, 1.0);
        p.curve(2.4, C - 64, 90, C - 50, 72, C - 30, 70);
    }

    private static void star(Pencil p) {
        double[] xy = new double[20];
        for (int i = 0; i < 10; i++) {
            double a = -PI / 2 + i * PI / 5, rr = i % 2 == 0 ? 108 : 46;
            xy[i * 2] = C + cos(a) * rr;
            xy[i * 2 + 1] = 136 + sin(a) * rr;
        }
        p.smudge(Pencil.ell(C, 136, 120, 120), GOLD, 30);
        p.part(Pencil.poly(xy), 0.1, GOLD, 1.0);
    }

    private static void shieldIcon(Pencil p) {
        Shape s = Pencil.blob(C - 90, 40, C, 24, C + 90, 40, C + 84, 140, C, 236, C - 84, 140);
        p.part(s, 0.45, ICE, 0.5);
        p.part(Pencil.blob(C - 60, 60, C, 50, C + 60, 60, C + 56, 136, C, 204, C - 56, 136), 0.1);
        p.part(Pencil.limb(18, C, 64, C, 190), 0.5, GOLD, 0.8);
        p.part(Pencil.limb(18, C - 50, 112, C + 50, 112), 0.5, GOLD, 0.8);
    }

    private static void wingIcon(Pencil p) {
        p.part(Pencil.blob(60, 200, 50, 120, 90, 50, 170, 24, 236, 40, 210, 90, 226, 120, 190, 150, 200, 180, 150, 200), 0.15, ICE, 0.4);
        for (int i = 0; i < 4; i++) p.curve(2.2, 70 + i * 6, 190 - i * 20, 130 + i * 10, 120 - i * 18, 200 - i * 4, 60 + i * 30);
    }

    private static void coins(Pencil p) {
        for (int i = 0; i < 3; i++) p.part(Pencil.ell(C - 20, 200 - i * 22, 74, 26), 0.2, GOLD, 1.0);
        p.part(Pencil.ell(C + 30, 116, 70, 70), 0.15, GOLD, 1.0);
        p.outline(Pencil.ell(C + 30, 116, 50, 50), 2);
        p.ink(C + 30, 86, C + 30, 146, 3);
        p.curve(3, C + 50, 96, C + 30, 88, C + 12, 102, C + 30, 116, C + 50, 130, C + 30, 144, C + 10, 136);
    }

    private static void orb(Pencil p) {
        p.smudge(Pencil.ell(C, 120, 110, 110), PURPLE, 40);
        p.part(Pencil.ell(C, 120, 76, 76), 0.15, PURPLE, 1.0);
        p.curve(2.6, C - 44, 92, C - 30, 70, C - 6, 60);
        p.part(Pencil.blob(C - 60, 210, C - 40, 180, C + 40, 180, C + 60, 210, C, 236), 0.6, BROWN, 0.7);
        p.colorLine(2.5, YELLOW, C + 70, 40, C + 70, 64);
        p.colorLine(2.5, YELLOW, C + 58, 52, C + 82, 52);
    }

    private static void drop(Pencil p) {
        p.part(Pencil.blob(C, 20, C + 40, 100, C + 74, 160, C + 50, 222, C, 238, C - 50, 222, C - 74, 160, C - 40, 100), 0.2, RED, 1.0);
        p.curve(2.6, C - 40, 160, C - 34, 190, C - 14, 208);
    }

    private static void spikes(Pencil p) {
        p.part(Pencil.limb(30, 30, 210, 226, 210), 0.7);
        for (int i = 0; i < 5; i++) {
            double x = 50 + i * 39;
            p.part(Pencil.poly(x - 18, 200, x, 50 + (i % 2) * 40, x + 18, 200), 0.35, RED, 0.3);
        }
    }

    private static void clover(Pencil p) {
        p.part(Pencil.limb(10, C, 140, C + 20, 240), 0.4, GREEN, 0.8);
        for (int i = 0; i < 4; i++) {
            double a = PI / 4 + i * PI / 2;
            p.part(Pencil.ell(C + cos(a) * 48, 120 + sin(a) * 48, 44, 44), 0.2, GREEN, 1.0);
        }
        p.dot(C, 120, 10, GREEN);
    }

    // ================= catalogs =================

    static Map<String, Monsters.Drawing> items() {
        Map<String, Monsters.Drawing> m = new LinkedHashMap<>();
        m.put("old_sword", p -> sword(p, 24, 30, 178, 70, 0.35, BROWN, null, null, CHIPS));
        m.put("steel_sword", p -> sword(p, 24, 24, 178, 76, 0.12, null, null, null, 0));
        m.put("mithril_sword", p -> sword(p, 26, 20, 178, 80, 0.05, ICE, ICE, ICE, 0));
        m.put("dragon_sword", p -> sword(p, 34, 14, 176, 104, 0.2, RED, RED, RED, 0));
        m.put("holy_sword", p -> sword(p, 34, 10, 176, 110, 0.03, GOLD, GOLD, ICE, GLOW | WINGS));

        m.put("leather_armor", p -> armor(p, LEATHER, 0.35, BROWN, null, 0));
        m.put("iron_armor", p -> armor(p, PLATE, 0.45, null, null, 0));
        m.put("scale_armor", p -> armor(p, SCALE, 0.3, GREEN, GREEN, 0));
        m.put("dragon_armor", p -> armor(p, PLATE, 0.55, RED, RED, SPIKES));
        m.put("guardian_armor", p -> armor(p, PLATE, 0.15, GOLD, GOLD, GLOW));

        m.put("potion", p -> potion(p, RED, 0.85, false));
        m.put("high_potion", p -> potion(p, RED, 1.0, true));
        m.put("ether", p -> potion(p, ICE, 0.9, false));

        // Boss gear.
        m.put("wolf_fang", p -> dagger(p, C, 40, 0.05, null, 0, true));
        m.put("alpha_cloak", p -> armor(p, CLOAK, 0.5, BROWN, GOLD, 0));
        m.put("orc_axe", Icons::axe);
        m.put("crystal_staff", p -> staff(p, ICE));
        m.put("orc_plate", p -> armor(p, PLATE, 0.65, GREEN, BROWN, SPIKES));
        m.put("lich_scythe", Icons::scythe);
        m.put("dead_robe", p -> armor(p, ROBE, 0.7, PURPLE, GOLD, 0));
        m.put("bone_armor", p -> armor(p, PLATE, 0.08, null, null, BONES));
        m.put("demon_blade", p -> sword(p, 36, 12, 176, 100, 0.85, RED, RED, RED, FLAME));
        m.put("hound_twins", p -> {
            dagger(p, C - 34, -24, 0.3, ORANGE, FLAME, false);
            dagger(p, C + 34, 24, 0.3, ORANGE, FLAME, false);
        });
        m.put("demon_guard_plate", p -> armor(p, PLATE, 0.8, RED, RED, SPIKES));
        m.put("fallen_vestment", p -> armor(p, ROBE, 0.5, PURPLE, PURPLE, BONES));
        m.put("frost_spear", p -> spear(p, ICE, 0));
        m.put("glacier_bow", p -> bow(p, ICE));
        m.put("frozen_plate", p -> armor(p, PLATE, 0.2, ICE, ICE, GLOW));
        m.put("blizzard_cloak", p -> armor(p, CLOAK, 0.15, ICE, ICE, 0));
        m.put("lava_greatsword", p -> sword(p, 38, 10, 176, 106, 0.75, ORANGE, ORANGE, ORANGE, CRACKS | GLOW));
        m.put("flame_dagger", p -> dagger(p, C, 40, 0.2, ORANGE, FLAME, false));
        m.put("magma_heart_plate", p -> armor(p, PLATE, 0.8, null, ORANGE, CORE | CRACKS));
        m.put("phoenix_armor", p -> armor(p, CLOAK, 0.2, ORANGE, RED, FLAME));
        m.put("judgment_blade", p -> sword(p, 38, 8, 176, 116, 0.05, GOLD, GOLD, RED, GLOW | WINGS));
        m.put("thunder_lance", p -> spear(p, YELLOW, GLOW));
        m.put("six_wing_armor", p -> armor(p, PLATE, 0.7, null, GOLD, WINGS));
        m.put("sky_order_plate", p -> armor(p, PLATE, 0.15, ICE, GOLD, WINGS | GLOW));

        // Treasure-wheel-only gear (보물 뽑기 전용).
        m.put("swift_rapier", p -> sword(p, 16, 16, 182, 66, 0.04, ICE, GOLD, ICE, 0));
        m.put("hunter_bow", p -> bow(p, GREEN));
        m.put("traveler_cloak", p -> armor(p, CLOAK, 0.4, GREEN, BROWN, 0));
        m.put("studded_vest", p -> armor(p, LEATHER, 0.45, BROWN, null, SPIKES));
        m.put("blue_moon_sword", p -> sword(p, 28, 18, 178, 84, 0.06, ICE, ICE, ICE, GLOW));
        m.put("blood_dagger", p -> dagger(p, C, 40, 0.25, RED, 0, false));
        m.put("storm_axe", p -> {
            axe(p);
            double[] a = tp(C + 40, 20), b = tp(C + 60, 60), c = tp(C + 36, 80), d = tp(C + 64, 130);
            p.colorLine(3.5, YELLOW, a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1]);
        });
        m.put("guardian_chain", p -> armor(p, SCALE, 0.45, null, ICE, 0));
        m.put("mana_robe", p -> armor(p, ROBE, 0.25, ICE, ICE, 0));
        m.put("breeze_cloak", p -> armor(p, CLOAK, 0.08, GREEN, ICE, 0));
        m.put("flame_spirit_sword", p -> sword(p, 30, 14, 178, 90, 0.15, ORANGE, ORANGE, YELLOW, FLAME));
        m.put("frost_fang_spear", p -> spear(p, ICE, GLOW));
        m.put("thunder_hammer", p -> hammer(p, ICE));
        m.put("dragon_scale_cuirass", p -> armor(p, SCALE, 0.4, RED, RED, 0));
        m.put("shadow_leather", p -> armor(p, LEATHER, 0.85, PURPLE, null, 0));
        m.put("paladin_plate", p -> armor(p, PLATE, 0.1, GOLD, ICE, 0));
        m.put("starlight_sword", p -> sword(p, 34, 10, 176, 108, 0.03, PURPLE, GOLD, PURPLE, GLOW | WINGS));
        m.put("abyss_scythe", Icons::scythe);
        m.put("golden_lion_armor", p -> armor(p, PLATE, 0.2, GOLD, GOLD, SPIKES | GLOW));
        m.put("world_tree_robe", p -> armor(p, ROBE, 0.3, GREEN, GOLD, 0));

        // Town: treasure chests (보물 뽑기), the quest board scroll and the inn.
        m.put("chest_wood", p -> chest(p, 0.35, BROWN, new Color(110, 110, 120), false));
        m.put("chest_silver", p -> chest(p, 0.25, new Color(150, 160, 175), new Color(200, 205, 215), false));
        m.put("chest_gold", p -> chest(p, 0.15, GOLD, RED, true));
        m.put("quest_scroll", Icons::scroll);
        m.put("inn_bed", Icons::bed);
        return m;
    }

    static Map<String, Monsters.Drawing> augments() {
        Map<String, Monsters.Drawing> m = new LinkedHashMap<>();
        m.put("sword", p -> sword(p, 28, 18, 178, 84, 0.1, null, GOLD, RED, 0));
        m.put("shield", Icons::shieldIcon);
        m.put("heart", p -> heart(p, RED));
        m.put("star", Icons::star);
        m.put("wing", Icons::wingIcon);
        m.put("coin", Icons::coins);
        m.put("orb", Icons::orb);
        m.put("drop", Icons::drop);
        m.put("spikes", Icons::spikes);
        m.put("clover", Icons::clover);
        return m;
    }
}
