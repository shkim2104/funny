package game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A run augment (롤토체스 증강 style): picked from three cards every few floors, active only until the
 * player leaves the dungeon. Its effect is a {@link Bonus}; the icon key names its pencil icon
 * (images/augments/{icon}.png).
 */
public final class Augment {
    public enum Tier {
        SILVER("실버", "#b8c0cc"),
        GOLD("골드", "#e0b040"),
        PRISM("프리즘", "#c78cf0");

        private final String label;
        private final String colorHex;

        Tier(String label, String colorHex) {
            this.label = label;
            this.colorHex = colorHex;
        }

        public String getLabel() { return label; }
        public String getColorHex() { return colorHex; }
    }

    private final String name;
    private final Tier tier;
    private final String icon;
    private final Bonus bonus;

    private Augment(String name, Tier tier, String icon, Bonus bonus) {
        this.name = name;
        this.tier = tier;
        this.icon = icon;
        this.bonus = bonus;
    }

    public String getName() { return name; }
    public Tier getTier() { return tier; }
    public String getIcon() { return icon; }
    public Bonus getBonus() { return bonus; }

    /** The effect in one line, e.g. "공격력 +20%, 받는 피해 +10%". */
    public String getDescription() {
        return String.join(", ", bonus.lines());
    }

    // ---------- catalog ----------
    private static final List<Augment> ALL = new ArrayList<>();

    private static void add(Tier tier, String icon, String name, Bonus b) {
        ALL.add(new Augment(name, tier, icon, b));
    }

    static {
        Tier s = Tier.SILVER, g = Tier.GOLD, p = Tier.PRISM;
        add(s, "sword", "날카로운 칼날", Bonus.of().atkPct(0.10));
        add(s, "shield", "단단한 피부", Bonus.of().dmgReduction(0.08));
        add(s, "coin", "행운의 동전", Bonus.of().goldPct(0.30));
        add(s, "clover", "배움의 기쁨", Bonus.of().expPct(0.25));
        add(s, "heart", "응급 처치", Bonus.of().healAfterFight(0.10));
        add(s, "wing", "날렵한 발놀림", Bonus.of().dodge(0.08));
        add(s, "star", "매의 눈", Bonus.of().critChance(8));
        add(s, "spikes", "가시 갑옷", Bonus.of().thorns(0.15));
        add(s, "drop", "피의 맛", Bonus.of().lifesteal(0.05));
        add(s, "orb", "마나 샘", Bonus.of().mpOnKill(5));
        add(s, "sword", "기선 제압", Bonus.of().firstStrike(0.40));
        add(s, "shield", "작은 보호막", Bonus.of().shield(0.08));
        add(s, "heart", "재생", Bonus.of().regen(0.02));
        add(s, "orb", "마법 증폭", Bonus.of().skillDmg(0.12));
        add(s, "wing", "날쌘 몸", Bonus.of().spd(3));
        add(s, "sword", "갑옷 관통", Bonus.of().ignoreDef(0.15));
        add(s, "sword", "처형인", Bonus.of().execute(0.25));
        add(s, "heart", "전리품 사냥꾼", Bonus.of().hpOnKill(0.03));
        add(s, "shield", "강인함", Bonus.of().defPct(0.15));
        add(s, "orb", "절약 정신", Bonus.of().mpCostReduce(2));

        add(g, "sword", "광전사", Bonus.of().atkPct(0.20).dmgReduction(-0.10));
        add(g, "drop", "흡혈귀", Bonus.of().lifesteal(0.12));
        add(g, "star", "치명적인 일격", Bonus.of().critDmg(0.50));
        add(g, "sword", "연속 공격", Bonus.of().doubleStrike(0.25));
        add(g, "shield", "철벽", Bonus.of().dmgReduction(0.15));
        add(g, "sword", "거인 사냥꾼", Bonus.of().bossDmg(0.30));
        add(g, "star", "위기의 힘", Bonus.of().lowHpDmg(0.40));
        add(g, "sword", "성장하는 검", Bonus.of().stackAtkPerKill(0.03));
        add(g, "coin", "황금 손", Bonus.of().goldPct(0.60).expPct(0.20));
        add(g, "spikes", "고통의 가시", Bonus.of().thorns(0.30));
        add(g, "wing", "신속", Bonus.of().alwaysFirst());
        add(g, "orb", "마력 폭주", Bonus.of().skillDmg(0.25));
        add(g, "heart", "생명의 샘", Bonus.of().regen(0.04));
        add(g, "wing", "그림자 걸음", Bonus.of().dodge(0.15));
        add(g, "orb", "마나 흡수", Bonus.of().mpOnKill(12).mpCostReduce(1));
        add(g, "shield", "수호의 방벽", Bonus.of().shield(0.18));
        add(g, "star", "저격수", Bonus.of().critChance(15));
        add(g, "sword", "약점 포착", Bonus.of().ignoreDef(0.35));
        add(g, "heart", "전투 의학", Bonus.of().healAfterFight(0.20).hpOnKill(0.03));
        add(g, "sword", "독수리의 일격", Bonus.of().firstStrike(0.80).critChance(5));

        add(p, "heart", "불사의 의지", Bonus.of().revive(0.40));
        add(p, "drop", "피의 군주", Bonus.of().lifesteal(0.20).healAfterFight(0.10));
        add(p, "sword", "학살자", Bonus.of().stackAtkPerKill(0.06));
        add(p, "star", "천벌", Bonus.of().critChance(20).critDmg(0.60));
        add(p, "sword", "쌍검술의 극의", Bonus.of().doubleStrike(0.50));
        add(p, "shield", "무적의 요새", Bonus.of().dmgReduction(0.25).thorns(0.20));
        add(p, "orb", "대마법사", Bonus.of().skillDmg(0.45).mpCostReduce(3));
        add(p, "coin", "황금비", Bonus.of().goldPct(1.50));
        add(p, "wing", "시간 가속", Bonus.of().alwaysFirst().dodge(0.12).spd(5));
        add(p, "shield", "거인의 숨결", Bonus.of().shield(0.30).regen(0.03));
    }

    public static List<Augment> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static List<Augment> ofTier(Tier tier) {
        List<Augment> out = new ArrayList<>();
        for (Augment a : ALL) if (a.tier == tier) out.add(a);
        return out;
    }
}
