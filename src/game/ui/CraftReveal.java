package game.ui;

import game.model.CraftedGear;
import game.model.Item;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * What came out of the forge: the drawing, the name in its grade's color, then the stat and each special
 * effect written in one after another, like the blacksmith reading them off.
 */
final class CraftReveal {
    private static final int LINE_MS = 650;

    private CraftReveal() {}

    static void show(Component owner, CraftedGear made) {
        Window w = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        JDialog d = new JDialog(w, "제작 완료", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(10, 10), 34);
        page.setBorder(new EmptyBorder(18, 52, 16, 24));
        d.setContentPane(page);

        Item item = made.toItem();
        JLabel picture = new JLabel(ItemArt.item(item, 170));
        picture.setHorizontalAlignment(SwingConstants.CENTER);
        d.add(picture, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel grade = Sketch.label("[" + item.getGrade().getLabel() + "] " + item.getTypeLabel() + " · 직접 제작", Font.BOLD, 15f,
                Sketch.gradeInk(item.getGrade()));
        JLabel name = Sketch.label(made.getName(), Font.BOLD, 26f, Sketch.gradeInk(item.getGrade()));
        text.add(grade);
        text.add(Box.createVerticalStrut(4));
        text.add(name);
        text.add(Box.createVerticalStrut(14));

        List<JLabel> lines = new ArrayList<>();
        lines.add(Sketch.label(item.getStatText(), Font.BOLD, 19f, Sketch.BLUE_PENCIL));
        for (String effect : item.getBonus().lines()) {
            lines.add(Sketch.label("<html><body style='width:330px'>· " + effect + "</body></html>", Font.BOLD, 17f, Sketch.RED_PENCIL));
        }
        if (item.getBonus().isEmpty()) lines.add(Sketch.label("부가 능력 없음", Font.PLAIN, 15f, Sketch.INK_SOFT));
        for (JLabel l : lines) {
            l.setVisible(false);
            text.add(l);
            text.add(Box.createVerticalStrut(6));
        }
        d.add(text, BorderLayout.CENTER);

        JButton ok = new Sketch.Button("확인", true);
        ok.addActionListener(e -> d.dispose());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottom.setOpaque(false);
        bottom.add(ok);
        d.add(bottom, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(ok);

        int[] shown = {0};
        Timer reveal = new Timer(LINE_MS, e -> {
            if (shown[0] < lines.size()) {
                lines.get(shown[0]++).setVisible(true);
                Sfx.play(Sfx.Sound.COIN);
                text.revalidate();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        reveal.setInitialDelay(500);
        reveal.start();
        d.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) { reveal.stop(); }
        });
        d.setSize(720, 430);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
    }
}
