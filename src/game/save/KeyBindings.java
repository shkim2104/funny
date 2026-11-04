package game.save;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 단축키: every action the keyboard can do, its default key, and the key the player chose instead (kept in
 * Settings as key.ACTION). Keys are matched by key code and Ctrl/Shift/Alt only, so the same press works
 * whatever else is held. Two actions may share a key only where they can't both apply (see Scope.overlaps).
 */
public final class KeyBindings {
    private KeyBindings() {}

    /** Where an action listens. */
    public enum Scope {
        /** Any open window (창 닫기). */
        WINDOW("열린 창"),
        /** The town only. */
        TOWN("마을"),
        /** The town and the maps (던전 지도, 멀티버스), with no window open. */
        MAPS("마을·지도"),
        BATTLE("전투"),
        /** Anywhere in the game window. */
        ANYWHERE("어디서나");

        public final String label;

        Scope(String label) {
            this.label = label;
        }

        /** Whether one key on both would be ambiguous. 창 닫기 lives in windows only, so ESC can also open 설정. */
        public boolean overlaps(Scope o) {
            if (this == WINDOW || o == WINDOW) return this == o;
            if (this == ANYWHERE || o == ANYWHERE || this == o) return true;
            return (this == TOWN && o == MAPS) || (this == MAPS && o == TOWN);
        }
    }

    public enum Action {
        CLOSE("창 닫기", "ESCAPE", Scope.WINDOW),
        SETTINGS("설정", "ESCAPE", Scope.MAPS),
        STATUS("상태 보기", "C", Scope.TOWN),
        INVENTORY("인벤토리", "I", Scope.TOWN),
        STATS("스탯 분배", "S", Scope.TOWN),
        DUNGEON_MAP("던전 지도", "M", Scope.TOWN),
        SAVE("저장하기", "ctrl S", Scope.TOWN),
        QUESTS("퀘스트 (전투 중에도 보기)", "J", Scope.ANYWHERE),
        DEV("개발자 모드 (개발자 캐릭터만)", "F12", Scope.ANYWHERE),
        FULLSCREEN("전체 화면", "F11", Scope.ANYWHERE),
        ATTACK("공격 메뉴", "A", Scope.BATTLE),
        BASIC("기본 공격", "SPACE", Scope.BATTLE),
        SKILL1("스킬 1", "1", Scope.BATTLE),
        SKILL2("스킬 2", "2", Scope.BATTLE),
        SKILL3("스킬 3", "3", Scope.BATTLE),
        SKILL4("스킬 4", "4", Scope.BATTLE),
        HEAL("회복 메뉴", "Q", Scope.BATTLE),
        AUTO("자동 전투 켜기/끄기", "Z", Scope.BATTLE),
        FLEE("도망", "R", Scope.BATTLE),
        AUGMENTS("보유 증강 보기", "TAB", Scope.BATTLE);

        public final String label;
        public final String defaultKey;
        public final Scope scope;

        Action(String label, String defaultKey, Scope scope) {
            this.label = label;
            this.defaultKey = defaultKey;
            this.scope = scope;
        }
    }

    /** True while the settings page waits for a key to set: the key must not also do anything else. */
    public static volatile boolean capturing;

    private static final int MODS = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK | InputEvent.ALT_DOWN_MASK;

    public static KeyStroke get(Action a) {
        String saved = Settings.getKey(a.name());
        KeyStroke k = saved == null ? null : KeyStroke.getKeyStroke(saved);
        return k != null ? k : KeyStroke.getKeyStroke(a.defaultKey);
    }

    /**
     * Gives the action this key. Any other action that already had it where both would apply gets this
     * action's old key instead (a swap), so nothing is left unreachable. Returns the actions that moved.
     */
    public static List<Action> set(Action a, KeyStroke key) {
        KeyStroke old = get(a);
        List<Action> moved = new ArrayList<>();
        for (Action o : Action.values()) {
            if (o != a && o.scope.overlaps(a.scope) && same(get(o), key)) {
                Settings.setKey(o.name(), text(old));
                moved.add(o);
            }
        }
        Settings.setKey(a.name(), text(key));
        return moved;
    }

    public static void resetAll() {
        for (Action a : Action.values()) Settings.setKey(a.name(), null);
    }

    /** Whether a key press is this action's key. */
    public static boolean matches(Action a, KeyEvent e) {
        return same(get(a), KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiersEx() & MODS));
    }

    static boolean same(KeyStroke x, KeyStroke y) {
        return x != null && y != null && x.getKeyCode() == y.getKeyCode() && (x.getModifiers() & MODS) == (y.getModifiers() & MODS);
    }

    /** The KeyStroke spelling Settings keeps, e.g. "ctrl S". */
    static String text(KeyStroke k) {
        StringBuilder s = new StringBuilder();
        int m = k.getModifiers();
        if ((m & InputEvent.CTRL_DOWN_MASK) != 0) s.append("ctrl ");
        if ((m & InputEvent.ALT_DOWN_MASK) != 0) s.append("alt ");
        if ((m & InputEvent.SHIFT_DOWN_MASK) != 0) s.append("shift ");
        return s + codeName(k.getKeyCode());
    }

    /** The VK name KeyStroke.getKeyStroke(String) reads back, e.g. "ESCAPE", "F12", "SPACE", "A". */
    private static String codeName(int code) {
        for (java.lang.reflect.Field f : KeyEvent.class.getFields()) {
            try {
                if (f.getName().startsWith("VK_") && f.getType() == int.class && f.getInt(null) == code) return f.getName().substring(3);
            } catch (IllegalAccessException ignored) {
                // public static fields: not reached
            }
        }
        return "UNDEFINED";
    }

    /** How a key is written on screen: "Esc", "Ctrl+S", "Space", "F12". */
    public static String label(KeyStroke k) {
        if (k == null) return "없음";
        StringBuilder s = new StringBuilder();
        int m = k.getModifiers();
        if ((m & InputEvent.CTRL_DOWN_MASK) != 0) s.append("Ctrl+");
        if ((m & InputEvent.ALT_DOWN_MASK) != 0) s.append("Alt+");
        if ((m & InputEvent.SHIFT_DOWN_MASK) != 0) s.append("Shift+");
        switch (k.getKeyCode()) {
            case KeyEvent.VK_ESCAPE: return s + "Esc";
            case KeyEvent.VK_SPACE: return s + "Space";
            case KeyEvent.VK_TAB: return s + "Tab";
            case KeyEvent.VK_ENTER: return s + "Enter";
            default: return s + codeName(k.getKeyCode());
        }
    }

    public static String label(Action a) {
        return label(get(a));
    }

    /** Keys that can't be given to an action (modifiers alone, and Enter, which presses a window's main button). */
    public static boolean assignable(KeyEvent e) {
        int c = e.getKeyCode();
        return c != KeyEvent.VK_SHIFT && c != KeyEvent.VK_CONTROL && c != KeyEvent.VK_ALT && c != KeyEvent.VK_META
                && c != KeyEvent.VK_ENTER && c != KeyEvent.VK_UNDEFINED && c != KeyEvent.VK_WINDOWS && c != KeyEvent.VK_CONTEXT_MENU;
    }
}
