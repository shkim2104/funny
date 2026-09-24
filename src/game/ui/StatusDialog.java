package game.ui;

import game.model.Job;
import game.model.Player;
import game.model.Skill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.List;
import java.util.Map;

public class StatusDialog {
    private StatusDialog() {}

    public static void show(JFrame owner, Player player) {
        StringBuilder sb = new StringBuilder();
        sb.append("이름   : ").append(player.getName()).append("\n");
        sb.append("직업   : ").append(player.getJob().getDisplayName());
        if (player.getJobPath().size() > 1) {
            StringBuilder path = new StringBuilder();
            for (Job j : player.getJobPath()) {
                if (path.length() > 0) path.append(" → ");
                path.append(j.getDisplayName());
            }
            sb.append("  (").append(path).append(")");
        }
        sb.append("\n");
        sb.append("레벨   : ").append(player.getLevel())
                .append("  (EXP ").append(player.getExp()).append("/").append(player.getExpToNext()).append(")\n");
        sb.append("HP     : ").append(player.getHp()).append("/").append(player.getMaxHp()).append("\n");
        sb.append("MP     : ").append(player.getMp()).append("/").append(player.getMaxMp()).append("\n");
        sb.append("공격력 : ").append(player.getAtk()).append(" (기본 ").append(player.getBaseAtk()).append(")\n");
        sb.append("방어력 : ").append(player.getDef()).append(" (기본 ").append(player.getBaseDef()).append(")\n");
        sb.append("행운   : ").append(player.getLuck()).append(" (치명타 확률 ").append(player.getCritChance()).append("%)\n");
        sb.append("공격속도: ").append(player.getSpd()).append(" (높을수록 먼저 공격)\n");
        sb.append("스탯P  : ").append(player.getStatPoints()).append(player.getStatPoints() > 0 ? "  (마을에서 '스탯 분배'로 사용하세요)" : "").append("\n");
        sb.append("골드   : ").append(player.getGold()).append("G\n");
        sb.append("무기   : ").append(player.getWeaponName() == null ? "없음" : player.getWeaponName()).append("\n");
        sb.append("방어구 : ").append(player.getArmorName() == null ? "없음" : player.getArmorName()).append("\n\n");
        sb.append("스킬:\n");
        List<Skill> skills = player.getSkills();
        if (skills.isEmpty()) {
            sb.append(player.getJob() == Job.BEGINNER ? "  (Lv.10에 마을 전직소에서 전직하면 배웁니다)\n" : "  (없음)\n");
        } else {
            for (Skill s : skills) {
                sb.append("  - ").append(s.getName()).append(" (MP ").append(s.getMpCost()).append(") ")
                        .append(s.getDescription()).append("\n");
            }
        }
        sb.append("\n");
        sb.append("소지품:\n");

        Map<String, Integer> counts = player.getItemCounts();
        if (counts.isEmpty()) {
            sb.append("  (없음)\n");
        } else {
            for (Map.Entry<String, Integer> e : counts.entrySet()) {
                sb.append("  - ").append(e.getKey()).append(" x").append(e.getValue()).append("\n");
            }
        }

        JTextArea area = new JTextArea(sb.toString());
        area.setEditable(false);
        area.setFont(Theme.MONO_FONT);
        area.setBackground(Theme.SURFACE);
        area.setForeground(Theme.TEXT);
        area.setBorder(new EmptyBorder(12, 12, 12, 12));

        Dialogs.custom(owner, "상태", Theme.scroll(area), 380, 440);
    }
}
