package game.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * The ending: a notebook page where "끝" and the closing lines are written out in pencil, one after
 * another, then the credits, and a button back to the town that is at peace again.
 */
public final class EndingPanel extends JPanel {
    private static final String[] LINES = {
            "갈라졌던 하늘이 아물고, 아방가르드에 다시 아침이 왔다.",
            "사람들은 광장의 분수 옆에서, 별을 꺾은 영웅의 이야기를 했다.",
            "그리고 공책의 마지막 장에는, 아직 열리지 않은 길이 하나 그려져 있었다.",
    };
    private static final String[] CREDITS = {"공책 RPG", "기획 · 그림 · 개발  shkim2104", "끝까지 함께해 줘서 고마워요!"};
    private static final long LINE_MS = 1700;

    private final JButton back = new Sketch.Button("마을로", true);
    private final Timer timer;
    private long start;
    private String heroName = "";

    public EndingPanel(Runnable onBack) {
        super(new BorderLayout());
        setOpaque(true);
        setBorder(new EmptyBorder(0, 0, 30, 0));
        back.addActionListener(e -> onBack.run());
        back.setVisible(false);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(back);
        add(south, BorderLayout.SOUTH);
        timer = new Timer(30, e -> {
            if (elapsed() > (LINES.length + CREDITS.length + 2) * LINE_MS) {
                ((Timer) e.getSource()).stop();
                back.setVisible(true);
                back.requestFocusInWindow();
            }
            repaint();
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) { start -= LINE_MS; }   // a click hurries it on
        });
    }

    /** Starts writing the ending for this hero. */
    public void play(String heroName) {
        this.heroName = heroName;
        start = System.currentTimeMillis();
        back.setVisible(false);
        timer.restart();
    }

    private long elapsed() {
        return System.currentTimeMillis() - start;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Sketch.smooth(g);
        int w = getWidth(), h = getHeight();
        Sketch.paper(g2, w, h, 60);
        long t = elapsed();
        int y = Math.max(70, h / 6);

        g2.setFont(Sketch.hand(Font.BOLD, 64f));
        String title = "- 끝 -";
        FontMetrics fm = g2.getFontMetrics();
        float a = (float) Math.min(1, t / 900.0);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
        g2.setColor(Sketch.INK);
        g2.drawString(title, (w - fm.stringWidth(title)) / 2, y + fm.getAscent());
        Sketch.line(g2, w / 2.0 - 120, y + fm.getAscent() + 14, w / 2.0 - 120 + 240 * a, y + fm.getAscent() + 12, 3, 2.6f, Sketch.RED_PENCIL);
        y += fm.getHeight() + 40;

        g2.setFont(Sketch.hand(Font.PLAIN, 20f));
        fm = g2.getFontMetrics();
        String[] story = new String[LINES.length + 1];
        story[0] = heroName + "의 모험은 여기까지.";
        System.arraycopy(LINES, 0, story, 1, LINES.length);
        for (int i = 0; i < story.length; i++) y = writeLine(g2, story[i], t - (i + 1) * LINE_MS, y, fm, Sketch.INK, w);
        y += 30;
        g2.setFont(Sketch.hand(Font.BOLD, 17f));
        fm = g2.getFontMetrics();
        for (int i = 0; i < CREDITS.length; i++) {
            y = writeLine(g2, CREDITS[i], t - (story.length + 1 + i) * LINE_MS, y, fm, i == 0 ? Sketch.BLUE_PENCIL : Sketch.INK_SOFT, w);
        }
        g2.dispose();
    }

    /** One line written left to right as `since` grows; returns the y for the next line. */
    private static int writeLine(Graphics2D g2, String text, long since, int y, FontMetrics fm, Color c, int w) {
        if (since > 0) {
            int letters = (int) Math.min(text.length(), since / 45);
            String part = text.substring(0, letters);
            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(c);
            g2.drawString(part, (w - fm.stringWidth(text)) / 2, y + fm.getAscent());
        }
        return y + fm.getHeight() + 12;
    }
}
