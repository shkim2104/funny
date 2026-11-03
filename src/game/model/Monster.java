package game.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Monster {
    /** Chooses every move for a monster that fights by its own rules (the final boss). Never returns null. */
    public interface Brain {
        MonsterSkill next(Monster self);

        /** A brain with no memory, for a new fight. */
        Brain fresh();
    }

    private final String name;
    private final int maxHp;
    private int hp;
    private final int atk;
    private final int def;
    private final int spd;
    private final int expReward;
    private final int goldReward;
    private final List<MonsterSkill> skills = new ArrayList<>();
    // One fight's state; copy() starts it fresh.
    private final Set<MonsterSkill> triggered = new HashSet<>();
    private double attackMul = 1;
    private boolean guarding;
    private Brain brain;
    /** Something the monster did by itself this turn (e.g. awakening), to be shown once. */
    private String note;

    public Monster(String name, int maxHp, int atk, int def, int spd, int expReward, int goldReward) {
        this.name = name;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.atk = atk;
        this.def = def;
        this.spd = spd;
        this.expReward = expReward;
        this.goldReward = goldReward;
    }

    public Monster copy() {
        Monster m = new Monster(name, maxHp, atk, def, spd, expReward, goldReward);
        m.skills.addAll(skills);
        if (brain != null) m.brain = brain.fresh();
        return m;
    }

    /** Gives the monster a brain that picks all of its moves (its skills list is then just for show). */
    public Monster brain(Brain b) {
        brain = b;
        return this;
    }

    public boolean hasBrain() { return brain != null; }

    public void note(String text) { note = text; }

    /** The note left this turn, once; null if none. */
    public String takeNote() {
        String n = note;
        note = null;
        return n;
    }

    /** A fresh copy with HP, ATK and DEF multiplied; speed and rewards stay. */
    public Monster scaled(double mul) {
        Monster m = new Monster(name, (int) Math.round(maxHp * mul), (int) Math.round(atk * mul), (int) Math.round(def * mul),
                spd, expReward, goldReward);
        m.skills.addAll(skills);
        if (brain != null) m.brain = brain.fresh();
        return m;
    }

    /** Gives the monster its skills, in the order they are tried each turn. */
    public Monster skills(MonsterSkill... list) {
        skills.addAll(Arrays.asList(list));
        return this;
    }

    public List<MonsterSkill> getSkills() { return Collections.unmodifiableList(skills); }

    /** What it does this turn: a low-HP skill not used yet, else one by chance, else null for a plain attack. */
    public MonsterSkill rollSkill(Random rnd) {
        if (brain != null) return brain.next(this);
        for (MonsterSkill s : skills) {
            if (s.isTriggered() && !triggered.contains(s) && hp * 100 <= maxHp * s.getChance()) {
                triggered.add(s);
                return s;
            }
        }
        for (MonsterSkill s : skills) {
            if (!s.isTriggered() && rnd.nextInt(100) < s.getChance()) return s;
        }
        return null;
    }

    public void enrage(double mul) { attackMul = mul; }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public void guard() { guarding = true; }

    /** True once after guard(): the hit landing now is the one it blocks. */
    public boolean consumeGuard() {
        boolean was = guarding;
        guarding = false;
        return was;
    }

    public String getName() { return name; }
    public int getMaxHp() { return maxHp; }
    public int getHp() { return hp; }
    public int getAtk() { return (int) Math.round(atk * attackMul); }
    public int getDef() { return def; }
    public int getSpd() { return spd; }
    public int getExpReward() { return expReward; }
    public int getGoldReward() { return goldReward; }

    public boolean isAlive() { return hp > 0; }
    public void takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }
}
