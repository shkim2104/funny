package game.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

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

    private String monsterName = "";
    private int playerOffsetX = 0;
    private int monsterOffsetX = 0;
    private float playerHitFlash = 0f;
    private float monsterHitFlash = 0f;
    private long startTime = System.currentTimeMillis();
    private Timer idleTimer;

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

        g2.dispose();
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
