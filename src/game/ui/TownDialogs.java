package game.ui;

import game.model.Dungeon;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Material;
import game.model.Player;
import game.model.Quest;
import game.save.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The three town houses, as notebook-page dialogs (see Sketch): the inn (rest = full HP/MP + save),
 * the quest board (three requests, rewards collected here) and the treasure chest shop.
 */
public final class TownDialogs {
    private TownDialogs() {}

    private static JDialog page(Window owner, String title, int w, int h, Sketch.Page[] out) {
        JDialog d = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(14, 12), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 22));
        d.setContentPane(page);
        d.setSize(w, h);
        d.setLocationRelativeTo(owner);
        out[0] = page;
        return d;
    }

    private static JPanel row(int align, JComponent... cs) {
        JPanel p = new JPanel(new FlowLayout(align, 10, 0));
        p.setOpaque(false);
        for (JComponent c : cs) p.add(c);
        return p;
    }

    // ================= 여관 =================

    public static int innPrice(Player p) {
        return 5 + p.getLevel() * 2;
    }

    /**
     * The inn. Resting costs innPrice() and fully restores HP and MP; onRest runs after (the caller
     * saves the game there). Returns true if the player rested.
     */
    public static boolean inn(Window owner, Player player, Runnable onRest) {
        Sketch.Page[] pg = new Sketch.Page[1];
        JDialog d = page(owner, "여관", 520, 430, pg);
        int price = innPrice(player);
        boolean[] rested = {false};

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("여관"), BorderLayout.WEST);
        head.add(Sketch.label("소지금  " + player.getGold() + "G", Font.BOLD, 16f, Sketch.BLUE_PENCIL), BorderLayout.EAST);
        d.add(head, BorderLayout.NORTH);

        Sketch.Box box = new Sketch.Box(new BorderLayout(16, 0), 8101);
        box.setBorder(new EmptyBorder(18, 20, 18, 20));
        box.add(new JLabel(ItemArt.misc("inn_bed", 110)), BorderLayout.WEST);
        JPanel info = new JPanel(new GridLayout(0, 1, 0, 6));
        info.setOpaque(false);
        info.add(Sketch.label("따뜻한 침대에서 하룻밤 쉬어 갈까요?", Font.BOLD, 15f, Sketch.INK));
        info.add(Sketch.label("HP와 MP가 모두 회복되고, 모험이 기록돼요.", Font.PLAIN, 13f, Sketch.INK_SOFT));
        Sketch.Bar hp = new Sketch.Bar(Sketch.RED_PENCIL, 81);
        hp.set(player.getHp(), player.getMaxHp());
        Sketch.Bar mp = new Sketch.Bar(Sketch.BLUE_PENCIL, 82);
        mp.set(player.getMp(), player.getMaxMp());
        info.add(hp);
        info.add(mp);
        info.add(Sketch.label("숙박비  " + price + "G", Font.BOLD, 17f, Sketch.RED_PENCIL));
        box.add(info, BorderLayout.CENTER);
        d.add(box, BorderLayout.CENTER);

        JButton rest = new Sketch.Button("쉬기 (" + price + "G)", true);
        JButton leave = new Sketch.Button("나가기", false);
        boolean full = player.getHp() >= player.getMaxHp() && player.getMp() >= player.getMaxMp();
        rest.setEnabled(player.getGold() >= price && !full);
        if (full) rest.setText("이미 쌩쌩해요");
        else if (player.getGold() < price) rest.setText("골드가 부족해요");
        rest.addActionListener(e -> {
            player.spendGold(price);
            player.heal(player.getMaxHp());
            player.restoreMp(player.getMaxMp());
            rested[0] = true;
            d.dispose();
        });
        leave.addActionListener(e -> d.dispose());
        d.add(row(FlowLayout.CENTER, leave, rest), BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(rest.isEnabled() ? rest : leave);
        d.setVisible(true);
        if (rested[0] && onRest != null) onRest.run();
        return rested[0];
    }

    // ================= 의뢰 게시판 =================

    public static final int QUEST_SWAP_PRICE = 20;

    /** The quest board: three requests with progress; finished ones pay out here and are replaced. */
    public static void questBoard(Window owner, Player player, List<Dungeon> dungeons, Random rnd) {
        player.fillQuests(dungeons, rnd);
        Sketch.Page[] pg = new Sketch.Page[1];
        JDialog d = page(owner, "의뢰 게시판", 700, 600, pg);

        JLabel gold = Sketch.label("", Font.BOLD, 16f, Sketch.BLUE_PENCIL);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("의뢰 게시판"), BorderLayout.WEST);
        head.add(gold, BorderLayout.EAST);
        d.add(head, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(Quest.BOARD_SIZE, 1, 0, 10));
        list.setOpaque(false);
        d.add(list, BorderLayout.CENTER);
        JLabel note = Sketch.label(" ", Font.BOLD, 14f, Sketch.RED_PENCIL);

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            gold.setText("소지금  " + player.getGold() + "G");
            list.removeAll();
            int i = 0;
            for (Quest q : new ArrayList<>(player.getQuests())) list.add(questCard(q, i++, player, dungeons, rnd, note, refresh));
            list.revalidate();
            d.repaint();
        };
        refresh[0].run();

        JButton leave = new Sketch.Button("나가기", false);
        leave.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(note, BorderLayout.WEST);
        south.add(leave, BorderLayout.EAST);
        d.add(south, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private static JComponent questCard(Quest q, int index, Player player, List<Dungeon> dungeons, Random rnd,
                                        JLabel note, Runnable[] refresh) {
        Sketch.Box card = new Sketch.Box(new BorderLayout(14, 0), 8200 + index * 7L + q.getTitle().hashCode());
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.add(new JLabel(ItemArt.misc("quest_scroll", 64)), BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(0, 1, 0, 2));
        text.setOpaque(false);
        String kind = q.getType() == Quest.Type.BOSS ? "보스 토벌" : "사냥";
        text.add(Sketch.label("[" + kind + "] " + q.getTitle(), Font.BOLD, 15f, q.getType() == Quest.Type.BOSS ? Sketch.RED_PENCIL : Sketch.INK));
        text.add(Sketch.label("장소: " + q.getPlace(dungeons), Font.PLAIN, 12f, Sketch.INK_SOFT));
        text.add(Sketch.label("보상: " + q.getRewardText(), Font.PLAIN, 12f, Sketch.INK_SOFT));
        Sketch.Bar bar = new Sketch.Bar(q.isDone() ? new Color(54, 134, 58) : Sketch.BLUE_PENCIL, 8300 + index);
        bar.set(q.getProgress(), q.getNeed());
        text.add(bar);
        card.add(text, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 6));
        buttons.setOpaque(false);
        JButton claim = new Sketch.Button(q.isDone() ? "보상 받기" : "진행 중", q.isDone());
        claim.setEnabled(q.isDone());
        claim.addActionListener(e -> {
            player.earnGold(q.getGold());
            player.gainExp(q.getExp());
            if (q.getItem() != null) player.addItem(q.getItem());
            player.addMaterial(Material.ERASER_DUST, q.getEraserDust());
            player.replaceQuest(q, dungeons, rnd);
            note.setText("보상을 받았다!  " + q.getRewardText());
            refresh[0].run();
        });
        JButton swap = new Sketch.Button("다른 의뢰 (" + QUEST_SWAP_PRICE + "G)", false);
        swap.setEnabled(!q.isDone() && player.getGold() >= QUEST_SWAP_PRICE);
        swap.addActionListener(e -> {
            player.spendGold(QUEST_SWAP_PRICE);
            player.replaceQuest(q, dungeons, rnd);
            note.setText("새 의뢰가 붙었다.");
            refresh[0].run();
        });
        buttons.add(claim);
        buttons.add(swap);
        buttons.setPreferredSize(new Dimension(170, 0));
        card.add(buttons, BorderLayout.EAST);
        return card;
    }

    // ================= 보물 뽑기 =================

    /** A chest tier: name, icon, price, and grade weights for 일반/고급/희귀/영웅/전설 (boss gear never). */
    private static final class Chest {
        final String name, icon;
        final int price;
        final int[] weights;
        /** Chance (%) that the chest holds potions instead of gear. */
        final int potionChance;
        /** Chance (%) of 지우개 가루 on top of whatever the wheel gives. */
        final int eraserChance;

        Chest(String name, String icon, int price, int potionChance, int eraserChance, int... weights) {
            this.name = name;
            this.icon = icon;
            this.price = price;
            this.potionChance = potionChance;
            this.eraserChance = eraserChance;
            this.weights = weights;
        }
    }

    private static final Chest[] CHESTS = {
            new Chest("나무 상자", "chest_wood", 100, 30, 3, 60, 30, 9, 1, 0),
            new Chest("은 상자", "chest_silver", 400, 10, 10, 15, 40, 33, 10, 2),
            new Chest("황금 상자", "chest_gold", 1200, 0, 30, 0, 10, 45, 35, 10),
    };

    /** One wedge of the wheel: what it gives (a grade, or null = potions), its share, its pencil color. */
    private static final class Wedge {
        final String label;
        final Item.Grade grade;
        final double weight;
        final Color color;

        Wedge(String label, Item.Grade grade, double weight, Color color) {
            this.label = label;
            this.grade = grade;
            this.weight = weight;
            this.color = color;
        }
    }

    /** A chest's wheel: potion wedge (if any) plus one wedge per grade it can give, sized by odds. */
    private static List<Wedge> wedges(Chest c) {
        List<Wedge> out = new ArrayList<>();
        if (c.potionChance > 0) out.add(new Wedge("물약", null, c.potionChance, new Color(220, 90, 110)));
        Item.Grade[] grades = Item.Grade.values();
        for (int i = 0; i < c.weights.length; i++) {
            if (c.weights[i] <= 0) continue;
            out.add(new Wedge(grades[i].getLabel(), grades[i], c.weights[i] * (100 - c.potionChance) / 100.0,
                    Sketch.gradeInk(grades[i])));
        }
        return out;
    }

    /**
     * The treasure shop as a roulette: pick a chest, pay, and its wheel spins; where the pencil pointer
     * stops decides the grade (or potions). The result is rolled first and the wheel is steered to land
     * inside that wedge, so what you see is what you get.
     */
    public static void treasure(Window owner, Player player, Random rnd) {
        Sketch.Page[] pg = new Sketch.Page[1];
        JDialog d = page(owner, "보물 뽑기", 920, 620, pg);

        JLabel gold = Sketch.label("", Font.BOLD, 16f, Sketch.BLUE_PENCIL);
        JButton oddsBtn = new Sketch.Button("확률 보기", false);
        oddsBtn.setFont(Sketch.hand(Font.BOLD, 12f));
        oddsBtn.setBorder(new EmptyBorder(5, 12, 5, 12));
        oddsBtn.addActionListener(e -> showOdds(d));
        JPanel headRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        headRight.setOpaque(false);
        headRight.add(oddsBtn);
        headRight.add(gold);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("보물 뽑기"), BorderLayout.WEST);
        head.add(headRight, BorderLayout.EAST);
        d.add(head, BorderLayout.NORTH);

        Wheel wheel = new Wheel();
        wheel.setWedges(wedges(CHESTS[0]), CHESTS[0].name);

        // Result pane: icon, name, grade + stat.
        Sketch.Box result = new Sketch.Box(new BorderLayout(0, 8), 8501);
        result.setBorder(new EmptyBorder(18, 16, 18, 16));
        result.setPreferredSize(new Dimension(220, 0));
        JLabel resultIcon = new JLabel("", SwingConstants.CENTER);
        JLabel resultName = center(Sketch.label("돌림판을 돌려 보세요!", Font.BOLD, 15f, Sketch.INK_SOFT));
        JLabel resultInfo = center(Sketch.label(" ", Font.PLAIN, 13f, Sketch.INK));
        JLabel resultBonus = center(Sketch.label(" ", Font.BOLD, 13f, Sketch.RED_PENCIL));
        JPanel resultText = new JPanel(new GridLayout(3, 1, 0, 4));
        resultText.setOpaque(false);
        resultText.add(resultName);
        resultText.add(resultInfo);
        resultText.add(resultBonus);
        result.add(resultIcon, BorderLayout.CENTER);
        result.add(resultText, BorderLayout.SOUTH);

        // Chests stacked on the left.
        JPanel chests = new JPanel(new GridLayout(CHESTS.length, 1, 0, 10));
        chests.setOpaque(false);
        chests.setPreferredSize(new Dimension(250, 0));
        List<JButton> openBtns = new ArrayList<>();
        Runnable refresh = () -> {
            gold.setText("소지금  " + player.getGold() + "G");
            for (int i = 0; i < CHESTS.length; i++) openBtns.get(i).setEnabled(!wheel.isSpinning() && player.getGold() >= CHESTS[i].price);
        };
        for (Chest c : CHESTS) {
            Sketch.Box card = new Sketch.Box(new BorderLayout(10, 0), 8600 + c.price);
            card.setBorder(new EmptyBorder(8, 12, 8, 12));
            card.add(new JLabel(ItemArt.misc(c.icon, 72)), BorderLayout.WEST);
            JPanel txt = new JPanel(new GridLayout(3, 1, 0, 2));
            txt.setOpaque(false);
            txt.add(Sketch.label(c.name, Font.BOLD, 16f, Sketch.INK));
            txt.add(Sketch.label(c.price + "G", Font.BOLD, 15f, Sketch.BLUE_PENCIL));
            JButton open = new Sketch.Button("돌리기", true);
            open.setBorder(new EmptyBorder(4, 14, 4, 14));
            open.addActionListener(e -> {
                if (player.getGold() < c.price || wheel.isSpinning()) return;
                player.spendGold(c.price);
                List<Wedge> ws = wedges(c);
                int hit = pick(ws, rnd);
                Wedge w = ws.get(hit);
                Item got = w.grade == null ? ItemCatalog.get(POTIONS[rnd.nextInt(POTIONS.length)])
                        : randomOfGrade(w.grade, rnd);
                player.addItem(got.getName());
                boolean worn = Settings.isAutoEquip() && player.equipIfBetter(got.getName());
                boolean eraser = rnd.nextInt(100) < c.eraserChance;
                if (eraser) player.addMaterial(Material.ERASER_DUST, 1);
                resultIcon.setIcon(null);
                resultName.setText("두근두근...");
                resultName.setForeground(Sketch.INK_SOFT);
                resultInfo.setText(" ");
                resultBonus.setText(" ");
                wheel.setWedges(ws, c.name);
                wheel.spin(hit, rnd, () -> {
                    resultIcon.setIcon(ItemArt.item(got, 110));
                    resultName.setText(got.getName());
                    resultName.setForeground(Sketch.gradeInk(got.getGrade()));
                    resultInfo.setText("[" + got.getGrade().getLabel() + "] " + got.getStatText() + (worn ? " · 바로 장착!" : ""));
                    if (eraser) resultBonus.setText("+ " + Material.ERASER_DUST.getLabel() + " 1개!");
                    refresh.run();
                });
                refresh.run();
            });
            openBtns.add(open);
            txt.add(open);
            card.add(txt, BorderLayout.CENTER);
            chests.add(card);
        }
        refresh.run();

        JPanel center = new JPanel(new BorderLayout(14, 0));
        center.setOpaque(false);
        center.add(chests, BorderLayout.WEST);
        center.add(wheel, BorderLayout.CENTER);
        center.add(result, BorderLayout.EAST);
        d.add(center, BorderLayout.CENTER);

        JButton leave = new Sketch.Button("나가기", false);
        leave.addActionListener(e -> {
            if (!wheel.isSpinning()) d.dispose();
        });
        d.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { if (!wheel.isSpinning()) d.dispose(); }
        });
        d.add(row(FlowLayout.CENTER, leave), BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private static final String[] POTIONS = {"포션", "하이 포션", "에테르"};

    /**
     * Every item's exact chance per chest, worked out the same way the roll does: the wedge's share,
     * split evenly over the items of that grade (or the three potions).
     */
    private static void showOdds(Window owner) {
        Sketch.Page[] pg = new Sketch.Page[1];
        JDialog d = page(owner, "확률 보기", 560, 600, pg);
        d.add(Sketch.title("아이템별 확률"), BorderLayout.NORTH);

        StringBuilder html = new StringBuilder("<html><body style='width:340px'>");
        for (Chest c : CHESTS) {
            html.append("<br><b><font size='4' color='").append(Sketch.hex(Sketch.INK)).append("'>")
                    .append(c.name).append("</font></b>  <font color='").append(Sketch.hex(Sketch.BLUE_PENCIL)).append("'>")
                    .append(c.price).append("G</font><br>");
            for (Wedge w : wedges(c)) {
                List<String> names = new ArrayList<>();
                if (w.grade == null) {
                    for (String n : POTIONS) names.add(n);
                } else {
                    for (Item it : ItemCatalog.treasurePool(w.grade)) names.add(it.getName());
                }
                double each = w.weight / names.size();
                html.append("<font color='").append(Sketch.hex(w.color)).append("'><b>").append(w.label).append(" ")
                        .append(pct(w.weight)).append("</b></font><font color='").append(Sketch.hex(Sketch.INK_SOFT)).append("'>  →  ");
                List<String> parts = new ArrayList<>();
                for (String n : names) parts.add(n + " " + pct(each));
                html.append(String.join(", ", parts)).append("</font><br>");
            }
            html.append("<font color='").append(Sketch.hex(Sketch.INK_SOFT)).append("'>추가로 ").append(Material.ERASER_DUST.getLabel())
                    .append(" 1개: ").append(pct(c.eraserChance)).append("</font><br>");
        }
        html.append("<br><font color='").append(Sketch.hex(Sketch.RED_PENCIL))
                .append("'>※ 보스 전용 [신화] 장비는 보물 상자에서 나오지 않아요.</font></body></html>");
        JLabel text = new JLabel(html.toString());
        text.setFont(Sketch.hand(Font.PLAIN, 13f));
        text.setVerticalAlignment(SwingConstants.TOP);
        d.add(Sketch.scroll(text), BorderLayout.CENTER);

        JButton close = new Sketch.Button("닫기", false);
        close.addActionListener(e -> d.dispose());
        d.add(row(FlowLayout.CENTER, close), BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(close);
        d.setVisible(true);
    }

    /** 21% / 2.5% / 0.33% — whole numbers when whole, otherwise one or two decimals. */
    private static String pct(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return Math.round(v) + "%";
        return (v >= 1 ? String.format("%.1f", v) : String.format("%.2f", v)) + "%";
    }

    private static int pick(List<Wedge> ws, Random rnd) {
        double total = 0;
        for (Wedge w : ws) total += w.weight;
        double r = rnd.nextDouble() * total, acc = 0;
        for (int i = 0; i < ws.size(); i++) {
            acc += ws.get(i).weight;
            if (r < acc) return i;
        }
        return ws.size() - 1;
    }

    private static Item randomOfGrade(Item.Grade grade, Random rnd) {
        List<Item> pool = ItemCatalog.treasurePool(grade);
        return pool.get(rnd.nextInt(pool.size()));
    }

    private static JLabel center(JLabel l) {
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    /** The pencil roulette wheel: colored-pencil wedges, a red pencil pointer at the top, eased spin. */
    private static final class Wheel extends JComponent {
        private static final long SPIN_MS = 3600;
        private List<Wedge> wedges = new ArrayList<>();
        private String title = "";
        private double rotation;   // degrees, clockwise
        private Timer timer;

        Wheel() {
            setPreferredSize(new Dimension(360, 360));
        }

        boolean isSpinning() {
            return timer != null;
        }

        void setWedges(List<Wedge> ws, String title) {
            this.wedges = ws;
            this.title = title;
            repaint();
        }

        /** Which wedge is under the 12 o'clock pointer at this rotation. */
        private int wedgeAt(double rot) {
            double total = 0;
            for (Wedge w : wedges) total += w.weight;
            double a = ((-rot % 360) + 360) % 360, acc = 0;
            for (int i = 0; i < wedges.size(); i++) {
                acc += wedges.get(i).weight / total * 360;
                if (a < acc) return i;
            }
            return wedges.size() - 1;
        }

        /** Spins a few turns and stops with the pointer inside wedge `index` (not too close to its edges). */
        void spin(int index, Random rnd, Runnable onDone) {
            double total = 0;
            for (Wedge w : wedges) total += w.weight;
            double start = 0;
            for (int i = 0; i < index; i++) start += wedges.get(i).weight / total * 360;
            double span = wedges.get(index).weight / total * 360;
            double point = start + span * (0.2 + rnd.nextDouble() * 0.6);   // wheel angle that must end under the pointer
            double from = rotation;
            double target = -point;
            double delta = ((target - from) % 360 + 360) % 360 + 360 * 5;   // always forward, five extra turns
            long t0 = System.currentTimeMillis();
            // 돌림판 건너뛰기: land at once (a single timer tick, so the caller's flow stays the same).
            long spinMs = Settings.isSkipWheel() ? 1 : SPIN_MS;
            double[] last = {from};
            timer = new Timer(16, e -> {
                double t = Math.min(1, (System.currentTimeMillis() - t0) / (double) spinMs);
                double ease = 1 - Math.pow(1 - t, 4);   // fast start, long slow finish
                rotation = from + delta * ease;
                // A tick each time a wedge edge passes the pointer, like a real wheel's flapper.
                if (spinMs > 1 && wedgeAt(rotation) != wedgeAt(last[0])) Sfx.play(Sfx.Sound.TICK);
                last[0] = rotation;
                repaint();
                if (t >= 1) {
                    Sfx.play(Sfx.Sound.WHEEL_STOP);
                    ((Timer) e.getSource()).stop();
                    timer = null;
                    rotation = ((rotation % 360) + 360) % 360;
                    onDone.run();
                }
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = getWidth(), h = getHeight();
            double r = Math.min(w, h - 40) / 2.0 - 16, cx = w / 2.0, cy = 30 + (h - 30) / 2.0;
            double total = 0;
            for (Wedge wd : wedges) total += wd.weight;

            g2.setFont(Sketch.hand(Font.BOLD, 15f));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(Sketch.INK);
            g2.drawString(title, (int) (cx - fm.stringWidth(title) / 2.0), 20);

            // Wedges, clockwise from the top, turned by the current rotation.
            double a0 = rotation;
            int i = 0;
            for (Wedge wd : wedges) {
                double span = wd.weight / total * 360;
                // Arc2D angles run counter-clockwise from 3 o'clock; convert from "clockwise from 12".
                java.awt.geom.Arc2D arc = new java.awt.geom.Arc2D.Double(cx - r, cy - r, r * 2, r * 2, 90 - a0 - span, span,
                        java.awt.geom.Arc2D.PIE);
                g2.setColor(new Color(wd.color.getRed(), wd.color.getGreen(), wd.color.getBlue(), 70));
                g2.fill(arc);
                Sketch.hatch(g2, arc, 9100 + i, wd.color, 150, 6, 30 + i * 37);
                double mid = Math.toRadians(a0 + span / 2 - 90);
                Sketch.line(g2, cx, cy, cx + Math.cos(Math.toRadians(a0 - 90)) * r, cy + Math.sin(Math.toRadians(a0 - 90)) * r,
                        9200 + i, 1.8f, Sketch.INK);
                if (span >= 14) {
                    String text = wd.label + " " + Math.round(wd.weight) + "%";
                    Graphics2D gt = (Graphics2D) g2.create();
                    double lr = r * 0.62;
                    gt.translate(cx + Math.cos(mid) * lr, cy + Math.sin(mid) * lr);
                    // Labels in the lower half are turned the other way, so no text ends up upside down.
                    boolean lower = Math.sin(mid) > 0.05;
                    gt.rotate(lower ? mid - Math.PI / 2 : mid + Math.PI / 2);
                    gt.setFont(Sketch.hand(Font.BOLD, span >= 40 ? 15f : 12f));
                    FontMetrics tm = gt.getFontMetrics();
                    gt.setColor(Sketch.PAPER);
                    gt.drawString(text, -tm.stringWidth(text) / 2f + 1, 5 + 1);
                    gt.setColor(Sketch.INK);
                    gt.drawString(text, -tm.stringWidth(text) / 2f, 5);
                    gt.dispose();
                }
                a0 += span;
                i++;
            }
            Sketch.circle(g2, cx, cy, r, 9301, 2.6f, Sketch.INK);
            // Hub.
            g2.setColor(Sketch.PAPER);
            g2.fill(new java.awt.geom.Ellipse2D.Double(cx - 18, cy - 18, 36, 36));
            Sketch.circle(g2, cx, cy, 18, 9302, 2f, Sketch.INK);
            // Pointer: a red pencil arrow pointing down into the wheel at 12 o'clock.
            java.awt.geom.Path2D ptr = new java.awt.geom.Path2D.Double();
            ptr.moveTo(cx - 16, cy - r - 22);
            ptr.lineTo(cx + 16, cy - r - 22);
            ptr.lineTo(cx, cy - r + 8);
            ptr.closePath();
            g2.setColor(new Color(190, 62, 52, 90));
            g2.fill(ptr);
            Sketch.hatch(g2, ptr, 9303, Sketch.RED_PENCIL, 200, 3.5, 60);
            Sketch.line(g2, cx - 16, cy - r - 22, cx + 16, cy - r - 22, 9304, 2.2f, Sketch.RED_PENCIL);
            Sketch.line(g2, cx + 16, cy - r - 22, cx, cy - r + 8, 9305, 2.2f, Sketch.RED_PENCIL);
            Sketch.line(g2, cx, cy - r + 8, cx - 16, cy - r - 22, 9306, 2.2f, Sketch.RED_PENCIL);
            g2.dispose();
        }
    }
}
