package game.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Draws the player and monster in a Pokémon-style diagonal camera: the player is seen
 * from behind, up close in the bottom-left foreground; the monster faces the viewer,
 * smaller and further away in the top-right background. Each stands on its own platform.
 */
public class BattleStage extends JComponent {
    private static final int LUNGE = 40;
    private static final Color PLAYER_PLATFORM = new Color(50, 68, 48);
    private static final Color MONSTER_PLATFORM = new Color(54, 58, 68);
    private static final double PLAYER_SCALE = 1.15;
    private static final double MONSTER_SCALE = 0.72;

    private static final long DAMAGE_TEXT_DURATION = 750;
    private static final long LEVEL_UP_DURATION = 2400;
    private static final int LEVEL_UP_STREAKS = 14;
    private static final int LEVEL_UP_TWINKLES = 12;
    private static final String LEVEL_UP_TEXT = "LEVEL UP!";
    private static final long LETTER_STAGGER = 55;   // ms between each letter dropping in
    private static final long LETTER_DROP = 320;     // ms for one letter's drop + bounce

    private String monsterName = "";
    private int playerOffsetX = 0;
    private int monsterOffsetX = 0;
    private float playerHitFlash = 0f;
    private float monsterHitFlash = 0f;
    private long startTime = System.currentTimeMillis();
    private Timer idleTimer;
    private final List<DamageText> damageTexts = new ArrayList<>();
    private LevelUpFx levelUpFx;

    private static final class DamageText {
        final String text;
        final boolean onMonster;
        final boolean crit;
        final long start = System.currentTimeMillis();

        DamageText(String text, boolean onMonster, boolean crit) {
            this.text = text;
            this.onMonster = onMonster;
            this.crit = crit;
        }
    }

    /** A thin golden light streak shooting up from the player's feet. */
    private static final class Streak {
        final double xOff;     // horizontal offset from the player's center, as a fraction of body width
        final long delay;      // ms after the effect starts before this streak launches
        final double speed;    // px per ms
        final int length;

        Streak(double xOff, long delay, double speed, int length) {
            this.xOff = xOff;
            this.delay = delay;
            this.speed = speed;
            this.length = length;
        }
    }

    /** A four-pointed twinkle star that drifts up around the player's body. */
    private static final class Twinkle {
        final double xOff;     // fraction of body width from center
        final double yFrac;    // 0 = feet, 1 = head
        final double phase;
        final float size;

        Twinkle(double xOff, double yFrac, double phase, float size) {
            this.xOff = xOff;
            this.yFrac = yFrac;
            this.phase = phase;
            this.size = size;
        }
    }

    /** Old-MapleStory-style level-up: light pillar, rising streaks, twinkles and bouncing "LEVEL UP!" letters. Randomness is rolled once so playback is stable. */
    private static final class LevelUpFx {
        final long start = System.currentTimeMillis();
        final Streak[] streaks = new Streak[LEVEL_UP_STREAKS];
        final Twinkle[] twinkles = new Twinkle[LEVEL_UP_TWINKLES];

        LevelUpFx() {
            java.util.Random r = new java.util.Random();
            for (int i = 0; i < streaks.length; i++) {
                streaks[i] = new Streak((r.nextDouble() - 0.5) * 1.1, r.nextInt(900),
                        0.35 + r.nextDouble() * 0.3, 28 + r.nextInt(40));
            }
            for (int i = 0; i < twinkles.length; i++) {
                twinkles[i] = new Twinkle((r.nextDouble() - 0.5) * 1.5, r.nextDouble(),
                        r.nextDouble() * Math.PI * 2, 5 + r.nextFloat() * 6);
            }
        }
    }

    public BattleStage() {
        setOpaque(false);
        setPreferredSize(new Dimension(600, 220));
    }

    public void setMonsterName(String name) {
        this.monsterName = name;
        playerOffsetX = 0;
        monsterOffsetX = 0;
        playerHitFlash = 0f;
        monsterHitFlash = 0f;
        repaint();
    }

    public void startIdle() {
        if (idleTimer != null) idleTimer.stop();
        startTime = System.currentTimeMillis();
        idleTimer = new Timer(60, e -> repaint());
        idleTimer.start();
    }

    public void stopIdle() {
        if (idleTimer != null) {
            idleTimer.stop();
            idleTimer = null;
        }
    }

    /** Spawns a floating damage number over the player (onMonster=false) or monster (onMonster=true). */
    public void showDamage(int amount, boolean onMonster, boolean crit) {
        damageTexts.add(new DamageText(String.valueOf(amount), onMonster, crit));
        repaint();
    }

    /** Plays the old-MapleStory-style level-up effect over the player. */
    public void playLevelUp() {
        levelUpFx = new LevelUpFx();
        repaint();
    }

    /** Animates a lunge attack. onImpact fires at the peak of the lunge (apply damage there); onDone fires after the return. */
    public void animateAttack(boolean playerAttacking, Runnable onImpact, Runnable onDone) {
        final int frames = 12;
        final int half = frames / 2;
        final int[] frame = {0};
        final Timer[] holder = new Timer[1];
        Timer timer = new Timer(18, null);
        timer.addActionListener(e -> {
            frame[0]++;
            int f = frame[0];
            float progress = f <= half ? f / (float) half : 1 - (f - half) / (float) half;
            int off = Math.round(progress * LUNGE);
            if (playerAttacking) playerOffsetX = off;
            else monsterOffsetX = -off;

            if (f == half) {
                if (playerAttacking) monsterHitFlash = 1f;
                else playerHitFlash = 1f;
                if (onImpact != null) onImpact.run();
            }
            repaint();
            if (f >= frames) {
                holder[0].stop();
                playerOffsetX = 0;
                monsterOffsetX = 0;
                if (onDone != null) onDone.run();
            }
        });
        holder[0] = timer;
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        int playerGroundY = h - 26;
        int playerX = 150;
        int monsterGroundY = (int) Math.round(h * 0.42);
        int monsterX = Math.max(playerX + 230, w - 140);

        drawPlatform(g2, playerX, playerGroundY, 200, 40, PLAYER_PLATFORM);
        drawPlatform(g2, monsterX, monsterGroundY, 130, 26, MONSTER_PLATFORM);

        double t = (System.currentTimeMillis() - startTime) / 1000.0;
        int bob = (int) Math.round(Math.sin(t * 2.4) * 3);

        int px = playerX + playerOffsetX;
        int py = playerGroundY + bob;
        int mx = monsterX - monsterOffsetX;
        int my = monsterGroundY - bob;

        Graphics2D gp = (Graphics2D) g2.create();
        gp.translate(px, py);
        gp.scale(PLAYER_SCALE, PLAYER_SCALE);
        drawPlayer(gp, 0, 0);
        gp.dispose();
        if (playerHitFlash > 0) drawFlash(g2, px, playerGroundY, 56, playerHitFlash);

        if (monsterName != null && !monsterName.isEmpty()) {
            Graphics2D gm = (Graphics2D) g2.create();
            gm.translate(mx, my);
            gm.scale(MONSTER_SCALE, MONSTER_SCALE);
            drawMonster(gm, monsterName, 0, 0);
            gm.dispose();
            if (monsterHitFlash > 0) drawFlash(g2, mx, monsterGroundY, 44, monsterHitFlash);
        }

        if (playerHitFlash > 0) playerHitFlash = Math.max(0, playerHitFlash - 0.1f);
        if (monsterHitFlash > 0) monsterHitFlash = Math.max(0, monsterHitFlash - 0.1f);

        drawDamageTexts(g2, px, playerGroundY, mx, monsterGroundY);
        drawLevelUp(g2, px, playerGroundY);

        g2.dispose();
    }

    /**
     * Draws the old-MapleStory-style level-up effect centered on the player: a golden light
     * pillar bursting up from the feet, thin light streaks shooting upward, twinkling stars
     * around the body, and "LEVEL UP!" letters dropping in one by one above the head.
     */
    private void drawLevelUp(Graphics2D g2, int playerCx, int playerGroundY) {
        if (levelUpFx == null) return;
        long elapsed = System.currentTimeMillis() - levelUpFx.start;
        if (elapsed >= LEVEL_UP_DURATION) {
            levelUpFx = null;
            return;
        }
        float t = elapsed / (float) LEVEL_UP_DURATION;
        float fadeOut = t > 0.75f ? Math.max(0f, 1f - (t - 0.75f) / 0.25f) : 1f;

        double spriteScale = PLAYER_SPRITE != null ? PLAYER_SPRITE_HEIGHT / PLAYER_SPRITE.getHeight() : 1.0;
        int bodyH = (int) Math.round(PLAYER_SPRITE_HEIGHT * PLAYER_SCALE);
        int bodyW = PLAYER_SPRITE != null
                ? (int) Math.round(PLAYER_SPRITE.getWidth() * spriteScale * PLAYER_SCALE)
                : (int) Math.round(bodyH * 0.6);
        int bodyTop = playerGroundY - bodyH;

        // 1) Light pillar: shoots up to the top of the stage in the first 200ms, then slowly thins out.
        float grow = Math.min(1f, elapsed / 200f);
        float pillarAlpha = (t < 0.5f ? 1f : Math.max(0f, 1f - (t - 0.5f) / 0.35f));
        if (pillarAlpha > 0) {
            int pillarTop = Math.round(playerGroundY - playerGroundY * grow);
            int pillarH = playerGroundY - pillarTop;
            // Narrows over time, like the beam collapsing back into the character.
            float widthFactor = 1f - 0.45f * Math.max(0f, (t - 0.3f) / 0.5f);
            int[] widths = {Math.round(bodyW * 1.1f * widthFactor), Math.round(bodyW * 0.7f * widthFactor),
                    Math.round(bodyW * 0.32f * widthFactor)};
            Color[] cores = {new Color(255, 214, 90), new Color(255, 236, 150), new Color(255, 252, 225)};
            int[] alphas = {70, 90, 150};
            for (int i = 0; i < widths.length; i++) {
                int a = Math.round(alphas[i] * pillarAlpha);
                if (a <= 0 || widths[i] <= 0 || pillarH <= 0) continue;
                Color base = cores[i];
                g2.setPaint(new GradientPaint(0, playerGroundY, new Color(base.getRed(), base.getGreen(), base.getBlue(), a),
                        0, pillarTop, new Color(base.getRed(), base.getGreen(), base.getBlue(), 0)));
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(playerCx - widths[i] / 2f, pillarTop,
                        widths[i], pillarH, widths[i], widths[i]));
            }
        }

        // 2) Burst ring at the feet on the first beat.
        if (elapsed < 600) {
            float rp = elapsed / 600f;
            int rx = Math.round(bodyW * 0.4f + rp * bodyW * 0.9f);
            int ry = Math.max(4, rx / 4);
            g2.setStroke(new BasicStroke(3f));
            g2.setColor(new Color(255, 230, 120, Math.round(220 * (1 - rp))));
            g2.drawOval(playerCx - rx, playerGroundY - ry, rx * 2, ry * 2);
        }

        // 3) Thin light streaks racing upward from the feet.
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (Streak s : levelUpFx.streaks) {
            long se = elapsed - s.delay;
            if (se < 0) continue;
            int x = playerCx + (int) Math.round(s.xOff * bodyW);
            int headY = playerGroundY - (int) Math.round(se * s.speed);
            int tailY = headY + s.length;
            if (tailY < 0) continue;
            int a = Math.round(230 * fadeOut);
            if (a <= 0) continue;
            g2.setPaint(new GradientPaint(0, headY, new Color(255, 250, 215, a),
                    0, tailY, new Color(255, 200, 70, 0)));
            g2.drawLine(x, headY, x, Math.min(tailY, playerGroundY));
        }

        // 4) Twinkling four-pointed stars around the body, drifting upward.
        for (Twinkle tw : levelUpFx.twinkles) {
            double twinkle = Math.abs(Math.sin(elapsed / 110.0 + tw.phase));
            float size = (float) (tw.size * (0.35 + 0.65 * twinkle));
            int sx = playerCx + (int) Math.round(tw.xOff * bodyW);
            int sy = playerGroundY - (int) Math.round(tw.yFrac * bodyH) - Math.round(elapsed * 0.03f);
            int a = Math.round(255 * fadeOut * (float) (0.4 + 0.6 * twinkle));
            if (a <= 0) continue;
            g2.setColor(new Color(255, 245, 190, a));
            g2.fill(starShape(sx, sy, size));
        }

        // 5) "LEVEL UP!" letters drop in one by one with a bounce, above the player's head.
        Font font = Theme.dosFont(Font.BOLD, 32f);
        java.awt.font.FontRenderContext frc = g2.getFontRenderContext();
        java.awt.font.GlyphVector whole = font.createGlyphVector(frc, LEVEL_UP_TEXT);
        java.awt.geom.Rectangle2D wb = whole.getVisualBounds();
        float textAlpha = fadeOut;
        // Float up a little while fading out, like the original.
        int lift = t > 0.75f ? Math.round((t - 0.75f) / 0.25f * 18) : 0;
        int baseY = Math.max((int) Math.ceil(wb.getHeight()) + 8, bodyTop - 6) - lift;
        float startX = (float) (playerCx - wb.getWidth() / 2 - wb.getX());

        Graphics2D gt = (Graphics2D) g2.create();
        gt.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
        for (int i = 0; i < LEVEL_UP_TEXT.length(); i++) {
            long le = elapsed - i * LETTER_STAGGER;
            if (le < 0) break;
            float p = Math.min(1f, le / (float) LETTER_DROP);
            float dropOffset = (1f - easeOutBack(p)) * -36f;
            Shape glyph = whole.getGlyphOutline(i, startX, baseY + dropOffset);
            if (glyph.getBounds().isEmpty()) continue;
            java.awt.geom.Rectangle2D gb = glyph.getBounds2D();

            gt.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gt.setColor(new Color(92, 38, 8));
            gt.draw(glyph);
            gt.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            gt.setColor(new Color(255, 250, 220));
            gt.draw(glyph);
            gt.setPaint(new GradientPaint(0, (float) gb.getMinY(), new Color(255, 246, 130),
                    0, (float) gb.getMaxY(), new Color(255, 140, 20)));
            gt.fill(glyph);
        }
        gt.dispose();
        repaint();
    }

    /** A four-pointed sparkle star centered at (cx, cy). */
    private static Shape starShape(float cx, float cy, float r) {
        float inner = r * 0.28f;
        Path2D.Float p = new Path2D.Float();
        p.moveTo(cx, cy - r);
        p.lineTo(cx + inner, cy - inner);
        p.lineTo(cx + r, cy);
        p.lineTo(cx + inner, cy + inner);
        p.lineTo(cx, cy + r);
        p.lineTo(cx - inner, cy + inner);
        p.lineTo(cx - r, cy);
        p.lineTo(cx - inner, cy - inner);
        p.closePath();
        return p;
    }

    /** Overshoots slightly past 1 then settles, giving each dropped letter a small bounce. */
    private static float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1;
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    /** Draws and expires floating damage numbers that rise and fade above whichever side took the hit. */
    private void drawDamageTexts(Graphics2D g2, int playerCx, int playerGroundY, int monsterCx, int monsterGroundY) {
        if (damageTexts.isEmpty()) return;
        long now = System.currentTimeMillis();
        Iterator<DamageText> it = damageTexts.iterator();
        while (it.hasNext()) {
            DamageText dt = it.next();
            long elapsed = now - dt.start;
            if (elapsed >= DAMAGE_TEXT_DURATION) {
                it.remove();
                continue;
            }
            float progress = elapsed / (float) DAMAGE_TEXT_DURATION;
            int cx = dt.onMonster ? monsterCx : playerCx;
            int baseY = (dt.onMonster ? monsterGroundY : playerGroundY) - 70;
            int y = baseY - Math.round(progress * 34);
            int alpha = Math.round(255 * (1 - progress));

            Font font = Theme.dosFont(Font.BOLD, dt.crit ? 24f : 18f);
            g2.setFont(font);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(dt.text);

            g2.setColor(new Color(0, 0, 0, Math.round(alpha * 0.55f)));
            g2.drawString(dt.text, cx - tw / 2 + 2, y + 2);

            Color color = dt.crit ? new Color(255, 200, 60, alpha) : new Color(255, 255, 255, alpha);
            g2.setColor(color);
            g2.drawString(dt.text, cx - tw / 2, y);
        }
        repaint();
    }

    private void drawPlatform(Graphics2D g2, int cx, int groundY, int width, int height, Color fill) {
        g2.setColor(fill);
        g2.fillOval(cx - width / 2, groundY - height / 2, width, height);
        g2.setColor(fill.darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(cx - width / 2, groundY - height / 2, width, height);
        g2.setStroke(new BasicStroke(1));
    }

    private void drawFlash(Graphics2D g2, int cx, int groundY, int r, float strength) {
        g2.setColor(new Color(220, 70, 70, Math.round(150 * strength)));
        g2.fillOval(cx - r / 2, groundY - r - 20, r, r);
    }

    // ---------- player (seen from behind, close-up foreground) ----------
    private static final BufferedImage PLAYER_SPRITE = loadImage("images/player.png");
    private static final double PLAYER_SPRITE_HEIGHT = 150.0;

    private void drawPlayer(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - 26, groundY - 6, 52, 10);

        if (PLAYER_SPRITE == null) return;
        double scale = PLAYER_SPRITE_HEIGHT / PLAYER_SPRITE.getHeight();
        int dw = (int) Math.round(PLAYER_SPRITE.getWidth() * scale);
        int dh = (int) Math.round(PLAYER_SPRITE.getHeight() * scale);
        Graphics2D gi = (Graphics2D) g2.create();
        gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        gi.drawImage(PLAYER_SPRITE, x - dw / 2, groundY - dh, dw, dh, null);
        gi.dispose();
    }

    /** Loads a bundled resource image from the classpath, relative to this class's package. */
    private static BufferedImage loadImage(String path) {
        try (InputStream in = BattleStage.class.getResourceAsStream(path)) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {
            // Missing/unreadable resource: caller falls back to no sprite drawn.
        }
        return null;
    }

    // ---------- monster dispatch ----------
    private void drawMonster(Graphics2D g2, String name, int x, int groundY) {
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillOval(x - 26, groundY - 6, 52, 10);

        if (name.contains("마왕")) drawDemonLord(g2, x, groundY);
        else if (name.contains("리치")) drawLich(g2, x, groundY);
        else if (name.contains("마법사")) drawMage(g2, x, groundY);
        else if (name.contains("기사")) drawKnight(g2, x, groundY);
        else if (name.contains("망령")) drawGhost(g2, x, groundY);
        else if (name.contains("해골")) drawSkeleton(g2, x, groundY);
        else if (name.contains("오크")) drawOrc(g2, x, groundY);
        else if (name.contains("고블린")) drawGoblin(g2, x, groundY);
        else if (name.contains("박쥐")) drawBat(g2, x, groundY);
        else if (name.contains("늑대")) drawWolf(g2, x, groundY);
        else if (name.contains("멧돼지")) drawBoar(g2, x, groundY);
        else if (name.contains("들쥐")) drawRat(g2, x, groundY);
        else drawGenericBlob(g2, x, groundY, name);
    }

    private void drawRat(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(120, 100, 90));
        g2.fillOval(x - 20, groundY - 26, 40, 24);
        g2.fillOval(x - 26, groundY - 22, 14, 14);
        g2.setColor(new Color(150, 128, 116));
        g2.fillOval(x + 6, groundY - 34, 12, 12);
        g2.fillOval(x + 16, groundY - 34, 12, 12);
        g2.setColor(new Color(40, 40, 46));
        g2.fillOval(x - 22, groundY - 20, 3, 3);
        g2.setStroke(new BasicStroke(2));
        g2.setColor(new Color(190, 160, 150));
        g2.drawLine(x - 26, groundY - 10, x - 40, groundY - 4);
    }

    private void drawBoar(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(96, 74, 58));
        g2.fillRoundRect(x - 26, groundY - 36, 52, 32, 16, 16);
        g2.fillOval(x - 30, groundY - 32, 18, 18);
        g2.setColor(Color.WHITE);
        g2.fillOval(x - 30, groundY - 20, 6, 8);
        g2.fillOval(x - 24, groundY - 20, 6, 8);
        g2.setColor(new Color(40, 40, 46));
        g2.fillOval(x - 26, groundY - 26, 3, 3);
        g2.setColor(new Color(60, 46, 36));
        g2.fillRect(x - 4, groundY - 6, 8, 10);
        g2.fillRect(x + 12, groundY - 6, 8, 10);
    }

    private void drawWolf(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(120, 122, 130));
        g2.fillRoundRect(x - 22, groundY - 44, 46, 34, 14, 14);
        Path2D ears = new Path2D.Double();
        ears.moveTo(x - 18, groundY - 40);
        ears.lineTo(x - 12, groundY - 56);
        ears.lineTo(x - 4, groundY - 40);
        ears.closePath();
        g2.fill(ears);
        g2.setColor(new Color(150, 152, 160));
        g2.fillOval(x - 28, groundY - 34, 18, 16);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 26, groundY - 30, 3, 3);
        g2.fillRect(x - 6, groundY - 8, 8, 10);
        g2.fillRect(x + 10, groundY - 8, 8, 10);
    }

    private void drawBat(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(70, 54, 90));
        Path2D wingL = new Path2D.Double();
        wingL.moveTo(x - 6, groundY - 34);
        wingL.lineTo(x - 40, groundY - 46);
        wingL.lineTo(x - 26, groundY - 24);
        wingL.closePath();
        g2.fill(wingL);
        Path2D wingR = new Path2D.Double();
        wingR.moveTo(x + 6, groundY - 34);
        wingR.lineTo(x + 40, groundY - 46);
        wingR.lineTo(x + 26, groundY - 24);
        wingR.closePath();
        g2.fill(wingR);
        g2.fillOval(x - 12, groundY - 38, 24, 22);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 6, groundY - 30, 3, 3);
        g2.fillOval(x + 3, groundY - 30, 3, 3);
    }

    private void drawGoblin(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(86, 140, 74));
        g2.fillOval(x - 15, groundY - 46, 30, 28);
        Path2D earL = new Path2D.Double();
        earL.moveTo(x - 15, groundY - 36);
        earL.lineTo(x - 28, groundY - 40);
        earL.lineTo(x - 15, groundY - 28);
        earL.closePath();
        g2.fill(earL);
        Path2D earR = new Path2D.Double();
        earR.moveTo(x + 15, groundY - 36);
        earR.lineTo(x + 28, groundY - 40);
        earR.lineTo(x + 15, groundY - 28);
        earR.closePath();
        g2.fill(earR);
        g2.setColor(new Color(60, 100, 50));
        g2.fillRoundRect(x - 16, groundY - 20, 32, 20, 10, 10);
        g2.setColor(new Color(220, 220, 60));
        g2.fillOval(x - 8, groundY - 36, 5, 5);
        g2.fillOval(x + 3, groundY - 36, 5, 5);
        g2.setColor(new Color(150, 150, 160));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(x + 18, groundY - 18, x + 34, groundY - 34);
    }

    private void drawOrc(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(58, 96, 56));
        g2.fillRoundRect(x - 24, groundY - 60, 48, 40, 12, 12);
        g2.fillOval(x - 18, groundY - 54, 36, 30);
        g2.setColor(Color.WHITE);
        g2.fillRect(x - 10, groundY - 34, 5, 8);
        g2.fillRect(x + 5, groundY - 34, 5, 8);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 10, groundY - 44, 4, 4);
        g2.fillOval(x + 6, groundY - 44, 4, 4);
        g2.setColor(new Color(90, 90, 96));
        g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 22, groundY - 40, x + 40, groundY - 58);
        g2.fillRect(x + 34, groundY - 66, 14, 8);
    }

    private void drawSkeleton(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(224, 220, 205));
        g2.fillOval(x - 14, groundY - 52, 28, 26);
        g2.setColor(new Color(30, 30, 34));
        g2.fillOval(x - 8, groundY - 44, 5, 6);
        g2.fillOval(x + 3, groundY - 44, 5, 6);
        g2.fillRect(x - 3, groundY - 32, 6, 3);
        g2.setColor(new Color(224, 220, 205));
        g2.fillRoundRect(x - 12, groundY - 26, 24, 20, 6, 6);
        g2.setStroke(new BasicStroke(2));
        for (int i = 0; i < 3; i++) g2.drawLine(x - 10, groundY - 22 + i * 5, x + 10, groundY - 22 + i * 5);
    }

    private void drawGhost(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(150, 190, 220, 190));
        Path2D body = new Path2D.Double();
        body.moveTo(x - 20, groundY - 8);
        body.curveTo(x - 26, groundY - 50, x - 10, groundY - 60, x, groundY - 60);
        body.curveTo(x + 10, groundY - 60, x + 26, groundY - 50, x + 20, groundY - 8);
        body.lineTo(x + 12, groundY - 16);
        body.lineTo(x + 4, groundY - 8);
        body.lineTo(x - 4, groundY - 16);
        body.lineTo(x - 12, groundY - 8);
        body.closePath();
        g2.fill(body);
        g2.setColor(new Color(40, 40, 60));
        g2.fillOval(x - 9, groundY - 44, 6, 8);
        g2.fillOval(x + 3, groundY - 44, 6, 8);
    }

    private void drawLich(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(78, 58, 110));
        Path2D robe = new Path2D.Double();
        robe.moveTo(x - 22, groundY - 4);
        robe.lineTo(x - 16, groundY - 60);
        robe.lineTo(x + 16, groundY - 60);
        robe.lineTo(x + 22, groundY - 4);
        robe.closePath();
        g2.fill(robe);
        g2.setColor(new Color(224, 220, 205));
        g2.fillOval(x - 12, groundY - 74, 24, 22);
        g2.setColor(new Color(150, 90, 230));
        g2.fillOval(x - 7, groundY - 66, 4, 4);
        g2.fillOval(x + 3, groundY - 66, 4, 4);
        g2.setColor(new Color(150, 90, 230));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(x + 20, groundY - 6, x + 30, groundY - 66);
        g2.fillOval(x + 24, groundY - 72, 10, 10);
    }

    private void drawKnight(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(50, 54, 64));
        g2.fillRoundRect(x - 18, groundY - 52, 36, 38, 10, 10);
        g2.fillRoundRect(x - 13, groundY - 70, 26, 22, 8, 8);
        g2.setColor(new Color(120, 20, 30));
        g2.fillRect(x - 3, groundY - 78, 6, 10);
        g2.setColor(new Color(220, 60, 60));
        g2.fillRect(x - 9, groundY - 62, 18, 4);
        g2.setColor(new Color(150, 150, 160));
        g2.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 16, groundY - 42, x + 34, groundY - 66);
    }

    private void drawMage(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(60, 74, 130));
        Path2D robe = new Path2D.Double();
        robe.moveTo(x - 22, groundY - 4);
        robe.lineTo(x - 14, groundY - 56);
        robe.lineTo(x + 14, groundY - 56);
        robe.lineTo(x + 22, groundY - 4);
        robe.closePath();
        g2.fill(robe);
        g2.setColor(new Color(235, 205, 170));
        g2.fillOval(x - 11, groundY - 68, 22, 20);
        g2.setColor(new Color(40, 60, 120));
        Path2D hat = new Path2D.Double();
        hat.moveTo(x - 16, groundY - 60);
        hat.lineTo(x + 2, groundY - 92);
        hat.lineTo(x + 18, groundY - 60);
        hat.closePath();
        g2.fill(hat);
        g2.setColor(new Color(120, 170, 255));
        g2.setStroke(new BasicStroke(3));
        g2.drawLine(x + 20, groundY - 4, x + 30, groundY - 60);
        g2.fillOval(x + 24, groundY - 68, 10, 10);
    }

    private void drawDemonLord(Graphics2D g2, int x, int groundY) {
        g2.setColor(new Color(120, 30, 34));
        Path2D wingL = new Path2D.Double();
        wingL.moveTo(x - 18, groundY - 60);
        wingL.lineTo(x - 56, groundY - 78);
        wingL.lineTo(x - 40, groundY - 40);
        wingL.closePath();
        g2.fill(wingL);
        Path2D wingR = new Path2D.Double();
        wingR.moveTo(x + 18, groundY - 60);
        wingR.lineTo(x + 56, groundY - 78);
        wingR.lineTo(x + 40, groundY - 40);
        wingR.closePath();
        g2.fill(wingR);

        g2.setColor(new Color(150, 34, 40));
        g2.fillRoundRect(x - 22, groundY - 66, 44, 46, 14, 14);
        g2.fillOval(x - 16, groundY - 88, 32, 30);

        g2.setColor(new Color(20, 20, 24));
        Path2D hornL = new Path2D.Double();
        hornL.moveTo(x - 12, groundY - 86);
        hornL.lineTo(x - 24, groundY - 108);
        hornL.lineTo(x - 6, groundY - 92);
        hornL.closePath();
        g2.fill(hornL);
        Path2D hornR = new Path2D.Double();
        hornR.moveTo(x + 12, groundY - 86);
        hornR.lineTo(x + 24, groundY - 108);
        hornR.lineTo(x + 6, groundY - 92);
        hornR.closePath();
        g2.fill(hornR);

        g2.setColor(new Color(255, 210, 60));
        g2.fillOval(x - 9, groundY - 76, 5, 5);
        g2.fillOval(x + 4, groundY - 76, 5, 5);
    }

    private void drawGenericBlob(Graphics2D g2, int x, int groundY, String name) {
        int hash = name.hashCode();
        Color c = new Color(90 + Math.abs(hash) % 120, 90 + Math.abs(hash / 3) % 120, 90 + Math.abs(hash / 7) % 120);
        g2.setColor(c);
        Ellipse2D body = new Ellipse2D.Double(x - 24, groundY - 46, 48, 42);
        g2.fill(body);
        g2.setColor(new Color(220, 60, 60));
        g2.fillOval(x - 10, groundY - 34, 6, 6);
        g2.fillOval(x + 4, groundY - 34, 6, 6);
    }
}
