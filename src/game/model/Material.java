package game.model;

import java.util.Random;

/**
 * Forging materials for the blacksmith, kept as counts on the player (not in the bag), like gold.
 * Dungeon index d is 0-based. Later dungeons pay more, and a dead player keeps what they carry.
 */
public enum Material {
    /** From regular monsters and bosses, for the early enhancement steps. */
    GRAPHITE("흑연 조각", "graphite_chip"),
    /** From bosses, dungeon 2 on, for the late enhancement steps. */
    HARD_LEAD("단단한 심", "hard_lead"),
    /** Rare: one use stops an enhancement from destroying the piece. */
    ERASER_DUST("지우개 가루", "eraser_dust");

    private static final double KILL_DROP_PCT = 30, DROP_PCT_PER_LUCK = 0.2, MAX_KILL_DROP_PCT = 60;
    private static final double BOSS_ERASER_CHANCE = 0.20;

    private final String label;
    private final String icon;

    Material(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() { return label; }
    /** Pencil icon file name (images/materials/{icon}.png). */
    public String getIcon() { return icon; }

    /** Percent chance (0~100) that a regular monster drops graphite. Luck keeps paying past the crit cap. */
    public static double killDropChance(int luck) {
        return Math.min(MAX_KILL_DROP_PCT, KILL_DROP_PCT + luck * DROP_PCT_PER_LUCK);
    }

    /** Graphite from one regular kill in dungeon d: 0 when nothing dropped. */
    public static int rollKillGraphite(int d, int luck, Random rnd) {
        return rnd.nextDouble() * 100 < killDropChance(luck) ? 1 + d / 2 : 0;
    }

    public static int bossGraphite(int d) {
        return 3 + 2 * d;
    }

    /** None in dungeon 1, then 1, 1, 2, 2, 3, 3. */
    public static int bossHardLead(int d) {
        return (d + 1) / 2;
    }

    public static int rollBossEraser(Random rnd) {
        return rnd.nextDouble() < BOSS_ERASER_CHANCE ? 1 : 0;
    }
}
