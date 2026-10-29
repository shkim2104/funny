package game.ui;

import game.model.Bonus;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Job;
import game.model.Monster;
import game.model.Player;
import game.model.RunState;
import game.model.Skill;
import game.save.Settings;

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

    // Run augments + boss gear effects for the current fight (see Bonus).
    private RunState run;
    private Bonus bonus = Bonus.NONE;
    private boolean bossFight;
    private int shieldHp;
    private boolean firstHitDone;

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

    /** The whole battle screen sits on a notebook page (margin line on the left), like the dialogs. */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Sketch.smooth(g);
        Sketch.paper(g2, getWidth(), getHeight(), 8);
        g2.dispose();
    }
    /** Tall message window on the right holding the whole battle log, with the current gold in its header. */
    private JComponent buildLogWindow() {
        JLabel title = new JLabel("전투 기록");
        title.setFont(Sketch.hand(Font.BOLD, 16f));
        title.setForeground(BattleHud.INK);
        goldLabel.setFont(Sketch.hand(Font.BOLD, 14f));
        goldLabel.setForeground(Sketch.BLUE_PENCIL);
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
        log.setFont(Sketch.hand(Font.PLAIN, 14f));
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
        playerTurn();
        stage.setMonsterName(monster.getName());
        stage.startIdle();
        hud.setMonster(monster.getName(), monster.getHp(), monster.getMaxHp(), true);
        appendLog("=== " + monster.getName() + " 이(가) 나타났다! ===");
        beginFight();
        refreshStatus();
        hud.snapPlayer();
    }

    /** The hand-drawn hero shown on the stage. */
    public void setPlayerArt(CharacterArt art) {
        stage.setPlayerArt(art);
    }

    /** The dungeon run whose augments apply (null outside a run). */
    public void setRun(RunState run) {
        this.run = run;
    }

    /** Whether the next fight is the boss (for boss-damage effects). */
    public void setBossFight(boolean bossFight) {
        this.bossFight = bossFight;
    }

    /** Re-reads the effects (augments may have been picked since) and raises the fight-start barrier. */
    private void beginFight() {
        bonus = player.getGearBonus().plus(run != null ? run.total() : Bonus.NONE);
        firstHitDone = false;
        shieldHp = (int) Math.round(player.getMaxHp() * bonus.shield());
        if (shieldHp > 0) appendLog("보호막이 피해 " + shieldHp + "을(를) 막아 준다.");
    }

    /**
     * Continues the same dungeon run against a new monster without clearing the log or
     * popping a dialog, so floor results read as a running log instead of a click-through.
     */
    public void nextBattle(Monster monster, Consumer<Result> onFinish) {
        this.monster = monster;
        this.onFinish = onFinish;
        playerTurn();
        stage.setMonsterName(monster.getName());
        stage.startIdle();
        hud.setMonster(monster.getName(), monster.getHp(), monster.getMaxHp(), true);
        appendLog("=== " + monster.getName() + " 이(가) 나타났다! ===");
        beginFight();
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

    /** A blow-by-blow line: always in the message window, in the log unless 전투 기록 간단히 is on. */
    private void logDetail(String text) {
        if (Settings.isBriefLog()) prompt.setText(text.replace("===", "").trim());
        else appendLog(text);
    }

    /** A pause in ms at the chosen 전투 속도. */
    private static int scaled(int ms) {
        return (int) Math.max(1, Math.round(ms / Settings.battleSpeed()));
    }

    // ---------- turn start: 자동 포션, then 자동 전투 ----------
    private int turnId;
    private boolean attackMenuOpen;

    /** The player's turn begins: buttons on, then the convenience settings may act for them. */
    private void playerTurn() {
        setButtonsEnabled(true);
        int id = ++turnId;
        String potion = autoPotion();
        if (potion != null) {
            later(scaled(450), id, () -> {
                setButtonsEnabled(false);
                logDetail("[자동 포션]");
                usePotion(potion);
                monsterTurn();
            });
        } else if (Settings.isAutoBattle() && !bossFight) {
            later(scaled(600), id, () -> exchange(this::playerStrike));
        }
    }

    /** The potion 자동 포션 would drink now, or null (off, HP high enough, or none in the bag). */
    private String autoPotion() {
        int pct = Settings.getAutoPotionPercent();
        if (pct <= 0 || player.getHp() * 100 > player.getMaxHp() * pct) return null;
        boolean high = player.getInventory().contains("하이 포션"), small = player.getInventory().contains("포션");
        int missing = player.getMaxHp() - player.getHp();
        if (high && (missing >= 80 || !small)) return "하이 포션";
        return small ? "포션" : null;
    }

    /**
     * Runs an automatic action once the delay passes, but only if it is still this same turn, the
     * player hasn't acted or opened the skill menu, and no dialog is in front (checked again later if one is).
     */
    private void later(int ms, int id, Runnable action) {
        Timer t = new Timer(ms, e -> {
            if (id != turnId || !attackBtn.isEnabled() || attackMenuOpen || monster == null || !monster.isAlive()) return;
            // A modal dialog in front (아이템 list, 설정, Tab info...) holds the action until it closes.
            for (Window w : Window.getWindows()) {
                if (w instanceof Dialog && w.isShowing() && ((Dialog) w).isModal()) {
                    later(400, id, action);
                    return;
                }
            }
            action.run();
        });
        t.setRepeats(false);
        t.start();
    }

    // ---------- hotkeys (called by GameFrame) ----------

    /** 1~4: use that skill slot straight away (as if picked from the 공격 menu). */
    public void hotkeySkill(int slot) {
        if (player == null || !attackBtn.isEnabled() || player.getJob() == Job.BEGINNER) return;
        if (!attackMenuOpen) openAttackMenu();
        if (slotSkills[slot] == null) {
            closeAttackMenu();
            setButtonsEnabled(true);
            return;
        }
        useSlot(slot);
    }

    /** Tab: this run's augments and the equipped gear's effects, on a notebook page. */
    public void showRunInfo() {
        if (player == null) return;
        StringBuilder html = new StringBuilder("<html><body style='width:380px'>");
        html.append("<b>이번 던전의 증강</b><br>");
        if (run == null || run.getAugments().isEmpty()) {
            html.append("<font color='").append(Sketch.hex(Sketch.INK_SOFT)).append("'>아직 고른 증강이 없어요.</font><br>");
        } else {
            for (game.model.Augment a : run.getAugments()) {
                html.append("<font color='").append(a.getTier().getColorHex()).append("'><b>[").append(a.getTier().getLabel())
                        .append("] ").append(a.getName()).append("</b></font> <font color='").append(Sketch.hex(Sketch.INK_SOFT))
                        .append("'>").append(a.getDescription()).append("</font><br>");
            }
        }
        html.append("<br><b>장비 효과</b><br>");
        java.util.List<String> gear = player.getGearBonus().lines();
        html.append(gear.isEmpty() ? "<font color='" + Sketch.hex(Sketch.INK_SOFT) + "'>없음</font>" : String.join("<br>", gear));
        html.append("<br><br><b>모두 합친 효과</b><br>");
        java.util.List<String> total = bonus.lines();
        html.append("<font color='").append(Sketch.hex(Sketch.RED_PENCIL)).append("'>")
                .append(total.isEmpty() ? "없음" : String.join("<br>", total)).append("</font></body></html>");
        JLabel text = new JLabel(html.toString());
        text.setFont(Sketch.hand(Font.PLAIN, 14f));
        text.setForeground(Sketch.INK);
        text.setVerticalAlignment(SwingConstants.TOP);
        Dialogs.custom(this, "보유 증강 · 장비 효과", Sketch.scroll(text), 520, 520);
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
            b.setFont(Sketch.hand(Font.BOLD, 15f));
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
                boolean affordable = player.getMp() >= mpCost(s);
                b.setText(s.getName());
                b.setSubtitle("MP " + mpCost(s) + (affordable ? "" : " (부족)"));
                b.setColor(buttonColor(s.getFxColor()));
                b.setDimmed(!affordable);
                b.setToolTipText(s.getDescription());
                b.setEnabled(true);
            }
        }
        prompt.setText("어떤 기술을 쓸까?");
        attackMenuOpen = true;
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
        attackMenuOpen = false;
        actionCards.show(actionArea, "ACTIONS");
        actionArea.revalidate();
    }

    private void useSlot(int slot) {
        Skill skill = slotSkills[slot];
        if (skill == null) return;
        if (player.getMp() < mpCost(skill)) {
            appendLog("MP가 부족하다! (" + skill.getName() + ": MP " + mpCost(skill) + " 필요)");
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
        boolean monsterFirst = !bonus.isAlwaysFirst() && monster.getSpd() > player.getSpd() + bonus.spd();
        if (monsterFirst) {
            logDetail(monster.getName() + "이(가) 더 빨라 선제공격!");
            monsterStrike(() -> {
                if (!player.isAlive()) {
                    finishBattle(Result.LOSE);
                    return;
                }
                if (!monster.isAlive()) {   // fell to thorns
                    finishBattle(Result.WIN);
                    return;
                }
                playerAction.accept(() -> {
                    if (!monster.isAlive()) finishBattle(Result.WIN);
                    else roundOver();
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
                    else if (!monster.isAlive()) finishBattle(Result.WIN);
                    else roundOver();
                });
            });
        }
    }

    /** End of a round with both sides standing: regeneration ticks, then it's the player's turn again. */
    private void roundOver() {
        int regen = (int) Math.round(player.getMaxHp() * bonus.regen());
        if (regen > 0 && player.getHp() < player.getMaxHp()) {
            int before = player.getHp();
            player.heal(regen);
            logDetail("재생으로 HP를 " + (player.getHp() - before) + " 회복했다.");
            refreshStatus();
        }
        playerTurn();
    }

    /** MP a skill costs after cost-reduction effects. */
    private int mpCost(Skill s) {
        return Math.max(0, s.getMpCost() - bonus.mpCostReduce());
    }

    /**
     * Damage of one player hit with every effect applied. All damage multipliers stack multiplicatively:
     * ATK%, skill damage, per-kill stacks (compounding per kill), first strike, boss/execute/low-HP
     * bonuses and the crit multiplier (1.75x, times the crit-damage bonus). Returns {damage, crit ? 1 : 0}.
     */
    private int[] playerHit(int power, int def, boolean skill, boolean forceCrit) {
        double atkMul = (1 + bonus.atkPct()) * (skill ? 1 + bonus.skillDmg() : 1)
                * (run != null ? Math.pow(1 + bonus.stackAtkPerKill(), run.getKills()) : 1);
        int dmg = computeDamage((int) Math.round(power * atkMul), (int) Math.round(def * (1 - bonus.ignoreDef())));
        double mul = 1;
        if (!firstHitDone) {
            mul *= 1 + bonus.firstStrike();
            firstHitDone = true;
        }
        if (bossFight) mul *= 1 + bonus.bossDmg();
        if (monster.getHp() <= monster.getMaxHp() * 0.3) mul *= 1 + bonus.execute();
        if (player.getHp() <= player.getMaxHp() * 0.3) mul *= 1 + bonus.lowHpDmg();
        boolean crit = forceCrit || rnd.nextInt(100) < player.getCritChance() + bonus.critChance();
        if (crit) mul *= 1.75 * (1 + bonus.critDmg());
        return new int[]{Math.max(1, (int) Math.round(dmg * mul)), crit ? 1 : 0};
    }

    /** Heals the player by ratio of the damage that actually landed. */
    private void drainFrom(int dealt, double ratio) {
        if (ratio <= 0) return;
        int before = player.getHp();
        player.heal((int) Math.round(dealt * ratio));
        int healed = player.getHp() - before;
        if (healed > 0) {
            stage.playHeal();
            logDetail("HP를 " + healed + " 흡수했다!");
        }
    }

    private void playerStrike(Runnable onDone) {
        basicHit(true, onDone);
    }

    /** A basic attack; with a double-strike effect it may follow up once more (the follow-up can't chain). */
    private void basicHit(boolean first, Runnable onDone) {
        boolean[] again = {false};
        stage.animateAttack(true, () -> {
            int[] hit = playerHit(player.getAtk(), monster.getDef(), false, false);
            boolean crit = hit[1] == 1;
            int hpBefore = monster.getHp();
            monster.takeDamage(hit[0]);
            stage.showDamage(hit[0], true, crit);
            logDetail((first ? player.getName() + "의 공격! " : "연속 공격! ") + (crit ? "치명타! " : "")
                    + monster.getName() + "에게 " + hit[0] + "의 피해!");
            drainFrom(Math.min(hit[0], hpBefore), bonus.lifesteal());
            again[0] = first && monster.isAlive() && rnd.nextDouble() < bonus.doubleStrike();
            refreshStatus();
        }, () -> {
            if (again[0]) basicHit(false, onDone);
            else onDone.run();
        });
    }

    /** Order of a skill: pay HP (if any) -> hits -> heal (if any). Every part is optional data on the skill. */
    private void castSkill(Skill skill, Runnable onDone) {
        player.useMp(mpCost(skill));
        logDetail(player.getName() + "의 " + skill.getName() + "!");
        refreshStatus();

        if (skill.getHpCostRatio() > 0) {
            // Never lethal to the caster: it always leaves at least 1 HP.
            int cost = Math.min(player.getHp() - 1, (int) Math.round(player.getMaxHp() * skill.getHpCostRatio()));
            if (cost > 0) {
                player.takeDamage(cost);
                stage.showDamage(cost, false, false);
                logDetail(player.getName() + "은(는) HP " + cost + "을(를) 바쳤다!");
                refreshStatus();
            }
        }

        Runnable afterHits = () -> {
            if (skill.getHealRatio() > 0) {
                int before = player.getHp();
                player.heal((int) Math.round(player.getMaxHp() * skill.getHealRatio()));
                stage.playHeal();
                logDetail("HP를 " + (player.getHp() - before) + " 회복했다!");
                refreshStatus();
            }
            onDone.run();
        };

        if (skill.isAttack()) {
            skillHits(skill, skill.getHits(), 0, afterHits);
        } else {
            // Pure support skill: a short pause so the turn doesn't feel instant.
            Timer pause = new Timer(scaled(350), e -> afterHits.run());
            pause.setRepeats(false);
            pause.start();
        }
    }

    /** Plays `hits` lunges in a row, stopping early if the monster falls. */
    private void skillHits(Skill skill, int hits, int index, Runnable onDone) {
        stage.animateAttack(true, () -> {
            stage.playSkillFx(skill.getFx(), skill.getFxColor(), index);
            int def = skill.isIgnoreDefense() ? 0 : monster.getDef();
            int[] hit = playerHit(skill.powerFor(player), def, true, skill.isAlwaysCrit());
            int finalDmg = hit[0];
            boolean crit = hit[1] == 1;
            int hpBefore = monster.getHp();
            monster.takeDamage(finalDmg);
            stage.showDamage(finalDmg, true, crit);
            logDetail((hits > 1 ? (index + 1) + "타! " : "") + (crit ? "치명타! " : "")
                    + monster.getName() + "에게 " + finalDmg + "의 피해!");
            // Drain from the damage that actually landed, so overkill on the last hit doesn't over-heal.
            drainFrom(Math.min(finalDmg, hpBefore), skill.getDrainRatio() + bonus.lifesteal());
            refreshStatus();
        }, () -> {
            if (index + 1 < hits && monster.isAlive()) skillHits(skill, hits, index + 1, onDone);
            else onDone.run();
        });
    }

    private void monsterStrike(Runnable onDone) {
        stage.animateAttack(false, () -> {
            if (rnd.nextDouble() < bonus.dodge()) {
                logDetail(monster.getName() + "의 공격! " + player.getName() + "은(는) 가볍게 피했다!");
                return;
            }
            int def = (int) Math.round(player.getDef() * (1 + bonus.defPct()));
            int dmg = Math.max(1, (int) Math.round(computeDamage(monster.getAtk(), def) * (1 - bonus.dmgReduction())));
            int absorbed = Math.min(shieldHp, dmg);
            shieldHp -= absorbed;
            int taken = dmg - absorbed;
            player.takeDamage(taken);
            stage.showDamage(taken, false, false);
            logDetail(monster.getName() + "의 공격! " + player.getName() + "이(가) " + taken + "의 피해를 입었다!"
                    + (absorbed > 0 ? " (보호막이 " + absorbed + " 막음)" : ""));
            int reflected = (int) Math.round(dmg * bonus.thorns());
            if (reflected > 0) {
                monster.takeDamage(reflected);
                stage.showDamage(reflected, true, false);
                logDetail("가시가 " + monster.getName() + "에게 " + reflected + "의 피해를 돌려주었다!");
            }
            if (!player.isAlive() && bonus.revive() > 0 && run != null && !run.isReviveUsed()) {
                run.useRevive();
                player.heal((int) Math.round(player.getMaxHp() * bonus.revive()));
                stage.playHeal();
                appendLog(player.getName() + "은(는) 쓰러지지 않았다! 불굴의 힘으로 HP " + player.getHp() + "으로 일어섰다!");
            }
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
        usePotion(usable.get(idx));
        monsterTurn();
    }

    /** Drinks a potion or ether from the bag (used by 아이템 and by 자동 포션). */
    private void usePotion(String itemName) {
        Item item = ItemCatalog.get(itemName);
        if (item.getType() == Item.Type.POTION) {
            player.heal(item.getValue());
            stage.playHeal();
            appendLog(player.getName() + "은(는) " + item.getName() + "을(를) 사용해 HP를 " + item.getValue() + " 회복했다!");
        } else if (item.getType() == Item.Type.ETHER) {
            player.restoreMp(item.getValue());
            appendLog(player.getName() + "은(는) " + item.getName() + "을(를) 사용해 MP를 " + item.getValue() + " 회복했다!");
        }
        player.removeItem(itemName);
        refreshStatus();
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
            playerTurn();
            return;
        }
        setButtonsEnabled(false);
        monsterStrike(() -> {
            if (!player.isAlive()) {
                finishBattle(Result.LOSE);
            } else if (!monster.isAlive()) {
                finishBattle(Result.WIN);
            } else {
                roundOver();
            }
        });
    }

    /** On-kill effects: per-kill stacks, HP/MP back on kill, and the after-fight heal. */
    private void onKill() {
        if (run != null) run.addKill();
        int heal = (int) Math.round(player.getMaxHp() * (bonus.hpOnKill() + bonus.healAfterFight()));
        if (heal > 0 && player.getHp() < player.getMaxHp()) {
            int before = player.getHp();
            player.heal(heal);
            stage.playHeal();
            logDetail("전투 후 HP를 " + (player.getHp() - before) + " 회복했다.");
        }
        if (bonus.mpOnKill() > 0 && player.getMp() < player.getMaxMp()) {
            int before = player.getMp();
            player.restoreMp(bonus.mpOnKill());
            logDetail("MP를 " + (player.getMp() - before) + " 회복했다.");
        }
        refreshStatus();
    }

    private void finishBattle(Result result) {
        setButtonsEnabled(false);
        stage.stopIdle();
        if (result == Result.WIN) {
            appendLog(monster.getName() + "을(를) 물리쳤다!");
            stage.playMonsterDeath();
            stage.showRewards(run != null ? run.exp(monster.getExpReward()) : monster.getExpReward(),
                    run != null ? run.gold(monster.getGoldReward()) : monster.getGoldReward());
            onKill();
        } else if (result == Result.LOSE) {
            appendLog(player.getName() + "은(는) 쓰러졌다...");
        }
        Timer timer = new Timer(scaled(700), e -> onFinish.accept(result));
        timer.setRepeats(false);
        timer.start();
    }
}
