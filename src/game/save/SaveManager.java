package game.save;

import game.model.Job;
import game.model.Material;
import game.model.Player;
import game.model.Quest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Character saves, MapleStory-style: every character is its own file in a "characters" folder, and the
 * game picks one on the character select screen. The per-file line format is the same one the single
 * save.txt always used, so an old save.txt is simply moved in as a character the first time we look.
 */
public class SaveManager {
    /** Character slots shown on the select screen. */
    public static final int MAX_CHARACTERS = 6;

    private SaveManager() {}

    /** A character on disk: its file id, the loaded player, and when it was last saved. */
    public static final class CharacterSave {
        public final String id;
        public final Player player;
        public final long savedAt;

        CharacterSave(String id, Player player, long savedAt) {
            this.id = id;
            this.player = player;
            this.savedAt = savedAt;
        }
    }

    /** Data lives under the user's OS data folder so it works the same no matter where the game is launched from. */
    static File dataDir() {
        String appData = System.getenv("APPDATA");
        File dir = (appData != null && !appData.isEmpty())
                ? new File(appData, "NotebookRPG")
                : new File(System.getProperty("user.home"), ".notebookrpg");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static File charactersDir() {
        File dir = new File(dataDir(), "characters");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    private static File fileFor(String id) {
        return new File(charactersDir(), id + ".txt");
    }

    /** Moves a pre-character-select save.txt in as a character, once. */
    private static void migrateLegacySave() {
        File legacy = new File(dataDir(), "save.txt");
        if (!legacy.exists()) return;
        File target = fileFor(String.format("c%015d", legacy.lastModified()));
        try {
            Files.move(legacy.toPath(), target.toPath());
        } catch (IOException e) {
            System.out.println("이전 저장 파일을 옮기지 못했습니다: " + e.getMessage());
        }
    }

    /** Every saved character, oldest first (ids are creation timestamps). Unreadable files are skipped. */
    public static List<CharacterSave> listCharacters() {
        migrateLegacySave();
        List<CharacterSave> result = new ArrayList<>();
        File[] files = charactersDir().listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null) return result;
        for (File f : files) {
            String id = f.getName().substring(0, f.getName().length() - 4);
            Player p = load(id);
            if (p != null) result.add(new CharacterSave(id, p, f.lastModified()));
        }
        result.sort((a, b) -> a.id.compareTo(b.id));
        return result;
    }

    public static boolean hasCharacters() {
        return !listCharacters().isEmpty();
    }

    /** A fresh id for a new character; zero-padded timestamps so ids sort in creation order. */
    public static String newId() {
        String id;
        long t = System.currentTimeMillis();
        do {
            id = String.format("c%015d", t++);
        } while (fileFor(id).exists());
        return id;
    }

    /** The character, or (세이브 백업) the previous save if this one can't be read. */
    public static Player load(String id) {
        Player p = read(fileFor(id));
        File bak = backupFor(id);
        if (p == null && bak.exists()) {
            p = read(bak);
            if (p != null) System.out.println("세이브가 깨져서 백업에서 불러왔습니다: " + id);
        }
        return p;
    }

    /** The previous save, kept by 세이브 백업 (".bak", so it is never listed as a character). */
    private static File backupFor(String id) {
        return new File(charactersDir(), id + ".bak");
    }

    public static void delete(String id) {
        File f = fileFor(id);
        if (f.exists() && !f.delete()) System.out.println("캐릭터를 삭제하지 못했습니다: " + id);
        backupFor(id).delete();
        // The character's own drawing goes with it.
        File[] drawings = charactersDir().listFiles((d, name) -> name.startsWith(id + "_") && name.endsWith(".png"));
        if (drawings != null) for (File d : drawings) d.delete();
    }

    /** Where one part of a character's hand-drawn look is stored (PNG beside the save file). */
    public static File drawingFile(String id, String part) {
        return new File(charactersDir(), id + "_" + part + ".png");
    }

    /** Writes to a temp file first and then swaps it in, so a crash mid-save can't leave a half-written character. */
    public static void save(Player p, String id) {
        File target = fileFor(id);
        File tmp = new File(target.getParentFile(), id + ".tmp");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8))) {
            write(pw, p);
        } catch (IOException e) {
            System.out.println("저장에 실패했습니다: " + e.getMessage());
            return;
        }
        try {
            // 세이브 백업: keep the save being replaced, as long as it is itself readable.
            if (Settings.isSaveBackup() && target.exists() && read(target) != null) {
                Files.copy(target.toPath(), backupFor(id).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("게임을 저장했습니다.");
        } catch (IOException e) {
            System.out.println("저장에 실패했습니다: " + e.getMessage());
        }
    }

    private static void write(PrintWriter pw, Player p) {
        pw.println(p.getName());
        pw.println(p.getLevel());
        pw.println(p.getExp());
        pw.println(p.getExpToNext());
        pw.println(p.getMaxHp());
        pw.println(p.getHp());
        pw.println(p.getMaxMp());
        pw.println(p.getMp());
        pw.println(p.getBaseAtk());
        pw.println(p.getBaseDef());
        pw.println(p.getLuck());
        pw.println(p.getStatPoints());
        pw.println(p.getGold());
        pw.println(p.getUnlockedDungeon());
        pw.println(p.getWeaponName() == null ? "NONE" : p.getWeaponName());
        pw.println(p.getArmorName() == null ? "NONE" : p.getArmorName());
        List<String> inv = p.getConsumables();
        pw.println(inv.size());
        for (String item : inv) pw.println(item);
        pw.println(p.getSpd());
        // Job path as comma-separated enum names in tier order, e.g. "SWORDSMAN,PAGE" (empty = beginner).
        StringBuilder path = new StringBuilder();
        for (Job j : p.getJobPath()) {
            if (path.length() > 0) path.append(',');
            path.append(j.name());
        }
        pw.println(path);
        pw.println(Quest.encodeAll(p.getQuests()));
        // Material counts, comma-separated in Material's order.
        StringBuilder materials = new StringBuilder();
        for (Material m : Material.values()) {
            if (materials.length() > 0) materials.append(',');
            materials.append(p.getMaterial(m));
        }
        pw.println(materials);
        pw.println(p.encodeGear());
    }

    private static Player read(File file) {
        try (BufferedReader br = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return parse(br);
        } catch (IOException | RuntimeException e) {
            System.out.println("불러오기에 실패했습니다: " + file.getName() + " (" + e.getMessage() + ")");
            return null;
        }
    }

    /** A character as text, the same lines a save file holds (sent to the other player in a duel). */
    public static String encode(Player p) {
        StringWriter text = new StringWriter();
        try (PrintWriter pw = new PrintWriter(text)) {
            write(pw, p);
        }
        return text.toString();
    }

    /** The character from encode()'s text, or null if it can't be read. */
    public static Player decode(String text) {
        try (BufferedReader br = new BufferedReader(new StringReader(text))) {
            return parse(br);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static Player parse(BufferedReader br) throws IOException {
        String name = br.readLine();
        int level = Integer.parseInt(br.readLine());
        int exp = Integer.parseInt(br.readLine());
        int expToNext = Integer.parseInt(br.readLine());
        int maxHp = Integer.parseInt(br.readLine());
        int hp = Integer.parseInt(br.readLine());
        int maxMp = Integer.parseInt(br.readLine());
        int mp = Integer.parseInt(br.readLine());
        int baseAtk = Integer.parseInt(br.readLine());
        int baseDef = Integer.parseInt(br.readLine());
        int luck = Integer.parseInt(br.readLine());
        int statPoints = Integer.parseInt(br.readLine());
        int gold = Integer.parseInt(br.readLine());
        int unlockedDungeon = Integer.parseInt(br.readLine());
        String weaponName = br.readLine();
        if ("NONE".equals(weaponName)) weaponName = null;
        String armorName = br.readLine();
        if ("NONE".equals(armorName)) armorName = null;
        int invSize = Integer.parseInt(br.readLine());
        List<String> inventory = new ArrayList<>();
        for (int i = 0; i < invSize; i++) inventory.add(br.readLine());

        // Older saves predate the speed stat: default to a mid-range value when the line is missing.
        String spdLine = br.readLine();
        int spd = spdLine != null ? Integer.parseInt(spdLine) : 6;

        // The EXP curve is recomputed from the level rather than trusting the saved value, so
        // saves made under an older curve pick up the current one. Progress toward the next
        // level is kept but capped just below it (no surprise level-ups on load).
        expToNext = Player.expRequiredFor(level);
        exp = Math.min(exp, expToNext - 1);
        Player player = new Player(name, level, exp, expToNext, maxHp, hp, maxMp, mp,
                baseAtk, baseDef, luck, spd, statPoints, gold, unlockedDungeon, weaponName, armorName, inventory);

        // Saves from before jobs existed (no line) load as a beginner, free to advance once eligible.
        // The path stops at the first unknown/out-of-order job (e.g. a job renamed since), so
        // the player can simply re-pick that tier.
        String jobLine = br.readLine();
        List<Job> path = new ArrayList<>();
        if (jobLine != null && !jobLine.trim().isEmpty()) {
            for (String part : jobLine.trim().split(",")) {
                Job j;
                try {
                    j = Job.valueOf(part.trim());
                } catch (IllegalArgumentException unknownJob) {
                    break;
                }
                if (j.getTier() != path.size() + 1) break;
                path.add(j);
            }
        }
        player.setJobPath(path);

        // Saves from before the quest board have no line here; the board is filled on first visit.
        player.setQuests(Quest.decodeAll(br.readLine()));

        // Saves from before forging materials have no line here and start with none.
        String materialLine = br.readLine();
        if (materialLine != null && !materialLine.trim().isEmpty()) {
            String[] counts = materialLine.trim().split(",");
            Material[] all = Material.values();
            for (int i = 0; i < all.length && i < counts.length; i++) {
                player.addMaterial(all[i], Integer.parseInt(counts[i].trim()));
            }
        }

        // Saves from before gear pieces have no line here: the constructor already turned their
        // weapons and armor, listed in the inventory by name, into +0 pieces.
        String gearLine = br.readLine();
        if (gearLine != null) player.decodeGear(gearLine);
        return player;
    }
}
