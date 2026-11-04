package game.ui;

import game.model.GearPiece;
import game.model.Item;
import game.model.ItemCatalog;
import game.model.Player;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * 가방: the inventory as a pencil grid ruled on a notebook page. Every weapon, armor and potion stack sits in
 * a cell of its own; drag one onto another cell to move it there (swapping with what was there), or onto
 * the 무기/방어구 slots above to wear it. Click shows it on the right; double-click wears or takes it off.
 * Where things sit is the player's (Player.bagLayout) and is saved with the character.
 */
public class EquipDialog extends JDialog {
    static final int COLS = 6, CELL = 76, PAD = 10, MIN_ROWS = 5, ICON = 52;

    private final Player player;
    // Made in the constructor, once the player is known.
    private final BagGrid grid;
    private final EquipSlots slots;
    private final Ghost ghost = new Ghost();
    /** A GearPiece or a consumable name, or null. */
    private Object selected;

    private final JPanel detailBody = new JPanel();
    private final JLabel detailPlaceholder = new JLabel(
            "<html><div style='text-align:center;width:190px'>인벤토리에서 아이템을 누르면<br>여기에 적어 둘게요.<br><br>끌어서 다른 칸으로 옮기고,<br>위의 장착 칸에 놓으면 장착해요.</div></html>");
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailCardHost = new JPanel();
    private final JLabel detailIcon = new JLabel();
    private final JLabel detailName = new JLabel();
    private final JLabel detailStat = new JLabel();
    private final JLabel detailStatus = new JLabel();
    private final JButton detailAction = Sketch.bigButton("장착하기", true);
    private final JButton detailLock = Sketch.bigButton("잠그기", false);

    public EquipDialog(JFrame owner, Player player) {
        super(owner, "인벤토리", true);
        this.player = player;
        this.grid = new BagGrid();
        this.slots = new EquipSlots();
        setSize(960, 720);
        setMinimumSize(new Dimension(820, 560));
        setLocationRelativeTo(owner);
        Sketch.Page page = new Sketch.Page(new BorderLayout(16, 10), 34);
        page.setBorder(new EmptyBorder(14, 52, 12, 22));
        setContentPane(page);
        setGlassPane(ghost);

        JPanel head = new JPanel(new BorderLayout(16, 0));
        head.setOpaque(false);
        head.add(Sketch.title("인벤토리"), BorderLayout.WEST);
        head.add(slots, BorderLayout.CENTER);
        add(head, BorderLayout.NORTH);

        JScrollPane scroll = Sketch.scroll(grid);
        scroll.setPreferredSize(new Dimension(COLS * CELL + PAD * 2 + 18, 420));
        scroll.getVerticalScrollBar().setUnitIncrement(CELL / 3);
        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);
        center.add(scroll, BorderLayout.WEST);
        center.add(buildDetailArea(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        JButton closeBtn = Sketch.bigButton("나가기", false);
        closeBtn.addActionListener(e -> dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(closeBtn);
        add(bottom, BorderLayout.SOUTH);

        refresh();
    }

    // ---------- the right side: what is selected ----------

    private JComponent buildDetailArea() {
        Sketch.Box wrap = new Sketch.Box(new BorderLayout(), 4202);
        wrap.setBorder(new EmptyBorder(20, 22, 20, 22));
        detailCardHost.setLayout(detailCards);
        detailCardHost.setOpaque(false);

        detailPlaceholder.setFont(Sketch.hand(Font.PLAIN, 14f));
        detailPlaceholder.setForeground(Sketch.INK_SOFT);
        detailPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel placeholderWrap = new JPanel(new GridBagLayout());
        placeholderWrap.setOpaque(false);
        placeholderWrap.add(detailPlaceholder);

        detailBody.setOpaque(false);
        detailBody.setLayout(new BoxLayout(detailBody, BoxLayout.Y_AXIS));
        detailName.setFont(Sketch.hand(Font.BOLD, 20f));
        detailStat.setFont(Sketch.hand(Font.PLAIN, 14f));
        detailStat.setForeground(Sketch.INK);
        detailStatus.setFont(Sketch.hand(Font.BOLD, 14f));
        detailStatus.setForeground(Sketch.RED_PENCIL);
        detailAction.addActionListener(e -> toggleEquip());
        detailLock.addActionListener(e -> toggleLock());
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.setOpaque(false);
        buttons.add(detailAction);
        buttons.add(detailLock);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        for (JComponent c : new JComponent[]{detailIcon, detailName, detailStat, detailStatus, buttons}) c.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailBody.add(detailIcon);
        detailBody.add(Box.createVerticalStrut(6));
        detailBody.add(detailName);
        detailBody.add(Box.createVerticalStrut(8));
        detailBody.add(detailStat);
        detailBody.add(Box.createVerticalGlue());
        detailBody.add(detailStatus);
        detailBody.add(Box.createVerticalStrut(10));
        detailBody.add(buttons);

        detailCardHost.add(placeholderWrap, "EMPTY");
        detailCardHost.add(detailBody, "ITEM");
        detailCards.show(detailCardHost, "EMPTY");
        wrap.add(detailCardHost, BorderLayout.CENTER);
        return wrap;
    }

    private void select(Object thing) {
        selected = thing;
        showDetail();
        grid.repaint();
        slots.repaint();
    }

    private void showDetail() {
        if (selected instanceof String) {
            String name = (String) selected;
            int count = player.getConsumableCounts().getOrDefault(name, 0);
            if (count == 0) {
                selected = null;
                detailCards.show(detailCardHost, "EMPTY");
                return;
            }
            Item item = ItemCatalog.get(name);
            detailIcon.setIcon(ItemArt.item(item, 80));
            detailName.setText(item.getName());
            detailName.setForeground(Sketch.INK);
            detailStat.setText(Sketch.itemDetailHtml(item));
            detailStatus.setText("가진 개수: " + count + "개");
            detailAction.setVisible(false);
            detailLock.setVisible(false);
            detailCards.show(detailCardHost, "ITEM");
            return;
        }
        if (!(selected instanceof GearPiece) || !player.getGear().contains(selected)) {
            selected = null;
            detailCards.show(detailCardHost, "EMPTY");
            return;
        }
        GearPiece piece = (GearPiece) selected;
        Item item = piece.getItem();
        boolean worn = player.isWorn(piece);
        detailIcon.setIcon(ItemArt.piece(piece, 80));
        detailName.setText(piece.getDisplayName());
        detailName.setForeground(Sketch.gradeInk(item.getGrade()));
        detailStat.setText(Sketch.itemDetailHtml(item, piece.getStatDetail()));
        detailStatus.setText(worn ? "√ 지금 장착 중" : piece.isLocked() ? "잠금: 판매·분해되지 않아요" : " ");
        detailAction.setVisible(true);
        detailLock.setVisible(true);
        detailAction.setText(worn ? "장착 해제" : "장착하기");
        detailAction.setEnabled(!piece.isDestroyed());
        detailLock.setText(piece.isLocked() ? "잠금 해제" : "잠그기");
        detailCards.show(detailCardHost, "ITEM");
    }

    private void toggleEquip() {
        if (!(selected instanceof GearPiece)) return;
        GearPiece piece = (GearPiece) selected;
        if (player.isWorn(piece)) player.unequip(piece);
        else player.wear(piece);
        refresh();
    }

    private void toggleLock() {
        if (!(selected instanceof GearPiece)) return;
        GearPiece piece = (GearPiece) selected;
        piece.setLocked(!piece.isLocked());
        refresh();
    }

    private void refresh() {
        grid.relayout();
        showDetail();
        repaint();
    }

    // ---------- drawing one thing in a cell ----------

    private Image iconOf(Object thing, int size) {
        if (thing instanceof GearPiece) return ItemArt.piece((GearPiece) thing, size).getImage();
        return ItemArt.item(ItemCatalog.get((String) thing), size).getImage();
    }

    /** Icon, grade underline, +N, E (worn), lock and broken marks, or a stack count, inside the box at (x, y). */
    private void drawThing(Graphics2D g2, Object thing, int x, int y, int size, long seed) {
        int icon = Math.round(size * ICON / (float) CELL);
        g2.drawImage(iconOf(thing, icon), x + (size - icon) / 2, y + (size - icon) / 2 - 2, null);
        if (thing instanceof GearPiece) {
            GearPiece g = (GearPiece) thing;
            Color grade = Sketch.gradeInk(g.getItem().getGrade());
            Sketch.line(g2, x + 10, y + size - 9, x + size - 10, y + size - 9, seed + 3, 2.6f, grade);
            if (g.getEnhance() > 0) {
                g2.setFont(Sketch.hand(Font.BOLD, 13f));
                g2.setColor(Sketch.BLUE_PENCIL);
                String s = "+" + g.getEnhance();
                g2.drawString(s, x + size - 7 - g2.getFontMetrics().stringWidth(s), y + 17);
            }
            if (player.isWorn(g)) {
                Sketch.circle(g2, x + 15, y + 15, 10, seed + 5, 1.8f, Sketch.RED_PENCIL);
                g2.setFont(Sketch.hand(Font.BOLD, 13f));
                g2.setColor(Sketch.RED_PENCIL);
                g2.drawString("E", x + 11, y + 20);
            }
            if (g.isLocked()) {
                // A little padlock in the bottom-left corner.
                int lx = x + 9, ly = y + size - 26;
                g2.setColor(Sketch.INK);
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawArc(lx + 2, ly - 6, 8, 10, 0, 180);
                g2.fillRoundRect(lx, ly, 12, 9, 3, 3);
            }
            if (g.isDestroyed()) {
                Sketch.line(g2, x + 12, y + 12, x + size - 12, y + size - 12, seed + 7, 2.4f, Sketch.RED_PENCIL);
                Sketch.line(g2, x + size - 12, y + 12, x + 12, y + size - 12, seed + 8, 2.4f, Sketch.RED_PENCIL);
            }
        } else {
            int count = player.getConsumableCounts().getOrDefault((String) thing, 0);
            g2.setFont(Sketch.hand(Font.BOLD, 14f));
            g2.setColor(Sketch.INK);
            String s = "x" + count;
            g2.drawString(s, x + size - 8 - g2.getFontMetrics().stringWidth(s), y + size - 8);
        }
    }

    // ---------- the bag grid ----------

    /** The pencil-ruled grid of cells; also where drags start and end. */
    final class BagGrid extends JComponent implements Scrollable {
        Map<Integer, Object> cells;
        int rows = MIN_ROWS, hover = -1, dragFrom = -1, dropAt = -1;
        Point pressAt;
        boolean dragging;

        BagGrid() {
            setOpaque(false);
            MouseAdapter m = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    int c = cellAt(e.getPoint());
                    pressAt = e.getPoint();
                    dragFrom = cells.containsKey(c) ? c : -1;
                    if (dragFrom >= 0) select(cells.get(dragFrom));
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (dragFrom < 0 || pressAt == null) return;
                    if (!dragging && pressAt.distance(e.getPoint()) < 5) return;
                    dragging = true;
                    Point inRoot = SwingUtilities.convertPoint(BagGrid.this, e.getPoint(), getRootPane());
                    ghost.follow(iconOf(cells.get(dragFrom), ICON), inRoot);
                    dropAt = cellAt(e.getPoint());
                    slots.hoverFrom(inRoot, cells.get(dragFrom));
                    scrollRectToVisible(new Rectangle(e.getX(), e.getY() - 10, 1, 20));
                    repaint();
                }

                @Override public void mouseReleased(MouseEvent e) {
                    if (dragging) {
                        Object thing = cells.get(dragFrom);
                        Point inRoot = SwingUtilities.convertPoint(BagGrid.this, e.getPoint(), getRootPane());
                        if (!slots.dropOn(inRoot, thing)) {
                            int to = cellAt(e.getPoint());
                            if (to >= 0) player.moveInBag(dragFrom, to);
                        }
                        refresh();
                    }
                    dragging = false;
                    dragFrom = dropAt = -1;
                    pressAt = null;
                    ghost.clear();
                    slots.hoverFrom(null, null);
                    repaint();
                }

                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && selected instanceof GearPiece) toggleEquip();
                }

                @Override public void mouseMoved(MouseEvent e) {
                    int c = cellAt(e.getPoint());
                    if (c != hover) {
                        hover = c;
                        repaint();
                    }
                }

                @Override public void mouseExited(MouseEvent e) {
                    hover = -1;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
            relayout();
        }

        void relayout() {
            cells = player.bagLayout();
            int last = cells.isEmpty() ? 0 : ((java.util.TreeMap<Integer, Object>) cells).lastKey();
            // Always a free row at the bottom to drop things into.
            rows = Math.max(MIN_ROWS, Math.max(cells.size(), last + 1) / COLS + 2);
            revalidate();
            repaint();
        }

        /** The cell under p, or -1 outside the grid. */
        int cellAt(Point p) {
            int col = (p.x - PAD) / CELL, row = (p.y - PAD) / CELL;
            if (p.x < PAD || p.y < PAD || col >= COLS || row >= rows) return -1;
            return row * COLS + col;
        }

        Rectangle cellBox(int cell) {
            return new Rectangle(PAD + (cell % COLS) * CELL, PAD + (cell / COLS) * CELL, CELL, CELL);
        }

        @Override public Dimension getPreferredSize() {
            return new Dimension(COLS * CELL + PAD * 2, rows * CELL + PAD * 2);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = COLS * CELL, h = rows * CELL;
            Color rule = new Color(Sketch.INK.getRed(), Sketch.INK.getGreen(), Sketch.INK.getBlue(), 150);
            // Hand-ruled lines: each one its own wobble, running a little past the corners.
            for (int r = 0; r <= rows; r++) Sketch.line(g2, PAD - 5, PAD + r * CELL, PAD + w + 5, PAD + r * CELL, 9100 + r * 13L, 1.5f, rule);
            for (int c = 0; c <= COLS; c++) Sketch.line(g2, PAD + c * CELL, PAD - 5, PAD + c * CELL, PAD + h + 5, 9300 + c * 17L, 1.5f, rule);
            if (hover >= 0 && !dragging) {
                Rectangle b = cellBox(hover);
                Sketch.hatch(g2, Sketch.boxShape(b.x + 4, b.y + 4, b.width - 8, b.height - 8), 9500 + hover, Sketch.INK_SOFT, 45, 7, 35);
            }
            for (Map.Entry<Integer, Object> e : cells.entrySet()) {
                Rectangle b = cellBox(e.getKey());
                if (e.getValue() == selected) Sketch.box(g2, b.x + 3, b.y + 3, b.width - 6, b.height - 6, 9700 + e.getKey(), 2.6f, Sketch.RED_PENCIL);
                Composite keep = g2.getComposite();
                if (dragging && e.getKey() == dragFrom) g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                drawThing(g2, e.getValue(), b.x, b.y, CELL, 9900 + e.getKey() * 7L);
                g2.setComposite(keep);
            }
            if (dragging && dropAt >= 0 && dropAt != dragFrom) {
                Rectangle b = cellBox(dropAt);
                Sketch.circle(g2, b.getCenterX(), b.getCenterY(), CELL * 0.42, 9800 + dropAt, 2.6f, Sketch.RED_PENCIL);
            }
            g2.dispose();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return CELL / 3; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return CELL * 3; }
        @Override public boolean getScrollableTracksViewportWidth() { return false; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ---------- worn gear: two pencil boxes above the bag ----------

    /** The 무기 and 방어구 slots: show what is worn, take a drop to wear, double-click to take off. */
    final class EquipSlots extends JComponent {
        static final int BOX = 70;
        /** -1 none, 0 weapon, 1 armor: the slot a drag hovers over. */
        int dropHover = -1;

        EquipSlots() {
            setOpaque(false);
            setPreferredSize(new Dimension(420, BOX + 14));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int s = slotAt(e.getPoint());
                    GearPiece worn = s == 0 ? player.getWeapon() : s == 1 ? player.getArmor() : null;
                    if (worn == null) return;
                    select(worn);
                    if (e.getClickCount() == 2) toggleEquip();
                }
            });
        }

        Rectangle slotBox(int s) {
            return new Rectangle(s * (BOX + 150) + 6, 6, BOX, BOX);
        }

        int slotAt(Point p) {
            for (int s = 0; s < 2; s++) if (slotBox(s).contains(p)) return s;
            return -1;
        }

        /** While dragging: which slot (if any) the point in root coordinates is over, for a piece that fits it. */
        void hoverFrom(Point inRoot, Object thing) {
            int s = -1;
            if (inRoot != null && thing instanceof GearPiece && !((GearPiece) thing).isDestroyed()) {
                s = slotAt(SwingUtilities.convertPoint(getRootPane(), inRoot, this));
                if (s >= 0 && (s == 0) != ((GearPiece) thing).isWeapon()) s = -1;
            }
            if (s != dropHover) {
                dropHover = s;
                repaint();
            }
        }

        /** A drop at the point: wears the piece if it landed on its slot. True if it was taken. */
        boolean dropOn(Point inRoot, Object thing) {
            if (!(thing instanceof GearPiece)) return false;
            GearPiece g = (GearPiece) thing;
            int s = slotAt(SwingUtilities.convertPoint(getRootPane(), inRoot, this));
            if (s < 0 || (s == 0) != g.isWeapon() || g.isDestroyed()) return false;
            player.wear(g);
            select(g);
            return true;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            GearPiece[] worn = {player.getWeapon(), player.getArmor()};
            String[] labels = {"무기", "방어구"};
            for (int s = 0; s < 2; s++) {
                Rectangle b = slotBox(s);
                boolean hot = dropHover == s;
                if (hot) Sketch.hatch(g2, Sketch.boxShape(b.x + 4, b.y + 4, b.width - 8, b.height - 8), 9600 + s, Sketch.RED_PENCIL, 60, 6, 35);
                Sketch.box(g2, b.x, b.y, b.width, b.height, 9610 + s, hot || worn[s] == selected && worn[s] != null ? 2.8f : 2.2f,
                        hot || worn[s] == selected && worn[s] != null ? Sketch.RED_PENCIL : Sketch.INK);
                if (worn[s] != null) drawThing(g2, worn[s], b.x, b.y, BOX, 9620 + s);
                g2.setFont(Sketch.hand(Font.BOLD, 15f));
                g2.setColor(Sketch.INK_SOFT);
                g2.drawString(labels[s], b.x + BOX + 12, b.y + 26);
                g2.setFont(Sketch.hand(Font.BOLD, 14f));
                g2.setColor(worn[s] == null ? Sketch.INK_SOFT : Sketch.gradeInk(worn[s].getItem().getGrade()));
                String name = worn[s] == null ? "비어 있음" : worn[s].getDisplayName();
                FontMetrics fm = g2.getFontMetrics();
                while (name.length() > 2 && fm.stringWidth(name) > 136) name = name.substring(0, name.length() - 2) + "…";
                g2.drawString(name, b.x + BOX + 12, b.y + 48);
            }
            g2.dispose();
        }
    }

    // ---------- the icon that follows the mouse ----------

    /** The glass pane: paints the dragged icon under the mouse, over everything else. */
    static final class Ghost extends JComponent {
        Image image;
        Point at;

        void follow(Image image, Point at) {
            this.image = image;
            this.at = at;
            setVisible(true);
            repaint();
        }

        void clear() {
            image = null;
            setVisible(false);
        }

        @Override protected void paintComponent(Graphics g) {
            if (image == null || at == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
            g2.drawImage(image, at.x - ICON / 2, at.y - ICON / 2, null);
            g2.dispose();
        }
    }
}
