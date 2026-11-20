package cassebrique;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Partie a deux ordinateurs (TCP). L'HOTE (joueur blanc) fait tourner toute la simulation et envoie ~60 images
 * par seconde ; l'INVITE (joueur noir) n'envoie que la position de sa raquette et ses clics, et affiche ce qu'il recoit.
 * Les fils reseau ne touchent jamais au jeu : tout passe par des files, lues dans pump() depuis le fil du jeu.
 * Aucune dependance a JavaFX.
 */
final class NetSession {
    enum Status { IDLE, LISTENING, CONNECTING, CONNECTED }

    static final int PORT = 5757;
    private static final int VERSION = 1;
    private static final int READ_TIMEOUT_MS = 8000;

    private static final int T_HELLO = 0, T_START = 1, T_SNAP = 2, T_INPUT = 3, T_ACTION = 4, T_PAUSE = 5,
            T_RESULTS = 6, T_LOBBY = 7, T_QUIT = 8, T_PING = 9;

    volatile Status status = Status.IDLE;
    volatile String message = "";
    String lastAddress = "";
    volatile String localAddresses = "";

    private final Game game;
    private volatile boolean host;
    private volatile boolean closing;
    private volatile ServerSocket server;
    private volatile Socket socket;
    private volatile String lostReason;
    private final ConcurrentLinkedQueue<byte[]> inbox = new ConcurrentLinkedQueue<byte[]>();
    private volatile LinkedBlockingQueue<byte[]> outbox = new LinkedBlockingQueue<byte[]>();
    private int generation; // distingue les connexions successives (un vieux fil ne doit rien changer)

    private double snapAcc, pingAcc;
    private final ByteArrayOutputStream evBuf = new ByteArrayOutputStream();
    private final DataOutputStream ev = new DataOutputStream(evBuf);
    private int evCount;

    NetSession(Game game) {
        this.game = game;
    }

    boolean connected() {
        return status == Status.CONNECTED;
    }

    boolean isHost() {
        return host;
    }

    // ------------------------------------------------------------------ connexion

    /** Hote : attend un joueur sur le port PORT. */
    void host() {
        close();
        closing = false;
        host = true;
        status = Status.LISTENING;
        message = "";
        localAddresses = findAddresses();
        final int gen = ++generation;
        Thread t = new Thread(new Runnable() {
            public void run() {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    ss.bind(new InetSocketAddress(PORT));
                    server = ss;
                    Socket s = ss.accept();
                    ss.close();
                    if (gen != generation) {
                        s.close();
                        return;
                    }
                    attach(s, gen);
                } catch (IOException e) {
                    if (gen == generation && !closing) {
                        status = Status.IDLE;
                        message = "Impossible d'\u00e9couter sur le port " + PORT + " (d\u00e9j\u00e0 utilis\u00e9 ?)";
                    }
                } finally {
                    if (ss != null) {
                        try {
                            ss.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
        }, "net-accept");
        t.setDaemon(true);
        t.start();
    }

    /** Invite : se connecte a "adresse" ou "adresse:port". */
    void join(final String address) {
        close();
        closing = false;
        host = false;
        lastAddress = address;
        status = Status.CONNECTING;
        message = "Connexion \u00e0 " + address + "...";
        final int gen = ++generation;
        Thread t = new Thread(new Runnable() {
            public void run() {
                String h = address;
                int port = PORT;
                int c = address.lastIndexOf(':');
                if (c > 0 && address.indexOf(':') == c) { // un seul ':' = adresse:port (pas d'IPv6 ici)
                    try {
                        port = Integer.parseInt(address.substring(c + 1).trim());
                        h = address.substring(0, c).trim();
                    } catch (NumberFormatException e) {
                        // on garde tout comme nom d'hote
                    }
                }
                Socket s = new Socket();
                try {
                    s.connect(new InetSocketAddress(h, port), 6000);
                    if (gen != generation) {
                        s.close();
                        return;
                    }
                    attach(s, gen);
                } catch (IOException e) {
                    try {
                        s.close();
                    } catch (IOException e2) {
                        // ignore
                    }
                    if (gen == generation && !closing) {
                        status = Status.IDLE;
                        message = "Connexion impossible (" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()) + ")";
                    }
                }
            }
        }, "net-connect");
        t.setDaemon(true);
        t.start();
    }

    private void attach(Socket s, final int gen) throws IOException {
        s.setTcpNoDelay(true);
        s.setSoTimeout(READ_TIMEOUT_MS);
        socket = s;
        final DataInputStream in = new DataInputStream(s.getInputStream());
        final DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(s.getOutputStream(), 8192));
        final LinkedBlockingQueue<byte[]> q = new LinkedBlockingQueue<byte[]>();
        outbox = q;
        message = "Connect\u00e9, v\u00e9rification...";
        sendHello();

        Thread w = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        byte[] m = q.take();
                        if (m.length == 0) return; // signal d'arret
                        out.writeInt(m.length);
                        out.write(m);
                        if (q.isEmpty()) out.flush();
                    }
                } catch (Exception e) {
                    fail(gen, "Connexion perdue");
                }
            }
        }, "net-write");
        w.setDaemon(true);
        w.start();

        Thread r = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        int len = in.readInt();
                        if (len <= 0 || len > 2000000) throw new IOException("message invalide");
                        byte[] b = new byte[len];
                        in.readFully(b);
                        if (gen != generation) return;
                        inbox.add(b);
                    }
                } catch (SocketTimeoutException e) {
                    fail(gen, "Plus de r\u00e9ponse de l'autre ordinateur");
                } catch (Exception e) {
                    fail(gen, "Connexion perdue");
                }
            }
        }, "net-read");
        r.setDaemon(true);
        r.start();
    }

    private void fail(int gen, String why) {
        if (gen != generation || closing) return;
        lostReason = why;
    }

    /** Ferme tout (sans prevenir le jeu : c'est lui qui le demande). */
    void close() {
        closing = true;
        generation++;
        lostReason = null;
        try {
            if (outbox != null) outbox.offer(new byte[0]);
        } catch (Exception e) {
            // ignore
        }
        closeQuiet(server);
        closeQuiet(socket);
        server = null;
        socket = null;
        inbox.clear();
        evBuf.reset();
        evCount = 0;
        status = Status.IDLE;
        message = "";
    }

    private static void closeQuiet(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private static String findAddresses() {
        StringBuilder sb = new StringBuilder();
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            while (nis != null && nis.hasMoreElements()) {
                NetworkInterface ni = nis.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                Enumeration<InetAddress> as = ni.getInetAddresses();
                while (as.hasMoreElements()) {
                    InetAddress a = as.nextElement();
                    if (a instanceof Inet4Address && !a.isLinkLocalAddress()) {
                        if (sb.length() > 0) sb.append("   ");
                        sb.append(a.getHostAddress());
                    }
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return sb.length() == 0 ? "(adresse introuvable)" : sb.toString();
    }

    // ------------------------------------------------------------------ envoi

    private void send(byte[] m) {
        if (status == Status.IDLE) return;
        outbox.offer(m);
    }

    private static DataOutputStream begin(ByteArrayOutputStream bo, int type) throws IOException {
        DataOutputStream d = new DataOutputStream(bo);
        d.writeByte(type);
        return d;
    }

    private void simple(int type) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            begin(bo, type);
            send(bo.toByteArray());
        } catch (IOException e) {
            // ne peut pas arriver (flux en memoire)
        }
    }

    private void sendHello() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            begin(bo, T_HELLO).writeInt(VERSION);
            outbox.offer(bo.toByteArray());
        } catch (IOException e) {
            // ignore
        }
    }

    void sendStart(int level, int theme) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream d = begin(bo, T_START);
            d.writeInt(level);
            d.writeByte(theme);
            send(bo.toByteArray());
        } catch (IOException e) {
            // ignore
        }
        snapAcc = 1;
    }

    void sendAction() {
        simple(T_ACTION);
    }

    void sendPause() {
        simple(T_PAUSE);
    }

    void sendQuit() {
        simple(T_QUIT);
    }

    void sendLobby() {
        simple(T_LOBBY);
    }

    void sendResults() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            DataOutputStream d = begin(bo, T_RESULTS);
            d.writeUTF(game.resultTitle);
            d.writeUTF(game.resultNote);
            d.writeByte(game.winner + 1);
            d.writeByte(game.eliminatedMask);
            for (Paddle p : game.paddles) {
                d.writeInt(p.score);
                for (int i = 0; i < Paddle.CATS; i++) d.writeInt(p.pts[i]);
            }
            send(bo.toByteArray());
        } catch (IOException e) {
            // ignore
        }
    }

    // ------------------------------------------------------------------ evenements (hote -> invite)

    /** Enregistre les sons et effets de l'hote pour les envoyer avec la prochaine image. */
    Game.Events eventSink() {
        evBuf.reset();
        evCount = 0;
        return new Game.Events() {
            public void sound(Sfx s) {
                if (!ready(8)) return;
                try {
                    ev.writeByte(1);
                    ev.writeByte(s.ordinal());
                    evCount++;
                } catch (IOException e) {
                    // ignore
                }
            }

            public void spawn(double x, double y, int n, double hue, double speed) {
                if (!ready(16)) return;
                try {
                    ev.writeByte(2);
                    ev.writeFloat((float) x);
                    ev.writeFloat((float) y);
                    ev.writeByte(n);
                    ev.writeShort((int) hue);
                    ev.writeShort((int) speed);
                    evCount++;
                } catch (IOException e) {
                    // ignore
                }
            }

            public void beam(boolean vertical, double pos, double a, double b, double thick) {
                if (!ready(32)) return;
                try {
                    ev.writeByte(3);
                    ev.writeBoolean(vertical);
                    ev.writeFloat((float) pos);
                    ev.writeFloat((float) a);
                    ev.writeFloat((float) b);
                    ev.writeFloat((float) thick);
                    evCount++;
                } catch (IOException e) {
                    // ignore
                }
            }

            public void rainbow(double x1, double y1, double x2, double y2) {
                if (!ready(32)) return;
                try {
                    ev.writeByte(4);
                    ev.writeFloat((float) x1);
                    ev.writeFloat((float) y1);
                    ev.writeFloat((float) x2);
                    ev.writeFloat((float) y2);
                    evCount++;
                } catch (IOException e) {
                    // ignore
                }
            }

            public void death(double[] d) {
                if (!ready(32)) return;
                try {
                    ev.writeByte(5);
                    for (int i = 0; i < 6; i++) ev.writeFloat((float) d[i]);
                    evCount++;
                } catch (IOException e) {
                    // ignore
                }
            }

            private boolean ready(int more) {
                return status == Status.CONNECTED && evBuf.size() + more < 12000 && evCount < 250;
            }
        };
    }

    // ------------------------------------------------------------------ boucle (fil du jeu)

    /** A appeler a chaque image, depuis le fil du jeu. */
    void pump(double dt) {
        String lost = lostReason;
        if (lost != null) {
            lostReason = null;
            boolean was = status != Status.IDLE;
            close();
            if (was) game.onNetLost(lost);
            return;
        }
        byte[] m;
        while ((m = inbox.poll()) != null) {
            try {
                handle(new DataInputStream(new ByteArrayInputStream(m)));
            } catch (IOException e) {
                // message tronque : on l'ignore
            }
        }
        if (status != Status.CONNECTED) return;

        pingAcc += dt;
        if (pingAcc > 2) {
            pingAcc = 0;
            simple(T_PING);
        }
        if (!game.netPlay) return;
        if (game.remote) {
            sendInput();
        } else {
            snapAcc += dt;
            if (snapAcc >= 0.015) {
                snapAcc = 0;
                sendSnapshot();
            }
        }
    }

    private void sendInput() {
        if (game.paddles == null || game.localIdx >= game.paddles.length) return;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            begin(bo, T_INPUT).writeFloat((float) game.paddles[game.localIdx].pos);
            send(bo.toByteArray());
        } catch (IOException e) {
            // ignore
        }
    }

    private void handle(DataInputStream d) throws IOException {
        int type = d.readUnsignedByte();
        switch (type) {
        case T_HELLO:
            if (d.readInt() != VERSION) {
                close();
                message = "Versions diff\u00e9rentes du jeu : mettez le m\u00eame programme sur les deux ordinateurs";
                return;
            }
            if (status != Status.CONNECTED) {
                status = Status.CONNECTED;
                message = "";
                game.onNetConnected();
            }
            break;
        case T_START:
            if (!host) game.startRemote(d.readInt(), d.readUnsignedByte());
            break;
        case T_SNAP:
            if (!host && game.remote) applySnapshot(d);
            break;
        case T_INPUT:
            if (host) game.remotePos = d.readFloat();
            break;
        case T_ACTION:
            if (host) game.remoteAction();
            break;
        case T_PAUSE:
            if (host) game.remotePause();
            break;
        case T_QUIT:
            if (host) game.remoteQuit();
            break;
        case T_LOBBY:
            if (!host) game.remoteLobby();
            break;
        case T_RESULTS:
            if (!host && game.remote) readResults(d);
            break;
        default:
            break; // T_PING et inconnus
        }
    }

    private void readResults(DataInputStream d) throws IOException {
        String title = d.readUTF(), note = d.readUTF();
        int win = d.readUnsignedByte() - 1, mask = d.readUnsignedByte();
        int[] scores = new int[2];
        int[][] pts = new int[2][Paddle.CATS];
        for (int p = 0; p < 2; p++) {
            scores[p] = d.readInt();
            for (int i = 0; i < Paddle.CATS; i++) pts[p][i] = d.readInt();
        }
        game.remoteResults(title, note, win, mask, scores, pts);
    }

    // ------------------------------------------------------------------ images

    private void sendSnapshot() {
        Game g = game;
        if (g.paddles == null || g.paddles.length < 2) return;
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(4096);
            DataOutputStream d = begin(bo, T_SNAP);
            d.writeByte(g.state.ordinal());
            d.writeFloat((float) g.shake);
            d.writeFloat((float) g.slowTimer);
            d.writeFloat((float) g.nightTimer);
            d.writeShort(g.goldRemaining);
            for (Paddle p : g.paddles) {
                d.writeFloat((float) p.pos);
                d.writeFloat((float) p.len);
                d.writeByte(p.lives);
                d.writeFloat((float) p.wideTimer);
                d.writeFloat((float) p.shrinkTimer);
                d.writeFloat((float) p.stickyTimer);
                d.writeFloat((float) p.fireTimer);
                d.writeFloat((float) p.wallTimer);
                d.writeFloat((float) p.nobreakTimer);
                d.writeFloat((float) p.ghostTimer);
                d.writeFloat((float) p.starTimer);
                d.writeFloat((float) p.lavaTimer);
                d.writeFloat((float) p.drunkTimer);
                d.writeFloat((float) p.serveTimer);
                d.writeByte(p.serveSecond);
            }
            d.writeShort(g.bricks.size());
            for (Brick k : g.bricks) {
                d.writeByte((k.alive ? 1 : 0) | (k.phaseSolid ? 2 : 0) | (Math.min(3, Math.max(0, k.hp)) << 2));
                d.writeByte((int) Math.min(255, Math.max(0, k.frost * 10)));
                if (k.kind == Brick.Kind.MOVER) {
                    d.writeFloat((float) k.x);
                    d.writeFloat((float) k.y);
                }
            }
            d.writeByte(Math.min(60, g.balls.size()));
            int nb = 0;
            for (Ball b : g.balls) {
                if (nb++ >= 60) break;
                d.writeFloat((float) b.x);
                d.writeFloat((float) b.y);
                d.writeFloat((float) b.dx);
                d.writeFloat((float) b.dy);
                d.writeFloat((float) b.speedNow);
                d.writeByte((b.stuck ? 1 : 0) | (b.owner << 1));
                d.writeFloat((float) b.stickOffset);
            }
            d.writeShort(g.powerUps.size());
            for (PowerUp u : g.powerUps) {
                d.writeByte(u.type.ordinal());
                d.writeFloat((float) u.x);
                d.writeFloat((float) u.y);
                d.writeFloat((float) u.vx);
                d.writeFloat((float) u.vy);
                d.writeFloat((float) u.t);
            }
            d.writeShort(g.ghosts.size());
            for (Ghost o : g.ghosts) {
                d.writeByte(o.owner);
                d.writeFloat((float) o.x);
                d.writeFloat((float) o.y);
                d.writeFloat((float) o.vx);
                d.writeFloat((float) o.vy);
                d.writeFloat((float) o.t);
            }
            d.writeShort(g.stars.size());
            for (Star o : g.stars) {
                d.writeByte(o.owner);
                d.writeFloat((float) o.x);
                d.writeFloat((float) o.y);
                d.writeFloat((float) o.vx);
                d.writeFloat((float) o.vy);
                d.writeFloat((float) o.t);
            }
            ev.flush();
            d.writeShort(evCount);
            byte[] evb = evBuf.toByteArray();
            d.write(evb);
            evBuf.reset();
            evCount = 0;
            if (outbox.size() < 6) send(bo.toByteArray()); // reseau lent : on saute des images plutot que de s'accumuler
        } catch (IOException e) {
            // ne peut pas arriver (flux en memoire)
        }
    }

    private void applySnapshot(DataInputStream d) throws IOException {
        Game g = game;
        if (g.paddles == null || g.paddles.length < 2 || g.state == Game.State.RESULTS) return;
        int st = d.readUnsignedByte();
        if (st < Game.State.values().length) {
            Game.State s = Game.State.values()[st];
            if (s == Game.State.PLAYING || s == Game.State.PAUSED || s == Game.State.READY) g.state = s;
        }
        g.shake = Math.max(g.shake, d.readFloat());
        g.slowTimer = d.readFloat();
        g.nightTimer = d.readFloat();
        g.goldRemaining = d.readUnsignedShort();
        for (int i = 0; i < 2; i++) {
            Paddle p = g.paddles[i];
            double pos = d.readFloat(), len = d.readFloat();
            if (i != g.localIdx) { // sa propre raquette reste pilotee en local : pas de retard
                p.pos = pos;
                p.len = len;
            }
            p.lives = d.readUnsignedByte();
            p.wideTimer = d.readFloat();
            p.shrinkTimer = d.readFloat();
            p.stickyTimer = d.readFloat();
            p.fireTimer = d.readFloat();
            p.wallTimer = d.readFloat();
            p.nobreakTimer = d.readFloat();
            p.ghostTimer = d.readFloat();
            p.starTimer = d.readFloat();
            p.lavaTimer = d.readFloat();
            p.drunkTimer = d.readFloat();
            p.serveTimer = d.readFloat();
            p.serveSecond = d.readUnsignedByte();
        }
        int nk = d.readUnsignedShort();
        if (nk != g.bricks.size()) return; // niveau different : image ignoree
        for (Brick k : g.bricks) {
            int f = d.readUnsignedByte();
            int hp = (f >> 2) & 3;
            boolean alive = (f & 1) != 0;
            if (alive && k.alive && hp < k.hp) k.flash = 0.15;
            if (alive != k.alive || (f & 2) != 0 != k.phaseSolid) k.flash = Math.max(k.flash, 0.3);
            k.alive = alive;
            k.phaseSolid = (f & 2) != 0;
            k.hp = hp;
            k.frost = d.readUnsignedByte() / 10.0;
            if (k.kind == Brick.Kind.MOVER) {
                k.x = d.readFloat();
                k.y = d.readFloat();
            }
        }
        int nb = d.readUnsignedByte();
        while (g.balls.size() > nb) g.balls.remove(g.balls.size() - 1);
        while (g.balls.size() < nb) g.balls.add(new Ball());
        for (int i = 0; i < nb; i++) {
            Ball b = g.balls.get(i);
            double x = d.readFloat(), y = d.readFloat();
            if (Math.hypot(x - b.x, y - b.y) > 60) b.clearTrail();
            b.x = x;
            b.y = y;
            b.dx = d.readFloat();
            b.dy = d.readFloat();
            b.speed = d.readFloat();
            int f = d.readUnsignedByte();
            b.stuck = (f & 1) != 0;
            b.owner = (f >> 1) & 1;
            b.stickOffset = d.readFloat();
        }
        g.powerUps.clear();
        int np = d.readUnsignedShort();
        PowerUp.Type[] types = PowerUp.Type.values();
        for (int i = 0; i < np; i++) {
            PowerUp.Type t = types[Math.min(types.length - 1, d.readUnsignedByte())];
            PowerUp u = new PowerUp(t, d.readFloat(), d.readFloat());
            u.vx = d.readFloat();
            u.vy = d.readFloat();
            u.t = d.readFloat();
            g.powerUps.add(u);
        }
        g.ghosts.clear();
        int ng = d.readUnsignedShort();
        for (int i = 0; i < ng; i++) {
            int o = d.readUnsignedByte();
            Ghost gh = new Ghost(o & 1, d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat());
            gh.t = d.readFloat();
            g.ghosts.add(gh);
        }
        g.stars.clear();
        int ns = d.readUnsignedShort();
        for (int i = 0; i < ns; i++) {
            int o = d.readUnsignedByte();
            Star s = new Star(o & 1, d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat());
            s.t = d.readFloat();
            g.stars.add(s);
        }
        int ne = d.readUnsignedShort();
        for (int i = 0; i < ne; i++) {
            int et = d.readUnsignedByte();
            switch (et) {
            case 1: {
                int o = d.readUnsignedByte();
                if (o < Sfx.values().length) g.playLocal(Sfx.values()[o]);
                break;
            }
            case 2:
                g.spawn(d.readFloat(), d.readFloat(), d.readUnsignedByte(), d.readShort(), d.readShort());
                break;
            case 3:
                g.beams.add(new Beam(d.readBoolean(), d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat()));
                break;
            case 4:
                g.rainbows.add(new Rainbow(d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat()));
                break;
            case 5:
                g.deaths.add(new double[] { d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat(), d.readFloat() });
                break;
            default:
                return;
            }
        }
    }
}
