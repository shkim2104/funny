package game.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static game.model.Skill.Effect.*;

/**
 * Job tree. Every job names its parent, so advancing means picking one of the current job's
 * children once the player reaches that child's required level. The planned three routes per
 * class become new entries here with the class as their parent (e.g. a 2nd job whose parent is
 * WARRIOR), and their skills stack on top of the parent's.
 */
public enum Job {
    BEGINNER("초보자", null, 1, "아직 길을 정하지 않은 모험가"),

    WARRIOR("전사", BEGINNER, 10, "높은 체력으로 버티며 강력한 일격을 날린다",
            new Skill("파워 스트라이크", 10, 6, PHYSICAL_MULT, 1.8, "적을 강하게 벤다 (공격력 180%)"),
            new Skill("브랜디쉬", 15, 10, DOUBLE_HIT, 1.2, "두 번 연속으로 벤다 (120% x2)"),
            new Skill("레이징 블로우", 20, 14, RECKLESS_MULT, 3.0, "HP 10%를 바쳐 내려찍는다 (300%)")),

    MAGE("마법사", BEGINNER, 10, "MP를 마력으로 바꿔 방어를 꿰뚫는 마법을 쓴다",
            new Skill("에너지 볼트", 10, 6, MAGIC_MULT, 1.7, "마력 탄환을 쏜다 (마력 170%)"),
            new Skill("힐", 15, 12, HEAL_PERCENT, 0.35, "최대 HP의 35%를 회복한다"),
            new Skill("썬더 볼트", 20, 16, MAGIC_MULT_PIERCE, 2.3, "방어를 무시하는 번개 (마력 230%)")),

    ARCHER("궁수", BEGINNER, 10, "정확한 사격으로 급소를 노린다",
            new Skill("애로우 블로우", 10, 6, PHYSICAL_MULT, 1.7, "힘껏 당긴 화살 (공격력 170%)"),
            new Skill("더블 샷", 15, 10, DOUBLE_HIT, 1.15, "화살 두 발을 연달아 쏜다 (115% x2)"),
            new Skill("크리티컬 샷", 20, 14, GUARANTEED_CRIT_MULT, 1.6, "반드시 치명타가 나는 사격 (160%)")),

    THIEF("도적", BEGINNER, 10, "빠른 손놀림으로 몰아치는 연타",
            new Skill("럭키 세븐", 10, 6, DOUBLE_HIT, 1.0, "표창 두 개를 던진다 (100% x2)"),
            new Skill("트리플 스로우", 15, 11, TRIPLE_HIT, 0.85, "표창 세 개를 던진다 (85% x3)"),
            new Skill("어쌔신", 20, 15, GUARANTEED_CRIT_MULT, 1.8, "그림자에서 급소를 찌른다 (반드시 치명타, 180%)")),

    PIRATE("해적", BEGINNER, 10, "주먹과 총을 오가는 거친 싸움꾼",
            new Skill("더블 파이어", 10, 6, DOUBLE_HIT, 1.0, "총을 두 번 쏜다 (100% x2)"),
            new Skill("서머솔트 킥", 15, 10, PHYSICAL_MULT, 2.0, "공중제비 발차기 (공격력 200%)"),
            new Skill("코크스크류 블로우", 20, 15, RECKLESS_MULT, 3.2, "HP 10%를 바쳐 회전 펀치 (320%)"));

    private final String displayName;
    private final Job parent;
    private final int requiredLevel;
    private final String description;
    private final List<Skill> skills;

    Job(String displayName, Job parent, int requiredLevel, String description, Skill... skills) {
        this.displayName = displayName;
        this.parent = parent;
        this.requiredLevel = requiredLevel;
        this.description = description;
        this.skills = Collections.unmodifiableList(Arrays.asList(skills));
    }

    public String getDisplayName() { return displayName; }
    public Job getParent() { return parent; }
    public int getRequiredLevel() { return requiredLevel; }
    public String getDescription() { return description; }
    /** Skills this job itself grants (not including the parent's). */
    public List<Skill> getOwnSkills() { return skills; }

    /** Jobs this one can advance into next. */
    public List<Job> children() {
        List<Job> result = new ArrayList<>();
        for (Job j : values()) {
            if (j.parent == this) result.add(j);
        }
        return result;
    }
}
