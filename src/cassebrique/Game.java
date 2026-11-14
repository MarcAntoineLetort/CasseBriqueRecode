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
    static final double GAP = 4;
    static final double SOLO_BW = 60, SOLO_BH = 24;   // briques horizontales (solo)
    static final double DUEL_BW = 24, DUEL_BH = 60;   // briques verticales (duel)
    static final double OFFX = 34, OFFY = 100;        // grille solo

    static final double PADDLE_Y = H - 46;                      // solo : face haute de la raquette
    static final double LEFT_FACE = 44, RIGHT_FACE = W - 44;    // duel : faces des raquettes
    static final double SOLO_LEN = 110, DUEL_LEN = 130;
    static final double BALL_R = 7;
    static final double MAX_SPEED = 820;
    static final double KEY_SPEED = 780;
    static final double MOVER_SPEED = 70;

    // Les points, le multiplicateur, les vies et les durees des effets sont dans Rules.java

    // Lignes du tableau des scores
    static final int CAT_BALL = 0, CAT_SPECIAL = 1, CAT_GOLD = 2, CAT_LASTGOLD = 3,
            CAT_BONUS = 4, CAT_BALLS_LEFT = 5, CAT_PENALTY = 6;
    static final String[] CAT_NAMES = {
        "Briques cass\u00e9es par la balle",
        "Briques sp\u00e9ciales et effets",
        "Briques d'or",
        "Derni\u00e8re brique d'or",
        "Bonus ramass\u00e9s",
        "Balles restantes",
        "Balle adverse perdue" };
    static final double RES_START = 0.9, RES_STEP = 0.8;

    // Mise en page du menu et de l'ecran d'informations (partagee avec le rendu)
    static final int CARD_COLS = 4, PER_PAGE = 12;
    static final double CARD_W = 210, CARD_H = 80, CARD_GAP = 15, CARD_X0 = 37.5, CARD_Y0 = 215, CARD_PITCH_Y = 92;
    static final double CAMP_X = 300, CAMP_Y = 126, CAMP_W = 360, CAMP_H = 50;
    static final double PAGE_Y = 482, PAGE_W = 44, PAGE_H = 28, PREV_X = 376, NEXT_X = 540;
    static final double INFO_X = 380, INFO_Y = 545, INFO_W = 200, INFO_H = 38;
    static final double NAV_Y = 596, BACK_X = 20, BACK_W = 130;
    static final int INFO_PAGES = 5;

    enum State { MENU, INFO, READY, PLAYING, PAUSED, LEVEL_CLEAR, RESULTS }

    State state = State.MENU;
    private State beforePause = State.PLAYING;

    Paddle[] paddles;
    final List<Ball> balls = new ArrayList<Ball>();
    final List<Brick> bricks = new ArrayList<Brick>();
    final List<PowerUp> powerUps = new ArrayList<PowerUp>();
    final List<Particle> particles = new ArrayList<Particle>();
    final List<Beam> beams = new ArrayList<Beam>();
    final List<Ghost> ghosts = new ArrayList<Ghost>();
    final List<Star> stars = new ArrayList<Star>();
    final List<double[]> deaths = new ArrayList<double[]>();    // briques detruites en pleine nuit : x, y, w, h, teinte, duree
    private final List<double[]> waypoints = new ArrayList<double[]>(); // balises des briques mobiles : x, y
    private final Random rnd = new Random();

    // Partie en cours
    boolean campaign;           // true : niveaux solo enchaines ; false : un seul niveau choisi
    boolean twoPlayer;          // niveau duel en cours
    int level, levelBonus;
    int winner = -1;            // duel : 0 blanc, 1 noir, -1 egalite
    int remaining;              // briques destructibles restantes
    int goldTotal, goldRemaining;
    boolean endedByGold;
    private Paddle scorer;      // joueur credite des briques cassees en ce moment

    // Ecran de resultats
    String resultTitle = "", resultNote = "";
    int resLines, resRevealed;
    boolean resTotalShown, resFinalShown, resElimShown;
    int eliminatedMask;         // duel : 1 = blanc n'a plus de balles, 2 = noir, 3 = les deux

    // Records / options (sauvegardes par Storage)
    int highScore;
    final int[] levelBest = new int[Levels.count()];
    boolean saveDirty, newRecord, muted;

    // Balle et effets globaux
    double baseSpeed, ballSpeed;
    double slowTimer, nightTimer, buttonCooldown;
    double shake, stateTime, time;

    // Menu et informations
    int menuPage, infoPage, hoverCard = -1;
    boolean hoverCampaign, hoverPrev, hoverNext, hoverInfoBtn, hoverBack;
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
        return Math.min(Rules.MAX_MULTIPLIER, 1 + paddles[player].combo / Rules.COMBO_STEP);
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
        switch (state) {
        case MENU:
            if (hoverCard >= 0) {
                play(Sfx.MENU_CLICK);
                startSingle(hoverCard);
            } else if (hoverCampaign) {
                play(Sfx.MENU_CLICK);
                startCampaign();
            } else if (hoverInfoBtn) {
                play(Sfx.MENU_CLICK);
                openInfo();
            } else if (hoverPrev) {
                changePage(-1);
            } else if (hoverNext) {
                changePage(1);
            }
            break;
        case INFO:
            if (hoverBack) closeInfo();
            else if (hoverPrev) changeInfoPage(-1);
            else if (hoverNext) changeInfoPage(1);
            break;
        default:
            action(0);
            break;
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
        case INFO:
            closeInfo();
            break;
        case READY:
            launch();
            break;
        case PLAYING: {
            int only = twoPlayer && who > 0 ? who - 1 : -1;
            if (!releaseStuck(only)) fireStars(only);
            break;
        }
        case PAUSED:
            togglePause();
            break;
        case LEVEL_CLEAR:
            if (stateTime > 0.4) loadLevel(Levels.nextSolo(level), true);
            break;
        case RESULTS:
            if (!resultsAllShown()) skipResults();
            else if (stateTime > resultsAllEnd() + 0.3) goMenu();
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

    /** Retour au menu depuis la pause, un niveau termine ou l'ecran de resultats. */
    void backToMenu() {
        if (state == State.PAUSED || state == State.LEVEL_CLEAR || state == State.RESULTS) goMenu();
    }

    private void goMenu() {
        if (state != State.RESULTS) finalizeScore();
        balls.clear();
        powerUps.clear();
        ghosts.clear();
        stars.clear();
        beams.clear();
        clearKeys();
        twoPlayer = false;
        state = State.MENU;
        stateTime = 0;
        refreshHover();
    }

    // ------------------------------------------------------------------ menu et informations

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

    private static boolean in(double x, double y, double rx, double ry, double rw, double rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    private void refreshHover() {
        boolean menu = state == State.MENU;
        boolean info = state == State.INFO;
        hoverCard = menu ? cardAt(mx, my) : -1;
        hoverCampaign = menu && in(mx, my, CAMP_X, CAMP_Y, CAMP_W, CAMP_H);
        hoverInfoBtn = menu && in(mx, my, INFO_X, INFO_Y, INFO_W, INFO_H);
        hoverBack = info && in(mx, my, BACK_X, NAV_Y, BACK_W, PAGE_H);
        if (menu) {
            hoverPrev = menuPage > 0 && in(mx, my, PREV_X, PAGE_Y, PAGE_W, PAGE_H);
            hoverNext = menuPage < pageCount() - 1 && in(mx, my, NEXT_X, PAGE_Y, PAGE_W, PAGE_H);
        } else if (info) {
            hoverPrev = infoPage > 0 && in(mx, my, PREV_X, NAV_Y, PAGE_W, PAGE_H);
            hoverNext = infoPage < INFO_PAGES - 1 && in(mx, my, NEXT_X, NAV_Y, PAGE_W, PAGE_H);
        } else {
            hoverPrev = false;
            hoverNext = false;
        }
    }

    void changePage(int delta) {
        int n = (int) clamp(menuPage + delta, 0, pageCount() - 1);
        if (n != menuPage) {
            menuPage = n;
            play(Sfx.MENU_CLICK);
            refreshHover();
        }
    }

    void openInfo() {
        state = State.INFO;
        infoPage = 0;
        stateTime = 0;
        refreshHover();
    }

    void closeInfo() {
        state = State.MENU;
        refreshHover();
    }

    void changeInfoPage(int delta) {
        int n = (int) clamp(infoPage + delta, 0, INFO_PAGES - 1);
        if (n != infoPage) {
            infoPage = n;
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
        newRecord = false;
        paddles = null; // nouvelle partie : points et balles remis a zero
        loadLevel(index, false);
    }

    /** keepRun : suite de la campagne (on garde la raquette, ses points et ses balles). */
    void loadLevel(int index, boolean keepRun) {
        Levels.Level lv = Levels.get(index);
        Paddle carried = (keepRun && paddles != null && !twoPlayer && !lv.twoPlayer) ? paddles[0] : null;
        level = index;
        twoPlayer = lv.twoPlayer;
        bricks.clear();
        powerUps.clear();
        balls.clear();
        particles.clear();
        beams.clear();
        ghosts.clear();
        stars.clear();
        deaths.clear();
        waypoints.clear();
        remaining = 0;
        goldTotal = 0;

        double bw = twoPlayer ? DUEL_BW : SOLO_BW, bh = twoPlayer ? DUEL_BH : SOLO_BH;
        double pitchX = bw + GAP, pitchY = bh + GAP;
        int cols = 0;
        for (String row : lv.rows) cols = Math.max(cols, row.length());
        double ox = OFFX, oy = OFFY;
        if (twoPlayer) { // briques verticales, centrees : 9 lignes remplissent tout le terrain
            ox = (W - (cols * pitchX - GAP)) / 2;
            oy = TOP + (H - TOP - (lv.rows.length * pitchY - GAP)) / 2;
        }
        for (int r = 0; r < lv.rows.length; r++) {
            for (int c = 0; c < lv.rows[r].length(); c++) {
                char ch = lv.rows[r].charAt(c);
                if (ch == '.') continue;
                double bx = ox + c * pitchX, by = oy + r * pitchY;
                if (ch == 'o') { // balise invisible d'une brique mobile
                    waypoints.add(new double[] { bx, by });
                    continue;
                }
                Brick b = new Brick(c, r, bx, by, bw, bh, ch);
                if (b.kind == Brick.Kind.MOVER) { // sa case de depart est aussi un point de passage
                    waypoints.add(new double[] { bx, by });
                    b.wpCur = waypoints.size() - 1;
                }
                bricks.add(b);
                if (!b.indestructible()) remaining++;
                if (b.kind == Brick.Kind.GOLD) goldTotal++;
            }
        }
        for (Brick b : bricks) {
            if (b.kind == Brick.Kind.MOVER) b.wpTo = pickNext(b);
        }
        goldRemaining = goldTotal;
        endedByGold = false;
        eliminatedMask = 0;

        if (twoPlayer) {
            double mid = (TOP + H) / 2;
            paddles = new Paddle[] {
                new Paddle(0, true, LEFT_FACE, -1, DUEL_LEN, mid),
                new Paddle(1, true, RIGHT_FACE, +1, DUEL_LEN, mid) };
            winner = -1;
            baseSpeed = 470;
        } else {
            if (carried != null) {
                carried.resetForLevel(W / 2);
                paddles = new Paddle[] { carried };
            } else {
                paddles = new Paddle[] { new Paddle(0, false, PADDLE_Y, +1, SOLO_LEN, W / 2) };
            }
            baseSpeed = Math.min(MAX_SPEED - 150, 430 + index * 24);
        }
        ballSpeed = baseSpeed;
        slowTimer = 0;
        nightTimer = 0;
        buttonCooldown = 0;

        if (twoPlayer) { // les deux joueurs ont leur balle, decompte de 3 secondes avant de pouvoir la lancer
            for (Paddle p : paddles) {
                spawnStuckBall(p);
                p.serveTimer = Rules.SERVE_DELAY;
                p.serveSecond = (int) Math.ceil(Rules.SERVE_DELAY);
            }
            play(Sfx.COUNTDOWN);
            state = State.PLAYING;
        } else {
            spawnStuckBall(paddles[0]);
            state = State.READY;
        }
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

    private boolean hasBall(Paddle p) {
        for (Ball b : balls) {
            if (b.owner == p.index) return true;
        }
        return false;
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

    /** Solo : premier lancement ou apres une balle perdue, angle legerement aleatoire. */
    private void launch() {
        for (Ball b : balls) {
            if (!b.stuck) continue;
            b.stuck = false;
            setDir(b, paddles[b.owner], (rnd.nextDouble() - 0.5) * 0.5);
        }
        play(Sfx.LAUNCH);
        state = State.PLAYING;
    }

    /** Duel : a la fin du decompte, la balle du joueur part vers l'adversaire. */
    private void autoLaunch(Paddle p) {
        for (Ball b : balls) {
            if (!b.stuck || b.owner != p.index) continue;
            b.stuck = false;
            setDir(b, p, (rnd.nextDouble() - 0.5) * 0.5);
        }
    }

    /** Relance les balles collees ou en attente de service (only = joueur concerne, -1 = tous). */
    private boolean releaseStuck(int only) {
        boolean any = false;
        for (Ball b : balls) {
            if (!b.stuck || (only >= 0 && b.owner != only)) continue;
            Paddle p = paddles[b.owner];
            if (p.serveTimer > 0) continue; // decompte pas fini
            double rel = clamp(b.stickOffset / (p.len / 2), -1, 1);
            setDir(b, p, rel * 1.05 + (rel == 0 ? (rnd.nextDouble() - 0.5) * 0.4 : 0));
            b.stuck = false;
            any = true;
        }
        if (any) play(Sfx.LAUNCH);
        return any;
    }

    /** Solo : plus aucune balle. */
    private void loseLife() {
        Paddle p = paddles[0];
        p.lives--;
        p.combo = 0;
        shake = 14;
        powerUps.clear();
        ghosts.clear();
        stars.clear();
        p.resetEffects();
        slowTimer = 0;
        nightTimer = 0;
        if (p.lives <= 0) {
            play(Sfx.GAME_OVER);
            endRun("PARTIE TERMIN\u00c9E", "");
        } else {
            play(Sfx.LIFE_LOST);
            ballSpeed = Math.max(baseSpeed, ballSpeed * 0.9);
            spawnStuckBall(p);
            state = State.READY;
            stateTime = 0;
        }
    }

    /** Duel : une balle est sortie du terrain par un cote. */
    private void duelLoss(Ball b) {
        Paddle owner = paddles[b.owner];
        double ey = clamp(b.y, TOP, H);
        spawn(b.lostSide == 0 ? 12 : W - 12, ey, 18, 5, 260);
        if (b.lostSide != b.owner) { // sortie chez l'adversaire : c'est lui qui perd des points
            Paddle victim = paddles[b.lostSide];
            victim.award(CAT_PENALTY, -Rules.DUEL_PENALTY);
            victim.combo = 0;
            shake = Math.max(shake, 8);
            play(Sfx.PENALTY);
        } else { // sortie de son propre cote : on perd une vie, sauf s'il reste d'autres balles
            owner.combo = 0;
            shake = Math.max(shake, 10);
            if (!hasBall(owner)) {
                owner.lives--;
                play(Sfx.LIFE_LOST);
            }
        }
    }

    private void checkDuelEnd() {
        boolean e0 = paddles[0].lives <= 0, e1 = paddles[1].lives <= 0;
        if (!e0 && !e1) return;
        if (e0 && !e1) winner = 1;
        else if (e1 && !e0) winner = 0;
        else winner = paddles[0].score > paddles[1].score ? 0 : (paddles[1].score > paddles[0].score ? 1 : -1);
        eliminatedMask = (e0 ? 1 : 0) | (e1 ? 2 : 0);
        for (Paddle p : paddles) p.award(CAT_BALLS_LEFT, p.lives * Rules.BALL_BONUS);
        play(Sfx.GAME_OVER);
        endRun("R\u00c9SULTATS", ""); // le motif de la defaite n'est revele qu'a la fin du tableau
    }

    /** Niveau fini : s'il y a de l'or, il suffit de l'avoir tout ramasse. */
    private boolean isCleared() {
        return goldTotal > 0 ? goldRemaining <= 0 : remaining <= 0;
    }

    private void levelClear() {
        endedByGold = goldTotal > 0;
        balls.clear();
        powerUps.clear();
        ghosts.clear();
        stars.clear();
        for (Paddle p : paddles) p.award(CAT_BALLS_LEFT, p.lives * Rules.BALL_BONUS);
        play(Sfx.LEVEL_CLEAR);
        if (twoPlayer) {
            int a = paddles[0].score, b = paddles[1].score;
            winner = a > b ? 0 : (b > a ? 1 : -1);
            endRun("R\u00c9SULTATS", endedByGold ? "Tout l'or est ramass\u00e9" : "Toutes les briques sont cass\u00e9es");
        } else {
            levelBonus = paddles[0].lives * Rules.BALL_BONUS;
            if (campaign && Levels.nextSolo(level) >= 0) {
                for (int i = 0; i < 6; i++) {
                    spawn(80 + rnd.nextDouble() * (W - 160), 120 + rnd.nextDouble() * 200, 25, rnd.nextDouble() * 360, 260);
                }
                state = State.LEVEL_CLEAR;
                stateTime = 0;
            } else if (campaign) {
                endRun("VICTOIRE !", "");
            } else {
                endRun(endedByGold ? "TOUT L'OR EST RAMASS\u00c9 !" : "NIVEAU TERMIN\u00c9 !", "");
            }
        }
    }

    // ------------------------------------------------------------------ fin de partie et tableau des scores

    private void endRun(String title, String note) {
        resultTitle = title;
        resultNote = note;
        state = State.RESULTS;
        stateTime = 0;
        resRevealed = 0;
        resTotalShown = false;
        resFinalShown = false;
        resElimShown = false;
        resLines = twoPlayer ? Paddle.CATS : Paddle.CATS - 1;
        balls.clear();
        powerUps.clear();
        ghosts.clear();
        stars.clear();
        nightTimer = 0;
        finalizeScore();
    }

    double resultsTotalTime() {
        return RES_START + resLines * RES_STEP + 0.4;
    }

    /** Debut et fin de la banniere rouge "n'a plus de balles" (duel) : juste apres le total. */
    double resultsElimStart() {
        return resultsTotalTime() + Rules.ELIM_BANNER_DELAY;
    }

    double resultsElimEnd() {
        return resultsElimStart() + Rules.ELIM_BANNER_TIME;
    }

    boolean elimBannerActive() {
        return eliminatedMask != 0 && stateTime >= resultsElimStart() && stateTime < resultsElimEnd();
    }

    /** Affichage du gagnant : juste apres le total, ou apres la disparition de la banniere rouge. */
    double resultsFinalTime() {
        return eliminatedMask != 0 ? resultsElimEnd() + 0.4 : resultsTotalTime() + 1.4;
    }

    double resultsAllEnd() {
        return resultsFinalTime();
    }

    boolean resultsAllShown() {
        return resFinalShown;
    }

    private void skipResults() {
        stateTime = resultsAllEnd() + 0.01;
    }

    private String eliminationNote() {
        if (eliminatedMask == 1) return "Blanc n'a plus de balles";
        if (eliminatedMask == 2) return "Noir n'a plus de balles";
        return "Plus de balles pour les deux joueurs";
    }

    private void tickResults() {
        int should = (int) Math.floor((stateTime - RES_START) / RES_STEP) + 1;
        should = Math.max(0, Math.min(resLines, should));
        while (resRevealed < should) {
            resRevealed++;
            play(Sfx.TALLY);
            spawn(W / 2, 120 + resRevealed * 42, 6, 50, 160);
        }
        if (!resTotalShown && stateTime >= resultsTotalTime()) {
            resTotalShown = true;
            play(Sfx.TOTAL);
            for (int i = 0; i < 3; i++) spawn(W / 2 + (i - 1) * 160, 450, 14, 48, 280);
        }
        // 1) duel : la banniere rouge arrive juste apres le total
        if (!resElimShown && eliminatedMask != 0 && stateTime >= resultsElimStart()) {
            resElimShown = true;
            play(Sfx.ELIMINATION);
            shake = 22;
            spawn(W / 2, 340, 70, 0, 460);
            spawn(W / 2, 340, 40, 20, 380);
        }
        // 2) puis le gagnant (apres la disparition de la banniere) et, en haut, "X n'a plus de balles"
        if (!resFinalShown && stateTime >= resultsFinalTime()) {
            resFinalShown = true;
            if (eliminatedMask != 0) resultNote = eliminationNote();
            boolean celebrate = twoPlayer ? winner >= 0 : newRecord;
            if (celebrate) {
                play(Sfx.RECORD);
                confetti(60);
            }
        }
    }

    private void confetti(int n) {
        for (int i = 0; i < n; i++) spawn(rnd.nextDouble() * W, 90 + rnd.nextDouble() * 60, 1, rnd.nextDouble() * 360, 320);
    }

    private void finalizeScore() {
        if (twoPlayer || paddles == null) return; // pas de record en duel
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
        updateDeaths(dt);
        for (Brick k : bricks) {
            if (k.flash > 0) k.flash -= dt;
            if (k.frost > 0) k.frost -= dt;
        }

        if (state == State.READY || state == State.PLAYING) {
            for (Paddle p : paddles) movePaddle(p, dt);
            for (Ball b : balls) {
                if (b.stuck) follow(b);
            }
            updateMovers(dt);
        }

        if (state == State.PLAYING) {
            slowTimer = Math.max(0, slowTimer - dt);
            nightTimer = Math.max(0, nightTimer - dt);
            for (Paddle p : paddles) tickTimers(p, dt);

            for (int i = balls.size() - 1; i >= 0; i--) {
                Ball b = balls.get(i);
                stepBall(b, dt);
                if (b.dead) {
                    balls.remove(i);
                    if (twoPlayer) duelLoss(b);
                }
            }
            updateGhosts(dt);
            updateStars(dt);
            updatePowerUps(dt);

            if (isCleared()) {
                levelClear();
            } else if (twoPlayer) {
                checkDuelEnd();
                if (state == State.PLAYING) {
                    for (Paddle p : paddles) {
                        if (!hasBall(p) && p.lives > 0) { // nouvelle balle pour celui qui n'en a plus
                            spawnStuckBall(p);
                            p.serveTimer = Rules.RESPAWN_DELAY;
                            p.serveSecond = (int) Math.ceil(Rules.RESPAWN_DELAY);
                        }
                    }
                }
            } else if (balls.isEmpty()) {
                loseLife();
            }
        }

        if (state == State.RESULTS) tickResults();
    }

    private void tickTimers(Paddle p, double dt) {
        p.wideTimer = Math.max(0, p.wideTimer - dt);
        p.shrinkTimer = Math.max(0, p.shrinkTimer - dt);
        p.fireTimer = Math.max(0, p.fireTimer - dt);
        p.wallTimer = Math.max(0, p.wallTimer - dt);
        p.nobreakTimer = Math.max(0, p.nobreakTimer - dt);
        p.starTimer = Math.max(0, p.starTimer - dt);
        if (p.stickyTimer > 0) {
            p.stickyTimer -= dt;
            if (p.stickyTimer <= 0) releaseStuck(p.index); // fin du bonus : les balles collees repartent
        }
        if (p.ghostTimer > 0) { // un fantome par seconde pendant 5 secondes
            p.ghostNext -= dt;
            if (p.ghostNext <= 0 && p.ghostTimer > 0.5) {
                emitGhost(p);
                p.ghostNext += 1.0;
            }
            p.ghostTimer -= dt;
        }
        if (p.serveTimer > 0) {
            p.serveTimer = Math.max(0, p.serveTimer - dt);
            int sec = (int) Math.ceil(p.serveTimer);
            if (p.serveTimer <= 0) {
                play(Sfx.GO);
                autoLaunch(p); // la balle part toute seule : impossible de la garder pour les points
            } else if (sec != p.serveSecond) {
                play(Sfx.COUNTDOWN);
            }
            p.serveSecond = sec;
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

    // ------------------------------------------------------------------ balle

    private void stepBall(Ball b, double dt) {
        if (b.stuck) return;
        if (b.tpCooldown > 0) b.tpCooldown -= dt;
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
            if (b.dead) break;
            collideBricks(b);
            checkTeleport(b);
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

        boolean mine = b.owner == p.index; // chaque joueur a sa balle ; l'adversaire peut aussi la renvoyer
        if (mine) p.combo = 0;
        if (p.stickyTimer > 0 && mine) { // balle collante : elle reste sur la raquette jusqu'a la relance
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

    /** Teleporteur : quand la balle y est entierement, elle ressort d'un autre en gardant sa vitesse. */
    private void checkTeleport(Ball b) {
        if (b.tpCooldown > 0) return;
        for (Brick t : bricks) {
            if (t.kind != Brick.Kind.TELEPORT || !t.alive) continue;
            double cx = t.x + t.w / 2, cy = t.y + t.h / 2;
            if (Math.abs(b.x - cx) <= t.w / 2 - BALL_R && Math.abs(b.y - cy) <= t.h / 2 - BALL_R) {
                List<Brick> others = new ArrayList<Brick>();
                for (Brick o : bricks) {
                    if (o != t && o.kind == Brick.Kind.TELEPORT && o.alive) others.add(o);
                }
                if (others.isEmpty()) return;
                Brick dest = others.get(rnd.nextInt(others.size()));
                spawn(cx, cy, 12, 285, 200);
                b.x = dest.x + dest.w / 2;
                b.y = dest.y + dest.h / 2;
                b.tpCooldown = 0.4;
                b.clearTrail();
                spawn(b.x, b.y, 12, 285, 200);
                play(Sfx.TELEPORT);
                return;
            }
        }
    }

    private void collideBricks(Ball b) {
        scorer = paddles[b.owner];
        boolean harmless = scorer.nobreakTimer > 0;
        boolean fire = scorer.fireTimer > 0 && !harmless;
        for (Brick k : bricks) {
            if (!k.isSolid()) continue;
            double cx = clamp(b.x, k.x, k.x + k.w);
            double cy = clamp(b.y, k.y, k.y + k.h);
            double ddx = b.x - cx, ddy = b.y - cy;
            double d2 = ddx * ddx + ddy * ddy;
            if (d2 >= BALL_R * BALL_R) continue;

            if (fire && !k.unbreakable()) { // boule de feu : traverse
                hitBrick(k, true);
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
                double l = b.x - k.x, r = k.x + k.w - b.x, t = b.y - k.y, bo = k.y + k.h - b.y;
                double m = Math.min(Math.min(l, r), Math.min(t, bo));
                nx = 0;
                ny = 0;
                if (m == l) { nx = -1; b.x = k.x - BALL_R - 0.01; }
                else if (m == r) { nx = 1; b.x = k.x + k.w + BALL_R + 0.01; }
                else if (m == t) { ny = -1; b.y = k.y - BALL_R - 0.01; }
                else { ny = 1; b.y = k.y + k.h + BALL_R + 0.01; }
            }
            double dot = b.dx * nx + b.dy * ny;
            if (dot < 0) {
                b.dx -= 2 * dot * nx;
                b.dy -= 2 * dot * ny;
            }
            fixDirection(b);
            if (harmless && !k.unbreakable()) { // malus : la balle rebondit sans rien casser
                spawn(k.x + k.w / 2, k.y + k.h / 2, 2, 0, 60);
                play(Sfx.WALL);
            } else {
                hitBrick(k, true);
            }
            return; // un seul rebond par micro-pas
        }
    }

    // ------------------------------------------------------------------ briques

    /** direct : touchee par une balle (sinon par un fantome ou un effet). */
    private void hitBrick(Brick k, boolean direct) {
        if (k.frost > 0) { // brique givree : incassable pour le moment
            spawn(k.x + k.w / 2, k.y + k.h / 2, 3, 195, 120);
            play(Sfx.STEEL);
            return;
        }
        switch (k.kind) {
        case STEEL:
        case PHASE:
        case MOVER:
            spawn(k.x + k.w / 2, k.y + k.h / 2, 3, 210, 120);
            shake = Math.max(shake, 1.5);
            play(Sfx.STEEL);
            return;
        case BUTTON:
            if (direct && buttonCooldown <= 0) pressButton(k);
            return;
        default:
            break;
        }
        k.hp--;
        if (direct) {
            scorer.combo++;
            ballSpeed = Math.min(MAX_SPEED, ballSpeed + 2);
        }
        if (k.hp <= 0) {
            destroy(k, direct);
        } else {
            scorer.award(direct && k.kind == Brick.Kind.NORMAL ? CAT_BALL : CAT_SPECIAL, Rules.DAMAGE_POINTS * multiplier(scorer.index));
            k.flash = 0.15;
            spawn(k.x + k.w / 2, k.y + k.h / 2, 5, k.hue(), 160);
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
                spawn(o.x + o.w / 2, o.y + o.h / 2, 4, 185, 120);
            }
        }
        shake = Math.max(shake, 3);
        play(Sfx.BUTTON);
    }

    private void destroy(Brick k, boolean direct) {
        k.alive = false;
        remaining--;
        double cx = k.x + k.w / 2, cy = k.y + k.h / 2;
        if (nightTimer > 0) deaths.add(new double[] { k.x, k.y, k.w, k.h, k.hue(), 0.5 });

        int cat, points;
        if (k.kind == Brick.Kind.GOLD) {
            cat = CAT_GOLD;
            points = Rules.GOLD_POINTS;
        } else {
            cat = (direct && k.kind == Brick.Kind.NORMAL) ? CAT_BALL : CAT_SPECIAL;
            points = Rules.BRICK_POINTS * k.maxHp;
        }
        scorer.award(cat, points * multiplier(scorer.index));
        spawn(cx, cy, k.kind == Brick.Kind.GOLD ? 22 : 14, k.hue(), 240);
        play(k.kind == Brick.Kind.GOLD ? Sfx.GOLD : Sfx.BRICK_BREAK);

        if (k.kind == Brick.Kind.GOLD) {
            goldRemaining--;
            if (goldRemaining == 0 && goldTotal > 0) { // derniere brique d'or : gros bonus
                scorer.award(CAT_LASTGOLD, Rules.lastGold(twoPlayer));
                spawn(cx, cy, 40, 48, 360);
                shake = Math.max(shake, 6);
                play(Sfx.TOTAL);
            }
        }

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
        case FROST:
            freezeAround(k);
            break;
        case TIME:
            rewind();
            break;
        case NIGHT:
            nightTimer = Rules.NIGHT_TIME;
            play(Sfx.NIGHT);
            break;
        case BONUS:
            apply(k.bonus, scorer); // effet immediat, rien ne tombe
            break;
        case NORMAL:
            if (rnd.nextDouble() < Rules.DROP_CHANCE) dropPowerUp(k);
            break;
        default:
            break;
        }
    }

    private void explode(Brick k) {
        shake = Math.max(shake, 10);
        play(Sfx.EXPLOSION);
        for (Brick o : bricks) {
            if (o.alive && !o.unbreakable() && Math.abs(o.col - k.col) <= 1 && Math.abs(o.row - k.row) <= 1) {
                destroy(o, false);
            }
        }
    }

    /** Foudre : detruit toute la colonne (les briques incassables resistent). */
    private void lightning(Brick k) {
        double x = k.x + k.w / 2;
        beams.add(new Beam(true, x, TOP, fieldBottom(), 0));
        shake = Math.max(shake, 8);
        play(Sfx.LIGHTNING);
        for (double y = TOP + 20; y < fieldBottom(); y += 55) spawn(x, y, 3, 55, 170);
        for (Brick o : bricks) {
            if (o.alive && !o.unbreakable() && o.col == k.col) destroy(o, false);
        }
    }

    /** Roche : detruit toute la ligne (les briques incassables resistent). */
    private void rockslide(Brick k) {
        double y = k.y + k.h / 2;
        beams.add(new Beam(false, y, 0, W, k.h));
        shake = Math.max(shake, 8);
        play(Sfx.ROCK);
        for (double x = 40; x < W; x += 80) spawn(x, y, 3, 28, 170);
        for (Brick o : bricks) {
            if (o.alive && !o.unbreakable() && o.row == k.row) destroy(o, false);
        }
    }

    /** Givre : les 8 briques autour deviennent incassables pendant 10 secondes. */
    private void freezeAround(Brick k) {
        play(Sfx.FROST);
        shake = Math.max(shake, 3);
        for (Brick o : bricks) {
            if (o != k && o.alive && !o.indestructible() && Math.abs(o.col - k.col) <= 1 && Math.abs(o.row - k.row) <= 1) {
                o.frost = Rules.FROST_TIME;
                o.flash = 0.3;
                spawn(o.x + o.w / 2, o.y + o.h / 2, 6, 195, 140);
            }
        }
    }

    /** Temps : toutes les briques detruites reapparaissent, sauf l'or et les briques du temps. */
    private void rewind() {
        play(Sfx.TIMEWARP);
        shake = Math.max(shake, 5);
        for (Brick o : bricks) {
            if (!o.alive && o.kind != Brick.Kind.GOLD && o.kind != Brick.Kind.TIME) {
                o.alive = true;
                o.hp = o.maxHp;
                o.frost = 0;
                o.flash = 0.5;
                if (!o.indestructible()) remaining++;
                spawn(o.x + o.w / 2, o.y + o.h / 2, 5, 265, 160);
            }
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
        PowerUp u = new PowerUp(chosen, k.x + k.w / 2, k.y + k.h / 2);
        if (twoPlayer) u.vx = scorer.index == 0 ? -170 : 170; // glisse vers le camp du proprietaire de la balle
        else u.vy = 170;
        powerUps.add(u);
    }

    // ------------------------------------------------------------------ briques mobiles

    /** Prochain point de passage : le plus proche parmi les balises alignees, sans faire demi-tour si possible. */
    private int pickNext(Brick m) {
        if (m.wpCur < 0) return -1;
        double[] c = waypoints.get(m.wpCur);
        int best = -1, back = -1;
        double bd = 1e18, bkd = 1e18;
        for (int j = 0; j < waypoints.size(); j++) {
            if (j == m.wpCur) continue;
            double[] w = waypoints.get(j);
            boolean aligned = Math.abs(w[0] - c[0]) < 0.5 || Math.abs(w[1] - c[1]) < 0.5;
            if (!aligned) continue;
            double d = Math.abs(w[0] - c[0]) + Math.abs(w[1] - c[1]);
            if (j == m.wpPrev) {
                if (d < bkd) {
                    bkd = d;
                    back = j;
                }
            } else if (d < bd) {
                bd = d;
                best = j;
            }
        }
        return best >= 0 ? best : back;
    }

    private void updateMovers(double dt) {
        for (Brick m : bricks) {
            if (m.kind == Brick.Kind.MOVER && m.alive) moveBrick(m, dt);
        }
    }

    private void moveBrick(Brick m, double dt) {
        if (m.pause > 0) {
            m.pause -= dt;
            return;
        }
        if (m.wpTo < 0) return;
        double[] t = waypoints.get(m.wpTo);
        double dx = t[0] - m.x, dy = t[1] - m.y;
        double dist = Math.abs(dx) + Math.abs(dy);
        double step = Math.min(MOVER_SPEED * dt, dist);
        double nx = m.x, ny = m.y;
        if (Math.abs(dx) > Math.abs(dy)) nx += Math.signum(dx) * step;
        else ny += Math.signum(dy) * step;
        if (!tryMove(m, nx, ny)) { // bloquee (balle ecrasee, brique en travers) : demi-tour
            int old = m.wpTo;
            m.wpTo = m.wpCur;
            m.wpCur = old;
            m.wpPrev = -1;
            m.pause = 0.25;
            return;
        }
        if (step >= dist - 1e-9) { // arrivee au point de passage
            m.x = t[0];
            m.y = t[1];
            m.wpPrev = m.wpCur;
            m.wpCur = m.wpTo;
            m.wpTo = pickNext(m);
        }
    }

    /** Deplace la brique mobile ; refuse si elle heurte une brique ou si elle ecraserait une balle. */
    private boolean tryMove(Brick m, double nx, double ny) {
        double ox = m.x, oy = m.y;
        m.x = nx;
        m.y = ny;
        for (Brick o : bricks) {
            if (o == m || !o.isSolid()) continue;
            if (m.x < o.x + o.w && m.x + m.w > o.x && m.y < o.y + o.h && m.y + m.h > o.y) {
                m.x = ox;
                m.y = oy;
                return false;
            }
        }
        for (Ball b : balls) {
            if (b.stuck) continue;
            double cx = clamp(b.x, m.x, m.x + m.w), cy = clamp(b.y, m.y, m.y + m.h);
            double ddx = b.x - cx, ddy = b.y - cy;
            if (ddx * ddx + ddy * ddy >= BALL_R * BALL_R) continue;
            if (!pushBallOut(b, m)) { // pas la force de ecraser la balle
                m.x = ox;
                m.y = oy;
                return false;
            }
        }
        return true;
    }

    /** Chasse la balle hors de la brique mobile ; false si elle n'a nulle part ou aller (ecrasee). */
    private boolean pushBallOut(Ball b, Brick m) {
        double cx = clamp(b.x, m.x, m.x + m.w), cy = clamp(b.y, m.y, m.y + m.h);
        double ddx = b.x - cx, ddy = b.y - cy;
        double d2 = ddx * ddx + ddy * ddy;
        double nx, ny, px, py;
        if (d2 > 1e-9) {
            double d = Math.sqrt(d2);
            nx = ddx / d;
            ny = ddy / d;
            px = cx + nx * (BALL_R + 0.05);
            py = cy + ny * (BALL_R + 0.05);
        } else {
            double l = b.x - m.x, r = m.x + m.w - b.x, t = b.y - m.y, bo = m.y + m.h - b.y;
            double mn = Math.min(Math.min(l, r), Math.min(t, bo));
            if (mn == l) { nx = -1; ny = 0; px = m.x - BALL_R - 0.05; py = b.y; }
            else if (mn == r) { nx = 1; ny = 0; px = m.x + m.w + BALL_R + 0.05; py = b.y; }
            else if (mn == t) { nx = 0; ny = -1; px = b.x; py = m.y - BALL_R - 0.05; }
            else { nx = 0; ny = 1; px = b.x; py = m.y + m.h + BALL_R + 0.05; }
        }
        if (!freeSpot(px, py, m)) return false;
        b.x = px;
        b.y = py;
        double dot = b.dx * nx + b.dy * ny;
        if (dot < 0) {
            b.dx -= 2 * dot * nx;
            b.dy -= 2 * dot * ny;
            fixDirection(b);
        }
        return true;
    }

    /** La balle peut-elle se trouver ici : dans le terrain et sans toucher une autre brique ? */
    private boolean freeSpot(double px, double py, Brick ignore) {
        if (py < TOP + BALL_R) return false;
        if (twoPlayer) {
            if (py > H - BALL_R) return false;
        } else if (px < BALL_R || px > W - BALL_R) {
            return false;
        }
        for (Brick o : bricks) {
            if (o == ignore || !o.isSolid()) continue;
            double cx = clamp(px, o.x, o.x + o.w), cy = clamp(py, o.y, o.y + o.h);
            double ddx = px - cx, ddy = py - cy;
            if (ddx * ddx + ddy * ddy < BALL_R * BALL_R) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ fantomes et etoiles filantes

    private void emitGhost(Paddle p) {
        double speed = 150;
        double across = p.face - p.toward * 12;
        Ghost g;
        if (p.vertical) g = new Ghost(p.index, across, p.pos, -p.toward * speed, 0);
        else g = new Ghost(p.index, p.pos, across, 0, -p.toward * speed);
        ghosts.add(g);
        play(Sfx.GHOST);
    }

    private void updateGhosts(double dt) {
        for (int i = ghosts.size() - 1; i >= 0; i--) {
            Ghost g = ghosts.get(i);
            g.x += g.vx * dt;
            g.y += g.vy * dt;
            g.t += dt;
            if (g.x < -20 || g.x > W + 20 || g.y < TOP - 20 || g.y > H + 20) {
                ghosts.remove(i);
                continue;
            }
            Brick hit = firstBrickHit(g.x, g.y, 8);
            if (hit != null) {
                if (!hit.unbreakable()) { // casse la brique comme une balle, puis disparait
                    scorer = paddles[g.owner];
                    hitBrick(hit, false);
                } else {
                    spawn(g.x, g.y, 4, 150, 120);
                }
                spawn(g.x, g.y, 6, 150, 140);
                ghosts.remove(i);
            }
        }
    }

    private void fireStars(int only) {
        for (Paddle p : paddles) {
            if (p.starTimer <= 0 || (only >= 0 && p.index != only) || p.serveTimer > 0) continue;
            double speed = 520;
            double across = p.face - p.toward * 14;
            Star s;
            if (p.vertical) s = new Star(p.index, across, p.pos, -p.toward * speed, 0);
            else s = new Star(p.index, p.pos, across, 0, -p.toward * speed);
            stars.add(s);
            p.starTimer = 0; // une fois lancee, l'effet s'arrete
            play(Sfx.STAR_LAUNCH);
            spawn(s.x, s.y, 10, 45, 180);
        }
    }

    private void updateStars(double dt) {
        for (int i = stars.size() - 1; i >= 0; i--) {
            Star s = stars.get(i);
            s.x += s.vx * dt;
            s.y += s.vy * dt;
            s.t += dt;
            if (s.x < -30 || s.x > W + 30 || s.y < TOP - 30 || s.y > H + 30) {
                stars.remove(i);
                continue;
            }
            Brick hit = firstBrickHit(s.x, s.y, 9);
            if (hit != null) {
                starExplode(hit, paddles[s.owner], s.x, s.y);
                stars.remove(i);
            }
        }
    }

    /** L'etoile explose : la brique touchee et les 8 autour sont cassees (sauf les incassables). */
    private void starExplode(Brick center, Paddle owner, double x, double y) {
        scorer = owner;
        shake = Math.max(shake, 10);
        play(Sfx.STAR_EXPLODE);
        spawn(x, y, 40, 45, 380);
        for (Brick o : bricks) {
            if (o.alive && !o.unbreakable() && Math.abs(o.col - center.col) <= 1 && Math.abs(o.row - center.row) <= 1) {
                destroy(o, false);
            }
        }
    }

    private Brick firstBrickHit(double x, double y, double r) {
        for (Brick k : bricks) {
            if (!k.isSolid()) continue;
            double cx = clamp(x, k.x, k.x + k.w), cy = clamp(y, k.y, k.y + k.h);
            double ddx = x - cx, ddy = y - cy;
            if (ddx * ddx + ddy * ddy < r * r) return k;
        }
        return null;
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
        if (!type.bad) p.award(CAT_BONUS, Rules.CATCH_BONUS);
        spawn(p.vertical ? p.face : p.pos, p.vertical ? p.pos : p.face, 16, type.hue, 220);
        play(type.bad ? Sfx.MALUS : (type == PowerUp.Type.WALL ? Sfx.SHIELD : Sfx.POWERUP));
        switch (type) {
        case WIDE:
            p.wideTimer = Rules.WIDE_TIME;
            break;
        case SHRINK:
            p.shrinkTimer = Rules.SHRINK_TIME;
            break;
        case MULTI:
            List<Ball> extra = new ArrayList<Ball>();
            int owned = 0; // seules les balles de celui qui a ramasse le bonus sont multipliees
            for (Ball o : balls) {
                if (o.owner == p.index) owned++;
            }
            for (Ball b : balls) {
                if (b.stuck || b.owner != p.index) continue;
                for (int s = -1; s <= 1; s += 2) {
                    if (owned + extra.size() >= 10) break;
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
            slowTimer = Rules.SLOW_TIME;
            break;
        case FIRE:
            p.fireTimer = Rules.FIRE_TIME;
            break;
        case STICKY:
            p.stickyTimer = Rules.STICKY_TIME;
            break;
        case WALL:
            p.wallTimer = Rules.WALL_TIME;
            break;
        case NOBREAK:
            p.nobreakTimer = Rules.NOBREAK_TIME;
            break;
        case GHOSTS:
            p.ghostTimer = Rules.GHOST_TIME;
            p.ghostNext = 0;
            break;
        case STAR:
            p.starTimer = Rules.STAR_TIME;
            break;
        case LIFE:
            p.lives = Math.min(Rules.MAX_LIVES, p.lives + 1);
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
        for (int i = 0; i < n && particles.size() < 800; i++) {
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

    private void updateDeaths(double dt) {
        for (int i = deaths.size() - 1; i >= 0; i--) {
            double[] d = deaths.get(i);
            d[5] -= dt;
            if (d[5] <= 0) deaths.remove(i);
        }
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
