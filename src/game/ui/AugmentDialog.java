package game.ui;

import game.model.Augment;
import game.model.Item;
import game.model.Bonus;
import game.model.Player;
import game.model.RunState;
import game.save.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 롤토체스-style augment pick after a kill: three cards of one tier, pick one (no skipping), with a few
 * rerolls per run. Also hosts the boss-loot popup, since both show pencil icons on cards.
 */
public final class AugmentDialog {
    private static final int CARD_W = 210, CARD_H = 330;

    private final JDialog dialog;
    private final RunState run;
    private final Player player;
    private final Random rnd;
    private final JPanel cardRow = new JPanel(new GridLayout(1, RunState.OFFER_SIZE, 14, 0));
    private final JLabel tierLabel = new JLabel("", SwingConstants.CENTER);
    private final JButton rerollBtn = new Sketch.Button("", false);
    private Augment chosen;
    /** The three cards on screen (a reroll avoids them). */
    private List<Augment> current = new ArrayList<>();

    /** Shows the pick and returns the chosen augment (never null while there is anything left to offer). */
    public static Augment pick(Component owner, RunState run, Player player, Random rnd) {
        List<Augment> first = run.offer(rnd);
        if (first.isEmpty()) return null;
        AugmentDialog d = new AugmentDialog(owner, run, player, rnd);
        d.show(first);
        return d.chosen;
    }

    private AugmentDialog(Component owner, RunState run, Player player, Random rnd) {
        this.run = run;
        this.player = player;
        this.rnd = rnd;
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        dialog = new JDialog(w, "증강 선택", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);   // a pick is mandatory
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 12), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 24));
        dialog.setContentPane(page);

        tierLabel.setFont(Sketch.hand(Font.BOLD, 17f));
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("증강을 하나 고르세요"), BorderLayout.WEST);
        head.add(tierLabel, BorderLayout.EAST);
        dialog.add(head, BorderLayout.NORTH);

        cardRow.setOpaque(false);
        dialog.add(cardRow, BorderLayout.CENTER);

        JLabel ownedLabel = Sketch.label("보유 증강 " + run.getAugments().size() + "개", Font.BOLD, 14f, Sketch.INK);
        if (run.getAugments().isEmpty()) ownedLabel.setText("보유 증강 없음");
        AugmentStrip owned = new AugmentStrip(30, 10, 1, null);
        owned.set(run.getAugments());
        JPanel ownedRow = new JPanel(new BorderLayout(10, 0));
        ownedRow.setOpaque(false);
        ownedRow.add(ownedLabel, BorderLayout.WEST);
        ownedRow.add(owned, BorderLayout.CENTER);
        // Same tier, three different cards (see RunState.reroll).
        rerollBtn.addActionListener(e -> {
            if (run.useReroll()) setCards(run.reroll(current, rnd));
        });
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);
        bottom.add(ownedRow, BorderLayout.CENTER);
        bottom.add(rerollBtn, BorderLayout.EAST);
        dialog.add(bottom, BorderLayout.SOUTH);
    }

    /**
     * 증강 효과 미리 보기: how picking `a` changes the player's numbers, e.g. "치명타 확률 22% → 30%"
     * (gear effects and the augments already taken are counted, the same way the battle adds them).
     */
    private String preview(Augment a) {
        Bonus now = player.getGearBonus().plus(run.total());
        Bonus next = now.plus(a.getBonus());
        List<String> out = new ArrayList<>();
        diff(out, "공격력", player.getAtk() * (1 + now.atkPct()), player.getAtk() * (1 + next.atkPct()), "");
        diff(out, "방어력", player.getDef() * (1 + now.defPct()), player.getDef() * (1 + next.defPct()), "");
        diff(out, "치명타 확률", Math.min(100, player.getCritChance() + now.critChance()),
                Math.min(100, player.getCritChance() + next.critChance()), "%");
        diff(out, "치명타 피해", 175 * (1 + now.critDmg()), 175 * (1 + next.critDmg()), "%");
        diff(out, "스킬 피해", 100 * (1 + now.skillDmg()), 100 * (1 + next.skillDmg()), "%");
        diff(out, "받는 피해 감소", 100 * now.dmgReduction(), 100 * next.dmgReduction(), "%");
        diff(out, "회피", 100 * now.dodge(), 100 * next.dodge(), "%");
        diff(out, "흡혈", 100 * now.lifesteal(), 100 * next.lifesteal(), "%");
        diff(out, "공격속도", player.getSpd() + now.spd(), player.getSpd() + next.spd(), "");
        return out.isEmpty() ? null : String.join("<br>", out.subList(0, Math.min(3, out.size())));
    }

    private static void diff(List<String> out, String what, double before, double after, String unit) {
        long b = Math.round(before), a = Math.round(after);
        if (a != b) out.add(what + " " + b + unit + " → " + a + unit);
    }

    private void show(List<Augment> offer) {
        Sfx.play(Sfx.Sound.CARD);
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
            Card card = new Card(a, Settings.isAugmentPreview() ? preview(a) : null);
            card.addActionListener(e -> {
                chosen = a;
                dialog.dispose();
            });
            buttons.add(card);
            cardRow.add(card);
        }
        Augment.Tier tier = offer.get(0).getTier();
        tierLabel.setText(tier.getLabel() + " 증강");
        tierLabel.setForeground(Sketch.tierInk(tier));
        rerollBtn.setText("새로고침 (" + run.getRerollsLeft() + ")");
        rerollBtn.setEnabled(run.getRerollsLeft() > 0);
        JButton[] nav = buttons.toArray(new JButton[0]);
        Theme.arrowNav(nav);
        Theme.focusFirst(nav);
        cardRow.revalidate();
        cardRow.repaint();
    }

    /** One augment card: a pencil box hatched at the top in the tier's color, with icon, name and effect. */
    private static final class Card extends JButton {
        private final Color tierColor;
        private final long seed;
        private boolean hover;

        Card(Augment a, String previewHtml) {
            tierColor = Sketch.tierInk(a.getTier());
            seed = a.getName().hashCode();
            setLayout(new BorderLayout(0, 10));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(22, 16, 18, 16));
            setPreferredSize(new Dimension(CARD_W, CARD_H));

            JLabel icon = new JLabel(ItemArt.augment(a.getIcon(), 96), SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(110, 110));
            JLabel name = new JLabel(a.getName(), SwingConstants.CENTER);
            name.setFont(Sketch.hand(Font.BOLD, 19f));
            name.setForeground(tierColor);
            JLabel desc = new JLabel("<html><div style='text-align:center;width:128px'>"
                    + String.join("<br>", a.getBonus().lines())
                    + (previewHtml == null ? "" : "<br><br><font color='" + Sketch.hex(Sketch.BLUE_PENCIL) + "'>" + previewHtml + "</font>")
                    + "</div></html>", SwingConstants.CENTER);
            desc.setFont(Sketch.hand(Font.PLAIN, 14f));
            desc.setForeground(Sketch.INK);
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
            Graphics2D g2 = Sketch.smooth(g);
            boolean lit = hover || isFocusOwner();
            int w = getWidth(), h = getHeight();
            Sketch.hatch(g2, new java.awt.Rectangle(7, 7, w - 14, Math.round(h * 0.42f)), seed, tierColor, lit ? 150 : 90, 7, 40);
            if (lit) Sketch.hatch(g2, new java.awt.Rectangle(7, 7, w - 14, h - 14), seed + 1, tierColor, 34, 9, -40);
            Sketch.box(g2, 5, 5, w - 10, h - 10, seed + 2, lit ? 3.2f : 2.2f, tierColor);
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
