package game.ui;

import game.model.Player;
import game.model.Story;
import game.model.StoryMaterial;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/** ???'s request: the 이계의 단서 it asks for, how many the hero holds, and 건네주기 once they are all there. */
public final class RiftQuestDialog {
    private RiftQuestDialog() {}

    /** Shows the open request; returns true if the hero handed it in. */
    public static boolean show(Component owner, Player player) {
        int stage = player.getStoryStage();
        Window w = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
        JDialog d = new JDialog(w, "???의 부탁", Dialog.ModalityType.APPLICATION_MODAL);
        Sketch.Page page = new Sketch.Page(new BorderLayout(14, 12), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 22));
        d.setContentPane(page);

        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(Sketch.title(Story.requestTitle(stage)));
        head.add(Sketch.label(Story.requestHint(stage), Font.PLAIN, 14f, Sketch.INK_SOFT));
        d.add(head, BorderLayout.NORTH);

        Map<StoryMaterial, Integer> need = Story.request(stage);
        JPanel list = new JPanel(new GridLayout(need.size(), 1, 0, 10));
        list.setOpaque(false);
        int i = 0;
        for (Map.Entry<StoryMaterial, Integer> e : need.entrySet()) list.add(row(e.getKey(), player.getStoryMaterial(e.getKey()), e.getValue(), i++));
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(list, BorderLayout.NORTH);
        d.add(holder, BorderLayout.CENTER);

        boolean[] handed = {false};
        boolean ready = Story.canHandIn(player, stage);
        JButton give = new Sketch.Button(ready ? "건네주기" : "아직 모자라", ready);
        give.setEnabled(ready);
        give.addActionListener(e -> {
            handed[0] = Story.handIn(player);
            d.dispose();
        });
        JButton leave = new Sketch.Button("나가기", false);
        leave.addActionListener(e -> d.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        south.setOpaque(false);
        south.add(leave);
        south.add(give);
        d.add(south, BorderLayout.SOUTH);
        d.getRootPane().setDefaultButton(ready ? give : leave);

        d.setSize(640, 210 + need.size() * 96);
        d.setLocationRelativeTo(owner);
        d.setVisible(true);
        return handed[0];
    }

    private static JComponent row(StoryMaterial m, int have, int need, int index) {
        Sketch.Box card = new Sketch.Box(new BorderLayout(14, 0), 9801L + index * 13);
        card.setBorder(new EmptyBorder(10, 16, 10, 16));
        card.add(new JLabel(ItemArt.storyMaterial(m, 56)), BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 4));
        text.setOpaque(false);
        boolean done = have >= need;
        text.add(Sketch.label(m.getLabel() + (done ? "  √" : ""), Font.BOLD, 16f, done ? new Color(54, 134, 58) : Sketch.INK));
        Sketch.Bar bar = new Sketch.Bar(done ? new Color(54, 134, 58) : new Color(106, 44, 154), 9850L + index);
        bar.set(Math.min(have, need), need);
        text.add(bar);
        card.add(text, BorderLayout.CENTER);
        return card;
    }
}
