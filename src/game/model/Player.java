package game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Player {
    /** Stat points granted per level-up, to be spent by the player. */
    public static final int POINTS_PER_LEVEL = 4;
    public static final int HP_PER_POINT = 8;
    public static final int MP_PER_POINT = 4;
    public static final int ATK_PER_POINT = 2;
    public static final int DEF_PER_POINT = 2;
    public static final int LUCK_PER_POINT = 1;
    public static final int SPD_PER_POINT = 1;

    public enum Stat { HP, MP, ATK, DEF, LUCK, SPD }

    /**
     * EXP needed to go from `level` to the next one: 20 x level + level^2 / 10.
     * Grows gently (Lv.1: 20, Lv.20: 440, Lv.50: 1250, Lv.69: ~1880) so the 4th job at Lv.70 is
     * reachable (~59k total, roughly 40 runs of the last dungeon). The old x1.4-per-level curve
     * needed ~30k for Lv.20 alone and overflowed int around Lv.60.
     */
    public static int expRequiredFor(int level) {
        return 20 * level + level * level / 10;
    }

    private final String name;
    private int level;
    private int exp;
    private int expToNext;
    private int maxHp;
    private int hp;
    private int maxMp;
    private int mp;
    private int baseAtk;
    private int baseDef;
    private int luck;
    private int baseSpd;
    private int statPoints;
    private int gold;
    private int unlockedDungeon;
    private String weaponName;
    private String armorName;
    private final List<String> inventory = new ArrayList<>();
    private final List<Job> jobPath = new ArrayList<>();

    public Player(String name) {
        this.name = name;
        Random rnd = new Random();
        this.level = 1;
        this.exp = 0;
        this.expToNext = expRequiredFor(1);
        this.maxHp = 45 + rnd.nextInt(11);   // 45~55
        this.hp = this.maxHp;
        this.maxMp = 18 + rnd.nextInt(5);    // 18~22
        this.mp = this.maxMp;
        this.baseAtk = 6 + rnd.nextInt(5);   // 6~10
        this.baseDef = 3 + rnd.nextInt(3);   // 3~5
        this.luck = 3 + rnd.nextInt(6);      // 3~8
        this.baseSpd = 4 + rnd.nextInt(6);   // 4~9
        this.statPoints = 0;
        this.gold = 50;
        this.unlockedDungeon = 0;
        this.weaponName = null;
        this.armorName = null;
    }

    public Player(String name, int level, int exp, int expToNext, int maxHp, int hp, int maxMp, int mp,
                  int baseAtk, int baseDef, int luck, int baseSpd, int statPoints, int gold, int unlockedDungeon,
                  String weaponName, String armorName, List<String> inventory) {
        this.name = name;
        this.level = level;
        this.exp = exp;
        this.expToNext = expToNext;
        this.maxHp = maxHp;
        this.hp = hp;
        this.maxMp = maxMp;
        this.mp = mp;
        this.baseAtk = baseAtk;
        this.baseDef = baseDef;
        this.luck = luck;
        this.baseSpd = baseSpd;
        this.statPoints = statPoints;
        this.gold = gold;
        this.unlockedDungeon = unlockedDungeon;
        this.weaponName = weaponName;
        this.armorName = armorName;
        this.inventory.addAll(inventory);
    }

    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getExp() { return exp; }
    public int getExpToNext() { return expToNext; }
    public int getMaxHp() { return maxHp; }
    public int getHp() { return hp; }
    public int getMaxMp() { return maxMp; }
    public int getMp() { return mp; }
    public int getBaseAtk() { return baseAtk; }
    public int getBaseDef() { return baseDef; }
    public int getLuck() { return luck; }
    public int getSpd() { return baseSpd; }
    public int getStatPoints() { return statPoints; }
    public int getGold() { return gold; }
    public int getUnlockedDungeon() { return unlockedDungeon; }
    public String getWeaponName() { return weaponName; }
    public String getArmorName() { return armorName; }
    public List<String> getInventory() { return Collections.unmodifiableList(inventory); }

    public void setWeaponName(String weaponName) { this.weaponName = weaponName; }
    public void setArmorName(String armorName) { this.armorName = armorName; }
    public void setUnlockedDungeon(int unlockedDungeon) { this.unlockedDungeon = unlockedDungeon; }

    /** Current (latest) job; BEGINNER until the first advancement. */
    public Job getJob() {
        return jobPath.isEmpty() ? Job.BEGINNER : jobPath.get(jobPath.size() - 1);
    }

    /** Jobs taken so far, in tier order (1차, 2차, ...). Classes can be mixed freely. */
    public List<Job> getJobPath() { return Collections.unmodifiableList(jobPath); }

    /** Used when loading a save; gameplay goes through advanceTo(). */
    public void setJobPath(List<Job> path) {
        jobPath.clear();
        jobPath.addAll(path);
    }

    /** The tier the next advancement would be (1~4), or 0 once every tier is done. */
    public int getNextTier() {
        return jobPath.size() < Job.MAX_TIER ? jobPath.size() + 1 : 0;
    }

    /** Jobs the player may pick right now: every job of the next tier, from any class, once the level is reached. */
    public List<Job> getAvailableAdvancements() {
        int tier = getNextTier();
        if (tier == 0 || level < Job.levelForTier(tier)) return new ArrayList<>();
        return Job.ofTier(tier);
    }

    public boolean advanceTo(Job next) {
        if (!getAvailableAdvancements().contains(next)) return false;
        jobPath.add(next);
        return true;
    }

    /** Skills usable at the current level from every job taken, lowest level first. */
    public List<Skill> getSkills() {
        List<Skill> result = new ArrayList<>();
        for (Job j : jobPath) {
            for (Skill s : j.getSkills()) {
                if (level >= s.getRequiredLevel()) result.add(s);
            }
        }
        result.sort((a, b) -> Integer.compare(a.getRequiredLevel(), b.getRequiredLevel()));
        return result;
    }

    /** Power behind magic skills: weapon attack plus a share of max MP, so MP investment pays off for mages. */
    public int getMagicAtk() {
        return getAtk() + maxMp / 4;
    }

    public boolean useMp(int amount) {
        if (mp < amount) return false;
        mp -= amount;
        return true;
    }

    public int getAtk() {
        Item weapon = weaponName != null ? ItemCatalog.get(weaponName) : null;
        return baseAtk + (weapon != null ? weapon.getValue() : 0);
    }

    public int getDef() {
        Item armor = armorName != null ? ItemCatalog.get(armorName) : null;
        return baseDef + (armor != null ? armor.getValue() : 0);
    }

    /** Percent chance (0~100) to land a critical hit, driven by luck. */
    public int getCritChance() {
        return Math.min(50, 5 + luck * 2);
    }

    public boolean isAlive() { return hp > 0; }
    public void takeDamage(int dmg) { hp = Math.max(0, hp - dmg); }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    public void restoreMp(int amount) { mp = Math.min(maxMp, mp + amount); }
    public void reviveAtTown() { hp = Math.max(1, maxHp / 3); mp = maxMp / 2; }

    public void earnGold(int amount) { gold += amount; }
    public void spendGold(int amount) { gold = Math.max(0, gold - amount); }

    public void addItem(String itemName) { inventory.add(itemName); }
    public boolean removeItem(String itemName) { return inventory.remove(itemName); }

    public Map<String, Integer> getItemCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String item : inventory) {
            counts.merge(item, 1, Integer::sum);
        }
        return counts;
    }

    public List<String> getUsableItems() {
        List<String> result = new ArrayList<>();
        for (String itemName : getItemCounts().keySet()) {
            Item item = ItemCatalog.get(itemName);
            if (item.getType() == Item.Type.POTION || item.getType() == Item.Type.ETHER) {
                result.add(itemName);
            }
        }
        return result;
    }

    public List<String> getItemsByType(Item.Type type) {
        List<String> result = new ArrayList<>();
        for (String itemName : getItemCounts().keySet()) {
            Item item = ItemCatalog.get(itemName);
            if (item.getType() == type) result.add(itemName);
        }
        return result;
    }

    public void gainExp(int amount) {
        exp += amount;
        while (exp >= expToNext) {
            exp -= expToNext;
            levelUp();
        }
    }

    private void levelUp() {
        level++;
        hp = maxHp;
        mp = maxMp;
        statPoints += POINTS_PER_LEVEL;
        expToNext = expRequiredFor(level);
        System.out.println("\n*** 레벨 업! " + name + "이(가) Lv." + level
                + " 이(가) 되었습니다! (스탯 포인트 +" + POINTS_PER_LEVEL + ") ***");
    }

    /** Spends one stat point on the given stat. Returns false if no points remain. */
    public boolean spendStatPoint(Stat stat) {
        if (statPoints <= 0) return false;
        switch (stat) {
            case HP:
                maxHp += HP_PER_POINT;
                hp += HP_PER_POINT;
                break;
            case MP:
                maxMp += MP_PER_POINT;
                mp += MP_PER_POINT;
                break;
            case ATK:
                baseAtk += ATK_PER_POINT;
                break;
            case DEF:
                baseDef += DEF_PER_POINT;
                break;
            case LUCK:
                luck += LUCK_PER_POINT;
                break;
            case SPD:
                baseSpd += SPD_PER_POINT;
                break;
        }
        statPoints--;
        return true;
    }
}
