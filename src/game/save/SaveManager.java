package game.save;

import game.model.Player;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SaveManager {
    private SaveManager() {}

    /** Save file lives under the user's OS data folder so it works the same no matter where the game is launched from. */
    private static File saveFile() {
        String appData = System.getenv("APPDATA");
        File dir = (appData != null && !appData.isEmpty())
                ? new File(appData, "NotebookRPG")
                : new File(System.getProperty("user.home"), ".notebookrpg");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "save.txt");
    }

    public static boolean hasSave() {
        return saveFile().exists();
    }

    public static void save(Player p) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(saveFile()))) {
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
            List<String> inv = p.getInventory();
            pw.println(inv.size());
            for (String item : inv) pw.println(item);
            pw.println(p.getSpd());
            System.out.println("게임을 저장했습니다.");
        } catch (IOException e) {
            System.out.println("저장에 실패했습니다: " + e.getMessage());
        }
    }

    public static Player load() {
        try (BufferedReader br = new BufferedReader(new FileReader(saveFile()))) {
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

            return new Player(name, level, exp, expToNext, maxHp, hp, maxMp, mp,
                    baseAtk, baseDef, luck, spd, statPoints, gold, unlockedDungeon, weaponName, armorName, inventory);
        } catch (IOException | NumberFormatException e) {
            System.out.println("불러오기에 실패했습니다: " + e.getMessage());
            return null;
        }
    }
}
