package game.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Dialogs {
    private Dialogs() {}

    public static void message(Component owner, String title, String text) {
        JDialog dialog = build(owner, title);
        dialog.add(bodyLabel(text), BorderLayout.CENTER);

        JButton ok = Theme.primaryButton("확인");
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
        JButton no = Theme.button("아니오");
        JButton yes = Theme.primaryButton("예");
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
        JTextField field = Theme.textField();
        if (defaultValue != null) field.setText(defaultValue);
        center.add(field, BorderLayout.CENTER);
        dialog.add(center, BorderLayout.CENTER);

        String[] result = {null};
        JButton cancel = Theme.button("취소");
        JButton ok = Theme.primaryButton("확인");
        cancel.addActionListener(e -> dialog.dispose());
        ok.addActionListener(e -> { result[0] = field.getText(); dialog.dispose(); });
        field.addActionListener(e -> { result[0] = field.getText(); dialog.dispose(); });
        dialog.add(buttonRow(cancel, ok), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);

        SwingUtilities.invokeLater(field::requestFocusInWindow);
        finish(dialog, owner);
        return result[0];
    }

    /** Shows a list of options as stacked buttons; returns selected index or -1 if cancelled. */
    public static int choose(Component owner, String title, String prompt, String[] options) {
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
            JButton btn = Theme.button(options[i]);
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
            final int idx = i;
            btn.addActionListener(e -> { result[0] = idx; dialog.dispose(); });
            optionButtons[i] = btn;
            list.add(btn);
            list.add(Box.createVerticalStrut(8));
        }
        wrap.add(Theme.scroll(list), BorderLayout.CENTER);
        dialog.add(wrap, BorderLayout.CENTER);

        JButton cancel = Theme.button("취소");
        cancel.addActionListener(e -> dialog.dispose());
        dialog.add(buttonRow(cancel), BorderLayout.SOUTH);

        JButton[] navButtons = new JButton[optionButtons.length + 1];
        System.arraycopy(optionButtons, 0, navButtons, 0, optionButtons.length);
        navButtons[optionButtons.length] = cancel;
        Theme.arrowNav(navButtons);
        Theme.focusFirst(navButtons);

        dialog.setSize(380, Math.min(520, 220 + options.length * 46));
        dialog.setLocationRelativeTo(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner));
        dialog.setVisible(true);
        return result[0];
    }

    public static void custom(Component owner, String title, JComponent content, int width, int height) {
        JDialog dialog = build(owner, title);
        dialog.add(content, BorderLayout.CENTER);

        JButton ok = Theme.primaryButton("확인");
        ok.addActionListener(e -> dialog.dispose());
        dialog.add(buttonRow(ok), BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        Theme.arrowNav(ok);
        Theme.focusFirst(ok);

        dialog.setSize(width, height);
        dialog.setLocationRelativeTo(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner));
        dialog.setVisible(true);
    }

    private static JDialog build(Component owner, String title) {
        Window w = (owner instanceof Window) ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(w, title, Dialog.ModalityType.APPLICATION_MODAL);
        Theme.styleDialog(dialog);
        dialog.setLayout(new BorderLayout(16, 16));
        ((JComponent) dialog.getContentPane()).setBorder(new EmptyBorder(20, 22, 18, 22));

        JLabel header = Theme.header(title);
        header.setForeground(Theme.ACCENT);
        dialog.add(header, BorderLayout.NORTH);
        return dialog;
    }

    private static void finish(JDialog dialog, Component owner) {
        dialog.pack();
        Dimension pref = dialog.getSize();
        dialog.setSize(Math.max(360, pref.width), Math.max(170, pref.height));
        dialog.setLocationRelativeTo(owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner));
        dialog.setVisible(true);
    }

    /**
     * A plain wrapping JTextArea computes its preferred size before it has a real width to wrap
     * against, so dialog.pack() can undersize it and clip the tail of longer messages. An HTML
     * label with an explicit wrap width doesn't have that chicken-and-egg problem.
     */
    private static JComponent bodyLabel(String text) {
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        JLabel label = new JLabel("<html><body style='width:300px'>" + escaped + "</body></html>");
        label.setFont(Theme.BODY_FONT);
        label.setForeground(Theme.TEXT);
        return label;
    }

    private static JPanel buttonRow(JButton... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row.setOpaque(false);
        for (JButton b : buttons) row.add(b);
        return row;
    }
}
