package game.model;

public class Skill {
    public enum Effect {
        PHYSICAL_MULT,
        MAGIC_MULT,
        MAGIC_MULT_PIERCE,
        DOUBLE_HIT,
        TRIPLE_HIT,
        RECKLESS_MULT,
        GUARANTEED_CRIT_MULT,
        HEAL_PERCENT
    }

    private final String name;
    private final int mpCost;
    private final Effect effect;
    private final double amount;
    private final String description;

    public Skill(String name, int mpCost, Effect effect, double amount, String description) {
        this.name = name;
        this.mpCost = mpCost;
        this.effect = effect;
        this.amount = amount;
        this.description = description;
    }

    public String getName() { return name; }
    public int getMpCost() { return mpCost; }
    public Effect getEffect() { return effect; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
}
