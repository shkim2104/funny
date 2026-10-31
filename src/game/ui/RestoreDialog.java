package game.ui;

import game.model.Enhancement;
import game.model.GearPiece;
import game.model.Item;
import game.model.Player;
import game.save.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * The restoring window, opened over the blacksmith: the destroyed piece beside the copy it will use up.
 * Starting it fades the copy and the eraser away, a pencil redraws what was rubbed out (lines first, then
 * hatching), the color comes back and the stars fill to +5. Restoring happens the moment it is pressed;
 * the rest only shows it.
 */
public class RestoreDialog extends JDialog {
    private static final int FRAME_MS = 30, DONOR_FRAMES = 20, REDRAW_FRAMES = 95, COLOR_FRAMES = 14, STAR_FRAMES = 4;
    private static final Color GOOD_GREEN = new Color(54, 134, 58);

    private final Player player;
    private final GearPiece piece;
    private final GearPiece donor;

    private final EnhanceDialog.Stars stars = new EnhanceDialog.Stars();
    private final Bench bench = new Bench();
    private final JLabel nameLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel infoLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JButton startBtn = new Sketch.Button("복구 시작", true);

    private Timer animation;

    public RestoreDialog(Window owner, Player player, GearPiece piece, GearPiece donor) {
        super(owner, "장비 복구", Dialog.ModalityType.APPLICATION_MODAL);
        this.player = player;
        this.piece = piece;
        this.donor = donor;
        setSize(480, 560);
        setMinimumSize(new Dimension(440, 520));
        setLocationRelativeTo(owner);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 8), 34);
        page.setBorder(new EmptyBorder(14, 50, 12, 20));
        setContentPane(page);

        JPanel head = new JPanel(new BorderLayout(0, 6));
        head.setOpaque(false);
        head.add(Sketch.title("장비 복구"), BorderLayout.NORTH);
        head.add(stars, BorderLayout.CENTER);
        add(head, BorderLayout.NORTH);

        Item item = piece.getItem();
        nameLabel.setText(piece.getDisplayName());
        nameLabel.setFont(Sketch.hand(Font.BOLD, 20f));
        nameLabel.setForeground(Sketch.gradeInk(item.getGrade()));
        infoLabel.setFont(Sketch.hand(Font.BOLD, 14f));
        infoLabel.setForeground(Sketch.INK);
        infoLabel.setText(describeCost());
        bench.setPictures(piece, donor);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{bench, nameLabel, infoLabel}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(c);
            body.add(Box.createVerticalStrut(8));
        }
        add(body, BorderLayout.CENTER);

        JButton close = new Sketch.Button("닫기", false);
        close.addActionListener(e -> dispose());
        startBtn.addActionListener(e -> start());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        bottom.setOpaque(false);
        bottom.add(startBtn);
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (animation != null) animation.stop();
            }
        });
    }

    private String describeCost() {
        String warn = donor.getEnhance() > 0
                ? "<br><font color='" + Sketch.hex(Sketch.RED_PENCIL) + "'>재료의 강화 +" + donor.getEnhance() + "도 함께 사라져요.</font>" : "";
        return "<html><div style='text-align:center'>" + donor.getDisplayName() + "을(를) 재료로 써서<br>+"
                + Enhancement.RESTORE_LEVEL + "로 되돌려요." + warn + "</div></html>";
    }

    private void start() {
        if (!Enhancement.restore(player, piece, donor)) return;
        startBtn.setEnabled(false);
        if (Settings.isReducedEffects()) {
            bench.frame = Bench.LAST;
            finish();
            return;
        }
        Sfx.play(Sfx.Sound.PAGE);
        bench.started = true;
        animation = new Timer(FRAME_MS, e -> {
            int f = ++bench.frame;
            int starred = (f - DONOR_FRAMES - REDRAW_FRAMES - COLOR_FRAMES) / STAR_FRAMES;
            if (starred > stars.level && starred <= Enhancement.RESTORE_LEVEL) {
                stars.level = starred;
                Sfx.play(Sfx.Sound.TICK);
            }
            if (f >= Bench.LAST) {
                ((Timer) e.getSource()).stop();
                finish();
            }
            repaint();
        });
        animation.start();
    }

    private void finish() {
        Sfx.play(Sfx.Sound.HEAL);
        stars.level = piece.getEnhance();
        nameLabel.setText(piece.getDisplayName());
        infoLabel.setText("다시 그려졌어요!");
        infoLabel.setForeground(GOOD_GREEN);
        repaint();
    }

    /**
     * The destroyed piece and the copy side by side. The copy slides in and fades, taking the eraser and
     * crumbs with it, while the piece moves to the middle. Then the pencil draws what is missing, in
     * graphite, and the color comes back over the whole picture.
     */
    private static final class Bench extends JComponent {
        private static final int ICON = 124, DONOR_ICON = 84, HEIGHT = 176;
        static final int LAST = DONOR_FRAMES + REDRAW_FRAMES + COLOR_FRAMES + STAR_FRAMES * (Enhancement.RESTORE_LEVEL + 1);
        private Image erased, rubbed, whole, copy;
        private PencilRedraw pencil;
        boolean started;
        int frame;

        Bench() {
            setPreferredSize(new Dimension(360, HEIGHT));
            setMinimumSize(new Dimension(240, HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
        }

        void setPictures(GearPiece piece, GearPiece donor) {
            Item item = piece.getItem();
            erased = ItemArt.piece(piece, ICON).getImage();
            rubbed = ItemArt.rubbed(item, ICON).getImage();
            whole = ItemArt.item(item, ICON).getImage();
            copy = ItemArt.piece(donor, DONOR_ICON).getImage();
            pencil = new PencilRedraw(ItemArt.penciled(item, ICON).getImage(), rubbed);
        }

        private static double clamp(double v) {
            return Math.max(0, Math.min(1, v));
        }

        private static void drawFaded(Graphics2D g2, Image image, int x, int y, double alpha) {
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alpha));
            g2.drawImage(image, x, y, null);
            g2.setComposite(old);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Sketch.box(g2, 3, 3, w - 6, h - 6, 7401, 2f, Sketch.INK);

            double slide = clamp(frame / (double) DONOR_FRAMES);
            double redraw = clamp((frame - DONOR_FRAMES) / (double) REDRAW_FRAMES);
            double color = clamp((frame - DONOR_FRAMES - REDRAW_FRAMES) / (double) COLOR_FRAMES);
            int y = (h - ICON) / 2;
            int center = (w - ICON) / 2, left = center - 66;
            int mainX = (int) Math.round(left + (center - left) * slide);

            g2.drawImage(rubbed, mainX, y, this);
            if (slide < 1) {
                drawFaded(g2, erased, mainX, y, 1 - slide);
                drawCopy(g2, slide, mainX, center, h);
            }
            pencil.advance(redraw);
            if (redraw > 0) g2.drawImage(pencil.drawn(), mainX, y, this);
            if (color > 0) drawFaded(g2, whole, mainX, y, color);
            if (redraw > 0 && redraw < 1 && pencil.tip() != null) {
                drawPencil(g2, mainX + pencil.tip().getX(), y + pencil.tip().getY());
            }
            g2.dispose();
        }

        private void drawCopy(Graphics2D g2, double slide, int mainX, int center, int h) {
            int rest = center + 104, target = mainX + (ICON - DONOR_ICON) / 2;
            int x = (int) Math.round(rest + (target - rest) * slide);
            if (!started) {
                g2.setColor(Sketch.INK);
                g2.setFont(Sketch.hand(Font.BOLD, 26f));
                g2.drawString("+", center + 72, h / 2 + 9);
            }
            drawFaded(g2, copy, x, (h - DONOR_ICON) / 2, 1 - slide);
        }

        /** A small pencil held at a slant, its point on (x, y). */
        private static void drawPencil(Graphics2D g, double x, double y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.rotate(Math.toRadians(-50));
            Polygon wood = new Polygon(new int[]{0, 11, 11}, new int[]{0, -5, 5}, 3);
            Rectangle body = new Rectangle(11, -5, 38, 10), ferrule = new Rectangle(49, -5, 5, 10);
            Polygon lead = new Polygon(new int[]{0, 4, 4}, new int[]{0, -2, 2}, 3);
            g2.setStroke(new BasicStroke(1.3f));
            g2.setColor(new Color(240, 206, 50));
            g2.fill(body);
            g2.setColor(new Color(236, 214, 178));
            g2.fill(wood);
            g2.setColor(new Color(236, 148, 164));
            g2.fillRoundRect(54, -5, 8, 10, 4, 4);
            g2.setColor(new Color(170, 170, 178));
            g2.fill(ferrule);
            g2.setColor(Sketch.INK);
            g2.fill(lead);
            g2.draw(body);
            g2.draw(wood);
            g2.draw(ferrule);
            g2.dispose();
        }
    }
}
