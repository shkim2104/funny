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
    /** Halved from 2: ATK grew much faster than monsters, so most fights ended in one hit. */
    public static final int ATK_PER_POINT = 1;
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
    private GearPiece weapon;
    private GearPiece armor;
    /** Potions and ether, by name. Weapons and armor live in gear, one object per piece. */
    private final List<String> inventory = new ArrayList<>();
    private final List<GearPiece> gear = new ArrayList<>();
    private final List<Job> jobPath = new ArrayList<>();
    private final int[] materials = new int[Material.values().length];

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
        // Saves from before gear pieces list weapons and armor here by name: each becomes a +0 piece.
        for (String item : inventory) addItem(item);
        this.weapon = pieceNamed(weaponName);
        this.armor = pieceNamed(armorName);
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
    public String getWeaponName() { return weapon == null ? null : weapon.getItemName(); }
    public String getArmorName() { return armor == null ? null : armor.getItemName(); }

    /** Names of everything usable the player owns: potions and ether, then gear that isn't destroyed. */
    public List<String> getInventory() {
        List<String> all = new ArrayList<>(inventory);
        for (GearPiece g : gear) if (!g.isDestroyed()) all.add(g.getItemName());
        return Collections.unmodifiableList(all);
    }

    public List<String> getConsumables() { return Collections.unmodifiableList(inventory); }

    public void setWeaponName(String weaponName) {
        if (weaponName == null || !weaponName.equals(getWeaponName())) this.weapon = pieceNamed(weaponName);
    }

    public void setArmorName(String armorName) {
        if (armorName == null || !armorName.equals(getArmorName())) this.armor = pieceNamed(armorName);
    }
    public void setUnlockedDungeon(int unlockedDungeon) { this.unlockedDungeon = unlockedDungeon; }

    // ---------- quest board ----------
    private final List<Quest> quests = new ArrayList<>();

    public List<Quest> getQuests() { return Collections.unmodifiableList(quests); }

    /** Used when loading a save. */
    public void setQuests(List<Quest> loaded) {
        quests.clear();
        quests.addAll(loaded);
    }

    /** Tops the board up to Quest.BOARD_SIZE requests. */
    public void fillQuests(List<Dungeon> dungeons, Random rnd) {
        while (quests.size() < Quest.BOARD_SIZE) quests.add(Quest.random(this, dungeons, rnd));
    }

    /** Swaps one request for a fresh one (after its reward was taken, or the player asked for another). */
    public void replaceQuest(Quest q, List<Dungeon> dungeons, Random rnd) {
        int i = quests.indexOf(q);
        if (i >= 0) quests.set(i, Quest.random(this, dungeons, rnd));
    }

    /** Counts a kill on every request; returns the ones it just completed. */
    public List<Quest> recordKill(String monsterName) {
        List<Quest> done = new ArrayList<>();
        for (Quest q : quests) if (q.onKill(monsterName)) done.add(q);
        return done;
    }

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

    /**
     * Power behind magic skills: weapon attack plus a quarter of max MP. At a quarter, a stat point in MP
     * adds as much magic as one in ATK (4 MP = +1, same as +1 ATK) and also grows the mana pool, so it pays
     * off for mages. (It was half while a point gave +2 ATK.)
     */
    public int getMagicAtk() {
        return getAtk() + maxMp / 4;
    }

    public boolean useMp(int amount) {
        if (mp < amount) return false;
        mp -= amount;
        return true;
    }

    public int getAtk() {
        return baseAtk + (weapon != null ? weapon.getValue() : 0);
    }

    public int getDef() {
        return baseDef + (armor != null ? armor.getValue() : 0);
    }

    // ---------- gear pieces ----------

    public List<GearPiece> getGear() { return Collections.unmodifiableList(gear); }
    public GearPiece getWeapon() { return weapon; }
    public GearPiece getArmor() { return armor; }
    public boolean isWorn(GearPiece piece) { return piece == weapon || piece == armor; }

    /** Free to sell, scrap or sacrifice: not worn and not locked. */
    public boolean isSpare(GearPiece piece) { return !isWorn(piece) && !piece.isLocked(); }

    public void wear(GearPiece piece) {
        if (piece.isDestroyed()) return;
        if (piece.isWeapon()) weapon = piece;
        else armor = piece;
    }

    public void unequip(GearPiece piece) {
        if (piece == weapon) weapon = null;
        if (piece == armor) armor = null;
    }

    void removeGear(GearPiece piece) {
        unequip(piece);
        gear.remove(piece);
    }

    /** The shop's half price for a spare piece. Returns the gold, or 0 if it can't be sold. */
    public int sellGear(GearPiece piece) {
        if (!isSpare(piece) || piece.isDestroyed()) return 0;
        int price = piece.getItem().getPrice() / 2;
        removeGear(piece);
        gold += price;
        return price;
    }

    private static boolean isGear(Item item) {
        return item != null && (item.getType() == Item.Type.WEAPON || item.getType() == Item.Type.ARMOR);
    }

    /** A usable piece of that item not worn yet, made if the player has none (an old save's lost copy). */
    private GearPiece pieceNamed(String itemName) {
        if (!isGear(ItemCatalog.get(itemName == null ? "" : itemName))) return null;
        for (GearPiece g : gear) {
            if (g.getItemName().equals(itemName) && !g.isDestroyed() && !isWorn(g)) return g;
        }
        GearPiece made = new GearPiece(itemName);
        gear.add(made);
        return made;
    }

    /**
     * 새 장비 자동 장착: wears the best spare copy of an item if its stat, enhancement included, beats
     * what is worn now (or nothing is). Returns true if it was put on.
     */
    public boolean equipIfBetter(String itemName) {
        GearPiece best = null;
        for (GearPiece g : gear) {
            if (g.getItemName().equals(itemName) && !g.isDestroyed() && !isWorn(g)
                    && (best == null || g.getValue() > best.getValue())) best = g;
        }
        if (best == null) return false;
        GearPiece current = best.isWeapon() ? weapon : armor;
        if (current != null && current.getValue() >= best.getValue()) return false;
        wear(best);
        return true;
    }

    /**
     * 일반 장비 자동 판매: sells every plain [일반] weapon and armor in the bag at the shop's half price.
     * Worn, locked, enhanced and destroyed pieces stay. Returns {pieces sold, gold earned}.
     */
    public int[] sellCommonGear() {
        int count = 0, earned = 0;
        for (GearPiece g : new ArrayList<>(gear)) {
            if (g.getItem().getGrade() != Item.Grade.COMMON || g.getEnhance() > 0 || g.isDestroyed()) continue;
            int price = sellGear(g);
            if (price > 0) {
                count++;
                earned += price;
            }
        }
        return new int[]{count, earned};
    }

    /** Special effects of the equipped weapon and armor (boss gear), added up. */
    public Bonus getGearBonus() {
        Bonus b = Bonus.NONE;
        for (GearPiece piece : new GearPiece[]{weapon, armor}) {
            if (piece != null) b = b.plus(piece.getItem().getBonus());
        }
        return b;
    }

    // ---------- save format: name|level|flags per piece, joined by ';' (flags: W worn, L locked, D destroyed) ----------

    public String encodeGear() {
        List<String> parts = new ArrayList<>();
        for (GearPiece g : gear) {
            parts.add(g.getItemName() + "|" + g.getEnhance() + "|" + (isWorn(g) ? "W" : "") + (g.isLocked() ? "L" : "") + (g.isDestroyed() ? "D" : ""));
        }
        return String.join(";", parts);
    }

    /** Replaces all gear from a saved line. Pieces of items that no longer exist are dropped. */
    public void decodeGear(String line) {
        gear.clear();
        weapon = null;
        armor = null;
        if (line.trim().isEmpty()) return;
        for (String part : line.trim().split(";")) {
            String[] f = part.split("\\|", -1);
            if (f.length < 3 || !isGear(ItemCatalog.get(f[0]))) continue;
            GearPiece g = new GearPiece(f[0], Integer.parseInt(f[1]), f[2].contains("L"), f[2].contains("D"));
            gear.add(g);
            if (f[2].contains("W")) wear(g);
        }
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

    public int getMaterial(Material m) { return materials[m.ordinal()]; }
    public void addMaterial(Material m, int amount) { materials[m.ordinal()] += amount; }

    /** Takes the amount if the player has that much. Returns false (and takes nothing) otherwise. */
    public boolean spendMaterial(Material m, int amount) {
        if (materials[m.ordinal()] < amount) return false;
        materials[m.ordinal()] -= amount;
        return true;
    }

    public void addItem(String itemName) {
        if (isGear(ItemCatalog.get(itemName))) gear.add(new GearPiece(itemName));
        else inventory.add(itemName);
    }

    /** Uses up a potion or ether, or (console shop) a plain spare piece of gear by name. */
    public boolean removeItem(String itemName) {
        if (inventory.remove(itemName)) return true;
        for (GearPiece g : gear) {
            if (g.getItemName().equals(itemName) && isSpare(g) && g.getEnhance() == 0 && !g.isDestroyed()) {
                removeGear(g);
                return true;
            }
        }
        return false;
    }

    public Map<String, Integer> getConsumableCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String item : inventory) {
            counts.merge(item, 1, Integer::sum);
        }
        return counts;
    }

    public Map<String, Integer> getItemCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String item : getInventory()) {
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
