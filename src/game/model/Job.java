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
            Skill.of("피어싱 스피어", 10, 6).atk(1.2).hp(0.15)
                    .desc("창으로 꿰뚫는다 (공격력 120% + 최대 HP 15%)")),
    SPEARMAN("스피어맨", "전사", 2, "공격력+체력", "넘치는 체력을 실은 연속 찌르기",
            Skill.of("스피어 크러셔", 20, 12).atk(0.9).hp(0.12).hits(2)
                    .desc("두 번 찌른다 (공격력 90% + 최대 HP 12%) x2")),
    DRAGON_KNIGHT("드래곤나이트", "전사", 3, "공격력+체력", "용의 피로 싸우는 창기사",
            Skill.of("드래곤 로어", 50, 20).atk(1.8).hp(0.3).hpCost(0.1)
                    .desc("HP 10%를 바쳐 포효한다 (공격력 180% + 최대 HP 30%)")),
    DARK_KNIGHT("다크나이트", "전사", 4, "공격력+체력", "생명을 태워 어둠의 힘을 휘두른다",
            Skill.of("다크 임페일", 70, 30).atk(2.4).hp(0.45).hpCost(0.15)
                    .desc("HP 15%를 바쳐 꿰뚫는다 (공격력 240% + 최대 HP 45%)")),

    // ── 전사: 오로지 공격력 (우직한 검사) ──
    SWORDSMAN("검사", "전사", 1, "공격력", "공격력 하나만 믿고 베어 넘기는 우직한 전사",
            Skill.of("파워 스트라이크", 10, 6).atk(1.9)
                    .desc("있는 힘껏 벤다 (공격력 190%)")),
    FIGHTER("파이터", "전사", 2, "공격력", "쉴 새 없이 몰아치는 검",
            Skill.of("브랜디쉬", 20, 12).atk(1.4).hits(2)
                    .desc("두 번 벤다 (공격력 140% x2)")),
    CRUSADER("크루세이더", "전사", 3, "공격력", "기세를 몰아 연속 공격을 퍼붓는 성전사",
            Skill.of("콤보 어택", 50, 20).atk(1.3).hits(3)
                    .desc("세 번 몰아친다 (공격력 130% x3)")),
    HERO("히어로", "전사", 4, "공격력", "모든 것을 건 일격으로 전장을 끝낸다",
            Skill.of("레이징 블로우", 70, 30).atk(3.2).alwaysCrit()
                    .desc("반드시 치명타가 나는 필살의 일격 (공격력 320%)")),

    // ── 전사: 방어력 + 체력 (단단한 기사) ──
    SQUIRE("견습 기사", "전사", 1, "방어력+체력", "방패를 무기로 삼는 단단한 전사",
            Skill.of("실드 배시", 10, 6).atk(0.5).def(2.5).hp(0.1)
                    .desc("방패로 들이받는다 (공격력 50% + 방어력 250% + 최대 HP 10%)")),
    PAGE("페이지", "전사", 2, "방어력+체력", "막아내며 버티는 수호 기사",
            Skill.of("가드 크러쉬", 20, 12).def(3.0).hp(0.15).heal(0.1)
                    .desc("방패로 짓누르고 HP 10% 회복 (방어력 300% + 최대 HP 15%)")),
    KNIGHT("나이트", "전사", 3, "방어력+체력", "성스러운 돌격으로 방어를 꿰뚫는 기사",
            Skill.of("홀리 차지", 50, 20).def(3.5).hp(0.25).ignoreDefense()
                    .desc("방어를 무시하는 돌격 (방어력 350% + 최대 HP 25%)")),
    PALADIN("팔라딘", "전사", 4, "방어력+체력", "쓰러지지 않는 성기사",
            Skill.of("블래스트", 70, 30).def(4.5).hp(0.35).heal(0.2)
                    .desc("성스러운 일격 후 HP 20% 회복 (방어력 450% + 최대 HP 35%)")),

    // ── 다른 직업군: 1차만 임시로 있음 (루트/차수는 설계 후 추가) ──
    MAGE("마법사", "마법사", 1, "", "MP를 마력으로 바꿔 방어를 꿰뚫는 마법을 쓴다",
            Skill.of("에너지 볼트", 10, 6).magic(1.7).desc("마력 탄환을 쏜다 (마력 170%)"),
            Skill.of("힐", 15, 12).heal(0.35).desc("최대 HP의 35%를 회복한다"),
            Skill.of("썬더 볼트", 20, 16).magic(2.3).ignoreDefense().desc("방어를 무시하는 번개 (마력 230%)")),
    ARCHER("궁수", "궁수", 1, "", "정확한 사격으로 급소를 노린다",
            Skill.of("애로우 블로우", 10, 6).atk(1.7).desc("힘껏 당긴 화살 (공격력 170%)"),
            Skill.of("더블 샷", 15, 10).atk(1.15).hits(2).desc("화살 두 발을 연달아 쏜다 (115% x2)"),
            Skill.of("크리티컬 샷", 20, 14).atk(1.6).alwaysCrit().desc("반드시 치명타가 나는 사격 (160%)")),
    THIEF("도적", "도적", 1, "", "빠른 손놀림으로 몰아치는 연타",
            Skill.of("럭키 세븐", 10, 6).atk(1.0).hits(2).desc("표창 두 개를 던진다 (100% x2)"),
            Skill.of("트리플 스로우", 15, 11).atk(0.85).hits(3).desc("표창 세 개를 던진다 (85% x3)"),
            Skill.of("어쌔신", 20, 15).atk(1.8).alwaysCrit().desc("그림자에서 급소를 찌른다 (반드시 치명타, 180%)")),
    PIRATE("해적", "해적", 1, "", "주먹과 총을 오가는 거친 싸움꾼",
            Skill.of("더블 파이어", 10, 6).atk(1.0).hits(2).desc("총을 두 번 쏜다 (100% x2)"),
            Skill.of("서머솔트 킥", 15, 10).atk(2.0).desc("공중제비 발차기 (공격력 200%)"),
            Skill.of("코크스크류 블로우", 20, 15).atk(3.2).hpCost(0.1).desc("HP 10%를 바쳐 회전 펀치 (320%)"));

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
