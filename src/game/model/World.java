package game.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class World {
    private World() {}

    public static List<Dungeon> buildDungeons() {
        List<Dungeon> list = new ArrayList<>();

        list.add(new Dungeon(
                "1 : 초원",
                10,
                Arrays.asList(
                        new Monster("들쥐", 18, 8, 1, 8, 8, 10),
                        new Monster("멧돼지", 25, 11, 2, 5, 12, 14),
                        new Monster("들개", 20, 9, 1, 9, 9, 11),
                        new Monster("너구리", 22, 10, 2, 7, 10, 12)
                ),
                new Monster("초원의 우두머리 늑대", 64, 29, 5, 12, 30, 40),
                40, 30
        ));

        list.add(new Dungeon(
                "2 : 어두운 동굴",
                10,
                Arrays.asList(
                        new Monster("박쥐", 22, 12, 2, 14, 14, 16),
                        new Monster("고블린", 30, 15, 4, 8, 18, 20),
                        new Monster("동굴 거미", 26, 13, 3, 12, 16, 18),
                        new Monster("동굴 곰", 34, 14, 3, 6, 20, 22)
                ),
                new Monster("동굴의 오크 대장", 84, 41, 9, 10, 55, 70),
                80, 60
        ));

        list.add(new Dungeon(
                "3 : 잊혀진 폐허",
                10,
                Arrays.asList(
                        new Monster("해골 병사", 40, 20, 6, 7, 24, 26),
                        new Monster("망령", 35, 24, 4, 13, 26, 28),
                        new Monster("좀비", 45, 21, 5, 3, 25, 27),
                        new Monster("폐허의 도적", 38, 23, 5, 14, 25, 27)
                ),
                new Monster("폐허의 리치", 119, 66, 15, 11, 90, 120),
                150, 100
        ));

        list.add(new Dungeon(
                "4 : 마왕성",
                10,
                Arrays.asList(
                        new Monster("어둠의 기사", 55, 27, 9, 9, 34, 36),
                        new Monster("타락한 마법사", 48, 33, 5, 12, 36, 38),
                        new Monster("지옥견", 50, 29, 7, 16, 35, 37),
                        new Monster("악마 궁수", 46, 31, 6, 15, 35, 37)
                ),
                new Monster("마왕", 149, 90, 20, 14, 300, 500),
                220, 150
        ));

        list.add(new Dungeon(
                "5 : 얼음 협곡",
                10,
                Arrays.asList(
                        new Monster("서리 늑대", 60, 36, 7, 15, 40, 42),
                        new Monster("얼음 정령", 58, 38, 8, 13, 42, 44),
                        new Monster("눈사람 골렘", 75, 40, 10, 6, 45, 47),
                        new Monster("빙하 박쥐", 62, 42, 9, 17, 44, 46)
                ),
                new Monster("서리 여왕", 191, 117, 26, 16, 150, 170),
                200, 140
        ));

        list.add(new Dungeon(
                "6 : 불지옥 화산",
                10,
                Arrays.asList(
                        new Monster("화염 도마뱀", 80, 52, 11, 16, 55, 58),
                        new Monster("용암 골렘", 95, 50, 14, 7, 58, 60),
                        new Monster("불꽃 악령", 78, 56, 10, 18, 57, 59),
                        new Monster("화산 오우거", 90, 54, 13, 9, 58, 61)
                ),
                new Monster("화염 군주", 257, 159, 36, 17, 250, 300),
                320, 220
        ));

        list.add(new Dungeon(
                "7 : 천공의 성채",
                10,
                Arrays.asList(
                        new Monster("폭풍 그리폰", 110, 68, 16, 20, 75, 80),
                        new Monster("천공의 기사", 120, 70, 18, 14, 78, 82),
                        new Monster("뇌전 골렘", 130, 66, 20, 12, 80, 85),
                        new Monster("타락 천사", 115, 72, 17, 19, 79, 83)
                ),
                new Monster("타천사왕", 356, 207, 53, 22, 600, 900),
                0, 0
        ));

        return list;
    }
}
