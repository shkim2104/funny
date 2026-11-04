package game.ui;

import game.model.Augment;
import game.model.DevMode;
import game.model.Dungeon;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Job;
import game.model.Material;
import game.model.Player;
import game.model.RunState;
import game.model.Story;
import game.model.Tutorial;
import game.model.World;
import game.save.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 개발자 모드 window (F12 or the town button, for a character named DevMode.NAME): four tabs to set the
 * character, skip ahead, switch the battle cheats and hand out items. Changes are saved at once.
 */
public final class DevDialog {
    private DevDialog() {}

    /** What the window needs from the game around it. */
    public interface Host {
        /** The dungeon run in progress (null outside a fight): augments go here. */
        RunState run();
        void refreshRun();
        /** False in a fight: jumping elsewhere only from town or the maps. */
        boolean canTravel();
        void jumpTo(int dungeon, int floor, boolean boss);
        void finale();
        /** Something about the character changed: save it and redraw. */
        void changed();
    }

    /** How far one press of "+" moves: materials by a thousand, gold by a hundred thousand. */
    static final int MATERIAL_STEP = 1000, GOLD_STEP = 100_000, POINT_STEP = 10, GOLD_MAX = 990_000_000;

    static final String[] STORY = {"시작 전", "???의 부탁 1단계", "부탁 2단계", "부탁 3단계", "약화 물약 완성", "엔딩 후"};

    public static void show(JFrame owner, Player player, Host host) {
        if (!DevMode.isDev(player)) return;
        JDialog dialog = new JDialog(owner, "개발자 모드", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(0, 10), 77);
        page.setBorder(new EmptyBorder(14, 48, 12, 22));
        dialog.setContentPane(page);

        JLabel note = Sketch.label(" ", Font.BOLD, 13f, Sketch.BLUE_PENCIL);
        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("개발자 모드"), BorderLayout.WEST);
        head.add(note, BorderLayout.EAST);
        dialog.add(head, BorderLayout.NORTH);

        Sketch.Tabs tabs = new Sketch.Tabs(7701);
        tabs.addTab("캐릭터", characterTab(player, host, note));
        tabs.addTab("진행", progressTab(dialog, player, host, note));
        tabs.addTab("전투", battleTab(player, host, note));
        tabs.addTab("아이템", itemTab(player, host, note));
        dialog.add(tabs, BorderLayout.CENTER);

        JButton close = Sketch.bigButton("닫기", false);
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);

        // Tall enough that the character tab's 적용 is on screen without scrolling.
        dialog.setSize(860, 900);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    // ---------- small pieces ----------

    private static JLabel text(String s) {
        return Sketch.label(s, Font.BOLD, 14f, Sketch.INK);
    }

    private static Sketch.Stepper spinner(int value, int min, int max, int step) {
        return new Sketch.Stepper(value, min, max, step);
    }

    private static <T> JComboBox<T> combo(T[] items) {
        JComboBox<T> c = new JComboBox<>(items);
        c.setFont(Sketch.hand(Font.PLAIN, 14f));
        return c;
    }

    private static JCheckBox check(String s, boolean on) {
        JCheckBox c = new JCheckBox(s, on);
        c.setOpaque(false);
        c.setFont(Sketch.hand(Font.BOLD, 15f));
        c.setForeground(Sketch.INK);
        return c;
    }

    /** Label-control rows in a two-column grid, inside a pencil box. */
    private static Sketch.Box form(long seed, Object... labelAndControl) {
        Sketch.Box box = new Sketch.Box(new GridBagLayout(), seed);
        box.setBorder(new EmptyBorder(12, 16, 12, 16));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i + 1 < labelAndControl.length; i += 2) {
            c.gridx = 0;
            c.gridy = i / 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            Object l = labelAndControl[i];
            box.add(l instanceof Component ? (Component) l : text(String.valueOf(l)), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            box.add((Component) labelAndControl[i + 1], c);
        }
        return box;
    }

    private static JPanel column(Component... parts) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        for (Component k : parts) {
            if (k instanceof JComponent) ((JComponent) k).setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(k);
            p.add(Box.createVerticalStrut(8));
        }
        return p;
    }

    // ---------- 캐릭터 ----------

    private static JComponent characterTab(Player p, Host host, JLabel note) {
        Sketch.Stepper level = spinner(p.getLevel(), 1, 99, 1);
        List<JComboBox<Object>> jobs = new ArrayList<>();
        for (int t = 1; t <= Job.MAX_TIER; t++) {
            List<Object> opts = new ArrayList<>();
            opts.add("없음");
            opts.addAll(Job.ofTier(t));
            JComboBox<Object> c = combo(opts.toArray());
            c.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object v, int i, boolean sel, boolean focus) {
                    return super.getListCellRendererComponent(list, v instanceof Job ? ((Job) v).getDisplayName() + " (" + ((Job) v).getJobClass() + ")" : v, i, sel, focus);
                }
            });
            if (p.getJobPath().size() >= t) c.setSelectedItem(p.getJobPath().get(t - 1));
            jobs.add(c);
        }
        Sketch.Stepper gold = spinner(p.getGold(), 0, GOLD_MAX, GOLD_STEP);
        List<Sketch.Stepper> mats = new ArrayList<>();
        for (Material m : Material.values()) mats.add(spinner(p.getMaterial(m), 0, 9_999_000, MATERIAL_STEP));
        Sketch.Stepper points = spinner(0, -9999, 9999, POINT_STEP);
        JCheckBox heal = check("HP·MP 모두 회복", true);

        JButton apply = Sketch.bigButton("적용", true);
        apply.addActionListener(e -> {
            p.devSetLevel(level.getValue());
            List<Job> path = new ArrayList<>();
            for (JComboBox<Object> c : jobs) {
                if (!(c.getSelectedItem() instanceof Job)) break;
                path.add((Job) c.getSelectedItem());
            }
            p.setJobPath(path);
            p.devSetGold(gold.getValue());
            for (int i = 0; i < mats.size(); i++) p.devSetMaterial(Material.values()[i], mats.get(i).getValue());
            p.devAddStatPoints(points.getValue());
            points.setValue(0);
            if (heal.isSelected()) {
                p.heal(p.getMaxHp());
                p.restoreMp(p.getMaxMp());
            }
            host.changed();
            note.setText("Lv." + p.getLevel() + " " + p.getJob().getDisplayName() + " 적용!");
        });

        List<Object> rows = new ArrayList<>(List.of("레벨", level));
        for (int t = 0; t < jobs.size(); t++) rows.addAll(List.of((t + 1) + "차 전직", jobs.get(t)));
        rows.addAll(List.of("골드", gold));
        for (int i = 0; i < mats.size(); i++) rows.addAll(List.of(Material.values()[i].getLabel(), mats.get(i)));
        rows.addAll(List.of("스탯 포인트 더하기", points, "", heal, "", apply));
        return Sketch.scroll(column(form(7711, rows.toArray()),
                Sketch.label("전직은 위 차수부터 차례로 들어가요. '없음' 아래 차수는 무시돼요.", Font.PLAIN, 12f, Sketch.INK_SOFT)));
    }

    // ---------- 진행 ----------

    private static JComponent progressTab(JDialog dialog, Player p, Host host, JLabel note) {
        List<Dungeon> dungeons = World.buildDungeons();
        String[] names = dungeons.stream().map(Dungeon::getName).toArray(String[]::new);
        // "N던전까지 열기": picking dungeon N opens it and every one before it, saved at once (no 적용 to forget).
        String[] upTo = new String[names.length];
        for (int i = 0; i < names.length; i++) upTo[i] = (i + 1) + "던전까지 (" + names[i] + (i > 0 ? ", 1~" + (i + 1) + "던전 모두" : "") + ")";
        JComboBox<String> unlocked = combo(upTo);
        unlocked.setSelectedIndex(Math.min(p.getUnlockedDungeon(), names.length - 1));
        unlocked.addActionListener(e -> {
            p.setUnlockedDungeon(unlocked.getSelectedIndex());
            host.changed();
            note.setText("1~" + (unlocked.getSelectedIndex() + 1) + "던전 열림!");
        });
        JComboBox<String> story = combo(STORY);
        story.setSelectedIndex(Math.max(0, Math.min(STORY.length - 1, p.getStoryStage())));
        story.addActionListener(e -> {
            p.setStoryStage(story.getSelectedIndex());
            host.changed();
            note.setText("최종 스토리: " + STORY[story.getSelectedIndex()]);
        });
        JCheckBox tutorialDone = check("튜토리얼 끝내기", p.getTutorialStep() == Tutorial.DONE);
        tutorialDone.setEnabled(!tutorialDone.isSelected());
        tutorialDone.addActionListener(e -> {
            if (!tutorialDone.isSelected()) return;
            p.setTutorialStep(Tutorial.DONE);
            tutorialDone.setEnabled(false);
            host.changed();
            note.setText("튜토리얼 끝!");
        });

        JComboBox<String> where = combo(names);
        where.setSelectedIndex(Math.min(p.getUnlockedDungeon(), names.length - 1));
        String[] stages = new String[World.FLOORS + 1];
        for (int f = 1; f <= World.FLOORS; f++) stages[f - 1] = f + " 스테이지" + (f == Dungeon.MID_FLOOR ? " (중간 보스)" : "");
        stages[World.FLOORS] = "보스";
        JComboBox<String> stage = combo(stages);
        stage.setSelectedIndex(Dungeon.MID_FLOOR - 1);
        JButton go = Sketch.bigButton("바로 가기", true);
        go.addActionListener(e -> {
            int s = stage.getSelectedIndex();
            dialog.dispose();
            host.jumpTo(where.getSelectedIndex(), Math.min(s + 1, World.FLOORS), s == World.FLOORS);
        });
        JButton finale = Sketch.bigButton("최종 보스전 바로 시작", false);
        finale.addActionListener(e -> {
            dialog.dispose();
            host.finale();
        });
        go.setEnabled(host.canTravel());
        finale.setEnabled(host.canTravel());

        return Sketch.scroll(column(
                form(7721, "던전 열기", unlocked, "최종 스토리", story, "", tutorialDone,
                        "", Sketch.label("고르는 즉시 저장돼요.", Font.PLAIN, 12f, Sketch.INK_SOFT)),
                form(7722, "던전", where, "스테이지", stage, "", go, "", finale),
                Sketch.label(host.canTravel() ? "바로 가기는 현재 런을 버리고 새로 시작해요." : "바로 가기는 마을이나 던전 지도에서 쓸 수 있어요.",
                        Font.PLAIN, 12f, Sketch.INK_SOFT)));
    }

    // ---------- 전투 ----------

    private static JComponent battleTab(Player p, Host host, JLabel note) {
        JCheckBox inv = check("무적 (몬스터 공격과 독 피해 0)", DevMode.invincible);
        JCheckBox one = check("한 방 처치 (모든 공격이 마무리)", DevMode.oneHit);
        JCheckBox mp = check("MP 무한 (스킬 MP 0)", DevMode.infiniteMp);
        inv.addActionListener(e -> DevMode.invincible = inv.isSelected());
        one.addActionListener(e -> DevMode.oneHit = one.isSelected());
        mp.addActionListener(e -> DevMode.infiniteMp = mp.isSelected());

        // 기본 증강: ticked augments are held from the start of every run (saved in Settings).
        RunState run = host.run();
        Set<String> base = new LinkedHashSet<>(Settings.getDevAugments());
        List<Augment> all = new ArrayList<>(Augment.all());
        all.sort(Comparator.comparing((Augment a) -> a.getTier().ordinal()).thenComparing(Augment::getName));
        JList<Augment> list = new JList<>(all.toArray(new Augment[0]));
        list.setFixedCellHeight(34);
        list.setOpaque(false);
        JCheckBox cell = check("", false);
        cell.setFont(Sketch.hand(Font.PLAIN, 15f));
        list.setCellRenderer((l, a, i, sel, focus) -> {
            boolean held = run != null && run.getAugments().contains(a);
            cell.setSelected(base.contains(a.getName()));
            cell.setText("[" + a.getTier().getLabel() + "] " + a.getName() + (held ? "  (지금 보유)" : "") + " — " + a.getDescription());
            cell.setForeground(Sketch.tierInk(a.getTier()));
            cell.setOpaque(sel);
            cell.setBackground(new Color(255, 236, 160));
            return cell;
        });
        Runnable save = () -> {
            Settings.setDevAugments(base);
            list.repaint();
            note.setText("기본 증강 " + base.size() + "개");
        };
        list.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int i = list.locationToIndex(e.getPoint());
                if (i < 0 || !list.getCellBounds(i, i).contains(e.getPoint())) return;
                String name = list.getModel().getElementAt(i).getName();
                if (!base.remove(name)) base.add(name);
                save.run();
            }
        });
        JButton allOn = Sketch.bigButton("모두 체크", false);
        allOn.addActionListener(e -> {
            for (Augment a : all) base.add(a.getName());
            save.run();
        });
        JButton allOff = Sketch.bigButton("모두 해제", false);
        allOff.addActionListener(e -> {
            base.clear();
            save.run();
        });
        JButton take = Sketch.bigButton("고른 증강 지금 받기", true);
        take.setEnabled(run != null);
        take.addActionListener(e -> {
            Augment a = list.getSelectedValue();
            if (a == null || run == null || run.getAugments().contains(a)) return;
            run.take(a);
            host.refreshRun();
            list.repaint();
            note.setText(a.getName() + " 받음!");
        });

        JPanel aug = new JPanel(new BorderLayout(0, 6));
        aug.setOpaque(false);
        aug.add(Sketch.label("기본 증강 — 체크한 증강은 던전·바로 가기·최종전마다 처음부터 들고 시작해요", Font.BOLD, 14f, Sketch.INK), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(680, 300));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        aug.add(sp, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(allOn);
        buttons.add(allOff);
        buttons.add(take);
        aug.add(buttons, BorderLayout.SOUTH);
        return column(form(7731, "", inv, "", one, "", mp),
                Sketch.label("치트는 게임을 껐다 켜면 꺼지고, 콜로세움 대전에서는 듣지 않아요. 기본 증강은 계속 남아요."
                        + (run == null ? " '지금 받기'는 던전 전투 중에만 돼요." : ""), Font.PLAIN, 12f, Sketch.INK_SOFT), aug);
    }

    // ---------- 아이템 ----------

    private static JComponent itemTab(Player p, Host host, JLabel note) {
        List<Item> items = new ArrayList<>();
        for (Item it : ItemCatalog.all()) if (!it.isCrafted()) items.add(it);
        items.sort(Comparator.comparing((Item it) -> it.getType().ordinal()).thenComparing(it -> it.getGrade().ordinal()).thenComparing(Item::getName));
        JList<Item> list = new JList<>(items.toArray(new Item[0]));
        list.setFont(Sketch.hand(Font.PLAIN, 14f));
        list.setOpaque(false);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean focus) {
                Item it = (Item) v;
                JLabel c = (JLabel) super.getListCellRendererComponent(l, "[" + it.getGrade().getLabel() + "] " + it.getName() + "  " + it.getStatText(), i, sel, focus);
                c.setIcon(ItemArt.item(it, 28));
                if (!sel) c.setForeground(Sketch.gradeInk(it.getGrade()));
                c.setOpaque(sel);
                return c;
            }
        });
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(680, 420));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        Sketch.Stepper count = spinner(1, 1, 99, 1);
        Sketch.Stepper enhance = spinner(0, 0, game.model.Enhancement.MAX_LEVEL, 1);
        count.setPreferredSize(new Dimension(170, 42));
        enhance.setPreferredSize(new Dimension(170, 42));
        JButton give = Sketch.bigButton("지급", true);
        give.addActionListener(e -> {
            Item it = list.getSelectedValue();
            if (it == null) return;
            int n = count.getValue(), enh = enhance.getValue();
            boolean gear = it.getType() == Item.Type.WEAPON || it.getType() == Item.Type.ARMOR;
            for (int i = 0; i < n; i++) p.devGive(it.getName(), gear ? enh : 0);
            host.changed();
            note.setText(it.getName() + (gear && enh > 0 ? " +" + enh : "") + " " + n + "개 지급!");
        });
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(text("개수"));
        controls.add(count);
        controls.add(text("강화 (장비만)"));
        controls.add(enhance);
        controls.add(give);
        return column(sp, controls);
    }
}
