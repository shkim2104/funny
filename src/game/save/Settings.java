package game.save;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Game-wide options (not per character), kept in settings.properties next to the character saves.
 * Read once at startup; every change is written straight back. Each option has a default, so a
 * missing or broken file just means "all defaults".
 */
public final class Settings {
    private static final Properties PROPS = load();

    private Settings() {}

    private static File file() {
        return new File(SaveManager.dataDir(), "settings.properties");
    }

    private static Properties load() {
        Properties p = new Properties();
        File f = file();
        if (f.exists()) {
            try (InputStream in = Files.newInputStream(f.toPath())) {
                p.load(in);
            } catch (Exception e) {
                System.out.println("설정을 불러오지 못했습니다: " + e.getMessage());
            }
        }
        return p;
    }

    private static void store() {
        try (OutputStream out = Files.newOutputStream(file().toPath())) {
            PROPS.store(out, "공책 RPG settings");
        } catch (Exception e) {
            System.out.println("설정을 저장하지 못했습니다: " + e.getMessage());
        }
    }

    private static boolean bool(String key, boolean def) {
        String v = PROPS.getProperty(key);
        return v == null ? def : Boolean.parseBoolean(v.trim());
    }

    private static void putBool(String key, boolean value) {
        PROPS.setProperty(key, String.valueOf(value));
        store();
    }

    private static int num(String key, int def, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(PROPS.getProperty(key, String.valueOf(def)).trim())));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static void putNum(String key, int value) {
        PROPS.setProperty(key, String.valueOf(value));
        store();
    }

    // ---------- 저장·소리 ----------

    /** Save the current character when the game window is closed (or 종료 / 타이틀로). */
    public static boolean isAutoSaveOnExit() { return bool("autoSaveOnExit", true); }
    public static void setAutoSaveOnExit(boolean on) { putBool("autoSaveOnExit", on); }

    /** Keep the previous save as id.bak and fall back to it if the save can't be read. */
    public static boolean isSaveBackup() { return bool("saveBackup", true); }
    public static void setSaveBackup(boolean on) { putBool("saveBackup", on); }

    /** Background music volume, 0 (off) to 100. */
    public static int getMusicVolume() { return num("musicVolume", 70, 0, 100); }
    public static void setMusicVolume(int v) { putNum("musicVolume", Math.max(0, Math.min(100, v))); }

    /** Sound effect volume, 0 (off) to 100. */
    public static int getSfxVolume() { return num("sfxVolume", 70, 0, 100); }
    public static void setSfxVolume(int v) { putNum("sfxVolume", Math.max(0, Math.min(100, v))); }

    // ---------- 전투 ----------

    /** Battle animation speed in tenths: 10 = 1x, 15 = 1.5x, 20 = 2x. */
    public static int getBattleSpeedTenths() { return num("battleSpeed", 10, 10, 20); }
    public static void setBattleSpeedTenths(int v) { putNum("battleSpeed", v); }

    public static double battleSpeed() { return getBattleSpeedTenths() / 10.0; }

    /** Repeat basic attacks automatically (never in boss fights). */
    public static boolean isAutoBattle() { return bool("autoBattle", false); }
    public static void setAutoBattle(boolean on) { putBool("autoBattle", on); }

    /** Drink a potion automatically at or below this HP percent; 0 = off. */
    public static int getAutoPotionPercent() { return num("autoPotion", 0, 0, 50); }
    public static void setAutoPotionPercent(int v) { putNum("autoPotion", v); }

    /** No screen shake, white flash or hit-stop. */
    public static boolean isReducedEffects() { return bool("reducedEffects", false); }
    public static void setReducedEffects(boolean on) { putBool("reducedEffects", on); }

    /** Battle log keeps only the important lines (no per-hit damage). */
    public static boolean isBriefLog() { return bool("briefLog", false); }
    public static void setBriefLog(boolean on) { putBool("briefLog", on); }

    // ---------- 증강 ----------

    /** Augment cards show how the pick changes the player's numbers. */
    public static boolean isAugmentPreview() { return bool("augmentPreview", true); }
    public static void setAugmentPreview(boolean on) { putBool("augmentPreview", on); }

    /** Tab in battle lists this run's augments. */
    public static boolean isAugmentListKey() { return bool("augmentListKey", true); }
    public static void setAugmentListKey(boolean on) { putBool("augmentListKey", on); }

    // ---------- 마을·아이템 ----------

    /** Gear from bosses, chests and the treasure wheel is equipped if it beats what's worn. */
    public static boolean isAutoEquip() { return bool("autoEquip", false); }
    public static void setAutoEquip(boolean on) { putBool("autoEquip", on); }

    /** Unequipped [일반] gear is sold on coming back to town. */
    public static boolean isAutoSellCommon() { return bool("autoSellCommon", false); }
    public static void setAutoSellCommon(boolean on) { putBool("autoSellCommon", on); }

    /** Selling 영웅 grade or better asks first. */
    public static boolean isSellConfirm() { return bool("sellConfirm", true); }
    public static void setSellConfirm(boolean on) { putBool("sellConfirm", on); }

    /** The treasure wheel lands at once instead of spinning. */
    public static boolean isSkipWheel() { return bool("skipWheel", false); }
    public static void setSkipWheel(boolean on) { putBool("skipWheel", on); }

    /** A notice in town when quest board requests are finished. */
    public static boolean isQuestNotify() { return bool("questNotify", true); }
    public static void setQuestNotify(boolean on) { putBool("questNotify", on); }

    // ---------- 화면·조작 ----------

    public static boolean isStartFullscreen() { return bool("startFullscreen", true); }
    public static void setStartFullscreen(boolean on) { putBool("startFullscreen", on); }

    public static boolean isPageTurn() { return bool("pageTurn", true); }
    public static void setPageTurn(boolean on) { putBool("pageTurn", on); }

    /** Bigger pencil text (applies to screens built after the change, i.e. fully after a restart). */
    public static boolean isLargeText() { return bool("largeText", false); }
    public static void setLargeText(boolean on) { putBool("largeText", on); }

    /** 1~4 skills, I equipment, S stats, Esc settings. */
    public static boolean isHotkeys() { return bool("hotkeys", true); }
    public static void setHotkeys(boolean on) { putBool("hotkeys", on); }

    /** The name label that follows the mouse over buildings and regions. */
    public static boolean isBuildingLabels() { return bool("buildingLabels", true); }
    public static void setBuildingLabels(boolean on) { putBool("buildingLabels", on); }
}
