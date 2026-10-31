package game.net;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * The connection between two games on the same network: one TCP socket carrying text messages, each
 * sent as its length then its bytes. A quiet ping every couple of seconds lets each side notice when
 * the other is gone, even if the network dropped without closing the socket.
 */
public final class PvpLink implements Closeable {
    public static final int PORT = 47100;
    private static final int CONNECT_TIMEOUT_MS = 5000, PING_MS = 2000, SILENCE_LIMIT_MS = 8000;
    /** Hand-drawn pictures make the first message large; nothing legitimate is bigger than this. */
    private static final int MAX_MESSAGE_BYTES = 8 * 1024 * 1024;
    private static final String PING = "PING";

    /** Called on the link's own thread. */
    public interface Handler {
        void onMessage(String message);
        void onClosed(String reason);
    }

    private final Socket socket;
    private final DataOutputStream out;
    private final Handler handler;
    private volatile boolean closed;

    private PvpLink(Socket socket, Handler handler) throws IOException {
        this.socket = socket;
        this.handler = handler;
        socket.setSoTimeout(SILENCE_LIMIT_MS);
        socket.setTcpNoDelay(true);
        this.out = new DataOutputStream(socket.getOutputStream());
    }

    /** Waits for one player to join. close() on the returned host from another thread cancels the wait. */
    public static final class Host implements Closeable {
        private final ServerSocket server;

        public Host() throws IOException {
            server = new ServerSocket(PORT);
        }

        /** Blocks until someone connects; throws if closed first. */
        public PvpLink accept(Handler handler) throws IOException {
            Socket s = server.accept();
            server.close();
            PvpLink link = new PvpLink(s, handler);
            link.start();
            return link;
        }

        @Override public void close() throws IOException {
            server.close();
        }
    }

    public static PvpLink join(String address, Handler handler) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(address, PORT), CONNECT_TIMEOUT_MS);
        PvpLink link = new PvpLink(s, handler);
        link.start();
        return link;
    }

    private void start() {
        thread("대전 수신", this::readLoop);
        thread("대전 신호", this::pingLoop);
    }

    private static void thread(String name, Runnable r) {
        Thread t = new Thread(r, name);
        t.setDaemon(true);
        t.start();
    }

    public synchronized void send(String message) {
        if (closed) return;
        try {
            byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);
            out.write(bytes);
            out.flush();
        } catch (IOException e) {
            shutDown("상대와의 연결이 끊어졌어요.");
        }
    }

    private void readLoop() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            while (!closed) {
                int size = in.readInt();
                if (size < 0 || size > MAX_MESSAGE_BYTES) {
                    shutDown("상대가 잘못된 데이터를 보냈어요.");
                    return;
                }
                byte[] bytes = new byte[size];
                in.readFully(bytes);
                String message = new String(bytes, StandardCharsets.UTF_8);
                if (!PING.equals(message)) handler.onMessage(message);
            }
        } catch (SocketTimeoutException e) {
            shutDown("상대의 응답이 없어요. 네트워크를 확인해 주세요.");
        } catch (IOException e) {
            shutDown("상대와의 연결이 끊어졌어요.");
        }
    }

    private void pingLoop() {
        while (!closed) {
            send(PING);
            try {
                Thread.sleep(PING_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void shutDown(String reason) {
        synchronized (this) {
            if (closed) return;
            closed = true;
        }
        closeSocket();
        handler.onClosed(reason);
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Already gone.
        }
    }

    /** Ends the link from this side, without calling onClosed. */
    @Override public void close() {
        synchronized (this) {
            closed = true;
        }
        closeSocket();
    }
}
