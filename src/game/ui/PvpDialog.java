package game.ui;

import game.model.Player;
import game.model.PvpBattle;
import game.net.PvpLink;
import game.net.PvpSession;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

/**
 * The colosseum's gate: make a room or join one by its address. Once both heroes are known this
 * window closes and hands the connected session over to the arena.
 */
public class PvpDialog extends JDialog implements PvpSession.Listener {
    /** Called with the connected session when the duel can begin. */
    public interface OnStart {
        void start(PvpSession session, Player opponent, CharacterArt opponentArt);
    }

    private final Player player;
    private final CharacterArt myArt;
    private final OnStart onStart;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    private final JTextField addressField = Theme.textField();
    private final JLabel lobbyNote = Sketch.label(" ", Font.BOLD, 13f, Sketch.RED_PENCIL);
    private final JLabel addressLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel waitNote = new JLabel(" ", SwingConstants.CENTER);

    private PvpSession session;
    private PvpLink.Host waiter;
    private boolean handedOff;

    public PvpDialog(Window owner, Player player, CharacterArt myArt, OnStart onStart) {
        super(owner, "콜로세움", Dialog.ModalityType.APPLICATION_MODAL);
        this.player = player;
        this.myArt = myArt;
        this.onStart = onStart;
        setSize(860, 520);
        setMinimumSize(new Dimension(760, 480));
        setLocationRelativeTo(owner);
        root.setOpaque(false);
        Sketch.Page page = new Sketch.Page(new BorderLayout(), 34);
        page.setBorder(new EmptyBorder(16, 52, 14, 22));
        page.add(root, BorderLayout.CENTER);
        setContentPane(page);
        root.add(buildLobby(), "LOBBY");
        root.add(buildWaiting(), "WAIT");
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (!handedOff) shutDown();
            }
        });
    }

    private JComponent buildLobby() {
        JPanel lobby = new JPanel(new BorderLayout(0, 16));
        lobby.setOpaque(false);
        JPanel head = new JPanel(new GridLayout(2, 1, 0, 2));
        head.setOpaque(false);
        head.add(Sketch.title("콜로세움"));
        head.add(Sketch.label("영웅끼리 맞붙는 곳이에요. 두 PC가 같은 와이파이나 핫스팟에 있어야 해요.", Font.PLAIN, 14f, Sketch.INK_SOFT));
        lobby.add(head, BorderLayout.NORTH);

        JButton create = new Sketch.Button("방 만들기", true);
        create.addActionListener(e -> createRoom());
        Sketch.Box left = new Sketch.Box(new BorderLayout(0, 14), 9001);
        left.setBorder(new EmptyBorder(24, 26, 24, 26));
        left.add(Sketch.label("도전자를 기다려요", Font.BOLD, 18f, Sketch.INK), BorderLayout.NORTH);
        left.add(Sketch.label("<html>만들면 나오는 주소를 상대에게 알려 주세요.</html>", Font.PLAIN, 14f, Sketch.INK_SOFT), BorderLayout.CENTER);
        left.add(create, BorderLayout.SOUTH);

        JButton join = new Sketch.Button("도전하기", true);
        join.addActionListener(e -> joinRoom());
        addressField.addActionListener(e -> joinRoom());
        addressField.setBackground(Color.WHITE);
        addressField.setForeground(Sketch.INK);
        addressField.setCaretColor(Sketch.INK);
        addressField.setFont(Sketch.hand(Font.BOLD, 17f));
        JPanel form = new JPanel(new BorderLayout(0, 6));
        form.setOpaque(false);
        form.add(Sketch.label("상대가 알려 준 주소", Font.PLAIN, 14f, Sketch.INK_SOFT), BorderLayout.NORTH);
        form.add(addressField, BorderLayout.CENTER);
        Sketch.Box right = new Sketch.Box(new BorderLayout(0, 14), 9002);
        right.setBorder(new EmptyBorder(24, 26, 24, 26));
        right.add(Sketch.label("상대에게 도전해요", Font.BOLD, 18f, Sketch.INK), BorderLayout.NORTH);
        right.add(form, BorderLayout.CENTER);
        right.add(join, BorderLayout.SOUTH);

        JPanel choices = new JPanel(new GridLayout(1, 2, 18, 0));
        choices.setOpaque(false);
        choices.add(left);
        choices.add(right);
        JPanel holder = new JPanel(new BorderLayout());
        holder.setOpaque(false);
        holder.add(choices, BorderLayout.NORTH);
        lobby.add(holder, BorderLayout.CENTER);

        JButton back = new Sketch.Button("나가기", false);
        back.addActionListener(e -> dispose());
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(lobbyNote, BorderLayout.WEST);
        south.add(back, BorderLayout.EAST);
        lobby.add(south, BorderLayout.SOUTH);
        return lobby;
    }

    private JComponent buildWaiting() {
        JPanel wait = new JPanel(new BorderLayout(0, 20));
        wait.setOpaque(false);
        wait.add(Sketch.title("도전자를 기다리는 중"), BorderLayout.NORTH);
        addressLabel.setFont(Sketch.hand(Font.BOLD, 34f));
        addressLabel.setForeground(Sketch.BLUE_PENCIL);
        waitNote.setFont(Sketch.hand(Font.PLAIN, 15f));
        waitNote.setForeground(Sketch.INK_SOFT);
        JPanel center = new JPanel(new GridLayout(3, 1, 0, 8));
        center.setOpaque(false);
        JLabel tell = new JLabel("상대에게 이 주소를 알려 주세요", SwingConstants.CENTER);
        tell.setFont(Sketch.hand(Font.BOLD, 18f));
        tell.setForeground(Sketch.INK);
        center.add(tell);
        center.add(addressLabel);
        center.add(waitNote);
        wait.add(center, BorderLayout.CENTER);
        JButton cancel = new Sketch.Button("취소", false);
        cancel.addActionListener(e -> {
            shutDown();
            showLobby(" ");
        });
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER));
        south.setOpaque(false);
        south.add(cancel);
        wait.add(south, BorderLayout.SOUTH);
        return wait;
    }

    private void showLobby(String note) {
        lobbyNote.setText(note);
        cards.show(root, "LOBBY");
    }

    private PvpSession newSession(boolean host) {
        return new PvpSession(host, player, myArt.toBase64(), this, SwingUtilities::invokeLater);
    }

    private void createRoom() {
        try {
            waiter = new PvpLink.Host();
        } catch (IOException e) {
            showLobby("방을 만들지 못했어요. 이미 다른 대전 창이 열려 있는지 확인해 주세요.");
            return;
        }
        List<String> addresses = PvpSession.localAddresses();
        addressLabel.setText(addresses.isEmpty() ? "주소를 찾지 못했어요" : String.join("   ", addresses));
        waitNote.setText("<html><div style='text-align:center'>윈도우 방화벽 창이 뜨면 <b>액세스 허용</b>을 눌러 주세요.<br>"
                + "주소가 여러 개면 상대와 같은 네트워크의 주소를 쓰세요.</div></html>");
        cards.show(root, "WAIT");
        PvpSession s = newSession(true);
        session = s;
        PvpLink.Host w = waiter;
        background(() -> {
            try {
                s.hostOn(w);
            } catch (IOException e) {
                // Cancelled: the waiter was closed and the lobby is already back.
            }
        });
    }

    private void joinRoom() {
        String address = addressField.getText().trim();
        if (address.isEmpty()) {
            showLobby("상대가 알려 준 주소를 입력해 주세요.");
            return;
        }
        showLobby("연결하는 중이에요...");
        PvpSession s = newSession(false);
        session = s;
        background(() -> {
            try {
                s.joinAt(address);
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> showLobby("연결하지 못했어요. 주소와 네트워크, 상대의 방을 확인해 주세요."));
            }
        });
    }

    private static void background(Runnable r) {
        Thread t = new Thread(r, "대전 연결");
        t.setDaemon(true);
        t.start();
    }

    @Override public void onReady(Player opponent, List<String> opponentArt) {
        handedOff = true;
        closeWaiter();
        PvpSession s = session;
        dispose();
        onStart.start(s, opponent, CharacterArt.fromBase64(opponentArt));
    }

    // The arena takes these over before any of them can happen.
    @Override public void onOpponentChose() { }
    @Override public void onRound(List<PvpBattle.Event> events) { }

    @Override public void onClosed(String reason) {
        shutDown();
        showLobby(reason);
    }

    private void closeWaiter() {
        if (waiter == null) return;
        try {
            waiter.close();
        } catch (IOException ignored) {
            // Already closed.
        }
        waiter = null;
    }

    private void shutDown() {
        closeWaiter();
        if (session != null) session.close();
        session = null;
    }
}
