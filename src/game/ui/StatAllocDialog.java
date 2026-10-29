package game.ui;

import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Stat point allocation, as a pencil sketch on a notebook page (see Sketch). */
public class StatAllocDialog {
    private StatAllocDialog() {}

    public static void show(JFrame owner, Player player) {
        JDialog dialog = new JDialog(owner, "스탯 분배", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 12), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 24));
        dialog.setContentPane(page);

        JLabel pointsLabel = Sketch.label("", Font.BOLD, 18f, Sketch.RED_PENCIL);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("스탯 분배"), BorderLayout.WEST);
        head.add(pointsLabel, BorderLayout.EAST);
        dialog.add(head, BorderLayout.NORTH);

        Sketch.Box card = new Sketch.Box(new GridLayout(6, 1), 6101);
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        List<JButton> plusButtons = new ArrayList<>();
        List<Runnable> rowRefresh = new ArrayList<>();
        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            pointsLabel.setText("남은 포인트  " + player.getStatPoints());
            for (Runnable r : rowRefresh) r.run();
            boolean has = player.getStatPoints() > 0;
            for (JButton b : plusButtons) b.setEnabled(has);
            dialog.repaint();
        };

        card.add(statRow("heart", "HP", "최대 HP +" + Player.HP_PER_POINT,
                () -> player.getHp() + " / " + player.getMaxHp(), Player.Stat.HP, player, plusButtons, rowRefresh, refresh));
        card.add(statRow("orb", "MP", "최대 MP +" + Player.MP_PER_POINT,
                () -> player.getMp() + " / " + player.getMaxMp(), Player.Stat.MP, player, plusButtons, rowRefresh, refresh));
        card.add(statRow("sword", "공격력", "공격력 +" + Player.ATK_PER_POINT,
                () -> String.valueOf(player.getBaseAtk()), Player.Stat.ATK, player, plusButtons, rowRefresh, refresh));
        card.add(statRow("shield", "방어력", "방어력 +" + Player.DEF_PER_POINT,
                () -> String.valueOf(player.getBaseDef()), Player.Stat.DEF, player, plusButtons, rowRefresh, refresh));
        card.add(statRow("clover", "행운", "행운 +" + Player.LUCK_PER_POINT + " · 치명타↑",
                () -> String.valueOf(player.getLuck()), Player.Stat.LUCK, player, plusButtons, rowRefresh, refresh));
        card.add(statRow("wing", "공격속도", "공격속도 +" + Player.SPD_PER_POINT + " · 선공",
                () -> String.valueOf(player.getSpd()), Player.Stat.SPD, player, plusButtons, rowRefresh, refresh));

        refresh[0].run();
        dialog.add(card, BorderLayout.CENTER);

        JButton close = new Sketch.Button("나가기", false);
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);

        JButton[] navButtons = new JButton[plusButtons.size() + 1];
        for (int i = 0; i < plusButtons.size(); i++) navButtons[i] = plusButtons.get(i);
        navButtons[plusButtons.size()] = close;
        Theme.arrowNav(navButtons);
        Theme.focusFirst(navButtons);

        dialog.setSize(520, 580);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /** One stat: pencil icon, name with what a point gives, the current value, and a +1 button. */
    private static JPanel statRow(String icon, String name, String hint, Supplier<String> value, Player.Stat stat, Player player,
                                  List<JButton> plusButtons, List<Runnable> rowRefresh, Runnable[] refresh) {
        long seed = name.hashCode();
        JPanel row = new JPanel(new BorderLayout(12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Sketch.smooth(g);
                Sketch.line(g2, 4, getHeight() - 2, getWidth() - 4, getHeight() - 2, seed, 1.1f, new Color(118, 112, 106, 120));
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 4, 0, 4));

        row.add(new JLabel(ItemArt.augment(icon, 40)), BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(Sketch.label(name, Font.BOLD, 16f, Sketch.INK));
        text.add(Sketch.label(hint + " / 포인트", Font.PLAIN, 12f, Sketch.INK_SOFT));
        row.add(text, BorderLayout.CENTER);

        JLabel valueLabel = Sketch.label("", Font.BOLD, 18f, Sketch.BLUE_PENCIL);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        rowRefresh.add(() -> valueLabel.setText(value.get()));

        JButton plus = new Sketch.Button("+1", true);
        plus.addActionListener(e -> {
            player.spendStatPoint(stat);
            refresh[0].run();
        });
        plusButtons.add(plus);
        JPanel east = new JPanel(new BorderLayout(12, 0));
        east.setOpaque(false);
        east.add(valueLabel, BorderLayout.CENTER);
        JPanel btnWrap = new JPanel(new GridBagLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(plus);
        east.add(btnWrap, BorderLayout.EAST);
        row.add(east, BorderLayout.EAST);
        return row;
    }
}
