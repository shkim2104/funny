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
            Skill.of("피어싱 스피어", 10, 6).atk(1.2).hp(0.15).fx(Skill.Fx.THRUST, 0xE8C46A)
                    .desc("창으로 꿰뚫는다 (공격력 120% + 최대 HP 15%)")),
    SPEARMAN("스피어맨", "전사", 2, "공격력+체력", "넘치는 체력을 실은 연속 찌르기",
            Skill.of("스피어 크러셔", 20, 12).atk(0.9).hp(0.12).hits(2).fx(Skill.Fx.THRUST, 0xF0A040)
                    .desc("두 번 찌른다 (공격력 90% + 최대 HP 12%) x2")),
    DRAGON_KNIGHT("드래곤나이트", "전사", 3, "공격력+체력", "용의 피로 싸우는 창기사",
            Skill.of("드래곤 로어", 50, 20).atk(1.8).hp(0.3).hpCost(0.1).fx(Skill.Fx.SMASH, 0xFF5A2A)
                    .desc("HP 10%를 바쳐 포효한다 (공격력 180% + 최대 HP 30%)")),
    DARK_KNIGHT("다크나이트", "전사", 4, "공격력+체력", "생명을 태워 어둠의 힘을 휘두른다",
            Skill.of("다크 임페일", 70, 30).atk(2.4).hp(0.45).hpCost(0.15).fx(Skill.Fx.THRUST, 0x9B4DFF)
                    .desc("HP 15%를 바쳐 꿰뚫는다 (공격력 240% + 최대 HP 45%)")),

    // ── 전사: 오로지 공격력 (우직한 검사) ──
    SWORDSMAN("검사", "전사", 1, "공격력", "공격력 하나만 믿고 베어 넘기는 우직한 전사",
            Skill.of("파워 스트라이크", 10, 6).atk(1.9).fx(Skill.Fx.SLASH, 0xFF7A3C)
                    .desc("있는 힘껏 벤다 (공격력 190%)")),
    FIGHTER("파이터", "전사", 2, "공격력", "쉴 새 없이 몰아치는 검",
            Skill.of("브랜디쉬", 20, 12).atk(1.4).hits(2).fx(Skill.Fx.SLASH, 0x6FC3FF)
                    .desc("두 번 벤다 (공격력 140% x2)")),
    CRUSADER("크루세이더", "전사", 3, "공격력", "기세를 몰아 연속 공격을 퍼붓는 성전사",
            Skill.of("콤보 어택", 50, 20).atk(1.3).hits(3).fx(Skill.Fx.SLASH, 0xFFD24A)
                    .desc("세 번 몰아친다 (공격력 130% x3)")),
    HERO("히어로", "전사", 4, "공격력", "모든 것을 건 일격으로 전장을 끝낸다",
            Skill.of("레이징 블로우", 70, 30).atk(3.2).alwaysCrit().fx(Skill.Fx.SLASH, 0xFF2E3E)
                    .desc("반드시 치명타가 나는 필살의 일격 (공격력 320%)")),

    // ── 전사: 방어력 + 체력 (단단한 기사) ──
    SQUIRE("견습 기사", "전사", 1, "방어력+체력", "방패를 무기로 삼는 단단한 전사",
            Skill.of("실드 배시", 10, 6).atk(0.5).def(2.5).hp(0.1).fx(Skill.Fx.SMASH, 0x8FB4E0)
                    .desc("방패로 들이받는다 (공격력 50% + 방어력 250% + 최대 HP 10%)")),
    PAGE("페이지", "전사", 2, "방어력+체력", "막아내며 버티는 수호 기사",
            Skill.of("가드 크러쉬", 20, 12).def(3.0).hp(0.15).heal(0.1).fx(Skill.Fx.SMASH, 0x7FD0C8)
                    .desc("방패로 짓누르고 HP 10% 회복 (방어력 300% + 최대 HP 15%)")),
    KNIGHT("나이트", "전사", 3, "방어력+체력", "성스러운 돌격으로 방어를 꿰뚫는 기사",
            Skill.of("홀리 차지", 50, 20).def(3.5).hp(0.25).ignoreDefense().fx(Skill.Fx.THRUST, 0xFFE27A)
                    .desc("방어를 무시하는 돌격 (방어력 350% + 최대 HP 25%)")),
    PALADIN("팔라딘", "전사", 4, "방어력+체력", "쓰러지지 않는 성기사",
            Skill.of("블래스트", 70, 30).def(4.5).hp(0.35).heal(0.2).fx(Skill.Fx.SMASH, 0xFFF0A0)
                    .desc("성스러운 일격 후 HP 20% 회복 (방어력 450% + 최대 HP 35%)")),

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
            Skill.of("썬더 볼트", 10, 8).magic(1.5).ignoreDefense().fx(Skill.Fx.LIGHTNING, 0xFFE84A)
                    .desc("방어를 무시하는 번개 (마력 150%)")),
    IL_WIZARD("위자드(썬,콜)", "마법사", 2, "마력+방어무시", "얼음과 번개를 오가는 위자드",
            Skill.of("콜드 빔", 20, 15).magic(1.2).hits(2).ignoreDefense().fx(Skill.Fx.MAGIC, 0x7CC8FF)
                    .desc("냉기 광선을 두 번 쏜다 (마력 120% x2, 방어 무시)")),
    IL_MAGE("메이지(썬,콜)", "마법사", 3, "마력+방어무시", "번개 창을 연달아 내리꽂는 메이지",
            Skill.of("썬더 스피어", 50, 26).magic(1.3).hits(3).ignoreDefense().fx(Skill.Fx.LIGHTNING, 0xFFE04A)
                    .desc("번개 창을 세 번 내리꽂는다 (마력 130% x3, 방어 무시)")),
    IL_ARCHMAGE("아크메이지(썬,콜)", "마법사", 4, "마력+방어무시", "끝없이 이어지는 번개를 부르는 대마법사",
            Skill.of("체인 라이트닝", 70, 38).magic(1.4).hits(4).ignoreDefense().fx(Skill.Fx.LIGHTNING, 0xBFE0FF)
                    .desc("연쇄 번개가 네 번 몰아친다 (마력 140% x4, 방어 무시)")),

    // ── 마법사: 마력 + 회복 (사제, 싸우면서 버티는 성직자) ──
    ACOLYTE("복사", "마법사", 1, "마력+회복", "치유의 기도를 배우는 견습 성직자",
            // Pure heal: no hit effect; the color only tints its button (the heal itself plays a recovery effect).
            Skill.of("힐", 10, 10).heal(0.3).fx(Skill.Fx.NONE, 0x7CF09A)
                    .desc("최대 HP의 30%를 회복한다")),
    CLERIC("클레릭", "마법사", 2, "마력+회복", "성스러운 화살로 싸우며 스스로를 치유한다",
            Skill.of("홀리 애로우", 20, 14).magic(2.0).heal(0.1).fx(Skill.Fx.MAGIC, 0xFFD86A)
                    .desc("성스러운 화살을 쏘고 HP 10% 회복 (마력 200%)")),
    PRIEST("프리스트", "마법사", 3, "마력+회복", "빛의 힘으로 적을 태우고 상처를 아물게 한다",
            Skill.of("샤이닝 레이", 50, 24).magic(1.4).hits(2).heal(0.15).fx(Skill.Fx.SMASH, 0xFFF0B0)
                    .desc("빛줄기가 두 번 내리친 뒤 HP 15% 회복 (마력 140% x2)")),
    BISHOP("비숍", "마법사", 4, "마력+회복", "하늘의 심판을 내리는 대성직자",
            Skill.of("제네시스", 70, 40).magic(3.6).heal(0.25).fx(Skill.Fx.LIGHTNING, 0xFFCC4A)
                    .desc("하늘의 빛이 내리꽂힌 뒤 HP 25% 회복 (마력 360%)")),

    // ── 다른 직업군: 1차만 임시로 있음 (루트/차수는 설계 후 추가) ──
    ARCHER("궁수", "궁수", 1, "", "정확한 사격으로 급소를 노린다",
            Skill.of("애로우 블로우", 10, 6).atk(1.7).fx(Skill.Fx.ARROW, 0x9EE06A).desc("힘껏 당긴 화살 (공격력 170%)"),
            Skill.of("더블 샷", 15, 10).atk(1.15).hits(2).fx(Skill.Fx.ARROW, 0x6AD0E0).desc("화살 두 발을 연달아 쏜다 (115% x2)"),
            Skill.of("크리티컬 샷", 20, 14).atk(1.6).alwaysCrit().fx(Skill.Fx.ARROW, 0xFF6A4A).desc("반드시 치명타가 나는 사격 (160%)")),
    THIEF("도적", "도적", 1, "", "빠른 손놀림으로 몰아치는 연타",
            Skill.of("럭키 세븐", 10, 6).atk(1.0).hits(2).fx(Skill.Fx.SHURIKEN, 0xC8D0E0).desc("표창 두 개를 던진다 (100% x2)"),
            Skill.of("트리플 스로우", 15, 11).atk(0.85).hits(3).fx(Skill.Fx.SHURIKEN, 0x7AE0B0).desc("표창 세 개를 던진다 (85% x3)"),
            Skill.of("어쌔신", 20, 15).atk(1.8).alwaysCrit().fx(Skill.Fx.SLASH, 0xB050FF).desc("그림자에서 급소를 찌른다 (반드시 치명타, 180%)")),
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
