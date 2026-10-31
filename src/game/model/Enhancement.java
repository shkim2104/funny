package game.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The blacksmith's rules. A "target" is the level being attempted: target 3 takes a +2 piece to +3.
 * An attempt always consumes its cost. It succeeds, fails (the level stays), or from +13 on may destroy
 * the piece. A destroyed piece stays in the bag and can be restored to +5 by sacrificing a copy of it.
 */
public final class Enhancement {
    public enum Result { SUCCESS, FAIL, DESTROYED }

    public static final int MAX_LEVEL = 20;
    /** The level a destroyed piece comes back at. */
    public static final int RESTORE_LEVEL = 5;
    /** Stat gained per level, as a share of the item's base stat (always at least +1 per level). */
    private static final double GAIN_PER_LEVEL = 0.05;

    // Per target level, index 0 = target +1.
    private static final int[] SUCCESS_PCT = {100, 95, 90, 85, 80, 75, 70, 65, 60, 55, 40, 40, 40, 30, 30, 30, 30, 30, 5, 5};
    private static final int[] DESTROY_PCT = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 3, 4, 5, 6, 8, 10, 15};
    private static final int[] GRAPHITE = {5, 8, 12, 16, 20, 26, 32, 40, 50, 60, 70, 80, 90, 100, 110, 120, 130, 140, 160, 200};
    private static final int[] HARD_LEAD = {0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 7, 9, 11, 13, 16, 19, 22, 25, 30, 40};
    private static final int[] GOLD = {50, 80, 120, 180, 250, 450, 650, 900, 1200, 1500, 2000, 2500, 3000, 3800, 4600, 5500, 6500, 8000, 11000, 15000};

    /** 분해: graphite per grade, 일반 to 전설. Boss gear (신화) gives hard lead instead. */
    private static final int[] SCRAP_GRAPHITE = {1, 2, 4, 8, 15, 0};
    private static final int MYTHIC_SCRAP_LEAD = 1;

    private Enhancement() {}

    public static int successPct(int target) { return SUCCESS_PCT[target - 1]; }
    public static int destroyPct(int target) { return DESTROY_PCT[target - 1]; }
    public static int failPct(int target) { return 100 - successPct(target) - destroyPct(target); }
    public static int graphiteCost(int target) { return GRAPHITE[target - 1]; }
    public static int hardLeadCost(int target) { return HARD_LEAD[target - 1]; }
    public static int goldCost(int target) { return GOLD[target - 1]; }

    public static int boostedValue(int base, int level) {
        return base + Math.max(level, (int) Math.round(base * GAIN_PER_LEVEL * level));
    }

    /** A piece can be enhanced unless it is destroyed or already at the top. */
    public static boolean canEnhance(GearPiece piece) {
        return !piece.isDestroyed() && piece.getEnhance() < MAX_LEVEL;
    }

    /** Whether the player can pay for the piece's next attempt, with or without 지우개 가루. */
    public static boolean canAfford(Player player, GearPiece piece, boolean useEraser) {
        int target = piece.getEnhance() + 1;
        return player.getGold() >= goldCost(target) && player.getMaterial(Material.GRAPHITE) >= graphiteCost(target)
                && player.getMaterial(Material.HARD_LEAD) >= hardLeadCost(target)
                && (!useEraser || player.getMaterial(Material.ERASER_DUST) >= 1);
    }

    /**
     * Pays and rolls one attempt. Returns null, with nothing spent, if it isn't possible. With useEraser
     * and a chance of destruction, one 지우개 가루 turns that chance into a plain failure.
     */
    public static Result attempt(Player player, GearPiece piece, boolean useEraser, Random rnd) {
        if (!canEnhance(piece)) return null;
        int target = piece.getEnhance() + 1;
        boolean eraser = useEraser && destroyPct(target) > 0;
        if (!canAfford(player, piece, eraser)) return null;
        player.spendGold(goldCost(target));
        player.spendMaterial(Material.GRAPHITE, graphiteCost(target));
        player.spendMaterial(Material.HARD_LEAD, hardLeadCost(target));
        if (eraser) player.spendMaterial(Material.ERASER_DUST, 1);

        int roll = rnd.nextInt(100);
        if (roll < successPct(target)) {
            piece.setEnhance(target);
            return Result.SUCCESS;
        }
        if (roll < successPct(target) + destroyPct(target) && !eraser) {
            piece.markDestroyed();
            player.unequip(piece);
            return Result.DESTROYED;
        }
        return Result.FAIL;
    }

    /** Copies of a destroyed piece that may be sacrificed to restore it, the least enhanced first. */
    public static List<GearPiece> restoreDonors(Player player, GearPiece broken) {
        List<GearPiece> donors = new ArrayList<>();
        for (GearPiece g : player.getGear()) {
            if (g != broken && g.getItemName().equals(broken.getItemName()) && player.isSpare(g) && !g.isDestroyed()) donors.add(g);
        }
        donors.sort((a, b) -> Integer.compare(a.getEnhance(), b.getEnhance()));
        return donors;
    }

    /** 전승: the donor is used up and the broken piece returns at +5. */
    public static boolean restore(Player player, GearPiece broken, GearPiece donor) {
        if (!broken.isDestroyed() || !restoreDonors(player, broken).contains(donor)) return false;
        player.removeGear(donor);
        broken.restoreAt(RESTORE_LEVEL);
        return true;
    }

    public static int scrapGraphite(GearPiece piece) {
        return SCRAP_GRAPHITE[piece.getItem().getGrade().ordinal()];
    }

    public static int scrapHardLead(GearPiece piece) {
        return piece.getItem().getGrade() == Item.Grade.MYTHIC ? MYTHIC_SCRAP_LEAD : 0;
    }

    /** 분해: turns a spare piece (not worn, not locked) into materials. Enhancement is not refunded. */
    public static boolean disassemble(Player player, GearPiece piece) {
        if (!player.isSpare(piece)) return false;
        player.addMaterial(Material.GRAPHITE, scrapGraphite(piece));
        player.addMaterial(Material.HARD_LEAD, scrapHardLead(piece));
        player.removeGear(piece);
        return true;
    }
}
