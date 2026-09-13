package game.model;

public class Monster {
    private final String name;
    private final int maxHp;
    private int hp;
    private final int atk;
    private final int def;
    private final int expReward;
    private final int goldReward;

    public Monster(String name, int maxHp, int atk, int def, int expReward, int goldReward) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.atk = atk;
        this.def = def;
        this.expReward = expReward;
        this.goldReward = goldReward;
    }

    public Monster copy() {
        return new Monster(name, maxHp, atk, def, expReward, goldReward);
    }

    public String getName() { return name; }
    public int getMaxHp() { return maxHp; }
    public int getHp() { return hp; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public int getExpReward() { return expReward; }
    public int getGoldReward() { return goldReward; }

    public boolean isAlive() { return hp > 0; }
    public void takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }
}
