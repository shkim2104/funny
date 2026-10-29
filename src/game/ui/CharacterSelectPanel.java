package game.ui;

import game.model.Player;
import game.save.SaveManager;
import game.save.SaveManager.CharacterSave;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MapleStory-style character select: a grid of character cards (sprite, name, level, job, last save)
 * plus "+ 캐릭터 생성" cards for the empty slots. Click a card to select it and click it again (or press
 * 게임 시작 / Enter) to play. The panel is transparent; the caller puts it over a background.
 */
public class CharacterSelectPanel extends JPanel {
    /** What the select screen asks the game to do. */
    public interface Listener {
        void start(String id, Player player);
        void create();
        void delete(String id, Player player);
        void redraw(String id, Player player);
        void back();
    }

    private static final int COLUMNS = 3;
    private static final SimpleDateFormat SAVED_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private final Listener listener;
    private final JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 14, 14));
    private final JButton startBtn = new Sketch.Button("게임 시작", true);
    private final JButton deleteBtn = new Sketch.Button("캐릭터 삭제", false);
    private final JButton redrawBtn = new Sketch.Button("다시 그리기", false);
    /** Each character's own drawing, loaded once per refresh. */
    private final java.util.Map<String, CharacterArt> arts = new java.util.HashMap<>();
    private final JButton backBtn = new Sketch.Button("뒤로", false);
    private final List<CharacterCard> cards = new ArrayList<>();
    private CharacterCard selected;

    public CharacterSelectPanel(Listener listener) {
        super(new GridBagLayout());
        this.listener = listener;
        setOpaque(false);

        JLabel title = Sketch.title("캐릭터 선택");
        title.setFont(Sketch.hand(Font.BOLD, 32f));
        JPanel titlePill = new Sketch.Note(new BorderLayout(), 9801);
        titlePill.setBorder(new EmptyBorder(12, 34, 10, 34));
        titlePill.add(title);

        grid.setOpaque(false);

        startBtn.addActionListener(e -> {
            if (selected != null && selected.save != null) listener.start(selected.save.id, selected.save.player);
        });
        deleteBtn.addActionListener(e -> {
            if (selected != null && selected.save != null) listener.delete(selected.save.id, selected.save.player);
        });
        redrawBtn.addActionListener(e -> {
            if (selected != null && selected.save != null) listener.redraw(selected.save.id, selected.save.player);
        });
        backBtn.addActionListener(e -> listener.back());
        JPanel buttons = new Sketch.Note(new GridLayout(1, 4, 10, 0), 9802);
        buttons.setBorder(new EmptyBorder(10, 14, 10, 14));
        buttons.add(backBtn);
        buttons.add(deleteBtn);
        buttons.add(redrawBtn);
        buttons.add(startBtn);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        titlePill.setAlignmentX(Component.CENTER_ALIGNMENT);
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        JPanel titleWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        titleWrap.setOpaque(false);
        titleWrap.add(titlePill);
        titleWrap.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(titleWrap);
        column.add(Box.createVerticalStrut(22));
        column.add(grid);
        column.add(Box.createVerticalStrut(18));
        column.add(buttons);
        buttons.setMaximumSize(new Dimension(COLUMNS * CharacterCard.W + (COLUMNS - 1) * 14 + 28, 70));
        add(column);

        // Esc backs out, like the back button.
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "select-back");
        getActionMap().put("select-back", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                listener.back();
            }
        });
    }

    /**
     * Rebuilds the cards from the saved characters (empty slots fill the rest) and selects selectId
     * if given, otherwise the most recently saved character.
     */
    public void refresh(List<CharacterSave> saves, String selectId) {
        grid.removeAll();
        cards.clear();
        selected = null;
        arts.clear();
        for (CharacterSave s : saves) arts.put(s.id, CharacterArt.load(s.id));
        for (int i = 0; i < SaveManager.MAX_CHARACTERS; i++) {
            CharacterCard card = new CharacterCard(i < saves.size() ? saves.get(i) : null);
            cards.add(card);
            grid.add(card);
        }
        Theme.arrowNav(cards.toArray(new JButton[0]));
        CharacterCard toSelect = null;
        for (CharacterCard c : cards) {
            if (c.save == null) continue;
            if (selectId != null) {
                if (c.save.id.equals(selectId)) toSelect = c;
            } else if (toSelect == null || c.save.savedAt > toSelect.save.savedAt) {
                toSelect = c;
            }
        }
        select(toSelect);
        grid.revalidate();
        grid.repaint();
        CharacterCard focus = toSelect != null ? toSelect : cards.get(0);
        SwingUtilities.invokeLater(focus::requestFocusInWindow);
    }

    private void select(CharacterCard card) {
        selected = card;
        boolean hasCharacter = card != null && card.save != null;
        startBtn.setEnabled(hasCharacter);
        deleteBtn.setEnabled(hasCharacter);
        redrawBtn.setEnabled(hasCharacter);
        for (CharacterCard c : cards) c.repaint();
    }

    /** One slot: a character card, or an empty "+ 캐릭터 생성" slot. */
    private final class CharacterCard extends JButton {
        static final int W = 190, H = 236;
        final CharacterSave save;
        boolean hover;

        CharacterCard(CharacterSave save) {
            this.save = save;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
            // Focus only moves the highlight; selecting happens on click/Enter. (Selecting on focus would make the
            // first click both select and start, since a mouse press focuses the card before the action fires.)
            addFocusListener(new java.awt.event.FocusAdapter() {
                @Override public void focusGained(java.awt.event.FocusEvent e) { repaint(); }
                @Override public void focusLost(java.awt.event.FocusEvent e) { repaint(); }
            });
            // Empty slot → create; a character → select, or start if it was already selected.
            addActionListener(e -> {
                if (this.save == null) {
                    listener.create();
                } else if (selected == this) {
                    listener.start(this.save.id, this.save.player);
                } else {
                    select(this);
                }
            });
        }

        /** Full size when there's room; shrinks (keeping proportions) so two rows still fit a small window. */
        @Override
        public Dimension getPreferredSize() {
            int avail = CharacterSelectPanel.this.getHeight();
            int h = avail <= 0 ? H : Math.max(170, Math.min(H, (avail - 200) / 2));
            return new Dimension(Math.round(h * W / (float) H), h);
        }

        /**
         * A scrap of notebook paper: the hero drawing on top, name / level / last save written below.
         * Selected = highlighter under the name and a heavier red-pencil frame; an empty slot is a faint
         * dashed pencil box with a big "+".
         */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Sketch.smooth(g);
            int w = getWidth(), h = getHeight();
            boolean isSelected = selected == this;
            boolean lit = hover || isFocusOwner();
            long seed = save == null ? cards.indexOf(this) * 97L + 5 : save.id.hashCode();

            if (save == null) {
                g2.setColor(new Color(247, 243, 230, lit ? 230 : 170));
                g2.fillRect(6, 6, w - 12, h - 12);
                dashedBox(g2, 6, 6, w - 12, h - 12, seed, lit ? Sketch.INK : Sketch.INK_SOFT);
                g2.setFont(Sketch.hand(Font.BOLD, 46f));
                g2.setColor(lit ? Sketch.INK : Sketch.INK_SOFT);
                drawCentered(g2, "+", h / 2 - 4);
                g2.setFont(Sketch.hand(Font.BOLD, 15f));
                drawCentered(g2, "캐릭터 생성", h / 2 + 30);
                g2.dispose();
                return;
            }

            // Paper card with a soft shadow and ruled lines.
            g2.setColor(new Color(0, 0, 0, 55));
            g2.fillRect(10, 10, w - 12, h - 12);
            g2.setColor(Sketch.PAPER);
            g2.fillRect(5, 5, w - 12, h - 12);
            g2.setColor(new Color(110, 150, 205, 60));
            g2.setStroke(new BasicStroke(1.2f));
            for (int y = 30; y < h - 12; y += 24) g2.drawLine(6, y, w - 8, y);

            // The hand-drawn hero near the top; the text block below keeps a fixed ~118px.
            int spriteH = Math.max(50, h - 118);
            int groundY = 20 + spriteH;
            CharacterArt art = arts.get(save.id);
            if (art != null) art.drawStill(g2, w / 2, groundY - 2, spriteH - 10);

            Player p = save.player;
            g2.setFont(Sketch.hand(Font.BOLD, 18f));
            String name = fit(g2, p.getName(), w - 24);
            if (isSelected) {
                int nw = g2.getFontMetrics().stringWidth(name);
                Sketch.marker(g2, w / 2.0 - nw / 2.0 - 10, groundY + 14, nw + 20, 18, seed + 7);
            }
            g2.setColor(Sketch.INK);
            drawCentered(g2, name, groundY + 30);
            g2.setFont(Sketch.hand(Font.BOLD, 14f));
            g2.setColor(Sketch.BLUE_PENCIL);
            drawCentered(g2, "Lv." + p.getLevel() + "  " + p.getJob().getDisplayName(), groundY + 52);
            g2.setFont(Sketch.hand(Font.PLAIN, 11f));
            g2.setColor(Sketch.INK_SOFT);
            drawCentered(g2, SAVED_FORMAT.format(new Date(save.savedAt)), groundY + 72);

            if (isSelected) {
                Sketch.box(g2, 5, 5, w - 12, h - 12, seed + 1, 3.2f, Sketch.RED_PENCIL);
            } else {
                Sketch.box(g2, 5, 5, w - 12, h - 12, seed + 1, lit ? 2.6f : 1.9f, lit ? Sketch.INK : Sketch.INK_SOFT);
            }
            g2.dispose();
        }

        /** A pencil box drawn in short dashes, for empty slots. */
        private void dashedBox(Graphics2D g2, double x, double y, double bw, double bh, long seed, Color c) {
            double dash = 14, gap = 9;
            double[][] sides = {{x, y, x + bw, y}, {x + bw, y, x + bw, y + bh}, {x + bw, y + bh, x, y + bh}, {x, y + bh, x, y}};
            long s = seed;
            for (double[] side : sides) {
                double len = Math.hypot(side[2] - side[0], side[3] - side[1]);
                double dx = (side[2] - side[0]) / len, dy = (side[3] - side[1]) / len;
                for (double t = 0; t < len; t += dash + gap) {
                    double e = Math.min(len, t + dash);
                    Sketch.line(g2, side[0] + dx * t, side[1] + dy * t, side[0] + dx * e, side[1] + dy * e, s++, 1.8f, c);
                }
            }
        }

        private void drawCentered(Graphics2D g2, String text, int baseline) {
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, baseline);
        }
    }

    private static String fit(Graphics2D g2, String text, int maxW) {
        FontMetrics fm = g2.getFontMetrics();
        if (fm.stringWidth(text) <= maxW) return text;
        String s = text;
        while (s.length() > 1 && fm.stringWidth(s + "…") > maxW) s = s.substring(0, s.length() - 1);
        return s + "…";
    }

}
