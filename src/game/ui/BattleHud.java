package game.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Pokémon-style status boxes painted over the battle stage: the foe's box in the top-left (name + HP
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
    private static final Color TRACK = new Color(70, 72, 78);
    private static final Color HP_TAG = new Color(232, 160, 40);
    private static final Color HP_HIGH = new Color(88, 208, 120);
    private static final Color HP_MID = new Color(246, 196, 40);
    private static final Color HP_LOW = new Color(236, 78, 62);
    private static final Color MP_FILL = new Color(96, 150, 236);
    private static final Color EXP_FILL = new Color(72, 196, 248);

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
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        int bw = Math.max(220, Math.min(320, Math.round(w * 0.34f)));
        if (!monsterName.isEmpty()) paintMonsterBox(g2, 14, 14, bw);
        if (!playerName.isEmpty()) {
            int ph = 104;
            paintPlayerBox(g2, w - bw - 14, h - ph - 14, bw, ph);
        }
        g2.dispose();
    }

    private void paintMonsterBox(Graphics2D g2, int x, int y, int bw) {
        int bh = 62;
        // Foe box: slanted right edge, like the opponent's panel in the handheld games.
        paintBox(g2, x, y, bw, bh, false);
        g2.setFont(Theme.dosFont(Font.BOLD, 17f));
        g2.setColor(INK);
        g2.drawString(fit(g2, monsterName, bw - 36), x + 16, y + 24);
        paintHpBar(g2, x + 16, y + 34, bw - 40, shownMonsterHp / monsterMaxHp);
    }

    private void paintPlayerBox(Graphics2D g2, int x, int y, int bw, int bh) {
        paintBox(g2, x, y, bw, bh, true);
        int left = x + 22, right = x + bw - 14;

        g2.setFont(Theme.dosFont(Font.BOLD, 17f));
        g2.setColor(INK);
        String lv = "Lv." + level;
        Font lvFont = Theme.dosFont(Font.BOLD, 15f);
        int lvW = g2.getFontMetrics(lvFont).stringWidth(lv);
        g2.drawString(fit(g2, playerName, right - left - lvW - 10), left, y + 24);
        g2.setFont(lvFont);
        g2.drawString(lv, right - lvW, y + 24);

        int barW = right - left;
        paintHpBar(g2, left, y + 32, barW, shownHp / maxHp);

        // HP numbers under the bar on the right; job name on the left of the same line.
        g2.setFont(Theme.dosFont(Font.PLAIN, 12f));
        g2.setColor(INK_SOFT);
        g2.drawString(jobName, left, y + 58);
        g2.setFont(Theme.dosFont(Font.BOLD, 15f));
        g2.setColor(INK);
        String nums = Math.round(shownHp) + " / " + maxHp;
        g2.drawString(nums, right - g2.getFontMetrics().stringWidth(nums), y + 59);

        // MP: a slimmer blue bar with its own tag and numbers.
        int mpY = y + 68;
        paintTag(g2, left, mpY, "MP", MP_FILL);
        g2.setFont(Theme.dosFont(Font.PLAIN, 12f));
        String mpNums = mp + "/" + maxMp;
        int mpNumW = g2.getFontMetrics().stringWidth(mpNums);
        int mpBarX = left + 30;
        int mpBarW = barW - 30 - mpNumW - 8;
        paintBar(g2, mpBarX, mpY + 2, mpBarW, 7, shownMp / maxMp, MP_FILL);
        g2.setColor(INK);
        g2.drawString(mpNums, right - mpNumW, mpY + 10);

        // EXP strip along the bottom edge of the box.
        int expY = y + bh - 15;
        g2.setFont(Theme.dosFont(Font.BOLD, 10f));
        g2.setColor(INK_SOFT);
        g2.drawString("EXP", left, expY + 7);
        paintBar(g2, left + 30, expY, barW - 30, 6, shownExp / expMax, EXP_FILL);
    }

    /** Cream panel with a dark rim and a darker shaded underside; playerSide slants the left edge instead of the right. */
    private static void paintBox(Graphics2D g2, int x, int y, int bw, int bh, boolean playerSide) {
        int slant = 14;
        Path2D.Float p = new Path2D.Float();
        if (playerSide) {
            p.moveTo(x + slant, y);
            p.lineTo(x + bw, y);
            p.lineTo(x + bw, y + bh);
            p.lineTo(x, y + bh);
        } else {
            p.moveTo(x, y);
            p.lineTo(x + bw, y);
            p.lineTo(x + bw - slant, y + bh);
            p.lineTo(x, y + bh);
        }
        p.closePath();

        // Drop shadow so the box lifts off busy backdrops.
        Graphics2D gs = (Graphics2D) g2.create();
        gs.translate(3, 4);
        gs.setColor(new Color(0, 0, 0, 70));
        gs.fill(p);
        gs.dispose();

        g2.setPaint(new GradientPaint(0, y, BOX_FILL, 0, y + bh, BOX_SHADE));
        g2.fill(p);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(BOX_EDGE);
        g2.draw(p);
        g2.setStroke(new BasicStroke(1f));
    }

    /** "HP" tag plus a bar that goes green → yellow → red. */
    private static void paintHpBar(Graphics2D g2, int x, int y, int w, float ratio) {
        paintTag(g2, x, y, "HP", HP_TAG);
        Color fill = ratio > 0.5f ? HP_HIGH : ratio > 0.2f ? HP_MID : HP_LOW;
        paintBar(g2, x + 30, y + 1, w - 30, 10, ratio, fill);
    }

    private static void paintTag(Graphics2D g2, int x, int y, String text, Color color) {
        g2.setColor(TRACK);
        g2.fill(new RoundRectangle2D.Float(x, y, 28, 12, 6, 6));
        g2.setFont(Theme.dosFont(Font.BOLD, 11f));
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x + (28 - fm.stringWidth(text)) / 2, y + 10);
    }

    private static void paintBar(Graphics2D g2, int x, int y, int w, int h, float ratio, Color fill) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        g2.setColor(TRACK);
        g2.fill(new RoundRectangle2D.Float(x - 2, y - 2, w + 4, h + 4, h + 4, h + 4));
        g2.setColor(new Color(236, 236, 228));
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
        int fw = Math.round(w * ratio);
        if (fw > 0) {
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(x, y, fw, h, h, h));
            // Light upper band gives the bar the glossy two-tone look.
            g2.setColor(new Color(255, 255, 255, 90));
            g2.fill(new RoundRectangle2D.Float(x, y, fw, h / 2f, h / 2f, h / 2f));
        }
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
