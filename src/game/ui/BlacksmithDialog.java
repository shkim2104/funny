package game.ui;

import game.model.CraftedGear;
import game.model.Crafting;
import game.model.Enhancement;
import game.model.GearPiece;
import game.model.Item;
import game.model.Material;
import game.model.Player;
import game.save.Settings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * The blacksmith, on a notebook page: enhance gear, scrap spare pieces, restore destroyed ones, and forge
 * gear the player draws (제작).
 */
public class BlacksmithDialog extends JDialog {
    private enum Mode { ENHANCE, SCRAP, RESTORE, CRAFT }

    private final Player player;
    private final Random rnd;
    /** The character's save, for the drawings of forged gear (null: drawings are kept for this run only). */
    private final String saveId;
    private Mode mode = Mode.ENHANCE;
    private Item.Type craftType = Item.Type.WEAPON;
    private final DefaultListModel<Item.Grade> gradeModel = new DefaultListModel<>();
    private final JList<Item.Grade> gradeList = new JList<>(gradeModel);
    private final JButton weaponTab = new Sketch.Button("√ 무기", false);
    private final JButton armorTab = new Sketch.Button("방어구", false);

    private final JLabel goldLabel = new JLabel();
    private final JLabel[] materialLabels = new JLabel[Material.values().length];

    private final DefaultListModel<GearPiece> enhanceModel = new DefaultListModel<>();
    private final JList<GearPiece> enhanceList = new JList<>(enhanceModel);
    private final DefaultListModel<GearPiece> scrapModel = new DefaultListModel<>();
    private final JList<GearPiece> scrapList = new JList<>(scrapModel);
    private final DefaultListModel<GearPiece> restoreModel = new DefaultListModel<>();
    private final JList<GearPiece> restoreList = new JList<>(restoreModel);

    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailHost = new JPanel(detailCards);
    private final JLabel placeholder = new JLabel();
    private final JLabel detailIcon = new JLabel();
    private final JLabel detailName = new JLabel();
    private final JLabel detailInfo = new JLabel();
    private final JLabel resultLabel = new JLabel(" ");
    private final JButton actionBtn = new Sketch.Button("강화하기", true);

    public BlacksmithDialog(JFrame owner, Player player, Random rnd) {
        this(owner, player, rnd, null);
    }

    public BlacksmithDialog(JFrame owner, Player player, Random rnd, String saveId) {
        super(owner, "대장간", true);
        this.player = player;
        this.rnd = rnd;
        this.saveId = saveId;
        setSize(860, 640);
        setMinimumSize(new Dimension(700, 560));
        setLocationRelativeTo(owner);
        Sketch.Page page = new Sketch.Page(new BorderLayout(14, 12), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 22));
        setContentPane(page);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.add(Sketch.title("대장간"), BorderLayout.WEST);
        head.add(buildWallet(), BorderLayout.EAST);
        add(head, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 16, 0));
        center.setOpaque(false);
        center.add(buildLists());
        center.add(buildDetail());
        add(center, BorderLayout.CENTER);

        JButton close = new Sketch.Button("나가기", false);
        close.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(close);
        add(bottom, BorderLayout.SOUTH);

        refresh();
        showDetail();
    }

    private JComponent buildWallet() {
        JPanel wallet = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        wallet.setOpaque(false);
        goldLabel.setFont(Sketch.hand(Font.BOLD, 16f));
        goldLabel.setForeground(Sketch.BLUE_PENCIL);
        wallet.add(goldLabel);
        for (Material m : Material.values()) {
            JLabel l = new JLabel(ItemArt.material(m, 30));
            l.setFont(Sketch.hand(Font.BOLD, 15f));
            l.setForeground(Sketch.INK);
            l.setToolTipText(m.getLabel());
            materialLabels[m.ordinal()] = l;
            wallet.add(l);
        }
        return wallet;
    }

    private JComponent buildLists() {
        for (JList<GearPiece> list : List.of(enhanceList, scrapList, restoreList)) {
            list.setCellRenderer(new PieceRow());
            list.setFixedCellHeight(60);
            list.setFixedCellWidth(1);   // rows take the list's width, however long a name is
            list.setOpaque(false);
            list.addListSelectionListener(e -> {
                if (e.getValueIsAdjusting()) return;
                resultLabel.setText(" ");
                showDetail();
            });
        }
        Sketch.toggleRows(scrapList);
        enhanceList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) act();
            }
        });
        Sketch.Tabs tabs = new Sketch.Tabs(6101);
        tabs.addTab("강화", Sketch.scroll(enhanceList));
        tabs.addTab("분해", buildScrapTab());
        tabs.addTab("복구", Sketch.scroll(restoreList));
        tabs.addTab("제작", buildCraftTab());
        tabs.onChange(i -> {
            mode = Mode.values()[i];
            resultLabel.setText(" ");
            showDetail();
        });
        return tabs;
    }

    /** The scrap list with its bulk buttons. Rows pick and unpick with a click. */
    private JComponent buildScrapTab() {
        JButton all = new Sketch.Button("전체 선택", false);
        all.addActionListener(e -> selectAllScrap());
        JButton none = new Sketch.Button("선택 해제", false);
        none.addActionListener(e -> scrapList.clearSelection());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 6, 0));
        buttons.setOpaque(false);
        buttons.add(all);
        buttons.add(none);
        JPanel tab = new JPanel(new BorderLayout(0, 6));
        tab.setOpaque(false);
        tab.add(Sketch.scroll(scrapList), BorderLayout.CENTER);
        tab.add(buttons, BorderLayout.SOUTH);
        return tab;
    }

    /** 제작: weapon or armor on top, then the six grades with what each costs. */
    private JComponent buildCraftTab() {
        for (Item.Grade g : Item.Grade.values()) gradeModel.addElement(g);
        gradeList.setCellRenderer(new GradeRow());
        gradeList.setFixedCellHeight(56);
        gradeList.setFixedCellWidth(1);
        gradeList.setOpaque(false);
        gradeList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            resultLabel.setText(" ");
            showDetail();
        });
        gradeList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) act();
            }
        });
        weaponTab.addActionListener(e -> setCraftType(Item.Type.WEAPON));
        armorTab.addActionListener(e -> setCraftType(Item.Type.ARMOR));
        JPanel kinds = new JPanel(new GridLayout(1, 2, 6, 0));
        kinds.setOpaque(false);
        kinds.add(weaponTab);
        kinds.add(armorTab);
        JPanel tab = new JPanel(new BorderLayout(0, 6));
        tab.setOpaque(false);
        tab.add(kinds, BorderLayout.NORTH);
        tab.add(Sketch.scroll(gradeList), BorderLayout.CENTER);
        return tab;
    }

    private void setCraftType(Item.Type type) {
        craftType = type;
        weaponTab.setText((type == Item.Type.WEAPON ? "√ " : "") + "무기");
        armorTab.setText((type == Item.Type.ARMOR ? "√ " : "") + "방어구");
        gradeList.repaint();
        resultLabel.setText(" ");
        showDetail();
    }

    /** Everything on the list but destroyed pieces, which can still be restored; those are picked by hand. */
    private void selectAllScrap() {
        List<Integer> rows = new ArrayList<>();
        for (int i = 0; i < scrapModel.size(); i++) if (!scrapModel.get(i).isDestroyed()) rows.add(i);
        scrapList.setSelectedIndices(rows.stream().mapToInt(Integer::intValue).toArray());
    }

    private JComponent buildDetail() {
        Sketch.Box wrap = new Sketch.Box(new BorderLayout(), 6202);
        wrap.setBorder(new EmptyBorder(18, 20, 18, 20));
        detailHost.setOpaque(false);

        placeholder.setFont(Sketch.hand(Font.PLAIN, 14f));
        placeholder.setForeground(Sketch.INK_SOFT);
        placeholder.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        empty.add(placeholder);

        detailName.setFont(Sketch.hand(Font.BOLD, 20f));
        detailInfo.setFont(Sketch.hand(Font.PLAIN, 14f));
        detailInfo.setForeground(Sketch.INK);
        resultLabel.setFont(Sketch.hand(Font.BOLD, 16f));
        actionBtn.addActionListener(e -> act());

        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[]{detailIcon, detailName, detailInfo, actionBtn}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        body.add(detailIcon);
        body.add(Box.createVerticalStrut(4));
        body.add(detailName);
        body.add(Box.createVerticalStrut(6));
        body.add(detailInfo);
        body.add(Box.createVerticalGlue());
        body.add(actionBtn);

        detailHost.add(empty, "EMPTY");
        detailHost.add(body, "PIECE");
        wrap.add(detailHost, BorderLayout.CENTER);
        // Outside the cards: a scrapped or restored piece leaves its list, and this must still show.
        resultLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        wrap.add(resultLabel, BorderLayout.SOUTH);
        return wrap;
    }

    private JList<GearPiece> activeList() {
        return mode == Mode.ENHANCE ? enhanceList : mode == Mode.SCRAP ? scrapList : restoreList;
    }

    private void showDetail() {
        if (mode == Mode.CRAFT) {
            showCraft();
            return;
        }
        if (mode == Mode.SCRAP && scrapList.getSelectedValuesList().size() > 1) {
            showScrapBulk(scrapList.getSelectedValuesList());
            detailCards.show(detailHost, "PIECE");
            return;
        }
        GearPiece piece = activeList().getSelectedValue();
        if (piece == null) {
            placeholder.setText("<html><div style='text-align:center;width:190px'>" + emptyHint() + "</div></html>");
            detailCards.show(detailHost, "EMPTY");
            return;
        }
        Item item = piece.getItem();
        detailIcon.setIcon(ItemArt.piece(piece, 72));
        detailName.setText(piece.getDisplayName());
        detailName.setForeground(Sketch.gradeInk(item.getGrade()));
        if (mode == Mode.ENHANCE) showEnhance(piece);
        else if (mode == Mode.SCRAP) showScrap(piece);
        else showRestore(piece);
        detailCards.show(detailHost, "PIECE");
    }

    private String emptyHint() {
        if (mode == Mode.CRAFT) return "만들 등급을 왼쪽에서 골라 주세요. 등급이 높을수록 " + Material.COLOR_LEAD.getLabel()
                + "이 많이 들고, 능력치와 부가 능력이 좋아져요.";
        if (mode == Mode.ENHANCE) return "강화할 장비를 왼쪽에서 골라 주세요.";
        if (mode == Mode.SCRAP) return "장착하지 않고 잠그지 않은 장비를 재료로 바꿀 수 있어요.";
        if (!restoreModel.isEmpty()) return "복구할 장비를 왼쪽에서 골라 주세요.";
        return "파괴된 장비가 없어요. 파괴된 장비는 같은 장비를 재료로 +" + Enhancement.RESTORE_LEVEL + "까지 복구할 수 있어요.";
    }

    private void showEnhance(GearPiece piece) {
        if (!Enhancement.canEnhance(piece)) {
            detailInfo.setText(html(piece.getStatDetail() + "<br><br>최고 단계예요."));
            actionBtn.setText("강화 창 열기");
            actionBtn.setEnabled(false);
            return;
        }
        int target = piece.getEnhance() + 1;
        int destroy = Enhancement.destroyPct(target);
        detailInfo.setText(html(piece.getStatDetail() + "<br><br>다음 단계 <b>+" + target + "</b><br>성공 " + Enhancement.successPct(target) + "%"
                + (destroy > 0 ? "  <font color='" + Sketch.hex(Sketch.RED_PENCIL) + "'>파괴 " + destroy + "%</font>" : "")));
        actionBtn.setText("강화 창 열기");
        actionBtn.setEnabled(true);
    }

    /** The chosen grade: the stat range, how many effects, and the cost against what the player has. */
    private void showCraft() {
        Item.Grade grade = gradeList.getSelectedValue();
        if (grade == null) {
            placeholder.setText("<html><div style='text-align:center;width:190px'>" + emptyHint() + "</div></html>");
            detailCards.show(detailHost, "EMPTY");
            return;
        }
        int g = grade.ordinal(), lead = Crafting.COLOR_COST[g], gold = Crafting.GOLD_COST[g];
        int haveLead = player.getMaterial(Material.COLOR_LEAD);
        String stat = craftType == Item.Type.WEAPON ? "공격력" : "방어력";
        String red = Sketch.hex(Sketch.RED_PENCIL);
        detailIcon.setIcon(ItemArt.material(Material.COLOR_LEAD, 72));
        detailName.setText("[" + grade.getLabel() + "] " + (craftType == Item.Type.WEAPON ? "무기" : "방어구") + " 제작");
        detailName.setForeground(Sketch.gradeInk(grade));
        int effects = Crafting.EFFECT_COUNT[g];
        detailInfo.setText(html(stat + " <b>" + Crafting.minValue(craftType, grade) + " ~ " + Crafting.maxValue(craftType, grade)
                + "</b> 중에서 정해져요.<br>부가 능력 <b>" + effects + "개</b>"
                + (effects > 0 ? " (종류와 수치 모두 랜덤)" : "") + "<br><br>필요한 재료<br>"
                + (haveLead < lead ? "<font color='" + red + "'>" : "") + "✎ " + Material.COLOR_LEAD.getLabel() + " " + lead
                + "개 (가진 것 " + haveLead + "개)" + (haveLead < lead ? "</font>" : "") + "<br>"
                + (player.getGold() < gold ? "<font color='" + red + "'>" : "") + "✎ 골드 " + gold + "G"
                + (player.getGold() < gold ? "</font>" : "")
                + "<br><br>그린 그림이 장비 모습이 돼요. 무기는 전투에서 손에 그린 크기 그대로 들어요."));
        actionBtn.setText("그리러 가기");
        actionBtn.setEnabled(Crafting.canAfford(player, grade));
        detailCards.show(detailHost, "PIECE");
    }

    private void showScrap(GearPiece piece) {
        int graphite = Enhancement.scrapGraphite(piece), lead = Enhancement.scrapHardLead(piece), color = Enhancement.scrapColorLead(piece);
        String gain = (graphite > 0 ? Material.GRAPHITE.getLabel() + " " + graphite + "개" : Material.HARD_LEAD.getLabel() + " " + lead + "개")
                + (color > 0 ? ", " + Material.COLOR_LEAD.getLabel() + " " + color + "개" : "");
        detailInfo.setText(html(piece.getStatDetail() + "<br><br>분해하면 <b>" + gain + "</b>을(를) 얻어요."
                + (piece.getEnhance() > 0 ? "<br>강화에 쓴 재료는 돌아오지 않아요." : "")));
        actionBtn.setText("분해하기");
        actionBtn.setEnabled(true);
    }

    private void showScrapBulk(List<GearPiece> picked) {
        int graphite = picked.stream().mapToInt(Enhancement::scrapGraphite).sum();
        int lead = picked.stream().mapToInt(Enhancement::scrapHardLead).sum();
        int color = picked.stream().mapToInt(Enhancement::scrapColorLead).sum();
        long enhanced = picked.stream().filter(p -> p.getEnhance() > 0).count();
        detailIcon.setIcon(null);
        detailName.setText(picked.size() + "개 선택");
        detailName.setForeground(Sketch.INK);
        detailInfo.setText(html("분해하면 <b>" + Material.GRAPHITE.getLabel() + " " + graphite + "개</b>"
                + (lead > 0 ? ", <b>" + Material.HARD_LEAD.getLabel() + " " + lead + "개</b>" : "")
                + (color > 0 ? ", <b>" + Material.COLOR_LEAD.getLabel() + " " + color + "개</b>" : "") + "를 얻어요."
                + (enhanced > 0 ? "<br>강화한 장비 " + enhanced + "개가 포함되어 있어요." : "")));
        actionBtn.setText("모두 분해하기");
        actionBtn.setEnabled(true);
    }

    private void showRestore(GearPiece piece) {
        List<GearPiece> donors = Enhancement.restoreDonors(player, piece);
        if (donors.isEmpty() && piece.getItem().isCrafted()) {
            detailInfo.setText(html("직접 만든 장비는 세상에 하나뿐이라 복구할 수 없어요.<br>분해하면 재료로 바꿀 수 있어요."));
        } else if (donors.isEmpty()) {
            detailInfo.setText(html("가방에 같은 장비가 있어야 복구할 수 있어요.<br>장착했거나 잠근 장비는 재료로 쓸 수 없어요."));
        } else {
            detailInfo.setText(html("복구하면 <b>+" + Enhancement.RESTORE_LEVEL + "</b>로 돌아와요.<br><br>재료로 사라지는 장비:<br><b>"
                    + donors.get(0).getDisplayName() + "</b>"));
        }
        actionBtn.setText("복구하기");
        actionBtn.setEnabled(!donors.isEmpty());
    }

    private static String html(String body) {
        return "<html><body style='width:240px'>" + body + "</body></html>";
    }

    private void act() {
        if (mode == Mode.CRAFT) {
            craft();
            return;
        }
        List<GearPiece> picked = scrapList.getSelectedValuesList();
        if (mode == Mode.SCRAP && picked.size() > 1) {
            scrapAll(picked);
            return;
        }
        GearPiece piece = activeList().getSelectedValue();
        if (piece == null) return;
        if (mode == Mode.ENHANCE) enhance(piece);
        else if (mode == Mode.SCRAP) scrap(piece);
        else restore(piece);
    }

    private void enhance(GearPiece piece) {
        new EnhanceDialog(this, player, piece, rnd).setVisible(true);
        refresh();
        showDetail();
    }

    private void scrap(GearPiece piece) {
        Item item = piece.getItem();
        boolean valuable = Settings.isSellConfirm() && item.getGrade().ordinal() >= Item.Grade.EPIC.ordinal();
        if ((valuable || piece.getEnhance() > 0 || piece.isDestroyed())
                && !Dialogs.confirm(this, "분해 확인", piece.getDisplayName() + "을(를) 분해할까요?")) {
            return;
        }
        if (!Enhancement.disassemble(player, piece)) return;
        Sfx.play(Sfx.Sound.COIN);
        refresh();
        showDetail();
        result(piece.getDisplayName() + " 분해 완료!", Sketch.BLUE_PENCIL);
    }

    /** A bulk scrap always asks first. */
    private void scrapAll(List<GearPiece> picked) {
        if (!Dialogs.confirm(this, "분해 확인", "장비 " + picked.size() + "개를 분해할까요?")) return;
        for (GearPiece piece : picked) Enhancement.disassemble(player, piece);
        Sfx.play(Sfx.Sound.COIN);
        refresh();
        showDetail();
        result("장비 " + picked.size() + "개 분해 완료!", Sketch.BLUE_PENCIL);
    }

    private void restore(GearPiece piece) {
        List<GearPiece> donors = Enhancement.restoreDonors(player, piece);
        if (donors.isEmpty()) return;
        new RestoreDialog(this, player, piece, donors.get(0)).setVisible(true);
        refresh();
        showDetail();
        if (!piece.isDestroyed()) result(piece.getDisplayName() + " 복구 완료!", new Color(54, 134, 58));
    }

    /** Draw it, pay, roll it, keep the drawing, then show what came out. */
    private void craft() {
        Item.Grade grade = gradeList.getSelectedValue();
        if (grade == null || !Crafting.canAfford(player, grade)) return;
        CharacterArt hero = saveId != null ? CharacterArt.load(saveId) : CharacterArt.defaultDoodle();
        DrawGearDialog.Result drawn = DrawGearDialog.draw(this, craftType, grade, hero);
        if (drawn == null) return;
        CraftedGear made = Crafting.craft(player, drawn.name, craftType, grade, rnd);
        if (made == null) {
            result("제작하지 못했어요. 이름이나 재료를 확인해 주세요.", Sketch.RED_PENCIL);
            return;
        }
        GearArt.save(saveId, made.getName(), drawn.drawing);
        Sfx.play(Sfx.Sound.COIN);
        refresh();
        showDetail();
        CraftReveal.show(this, made);
        result(made.getName() + " 제작 완료! 가방에 넣었어요.", new Color(54, 134, 58));
    }

    private void result(String text, Color color) {
        resultLabel.setText(text);
        resultLabel.setForeground(color);
    }

    private void refresh() {
        goldLabel.setText(player.getGold() + "G");
        for (Material m : Material.values()) materialLabels[m.ordinal()].setText(String.valueOf(player.getMaterial(m)));

        List<GearPiece> sorted = new ArrayList<>(player.getGear());
        sorted.sort(Comparator.comparing((GearPiece g) -> !player.isWorn(g))
                .thenComparing(g -> -g.getItem().getGrade().ordinal()).thenComparing(g -> -g.getValue()));
        fill(enhanceList, enhanceModel, sorted, g -> !g.isDestroyed());
        fill(scrapList, scrapModel, sorted, player::isSpare);
        fill(restoreList, restoreModel, sorted, GearPiece::isDestroyed);
        repaint();
    }

    /** Refills a list, keeping the selected piece selected if it is still there. */
    private static void fill(JList<GearPiece> list, DefaultListModel<GearPiece> model, List<GearPiece> pieces,
                             java.util.function.Predicate<GearPiece> keep) {
        GearPiece selected = list.getSelectedValue();
        model.clear();
        for (GearPiece g : pieces) if (keep.test(g)) model.addElement(g);
        if (selected != null && model.contains(selected)) list.setSelectedValue(selected, false);
    }

    /** A grade to forge: its color, and the 색연필 심 and gold it takes. */
    private final class GradeRow implements ListCellRenderer<Item.Grade> {
        private final Sketch.Row row = new Sketch.Row();

        @Override
        public Component getListCellRendererComponent(JList<? extends Item.Grade> list, Item.Grade grade, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            Item sample = new Item(grade.getLabel(), craftType, 0, 0, grade);
            String kind = craftType == Item.Type.WEAPON ? "무기" : "방어구";
            int g = grade.ordinal();
            return row.set(sample, grade.getLabel() + " " + kind, "심 " + Crafting.COLOR_COST[g] + " · " + Crafting.GOLD_COST[g] + "G",
                    isSelected, index).icon(ItemArt.material(Material.COLOR_LEAD, 40));
        }
    }

    private final class PieceRow implements ListCellRenderer<GearPiece> {
        private final Sketch.Row row = new Sketch.Row();

        @Override
        public Component getListCellRendererComponent(JList<? extends GearPiece> list, GearPiece piece, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String state = player.isWorn(piece) ? (piece.isLocked() ? "장착 · 잠금" : "장착") : piece.isLocked() ? "잠금" : "";
            return row.set(piece.getItem(), piece.getDisplayName(), piece.getStatText(), isSelected, index).note(state)
                    .icon(ItemArt.piece(piece, 44));
        }
    }
}
