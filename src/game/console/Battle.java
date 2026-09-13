package game.console;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Monster;
import game.model.Player;

import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Battle {
    public enum Result { WIN, LOSE, FLEE }

    private final Scanner sc;
    private final Random rnd = new Random();

    public Battle(Scanner sc) {
        this.sc = sc;
    }

    public Result fight(Player player, Monster monster) {
        System.out.println("\n=== " + monster.getName() + " 이(가) 나타났다! ===");

        while (player.isAlive() && monster.isAlive()) {
            printStatus(player, monster);
            System.out.println("1) 공격  2) 아이템 사용  3) 도망");
            System.out.print("> ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1":
                    playerAttack(player, monster);
                    break;
                case "2":
                    useItem(player);
                    break;
                case "3":
                    if (rnd.nextInt(100) < 50) {
                        System.out.println("성공적으로 도망쳤다!");
                        return Result.FLEE;
                    } else {
                        System.out.println("도망에 실패했다!");
                    }
                    break;
                default:
                    System.out.println("올바른 번호를 입력하세요.");
                    continue;
            }

            if (!monster.isAlive()) break;
            monsterAttack(player, monster);
        }

        if (!player.isAlive()) {
            System.out.println("\n" + player.getName() + "은(는) 쓰러졌다...");
            return Result.LOSE;
        } else {
            System.out.println("\n" + monster.getName() + "을(를) 물리쳤다!");
            return Result.WIN;
        }
    }

    private void printStatus(Player player, Monster monster) {
        System.out.println("--------------------------------------------------");
        System.out.printf("%s  HP %d/%d  MP %d/%d%n", player.getName(), player.getHp(), player.getMaxHp(), player.getMp(), player.getMaxMp());
        System.out.printf("%s  HP %d/%d%n", monster.getName(), monster.getHp(), monster.getMaxHp());
    }

    /** Defense mitigates damage on a diminishing-returns curve: dmg = ATK * 50 / (DEF + 50). */
    private int computeDamage(int atk, int def) {
        double mitigated = atk * 50.0 / (def + 50.0);
        int dmg = Math.round((float) mitigated) + rnd.nextInt(5) - 2;
        return Math.max(1, dmg);
    }

    private void playerAttack(Player player, Monster monster) {
        int dmg = computeDamage(player.getAtk(), monster.getDef());
        boolean crit = rnd.nextInt(100) < player.getCritChance();
        if (crit) dmg = Math.round(dmg * 1.75f);
        monster.takeDamage(dmg);
        System.out.println(player.getName() + "의 공격! " + (crit ? "치명타! " : "")
                + monster.getName() + "에게 " + dmg + "의 피해!");
    }

    private void monsterAttack(Player player, Monster monster) {
        int dmg = computeDamage(monster.getAtk(), player.getDef());
        player.takeDamage(dmg);
        System.out.println(monster.getName() + "의 공격! " + player.getName() + "이(가) " + dmg + "의 피해를 입었다!");
    }

    private void useItem(Player player) {
        List<String> usable = player.getUsableItems();
        if (usable.isEmpty()) {
            System.out.println("사용할 수 있는 아이템이 없다.");
            return;
        }

        System.out.println("사용할 아이템을 선택하세요:");
        for (int i = 0; i < usable.size(); i++) {
            Item item = ItemCatalog.get(usable.get(i));
            System.out.println((i + 1) + ") " + item.describe());
        }
        System.out.println("0) 취소");
        System.out.print("> ");
        String input = sc.nextLine().trim();

        try {
            int idx = Integer.parseInt(input);
            if (idx == 0) return;
            if (idx < 1 || idx > usable.size()) {
                System.out.println("잘못된 입력입니다.");
                return;
            }
            String itemName = usable.get(idx - 1);
            Item item = ItemCatalog.get(itemName);
            if (item.getType() == Item.Type.POTION) {
                player.heal(item.getValue());
                System.out.println(player.getName() + "은(는) " + item.getName() + "을(를) 사용해 HP를 " + item.getValue() + " 회복했다!");
            } else if (item.getType() == Item.Type.ETHER) {
                player.restoreMp(item.getValue());
                System.out.println(player.getName() + "은(는) " + item.getName() + "을(를) 사용해 MP를 " + item.getValue() + " 회복했다!");
            }
            player.removeItem(itemName);
        } catch (NumberFormatException e) {
            System.out.println("잘못된 입력입니다.");
        }
    }
}
