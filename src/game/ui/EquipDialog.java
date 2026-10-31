package game.ui;

import game.model.GearPiece;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Inventory / equipment, drawn as a pencil sketch on a notebook page (see Sketch). */
public class EquipDialog extends JDialog {
    private static final int WEAPONS = 0, ARMOR = 1, CONSUMABLES = 2;

    private final Player player;
    private final JLabel currentLabel = new JLabel();

    private final DefaultListModel<GearPiece> weaponModel = new DefaultListModel<>();
    private final JList<GearPiece> weaponList = new JList<>(weaponModel);
    private final DefaultListModel<GearPiece> armorModel = new DefaultListModel<>();
    private final JList<GearPiece> armorList = new JList<>(armorModel);
    private final DefaultListModel<Map.Entry<String, Integer>> consumableModel = new DefaultListModel<>();
    private final JList<Map.Entry<String, Integer>> consumableList = new JList<>(consumableModel);

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
    private final JButton detailLock = new Sketch.Button("잠그기", false);

    private int activeTab = WEAPONS;

    public EquipDialog(JFrame owner, Player player) {
        super(owner, "장비 변경", true);
        this.player = player;
        setSize(760, 580);
        setMinimumSize(new Dimension(620, 460));
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
        for (JList<GearPiece> list : List.of(weaponList, armorList)) {
            list.setCellRenderer(new EquipRowRenderer());
            list.setFixedCellHeight(60);
            list.setFixedCellWidth(1);   // rows take the list's width, however long a name is
            list.setOpaque(false);
            list.addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) showDetail();
            });
        }
        consumableList.setCellRenderer(new ConsumableRowRenderer());
        consumableList.setFixedCellHeight(60);
        consumableList.setOpaque(false);
        consumableList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showDetail();
        });

        Sketch.Tabs tabs = new Sketch.Tabs(4101);
        tabs.addTab("무기", Sketch.scroll(weaponList));
        tabs.addTab("방어구", Sketch.scroll(armorList));
        tabs.addTab("소모품", Sketch.scroll(consumableList));
        tabs.onChange(i -> {
            activeTab = i;
            showDetail();
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
        detailLock.addActionListener(e -> toggleLock());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(detailAction);
        buttons.add(detailLock);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        for (JComponent c : new JComponent[]{detailIcon, detailName, detailStat, detailStatus, buttons}) {
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
        detailBody.add(buttons);

        detailCardHost.add(placeholderWrap, "EMPTY");
        detailCardHost.add(detailBody, "ITEM");
        detailCards.show(detailCardHost, "EMPTY");
        wrap.add(detailCardHost, BorderLayout.CENTER);
        return wrap;
    }

    private GearPiece selectedPiece() {
        return activeTab == WEAPONS ? weaponList.getSelectedValue() : activeTab == ARMOR ? armorList.getSelectedValue() : null;
    }

    private void showDetail() {
        if (activeTab == CONSUMABLES) {
            Map.Entry<String, Integer> entry = consumableList.getSelectedValue();
            if (entry == null) {
                detailCards.show(detailCardHost, "EMPTY");
                return;
            }
            Item item = ItemCatalog.get(entry.getKey());
            detailIcon.setIcon(ItemArt.item(item, 80));
            detailName.setText(item.getName());
            detailName.setForeground(Sketch.INK);
            detailStat.setText(Sketch.itemDetailHtml(item));
            detailStatus.setText("가진 개수: " + entry.getValue() + "개");
            detailAction.setVisible(false);
            detailLock.setVisible(false);
            detailCards.show(detailCardHost, "ITEM");
            return;
        }
        GearPiece piece = selectedPiece();
        if (piece == null) {
            detailCards.show(detailCardHost, "EMPTY");
            return;
        }
        Item item = piece.getItem();
        boolean worn = player.isWorn(piece);
        detailIcon.setIcon(ItemArt.piece(piece, 80));
        detailName.setText(piece.getDisplayName());
        detailName.setForeground(Sketch.gradeInk(item.getGrade()));
        detailStat.setText(Sketch.itemDetailHtml(item, piece.getStatDetail()));
        detailStatus.setText(worn ? "√ 지금 장착 중" : piece.isLocked() ? "잠금: 판매·분해되지 않아요" : " ");
        detailAction.setVisible(true);
        detailLock.setVisible(true);
        detailAction.setText(worn ? "장착 해제" : "장착하기");
        detailAction.setEnabled(!piece.isDestroyed());
        detailLock.setText(piece.isLocked() ? "잠금 해제" : "잠그기");
        detailCards.show(detailCardHost, "ITEM");
    }

    private void toggleEquip() {
        GearPiece piece = selectedPiece();
        if (piece == null) return;
        if (player.isWorn(piece)) player.unequip(piece);
        else player.wear(piece);
        refresh();
        showDetail();
    }

    private void toggleLock() {
        GearPiece piece = selectedPiece();
        if (piece == null) return;
        piece.setLocked(!piece.isLocked());
        refresh();
        showDetail();
    }

    private void refresh() {
        currentLabel.setText("무기: " + (player.getWeapon() == null ? "없음" : player.getWeapon().getDisplayName())
                + "      방어구: " + (player.getArmor() == null ? "없음" : player.getArmor().getDisplayName()));

        List<GearPiece> sorted = new ArrayList<>(player.getGear());
        sorted.sort(Comparator.comparing((GearPiece g) -> !player.isWorn(g)).thenComparing(GearPiece::isDestroyed)
                .thenComparing(g -> -g.getItem().getGrade().ordinal()).thenComparing(g -> -g.getValue()));
        fill(weaponList, weaponModel, sorted, true);
        fill(armorList, armorModel, sorted, false);

        consumableModel.clear();
        for (Map.Entry<String, Integer> e : player.getConsumableCounts().entrySet()) consumableModel.addElement(e);
        repaint();
    }

    /** Refills a gear list, keeping the selected piece selected. */
    private static void fill(JList<GearPiece> list, DefaultListModel<GearPiece> model, List<GearPiece> pieces, boolean weapons) {
        GearPiece selected = list.getSelectedValue();
        model.clear();
        for (GearPiece g : pieces) if (g.isWeapon() == weapons) model.addElement(g);
        if (selected != null && model.contains(selected)) list.setSelectedValue(selected, false);
    }

    // ---------- row renderers ----------
    private final class EquipRowRenderer implements ListCellRenderer<GearPiece> {
        private final Sketch.Row row = new Sketch.Row();

        @Override
        public Component getListCellRendererComponent(JList<? extends GearPiece> list, GearPiece piece, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String state = player.isWorn(piece) ? (piece.isLocked() ? "장착 · 잠금" : "장착") : piece.isLocked() ? "잠금" : "";
            return row.set(piece.getItem(), piece.getDisplayName(), piece.getStatText(), isSelected, index).note(state)
                    .icon(ItemArt.piece(piece, 44));
        }
    }

    private static final class ConsumableRowRenderer implements ListCellRenderer<Map.Entry<String, Integer>> {
        private final Sketch.Row row = new Sketch.Row();

        @Override
        public Component getListCellRendererComponent(JList<? extends Map.Entry<String, Integer>> list, Map.Entry<String, Integer> entry,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            Item item = ItemCatalog.get(entry.getKey());
            return row.set(item, entry.getKey() + " x" + entry.getValue(), item.getStatText(), isSelected, index);
        }
    }
}
