package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A request from the town's quest board: hunt N of a monster, or take down a dungeon's 중간 보스 or boss
 * twice. Only the two newest dungeons the player can enter are asked for, so a request is always a
 * real errand. Progress counts kills made after the quest was posted; the reward is collected at the
 * board. Gold and EXP are written into the quest; materials follow from its type, dungeon and count.
 */
public final class Quest {
    public enum Type { HUNT, MID, BOSS }

    /** How many requests hang on the board at once. */
    public static final int BOARD_SIZE = 3;
    /** Hunts ask for this many kills (MIN + 0~SPREAD-1). */
    static final int HUNT_MIN = 10, HUNT_SPREAD = 9;
    /** A 중간 보스 or boss request: take it down this many times. */
    static final int BOSS_NEED = 2;
    /** Rewards against the monsters' own: gold and EXP per kill asked for. */
    static final double HUNT_GOLD = 3.5, HUNT_EXP = 3, MID_GOLD = 5, MID_EXP = 3, BOSS_GOLD = 2, BOSS_EXP = 3;
    /** Percent: hunts (then 중간 보스, then boss) out of every new request. */
    static final int HUNT_PCT = 60, MID_PCT = 25;

    private final Type type;
    private final String target;
    private final int dungeon;
    private final int need;
    private int progress;
    private final int gold;
    private final int exp;
    /** A bonus item name, or null. */
    private final String item;

    private Quest(Type type, String target, int dungeon, int need, int progress, int gold, int exp, String item) {
        this.type = type;
        this.target = target;
        this.dungeon = dungeon;
        this.need = need;
        this.progress = progress;
        this.gold = gold;
        this.exp = exp;
        this.item = item;
    }

    public Type getType() { return type; }
    public String getTarget() { return target; }
    public int getDungeon() { return dungeon; }
    public int getNeed() { return need; }
    public int getProgress() { return progress; }
    public int getGold() { return gold; }
    public int getExp() { return exp; }
    public String getItem() { return item; }
    public boolean isDone() { return progress >= need; }

    public String getTitle() {
        if (type == Type.HUNT) return target + " " + need + "마리 처치";
        return target + (need > 1 ? " " + need + "번" : "") + " 토벌";
    }

    /** "사냥", "중간 보스 토벌" or "보스 토벌", for the board. */
    public String getKind() {
        return type == Type.HUNT ? "사냥" : type == Type.MID ? "중간 보스 토벌" : "보스 토벌";
    }

    /** Where to go, e.g. "1 : 초원". */
    public String getPlace(List<Dungeon> dungeons) {
        return dungeon >= 0 && dungeon < dungeons.size() ? dungeons.get(dungeon).getName() : "";
    }

    /**
     * Materials on top of gold and EXP: hunts pay graphite (half the kills asked), 중간 보스 requests
     * 색연필 심, boss requests 지우개 가루 (the steadiest way to get it) and 단단한 심.
     */
    public int getMaterial(Material m) {
        switch (m) {
            case GRAPHITE: return type == Type.HUNT ? need / 2 : 0;
            case COLOR_LEAD: return type == Type.MID ? 3 + 2 * dungeon : 0;
            case ERASER_DUST: return type == Type.BOSS ? 2 : 0;
            case HARD_LEAD: return type == Type.BOSS ? 2 + dungeon : 0;
            default: return 0;
        }
    }

    public String getRewardText() {
        StringBuilder s = new StringBuilder(gold + "G · EXP " + exp + (item != null ? " · " + item : ""));
        for (Material m : Material.values()) {
            if (getMaterial(m) > 0) s.append(" · ").append(m.getLabel()).append(" ").append(getMaterial(m)).append("개");
        }
        return s.toString();
    }

    /** Pays the whole reward to the player (the board then replaces the quest). */
    public void payTo(Player player) {
        player.earnGold(gold);
        player.gainExp(exp);
        if (item != null) player.addItem(item);
        for (Material m : Material.values()) player.addMaterial(m, getMaterial(m));
    }

    /** Counts a kill toward this quest; true if that kill just completed it. */
    public boolean onKill(String monsterName) {
        if (isDone() || !target.equals(monsterName)) return false;
        progress++;
        return isDone();
    }

    /** A new request for this player: one of the two newest dungeons they can enter, a hunt most of the time. */
    public static Quest random(Player player, List<Dungeon> dungeons, Random rnd) {
        int maxDungeon = Math.min(player.getUnlockedDungeon(), dungeons.size() - 1);
        int d = Math.max(0, maxDungeon - rnd.nextInt(2));
        Dungeon dungeon = dungeons.get(d);
        int roll = rnd.nextInt(100);
        if (roll < HUNT_PCT) {
            List<Monster> pool = dungeon.getMonsterPool();
            Monster m = pool.get(rnd.nextInt(pool.size()));
            int need = HUNT_MIN + rnd.nextInt(HUNT_SPREAD);
            String item = rnd.nextInt(100) < 50 ? (d >= 3 ? "하이 포션" : "포션") : null;
            return new Quest(Type.HUNT, m.getName(), d, need, 0, (int) Math.round(m.getGoldReward() * need * HUNT_GOLD),
                    (int) Math.round(m.getExpReward() * need * HUNT_EXP), item);
        }
        Monster mid = dungeon.getMidBoss();
        if (roll < HUNT_PCT + MID_PCT && mid != null) {
            return new Quest(Type.MID, mid.getName(), d, BOSS_NEED, 0, (int) Math.round(mid.getGoldReward() * MID_GOLD),
                    (int) Math.round(mid.getExpReward() * BOSS_NEED * MID_EXP), null);
        }
        Monster boss = dungeon.getBoss();
        return new Quest(Type.BOSS, boss.getName(), d, BOSS_NEED, 0, (int) Math.round((boss.getGoldReward() * 3 + dungeon.getRewardGold()) * BOSS_GOLD),
                (int) Math.round(boss.getExpReward() * BOSS_NEED * BOSS_EXP), d >= 2 ? "에테르" : "하이 포션");
    }

    // ---------- save format: fields joined by '|', quests joined by ';' ----------

    String encode() {
        return type + "|" + target + "|" + dungeon + "|" + need + "|" + progress + "|" + gold + "|" + exp + "|" + (item == null ? "-" : item);
    }

    static Quest decode(String s) {
        String[] f = s.split("\\|");
        if (f.length < 8) return null;
        try {
            return new Quest(Type.valueOf(f[0]), f[1], Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]),
                    Integer.parseInt(f[5]), Integer.parseInt(f[6]), f[7].equals("-") ? null : f[7]);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String encodeAll(List<Quest> quests) {
        List<String> parts = new ArrayList<>();
        for (Quest q : quests) parts.add(q.encode());
        return String.join(";", parts);
    }

    public static List<Quest> decodeAll(String line) {
        List<Quest> out = new ArrayList<>();
        if (line == null || line.trim().isEmpty()) return out;
        for (String part : line.trim().split(";")) {
            Quest q = decode(part);
            if (q != null) out.add(q);
        }
        return out;
    }
}
