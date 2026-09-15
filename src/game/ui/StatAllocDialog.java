package game.ui;

import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class StatAllocDialog {
    private StatAllocDialog() {}

    public static void show(JFrame owner, Player player) {
        JDialog dialog = new JDialog(owner, "스탯 분배", Dialog.ModalityType.APPLICATION_MODAL);
        Theme.styleDialog(dialog);
        dialog.setLayout(new BorderLayout(14, 14));
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(18, 18, 16, 18));

        JLabel pointsLabel = new JLabel();
        pointsLabel.setForeground(Theme.ACCENT);
        pointsLabel.setFont(Theme.TITLE_FONT.deriveFont(20f));
        dialog.add(pointsLabel, BorderLayout.NORTH);

        JPanel card = Theme.panel(new GridLayout(6, 1));
        card.setBorder(new EmptyBorder(4, 4, 4, 4));

        List<JButton> plusButtons = new ArrayList<>();
        JLabel hpLabel = new JLabel();
        JLabel mpLabel = new JLabel();
        JLabel atkLabel = new JLabel();
        JLabel defLabel = new JLabel();
        JLabel luckLabel = new JLabel();
        JLabel spdLabel = new JLabel();

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            pointsLabel.setText("남은 스탯 포인트: " + player.getStatPoints());
            hpLabel.setText("<html>HP&nbsp;&nbsp;" + player.getHp() + "/" + player.getMaxHp() + "</html>");
            mpLabel.setText("<html>MP&nbsp;&nbsp;" + player.getMp() + "/" + player.getMaxMp() + "</html>");
            atkLabel.setText("<html>공격력&nbsp;&nbsp;" + player.getBaseAtk() + "</html>");
            defLabel.setText("<html>방어력&nbsp;&nbsp;" + player.getBaseDef() + "</html>");
            luckLabel.setText("<html>행운&nbsp;&nbsp;" + player.getLuck() + "</html>");
            spdLabel.setText("<html>공격속도&nbsp;&nbsp;" + player.getSpd() + "</html>");
            boolean has = player.getStatPoints() > 0;
            for (JButton b : plusButtons) b.setEnabled(has);
        };

        card.add(statRow(hpLabel, "+" + Player.HP_PER_POINT + "/포인트", plusButtons,
                () -> { player.spendStatPoint(Player.Stat.HP); refresh[0].run(); }));
        card.add(statRow(mpLabel, "+" + Player.MP_PER_POINT + "/포인트", plusButtons,
                () -> { player.spendStatPoint(Player.Stat.MP); refresh[0].run(); }));
        card.add(statRow(atkLabel, "+" + Player.ATK_PER_POINT + "/포인트", plusButtons,
                () -> { player.spendStatPoint(Player.Stat.ATK); refresh[0].run(); }));
        card.add(statRow(defLabel, "+" + Player.DEF_PER_POINT + "/포인트", plusButtons,
                () -> { player.spendStatPoint(Player.Stat.DEF); refresh[0].run(); }));
        card.add(statRow(luckLabel, "+" + Player.LUCK_PER_POINT + "/포인트", plusButtons,
                () -> { player.spendStatPoint(Player.Stat.LUCK); refresh[0].run(); }));
        card.add(statRow(spdLabel, "+" + Player.SPD_PER_POINT + "/포인트", plusButtons,
                () -> { player.spendStatPoint(Player.Stat.SPD); refresh[0].run(); }));

        refresh[0].run();
        dialog.add(card, BorderLayout.CENTER);

        JButton close = Theme.button("나가기");
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

        dialog.setSize(460, 500);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JPanel statRow(JLabel valueLabel, String hint, List<JButton> plusButtons, Runnable onPlus) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(true);
        row.setBackground(Theme.SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                new EmptyBorder(0, 16, 0, 14)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        valueLabel.setFont(Theme.HEADER_FONT);
        valueLabel.setForeground(Theme.TEXT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel hintLabel = Theme.body(hint);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        left.add(valueLabel);
        left.add(hintLabel);
        row.add(left, BorderLayout.WEST);

        JButton plus = Theme.primaryButton("+1");
        plus.addActionListener(e -> onPlus.run());
        plusButtons.add(plus);
        JPanel btnWrap = new JPanel(new GridBagLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(plus);
        row.add(btnWrap, BorderLayout.EAST);

        return row;
    }
}
