package game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 대장간 제작: the player draws a weapon or armor and forges it with 색연필 심 and gold. The grade sets the
 * cost, the range the stat is rolled in and how many special effects it gets; which effects and how
 * strong are rolled too. Index = Item.Grade ordinal (일반 .. 신화).
 */
public final class Crafting {
    public static final int[] COLOR_COST = {8, 20, 45, 90, 180, 400};
    public static final int[] GOLD_COST = {500, 1500, 4000, 10000, 25000, 60000};
    public static final int[] EFFECT_COUNT = {0, 1, 1, 2, 2, 3};
    /** The middle of each grade's stat roll: shop and treasure gear of that grade, and 6던전 boss gear for 신화. */
    private static final int[] WEAPON_MID = {8, 16, 26, 38, 52, 105};
    private static final int[] ARMOR_MID = {6, 13, 20, 30, 42, 84};
    private static final double LOW = 0.85, HIGH = 1.15;
    /** How strong each grade's effects are, times the effect's base step below. */
    private static final double[] EFFECT_SCALE = {0, 1, 1.5, 2.2, 3, 4.2};
    /** Each effect is rolled between this share of its grade's strength and the full strength. */
    private static final double EFFECT_LOW = 0.6;
    public static final int MAX_NAME = 12;

    /** The effects a forged piece can roll, with the step one 고급 grade gives. */
    public enum Effect {
        CRIT_CHANCE("critChance", true, 2),
        CRIT_DMG("critDmg", true, 0.06),
        SKILL_DMG("skillDmg", true, 0.04),
        FIRST_STRIKE("firstStrike", true, 0.10),
        EXECUTE("execute", true, 0.06),
        BOSS_DMG("bossDmg", true, 0.05),
        LIFESTEAL("lifesteal", true, 0.008),
        DOUBLE_STRIKE("doubleStrike", true, 0.05),
        IGNORE_DEF("ignoreDef", true, 0.04),
        ATK_PCT("atkPct", true, 0.03),
        DODGE("dodge", false, 0.02),
        THORNS("thorns", false, 0.03),
        DMG_REDUCTION("dmgReduction", false, 0.025),
        SHIELD("shield", false, 0.03),
        REGEN("regen", false, 0.006),
        DEF_PCT("defPct", false, 0.04),
        HP_ON_KILL("hpOnKill", false, 0.02),
        MP_ON_KILL("mpOnKill", false, 1),
        SPD("spd", false, 1);

        final String key;
        final boolean weapon;
        final double step;

        Effect(String key, boolean weapon, double step) {
            this.key = key;
            this.weapon = weapon;
            this.step = step;
        }

        /** Whole-number effects (MP per kill, speed) round to at least 1. */
        boolean whole() {
            return this == MP_ON_KILL || this == SPD;
        }

        static Effect byKey(String key) {
            for (Effect e : values()) if (e.key.equals(key)) return e;
            return null;
        }

        Bonus apply(Bonus b, double v) {
            switch (this) {
                case CRIT_CHANCE: return b.critChance(v);
                case CRIT_DMG: return b.critDmg(v);
                case SKILL_DMG: return b.skillDmg(v);
                case FIRST_STRIKE: return b.firstStrike(v);
                case EXECUTE: return b.execute(v);
                case BOSS_DMG: return b.bossDmg(v);
                case LIFESTEAL: return b.lifesteal(v);
                case DOUBLE_STRIKE: return b.doubleStrike(v);
                case IGNORE_DEF: return b.ignoreDef(v);
                case ATK_PCT: return b.atkPct(v);
                case DODGE: return b.dodge(v);
                case THORNS: return b.thorns(v);
                case DMG_REDUCTION: return b.dmgReduction(v);
                case SHIELD: return b.shield(v);
                case REGEN: return b.regen(v);
                case DEF_PCT: return b.defPct(v);
                case HP_ON_KILL: return b.hpOnKill(v);
                case MP_ON_KILL: return b.mpOnKill((int) Math.round(v));
                default: return b.spd((int) Math.round(v));
            }
        }
    }

    private Crafting() {}

    public static int minValue(Item.Type type, Item.Grade grade) {
        return (int) Math.round(mid(type, grade) * LOW);
    }

    public static int maxValue(Item.Type type, Item.Grade grade) {
        return (int) Math.round(mid(type, grade) * HIGH);
    }

    private static int mid(Item.Type type, Item.Grade grade) {
        return (type == Item.Type.WEAPON ? WEAPON_MID : ARMOR_MID)[grade.ordinal()];
    }

    public static boolean canAfford(Player p, Item.Grade grade) {
        return p.getMaterial(Material.COLOR_LEAD) >= COLOR_COST[grade.ordinal()] && p.getGold() >= GOLD_COST[grade.ordinal()];
    }

    /** Why a name can't be used, or null if it can. */
    public static String nameProblem(String name) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) return "장비 이름을 적어 주세요.";
        if (n.length() > MAX_NAME) return "이름은 " + MAX_NAME + "글자까지예요.";
        if (n.matches(".*[|;,=\\t\\n\\r].*")) return "이름에 | ; , = 기호는 쓸 수 없어요.";
        if (ItemCatalog.get(n) != null) return "이미 있는 장비 이름이에요. 다른 이름을 붙여 주세요.";
        return null;
    }

    /**
     * Pays and forges: the stat and effects are rolled, the item is registered and a +0 piece goes into
     * the bag. Returns null, with nothing spent, if the name is taken or the player can't afford it.
     */
    public static CraftedGear craft(Player p, String name, Item.Type type, Item.Grade grade, Random rnd) {
        if (nameProblem(name) != null || !canAfford(p, grade)) return null;
        p.spendMaterial(Material.COLOR_LEAD, COLOR_COST[grade.ordinal()]);
        p.spendGold(GOLD_COST[grade.ordinal()]);
        int lo = minValue(type, grade), hi = maxValue(type, grade);
        int value = lo + rnd.nextInt(hi - lo + 1);
        List<Effect> pool = new ArrayList<>();
        for (Effect e : Effect.values()) if (e.weapon == (type == Item.Type.WEAPON)) pool.add(e);
        Collections.shuffle(pool, rnd);
        Map<Effect, Double> effects = new LinkedHashMap<>();
        double strength = EFFECT_SCALE[grade.ordinal()];
        for (int i = 0; i < EFFECT_COUNT[grade.ordinal()] && i < pool.size(); i++) {
            Effect e = pool.get(i);
            double v = e.step * strength * (EFFECT_LOW + (1 - EFFECT_LOW) * rnd.nextDouble());
            effects.put(e, e.whole() ? Math.max(1, Math.round(v)) : round(v, e == Effect.CRIT_CHANCE ? 1 : 3));
        }
        CraftedGear made = new CraftedGear(name.trim(), type, grade, value, effects);
        p.addCrafted(made);
        p.addItem(made.getName());
        return made;
    }

    private static double round(double v, int places) {
        double k = Math.pow(10, places);
        return Math.round(v * k) / k;
    }
}
