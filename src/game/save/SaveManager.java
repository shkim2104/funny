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
    private static final String SAVE_FILE = "save.txt";

    private SaveManager() {}

    public static boolean hasSave() {
        return new File(SAVE_FILE).exists();
    }

    public static void save(Player p) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(SAVE_FILE))) {
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
            System.out.println("게임을 저장했습니다.");
        } catch (IOException e) {
            System.out.println("저장에 실패했습니다: " + e.getMessage());
        }
    }

    public static Player load() {
        try (BufferedReader br = new BufferedReader(new FileReader(SAVE_FILE))) {
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

            return new Player(name, level, exp, expToNext, maxHp, hp, maxMp, mp,
                    baseAtk, baseDef, luck, statPoints, gold, unlockedDungeon, weaponName, armorName, inventory);
        } catch (IOException | NumberFormatException e) {
            System.out.println("불러오기에 실패했습니다: " + e.getMessage());
            return null;
        }
    }
}
