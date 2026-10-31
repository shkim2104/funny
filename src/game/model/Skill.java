package game.model;

/**
 * A battle skill, described as data so every job can mix stats however it likes.
 *
 * Damage per hit starts from a power value that adds up the caster's stats times their ratios:
 *   power = ATK x atkRatio + MaxHP x hpRatio + DEF x defRatio + magic power x magicRatio
 *         + (SPD x spdRatio + LUK x lukRatio) x STAT_PARITY
 * and then goes through the usual defense formula. Build skills with the fluent setters, e.g.
 *   Skill.of("브랜디쉬", 20, 12).atk(1.4).hits(2).desc("...")
 */
public class Skill {
    /** Visual effect played on each hit, tinted with the skill's fx color. */
    public enum Fx {
        NONE,
        /** Crescent sword trail across the target. */
        SLASH,
        /** Spear-tip streak driving into the target. */
        THRUST,
        /** Blunt impact: shockwave rings, rays and dust (shields, fists, kicks, roars). */
        SMASH,
        /** Glowing orb flying from the caster, bursting on the target. */
        MAGIC,
        /** Jagged bolt striking down from the sky. */
        LIGHTNING,
        /** Arrow flying from the caster. */
        ARROW,
        /** Spinning throwing star on a slight arc. */
        SHURIKEN,
        /** Muzzle flash and a bullet tracer. */
        SHOT
    }

    private final String name;
    private final int requiredLevel;
    private final int mpCost;
    private String description = "";

    private int hits = 1;
    private double atkRatio;
    private double hpRatio;
    private double defRatio;
    private double magicRatio;
    private double spdRatio;
    private double lukRatio;
    /** Fraction of the damage dealt that heals the caster. */
    private double drainRatio;
    private boolean ignoreDefense;
    private boolean alwaysCrit;
    /** Fraction of max HP paid before attacking (never lethal). */
    private double hpCostRatio;
    /** Fraction of the HP paid that turns into a shield for this fight. */
    private double hpCostShield;
    /** Fraction of max HP restored (after the hits, if the skill also attacks). */
    private double healRatio;
    private Fx fx = Fx.NONE;
    /** Effect tint as 0xRRGGBB (kept as an int so the model doesn't depend on AWT). */
    private int fxColor = 0xFFFFFF;

    private Skill(String name, int requiredLevel, int mpCost) {
        this.name = name;
        this.requiredLevel = requiredLevel;
        this.mpCost = mpCost;
    }

    public static Skill of(String name, int requiredLevel, int mpCost) {
        return new Skill(name, requiredLevel, mpCost);
    }

    public Skill atk(double ratio) { this.atkRatio = ratio; return this; }
    public Skill hp(double ratio) { this.hpRatio = ratio; return this; }
    public Skill def(double ratio) { this.defRatio = ratio; return this; }
    /** Scales with magic power (ATK + MaxMP/4). */
    public Skill magic(double ratio) { this.magicRatio = ratio; return this; }
    /**
     * Scales with speed. The ratios in Job are written 2x the skill's ATK ratio, from when a stat point gave
     * +2 ATK; powerFor applies them at STAT_PARITY (half), so with a point now giving +1 ATK and +1 SPD both
     * stats stay worth the same per point. SPD's extra perk is striking first, ATK's a stronger basic attack.
     */
    public Skill spd(double ratio) { this.spdRatio = ratio; return this; }
    /** Scales with luck, the same way as SPD (applied at STAT_PARITY). */
    public Skill luk(double ratio) { this.lukRatio = ratio; return this; }

    /**
     * SPD and LUK skill ratios are counted at this share: they were balanced against +2 ATK per point,
     * and a point gives +1 ATK now.
     */
    private static final double STAT_PARITY = Player.ATK_PER_POINT / 2.0;

    /** A shield from paid HP never grows past this share of max HP. */
    public static final double MAX_HP_SHIELD = 0.75;

    /** Heals the caster by this fraction of each hit's damage. */
    public Skill drain(double ratio) { this.drainRatio = ratio; return this; }
    public Skill hits(int hits) { this.hits = hits; return this; }
    public Skill ignoreDefense() { this.ignoreDefense = true; return this; }
    public Skill alwaysCrit() { this.alwaysCrit = true; return this; }
    public Skill hpCost(double ratio) { this.hpCostRatio = ratio; return this; }
    /** The HP paid by hpCost comes back as a shield, this share of it. */
    public Skill hpShield(double ratio) { this.hpCostShield = ratio; return this; }
    public Skill heal(double ratio) { this.healRatio = ratio; return this; }
    public Skill desc(String description) { this.description = description; return this; }
    public Skill fx(Fx fx, int rgb) { this.fx = fx; this.fxColor = rgb; return this; }

    public String getName() { return name; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getMpCost() { return mpCost; }
    public String getDescription() { return description; }
    public int getHits() { return hits; }
    public boolean isIgnoreDefense() { return ignoreDefense; }
    public boolean isAlwaysCrit() { return alwaysCrit; }
    public double getHpCostRatio() { return hpCostRatio; }
    public double getHpCostShield() { return hpCostShield; }
    public double getHealRatio() { return healRatio; }
    public double getDrainRatio() { return drainRatio; }
    public Fx getFx() { return fx; }
    public int getFxColor() { return fxColor; }

    /** True if the skill deals damage (a pure heal has no damage ratios). */
    public boolean isAttack() {
        return atkRatio > 0 || hpRatio > 0 || defRatio > 0 || magicRatio > 0 || spdRatio > 0 || lukRatio > 0;
    }

    /** Power of one hit for this caster, before the target's defense. */
    public int powerFor(Player p) {
        double power = p.getAtk() * atkRatio + p.getMaxHp() * hpRatio
                + p.getDef() * defRatio + p.getMagicAtk() * magicRatio
                + (p.getSpd() * spdRatio + p.getLuck() * lukRatio) * STAT_PARITY;
        return (int) Math.round(power);
    }
}
