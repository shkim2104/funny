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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

        JButton close = new Sketch.Button("닫기", false);
        close.addActionListener(e -> dialog.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(close);
        dialog.add(south, BorderLayout.SOUTH);

        // Tall enough that the character tab's 적용 is on screen without scrolling.
        dialog.setSize(780, 760);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    // ---------- small pieces ----------

    private static JLabel text(String s) {
        return Sketch.label(s, Font.BOLD, 14f, Sketch.INK);
    }

    private static JSpinner spinner(int value, int min, int max, int step) {
        JSpinner s = new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, value)), min, max, step));
        s.setFont(Sketch.hand(Font.BOLD, 14f));
        return s;
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
        JSpinner level = spinner(p.getLevel(), 1, 99, 1);
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
        JSpinner gold = spinner(p.getGold(), 0, 99_999_999, 1000);
        List<JSpinner> mats = new ArrayList<>();
        for (Material m : Material.values()) mats.add(spinner(p.getMaterial(m), 0, 99_999, 10));
        JSpinner points = spinner(0, -999, 999, 4);
        JCheckBox heal = check("HP·MP 모두 회복", true);

        JButton apply = new Sketch.Button("적용", true);
        apply.addActionListener(e -> {
            p.devSetLevel((Integer) level.getValue());
            List<Job> path = new ArrayList<>();
            for (JComboBox<Object> c : jobs) {
                if (!(c.getSelectedItem() instanceof Job)) break;
                path.add((Job) c.getSelectedItem());
            }
            p.setJobPath(path);
            p.devSetGold((Integer) gold.getValue());
            for (int i = 0; i < mats.size(); i++) p.devSetMaterial(Material.values()[i], (Integer) mats.get(i).getValue());
            p.devAddStatPoints((Integer) points.getValue());
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
        JComboBox<String> unlocked = combo(names);
        unlocked.setSelectedIndex(Math.min(p.getUnlockedDungeon(), names.length - 1));
        JComboBox<String> story = combo(STORY);
        story.setSelectedIndex(Math.max(0, Math.min(STORY.length - 1, p.getStoryStage())));
        JCheckBox tutorialDone = check("튜토리얼 끝내기", p.getTutorialStep() == Tutorial.DONE);
        JButton apply = new Sketch.Button("적용", true);
        apply.addActionListener(e -> {
            p.setUnlockedDungeon(unlocked.getSelectedIndex());
            p.setStoryStage(story.getSelectedIndex());
            if (tutorialDone.isSelected()) p.setTutorialStep(Tutorial.DONE);
            host.changed();
            note.setText("진행 상황 적용!");
        });

        JComboBox<String> where = combo(names);
        where.setSelectedIndex(Math.min(p.getUnlockedDungeon(), names.length - 1));
        String[] stages = new String[World.FLOORS + 1];
        for (int f = 1; f <= World.FLOORS; f++) stages[f - 1] = f + " 스테이지" + (f == Dungeon.MID_FLOOR ? " (중간 보스)" : "");
        stages[World.FLOORS] = "보스";
        JComboBox<String> stage = combo(stages);
        stage.setSelectedIndex(Dungeon.MID_FLOOR - 1);
        JButton go = new Sketch.Button("바로 가기", true);
        go.addActionListener(e -> {
            int s = stage.getSelectedIndex();
            dialog.dispose();
            host.jumpTo(where.getSelectedIndex(), Math.min(s + 1, World.FLOORS), s == World.FLOORS);
        });
        JButton finale = new Sketch.Button("최종 보스전 바로 시작", false);
        finale.addActionListener(e -> {
            dialog.dispose();
            host.finale();
        });
        go.setEnabled(host.canTravel());
        finale.setEnabled(host.canTravel());

        return Sketch.scroll(column(
                form(7721, "열린 던전", unlocked, "최종 스토리", story, "", tutorialDone, "", apply),
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

        RunState run = host.run();
        List<Augment> all = new ArrayList<>(Augment.all());
        all.sort(Comparator.comparing((Augment a) -> a.getTier().ordinal()).thenComparing(Augment::getName));
        JList<Augment> list = new JList<>(all.toArray(new Augment[0]));
        list.setFont(Sketch.hand(Font.PLAIN, 14f));
        list.setOpaque(false);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean focus) {
                Augment a = (Augment) v;
                boolean held = run != null && run.getAugments().contains(a);
                JLabel c = (JLabel) super.getListCellRendererComponent(l, "[" + a.getTier().getLabel() + "] " + a.getName()
                        + (held ? "  (보유)" : "") + " — " + a.getDescription(), i, sel, focus);
                if (!sel) c.setForeground(Sketch.tierInk(a.getTier()));
                c.setOpaque(sel);
                return c;
            }
        });
        JButton take = new Sketch.Button("이 증강 받기", true);
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
        aug.add(Sketch.label(run != null ? "증강 받기 (지금 런에 바로 들어가요)" : "증강 받기는 던전 전투 중에만 돼요", Font.BOLD, 14f, Sketch.INK), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(600, 240));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        aug.add(sp, BorderLayout.CENTER);
        JPanel takeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        takeRow.setOpaque(false);
        takeRow.add(take);
        aug.add(takeRow, BorderLayout.SOUTH);
        return column(form(7731, "", inv, "", one, "", mp),
                Sketch.label("치트는 게임을 껐다 켜면 꺼지고, 콜로세움 대전에서는 듣지 않아요.", Font.PLAIN, 12f, Sketch.INK_SOFT), aug);
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
        sp.setPreferredSize(new Dimension(600, 300));
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        JSpinner count = spinner(1, 1, 99, 1);
        JSpinner enhance = spinner(0, 0, game.model.Enhancement.MAX_LEVEL, 1);
        JButton give = new Sketch.Button("지급", true);
        give.addActionListener(e -> {
            Item it = list.getSelectedValue();
            if (it == null) return;
            int n = (Integer) count.getValue();
            boolean gear = it.getType() == Item.Type.WEAPON || it.getType() == Item.Type.ARMOR;
            for (int i = 0; i < n; i++) p.devGive(it.getName(), gear ? (Integer) enhance.getValue() : 0);
            host.changed();
            note.setText(it.getName() + (gear && (Integer) enhance.getValue() > 0 ? " +" + enhance.getValue() : "") + " " + n + "개 지급!");
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
