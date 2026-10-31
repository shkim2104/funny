package game.ui;

import game.model.Enhancement;
import game.model.GearPiece;
import game.model.Item;
import game.model.Material;
import game.model.Player;
import game.save.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * The enhancement window, opened over the blacksmith: one piece on the workbench, its level as a row
 * of twenty stars, the odds and costs of the next attempt. It stays open between attempts. The attempt
 * is settled the moment it is pressed; the hammering only delays showing the result.
 */
public class EnhanceDialog extends JDialog {
    private static final int FRAME_MS = 55, FRAMES_PER_STRIKE = 5, STRIKES = 3;
    private static final Color GOOD_GREEN = new Color(54, 134, 58);

    private final Player player;
    private final GearPiece piece;
    private final Random rnd;

    private final Stars stars = new Stars();
    private final Workbench bench = new Workbench();
    private final JLabel nameLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel statLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel oddsLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JPanel costs = new JPanel(new GridLayout(0, 1, 0, 2));
    private final JCheckBox eraserBox = new JCheckBox();
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JButton enhanceBtn = new Sketch.Button("강화하기", true);

    private Timer hammer;
    private boolean working;
    /** Risk is confirmed once per window, so repeated attempts stay quick. */
    private boolean riskAccepted;

    public EnhanceDialog(Window owner, Player player, GearPiece piece, Random rnd) {
        super(owner, "장비 강화", Dialog.ModalityType.APPLICATION_MODAL);
        this.player = player;
        this.piece = piece;
        this.rnd = rnd;
        setSize(480, 700);
        setMinimumSize(new Dimension(440, 640));
        setLocationRelativeTo(owner);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 8), 34);
        page.setBorder(new EmptyBorder(14, 50, 12, 20));
        setContentPane(page);

        JPanel head = new JPanel(new BorderLayout(0, 6));
        head.setOpaque(false);
        head.add(Sketch.title("장비 강화"), BorderLayout.NORTH);
        head.add(stars, BorderLayout.CENTER);
        add(head, BorderLayout.NORTH);

        nameLabel.setFont(Sketch.hand(Font.BOLD, 20f));
        statLabel.setFont(Sketch.hand(Font.BOLD, 15f));
        statLabel.setForeground(Sketch.BLUE_PENCIL);
        oddsLabel.setFont(Sketch.hand(Font.BOLD, 15f));
        oddsLabel.setForeground(Sketch.INK);
        costs.setOpaque(false);
        eraserBox.setOpaque(false);
        eraserBox.setFocusPainted(false);
        eraserBox.setFont(Sketch.hand(Font.BOLD, 13f));
        eraserBox.setForeground(Sketch.INK);
        eraserBox.addActionListener(e -> update());
        messageLabel.setFont(Sketch.hand(Font.BOLD, 16f));

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{bench, nameLabel, statLabel, oddsLabel, costs, eraserBox}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
            body.add(c);
            body.add(Box.createVerticalStrut(6));
        }
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(Box.createVerticalGlue());
        body.add(messageLabel);
        body.add(Box.createVerticalStrut(6));
        add(body, BorderLayout.CENTER);

        JButton close = new Sketch.Button("닫기", false);
        close.addActionListener(e -> dispose());
        enhanceBtn.addActionListener(e -> attempt());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        bottom.setOpaque(false);
        bottom.add(enhanceBtn);
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(enhanceBtn);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (hammer != null) hammer.stop();
            }
        });
        update();
    }

    /** Puts the piece's current state on screen. */
    private void update() {
        Item item = piece.getItem();
        stars.level = piece.isDestroyed() ? 0 : piece.getEnhance();
        stars.next = Enhancement.canEnhance(piece);
        bench.setPiece(piece);
        nameLabel.setText(piece.getDisplayName());
        nameLabel.setForeground(Sketch.gradeInk(item.getGrade()));
        costs.removeAll();
        eraserBox.setVisible(false);
        if (piece.isDestroyed() || !Enhancement.canEnhance(piece)) {
            statLabel.setText(piece.isDestroyed() ? "대장간 복구 탭에서 되살릴 수 있어요" : piece.getStatText());
            oddsLabel.setText(piece.isDestroyed() ? " " : "최고 단계예요");
            enhanceBtn.setEnabled(false);
        } else {
            int target = piece.getEnhance() + 1;
            boolean risky = Enhancement.destroyPct(target) > 0, eraser = risky && eraserBox.isSelected();
            int destroy = eraser ? 0 : Enhancement.destroyPct(target);
            statLabel.setText((piece.isWeapon() ? "공격력 " : "방어력 ") + piece.getValue() + " → "
                    + Enhancement.boostedValue(item.getValue(), target));
            oddsLabel.setText("<html>성공 " + Enhancement.successPct(target) + "%  ·  실패 " + (100 - Enhancement.successPct(target) - destroy) + "%"
                    + (destroy > 0 ? "  ·  <font color='" + Sketch.hex(Sketch.RED_PENCIL) + "'>파괴 " + destroy + "%</font>" : "") + "</html>");
            costs.add(cost(ItemArt.augment("coin", 26), "골드", Enhancement.goldCost(target), player.getGold()));
            costs.add(cost(Material.GRAPHITE, Enhancement.graphiteCost(target)));
            if (Enhancement.hardLeadCost(target) > 0) costs.add(cost(Material.HARD_LEAD, Enhancement.hardLeadCost(target)));
            eraserBox.setText(Material.ERASER_DUST.getLabel() + " 1개로 파괴 막기 (보유 " + player.getMaterial(Material.ERASER_DUST) + ")");
            eraserBox.setVisible(risky);
            if (eraser && player.getMaterial(Material.ERASER_DUST) < 1) eraserBox.setSelected(false);
            eraserBox.setEnabled(player.getMaterial(Material.ERASER_DUST) >= 1);
            enhanceBtn.setEnabled(!working && Enhancement.canAfford(player, piece, risky && eraserBox.isSelected()));
        }
        // Keeps the rows together: a BoxLayout would otherwise stretch them over the free space.
        costs.setMaximumSize(new Dimension(Integer.MAX_VALUE, costs.getPreferredSize().height));
        costs.revalidate();
        repaint();
    }

    private JComponent cost(Material m, int need) {
        return cost(ItemArt.material(m, 26), m.getLabel(), need, player.getMaterial(m));
    }

    private static JComponent cost(Icon icon, String name, int need, int have) {
        JLabel l = new JLabel(name + "  " + need + "  (보유 " + have + ")", icon, SwingConstants.CENTER);
        l.setFont(Sketch.hand(Font.BOLD, 14f));
        l.setForeground(have >= need ? Sketch.INK : Sketch.RED_PENCIL);
        return l;
    }

    private void attempt() {
        if (working || !Enhancement.canEnhance(piece)) return;
        int target = piece.getEnhance() + 1;
        boolean risky = Enhancement.destroyPct(target) > 0, eraser = risky && eraserBox.isSelected();
        if (risky && !eraser && !riskAccepted) {
            if (!Dialogs.confirm(this, "파괴 위험", "이제부터 강화에 실패하면 장비가 파괴될 수 있어요.\n"
                    + "이번 시도의 파괴 확률은 " + Enhancement.destroyPct(target) + "%예요. 강화할까요?")) {
                return;
            }
            riskAccepted = true;
        }
        Enhancement.Result result = Enhancement.attempt(player, piece, eraser, rnd);
        if (result == null) return;

        messageLabel.setText(" ");
        bench.stamp = null;
        if (Settings.isReducedEffects()) {
            reveal(result);
            return;
        }
        working = true;
        enhanceBtn.setEnabled(false);
        int[] frame = {0};
        hammer = new Timer(FRAME_MS, e -> {
            int f = frame[0]++;
            if (f >= FRAMES_PER_STRIKE * STRIKES) {
                ((Timer) e.getSource()).stop();
                bench.strike = 0;
                working = false;
                reveal(result);
                return;
            }
            int phase = f % FRAMES_PER_STRIKE;
            if (phase == 0) Sfx.play(Sfx.Sound.TICK);
            bench.strike = FRAMES_PER_STRIKE - phase;
            bench.repaint();
        });
        hammer.start();
    }

    private void reveal(Enhancement.Result result) {
        switch (result) {
            case SUCCESS:
                Sfx.play(Sfx.Sound.LEVEL_UP);
                bench.stamp("성공!", GOOD_GREEN);
                message("+" + piece.getEnhance() + " 강화 성공!", GOOD_GREEN);
                break;
            case FAIL:
                Sfx.play(Sfx.Sound.HURT);
                bench.stamp("실패", Sketch.INK_SOFT);
                message("강화 실패... 단계는 그대로예요.", Sketch.INK_SOFT);
                break;
            default:
                Sfx.play(Sfx.Sound.CRIT);
                bench.stamp("파괴!", Sketch.RED_PENCIL);
                message("장비가 파괴됐어요...", Sketch.RED_PENCIL);
        }
        update();
    }

    private void message(String text, Color color) {
        messageLabel.setText(text);
        messageLabel.setForeground(color);
    }

    /** The level as twenty pencil stars, two rows of ten: filled up to the level, the next one in red. */
    static final class Stars extends JComponent {
        private static final int SIZE = 22, STEP = 28, GROUP_GAP = 10;
        int level;
        boolean next;

        Stars() {
            Dimension d = new Dimension(10 * STEP + GROUP_GAP, 2 * STEP);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int left = (getWidth() - (10 * STEP + GROUP_GAP)) / 2;
            for (int i = 0; i < Enhancement.MAX_LEVEL; i++) {
                int col = i % 10, row = i / 10;
                double cx = left + col * STEP + (col >= 5 ? GROUP_GAP : 0) + STEP / 2.0, cy = row * STEP + STEP / 2.0;
                Shape star = star(cx, cy, SIZE / 2.0 + (i % 3 == 0 ? 0.6 : 0));
                if (i < level) {
                    g2.setColor(new Color(214, 170, 44));
                    g2.fill(star);
                    Sketch.hatch(g2, star, i * 7L + 3, new Color(150, 110, 20), 150, 4, 40);
                }
                boolean isNext = next && i == level;
                g2.setStroke(new BasicStroke(isNext ? 2.2f : 1.4f));
                g2.setColor(i < level ? Sketch.INK : isNext ? Sketch.RED_PENCIL : Sketch.INK_SOFT);
                g2.draw(star);
            }
            g2.dispose();
        }

        private static Shape star(double cx, double cy, double r) {
            Path2D p = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * 0.45;
                double x = cx + Math.cos(a) * rr, y = cy + Math.sin(a) * rr;
                if (i == 0) p.moveTo(x, y);
                else p.lineTo(x, y);
            }
            p.closePath();
            return p;
        }
    }

    /** A pencil-boxed bench showing the piece, shaken by the hammer, with the result stamped across it. */
    private static final class Workbench extends JComponent {
        private static final int ICON = 128, HEIGHT = 168;
        private Image image;
        /** Frames left of the current hammer strike's shake; 0 at rest. */
        int strike;
        String stamp;
        private Color stampColor = Sketch.INK;

        Workbench() {
            setPreferredSize(new Dimension(360, HEIGHT));
            setMinimumSize(new Dimension(240, HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
        }

        void setPiece(GearPiece piece) {
            ImageIcon icon = ItemArt.piece(piece, ICON);
            image = icon == null ? null : icon.getImage();
        }

        void stamp(String text, Color color) {
            stamp = text;
            stampColor = color;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            Sketch.box(g2, 3, 3, w - 6, h - 6, 7301, 2f, Sketch.INK);
            int x = (w - ICON) / 2 + (strike % 2 == 0 ? strike : -strike), y = (h - ICON) / 2 + (strike > 0 ? 2 : 0);
            if (image != null) g2.drawImage(image, x, y, this);
            if (strike >= 3) {
                // Sparks from the blow.
                double cx = w / 2.0, cy = h / 2.0;
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4 + strike, r0 = 40, r1 = 58 + strike * 4;
                    Sketch.line(g2, cx + Math.cos(a) * r0, cy + Math.sin(a) * r0, cx + Math.cos(a) * r1, cy + Math.sin(a) * r1,
                            i * 11L + strike, 2f, new Color(214, 150, 40));
                }
            }
            if (stamp != null) drawStamp(g2, w, h, stamp, stampColor);
            g2.dispose();
        }
    }

    /** A result stamped slantwise across the middle of a w x h area. */
    static void drawStamp(Graphics2D g, int w, int h, String text, Color color) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Sketch.hand(Font.BOLD, 38f));
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.rotate(Math.toRadians(-9), w / 2.0, h / 2.0);
        g2.setColor(new Color(247, 243, 230, 205));
        g2.fillRoundRect((w - tw) / 2 - 14, h / 2 - 32, tw + 28, 60, 12, 12);
        g2.setColor(color);
        g2.drawString(text, (w - tw) / 2, h / 2 + 12);
        Sketch.box(g2, (w - tw) / 2.0 - 14, h / 2.0 - 32, tw + 28, 60, 7321, 2.6f, color);
        g2.dispose();
    }
}
