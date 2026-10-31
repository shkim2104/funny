package game.model;

/** What monster skills have done to the player in the current fight. Lasts one fight only. */
public final class FightStatus {
    private int poisonRounds;
    private double poisonShare;
    private int breakRounds;
    private double defenseCut;
    private boolean stunned;
    /** A stun can't land again right after one, so nobody loses turn after turn. */
    private int stunShield;

    /** The after-effects of a skill that landed a hit. */
    public void apply(MonsterSkill skill) {
        switch (skill.getKind()) {
            case POISON:
                poisonRounds = MonsterSkill.STATUS_ROUNDS;
                poisonShare = skill.getPower();
                break;
            case ARMOR_BREAK:
                breakRounds = MonsterSkill.STATUS_ROUNDS;
                defenseCut = skill.getPower();
                break;
            case STUN:
                if (stunShield == 0) {
                    stunned = true;
                    stunShield = 3;
                }
                break;
            default:
        }
    }

    /** Defense multiplier while armor is broken. */
    public double defenseMul() {
        return breakRounds > 0 ? 1 - defenseCut : 1;
    }

    /** End of a round: counts down, and returns the HP poison takes now (it never kills: at least 1 HP stays). */
    public int endRound(Player player) {
        if (breakRounds > 0) breakRounds--;
        if (stunShield > 0) stunShield--;
        if (poisonRounds <= 0) return 0;
        poisonRounds--;
        int dmg = Math.min(player.getHp() - 1, (int) Math.round(player.getMaxHp() * poisonShare));
        dmg = Math.max(0, dmg);
        player.takeDamage(dmg);
        return dmg;
    }

    /** True once if the player must lose this turn. */
    public boolean consumeStun() {
        boolean was = stunned;
        stunned = false;
        return was;
    }
}
