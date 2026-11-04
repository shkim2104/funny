package game.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A weapon or armor the player forged and drew (대장간 제작): its rolled stat and effects. It becomes a
 * catalog Item under its own name, so pieces, enhancement and saving work like any other gear. The
 * drawing is kept apart (a PNG next to the save, see SaveManager.drawingFile and artKey()).
 */
public final class CraftedGear {
    /** Gold per point of the stat; selling pays half. */
    private static final int PRICE_PER_POINT = 25;

    private final String name;
    private final Item.Type type;
    private final Item.Grade grade;
    private final int value;
    private final Map<Crafting.Effect, Double> effects;

    CraftedGear(String name, Item.Type type, Item.Grade grade, int value, Map<Crafting.Effect, Double> effects) {
        this.name = name;
        this.type = type;
        this.grade = grade;
        this.value = value;
        this.effects = new LinkedHashMap<>(effects);
    }

    public String getName() { return name; }
    public Item.Type getType() { return type; }
    public Item.Grade getGrade() { return grade; }
    public int getValue() { return value; }
    public Map<Crafting.Effect, Double> getEffects() { return Collections.unmodifiableMap(effects); }

    /** The same gear under another name (a duel opponent's piece named like one of ours). */
    CraftedGear renamed(String newName) {
        return new CraftedGear(newName, type, grade, value, effects);
    }

    public Item toItem() {
        Bonus b = Bonus.of();
        for (Map.Entry<Crafting.Effect, Double> e : effects.entrySet()) b = e.getKey().apply(b, e.getValue());
        return new Item(name, type, value, value * PRICE_PER_POINT, grade).bonus(b).crafted();
    }

    /** File key of the drawing: a name may hold letters a file name can't. */
    public String artKey() {
        return artKey(name);
    }

    public static String artKey(String itemName) {
        return "craft_" + Integer.toHexString(itemName.hashCode());
    }

    // ---------- save format: name|W or A|grade|value|key=v,key=v ----------

    String encode() {
        StringBuilder fx = new StringBuilder();
        for (Map.Entry<Crafting.Effect, Double> e : effects.entrySet()) {
            if (fx.length() > 0) fx.append(',');
            fx.append(e.getKey().key).append('=').append(e.getValue());
        }
        return name + "|" + (type == Item.Type.WEAPON ? "W" : "A") + "|" + grade.name() + "|" + value + "|" + fx;
    }

    static CraftedGear decode(String text) {
        String[] f = text.split("\\|", -1);
        if (f.length < 5 || f[0].trim().isEmpty()) return null;
        try {
            Item.Type type = "W".equals(f[1]) ? Item.Type.WEAPON : Item.Type.ARMOR;
            Item.Grade grade = Item.Grade.valueOf(f[2]);
            int value = Integer.parseInt(f[3]);
            Map<Crafting.Effect, Double> effects = new LinkedHashMap<>();
            if (!f[4].isEmpty()) {
                for (String kv : f[4].split(",")) {
                    String[] p = kv.split("=");
                    Crafting.Effect e = p.length == 2 ? Crafting.Effect.byKey(p[0]) : null;
                    if (e != null) effects.put(e, Double.parseDouble(p[1]));
                }
            }
            return new CraftedGear(f[0], type, grade, value, effects);
        } catch (RuntimeException unreadable) {
            return null;
        }
    }

    /** The same stat and effects: a duel opponent's copy of a piece we already know. */
    boolean sameAs(Item item) {
        if (item == null || !item.isCrafted()) return false;
        Item mine = toItem();
        return item.getType() == mine.getType() && item.getGrade() == mine.getGrade() && item.getValue() == mine.getValue()
                && item.getBonus().lines().equals(mine.getBonus().lines());
    }
}
