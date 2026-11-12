package cassebrique;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** Etat et logique du jeu (solo et duel). Aucune dependance a JavaFX. */
final class Game {
    // Monde logique (le rendu est mis a l'echelle de la fenetre)
    static final double W = 960, H = 640;
    static final double TOP = 56;
    static final double BW = 60, BH = 24, GAP = 4;
    static final double OFFX = 34, OFFY = 100; // grille solo (14 colonnes)
    static final int DUEL_COLS = 11;           // grille duel, centree

    static final double PADDLE_Y = H - 46;               // solo : face haute de la raquette
    static final double LEFT_FACE = 44, RIGHT_FACE = W - 44; // duel : faces des raquettes
    static final double SOLO_LEN = 110, DUEL_LEN = 130;
    static final double BALL_R = 7;
    static final double MAX_SPEED = 820;
    static final double KEY_SPEED = 780;
    static final int DUEL_PENALTY = 100; // points perdus quand la balle sort de son cote

    // Mise en page du menu (partagee avec le rendu)
    static final int CARD_COLS = 4, PER_PAGE = 12;
    static final double CARD_W = 210, CARD_H = 80, CARD_GAP = 15, CARD_X0 = 37.5, CARD_Y0 = 215, CARD_PITCH_Y = 92;
    static final double CAMP_X = 300, CAMP_Y = 126, CAMP_W = 360, CAMP_H = 50;
    static final double PAGE_Y = 482, PAGE_W = 44, PAGE_H = 28, PREV_X = 376, NEXT_X = 540;

    enum State { MENU, READY, PLAYING, PAUSED, LEVEL_CLEAR, GAME_OVER, WIN }

    State state = State.MENU;
    private State beforePause = State.PLAYING;

    Paddle[] paddles;
    final List<Ball> balls = new ArrayList<Ball>();
    final List<Brick> bricks = new ArrayList<Brick>();
    final List<PowerUp> powerUps = new ArrayList<PowerUp>();
    final List<Particle> particles = new ArrayList<Particle>();
    final List<Beam> beams = new ArrayList<Beam>();
    private final Random rnd = new Random();

    // Partie en cours
    boolean campaign;           // true : niveaux solo enchaines ; false : un seul niveau choisi
    boolean twoPlayer;          // niveau duel en cours
    int level, lives, levelBonus;
    int server;                 // duel : joueur qui sert
    int winner = -1;            // duel : 0 blanc, 1 noir, -1 egalite
    int remaining;              // briques destructibles restantes
    int goldTotal, goldRemaining;
    boolean endedByGold;
    private Paddle scorer;      // joueur credite des briques cassees en ce moment

    // Records / options (sauvegardes par Storage)
    int highScore;
    final int[] levelBest = new int[Levels.count()];
    boolean saveDirty, newRecord, muted;

    // Balle et effets globaux
    double baseSpeed, ballSpeed;
    double slowTimer, buttonCooldown;
    double shake, stateTime, time;

    // Menu
    int menuPage, hoverCard = -1;
    boolean hoverCampaign, hoverPrev, hoverNext;
    private double mx, my = -1000;

    /** Branche le son (voir Sounds). Peut rester null. */
    Consumer<Sfx> sfx;

    private double mouseX = Double.NaN;

    Game() {
        loadLevel(0, false); // initialise le decor ; on retourne ensuite au menu
        state = State.MENU;
    }

    int score() {
        return paddles[0].score;
    }

    int multiplier(int player) {
        return Math.min(8, 1 + paddles[player].combo / 4);
    }

    int pageCount() {
        return Math.max(1, (Levels.count() + PER_PAGE - 1) / PER_PAGE);
    }

    private double fieldBottom() {
        return twoPlayer ? H : PADDLE_Y;
    }

    // ------------------------------------------------------------------ entrees

    /** dir : -1 = gauche/haut, +1 = droite/bas. */
    void setMove(int player, int dir, boolean down) {
        if (paddles == null || player >= paddles.length) return;
        Paddle p = paddles[player];
        if (dir < 0) p.keyNeg = down;
        else p.keyPos = down;
        if (down && !twoPlayer) mouseX = Double.NaN;
    }

    void clearKeys() {
        if (paddles == null) return;
        for (Paddle p : paddles) p.keyNeg = p.keyPos = false;
    }

    void mouseMoved(double x, double y) {
        mx = x;
        my = y;
        if (!twoPlayer) mouseX = x;
        refreshHover();
    }

    /** Le curseur quitte la fenetre : la raquette file vers le bord par lequel il est sorti. */
    void mouseExited(double x) {
        if (!twoPlayer) mouseX = x;
        mx = -1000;
        my = -1000;
        refreshHover();
    }

    void click(double x, double y) {
        mouseMoved(x, y);
        if (state == State.MENU) {
            if (hoverCard >= 0) {
                play(Sfx.MENU_CLICK);
                startSingle(hoverCard);
            } else if (hoverCampaign) {
                play(Sfx.MENU_CLICK);
                startCampaign();
            } else if (hoverPrev) {
                changePage(-1);
            } else if (hoverNext) {
                changePage(1);
            }
        } else {
            action(0);
        }
    }

    /**
     * Espace / entree / clic. who : 0 = clic ou indifferent, 1 = espace (joueur blanc), 2 = entree (joueur noir).
     */
    void action(int who) {
        switch (state) {
        case MENU:
            startCampaign();
            break;
        case READY:
            if (!twoPlayer || who == 0 || who - 1 == server) launch();
            break;
        case PLAYING:
            releaseStuck(twoPlayer && who > 0 ? who - 1 : -1);
            break;
        case PAUSED:
            togglePause();
            break;
        case LEVEL_CLEAR:
            if (stateTime > 0.4) {
                if (campaign) loadLevel(Levels.nextSolo(level), true);
                else goMenu();
            }
            break;
        case GAME_OVER:
        case WIN:
            if (stateTime > 0.6) goMenu();
            break;
        default:
            break;
        }
    }

    void togglePause() {
        if (state == State.PLAYING || state == State.READY) {
            beforePause = state;
            state = State.PAUSED;
        } else if (state == State.PAUSED) {
            state = beforePause;
        }
    }

    void pauseIfPlaying() {
        if (state == State.PLAYING || state == State.READY) togglePause();
    }

    /** Retour au menu depuis la pause ou un ecran de fin. */
    void backToMenu() {
        if (state == State.PAUSED || state == State.LEVEL_CLEAR || state == State.GAME_OVER || state == State.WIN) {
            goMenu();
        }
    }

    private void goMenu() {
        finalizeScore();
        balls.clear();
        powerUps.clear();
        beams.clear();
        clearKeys();
        twoPlayer = false;
        state = State.MENU;
        stateTime = 0;
        refreshHover();
    }

    // ------------------------------------------------------------------ menu

    static double cardX(int local) {
        return CARD_X0 + (local % CARD_COLS) * (CARD_W + CARD_GAP);
    }

    static double cardY(int local) {
        return CARD_Y0 + (local / CARD_COLS) * CARD_PITCH_Y;
    }

    int cardAt(double x, double y) {
        int first = menuPage * PER_PAGE;
        for (int local = 0; local < PER_PAGE && first + local < Levels.count(); local++) {
            double cx = cardX(local), cy = cardY(local);
            if (x >= cx && x <= cx + CARD_W && y >= cy && y <= cy + CARD_H) return first + local;
        }
        return -1;
    }

    boolean overCampaign(double x, double y) {
        return x >= CAMP_X && x <= CAMP_X + CAMP_W && y >= CAMP_Y && y <= CAMP_Y + CAMP_H;
    }

    private static boolean in(double x, double y, double rx, double ry, double rw, double rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private void refreshHover() {
        boolean menu = state == State.MENU;
        hoverCard = menu ? cardAt(mx, my) : -1;
        hoverCampaign = menu && overCampaign(mx, my);
        hoverPrev = menu && menuPage > 0 && in(mx, my, PREV_X, PAGE_Y, PAGE_W, PAGE_H);
        hoverNext = menu && menuPage < pageCount() - 1 && in(mx, my, NEXT_X, PAGE_Y, PAGE_W, PAGE_H);
    }

    void changePage(int delta) {
        int n = (int) clamp(menuPage + delta, 0, pageCount() - 1);
        if (n != menuPage) {
            menuPage = n;
            play(Sfx.MENU_CLICK);
            refreshHover();
        }
    }

    // ------------------------------------------------------------------ cycle de vie

    void startCampaign() {
        campaign = true;
        beginRun(Levels.firstSolo());
    }

    void startSingle(int index) {
        campaign = false;
        beginRun(index);
    }

    private void beginRun(int index) {
        lives = 3;
        newRecord = false;
        loadLevel(index, false);
    }

    void loadLevel(int index, boolean keepScore) {
        Levels.Level lv = Levels.get(index);
        int keep = (keepScore && paddles != null && !twoPlayer && !lv.twoPlayer) ? paddles[0].score : 0;
        level = index;
        twoPlayer = lv.twoPlayer;
        bricks.clear();
        powerUps.clear();
        balls.clear();
        particles.clear();
        beams.clear();
        remaining = 0;
        goldTotal = 0;

        double pitchX = BW + GAP, pitchY = BH + GAP;
        double ox = OFFX, oy = OFFY;
        if (twoPlayer) {
            ox = (W - (DUEL_COLS * pitchX - GAP)) / 2;
            oy = TOP + (H - TOP - (lv.rows.length * pitchY - GAP)) / 2;
        }
        for (int r = 0; r < lv.rows.length; r++) {
            for (int c = 0; c < lv.rows[r].length(); c++) {
                char ch = lv.rows[r].charAt(c);
                if (ch == '.') continue;
                Brick b = new Brick(c, r, ox + c * pitchX, oy + r * pitchY, ch);
                bricks.add(b);
                if (!b.indestructible()) remaining++;
                if (b.kind == Brick.Kind.GOLD) goldTotal++;
            }
        }
        goldRemaining = goldTotal;
        endedByGold = false;

        if (twoPlayer) {
            double mid = (TOP + H) / 2;
            paddles = new Paddle[] {
                new Paddle(0, true, LEFT_FACE, -1, DUEL_LEN, mid),
                new Paddle(1, true, RIGHT_FACE, +1, DUEL_LEN, mid) };
            server = 0;
            winner = -1;
            baseSpeed = 470;
        } else {
            paddles = new Paddle[] { new Paddle(0, false, PADDLE_Y, +1, SOLO_LEN, W / 2) };
            paddles[0].score = keep;
            baseSpeed = Math.min(MAX_SPEED - 150, 430 + index * 28);
        }
        ballSpeed = baseSpeed;
        slowTimer = 0;
        buttonCooldown = 0;
        spawnStuckBall(paddles[twoPlayer ? server : 0]);
        state = State.READY;
        stateTime = 0;
    }

    private void spawnStuckBall(Paddle p) {
        Ball b = new Ball();
        b.stuck = true;
        b.owner = p.index;
        b.stickOffset = 0;
        b.x = p.vertical ? p.face - p.toward * (BALL_R + 1) : p.pos;
        b.y = p.vertical ? p.pos : p.face - p.toward * (BALL_R + 1);
        balls.add(b);
    }

    /** Oriente la balle comme si elle quittait la raquette p avec cet angle (0 = droit devant). */
    private void setDir(Ball b, Paddle p, double angle) {
        double along = Math.sin(angle);
        double across = -p.toward * Math.cos(angle);
        if (p.vertical) {
            b.dx = across;
            b.dy = along;
            b.x = p.face - p.toward * BALL_R;
        } else {
            b.dx = along;
            b.dy = across;
            b.y = p.face - p.toward * BALL_R;
        }
    }

    /** Premier lancement (ou service) : angle legerement aleatoire. */
    private void launch() {
        for (Ball b : balls) {
            if (!b.stuck) continue;
            b.stuck = false;
            setDir(b, paddles[b.owner], (rnd.nextDouble() - 0.5) * 0.5);
        }
        play(Sfx.LAUNCH);
        state = State.PLAYING;
    }

    /** Relance les balles collees (only = joueur concerne, -1 = tous) : l'angle depend de ou elles se sont collees. */
    private void releaseStuck(int only) {
        boolean any = false;
        for (Ball b : balls) {
            if (!b.stuck || (only >= 0 && b.owner != only)) continue;
            Paddle p = paddles[b.owner];
            double rel = clamp(b.stickOffset / (p.len / 2), -1, 1);
            setDir(b, p, rel * 1.05);
            b.stuck = false;
            any = true;
        }
        if (any) play(Sfx.LAUNCH);
    }

    /** Solo : plus de balle. */
    private void loseLife() {
        lives--;
        paddles[0].combo = 0;
        shake = 14;
        powerUps.clear();
        paddles[0].resetEffects();
        slowTimer = 0;
        if (lives <= 0) {
            state = State.GAME_OVER;
            stateTime = 0;
            play(Sfx.GAME_OVER);
            finalizeScore();
        } else {
            play(Sfx.LIFE_LOST);
            ballSpeed = Math.max(baseSpeed, ballSpeed * 0.9);
            spawnStuckBall(paddles[0]);
            state = State.READY;
            stateTime = 0;
        }
    }

    /** Duel : la balle est sortie chez un joueur, il perd des points. */
    private void duelBallLost(Ball b) {
        Paddle p = paddles[b.lostSide];
        p.score = Math.max(0, p.score - DUEL_PENALTY);
        p.combo = 0;
        server = b.lostSide; // celui qui a rate sert
        shake = 10;
        spawn(b.lostSide == 0 ? 12 : W - 12, clamp(b.y, TOP, H), 18, 5, 260);
        play(Sfx.LIFE_LOST);
    }

    /** Duel : plus aucune balle en jeu, nouveau service. */
    private void newServe() {
        powerUps.clear();
        for (Paddle p : paddles) {
            p.resetEffects();
            p.combo = 0;
        }
        slowTimer = 0;
        ballSpeed = baseSpeed;
        spawnStuckBall(paddles[server]);
        state = State.READY;
        stateTime = 0;
    }

    /** Niveau fini : s'il y a de l'or, il suffit de l'avoir tout ramasse. */
    private boolean isCleared() {
        return goldTotal > 0 ? goldRemaining <= 0 : remaining <= 0;
    }

    private void levelClear() {
        endedByGold = goldTotal > 0;
        balls.clear();
        powerUps.clear();
        if (twoPlayer) {
            int a = paddles[0].score, b = paddles[1].score;
            winner = a > b ? 0 : (b > a ? 1 : -1);
            levelBonus = 0;
        } else {
            levelBonus = lives * 100;
            paddles[0].score += levelBonus;
        }
        for (int i = 0; i < 6; i++) {
            spawn(80 + rnd.nextDouble() * (W - 160), 120 + rnd.nextDouble() * 200, 25, rnd.nextDouble() * 360, 260);
        }
        stateTime = 0;
        play(Sfx.LEVEL_CLEAR);
        if (campaign && Levels.nextSolo(level) < 0) {
            state = State.WIN;
            finalizeScore();
        } else {
            state = State.LEVEL_CLEAR;
            if (!campaign) finalizeScore();
        }
    }

    private void finalizeScore() {
        if (twoPlayer) return; // pas de record en duel
        int score = paddles[0].score;
        if (campaign) {
            if (score > highScore) {
                highScore = score;
                newRecord = true;
                saveDirty = true;
            }
        } else if (score > levelBest[level]) {
            levelBest[level] = score;
            newRecord = true;
            saveDirty = true;
        }
    }

    private void play(Sfx s) {
        if (!muted && sfx != null) sfx.accept(s);
    }

    // ------------------------------------------------------------------ mise a jour

    void update(double dt) {
        time += dt;
        stateTime += dt;
        shake = Math.max(0, shake - dt * 30);
        buttonCooldown = Math.max(0, buttonCooldown - dt);
        updateParticles(dt);
        updateBeams(dt);
        for (Brick k : bricks) {
            if (k.flash > 0) k.flash -= dt;
        }

        if (state == State.READY || state == State.PLAYING) {
            for (Paddle p : paddles) movePaddle(p, dt);
            for (Ball b : balls) {
                if (b.stuck) follow(b);
            }
        }

        if (state == State.PLAYING) {
            slowTimer = Math.max(0, slowTimer - dt);
            for (Paddle p : paddles) tickTimers(p, dt);

            for (int i = balls.size() - 1; i >= 0; i--) {
                Ball b = balls.get(i);
                stepBall(b, dt);
                if (b.dead) {
                    if (twoPlayer) duelBallLost(b);
                    balls.remove(i);
                }
            }
            updatePowerUps(dt);

            if (isCleared()) levelClear();
            else if (balls.isEmpty()) {
                if (twoPlayer) newServe();
                else loseLife();
            }
        }
    }

    private void tickTimers(Paddle p, double dt) {
        p.wideTimer = Math.max(0, p.wideTimer - dt);
        p.shrinkTimer = Math.max(0, p.shrinkTimer - dt);
        p.fireTimer = Math.max(0, p.fireTimer - dt);
        p.wallTimer = Math.max(0, p.wallTimer - dt);
        p.nobreakTimer = Math.max(0, p.nobreakTimer - dt);
        if (p.stickyTimer > 0) {
            p.stickyTimer -= dt;
            if (p.stickyTimer <= 0) releaseStuck(p.index); // fin du bonus : les balles collees repartent
        }
    }

    private void follow(Ball b) {
        Paddle p = paddles[b.owner];
        double half = p.len / 2 - BALL_R * 0.5;
        b.stickOffset = clamp(b.stickOffset, -half, half);
        double along = p.pos + b.stickOffset;
        double across = p.face - p.toward * (BALL_R + 1);
        b.x = p.vertical ? across : along;
        b.y = p.vertical ? along : across;
        b.clearTrail();
    }

    private void movePaddle(Paddle p, double dt) {
        double old = p.pos;
        if (p.keyNeg != p.keyPos) {
            p.pos += (p.keyPos ? 1 : -1) * KEY_SPEED * dt;
        } else if (!twoPlayer && !Double.isNaN(mouseX)) {
            p.pos += (mouseX - p.pos) * Math.min(1, dt * 45);
        }
        p.updateTargetLen();
        p.len += (p.targetLen - p.len) * Math.min(1, dt * 10);
        double half = p.len / 2;
        p.pos = p.vertical ? clamp(p.pos, TOP + half, H - half) : clamp(p.pos, half, W - half);
        p.vel = dt > 0 ? (p.pos - old) / dt : 0;
    }

    private void stepBall(Ball b, double dt) {
        if (b.stuck) return;
        double speed = ballSpeed * (slowTimer > 0 ? 0.65 : 1.0);
        double dist = speed * dt;
        int n = Math.max(1, (int) Math.ceil(dist / 3.0)); // pas <= 3px : pas de tunnel
        double step = dist / n;
        for (int i = 0; i < n && !b.dead && !b.stuck; i++) {
            b.x += b.dx * step;
            b.y += b.dy * step;
            collideWalls(b);
            for (Paddle p : paddles) {
                if (hitPaddle(b, p)) break;
            }
            if (b.stuck) break;
            for (Paddle p : paddles) hitShield(b, p);
            checkLost(b);
            if (!b.dead) collideBricks(b);
        }
        b.pushTrail();
    }

    private void collideWalls(Ball b) {
        boolean bounced = false;
        if (!twoPlayer) {
            if (b.x - BALL_R < 0) {
                b.x = BALL_R;
                b.dx = Math.abs(b.dx);
                bounced = true;
            } else if (b.x + BALL_R > W) {
                b.x = W - BALL_R;
                b.dx = -Math.abs(b.dx);
                bounced = true;
            }
            if (b.y - BALL_R < TOP) {
                b.y = TOP + BALL_R;
                b.dy = Math.abs(b.dy);
                bounced = true;
            }
        } else { // duel : murs en haut et en bas, les cotes sont les camps des joueurs
            if (b.y - BALL_R < TOP) {
                b.y = TOP + BALL_R;
                b.dy = Math.abs(b.dy);
                bounced = true;
            } else if (b.y + BALL_R > H) {
                b.y = H - BALL_R;
                b.dy = -Math.abs(b.dy);
                bounced = true;
            }
        }
        if (bounced) play(Sfx.WALL);
    }

    private void checkLost(Ball b) {
        if (!twoPlayer) {
            if (b.y - BALL_R > H) b.dead = true;
        } else if (b.x + BALL_R < 0) {
            b.dead = true;
            b.lostSide = 0;
        } else if (b.x - BALL_R > W) {
            b.dead = true;
            b.lostSide = 1;
        }
    }

    /** Mur de secours derriere une raquette (bonus). */
    private void hitShield(Ball b, Paddle p) {
        if (p.wallTimer <= 0) return;
        double across = p.vertical ? b.x : b.y;
        double v = p.vertical ? b.dx : b.dy;
        double wf = p.wallFace();
        if (p.toward * v > 0 && p.toward * across + BALL_R >= p.toward * wf) {
            double pos = wf - p.toward * BALL_R;
            if (p.vertical) {
                b.x = pos;
                b.dx = -b.dx;
            } else {
                b.y = pos;
                b.dy = -b.dy;
            }
            fixDirection(b);
            play(Sfx.WALL);
        }
    }

    private boolean hitPaddle(Ball b, Paddle p) {
        double across = p.vertical ? b.x : b.y;
        double along = p.vertical ? b.y : b.x;
        double v = p.vertical ? b.dx : b.dy;
        if (p.toward * v <= 0) return false;                         // elle s'eloigne
        double t = p.toward * across, tf = p.toward * p.face;
        if (t + BALL_R < tf) return false;                           // pas encore au contact
        if (t > tf + Paddle.T * 0.6) return false;                   // deja passee derriere
        double half = p.len / 2;
        if (Math.abs(along - p.pos) > half + BALL_R * 0.5) return false;

        b.owner = p.index;
        p.combo = 0;
        if (p.stickyTimer > 0) { // balle collante : elle reste sur la raquette jusqu'a la relance
            b.stuck = true;
            b.stickOffset = along - p.pos;
            b.clearTrail();
            play(Sfx.STICKY);
            return true;
        }
        double rel = clamp((along - p.pos) / half, -1, 1);
        rel = clamp(rel + clamp(p.vel / 2500.0, -0.25, 0.25), -1, 1); // effet de la raquette
        setDir(b, p, rel * 1.05); // ~60 degres max
        ballSpeed = Math.min(MAX_SPEED, ballSpeed + 5);
        spawn(p.vertical ? p.face : b.x, p.vertical ? b.y : p.face, 6, 210, 140);
        play(Sfx.PADDLE);
        return true;
    }

    private void collideBricks(Ball b) {
        scorer = paddles[b.owner];
        boolean harmless = scorer.nobreakTimer > 0;
        boolean fire = scorer.fireTimer > 0 && !harmless;
        for (Brick k : bricks) {
            if (!k.isSolid()) continue;
            double cx = clamp(b.x, k.x, k.x + BW);
            double cy = clamp(b.y, k.y, k.y + BH);
            double ddx = b.x - cx, ddy = b.y - cy;
            double d2 = ddx * ddx + ddy * ddy;
            if (d2 >= BALL_R * BALL_R) continue;

            if (fire && !k.indestructible()) { // boule de feu : traverse
                hitBrick(k);
                continue;
            }

            double nx, ny;
            if (d2 > 1e-9) {
                double d = Math.sqrt(d2);
                nx = ddx / d;
                ny = ddy / d;
                b.x = cx + nx * (BALL_R + 0.01);
                b.y = cy + ny * (BALL_R + 0.01);
            } else { // centre dans la brique : on sort par le cote le moins profond
                double l = b.x - k.x, r = k.x + BW - b.x, t = b.y - k.y, bo = k.y + BH - b.y;
                double m = Math.min(Math.min(l, r), Math.min(t, bo));
                nx = 0;
                ny = 0;
                if (m == l) { nx = -1; b.x = k.x - BALL_R - 0.01; }
                else if (m == r) { nx = 1; b.x = k.x + BW + BALL_R + 0.01; }
                else if (m == t) { ny = -1; b.y = k.y - BALL_R - 0.01; }
                else { ny = 1; b.y = k.y + BH + BALL_R + 0.01; }
            }
            double dot = b.dx * nx + b.dy * ny;
            if (dot < 0) {
                b.dx -= 2 * dot * nx;
                b.dy -= 2 * dot * ny;
            }
            fixDirection(b);
            if (harmless && !k.indestructible()) { // malus : la balle rebondit sans rien casser
                spawn(k.x + BW / 2, k.y + BH / 2, 2, 0, 60);
                play(Sfx.WALL);
            } else {
                hitBrick(k);
            }
            return; // un seul rebond par micro-pas
        }
    }

    // ------------------------------------------------------------------ briques

    private void hitBrick(Brick k) {
        switch (k.kind) {
        case STEEL:
        case PHASE:
            spawn(k.x + BW / 2, k.y + BH / 2, 3, 210, 120);
            shake = Math.max(shake, 1.5);
            play(Sfx.STEEL);
            return;
        case BUTTON:
            if (buttonCooldown <= 0) pressButton(k);
            return;
        default:
            break;
        }
        k.hp--;
        scorer.combo++;
        ballSpeed = Math.min(MAX_SPEED, ballSpeed + 2);
        if (k.hp <= 0) {
            destroy(k);
        } else {
            scorer.score += 2 * multiplier(scorer.index);
            k.flash = 0.15;
            spawn(k.x + BW / 2, k.y + BH / 2, 5, k.hue(), 160);
            play(Sfx.BRICK_HIT);
        }
    }

    /** Bouton : fait apparaitre / disparaitre toutes les briques interrupteurs. */
    private void pressButton(Brick k) {
        buttonCooldown = 0.3;
        k.flash = 0.3;
        for (Brick o : bricks) {
            if (o.alive && o.kind == Brick.Kind.PHASE) {
                o.phaseSolid = !o.phaseSolid;
                o.flash = 0.3;
                spawn(o.x + BW / 2, o.y + BH / 2, 4, 185, 120);
            }
        }
        shake = Math.max(shake, 3);
        play(Sfx.BUTTON);
    }

    private void destroy(Brick k) {
        k.alive = false;
        remaining--;
        if (k.kind == Brick.Kind.GOLD) goldRemaining--;
        int points = k.kind == Brick.Kind.GOLD ? 150 : 10 * k.maxHp;
        scorer.score += points * multiplier(scorer.index);
        spawn(k.x + BW / 2, k.y + BH / 2, k.kind == Brick.Kind.GOLD ? 22 : 14, k.hue(), 240);
        play(k.kind == Brick.Kind.GOLD ? Sfx.GOLD : Sfx.BRICK_BREAK);

        switch (k.kind) {
        case EXPLOSIVE:
            explode(k);
            break;
        case LIGHTNING:
            lightning(k);
            break;
        case ROCK:
            rockslide(k);
            break;
        case BONUS:
            apply(k.bonus, scorer); // effet immediat, rien ne tombe
            break;
        case NORMAL:
            if (rnd.nextDouble() < 0.14) dropPowerUp(k);
            break;
        default:
            break;
        }
    }

    private void explode(Brick k) {
        shake = Math.max(shake, 10);
        play(Sfx.EXPLOSION);
        for (Brick o : bricks) {
            if (o.alive && !o.indestructible() && Math.abs(o.col - k.col) <= 1 && Math.abs(o.row - k.row) <= 1) {
                destroy(o);
            }
        }
    }

    /** Foudre : detruit toute la colonne (les briques indestructibles resistent). */
    private void lightning(Brick k) {
        double x = k.x + BW / 2;
        beams.add(new Beam(true, x, TOP, fieldBottom()));
        shake = Math.max(shake, 8);
        play(Sfx.LIGHTNING);
        for (double y = TOP + 20; y < fieldBottom(); y += 55) spawn(x, y, 3, 55, 170);
        for (Brick o : bricks) {
            if (o.alive && !o.indestructible() && o.col == k.col) destroy(o);
        }
    }

    /** Roche : detruit toute la ligne (les briques indestructibles resistent). */
    private void rockslide(Brick k) {
        double y = k.y + BH / 2;
        beams.add(new Beam(false, y, 0, W));
        shake = Math.max(shake, 8);
        play(Sfx.ROCK);
        for (double x = 40; x < W; x += 80) spawn(x, y, 3, 28, 170);
        for (Brick o : bricks) {
            if (o.alive && !o.indestructible() && o.row == k.row) destroy(o);
        }
    }

    private void dropPowerUp(Brick k) {
        PowerUp.Type[] all = PowerUp.Type.values();
        double total = 0;
        for (PowerUp.Type t : all) total += t.weight;
        double roll = rnd.nextDouble() * total;
        PowerUp.Type chosen = all[0];
        for (PowerUp.Type t : all) {
            roll -= t.weight;
            if (roll <= 0) {
                chosen = t;
                break;
            }
        }
        PowerUp u = new PowerUp(chosen, k.x + BW / 2, k.y + BH / 2);
        if (twoPlayer) u.vx = scorer.index == 0 ? -170 : 170; // glisse vers le camp de celui qui a casse la brique
        else u.vy = 170;
        powerUps.add(u);
    }

    // ------------------------------------------------------------------ bonus et malus

    private static boolean overlaps(Paddle p, PowerUp u) {
        double x0 = p.rectX(), y0 = p.rectY(), x1 = x0 + p.rectW(), y1 = y0 + p.rectH();
        return u.x + 16 >= x0 && u.x - 16 <= x1 && u.y + 10 >= y0 && u.y - 10 <= y1;
    }

    private void updatePowerUps(double dt) {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp u = powerUps.get(i);
            u.x += u.vx * dt;
            u.y += u.vy * dt;
            u.t += dt;
            Paddle catcher = null;
            for (Paddle p : paddles) {
                if (overlaps(p, u)) {
                    catcher = p;
                    break;
                }
            }
            if (catcher != null) {
                apply(u.type, catcher);
                powerUps.remove(i);
            } else if (u.y > H + 20 || u.x < -30 || u.x > W + 30) {
                powerUps.remove(i);
            }
        }
    }

    private void apply(PowerUp.Type type, Paddle p) {
        if (!type.bad) p.score += 50;
        spawn(p.vertical ? p.face : p.pos, p.vertical ? p.pos : p.face, 16, type.hue, 220);
        play(type.bad ? Sfx.MALUS : (type == PowerUp.Type.WALL ? Sfx.SHIELD : Sfx.POWERUP));
        switch (type) {
        case WIDE:
            p.wideTimer = 12;
            break;
        case SHRINK:
            p.shrinkTimer = 10;
            break;
        case MULTI:
            List<Ball> extra = new ArrayList<Ball>();
            for (Ball b : balls) {
                if (b.stuck) continue;
                for (int s = -1; s <= 1; s += 2) {
                    if (balls.size() + extra.size() >= 10) break;
                    Ball n = new Ball();
                    n.x = b.x;
                    n.y = b.y;
                    n.owner = b.owner;
                    double a = Math.atan2(b.dy, b.dx) + s * 0.4;
                    n.dx = Math.cos(a);
                    n.dy = Math.sin(a);
                    fixDirection(n);
                    extra.add(n);
                }
            }
            balls.addAll(extra);
            break;
        case SLOW:
            slowTimer = 8;
            break;
        case FIRE:
            p.fireTimer = 6;
            break;
        case STICKY:
            p.stickyTimer = 15;
            break;
        case WALL:
            p.wallTimer = 8;
            break;
        case NOBREAK:
            p.nobreakTimer = 5;
            break;
        case LIFE:
            if (twoPlayer) p.score += 150; // pas de vies en duel : la vie vaut des points
            else lives = Math.min(6, lives + 1);
            break;
        }
    }

    // ------------------------------------------------------------------ utilitaires

    /** Normalise la direction et evite les trajectoires qui s'eternisent le long d'un mur. */
    private void fixDirection(Ball b) {
        double len = Math.hypot(b.dx, b.dy);
        if (len < 1e-9) {
            b.dx = twoPlayer ? 1 : 0;
            b.dy = twoPlayer ? 0 : -1;
            return;
        }
        b.dx /= len;
        b.dy /= len;
        if (twoPlayer) { // duel : la balle doit toujours avancer d'un camp vers l'autre
            if (Math.abs(b.dx) < 0.22) {
                b.dx = (b.dx < 0 ? -1 : 1) * 0.22;
                b.dy = (b.dy < 0 ? -1 : 1) * Math.sqrt(1 - b.dx * b.dx);
            }
        } else if (Math.abs(b.dy) < 0.22) {
            b.dy = (b.dy < 0 ? -1 : 1) * 0.22;
            b.dx = (b.dx < 0 ? -1 : 1) * Math.sqrt(1 - b.dy * b.dy);
        }
    }

    private void spawn(double x, double y, int n, double hue, double speed) {
        for (int i = 0; i < n && particles.size() < 700; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double s = speed * (0.3 + rnd.nextDouble());
            Particle p = new Particle();
            p.x = x;
            p.y = y;
            p.vx = Math.cos(a) * s;
            p.vy = Math.sin(a) * s - 40;
            p.life = p.maxLife = 0.35 + rnd.nextDouble() * 0.5;
            p.size = 2 + rnd.nextDouble() * 3;
            p.hue = hue + rnd.nextDouble() * 30 - 15;
            particles.add(p);
        }
    }

    private void updateParticles(double dt) {
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= dt;
            if (p.life <= 0) {
                particles.remove(i);
                continue;
            }
            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.vy += 500 * dt;
        }
    }

    private void updateBeams(double dt) {
        for (int i = beams.size() - 1; i >= 0; i--) {
            Beam b = beams.get(i);
            b.life -= dt;
            if (b.life <= 0) beams.remove(i);
        }
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
