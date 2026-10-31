package game.model;

/**
 * One weapon or armor the player actually owns. Two copies of the same item are two pieces, each with
 * its own enhancement level, lock and destroyed state. The item itself stays a shared catalog entry.
 */
public final class GearPiece {
    private final String itemName;
    private int enhance;
    private boolean locked;
    private boolean destroyed;

    public GearPiece(String itemName) {
        this.itemName = itemName;
    }

    GearPiece(String itemName, int enhance, boolean locked, boolean destroyed) {
        this.itemName = itemName;
        this.enhance = enhance;
        this.locked = locked;
        this.destroyed = destroyed;
    }

    public String getItemName() { return itemName; }
    public Item getItem() { return ItemCatalog.get(itemName); }
    public int getEnhance() { return enhance; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public boolean isDestroyed() { return destroyed; }
    public boolean isWeapon() { return getItem().getType() == Item.Type.WEAPON; }

    /** The attack or defense this piece gives, enhancement included. */
    public int getValue() {
        return Enhancement.boostedValue(getItem().getValue(), enhance);
    }

    /** "강철 검 +3", or "파괴된 강철 검". */
    public String getDisplayName() {
        if (destroyed) return "파괴된 " + itemName;
        return enhance > 0 ? itemName + " +" + enhance : itemName;
    }

    /** "공격력 +21", or "파괴됨" while it can't be used. */
    public String getStatText() {
        if (destroyed) return "파괴됨";
        return (isWeapon() ? "공격력 +" : "방어력 +") + getValue();
    }

    /** The stat with where it comes from, for detail panes. */
    public String getStatDetail() {
        if (destroyed) return "파괴됨 · 같은 장비로 복구할 수 있어요";
        return enhance > 0 ? getStatText() + " (기본 " + getItem().getValue() + " + 강화 " + (getValue() - getItem().getValue()) + ")"
                : getStatText();
    }

    void setEnhance(int enhance) { this.enhance = enhance; }
    void markDestroyed() { this.destroyed = true; }

    /** 전승: back in one piece at the given level. */
    void restoreAt(int level) {
        this.destroyed = false;
        this.enhance = level;
    }
}
