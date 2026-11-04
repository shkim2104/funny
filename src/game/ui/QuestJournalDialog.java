package game.ui;

import game.model.Dungeon;
import game.model.Player;
import game.model.Quest;
import game.model.Story;
import game.model.StoryMaterial;
import game.model.World;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 퀘스트 (J): the final story's request from ??? in full (every stage, every clue with where and how it
 * drops and how many are in hand) and the quest board's requests. Finished ones carry a big "!". Rewards
 * are taken here only away from a fight: during a dungeon battle it is a page to read.
 */
public final class QuestJournalDialog {
    private QuestJournalDialog() {}

    /** What the window needs from the game. */
    public interface Host {
        /** True while a dungeon fight is on: nothing can be handed in or claimed then. */
        boolean inBattle();
        /** Hands ???'s request in (the game plays the scene); called after this window closes. */
        void handIn();
        /** Pays a finished quest board request and puts up a new one. */
        void claim(Quest q);
    }

    static final Color DONE_GREEN = new Color(54, 134, 58), STORY_PURPLE = new Color(106, 44, 154);
    private static final String[] STAGES = {"1단계", "2단계", "3단계", "약 완성", "최종 보스"};

    /** Something finished waits: ???'s request ready to hand in, the potion made, or a quest board request done. */
    public static boolean anyReady(Player p) {
        return storyReady(p) || p.getQuests().stream().anyMatch(Quest::isDone);
    }

    public static boolean storyReady(Player p) {
        int s = p.getStoryStage();
        return s == Story.POTION_MADE || s >= Story.FIRST_REQUEST && s <= Story.LAST_REQUEST && Story.canHandIn(p, s);
    }

    public static void show(Window owner, Player player, Host host) {
        JDialog d = new JDialog(owner, "퀘스트", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 10), 47);
        page.setBorder(new EmptyBorder(14, 52, 12, 22));
        d.setContentPane(page);
        Runnable[] after = {null};
        int[] tab = {storyReady(player) || player.getStoryStage() >= Story.FIRST_REQUEST && player.getStoryStage() < Story.ENDED ? 0 : 1};
        Runnable[] fill = new Runnable[1];
        fill[0] = () -> {
            page.removeAll();
            JPanel head = new JPanel(new BorderLayout());
            head.setOpaque(false);
            head.add(Sketch.title("퀘스트"), BorderLayout.WEST);
            if (host.inBattle()) {
                head.add(Sketch.label("전투 중에는 보상을 받을 수 없어요. 마을에서 받아 주세요.", Font.BOLD, 14f, Sketch.RED_PENCIL), BorderLayout.EAST);
            }
            page.add(head, BorderLayout.NORTH);
            Sketch.Tabs tabs = new Sketch.Tabs(4701);
            tabs.addTab((storyReady(player) ? "! " : "") + "???의 부탁", Sketch.scroll(storyPage(d, player, host, after)));
            tabs.addTab((player.getQuests().stream().anyMatch(Quest::isDone) ? "! " : "") + "의뢰 게시판", Sketch.scroll(boardPage(player, host, fill)));
            tabs.setSelected(tab[0]);
            tabs.onChange(i -> tab[0] = i);
            page.add(tabs, BorderLayout.CENTER);
            JButton close = Sketch.bigButton("닫기", false);
            close.addActionListener(e -> d.dispose());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
            south.setOpaque(false);
            south.add(close);
            page.add(south, BorderLayout.SOUTH);
            page.revalidate();
            page.repaint();
        };
        fill[0].run();
        d.setSize(900, 780);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
        if (after[0] != null) after[0].run();
    }

    // ---------- ???의 부탁 ----------

    private static JComponent storyPage(JDialog d, Player p, Host host, Runnable[] after) {
        int stage = p.getStoryStage();
        JPanel col = column();
        col.add(stageTrail(stage));
        if (stage < Story.FIRST_REQUEST) {
            col.add(note("아직 시작되지 않은 이야기예요.", "7던전 '천공의 성채'를 처음 깨면 하늘이 갈라지고, 마을 광장에 ???가 나타나요.", Sketch.INK_SOFT, false));
            return wrap(col);
        }
        if (stage == Story.POTION_MADE) {
            col.add(note("이계 약화 물약이 완성됐어요!", "마을 하늘의 균열을 눌러 이계의 존재와 싸우세요. 물약 덕분에 그것은 많이 약해져 있어요.", STORY_PURPLE, true));
            return wrap(col);
        }
        if (stage >= Story.ENDED) {
            col.add(note("이야기를 모두 마쳤어요.", "이계의 존재를 쓰러뜨리고 패시브 '" + Story.PASSIVE_NAME + "'(모든 능력치 +5%)를 얻었어요.", DONE_GREEN, false));
            return wrap(col);
        }
        boolean ready = Story.canHandIn(p, stage);
        col.add(Sketch.label(Story.requestTitle(stage), Font.BOLD, 20f, STORY_PURPLE));
        col.add(Sketch.label("<html><div style='width:560px'>" + Story.requestSummary(stage) + "</div></html>", Font.PLAIN, 14f, Sketch.INK));
        int i = 0;
        for (Map.Entry<StoryMaterial, Integer> e : Story.request(stage).entrySet()) {
            col.add(materialRow(e.getKey(), p.getStoryMaterial(e.getKey()), e.getValue(), i++));
        }
        if (ready) {
            col.add(note("모두 모았어요!", "마을 광장의 ???에게 건네주면 다음 이야기가 이어져요.", DONE_GREEN, true));
            JButton give = Sketch.bigButton("???에게 건네주기", true);
            give.setEnabled(!host.inBattle());
            give.addActionListener(e -> {
                after[0] = host::handIn;
                d.dispose();
            });
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            row.setOpaque(false);
            row.add(give);
            col.add(row);
        } else {
            col.add(Sketch.label("<html><div style='width:560px'>단서는 그 재료가 나오는 던전에서만, 그리고 지금 부탁이 열려 있을 때만 떨어져요.</div></html>",
                    Font.PLAIN, 12f, Sketch.INK_SOFT));
        }
        return wrap(col);
    }

    /** 1단계 → 2단계 → 3단계 → 약 완성 → 최종 보스: done ones ticked, the current one circled. */
    private static JComponent stageTrail(int stage) {
        JComponent trail = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = Sketch.smooth(g);
                int n = STAGES.length, w = getWidth(), step = Math.max(110, (w - 20) / n);
                g2.setFont(Sketch.hand(Font.BOLD, 15f));
                FontMetrics fm = g2.getFontMetrics();
                for (int i = 0; i < n; i++) {
                    int at = i + 1, x = 10 + i * step;
                    boolean done = stage > at || stage >= Story.ENDED, now = stage == at;
                    String s = (done ? "√ " : "") + STAGES[i];
                    int tw = fm.stringWidth(s);
                    if (now) Sketch.circle(g2, x + tw / 2.0, 22, Math.max(28, tw * 0.65), 4800 + i, 2.4f, Sketch.RED_PENCIL);
                    g2.setColor(done ? DONE_GREEN : now ? Sketch.RED_PENCIL : Sketch.INK_SOFT);
                    g2.drawString(s, x, 28);
                    if (i < n - 1) Sketch.line(g2, x + tw + 10, 22, x + step - 14, 22, 4810 + i, 1.6f, Sketch.INK_SOFT);
                }
                g2.dispose();
            }
        };
        trail.setPreferredSize(new Dimension(680, 46));
        trail.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return trail;
    }

    /** One clue: its picture, where it drops and how, and how many are in hand out of how many. Also for ???'s own window. */
    static JComponent materialRow(StoryMaterial m, int have, int need, int index) {
        Sketch.Box card = new Sketch.Box(new BorderLayout(14, 0), 9801L + index * 13);
        card.setBorder(new EmptyBorder(10, 16, 10, 16));
        card.add(new JLabel(ItemArt.storyMaterial(m, 56)), BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(3, 1, 0, 2));
        text.setOpaque(false);
        boolean done = have >= need;
        text.add(Sketch.label(m.getLabel() + "   " + Math.min(have, need) + " / " + need + (done ? "  √ 다 모았어요" : ""), Font.BOLD, 16f, done ? DONE_GREEN : Sketch.INK));
        text.add(Sketch.label("어디서: " + dungeonName(m.getDungeon()) + "   ·   어떻게: 일반 몬스터 " + StoryMaterial.KILL_DROP_PCT
                + "% 확률로 1개, 보스는 " + StoryMaterial.BOSS_COUNT + "개 확정", Font.PLAIN, 13f, Sketch.INK_SOFT));
        Sketch.Bar bar = new Sketch.Bar(done ? DONE_GREEN : STORY_PURPLE, 9850L + index);
        bar.set(Math.min(have, need), need);
        text.add(bar);
        card.add(text, BorderLayout.CENTER);
        if (done) card.add(new Bang(9870L + index), BorderLayout.EAST);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        return card;
    }

    private static List<Dungeon> dungeons;

    static String dungeonName(int d) {
        if (dungeons == null) dungeons = World.buildDungeons();
        return d >= 0 && d < dungeons.size() ? dungeons.get(d).getName() : "";
    }

    // ---------- 의뢰 게시판 ----------

    private static JComponent boardPage(Player p, Host host, Runnable[] fill) {
        JPanel col = column();
        if (p.getQuests().isEmpty()) {
            col.add(note("붙어 있는 의뢰가 없어요.", "마을 의뢰소에 가면 새 의뢰가 붙어요.", Sketch.INK_SOFT, false));
            return wrap(col);
        }
        int i = 0;
        for (Quest q : p.getQuests()) {
            Sketch.Box card = new Sketch.Box(new BorderLayout(14, 0), 8200 + i * 7L + q.getTitle().hashCode());
            card.setBorder(new EmptyBorder(12, 16, 12, 16));
            card.add(new JLabel(ItemArt.misc("quest_scroll", 56)), BorderLayout.WEST);
            JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
            text.setOpaque(false);
            text.add(Sketch.label("[" + q.getKind() + "] " + q.getTitle() + "   " + q.getProgress() + " / " + q.getNeed() + (q.isDone() ? "  √ 완료" : ""),
                    Font.BOLD, 15f, q.isDone() ? DONE_GREEN : q.getType() != Quest.Type.HUNT ? Sketch.RED_PENCIL : Sketch.INK));
            text.add(Sketch.label("장소: " + dungeonName(q.getDungeon()), Font.PLAIN, 12f, Sketch.INK_SOFT));
            text.add(Sketch.label("보상: " + q.getRewardText(), Font.PLAIN, 12f, Sketch.INK_SOFT));
            Sketch.Bar bar = new Sketch.Bar(q.isDone() ? DONE_GREEN : Sketch.BLUE_PENCIL, 8300 + i);
            bar.set(q.getProgress(), q.getNeed());
            text.add(bar);
            card.add(text, BorderLayout.CENTER);
            if (q.isDone()) {
                JPanel east = new JPanel(new BorderLayout(8, 0));
                east.setOpaque(false);
                east.add(new Bang(8400L + i), BorderLayout.WEST);
                JButton claim = Sketch.bigButton("보상 받기", true);
                claim.setEnabled(!host.inBattle());
                claim.addActionListener(e -> {
                    host.claim(q);
                    fill[0].run();
                });
                east.add(claim, BorderLayout.CENTER);
                card.add(east, BorderLayout.EAST);
            }
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
            col.add(card);
            i++;
        }
        col.add(Sketch.label("끝낸 의뢰는 여기나 마을 의뢰소에서 보상을 받을 수 있어요 (던전 전투 중에는 안 돼요).", Font.PLAIN, 12f, Sketch.INK_SOFT));
        return wrap(col);
    }

    // ---------- small pieces ----------

    private static JPanel column() {
        JPanel col = new JPanel() {
            @Override
            public Component add(Component c) {
                if (c instanceof JComponent) ((JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
                Component r = super.add(c);
                super.add(Box.createVerticalStrut(10));
                return r;
            }
        };
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBorder(new EmptyBorder(10, 8, 10, 8));
        return col;
    }

    private static JComponent wrap(JComponent col) {
        JPanel w = new JPanel(new BorderLayout());
        w.setOpaque(false);
        w.add(col, BorderLayout.NORTH);
        return w;
    }

    /** A boxed note: a bold line and a softer one, with the big "!" when something waits. */
    private static JComponent note(String title, String body, Color ink, boolean bang) {
        Sketch.Box box = new Sketch.Box(new BorderLayout(14, 0), title.hashCode());
        box.setBorder(new EmptyBorder(12, 16, 12, 16));
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 4));
        text.setOpaque(false);
        text.add(Sketch.label(title, Font.BOLD, 17f, ink));
        text.add(Sketch.label("<html><div style='width:500px'>" + body + "</div></html>", Font.PLAIN, 13f, Sketch.INK_SOFT));
        box.add(text, BorderLayout.CENTER);
        if (bang) box.add(new Bang(title.hashCode() + 1L), BorderLayout.WEST);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, box.getPreferredSize().height));
        return box;
    }

    /** The "!" mark: a red pencil exclamation in a yellow circle. Also drawn on the town map (MapScreen). */
    static final class Bang extends JComponent {
        final long seed;

        Bang(long seed) {
            this.seed = seed;
            setPreferredSize(new Dimension(52, 52));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            paintBang(g2, getWidth() / 2.0, getHeight() / 2.0, 22, seed);
            g2.dispose();
        }
    }

    /** Draws the "!" mark centred at (cx, cy) with radius r. */
    public static void paintBang(Graphics2D g2, double cx, double cy, double r, long seed) {
        g2.setColor(new Color(255, 226, 96, 235));
        g2.fill(new java.awt.geom.Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        Sketch.circle(g2, cx, cy, r, seed, 2.4f, Sketch.RED_PENCIL);
        Sketch.line(g2, cx, cy - r * 0.62, cx, cy + r * 0.2, seed + 1, (float) Math.max(3, r * 0.22), Sketch.RED_PENCIL);
        g2.setColor(Sketch.RED_PENCIL);
        double dot = Math.max(3, r * 0.2);
        g2.fill(new java.awt.geom.Ellipse2D.Double(cx - dot / 2, cy + r * 0.42 - dot / 2, dot, dot));
    }
}
