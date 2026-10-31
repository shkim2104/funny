package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A duel between two players' own characters, with no screen or network in it. Both sides pick an
 * action in secret; resolve() then plays the round, the faster side first, and returns what happened
 * as a list of events. Only the host runs this; the other side just shows the events, so both screens
 * always agree.
 */
public final class PvpBattle {
    /** Action codes: a skill's index in the fighter's skill list, or one of these. */
    public static final int ATTACK = -1, POTION = -2;

    /**
     * Characters hit far harder than they can take in dungeons, so a duel has its own, larger HP:
     * max HP x HP_MUL plus HP_PER_LEVEL per level. The flat part keeps HP builds from doubling their lead.
     */
    public static final int HP_MUL = 2, HP_PER_LEVEL = 20;
    public static final int POTIONS = 3;
    /** A potion restores this share of duel HP. */
    private static final double POTION_HEAL = 0.25;
    private static final double CRIT_DAMAGE = 1.75;
    /** One action can take at most this share of the target's duel HP, so nobody is erased in a turn. */
    private static final double ACTION_DAMAGE_CAP = 0.35;
    /** "Ignores defense" skills ignore this share of it here: full ignore decides a duel by itself. */
    private static final double DEFENSE_IGNORED = 0.5;
    /** Healing skills restore this share of what they do in a dungeon: duel HP is much larger. */
    private static final double SKILL_HEAL_SHARE = 0.35;
    /** HP paid for a skill turns into this share of the shield it gives in a dungeon. */
    private static final double HP_SHIELD_SHARE = 0.5;
    /** MP back each round, so a long duel doesn't become two characters tapping each other. */
    private static final double MP_REGEN = 0.1;
    /** From this round on, all damage grows by SUDDEN_DEATH_STEP per round, so every duel ends. */
    private static final int SUDDEN_DEATH_ROUND = 10;
    private static final double SUDDEN_DEATH_STEP = 0.3;

    /** One thing that happened, with both sides' numbers after it. side is who acted (0 or 1). */
    public static final class Event {
        public final int side;
        public final String text;
        /** Damage to the other side (0 if none), and whether it was a critical hit. */
        public final int damage;
        public final boolean crit;
        public final int[] hp, mp;
        /** True if this is a blow to animate (it may have missed); fx is the skill's effect name, or "". */
        public final boolean hit;
        public final String fx;
        public final int fxColor;

        public Event(int side, String text, int damage, boolean crit, int[] hp, int[] mp, boolean hit, String fx, int fxColor) {
            this.side = side;
            this.text = text;
            this.damage = damage;
            this.crit = crit;
            this.hp = hp;
            this.mp = mp;
            this.hit = hit;
            this.fx = fx;
            this.fxColor = fxColor;
        }
    }

    private static final class Side {
        final Player player;
        final Bonus bonus;
        final int maxHp;
        int hp, mp, shield, potions = POTIONS;
        boolean firstHitDone;

        Side(Player player) {
            this.player = player;
            this.bonus = player.getGearBonus();
            this.maxHp = player.getMaxHp() * HP_MUL + player.getLevel() * HP_PER_LEVEL;
            this.hp = maxHp;
            this.mp = player.getMaxMp();
            this.shield = (int) Math.round(maxHp * bonus.shield());
        }
    }

    private final Side[] sides;
    private final Random rnd;
    private int rounds;
    /** Damage the current action has dealt so far, against ACTION_DAMAGE_CAP. */
    private int dealtThisAction;
    /** The skill being used, for the hit effect shown with each blow. */
    private Skill acting;

    public PvpBattle(Player first, Player second, Random rnd) {
        this.sides = new Side[]{new Side(first), new Side(second)};
        this.rnd = rnd;
    }

    public int getHp(int side) { return sides[side].hp; }
    public int getMaxHp(int side) { return sides[side].maxHp; }
    public int getMp(int side) { return sides[side].mp; }
    public int getPotions(int side) { return sides[side].potions; }
    public int getRounds() { return rounds; }

    public boolean isOver() { return sides[0].hp <= 0 || sides[1].hp <= 0; }

    /** The side that doesn't run the battle copies the host's numbers after each round. */
    public void sync(int[] hp, int[] mp, int[] potions) {
        for (int side = 0; side < 2; side++) {
            sides[side].hp = hp[side];
            sides[side].mp = mp[side];
            sides[side].potions = potions[side];
        }
        rounds++;
    }

    /** The winning side, or -1 while the duel goes on. */
    public int getWinner() {
        if (sides[1].hp <= 0) return 0;
        return sides[0].hp <= 0 ? 1 : -1;
    }

    /** True if this action can be taken now; anything else is played as a plain attack. */
    public boolean isLegal(int side, int action) {
        Side s = sides[side];
        if (action == ATTACK) return true;
        if (action == POTION) return s.potions > 0;
        List<Skill> skills = s.player.getSkills();
        return action >= 0 && action < skills.size() && s.mp >= mpCost(s, skills.get(action));
    }

    private static int mpCost(Side s, Skill skill) {
        return Math.max(0, skill.getMpCost() - s.bonus.mpCostReduce());
    }

    /** Plays one round from both sides' actions. */
    public List<Event> resolve(int firstAction, int secondAction) {
        rounds++;
        List<Event> events = new ArrayList<>();
        int spd0 = sides[0].player.getSpd() + sides[0].bonus.spd(), spd1 = sides[1].player.getSpd() + sides[1].bonus.spd();
        boolean firstGoesFirst = spd0 != spd1 ? spd0 > spd1 : rnd.nextBoolean();
        int lead = firstGoesFirst ? 0 : 1;
        for (int side : new int[]{lead, 1 - lead}) {
            if (isOver()) break;
            act(side, side == 0 ? firstAction : secondAction, events);
        }
        if (!isOver()) {
            for (int side = 0; side < 2; side++) {
                Side s = sides[side];
                s.mp = Math.min(s.player.getMaxMp(), s.mp + (int) Math.round(s.player.getMaxMp() * MP_REGEN));
                int regen = (int) Math.round(s.maxHp * s.bonus.regen());
                if (regen > 0 && s.hp < s.maxHp) {
                    s.hp = Math.min(s.maxHp, s.hp + regen);
                    events.add(event(side, s.player.getName() + "의 HP가 " + regen + " 회복됐다.", 0, false));
                }
            }
        }
        return events;
    }

    private void act(int side, int action, List<Event> events) {
        Side me = sides[side], foe = sides[1 - side];
        String name = me.player.getName();
        dealtThisAction = 0;
        acting = null;
        if (!isLegal(side, action)) action = ATTACK;
        if (action == POTION) {
            me.potions--;
            int heal = (int) Math.round(me.maxHp * POTION_HEAL);
            me.hp = Math.min(me.maxHp, me.hp + heal);
            events.add(event(side, name + "은(는) 포션을 마셨다! (HP +" + heal + ")", 0, false));
            return;
        }
        if (action == ATTACK) {
            strike(side, me.player.getAtk(), false, false, false, 0, name + "의 공격!", events);
            if (!isOver() && rnd.nextDouble() < me.bonus.doubleStrike()) {
                strike(side, me.player.getAtk(), false, false, false, 0, name + "의 연속 공격!", events);
            }
            return;
        }
        Skill skill = me.player.getSkills().get(action);
        acting = skill;
        me.mp -= mpCost(me, skill);
        if (skill.getHpCostRatio() > 0) {
            int cost = Math.min(me.hp - 1, (int) Math.round(me.maxHp * skill.getHpCostRatio()));
            me.hp -= cost;
            int cap = (int) Math.round(me.maxHp * Skill.MAX_HP_SHIELD);
            me.shield = Math.min(cap, me.shield + (int) Math.round(cost * skill.getHpCostShield() * HP_SHIELD_SHARE));
            events.add(event(side, name + "은(는) HP " + cost + "을(를) 바쳤다!", 0, false));
        }
        if (skill.isAttack()) {
            for (int i = 0; i < skill.getHits() && foe.hp > 0; i++) {
                strike(side, skill.powerFor(me.player), true, skill.isIgnoreDefense(), skill.isAlwaysCrit(), skill.getDrainRatio(),
                        name + "의 " + skill.getName() + "!", events);
            }
        }
        if (skill.getHealRatio() > 0 && !isOver()) {
            int heal = (int) Math.round(me.maxHp * skill.getHealRatio() * SKILL_HEAL_SHARE);
            me.hp = Math.min(me.maxHp, me.hp + heal);
            events.add(event(side, name + "의 " + skill.getName() + "! (HP +" + heal + ")", 0, false));
        }
    }

    /** One hit, by BattlePanel's rules. */
    private void strike(int side, int power, boolean isSkill, boolean ignoreDefense, boolean forceCrit, double drain,
                        String text, List<Event> events) {
        Side me = sides[side], foe = sides[1 - side];
        if (rnd.nextDouble() < foe.bonus.dodge()) {
            events.add(blow(side, text + " 빗나갔다!", 0, false));
            return;
        }
        int def = (int) Math.round(foe.player.getDef() * (1 + foe.bonus.defPct()));
        def = (int) Math.round(def * (1 - me.bonus.ignoreDef()) * (ignoreDefense ? 1 - DEFENSE_IGNORED : 1));
        int boosted = (int) Math.round(power * (1 + me.bonus.atkPct()) * (isSkill ? 1 + me.bonus.skillDmg() : 1));
        int dmg = Math.max(1, Math.round((float) (boosted * 50.0 / (def + 50.0))) + rnd.nextInt(5) - 2);
        double mul = 1;
        if (!me.firstHitDone) {
            mul *= 1 + me.bonus.firstStrike();
            me.firstHitDone = true;
        }
        if (foe.hp * 10 <= foe.maxHp * 3) mul *= 1 + me.bonus.execute();
        if (me.hp * 10 <= me.maxHp * 3) mul *= 1 + me.bonus.lowHpDmg();
        boolean crit = forceCrit || rnd.nextInt(100) < me.player.getCritChance() + me.bonus.critChance();
        if (crit) mul *= CRIT_DAMAGE * (1 + me.bonus.critDmg());
        if (rounds > SUDDEN_DEATH_ROUND) mul *= 1 + (rounds - SUDDEN_DEATH_ROUND) * SUDDEN_DEATH_STEP;
        int total = Math.max(1, (int) Math.round(dmg * mul * (1 - foe.bonus.dmgReduction())));
        int capLeft = (int) Math.round(foe.maxHp * ACTION_DAMAGE_CAP) - dealtThisAction;
        total = Math.max(1, Math.min(total, capLeft));
        dealtThisAction += total;

        int absorbed = Math.min(foe.shield, total);
        foe.shield -= absorbed;
        int taken = total - absorbed;
        foe.hp = Math.max(0, foe.hp - taken);
        double steal = drain + me.bonus.lifesteal();
        if (steal > 0) me.hp = Math.min(me.maxHp, me.hp + (int) Math.round(taken * steal));
        int reflected = (int) Math.round(total * foe.bonus.thorns());
        if (reflected > 0) me.hp = Math.max(1, me.hp - reflected);
        events.add(blow(side, text, taken, crit));
    }

    private Event event(int side, String text, int damage, boolean crit) {
        return new Event(side, text, damage, crit, new int[]{sides[0].hp, sides[1].hp}, new int[]{sides[0].mp, sides[1].mp}, false, "", 0);
    }

    private Event blow(int side, String text, int damage, boolean crit) {
        boolean fx = acting != null && acting.getFx() != Skill.Fx.NONE;
        return new Event(side, text, damage, crit, new int[]{sides[0].hp, sides[1].hp}, new int[]{sides[0].mp, sides[1].mp}, true,
                fx ? acting.getFx().name() : "", fx ? acting.getFxColor() : 0);
    }
}
