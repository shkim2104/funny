package game.ui;

import game.ui.CharacterArt.Part;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * "Draw your own hero": the player pencils their character part by part (head, body, arm, leg) on a
 * notebook page, over dashed guides showing where each part goes. Arms and legs are drawn once and
 * mirrored. Used when creating a character and for 다시 그리기 on the select screen.
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

    private static final Color LEAD = new Color(44, 42, 52, 230);
    private static final float[] SIZES = {4.5f, 7.5f, 12f};
    private static final String[] SIZE_NAMES = {"가늘게", "보통", "굵게"};
    private static final Part[] STEPS = Part.values();

    private final JDialog dialog;
    private final CharacterArt art;
    private final JTextField nameField;
    private final Canvas canvas = new Canvas();
    private final StepBar stepBar = new StepBar();
    private final JLabel tipLabel = Sketch.label("", Font.PLAIN, 13f, Sketch.INK_SOFT);
    private final JButton prevBtn = new Sketch.Button("◀ 이전", false);
    private final JButton nextBtn = new Sketch.Button("다음 ▶", true);
    private final JButton[] sizeBtns = new JButton[SIZES.length];
    private final JButton eraserBtn = new Sketch.Button("지우개", false);
    private final Deque<BufferedImage[]> undo = new ArrayDeque<>();   // {snapshot} per stroke, with the step index in slot 1
    private final Deque<Integer> undoStep = new ArrayDeque<>();
    private int step;
    private int size = 1;
    private boolean eraser;
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
        Sketch.Page page = new Sketch.Page(new BorderLayout(14, 10), 34);
        page.setBorder(new EmptyBorder(14, 52, 14, 22));
        dialog.setContentPane(page);

        // ---------- top: title, name, steps ----------
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
        top.add(stepBar, BorderLayout.SOUTH);
        dialog.add(top, BorderLayout.NORTH);

        // ---------- center: the page to draw on ----------
        Sketch.Box frame = new Sketch.Box(new BorderLayout(), 7301);
        frame.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.add(canvas, BorderLayout.CENTER);
        dialog.add(frame, BorderLayout.CENTER);

        // ---------- right: tools ----------
        JPanel tools = new JPanel();
        tools.setOpaque(false);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        tools.setPreferredSize(new Dimension(170, 0));
        addTool(tools, Sketch.label("연필 굵기", Font.BOLD, 15f, Sketch.INK));
        for (int i = 0; i < SIZES.length; i++) {
            final int idx = i;
            sizeBtns[i] = new Sketch.Button("", false);
            sizeBtns[i].addActionListener(e -> {
                size = idx;
                eraser = false;
                refreshTools();
            });
            addTool(tools, sizeBtns[i]);
        }
        tools.add(javax.swing.Box.createVerticalStrut(8));
        eraserBtn.addActionListener(e -> {
            eraser = !eraser;
            refreshTools();
        });
        addTool(tools, eraserBtn);
        JButton undoBtn = new Sketch.Button("되돌리기", false);
        undoBtn.addActionListener(e -> undo());
        addTool(tools, undoBtn);
        JButton clearBtn = new Sketch.Button("이 부위 지우기", false);
        clearBtn.addActionListener(e -> clearPart());
        addTool(tools, clearBtn);
        tools.add(javax.swing.Box.createVerticalStrut(14));
        addTool(tools, tipLabel);
        tools.add(javax.swing.Box.createVerticalGlue());
        dialog.add(tools, BorderLayout.EAST);

        // ---------- bottom: navigation ----------
        JButton cancel = new Sketch.Button("취소", false);
        cancel.addActionListener(e -> dialog.dispose());
        prevBtn.addActionListener(e -> goTo(step - 1));
        nextBtn.addActionListener(e -> {
            if (step < STEPS.length - 1) {
                goTo(step + 1);
            } else {
                confirmed = true;
                dialog.dispose();
            }
        });
        this.finishText = finishText;
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(cancel, BorderLayout.WEST);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        nav.setOpaque(false);
        nav.add(prevBtn);
        nav.add(nextBtn);
        bottom.add(nav, BorderLayout.EAST);
        dialog.add(bottom, BorderLayout.SOUTH);

        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl Z"), "undo");
        dialog.getRootPane().getActionMap().put("undo", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { undo(); }
        });
        goTo(0);
        refreshTools();
    }

    private final String finishText;

    private static void addTool(JPanel tools, JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        tools.add(c);
        tools.add(javax.swing.Box.createVerticalStrut(6));
    }

    private void show() {
        dialog.setSize(900, 760);
        dialog.setMinimumSize(new Dimension(760, 640));
        dialog.setLocationRelativeTo(dialog.getOwner());
        if (nameField.isShowing() || dialog.getTitle().startsWith("내")) SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        dialog.setVisible(true);
    }

    private void goTo(int s) {
        step = Math.max(0, Math.min(STEPS.length - 1, s));
        Part p = STEPS[step];
        tipLabel.setText("<html><body style='width:135px'><b>" + (step + 1) + ". " + p.label + "</b><br>" + p.tip
                + "<br><br>빨간 점선 안에 그려요.</body></html>");
        prevBtn.setEnabled(step > 0);
        nextBtn.setText(step == STEPS.length - 1 ? finishText : "다음 ▶");
        stepBar.repaint();
        canvas.repaint();
    }

    private void refreshTools() {
        for (int i = 0; i < SIZES.length; i++) sizeBtns[i].setText((!eraser && size == i ? "√ " : "") + SIZE_NAMES[i]);
        eraserBtn.setText((eraser ? "√ " : "") + "지우개");
        dialog.repaint();
    }

    private void snapshot() {
        BufferedImage src = art.image(STEPS[step]);
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        undo.push(new BufferedImage[]{copy});
        undoStep.push(step);
        while (undo.size() > 40) {
            undo.removeLast();
            undoStep.removeLast();
        }
    }

    private void undo() {
        if (undo.isEmpty()) return;
        BufferedImage snap = undo.pop()[0];
        int s = undoStep.pop();
        BufferedImage target = art.image(STEPS[s]);
        Graphics2D g = target.createGraphics();
        g.setComposite(AlphaComposite.Src);
        g.drawImage(snap, 0, 0, null);
        g.dispose();
        art.changed();
        if (s != step) goTo(s);
        canvas.repaint();
    }

    private void clearPart() {
        snapshot();
        BufferedImage img = art.image(STEPS[step]);
        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
        art.changed();
        canvas.repaint();
    }

    /** "1 머리 · 2 몸통 · 3 팔 · 4 다리" with the current step highlighted. */
    private final class StepBar extends JComponent {
        StepBar() {
            setPreferredSize(new Dimension(400, 34));
            setFont(Sketch.hand(Font.BOLD, 15f));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int x = 4;
            for (int i = 0; i < STEPS.length; i++) {
                String s = (i + 1) + " " + STEPS[i].label;
                int w = fm.stringWidth(s);
                if (i == step) Sketch.marker(g2, x - 6, 8, w + 12, 18, i * 13L + 3);
                g2.setColor(i == step ? Sketch.INK : i < step ? Sketch.BLUE_PENCIL : Sketch.INK_SOFT);
                g2.drawString((i < step ? "√" : "") + s, x, 22);
                x += w + 46;
                if (i < STEPS.length - 1) {
                    g2.setColor(Sketch.INK_SOFT);
                    g2.drawString("→", x - 32, 22);
                }
            }
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
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            MouseAdapter m = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SwingUtilities.isLeftMouseButton(e)) return;
                    Point2D fig = toFigure(e.getPoint());
                    lastRight = STEPS[step].paired() && fig.getX() > CharacterArt.FIG_W / 2.0;
                    snapshot();
                    last = toPart(fig, lastRight);
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
            scale = Math.min(getWidth() / (double) CharacterArt.FIG_W, getHeight() / (double) CharacterArt.FIG_H) * 0.96;
            offX = (getWidth() - CharacterArt.FIG_W * scale) / 2;
            offY = (getHeight() - CharacterArt.FIG_H * scale) / 2;
        }

        private Point2D toFigure(Point p) {
            layoutFigure();
            return new Point2D.Double((p.x - offX) / scale, (p.y - offY) / scale);
        }

        private Point2D toPart(Point2D fig, boolean right) {
            try {
                return CharacterArt.toFigure(STEPS[step], right).inverseTransform(fig, null);
            } catch (NoninvertibleTransformException e) {
                return fig;
            }
        }

        private void stroke(Point2D a, Point2D b) {
            BufferedImage img = art.image(STEPS[step]);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            if (eraser) {
                g.setComposite(AlphaComposite.Clear);
                g.setStroke(new BasicStroke(20f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g.setColor(LEAD);
                g.setStroke(new BasicStroke(SIZES[size], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }
            g.draw(new java.awt.geom.Line2D.Double(a, b));
            g.dispose();
            art.changed();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            layoutFigure();
            Graphics2D gf = (Graphics2D) g2.create();
            gf.translate(offX, offY);
            gf.scale(scale, scale);

            // Dashed guides for every part; the current one in red pencil.
            float[] dash = {9f, 7f};
            for (Part p : Part.values()) {
                boolean cur = p == STEPS[step];
                gf.setColor(cur ? new Color(190, 62, 52, 200) : new Color(118, 112, 106, 90));
                gf.setStroke(new BasicStroke(cur ? 2.6f : 1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, dash, 0f));
                for (boolean right : p.paired() ? new boolean[]{false, true} : new boolean[]{false}) {
                    Point o = CharacterArt.origin(p, right);
                    gf.drawRoundRect(o.x, o.y, p.w, p.h, 18, 18);
                }
            }
            if (STEPS[step] == Part.HEAD && art.isEmpty(Part.HEAD)) {
                gf.setColor(new Color(118, 112, 106, 120));
                gf.setFont(Sketch.hand(Font.BOLD, 26f));
                gf.drawString("여기에 머리!", 128, 118);
            }

            // The drawing so far: current part solid, done parts a little faded, later ones faint.
            float[] alpha = new float[Part.values().length];
            for (int i = 0; i < alpha.length; i++) alpha[i] = i == step ? 1f : i < step ? 0.55f : 0.3f;
            art.paintFigure(gf, 0, 0, 0, 0, alpha);
            gf.dispose();

            // Brush outline under the mouse.
            if (mouse != null) {
                double r = (eraser ? 20f : SIZES[size]) * scale / 2 + 1;
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
