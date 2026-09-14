package game.model;

public class Item {
    public enum Type { WEAPON, ARMOR, POTION, ETHER }

    public enum Grade {
        COMMON("일반", "#9aa0ad"),
        UNCOMMON("고급", "#6bcf6b"),
        RARE("희귀", "#5aa0e0"),
        EPIC("영웅", "#b06be0"),
        LEGENDARY("전설", "#e0a83a");

        private final String label;
        private final String colorHex;

        Grade(String label, String colorHex) {
            this.label = label;
            this.colorHex = colorHex;
        }

        public String getLabel() { return label; }
        public String getColorHex() { return colorHex; }
    }

    private final String name;
    private final Type type;
    private final int value;
    private final int price;
    private final Grade grade;

    public Item(String name, Type type, int value, int price, Grade grade) {
        this.name = name;
        this.type = type;
        this.value = value;
        this.price = price;
        this.grade = grade;
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public int getValue() { return value; }
    public int getPrice() { return price; }
    public Grade getGrade() { return grade; }

    public String describe() {
        String base;
        switch (type) {
            case WEAPON: base = name + " (공격력 +" + value + ", " + price + "G)"; break;
            case ARMOR: base = name + " (방어력 +" + value + ", " + price + "G)"; break;
            case POTION: base = name + " (HP " + value + " 회복, " + price + "G)"; break;
            case ETHER: base = name + " (MP " + value + " 회복, " + price + "G)"; break;
            default: base = name;
        }
        if (type == Type.WEAPON || type == Type.ARMOR) {
            return "[" + grade.getLabel() + "] " + base;
        }
        return base;
    }

    public String getTypeLabel() {
        switch (type) {
            case WEAPON: return "무기";
            case ARMOR: return "방어구";
            case POTION: return "포션";
            case ETHER: return "에테르";
            default: return "";
        }
    }

    public String getStatText() {
        switch (type) {
            case WEAPON: return "공격력 +" + value;
            case ARMOR: return "방어력 +" + value;
            case POTION: return "HP " + value + " 회복";
            case ETHER: return "MP " + value + " 회복";
            default: return "";
        }
    }
}
