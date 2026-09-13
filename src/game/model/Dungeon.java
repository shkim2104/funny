package game.model;

import java.util.List;
import java.util.Random;

public class Dungeon {
    private final String name;
    private final int floors;
    private final List<Monster> monsterPool;
    private final Monster boss;
    private final int rewardGold;
    private final int rewardExp;

    public Dungeon(String name, int floors, List<Monster> monsterPool, Monster boss, int rewardGold, int rewardExp) {
        this.name = name;
        this.floors = floors;
        this.monsterPool = monsterPool;
        this.boss = boss;
        this.rewardGold = rewardGold;
        this.rewardExp = rewardExp;
    }

    public String getName() { return name; }
    public int getFloors() { return floors; }
    public int getRewardGold() { return rewardGold; }
    public int getRewardExp() { return rewardExp; }

    public Monster randomMonster(Random rnd) {
        return monsterPool.get(rnd.nextInt(monsterPool.size())).copy();
    }

    public Monster spawnBoss() {
        return boss.copy();
    }
}
