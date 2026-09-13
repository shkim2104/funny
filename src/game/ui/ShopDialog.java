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
    private final JList<String> buyList = new JList<>();
    private final JList<String> sellList = new JList<>();
    private List<Item> buyItems;
    private List<String> sellNames;

    public ShopDialog(JFrame owner, Player player) {
        super(owner, "상점", true);
        this.player = player;
        Theme.styleDialog(this);
        setSize(480, 440);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(12, 12));
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        goldLabel.setForeground(Theme.ACCENT);
        add(goldLabel, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 12));
        center.setOpaque(false);

        JPanel buyPanel = Theme.panel(new BorderLayout(6, 6));
        buyPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buyPanel.add(Theme.header("구매 목록"), BorderLayout.NORTH);
        Theme.styleList(buyList);
        buyPanel.add(Theme.scroll(buyList), BorderLayout.CENTER);
        JButton buyBtn = Theme.button("구매하기");
        buyBtn.addActionListener(e -> buy());
        buyPanel.add(buyBtn, BorderLayout.SOUTH);

        JPanel sellPanel = Theme.panel(new BorderLayout(6, 6));
        sellPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sellPanel.add(Theme.header("판매 목록"), BorderLayout.NORTH);
        Theme.styleList(sellList);
        sellPanel.add(Theme.scroll(sellList), BorderLayout.CENTER);
        JButton sellBtn = Theme.button("판매하기");
        sellBtn.addActionListener(e -> sell());
        sellPanel.add(sellBtn, BorderLayout.SOUTH);

        center.add(buyPanel);
        center.add(sellPanel);
        add(center, BorderLayout.CENTER);

        JButton closeBtn = Theme.button("나가기");
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    private void buy() {
        int idx = buyList.getSelectedIndex();
        if (idx < 0) return;
        Item item = buyItems.get(idx);
        if (player.getGold() < item.getPrice()) {
            Dialogs.message(this, "알림", "골드가 부족합니다.");
            return;
        }
        player.spendGold(item.getPrice());
        player.addItem(item.getName());
        refresh();
    }

    private void sell() {
        int idx = sellList.getSelectedIndex();
        if (idx < 0) return;
        String name = sellNames.get(idx);
        Item item = ItemCatalog.get(name);
        player.removeItem(name);
        player.earnGold(item.getPrice() / 2);
        refresh();
    }

    private void refresh() {
        goldLabel.setText("소지금: " + player.getGold() + "G");

        buyItems = new ArrayList<>(ItemCatalog.all());
        buyList.setListData(buyItems.stream().map(Item::describe).toArray(String[]::new));

        Map<String, Integer> counts = player.getItemCounts();
        sellNames = new ArrayList<>(counts.keySet());
        String[] sellDisplay = sellNames.stream().map(name -> {
            Item item = ItemCatalog.get(name);
            int sellPrice = item.getPrice() / 2;
            return name + " x" + counts.get(name) + " (판매가 " + sellPrice + "G)";
        }).toArray(String[]::new);
        sellList.setListData(sellDisplay);
    }
}
