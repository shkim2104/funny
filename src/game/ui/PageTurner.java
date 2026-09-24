package game.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;

/**
 * Notebook page-turn transitions, drawn on the frame's glass pane.
 *
 * The page rotates around the left edge (the notebook's spiral) with a bit of perspective, so
 * turning forward lifts the current screen away to reveal the next one, and turning back lays the
 * previous screen down over the current one like closing a page. While a turn is playing the glass
 * pane swallows mouse input so nothing underneath can be clicked mid-animation.
 */
public class PageTurner extends JComponent {
    private static final long DURATION_NANOS = 620_000_000L;
    private static final int STRIPS = 48;
    private static final Color PAPER = new Color(247, 242, 228);
    private static final Color PAPER_LINE = new Color(150, 180, 220, 150);
    private static final Color PAPER_MARGIN = new Color(220, 120, 120, 170);

    private final JFrame frame;
    private Timer timer;

    // Current animation state, read by paintComponent.
    private BufferedImage page;      // the sheet that rotates
    private BufferedImage under;     // what lies beneath it
    private double angle;            // 0 = flat on screen, PI/2 = edge-on (gone)
    private BufferedImage still;     // shown without animation (the blank page behind a dialog)
    private BufferedImage frameBuffer;

    public PageTurner(JFrame frame) {
        this.frame = frame;
        // Opaque: whenever this pane is visible it paints the whole window itself, so Swing must not
        // also repaint every screen underneath it on each animation frame.
        setOpaque(true);
        // Swallow clicks while visible so the screen underneath can't be used mid-turn.
        addMouseListener(new MouseAdapter() {});
        addMouseMotionListener(new MouseAdapter() {});
    }

    /**
     * Swaps the screen (swap runs immediately, before the animation) with a page turn.
     * forward = turn the current page away; otherwise lay the new page back down over it.
     */
    public void turn(Runnable swap, boolean forward) {
        if (!canAnimate()) {
            swap.run();
            return;
        }
        BufferedImage from = snapshot();
        swap.run();
        frame.getContentPane().validate();
        BufferedImage to = snapshot();
        if (forward) {
            animate(from, to, true, this::finish);
        } else {
            animate(to, from, false, this::finish);
        }
    }

    /**
     * Turns to a blank notebook page, runs the (modal) dialog on top of it, then turns back to
     * the refreshed screen once the dialog closes.
     */
    public void turnToBlankPage(Runnable modal) {
        if (!canAnimate()) {
            modal.run();
            return;
        }
        BufferedImage paper = paperImage(frame.getContentPane().getWidth(), frame.getContentPane().getHeight());
        animate(snapshot(), paper, true, () -> {
            still = paper;
            repaint();
            // Leave the timer callback before blocking on the modal dialog.
            SwingUtilities.invokeLater(() -> {
                modal.run();
                still = null;
                animate(snapshot(), paper, false, this::finish);
            });
        });
    }

    private boolean canAnimate() {
        Container c = frame.getContentPane();
        return timer == null && frame.isShowing() && c.getWidth() > 0 && c.getHeight() > 0;
    }

    private void animate(BufferedImage sheet, BufferedImage beneath, boolean forward, Runnable onDone) {
        page = sheet;
        under = beneath;
        angle = forward ? 0 : Math.PI / 2;
        setVisible(true);
        // nanoTime: currentTimeMillis only ticks every ~15ms on Windows, which made frames uneven.
        long start = System.nanoTime();
        timer = new Timer(10, e -> {
            double t = Math.min(1.0, (System.nanoTime() - start) / (double) DURATION_NANOS);
            double eased = t * t * (3 - 2 * t);
            angle = (forward ? eased : 1 - eased) * Math.PI / 2;
            repaint();
            if (t >= 1.0) {
                ((Timer) e.getSource()).stop();
                timer = null;
                page = null;
                under = null;
                onDone.run();
            }
        });
        timer.start();
    }

    private void finish() {
        setVisible(false);
        page = null;
        under = null;
    }

    /**
     * Images compatible with the screen can be cached on the graphics card by Java2D, which makes
     * the per-frame scaled draws of full-window images far cheaper than plain BufferedImages.
     */
    private BufferedImage newImage(int w, int h) {
        GraphicsConfiguration gc = frame.getGraphicsConfiguration();
        return gc != null ? gc.createCompatibleImage(w, h, Transparency.OPAQUE)
                : new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    }

    private BufferedImage snapshot() {
        Container c = frame.getContentPane();
        BufferedImage img = newImage(c.getWidth(), c.getHeight());
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
        return img;
    }

    /** A blank lined notebook page with a red margin and spiral holes along the left edge. */
    private BufferedImage paperImage(int w, int h) {
        BufferedImage img = newImage(w, h);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(PAPER);
        g.fillRect(0, 0, w, h);
        g.setColor(PAPER_LINE);
        for (int y = 90; y < h; y += 30) g.drawLine(0, y, w, y);
        g.setColor(PAPER_MARGIN);
        g.setStroke(new BasicStroke(1.5f));
        g.drawLine(78, 0, 78, h);
        g.setColor(new Color(60, 56, 50));
        for (int y = 30; y < h; y += 44) {
            g.fillOval(22, y, 14, 14);
        }
        g.dispose();
        return img;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (still != null && page == null) {
            g.drawImage(still, 0, 0, null);
            return;
        }
        if (page == null) {
            // Opaque component: never leave the area unpainted.
            g.setColor(Theme.BG);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        // Compose each frame in a reusable in-memory buffer and blit it once: translucent fills
        // (shadow, page shading) are far cheaper on a plain image than on the screen surface.
        int w = page.getWidth(), h = page.getHeight();
        if (frameBuffer == null || frameBuffer.getWidth() != w || frameBuffer.getHeight() != h) {
            frameBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        }
        Graphics2D g2 = frameBuffer.createGraphics();
        // Nearest-neighbor on purpose: without GPU acceleration (common on Windows Java2D) bilinear
        // scaling of a full-window image cost ~50ms per frame vs ~6ms, and the difference isn't
        // visible while the page is moving.
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g2.drawImage(under, 0, 0, null);

        double sin = Math.sin(angle), cos = Math.cos(angle);
        // Perspective: the page's free edge swings toward the viewer, so it grows as it lifts.
        double focal = w * 5.0;
        double[] xs = new double[STRIPS + 1];
        double[] scales = new double[STRIPS + 1];
        for (int i = 0; i <= STRIPS; i++) {
            double d = w * i / (double) STRIPS;
            double s = focal / (focal - d * sin);
            scales[i] = s;
            xs[i] = d * cos * s;
        }
        double edgeX = xs[STRIPS];

        // Shadow the page casts on the sheet beneath, strongest mid-turn.
        int shadowW = (int) Math.round(120 * sin);
        if (shadowW > 0) {
            int alpha = (int) Math.round(90 * Math.sin(angle * 2) + 40 * sin);
            g2.setPaint(new GradientPaint((float) edgeX, 0, new Color(0, 0, 0, alpha),
                    (float) (edgeX + shadowW), 0, new Color(0, 0, 0, 0)));
            g2.fillRect((int) edgeX, 0, shadowW, h);
        }

        // The page itself, drawn as vertical strips so each column gets its own perspective height.
        for (int i = 0; i < STRIPS; i++) {
            int sx0 = (int) Math.round(w * i / (double) STRIPS);
            int sx1 = (int) Math.round(w * (i + 1) / (double) STRIPS);
            int dx0 = (int) Math.floor(xs[i]);
            int dx1 = (int) Math.ceil(xs[i + 1]);
            if (dx1 <= dx0) dx1 = dx0 + 1;
            double s = (scales[i] + scales[i + 1]) / 2;
            int dh = (int) Math.round(h * s);
            int dy = (h - dh) / 2;
            g2.drawImage(page, dx0, dy, dx1, dy + dh, sx0, 0, sx1, h, null);
        }
        // Darken the lifted page. Plain rectangles per strip (edge to edge, never overlapping so no
        // doubled-dark seams) rasterize about twice as fast as one translucent polygon.
        int shade = (int) Math.round(110 * sin);
        if (shade > 0) {
            g2.setColor(new Color(0, 0, 0, shade));
            for (int i = 0; i < STRIPS; i++) {
                int dx0 = (int) Math.round(xs[i]);
                int dx1 = (int) Math.round(xs[i + 1]);
                if (dx1 <= dx0) continue;
                int dh = (int) Math.round(h * (scales[i] + scales[i + 1]) / 2);
                g2.fillRect(dx0, (h - dh) / 2, dx1 - dx0, dh);
            }
        }
        // A thin highlight along the lifting edge reads as paper thickness.
        if (sin > 0.02) {
            g2.setColor(new Color(255, 255, 255, (int) Math.round(120 * sin)));
            g2.setStroke(new BasicStroke(2f));
            double s = scales[STRIPS];
            int dh = (int) Math.round(h * s);
            g2.drawLine((int) edgeX, (h - dh) / 2, (int) edgeX, (h + dh) / 2);
        }
        g2.dispose();
        g.drawImage(frameBuffer, 0, 0, null);
    }
}
