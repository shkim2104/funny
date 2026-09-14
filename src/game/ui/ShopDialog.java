package game.ui;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopDialog extends JDialog {
    private final Player player;
    private final JLabel goldLabel = Theme.header("");

    private final DefaultListModel<Item> buyModel = new DefaultListModel<>();
    private final JList<Item> buyList = new JList<>(buyModel);
    private final DefaultListModel<SellEntry> sellModel = new DefaultListModel<>();
    private final JList<SellEntry> sellList = new JList<>(sellModel);

    private final JPanel detailBody = new JPanel();
    private final JLabel detailPlaceholder = Theme.body("<html><div style='text-align:center;width:160px'>왼쪽 목록에서 아이템을 선택하면<br>여기에 상세 정보가 표시됩니다.</div></html>");
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailCardHost = new JPanel();
    private final JLabel detailName = new JLabel();
    private final JLabel detailStat = Theme.body("");
    private final JLabel detailPrice = new JLabel();
    private final JLabel detailOwned = Theme.body("");
    private final JButton detailAction = Theme.primaryButton("구매하기");

    private boolean sellMode = false;
    private Item selectedBuyItem;
    private SellEntry selectedSellEntry;

    public ShopDialog(JFrame owner, Player player) {
        super(owner, "상점", true);
        this.player = player;
        Theme.styleDialog(this);
        setSize(680, 480);
        setMinimumSize(new Dimension(560, 400));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(14, 14));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(18, 18, 16, 18));

        goldLabel.setForeground(Theme.ACCENT);
        goldLabel.setFont(Theme.TITLE_FONT.deriveFont(20f));
        add(goldLabel, BorderLayout.NORTH);

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
        buyList.setCellRenderer(new BuyRowRenderer());
        buyList.setFixedCellHeight(56);
        buyList.setBackground(Theme.SURFACE);
        buyList.setSelectionBackground(Theme.SURFACE);
        buyList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedBuyItem = buyList.getSelectedValue();
            showBuyDetail(selectedBuyItem);
        });

        sellList.setCellRenderer(new SellRowRenderer());
        sellList.setFixedCellHeight(56);
        sellList.setBackground(Theme.SURFACE);
        sellList.setSelectionBackground(Theme.SURFACE);
        sellList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            selectedSellEntry = sellList.getSelectedValue();
            showSellDetail(selectedSellEntry);
        });

        JTabbedPane tabs = new JTabbedPane();
        Theme.styleTabs(tabs);
        tabs.addTab("구매", Theme.scroll(buyList));
        tabs.addTab("판매", Theme.scroll(sellList));
        tabs.addChangeListener(e -> {
            sellMode = tabs.getSelectedIndex() == 1;
            if (sellMode) showSellDetail(selectedSellEntry);
            else showBuyDetail(selectedBuyItem);
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

        detailPrice.setFont(Theme.HEADER_FONT);
        detailPrice.setForeground(Theme.ACCENT);
        detailPrice.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailOwned.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailAction.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailAction.addActionListener(e -> {
            if (sellMode) sell();
            else buy();
        });

        detailBody.add(detailName);
        detailBody.add(Box.createVerticalStrut(10));
        detailBody.add(detailStat);
        detailBody.add(Box.createVerticalStrut(6));
        detailBody.add(detailOwned);
        detailBody.add(Box.createVerticalGlue());
        detailBody.add(detailPrice);
        detailBody.add(Box.createVerticalStrut(12));
        detailBody.add(detailAction);

        detailCardHost.add(placeholderWrap, "EMPTY");
        detailCardHost.add(detailBody, "ITEM");
        detailCards.show(detailCardHost, "EMPTY");

        wrap.add(detailCardHost, BorderLayout.CENTER);
        return wrap;
    }

    private void showBuyDetail(Item item) {
        if (item == null) {
            detailCards.show(detailCardHost, "EMPTY");
            return;
        }
        detailName.setText(item.getName());
        detailName.setForeground(Theme.gradeColor(item));
        detailStat.setText("<html>" + Theme.gradeTagHtml(item) + " " + item.getTypeLabel() + "<br>" + item.getStatText() + "</html>");
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
        int sellPrice = item.getPrice() / 2;
        detailName.setText(item.getName());
        detailName.setForeground(Theme.gradeColor(item));
        detailStat.setText("<html>" + Theme.gradeTagHtml(item) + " " + item.getTypeLabel() + "<br>" + item.getStatText() + "</html>");
        detailOwned.setText("보유 수량: " + entry.count + "개");
        detailPrice.setText("판매가 " + sellPrice + "G");
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
        goldLabel.setText("소지금: " + player.getGold() + "G");

        buyModel.clear();
        for (Item item : ItemCatalog.all()) buyModel.addElement(item);

        Map<String, Integer> counts = player.getItemCounts();
        sellModel.clear();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            sellModel.addElement(new SellEntry(e.getKey(), e.getValue()));
        }

        if (selectedBuyItem != null) detailAction.setEnabled(!sellMode && player.getGold() >= selectedBuyItem.getPrice());
    }

    private static class SellEntry {
        final String name;
        final int count;
        SellEntry(String name, int count) { this.name = name; this.count = count; }
    }

    // ---------- row renderers ----------
    private static class BuyRowRenderer extends JPanel implements ListCellRenderer<Item> {
        private final JLabel tag = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel price = new JLabel();

        BuyRowRenderer() {
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
            price.setFont(Theme.HEADER_FONT);
            price.setForeground(Theme.ACCENT);
            add(price, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Item> list, Item item, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            tag.setText(item.getGrade().getLabel());
            tag.setForeground(Theme.gradeColor(item));
            name.setText(item.getName());
            name.setForeground(Theme.TEXT);
            price.setText(item.getPrice() + "G");
            setOpaque(true);
            setBackground(isSelected ? new Color(58, 50, 34) : Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    new EmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }

    private static class SellRowRenderer extends JPanel implements ListCellRenderer<SellEntry> {
        private final JLabel tag = new JLabel();
        private final JLabel name = new JLabel();
        private final JLabel price = new JLabel();

        SellRowRenderer() {
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
            price.setFont(Theme.HEADER_FONT);
            price.setForeground(Theme.ACCENT);
            add(price, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends SellEntry> list, SellEntry entry, int index,
                                                        boolean isSelected, boolean cellHasFocus) {
            Item item = ItemCatalog.get(entry.name);
            tag.setText(item.getGrade().getLabel());
            tag.setForeground(Theme.gradeColor(item));
            name.setText(entry.name + " x" + entry.count);
            name.setForeground(Theme.TEXT);
            price.setText((item.getPrice() / 2) + "G");
            setOpaque(true);
            setBackground(isSelected ? new Color(58, 50, 34) : Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER),
                    new EmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }
}
