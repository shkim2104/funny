package game.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A bundle of battle effects, shared by run augments and boss gear so the battle code reads one total.
 * Percentages are fractions (0.1 = 10%) except critChance, which is in percentage points like
 * Player.getCritChance(). Build with the fluent setters; combine with plus().
 */
public final class Bonus {
    public static final Bonus NONE = new Bonus();

    double atkPct, defPct, dmgReduction, skillDmg, critChance, critDmg, ignoreDef, firstStrike, execute,
            bossDmg, lowHpDmg, stackAtkPerKill, doubleStrike, lifesteal, thorns, dodge, shield, regen,
            hpOnKill, healAfterFight, goldPct, expPct, revive;
    int spd, mpOnKill, mpCostReduce;
    boolean alwaysFirst;

    public static Bonus of() {
        return new Bonus();
    }

    public Bonus atkPct(double v) { atkPct = v; return this; }
    public Bonus defPct(double v) { defPct = v; return this; }
    /** Fraction of incoming damage removed; negative means taking more. */
    public Bonus dmgReduction(double v) { dmgReduction = v; return this; }
    public Bonus skillDmg(double v) { skillDmg = v; return this; }
    public Bonus critChance(double points) { critChance = points; return this; }
    /** Added to the 1.75x crit multiplier. */
    public Bonus critDmg(double v) { critDmg = v; return this; }
    public Bonus ignoreDef(double v) { ignoreDef = v; return this; }
    /** Extra damage on the first hit of each fight. */
    public Bonus firstStrike(double v) { firstStrike = v; return this; }
    /** Extra damage against monsters at or below 30% HP. */
    public Bonus execute(double v) { execute = v; return this; }
    public Bonus bossDmg(double v) { bossDmg = v; return this; }
    /** Extra damage while the player is at or below 30% HP. */
    public Bonus lowHpDmg(double v) { lowHpDmg = v; return this; }
    /** ATK% gained per monster killed in this run. */
    public Bonus stackAtkPerKill(double v) { stackAtkPerKill = v; return this; }
    /** Chance for a basic attack to strike once more. */
    public Bonus doubleStrike(double v) { doubleStrike = v; return this; }
    public Bonus lifesteal(double v) { lifesteal = v; return this; }
    /** Fraction of damage taken dealt back to the attacker. */
    public Bonus thorns(double v) { thorns = v; return this; }
    public Bonus dodge(double v) { dodge = v; return this; }
    /** Barrier at the start of each fight, as a fraction of max HP. */
    public Bonus shield(double v) { shield = v; return this; }
    /** HP restored at the end of every round, as a fraction of max HP. */
    public Bonus regen(double v) { regen = v; return this; }
    public Bonus hpOnKill(double v) { hpOnKill = v; return this; }
    public Bonus healAfterFight(double v) { healAfterFight = v; return this; }
    public Bonus goldPct(double v) { goldPct = v; return this; }
    public Bonus expPct(double v) { expPct = v; return this; }
    /** Once per run, a lethal hit leaves the player at this fraction of max HP instead. */
    public Bonus revive(double v) { revive = v; return this; }
    public Bonus spd(int v) { spd = v; return this; }
    public Bonus mpOnKill(int v) { mpOnKill = v; return this; }
    public Bonus mpCostReduce(int v) { mpCostReduce = v; return this; }
    public Bonus alwaysFirst() { alwaysFirst = true; return this; }

    /**
     * Both bonuses combined. Damage-up effects multiply (+20% and +20% = +44%), so leaning into one kind of
     * augment compounds like a real build. Damage reduction, dodge and defense ignore multiply what's left
     * (25% and 15% reduction = 1 - 0.75 x 0.85 = 36%), so they never reach 100%. Chances, lifesteal,
     * heals and flat numbers simply add. Revive keeps the stronger one: it only triggers once anyway.
     */
    public Bonus plus(Bonus o) {
        Bonus b = new Bonus();
        b.atkPct = mul(atkPct, o.atkPct);
        b.defPct = mul(defPct, o.defPct);
        b.dmgReduction = rest(dmgReduction, o.dmgReduction);
        b.skillDmg = mul(skillDmg, o.skillDmg);
        b.critChance = critChance + o.critChance;
        b.critDmg = mul(critDmg, o.critDmg);
        b.ignoreDef = rest(ignoreDef, o.ignoreDef);
        b.firstStrike = mul(firstStrike, o.firstStrike);
        b.execute = mul(execute, o.execute);
        b.bossDmg = mul(bossDmg, o.bossDmg);
        b.lowHpDmg = mul(lowHpDmg, o.lowHpDmg);
        b.stackAtkPerKill = mul(stackAtkPerKill, o.stackAtkPerKill);
        b.doubleStrike = doubleStrike + o.doubleStrike;
        b.lifesteal = lifesteal + o.lifesteal;
        b.thorns = thorns + o.thorns;
        b.dodge = rest(dodge, o.dodge);
        b.shield = shield + o.shield;
        b.regen = regen + o.regen;
        b.hpOnKill = hpOnKill + o.hpOnKill;
        b.healAfterFight = healAfterFight + o.healAfterFight;
        b.goldPct = goldPct + o.goldPct;
        b.expPct = expPct + o.expPct;
        b.revive = Math.max(revive, o.revive);
        b.spd = spd + o.spd;
        b.mpOnKill = mpOnKill + o.mpOnKill;
        b.mpCostReduce = mpCostReduce + o.mpCostReduce;
        b.alwaysFirst = alwaysFirst || o.alwaysFirst;
        return b;
    }

    /** These effects at the given strength: 0.5 halves every number. "항상 먼저 공격" stays as it is. */
    public Bonus scaled(double k) {
        Bonus b = new Bonus();
        b.atkPct = atkPct * k;
        b.defPct = defPct * k;
        b.dmgReduction = dmgReduction * k;
        b.skillDmg = skillDmg * k;
        b.critChance = critChance * k;
        b.critDmg = critDmg * k;
        b.ignoreDef = ignoreDef * k;
        b.firstStrike = firstStrike * k;
        b.execute = execute * k;
        b.bossDmg = bossDmg * k;
        b.lowHpDmg = lowHpDmg * k;
        b.stackAtkPerKill = stackAtkPerKill * k;
        b.doubleStrike = doubleStrike * k;
        b.lifesteal = lifesteal * k;
        b.thorns = thorns * k;
        b.dodge = dodge * k;
        b.shield = shield * k;
        b.regen = regen * k;
        b.hpOnKill = hpOnKill * k;
        b.healAfterFight = healAfterFight * k;
        b.goldPct = goldPct;
        b.expPct = expPct;
        b.revive = revive * k;
        b.spd = (int) Math.round(spd * k);
        b.mpOnKill = (int) Math.round(mpOnKill * k);
        b.mpCostReduce = (int) Math.round(mpCostReduce * k);
        b.alwaysFirst = alwaysFirst;
        return b;
    }

    /** Multiplicative stacking of "+x%" effects: (1 + a)(1 + b) - 1. */
    private static double mul(double a, double b) {
        return (1 + a) * (1 + b) - 1;
    }

    /** Stacking of "-x%" effects on what's left: 1 - (1 - a)(1 - b). A negative value (taking more) works too. */
    private static double rest(double a, double b) {
        return 1 - (1 - a) * (1 - b);
    }

    // ---------- reads used by the battle ----------
    public double atkPct() { return atkPct; }
    public double defPct() { return defPct; }
    public double dmgReduction() { return dmgReduction; }
    public double skillDmg() { return skillDmg; }
    public double critChance() { return critChance; }
    public double critDmg() { return critDmg; }
    public double ignoreDef() { return ignoreDef; }
    public double firstStrike() { return firstStrike; }
    public double execute() { return execute; }
    public double bossDmg() { return bossDmg; }
    public double lowHpDmg() { return lowHpDmg; }
    public double stackAtkPerKill() { return stackAtkPerKill; }
    public double doubleStrike() { return Math.min(0.75, doubleStrike); }
    public double lifesteal() { return lifesteal; }
    public double thorns() { return thorns; }
    public double dodge() { return dodge; }
    public double shield() { return shield; }
    public double regen() { return regen; }
    public double hpOnKill() { return hpOnKill; }
    public double healAfterFight() { return healAfterFight; }
    public double goldPct() { return goldPct; }
    public double expPct() { return expPct; }
    public double revive() { return revive; }
    public int spd() { return spd; }
    public int mpOnKill() { return mpOnKill; }
    public int mpCostReduce() { return mpCostReduce; }
    public boolean isAlwaysFirst() { return alwaysFirst; }

    /** One readable line per effect, e.g. "흡혈 12%", in a fixed order. */
    public List<String> lines() {
        List<String> l = new ArrayList<>();
        pct(l, "공격력", atkPct);
        pct(l, "방어력", defPct);
        if (dmgReduction > 0) l.add("받는 피해 -" + p(dmgReduction));
        if (dmgReduction < 0) l.add("받는 피해 +" + p(-dmgReduction));
        pct(l, "스킬 피해", skillDmg);
        if (critChance > 0) l.add("치명타 확률 +" + Math.round(critChance) + "%");
        pct(l, "치명타 피해", critDmg);
        if (ignoreDef > 0) l.add("적 방어력 " + p(ignoreDef) + " 무시");
        if (firstStrike > 0) l.add("전투 첫 공격 피해 +" + p(firstStrike));
        if (execute > 0) l.add("HP 30% 이하 적에게 피해 +" + p(execute));
        if (bossDmg > 0) l.add("보스에게 피해 +" + p(bossDmg));
        if (lowHpDmg > 0) l.add("내 HP 30% 이하일 때 피해 +" + p(lowHpDmg));
        if (stackAtkPerKill > 0) l.add("처치할 때마다 공격력 +" + p(stackAtkPerKill) + " (누적)");
        if (doubleStrike > 0) l.add("기본 공격 " + p(doubleStrike) + " 확률로 한 번 더");
        if (lifesteal > 0) l.add("흡혈 " + p(lifesteal));
        if (thorns > 0) l.add("받은 피해 " + p(thorns) + " 반사");
        if (dodge > 0) l.add("회피 " + p(dodge));
        if (shield > 0) l.add("전투 시작 시 최대 HP " + p(shield) + " 보호막");
        if (regen > 0) l.add("매 턴 최대 HP " + p(regen) + " 회복");
        if (hpOnKill > 0) l.add("처치 시 최대 HP " + p(hpOnKill) + " 회복");
        if (healAfterFight > 0) l.add("승리 후 최대 HP " + p(healAfterFight) + " 회복");
        pct(l, "골드 획득", goldPct);
        pct(l, "경험치 획득", expPct);
        if (revive > 0) l.add("던전에서 한 번, 쓰러질 때 HP " + p(revive) + "로 부활");
        if (spd > 0) l.add("공격속도 +" + spd);
        if (mpOnKill > 0) l.add("처치 시 MP " + mpOnKill + " 회복");
        if (mpCostReduce > 0) l.add("스킬 MP 소모 -" + mpCostReduce);
        if (alwaysFirst) l.add("항상 먼저 공격");
        return l;
    }

    public boolean isEmpty() {
        return lines().isEmpty();
    }

    private static void pct(List<String> l, String what, double v) {
        if (v > 0) l.add(what + " +" + p(v));
    }

    private static String p(double v) {
        return Math.round(v * 100) + "%";
    }
}
