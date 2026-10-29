package game.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Status boxes painted over the battle stage as pencil notes on paper scraps: the foe's box in the top-left (name + HP
 * bar, no numbers), the player's in the bottom-right (name, level, HP with numbers, MP, and a thin EXP
 * strip along the bottom). Bars drain toward their new value instead of jumping, and HP turns
 * yellow then red as it runs low.
 */
public class BattleHud {
    static final Color BOX_FILL = new Color(250, 248, 236);
    static final Color BOX_SHADE = new Color(216, 210, 186);
    static final Color BOX_EDGE = new Color(44, 50, 58);
    static final Color INK = new Color(46, 46, 54);
    static final Color INK_SOFT = new Color(120, 116, 104);
    private static final Color HP_TAG = new Color(232, 160, 40);
    private static final Color HP_HIGH = new Color(54, 150, 70);
    private static final Color HP_MID = new Color(212, 158, 24);
    private static final Color HP_LOW = new Color(196, 62, 52);
    private static final Color MP_FILL = new Color(52, 96, 170);
    private static final Color EXP_FILL = new Color(196, 140, 20);

    /** Bars move this fraction of their full length per second, like the handheld games' steady drain. */
    private static final float DRAIN_PER_SEC = 0.9f;

    private final Runnable repaint;
    private final Timer anim;
    private long lastTick;

    private String monsterName = "";
    private int monsterHp, monsterMaxHp = 1;
    private float shownMonsterHp;

    private String playerName = "";
    private String jobName = "";
    private int level;
    private int hp, maxHp = 1, mp, maxMp = 1, exp, expMax = 1;
    private float shownHp, shownMp, shownExp;

    public BattleHud(Runnable repaint) {
        this.repaint = repaint;
        anim = new Timer(16, e -> tick());
    }

    /** snap = jump straight to the value (a new monster appearing) instead of draining to it. */
    public void setMonster(String name, int hp, int maxHp, boolean snap) {
        this.monsterName = name;
        this.monsterHp = Math.max(0, hp);
        this.monsterMaxHp = Math.max(1, maxHp);
        if (snap) shownMonsterHp = monsterHp;
        kick();
    }

    public void setPlayer(String name, String job, int level, int hp, int maxHp, int mp, int maxMp, int exp, int expMax) {
        boolean levelChanged = level != this.level;
        this.playerName = name;
        this.jobName = job;
        this.level = level;
        this.hp = Math.max(0, hp);
        this.maxHp = Math.max(1, maxHp);
        this.mp = Math.max(0, mp);
        this.maxMp = Math.max(1, maxMp);
        this.exp = Math.max(0, exp);
        this.expMax = Math.max(1, expMax);
        // EXP only ever fills; after a level-up it restarts from empty rather than draining backwards.
        if (levelChanged || this.exp < shownExp) shownExp = 0;
        kick();
    }

    /** Updates only MP (refilled on leaving a dungeon) without touching the HP shown. */
    public void setPlayerMp(int mp, int maxMp) {
        this.mp = Math.max(0, mp);
        this.maxMp = Math.max(1, maxMp);
        kick();
    }

    /** Jumps every player bar to its current value (entering a dungeon). */
    public void snapPlayer() {
        shownHp = hp;
        shownMp = mp;
        shownExp = exp;
        repaint.run();
    }

    private void kick() {
        if (!anim.isRunning()) {
            lastTick = System.nanoTime();
            anim.start();
        }
        repaint.run();
    }

    private void tick() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastTick) / 1e9f);
        lastTick = now;
        shownMonsterHp = approach(shownMonsterHp, monsterHp, monsterMaxHp * DRAIN_PER_SEC * dt);
        shownHp = approach(shownHp, hp, maxHp * DRAIN_PER_SEC * dt);
        shownMp = approach(shownMp, mp, maxMp * DRAIN_PER_SEC * dt);
        shownExp = approach(shownExp, exp, expMax * DRAIN_PER_SEC * 0.6f * dt);
        if (shownMonsterHp == monsterHp && shownHp == hp && shownMp == mp && shownExp == exp) anim.stop();
        repaint.run();
    }

    private static float approach(float cur, float target, float step) {
        if (cur < target) return Math.min(target, cur + step);
        if (cur > target) return Math.max(target, cur - step);
        return cur;
    }

    /** Paints both boxes into a stage of size w x h. */
    public void paint(Graphics2D g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int bw = Math.max(220, Math.min(320, Math.round(w * 0.34f)));
        if (!monsterName.isEmpty()) paintMonsterBox(g2, 14, 14, bw);
        if (!playerName.isEmpty()) {
            int ph = 116;
            paintPlayerBox(g2, w - bw - 14, h - ph - 14, bw, ph);
        }
        g2.dispose();
    }

    private void paintMonsterBox(Graphics2D g2, int x, int y, int bw) {
        int bh = 62;
        paintBox(g2, x, y, bw, bh, false);
        g2.setFont(Sketch.hand(Font.BOLD, 17f));
        g2.setColor(INK);
        g2.drawString(fit(g2, monsterName, bw - 36), x + 16, y + 24);
        paintHpBar(g2, x + 16, y + 34, bw - 40, shownMonsterHp / monsterMaxHp);
    }

    private void paintPlayerBox(Graphics2D g2, int x, int y, int bw, int bh) {
        paintBox(g2, x, y, bw, bh, true);
        int left = x + 22, right = x + bw - 14;

        g2.setFont(Sketch.hand(Font.BOLD, 17f));
        g2.setColor(INK);
        String lv = "Lv." + level;
        Font lvFont = Sketch.hand(Font.BOLD, 15f);
        int lvW = g2.getFontMetrics(lvFont).stringWidth(lv);
        g2.drawString(fit(g2, playerName, right - left - lvW - 10), left, y + 24);
        g2.setFont(lvFont);
        g2.drawString(lv, right - lvW, y + 24);

        int barW = right - left;
        paintHpBar(g2, left, y + 32, barW, shownHp / maxHp);

        // HP numbers under the bar on the right; job name on the left of the same line.
        g2.setFont(Sketch.hand(Font.PLAIN, 12f));
        g2.setColor(INK_SOFT);
        g2.drawString(jobName, left, y + 63);
        g2.setFont(Sketch.hand(Font.BOLD, 15f));
        g2.setColor(INK);
        String nums = Math.round(shownHp) + " / " + maxHp;
        g2.drawString(nums, right - g2.getFontMetrics().stringWidth(nums), y + 64);

        // MP: a slimmer blue bar with its own tag and numbers.
        int mpY = y + 74;
        paintTag(g2, left, mpY, "MP", MP_FILL);
        g2.setFont(Sketch.hand(Font.PLAIN, 12f));
        String mpNums = mp + "/" + maxMp;
        int mpNumW = g2.getFontMetrics().stringWidth(mpNums);
        int mpBarX = left + 30;
        int mpBarW = barW - 30 - mpNumW - 16;
        paintBar(g2, mpBarX, mpY + 1, mpBarW, 9, shownMp / maxMp, MP_FILL);
        g2.setColor(INK);
        g2.drawString(mpNums, right - mpNumW, mpY + 10);

        // EXP strip along the bottom edge of the box.
        int expY = y + bh - 18;
        g2.setFont(Sketch.hand(Font.BOLD, 11f));
        g2.setColor(INK_SOFT);
        g2.drawString("EXP", left, expY + 8);
        paintBar(g2, left + 30, expY, barW - 30, 8, shownExp / expMax, EXP_FILL);
    }

    /** A scrap of ruled notebook paper with a soft shadow and a pencil frame (playerSide only varies the seed). */
    private static void paintBox(Graphics2D g2, int x, int y, int bw, int bh, boolean playerSide) {
        g2.setColor(new Color(0, 0, 0, 55));
        g2.fillRect(x + 4, y + 5, bw, bh);
        g2.setColor(Sketch.PAPER);
        g2.fillRect(x, y, bw, bh);
        g2.setColor(new Color(110, 150, 205, 60));
        g2.setStroke(new BasicStroke(1.1f));
        for (int ly = y + 22; ly < y + bh - 4; ly += 20) g2.drawLine(x + 2, ly, x + bw - 2, ly);
        Sketch.box(g2, x, y, bw, bh, playerSide ? 1202 : 1201, 2.4f, INK);
        g2.setStroke(new BasicStroke(1f));
    }

    /** "HP" written in pencil plus a colored-pencil bar that goes green → yellow → red. */
    private static void paintHpBar(Graphics2D g2, int x, int y, int w, float ratio) {
        paintTag(g2, x, y, "HP", INK);
        Color fill = ratio > 0.5f ? HP_HIGH : ratio > 0.2f ? HP_MID : HP_LOW;
        paintBar(g2, x + 30, y - 1, w - 30, 13, ratio, fill);
    }

    private static void paintTag(Graphics2D g2, int x, int y, String text, Color color) {
        g2.setFont(Sketch.hand(Font.BOLD, 13f));
        g2.setColor(color);
        g2.drawString(text, x, y + 11);
    }

    /** A wobbly pencil box hatched in color up to the ratio (seeded by position, so it holds still). */
    private static void paintBar(Graphics2D g2, int x, int y, int w, int h, float ratio, Color fill) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        long seed = x * 31L + y * 7L + w;
        double fw = w * ratio;
        if (fw > 0.5) {
            Shape s = new java.awt.geom.Rectangle2D.Double(x, y, fw, h);
            g2.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 70));
            g2.fill(s);
            Sketch.hatch(g2, s, seed, fill, 200, h > 8 ? 3.6 : 3, 60);
        }
        Sketch.box(g2, x, y, w, h, seed + 1, 1.6f, INK);
    }
    /** Trims a name with "…" so it never runs past its box. */
    private static String fit(Graphics2D g2, String text, int maxW) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxW) return text;
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
