package game.ui;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class EquipDialog extends JDialog {
    private final Player player;
    private final JLabel currentLabel = Theme.header("");
    private final JList<String> weaponList = new JList<>();
    private final JList<String> armorList = new JList<>();

    public EquipDialog(JFrame owner, Player player) {
        super(owner, "장비 변경", true);
        this.player = player;
        Theme.styleDialog(this);
        setSize(460, 420);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        currentLabel.setForeground(Theme.ACCENT);
        add(currentLabel, BorderLayout.NORTH);

        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 12, 12));
        listsPanel.setOpaque(false);

        listsPanel.add(buildColumn("무기", weaponList, () -> equipSelected(weaponList, Item.Type.WEAPON),
                () -> { player.setWeaponName(null); refresh(); }));
        listsPanel.add(buildColumn("방어구", armorList, () -> equipSelected(armorList, Item.Type.ARMOR),
                () -> { player.setArmorName(null); refresh(); }));

        add(listsPanel, BorderLayout.CENTER);

        JButton closeBtn = Theme.button("나가기");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildColumn(String title, JList<String> list, Runnable onEquip, Runnable onUnequip) {
        JPanel panel = Theme.panel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.add(Theme.header(title), BorderLayout.NORTH);

        Theme.styleList(list);
        panel.add(Theme.scroll(list), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new GridLayout(2, 1, 6, 6));
        btnRow.setOpaque(false);
        JButton equipBtn = Theme.button("장착");
        equipBtn.addActionListener(e -> onEquip.run());
        JButton unequipBtn = Theme.button("해제");
        unequipBtn.addActionListener(e -> onUnequip.run());
        btnRow.add(equipBtn);
        btnRow.add(unequipBtn);
        panel.add(btnRow, BorderLayout.SOUTH);

        return panel;
    }

    private void equipSelected(JList<String> list, Item.Type type) {
        String display = list.getSelectedValue();
        if (display == null) return;
        String name = display.substring(0, display.indexOf(" ("));
        if (type == Item.Type.WEAPON) player.setWeaponName(name);
        else player.setArmorName(name);
        refresh();
    }

    private void refresh() {
        currentLabel.setText("현재 무기: " + (player.getWeaponName() == null ? "없음" : player.getWeaponName())
                + "     현재 방어구: " + (player.getArmorName() == null ? "없음" : player.getArmorName()));

        List<String> weapons = player.getItemsByType(Item.Type.WEAPON);
        weaponList.setListData(weapons.stream().map(n -> ItemCatalog.get(n).describe()).toArray(String[]::new));

        List<String> armors = player.getItemsByType(Item.Type.ARMOR);
        armorList.setListData(armors.stream().map(n -> ItemCatalog.get(n).describe()).toArray(String[]::new));
    }
}
