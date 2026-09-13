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
        dialog.setLayout(new BorderLayout(16, 16));
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(20, 22, 18, 22));

        JLabel header = Theme.header("스탯 분배");
        header.setForeground(Theme.ACCENT);
        dialog.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel pointsLabel = Theme.header("");
        center.add(pointsLabel);
        center.add(Box.createVerticalStrut(12));

        List<JButton> plusButtons = new ArrayList<>();
        JLabel hpLabel = Theme.body("");
        JLabel mpLabel = Theme.body("");
        JLabel atkLabel = Theme.body("");
        JLabel defLabel = Theme.body("");
        JLabel luckLabel = Theme.body("");

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            pointsLabel.setText("남은 스탯 포인트: " + player.getStatPoints());
            hpLabel.setText("<html>HP&nbsp;&nbsp;" + player.getHp() + "/" + player.getMaxHp()
                    + " &nbsp;<font color='#8c8f9a'>(+" + Player.HP_PER_POINT + "/포인트)</font></html>");
            mpLabel.setText("<html>MP&nbsp;&nbsp;" + player.getMp() + "/" + player.getMaxMp()
                    + " &nbsp;<font color='#8c8f9a'>(+" + Player.MP_PER_POINT + "/포인트)</font></html>");
            atkLabel.setText("<html>공격력&nbsp;&nbsp;&nbsp;" + player.getBaseAtk()
                    + " &nbsp;<font color='#8c8f9a'>(+" + Player.ATK_PER_POINT + "/포인트)</font></html>");
            defLabel.setText("<html>방어력&nbsp;&nbsp;&nbsp;" + player.getBaseDef()
                    + " &nbsp;<font color='#8c8f9a'>(+" + Player.DEF_PER_POINT + "/포인트)</font></html>");
            luckLabel.setText("<html>행운&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + player.getLuck()
                    + " &nbsp;<font color='#8c8f9a'>(+" + Player.LUCK_PER_POINT + "/포인트)</font></html>");
            boolean has = player.getStatPoints() > 0;
            for (JButton b : plusButtons) b.setEnabled(has);
        };

        center.add(row(hpLabel, plusButtons, () -> { player.spendStatPoint(Player.Stat.HP); refresh[0].run(); }));
        center.add(Box.createVerticalStrut(6));
        center.add(row(mpLabel, plusButtons, () -> { player.spendStatPoint(Player.Stat.MP); refresh[0].run(); }));
        center.add(Box.createVerticalStrut(6));
        center.add(row(atkLabel, plusButtons, () -> { player.spendStatPoint(Player.Stat.ATK); refresh[0].run(); }));
        center.add(Box.createVerticalStrut(6));
        center.add(row(defLabel, plusButtons, () -> { player.spendStatPoint(Player.Stat.DEF); refresh[0].run(); }));
        center.add(Box.createVerticalStrut(6));
        center.add(row(luckLabel, plusButtons, () -> { player.spendStatPoint(Player.Stat.LUCK); refresh[0].run(); }));

        refresh[0].run();
        dialog.add(center, BorderLayout.CENTER);

        JButton close = Theme.primaryButton("닫기");
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        south.setOpaque(false);
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);

        JButton[] navButtons = new JButton[plusButtons.size() + 1];
        for (int i = 0; i < plusButtons.size(); i++) navButtons[i] = plusButtons.get(i);
        navButtons[plusButtons.size()] = close;
        Theme.arrowNav(navButtons);
        Theme.focusFirst(navButtons);

        dialog.setSize(400, 400);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    private static JPanel row(JLabel label, List<JButton> plusButtons, Runnable onPlus) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        r.add(label, BorderLayout.CENTER);
        JButton plus = Theme.button("+1");
        plus.addActionListener(e -> onPlus.run());
        plusButtons.add(plus);
        r.add(plus, BorderLayout.EAST);
        return r;
    }
}
