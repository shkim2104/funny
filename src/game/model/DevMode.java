package game.model;

/**
 * 개발자 모드: only a character named exactly {@link #NAME} can open the developer window (F12, or the
 * town menu button). The battle cheats are switches for this run of the game only: they start off,
 * are never saved, and only work for that character outside the colosseum.
 */
public final class DevMode {
    private DevMode() {}

    /** The secret name. */
    public static final String NAME = "개발자";

    /** 무적: monsters' hits and poison take no HP. */
    public static boolean invincible;
    /** 한 방 처치: every hit finishes the monster. */
    public static boolean oneHit;
    /** MP 무한: skills cost nothing. */
    public static boolean infiniteMp;

    public static boolean isDev(Player p) {
        return p != null && NAME.equals(p.getName());
    }

    public static boolean invincible(Player p) { return invincible && isDev(p); }
    public static boolean oneHit(Player p) { return oneHit && isDev(p); }
    public static boolean infiniteMp(Player p) { return infiniteMp && isDev(p); }

    /** The switches that are on, e.g. "무적 · 한 방 처치", or "" when none is (or p is not the developer). */
    public static String activeText(Player p) {
        if (!isDev(p)) return "";
        StringBuilder s = new StringBuilder();
        if (invincible) s.append("무적");
        if (oneHit) s.append(s.length() > 0 ? " · " : "").append("한 방 처치");
        if (infiniteMp) s.append(s.length() > 0 ? " · " : "").append("MP 무한");
        return s.toString();
    }

    /** Turns every switch off (a new game, or tests). */
    public static void reset() {
        invincible = oneHit = infiniteMp = false;
    }
}
