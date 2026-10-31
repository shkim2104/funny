package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A request from the town's quest board: hunt N of a monster, or take down a dungeon's boss. Only
 * dungeons the player can already enter are asked for. Progress counts kills made after the quest
 * was posted; the reward is collected at the board.
 */
public final class Quest {
    public enum Type { HUNT, BOSS }

    /** How many requests hang on the board at once. */
    public static final int BOARD_SIZE = 3;

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
        return type == Type.HUNT ? target + " " + need + "마리 처치" : target + " 토벌";
    }

    /** Where to go, e.g. "1 : 초원". */
    public String getPlace(List<Dungeon> dungeons) {
        return dungeon >= 0 && dungeon < dungeons.size() ? dungeons.get(dungeon).getName() : "";
    }

    /** Boss requests also pay in 지우개 가루, the steadiest way to get it. */
    public int getEraserDust() { return type == Type.BOSS ? 1 : 0; }

    public String getRewardText() {
        return gold + "G · EXP " + exp + (item != null ? " · " + item : "")
                + (getEraserDust() > 0 ? " · " + Material.ERASER_DUST.getLabel() + " " + getEraserDust() + "개" : "");
    }

    /** Counts a kill toward this quest; true if that kill just completed it. */
    public boolean onKill(String monsterName) {
        if (isDone() || !target.equals(monsterName)) return false;
        progress++;
        return isDone();
    }

    /** A new request for this player: one of the dungeons they can enter, a hunt most of the time. */
    public static Quest random(Player player, List<Dungeon> dungeons, Random rnd) {
        int maxDungeon = Math.min(player.getUnlockedDungeon(), dungeons.size() - 1);
        int d = rnd.nextInt(maxDungeon + 1);
        Dungeon dungeon = dungeons.get(d);
        if (rnd.nextInt(100) < 72) {
            List<Monster> pool = dungeon.getMonsterPool();
            Monster m = pool.get(rnd.nextInt(pool.size()));
            int need = 5 + rnd.nextInt(6);
            String item = rnd.nextInt(100) < 35 ? (d >= 3 ? "하이 포션" : "포션") : null;
            return new Quest(Type.HUNT, m.getName(), d, need, 0,
                    (int) Math.round(m.getGoldReward() * need * 1.5), m.getExpReward() * need, item);
        }
        Monster boss = dungeon.getBoss();
        return new Quest(Type.BOSS, boss.getName(), d, 1, 0, boss.getGoldReward() * 3 + dungeon.getRewardGold(),
                boss.getExpReward() * 2, d >= 2 ? "에테르" : "하이 포션");
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
