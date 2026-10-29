package game.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * One dungeon run: the augments picked so far, kills (for stacking augments), the once-per-run revive
 * and rerolls left. Created on entering a dungeon and dropped on leaving, so augments never carry over.
 */
public final class RunState {
    public static final int OFFER_SIZE = 3;
    public static final int REROLLS_PER_RUN = 2;

    private final int dungeonIndex;
    private final List<Augment> augments = new ArrayList<>();
    private int kills;
    private boolean reviveUsed;
    private int rerollsLeft = REROLLS_PER_RUN;

    public RunState(int dungeonIndex) {
        this.dungeonIndex = dungeonIndex;
    }

    public List<Augment> getAugments() { return Collections.unmodifiableList(augments); }
    public int getKills() { return kills; }
    public void addKill() { kills++; }
    public boolean isReviveUsed() { return reviveUsed; }
    public void useRevive() { reviveUsed = true; }
    public int getRerollsLeft() { return rerollsLeft; }

    public boolean useReroll() {
        if (rerollsLeft <= 0) return false;
        rerollsLeft--;
        return true;
    }

    public void take(Augment a) {
        augments.add(a);
    }

    /** Every augment's bonus added up. */
    public Bonus total() {
        Bonus b = Bonus.NONE;
        for (Augment a : augments) b = b.plus(a.getBonus());
        return b;
    }

    /**
     * Three cards of one tier, like 롤토체스: mostly silver, sometimes gold, rarely prism, with later
     * dungeons leaning higher. Augments already taken are not offered again.
     */
    public List<Augment> offer(Random rnd) {
        int prism = 8 + dungeonIndex * 2, gold = 32 + dungeonIndex * 3;
        int roll = rnd.nextInt(100);
        Augment.Tier tier = roll < prism ? Augment.Tier.PRISM : roll < prism + gold ? Augment.Tier.GOLD : Augment.Tier.SILVER;
        return draw(tier, new ArrayList<>(), rnd);
    }

    /**
     * 새로고침: keeps the tier of the cards on screen and deals three others. The cards just shown come
     * back only if that tier has fewer than three others left; other tiers fill in only after that.
     */
    public List<Augment> reroll(List<Augment> shown, Random rnd) {
        Augment.Tier tier = shown.isEmpty() ? Augment.Tier.SILVER : shown.get(0).getTier();
        return draw(tier, shown, rnd);
    }

    /** Three untaken augments of the tier, avoiding `avoid` first, then the tier, then any tier. */
    private List<Augment> draw(Augment.Tier tier, List<Augment> avoid, Random rnd) {
        List<Augment> out = new ArrayList<>();
        fill(out, Augment.ofTier(tier), avoid, rnd);
        fill(out, Augment.ofTier(tier), new ArrayList<>(), rnd);   // tier nearly used up: allow the shown ones
        fill(out, Augment.all(), avoid, rnd);                       // still short: other tiers, not the shown ones
        fill(out, Augment.all(), new ArrayList<>(), rnd);
        return out;
    }

    private void fill(List<Augment> out, List<Augment> from, List<Augment> avoid, Random rnd) {
        List<Augment> pool = new ArrayList<>(from);
        pool.removeAll(augments);
        pool.removeAll(avoid);
        pool.removeAll(out);
        Collections.shuffle(pool, rnd);
        for (Augment a : pool) {
            if (out.size() >= OFFER_SIZE) return;
            out.add(a);
        }
    }

    /** Gold and EXP after the gold/EXP augments. */
    public int gold(int base) {
        return (int) Math.round(base * (1 + total().goldPct()));
    }

    public int exp(int base) {
        return (int) Math.round(base * (1 + total().expPct()));
    }
}
