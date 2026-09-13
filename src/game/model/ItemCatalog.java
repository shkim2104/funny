package game.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ItemCatalog {
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    static {
        register(new Item("낡은 검", Item.Type.WEAPON, 5, 45, Item.Grade.COMMON));
        register(new Item("강철 검", Item.Type.WEAPON, 12, 135, Item.Grade.UNCOMMON));
        register(new Item("미스릴 검", Item.Type.WEAPON, 22, 330, Item.Grade.RARE));
        register(new Item("용의 검", Item.Type.WEAPON, 38, 650, Item.Grade.EPIC));
        register(new Item("전설의 성검", Item.Type.WEAPON, 60, 1400, Item.Grade.LEGENDARY));

        register(new Item("가죽 갑옷", Item.Type.ARMOR, 4, 30, Item.Grade.COMMON));
        register(new Item("철 갑옷", Item.Type.ARMOR, 10, 90, Item.Grade.UNCOMMON));
        register(new Item("용린 갑옷", Item.Type.ARMOR, 18, 220, Item.Grade.RARE));
        register(new Item("용의 갑옷", Item.Type.ARMOR, 30, 550, Item.Grade.EPIC));
        register(new Item("전설의 수호 갑옷", Item.Type.ARMOR, 48, 1150, Item.Grade.LEGENDARY));

        register(new Item("포션", Item.Type.POTION, 30, 15, Item.Grade.COMMON));
        register(new Item("하이 포션", Item.Type.POTION, 80, 40, Item.Grade.COMMON));
        register(new Item("에테르", Item.Type.ETHER, 25, 35, Item.Grade.COMMON));
    }

    private ItemCatalog() {}

    private static void register(Item item) {
        ITEMS.put(item.getName(), item);
    }

    public static Item get(String name) {
        return ITEMS.get(name);
    }

    public static Collection<Item> all() {
        return ITEMS.values();
    }

    /** Weapons and armor (equipable gear) of the given grade. */
    public static List<Item> equipableOfGrade(Item.Grade grade) {
        List<Item> result = new ArrayList<>();
        for (Item item : ITEMS.values()) {
            if ((item.getType() == Item.Type.WEAPON || item.getType() == Item.Type.ARMOR) && item.getGrade() == grade) {
                result.add(item);
            }
        }
        return result;
    }
}
