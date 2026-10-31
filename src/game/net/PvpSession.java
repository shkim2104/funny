package game.net;

import game.model.Player;
import game.model.PvpBattle;
import game.save.SaveManager;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;

/**
 * One duel over a PvpLink. The player who made the room (the host) is side 0 and runs the real
 * PvpBattle: it collects both actions, resolves the round and sends the result. The other side only
 * sends its action and shows what it receives, so both screens always agree. Messages are
 * tab-separated fields; free text and data are Base64 so they can't break the format.
 */
public final class PvpSession {
    /** Raised when the rules change: two different versions must not duel. */
    private static final int PROTOCOL = 1;
    private static final Base64.Encoder B64 = Base64.getEncoder();
    private static final Base64.Decoder UNB64 = Base64.getDecoder();

    /** Called through the Executor given to the session (the Swing thread in the game). */
    public interface Listener {
        /** Both characters are known; the first round can start. */
        void onReady(Player opponent, List<String> opponentArt);
        /** The other player has picked (their pick stays secret). */
        void onOpponentChose();
        void onRound(List<PvpBattle.Event> events);
        void onClosed(String reason);
    }

    private final boolean host;
    private final Player me;
    private final List<String> myArt;
    private Listener listener;
    private final Executor post;
    private final Random rnd = new Random();

    private PvpLink link;
    private PvpBattle battle;
    private volatile boolean finished;
    private Integer myAction, theirAction;
    private final List<PvpBattle.Event> incoming = new ArrayList<>();

    public PvpSession(boolean host, Player me, List<String> myArt, Listener listener, Executor post) {
        this.host = host;
        this.me = me;
        this.myArt = myArt;
        this.listener = listener;
        this.post = post;
    }

    private final PvpLink.Handler handler = new PvpLink.Handler() {
        @Override public void onMessage(String message) {
            post.execute(() -> receive(message));
        }

        @Override public void onClosed(String reason) {
            post.execute(() -> {
                if (!finished) listener.onClosed(reason);
            });
        }
    };

    /** Blocks until someone joins; call off the Swing thread. Closing the given host cancels it. */
    public void hostOn(PvpLink.Host waiter) throws IOException {
        link = waiter.accept(handler);
        sendHello();
    }

    /** Connects to a room; call off the Swing thread. */
    public void joinAt(String address) throws IOException {
        link = PvpLink.join(address, handler);
        sendHello();
    }

    private void sendHello() {
        StringBuilder sb = new StringBuilder("HELLO\t" + PROTOCOL + "\t" + text64(SaveManager.encode(me)));
        for (String part : myArt) sb.append('\t').append(part);
        link.send(sb.toString());
    }

    private static String text64(String s) {
        return B64.encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String untext64(String s) {
        return new String(UNB64.decode(s), StandardCharsets.UTF_8);
    }

    /** Hands the session's events to whoever shows the duel next. */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public int mySide() { return host ? 0 : 1; }
    public int theirSide() { return host ? 1 : 0; }
    public PvpBattle getBattle() { return battle; }

    /** The local player's action for this round. */
    public void choose(int action) {
        if (battle == null || battle.isOver()) return;
        if (!host) {
            link.send("ACT\t" + action);
            return;
        }
        myAction = action;
        resolveIfBothChose();
    }

    private void receive(String message) {
        String[] f = message.split("\t", -1);
        try {
            switch (f[0]) {
                case "HELLO":
                    receiveHello(f);
                    break;
                case "ACT":
                    if (host && theirAction == null) {
                        theirAction = Integer.parseInt(f[1]);
                        listener.onOpponentChose();
                        resolveIfBothChose();
                    }
                    break;
                case "CHOSE":
                    listener.onOpponentChose();
                    break;
                case "EV":
                    incoming.add(decodeEvent(f));
                    break;
                case "END":
                    receiveEnd(f);
                    break;
                case "BYE":
                    fail("상대가 항복했다! 콜로세움의 승자다!");
                    break;
                default:
            }
        } catch (RuntimeException e) {
            fail("상대가 보낸 데이터를 읽지 못했어요.");
        }
    }

    private void fail(String reason) {
        close();
        listener.onClosed(reason);
    }

    private void receiveHello(String[] f) {
        if (battle != null || Integer.parseInt(f[1]) != PROTOCOL) {
            fail("게임 버전이 달라서 대전할 수 없어요. 같은 버전으로 맞춰 주세요.");
            return;
        }
        Player other = SaveManager.decode(untext64(f[2]));
        if (other == null) {
            fail("상대 캐릭터를 읽지 못했어요.");
            return;
        }
        List<String> art = new ArrayList<>();
        for (int i = 3; i < f.length; i++) art.add(f[i]);
        battle = host ? new PvpBattle(me, other, rnd) : new PvpBattle(other, me, rnd);
        listener.onReady(other, art);
    }

    private void resolveIfBothChose() {
        if (myAction == null) return;
        if (theirAction == null) {
            link.send("CHOSE");
            return;
        }
        List<PvpBattle.Event> events = battle.resolve(myAction, theirAction);
        myAction = null;
        theirAction = null;
        for (PvpBattle.Event e : events) {
            link.send("EV\t" + e.side + "\t" + e.damage + "\t" + (e.crit ? 1 : 0) + "\t" + e.hp[0] + "\t" + e.hp[1] + "\t" + e.mp[0] + "\t" + e.mp[1]
                    + "\t" + text64(e.text) + "\t" + (e.hit ? 1 : 0) + "\t" + e.fx + "\t" + e.fxColor);
        }
        link.send("END\t" + battle.getHp(0) + "\t" + battle.getHp(1) + "\t" + battle.getMp(0) + "\t" + battle.getMp(1)
                + "\t" + battle.getPotions(0) + "\t" + battle.getPotions(1));
        if (battle.isOver()) finished = true;
        listener.onRound(events);
    }

    private static PvpBattle.Event decodeEvent(String[] f) {
        return new PvpBattle.Event(Integer.parseInt(f[1]), untext64(f[8]), Integer.parseInt(f[2]), f[3].equals("1"),
                new int[]{Integer.parseInt(f[4]), Integer.parseInt(f[5])}, new int[]{Integer.parseInt(f[6]), Integer.parseInt(f[7])},
                f[9].equals("1"), f[10], Integer.parseInt(f[11]));
    }

    private void receiveEnd(String[] f) {
        battle.sync(new int[]{Integer.parseInt(f[1]), Integer.parseInt(f[2])}, new int[]{Integer.parseInt(f[3]), Integer.parseInt(f[4])},
                new int[]{Integer.parseInt(f[5]), Integer.parseInt(f[6])});
        List<PvpBattle.Event> events = new ArrayList<>(incoming);
        incoming.clear();
        if (battle.isOver()) finished = true;
        listener.onRound(events);
    }

    /** Gives up the duel: the other side is told, then the link closes. */
    public void surrender() {
        if (link != null) link.send("BYE");
        close();
    }

    public void close() {
        finished = true;
        if (link != null) link.close();
    }

    /** Adapters for virtual machines and VPNs have addresses the other laptop can't reach. */
    private static boolean isVirtualAdapter(String name) {
        String n = name == null ? "" : name.toLowerCase();
        for (String word : new String[]{"vmware", "virtualbox", "hyper-v", "vethernet", "wsl", "tailscale", "zerotier", "tap-", "vpn"}) {
            if (n.contains(word)) return true;
        }
        return false;
    }

    /** This computer's addresses on its networks, for the other player to type in. */
    public static List<String> localAddresses() {
        List<String> out = new ArrayList<>();
        try {
            for (NetworkInterface net : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!net.isUp() || net.isLoopback() || net.isVirtual() || isVirtualAdapter(net.getDisplayName())) continue;
                for (InetAddress a : Collections.list(net.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) out.add(a.getHostAddress());
                }
            }
        } catch (IOException e) {
            // Nothing to show: the room still works, the player can look the address up.
        }
        return out;
    }
}
