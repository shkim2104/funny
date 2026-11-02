package game.ui;

import game.model.Augment;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * This run's augments as a row of small pencil icons ringed in their tier's color. Hovering one shows
 * its name and effect, and clicking any of them runs the optional onClick.
 */
public final class AugmentStrip extends JPanel {
    private final int iconSize;
    private final Runnable onClick;

    public AugmentStrip(int iconSize, int columns, int rows, Runnable onClick) {
        super(new GridLayout(rows, columns, 3, 3));
        this.iconSize = iconSize;
        this.onClick = onClick;
        setOpaque(false);
        int cell = iconSize + 8;
        setPreferredSize(new Dimension(columns * cell + (columns - 1) * 3, rows * cell + (rows - 1) * 3));
    }

    public void set(List<Augment> augments) {
        removeAll();
        int cells = ((GridLayout) getLayout()).getColumns() * ((GridLayout) getLayout()).getRows();
        for (int i = 0; i < cells; i++) add(i < augments.size() ? new Icon(augments.get(i)) : new JLabel());
        revalidate();
        repaint();
    }

    private final class Icon extends JComponent {
        private final Augment augment;
        private final Image image;

        Icon(Augment augment) {
            this.augment = augment;
            image = ItemArt.augment(augment.getIcon(), iconSize).getImage();
            setToolTipText("<html><b>[" + augment.getTier().getLabel() + "] " + augment.getName() + "</b><br>"
                    + String.join("<br>", augment.getBonus().lines()) + "</html>");
            if (onClick != null) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) { onClick.run(); }
                });
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = getWidth(), h = getHeight();
            int d = Math.min(w, h) - 2;
            g2.setColor(Sketch.PAPER);
            g2.fillOval((w - d) / 2, (h - d) / 2, d, d);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(image, (w - iconSize) / 2, (h - iconSize) / 2, iconSize, iconSize, null);
            Sketch.circle(g2, w / 2.0, h / 2.0, d / 2.0, augment.getName().hashCode(), 2.2f, Sketch.tierInk(augment.getTier()));
            g2.dispose();
        }
    }
}
