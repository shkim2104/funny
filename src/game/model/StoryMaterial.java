package game.model;

import java.util.Random;

/**
 * 이계의 단서: the materials ??? brews the weakening potion from. One comes from each dungeon, and only
 * once the hero has cleared the last dungeon for the first time (the invasion has begun).
 */
public enum StoryMaterial {
    STARLIGHT_DEW("별빛 이슬", "starlight_dew", 0),
    METEOR_SHARD("운석 조각", "meteor_shard", 1),
    CONSTELLATION_SLAB("별자리 석판 조각", "constellation_slab", 2),
    DARK_CORE("어둠의 핵", "dark_core", 3),
    ETERNAL_ICE("영원빙 결정", "eternal_ice", 4),
    SUN_EMBER("태양의 불씨", "sun_ember", 5),
    FALLEN_FEATHER("타락한 날개깃", "fallen_feather", 6);

    /** Chance (%) that a regular monster of the material's dungeon drops one. */
    public static final int KILL_DROP_PCT = 22;
    /** How many the dungeon's boss always drops. */
    public static final int BOSS_COUNT = 3;

    private final String label;
    private final String icon;
    private final int dungeon;

    StoryMaterial(String label, String icon, int dungeon) {
        this.label = label;
        this.icon = icon;
        this.dungeon = dungeon;
    }

    public String getLabel() { return label; }
    /** Icon key: images/story_items/{icon}.png. */
    public String getIcon() { return icon; }
    /** Dungeon index (World.buildDungeons() order) it drops in. */
    public int getDungeon() { return dungeon; }

    /** The material that drops in dungeon d. */
    public static StoryMaterial of(int d) {
        for (StoryMaterial m : values()) if (m.dungeon == d) return m;
        return null;
    }

    /** Rolls a regular kill in dungeon d: 0 or 1. */
    public static int rollKill(Random rnd) {
        return rnd.nextInt(100) < KILL_DROP_PCT ? 1 : 0;
    }
}
