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
    private final Theme.Meter playerHpBar = Theme.bar(Theme.HP);
    private final JLabel monsterLabel = Theme.header("");
    private final Theme.Meter monsterHpBar = Theme.bar(Theme.HP);
    private final BattleStage stage = new BattleStage();
    private final JTextArea log = new JTextArea();
    private final JButton attackBtn = Theme.primaryButton("공격");
    private final JButton itemBtn = Theme.button("아이템 사용");
    private final JButton fleeBtn = Theme.button("도망");

    public BattlePanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel statusPanel = Theme.panel(new GridLayout(2, 1, 6, 6));
        statusPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        JPanel pRow = new JPanel(new BorderLayout(10, 2));
        pRow.setOpaque(false);
        pRow.add(playerLabel, BorderLayout.NORTH);
        pRow.add(playerHpBar, BorderLayout.CENTER);

        JPanel mRow = new JPanel(new BorderLayout(10, 2));
        mRow.setOpaque(false);
        mRow.add(monsterLabel, BorderLayout.NORTH);
        mRow.add(monsterHpBar, BorderLayout.CENTER);

        statusPanel.add(pRow);
        statusPanel.add(mRow);

        JPanel topPanel = new JPanel(new BorderLayout(0, 10));
        topPanel.setOpaque(false);
        topPanel.add(statusPanel, BorderLayout.NORTH);
        topPanel.add(stage, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setBackground(Theme.PANEL);
        log.setForeground(Theme.TEXT);
        log.setFont(Theme.MONO_FONT);
        log.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(Theme.scroll(log), BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new GridLayout(1, 3, 10, 10));
        btnRow.setOpaque(false);
        attackBtn.addActionListener(e -> onAttack());
        itemBtn.addActionListener(e -> onItem());
        fleeBtn.addActionListener(e -> onFlee());
        btnRow.add(attackBtn);
        btnRow.add(itemBtn);
        btnRow.add(fleeBtn);
        add(btnRow, BorderLayout.SOUTH);
        Theme.arrowNav(attackBtn, itemBtn, fleeBtn);
    }

    public void startBattle(Player player, Monster monster, Consumer<Result> onFinish) {
        this.player = player;
        this.monster = monster;
        this.onFinish = onFinish;
        log.setText("");
        setButtonsEnabled(true);
        stage.setMonsterName(monster.getName());
        stage.startIdle();
        appendLog("=== " + monster.getName() + " 이(가) 나타났다! ===");
        refreshStatus();
    }

    private void refreshStatus() {
        playerLabel.setText(player.getName() + "   HP " + player.getHp() + "/" + player.getMaxHp()
                + "   MP " + player.getMp() + "/" + player.getMaxMp());
        playerHpBar.setMaximum(player.getMaxHp());
        playerHpBar.setValue(player.getHp());
        monsterLabel.setText(monster.getName() + "   HP " + monster.getHp() + "/" + monster.getMaxHp());
        monsterHpBar.setMaximum(monster.getMaxHp());
        monsterHpBar.setValue(monster.getHp());
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
            appendLog(player.getName() + "의 공격! " + (crit ? "치명타! " : "")
                    + monster.getName() + "에게 " + finalDmg + "의 피해!");
            refreshStatus();
        }, onDone);
    }

    private void monsterStrike(Runnable onDone) {
        stage.animateAttack(false, () -> {
            int dmg = computeDamage(monster.getAtk(), player.getDef());
            player.takeDamage(dmg);
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
        } else if (result == Result.LOSE) {
            appendLog(player.getName() + "은(는) 쓰러졌다...");
        }
        Timer timer = new Timer(700, e -> onFinish.accept(result));
        timer.setRepeats(false);
        timer.start();
    }
}
