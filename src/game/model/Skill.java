package game.model;

public class Skill {
    public enum Effect {
        /** One physical hit at amount x ATK. */
        PHYSICAL_MULT,
        /** One magic hit at amount x magic power (ATK + MaxMP/4). */
        MAGIC_MULT,
        /** Like MAGIC_MULT but ignores the target's defense. */
        MAGIC_MULT_PIERCE,
        /** Two physical hits, each at amount x ATK. */
        DOUBLE_HIT,
        /** Three physical hits, each at amount x ATK. */
        TRIPLE_HIT,
        /** One physical hit at amount x ATK that also costs 10% of the caster's max HP. */
        RECKLESS_MULT,
        /** One physical hit at amount x ATK that is always a critical. */
        GUARANTEED_CRIT_MULT,
        /** Heals amount x max HP. */
        HEAL_PERCENT
    }

    private final String name;
    private final int requiredLevel;
    private final int mpCost;
    private final Effect effect;
    private final double amount;
    private final String description;

    public Skill(String name, int requiredLevel, int mpCost, Effect effect, double amount, String description) {
        this.name = name;
        this.requiredLevel = requiredLevel;
        this.mpCost = mpCost;
        this.effect = effect;
        this.amount = amount;
        this.description = description;
    }

    public String getName() { return name; }
    public int getRequiredLevel() { return requiredLevel; }
    public int getMpCost() { return mpCost; }
    public Effect getEffect() { return effect; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
}
