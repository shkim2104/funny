package game.ui;

import game.model.Dungeon;
import game.model.FloorReward;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Job;
import game.model.Material;
import game.model.Monster;
import game.model.Augment;
import game.model.Player;
import game.model.Quest;
import game.model.RunState;
import game.model.Skill;
import game.model.World;
import game.save.SaveManager;
import game.save.Settings;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameFrame extends JFrame {
    private final Random rnd = new Random();
    private final List<Dungeon> dungeons = World.buildDungeons();

    private Player player;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);
    private final BattlePanel battlePanel = new BattlePanel();
    private final PageTurner pageTurner = new PageTurner(this);

    /** Clickable regions on dungeonentrence.png (artwork pixels), in the same order as World.buildDungeons(). */
    private static final int[][] DUNGEON_MAP_AREAS = {
            {60, 600, 410, 260},    // 1 초원
            {470, 700, 260, 300},   // 2 어두운 동굴
            {490, 460, 330, 230},   // 3 잊혀진 폐허
            {860, 360, 340, 280},   // 4 마왕성
            {760, 690, 370, 320},   // 5 얼음 협곡
            {180, 220, 380, 300},   // 6 불화산
            {450, 20, 450, 240},    // 7 천공의 성채
    };
    private static final String NOT_READY = "아직 개발중인 컨텐츠입니다.";

    private JLabel townInfoLabel;
    private JButton[] townButtons;
    private JButton[] titleButtons;
    private CharacterSelectPanel characterSelect;
    /** File id of the character being played; 저장하기 writes back to it. */
    private String currentSaveId;
    private MapScreen townMap;
    private MapScreen dungeonMap;
    private JButton dungeonBackButton;

    private int currentDungeonIndex;
    private int currentFloor;
    private boolean bossStage;
    /** Augments etc. of the dungeon run in progress; null outside a dungeon. */
    private RunState run;

    private boolean fullscreen;

    public GameFrame() {
        super("공책 RPG");
        Theme.apply(this);
        setIconImages(loadAppIcons());
        // Closing the window goes through exitGame() so the auto-save setting is honored.
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                exitGame();
            }
        });
        setSize(680, 560);
        setMinimumSize(new Dimension(560, 460));
        setLocationRelativeTo(null);

        cards.setOpaque(false);
        cards.add(buildTitlePanel(), "TITLE");
        cards.add(buildCharacterSelectPanel(), "SELECT");
        cards.add(buildTownPanel(), "TOWN");
        cards.add(buildDungeonMapPanel(), "DUNGEON_MAP");
        cards.add(battlePanel, "BATTLE");
        setContentPane(cards);
        setGlassPane(pageTurner);
        pageTurner.setVisible(false);
        bindFullscreenToggle();
        bindHotkeys();

        showTitle();
        if (Settings.isStartFullscreen()) toggleFullscreen();
        setVisible(true);
    }

    /**
     * 단축키 (when the setting is on): 1~4 skills and Tab (보유 증강) in battle, I 장비 / S 스탯 분배 in town,
     * Esc 설정 in town and on the maps. Keys only act on the screen they belong to.
     */
    private void bindHotkeys() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();
        for (int i = 0; i < 4; i++) {
            final int slot = i;
            hotkey(im, am, String.valueOf(i + 1), "BATTLE", () -> battlePanel.hotkeySkill(slot));
        }
        hotkey(im, am, "TAB", "BATTLE", () -> {
            if (Settings.isAugmentListKey()) battlePanel.showRunInfo();
        });
        hotkey(im, am, "I", "TOWN", this::openEquip);
        hotkey(im, am, "S", "TOWN", this::openStatAlloc);
        hotkey(im, am, "ESCAPE", "TOWN", this::openSettings);
        hotkey(im, am, "ESCAPE", "DUNGEON_MAP", this::openSettings);
        // Tab normally moves focus; let the window see it instead.
        setFocusTraversalKeysEnabled(false);
    }

    private final java.util.Map<String, java.util.Map<String, Runnable>> hotkeys = new java.util.HashMap<>();

    private void hotkey(InputMap im, ActionMap am, String key, String screen, Runnable action) {
        hotkeys.computeIfAbsent(key, k -> new java.util.HashMap<>()).put(screen, action);
        im.put(KeyStroke.getKeyStroke(key), "hotkey-" + key);
        am.put("hotkey-" + key, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                // Tab (보유 증강) has its own setting; the rest follow 단축키.
                if (!key.equals("TAB") && !Settings.isHotkeys()) return;
                Runnable r = hotkeys.get(key).get(currentCard);
                if (r != null && player != null) r.run();
            }
        });
    }

    /** Binds F11 to toggle maximized mode, a common convenience shortcut. */
    private void bindFullscreenToggle() {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("F11"), "toggleFullscreen");
        root.getActionMap().put("toggleFullscreen", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                toggleFullscreen();
            }
        });
    }

    // Maximizes via setExtendedState rather than a dispose()+setUndecorated() borderless toggle:
    // that approach turned out to (1) auto-minimize whenever the window lost focus in true
    // exclusive full-screen mode, and (2) occasionally let AWT see zero displayable windows
    // mid-toggle and tear down the whole JVM. Plain maximize keeps the title bar but never
    // disposes the frame, so neither failure mode can happen.
    private void toggleFullscreen() {
        setExtendedState(fullscreen ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
        fullscreen = !fullscreen;
    }

    /** Switches screens with a notebook page turn: forward flips the page away, back lays the page down again. */
    private void showCard(String name, boolean forward) {
        currentCard = name;
        pageTurner.turn(() -> cardLayout.show(cards, name), forward);
    }

    /** Which screen is showing (TITLE / SELECT / TOWN / DUNGEON_MAP / BATTLE), for the hotkeys. */
    private String currentCard = "TITLE";

    private void showTitle() {
        Music.play("title");
        showCard("TITLE", false);
        Theme.focusFirst(titleButtons);
    }

    /** Character select screen, with selectId (if any) pre-selected. */
    private void openCharacterSelect(String selectId) {
        characterSelect.refresh(SaveManager.listCharacters(), selectId);
        showCard("SELECT", true);
    }

    private void startCharacter(String id, Player loaded) {
        player = loaded;
        currentSaveId = id;
        battlePanel.setPlayerArt(CharacterArt.load(id));
        showTown(true);
        townMap.toast(loaded.getName() + "님, 다시 오신 것을 환영합니다!");
    }

    /** Makes a new character in an empty slot and saves it right away, so it shows up on the select screen. */
    private void createCharacter() {
        if (SaveManager.listCharacters().size() >= SaveManager.MAX_CHARACTERS) {
            Dialogs.message(this, "캐릭터 생성", "캐릭터는 최대 " + SaveManager.MAX_CHARACTERS + "명까지 만들 수 있습니다.");
            return;
        }
        CharacterArt art = createNewPlayer();
        if (art == null) return;
        String id = SaveManager.newId();
        SaveManager.save(player, id);
        art.save(id);
        characterSelect.refresh(SaveManager.listCharacters(), id);
    }

    /** 다시 그리기 on the select screen: redraw a saved character, starting from their current drawing. */
    private void redrawCharacter(String id, Player p) {
        CharacterArt art = DrawCharacterDialog.edit(this, p.getName(), CharacterArt.load(id));
        if (art == null) return;
        art.save(id);
        characterSelect.refresh(SaveManager.listCharacters(), id);
    }

    private void deleteCharacter(String id, Player p) {
        if (!Dialogs.confirm(this, "캐릭터 삭제", p.getName() + " (Lv." + p.getLevel() + " " + p.getJob().getDisplayName()
                + ")을(를) 삭제할까요?\n삭제한 캐릭터는 되돌릴 수 없습니다.")) {
            return;
        }
        SaveManager.delete(id);
        characterSelect.refresh(SaveManager.listCharacters(), null);
    }

    /** With 자동 저장 on, goes back right after saving; otherwise warns that unsaved progress is lost. */
    private void backToTitle() {
        if (Settings.isAutoSaveOnExit()) {
            autoSave();
        } else if (!Dialogs.confirm(this, "타이틀로", "저장하지 않은 진행 상황은 사라집니다.\n타이틀 화면으로 돌아갈까요?")) {
            return;
        }
        player = null;
        currentSaveId = null;
        showTitle();
    }

    /** Window close (X) and 종료: save the character in play first when 자동 저장 is on. */
    private void exitGame() {
        if (Settings.isAutoSaveOnExit()) autoSave();
        System.exit(0);
    }

    /** Saves the character being played, if there is one (nothing to save on the title screen). */
    private void autoSave() {
        if (player != null && currentSaveId != null) SaveManager.save(player, currentSaveId);
    }

    private void openSettings() {
        SettingsDialog.show(this);
    }

    /** The player names and draws their hero, then a new player is rolled; null if they cancelled. */
    private CharacterArt createNewPlayer() {
        DrawCharacterDialog.Result made = DrawCharacterDialog.create(this);
        if (made == null) return null;
        String name = made.name.trim().isEmpty() ? "용사" : made.name.trim();
        player = new Player(name);
        player.addItem("포션");
        player.addItem("포션");
        Dialogs.message(this, "모험 시작", name + "의 모험이 시작됩니다!\n\n"
                + "초기 능력치\n"
                + "HP " + player.getMaxHp() + "   MP " + player.getMaxMp()
                + "   공격력 " + player.getBaseAtk() + "   방어력 " + player.getBaseDef()
                + "\n행운 " + player.getLuck() + " (치명타 확률 " + player.getCritChance() + "%)");
        return made.art;
    }

    /** Character select over the same town artwork as the title screen. */
    private JPanel buildCharacterSelectPanel() {
        characterSelect = new CharacterSelectPanel(new CharacterSelectPanel.Listener() {
            @Override public void start(String id, Player p) { startCharacter(id, p); }
            @Override public void create() { createCharacter(); }
            @Override public void delete(String id, Player p) { deleteCharacter(id, p); }
            @Override public void redraw(String id, Player p) { redrawCharacter(id, p); }
            @Override public void back() { showTitle(); }
        });
        BackgroundPanel panel = new BackgroundPanel(new BorderLayout(), loadImage("images/town_bg.png"));
        panel.add(characterSelect, BorderLayout.CENTER);
        return panel;
    }

    /** Title screen: the pencil title scene, the game's name on a paper scrap, start (→ character select) and quit. */
    private JPanel buildTitlePanel() {
        BackgroundPanel panel = new BackgroundPanel(new GridBagLayout(), loadImage("images/town_bg.png"));

        JLabel banner = Sketch.title("공책 RPG");
        banner.setFont(Sketch.hand(Font.BOLD, 56f));
        banner.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel bannerPill = new Sketch.Note(new BorderLayout(), 9701);
        bannerPill.setBorder(new EmptyBorder(18, 44, 14, 44));
        bannerPill.add(banner, BorderLayout.CENTER);
        JLabel sub = Sketch.label("내가 그린 용사의 공책 속 모험", Font.PLAIN, 15f, Sketch.INK_SOFT);
        sub.setHorizontalAlignment(SwingConstants.CENTER);
        bannerPill.add(sub, BorderLayout.SOUTH);

        titleButtons = new JButton[] {
                sketchButton("게임 시작", true, () -> openCharacterSelect(null)),
                sketchButton("설정", false, this::openSettings),
                sketchButton("종료", false, this::exitGame)
        };
        JPanel buttonPanel = new Sketch.Note(new GridLayout(titleButtons.length, 1, 10, 8), 9702);
        buttonPanel.setBorder(new EmptyBorder(14, 22, 14, 22));
        for (JButton b : titleButtons) buttonPanel.add(b);
        Theme.arrowNav(titleButtons);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        bannerPill.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(260, buttonPanel.getPreferredSize().height));
        // Wrapped in a FlowLayout so the scrap keeps its natural size (a fixed size from before the window
        // was shown could clip the title).
        JPanel bannerWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bannerWrap.setOpaque(false);
        bannerWrap.add(bannerPill);
        bannerWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(bannerWrap);
        column.add(Box.createVerticalStrut(36));
        column.add(buttonPanel);
        panel.add(column);
        return panel;
    }

    /** Town: click buildings on town.png; character management lives in the side menu. */
    private JPanel buildTownPanel() {
        // The job hall (전직소) is drawn into the pencil town art itself; JobHallArt.AREA still marks its hotspot.
        townMap = new MapScreen(loadImage("images/town.png"));
        townMap.addHotspot("대장간", 130, 200, 430, 350, this::openBlacksmith);
        townMap.addHotspot("박물관", 680, 160, 395, 360, () -> townMap.toast(NOT_READY));
        townMap.addHotspot("상점", 725, 630, 345, 340, this::openShop);
        townMap.addHotspot("던전 입구", 860, 990, 330, 260, () -> openDungeonMap(true));
        Rectangle hall = JobHallArt.AREA;
        townMap.addHotspot("전직소", hall.x, hall.y, hall.width, hall.height, this::openAdvancement);
        townMap.addHotspot("여관", 100, 880, 190, 180, this::openInn);
        townMap.addHotspot("의뢰 게시판", 108, 718, 202, 165, this::openQuestBoard);
        townMap.addHotspot("보물 뽑기", 450, 815, 200, 150, this::openTreasure);

        townInfoLabel = Theme.body("");
        townButtons = new JButton[] {
                menuButton("상태 보기", this::showStatus),
                menuButton("장비 변경", this::openEquip),
                menuButton("스탯 분배", this::openStatAlloc),
                menuButton("저장하기", () -> {
                    SaveManager.save(player, currentSaveId);
                    townMap.toast("게임을 저장했습니다.");
                }),
                menuButton("설정", this::openSettings),
                menuButton("타이틀로", this::backToTitle)
        };
        return mapWithSideMenu(townMap, "마을", townInfoLabel, "건물을 클릭해 이동하세요", townButtons);
    }

    /** Dungeon select: click a region on the world map; regions past the unlocked one are dimmed. */
    private JPanel buildDungeonMapPanel() {
        dungeonMap = new MapScreen(loadImage("images/dungeonentrence.png"));
        for (int i = 0; i < Math.min(DUNGEON_MAP_AREAS.length, dungeons.size()); i++) {
            final int idx = i;
            int[] a = DUNGEON_MAP_AREAS[i];
            Dungeon d = dungeons.get(i);
            dungeonMap.addHotspot(d.getName() + "  (" + d.getFloors() + "층 + 보스)", a[0], a[1], a[2], a[3],
                    () -> tryEnterDungeon(idx),
                    () -> player != null && idx > player.getUnlockedDungeon());
        }
        dungeonBackButton = menuButton("마을로 돌아가기", () -> showTown(false));
        return mapWithSideMenu(dungeonMap, "던전 입구", null, "지역을 클릭해 입장하세요", dungeonBackButton);
    }

    /** Map on the left, a slim menu column on the right (title, optional info, hint, buttons). */
    private JPanel mapWithSideMenu(MapScreen map, String title, JLabel info, String hint, JButton... buttons) {
        JPanel side = new JPanel();
        side.setBackground(Theme.PANEL);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new EmptyBorder(24, 20, 24, 20));

        JLabel titleLabel = Theme.title(title);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(titleLabel);
        side.add(Box.createVerticalStrut(12));
        if (info != null) {
            info.setAlignmentX(Component.LEFT_ALIGNMENT);
            side.add(info);
            side.add(Box.createVerticalStrut(12));
        }
        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(Theme.SMALL_FONT);
        hintLabel.setForeground(Theme.TEXT_DIM);
        hintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        side.add(hintLabel);
        side.add(Box.createVerticalStrut(20));

        for (JButton b : buttons) {
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
            side.add(b);
            side.add(Box.createVerticalStrut(10));
        }
        side.add(Box.createVerticalGlue());
        Theme.arrowNav(buttons);
        side.setPreferredSize(new Dimension(220, 0));

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.BG);
        panel.add(map, BorderLayout.CENTER);
        panel.add(side, BorderLayout.EAST);
        return panel;
    }

    /** Paints a bundled image scaled to cover the panel (cropping overflow), falling back to a flat color if missing. */
    private static class BackgroundPanel extends JPanel {
        private final Image background;

        BackgroundPanel(LayoutManager layout, Image background) {
            super(layout);
            this.background = background;
            setOpaque(true);
            setBackground(Theme.BG);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (background == null) return;
            int pw = getWidth(), ph = getHeight();
            int iw = background.getWidth(this), ih = background.getHeight(this);
            if (iw <= 0 || ih <= 0) return;
            double scale = Math.max(pw / (double) iw, ph / (double) ih);
            int sw = (int) Math.ceil(iw * scale);
            int sh = (int) Math.ceil(ih * scale);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(background, (pw - sw) / 2, (ph - sh) / 2, sw, sh, this);
            g2.dispose();
        }
    }

    /** A soft translucent dark rounded backing, so overlaid text stays readable on any background art. */
    private static class TranslucentPill extends JPanel {
        TranslucentPill() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(10, 12, 16, 140));
            g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Loads a bundled resource image from the classpath, relative to this class's package. */
    private static Image loadImage(String path) {
        try (InputStream in = GameFrame.class.getResourceAsStream(path)) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {
            // Missing/unreadable resource: caller falls back to a flat background.
        }
        return null;
    }

    private JButton sketchButton(String text, boolean primary, Runnable action) {
        JButton btn = new Sketch.Button(text, primary);
        btn.setFont(Sketch.hand(Font.BOLD, 18f));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JButton makeButton(String text, Runnable action) {
        JButton btn = Theme.ghostButton(text);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JButton makePrimaryButton(String text, Runnable action) {
        JButton btn = Theme.ghostPrimaryButton(text);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private JButton menuButton(String text, Runnable action) {
        JButton btn = Theme.button(text);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void refreshTown() {
        String statPointsNote = player.getStatPoints() > 0
                ? "<br><font color='#c7a86a'>스탯 포인트 " + player.getStatPoints() + "</font>"
                : "";
        townInfoLabel.setText("<html>Lv." + player.getLevel() + " " + player.getJob().getDisplayName() + " " + player.getName()
                + "<br>HP " + player.getHp() + "/" + player.getMaxHp()
                + "<br>골드 " + player.getGold() + "G"
                + "<br>흑연 " + player.getMaterial(Material.GRAPHITE) + " · 심 " + player.getMaterial(Material.HARD_LEAD)
                + " · 가루 " + player.getMaterial(Material.ERASER_DUST)
                + statPointsNote + "</html>");
    }

    // Town windows open on a fresh notebook page and turn back to the town when closed.
    private void showStatus() {
        pageTurner.turnToBlankPage(() -> StatusDialog.show(this, player));
    }

    private void openStatAlloc() {
        pageTurner.turnToBlankPage(() -> {
            StatAllocDialog.show(this, player);
            refreshTown();
        });
    }

    /**
     * Job advancement. Any job of the next tier can be picked, from any class, so paths like
     * 1차 전사 → 2차 궁수 → 3차 마법사 are allowed; toasts explain when nothing can be picked yet.
     */
    private void openAdvancement() {
        int tier = player.getNextTier();
        if (tier == 0) {
            townMap.toast("모든 전직을 마쳤습니다. (" + player.getJob().getDisplayName() + ")");
            return;
        }
        if (player.getLevel() < Job.levelForTier(tier)) {
            townMap.toast(tier + "차 전직은 Lv." + Job.levelForTier(tier) + "부터 할 수 있습니다. (현재 Lv." + player.getLevel() + ")");
            return;
        }
        List<Job> available = player.getAvailableAdvancements();
        if (available.isEmpty()) {
            townMap.toast(tier + "차 전직 직업은 아직 준비 중입니다.");
            return;
        }
        pageTurner.turnToBlankPage(() -> {
            String[] options = new String[available.size()];
            for (int i = 0; i < available.size(); i++) {
                Job j = available.get(i);
                StringBuilder skills = new StringBuilder();
                for (Skill s : j.getSkills()) {
                    if (skills.length() > 0) skills.append(", ");
                    skills.append(s.getName());
                    if (s.getRequiredLevel() > j.getRequiredLevel()) skills.append(" (Lv.").append(s.getRequiredLevel()).append(")");
                }
                String focus = j.getFocus().isEmpty() ? "" : "  <font color='#c7a86a'>" + j.getFocus() + "</font>";
                options[i] = "<html><b>[" + j.getJobClass() + "] " + j.getDisplayName() + "</b>" + focus
                        + "<br><font color='#8c8f9a'>" + j.getDescription() + "</font>"
                        + "<br>스킬: " + skills + "</html>";
            }
            int idx = Dialogs.choose(this, tier + "차 전직",
                    tier + "차 전직 — 어떤 길을 이어 가시겠습니까? (직업군 제한 없음)", options, 560);
            if (idx < 0) return;
            Job chosen = available.get(idx);
            if (!Dialogs.confirm(this, "전직", chosen.getDisplayName() + "(으)로 전직할까요?\n한 번 정하면 바꿀 수 없습니다.")) return;
            player.advanceTo(chosen);
            refreshTown();
            StringBuilder learned = new StringBuilder();
            for (Skill s : player.getSkills()) {
                if (learned.length() > 0) learned.append(", ");
                learned.append(s.getName());
            }
            Dialogs.message(this, "전직 완료", player.getName() + "은(는) " + chosen.getDisplayName() + "이(가) 되었다!"
                    + (learned.length() > 0 ? "\n\n사용 가능한 스킬: " + learned : ""));
        });
    }

    /** 여관: pay to rest (full HP/MP); resting also saves, like a save point. */
    private void openInn() {
        pageTurner.turnToBlankPage(() -> {
            boolean rested = TownDialogs.inn(this, player, () -> SaveManager.save(player, currentSaveId));
            refreshTown();
            if (rested) townMap.toast("푹 쉬었다! HP와 MP가 모두 회복되고 모험이 기록되었다.");
        });
    }

    private void openQuestBoard() {
        pageTurner.turnToBlankPage(() -> {
            TownDialogs.questBoard(this, player, dungeons, rnd);
            refreshTown();
        });
    }

    private void openTreasure() {
        pageTurner.turnToBlankPage(() -> {
            TownDialogs.treasure(this, player, rnd);
            refreshTown();
        });
    }

    private void openBlacksmith() {
        pageTurner.turnToBlankPage(() -> {
            new BlacksmithDialog(this, player, rnd).setVisible(true);
            refreshTown();
        });
    }

    private void openShop() {
        pageTurner.turnToBlankPage(() -> {
            new ShopDialog(this, player).setVisible(true);
            refreshTown();
        });
    }

    private void openEquip() {
        pageTurner.turnToBlankPage(() -> {
            new EquipDialog(this, player).setVisible(true);
            refreshTown();
        });
    }

    private void openDungeonMap(boolean forward) {
        Music.play("town");
        showCard("DUNGEON_MAP", forward);
        Theme.focusFirst(dungeonBackButton);
    }

    /** Only the next dungeon after the last one cleared (and everything before it) can be entered. */
    private void tryEnterDungeon(int dungeonIndex) {
        if (dungeonIndex > player.getUnlockedDungeon()) {
            dungeonMap.toast("아직 때가 아니다..");
            return;
        }
        enterDungeon(dungeonIndex);
    }

    private void enterDungeon(int dungeonIndex) {
        currentDungeonIndex = dungeonIndex;
        currentFloor = 1;
        bossStage = false;
        // Each dungeon gets its own song as they are made; until then they share 1던전's.
        Music.play("dungeon" + (dungeonIndex + 1), "dungeon1");
        run = new RunState(dungeonIndex);
        battlePanel.setRun(run);
        battlePanel.setBossFight(false);
        Dungeon dungeon = dungeons.get(currentDungeonIndex);
        Monster monster = dungeon.randomMonster(rnd);
        // Start the battle inside the swap so the incoming page already shows the monster.
        pageTurner.turn(() -> {
            cardLayout.show(cards, "BATTLE");
            currentCard = "BATTLE";
            battlePanel.setDungeon(currentDungeonIndex);
            battlePanel.startBattle(player, monster, result -> onFightFinished(result, monster));
        }, true);
    }

    private void startNextFight() {
        Dungeon dungeon = dungeons.get(currentDungeonIndex);
        Monster monster = bossStage ? dungeon.spawnBoss() : dungeon.randomMonster(rnd);
        if (bossStage) Music.play("boss");
        battlePanel.setBossFight(bossStage);
        battlePanel.nextBattle(monster, result -> onFightFinished(result, monster));
    }

    private void onFightFinished(BattlePanel.Result result, Monster monster) {
        Dungeon dungeon = dungeons.get(currentDungeonIndex);

        if (result == BattlePanel.Result.LOSE) {
            handleDefeat();
            return;
        }
        if (result == BattlePanel.Result.FLEE) {
            endDungeonRun(bossStage ? "보스에게서 도망쳤다. 던전 클리어에 실패했다." : "던전에서 물러난다.");
            return;
        }

        int beforeLevel = player.getLevel();
        List<Skill> beforeSkills = player.getSkills();
        boolean couldAdvance = !player.getAvailableAdvancements().isEmpty();
        // Quest board progress: every kill counts toward matching hunts / boss requests.
        for (Quest q : player.recordKill(monster.getName())) {
            battlePanel.logResult("[의뢰 완료] " + q.getTitle() + " — 마을 의뢰 게시판에서 보상을 받으세요.");
        }
        int expGain = run.exp(monster.getExpReward()), goldGain = run.gold(monster.getGoldReward());
        player.gainExp(expGain);
        player.earnGold(goldGain);
        int graphite = bossStage ? 0 : Material.rollKillGraphite(currentDungeonIndex, player.getLuck(), rnd);
        player.addMaterial(Material.GRAPHITE, graphite);
        StringBuilder msg = new StringBuilder("EXP " + expGain + ", 골드 " + goldGain + "G"
                + (graphite > 0 ? ", " + Material.GRAPHITE.getLabel() + " " + graphite + "개" : "") + " 획득!");
        if (player.getLevel() > beforeLevel) {
            int gained = (player.getLevel() - beforeLevel) * Player.POINTS_PER_LEVEL;
            msg.append("\n레벨 업! Lv.").append(player.getLevel()).append("이 되었습니다! (스탯 포인트 +")
                    .append(gained).append(",마을에서 분배하세요)");
            appendGrowthNotes(msg, beforeSkills, couldAdvance);
            battlePanel.showLevelUp();
        }

        if (!bossStage) {
            int clearedFloor = currentFloor;
            if (currentFloor >= dungeon.getFloors()) {
                battlePanel.logResult(msg + "\n>>> 보스 등장! <<<");
                bossStage = true;
            } else {
                battlePanel.logResult(msg.toString());
                currentFloor++;
            }
            if (RunState.isAugmentFloor(clearedFloor)) offerAugment();
            else giveFloorReward(dungeon);
            startNextFight();
            return;
        }

        StringBuilder finalMsg = new StringBuilder("=== 던전 클리어! ===\n보스전 승리! " + msg);
        if (dungeon.getRewardGold() > 0 || dungeon.getRewardExp() > 0) {
            player.gainExp(dungeon.getRewardExp());
            player.earnGold(dungeon.getRewardGold());
            finalMsg.append("\n").append(dungeon.getName()).append(" 클리어 보상! EXP ")
                    .append(dungeon.getRewardExp()).append(", 골드 ").append(dungeon.getRewardGold()).append("G 추가 획득!");
        }
        finalMsg.append("\n강화 재화 획득! ").append(rollBossMaterials());

        Item chestItem = ItemCatalog.rollChest(currentDungeonIndex, rnd);
        player.addItem(chestItem.getName());
        finalMsg.append("\n보물 상자를 발견했다! ").append(chestItem.describe()).append(" 획득!");
        autoEquip(chestItem.getName(), finalMsg);

        // The boss's own gear: always one, preferring pieces not collected yet.
        Item bossItem = ItemCatalog.rollBossDrop(currentDungeonIndex, player, rnd);
        if (bossItem != null) {
            player.addItem(bossItem.getName());
            finalMsg.append("\n").append(monster.getName()).append("의 전리품! ").append(bossItem.describe())
                    .append(" — ").append(bossItem.getEffectText());
            AugmentDialog.showLoot(this, monster.getName(), bossItem);
            autoEquip(bossItem.getName(), finalMsg);
        }

        if (currentDungeonIndex == player.getUnlockedDungeon() && currentDungeonIndex + 1 < dungeons.size()) {
            player.setUnlockedDungeon(currentDungeonIndex + 1);
            finalMsg.append("\n다음 던전이 개방되었습니다. (").append(dungeons.get(currentDungeonIndex + 1).getName()).append(")");
        } else if (currentDungeonIndex == dungeons.size() - 1) {
            finalMsg.append("\n*** 축하합니다! 모든 던전을 클리어했습니다! ***");
        }

        endDungeonRun(finalMsg.toString());
    }

    /** Gives this dungeon boss's materials and returns them as one line, e.g. "흑연 조각 5개, 단단한 심 1개". */
    private String rollBossMaterials() {
        int[] counts = {Material.bossGraphite(currentDungeonIndex), Material.bossHardLead(currentDungeonIndex), Material.rollBossEraser(rnd)};
        Material[] kinds = {Material.GRAPHITE, Material.HARD_LEAD, Material.ERASER_DUST};
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < kinds.length; i++) {
            if (counts[i] <= 0) continue;
            player.addMaterial(kinds[i], counts[i]);
            parts.add(kinds[i].getLabel() + " " + counts[i] + "개");
        }
        return String.join(", ", parts);
    }

    /** After a level-up: announce newly learned skills and a newly reachable job advancement. */
    private void appendGrowthNotes(StringBuilder msg, List<Skill> beforeSkills, boolean couldAdvance) {
        for (Skill s : player.getSkills()) {
            if (!beforeSkills.contains(s)) msg.append("\n새로운 스킬을 배웠다: ").append(s.getName());
        }
        if (!couldAdvance && !player.getAvailableAdvancements().isEmpty()) {
            msg.append("\n>>> 전직할 수 있게 되었습니다! 마을의 전직소를 찾아가 보세요. <<<");
        }
    }

    /** Loads the bundled multi-resolution app icon (title bar, taskbar, alt-tab). */
    private static List<Image> loadAppIcons() {
        List<Image> icons = new ArrayList<>();
        for (String size : new String[] {"16", "32", "48", "128", "256"}) {
            Image icon = loadImage("icons/icon_" + size + ".png");
            if (icon != null) icons.add(icon);
        }
        return icons;
    }

    private void handleDefeat() {
        int penalty = player.getGold() / 4;
        player.spendGold(penalty);
        player.reviveAtTown();
        endDungeonRun("정신을 잃고 던전 밖으로 실려나왔다...\n골드 " + penalty + "G를 잃었다.");
    }

    /** Every way out of a dungeon (clear, retreat, defeat) ends here: MP is fully restored on the way out. */
    private void endDungeonRun(String outcome) {
        player.restoreMp(player.getMaxMp());
        String fade = run != null && !run.getAugments().isEmpty() ? "\n증강 효과가 모두 사라졌다." : "";
        run = null;
        battlePanel.setRun(null);
        battlePanel.endRun(outcome + "\n던전을 나서며 MP가 모두 회복되었다." + fade, "던전 지도로", this::returnToDungeonMap);
    }

    /** A floor without an augment: one random find, shown in the log so 자동 전투 keeps going. */
    private void giveFloorReward(Dungeon dungeon) {
        FloorReward reward = FloorReward.give(currentDungeonIndex, dungeon, player, rnd);
        StringBuilder note = new StringBuilder("[발견] " + reward.getText() + " 획득!");
        if (reward.getGearName() != null) autoEquip(reward.getGearName(), note);
        battlePanel.logResult(note.toString());
    }

    /** After a kill on an augment floor: pick one of three augments for the rest of this run. */
    private void offerAugment() {
        Augment a = AugmentDialog.pick(this, run, player, rnd);
        if (a == null) return;
        run.take(a);
        battlePanel.logResult("[" + a.getTier().getLabel() + " 증강] " + a.getName() + " — " + a.getDescription());
    }

    /** After a cleared or abandoned run: back on the world map, so the next dungeon is one click away. */
    private void returnToDungeonMap() {
        refreshTown();
        openDungeonMap(false);
    }

    private void showTown(boolean forward) {
        Music.play("town");
        List<String> notes = townArrival();
        refreshTown();
        showCard("TOWN", forward);
        Theme.focusFirst(townButtons);
        if (!notes.isEmpty()) townMap.toast(String.join("  ·  ", notes));
    }

    /** 새 장비 자동 장착: wears the piece if it beats the current one, and says so in the run's message. */
    private void autoEquip(String itemName, StringBuilder msg) {
        if (Settings.isAutoEquip() && player.equipIfBetter(itemName)) msg.append("\n").append(itemName).append("을(를) 바로 장착했다!");
    }

    /** Coming back to town: 일반 장비 자동 판매 and 의뢰 완료 알림, as short notes for one toast. */
    private List<String> townArrival() {
        List<String> notes = new ArrayList<>();
        if (player == null) return notes;
        if (Settings.isAutoSellCommon()) {
            int[] sold = player.sellCommonGear();
            if (sold[0] > 0) notes.add("[일반] 장비 " + sold[0] + "개를 " + sold[1] + "G에 팔았어요");
        }
        if (Settings.isQuestNotify()) {
            long done = player.getQuests().stream().filter(Quest::isDone).count();
            if (done > 0) notes.add("끝낸 의뢰가 " + done + "개 있어요! 의뢰 게시판에서 보상을 받으세요");
        }
        return notes;
    }
}
