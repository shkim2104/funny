package game.ui;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/** The town shop, drawn as a pencil sketch on a notebook page (see Sketch). Boss gear is never sold here. */
public class ShopDialog extends JDialog {
    private final Player player;
    private final JLabel goldLabel = new JLabel();

    private final DefaultListModel<Item> buyModel = new DefaultListModel<>();
    private final JList<Item> buyList = new JList<>(buyModel);
    private final DefaultListModel<SellEntry> sellModel = new DefaultListModel<>();
    private final JList<SellEntry> sellList = new JList<>(sellModel);

    private final JPanel detailBody = new JPanel();
    private final JLabel detailPlaceholder = new JLabel(
            "<html><div style='text-align:center;width:170px'>왼쪽 목록에서 아이템을 고르면<br>여기에 적어 둘게요.</div></html>");
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailCardHost = new JPanel();
    private final JLabel detailIcon = new JLabel();
    private final JLabel detailName = new JLabel();
    private final JLabel detailStat = new JLabel();
    private final JLabel detailPrice = new JLabel();
    private final JLabel detailOwned = new JLabel();
    private final JButton detailAction = new Sketch.Button("구매하기", true);

    private boolean sellMode = false;
    private Item selectedBuyItem;
    private SellEntry selectedSellEntry;

    public ShopDialog(JFrame owner, Player player) {
        super(owner, "상점", true);
        this.player = player;
        setSize(720, 560);
        setMinimumSize(new Dimension(580, 440));
        setLocationRelativeTo(owner);
        Sketch.Page page = new Sketch.Page(new BorderLayout(14, 12), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 22));
        setContentPane(page);

        goldLabel.setFont(Sketch.hand(Font.BOLD, 18f));
        goldLabel.setForeground(Sketch.BLUE_PENCIL);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("상점"), BorderLayout.WEST);
        head.add(goldLabel, BorderLayout.EAST);
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
        buyList.setCellRenderer(new BuyRowRenderer());
        buyList.setFixedCellHeight(60);
        buyList.setOpaque(false);
        buyList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedBuyItem = buyList.getSelectedValue();
            showBuyDetail(selectedBuyItem);
        });

        sellList.setCellRenderer(new SellRowRenderer());
        sellList.setFixedCellHeight(60);
        sellList.setOpaque(false);
        sellList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedSellEntry = sellList.getSelectedValue();
            showSellDetail(selectedSellEntry);
        });

        Sketch.Tabs tabs = new Sketch.Tabs(5101);
        tabs.addTab("구매", Sketch.scroll(buyList));
        tabs.addTab("판매", Sketch.scroll(sellList));
        tabs.onChange(i -> {
            sellMode = i == 1;
            if (sellMode) showSellDetail(selectedSellEntry);
            else showBuyDetail(selectedBuyItem);
        });
        return tabs;
    }

    // ---------- right: selected item detail ----------
    private JComponent buildDetailArea() {
        Sketch.Box wrap = new Sketch.Box(new BorderLayout(), 5202);
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
        detailOwned.setFont(Sketch.hand(Font.PLAIN, 13f));
        detailOwned.setForeground(Sketch.INK_SOFT);
        detailPrice.setFont(Sketch.hand(Font.BOLD, 17f));
        detailPrice.setForeground(Sketch.BLUE_PENCIL);
        detailAction.addActionListener(e -> {
            if (sellMode) sell();
            else buy();
        });
        for (JComponent c : new JComponent[]{detailIcon, detailName, detailStat, detailOwned, detailPrice, detailAction}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        detailBody.add(detailIcon);
        detailBody.add(Box.createVerticalStrut(6));
        detailBody.add(detailName);
        detailBody.add(Box.createVerticalStrut(8));
        detailBody.add(detailStat);
        detailBody.add(Box.createVerticalStrut(6));
        detailBody.add(detailOwned);
        detailBody.add(Box.createVerticalGlue());
        detailBody.add(detailPrice);
        detailBody.add(Box.createVerticalStrut(10));
        detailBody.add(detailAction);

        detailCardHost.add(placeholderWrap, "EMPTY");
        detailCardHost.add(detailBody, "ITEM");
        detailCards.show(detailCardHost, "EMPTY");
        wrap.add(detailCardHost, BorderLayout.CENTER);
        return wrap;
    }

    private void showItem(Item item) {
        detailIcon.setIcon(ItemArt.item(item, 80));
        detailName.setText(item.getName());
        detailName.setForeground(Sketch.gradeInk(item.getGrade()));
        detailStat.setText(Sketch.itemDetailHtml(item));
    }

    private void showBuyDetail(Item item) {
        if (item == null) {
            detailCards.show(detailCardHost, "EMPTY");
            return;
        }
        showItem(item);
        detailOwned.setText(" ");
        detailPrice.setText(item.getPrice() + "G");
        detailAction.setText("구매하기");
        detailAction.setEnabled(player.getGold() >= item.getPrice());
        detailCards.show(detailCardHost, "ITEM");
    }

    private void showSellDetail(SellEntry entry) {
        if (entry == null) {
            detailCards.show(detailCardHost, "EMPTY");
            return;
        }
        Item item = ItemCatalog.get(entry.name);
        showItem(item);
        detailOwned.setText("가진 개수: " + entry.count + "개");
        detailPrice.setText("판매가 " + (item.getPrice() / 2) + "G");
        detailAction.setText("판매하기");
        detailAction.setEnabled(true);
        detailCards.show(detailCardHost, "ITEM");
    }

    private void buy() {
        if (selectedBuyItem == null) return;
        Item item = selectedBuyItem;
        if (player.getGold() < item.getPrice()) {
            Dialogs.message(this, "알림", "골드가 부족합니다.");
            return;
        }
        player.spendGold(item.getPrice());
        player.addItem(item.getName());
        refresh();
        showBuyDetail(item);
    }

    private void sell() {
        if (selectedSellEntry == null) return;
        String name = selectedSellEntry.name;
        Item item = ItemCatalog.get(name);
        player.removeItem(name);
        player.earnGold(item.getPrice() / 2);
        selectedSellEntry = null;
        refresh();
        showSellDetail(null);
    }

    private void refresh() {
        goldLabel.setText("소지금  " + player.getGold() + "G");

        buyModel.clear();
        for (Item item : ItemCatalog.shopItems()) buyModel.addElement(item);

        Map<String, Integer> counts = player.getItemCounts();
        sellModel.clear();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sellModel.addElement(new SellEntry(e.getKey(), e.getValue()));
        }

        if (selectedBuyItem != null) detailAction.setEnabled(!sellMode && player.getGold() >= selectedBuyItem.getPrice());
        repaint();
    }

    private static class SellEntry {
        final String name;
        final int count;
        SellEntry(String name, int count) { this.name = name; this.count = count; }
    }

    // ---------- row renderers ----------
    private static class BuyRowRenderer implements ListCellRenderer<Item> {
        private final Sketch.Row row = new Sketch.Row();

        @Override
        public Component getListCellRendererComponent(JList<? extends Item> list, Item item, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            return row.set(item, item.getName(), item.getPrice() + "G", isSelected, index);
        }
    }

    private static class SellRowRenderer implements ListCellRenderer<SellEntry> {
        private final Sketch.Row row = new Sketch.Row();

        @Override
        public Component getListCellRendererComponent(JList<? extends SellEntry> list, SellEntry entry, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Item item = ItemCatalog.get(entry.name);
            return row.set(item, entry.name + " x" + entry.count, (item.getPrice() / 2) + "G", isSelected, index);
        }
    }
}
