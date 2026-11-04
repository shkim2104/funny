package game.ui;

import game.model.Job;
import game.model.Player;
import game.model.Skill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 전직소 as a notebook page: one tab per class (전사, 마법사, ...); in a tab, a route (e.g. 검사 계열) shows its
 * 1~4차 jobs side by side, pencil arrows between them, each card with what the job is about and every skill
 * it teaches (level, MP, what it does). The next tier's cards carry the button to advance; any class may be
 * picked at any tier, as before. Returns the job chosen (already confirmed), or null.
 */
public final class JobHallDialog {
    private JobHallDialog() {}

    static final int CARD_W = 222;

    /** Routes in Job order: a route starts at each 1차 job and runs until the next one. */
    static Map<String, List<List<Job>>> routesByClass() {
        Map<String, List<List<Job>>> out = new LinkedHashMap<>();
        List<Job> route = null;
        for (Job j : Job.values()) {
            if (j == Job.BEGINNER) continue;
            if (j.getTier() == 1 || route == null) {
                route = new ArrayList<>();
                out.computeIfAbsent(j.getJobClass(), k -> new ArrayList<>()).add(route);
            }
            route.add(j);
        }
        return out;
    }

    public static Job show(JFrame owner, Player player) {
        JDialog dialog = new JDialog(owner, "전직소", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 10), 61);
        page.setBorder(new EmptyBorder(14, 52, 12, 22));
        dialog.setContentPane(page);
        Job[] chosen = {null};

        int next = player.getNextTier();
        StringBuilder path = new StringBuilder();
        for (Job j : player.getJobPath()) path.append(path.length() > 0 ? " → " : "").append(j.getDisplayName());
        String where = (path.length() == 0 ? "아직 초보자" : path.toString()) + "   ·   "
                + (next == 0 ? "모든 전직을 마쳤어요!" : next + "차 전직은 Lv." + Job.levelForTier(next) + "부터 (지금 Lv." + player.getLevel() + ")");
        JPanel head = new JPanel(new BorderLayout(0, 4));
        head.setOpaque(false);
        head.add(Sketch.title("전직소"), BorderLayout.NORTH);
        head.add(Sketch.label(where, Font.BOLD, 15f, next != 0 && player.getLevel() >= Job.levelForTier(next) ? Sketch.RED_PENCIL : Sketch.INK_SOFT), BorderLayout.CENTER);
        head.add(Sketch.label("직업군을 고르고, 계열을 눌러 1차부터 4차까지 어떤 직업과 스킬이 있는지 보세요. 직업군은 차수마다 자유롭게 바꿀 수 있어요.",
                Font.PLAIN, 12f, Sketch.INK_SOFT), BorderLayout.SOUTH);
        dialog.add(head, BorderLayout.NORTH);

        Sketch.Tabs tabs = new Sketch.Tabs(6101);
        int startTab = 0, i = 0;
        for (Map.Entry<String, List<List<Job>>> e : routesByClass().entrySet()) {
            tabs.addTab(e.getKey(), classTab(dialog, player, e.getValue(), chosen));
            if (!player.getJobPath().isEmpty() && player.getJob().getJobClass().equals(e.getKey())) startTab = i;
            i++;
        }
        tabs.setSelected(startTab);
        dialog.add(tabs, BorderLayout.CENTER);

        JButton close = Sketch.bigButton("나가기", false);
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);

        dialog.setSize(1200, 800);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return chosen[0];
    }

    /** A class: its route buttons on top, the chosen route's four tiers below. */
    private static JComponent classTab(JDialog dialog, Player player, List<List<Job>> routes, Job[] chosen) {
        JPanel tab = new JPanel(new BorderLayout(0, 8));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(8, 6, 6, 6));
        CardLayout cards = new CardLayout();
        JPanel views = new JPanel(cards);
        views.setOpaque(false);
        JPanel routeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        routeRow.setOpaque(false);
        List<JButton> routeButtons = new ArrayList<>();
        int start = 0;
        for (int r = 0; r < routes.size(); r++) {
            List<Job> route = routes.get(r);
            String key = "route" + r;
            views.add(Sketch.scroll(routeView(dialog, player, route, chosen)), key);
            JButton b = new Sketch.Button(route.get(0).getDisplayName() + " 계열" + (route.get(0).getFocus().isEmpty() ? "" : " · " + route.get(0).getFocus()), false);
            b.addActionListener(e -> {
                cards.show(views, key);
                for (JButton o : routeButtons) o.setForeground(o == b ? Sketch.RED_PENCIL : Sketch.INK);
            });
            routeButtons.add(b);
            routeRow.add(b);
            for (Job j : route) if (player.getJobPath().contains(j)) start = r;
        }
        tab.add(routeRow, BorderLayout.NORTH);
        tab.add(views, BorderLayout.CENTER);
        routeButtons.get(start).doClick();
        return tab;
    }

    /** One route: a card per tier 1~4 (a gap where the route has no job of that tier), arrows between. */
    private static JComponent routeView(JDialog dialog, Player player, List<Job> route, Job[] chosen) {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.VERTICAL;
        c.insets = new Insets(6, 2, 6, 2);
        int x = 0;
        for (int tier = 1; tier <= Job.MAX_TIER; tier++) {
            Job job = null;
            for (Job j : route) if (j.getTier() == tier) job = j;
            if (tier > 1) {
                c.gridx = x++;
                c.fill = GridBagConstraints.NONE;
                c.anchor = GridBagConstraints.CENTER;
                row.add(new Arrow(tier * 31L + route.get(0).ordinal()), c);
                c.fill = GridBagConstraints.VERTICAL;
                c.anchor = GridBagConstraints.NORTH;
            }
            c.gridx = x++;
            row.add(job == null ? emptyCard(tier) : jobCard(dialog, player, job, chosen), c);
        }
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(row, BorderLayout.NORTH);
        return wrap;
    }

    private static JComponent emptyCard(int tier) {
        Sketch.Box box = new Sketch.Box(new BorderLayout(), 6200 + tier);
        box.setBorder(new EmptyBorder(14, 14, 14, 14));
        box.setPreferredSize(new Dimension(CARD_W, 200));
        box.add(Sketch.label("<html><div style='width:" + (CARD_W - 40) + "px'><b>" + tier + "차</b><br><br>이 계열에는 " + tier
                + "차 직업이 없어요.<br>다른 직업군의 " + tier + "차 직업을 골라 이어 갈 수 있어요.</div></html>", Font.PLAIN, 13f, Sketch.INK_SOFT), BorderLayout.NORTH);
        return box;
    }

    private static JComponent jobCard(JDialog dialog, Player player, Job job, Job[] chosen) {
        boolean current = !player.getJobPath().isEmpty() && player.getJob() == job;
        boolean passed = player.getJobPath().contains(job) && !current;
        int next = player.getNextTier();
        boolean isNext = job.getTier() == next;
        boolean levelOk = player.getLevel() >= job.getRequiredLevel();

        Sketch.Box box = new Sketch.Box(new BorderLayout(0, 8), 6300L + job.ordinal()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (current) {
                    Graphics2D g2 = Sketch.smooth(g);
                    Sketch.box(g2, 3, 3, getWidth() - 6, getHeight() - 6, 6400L + job.ordinal(), 3f, Sketch.RED_PENCIL);
                    g2.dispose();
                }
            }
        };
        box.setBorder(new EmptyBorder(14, 14, 14, 14));
        // The hand font runs wider than HTML measures it: wrap well inside the card.
        int w = CARD_W - 84;
        StringBuilder html = new StringBuilder("<html><div style='width:" + w + "px'>");
        html.append("<font color='").append(Sketch.hex(Sketch.INK_SOFT)).append("'>").append(job.getTier()).append("차 · Lv.").append(job.getRequiredLevel())
                .append(current ? " · <font color='" + Sketch.hex(Sketch.RED_PENCIL) + "'><b>지금 직업</b></font>" : passed ? " · √ 거쳐 온 직업" : "").append("</font><br>");
        html.append("<font size='+1'><b>").append(job.getDisplayName()).append("</b></font><br>");
        if (!job.getFocus().isEmpty()) html.append("<font color='").append(Sketch.hex(Sketch.BLUE_PENCIL)).append("'>").append(job.getFocus()).append("</font><br>");
        html.append("<font color='").append(Sketch.hex(Sketch.INK_SOFT)).append("'>").append(job.getDescription()).append("</font><br><br>");
        html.append("<b>스킬</b><br>");
        for (Skill s : job.getSkills()) {
            html.append("<b>· ").append(s.getName()).append("</b> <font color='").append(Sketch.hex(Sketch.BLUE_PENCIL)).append("'>Lv.")
                    .append(s.getRequiredLevel()).append(" · MP ").append(s.getMpCost()).append("</font><br><font color='")
                    .append(Sketch.hex(Sketch.INK_SOFT)).append("'>").append(s.getDescription()).append("</font><br>");
        }
        html.append("</div></html>");
        JLabel text = new JLabel(html.toString());
        text.setFont(Sketch.hand(Font.PLAIN, 13f));
        text.setForeground(Sketch.INK);
        text.setVerticalAlignment(SwingConstants.TOP);
        box.add(text, BorderLayout.CENTER);

        if (isNext) {
            JButton go = Sketch.bigButton(levelOk ? "이 직업으로 전직" : "Lv." + job.getRequiredLevel() + "부터", true);
            go.setEnabled(levelOk);
            go.addActionListener(e -> {
                if (!Dialogs.confirm(dialog, "전직", job.getDisplayName() + "(으)로 전직할까요?\n한 번 정하면 바꿀 수 없습니다.")) return;
                chosen[0] = job;
                dialog.dispose();
            });
            box.add(go, BorderLayout.SOUTH);
        }
        box.setPreferredSize(new Dimension(CARD_W, Math.max(260, box.getPreferredSize().height)));
        return box;
    }

    /** A pencil arrow from one tier's card to the next. */
    private static final class Arrow extends JComponent {
        final long seed;

        Arrow(long seed) {
            this.seed = seed;
            setPreferredSize(new Dimension(34, 40));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int y = getHeight() / 2;
            Sketch.line(g2, 3, y, getWidth() - 5, y, seed, 2.2f, Sketch.INK_SOFT);
            Sketch.line(g2, getWidth() - 14, y - 8, getWidth() - 4, y, seed + 1, 2.2f, Sketch.INK_SOFT);
            Sketch.line(g2, getWidth() - 14, y + 8, getWidth() - 4, y, seed + 2, 2.2f, Sketch.INK_SOFT);
            g2.dispose();
        }
    }
}
