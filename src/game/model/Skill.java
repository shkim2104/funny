package game.model;

/**
 * A battle skill, described as data so every job can mix stats however it likes.
 *
 * Damage per hit starts from a power value that adds up the caster's stats times their ratios:
 *   power = ATK x atkRatio + MaxHP x hpRatio + DEF x defRatio + magic power x magicRatio + SPD x spdRatio
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
    private boolean ignoreDefense;
    private boolean alwaysCrit;
    /** Fraction of max HP paid before attacking (never lethal). */
    private double hpCostRatio;
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
    /** Scales with magic power (ATK + MaxMP/2). */
    public Skill magic(double ratio) { this.magicRatio = ratio; return this; }
    /**
     * Scales with speed. A stat point gives +1 SPD but +2 ATK, so a speed ratio of exactly 2x the skill's
     * ATK ratio makes both stats worth the same per point: damage then doesn't depend on how the points are
     * split, and SPD's extra perk is striking first while ATK's is a stronger basic attack.
     */
    public Skill spd(double ratio) { this.spdRatio = ratio; return this; }
    public Skill hits(int hits) { this.hits = hits; return this; }
    public Skill ignoreDefense() { this.ignoreDefense = true; return this; }
    public Skill alwaysCrit() { this.alwaysCrit = true; return this; }
    public Skill hpCost(double ratio) { this.hpCostRatio = ratio; return this; }
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
    public double getHealRatio() { return healRatio; }
    public Fx getFx() { return fx; }
    public int getFxColor() { return fxColor; }

    /** True if the skill deals damage (a pure heal has no damage ratios). */
    public boolean isAttack() {
        return atkRatio > 0 || hpRatio > 0 || defRatio > 0 || magicRatio > 0 || spdRatio > 0;
    }

    /** Power of one hit for this caster, before the target's defense. */
    public int powerFor(Player p) {
        double power = p.getAtk() * atkRatio + p.getMaxHp() * hpRatio
                + p.getDef() * defRatio + p.getMagicAtk() * magicRatio + p.getSpd() * spdRatio;
        return (int) Math.round(power);
    }
}
