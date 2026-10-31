package game.model;

import java.util.Random;

/**
 * What a floor without an augment pays out: one random find, already handed to the player when
 * give() returns. The game and the balance tools share this, so they can't drift apart.
 */
public final class FloorReward {
    private enum Kind { GOLD, GRAPHITE, POTION, GEAR, HARD_LEAD, ERASER }

    /** Chance (%) of each kind, in Kind's order. */
    private static final int[] WEIGHTS = {35, 25, 20, 12, 6, 2};
    private static final int GOLD_FLOORS = 5, GRAPHITE_MUL = 1, HIGH_POTION_LEVEL = 20;

    private final String text;
    private final String gearName;

    private FloorReward(String text, String gearName) {
        this.text = text;
        this.gearName = gearName;
    }

    /** One line for the battle log, e.g. "골드 42G". */
    public String getText() { return text; }

    /** The weapon or armor found, for 자동 장착; null for anything else. */
    public String getGearName() { return gearName; }

    /** Rolls one find for dungeon index d and gives it to the player. */
    public static FloorReward give(int d, Dungeon dungeon, Player player, Random rnd) {
        switch (roll(rnd)) {
            case GOLD: {
                int gold = (int) Math.round(GOLD_FLOORS * dungeon.getMonsterPool().stream().mapToInt(Monster::getGoldReward).average().orElse(0));
                player.earnGold(gold);
                return new FloorReward("골드 " + gold + "G", null);
            }
            case POTION: {
                String potion = potionFor(player, rnd);
                player.addItem(potion);
                return new FloorReward(potion, null);
            }
            case GEAR: {
                Item item = ItemCatalog.rollChest(d, rnd);
                player.addItem(item.getName());
                return new FloorReward(item.describe(), item.getName());
            }
            case HARD_LEAD:
                // 단단한 심 starts at dungeon 2; dungeon 1 pays graphite instead.
                if (d > 0) return material(player, Material.HARD_LEAD, 1);
                return material(player, Material.GRAPHITE, GRAPHITE_MUL);
            case ERASER:
                return material(player, Material.ERASER_DUST, 1);
            default:
                return material(player, Material.GRAPHITE, (1 + d / 2) * GRAPHITE_MUL);
        }
    }

    private static Kind roll(Random rnd) {
        int r = rnd.nextInt(100), acc = 0;
        for (Kind k : Kind.values()) {
            acc += WEIGHTS[k.ordinal()];
            if (r < acc) return k;
        }
        return Kind.GOLD;
    }

    private static String potionFor(Player player, Random rnd) {
        if ("마법사".equals(player.getJob().getJobClass()) && rnd.nextBoolean()) return "에테르";
        return player.getLevel() < HIGH_POTION_LEVEL ? "포션" : "하이 포션";
    }

    private static FloorReward material(Player player, Material m, int count) {
        player.addMaterial(m, count);
        return new FloorReward(m.getLabel() + " " + count + "개", null);
    }
}
