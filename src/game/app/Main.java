package game.app;

import game.console.Battle;
import game.console.Shop;
import game.model.Dungeon;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Monster;
import game.model.Player;
import game.model.World;
import game.save.SaveManager;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Random rnd = new Random();
        List<Dungeon> dungeons = World.buildDungeons();
        Battle battle = new Battle(sc);
        Shop shop = new Shop(sc);

        System.out.println("========================================");
        System.out.println("        공책 RPG 에 오신 것을 환영합니다");
        System.out.println("========================================");

        Player player = loadOrCreatePlayer(sc);

        boolean playing = true;
        while (playing) {
            System.out.println("\n============ 마을 ============");
            System.out.println("Lv." + player.getLevel() + " " + player.getName()
                    + "   HP " + player.getHp() + "/" + player.getMaxHp()
                    + "   골드 " + player.getGold() + "G"
                    + (player.getStatPoints() > 0 ? "   [스탯 포인트 " + player.getStatPoints() + "]" : ""));
            System.out.println("1) 상태 보기");
            System.out.println("2) 상점");
            System.out.println("3) 장비 변경");
            System.out.println("4) 스탯 분배");
            System.out.println("5) 던전 입장");
            System.out.println("6) 저장하기");
            System.out.println("7) 종료");
            System.out.print("> ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1": showStatus(player); break;
                case "2": shop.open(player); break;
                case "3": equipMenu(player, sc); break;
                case "4": allocateStats(player, sc); break;
                case "5": enterDungeonMenu(player, dungeons, battle, rnd, sc); break;
                case "6": SaveManager.save(player); break;
                case "7":
                    System.out.println("게임을 종료합니다. 다음에 또 만나요!");
                    playing = false;
                    break;
                default:
                    System.out.println("올바른 번호를 입력하세요.");
            }
        }
        sc.close();
    }

    private static Player loadOrCreatePlayer(Scanner sc) {
        if (SaveManager.hasSave()) {
            System.out.print("이어하기 데이터가 있습니다. 불러올까요? (y/n) > ");
            String ans = sc.nextLine().trim();
            if (ans.equalsIgnoreCase("y")) {
                Player loaded = SaveManager.load();
                if (loaded != null) {
                    System.out.println(loaded.getName() + "님, 다시 오신 것을 환영합니다!");
                    return loaded;
                }
            }
        }
        return createNewPlayer(sc);
    }

    private static Player createNewPlayer(Scanner sc) {
        System.out.print("주인공의 이름을 입력하세요: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "용사";
        Player player = new Player(name);
        player.addItem("포션");
        player.addItem("포션");
        System.out.println("\n" + name + "의 모험이 시작됩니다!");
        return player;
    }

    private static void showStatus(Player player) {
        System.out.println("\n=== 상태 ===");
        System.out.println("이름: " + player.getName());
        System.out.println("레벨: " + player.getLevel() + "  (EXP " + player.getExp() + "/" + player.getExpToNext() + ")");
        System.out.println("HP: " + player.getHp() + "/" + player.getMaxHp());
        System.out.println("MP: " + player.getMp() + "/" + player.getMaxMp());
        System.out.println("공격력: " + player.getAtk() + " (기본 " + player.getBaseAtk() + ")");
        System.out.println("방어력: " + player.getDef() + " (기본 " + player.getBaseDef() + ")");
        System.out.println("행운: " + player.getLuck() + " (치명타 확률 " + player.getCritChance() + "%)");
        System.out.println("스탯 포인트: " + player.getStatPoints());
        System.out.println("골드: " + player.getGold() + "G");
        System.out.println("무기: " + (player.getWeaponName() == null ? "없음" : player.getWeaponName()));
        System.out.println("방어구: " + (player.getArmorName() == null ? "없음" : player.getArmorName()));

        Map<String, Integer> counts = player.getItemCounts();
        System.out.println("소지품:");
        if (counts.isEmpty()) {
            System.out.println("  (없음)");
        } else {
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                System.out.println("  - " + e.getKey() + " x" + e.getValue());
            }
        }
    }

    private static void allocateStats(Player player, Scanner sc) {
        if (player.getStatPoints() <= 0) {
            System.out.println("분배할 스탯 포인트가 없습니다.");
            return;
        }
        while (player.getStatPoints() > 0) {
            System.out.println("\n=== 스탯 분배 (남은 포인트: " + player.getStatPoints() + ") ===");
            System.out.println("1) 최대 HP +" + Player.HP_PER_POINT + " (현재 " + player.getMaxHp() + ")");
            System.out.println("2) 최대 MP +" + Player.MP_PER_POINT + " (현재 " + player.getMaxMp() + ")");
            System.out.println("3) 공격력 +" + Player.ATK_PER_POINT + " (현재 " + player.getBaseAtk() + ")");
            System.out.println("4) 방어력 +" + Player.DEF_PER_POINT + " (현재 " + player.getBaseDef() + ")");
            System.out.println("5) 행운 +" + Player.LUCK_PER_POINT + " (현재 " + player.getLuck() + ")");
            System.out.println("0) 나중에 하기");
            System.out.print("> ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1": player.spendStatPoint(Player.Stat.HP); break;
                case "2": player.spendStatPoint(Player.Stat.MP); break;
                case "3": player.spendStatPoint(Player.Stat.ATK); break;
                case "4": player.spendStatPoint(Player.Stat.DEF); break;
                case "5": player.spendStatPoint(Player.Stat.LUCK); break;
                case "0": return;
                default: System.out.println("올바른 번호를 입력하세요.");
            }
        }
        System.out.println("모든 스탯 포인트를 분배했습니다.");
    }

    private static void equipMenu(Player player, Scanner sc) {
        while (true) {
            System.out.println("\n=== 장비 변경 ===");
            System.out.println("현재 무기: " + (player.getWeaponName() == null ? "없음" : player.getWeaponName()));
            System.out.println("현재 방어구: " + (player.getArmorName() == null ? "없음" : player.getArmorName()));
            System.out.println("1) 무기 장착  2) 방어구 장착  3) 나가기");
            System.out.print("> ");
            String input = sc.nextLine().trim();

            switch (input) {
                case "1": equipItem(player, sc, Item.Type.WEAPON); break;
                case "2": equipItem(player, sc, Item.Type.ARMOR); break;
                case "3": return;
                default: System.out.println("올바른 번호를 입력하세요.");
            }
        }
    }

    private static void equipItem(Player player, Scanner sc, Item.Type type) {
        List<String> owned = player.getItemsByType(type);
        if (owned.isEmpty()) {
            System.out.println("보유한 아이템이 없습니다.");
            return;
        }

        System.out.println("\n0) 장착 해제");
        for (int i = 0; i < owned.size(); i++) {
            System.out.println((i + 1) + ") " + ItemCatalog.get(owned.get(i)).describe());
        }
        System.out.print("> ");
        String input = sc.nextLine().trim();

        try {
            int idx = Integer.parseInt(input);
            if (idx == 0) {
                if (type == Item.Type.WEAPON) player.setWeaponName(null);
                else player.setArmorName(null);
                System.out.println("장착을 해제했습니다.");
                return;
            }
            if (idx < 1 || idx > owned.size()) {
                System.out.println("잘못된 입력입니다.");
                return;
            }
            String name = owned.get(idx - 1);
            if (type == Item.Type.WEAPON) player.setWeaponName(name);
            else player.setArmorName(name);
            System.out.println(name + "을(를) 장착했습니다.");
        } catch (NumberFormatException e) {
            System.out.println("취소했습니다.");
        }
    }

    private static void enterDungeonMenu(Player player, List<Dungeon> dungeons, Battle battle, Random rnd, Scanner sc) {
        System.out.println("\n=== 던전 선택 ===");
        for (int i = 0; i <= player.getUnlockedDungeon() && i < dungeons.size(); i++) {
            System.out.println((i + 1) + ") " + dungeons.get(i).getName());
        }
        System.out.println("0) 취소");
        System.out.print("> ");
        String input = sc.nextLine().trim();

        try {
            int idx = Integer.parseInt(input);
            if (idx == 0) return;
            if (idx < 1 || idx > player.getUnlockedDungeon() + 1 || idx > dungeons.size()) {
                System.out.println("잘못된 입력입니다.");
                return;
            }
            runDungeon(player, dungeons, idx - 1, battle, rnd, sc);
        } catch (NumberFormatException e) {
            System.out.println("잘못된 입력입니다.");
        }
    }

    private static void runDungeon(Player player, List<Dungeon> dungeons, int dungeonIndex, Battle battle, Random rnd, Scanner sc) {
        Dungeon dungeon = dungeons.get(dungeonIndex);
        System.out.println("\n>>> " + dungeon.getName() + "에 입장합니다! <<<");

        for (int floor = 1; floor <= dungeon.getFloors(); floor++) {
            System.out.println("\n-- " + floor + " / " + dungeon.getFloors() + " 층 --");
            Monster monster = dungeon.randomMonster(rnd);
            Battle.Result result = battle.fight(player, monster);

            if (result == Battle.Result.LOSE) {
                handleDefeat(player);
                return;
            } else if (result == Battle.Result.FLEE) {
                System.out.println("던전에서 물러납니다.");
                return;
            }

            player.gainExp(monster.getExpReward());
            player.earnGold(monster.getGoldReward());
            System.out.println("EXP " + monster.getExpReward() + ", 골드 " + monster.getGoldReward() + "G 획득!");
        }

        System.out.println("\n>>> 보스 등장! <<<");
        Monster boss = dungeon.spawnBoss();
        Battle.Result bossResult = battle.fight(player, boss);

        if (bossResult == Battle.Result.LOSE) {
            handleDefeat(player);
            return;
        } else if (bossResult == Battle.Result.FLEE) {
            System.out.println("보스에게서 도망쳤습니다. 던전 클리어에 실패했습니다.");
            return;
        }

        player.gainExp(boss.getExpReward());
        player.earnGold(boss.getGoldReward());
        System.out.println("보스전 승리! EXP " + boss.getExpReward() + ", 골드 " + boss.getGoldReward() + "G 획득!");

        if (dungeon.getRewardGold() > 0 || dungeon.getRewardExp() > 0) {
            player.gainExp(dungeon.getRewardExp());
            player.earnGold(dungeon.getRewardGold());
            System.out.println(dungeon.getName() + " 클리어 보상! EXP " + dungeon.getRewardExp() + ", 골드 " + dungeon.getRewardGold() + "G 추가 획득!");
        }

        if (dungeonIndex == player.getUnlockedDungeon() && dungeonIndex + 1 < dungeons.size()) {
            player.setUnlockedDungeon(dungeonIndex + 1);
            System.out.println("새로운 던전이 해금되었습니다: " + dungeons.get(dungeonIndex + 1).getName());
        } else if (dungeonIndex == dungeons.size() - 1) {
            System.out.println("\n*** 축하합니다! 모든 던전을 클리어했습니다! ***");
        }
    }

    private static void handleDefeat(Player player) {
        System.out.println("\n정신을 잃고 마을로 실려왔습니다...");
        int penalty = player.getGold() / 4;
        player.spendGold(penalty);
        player.reviveAtTown();
        System.out.println("골드 " + penalty + "G를 잃었습니다.");
    }
}
