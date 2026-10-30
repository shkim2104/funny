package game.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class World {
    private World() {}

    /** Regular floors before the boss, in every dungeon. */
    public static final int FLOORS = 15;

    /**
     * Monster toughness per dungeon (index = dungeon), applied to the base stats written below. Raised
     * after ATK per stat point was halved, so a regular fight takes a few turns instead of one hit.
     * Speed, EXP and gold are not scaled.
     */
    private static final double[] HP_MUL = {1.5, 3.0, 3.8, 4.4, 4.8, 5.0, 5.2};
    private static final double[] ATK_MUL = {0.8, 0.95, 1.0, 1.05, 1.1, 1.1, 1.15};
    private static final double[] DEF_MUL = {1.0, 1.5, 1.6, 1.6, 1.7, 1.7, 1.8};

    /** Bosses get this share of their dungeon's HP / ATK multipliers: a hard fight, but not a wall. */
    private static final double BOSS_HP_SHARE = 0.75, BOSS_ATK_SHARE = 0.85;

    /** A dungeon's boss, scaled like m() but with the boss shares. */
    private static Monster boss(int d, String name, int hp, int atk, int def, int spd, int exp, int gold) {
        return new Monster(name, (int) Math.round(hp * HP_MUL[d] * BOSS_HP_SHARE), (int) Math.round(atk * ATK_MUL[d] * BOSS_ATK_SHARE),
                (int) Math.round(def * DEF_MUL[d]), spd, exp, gold);
    }

    /** A monster of dungeon d from its base stats: name, HP, ATK, DEF, SPD, EXP, gold. */
    private static Monster m(int d, String name, int hp, int atk, int def, int spd, int exp, int gold) {
        return new Monster(name, (int) Math.round(hp * HP_MUL[d]), (int) Math.round(atk * ATK_MUL[d]),
                (int) Math.round(def * DEF_MUL[d]), spd, exp, gold);
    }

    public static List<Dungeon> buildDungeons() {
        List<Dungeon> list = new ArrayList<>();

        list.add(new Dungeon(
                "1 : 초원",
                FLOORS,
                Arrays.asList(
                        m(0, "들쥐", 18, 8, 1, 8, 8, 10),
                        m(0, "멧돼지", 25, 11, 2, 5, 12, 14),
                        m(0, "들개", 20, 9, 1, 9, 9, 11),
                        m(0, "너구리", 22, 10, 2, 7, 10, 12)
                ),
                boss(0, "초원의 우두머리 늑대", 64, 29, 5, 12, 30, 40),
                40, 30
        ));

        list.add(new Dungeon(
                "2 : 어두운 동굴",
                FLOORS,
                Arrays.asList(
                        m(1, "박쥐", 22, 12, 2, 14, 14, 16),
                        m(1, "고블린", 30, 15, 4, 8, 18, 20),
                        m(1, "동굴 거미", 26, 13, 3, 12, 16, 18),
                        m(1, "동굴 곰", 34, 14, 3, 6, 20, 22)
                ),
                boss(1, "동굴의 오크 대장", 84, 41, 9, 10, 55, 70),
                80, 60
        ));

        list.add(new Dungeon(
                "3 : 잊혀진 폐허",
                FLOORS,
                Arrays.asList(
                        m(2, "해골 병사", 40, 20, 6, 7, 24, 26),
                        m(2, "망령", 35, 24, 4, 13, 26, 28),
                        m(2, "좀비", 45, 21, 5, 3, 25, 27),
                        m(2, "폐허의 도적", 38, 23, 5, 14, 25, 27)
                ),
                boss(2, "폐허의 리치", 119, 66, 15, 11, 90, 120),
                150, 100
        ));

        list.add(new Dungeon(
                "4 : 마왕성",
                FLOORS,
                Arrays.asList(
                        m(3, "어둠의 기사", 55, 27, 9, 9, 34, 36),
                        m(3, "타락한 마법사", 48, 33, 5, 12, 36, 38),
                        m(3, "지옥견", 50, 29, 7, 16, 35, 37),
                        m(3, "악마 궁수", 46, 31, 6, 15, 35, 37)
                ),
                boss(3, "마왕", 149, 90, 20, 14, 300, 500),
                220, 150
        ));

        // EXP raised (monsters +4, boss 150 → 260, clear 140 → 170) so a run here pays more than 마왕성.
        list.add(new Dungeon(
                "5 : 얼음 협곡",
                FLOORS,
                Arrays.asList(
                        m(4, "서리 늑대", 60, 36, 7, 15, 44, 42),
                        m(4, "얼음 정령", 58, 38, 8, 13, 46, 44),
                        m(4, "눈사람 골렘", 75, 40, 10, 6, 48, 47),
                        m(4, "빙하 박쥐", 62, 42, 9, 17, 47, 46)
                ),
                boss(4, "서리 여왕", 191, 117, 26, 16, 260, 170),
                200, 170
        ));

        list.add(new Dungeon(
                "6 : 불지옥 화산",
                FLOORS,
                Arrays.asList(
                        m(5, "화염 도마뱀", 80, 52, 11, 16, 55, 58),
                        m(5, "용암 골렘", 95, 50, 14, 7, 58, 60),
                        m(5, "불꽃 악령", 78, 56, 10, 18, 57, 59),
                        m(5, "화산 오우거", 90, 54, 13, 9, 58, 61)
                ),
                boss(5, "화염 군주", 257, 159, 36, 17, 250, 300),
                320, 220
        ));

        list.add(new Dungeon(
                "7 : 천공의 성채",
                FLOORS,
                Arrays.asList(
                        m(6, "폭풍 그리폰", 110, 68, 16, 20, 75, 80),
                        m(6, "천공의 기사", 120, 70, 18, 14, 78, 82),
                        m(6, "뇌전 골렘", 130, 66, 20, 12, 80, 85),
                        m(6, "타락 천사", 115, 72, 17, 19, 79, 83)
                ),
                boss(6, "타천사왕", 356, 207, 53, 22, 600, 900),
                0, 0
        ));

        return list;
    }
}
