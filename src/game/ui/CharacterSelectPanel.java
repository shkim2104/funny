package game.ui;

import game.model.Player;
import game.save.SaveManager;
import game.save.SaveManager.CharacterSave;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
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
        void back();
    }

    private static final int COLUMNS = 3;
    private static final BufferedImage SPRITE = loadSprite();
    private static final SimpleDateFormat SAVED_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private final Listener listener;
    private final JPanel grid = new JPanel(new GridLayout(0, COLUMNS, 14, 14));
    private final JButton startBtn = Theme.ghostPrimaryButton("게임 시작");
    private final JButton deleteBtn = Theme.ghostButton("캐릭터 삭제");
    private final JButton backBtn = Theme.ghostButton("뒤로");
    private final List<CharacterCard> cards = new ArrayList<>();
    private CharacterCard selected;

    public CharacterSelectPanel(Listener listener) {
        super(new GridBagLayout());
        this.listener = listener;
        setOpaque(false);

        JLabel title = new JLabel("캐릭터 선택", SwingConstants.CENTER);
        title.setFont(Theme.dosFont(Font.BOLD, 34f));
        title.setForeground(Color.WHITE);
        JPanel titlePill = new Pill();
        titlePill.setBorder(new EmptyBorder(10, 30, 10, 30));
        titlePill.add(title);

        grid.setOpaque(false);

        startBtn.addActionListener(e -> {
            if (selected != null && selected.save != null) listener.start(selected.save.id, selected.save.player);
        });
        deleteBtn.addActionListener(e -> {
            if (selected != null && selected.save != null) listener.delete(selected.save.id, selected.save.player);
        });
        backBtn.addActionListener(e -> listener.back());
        JPanel buttons = new JPanel(new GridLayout(1, 3, 10, 0));
        buttons.setOpaque(false);
        buttons.add(backBtn);
        buttons.add(deleteBtn);
        buttons.add(startBtn);

        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        titlePill.setAlignmentX(Component.CENTER_ALIGNMENT);
        grid.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttons.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(titlePill);
        column.add(Box.createVerticalStrut(22));
        column.add(grid);
        column.add(Box.createVerticalStrut(18));
        column.add(buttons);
        buttons.setMaximumSize(new Dimension(COLUMNS * CharacterCard.W + (COLUMNS - 1) * 14, 46));
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

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            boolean isSelected = selected == this;
            RoundRectangle2D box = new RoundRectangle2D.Float(2, 2, w - 4, h - 4, 18, 18);

            if (save == null) {
                g2.setColor(new Color(10, 12, 16, hover || isFocusOwner() ? 150 : 100));
                g2.fill(box);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{8f, 7f}, 0f));
                g2.setColor(isFocusOwner() ? Theme.ACCENT_HOVER : new Color(255, 255, 255, 110));
                g2.draw(box);
                g2.setFont(Theme.dosFont(Font.BOLD, 46f));
                g2.setColor(new Color(255, 255, 255, 170));
                drawCentered(g2, "+", h / 2 - 4);
                g2.setFont(Theme.dosFont(Font.BOLD, 15f));
                drawCentered(g2, "캐릭터 생성", h / 2 + 30);
                g2.dispose();
                return;
            }

            // Card body; the selected one gets a warm glow and gold rim.
            g2.setColor(new Color(10, 12, 16, isSelected ? 200 : (hover ? 170 : 140)));
            g2.fill(box);
            if (isSelected) {
                g2.setPaint(new RadialGradientPaint(new Point(w / 2, h / 2 - 20), w * 0.6f,
                        new float[]{0f, 1f}, new Color[]{new Color(199, 168, 106, 70), new Color(199, 168, 106, 0)}));
                g2.fill(box);
            }

            // Sprite on a small shadow, near the top; the text block below keeps a fixed ~118px.
            int spriteH = Math.max(50, h - 118);
            int groundY = 20 + spriteH;
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fillOval(w / 2 - 34, groundY - 7, 68, 14);
            if (SPRITE != null) {
                int sw = (int) Math.round(SPRITE.getWidth() * (spriteH / (double) SPRITE.getHeight()));
                Graphics2D gi = (Graphics2D) g2.create();
                gi.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                gi.drawImage(SPRITE, w / 2 - sw / 2, groundY - spriteH, sw, spriteH, null);
                gi.dispose();
            }

            Player p = save.player;
            g2.setFont(Theme.dosFont(Font.BOLD, 18f));
            g2.setColor(Color.WHITE);
            drawCentered(g2, fit(g2, p.getName(), w - 20), groundY + 30);
            g2.setFont(Theme.dosFont(Font.BOLD, 14f));
            g2.setColor(Theme.ACCENT_HOVER);
            drawCentered(g2, "Lv." + p.getLevel() + "  " + p.getJob().getDisplayName(), groundY + 52);
            g2.setFont(Theme.dosFont(Font.PLAIN, 11f));
            g2.setColor(new Color(190, 192, 200));
            drawCentered(g2, SAVED_FORMAT.format(new Date(save.savedAt)), groundY + 72);

            g2.setStroke(new BasicStroke(isSelected ? 3f : (isFocusOwner() || hover ? 2f : 1f)));
            g2.setColor(isSelected ? Theme.ACCENT : new Color(255, 255, 255, isFocusOwner() || hover ? 140 : 60));
            g2.draw(box);
            g2.dispose();
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

    /** Dark rounded backing so the title stays readable over the background art. */
    private static final class Pill extends JPanel {
        Pill() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(10, 12, 16, 140));
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static BufferedImage loadSprite() {
        try (InputStream in = CharacterSelectPanel.class.getResourceAsStream("images/player.png")) {
            if (in != null) return ImageIO.read(in);
        } catch (Exception ignored) {
            // Missing sprite: cards just show the text.
        }
        return null;
    }
}
