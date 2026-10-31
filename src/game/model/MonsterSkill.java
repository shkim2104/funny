package game.model;

/**
 * Something a monster can do on its turn instead of a plain attack. Most fire by chance; ENRAGE and
 * RECOVER fire once, the first turn the monster's HP is at or below their threshold.
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
        RECOVER
    }

    /** Status effects last this many rounds. */
    public static final int STATUS_ROUNDS = 3;

    private final String name;
    private final Kind kind;
    /** Chance (%) per turn; for ENRAGE and RECOVER, the HP (%) at which they fire. */
    private final int chance;
    private final double power;

    private MonsterSkill(String name, Kind kind, int chance, double power) {
        this.name = name;
        this.kind = kind;
        this.chance = chance;
        this.power = power;
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

    public String getName() { return name; }
    public Kind getKind() { return kind; }
    public int getChance() { return chance; }
    public double getPower() { return power; }

    /** True if this fires once at low HP rather than by chance. */
    public boolean isTriggered() { return kind == Kind.ENRAGE || kind == Kind.RECOVER; }

    /** True if the skill hits the player. */
    public boolean attacks() { return !isTriggered() && kind != Kind.GUARD; }

    /** Hits this skill lands. */
    public int hits() { return kind == Kind.MULTI ? 2 : 1; }

    /** Damage multiplier of each hit. */
    public double damageMul() { return kind == Kind.HEAVY || kind == Kind.MULTI ? power : 1; }

    /** The skill in words, e.g. "돌진 (25%): 피해 160%". */
    public String describe() {
        String when = isTriggered() ? "HP " + chance + "% 이하" : chance + "%";
        return name + " (" + when + "): " + effectText();
    }

    private String effectText() {
        int pct = (int) Math.round(power * 100);
        switch (kind) {
            case HEAVY: return "피해 " + pct + "%";
            case MULTI: return "피해 " + pct + "% 2회";
            case DRAIN: return "입힌 피해의 " + pct + "% 회복";
            case POISON: return STATUS_ROUNDS + "턴 동안 최대 HP " + pct + "% 지속 피해";
            case ARMOR_BREAK: return STATUS_ROUNDS + "턴 동안 방어력 -" + pct + "%";
            case STUN: return "다음 턴 행동 불가";
            case GUARD: return "다음에 받는 1타 피해 -" + pct + "%";
            case ENRAGE: return "공격력 " + pct + "%로 상승";
            default: return "최대 HP " + pct + "% 회복";
        }
    }
}
