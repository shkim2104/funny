package game.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ItemCatalog {
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** Boss-only gear per dungeon, in World.buildDungeons() order. */
    private static final List<List<Item>> BOSS_DROPS = new ArrayList<>();

    // Balance: every piece has a step (1~7, the dungeon it is meant for). Step baseline ATK is
    // 8 / 16 / 26 / 38 / 52 / 68 / 86 and armor is 0.8x that. At the same step, shop gear = baseline
    // with no effect, treasure-wheel gear = 1.25x with one mild effect, boss gear = 1.55x with a strong one.
    static {
        // ---------- shop: steps 1~5 ----------
        register(new Item("낡은 검", Item.Type.WEAPON, 8, 50, Item.Grade.COMMON).icon("old_sword"));
        register(new Item("강철 검", Item.Type.WEAPON, 16, 160, Item.Grade.UNCOMMON).icon("steel_sword"));
        register(new Item("미스릴 검", Item.Type.WEAPON, 26, 380, Item.Grade.RARE).icon("mithril_sword"));
        register(new Item("용의 검", Item.Type.WEAPON, 38, 750, Item.Grade.EPIC).icon("dragon_sword"));
        register(new Item("전설의 성검", Item.Type.WEAPON, 52, 1400, Item.Grade.LEGENDARY).icon("holy_sword"));

        register(new Item("가죽 갑옷", Item.Type.ARMOR, 6, 35, Item.Grade.COMMON).icon("leather_armor"));
        register(new Item("철 갑옷", Item.Type.ARMOR, 13, 110, Item.Grade.UNCOMMON).icon("iron_armor"));
        register(new Item("용린 갑옷", Item.Type.ARMOR, 21, 260, Item.Grade.RARE).icon("scale_armor"));
        register(new Item("용의 갑옷", Item.Type.ARMOR, 30, 560, Item.Grade.EPIC).icon("dragon_armor"));
        register(new Item("전설의 수호 갑옷", Item.Type.ARMOR, 42, 1150, Item.Grade.LEGENDARY).icon("guardian_armor"));

        register(new Item("포션", Item.Type.POTION, 30, 15, Item.Grade.COMMON).icon("potion"));
        register(new Item("하이 포션", Item.Type.POTION, 80, 40, Item.Grade.COMMON).icon("high_potion"));
        register(new Item("에테르", Item.Type.ETHER, 25, 35, Item.Grade.COMMON).icon("ether"));

        // ---------- treasure wheel only: 10 weapons + 10 armor, 고급 = step 2 ... 전설 = step 5 ----------
        gacha(weapon("날렵한 세검", 20, Item.Grade.UNCOMMON, "swift_rapier", Bonus.of().critChance(4)));
        gacha(weapon("사냥꾼의 단궁", 19, Item.Grade.UNCOMMON, "hunter_bow", Bonus.of().firstStrike(0.20)));
        gacha(armor("여행자의 망토", 16, Item.Grade.UNCOMMON, "traveler_cloak", Bonus.of().dodge(0.04)));
        gacha(armor("징 박힌 가죽 조끼", 17, Item.Grade.UNCOMMON, "studded_vest", Bonus.of().thorns(0.06)));

        gacha(weapon("푸른 달의 검", 33, Item.Grade.RARE, "blue_moon_sword", Bonus.of().skillDmg(0.08)));
        gacha(weapon("피에 젖은 단도", 31, Item.Grade.RARE, "blood_dagger", Bonus.of().lifesteal(0.04)));
        gacha(weapon("폭풍 도끼", 34, Item.Grade.RARE, "storm_axe", Bonus.of().critDmg(0.15)));
        gacha(armor("수호자의 사슬 갑옷", 26, Item.Grade.RARE, "guardian_chain", Bonus.of().dmgReduction(0.05)));
        gacha(armor("마나 직물 로브", 24, Item.Grade.RARE, "mana_robe", Bonus.of().mpOnKill(3)));
        gacha(armor("바람결 망토", 25, Item.Grade.RARE, "breeze_cloak", Bonus.of().dodge(0.05).spd(1)));

        gacha(weapon("불꽃 정령의 검", 48, Item.Grade.EPIC, "flame_spirit_sword", Bonus.of().execute(0.12)));
        gacha(weapon("서리 이빨 창", 47, Item.Grade.EPIC, "frost_fang_spear", Bonus.of().firstStrike(0.35)));
        gacha(weapon("천둥 망치", 46, Item.Grade.EPIC, "thunder_hammer", Bonus.of().doubleStrike(0.12)));
        gacha(armor("용비늘 흉갑", 38, Item.Grade.EPIC, "dragon_scale_cuirass", Bonus.of().shield(0.07)));
        gacha(armor("그림자 가죽 갑옷", 36, Item.Grade.EPIC, "shadow_leather", Bonus.of().dodge(0.08)));
        gacha(armor("성기사의 판금", 39, Item.Grade.EPIC, "paladin_plate", Bonus.of().regen(0.015)));

        gacha(weapon("별빛 성검", 66, Item.Grade.LEGENDARY, "starlight_sword", Bonus.of().critChance(8).critDmg(0.20)));
        gacha(weapon("심연의 낫", 64, Item.Grade.LEGENDARY, "abyss_scythe", Bonus.of().lifesteal(0.07)));
        gacha(armor("황금 사자 갑옷", 52, Item.Grade.LEGENDARY, "golden_lion_armor", Bonus.of().dmgReduction(0.08)));
        gacha(armor("세계수 로브", 50, Item.Grade.LEGENDARY, "world_tree_robe", Bonus.of().regen(0.02).mpOnKill(4)));

        // ---------- boss gear: 12 weapons + 12 armor, step = dungeon, each with its own strong effect ----------
        boss(0, weapon("늑대왕의 송곳니", 12, "wolf_fang", Bonus.of().lifesteal(0.08)),
                armor("우두머리의 가죽 망토", 10, "alpha_cloak", Bonus.of().dodge(0.08)));
        boss(1, weapon("오크 대장의 전투도끼", 25, "orc_axe", Bonus.of().critDmg(0.30)),
                weapon("동굴 수정 지팡이", 23, "crystal_staff", Bonus.of().skillDmg(0.15).mpCostReduce(1)),
                armor("오크 족장의 철판 갑옷", 20, "orc_plate", Bonus.of().thorns(0.15)));
        boss(2, weapon("리치의 영혼 낫", 40, "lich_scythe", Bonus.of().lifesteal(0.12).ignoreDef(0.15)),
                armor("망자의 로브", 30, "dead_robe", Bonus.of().dmgReduction(0.10).mpOnKill(4)),
                armor("해골 기사의 뼈 갑옷", 32, "bone_armor", Bonus.of().shield(0.15)));
        boss(3, weapon("마왕의 흑염검", 59, "demon_blade", Bonus.of().execute(0.30).critChance(6)),
                weapon("지옥견 이빨 쌍검", 55, "hound_twins", Bonus.of().doubleStrike(0.25)),
                armor("마왕성 근위대 판금", 47, "demon_guard_plate", Bonus.of().defPct(0.20).thorns(0.10)),
                armor("타락한 마법사의 법의", 44, "fallen_vestment", Bonus.of().skillDmg(0.20).mpOnKill(8)));
        boss(4, weapon("서리 여왕의 빙결창", 81, "frost_spear", Bonus.of().firstStrike(0.60).spd(3)),
                weapon("빙하 결정 활", 75, "glacier_bow", Bonus.of().critChance(12)),
                armor("빙결 수호 갑옷", 64, "frozen_plate", Bonus.of().dmgReduction(0.15)),
                armor("눈보라 망토", 60, "blizzard_cloak", Bonus.of().dodge(0.15)));
        boss(5, weapon("화염 군주의 용암 대검", 105, "lava_greatsword", Bonus.of().bossDmg(0.30).lifesteal(0.06)),
                weapon("불꽃 악령의 단검", 98, "flame_dagger", Bonus.of().critChance(10).critDmg(0.40)),
                armor("용암 골렘의 심장 갑옷", 84, "magma_heart_plate", Bonus.of().regen(0.04)),
                armor("불사조 깃털 갑옷", 78, "phoenix_armor", Bonus.of().revive(0.30)));
        boss(6, weapon("타천사왕의 심판검", 133, "judgment_blade", Bonus.of().critChance(15).critDmg(0.50).ignoreDef(0.20)),
                weapon("천공의 뇌창", 124, "thunder_lance", Bonus.of().doubleStrike(0.30).firstStrike(0.40)),
                armor("타천사왕의 여섯 날개 갑옷", 107, "six_wing_armor", Bonus.of().dodge(0.15).dmgReduction(0.15)),
                armor("천공 기사단의 성갑", 100, "sky_order_plate", Bonus.of().shield(0.25).thorns(0.20)));
    }
    private ItemCatalog() {}

    private static void register(Item item) {
        ITEMS.put(item.getName(), item);
    }

    /** Boss gear (신화): priced at 30G per point, so selling one pays half that. */
    private static Item weapon(String name, int atk, String icon, Bonus bonus) {
        return new Item(name, Item.Type.WEAPON, atk, atk * 30, Item.Grade.MYTHIC).icon(icon).bonus(bonus);
    }

    private static Item armor(String name, int def, String icon, Bonus bonus) {
        return new Item(name, Item.Type.ARMOR, def, def * 30, Item.Grade.MYTHIC).icon(icon).bonus(bonus);
    }

    /** Treasure-wheel gear: 20G per point. */
    private static Item weapon(String name, int atk, Item.Grade grade, String icon, Bonus bonus) {
        return new Item(name, Item.Type.WEAPON, atk, atk * 20, grade).icon(icon).bonus(bonus).gacha();
    }

    private static Item armor(String name, int def, Item.Grade grade, String icon, Bonus bonus) {
        return new Item(name, Item.Type.ARMOR, def, def * 20, grade).icon(icon).bonus(bonus).gacha();
    }

    private static void gacha(Item item) {
        register(item);
    }

    private static void boss(int dungeonIndex, Item... items) {
        while (BOSS_DROPS.size() <= dungeonIndex) BOSS_DROPS.add(new ArrayList<>());
        for (Item item : items) {
            register(item);
            BOSS_DROPS.get(dungeonIndex).add(item);
        }
    }

    public static Item get(String name) {
        return ITEMS.get(name);
    }

    public static Collection<Item> all() {
        return ITEMS.values();
    }

    /** What the shop sells: everything except boss-only and treasure-only gear. */
    public static List<Item> shopItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : ITEMS.values()) if (item.getGrade() != Item.Grade.MYTHIC && !item.isGachaOnly()) result.add(item);
        return result;
    }

    /** Shop weapons and armor of the given grade (what a boss's treasure chest can hold). */
    public static List<Item> equipableOfGrade(Item.Grade grade) {
        List<Item> result = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if (isGear(item) && item.getGrade() == grade && !item.isGachaOnly()) result.add(item);
        }
        return result;
    }

    /** What the treasure wheel can give at a grade: the shop gear plus the treasure-only pieces. */
    public static List<Item> treasurePool(Item.Grade grade) {
        List<Item> result = new ArrayList<>();
        for (Item item : ITEMS.values()) if (isGear(item) && item.getGrade() == grade) result.add(item);
        return result;
    }

    private static boolean isGear(Item item) {
        return item.getType() == Item.Type.WEAPON || item.getType() == Item.Type.ARMOR;
    }

    public static List<Item> bossDrops(int dungeonIndex) {
        return dungeonIndex >= 0 && dungeonIndex < BOSS_DROPS.size() ? BOSS_DROPS.get(dungeonIndex) : new ArrayList<>();
    }

    /** One of this boss's items, preferring ones the player doesn't own yet; null if the boss has none. */
    public static Item rollBossDrop(int dungeonIndex, Player player, Random rnd) {
        List<Item> pool = bossDrops(dungeonIndex);
        if (pool.isEmpty()) return null;
        List<Item> missing = new ArrayList<>();
        for (Item item : pool) {
            boolean owned = player.getInventory().contains(item.getName()) || item.getName().equals(player.getWeaponName())
                    || item.getName().equals(player.getArmorName());
            if (!owned) missing.add(item);
        }
        List<Item> from = missing.isEmpty() ? pool : missing;
        return from.get(rnd.nextInt(from.size()));
    }
}
