package game.ui;

import game.model.Dungeon;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Job;
import game.model.Monster;
import game.model.Player;
import game.model.Skill;
import game.model.World;
import game.save.SaveManager;

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

    /** Chest grade odds per dungeon tier (일반/고급/희귀/영웅/전설), later dungeons weighted toward higher grades. */
    private static final int[][] CHEST_GRADE_WEIGHTS = {
            {70, 25, 5, 0, 0},
            {45, 35, 18, 2, 0},
            {20, 35, 35, 10, 0},
            {5, 20, 40, 30, 5},
            {0, 10, 35, 45, 10},
            {0, 5, 20, 50, 25},
            {0, 0, 10, 40, 50},
    };

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
    private JButton continueButton;
    private MapScreen townMap;
    private MapScreen dungeonMap;
    private JButton dungeonBackButton;

    private int currentDungeonIndex;
    private int currentFloor;
    private boolean bossStage;

    private boolean fullscreen;

    public GameFrame() {
        super("공책 RPG");
        Theme.apply(this);
        setIconImages(loadAppIcons());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 560);
        setMinimumSize(new Dimension(560, 460));
        setLocationRelativeTo(null);

        cards.setOpaque(false);
        cards.add(buildTitlePanel(), "TITLE");
        cards.add(buildTownPanel(), "TOWN");
        cards.add(buildDungeonMapPanel(), "DUNGEON_MAP");
        cards.add(battlePanel, "BATTLE");
        setContentPane(cards);
        setGlassPane(pageTurner);
        pageTurner.setVisible(false);
        bindFullscreenToggle();

        showTitle();
        toggleFullscreen();
        setVisible(true);
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
        pageTurner.turn(() -> cardLayout.show(cards, name), forward);
    }

    private void showTitle() {
        continueButton.setEnabled(SaveManager.hasSave());
        showCard("TITLE", false);
        Theme.focusFirst(titleButtons);
    }

    private void newGame() {
        if (SaveManager.hasSave() && !Dialogs.confirm(this, "새로 시작",
                "저장된 모험이 있습니다.\n새로 시작하면 다음 저장 때 기존 데이터를 덮어씁니다. 계속할까요?")) {
            return;
        }
        createNewPlayer();
        showTown(true);
    }

    private void continueGame() {
        Player loaded = SaveManager.load();
        if (loaded == null) {
            Dialogs.message(this, "이어하기", "저장 데이터를 불러오지 못했습니다.");
            return;
        }
        player = loaded;
        showTown(true);
        townMap.toast(loaded.getName() + "님, 다시 오신 것을 환영합니다!");
    }

    private void backToTitle() {
        if (Dialogs.confirm(this, "타이틀로", "저장하지 않은 진행 상황은 사라집니다.\n타이틀 화면으로 돌아갈까요?")) {
            showTitle();
        }
    }

    private void createNewPlayer() {
        String name = Dialogs.input(this, "새 모험", "주인공의 이름을 입력하세요:", "");
        if (name == null || name.trim().isEmpty()) name = "용사";
        player = new Player(name.trim());
        player.addItem("포션");
        player.addItem("포션");
        Dialogs.message(this, "모험 시작", name.trim() + "의 모험이 시작됩니다!\n\n"
                + "초기 능력치\n"
                + "HP " + player.getMaxHp() + "   MP " + player.getMaxMp()
                + "   공격력 " + player.getBaseAtk() + "   방어력 " + player.getBaseDef()
                + "\n행운 " + player.getLuck() + " (치명타 확률 " + player.getCritChance() + "%)");
    }

    /** Title screen: the old town artwork with the game's name and start/continue/quit. */
    private JPanel buildTitlePanel() {
        BackgroundPanel panel = new BackgroundPanel(new GridBagLayout(), loadImage("images/town_bg.png"));

        JLabel banner = new JLabel("공책 RPG", SwingConstants.CENTER);
        banner.setFont(Theme.dosFont(Font.BOLD, 52));
        banner.setForeground(Color.WHITE);
        JPanel bannerPill = new TranslucentPill();
        bannerPill.setBorder(new EmptyBorder(14, 36, 14, 36));
        bannerPill.add(banner);

        continueButton = makeButton("이어하기", this::continueGame);
        titleButtons = new JButton[] {
                makePrimaryButton("새로 시작", this::newGame),
                continueButton,
                makeButton("종료", () -> System.exit(0))
        };
        JPanel buttonPanel = new JPanel(new GridLayout(titleButtons.length, 1, 10, 10));
        buttonPanel.setOpaque(false);
        for (JButton b : titleButtons) buttonPanel.add(b);
        Theme.arrowNav(titleButtons);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        bannerPill.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(240, buttonPanel.getPreferredSize().height));
        column.add(bannerPill);
        column.add(Box.createVerticalStrut(36));
        column.add(buttonPanel);
        panel.add(column);
        return panel;
    }

    /** Town: click buildings on town.png; character management lives in the side menu. */
    private JPanel buildTownPanel() {
        // The job hall (전직소) is drawn into an empty corner of the town art.
        townMap = new MapScreen(JobHallArt.paintOnto(loadImage("images/town.png")));
        townMap.addHotspot("대장간", 130, 200, 430, 350, () -> townMap.toast(NOT_READY));
        townMap.addHotspot("박물관", 680, 160, 395, 360, () -> townMap.toast(NOT_READY));
        townMap.addHotspot("상점", 725, 630, 345, 340, this::openShop);
        townMap.addHotspot("던전 입구", 860, 990, 330, 260, () -> openDungeonMap(true));
        Rectangle hall = JobHallArt.AREA;
        townMap.addHotspot("전직소", hall.x, hall.y, hall.width, hall.height, this::openAdvancement);

        townInfoLabel = Theme.body("");
        townButtons = new JButton[] {
                menuButton("상태 보기", this::showStatus),
                menuButton("장비 변경", this::openEquip),
                menuButton("스탯 분배", this::openStatAlloc),
                menuButton("저장하기", () -> {
                    SaveManager.save(player);
                    townMap.toast("게임을 저장했습니다.");
                }),
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

    /** Job advancement: toasts why it isn't possible yet, otherwise lets the player pick the next job. */
    private void openAdvancement() {
        Job current = player.getJob();
        List<Job> next = current.children();
        if (next.isEmpty()) {
            townMap.toast(current.getDisplayName() + "의 다음 전직은 아직 준비 중입니다.");
            return;
        }
        List<Job> available = player.getAvailableAdvancements();
        if (available.isEmpty()) {
            townMap.toast("Lv." + next.get(0).getRequiredLevel() + "부터 전직할 수 있습니다. (현재 Lv." + player.getLevel() + ")");
            return;
        }
        pageTurner.turnToBlankPage(() -> {
            String[] options = new String[available.size()];
            for (int i = 0; i < available.size(); i++) {
                Job j = available.get(i);
                StringBuilder skills = new StringBuilder();
                for (Skill s : j.getOwnSkills()) {
                    if (skills.length() > 0) skills.append(", ");
                    skills.append(s.getName()).append("(Lv.").append(s.getRequiredLevel()).append(")");
                }
                options[i] = j.getDisplayName() + "  -  " + j.getDescription() + "   [" + skills + "]";
            }
            int idx = Dialogs.choose(this, "전직", "어떤 길을 걸으시겠습니까?", options);
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
        Dungeon dungeon = dungeons.get(currentDungeonIndex);
        Monster monster = dungeon.randomMonster(rnd);
        // Start the battle inside the swap so the incoming page already shows the monster.
        pageTurner.turn(() -> {
            cardLayout.show(cards, "BATTLE");
            battlePanel.startBattle(player, monster, result -> onFightFinished(result, monster));
        }, true);
    }

    private void startNextFight() {
        Dungeon dungeon = dungeons.get(currentDungeonIndex);
        Monster monster = bossStage ? dungeon.spawnBoss() : dungeon.randomMonster(rnd);
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
        player.gainExp(monster.getExpReward());
        player.earnGold(monster.getGoldReward());
        StringBuilder msg = new StringBuilder("EXP " + monster.getExpReward() + ", 골드 " + monster.getGoldReward() + "G 획득!");
        if (player.getLevel() > beforeLevel) {
            int gained = (player.getLevel() - beforeLevel) * Player.POINTS_PER_LEVEL;
            msg.append("\n레벨 업! Lv.").append(player.getLevel()).append("이 되었습니다! (스탯 포인트 +")
                    .append(gained).append(",마을에서 분배하세요)");
            appendGrowthNotes(msg, beforeSkills, couldAdvance);
            battlePanel.showLevelUp();
        }

        if (!bossStage) {
            if (currentFloor >= dungeon.getFloors()) {
                battlePanel.logResult(msg + "\n>>> 보스 등장! <<<");
                bossStage = true;
            } else {
                battlePanel.logResult(msg.toString());
                currentFloor++;
            }
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

        Item chestItem = rollChestItem(currentDungeonIndex);
        player.addItem(chestItem.getName());
        finalMsg.append("\n보물 상자를 발견했다! ").append(chestItem.describe()).append(" 획득!");

        if (currentDungeonIndex == player.getUnlockedDungeon() && currentDungeonIndex + 1 < dungeons.size()) {
            player.setUnlockedDungeon(currentDungeonIndex + 1);
            finalMsg.append("\n다음 던전이 개방되었습니다. (").append(dungeons.get(currentDungeonIndex + 1).getName()).append(")");
        } else if (currentDungeonIndex == dungeons.size() - 1) {
            finalMsg.append("\n*** 축하합니다! 모든 던전을 클리어했습니다! ***");
        }

        endDungeonRun(finalMsg.toString());
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

    private Item rollChestItem(int dungeonIndex) {
        int[] weights = CHEST_GRADE_WEIGHTS[Math.min(dungeonIndex, CHEST_GRADE_WEIGHTS.length - 1)];
        int total = 0;
        for (int w : weights) total += w;
        int roll = rnd.nextInt(total);
        Item.Grade[] grades = Item.Grade.values();
        Item.Grade chosenGrade = grades[grades.length - 1];
        int acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (roll < acc) {
                chosenGrade = grades[i];
                break;
            }
        }
        List<Item> candidates = ItemCatalog.equipableOfGrade(chosenGrade);
        return candidates.get(rnd.nextInt(candidates.size()));
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
        battlePanel.endRun(outcome + "\n던전을 나서며 MP가 모두 회복되었다.", "던전 지도로", this::returnToDungeonMap);
    }

    /** After a cleared or abandoned run: back on the world map, so the next dungeon is one click away. */
    private void returnToDungeonMap() {
        refreshTown();
        openDungeonMap(false);
    }

    private void showTown(boolean forward) {
        refreshTown();
        showCard("TOWN", forward);
        Theme.focusFirst(townButtons);
    }
}
