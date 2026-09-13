package game.ui;

import game.model.Dungeon;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Monster;
import game.model.Player;
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

    private JLabel townInfoLabel;
    private JButton[] townButtons;

    private int currentDungeonIndex;
    private int currentFloor;
    private boolean bossStage;

    public GameFrame() {
        super("공책 RPG");
        Theme.apply(this);
        setIconImages(loadAppIcons());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(680, 560);
        setMinimumSize(new Dimension(560, 460));
        setLocationRelativeTo(null);

        cards.setOpaque(false);
        cards.add(buildTownPanel(), "TOWN");
        cards.add(battlePanel, "BATTLE");
        setContentPane(cards);

        startGame();
        setVisible(true);
    }

    private void startGame() {
        if (SaveManager.hasSave()) {
            boolean load = Dialogs.confirm(this, "이어하기", "이어하기 데이터가 있습니다. 불러올까요?");
            if (load) {
                Player loaded = SaveManager.load();
                if (loaded != null) {
                    player = loaded;
                    Dialogs.message(this, "환영합니다", loaded.getName() + "님, 다시 오신 것을 환영합니다!");
                }
            }
        }
        if (player == null) {
            createNewPlayer();
        }
        refreshTown();
        cardLayout.show(cards, "TOWN");
        Theme.focusFirst(townButtons);
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

    private JPanel buildTownPanel() {
        DragonBackgroundPanel panel = new DragonBackgroundPanel(new BorderLayout(16, 16));
        panel.setBorder(new EmptyBorder(24, 32, 24, 32));

        JLabel banner = new JLabel("공책 RPG — 마을");
        banner.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        banner.setForeground(new Color(150, 140, 185));
        panel.add(banner, BorderLayout.NORTH);

        // The player info bar is kept updated (townInfoLabel) but not shown, so the artwork stays clear.
        townInfoLabel = Theme.header("");

        JPanel buttonPanel = new JPanel(new GridLayout(7, 1, 10, 10));
        buttonPanel.setOpaque(false);
        townButtons = new JButton[] {
                makeButton("상태 보기", () -> showStatus()),
                makeButton("상점", () -> openShop()),
                makeButton("장비 변경", () -> openEquip()),
                makeButton("스탯 분배", () -> openStatAlloc()),
                makePrimaryButton("던전 입장", () -> openDungeonSelect()),
                makeButton("저장하기", () -> {
                    SaveManager.save(player);
                    Dialogs.message(this, "저장 완료", "게임을 저장했습니다.");
                }),
                makeButton("종료", () -> System.exit(0))
        };
        for (JButton b : townButtons) buttonPanel.add(b);
        Theme.arrowNav(townButtons);

        // Center the button column vertically within the right-hand side of the screen.
        JPanel eastWrap = new JPanel(new GridBagLayout());
        eastWrap.setOpaque(false);
        eastWrap.add(buttonPanel);
        panel.add(eastWrap, BorderLayout.EAST);

        return panel;
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

    private void refreshTown() {
        String statPointsNote = player.getStatPoints() > 0
                ? "&nbsp;&nbsp;&nbsp; <font color='#c7a86a'>스탯 포인트 " + player.getStatPoints() + "</font>"
                : "";
        townInfoLabel.setText("<html>Lv." + player.getLevel() + " " + player.getName()
                + "&nbsp;&nbsp;&nbsp; HP " + player.getHp() + "/" + player.getMaxHp()
                + "&nbsp;&nbsp;&nbsp; 골드 " + player.getGold() + "G"
                + statPointsNote + "</html>");
    }

    private void showStatus() {
        StatusDialog.show(this, player);
    }

    private void openStatAlloc() {
        StatAllocDialog.show(this, player);
        refreshTown();
    }

    private void openShop() {
        ShopDialog dialog = new ShopDialog(this, player);
        dialog.setVisible(true);
        refreshTown();
    }

    private void openEquip() {
        EquipDialog dialog = new EquipDialog(this, player);
        dialog.setVisible(true);
        refreshTown();
    }

    private void openDungeonSelect() {
        int maxIdx = Math.min(player.getUnlockedDungeon(), dungeons.size() - 1);
        String[] options = new String[maxIdx + 1];
        for (int i = 0; i <= maxIdx; i++) {
            Dungeon d = dungeons.get(i);
            options[i] = d.getName() + "  (" + d.getFloors() + "층 + 보스)";
        }
        int idx = Dialogs.choose(this, "던전 선택", "입장할 던전을 선택하세요", options);
        if (idx >= 0) {
            enterDungeon(idx);
        }
    }

    private void enterDungeon(int dungeonIndex) {
        currentDungeonIndex = dungeonIndex;
        currentFloor = 1;
        bossStage = false;
        cardLayout.show(cards, "BATTLE");
        startNextFight();
    }

    private void startNextFight() {
        Dungeon dungeon = dungeons.get(currentDungeonIndex);
        Monster monster = bossStage ? dungeon.spawnBoss() : dungeon.randomMonster(rnd);
        battlePanel.startBattle(player, monster, result -> onFightFinished(result, monster));
    }

    private void onFightFinished(BattlePanel.Result result, Monster monster) {
        Dungeon dungeon = dungeons.get(currentDungeonIndex);

        if (result == BattlePanel.Result.LOSE) {
            handleDefeat();
            return;
        }
        if (result == BattlePanel.Result.FLEE) {
            Dialogs.message(this, "후퇴",
                    bossStage ? "보스에게서 도망쳤습니다. 던전 클리어에 실패했습니다." : "던전에서 물러납니다.");
            returnToTown();
            return;
        }

        int beforeLevel = player.getLevel();
        player.gainExp(monster.getExpReward());
        player.earnGold(monster.getGoldReward());
        StringBuilder msg = new StringBuilder("EXP " + monster.getExpReward() + ", 골드 " + monster.getGoldReward() + "G 획득!");
        if (player.getLevel() > beforeLevel) {
            int gained = (player.getLevel() - beforeLevel) * Player.POINTS_PER_LEVEL;
            msg.append("\n레벨 업! Lv.").append(player.getLevel()).append("이 되었습니다! (스탯 포인트 +")
                    .append(gained).append(", 마을에서 분배하세요)");
        }

        if (!bossStage) {
            if (currentFloor >= dungeon.getFloors()) {
                Dialogs.message(this, "전투 승리", msg + "\n\n>>> 보스 등장! <<<");
                bossStage = true;
            } else {
                Dialogs.message(this, "전투 승리", msg.toString());
                currentFloor++;
            }
            startNextFight();
            return;
        }

        StringBuilder finalMsg = new StringBuilder("보스전 승리! " + msg);
        if (dungeon.getRewardGold() > 0 || dungeon.getRewardExp() > 0) {
            player.gainExp(dungeon.getRewardExp());
            player.earnGold(dungeon.getRewardGold());
            finalMsg.append("\n").append(dungeon.getName()).append(" 클리어 보상! EXP ")
                    .append(dungeon.getRewardExp()).append(", 골드 ").append(dungeon.getRewardGold()).append("G 추가 획득!");
        }

        Item chestItem = rollChestItem(currentDungeonIndex);
        player.addItem(chestItem.getName());
        finalMsg.append("\n\n보물 상자를 발견했다! ").append(chestItem.describe()).append(" 획득!");

        if (currentDungeonIndex == player.getUnlockedDungeon() && currentDungeonIndex + 1 < dungeons.size()) {
            player.setUnlockedDungeon(currentDungeonIndex + 1);
            finalMsg.append("\n새로운 던전이 해금되었습니다: ").append(dungeons.get(currentDungeonIndex + 1).getName());
        } else if (currentDungeonIndex == dungeons.size() - 1) {
            finalMsg.append("\n\n*** 축하합니다! 모든 던전을 클리어했습니다! ***");
        }

        Dialogs.message(this, "던전 클리어", finalMsg.toString());
        returnToTown();
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
        Dialogs.message(this, "패배", "정신을 잃고 마을로 실려왔습니다...\n골드 " + penalty + "G를 잃었습니다.");
        returnToTown();
    }

    private void returnToTown() {
        refreshTown();
        cardLayout.show(cards, "TOWN");
        Theme.focusFirst(townButtons);
    }
}
