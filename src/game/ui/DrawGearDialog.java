package game.ui;

import game.model.Crafting;
import game.model.Item;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 대장간 제작's drawing page: the player draws their weapon or armor in colored pencil, at the hero's own
 * scale (a faded hero stands behind for size), and names it. A weapon's hand is marked: whatever is drawn
 * through it is held there in battle, as big as it was drawn.
 */
final class DrawGearDialog {
    /** What the page hands back: the name and the picture (GearArt.GEAR_W x GEAR_H). */
    static final class Result {
        final String name;
        final BufferedImage drawing;

        Result(String name, BufferedImage drawing) {
            this.name = name;
            this.drawing = drawing;
        }
    }

    private enum Tool { PENCIL, FILL, ERASER }

    private static final int STROKE_ALPHA = 235, FILL_ALPHA = 200, FILL_EDGE_ALPHA = 110;
    private static final float[] SIZES = {4.5f, 7.5f, 12f, 20f, 32f};
    private static final float ERASER_SIZE = 24f;

    private final JDialog dialog;
    private final Item.Type type;
    private final CharacterArt hero;
    private final BufferedImage drawing = new BufferedImage(GearArt.GEAR_W, GearArt.GEAR_H, BufferedImage.TYPE_INT_ARGB);
    private final JTextField nameField = new JTextField();
    private final JLabel problem = Sketch.label(" ", Font.BOLD, 14f, Sketch.RED_PENCIL);
    private final Canvas canvas = new Canvas();
    private final JButton pencilBtn = new Sketch.Button("연필", false);
    private final JButton fillBtn = new Sketch.Button("칠하기", false);
    private final JButton eraserBtn = new Sketch.Button("지우개", false);
    private final JButton finish = new Sketch.Button("제작!", true);
    private final SizeDot[] sizeDots = new SizeDot[SIZES.length];
    private final Deque<BufferedImage> undo = new ArrayDeque<>();
    private final PencilPalette palette = new PencilPalette(c -> {
        this.color = c;
        if (this.tool == Tool.ERASER) this.tool = Tool.PENCIL;
        refreshTools();
    });
    private Color color = PencilPalette.BOX[0];
    private int size = 1;
    private Tool tool = Tool.PENCIL;
    private boolean confirmed;

    /** Opens the page; null if the player cancelled. */
    static Result draw(Component owner, Item.Type type, Item.Grade grade, CharacterArt hero) {
        DrawGearDialog d = new DrawGearDialog(owner, type, grade, hero);
        d.dialog.setSize(1060, 800);
        d.dialog.setMinimumSize(new Dimension(940, 720));
        d.dialog.setLocationRelativeTo(d.dialog.getOwner());
        SwingUtilities.invokeLater(d.nameField::requestFocusInWindow);
        d.dialog.setVisible(true);
        return d.confirmed ? new Result(d.nameField.getText().trim(), d.drawing) : null;
    }

    private DrawGearDialog(Component owner, Item.Type type, Item.Grade grade, CharacterArt hero) {
        this.type = type;
        this.hero = hero;
        Window w = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        String kind = type == Item.Type.WEAPON ? "무기" : "방어구";
        dialog = new JDialog(w, "장비 그리기 — [" + grade.getLabel() + "] " + kind, Dialog.ModalityType.APPLICATION_MODAL);
        // ESC must not throw a drawing away; its own buttons (and X) still close it.
        dialog.getRootPane().putClientProperty("noEscClose", Boolean.TRUE);
        Sketch.Page page = new Sketch.Page(new BorderLayout(12, 10), 34);
        page.setBorder(new EmptyBorder(14, 52, 14, 20));
        dialog.setContentPane(page);

        JPanel top = new JPanel(new BorderLayout(16, 6));
        top.setOpaque(false);
        JLabel title = Sketch.title("내 " + kind + " 그리기");
        top.add(title, BorderLayout.WEST);
        JPanel name = new JPanel(new BorderLayout(8, 0));
        name.setOpaque(false);
        name.add(Sketch.label("이름", Font.BOLD, 16f, Sketch.INK), BorderLayout.WEST);
        nameField.setOpaque(false);
        nameField.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Sketch.INK),
                new EmptyBorder(4, 6, 4, 6)));
        nameField.setFont(Sketch.hand(Font.BOLD, 17f));
        nameField.setForeground(Sketch.INK);
        nameField.setCaretColor(Sketch.INK);
        nameField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refreshFinish(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refreshFinish(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refreshFinish(); }
        });
        name.add(nameField, BorderLayout.CENTER);
        name.setPreferredSize(new Dimension(300, 36));
        top.add(name, BorderLayout.EAST);
        dialog.add(top, BorderLayout.NORTH);

        Sketch.Box frame = new Sketch.Box(new BorderLayout(), 7311);
        frame.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.add(canvas, BorderLayout.CENTER);
        dialog.add(frame, BorderLayout.CENTER);
        dialog.add(buildTools(), BorderLayout.EAST);

        JButton cancel = new Sketch.Button("취소", false);
        cancel.addActionListener(e -> dialog.dispose());
        finish.addActionListener(e -> {
            confirmed = true;
            dialog.dispose();
        });
        String tip = type == Item.Type.WEAPON
                ? "<html>빨간 동그라미가 <b>손</b>이에요. 손을 지나가게 손잡이를 그리면 전투에서 그 자리를 쥐어요. 크게 그리면 크게 들어요.</html>"
                : "<html>흐린 캐릭터는 크기를 보라고 있는 거예요. 원하는 갑옷이나 망토를 그려 보세요.</html>";
        JPanel bottom = new JPanel(new BorderLayout(14, 0));
        bottom.setOpaque(false);
        bottom.add(cancel, BorderLayout.WEST);
        JPanel mid = new JPanel(new GridLayout(2, 1));
        mid.setOpaque(false);
        mid.add(Sketch.label(tip, Font.PLAIN, 13f, Sketch.INK_SOFT));
        mid.add(problem);
        bottom.add(mid, BorderLayout.CENTER);
        bottom.add(finish, BorderLayout.EAST);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
        dialog.getRootPane().getActionMap().put("undo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { undo(); }
        });
        refreshTools();
        refreshFinish();
    }

    private JComponent buildTools() {
        JPanel tools = new JPanel();
        tools.setOpaque(false);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        tools.setPreferredSize(new Dimension(196, 0));
        addTool(tools, Sketch.label("색연필  (+ 로 색 추가)", Font.BOLD, 14f, Sketch.INK));
        addFixed(tools, palette);
        addTool(tools, Sketch.label("굵기", Font.BOLD, 14f, Sketch.INK));
        JPanel sizes = new JPanel(new GridLayout(1, SIZES.length, 4, 0));
        sizes.setOpaque(false);
        for (int i = 0; i < SIZES.length; i++) {
            final int idx = i;
            sizeDots[i] = new SizeDot(SIZES[i]);
            sizeDots[i].addActionListener(e -> {
                size = idx;
                if (tool == Tool.ERASER) tool = Tool.PENCIL;
                refreshTools();
            });
            sizes.add(sizeDots[i]);
        }
        sizes.setPreferredSize(new Dimension(196, 40));
        sizes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        addFixed(tools, sizes);
        pencilBtn.addActionListener(e -> useTool(Tool.PENCIL));
        fillBtn.addActionListener(e -> useTool(Tool.FILL));
        eraserBtn.addActionListener(e -> useTool(Tool.ERASER));
        JButton undoBtn = new Sketch.Button("되돌리기", false);
        undoBtn.addActionListener(e -> undo());
        JButton clearBtn = new Sketch.Button("모두 지우기", false);
        clearBtn.addActionListener(e -> clear());
        JPanel buttons = new JPanel(new GridLayout(5, 1, 0, 0));
        buttons.setOpaque(false);
        for (JButton b : new JButton[]{pencilBtn, fillBtn, eraserBtn, undoBtn, clearBtn}) buttons.add(b);
        addFixed(tools, buttons);
        tools.add(Box.createVerticalGlue());
        return tools;
    }

    private static void addTool(JPanel tools, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        tools.add(c);
        tools.add(Box.createVerticalStrut(4));
    }

    private static void addFixed(JPanel tools, JComponent c) {
        addTool(tools, c);
        tools.add(Box.createVerticalStrut(8));
    }

    private void useTool(Tool t) {
        tool = t;
        refreshTools();
    }

    private void refreshTools() {
        pencilBtn.setText((tool == Tool.PENCIL ? "√ " : "") + "연필");
        fillBtn.setText((tool == Tool.FILL ? "√ " : "") + "칠하기");
        eraserBtn.setText((tool == Tool.ERASER ? "√ " : "") + "지우개");
        palette.setInHand(tool != Tool.ERASER);
        canvas.setCursor(Cursor.getPredefinedCursor(tool == Tool.FILL ? Cursor.HAND_CURSOR : Cursor.CROSSHAIR_CURSOR));
        dialog.repaint();
    }

    /** 제작! needs a name that is free and something on the page. */
    private void refreshFinish() {
        String why = Crafting.nameProblem(nameField.getText());
        if (why == null && GearArt.isBlank(drawing)) why = "장비를 그려 주세요.";
        problem.setText(why == null ? " " : why);
        finish.setEnabled(why == null);
    }

    private void snapshot() {
        BufferedImage copy = new BufferedImage(drawing.getWidth(), drawing.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(drawing, 0, 0, null);
        g.dispose();
        undo.push(copy);
        while (undo.size() > 40) undo.removeLast();
    }

    private void undo() {
        if (undo.isEmpty()) return;
        Graphics2D g = drawing.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(undo.pop(), 0, 0, null);
        g.dispose();
        changed();
    }

    private void clear() {
        snapshot();
        Graphics2D g = drawing.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, drawing.getWidth(), drawing.getHeight());
        g.dispose();
        changed();
    }

    private void changed() {
        canvas.repaint();
        refreshFinish();
    }

    private void fill(int x, int y) {
        int w = drawing.getWidth(), h = drawing.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        int[] px = drawing.getRGB(0, 0, w, h, null, 0, w);
        if ((px[y * w + x] >>> 24) >= FILL_EDGE_ALPHA) return;
        int paint = (FILL_ALPHA << 24) | (color.getRGB() & 0xFFFFFF);
        boolean[] seen = new boolean[w * h];
        int[] stack = new int[w * h];
        int top = 0;
        stack[top++] = y * w + x;
        seen[y * w + x] = true;
        while (top > 0) {
            int at = stack[--top];
            px[at] = paint;
            int cx = at % w, cy = at / w;
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = cx + d[0], ny = cy + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                int next = ny * w + nx;
                if (seen[next] || (px[next] >>> 24) >= FILL_EDGE_ALPHA) continue;
                seen[next] = true;
                stack[top++] = next;
            }
        }
        drawing.setRGB(0, 0, w, h, px, 0, w);
    }

    /** One pencil size, shown at its real thickness. */
    private final class SizeDot extends JButton {
        private final float width;

        SizeDot(float width) {
            this.width = width;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = getWidth(), h = getHeight();
            g2.setColor(color);
            double d = Math.min(width * 0.8, h - 10);
            g2.fill(new Ellipse2D.Double((w - d) / 2, (h - d) / 2, d, d));
            boolean on = SIZES[size] == width && tool != Tool.ERASER;
            Sketch.box(g2, 3, 3, w - 6, h - 6, (long) width + 7510, on ? 2.4f : 1.3f, on ? Sketch.RED_PENCIL : Sketch.INK_SOFT);
            g2.dispose();
        }
    }

    /** The page: the faded hero for scale, the hand mark for a weapon, and the drawing on top. */
    private final class Canvas extends JComponent {
        private double scale = 1, offX, offY;
        private Point2D last;
        private Point mouse;

        Canvas() {
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    Point2D p = toPage(e.getPoint());
                    snapshot();
                    if (tool == Tool.FILL) {
                        fill((int) p.getX(), (int) p.getY());
                        changed();
                        return;
                    }
                    last = p;
                    stroke(p, p);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    mouse = e.getPoint();
                    if (last == null) return;
                    Point2D p = toPage(e.getPoint());
                    stroke(last, p);
                    last = p;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    last = null;
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    mouse = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mouse = null;
                    repaint();
                }
            };
            addMouseListener(m);
            addMouseMotionListener(m);
        }

        private void layoutPage() {
            scale = Math.min(getWidth() / (double) GearArt.GEAR_W, getHeight() / (double) GearArt.GEAR_H) * 0.98;
            offX = (getWidth() - GearArt.GEAR_W * scale) / 2;
            offY = (getHeight() - GearArt.GEAR_H * scale) / 2;
        }

        private Point2D toPage(Point p) {
            layoutPage();
            return new Point2D.Double((p.x - offX) / scale, (p.y - offY) / scale);
        }

        private void stroke(Point2D a, Point2D b) {
            Graphics2D g = drawing.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            if (tool == Tool.ERASER) {
                g.setComposite(AlphaComposite.Clear);
                g.setStroke(new BasicStroke(ERASER_SIZE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), STROKE_ALPHA));
                g.setStroke(new BasicStroke(SIZES[size], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g.draw(new Line2D.Double(a, b));
            g.dispose();
            changed();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            layoutPage();
            Graphics2D gp = (Graphics2D) g2.create();
            gp.translate(offX, offY);
            gp.scale(scale, scale);
            gp.setColor(new Color(190, 62, 52, 150));
            float[] dash = {9f, 7f};
            gp.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
            gp.drawRoundRect(0, 0, GearArt.GEAR_W, GearArt.GEAR_H, 18, 18);

            // The hero, faded, at the same scale as the gear: a weapon's hand on the grip, armor on the body.
            Graphics2D gh = (Graphics2D) gp.create();
            gh.clipRect(0, 0, GearArt.GEAR_W, GearArt.GEAR_H);
            gh.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            Point hand = CharacterArt.restingHand();
            if (type == Item.Type.WEAPON) gh.translate(GearArt.GRIP_X - hand.x, GearArt.GRIP_Y - hand.y);
            else gh.translate((GearArt.GEAR_W - CharacterArt.FIG_W) / 2.0, -120);
            gh.drawImage(hero.flat(), 0, 0, null);
            gh.dispose();

            if (type == Item.Type.WEAPON) {
                gp.setColor(new Color(190, 62, 52, 190));
                gp.setStroke(new BasicStroke(3f));
                gp.draw(new Ellipse2D.Double(GearArt.GRIP_X - 22, GearArt.GRIP_Y - 22, 44, 44));
                gp.setFont(Sketch.hand(Font.BOLD, 20f));
                gp.drawString("손", GearArt.GRIP_X + 28, GearArt.GRIP_Y + 8);
            }
            gp.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            gp.drawImage(drawing, 0, 0, null);
            gp.dispose();

            if (mouse != null && tool != Tool.FILL) {
                double r = (tool == Tool.ERASER ? ERASER_SIZE : SIZES[size]) * scale / 2 + 1;
                g2.setColor(new Color(50, 48, 58, 140));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new Ellipse2D.Double(mouse.x - r, mouse.y - r, r * 2, r * 2));
            }
            g2.dispose();
        }
    }
}
