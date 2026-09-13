package game.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Animated multi-layer dragon scene used behind the town screen: the whole dragon (wings +
 * body, kept rigidly together so the wings never separate from the body) gently bobs, and the
 * fire breath pulses. Sky/ruins/foreground stay static so the moving parts read clearly.
 */
public class DragonBackgroundPanel extends JPanel {
    private final BufferedImage sky = loadImage("images/bg/01-sky.png");
    private final BufferedImage wingFar = loadImage("images/bg/02a-wing-far.png");
    private final BufferedImage body = loadImage("images/bg/02b-body.png");
    private final BufferedImage wingNear = loadImage("images/bg/02c-wing-near.png");
    private final BufferedImage ruins = loadImage("images/bg/03-ruins.png");
    private final BufferedImage foreground = loadImage("images/bg/04-foreground.png");
    private final BufferedImage breath = loadImage("images/bg/05-breath.png");

    private final long startTime = System.currentTimeMillis();

    public DragonBackgroundPanel(LayoutManager layout) {
        super(layout);
        setOpaque(true);
        setBackground(Theme.BG);
        new Timer(50, e -> repaint()).start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sky == null) return;

        int pw = getWidth(), ph = getHeight();
        int iw = sky.getWidth(), ih = sky.getHeight();
        double scale = Math.max(pw / (double) iw, ph / (double) ih);
        int ox = (int) Math.round((pw - iw * scale) / 2.0);
        int oy = (int) Math.round((ph - ih * scale) / 2.0);

        double t = (System.currentTimeMillis() - startTime) / 1000.0;
        // The whole dragon (wings + body) moves as one rigid group so the wings never separate
        // from the body's silhouette; only the free-floating fire breath animates independently.
        int bob = (int) Math.round(Math.sin(t * 1.1) * 4);
        float breathAlpha = (float) (0.72 + 0.28 * Math.sin(t * 5.0));

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.translate(ox, oy);
        g2.scale(scale, scale);

        draw(g2, sky, 0, 0);
        draw(g2, wingFar, 0, bob);
        draw(g2, body, 0, bob);
        draw(g2, wingNear, 0, bob);
        draw(g2, ruins, 0, 0);
        draw(g2, foreground, 0, 0);
        drawWithAlpha(g2, breath, 0, bob, breathAlpha);

        g2.dispose();
    }

    private void draw(Graphics2D g2, BufferedImage img, int dx, int dy) {
        if (img != null) g2.drawImage(img, dx, dy, null);
    }

    private void drawWithAlpha(Graphics2D g2, BufferedImage img, int dx, int dy, float alpha) {
        if (img == null) return;
        Graphics2D ga = (Graphics2D) g2.create();
        ga.translate(dx, dy);
        ga.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        ga.drawImage(img, 0, 0, null);
        ga.dispose();
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream in = DragonBackgroundPanel.class.getResourceAsStream(path)) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {
            // Missing/unreadable resource: paintComponent falls back to a flat background.
        }
        return null;
    }
}
