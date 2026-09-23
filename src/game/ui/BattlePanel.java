package game.ui;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Monster;
import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

public class BattlePanel extends JPanel {
    public enum Result { WIN, LOSE, FLEE }

    private final Random rnd = new Random();
    private Player player;
    private Monster monster;
    private Consumer<Result> onFinish;

    private final JLabel playerLabel = Theme.header("");
    private final Theme.Meter playerHpBar = Theme.bar(Theme.PLAYER_HP);
    private final Theme.Meter playerMpBar = Theme.bar(Theme.MP);
    private final Theme.Meter playerExpBar = Theme.bar(Theme.EXP);
    private final JLabel goldLabel = new JLabel();
    private final JLabel monsterLabel = Theme.header("");
    private final Theme.Meter monsterHpBar = Theme.bar(Theme.HP);
    private final BattleStage stage = new BattleStage();
    private final JTextArea log = new JTextArea();
    private final JButton attackBtn = Theme.primaryButton("공격");
    private final JButton itemBtn = Theme.button("아이템 사용");
    private final JButton fleeBtn = Theme.button("도망");
    private final JButton returnBtn = Theme.primaryButton("마을로 돌아가기");
    private final CardLayout actionCards = new CardLayout();
    private final JPanel actionArea = new JPanel(actionCards);
    private Runnable onReturn;

    public BattlePanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Monster card sits above the stage (it appears far/top-right on the stage);
        // the player card sits below the stage (it appears close/bottom-left) — matching
        // positions make it obvious at a glance which side is which.
        JPanel mContent = new JPanel();
        mContent.setOpaque(false);
        mContent.setLayout(new BoxLayout(mContent, BoxLayout.Y_AXIS));
        monsterLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mContent.add(badge("몬스터", Theme.HP));
        mContent.add(Box.createVerticalStrut(2));
        mContent.add(monsterLabel);
        mContent.add(Box.createVerticalStrut(6));
        mContent.add(meterRow("HP", monsterHpBar));
        JPanel monsterCard = accentCard(Theme.HP, mContent);
        add(monsterCard, BorderLayout.NORTH);

        add(stage, BorderLayout.CENTER);

        JPanel pContent = new JPanel();
        pContent.setOpaque(false);
        pContent.setLayout(new BoxLayout(pContent, BoxLayout.Y_AXIS));
        // Name on the left, current gold small on the right of the same row.
        goldLabel.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD));
        goldLabel.setForeground(Theme.EXP);
        JPanel nameRow = new JPanel(new BorderLayout(8, 0));
        nameRow.setOpaque(false);
        nameRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        nameRow.add(playerLabel, BorderLayout.WEST);
        nameRow.add(goldLabel, BorderLayout.EAST);
        pContent.add(badge("아군", Theme.PLAYER_HP));
        pContent.add(Box.createVerticalStrut(2));
        pContent.add(nameRow);
        pContent.add(Box.createVerticalStrut(6));
        pContent.add(meterRow("HP", playerHpBar));
        pContent.add(Box.createVerticalStrut(4));
        pContent.add(meterRow("MP", playerMpBar));
        pContent.add(Box.createVerticalStrut(4));
        pContent.add(meterRow("EXP", playerExpBar));
        JPanel playerCard = accentCard(Theme.PLAYER_HP, pContent);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setRows(3);
        log.setBackground(Theme.PANEL);
        log.setForeground(Theme.TEXT);
        log.setFont(Theme.MONO_FONT);
        log.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane logScroll = Theme.scroll(log);
        logScroll.setPreferredSize(new Dimension(0, 92));

        JPanel btnRow = new JPanel(new GridLayout(1, 3, 10, 10));
        btnRow.setOpaque(false);
        attackBtn.addActionListener(e -> onAttack());
        itemBtn.addActionListener(e -> onItem());
        fleeBtn.addActionListener(e -> onFlee());
        btnRow.add(attackBtn);
        btnRow.add(itemBtn);
        btnRow.add(fleeBtn);
        Theme.arrowNav(attackBtn, itemBtn, fleeBtn);

        // The action row swaps to a single "return" button once the dungeon run ends, so the
        // run's outcome stays readable in the log instead of being shown in a modal dialog.
        returnBtn.addActionListener(e -> {
            Runnable r = onReturn;
            onReturn = null;
            if (r != null) r.run();
        });
        actionArea.setOpaque(false);
        actionArea.add(btnRow, "ACTIONS");
        actionArea.add(returnBtn, "RETURN");

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.add(playerCard);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(logScroll);
        bottomPanel.add(Box.createVerticalStrut(10));
        bottomPanel.add(actionArea);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void startBattle(Player player, Monster monster, Consumer<Result> onFinish) {
        this.player = player;
        this.monster = monster;
        this.onFinish = onFinish;
        log.setText("");
        actionCards.show(actionArea, "ACTIONS");
        setButtonsEnabled(true);
        stage.setMonsterName(monster.getName());
        stage.startIdle();
        appendLog("=== " + monster.getName() + " 이(가) 나타났다! ===");
        refreshStatus();
    }

    /**
     * Continues the same dungeon run against a new monster without clearing the log or
     * popping a dialog, so floor results read as a running log instead of a click-through.
     */
    public void nextBattle(Monster monster, Consumer<Result> onFinish) {
        this.monster = monster;
        this.onFinish = onFinish;
        setButtonsEnabled(true);
        stage.setMonsterName(monster.getName());
        stage.startIdle();
        appendLog("=== " + monster.getName() + " 이(가) 나타났다! ===");
        refreshStatus();
    }

    /** Writes a floor/battle result line into the log instead of showing a dialog. */
    public void logResult(String text) {
        appendLog(text);
    }

    /**
     * Ends the dungeon run: logs the outcome and replaces the action buttons with a single
     * "return to town" button, which runs onReturn when pressed.
     */
    public void endRun(String text, Runnable onReturn) {
        appendLog(text);
        // Only gold is refreshed here: on defeat the player is already revived, and the HP bar
        // should keep showing the knockout rather than a full bar.
        refreshGold();
        this.onReturn = onReturn;
        setButtonsEnabled(false);
        actionCards.show(actionArea, "RETURN");
        returnBtn.requestFocusInWindow();
    }

    /** Plays the level-up banner over the stage. */
    public void showLevelUp() {
        stage.playLevelUp();
    }

    private void refreshStatus() {
        playerLabel.setText(player.getName());
        playerHpBar.setMaximum(player.getMaxHp());
        playerHpBar.setValue(player.getHp());
        playerMpBar.setMaximum(player.getMaxMp());
        playerMpBar.setValue(player.getMp());
        playerExpBar.setMaximum(player.getExpToNext());
        playerExpBar.setValue(player.getExp());
        refreshGold();
        monsterLabel.setText(monster.getName());
        monsterHpBar.setMaximum(monster.getMaxHp());
        monsterHpBar.setValue(monster.getHp());
    }

    private void refreshGold() {
        goldLabel.setText("소지금 " + player.getGold() + "G");
    }

    /** Small bold role tag ("아군"/"몬스터") colored to match that side's HP bar. */
    private static JLabel badge(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD));
        l.setForeground(color);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    /** Caption + meter row; the caption identifies which stat the bar represents (HP vs MP). */
    private static JPanel meterRow(String caption, Theme.Meter meter) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel tag = new JLabel(caption);
        tag.setFont(Theme.SMALL_FONT.deriveFont(Font.BOLD));
        tag.setForeground(Theme.TEXT_DIM);
        tag.setPreferredSize(new Dimension(Math.max(26, tag.getPreferredSize().width), tag.getPreferredSize().height));
        row.add(tag, BorderLayout.WEST);
        row.add(meter, BorderLayout.CENTER);
        return row;
    }

    /** Card with a colored left accent stripe so the player and monster panels read as separate, distinct blocks. */
    private static JPanel accentCard(Color accent, JComponent content) {
        JPanel card = Theme.panel(new BorderLayout(10, 0));
        card.setBorder(new EmptyBorder(10, 10, 10, 12));
        JPanel stripe = new JPanel();
        stripe.setOpaque(true);
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(4, 0));
        card.add(stripe, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void appendLog(String text) {
        log.append(text + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }

    private void setButtonsEnabled(boolean enabled) {
        attackBtn.setEnabled(enabled);
        itemBtn.setEnabled(enabled);
        fleeBtn.setEnabled(enabled);
        if (enabled) {
            Theme.focusFirst(attackBtn, itemBtn, fleeBtn);
        }
    }

    /** Defense mitigates damage on a diminishing-returns curve: dmg = ATK * 50 / (DEF + 50). */
    private int computeDamage(int atk, int def) {
        double mitigated = atk * 50.0 / (def + 50.0);
        int dmg = Math.round((float) mitigated) + rnd.nextInt(5) - 2;
        return Math.max(1, dmg);
    }

    private void onAttack() {
        setButtonsEnabled(false);
        if (monster.getSpd() > player.getSpd()) {
            appendLog(monster.getName() + "이(가) 더 빨라 선제공격!");
            monsterStrike(() -> {
                if (!player.isAlive()) {
                    finishBattle(Result.LOSE);
                    return;
                }
                playerStrike(() -> {
                    if (!monster.isAlive()) finishBattle(Result.WIN);
                    else setButtonsEnabled(true);
                });
            });
        } else {
            playerStrike(() -> {
                if (!monster.isAlive()) {
                    finishBattle(Result.WIN);
                    return;
                }
                monsterStrike(() -> {
                    if (!player.isAlive()) finishBattle(Result.LOSE);
                    else setButtonsEnabled(true);
                });
            });
        }
    }

    private void playerStrike(Runnable onDone) {
        stage.animateAttack(true, () -> {
            int dmg = computeDamage(player.getAtk(), monster.getDef());
            boolean crit = rnd.nextInt(100) < player.getCritChance();
            int finalDmg = crit ? Math.round(dmg * 1.75f) : dmg;
            monster.takeDamage(finalDmg);
            stage.showDamage(finalDmg, true, crit);
            appendLog(player.getName() + "의 공격! " + (crit ? "치명타! " : "")
                    + monster.getName() + "에게 " + finalDmg + "의 피해!");
            refreshStatus();
        }, onDone);
    }

    private void monsterStrike(Runnable onDone) {
        stage.animateAttack(false, () -> {
            int dmg = computeDamage(monster.getAtk(), player.getDef());
            player.takeDamage(dmg);
            stage.showDamage(dmg, false, false);
            appendLog(monster.getName() + "의 공격! " + player.getName() + "이(가) " + dmg + "의 피해를 입었다!");
            refreshStatus();
        }, onDone);
    }

    private void onItem() {
        List<String> usable = player.getUsableItems();
        if (usable.isEmpty()) {
            Dialogs.message(this, "알림", "사용할 수 있는 아이템이 없습니다.");
            return;
        }
        String[] options = usable.stream()
                .map(name -> ItemCatalog.get(name).describe())
                .toArray(String[]::new);
        int idx = Dialogs.choose(this, "아이템 사용", "사용할 아이템을 선택하세요", options);
        if (idx < 0) return;

        setButtonsEnabled(false);
        String itemName = usable.get(idx);
        Item item = ItemCatalog.get(itemName);
        if (item.getType() == Item.Type.POTION) {
            player.heal(item.getValue());
            appendLog(player.getName() + "은(는) " + item.getName() + "을(를) 사용해 HP를 " + item.getValue() + " 회복했다!");
        } else if (item.getType() == Item.Type.ETHER) {
            player.restoreMp(item.getValue());
            appendLog(player.getName() + "은(는) " + item.getName() + "을(를) 사용해 MP를 " + item.getValue() + " 회복했다!");
        }
        player.removeItem(itemName);
        refreshStatus();
        monsterTurn();
    }

    private void onFlee() {
        setButtonsEnabled(false);
        if (rnd.nextInt(100) < 50) {
            appendLog("성공적으로 도망쳤다!");
            finishBattle(Result.FLEE);
        } else {
            appendLog("도망에 실패했다!");
            monsterTurn();
        }
    }

    private void monsterTurn() {
        if (!monster.isAlive()) {
            setButtonsEnabled(true);
            return;
        }
        setButtonsEnabled(false);
        monsterStrike(() -> {
            if (!player.isAlive()) {
                finishBattle(Result.LOSE);
            } else {
                setButtonsEnabled(true);
            }
        });
    }

    private void finishBattle(Result result) {
        setButtonsEnabled(false);
        stage.stopIdle();
        if (result == Result.WIN) {
            appendLog(monster.getName() + "을(를) 물리쳤다!");
            stage.showRewards(monster.getExpReward(), monster.getGoldReward());
        } else if (result == Result.LOSE) {
            appendLog(player.getName() + "은(는) 쓰러졌다...");
        }
        Timer timer = new Timer(700, e -> onFinish.accept(result));
        timer.setRepeats(false);
        timer.start();
    }
}
