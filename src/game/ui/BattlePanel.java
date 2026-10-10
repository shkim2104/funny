package game.ui;

import game.model.Item;
import game.model.ItemCatalog;
import game.model.Job;
import game.model.Monster;
import game.model.Player;
import game.model.Skill;

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

    // Command colors, after the handheld games' FIGHT / BAG / RUN buttons.
    private static final Color FIGHT_RED = new Color(222, 74, 62);
    private static final Color BAG_YELLOW = new Color(226, 164, 36);
    private static final Color RUN_BLUE = new Color(62, 126, 220);
    private static final Color BACK_GRAY = new Color(112, 118, 132);
    private static final int ACTION_AREA_HEIGHT = 128;
    private static final int LOG_MIN_WIDTH = 170;
    private static final int LOG_MAX_WIDTH = 300;

    private final BattleStage stage = new BattleStage();
    private final BattleHud hud = stage.getHud();
    private final JLabel goldLabel = new JLabel();
    private final JTextArea log = new JTextArea();
    /** Bottom-left message window: the latest battle line, or "무엇을 할까?" when it's the player's turn. */
    private final MessageBox prompt = new MessageBox(new BorderLayout());
    private final MessageBox returnPrompt = new MessageBox(new BorderLayout());
    private final CommandButton attackBtn = new CommandButton("공격", FIGHT_RED);
    private final CommandButton itemBtn = new CommandButton("아이템", BAG_YELLOW);
    private final CommandButton fleeBtn = new CommandButton("도망", RUN_BLUE);
    private final CommandButton returnBtn = new CommandButton("", RUN_BLUE);
    private final CardLayout actionCards = new CardLayout();
    private final JPanel actionArea = new JPanel(actionCards);

    // Attack menu (after job advancement): 2x2 skill slots, with basic attack and back stacked on the right.
    private static final int SKILL_SLOTS = 4;
    private final CommandButton basicAttackBtn = new CommandButton("기본 공격", FIGHT_RED);
    private final CommandButton[] skillSlotBtns = new CommandButton[SKILL_SLOTS];
    private final CommandButton menuBackBtn = new CommandButton("뒤로", BACK_GRAY);
    private final Skill[] slotSkills = new Skill[SKILL_SLOTS];
    private Runnable onReturn;

    public BattlePanel() {
        // Handheld-style layout: the scene (with its status boxes painted on top) fills the left
        // column above a message window + command buttons; the full battle log runs down the right.
        setLayout(new BorderLayout(12, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        attackBtn.addActionListener(e -> onAttack());
        itemBtn.addActionListener(e -> onItem());
        fleeBtn.addActionListener(e -> onFlee());
        Theme.arrowNav(attackBtn, itemBtn, fleeBtn);

        // 공격 is the big button on the left; 아이템 and 도망 stack beside it.
        JPanel commands = new JPanel(new GridBagLayout()) {
            @Override
            public Dimension getPreferredSize() {
                // Commands take a fair share of a narrow window but stop growing on a wide one.
                int col = getParent() == null ? 0 : getParent().getWidth();
                return new Dimension(Math.max(190, Math.min(280, Math.round(col * 0.4f))), 0);
            }
        };
        commands.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 0;
        c.gridy = 0;
        c.gridheight = 2;
        c.weightx = 1.3;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 8);
        commands.add(attackBtn, c);
        c.gridx = 1;
        c.gridheight = 1;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 4, 0);
        commands.add(itemBtn, c);
        c.gridy = 1;
        c.insets = new Insets(4, 0, 0, 0);
        commands.add(fleeBtn, c);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        actions.add(prompt, BorderLayout.CENTER);
        actions.add(commands, BorderLayout.EAST);

        // Once the dungeon run ends the commands give way to a single "return" button, so the
        // run's outcome stays readable in the log instead of being shown in a modal dialog.
        returnBtn.addActionListener(e -> {
            Runnable r = onReturn;
            onReturn = null;
            if (r != null) r.run();
        });
        returnBtn.setPreferredSize(new Dimension(200, 0));
        JPanel returnCard = new JPanel(new BorderLayout(10, 0));
        returnCard.setOpaque(false);
        returnCard.add(returnPrompt, BorderLayout.CENTER);
        returnCard.add(returnBtn, BorderLayout.EAST);

        actionArea.setOpaque(false);
        actionArea.setPreferredSize(new Dimension(0, ACTION_AREA_HEIGHT));
        actionArea.add(actions, "ACTIONS");
        actionArea.add(returnCard, "RETURN");
        actionArea.add(buildAttackMenu(), "ATTACK_MENU");

        JPanel battleColumn = new JPanel(new BorderLayout(0, 10));
        battleColumn.setOpaque(false);
        battleColumn.add(stage, BorderLayout.CENTER);
        battleColumn.add(actionArea, BorderLayout.SOUTH);
        add(battleColumn, BorderLayout.CENTER);
        add(buildLogWindow(), BorderLayout.EAST);
    }

    /** Tall message window on the right holding the whole battle log, with the current gold in its header. */
    private JComponent buildLogWindow() {
        JLabel title = new JLabel("전투 기록");
        title.setFont(Theme.dosFont(Font.BOLD, 16f));
        title.setForeground(BattleHud.INK);
        goldLabel.setFont(Theme.dosFont(Font.BOLD, 14f));
        goldLabel.setForeground(new Color(168, 112, 20));
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 8, 0));
        header.add(title, BorderLayout.WEST);
        header.add(goldLabel, BorderLayout.EAST);

        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setOpaque(false);
        log.setForeground(BattleHud.INK);
        log.setFont(Theme.dosFont(Font.PLAIN, 14f));
        log.setBorder(new EmptyBorder(0, 0, 0, 4));
        JScrollPane logScroll = Theme.scroll(log);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(BorderFactory.createEmptyBorder());

        // A thin rule between header and log, drawn in the box's ink color.
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(2, 0, 0, 0, BattleHud.BOX_SHADE),
                new EmptyBorder(8, 0, 0, 0)));
        body.add(logScroll, BorderLayout.CENTER);

        MessageBox window = new MessageBox(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                // About a fifth of the window: slim in the small window, roomier in fullscreen.
                int total = BattlePanel.this.getWidth();
                return new Dimension(Math.max(LOG_MIN_WIDTH, Math.min(LOG_MAX_WIDTH, Math.round(total * 0.22f))), 0);
            }
        };
        window.add(header, BorderLayout.NORTH);
        window.add(body, BorderLayout.CENTER);
        return window;
    }

    /** Sets the stage's backdrop to the given dungeon's (World.buildDungeons() index). */
    public void setDungeon(int dungeonIndex) {
        stage.setDungeon(dungeonIndex);
    }

    public void startBattle(Player player, Monster monster, Consumer<Result> onFinish) {
        this.player = player;
        this.monster = monster;
        this.onFinish = onFinish;
        log.setText("");
        actionCards.show(actionArea, "ACTIONS");
        actionArea.revalidate();
        setButtonsEnabled(true);
        stage.setMonsterName(monster.getName());
        stage.startIdle();
        hud.setMonster(monster.getName(), monster.getHp(), monster.getMaxHp(), true);
        appendLog("=== " + monster.getName() + " 이(가) 나타났다! ===");
        refreshStatus();
        hud.snapPlayer();
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
        hud.setMonster(monster.getName(), monster.getHp(), monster.getMaxHp(), true);
        appendLog("=== " + monster.getName() + " 이(가) 나타났다! ===");
        refreshStatus();
    }

    /** Writes a floor/battle result line into the log instead of showing a dialog. */
    public void logResult(String text) {
        appendLog(text);
    }

    /**
     * Ends the dungeon run: logs the outcome and replaces the action buttons with a single
     * return button labeled returnLabel, which runs onReturn when pressed.
     */
    public void endRun(String text, String returnLabel, Runnable onReturn) {
        appendLog(text);
        returnBtn.setText(returnLabel);
        // The full outcome is in the log; the message window just headlines it.
        returnPrompt.setText(text.split("\n")[0]);
        // Only gold and MP are refreshed here: on defeat the player is already revived, and the HP
        // bar should keep showing the knockout rather than a full bar. MP is refilled on the way out.
        refreshGold();
        hud.setPlayerMp(player.getMp(), player.getMaxMp());
        this.onReturn = onReturn;
        setButtonsEnabled(false);
        actionCards.show(actionArea, "RETURN");
        actionArea.revalidate();
        returnBtn.requestFocusInWindow();
    }

    /** Plays the level-up banner over the stage. */
    public void showLevelUp() {
        stage.playLevelUp();
    }

    private void refreshStatus() {
        hud.setPlayer(player.getName(), player.getJob().getDisplayName(), player.getLevel(),
                player.getHp(), player.getMaxHp(), player.getMp(), player.getMaxMp(),
                player.getExp(), player.getExpToNext());
        hud.setMonster(monster.getName(), monster.getHp(), monster.getMaxHp(), false);
        refreshGold();
    }

    private void refreshGold() {
        goldLabel.setText(player.getGold() + "G");
    }

    /** Adds a line to the side log and shows it in the message window, like the handheld battle text. */
    private void appendLog(String text) {
        log.append(text + "\n");
        log.setCaretPosition(log.getDocument().getLength());
        prompt.setText(text.replace("===", "").trim());
    }

    private void setButtonsEnabled(boolean enabled) {
        attackBtn.setEnabled(enabled);
        itemBtn.setEnabled(enabled);
        fleeBtn.setEnabled(enabled);
        if (enabled) {
            if (player != null) prompt.setText(player.getName() + "은(는) 무엇을 할까?");
            Theme.focusFirst(attackBtn, itemBtn, fleeBtn);
        }
    }

    /** Defense mitigates damage on a diminishing-returns curve: dmg = ATK * 50 / (DEF + 50). */
    private int computeDamage(int atk, int def) {
        double mitigated = atk * 50.0 / (def + 50.0);
        int dmg = Math.round((float) mitigated) + rnd.nextInt(5) - 2;
        return Math.max(1, dmg);
    }

    /** Beginners just attack; once advanced, 공격 opens the attack menu to pick a basic attack or a skill. */
    private void onAttack() {
        if (player.getJob() == Job.BEGINNER) {
            exchange(this::playerStrike);
        } else {
            openAttackMenu();
        }
    }

    private JPanel buildAttackMenu() {
        basicAttackBtn.addActionListener(e -> {
            closeAttackMenu();
            exchange(this::playerStrike);
        });
        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        for (int i = 0; i < SKILL_SLOTS; i++) {
            final int slot = i;
            CommandButton b = new CommandButton("", BACK_GRAY);
            b.setFont(Theme.dosFont(Font.BOLD, 15f));
            b.addActionListener(e -> useSlot(slot));
            skillSlotBtns[i] = b;
            grid.add(b);
        }

        menuBackBtn.addActionListener(e -> {
            closeAttackMenu();
            setButtonsEnabled(true);
        });

        JButton[] nav = new JButton[SKILL_SLOTS + 2];
        System.arraycopy(skillSlotBtns, 0, nav, 0, SKILL_SLOTS);
        nav[SKILL_SLOTS] = basicAttackBtn;
        nav[SKILL_SLOTS + 1] = menuBackBtn;
        Theme.arrowNav(nav);

        JPanel side = new JPanel(new GridLayout(2, 1, 0, 8));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(130, 0));
        side.add(basicAttackBtn);
        side.add(menuBackBtn);

        JPanel menu = new JPanel(new BorderLayout(10, 0));
        menu.setOpaque(false);
        menu.add(grid, BorderLayout.CENTER);
        menu.add(side, BorderLayout.EAST);
        // Esc backs out of the menu, like the back button.
        menu.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "menu-back");
        menu.getActionMap().put("menu-back", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                menuBackBtn.doClick();
            }
        });
        return menu;
    }

    /**
     * Fills the four slots with the player's skills (the newest four if there are more, e.g. after
     * a 2nd job), greys out ones the player can't afford, and shows the menu.
     */
    private void openAttackMenu() {
        List<Skill> skills = player.getSkills();
        int from = Math.max(0, skills.size() - SKILL_SLOTS);
        for (int i = 0; i < SKILL_SLOTS; i++) {
            int idx = from + i;
            Skill s = idx < skills.size() ? skills.get(idx) : null;
            slotSkills[i] = s;
            CommandButton b = skillSlotBtns[i];
            if (s == null) {
                b.setText("빈 슬롯");
                b.setSubtitle(null);
                b.setColor(BACK_GRAY);
                b.setDimmed(false);
                b.setToolTipText(null);
                b.setEnabled(false);
            } else {
                // Each skill wears its effect color; unaffordable ones stay clickable (dimmed) so
                // pressing one explains the MP shortage in the log.
                boolean affordable = player.getMp() >= s.getMpCost();
                b.setText(s.getName());
                b.setSubtitle("MP " + s.getMpCost() + (affordable ? "" : " (부족)"));
                b.setColor(buttonColor(s.getFxColor()));
                b.setDimmed(!affordable);
                b.setToolTipText(s.getDescription());
                b.setEnabled(true);
            }
        }
        prompt.setText("어떤 기술을 쓸까?");
        actionCards.show(actionArea, "ATTACK_MENU");
        actionArea.revalidate();
        basicAttackBtn.requestFocusInWindow();
    }

    /**
     * A skill's effect color as a button face: very light effect colors (silver, white) would swallow
     * the white label, so brightness is capped and near-greys get a little saturation back.
     */
    private static Color buttonColor(int rgb) {
        Color c = new Color(rgb);
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float sat = hsb[1] < 0.3f ? Math.max(hsb[1], 0.3f) : hsb[1];
        return Color.getHSBColor(hsb[0], sat, Math.min(hsb[2], 0.72f));
    }

    private void closeAttackMenu() {
        actionCards.show(actionArea, "ACTIONS");
        actionArea.revalidate();
        actionArea.revalidate();
    }

    private void useSlot(int slot) {
        Skill skill = slotSkills[slot];
        if (skill == null) return;
        if (player.getMp() < skill.getMpCost()) {
            appendLog("MP가 부족하다! (" + skill.getName() + ": MP " + skill.getMpCost() + " 필요)");
            return;
        }
        closeAttackMenu();
        exchange(done -> castSkill(skill, done));
    }

    /**
     * One round: the player's action and the monster's attack, in speed order (the faster side
     * goes first). playerAction receives the callback to run once it has finished animating.
     */
    private void exchange(Consumer<Runnable> playerAction) {
        setButtonsEnabled(false);
        if (monster.getSpd() > player.getSpd()) {
            appendLog(monster.getName() + "이(가) 더 빨라 선제공격!");
            monsterStrike(() -> {
                if (!player.isAlive()) {
                    finishBattle(Result.LOSE);
                    return;
                }
                playerAction.accept(() -> {
                    if (!monster.isAlive()) finishBattle(Result.WIN);
                    else setButtonsEnabled(true);
                });
            });
        } else {
            playerAction.accept(() -> {
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

    /** Order of a skill: pay HP (if any) -> hits -> heal (if any). Every part is optional data on the skill. */
    private void castSkill(Skill skill, Runnable onDone) {
        player.useMp(skill.getMpCost());
        appendLog(player.getName() + "의 " + skill.getName() + "!");
        refreshStatus();

        if (skill.getHpCostRatio() > 0) {
            // Never lethal to the caster: it always leaves at least 1 HP.
            int cost = Math.min(player.getHp() - 1, (int) Math.round(player.getMaxHp() * skill.getHpCostRatio()));
            if (cost > 0) {
                player.takeDamage(cost);
                stage.showDamage(cost, false, false);
                appendLog(player.getName() + "은(는) HP " + cost + "을(를) 바쳤다!");
                refreshStatus();
            }
        }

        Runnable afterHits = () -> {
            if (skill.getHealRatio() > 0) {
                int before = player.getHp();
                player.heal((int) Math.round(player.getMaxHp() * skill.getHealRatio()));
                stage.playHeal();
                appendLog("HP를 " + (player.getHp() - before) + " 회복했다!");
                refreshStatus();
            }
            onDone.run();
        };

        if (skill.isAttack()) {
            skillHits(skill, skill.getHits(), 0, afterHits);
        } else {
            // Pure support skill: a short pause so the turn doesn't feel instant.
            Timer pause = new Timer(350, e -> afterHits.run());
            pause.setRepeats(false);
            pause.start();
        }
    }

    /** Plays `hits` lunges in a row, stopping early if the monster falls. */
    private void skillHits(Skill skill, int hits, int index, Runnable onDone) {
        stage.animateAttack(true, () -> {
            stage.playSkillFx(skill.getFx(), skill.getFxColor(), index);
            int def = skill.isIgnoreDefense() ? 0 : monster.getDef();
            int dmg = computeDamage(skill.powerFor(player), def);
            boolean crit = skill.isAlwaysCrit() || rnd.nextInt(100) < player.getCritChance();
            int finalDmg = crit ? Math.round(dmg * 1.75f) : dmg;
            int hpBefore = monster.getHp();
            monster.takeDamage(finalDmg);
            stage.showDamage(finalDmg, true, crit);
            appendLog((hits > 1 ? (index + 1) + "타! " : "") + (crit ? "치명타! " : "")
                    + monster.getName() + "에게 " + finalDmg + "의 피해!");
            if (skill.getDrainRatio() > 0) {
                // Drain from the damage that actually landed, so overkill on the last hit doesn't over-heal.
                int dealt = Math.min(finalDmg, hpBefore);
                int before = player.getHp();
                player.heal((int) Math.round(dealt * skill.getDrainRatio()));
                int healed = player.getHp() - before;
                if (healed > 0) {
                    stage.playHeal();
                    appendLog("HP를 " + healed + " 흡수했다!");
                }
            }
            refreshStatus();
        }, () -> {
            if (index + 1 < hits && monster.isAlive()) skillHits(skill, hits, index + 1, onDone);
            else onDone.run();
        });
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
            stage.playMonsterDeath();
            stage.showRewards(monster.getExpReward(), monster.getGoldReward());
        } else if (result == Result.LOSE) {
            appendLog(player.getName() + "은(는) 쓰러졌다...");
        }
        Timer timer = new Timer(700, e -> onFinish.accept(result));
        timer.setRepeats(false);
        timer.start();
    }
}
