package game.ui;

import game.ui.CharacterArt.Part;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * "Draw your own hero": the player draws their character in colored pencil on a notebook page. Parts
 * are picked freely on the left, the full figure stays in view, and a small preview shows the hero as
 * they will stand in battle. Arms and legs are drawn once and mirrored. Used when creating a character
 * and for 다시 그리기 on the select screen.
 */
public final class DrawCharacterDialog {
    /** What creating hands back: the typed name (may be blank) and the drawing. */
    public static final class Result {
        public final String name;
        public final CharacterArt art;

        Result(String name, CharacterArt art) {
            this.name = name;
            this.art = art;
        }
    }

    private enum Tool { PENCIL, FILL, ERASER }

    private static final Color[] PENCILS = {
            new Color(44, 42, 52), new Color(120, 116, 112), new Color(122, 78, 44), new Color(240, 196, 160),
            new Color(214, 58, 50), new Color(238, 134, 40), new Color(244, 200, 50), new Color(66, 156, 76),
            new Color(52, 120, 214), new Color(40, 56, 130), new Color(142, 78, 190), new Color(238, 130, 170)};
    private static final int STROKE_ALPHA = 235, FILL_ALPHA = 190;
    /** Fill stops at pixels at least this opaque, so a lightly shaded area can still be filled. */
    private static final int FILL_EDGE_ALPHA = 110;
    private static final float[] SIZES = {4.5f, 7.5f, 12f, 20f};
    private static final float ERASER_SIZE = 20f;
    private static final Part[] PARTS = Part.values();

    private final JDialog dialog;
    private final CharacterArt art;
    private final JTextField nameField;
    private final Canvas canvas = new Canvas();
    private final Preview preview = new Preview();
    private final PartButton[] partBtns = new PartButton[PARTS.length];
    private final Swatch[] swatches = new Swatch[PENCILS.length];
    private final SizeDot[] sizeDots = new SizeDot[SIZES.length];
    private final JButton pencilBtn = new Sketch.Button("연필", false);
    private final JButton fillBtn = new Sketch.Button("칠하기", false);
    private final JButton eraserBtn = new Sketch.Button("지우개", false);
    private final JLabel tipLabel = Sketch.label("", Font.PLAIN, 13f, Sketch.INK_SOFT);
    private final Deque<BufferedImage> undo = new ArrayDeque<>();   // one part's picture before each stroke
    private final Deque<Part> undoPart = new ArrayDeque<>();
    private Part part = Part.HEAD;
    private Color color = PENCILS[0];
    private int size = 1;
    private Tool tool = Tool.PENCIL;
    private boolean confirmed;

    public static Result create(Component owner) {
        DrawCharacterDialog d = new DrawCharacterDialog(owner, "내 캐릭터 그리기", null, CharacterArt.blank(), "완성!");
        d.show();
        if (!d.confirmed) return null;
        d.art.fillEmptyFromDefault();
        return new Result(d.nameField.getText(), d.art);
    }

    /** Redraw an existing character, starting from their current drawing; null if cancelled. */
    public static CharacterArt edit(Component owner, String name, CharacterArt current) {
        DrawCharacterDialog d = new DrawCharacterDialog(owner, "다시 그리기 — " + name, name, current.copy(), "저장");
        d.show();
        if (!d.confirmed) return null;
        d.art.fillEmptyFromDefault();
        return d.art;
    }

    private DrawCharacterDialog(Component owner, String title, String fixedName, CharacterArt art, String finishText) {
        this.art = art;
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        dialog = new JDialog(w, title, Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(12, 10), 34);
        page.setBorder(new EmptyBorder(14, 52, 14, 20));
        dialog.setContentPane(page);

        // ---------- top: title and name ----------
        JPanel top = new JPanel(new BorderLayout(16, 6));
        top.setOpaque(false);
        top.add(Sketch.title(fixedName == null ? "내 캐릭터 그리기" : "다시 그리기"), BorderLayout.WEST);
        nameField = new UnderlineField();
        if (fixedName == null) {
            JPanel name = new JPanel(new BorderLayout(8, 0));
            name.setOpaque(false);
            name.add(Sketch.label("이름", Font.BOLD, 16f, Sketch.INK), BorderLayout.WEST);
            name.add(nameField, BorderLayout.CENTER);
            name.setPreferredSize(new Dimension(260, 36));
            top.add(name, BorderLayout.EAST);
        } else {
            top.add(Sketch.label(fixedName, Font.BOLD, 18f, Sketch.BLUE_PENCIL), BorderLayout.EAST);
        }
        dialog.add(top, BorderLayout.NORTH);

        // ---------- left: parts, in any order ----------
        JPanel parts = new JPanel(new GridLayout(PARTS.length, 1, 0, 8));
        parts.setOpaque(false);
        parts.setPreferredSize(new Dimension(116, 0));
        for (int i = 0; i < PARTS.length; i++) {
            final Part p = PARTS[i];
            partBtns[i] = new PartButton(p);
            partBtns[i].addActionListener(e -> selectPart(p));
            parts.add(partBtns[i]);
        }
        dialog.add(parts, BorderLayout.WEST);

        // ---------- center: the page to draw on ----------
        Sketch.Box frame = new Sketch.Box(new BorderLayout(), 7301);
        frame.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.add(canvas, BorderLayout.CENTER);
        dialog.add(frame, BorderLayout.CENTER);

        dialog.add(buildTools(), BorderLayout.EAST);

        // ---------- bottom ----------
        JButton cancel = new Sketch.Button("취소", false);
        cancel.addActionListener(e -> dialog.dispose());
        JButton finish = new Sketch.Button(finishText, true);
        finish.addActionListener(e -> {
            confirmed = true;
            dialog.dispose();
        });
        JPanel bottom = new JPanel(new BorderLayout(14, 0));
        bottom.setOpaque(false);
        bottom.add(cancel, BorderLayout.WEST);
        bottom.add(tipLabel, BorderLayout.CENTER);
        bottom.add(finish, BorderLayout.EAST);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
        dialog.getRootPane().getActionMap().put("undo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { undo(); }
        });
        selectPart(Part.HEAD);
        refreshTools();
    }

    /** Right column: the hero preview, colored pencils, sizes and tools. */
    private JComponent buildTools() {
        JPanel tools = new JPanel();
        tools.setOpaque(false);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        tools.setPreferredSize(new Dimension(196, 0));

        addTool(tools, Sketch.label("전투에서 보이는 모습", Font.BOLD, 14f, Sketch.INK));
        preview.setPreferredSize(new Dimension(196, 150));
        preview.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        addFixed(tools, preview);

        addTool(tools, Sketch.label("색연필", Font.BOLD, 14f, Sketch.INK));
        JPanel palette = new JPanel(new GridLayout(3, 4, 5, 5));
        palette.setOpaque(false);
        for (int i = 0; i < PENCILS.length; i++) {
            final Color c = PENCILS[i];
            swatches[i] = new Swatch(c);
            swatches[i].addActionListener(e -> {
                color = c;
                if (tool == Tool.ERASER) tool = Tool.PENCIL;
                refreshTools();
            });
            palette.add(swatches[i]);
        }
        palette.setPreferredSize(new Dimension(196, 126));
        palette.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        addFixed(tools, palette);

        addTool(tools, Sketch.label("굵기", Font.BOLD, 14f, Sketch.INK));
        JPanel sizes = new JPanel(new GridLayout(1, SIZES.length, 5, 0));
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
        JButton clearBtn = new Sketch.Button("이 부위 지우기", false);
        clearBtn.addActionListener(e -> clearPart());
        JPanel buttons = new JPanel(new GridLayout(5, 1, 0, 0));
        buttons.setOpaque(false);
        for (JButton b : new JButton[]{pencilBtn, fillBtn, eraserBtn, undoBtn, clearBtn}) buttons.add(b);
        addFixed(tools, buttons);
        tools.add(javax.swing.Box.createVerticalGlue());
        return tools;
    }

    private static void addTool(JPanel tools, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        tools.add(c);
        tools.add(javax.swing.Box.createVerticalStrut(4));
    }

    private static void addFixed(JPanel tools, JComponent c) {
        addTool(tools, c);
        tools.add(javax.swing.Box.createVerticalStrut(8));
    }

    private void show() {
        dialog.setSize(1020, 790);
        dialog.setMinimumSize(new Dimension(900, 720));
        dialog.setLocationRelativeTo(dialog.getOwner());
        if (dialog.getTitle().startsWith("내")) SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private void selectPart(Part p) {
        part = p;
        tipLabel.setText("<html><b>" + p.label + "</b>  " + p.tip + "  빨간 점선 안이라면 어디든 그릴 수 있어요.</html>");
        for (PartButton b : partBtns) b.repaint();
        canvas.repaint();
    }

    private void useTool(Tool t) {
        tool = t;
        refreshTools();
    }

    private void refreshTools() {
        pencilBtn.setText((tool == Tool.PENCIL ? "√ " : "") + "연필");
        fillBtn.setText((tool == Tool.FILL ? "√ " : "") + "칠하기");
        eraserBtn.setText((tool == Tool.ERASER ? "√ " : "") + "지우개");
        canvas.setCursor(Cursor.getPredefinedCursor(tool == Tool.FILL ? Cursor.HAND_CURSOR : Cursor.CROSSHAIR_CURSOR));
        dialog.repaint();
    }

    private void drawingChanged() {
        art.changed();
        canvas.repaint();
        preview.repaint();
        for (PartButton b : partBtns) b.repaint();
    }

    private void snapshot() {
        BufferedImage src = art.image(part);
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        undo.push(copy);
        undoPart.push(part);
        while (undo.size() > 40) {
            undo.removeLast();
            undoPart.removeLast();
        }
    }

    private void undo() {
        if (undo.isEmpty()) return;
        BufferedImage snap = undo.pop();
        Part p = undoPart.pop();
        Graphics2D g = art.image(p).createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(snap, 0, 0, null);
        g.dispose();
        if (p != part) selectPart(p);
        drawingChanged();
    }

    private void clearPart() {
        snapshot();
        BufferedImage img = art.image(part);
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        drawingChanged();
    }

    /** Colors the open area around (x, y) on the part's picture, up to lines of pencil or the canvas edge. */
    private void fill(int x, int y) {
        BufferedImage img = art.image(part);
        int w = img.getWidth(), h = img.getHeight();
        if (x < 0 || y < 0 || x >= w || y >= h) return;
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
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
        img.setRGB(0, 0, w, h, px, 0, w);
    }

    /** A part with a small picture of what has been drawn on it so far. */
    private final class PartButton extends JButton {
        private final Part target;

        PartButton(Part target) {
            this.target = target;
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
            boolean on = target == part;
            if (on) Sketch.marker(g2, 8, h - 36, w - 16, 24, target.ordinal() * 13L + 3);
            Sketch.box(g2, 4, 4, w - 8, h - 8, 7400L + target.ordinal(), on ? 2.6f : 1.8f, on ? Sketch.RED_PENCIL : Sketch.INK);
            BufferedImage img = art.image(target);
            double s = Math.min((w - 24) / (double) img.getWidth(), (h - 56) / (double) img.getHeight());
            int iw = (int) (img.getWidth() * s), ih = (int) (img.getHeight() * s);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(img, (w - iw) / 2, 14, iw, ih, null);
            g2.setFont(Sketch.hand(Font.BOLD, 16f));
            g2.setColor(on ? Sketch.INK : Sketch.INK_SOFT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(target.label, (w - fm.stringWidth(target.label)) / 2, h - 18);
            g2.dispose();
        }
    }

    /** One colored pencil: a hatched dab of its color, circled while it is the one in hand. */
    private final class Swatch extends JButton {
        private final Color pencil;

        Swatch(Color pencil) {
            this.pencil = pencil;
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
            int r = Math.min(w, h) / 2 - 7;
            g2.setColor(pencil);
            g2.fillOval(w / 2 - r, h / 2 - r, r * 2, r * 2);
            boolean on = pencil.equals(color) && tool != Tool.ERASER;
            Sketch.circle(g2, w / 2.0, h / 2.0, r + (on ? 4 : 1), pencil.getRGB(), on ? 2.6f : 1.3f, on ? Sketch.RED_PENCIL : Sketch.INK);
            g2.dispose();
        }
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
            double d = width * 0.9;
            g2.fill(new java.awt.geom.Ellipse2D.Double((w - d) / 2, (h - d) / 2, d, d));
            boolean on = SIZES[size] == width && tool != Tool.ERASER;
            Sketch.box(g2, 3, 3, w - 6, h - 6, (long) width + 7500, on ? 2.4f : 1.3f, on ? Sketch.RED_PENCIL : Sketch.INK_SOFT);
            g2.dispose();
        }
    }

    /** The hero as they will stand in battle, at about that size, updated with each stroke. */
    private final class Preview extends JComponent {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            Sketch.box(g2, 2, 2, getWidth() - 4, getHeight() - 4, 7601, 1.8f, Sketch.INK);
            art.drawStill(g2, getWidth() / 2, getHeight() - 12, getHeight() - 56);
            g2.dispose();
        }
    }

    /** The drawing surface: the whole figure laid out, strokes go into the current part's image. */
    private final class Canvas extends JComponent {
        private double scale = 1, offX, offY;
        private Point2D last;
        private boolean lastRight;
        private Point mouse;

        Canvas() {
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    Point2D fig = toFigure(e.getPoint());
                    lastRight = part.paired() && fig.getX() > CharacterArt.FIG_W / 2.0;
                    snapshot();
                    Point2D at = toPart(fig, lastRight);
                    if (tool == Tool.FILL) {
                        fill((int) at.getX(), (int) at.getY());
                        drawingChanged();
                        return;
                    }
                    last = at;
                    stroke(last, last);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    mouse = e.getPoint();
                    if (last == null) return;
                    Point2D p = toPart(toFigure(e.getPoint()), lastRight);
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

        private void layoutFigure() {
            scale = Math.min(getWidth() / (double) CharacterArt.FIG_W, getHeight() / (double) CharacterArt.FIG_H) * 0.97;
            offX = (getWidth() - CharacterArt.FIG_W * scale) / 2;
            offY = (getHeight() - CharacterArt.FIG_H * scale) / 2;
        }

        private Point2D toFigure(Point p) {
            layoutFigure();
            return new Point2D.Double((p.x - offX) / scale, (p.y - offY) / scale);
        }

        private Point2D toPart(Point2D fig, boolean right) {
            try {
                return CharacterArt.toFigure(part, right).inverseTransform(fig, null);
            } catch (NoninvertibleTransformException e) {
                return fig;
            }
        }

        private void stroke(Point2D a, Point2D b) {
            Graphics2D g = art.image(part).createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            if (tool == Tool.ERASER) {
                g.setComposite(AlphaComposite.Clear);
                g.setStroke(new BasicStroke(ERASER_SIZE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), STROKE_ALPHA));
                g.setStroke(new BasicStroke(SIZES[size], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g.draw(new java.awt.geom.Line2D.Double(a, b));
            g.dispose();
            drawingChanged();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            layoutFigure();
            Graphics2D gf = (Graphics2D) g2.create();
            gf.translate(offX, offY);
            gf.scale(scale, scale);

            // Grey dashes mark where each part usually sits; red dashes the room this part has.
            float[] dash = {9f, 7f};
            gf.setColor(new Color(118, 112, 106, 80));
            gf.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
            for (Part p : PARTS) {
                for (boolean right : p.paired() ? new boolean[]{false, true} : new boolean[]{false}) {
                    Point o = CharacterArt.origin(p, right);
                    int bx = right ? o.x + p.w - p.baseX - p.baseW : o.x + p.baseX;
                    gf.drawRoundRect(bx, o.y + p.baseY, p.baseW, p.baseH, 18, 18);
                }
            }
            gf.setColor(new Color(190, 62, 52, 200));
            gf.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
            for (boolean right : part.paired() ? new boolean[]{false, true} : new boolean[]{false}) {
                Point o = CharacterArt.origin(part, right);
                gf.drawRoundRect(o.x, o.y, part.w, part.h, 18, 18);
            }

            // The drawing so far: the part in hand solid, the rest a little faded.
            float[] alpha = new float[PARTS.length];
            for (int i = 0; i < alpha.length; i++) alpha[i] = PARTS[i] == part ? 1f : 0.45f;
            art.paintFigure(gf, 0, 0, 0, 0, alpha);
            gf.dispose();

            // Brush outline under the mouse.
            if (mouse != null && tool != Tool.FILL) {
                double r = (tool == Tool.ERASER ? ERASER_SIZE : SIZES[size]) * scale / 2 + 1;
                g2.setColor(new Color(50, 48, 58, 140));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new java.awt.geom.Ellipse2D.Double(mouse.x - r, mouse.y - r, r * 2, r * 2));
            }
            g2.dispose();
        }
    }

    /** A see-through text field written on a pencil underline. */
    private static final class UnderlineField extends JTextField {
        UnderlineField() {
            setOpaque(false);
            setBorder(new EmptyBorder(4, 6, 6, 6));
            setFont(Sketch.hand(Font.BOLD, 17f));
            setForeground(Sketch.INK);
            setCaretColor(Sketch.INK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            Sketch.line(g2, 2, getHeight() - 3, getWidth() - 2, getHeight() - 4, 991, 2f, Sketch.INK);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
