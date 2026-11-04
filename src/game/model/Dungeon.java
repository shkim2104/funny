package game.model;

import java.util.List;
import java.util.Random;

public class Dungeon {
    private final String name;
    private final int floors;
    private final List<Monster> monsterPool;
    private final Monster boss;
    /** 중간 보스: fought on MID_FLOOR in place of a regular monster. */
    private final Monster midBoss;
    private final int rewardGold;
    private final int rewardExp;

    /** The stage (1-based) where the 중간 보스 waits, halfway through. */
    public static final int MID_FLOOR = 10;

    public Dungeon(String name, int floors, List<Monster> monsterPool, Monster midBoss, Monster boss, int rewardGold, int rewardExp) {
        this.name = name;
        this.floors = floors;
        this.monsterPool = monsterPool;
        this.midBoss = midBoss;
        this.boss = boss;
        this.rewardGold = rewardGold;
        this.rewardExp = rewardExp;
    }

    public String getName() { return name; }
    public int getFloors() { return floors; }
    public int getRewardGold() { return rewardGold; }
    public int getRewardExp() { return rewardExp; }

    /** A regular monster for this stage (1-based): stronger the further in it is. */
    public Monster randomMonster(int floor, Random rnd) {
        return monsterPool.get(rnd.nextInt(monsterPool.size())).scaled(1 + (floor - 1) * World.FLOOR_GROWTH);
    }

    public Monster spawnBoss() {
        return boss.copy();
    }

    /** True if this stage (1-based) is the 중간 보스's. */
    public boolean isMidFloor(int floor) {
        return midBoss != null && floor == MID_FLOOR && floor < floors;
    }

    public Monster spawnMidBoss() {
        return midBoss.copy();
    }

    public Monster getMidBoss() {
        return midBoss;
    }

    /** The regular monsters (templates; copy() before fighting one). */
    public List<Monster> getMonsterPool() {
        return java.util.Collections.unmodifiableList(monsterPool);
    }

    public Monster getBoss() {
        return boss;
    }
}
