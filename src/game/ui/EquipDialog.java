package game.ui;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/** Inventory / equipment, drawn as a pencil sketch on a notebook page (see Sketch). */
public class EquipDialog extends JDialog {
    private final Player player;
    private final JLabel currentLabel = new JLabel();

    private final DefaultListModel<String> weaponModel = new DefaultListModel<>();
    private final JList<String> weaponList = new JList<>(weaponModel);
    private final DefaultListModel<String> armorModel = new DefaultListModel<>();
    private final JList<String> armorList = new JList<>(armorModel);

    private final JPanel detailBody = new JPanel();
    private final JLabel detailPlaceholder = new JLabel(
            "<html><div style='text-align:center;width:170px'>왼쪽 목록에서 아이템을 고르면<br>여기에 적어 둘게요.</div></html>");
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailCardHost = new JPanel();
    private final JLabel detailIcon = new JLabel();
    private final JLabel detailName = new JLabel();
    private final JLabel detailStat = new JLabel();
    private final JLabel detailStatus = new JLabel();
    private final JButton detailAction = new Sketch.Button("장착하기", true);

    private Item.Type activeType = Item.Type.WEAPON;
    private String selectedWeapon;
    private String selectedArmor;

    public EquipDialog(JFrame owner, Player player) {
        super(owner, "장비 변경", true);
        this.player = player;
        setSize(720, 560);
        setMinimumSize(new Dimension(580, 440));
        setLocationRelativeTo(owner);
        Sketch.Page page = new Sketch.Page(new BorderLayout(14, 12), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 22));
        setContentPane(page);

        JLabel title = Sketch.title("장비 · 인벤토리");
        currentLabel.setFont(Sketch.hand(Font.BOLD, 14f));
        currentLabel.setForeground(Sketch.INK_SOFT);
        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        head.add(title, BorderLayout.NORTH);
        head.add(currentLabel, BorderLayout.CENTER);
        add(head, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);
        center.add(buildListArea());
        center.add(buildDetailArea());
        add(center, BorderLayout.CENTER);

        JButton closeBtn = new Sketch.Button("나가기", false);
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    // ---------- left: tabbed item list ----------
    private JComponent buildListArea() {
        for (JList<String> list : List.of(weaponList, armorList)) {
            list.setCellRenderer(new EquipRowRenderer());
            list.setFixedCellHeight(60);
            list.setOpaque(false);
        }
        weaponList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedWeapon = weaponList.getSelectedValue();
            showDetail(selectedWeapon);
        });
        armorList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedArmor = armorList.getSelectedValue();
            showDetail(selectedArmor);
        });

        Sketch.Tabs tabs = new Sketch.Tabs(4101);
        tabs.addTab("무기", Sketch.scroll(weaponList));
        tabs.addTab("방어구", Sketch.scroll(armorList));
        tabs.onChange(i -> {
            activeType = i == 0 ? Item.Type.WEAPON : Item.Type.ARMOR;
            showDetail(activeType == Item.Type.WEAPON ? selectedWeapon : selectedArmor);
        });
        return tabs;
    }

    // ---------- right: selected item detail ----------
    private JComponent buildDetailArea() {
        Sketch.Box wrap = new Sketch.Box(new BorderLayout(), 4202);
        wrap.setBorder(new EmptyBorder(20, 22, 20, 22));

        detailCardHost.setLayout(detailCards);
        detailCardHost.setOpaque(false);

        detailPlaceholder.setFont(Sketch.hand(Font.PLAIN, 14f));
        detailPlaceholder.setForeground(Sketch.INK_SOFT);
        detailPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel placeholderWrap = new JPanel(new GridBagLayout());
        placeholderWrap.setOpaque(false);
        placeholderWrap.add(detailPlaceholder);

        detailBody.setOpaque(false);
        detailBody.setLayout(new BoxLayout(detailBody, BoxLayout.Y_AXIS));
        detailName.setFont(Sketch.hand(Font.BOLD, 20f));
        detailStat.setFont(Sketch.hand(Font.PLAIN, 14f));
        detailStat.setForeground(Sketch.INK);
        detailStatus.setFont(Sketch.hand(Font.BOLD, 14f));
        detailStatus.setForeground(Sketch.RED_PENCIL);
        detailAction.addActionListener(e -> toggleEquip());
        for (JComponent c : new JComponent[]{detailIcon, detailName, detailStat, detailStatus, detailAction}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        detailBody.add(detailIcon);
        detailBody.add(Box.createVerticalStrut(6));
        detailBody.add(detailName);
        detailBody.add(Box.createVerticalStrut(8));
        detailBody.add(detailStat);
        detailBody.add(Box.createVerticalGlue());
        detailBody.add(detailStatus);
        detailBody.add(Box.createVerticalStrut(10));
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

        detailIcon.setIcon(ItemArt.item(item, 80));
        detailName.setText(item.getName());
        detailName.setForeground(Sketch.gradeInk(item.getGrade()));
        detailStat.setText(Sketch.itemDetailHtml(item));
        detailStatus.setText(equipped ? "√ 지금 장착 중" : " ");
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
        currentLabel.setText("무기: " + (player.getWeaponName() == null ? "없음" : player.getWeaponName())
                + "      방어구: " + (player.getArmorName() == null ? "없음" : player.getArmorName()));

        weaponModel.clear();
        for (String name : player.getItemsByType(Item.Type.WEAPON)) weaponModel.addElement(name);
        armorModel.clear();
        for (String name : player.getItemsByType(Item.Type.ARMOR)) armorModel.addElement(name);
        repaint();
    }

    // ---------- row renderer ----------
    private final class EquipRowRenderer implements ListCellRenderer<String> {
        private final Sketch.Row row = new Sketch.Row();

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String itemName, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Item item = ItemCatalog.get(itemName);
            boolean worn = itemName.equals(player.getWeaponName()) || itemName.equals(player.getArmorName());
            return row.set(item, item.getName() + (worn ? "  √" : ""), item.getStatText(), isSelected, index);
        }
    }
}
