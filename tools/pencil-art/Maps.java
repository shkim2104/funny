import java.awt.*;
import java.awt.geom.Area;

import static java.lang.Math.*;

/**
 * The town map, the world (dungeon) map and the title scene, penciled on notebook paper. Every
 * building / region is drawn inside the clickable rectangle GameFrame already uses for it
 * (town.png: 1199x1312, dungeonentrence.png: 1312x1199), so no hotspot has to move. The job hall
 * corner of the town (290,1060 310x225) is left as grass: JobHallArt paints the hall there at runtime.
 */
final class Maps {
    static final int TOWN_W = 1199, TOWN_H = 1312, WORLD_W = 1312, WORLD_H = 1199, MULTI_W = 1312, MULTI_H = 1000;
    private static final Color INK = Pencil.LEAD, BLUE = new Color(52, 86, 170);
    private static final Color RED = Monsters.RED, ORANGE = Monsters.ORANGE, ICE = Monsters.ICE, GOLD = Monsters.GOLD,
            PURPLE = Monsters.PURPLE, GREEN = Monsters.GREEN, YELLOW = Monsters.YELLOW, BROWN = Monsters.BROWN;

    private Maps() {}

    /** A boxed, slightly tilted label with an arrow pointing down at what it names. */
    private static void label(Pencil p, String s, double x, double y, float size, double tilt, boolean arrow) {
        double w = p.textWidth(s, size);
        p.part(Pencil.poly(x - 14, y - size - 8, x + w + 14, y - size - 12, x + w + 16, y + 12, x - 12, y + 14), 0.0);
        p.text(s, x, y, size, BLUE, tilt);
        if (arrow) p.curve(2.4, x + w / 2, y + 16, x + w / 2 + 8, y + 40, x + w / 2 - 2, y + 62);
    }

    private static void stickPerson(Pencil p, double x, double y, double s) {
        p.outline(Pencil.ell(x, y - 46 * s, 9 * s, 9 * s), 1.8);
        p.ink(x, y - 37 * s, x, y - 14 * s, 1.8);
        p.ink(x, y - 30 * s, x - 10 * s, y - 20 * s, 1.6);
        p.ink(x, y - 30 * s, x + 10 * s, y - 22 * s, 1.6);
        p.ink(x, y - 14 * s, x - 7 * s, y, 1.6);
        p.ink(x, y - 14 * s, x + 7 * s, y, 1.6);
    }

    private static void pine(Pencil p, double x, double base, double s) {
        p.part(Pencil.limb(10 * s, x, base, x, base - 30 * s), 0.6, BROWN, 0.6);
        for (int i = 0; i < 3; i++) {
            double y = base - 20 * s - i * 32 * s, w = (46 - i * 10) * s;
            p.part(Pencil.poly(x - w, y, x, y - 54 * s, x + w, y), 0.35, GREEN, 0.8);
        }
    }

    private static void house(Pencil p, double x, double y, double w, double h, Color roof, boolean chimney) {
        if (chimney) p.part(Pencil.poly(x + w * 0.62, y - h * 0.55, x + w * 0.78, y - h * 0.55, x + w * 0.78, y - h * 0.1, x + w * 0.62, y - h * 0.1), 0.5);
        p.part(Pencil.poly(x, y, x + w, y, x + w, y + h, x, y + h), 0.12, BROWN, 0.35);
        p.part(Pencil.poly(x - w * 0.12, y + 4, x + w / 2, y - h * 0.75, x + w * 1.12, y + 4), 0.35, roof, 0.85);
        p.part(Pencil.poly(x + w * 0.42, y + h, x + w * 0.42, y + h * 0.45, x + w * 0.62, y + h * 0.45, x + w * 0.62, y + h), 0.6, BROWN, 0.7);
        p.part(Pencil.poly(x + w * 0.12, y + h * 0.25, x + w * 0.3, y + h * 0.25, x + w * 0.3, y + h * 0.5, x + w * 0.12, y + h * 0.5), 0.1, ICE, 0.5);
        p.part(Pencil.poly(x + w * 0.72, y + h * 0.25, x + w * 0.9, y + h * 0.25, x + w * 0.9, y + h * 0.5, x + w * 0.72, y + h * 0.5), 0.1, ICE, 0.5);
    }

    /** A small castle tower standing on base, roof cone and a pennant on top, lit windows. */
    private static void tower(Pencil p, double x, double w, double top, double base, double tone) {
        p.part(Pencil.poly(x - w / 2, base, x + w / 2, base, x + w / 2, top, x - w / 2, top), tone);
        p.part(Pencil.poly(x - w * 0.62, top, x + w * 0.62, top, x, top - w * 1.4), min(1, tone + 0.25));
        double tip = top - w * 1.4;
        p.ink(x, tip, x, tip - 22, 1.8);
        p.part(Pencil.poly(x, tip - 22, x + 20, tip - 16, x, tip - 10), 0.1, RED, 0.9);
        for (double wy = top + 14; wy < base - 18; wy += 34) {
            p.part(Pencil.ell(x, wy + 6, w * 0.13, w * 0.2), 0.1, ORANGE, 0.9);
        }
    }

    private static void fence(Pencil p, double x0, double x1, double y) {
        p.ink(x0, y - 14, x1, y - 14, 1.8);
        p.ink(x0, y - 4, x1, y - 4, 1.8);
        for (double x = x0; x <= x1; x += 18) p.ink(x, y + 2, x, y - 22, 2);
    }

    private static void flowers(Pencil p, int n, double x0, double y0, double x1, double y1) {
        for (int i = 0; i < n; i++) {
            double x = x0 + p.r.nextDouble() * (x1 - x0), y = y0 + p.r.nextDouble() * (y1 - y0);
            Color c = i % 3 == 0 ? RED : i % 3 == 1 ? YELLOW : PURPLE;
            for (int k = 0; k < 5; k++) {
                double a = PI * 2 * k / 5;
                p.smudge(Pencil.ell(x + cos(a) * 5, y + sin(a) * 4, 4, 3), c, 200);
            }
            p.smudge(Pencil.ell(x, y, 2.2, 2), YELLOW, 240);
        }
    }

    private static void star(Pencil p, double x, double y, double r, Color c) {
        double[] xy = new double[20];
        for (int i = 0; i < 10; i++) {
            double a = -PI / 2 + i * PI / 5, rr = i % 2 == 0 ? r : r * 0.42;
            xy[i * 2] = x + cos(a) * rr;
            xy[i * 2 + 1] = y + sin(a) * rr;
        }
        p.part(Pencil.poly(xy), 0.0, c, 0.6);
    }

    // ================= town =================

    static void town(Pencil p) {
        // Roads first: a cobbled plaza and paths out to every building.
        p.part(Pencil.blob(600, 560, 640, 700, 720, 860, 860, 1000, 980, 1080, 1010, 1060, 900, 940, 760, 780, 700, 620), 0.06, BROWN, 0.4);
        p.part(Pencil.blob(560, 620, 380, 520, 300, 520, 330, 560, 520, 650), 0.06, BROWN, 0.4);
        p.part(Pencil.blob(640, 620, 780, 520, 880, 520, 860, 560, 700, 650), 0.06, BROWN, 0.4);
        p.part(Pencil.ell(600, 700, 230, 160), 0.08, BROWN, 0.45);
        for (int ring = 1; ring <= 3; ring++) p.outline(Pencil.ell(600, 700, 230 - ring * 50, 160 - ring * 35), 1.2);

        // Fountain in the middle of the plaza.
        p.part(Pencil.ell(600, 710, 90, 40), 0.25);
        p.part(Pencil.ell(600, 706, 72, 28), 0.05, ICE, 0.9);
        p.part(Pencil.limb(18, 600, 706, 600, 640), 0.25);
        p.part(Pencil.ell(600, 640, 28, 12), 0.2);
        for (int i = -1; i <= 1; i++) p.colorLine(2.2, ICE, 600, 636, 600 + i * 22, 610, 600 + i * 34, 640);
        for (double[] b : new double[][]{{470, 640}, {730, 640}}) p.part(Pencil.poly(b[0] - 34, b[1], b[0] + 34, b[1] - 6, b[0] + 36, b[1] + 8, b[0] - 32, b[1] + 14), 0.4, BROWN, 0.7);

        // ---- 대장간 (hotspot 130,200 430x350) ----
        for (int i = 0; i < 4; i++) p.part(Pencil.ell(352 + i * 22, 250 - i * 26, 18 + i * 5, 14 + i * 3), 0.04, ICE, 0.25);   // smoke
        house(p, 170, 360, 250, 160, RED, true);
        p.part(Pencil.blob(250, 520, 250, 440, 290, 410, 330, 440, 330, 520), 0.85, ORANGE, 0.5);   // forge mouth
        p.smudge(Pencil.ell(290, 490, 40, 30), ORANGE, 70);
        p.part(Pencil.poly(450, 470, 520, 470, 506, 486, 494, 486, 498, 516, 474, 516, 478, 486, 462, 486), 0.8);   // anvil
        for (int i = 0; i < 3; i++) p.part(Pencil.poly(440 + i * 30, 380, 452 + i * 30, 380, 448 + i * 30, 450, 444 + i * 30, 450), 0.05);   // swords
        p.ink(430, 376, 530, 376, 3);
        label(p, "대장간", 200, 232, 38, -2, true);

        // ---- 박물관 (hotspot 680,160 395x360) ----
        p.part(Pencil.ell(878, 232, 90, 60), 0.3, ICE, 0.8);   // dome
        p.part(Pencil.poly(700, 300, 878, 236, 1056, 300), 0.25, ICE, 0.7);
        p.part(Pencil.poly(710, 300, 1046, 300, 1046, 470, 710, 470), 0.06, GOLD, 0.25);
        for (int i = 0; i < 5; i++) p.part(Pencil.limb(18, 740 + i * 68, 312, 740 + i * 68, 464), 0.08);
        p.part(Pencil.blob(850, 470, 850, 400, 878, 380, 906, 400, 906, 470), 0.65, BROWN, 0.6);
        for (int i = 0; i < 3; i++) p.part(Pencil.poly(690 - i * 12, 470 + i * 14, 1066 + i * 12, 470 + i * 14, 1066 + i * 12, 484 + i * 14, 690 - i * 12, 484 + i * 14), 0.15);
        star(p, 878, 278, 14, GOLD);
        label(p, "박물관", 820, 196, 38, 2, false);

        // ---- 상점 (hotspot 725,630 345x340) ----
        house(p, 780, 760, 250, 170, RED, false);
        Area awning = new Area(Pencil.poly(770, 820, 1040, 820, 1060, 870, 750, 870));
        p.part(awning, 0.05);
        for (int i = 0; i < 6; i++) {
            double x = 750 + i * 52;
            p.part(Pencil.poly(x, 870, x + 26, 870, x + 22, 820, x + 4, 820), 0.1, RED, 1.0);
        }
        for (double[] c : new double[][]{{780, 930}, {840, 940}, {990, 930}}) {
            p.part(Pencil.poly(c[0] - 26, c[1] - 20, c[0] + 26, c[1] - 20, c[0] + 26, c[1] + 16, c[0] - 26, c[1] + 16), 0.35, BROWN, 0.7);
            p.dot(c[0] - 8, c[1] - 24, 7, c[0] < 820 ? RED : c[0] < 900 ? GREEN : ORANGE);
            p.dot(c[0] + 8, c[1] - 24, 7, YELLOW);
        }
        p.ink(1060, 700, 1060, 760, 2.4);
        p.part(Pencil.ell(1060, 780, 26, 22), 0.05, GOLD, 0.8);
        p.text("G", 1050, 790, 26, INK, 0);
        label(p, "상점", 860, 672, 38, -2, true);

        // ---- 던전 입구 (hotspot 860,990 330x260) ----
        p.part(Pencil.blob(880, 1250, 900, 1110, 980, 1030, 1080, 1010, 1170, 1060, 1190, 1250), 0.45);
        p.part(Pencil.blob(970, 1250, 970, 1140, 1030, 1100, 1090, 1140, 1090, 1250), 0.97);
        for (double tx : new double[]{945, 1115}) {
            p.part(Pencil.limb(10, tx, 1200, tx, 1150), 0.6, BROWN, 0.6);
            p.part(Pencil.flame(tx, 1150, 22, 40, 4), 0.03, ORANGE, 1.0);
        }
        p.dots(24, 4.5, BLUE, 880, 980, 840, 1050, 900, 1120, 960, 1180);
        p.part(Pencil.poly(800, 1010, 1010, 1000, 1040, 1028, 1010, 1056, 800, 1060), 0.03);
        p.text("던전 입구 →", 816, 1044, 28, BLUE, -1);
        p.ink(900, 1060, 904, 1130, 3);

        // ---- 전직소 (hotspot 290,1060 310x225): a little hall with the three class weapons over the door ----
        p.part(Pencil.poly(316, 1272, 574, 1272, 574, 1160, 316, 1160), 0.14, GOLD, 0.3);
        p.part(Pencil.poly(298, 1166, 445, 1092, 592, 1166), 0.35, PURPLE, 0.8);
        for (double cx : new double[]{340, 550}) p.part(Pencil.limb(14, cx, 1170, cx, 1268), 0.08);
        p.part(Pencil.blob(415, 1272, 415, 1210, 445, 1190, 475, 1210, 475, 1272), 0.65, BROWN, 0.6);
        p.part(Pencil.ell(445, 1138, 22, 22), 0.03);
        p.ink(430, 1150, 460, 1124, 2.4);   // sword
        p.ink(460, 1150, 430, 1124, 2.4);   // staff
        p.curve(2, 436, 1124, 445, 1132, 454, 1124);   // bow
        for (double fx : new double[]{340, 550}) {
            p.ink(fx, 1160, fx, 1120, 2);
            p.part(Pencil.poly(fx, 1120, fx + 22, 1126, fx, 1134), 0.1, fx < 400 ? RED : ICE, 0.9);
        }
        for (double wx : new double[]{372, 518}) p.part(Pencil.ell(wx, 1210, 14, 18), 0.1, ORANGE, 0.8);
        label(p, "전직소", 470, 1098, 30, 2, false);

        // ---- houses, trees, fences and folks around the plaza ----
        house(p, 110, 600, 130, 90, RED, true);
        house(p, 160, 790, 130, 90, ORANGE, false);
        house(p, 120, 960, 150, 100, ICE, true);
        house(p, 470, 880, 120, 80, RED, false);
        house(p, 610, 950, 120, 80, ORANGE, true);
        fence(p, 90, 270, 1090);
        fence(p, 440, 600, 990);

        // ---- 여관 / 의뢰소 / 보물 뽑기: signs on three of the houses (GameFrame has a hotspot on each) ----
        label(p, "여관", 166, 952, 24, -2, false);
        p.text("z Z", 238, 940, 20, INK, -8);
        label(p, "의뢰소", 182, 786, 24, 2, false);
        p.part(Pencil.poly(112, 820, 152, 820, 152, 858, 112, 858), 0.3, BROWN, 0.7);   // notice board
        p.ink(118, 858, 118, 884, 2.4);
        p.ink(146, 858, 146, 884, 2.4);
        for (double[] n : new double[][]{{118, 826}, {134, 830}, {124, 842}}) {
            p.part(Pencil.poly(n[0], n[1], n[0] + 14, n[1], n[0] + 14, n[1] + 12, n[0], n[1] + 12), 0.0);
            p.dot(n[0] + 7, n[1] + 1, 2, RED);
        }
        label(p, "보물 뽑기", 476, 876, 21, -1, false);
        p.part(Pencil.poly(604, 936, 644, 936, 642, 962, 606, 962), 0.3, GOLD, 0.9);   // little chest
        p.part(Pencil.blob(604, 938, 610, 922, 624, 916, 638, 922, 644, 938), 0.3, GOLD, 0.9);
        p.dot(624, 944, 4, Pencil.LEAD);
        for (double[] t : new double[][]{{70, 360}, {80, 650}, {560, 330}, {620, 520}, {1120, 400}, {1110, 640}, {700, 1100}, {60, 1250}}) {
            pine(p, t[0], t[1], 1.0);
        }
        Backdrops.tree(p, 400, 920, 0.6);
        Backdrops.tree(p, 1140, 980, 0.55);
        for (double[] s : new double[][]{{540, 560}, {660, 800}, {450, 760}, {820, 560}, {720, 880}}) stickPerson(p, s[0], s[1], 0.9);
        for (double[] l : new double[][]{{520, 600}, {690, 600}, {410, 720}, {800, 720}}) {
            p.ink(l[0], l[1], l[0], l[1] - 70, 2.4);
            p.part(Pencil.poly(l[0] - 9, l[1] - 70, l[0] + 9, l[1] - 70, l[0] + 6, l[1] - 88, l[0] - 6, l[1] - 88), 0.05, YELLOW, 0.9);
        }
        flowers(p, 30, 100, 560, 1150, 1050);

        // ---- doodles ----
        p.part(Pencil.poly(110, 40, 440, 30, 450, 128, 116, 136), 0.0);
        p.text("우리 마을", 150, 110, 58, BLUE, -2);
        star(p, 86, 80, 18, null);
        star(p, 480, 60, 16, null);
        p.text("작은 마을,", 940, 70, 26, BLUE, 2);
        p.text("큰 모험의 시작!", 920, 108, 26, BLUE, 2);
        p.curve(2.4, 1110, 136, 1126, 150, 1142, 136);
        p.dot(1114, 120, 3, INK);
        p.dot(1138, 120, 3, INK);
        p.text("언젠가 여기서 퀘스트를...!", 36, 1302, 20, BLUE, -2);
        portalHouse(p);
    }

    /** The door to other worlds, drawn last over the plain house on that spot so the rest of the town keeps its strokes. */
    private static void portalHouse(Pencil p) {
        house(p, 110, 600, 130, 90, PURPLE, false);
        p.smudge(Pencil.ell(175, 640, 58, 62), PURPLE, 60);
        p.part(Pencil.ell(175, 648, 30, 42), 0.35, PURPLE, 1.0);
        p.part(Pencil.ell(175, 648, 19, 29), 0.08, ICE, 0.9);
        p.curve(2.2, 175, 648, 184, 642, 180, 630, 166, 632, 160, 650, 172, 668, 190, 662);
        label(p, "???", 150, 534, 26, -2, false);
    }

    // ================= the road between worlds =================

    /** A planet with nothing to see yet: solid graphite, only its outline hints at what it will be. */
    private static void silhouette(Pencil p, double x, double y, double r, int kind) {
        Color dark = new Color(46, 44, 54);
        p.smudge(Pencil.ell(x, y, r * 1.28, r * 1.28), Pencil.PAPER, 225);
        java.util.function.Consumer<Shape> solid = s -> {
            p.g.setColor(dark);
            p.g.fill(s);
            p.outline(s, 2.6);
        };
        if (kind == 0) {
            solid.accept(Pencil.limb(r * 0.16, x - r * 1.35, y + r * 0.34, x, y + r * 0.1, x + r * 1.35, y - r * 0.34));
        } else if (kind == 1) {
            for (int i = 0; i < 7; i++) {
                double a = -PI * 0.92 + i * PI * 0.84 / 6, tip = r * 1.34;
                solid.accept(Pencil.poly(x + cos(a - 0.2) * r * 0.9, y + sin(a - 0.2) * r * 0.9, x + cos(a) * tip, y + sin(a) * tip,
                        x + cos(a + 0.2) * r * 0.9, y + sin(a + 0.2) * r * 0.9));
            }
        } else if (kind == 2) {
            solid.accept(Pencil.ell(x + r * 1.05, y - r * 0.8, r * 0.3, r * 0.3));
            solid.accept(Pencil.ell(x - r * 1.1, y + r * 0.72, r * 0.2, r * 0.2));
        }
        solid.accept(kind == 3 ? Pencil.rock(p.r, x, y, r * 1.1, r * 1.2, 11) : Pencil.ell(x, y, r, r));
        if (kind != 3) p.hatch(Pencil.ell(x, y, r * 0.96, r * 0.96), 1, 42, new Color(90, 88, 100));
    }

    /** The planet of heroes: an arena standing on a round world, the only one that can be seen. */
    private static void colosseumPlanet(Pencil p) {
        double x = 656, y = 470, r = 178;
        p.smudge(Pencil.ell(x, y, r * 1.36, r * 1.36), Pencil.PAPER, 235);
        p.smudge(Pencil.ell(x, y, r * 1.26, r * 1.26), ORANGE, 42);
        p.part(Pencil.ell(x, y, r, r), 0.18, ORANGE, 0.7);
        for (double[] c : new double[][]{{-92, 74, 30}, {78, 104, 22}, {112, -8, 18}, {-118, -22, 14}}) {
            p.part(Pencil.ell(x + c[0], y + c[1], c[2], c[2] * 0.72), 0.45, BROWN, 0.5);
        }
        p.outlineOpen(Pencil.spline(x - 262, y + 46, x - 120, y + 98, x + 120, y + 96, x + 262, y + 40), 3.2);
        // The arena, sitting on top of the world.
        double top = y - 212, w = 182;
        p.part(Pencil.ell(x, top + 118, w * 1.04, 30), 0.5, BROWN, 0.6);
        p.part(Pencil.poly(x - w, top + 112, x - w + 10, top + 30, x + w - 10, top + 30, x + w, top + 112), 0.08, GOLD, 0.45);
        p.part(Pencil.ell(x, top + 30, w - 10, 26), 0.42, BROWN, 0.6);
        p.part(Pencil.ell(x, top + 30, w - 42, 15), 0.1, ORANGE, 0.55);
        for (int i = 0; i < 9; i++) {
            double ax = x - w + 30 + i * (w * 2 - 60) / 8.0;
            p.part(Pencil.poly(ax - 11, top + 104, ax - 11, top + 74, ax, top + 60, ax + 11, top + 74, ax + 11, top + 104), 0.72, BROWN, 0.4);
        }
        for (double fx : new double[]{x - w + 20, x, x + w - 20}) {
            p.ink(fx, top + 26, fx, top - 28, 2.2);
            p.part(Pencil.poly(fx, top - 28, fx + 30, top - 19, fx, top - 8), 0.08, RED, 0.95);
        }
        label(p, "콜로세움", x - 76, y + 120, 34, -2, false);
    }

    static void multiverse(Pencil p) {
        p.hatch(new java.awt.Rectangle(0, 0, MULTI_W, MULTI_H), 0.42, 38, Pencil.LEAD);
        p.hatch(new java.awt.Rectangle(0, 0, MULTI_W, MULTI_H), 0.3, -40, Pencil.LEAD);
        for (int i = 0; i < 90; i++) {
            double sx = p.r.nextDouble() * MULTI_W, sy = p.r.nextDouble() * MULTI_H;
            if (i % 5 == 0) star(p, sx, sy, 7 + p.r.nextDouble() * 6, null);
            else p.smudge(Pencil.ell(sx, sy, 2.4, 2.4), Pencil.PAPER, 235);
        }
        // The roads out of the door, one to each world.
        double gateX = 656, gateY = 905;
        for (double[] to : new double[][]{{656, 650}, {250, 245}, {1062, 245}, {250, 770}, {1062, 770}}) {
            p.dots(19, 3.6, Pencil.PAPER, gateX, gateY, (gateX + to[0]) / 2, (gateY + to[1]) / 2 + 46, to[0], to[1]);
        }
        p.smudge(Pencil.ell(gateX, gateY, 118, 72), Pencil.PAPER, 225);
        p.smudge(Pencil.ell(gateX, gateY, 96, 56), PURPLE, 110);
        p.part(Pencil.ell(gateX, gateY, 78, 42), 0.3, PURPLE, 1.0);
        p.part(Pencil.ell(gateX, gateY, 48, 25), 0.06, ICE, 0.95);
        p.curve(2.4, gateX, gateY, gateX + 22, gateY - 9, gateX + 8, gateY - 20, gateX - 26, gateY - 8, gateX - 18, gateY + 16, gateX + 34, gateY + 14);

        silhouette(p, 250, 245, 104, 0);
        silhouette(p, 1062, 245, 96, 1);
        silhouette(p, 250, 770, 92, 2);
        silhouette(p, 1062, 770, 106, 3);
        colosseumPlanet(p);

        p.smudge(Pencil.ell(226, 72, 190, 54), Pencil.PAPER, 235);
        p.part(Pencil.poly(60, 30, 390, 22, 398, 112, 66, 120), 0.0);
        p.text("차원의 길", 96, 94, 54, BLUE, -2);
    }

    /** The duel stage: sand in front, the stands and their crowd behind. Feet land on the sand. */
    static void arena(Pencil p) {
        int w = Backdrops.W, h = Backdrops.H;
        double horizon = Backdrops.HORIZON;
        p.part(Pencil.poly(0, horizon - 20, w, horizon - 20, w, h, 0, h), 0.1, YELLOW, 0.5);
        for (int ring = 1; ring <= 3; ring++) p.outline(Pencil.ell(w / 2.0, 760, 250 * ring, 66 * ring), 1.5);
        for (int i = 0; i < 70; i++) {
            double sx = p.r.nextDouble() * w, sy = horizon + 30 + p.r.nextDouble() * (h - horizon - 40);
            p.ink(sx, sy, sx + 18 + p.r.nextDouble() * 26, sy + 1, 1.2);
        }
        // The wall around the sand, with gates.
        p.part(Pencil.poly(0, horizon - 110, w, horizon - 110, w, horizon - 18, 0, horizon - 18), 0.5, BROWN, 0.6);
        for (int i = 0; i < 9; i++) {
            double ax = 90 + i * 186;
            p.part(Pencil.poly(ax - 46, horizon - 18, ax - 46, horizon - 74, ax, horizon - 102, ax + 46, horizon - 74, ax + 46, horizon - 18), 0.95);
            p.ink(ax - 22, horizon - 18, ax - 22, horizon - 90, 1.8);
            p.ink(ax + 22, horizon - 18, ax + 22, horizon - 90, 1.8);
            p.ink(ax - 46, horizon - 56, ax + 46, horizon - 56, 1.8);
        }
        // Rows of stands rising behind it, full of little heads.
        for (int row = 0; row < 7; row++) {
            double y1 = horizon - 110 - row * 62, y0 = y1 - 62;
            p.part(Pencil.poly(0, y1, w, y1, w, y0, 0, y0), row % 2 == 0 ? 0.3 : 0.2, BROWN, 0.35);
            for (double hx = 14 + (row % 2) * 16; hx < w; hx += 33) {
                double hy = y0 + 25 + p.r.nextDouble() * 7;
                Color shirt = hx % 5 < 1.5 ? RED : hx % 5 < 3 ? ICE : GOLD;
                p.smudge(Pencil.ell(hx, hy + 18, 12, 10), shirt, 150);
                p.outline(Pencil.ell(hx, hy, 8.5, 8.5), 1.3);
            }
        }
        for (double fx : new double[]{170, 480, 836, 1190, 1500}) {
            p.ink(fx, horizon - 110, fx, 50, 3);
            p.part(Pencil.poly(fx, 50, fx + 104, 80, fx + 76, 122, fx + 104, 164, fx, 136), 0.08, RED, 0.95);
        }
    }
    // ================= world map =================

    static void world(Pencil p) {
        // Sea: waves all over, then the big island.
        for (int i = 0; i < 90; i++) {
            double x = 60 + p.r.nextDouble() * (WORLD_W - 100), y = 230 + p.r.nextDouble() * (WORLD_H - 280);
            p.colorLine(1.8, ICE, x - 14, y, x - 4, y - 6, x + 6, y, x + 16, y - 6);
        }
        Shape island = Pencil.blob(60, 650, 140, 470, 180, 300, 420, 230, 700, 300, 980, 270, 1180, 320, 1230, 520,
                1150, 700, 1180, 900, 1060, 1060, 820, 1040, 620, 1080, 420, 1000, 200, 960, 70, 860);
        p.part(island, 0.06, new Color(200, 180, 110), 0.35);
        p.outline(island, 3.2);
        for (double[] isl : new double[][]{{1180, 220, 60, 30}, {110, 1000, 50, 26}, {1240, 1000, 40, 22}}) {
            p.part(Pencil.rock(p.r, isl[0], isl[1], isl[2], isl[3], 8), 0.06, GREEN, 0.5);
        }

        // ---- ① 초원 (60,600 410x260) ----
        p.part(Pencil.blob(80, 760, 150, 640, 300, 610, 450, 660, 460, 820, 320, 860, 130, 850), 0.05, GREEN, 0.6);
        for (double[] t : new double[][]{{150, 720}, {200, 700}, {250, 730}, {380, 700}, {420, 740}, {170, 810}, {330, 800}, {400, 820}}) {
            Backdrops.tree(p, t[0], t[1], 0.22);
        }
        p.part(Pencil.ell(290, 780, 46, 22), 0.05, ICE, 0.9);
        label(p, "① 초원", 120, 640, 30, -2, false);

        // ---- ② 어두운 동굴 (470,700 260x300) ----
        p.part(Pencil.poly(480, 960, 540, 790, 600, 730, 660, 780, 720, 960), 0.55);
        p.part(Pencil.blob(560, 960, 560, 880, 600, 850, 640, 880, 640, 960), 0.97);
        for (double[] b : new double[][]{{690, 760}, {720, 800}}) {
            p.curve(2, b[0] - 12, b[1], b[0] - 4, b[1] - 8, b[0], b[1]);
            p.curve(2, b[0], b[1], b[0] + 4, b[1] - 8, b[0] + 12, b[1]);
        }
        label(p, "② 어두운 동굴", 520, 1010, 28, 1, false);

        // ---- ③ 잊혀진 폐허 (490,460 330x230) ----
        for (double cx : new double[]{560, 620, 700, 760}) Backdrops.column(p, cx, 670, 26, cx == 700 ? 80 : 130, cx == 700, 0.25);
        Area arch = new Area(Pencil.ell(660, 560, 60, 40));
        arch.subtract(new Area(Pencil.ell(660, 560, 36, 24)));
        arch.subtract(new Area(new Rectangle(580, 560, 160, 60)));
        p.part(arch, 0.25);
        label(p, "③ 잊혀진 폐허", 580, 480, 28, -2, false);

        // ---- ④ 마왕성 (860,360 340x280) ----
        p.part(Pencil.blob(880, 600, 940, 560, 1100, 560, 1190, 600, 1100, 640, 960, 640), 0.7, PURPLE, 0.6);
        for (double[] t : new double[][]{{930, 460, 40}, {990, 420, 50}, {1060, 440, 44}, {1130, 480, 36}}) {
            tower(p, t[0], t[2], t[1], 600, 0.8);
        }
        p.smudge(Pencil.ell(1030, 520, 150, 80), PURPLE, 30);
        label(p, "④ 마왕성", 930, 392, 30, 2, false);

        // ---- ⑤ 얼음 협곡 (760,690 370x320) ----
        for (int i = 0; i < 9; i++) {
            double x = 790 + i * 38, h = 80 + (i % 3) * 50;
            p.part(Pencil.poly(x - 20, 960, x + 2, 960 - h, x + 20, 960), 0.08, ICE, 1.0);
        }
        p.outlineOpen(Pencil.spline(840, 900, 930, 860, 1020, 900), 4);
        label(p, "⑤ 얼음 협곡", 860, 735, 28, 2, false);

        // ---- ⑥ 불화산 (180,220 380x300) ----
        for (int i = 0; i < 4; i++) p.part(Pencil.ell(380 + i * 18 - (i % 2) * 40, 240 - i * 12, 40 + i * 8, 28 + i * 4), 0.3);
        p.part(Pencil.poly(200, 510, 330, 300, 420, 300, 550, 510), 0.6);
        p.part(Pencil.ell(375, 302, 46, 12), 0.05, ORANGE, 1.0);
        p.colorLine(5, ORANGE, 350, 306, 330, 380, 290, 450, 260, 505);
        p.colorLine(4, ORANGE, 400, 306, 430, 390, 470, 500);
        label(p, "⑥ 불화산", 190, 280, 30, -3, false);

        // ---- ⑦ 천공의 성채 (450,20 450x240) ----
        Backdrops.cloud(p, 520, 200, 160, 0.03);
        Backdrops.cloud(p, 840, 190, 150, 0.03);
        p.part(Pencil.poly(580, 200, 780, 200, 730, 250, 680, 270, 630, 250), 0.5);
        for (double[] t : new double[][]{{620, 140, 28}, {680, 110, 34}, {740, 140, 28}}) {
            tower(p, t[0], t[2], t[1], 205, 0.2);
        }
        for (int i = 0; i < 4; i++) star(p, 560 + i * 80, 50 + (i % 2) * 40, 9, YELLOW);
        label(p, "⑦ 천공의 성채", 760, 70, 28, 1, false);

        // ---- the route 1 → 7 ----
        p.dots(26, 5, BLUE, 300, 760, 450, 820, 600, 900, 640, 760, 650, 620, 800, 580, 950, 560, 1000, 620,
                960, 760, 900, 880, 700, 700, 500, 560, 380, 420, 500, 330, 640, 250, 680, 230);

        // ---- doodles ----
        p.text("< 섬나라 아반가르드 지도 >", 60, 70, 34, BLUE, -1);
        p.text("7개의 지역, 그리고 마지막 성으로...!", 70, 120, 22, BLUE, -1);
        double cx = 1180, cy = 1080;
        p.part(Pencil.poly(cx, cy - 70, cx + 14, cy, cx, cy + 70, cx - 14, cy), 0.3, BLUE, 0.6);
        p.part(Pencil.poly(cx - 70, cy, cx, cy - 14, cx + 70, cy, cx, cy + 14), 0.3, BLUE, 0.6);
        p.text("N", cx - 8, cy - 80, 22, BLUE, 0);
        p.text("퀘스트: 7개 지역 모두 탐험하기!", 70, 1150, 22, BLUE, -2);
    }

    // ================= title scene (1672x941) =================

    static void title(Pencil p) {
        double hz = 560;
        p.part(Pencil.ell(1330, 150, 60, 60), 0.0, YELLOW, 0.8);
        for (int i = 0; i < 12; i++) {
            double a = PI * 2 * i / 12;
            p.colorLine(3, YELLOW, 1330 + cos(a) * 80, 150 + sin(a) * 80, 1330 + cos(a) * 108, 150 + sin(a) * 108);
        }
        Backdrops.cloud(p, 300, 110, 220, 0.03);
        Backdrops.cloud(p, 820, 80, 180, 0.03);
        // Snowy mountain range.
        double[] peaks = {-40, 470, 180, 160, 420, 330, 640, 110, 860, 300, 1080, 120, 1300, 300, 1520, 150, 1720, 420};
        double[] range = new double[peaks.length + 4];
        System.arraycopy(peaks, 0, range, 0, peaks.length);
        range[peaks.length] = 1720;
        range[peaks.length + 1] = hz;
        range[peaks.length + 2] = -40;
        range[peaks.length + 3] = hz;
        p.part(Pencil.poly(range), 0.22, ICE, 0.35);
        for (int i = 2; i + 1 < peaks.length - 2; i += 4) {
            double x = peaks[i], y = peaks[i + 1];
            p.part(Pencil.poly(x - 46, y + 60, x, y, x + 46, y + 60, x + 18, y + 48, x, y + 70, x - 18, y + 48), 0.0);
        }
        p.part(Pencil.blob(-60, hz, 300, 470, 700, 500, 1100, 470, 1500, 490, 1740, 470, 1740, hz + 30, -60, hz + 30), 0.18, GREEN, 0.5);
        p.part(Pencil.poly(-20, hz + 10, 1692, hz + 10, 1692, 961, -20, 961), 0.05, GREEN, 0.4);
        // A river winding down from the mountains.
        p.part(Pencil.blob(820, hz + 6, 846, hz + 6, 900, 700, 1000, 820, 1060, 960, 930, 960, 880, 820, 830, 680), 0.03, ICE, 0.9);
        p.ink(860, 740, 940, 736, 4);   // little bridge
        for (double[] t : new double[][]{{260, hz + 20}, {420, hz + 6}, {1240, hz + 10}, {1420, hz + 26}}) pine(p, t[0], t[1], 0.9);
        for (int i = 0; i < 60; i++) {
            double x = p.r.nextDouble() * 1672, y = hz + 40 + p.r.nextDouble() * 360;
            p.ink(x, y, x - 4, y - 10, 1.4);
            p.ink(x, y, x + 3, y - 12, 1.4);
        }
        flowers(p, 24, 200, hz + 60, 1600, 920);
        // The hero on a cliff, looking out — a doodle with a cape and a sword on the back.
        p.part(Pencil.blob(-40, 760, 160, 730, 340, 760, 420, 820, 380, 961, -40, 961), 0.5, BROWN, 0.6);
        double hx = 250, hy = 752, s = 1.7;
        p.part(Pencil.limb(8 * s, hx - 30 * s, hy - 60 * s, hx + 34 * s, hy - 190 * s), 0.1);   // sword on the back
        p.part(Pencil.limb(10 * s, hx + 22 * s, hy - 186 * s, hx + 40 * s, hy - 196 * s), 0.6, BROWN, 0.6);   // hilt
        p.part(Pencil.blob(hx - 34 * s, hy - 120 * s, hx + 30 * s, hy - 120 * s, hx + 70 * s, hy - 10 * s, hx + 40 * s, hy,
                hx - 50 * s, hy - 6 * s, hx - 60 * s, hy - 60 * s), 0.25, BLUE, 0.9);   // cape
        p.part(Pencil.limb(14 * s, hx - 12 * s, hy - 40 * s, hx - 16 * s, hy), 0.5);
        p.part(Pencil.limb(14 * s, hx + 12 * s, hy - 40 * s, hx + 16 * s, hy), 0.5);
        p.part(Pencil.poly(hx - 26 * s, hy - 130 * s, hx + 26 * s, hy - 130 * s, hx + 22 * s, hy - 40 * s, hx - 22 * s, hy - 40 * s), 0.4, BROWN, 0.5);
        p.part(Pencil.ell(hx, hy - 156 * s, 26 * s, 28 * s), 0.08);
        p.part(Pencil.blob(hx - 28 * s, hy - 160 * s, hx - 20 * s, hy - 186 * s, hx + 4 * s, hy - 190 * s, hx + 26 * s, hy - 176 * s,
                hx + 28 * s, hy - 150 * s, hx, hy - 168 * s), 0.55, BROWN, 0.7);   // hair (seen from behind)
        // Leave the middle open: the game's title and buttons sit there.
    }
}
