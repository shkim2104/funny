package game.ui;

import game.model.Player;
import game.model.Story;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * A story scene on a notebook page: who is talking (their picture and name) and the line, written out
 * a letter at a time. A click or Enter finishes the line, the next one moves on. The last line shows the
 * scene's choices as buttons; show() returns the one picked (0 for a plain "다음").
 */
public final class StoryDialog {
    private static final int TYPE_MS = 28;

    private final JDialog dialog;
    private final Story.Scene scene;
    private final Player player;
    private final CharacterArt heroArt;
    private final Portrait portrait = new Portrait();
    private final JLabel nameLabel = Sketch.label(" ", Font.BOLD, 20f, Sketch.INK);
    private final JLabel textLabel = new JLabel();
    private final JLabel moreLabel = Sketch.label(" ", Font.BOLD, 13f, Sketch.INK_SOFT);
    private final JPanel choiceRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
    private final Timer typer;
    private int line, shown;
    private int choice = -1;

    /** Plays the scene and returns the index of the choice made (0 when the scene has none, -1 if closed). */
    public static int show(Component owner, Story.Scene scene, Player player, CharacterArt heroArt) {
        StoryDialog d = new StoryDialog(owner, scene, player, heroArt);
        d.begin();
        return d.choice;
    }

    private StoryDialog(Component owner, Story.Scene scene, Player player, CharacterArt heroArt) {
        this.scene = scene;
        this.player = player;
        this.heroArt = heroArt;
        Window w = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        dialog = new JDialog(w, "이야기", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(18, 10), 34);
        page.setBorder(new EmptyBorder(18, 52, 16, 24));
        dialog.setContentPane(page);

        portrait.setPreferredSize(new Dimension(190, 210));
        dialog.add(portrait, BorderLayout.WEST);

        textLabel.setFont(Sketch.hand(Font.PLAIN, 18f));
        textLabel.setForeground(Sketch.INK);
        textLabel.setVerticalAlignment(SwingConstants.TOP);
        Sketch.Box box = new Sketch.Box(new BorderLayout(0, 8), 9901);
        box.setBorder(new EmptyBorder(16, 20, 14, 20));
        box.add(nameLabel, BorderLayout.NORTH);
        box.add(textLabel, BorderLayout.CENTER);
        JPanel foot = new JPanel(new BorderLayout());
        foot.setOpaque(false);
        choiceRow.setOpaque(false);
        foot.add(moreLabel, BorderLayout.WEST);
        foot.add(choiceRow, BorderLayout.EAST);
        box.add(foot, BorderLayout.SOUTH);
        dialog.add(box, BorderLayout.CENTER);

        MouseAdapter click = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { advance(); }
        };
        page.addMouseListener(click);
        box.addMouseListener(click);
        textLabel.addMouseListener(click);
        portrait.addMouseListener(click);
        for (String key : new String[]{"ENTER", "SPACE"}) {
            dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), "story-next");
        }
        dialog.getRootPane().getActionMap().put("story-next", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { advance(); }
        });
        typer = new Timer(TYPE_MS, e -> {
            String text = current().text;
            if (shown < text.length()) {
                shown++;
                showText();
            } else {
                ((Timer) e.getSource()).stop();
                finishedLine();
            }
        });
    }

    private Story.Line current() {
        return scene.lines.get(line);
    }

    private void begin() {
        startLine();
        dialog.setSize(820, 340);
        dialog.setLocationRelativeTo(dialog.getOwner());
        dialog.setVisible(true);
    }

    private void startLine() {
        Story.Line l = current();
        shown = 0;
        String name = l.speaker.name(player);
        nameLabel.setText(name == null || name.isEmpty() ? " " : name);
        nameLabel.setForeground(l.speaker == Story.Speaker.BOSS ? new Color(106, 44, 154)
                : l.speaker == Story.Speaker.MYSTERY ? Sketch.BLUE_PENCIL : Sketch.INK);
        portrait.speaker = l.speaker;
        portrait.repaint();
        choiceRow.removeAll();
        moreLabel.setText(" ");
        showText();
        typer.restart();
    }

    private void showText() {
        Story.Line l = current();
        boolean narration = l.speaker == Story.Speaker.NARRATION;
        String text = l.text.substring(0, shown).replace("\n", "<br>");
        textLabel.setText("<html><body style='width:350px'>" + (narration ? "<i>" + text + "</i>" : text) + "</body></html>");
        textLabel.setForeground(narration ? Sketch.INK_SOFT : Sketch.INK);
    }

    private boolean last() {
        return line == scene.lines.size() - 1;
    }

    /** The line is all written: say how to go on, or put up the choices. */
    private void finishedLine() {
        if (!last()) {
            moreLabel.setText("▼ 클릭해서 계속");
            return;
        }
        if (scene.choices.isEmpty()) {
            addChoice("닫기", 0, false);
        } else {
            for (int i = 0; i < scene.choices.size(); i++) addChoice(scene.choices.get(i), i, i == 0);
        }
        choiceRow.revalidate();
        dialog.repaint();
    }

    private void addChoice(String text, int index, boolean primary) {
        JButton b = new Sketch.Button(text, primary);
        b.addActionListener(e -> {
            choice = index;
            dialog.dispose();
        });
        choiceRow.add(b);
        if (index == 0) SwingUtilities.invokeLater(b::requestFocusInWindow);
    }

    private void advance() {
        if (typer.isRunning()) {
            typer.stop();
            shown = current().text.length();
            showText();
            finishedLine();
            return;
        }
        if (last()) return;   // the choices close it
        line++;
        startLine();
    }

    // ---------- the speaker's picture ----------

    private static BufferedImage load(String path) {
        try (InputStream in = StoryDialog.class.getResourceAsStream(path)) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {
            // Missing picture: a penciled stand-in is drawn instead.
        }
        return null;
    }

    private static final BufferedImage MYSTERY = load("images/story/npc_mystery.png");
    private static final BufferedImage BOSS = load("images/story/boss_face.png");

    private final class Portrait extends JComponent {
        Story.Speaker speaker = Story.Speaker.NARRATION;

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = getWidth(), h = getHeight();
            Sketch.box(g2, 4, 4, w - 8, h - 8, 9902, 2.2f, Sketch.INK);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int inner = Math.min(w, h) - 30;
            int x = (w - inner) / 2, y = (h - inner) / 2;
            switch (speaker) {
                case HERO:
                    if (heroArt != null) heroArt.drawStill(g2, w / 2, h - 16, h - 46);
                    break;
                case MYSTERY:
                    if (MYSTERY != null) g2.drawImage(MYSTERY, x, y, inner, inner, null);
                    else standIn(g2, w, h, Sketch.BLUE_PENCIL, "?");
                    break;
                case BOSS:
                    if (BOSS != null) g2.drawImage(BOSS, x, y, inner, inner, null);
                    else standIn(g2, w, h, new Color(106, 44, 154), "✦");
                    break;
                default:
                    Sketch.hatch(g2, new Rectangle(14, 14, w - 28, h - 28), 9903, Sketch.INK_SOFT, 50, 12, 40);
            }
            g2.dispose();
        }

        /** Until the picture is drawn: a hooded shape with a mark where the face would be. */
        private void standIn(Graphics2D g2, int w, int h, Color c, String mark) {
            Sketch.circle(g2, w / 2.0, h * 0.42, w * 0.26, mark.hashCode(), 2.6f, Sketch.INK);
            Sketch.hatch(g2, new java.awt.geom.Ellipse2D.Double(w / 2.0 - w * 0.26, h * 0.42 - w * 0.26, w * 0.52, w * 0.52),
                    7, c, 120, 7, 45);
            g2.setFont(Sketch.hand(Font.BOLD, 46f));
            g2.setColor(Sketch.PAPER);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(mark, (w - fm.stringWidth(mark)) / 2, (int) (h * 0.42) + fm.getAscent() / 3);
            Sketch.line(g2, w * 0.2, h - 20, w * 0.5, h * 0.62, 11, 2.6f, Sketch.INK);
            Sketch.line(g2, w * 0.8, h - 20, w * 0.5, h * 0.62, 12, 2.6f, Sketch.INK);
        }
    }
}
