package game.ui;

import game.model.Augment;
import game.model.Item;
import game.model.RunState;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 롤토체스-style augment pick after a kill: three cards of one tier, pick one (no skipping), with a few
 * rerolls per run. Also hosts the boss-loot popup, since both show pencil icons on cards.
 */
public final class AugmentDialog {
    private static final int CARD_W = 210, CARD_H = 290;

    private final JDialog dialog;
    private final RunState run;
    private final Random rnd;
    private final JPanel cardRow = new JPanel(new GridLayout(1, RunState.OFFER_SIZE, 14, 0));
    private final JLabel tierLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton rerollBtn = Theme.button("");
    private Augment chosen;
    /** The three cards on screen (a reroll avoids them). */
    private List<Augment> current = new ArrayList<>();

    /** Shows the pick and returns the chosen augment (never null while there is anything left to offer). */
    public static Augment pick(Component owner, RunState run, Random rnd) {
        List<Augment> first = run.offer(rnd);
        if (first.isEmpty()) return null;
        AugmentDialog d = new AugmentDialog(owner, run, rnd);
        d.show(first);
        return d.chosen;
    }

    private AugmentDialog(Component owner, RunState run, Random rnd) {
        this.run = run;
        this.rnd = rnd;
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        dialog = new JDialog(w, "증강 선택", Dialog.ModalityType.APPLICATION_MODAL);
        Theme.styleDialog(dialog);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);   // a pick is mandatory
        dialog.setLayout(new BorderLayout(0, 14));
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(18, 22, 16, 22));

        JLabel title = new JLabel("증강을 하나 고르세요", SwingConstants.CENTER);
        title.setFont(Theme.dosFont(Font.BOLD, 24f));
        title.setForeground(Theme.TEXT);
        tierLabel.setFont(Theme.dosFont(Font.BOLD, 15f));
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 4));
        head.setOpaque(false);
        head.add(title);
        head.add(tierLabel);
        dialog.add(head, BorderLayout.NORTH);

        cardRow.setOpaque(false);
        dialog.add(cardRow, BorderLayout.CENTER);

        List<String> owned = new ArrayList<>();
        for (Augment a : run.getAugments()) owned.add(a.getName());
        JLabel ownedLabel = new JLabel("<html><body style='width:430px'>보유 증강: "
                + (owned.isEmpty() ? "없음" : String.join(", ", owned)) + "</body></html>");
        ownedLabel.setFont(Theme.SMALL_FONT);
        ownedLabel.setForeground(Theme.TEXT_DIM);
        // Same tier, three different cards (see RunState.reroll).
        rerollBtn.addActionListener(e -> {
            if (run.useReroll()) setCards(run.reroll(current, rnd));
        });
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        bottom.add(ownedLabel, BorderLayout.CENTER);
        bottom.add(rerollBtn, BorderLayout.EAST);
        dialog.add(bottom, BorderLayout.SOUTH);
    }

    private void show(List<Augment> offer) {
        setCards(offer);
        dialog.pack();
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
    }

    private void setCards(List<Augment> offer) {
        current = offer;
        cardRow.removeAll();
        List<JButton> buttons = new ArrayList<>();
        for (Augment a : offer) {
            Card card = new Card(a);
            card.addActionListener(e -> {
                chosen = a;
                dialog.dispose();
            });
            buttons.add(card);
            cardRow.add(card);
        }
        Augment.Tier tier = offer.get(0).getTier();
        tierLabel.setText(tier.getLabel() + " 증강");
        tierLabel.setForeground(Color.decode(tier.getColorHex()));
        rerollBtn.setText("새로고침 (" + run.getRerollsLeft() + ")");
        rerollBtn.setEnabled(run.getRerollsLeft() > 0);
        JButton[] nav = buttons.toArray(new JButton[0]);
        Theme.arrowNav(nav);
        Theme.focusFirst(nav);
        cardRow.revalidate();
        cardRow.repaint();
    }

    /** One augment card: tier-colored frame, pencil icon on a paper disc, name and effect. */
    private static final class Card extends JButton {
        private final Color tierColor;
        private boolean hover;

        Card(Augment a) {
            tierColor = Color.decode(a.getTier().getColorHex());
            setLayout(new BorderLayout(0, 10));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(22, 16, 18, 16));
            setPreferredSize(new Dimension(CARD_W, CARD_H));

            JLabel icon = new JLabel(ItemArt.augment(a.getIcon(), 88), SwingConstants.CENTER) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int d = 104;
                    g2.setColor(new Color(247, 243, 230));
                    g2.fillOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                    g2.setColor(tierColor);
                    g2.setStroke(new BasicStroke(3f));
                    g2.drawOval((getWidth() - d) / 2, (getHeight() - d) / 2, d, d);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            icon.setPreferredSize(new Dimension(110, 110));
            JLabel name = new JLabel(a.getName(), SwingConstants.CENTER);
            name.setFont(Theme.dosFont(Font.BOLD, 19f));
            name.setForeground(tierColor);
            JLabel desc = new JLabel("<html><div style='text-align:center;width:160px'>"
                    + String.join("<br>", a.getBonus().lines()) + "</div></html>", SwingConstants.CENTER);
            desc.setFont(Theme.BODY_FONT.deriveFont(14f));
            desc.setForeground(Theme.TEXT);
            desc.setVerticalAlignment(SwingConstants.TOP);
            JPanel text = new JPanel(new BorderLayout(0, 8));
            text.setOpaque(false);
            text.add(name, BorderLayout.NORTH);
            text.add(desc, BorderLayout.CENTER);
            add(icon, BorderLayout.NORTH);
            add(text, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean lit = hover || isFocusOwner();
            RoundRectangle2D box = new RoundRectangle2D.Float(2, 2, getWidth() - 4, getHeight() - 4, 18, 18);
            g2.setColor(lit ? new Color(44, 46, 54) : Theme.SURFACE);
            g2.fill(box);
            g2.setPaint(new GradientPaint(0, 0, new Color(tierColor.getRed(), tierColor.getGreen(), tierColor.getBlue(), 70),
                    0, getHeight() * 0.45f, new Color(tierColor.getRed(), tierColor.getGreen(), tierColor.getBlue(), 0)));
            g2.fill(box);
            g2.setColor(tierColor);
            g2.setStroke(new BasicStroke(lit ? 3.5f : 2f));
            g2.draw(box);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------- boss loot ----------

    /** The boss-drop popup: the gear's pencil icon, name in its grade color, stat and special effects. */
    public static void showLoot(Component owner, String bossName, Item item) {
        JPanel content = new JPanel(new BorderLayout(18, 0));
        content.setOpaque(false);
        JLabel icon = new JLabel(ItemArt.item(item, 128));
        icon.setPreferredSize(new Dimension(140, 140));
        content.add(icon, BorderLayout.WEST);

        StringBuilder html = new StringBuilder("<html><body style='width:230px'>")
                .append("<font color='#8c8f9a'>").append(bossName).append("이(가) 떨어뜨렸다!</font><br><br>")
                .append("<font size='5' color='").append(item.getGrade().getColorHex()).append("'><b>")
                .append(item.getName()).append("</b></font><br>")
                .append(Theme.gradeTagHtml(item)).append(" ").append(item.getTypeLabel()).append(" · ")
                .append(item.getStatText()).append("<br><br>");
        for (String line : item.getBonus().lines()) html.append("<font color='#c7a86a'>◆ ").append(line).append("</font><br>");
        html.append("</body></html>");
        JLabel text = new JLabel(html.toString());
        text.setFont(Theme.BODY_FONT.deriveFont(14f));
        text.setForeground(Theme.TEXT);
        content.add(text, BorderLayout.CENTER);
        Dialogs.custom(owner, "보스 전리품", content, 470, 330);
    }
}
