package game.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class World {
    private World() {}

    /** Regular floors before the boss, in every dungeon. */
    public static final int FLOORS = 20;
    /** Each stage past the first adds this share of a regular monster's HP, ATK and DEF. */
    public static final double FLOOR_GROWTH = 0.02;

    /**
     * Toughness per dungeon (index = dungeon), applied to the base stats written below. Regular monsters
     * are a steady few turns each, since augments pile up over a run; each boss is the dungeon's real
     * test, with its own multipliers and BOSS_AUGMENT_SHARE. Tuned (with Player.TIER_BONUS) so each dungeon
     * falls at the advancement meant for it: dungeon 2 after 1차, 3~4 at 2차, 5~7 at 3차 and the final boss
     * at 4차. A typical character with augments first clears them around Lv. 7, 17, 27, 35, 52, 61 and 66
     * with two potions per stage (v2.3.0). Speed and gold are not scaled.
     */
    private static final double[] HP_MUL = {1.3, 6.0, 9.7, 10.5, 16.0, 11.8, 8.9};
    private static final double[] ATK_MUL = {0.5, 1.05, 1.34, 1.35, 2.18, 1.70, 1.40};
    private static final double[] DEF_MUL = {0.8, 1.9, 2.3, 2.6, 2.75, 2.9, 3.1};
    private static final double[] BOSS_HP_MUL = {0.75, 1.95, 4.6, 5.1, 9.15, 7.07, 5.16};
    private static final double[] BOSS_ATK_MUL = {0.55, 0.72, 1.09, 1.1, 1.86, 1.44, 1.18};
    private static final double[] EXP_MUL = {1.25, 1.25, 1.25, 1.25, 1.25, 1.25, 1.25};
    /** 중간 보스 on top of the dungeon's regular multipliers: a few regular monsters' worth, not a real boss. */
    private static final double[] MID_HP_MUL = {1.2, 1.0, 0.6, 0.65, 0.6, 0.7, 1.0};
    private static final double[] MID_ATK_MUL = {0.85, 0.95, 0.9, 0.85, 0.77, 0.8, 1.0};

    /** A dungeon's 중간 보스: like m() with MID_HP_MUL and MID_ATK_MUL on top. */
    private static Monster mid(int d, String name, int hp, int atk, int def, int spd, int exp, int gold) {
        return new Monster(name, (int) Math.round(hp * HP_MUL[d] * MID_HP_MUL[d]), (int) Math.round(atk * ATK_MUL[d] * MID_ATK_MUL[d]),
                (int) Math.round(def * DEF_MUL[d]), spd, (int) Math.round(exp * EXP_MUL[d]), gold);
    }

    /** A dungeon's boss, scaled like m() but with the boss multipliers. */
    private static Monster boss(int d, String name, int hp, int atk, int def, int spd, int exp, int gold) {
        return new Monster(name, (int) Math.round(hp * BOSS_HP_MUL[d]), (int) Math.round(atk * BOSS_ATK_MUL[d]),
                (int) Math.round(def * DEF_MUL[d]), spd, (int) Math.round(exp * EXP_MUL[d]), gold);
    }

    /** 위압: in a boss fight, augments work at this strength. Gear and stats still count in full. */
    public static final double BOSS_AUGMENT_SHARE = 0.75;

    /** A monster of dungeon d from its base stats: name, HP, ATK, DEF, SPD, EXP, gold. */
    private static Monster m(int d, String name, int hp, int atk, int def, int spd, int exp, int gold) {
        return new Monster(name, (int) Math.round(hp * HP_MUL[d]), (int) Math.round(atk * ATK_MUL[d]),
                (int) Math.round(def * DEF_MUL[d]), spd, (int) Math.round(exp * EXP_MUL[d]), gold);
    }

    // ---------- the final boss ----------
    /** 이계의 존재 as the hero meets it, after the weakening potion. */
    /** HP, ATK, DEF, SPD and 초신성's power (an array so the balance tools can try other numbers). */
    private static final double[] FINAL = {19200, 632, 40, 24, 2.4};
    /** Without the potion it would be this much stronger (only for the balance tools: the game never lets it be fought). */
    static final double UNWEAKENED_HP = 1 / 0.6, UNWEAKENED_ATK = 1 / 0.7, UNWEAKENED_NOVA = 3.2;
    public static final String FINAL_BOSS = "이계의 존재";
    /** Rewards: the first defeat, then each one after. */
    public static final int FINAL_EXP = 3000, FINAL_GOLD = 5000, REPEAT_EXP = 800, REPEAT_GOLD = 1200;

    public static Monster finalBoss(boolean weakened) {
        double hp = weakened ? 1 : UNWEAKENED_HP, atk = weakened ? 1 : UNWEAKENED_ATK;
        MonsterSkill shower = MonsterSkill.shower("유성우", 3, 0.55).fx(Skill.Fx.MAGIC, 0xB070FF);
        MonsterSkill gravity = MonsterSkill.gravity("블랙홀", 0.9, 0.3).fx(Skill.Fx.SMASH, 0x4A2470);
        MonsterSkill charge = MonsterSkill.charge("초신성 — 빛을 모은다");
        MonsterSkill nova = MonsterSkill.nova("초신성", weakened ? FINAL[4] : UNWEAKENED_NOVA).fx(Skill.Fx.LIGHTNING, 0xFFE6A0);
        MonsterSkill horizon = MonsterSkill.horizon("사건의 지평선", 0.08);
        return new Monster(FINAL_BOSS, (int) Math.round(FINAL[0] * hp), (int) Math.round(FINAL[1] * atk), (int) FINAL[2], (int) FINAL[3], 0, 0)
                .skills(shower, gravity, nova, horizon)
                .brain(new CosmicBrain(shower, gravity, charge, nova, horizon));
    }

    public static List<Dungeon> buildDungeons() {
        List<Dungeon> list = new ArrayList<>();

        list.add(new Dungeon(
                "1 : 초원",
                FLOORS,
                Arrays.asList(
                        m(0, "들쥐", 18, 8, 1, 8, 8, 10).skills(MonsterSkill.heavy("앞니 물기", 20, 1.4)),
                        m(0, "멧돼지", 25, 11, 2, 5, 12, 14).skills(MonsterSkill.heavy("돌진", 25, 1.6)),
                        m(0, "들개", 20, 9, 1, 9, 9, 11).skills(MonsterSkill.multi("연속 물기", 20, 0.7)),
                        m(0, "너구리", 22, 10, 2, 7, 10, 12).skills(MonsterSkill.armorBreak("흙 뿌리기", 20, 0.2))
                ),
                mid(0, "성난 들소", 68, 11, 2, 8, 40, 48).skills(MonsterSkill.heavy("뿔 박치기", 30, 1.5),
                        MonsterSkill.stun("땅 구르기", 15)),
                boss(0, "초원의 우두머리 늑대", 64, 29, 5, 12, 30, 40).skills(MonsterSkill.enrage("우두머리의 포효", 50, 1.3),
                        MonsterSkill.heavy("할퀴기", 25, 1.5)),
                40, 30
        ));

        list.add(new Dungeon(
                "2 : 어두운 동굴",
                FLOORS,
                Arrays.asList(
                        m(1, "박쥐", 22, 12, 2, 14, 14, 16).skills(MonsterSkill.drain("흡혈", 30, 0.5)),
                        m(1, "고블린", 30, 15, 4, 8, 18, 20).skills(MonsterSkill.heavy("몽둥이질", 25, 1.6)),
                        m(1, "동굴 거미", 26, 13, 3, 12, 16, 18).skills(MonsterSkill.poison("독니", 30, 0.03)),
                        m(1, "동굴 곰", 34, 14, 3, 6, 20, 22).skills(MonsterSkill.stun("앞발 내려치기", 15))
                ),
                mid(1, "고블린 주술사", 90, 16, 4, 11, 70, 76).skills(MonsterSkill.poison("독 주문", 30, 0.03),
                        MonsterSkill.recover("주술 치유", 40, 0.12)),
                boss(1, "동굴의 오크 대장", 84, 41, 9, 10, 55, 70).skills(MonsterSkill.enrage("전투의 함성", 50, 1.3),
                        MonsterSkill.stun("대지 강타", 15), MonsterSkill.heavy("찍어 내리기", 25, 1.6)),
                80, 60
        ));

        list.add(new Dungeon(
                "3 : 잊혀진 폐허",
                FLOORS,
                Arrays.asList(
                        m(2, "해골 병사", 40, 20, 6, 7, 24, 26).skills(MonsterSkill.guard("뼈 방패", 25)),
                        m(2, "망령", 35, 24, 4, 13, 26, 28).skills(MonsterSkill.armorBreak("저주", 30, 0.3)),
                        m(2, "좀비", 45, 21, 5, 3, 25, 27).skills(MonsterSkill.poison("썩은 물기", 30, 0.03)),
                        m(2, "폐허의 도적", 38, 23, 5, 14, 25, 27).skills(MonsterSkill.multi("이중 찌르기", 25, 0.75))
                ),
                mid(2, "해골 기사", 126, 26, 6, 10, 100, 108).skills(MonsterSkill.guard("녹슨 방패", 25),
                        MonsterSkill.heavy("망자의 일격", 25, 1.6)),
                boss(2, "폐허의 리치", 119, 66, 15, 11, 90, 120).skills(MonsterSkill.recover("재생의 의식", 40, 0.2),
                        MonsterSkill.armorBreak("죽음의 저주", 20, 0.3), MonsterSkill.drain("생명력 흡수", 25, 0.5)),
                150, 100
        ));

        list.add(new Dungeon(
                "4 : 마왕성",
                FLOORS,
                Arrays.asList(
                        m(3, "어둠의 기사", 55, 27, 9, 9, 34, 36).skills(MonsterSkill.heavy("암흑 베기", 25, 1.7)),
                        m(3, "타락한 마법사", 48, 33, 5, 12, 36, 38).skills(MonsterSkill.armorBreak("부식의 저주", 30, 0.3)),
                        m(3, "지옥견", 50, 29, 7, 16, 35, 37).skills(MonsterSkill.poison("화염 숨결", 30, 0.03)),
                        m(3, "악마 궁수", 46, 31, 6, 15, 35, 37).skills(MonsterSkill.multi("이중 사격", 25, 0.8))
                ),
                mid(3, "마왕의 근위대장", 167, 36, 8, 14, 140, 148).skills(MonsterSkill.armorBreak("갑옷 파쇄", 30, 0.3),
                        MonsterSkill.multi("연속 베기", 25, 0.8)),
                boss(3, "마왕", 149, 90, 20, 14, 300, 500).skills(MonsterSkill.enrage("마왕의 각성", 50, 1.35),
                        MonsterSkill.poison("지옥불", 25, 0.04), MonsterSkill.heavy("파멸의 일격", 20, 2.0)),
                220, 150
        ));

        // EXP raised (monsters +4, boss 150 → 260, clear 140 → 170) so a run here pays more than 마왕성.
        list.add(new Dungeon(
                "5 : 얼음 협곡",
                FLOORS,
                Arrays.asList(
                        m(4, "서리 늑대", 60, 36, 7, 15, 44, 42).skills(MonsterSkill.stun("빙결 물기", 15)),
                        m(4, "얼음 정령", 58, 38, 8, 13, 46, 44).skills(MonsterSkill.armorBreak("냉기", 30, 0.3)),
                        m(4, "눈사람 골렘", 75, 40, 10, 6, 48, 47).skills(MonsterSkill.guard("얼음 갑옷", 25)),
                        m(4, "빙하 박쥐", 62, 42, 9, 17, 47, 46).skills(MonsterSkill.drain("얼음 흡혈", 30, 0.5))
                ),
                mid(4, "설인", 204, 47, 10, 13, 184, 180).skills(MonsterSkill.stun("눈덩이 투척", 15),
                        MonsterSkill.heavy("얼음 주먹", 25, 1.6)),
                boss(4, "서리 여왕", 191, 117, 26, 16, 260, 170).skills(MonsterSkill.recover("빙결의 치유", 40, 0.2),
                        MonsterSkill.stun("절대 영도", 15), MonsterSkill.guard("얼음 장막", 20), MonsterSkill.multi("눈보라", 20, 0.9)),
                200, 170
        ));

        list.add(new Dungeon(
                "6 : 불지옥 화산",
                FLOORS,
                Arrays.asList(
                        m(5, "화염 도마뱀", 80, 52, 11, 16, 55, 58).skills(MonsterSkill.poison("화염 숨결", 30, 0.04)),
                        m(5, "용암 골렘", 95, 50, 14, 7, 58, 60).skills(MonsterSkill.guard("용암 피부", 25)),
                        m(5, "불꽃 악령", 78, 56, 10, 18, 57, 59).skills(MonsterSkill.multi("연속 불꽃", 25, 0.8)),
                        m(5, "화산 오우거", 90, 54, 13, 9, 58, 61).skills(MonsterSkill.stun("대지 강타", 15))
                ),
                mid(5, "화염 와이번", 274, 64, 14, 13, 228, 238).skills(MonsterSkill.poison("화염 숨결", 30, 0.04),
                        MonsterSkill.multi("날개 연타", 25, 0.8)),
                boss(5, "화염 군주", 257, 159, 36, 17, 250, 300).skills(MonsterSkill.enrage("끓어오르는 분노", 50, 1.4),
                        MonsterSkill.poison("지옥불", 25, 0.05), MonsterSkill.multi("화염 폭풍", 20, 0.9)),
                320, 220
        ));

        list.add(new Dungeon(
                "7 : 천공의 성채",
                FLOORS,
                Arrays.asList(
                        m(6, "폭풍 그리폰", 110, 68, 16, 20, 75, 80).skills(MonsterSkill.multi("폭풍 발톱", 25, 0.8)),
                        m(6, "천공의 기사", 120, 70, 18, 14, 78, 82).skills(MonsterSkill.heavy("신성 강타", 25, 1.8)),
                        m(6, "뇌전 골렘", 130, 66, 20, 12, 80, 85).skills(MonsterSkill.stun("감전", 20)),
                        m(6, "타락 천사", 115, 72, 17, 19, 79, 83).skills(MonsterSkill.armorBreak("타락의 빛", 30, 0.35))
                ),
                mid(6, "폭풍의 수호자", 380, 83, 20, 17, 312, 330).skills(MonsterSkill.stun("낙뢰", 15),
                        MonsterSkill.multi("번개 사슬", 25, 0.8)),
                boss(6, "타천사왕", 356, 207, 53, 22, 600, 900).skills(MonsterSkill.enrage("타락의 각성", 50, 1.4),
                        MonsterSkill.recover("빛의 재생", 30, 0.2), MonsterSkill.armorBreak("타락의 빛", 20, 0.35), MonsterSkill.heavy("심판", 20, 2.2)),
                0, 0
        ));

        return list;
    }
}
