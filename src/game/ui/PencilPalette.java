package game.ui;

import game.save.Settings;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The colored pencils of the drawing pages: twelve in the box, plus the ones the player mixed with
 * 색 추가 (kept in Settings, so they stay for every drawing). The pencil in hand is circled in red.
 */
final class PencilPalette extends JPanel {
    static final Color[] BOX = {
            new Color(44, 42, 52), new Color(120, 116, 112), new Color(122, 78, 44), new Color(240, 196, 160),
            new Color(214, 58, 50), new Color(238, 134, 40), new Color(244, 200, 50), new Color(66, 156, 76),
            new Color(52, 120, 214), new Color(40, 56, 130), new Color(142, 78, 190), new Color(238, 130, 170)};
    /** Mixed pencils kept; the oldest goes when a new one is added past this. */
    static final int MAX_CUSTOM = 7;
    private static final int COLS = 4, CELL = 42;

    private final Consumer<Color> onPick;
    private Color current = BOX[0];
    private boolean inHand = true;

    PencilPalette(Consumer<Color> onPick) {
        super(new GridLayout(0, COLS, 5, 5));
        this.onPick = onPick;
        setOpaque(false);
        rebuild();
    }

    Color current() {
        return current;
    }

    /** False while the eraser is in hand: no pencil is circled. */
    void setInHand(boolean inHand) {
        this.inHand = inHand;
        repaint();
    }

    private void rebuild() {
        removeAll();
        List<Color> all = new ArrayList<>(List.of(BOX));
        for (int rgb : Settings.getCustomColors()) all.add(new Color(rgb));
        for (Color c : all) add(new Swatch(c));
        JButton mix = new MixButton();
        mix.setToolTipText("색 추가: 원하는 색을 골라 색연필로 만들어요");
        mix.addActionListener(e -> mix());
        add(mix);
        int rows = (all.size() + 1 + COLS - 1) / COLS;
        Dimension d = new Dimension(196, rows * CELL);
        setPreferredSize(d);
        setMaximumSize(new Dimension(Integer.MAX_VALUE, d.height));
        revalidate();
        repaint();
    }

    /** 색 추가: any color, which then stays in the box. */
    private void mix() {
        Color picked = JColorChooser.showDialog(SwingUtilities.getWindowAncestor(this), "색 추가", current);
        if (picked == null) return;
        Settings.addCustomColor(picked.getRGB(), MAX_CUSTOM);
        rebuild();
        pick(picked);
    }

    private void pick(Color c) {
        current = new Color(c.getRGB() & 0xFFFFFF);
        inHand = true;
        repaint();
        onPick.accept(current);
    }

    /** 색 추가: a pencil circle with a plus in it. */
    private static final class MixButton extends JButton {
        MixButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = getWidth(), h = getHeight();
            int r = Math.min(w, h) / 2 - 7;
            Sketch.circle(g2, w / 2.0, h / 2.0, r + 1, 4242, 1.6f, Sketch.INK);
            g2.setColor(Sketch.INK);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(w / 2 - r / 2, h / 2, w / 2 + r / 2, h / 2);
            g2.drawLine(w / 2, h / 2 - r / 2, w / 2, h / 2 + r / 2);
            g2.dispose();
        }
    }

    /** One colored pencil: a dab of its color, circled while it is the one in hand. */
    private final class Swatch extends JButton {
        private final Color pencil;

        Swatch(Color pencil) {
            this.pencil = pencil;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e -> pick(pencil));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = getWidth(), h = getHeight();
            int r = Math.min(w, h) / 2 - 7;
            g2.setColor(pencil);
            g2.fillOval(w / 2 - r, h / 2 - r, r * 2, r * 2);
            boolean on = inHand && (pencil.getRGB() & 0xFFFFFF) == (current.getRGB() & 0xFFFFFF);
            Sketch.circle(g2, w / 2.0, h / 2.0, r + (on ? 4 : 1), pencil.getRGB(), on ? 2.6f : 1.3f, on ? Sketch.RED_PENCIL : Sketch.INK);
            g2.dispose();
        }
    }
}
