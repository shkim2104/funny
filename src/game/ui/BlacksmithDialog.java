package game.ui;

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

/** The blacksmith, on a notebook page: enhance gear, scrap spare pieces, restore destroyed ones. */
public class BlacksmithDialog extends JDialog {
    private enum Mode { ENHANCE, SCRAP, RESTORE }

    private final Player player;
    private final Random rnd;
    private Mode mode = Mode.ENHANCE;

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
        super(owner, "대장간", true);
        this.player = player;
        this.rnd = rnd;
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
        enhanceList.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) act();
            }
        });
        Sketch.Tabs tabs = new Sketch.Tabs(6101);
        tabs.addTab("강화", Sketch.scroll(enhanceList));
        tabs.addTab("분해", Sketch.scroll(scrapList));
        tabs.addTab("복구", Sketch.scroll(restoreList));
        tabs.onChange(i -> {
            mode = Mode.values()[i];
            resultLabel.setText(" ");
            showDetail();
        });
        return tabs;
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

    private void showScrap(GearPiece piece) {
        int graphite = Enhancement.scrapGraphite(piece), lead = Enhancement.scrapHardLead(piece);
        String gain = graphite > 0 ? Material.GRAPHITE.getLabel() + " " + graphite + "개" : Material.HARD_LEAD.getLabel() + " " + lead + "개";
        detailInfo.setText(html(piece.getStatDetail() + "<br><br>분해하면 <b>" + gain + "</b>을(를) 얻어요."
                + (piece.getEnhance() > 0 ? "<br>강화에 쓴 재료는 돌아오지 않아요." : "")));
        actionBtn.setText("분해하기");
        actionBtn.setEnabled(true);
    }

    private void showRestore(GearPiece piece) {
        List<GearPiece> donors = Enhancement.restoreDonors(player, piece);
        if (donors.isEmpty()) {
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

    private void restore(GearPiece piece) {
        List<GearPiece> donors = Enhancement.restoreDonors(player, piece);
        if (donors.isEmpty()) return;
        new RestoreDialog(this, player, piece, donors.get(0)).setVisible(true);
        refresh();
        showDetail();
        if (!piece.isDestroyed()) result(piece.getDisplayName() + " 복구 완료!", new Color(54, 134, 58));
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
