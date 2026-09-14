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
    private final JLabel currentLabel = new JLabel();

    private final DefaultListModel<String> weaponModel = new DefaultListModel<>();
    private final JList<String> weaponList = new JList<>(weaponModel);
    private final DefaultListModel<String> armorModel = new DefaultListModel<>();
    private final JList<String> armorList = new JList<>(armorModel);

    private final JPanel detailBody = new JPanel();
    private final JLabel detailPlaceholder = Theme.body("<html><div style='text-align:center;width:160px'>왼쪽 목록에서 아이템을 선택하면<br>여기에 상세 정보가 표시됩니다.</div></html>");
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailCardHost = new JPanel();
    private final JLabel detailName = new JLabel();
    private final JLabel detailStat = Theme.body("");
    private final JLabel detailStatus = new JLabel();
    private final JButton detailAction = Theme.primaryButton("장착하기");

    private Item.Type activeType = Item.Type.WEAPON;
    private String selectedWeapon;
    private String selectedArmor;

    public EquipDialog(JFrame owner, Player player) {
        super(owner, "장비 변경", true);
        this.player = player;
        Theme.styleDialog(this);
        setSize(680, 480);
        setMinimumSize(new Dimension(560, 400));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(14, 14));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(18, 18, 16, 18));

        currentLabel.setForeground(Theme.ACCENT);
        currentLabel.setFont(Theme.TITLE_FONT.deriveFont(17f));
        add(currentLabel, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 14, 0));
        center.setOpaque(false);
        center.add(buildListArea());
        center.add(buildDetailArea());
        add(center, BorderLayout.CENTER);

        JButton closeBtn = Theme.button("나가기");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    // ---------- left: tabbed item list ----------
    private JComponent buildListArea() {
        weaponList.setCellRenderer(new EquipRowRenderer());
        weaponList.setFixedCellHeight(56);
        weaponList.setBackground(Theme.SURFACE);
        weaponList.setSelectionBackground(Theme.SURFACE);
        weaponList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedWeapon = weaponList.getSelectedValue();
            showDetail(selectedWeapon);
        });

        armorList.setCellRenderer(new EquipRowRenderer());
        armorList.setFixedCellHeight(56);
        armorList.setBackground(Theme.SURFACE);
        armorList.setSelectionBackground(Theme.SURFACE);
        armorList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedArmor = armorList.getSelectedValue();
            showDetail(selectedArmor);
        });

        JTabbedPane tabs = new JTabbedPane();
        Theme.styleTabs(tabs);
        tabs.addTab("무기", Theme.scroll(weaponList));
        tabs.addTab("방어구", Theme.scroll(armorList));
        tabs.addChangeListener(e -> {
            activeType = tabs.getSelectedIndex() == 0 ? Item.Type.WEAPON : Item.Type.ARMOR;
            showDetail(activeType == Item.Type.WEAPON ? selectedWeapon : selectedArmor);
        });
        return tabs;
    }

    // ---------- right: selected item detail ----------
    private JComponent buildDetailArea() {
        JPanel wrap = Theme.panel(new BorderLayout());
        wrap.setBorder(new EmptyBorder(16, 16, 16, 16));

        detailCardHost.setLayout(detailCards);
        detailCardHost.setOpaque(false);

        detailPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel placeholderWrap = new JPanel(new GridBagLayout());
        placeholderWrap.setOpaque(false);
        placeholderWrap.add(detailPlaceholder);

        detailBody.setOpaque(false);
        detailBody.setLayout(new BoxLayout(detailBody, BoxLayout.Y_AXIS));

        detailName.setFont(Theme.TITLE_FONT.deriveFont(19f));
        detailName.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailStat.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailStat.setFont(Theme.BODY_FONT.deriveFont(15f));

        detailStatus.setFont(Theme.HEADER_FONT);
        detailStatus.setForeground(Theme.ACCENT);
        detailStatus.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailAction.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailAction.addActionListener(e -> toggleEquip());

        detailBody.add(detailName);
        detailBody.add(Box.createVerticalStrut(10));
        detailBody.add(detailStat);
        detailBody.add(Box.createVerticalGlue());
        detailBody.add(detailStatus);
        detailBody.add(Box.createVerticalStrut(12));
        detailBody.add(detailAction);

        detailCardHost.add(placeholderWrap, "EMPTY");
        detailCardHost.add(detailBody, "ITEM");
        detailCards.show(detailCardHost, "EMPTY");

        wrap.add(detailCardHost, BorderLayout.CENTER);
        return wrap;
    }

    private void showDetail(String itemName) {
        if (itemName == null) {
            detailCards.show(detailCardHost, "EMPTY");
            return;
        }
        Item item = ItemCatalog.get(itemName);
        boolean equipped = itemName.equals(activeType == Item.Type.WEAPON ? player.getWeaponName() : player.getArmorName());

        detailName.setText(item.getName());
        detailName.setForeground(Theme.gradeColor(item));
        detailStat.setText("<html>" + Theme.gradeTagHtml(item) + " " + item.getTypeLabel() + "<br>" + item.getStatText() + "</html>");
        detailStatus.setText(equipped ? "현재 장착 중" : " ");
        detailAction.setText(equipped ? "장착 해제" : "장착하기");
        detailCards.show(detailCardHost, "ITEM");
    }

    private void toggleEquip() {
        String selected = activeType == Item.Type.WEAPON ? selectedWeapon : selectedArmor;
        if (selected == null) return;
        boolean equipped = selected.equals(activeType == Item.Type.WEAPON ? player.getWeaponName() : player.getArmorName());
        if (activeType == Item.Type.WEAPON) {
            player.setWeaponName(equipped ? null : selected);
        } else {
            player.setArmorName(equipped ? null : selected);
        }
        refresh();
        showDetail(selected);
    }

    private void refresh() {
        currentLabel.setText("현재 무기: " + (player.getWeaponName() == null ? "없음" : player.getWeaponName())
                + "     현재 방어구: " + (player.getArmorName() == null ? "없음" : player.getArmorName()));

        List<String> weapons = player.getItemsByType(Item.Type.WEAPON);
        weaponModel.clear();
        for (String name : weapons) weaponModel.addElement(name);

        List<String> armors = player.getItemsByType(Item.Type.ARMOR);
        armorModel.clear();
        for (String name : armors) armorModel.addElement(name);
    }

    // ---------- row renderer ----------
    private static class EquipRowRenderer extends JPanel implements ListCellRenderer<String> {
        private final JLabel tag = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel stat = new JLabel();

        EquipRowRenderer() {
            setLayout(new BorderLayout(10, 0));
            setBorder(new EmptyBorder(0, 14, 0, 14));
            JPanel left = new JPanel();
            left.setOpaque(false);
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            tag.setFont(Theme.SMALL_FONT);
            name.setFont(Theme.HEADER_FONT);
            left.add(tag);
            left.add(name);
            add(left, BorderLayout.WEST);
            stat.setFont(Theme.HEADER_FONT);
            stat.setForeground(Theme.ACCENT);
            add(stat, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String itemName, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            Item item = ItemCatalog.get(itemName);
            tag.setText(item.getGrade().getLabel());
            tag.setForeground(Theme.gradeColor(item));
            name.setText(item.getName());
            name.setForeground(Theme.TEXT);
            stat.setText(item.getStatText());
            setOpaque(true);
            setBackground(isSelected ? new Color(58, 50, 34) : Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    new EmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }
}
