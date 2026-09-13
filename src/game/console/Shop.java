package game.console;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Shop {
    private final Scanner sc;

    public Shop(Scanner sc) {
        this.sc = sc;
    }

    public void open(Player player) {
        while (true) {
            System.out.println("\n=== 상점 === (소지금: " + player.getGold() + "G)");
            System.out.println("1) 구매하기  2) 판매하기  3) 나가기");
            System.out.print("> ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1": buy(player); break;
                case "2": sell(player); break;
                case "3": return;
                default: System.out.println("올바른 번호를 입력하세요.");
            }
        }
    }

    private void buy(Player player) {
        List<Item> items = new ArrayList<>(ItemCatalog.all());
        System.out.println("\n-- 구매 목록 --");
        for (int i = 0; i < items.size(); i++) {
            System.out.println((i + 1) + ") " + items.get(i).describe());
        }
        System.out.println("0) 취소");
        System.out.print("> ");
        String input = sc.nextLine().trim();

        try {
            int idx = Integer.parseInt(input);
            if (idx == 0) return;
            if (idx < 1 || idx > items.size()) {
                System.out.println("잘못된 입력입니다.");
                return;
            }
            Item item = items.get(idx - 1);
            if (player.getGold() < item.getPrice()) {
                System.out.println("골드가 부족합니다.");
                return;
            }
            player.spendGold(item.getPrice());
            player.addItem(item.getName());
            System.out.println(item.getName() + "을(를) 구매했습니다.");
        } catch (NumberFormatException e) {
            System.out.println("잘못된 입력입니다.");
        }
    }

    private void sell(Player player) {
        Map<String, Integer> counts = player.getItemCounts();
        if (counts.isEmpty()) {
            System.out.println("판매할 아이템이 없습니다.");
            return;
        }

        List<String> names = new ArrayList<>(counts.keySet());
        System.out.println("\n-- 판매 목록 --");
        for (int i = 0; i < names.size(); i++) {
            Item item = ItemCatalog.get(names.get(i));
            int sellPrice = item.getPrice() / 2;
            System.out.println((i + 1) + ") " + item.getName() + " x" + counts.get(names.get(i)) + " (판매가 " + sellPrice + "G)");
        }
        System.out.println("0) 취소");
        System.out.print("> ");
        String input = sc.nextLine().trim();

        try {
            int idx = Integer.parseInt(input);
            if (idx == 0) return;
            if (idx < 1 || idx > names.size()) {
                System.out.println("잘못된 입력입니다.");
                return;
            }
            String name = names.get(idx - 1);
            Item item = ItemCatalog.get(name);
            player.removeItem(name);
            player.earnGold(item.getPrice() / 2);
            System.out.println(name + "을(를) 판매했습니다.");
        } catch (NumberFormatException e) {
            System.out.println("잘못된 입력입니다.");
        }
    }
}
