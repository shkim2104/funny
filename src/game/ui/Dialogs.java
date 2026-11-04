package game.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Message, yes/no, text, pick-one and custom windows, all as a notebook page in pencil (see Sketch), like the
 * rest of the game. ESC closes any of them (see KeyBindings): a yes/no then answers no, a pick-one cancels.
 */
public final class Dialogs {
    private Dialogs() {}

    public static void message(Component owner, String title, String text) {
        JDialog dialog = build(owner, title);
        dialog.add(bodyLabel(text), BorderLayout.CENTER);

        JButton ok = Sketch.bigButton("확인", true);
        ok.addActionListener(e -> dialog.dispose());
        dialog.add(buttonRow(ok), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        Theme.arrowNav(ok);
        Theme.focusFirst(ok);

        finish(dialog, owner);
    }

    public static boolean confirm(Component owner, String title, String text) {
        JDialog dialog = build(owner, title);
        dialog.add(bodyLabel(text), BorderLayout.CENTER);

        boolean[] result = {false};
        JButton no = Sketch.bigButton("아니오", false);
        JButton yes = Sketch.bigButton("예", true);
        no.addActionListener(e -> dialog.dispose());
        yes.addActionListener(e -> { result[0] = true; dialog.dispose(); });
        dialog.add(buttonRow(no, yes), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(yes);
        Theme.arrowNav(no, yes);
        Theme.focusFirst(yes, no);

        finish(dialog, owner);
        return result[0];
    }

    public static String input(Component owner, String title, String prompt, String defaultValue) {
        JDialog dialog = build(owner, title);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.setOpaque(false);
        center.add(bodyLabel(prompt), BorderLayout.NORTH);
        JTextField field = new JTextField();
        field.setFont(Sketch.hand(Font.BOLD, 16f));
        field.setForeground(Sketch.INK);
        field.setBackground(new Color(255, 253, 245));
        field.setCaretColor(Sketch.INK);
        field.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Sketch.INK_SOFT, 1), new EmptyBorder(6, 10, 6, 10)));
        if (defaultValue != null) field.setText(defaultValue);
        center.add(field, BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);

        String[] result = {null};
        JButton cancel = Sketch.bigButton("취소", false);
        JButton ok = Sketch.bigButton("확인", true);
        cancel.addActionListener(e -> dialog.dispose());
        ok.addActionListener(e -> { result[0] = field.getText(); dialog.dispose(); });
        field.addActionListener(e -> { result[0] = field.getText(); dialog.dispose(); });
        dialog.add(buttonRow(cancel, ok), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);

        SwingUtilities.invokeLater(field::requestFocusInWindow);
        finish(dialog, owner);
        return result[0];
    }

    /** Shows a list of options as stacked pencil buttons; returns selected index or -1 if cancelled. */
    public static int choose(Component owner, String title, String prompt, String[] options) {
        return choose(owner, title, prompt, options, 420);
    }

    /** Like choose(), with a custom width for long or multi-line (HTML) options; height follows the content. */
    public static int choose(Component owner, String title, String prompt, String[] options, int width) {
        JDialog dialog = build(owner, title);

        JPanel wrap = new JPanel(new BorderLayout(10, 10));
        wrap.setOpaque(false);
        if (prompt != null) wrap.add(bodyLabel(prompt), BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        int[] result = {-1};
        JButton[] optionButtons = new JButton[options.length];
        for (int i = 0; i < options.length; i++) {
            JButton btn = new Sketch.Button(options[i], false);
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
            final int idx = i;
            btn.addActionListener(e -> { result[0] = idx; dialog.dispose(); });
            optionButtons[i] = btn;
            list.add(btn);
            list.add(Box.createVerticalStrut(6));
        }
        wrap.add(Sketch.scroll(list), BorderLayout.CENTER);
        dialog.add(wrap, BorderLayout.CENTER);

        JButton cancel = Sketch.bigButton("취소", false);
        cancel.addActionListener(e -> dialog.dispose());
        dialog.add(buttonRow(cancel), BorderLayout.SOUTH);

        JButton[] navButtons = new JButton[optionButtons.length + 1];
        System.arraycopy(optionButtons, 0, navButtons, 0, optionButtons.length);
        navButtons[optionButtons.length] = cancel;
        Theme.arrowNav(navButtons);
        Theme.focusFirst(navButtons);

        int listHeight = 0;
        for (JButton b : optionButtons) listHeight += b.getPreferredSize().height + 6;
        dialog.setSize(width + 60, Math.min(680, 230 + listHeight));
        dialog.setLocationRelativeTo(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner));
        dialog.setVisible(true);
        return result[0];
    }

    public static void custom(Component owner, String title, JComponent content, int width, int height) {
        JDialog dialog = build(owner, title);
        dialog.add(content, BorderLayout.CENTER);

        JButton ok = Sketch.bigButton("확인", true);
        ok.addActionListener(e -> dialog.dispose());
        dialog.add(buttonRow(ok), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        Theme.arrowNav(ok);
        Theme.focusFirst(ok);

        dialog.setSize(width + 40, height + 40);
        dialog.setLocationRelativeTo(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner));
        dialog.setVisible(true);
    }

    /** A modal notebook page with the title written in pencil at the top. */
    private static JDialog build(Component owner, String title) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(w, title, Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(16, 14), title.hashCode());
        page.setBorder(new EmptyBorder(16, 52, 16, 24));
        dialog.setContentPane(page);
        dialog.add(Sketch.title(title), BorderLayout.NORTH);
        return dialog;
    }

    private static void finish(JDialog dialog, Component owner) {
        dialog.pack();
        Dimension pref = dialog.getSize();
        dialog.setSize(Math.max(420, pref.width), Math.max(220, pref.height));
        dialog.setLocationRelativeTo(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner));
        dialog.setVisible(true);
    }

    /**
     * A plain wrapping JTextArea computes its preferred size before it has a real width to wrap
     * against, so dialog.pack() can undersize it and clip the tail of longer messages. An HTML
     * label with an explicit wrap width doesn't have that chicken-and-egg problem.
     */
    private static JComponent bodyLabel(String text) {
        String escaped = text.startsWith("<html>") ? text.substring(6).replace("</html>", "")
                : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        JLabel label = new JLabel("<html><body style='width:320px'>" + escaped + "</body></html>");
        label.setFont(Sketch.hand(Font.PLAIN, 16f));
        label.setForeground(Sketch.INK);
        return label;
    }

    private static JPanel buttonRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setOpaque(false);
        for (JButton b : buttons) row.add(b);
        return row;
    }
}
