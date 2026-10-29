package game.ui;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Job;
import game.model.Player;
import game.model.Skill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Character status as a notebook page of pencil notes: gauges, stats with icons, gear, skills, bag. */
public class StatusDialog {
    private StatusDialog() {}

    public static void show(JFrame owner, Player player) {
        JDialog dialog = new JDialog(owner, "상태", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 10), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 20));
        dialog.setContentPane(page);

        // ---------- header: name, level and job ----------
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title(player.getName()), BorderLayout.WEST);
        head.add(Sketch.label("Lv." + player.getLevel() + "  " + player.getJob().getDisplayName(), Font.BOLD, 17f, Sketch.BLUE_PENCIL),
                BorderLayout.EAST);
        dialog.add(head, BorderLayout.NORTH);

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(0, 0, 0, 10));

        if (player.getJobPath().size() > 1) {
            List<String> path = new ArrayList<>();
            for (Job j : player.getJobPath()) path.add(j.getDisplayName());
            add(body, note("전직: " + String.join(" → ", path), Sketch.INK_SOFT));
        }

        // ---------- gauges ----------
        add(body, gauge("HP", Sketch.RED_PENCIL, player.getHp(), player.getMaxHp(), 11));
        add(body, gauge("MP", Sketch.BLUE_PENCIL, player.getMp(), player.getMaxMp(), 12));
        add(body, gauge("EXP", new Color(196, 140, 20), player.getExp(), player.getExpToNext(), 13));

        // ---------- stats ----------
        add(body, section("능력치"));
        JPanel stats = new JPanel(new GridLayout(0, 2, 12, 4));
        stats.setOpaque(false);
        stats.add(stat("sword", "공격력", player.getAtk() + "", "기본 " + player.getBaseAtk()));
        stats.add(stat("shield", "방어력", player.getDef() + "", "기본 " + player.getBaseDef()));
        stats.add(stat("clover", "행운", player.getLuck() + "", "치명타 " + player.getCritChance() + "%"));
        stats.add(stat("wing", "공격속도", player.getSpd() + "", "높을수록 먼저"));
        stats.add(stat("star", "스탯 포인트", player.getStatPoints() + "", player.getStatPoints() > 0 ? "마을에서 분배" : " "));
        stats.add(stat("coin", "골드", player.getGold() + "G", " "));
        add(body, stats);

        // ---------- gear ----------
        add(body, section("장비"));
        add(body, gear("무기", player.getWeaponName()));
        add(body, gear("방어구", player.getArmorName()));

        // ---------- skills ----------
        add(body, section("스킬"));
        List<Skill> skills = player.getSkills();
        if (skills.isEmpty()) {
            add(body, note(player.getJob() == Job.BEGINNER ? "Lv.10에 마을 전직소에서 전직하면 배워요." : "없음", Sketch.INK_SOFT));
        } else {
            for (Skill s : skills) {
                add(body, note("<b>" + s.getName() + "</b> <font color='" + Sketch.hex(Sketch.BLUE_PENCIL) + "'>MP " + s.getMpCost()
                        + "</font><br><font color='" + Sketch.hex(Sketch.INK_SOFT) + "'>" + s.getDescription() + "</font>", Sketch.INK));
            }
        }

        // ---------- bag ----------
        add(body, section("소지품"));
        Map<String, Integer> counts = player.getItemCounts();
        List<String> bag = new ArrayList<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) bag.add(e.getKey() + " x" + e.getValue());
        add(body, note(bag.isEmpty() ? "없음" : String.join(", ", bag), Sketch.INK));

        JScrollPane scroll = Sketch.scroll(body);
        dialog.add(scroll, BorderLayout.CENTER);

        JButton close = new Sketch.Button("닫기", false);
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);
        Theme.focusFirst(close);

        dialog.setSize(540, 640);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private static void add(JPanel body, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(c);
        body.add(Box.createVerticalStrut(6));
    }

    private static JComponent section(String title) {
        JLabel l = Sketch.title(title);
        l.setFont(Sketch.hand(Font.BOLD, 17f));
        l.setBorder(new EmptyBorder(10, 0, 4, 0));
        return l;
    }

    private static JComponent gauge(String name, Color color, int value, int max, long seed) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        JLabel label = Sketch.label(name, Font.BOLD, 15f, Sketch.INK);
        label.setPreferredSize(new Dimension(44, 34));
        Sketch.Bar bar = new Sketch.Bar(color, seed);
        bar.set(value, max);
        row.add(label, BorderLayout.WEST);
        row.add(bar, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        return row;
    }

    private static JComponent stat(String icon, String name, String value, String note) {
        JPanel cell = new JPanel(new BorderLayout(8, 0));
        cell.setOpaque(false);
        cell.add(new JLabel(ItemArt.augment(icon, 34)), BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.setOpaque(false);
        top.add(Sketch.label(name + "  ", Font.BOLD, 14f, Sketch.INK));
        top.add(Sketch.label(value, Font.BOLD, 16f, Sketch.BLUE_PENCIL));
        text.add(top);
        text.add(Sketch.label(note, Font.PLAIN, 11f, Sketch.INK_SOFT));
        cell.add(text, BorderLayout.CENTER);
        return cell;
    }

    private static JComponent gear(String slot, String itemName) {
        Item item = itemName == null ? null : ItemCatalog.get(itemName);
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(new JLabel(ItemArt.item(item, 44)), BorderLayout.WEST);
        StringBuilder html = new StringBuilder("<html><body style='width:300px'><font color='")
                .append(Sketch.hex(Sketch.INK_SOFT)).append("'>").append(slot).append("</font>  ");
        if (item == null) {
            html.append("<font color='").append(Sketch.hex(Sketch.INK_SOFT)).append("'>없음</font>");
        } else {
            html.append("<b><font color='").append(Sketch.hex(Sketch.gradeInk(item.getGrade()))).append("'>")
                    .append(item.getName()).append("</font></b>  <font color='").append(Sketch.hex(Sketch.BLUE_PENCIL)).append("'>")
                    .append(item.getStatText()).append("</font>");
            for (String line : item.getBonus().lines()) {
                html.append("<br><font color='").append(Sketch.hex(Sketch.RED_PENCIL)).append("'>✎ ").append(line).append("</font>");
            }
        }
        JLabel text = new JLabel(html.append("</body></html>").toString());
        text.setFont(Sketch.hand(Font.PLAIN, 14f));
        text.setForeground(Sketch.INK);
        row.add(text, BorderLayout.CENTER);
        return row;
    }

    private static JComponent note(String html, Color color) {
        JLabel l = new JLabel("<html><body style='width:380px'>✎ " + html + "</body></html>");
        l.setFont(Sketch.hand(Font.PLAIN, 14f));
        l.setForeground(color);
        return l;
    }
}
