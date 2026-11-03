package game.model;

/**
 * Something a monster can do on its turn instead of a plain attack. Most fire by chance; ENRAGE and
 * RECOVER fire once, the first turn the monster's HP is at or below their threshold. The cosmic kinds
 * (SHOWER to HORIZON) belong to the final boss and are picked by its CosmicBrain, never by chance.
 */
public final class MonsterSkill {
    public enum Kind {
        /** One stronger hit. */
        HEAVY,
        /** Two hits, each at the power given. */
        MULTI,
        /** A hit that heals the monster by a share of the damage. */
        DRAIN,
        /** A hit that also takes a share of max HP each round, for a few rounds. */
        POISON,
        /** A hit that also lowers the player's defense for a few rounds. */
        ARMOR_BREAK,
        /** A hit that also makes the player lose their next turn. */
        STUN,
        /** No attack: halves the player's next hit. */
        GUARD,
        /** No attack: attack power rises for the rest of the fight. */
        ENRAGE,
        /** No attack: heals a share of max HP. */
        RECOVER,
        /** Several hits in a row, each at the power given. */
        SHOWER,
        /** A hit that takes a share of the player's max MP and holds them still for their next turn. */
        GRAVITY,
        /** No attack: gathers light, warning of the NOVA that follows next turn. */
        CHARGE,
        /** One huge hit, released the turn after CHARGE. */
        NOVA,
        /** No attack: halves the player's next hit, heals a share of max HP and wipes the player's shield. */
        HORIZON
    }

    /** Status effects last this many rounds. */
    public static final int STATUS_ROUNDS = 3;

    private final String name;
    private final Kind kind;
    /** Chance (%) per turn; for ENRAGE and RECOVER, the HP (%) at which they fire. */
    private final int chance;
    private final double power;
    private final int hits;
    /** A second number some kinds use: GRAVITY's MP share, HORIZON's heal share. */
    private final double extra;
    /** The effect drawn on the hero for the cosmic kinds (null = just the lunge). */
    private Skill.Fx fx;
    private int fxColor;

    private MonsterSkill(String name, Kind kind, int chance, double power, int hits, double extra) {
        this.name = name;
        this.kind = kind;
        this.chance = chance;
        this.power = power;
        this.hits = hits;
        this.extra = extra;
    }

    private MonsterSkill(String name, Kind kind, int chance, double power) {
        this(name, kind, chance, power, kind == Kind.MULTI ? 2 : 1, 0);
    }

    public static MonsterSkill heavy(String name, int chance, double damage) { return new MonsterSkill(name, Kind.HEAVY, chance, damage); }
    public static MonsterSkill multi(String name, int chance, double damage) { return new MonsterSkill(name, Kind.MULTI, chance, damage); }
    public static MonsterSkill drain(String name, int chance, double share) { return new MonsterSkill(name, Kind.DRAIN, chance, share); }
    public static MonsterSkill poison(String name, int chance, double maxHpShare) { return new MonsterSkill(name, Kind.POISON, chance, maxHpShare); }
    public static MonsterSkill armorBreak(String name, int chance, double defenseCut) { return new MonsterSkill(name, Kind.ARMOR_BREAK, chance, defenseCut); }
    public static MonsterSkill stun(String name, int chance) { return new MonsterSkill(name, Kind.STUN, chance, 1); }
    public static MonsterSkill guard(String name, int chance) { return new MonsterSkill(name, Kind.GUARD, chance, 0.5); }
    public static MonsterSkill enrage(String name, int hpPct, double attack) { return new MonsterSkill(name, Kind.ENRAGE, hpPct, attack); }
    public static MonsterSkill recover(String name, int hpPct, double maxHpShare) { return new MonsterSkill(name, Kind.RECOVER, hpPct, maxHpShare); }

    // The final boss's moves: picked by its brain, so they carry no chance.
    public static MonsterSkill shower(String name, int hits, double damage) { return new MonsterSkill(name, Kind.SHOWER, 0, damage, hits, 0); }
    public static MonsterSkill gravity(String name, double damage, double mpShare) { return new MonsterSkill(name, Kind.GRAVITY, 0, damage, 1, mpShare); }
    public static MonsterSkill charge(String name) { return new MonsterSkill(name, Kind.CHARGE, 0, 0, 1, 0); }
    public static MonsterSkill nova(String name, double damage) { return new MonsterSkill(name, Kind.NOVA, 0, damage, 1, 0); }
    public static MonsterSkill horizon(String name, double healShare) { return new MonsterSkill(name, Kind.HORIZON, 0, 0.5, 1, healShare); }

    /** The effect drawn on the hero when this lands. */
    public MonsterSkill fx(Skill.Fx fx, int rgb) {
        this.fx = fx;
        this.fxColor = rgb;
        return this;
    }

    public String getName() { return name; }
    public Kind getKind() { return kind; }
    public int getChance() { return chance; }
    public double getPower() { return power; }
    public double getExtra() { return extra; }
    public Skill.Fx getFx() { return fx; }
    public int getFxColor() { return fxColor; }

    /** True if this fires once at low HP rather than by chance. */
    public boolean isTriggered() { return kind == Kind.ENRAGE || kind == Kind.RECOVER; }

    /** True if the skill hits the player. */
    public boolean attacks() {
        return !isTriggered() && kind != Kind.GUARD && kind != Kind.CHARGE && kind != Kind.HORIZON;
    }

    /** Hits this skill lands. */
    public int hits() { return hits; }

    /** Damage multiplier of each hit. */
    public double damageMul() {
        switch (kind) {
            case HEAVY: case MULTI: case SHOWER: case GRAVITY: case NOVA: return power;
            default: return 1;
        }
    }

    /** The skill in words, e.g. "돌진 (25%): 피해 160%". */
    public String describe() {
        String when = isTriggered() ? "HP " + chance + "% 이하" : chance + "%";
        if (chance == 0 && !isTriggered()) return name + ": " + effectText();
        return name + " (" + when + "): " + effectText();
    }

    private String effectText() {
        int pct = (int) Math.round(power * 100), extraPct = (int) Math.round(extra * 100);
        switch (kind) {
            case HEAVY: return "피해 " + pct + "%";
            case MULTI: return "피해 " + pct + "% 2회";
            case DRAIN: return "입힌 피해의 " + pct + "% 회복";
            case POISON: return STATUS_ROUNDS + "턴 동안 최대 HP " + pct + "% 지속 피해";
            case ARMOR_BREAK: return STATUS_ROUNDS + "턴 동안 방어력 -" + pct + "%";
            case STUN: return "다음 턴 행동 불가";
            case GUARD: return "다음에 받는 1타 피해 -" + pct + "%";
            case ENRAGE: return "공격력 " + pct + "%로 상승";
            case SHOWER: return "피해 " + pct + "% " + hits + "회";
            case GRAVITY: return "피해 " + pct + "%, 최대 MP " + extraPct + "% 흡수, 다음 턴 행동 불가";
            case CHARGE: return "빛을 모아 다음 턴 초신성을 터뜨림";
            case NOVA: return "피해 " + pct + "%";
            case HORIZON: return "다음에 받는 1타 피해 -50%, 최대 HP " + extraPct + "% 회복, 보호막 제거";
            default: return "최대 HP " + pct + "% 회복";
        }
    }
}
