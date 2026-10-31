package game.ui;

import game.model.Player;
import game.model.PvpBattle;
import game.model.Skill;
import game.net.PvpSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The colosseum: the same battle screen as a dungeon (stage, status boxes, message window, commands,
 * log), with the other player's drawn hero standing where a monster would. Both players pick in
 * secret against a clock; when both have, the round plays out here and on the other screen.
 */
public class PvpPanel extends JPanel implements PvpSession.Listener {
    private static final int TURN_SECONDS = 30, SKILL_SLOTS = 4, PAUSE_MS = 420;
    private static final int ACTION_AREA_HEIGHT = 128, LOG_MIN_WIDTH = 170, LOG_MAX_WIDTH = 300;
    private static final Color FIGHT_RED = new Color(222, 74, 62), BAG_YELLOW = new Color(226, 164, 36),
            RUN_BLUE = new Color(62, 126, 220), BACK_GRAY = new Color(112, 118, 132);

    private final BattleStage stage = new BattleStage();
    private final BattleHud hud = stage.getHud();
    private final JTextArea log = new JTextArea();
    private final MessageBox prompt = new MessageBox(new BorderLayout());
    private final MessageBox skillPrompt = new MessageBox(new BorderLayout());
    private final MessageBox endPrompt = new MessageBox(new BorderLayout());
    private final CommandButton attackBtn = new CommandButton("공격", FIGHT_RED);
    private final CommandButton potionBtn = new CommandButton("포션", BAG_YELLOW);
    private final CommandButton surrenderBtn = new CommandButton("항복", RUN_BLUE);
    private final CommandButton basicBtn = new CommandButton("기본 공격", FIGHT_RED);
    private final CommandButton backBtn = new CommandButton("뒤로", BACK_GRAY);
    private final CommandButton leaveBtn = new CommandButton("차원의 길로", RUN_BLUE);
    private final CommandButton[] slots = new CommandButton[SKILL_SLOTS];
    private final int[] slotAction = new int[SKILL_SLOTS];
    private final CardLayout actionCards = new CardLayout();
    private final JPanel actionArea = new JPanel(actionCards);

    private PvpSession session;
    private Player me, opponent;
    private Runnable onExit;
    private final Deque<PvpBattle.Event> playing = new ArrayDeque<>();
    private Timer turnTimer;
    private int secondsLeft;
    /** Counts hits so a multi-hit skill's effect alternates direction. */
    private int hitCount;
    private boolean chosen, ended;

    public PvpPanel() {
        setLayout(new BorderLayout(12, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        attackBtn.addActionListener(e -> openSkills());
        potionBtn.addActionListener(e -> pick(PvpBattle.POTION));
        surrenderBtn.addActionListener(e -> surrender());
        basicBtn.addActionListener(e -> pick(PvpBattle.ATTACK));
        backBtn.addActionListener(e -> actionCards.show(actionArea, "MAIN"));
        leaveBtn.addActionListener(e -> leave());

        actionArea.setOpaque(false);
        actionArea.setPreferredSize(new Dimension(0, ACTION_AREA_HEIGHT));
        actionArea.add(buildMain(), "MAIN");
        actionArea.add(buildSkills(), "SKILLS");
        actionArea.add(buildEnd(), "END");

        JPanel battleColumn = new JPanel(new BorderLayout(0, 10));
        battleColumn.setOpaque(false);
        battleColumn.add(stage, BorderLayout.CENTER);
        battleColumn.add(actionArea, BorderLayout.SOUTH);
        add(battleColumn, BorderLayout.CENTER);
        add(buildLog(), BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Sketch.smooth(g);
        Sketch.paper(g2, getWidth(), getHeight(), 8);
        g2.dispose();
    }

    private JComponent buildMain() {
        JPanel commands = new JPanel(new GridBagLayout());
        commands.setOpaque(false);
        commands.setPreferredSize(new Dimension(280, 0));
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
        commands.add(potionBtn, c);
        c.gridy = 1;
        c.insets = new Insets(4, 0, 0, 0);
        commands.add(surrenderBtn, c);
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(prompt, BorderLayout.CENTER);
        row.add(commands, BorderLayout.EAST);
        return row;
    }

    private JComponent buildSkills() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 8, 8));
        grid.setOpaque(false);
        for (int i = 0; i < SKILL_SLOTS; i++) {
            int slot = i;
            slots[i] = new CommandButton("", BACK_GRAY);
            slots[i].addActionListener(e -> pick(slotAction[slot]));
            grid.add(slots[i]);
        }
        JPanel side = new JPanel(new GridLayout(2, 1, 0, 8));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(130, 0));
        side.add(basicBtn);
        side.add(backBtn);
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(grid, BorderLayout.CENTER);
        row.add(side, BorderLayout.EAST);
        return row;
    }

    private JComponent buildEnd() {
        leaveBtn.setPreferredSize(new Dimension(200, 0));
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.add(endPrompt, BorderLayout.CENTER);
        row.add(leaveBtn, BorderLayout.EAST);
        return row;
    }

    private JComponent buildLog() {
        JLabel title = new JLabel("대전 기록");
        title.setFont(Sketch.hand(Font.BOLD, 16f));
        title.setForeground(BattleHud.INK);
        title.setBorder(new EmptyBorder(0, 0, 8, 0));
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setOpaque(false);
        log.setForeground(BattleHud.INK);
        log.setFont(Sketch.hand(Font.PLAIN, 14f));
        JScrollPane scroll = Theme.scroll(log);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        MessageBox window = new MessageBox(new BorderLayout()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(Math.max(LOG_MIN_WIDTH, Math.min(LOG_MAX_WIDTH, Math.round(PvpPanel.this.getWidth() * 0.22f))), 0);
            }
        };
        window.add(title, BorderLayout.NORTH);
        window.add(scroll, BorderLayout.CENTER);
        return window;
    }

    /** Takes over a connected session. onExit runs when the player leaves the arena. */
    public void start(PvpSession session, Player me, CharacterArt myArt, Player opponent, CharacterArt opponentArt, Runnable onExit) {
        this.session = session;
        this.me = me;
        this.opponent = opponent;
        this.onExit = onExit;
        ended = false;
        playing.clear();
        log.setText("");
        session.setListener(this);

        hud.setExpVisible(false);
        stage.setDungeon(BattleStage.ARENA);
        stage.setPlayerArt(myArt);
        stage.setOpponentArt(opponentArt);
        stage.setMonsterName(opponent.getName());
        stage.startIdle();
        PvpBattle b = session.getBattle();
        int mine = session.mySide(), theirs = session.theirSide();
        hud.setPlayer(me.getName(), me.getJob().getDisplayName(), me.getLevel(), b.getHp(mine), b.getMaxHp(mine), b.getMp(mine), me.getMaxMp(), 0, 1);
        hud.snapPlayer();
        hud.setMonster(opponentTitle(), b.getHp(theirs), b.getMaxHp(theirs), true);
        addLog("=== " + me.getName() + " 대 " + opponent.getName() + " ===");
        startRound();
    }

    private String opponentTitle() {
        return opponent.getName() + " Lv." + opponent.getLevel() + " " + opponent.getJob().getDisplayName();
    }

    private void startRound() {
        chosen = false;
        secondsLeft = TURN_SECONDS;
        PvpBattle b = session.getBattle();
        potionBtn.setText("포션 " + b.getPotions(session.mySide()));
        setCommandsEnabled(true);
        refreshSlots();
        actionCards.show(actionArea, "MAIN");
        showClock();
        turnTimer = new Timer(1000, e -> {
            secondsLeft--;
            if (!chosen) showClock();
            if (secondsLeft <= 0) {
                ((Timer) e.getSource()).stop();
                if (!chosen) pick(PvpBattle.ATTACK);
            }
        });
        turnTimer.start();
    }

    private void showClock() {
        String line = me.getName() + "은(는) 무엇을 할까?  (" + secondsLeft + "초)";
        prompt.setText(line);
        skillPrompt.setText(line);
    }

    /** The newest skills fill the slots: they are the strongest. */
    private void refreshSlots() {
        List<Skill> skills = me.getSkills();
        PvpBattle b = session.getBattle();
        for (int i = 0; i < SKILL_SLOTS; i++) {
            int index = skills.size() - SKILL_SLOTS + i;
            if (index < 0) {
                slots[i].setText("빈 슬롯");
                slots[i].setEnabled(false);
                continue;
            }
            Skill s = skills.get(index);
            slotAction[i] = index;
            slots[i].setText(s.getName());
            slots[i].setEnabled(b.isLegal(session.mySide(), index));
        }
    }

    private void openSkills() {
        if (me.getSkills().isEmpty()) pick(PvpBattle.ATTACK);
        else actionCards.show(actionArea, "SKILLS");
    }

    private void setCommandsEnabled(boolean on) {
        attackBtn.setEnabled(on);
        potionBtn.setEnabled(on && session.getBattle().isLegal(session.mySide(), PvpBattle.POTION));
        surrenderBtn.setEnabled(on);
    }

    private void pick(int action) {
        if (chosen || ended) return;
        chosen = true;
        if (turnTimer != null) turnTimer.stop();
        setCommandsEnabled(false);
        surrenderBtn.setEnabled(true);   // still free to give up while the other player decides
        actionCards.show(actionArea, "MAIN");
        prompt.setText("상대의 행동을 기다리는 중...");
        session.choose(action);
    }

    private void surrender() {
        if (ended || !Dialogs.confirm(this, "항복", "대전을 포기할까요?")) return;
        if (ended) return;   // the duel ended while the question was up
        session.surrender();
        finish("항복했어요...");
    }

    @Override public void onReady(Player other, List<String> art) {
        // Already past this point: the lobby handled it before handing the session over.
    }

    @Override public void onOpponentChose() {
        if (!chosen) addLog("상대가 행동을 골랐다!");
        else prompt.setText("양쪽 모두 골랐다!");
    }

    @Override public void onRound(List<PvpBattle.Event> events) {
        surrenderBtn.setEnabled(false);
        playing.addAll(events);
        playNext();
    }

    private void playNext() {
        if (ended) return;
        PvpBattle.Event e = playing.poll();
        if (e == null) {
            finishRound();
            return;
        }
        boolean mine = e.side == session.mySide();
        String line = e.text + (e.damage > 0 ? " " + e.damage + (e.crit ? "의 치명타!" : "의 피해!") : "");
        addLog(line);
        prompt.setText(line);
        if (e.hit) {
            stage.animateAttack(mine, () -> {
                showNumbers(e);
                if (e.damage > 0) stage.showDamage(e.damage, mine, e.crit);
                if (!e.fx.isEmpty()) stage.playSkillFx(Skill.Fx.valueOf(e.fx), e.fxColor, hitCount++, !mine);
            }, this::pauseThenNext);
        } else {
            showNumbers(e);
            if (mine) stage.playHeal();
            pauseThenNext();
        }
    }

    private void pauseThenNext() {
        Timer t = new Timer(PAUSE_MS, e -> playNext());
        t.setRepeats(false);
        t.start();
    }

    private void showNumbers(PvpBattle.Event e) {
        PvpBattle b = session.getBattle();
        int mine = session.mySide(), theirs = session.theirSide();
        hud.setPlayer(me.getName(), me.getJob().getDisplayName(), me.getLevel(), e.hp[mine], b.getMaxHp(mine), e.mp[mine], me.getMaxMp(), 0, 1);
        hud.setMonster(opponentTitle(), e.hp[theirs], b.getMaxHp(theirs), false);
    }

    private void finishRound() {
        PvpBattle b = session.getBattle();
        if (!b.isOver()) {
            startRound();
            return;
        }
        boolean won = b.getWinner() == session.mySide();
        if (won) stage.playMonsterDeath();
        Sfx.play(won ? Sfx.Sound.LEVEL_UP : Sfx.Sound.HURT);
        finish(won ? "승리했다! 콜로세움에 함성이 울려 퍼진다!" : "패배했다... 다음에는 꼭 이기자.");
    }

    @Override public void onClosed(String reason) {
        if (!ended) finish(reason);
    }

    private void finish(String text) {
        ended = true;
        if (turnTimer != null) turnTimer.stop();
        addLog("=== " + text + " ===");
        endPrompt.setText(text);
        actionCards.show(actionArea, "END");
    }

    private void leave() {
        stage.stopIdle();
        stage.setOpponentArt(null);
        session.close();
        onExit.run();
    }

    private void addLog(String line) {
        log.append(line + "\n");
        log.setCaretPosition(log.getDocument().getLength());
    }
}
