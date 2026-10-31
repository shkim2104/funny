package game.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Every job, organized by class (전사, 마법사, ...), route and advancement tier (1~4차).
 *
 * Advancing is not locked to a class: at each tier the player may pick ANY job of that tier, so a
 * 1차 전사 can become a 2차 궁수 and then a 3차 마법사, collecting the skills of every job taken.
 * Each warrior route learns one skill per tier, so a full path fills the four battle skill slots.
 */
public enum Job {
    BEGINNER("초보자", "", 0, "", "아직 길을 정하지 않은 모험가"),

    // ── 전사: 공격력 + 체력 (피를 대가로 강해지는 창술) ──
    LANCER("창잡이", "전사", 1, "공격력+체력", "창을 휘두르며 체력을 힘으로 바꾸는 전사",
            Skill.of("피어싱 스피어", 10, 6).atk(1.9).hp(0.24).fx(Skill.Fx.THRUST, 0xE8C46A)
                    .desc("창으로 꿰뚫는다 (공격력 190% + 최대 HP 24%)")),
    SPEARMAN("스피어맨", "전사", 2, "공격력+체력", "넘치는 체력을 실은 연속 찌르기",
            Skill.of("스피어 크러셔", 20, 12).atk(1.4).hp(0.19).hits(2).fx(Skill.Fx.THRUST, 0xF0A040)
                    .desc("두 번 찌른다 (공격력 140% + 최대 HP 19%) x2")),
    DRAGON_KNIGHT("드래곤나이트", "전사", 3, "공격력+체력", "용의 피로 싸우는 창기사",
            Skill.of("드래곤 로어", 50, 20).atk(2.8).hp(0.47).hpCost(0.1).hpShield(2.0).fx(Skill.Fx.SMASH, 0xFF5A2A)
                    .desc("HP 10%를 바쳐 포효하고, 바친 HP의 2배만큼 보호막을 두른다 (공격력 280% + 최대 HP 47%)")),
    DARK_KNIGHT("다크나이트", "전사", 4, "공격력+체력", "생명을 태워 어둠의 힘을 휘두른다",
            Skill.of("다크 임페일", 70, 30).atk(3.75).hp(0.71).hpCost(0.15).hpShield(2.0).fx(Skill.Fx.THRUST, 0x9B4DFF)
                    .desc("HP 15%를 바쳐 꿰뚫고, 바친 HP의 2배만큼 보호막을 두른다 (공격력 375% + 최대 HP 71%)")),

    // ── 전사: 오로지 공격력 (우직한 검사) ──
    SWORDSMAN("검사", "전사", 1, "공격력", "공격력 하나만 믿고 베어 넘기는 우직한 전사",
            Skill.of("파워 스트라이크", 10, 6).atk(2.55).fx(Skill.Fx.SLASH, 0xFF7A3C)
                    .desc("있는 힘껏 벤다 (공격력 255%)")),
    FIGHTER("파이터", "전사", 2, "공격력", "쉴 새 없이 몰아치는 검",
            Skill.of("브랜디쉬", 20, 12).atk(1.9).hits(2).fx(Skill.Fx.SLASH, 0x6FC3FF)
                    .desc("두 번 벤다 (공격력 190% x2)")),
    CRUSADER("크루세이더", "전사", 3, "공격력", "기세를 몰아 연속 공격을 퍼붓는 성전사",
            Skill.of("콤보 어택", 50, 20).atk(1.75).hits(3).fx(Skill.Fx.SLASH, 0xFFD24A)
                    .desc("세 번 몰아친다 (공격력 175% x3)")),
    HERO("히어로", "전사", 4, "공격력", "모든 것을 건 일격으로 전장을 끝낸다",
            Skill.of("레이징 블로우", 70, 30).atk(4.3).alwaysCrit().fx(Skill.Fx.SLASH, 0xFF2E3E)
                    .desc("반드시 치명타가 나는 필살의 일격 (공격력 430%)")),

    // ── 전사: 방어력 + 체력 (단단한 기사) ──
    SQUIRE("견습 기사", "전사", 1, "방어력+체력", "방패를 무기로 삼는 단단한 전사",
            Skill.of("실드 배시", 10, 6).atk(0.4).def(2).hp(0.08).fx(Skill.Fx.SMASH, 0x8FB4E0)
                    .desc("방패로 들이받는다 (공격력 40% + 방어력 200% + 최대 HP 8%)")),
    PAGE("페이지", "전사", 2, "방어력+체력", "막아내며 버티는 수호 기사",
            Skill.of("가드 크러쉬", 20, 12).def(2.4).hp(0.12).heal(0.1).fx(Skill.Fx.SMASH, 0x7FD0C8)
                    .desc("방패로 짓누르고 HP 10% 회복 (방어력 240% + 최대 HP 12%)")),
    KNIGHT("나이트", "전사", 3, "방어력+체력", "성스러운 돌격으로 방어를 꿰뚫는 기사",
            Skill.of("홀리 차지", 50, 20).def(2.8).hp(0.2).ignoreDefense().fx(Skill.Fx.THRUST, 0xFFE27A)
                    .desc("방어를 무시하는 돌격 (방어력 280% + 최대 HP 20%)")),
    PALADIN("팔라딘", "전사", 4, "방어력+체력", "쓰러지지 않는 성기사",
            Skill.of("블래스트", 70, 30).def(3.6).hp(0.28).heal(0.2).fx(Skill.Fx.SMASH, 0xFFF0A0)
                    .desc("성스러운 일격 후 HP 20% 회복 (방어력 360% + 최대 HP 28%)")),

    // Mage skills cost about 30% more MP than the warrior skill of the same tier: MP is their firepower.

    // ── 마법사: 마력 (불·독, 한 방이 가장 센 고화력) ──
    // MAGE keeps its enum name so saves that already advanced to "마법사" still load.
    MAGE("마법사", "마법사", 1, "마력", "MP를 마력으로 바꿔 강력한 한 방을 날리는 마법사",
            Skill.of("에너지 볼트", 10, 8).magic(1.8).fx(Skill.Fx.MAGIC, 0x8A7CFF)
                    .desc("마력 탄환을 쏜다 (마력 180%)")),
    FP_WIZARD("위자드(불,독)", "마법사", 2, "마력", "불꽃을 다루기 시작한 위자드",
            Skill.of("파이어 애로우", 20, 16).magic(2.6).fx(Skill.Fx.MAGIC, 0xFF8A3A)
                    .desc("불꽃 화살을 쏜다 (마력 260%)")),
    FP_MAGE("메이지(불,독)", "마법사", 3, "마력", "폭발하는 화염으로 적을 날려 버리는 메이지",
            Skill.of("익스플로전", 50, 28).magic(3.6).fx(Skill.Fx.SMASH, 0xFF5A2A)
                    .desc("적을 중심으로 폭발을 일으킨다 (마력 360%)")),
    FP_ARCHMAGE("아크메이지(불,독)", "마법사", 4, "마력", "하늘에서 불덩이를 떨어뜨리는 대마법사",
            Skill.of("메테오", 70, 42).magic(5.0).fx(Skill.Fx.SMASH, 0xE0283C)
                    .desc("거대한 운석을 떨어뜨린다 (마력 500%)")),

    // ── 마법사: 마력 + 방어 무시 (썬·콜, 방어를 꿰뚫는 연타) ──
    IL_ADEPT("번개술사", "마법사", 1, "마력+방어무시", "방어를 무시하는 번개를 다루는 마법사",
            Skill.of("썬더 볼트", 10, 8).magic(0.9).ignoreDefense().fx(Skill.Fx.LIGHTNING, 0xFFE84A)
                    .desc("방어를 무시하는 번개 (마력 90%)")),
    IL_WIZARD("위자드(썬,콜)", "마법사", 2, "마력+방어무시", "얼음과 번개를 오가는 위자드",
            Skill.of("콜드 빔", 20, 15).magic(0.7).hits(2).ignoreDefense().fx(Skill.Fx.MAGIC, 0x7CC8FF)
                    .desc("냉기 광선을 두 번 쏜다 (마력 70% x2, 방어 무시)")),
    IL_MAGE("메이지(썬,콜)", "마법사", 3, "마력+방어무시", "번개 창을 연달아 내리꽂는 메이지",
            Skill.of("썬더 스피어", 50, 26).magic(0.8).hits(3).ignoreDefense().fx(Skill.Fx.LIGHTNING, 0xFFE04A)
                    .desc("번개 창을 세 번 내리꽂는다 (마력 80% x3, 방어 무시)")),
    IL_ARCHMAGE("아크메이지(썬,콜)", "마법사", 4, "마력+방어무시", "끝없이 이어지는 번개를 부르는 대마법사",
            Skill.of("체인 라이트닝", 70, 38).magic(0.85).hits(4).ignoreDefense().fx(Skill.Fx.LIGHTNING, 0xBFE0FF)
                    .desc("연쇄 번개가 네 번 몰아친다 (마력 85% x4, 방어 무시)")),

    // ── 마법사: 마력 + 회복 (사제, 싸우면서 버티는 성직자) ──
    ACOLYTE("복사", "마법사", 1, "마력+회복", "치유의 기도를 배우는 견습 성직자",
            // Pure heal: no hit effect; the color only tints its button (the heal itself plays a recovery effect).
            Skill.of("힐", 10, 10).heal(0.3).fx(Skill.Fx.NONE, 0x7CF09A)
                    .desc("최대 HP의 30%를 회복한다")),
    CLERIC("클레릭", "마법사", 2, "마력+회복", "성스러운 화살로 싸우며 스스로를 치유한다",
            Skill.of("홀리 애로우", 20, 14).magic(2.1).heal(0.1).fx(Skill.Fx.MAGIC, 0xFFD86A)
                    .desc("성스러운 화살을 쏘고 HP 10% 회복 (마력 210%)")),
    PRIEST("프리스트", "마법사", 3, "마력+회복", "빛의 힘으로 적을 태우고 상처를 아물게 한다",
            Skill.of("샤이닝 레이", 50, 24).magic(1.45).hits(2).heal(0.15).fx(Skill.Fx.SMASH, 0xFFF0B0)
                    .desc("빛줄기가 두 번 내리친 뒤 HP 15% 회복 (마력 145% x2)")),
    BISHOP("비숍", "마법사", 4, "마력+회복", "하늘의 심판을 내리는 대성직자",
            Skill.of("제네시스", 70, 40).magic(3.8).heal(0.25).fx(Skill.Fx.LIGHTNING, 0xFFCC4A)
                    .desc("하늘의 빛이 내리꽂힌 뒤 HP 25% 회복 (마력 380%)")),

    // Archers are glass cannons: their skills scale only with ATK (and SPD), never HP or DEF, so they
    // get about 30% higher totals than the warrior skill of the same tier to make up for staying fragile.

    // ── 궁수: 공격력 (활, 여러 발을 쏘는 연사) ──
    // ARCHER keeps its enum name so saves that already advanced to "궁수" still load.
    ARCHER("궁수", "궁수", 1, "공격력", "화살을 연달아 쏘아 몰아붙이는 궁수",
            Skill.of("더블 샷", 10, 6).atk(1.45).hits(2).fx(Skill.Fx.ARROW, 0x6AD0E0)
                    .desc("화살 두 발을 연달아 쏜다 (공격력 145% x2)")),
    HUNTER("헌터", "궁수", 2, "공격력", "세 발씩 쏘아 붙이는 사냥꾼",
            Skill.of("트리플 애로우", 20, 12).atk(1.4).hits(3).fx(Skill.Fx.ARROW, 0x9EE06A)
                    .desc("화살 세 발을 연달아 쏜다 (공격력 140% x3)")),
    RANGER("레인저", "궁수", 3, "공격력", "화살비를 퍼붓는 숙련된 궁수",
            Skill.of("애로우 레인", 50, 20).atk(1.5).hits(4).fx(Skill.Fx.ARROW, 0xFFD24A)
                    .desc("화살비를 네 번 퍼붓는다 (공격력 150% x4)")),
    BOWMASTER("보우마스터", "궁수", 4, "공격력", "폭풍처럼 쏟아지는 화살의 대가",
            Skill.of("폭풍의 시", 70, 30).atk(1.65).hits(5).fx(Skill.Fx.ARROW, 0x4AD8C8)
                    .desc("화살을 폭풍처럼 다섯 번 쏟아낸다 (공격력 165% x5)")),

    // ── 궁수: 공격력 + 치명타 (석궁, 반드시 급소를 맞히는 저격) ──
    CROSSBOWMAN("석궁수", "궁수", 1, "공격력+치명타", "정확한 한 발로 급소를 노리는 궁수",
            Skill.of("크리티컬 샷", 10, 6).atk(1.6).alwaysCrit().fx(Skill.Fx.ARROW, 0xFF9A3A)
                    .desc("반드시 치명타가 나는 사격 (공격력 160%)")),
    SHARPSHOOTER("사수", "궁수", 2, "공격력+치명타", "묵직한 쇠화살로 급소를 꿰뚫는 사수",
            Skill.of("아이언 애로우", 20, 12).atk(2.3).alwaysCrit().fx(Skill.Fx.ARROW, 0xC0C8D8)
                    .desc("반드시 치명타가 나는 쇠화살 (공격력 230%)")),
    SNIPER("저격수", "궁수", 3, "공격력+치명타", "멀리서 숨을 고르고 한 발로 끝낸다",
            Skill.of("스나이핑", 50, 20).atk(3.25).alwaysCrit().fx(Skill.Fx.ARROW, 0xFF4A3A)
                    .desc("반드시 치명타가 나는 저격 (공격력 325%)")),
    MARKSMAN("신궁", "궁수", 4, "공격력+치명타", "어떤 갑옷도 꿰뚫는 화살의 신",
            Skill.of("피어싱", 70, 30).atk(4.2).alwaysCrit().ignoreDefense().fx(Skill.Fx.ARROW, 0xE0283C)
                    .desc("방어를 무시하고 반드시 치명타가 나는 관통 사격 (공격력 420%)")),

    // ── 궁수: 공격력 + 속도 (바람, 빠를수록 강해지는 궁수) ──
    // Speed ratio is always 2x the ATK ratio (see Skill.spd), so ATK and SPD points are worth the same;
    // totals sit ~5% under the bow route because speed also wins first strike.
    WIND_HUNTER("바람 사냥꾼", "궁수", 1, "공격력+속도", "바람을 타고 누구보다 먼저 쏜다",
            Skill.of("애로우 블로우", 10, 6).atk(3).spd(6).fx(Skill.Fx.ARROW, 0x9EE06A)
                    .desc("바람을 실은 화살 (공격력 300% + 속도 600%)")),
    WIND_ARCHER("윈드 아처", "궁수", 2, "공격력+속도", "바람의 화살을 연달아 날린다",
            Skill.of("윈드 샷", 20, 12).atk(2.1).spd(4.2).hits(2).fx(Skill.Fx.ARROW, 0x7AE0B0)
                    .desc("바람 화살을 두 번 쏜다 (공격력 210% + 속도 420%) x2")),
    STORM_SHOOTER("스톰 슈터", "궁수", 3, "공격력+속도", "폭풍을 화살에 담아 쏘는 궁수",
            Skill.of("스톰 애로우", 50, 20).atk(2.05).spd(4.1).hits(3).fx(Skill.Fx.LIGHTNING, 0x4AD8C8)
                    .desc("폭풍 화살이 세 번 내리친다 (공격력 205% + 속도 410%) x3")),
    WINDBREAKER("윈드브레이커", "궁수", 4, "공격력+속도", "바람 그 자체가 된 궁수",
            Skill.of("천공의 노래", 70, 30).atk(2.1).spd(4.2).hits(4).fx(Skill.Fx.ARROW, 0x6FC3FF)
                    .desc("하늘을 가르는 화살이 네 번 몰아친다 (공격력 210% + 속도 420%) x4")),

    // Thieves sit between warriors and archers: throwing stars and dual blades run ~10% above the warrior
    // skill of the same tier, daggers a little below it because every hit also drains HP.

    // ── 도적: 공격력 + 행운 (표창, 행운이 위력이 되는 원거리 연타) ──
    // Luck ratio is always 2x the ATK ratio (see Skill.luk), so ATK and LUK points are worth the same.
    // THIEF keeps its enum name so saves that already advanced to "도적" still load.
    THIEF("도적", "도적", 1, "공격력+행운", "행운을 실어 표창을 던지는 도적",
            Skill.of("럭키 세븐", 10, 6).atk(1.1).luk(2.2).hits(2).fx(Skill.Fx.SHURIKEN, 0xC8D0E0)
                    .desc("표창 두 개를 던진다 (공격력 110% + 행운 220%) x2")),
    ASSASSIN("어쌔신", "도적", 2, "공격력+행운", "표창을 세 개씩 날리는 암살자",
            Skill.of("트리플 스로우", 20, 12).atk(1.05).luk(2.1).hits(3).fx(Skill.Fx.SHURIKEN, 0x7AE0B0)
                    .desc("표창 세 개를 던진다 (공격력 105% + 행운 210%) x3")),
    HERMIT("허밋", "도적", 3, "공격력+행운", "거대한 표창 한 방으로 적을 꿰뚫는 은둔자",
            Skill.of("어벤져", 50, 20).atk(4.4).luk(8.8).fx(Skill.Fx.SHURIKEN, 0xB050FF)
                    .desc("거대한 표창을 던진다 (공격력 440% + 행운 880%)")),
    NIGHT_LORD("나이트로드", "도적", 4, "공격력+행운", "밤을 지배하는 표창의 군주",
            Skill.of("쿼드러플 스로우", 70, 30).atk(1.55).luk(3.1).hits(4).fx(Skill.Fx.SHURIKEN, 0xFFD24A)
                    .desc("표창 네 개를 던진다 (공격력 155% + 행운 310%) x4")),

    // ── 도적: 공격력 + 흡혈 (단검, 베는 만큼 HP를 빼앗는 근접형) ──
    DAGGER_THIEF("단검 도적", "도적", 1, "공격력+흡혈", "그림자 속에서 급소를 노리는 단검 도적",
            Skill.of("어쌔신", 10, 6).atk(1.35).alwaysCrit().drain(0.15).fx(Skill.Fx.SLASH, 0xB050FF)
                    .desc("반드시 치명타가 나는 기습, 피해의 15% 흡혈 (공격력 135%)")),
    BANDIT("시프", "도적", 2, "공격력+흡혈", "단검을 연달아 휘두르며 생명력을 훔친다",
            Skill.of("새비지 블로우", 20, 12).atk(1.15).hits(3).drain(0.1).fx(Skill.Fx.SLASH, 0xD06AFF)
                    .desc("단검으로 세 번 벤다, 피해의 10% 흡혈 (공격력 115% x3)")),
    CHIEF_BANDIT("시프마스터", "도적", 3, "공격력+흡혈", "부메랑처럼 파고드는 단검의 달인",
            Skill.of("부메랑 스텝", 50, 20).atk(2.4).hits(2).drain(0.15).fx(Skill.Fx.THRUST, 0x9B4DFF)
                    .desc("두 번 파고들어 찌른다, 피해의 15% 흡혈 (공격력 240% x2)")),
    SHADOWER("섀도어", "도적", 4, "공격력+흡혈", "그림자와 하나 되어 숨통을 끊는다",
            Skill.of("어쌔시네이트", 70, 30).atk(3.95).alwaysCrit().drain(0.2).fx(Skill.Fx.SLASH, 0xE0283C)
                    .desc("반드시 치명타가 나는 암살, 피해의 20% 흡혈 (공격력 395%)")),

    // ── 도적: 공격력 (이도, 두 자루 칼로 가장 많이 베는 난도질) ──
    SEMI_DUALER("세미듀어러", "도적", 1, "공격력", "양손에 단검을 든 견습 이도류",
            Skill.of("더블 스탭", 10, 6).atk(1.25).hits(2).fx(Skill.Fx.SLASH, 0x6FC3FF)
                    .desc("두 자루로 연달아 벤다 (공격력 125% x2)")),
    DUALER("듀어러", "도적", 2, "공격력", "칼날의 폭풍을 일으키는 이도류",
            Skill.of("블레이드 퓨리", 20, 12).atk(0.95).hits(4).fx(Skill.Fx.SLASH, 0x7AE0B0)
                    .desc("네 번 휘몰아친다 (공격력 95% x4)")),
    DUAL_MASTER("듀얼마스터", "도적", 3, "공격력", "쉬지 않는 칼날로 적을 갈아낸다",
            Skill.of("블레이드 스톰", 50, 20).atk(1.05).hits(5).fx(Skill.Fx.SLASH, 0xFFD24A)
                    .desc("칼날 폭풍이 다섯 번 몰아친다 (공격력 105% x5)")),
    DUAL_BLADE("듀얼블레이드", "도적", 4, "공격력", "여섯 갈래 칼날의 아수라",
            Skill.of("아수라", 70, 30).atk(1.25).hits(6).fx(Skill.Fx.SLASH, 0xFF4A6A)
                    .desc("아수라처럼 여섯 번 베어 낸다 (공격력 125% x6)")),

    // ── 다른 직업군: 1차만 임시로 있음 (루트/차수는 설계 후 추가) ──
    PIRATE("해적", "해적", 1, "", "주먹과 총을 오가는 거친 싸움꾼",
            Skill.of("더블 파이어", 10, 6).atk(1.0).hits(2).fx(Skill.Fx.SHOT, 0xFFB040).desc("총을 두 번 쏜다 (100% x2)"),
            Skill.of("서머솔트 킥", 15, 10).atk(2.0).fx(Skill.Fx.SMASH, 0x5AB0FF).desc("공중제비 발차기 (공격력 200%)"),
            Skill.of("코크스크류 블로우", 20, 15).atk(3.2).hpCost(0.1).fx(Skill.Fx.SMASH, 0xFF8A3A).desc("HP 10%를 바쳐 회전 펀치 (320%)"));

    /** Level needed for each advancement tier (index = tier; 0 = beginner). */
    private static final int[] TIER_LEVELS = {1, 10, 20, 50, 70};
    public static final int MAX_TIER = 4;

    private final String displayName;
    private final String jobClass;
    private final int tier;
    private final String focus;
    private final String description;
    private final List<Skill> skills;

    Job(String displayName, String jobClass, int tier, String focus, String description, Skill... skills) {
        this.displayName = displayName;
        this.jobClass = jobClass;
        this.tier = tier;
        this.focus = focus;
        this.description = description;
        this.skills = Collections.unmodifiableList(Arrays.asList(skills));
    }

    public String getDisplayName() { return displayName; }
    /** Class group such as "전사" (empty for the beginner). */
    public String getJobClass() { return jobClass; }
    public int getTier() { return tier; }
    /** Which stats this route's skills grow with, e.g. "방어력+체력". */
    public String getFocus() { return focus; }
    public String getDescription() { return description; }
    public int getRequiredLevel() { return levelForTier(tier); }
    public List<Skill> getSkills() { return skills; }

    public static int levelForTier(int tier) {
        return TIER_LEVELS[Math.max(0, Math.min(tier, TIER_LEVELS.length - 1))];
    }

    /** All jobs of one tier, from every class. */
    public static List<Job> ofTier(int tier) {
        List<Job> result = new ArrayList<>();
        for (Job j : values()) {
            if (j.tier == tier) result.add(j);
        }
        return result;
    }
}
