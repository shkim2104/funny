package game.ui;

import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Stat point allocation, as a pencil sketch on a notebook page (see Sketch). How many points one press
 * spends is chosen at the top (1, 5, 10, 50, 전부, or any number), so dozens go in at once.
 */
public class StatAllocDialog {
    private StatAllocDialog() {}

    /** The quick picks for "한 번에 올릴 양"; 0 stands for 전부 (every point left). */
    static final int[] QUICK = {1, 5, 10, 50, 0};

    /** Points one press spends now: the chosen amount, never more than what is left (전부 = all of it). */
    static int perPress(Player player, Sketch.Stepper amount, boolean[] all) {
        int left = player.getStatPoints();
        return Math.max(0, all[0] ? left : Math.min(amount.getValue(), left));
    }

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
        Sketch.Stepper amount = new Sketch.Stepper(1, 1, Math.max(1, player.getStatPoints()), 1);
        amount.setPreferredSize(new Dimension(190, 42));
        boolean[] all = {false};
        List<JButton> quick = new ArrayList<>();
        refresh[0] = () -> {
            pointsLabel.setText("남은 포인트  " + player.getStatPoints());
            amount.setRange(1, Math.max(1, player.getStatPoints()));
            for (Runnable r : rowRefresh) r.run();
            int n = perPress(player, amount, all);
            for (JButton b : plusButtons) {
                b.setText("+" + Math.max(1, n));
                b.setEnabled(n > 0);
            }
            for (int i = 0; i < quick.size(); i++) {
                boolean on = QUICK[i] == 0 ? all[0] : !all[0] && amount.getValue() == QUICK[i];
                quick.get(i).setForeground(on ? Sketch.RED_PENCIL : Sketch.INK);
                quick.get(i).setFont(Sketch.hand(on ? Font.BOLD : Font.PLAIN, 15f));
            }
            dialog.repaint();
        };
        amount.onChange(() -> {
            all[0] = false;
            refresh[0].run();
        });

        // "한 번에 올릴 양": quick picks and a number of one's own.
        JPanel amountRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        amountRow.setOpaque(false);
        amountRow.add(Sketch.label("한 번에 올릴 양", Font.BOLD, 15f, Sketch.INK));
        for (int q : QUICK) {
            JButton b = new Sketch.Button(q == 0 ? "전부" : String.valueOf(q), false);
            b.setBorder(new EmptyBorder(8, 12, 8, 12));
            b.addActionListener(e -> {
                all[0] = q == 0;
                if (q > 0) amount.setValue(q);
                refresh[0].run();
            });
            quick.add(b);
            amountRow.add(b);
        }
        amountRow.add(amount);

        card.add(statRow("heart", "HP", "최대 HP +" + Player.HP_PER_POINT,
                () -> player.getHp() + " / " + player.getMaxHp(), Player.Stat.HP, player, plusButtons, rowRefresh, refresh, amount, all));
        card.add(statRow("orb", "MP", "최대 MP +" + Player.MP_PER_POINT,
                () -> player.getMp() + " / " + player.getMaxMp(), Player.Stat.MP, player, plusButtons, rowRefresh, refresh, amount, all));
        card.add(statRow("sword", "공격력", "공격력 +" + Player.ATK_PER_POINT,
                () -> String.valueOf(player.getBaseAtk()), Player.Stat.ATK, player, plusButtons, rowRefresh, refresh, amount, all));
        card.add(statRow("shield", "방어력", "방어력 +" + Player.DEF_PER_POINT,
                () -> String.valueOf(player.getBaseDef()), Player.Stat.DEF, player, plusButtons, rowRefresh, refresh, amount, all));
        card.add(statRow("clover", "행운", "행운 +" + Player.LUCK_PER_POINT + " · 치명타↑",
                () -> String.valueOf(player.getLuck()), Player.Stat.LUCK, player, plusButtons, rowRefresh, refresh, amount, all));
        card.add(statRow("wing", "공격속도", "공격속도 +" + Player.SPD_PER_POINT + " · 선공",
                () -> String.valueOf(player.getSpd()), Player.Stat.SPD, player, plusButtons, rowRefresh, refresh, amount, all));

        JPanel middle = new JPanel(new BorderLayout(0, 10));
        middle.setOpaque(false);
        middle.add(amountRow, BorderLayout.NORTH);
        middle.add(card, BorderLayout.CENTER);
        refresh[0].run();
        dialog.add(middle, BorderLayout.CENTER);

        JButton close = Sketch.bigButton("나가기", false);
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

        dialog.setSize(640, 680);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    /** One stat: pencil icon, name with what a point gives, the current value, and a +1 button. */
    private static JPanel statRow(String icon, String name, String hint, Supplier<String> value, Player.Stat stat, Player player,
                                  List<JButton> plusButtons, List<Runnable> rowRefresh, Runnable[] refresh,
                                  Sketch.Stepper amount, boolean[] all) {
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
        plus.setPreferredSize(new Dimension(86, 46));
        plus.addActionListener(e -> {
            for (int n = perPress(player, amount, all); n > 0 && player.spendStatPoint(stat); n--) { }
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
