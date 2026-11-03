package game.model;

import java.util.Random;

/**
 * How 이계의 존재 fights: it never attacks plainly, it picks one of four cosmic moves each turn.
 * - Above 60% HP it trades 유성우 and 블랙홀, and answers a heavy blow with 사건의 지평선.
 * - At 60% it starts to gather 초신성 every 4th turn; at 25% it awakens once (ATK x1.3) and gathers
 *   every 3rd turn. A 초신성 is always gathered one turn ahead, so the hero can get ready.
 * - It never uses 블랙홀 twice in a row (so the hero is never held turn after turn), nor 지평선 back to back.
 * One brain per fight: Monster.copy() makes a fresh one.
 */
public final class CosmicBrain implements Monster.Brain {
    public static final double PHASE_TWO = 0.6, AWAKEN = 0.25, AWAKEN_ATTACK = 1.3, HEAVY_BLOW = 0.12;

    private final MonsterSkill shower, gravity, charge, nova, horizon;
    private final Random rnd = new Random();
    private MonsterSkill last;
    private boolean charging, awakened;
    private int sinceNova, horizonCooldown;
    private int lastHp = -1;

    public CosmicBrain(MonsterSkill shower, MonsterSkill gravity, MonsterSkill charge, MonsterSkill nova, MonsterSkill horizon) {
        this.shower = shower;
        this.gravity = gravity;
        this.charge = charge;
        this.nova = nova;
        this.horizon = horizon;
    }

    @Override
    public MonsterSkill next(Monster self) {
        double hp = self.getHp() / (double) self.getMaxHp();
        boolean struck = lastHp >= 0 && lastHp - self.getHp() >= self.getMaxHp() * HEAVY_BLOW;
        lastHp = self.getHp();
        if (horizonCooldown > 0) horizonCooldown--;
        sinceNova++;

        if (!awakened && hp <= AWAKEN) {
            awakened = true;
            self.enrage(AWAKEN_ATTACK);
            self.note("이계의 존재가 각성했다! 별빛이 붉게 타오른다!");
        }
        MonsterSkill pick;
        if (charging) {
            charging = false;
            sinceNova = 0;
            pick = nova;
        } else if (hp <= PHASE_TWO && sinceNova >= (awakened ? 3 : 4)) {
            charging = true;
            pick = charge;
        } else if (struck && horizonCooldown == 0 && last != horizon) {
            horizonCooldown = 3;
            pick = horizon;
        } else if (last == gravity || rnd.nextInt(100) < 60) {
            pick = shower;
        } else {
            pick = gravity;
        }
        last = pick;
        return pick;
    }

    @Override
    public Monster.Brain fresh() {
        return new CosmicBrain(shower, gravity, charge, nova, horizon);
    }
}
