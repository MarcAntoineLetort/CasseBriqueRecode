package cassebrique;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/** Etat et logique du jeu. Aucune dependance a JavaFX : tout est testable seul. */
final class Game {
    // Monde logique (le rendu est mis a l'echelle de la fenetre)
    static final double W = 960, H = 640;
    static final double TOP = 56;
    static final int COLS = 14;
    static final double BW = 60, BH = 24, GAP = 4;
    static final double OFFX = 34, OFFY = 100;

    static final double PADDLE_Y = H - 46, PADDLE_H = 14;
    static final double PADDLE_BASE_W = 110, PADDLE_WIDE_W = 180;
    static final double BALL_R = 7;
    static final double MAX_SPEED = 820;
    static final double KEY_SPEED = 780;

    // Mise en page du menu (partagee avec le rendu)
    static final int CARD_COLS = 4;
    static final double CARD_W = 210, CARD_H = 80, CARD_GAP = 15, CARD_X0 = 37.5, CARD_Y0 = 215, CARD_PITCH_Y = 92;
    static final double CAMP_X = 300, CAMP_Y = 126, CAMP_W = 360, CAMP_H = 50;

    enum State { MENU, READY, PLAYING, PAUSED, LEVEL_CLEAR, GAME_OVER, WIN }

    State state = State.MENU;
    private State beforePause = State.PLAYING;

    final List<Ball> balls = new ArrayList<Ball>();
    final List<Brick> bricks = new ArrayList<Brick>();
    final List<PowerUp> powerUps = new ArrayList<PowerUp>();
    final List<Particle> particles = new ArrayList<Particle>();
    final List<Beam> beams = new ArrayList<Beam>();
    private final Random rnd = new Random();

    // Partie en cours
    boolean campaign;           // true : niveaux enchaines ; false : un seul niveau choisi
    int level, lives, score, combo, levelBonus;
    int remaining;              // briques destructibles restantes
    int goldTotal, goldRemaining;
    boolean endedByGold;

    // Records / options (sauvegardes par Storage)
    int highScore;
    final int[] levelBest = new int[Levels.NAMES.length];
    boolean saveDirty, newRecord, muted;

    // Raquette, balle, bonus
    double paddleX = W / 2, paddleW = PADDLE_BASE_W, paddleTargetW = PADDLE_BASE_W, paddleVx;
    double baseSpeed, ballSpeed;
    double wideTimer, slowTimer, fireTimer, stickyTimer;
    double buttonCooldown;
    double shake, stateTime, time;

    // Menu
    int hoverCard = -1;
    boolean hoverCampaign;

    /** Branche le son (voir Sounds). Peut rester null. */
    Consumer<Sfx> sfx;

    private boolean left, right;
    private double mouseX = Double.NaN;

    Game() {
        loadLevel(0); // initialise le decor ; on retourne ensuite au menu
        state = State.MENU;
    }

    // ------------------------------------------------------------------ entrees

    void setLeft(boolean v) {
        left = v;
        if (v) mouseX = Double.NaN;
    }

    void setRight(boolean v) {
        right = v;
        if (v) mouseX = Double.NaN;
    }

    void mouseMoved(double x, double y) {
        mouseX = x;
        hoverCard = cardAt(x, y);
        hoverCampaign = overCampaign(x, y);
    }

    /** Le curseur quitte la fenetre : la raquette file vers le bord par lequel il est sorti. */
    void mouseExited(double x) {
        mouseX = x;
        hoverCard = -1;
        hoverCampaign = false;
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
            }
        } else {
            action();
        }
    }

    /** Espace / entree : fait avancer l'ecran courant. */
    void action() {
        switch (state) {
        case MENU:
            startCampaign();
            break;
        case READY:
            launch();
            break;
        case PLAYING:
            releaseStuck();
            break;
        case PAUSED:
            togglePause();
            break;
        case LEVEL_CLEAR:
            if (stateTime > 0.4) {
                if (campaign) loadLevel(level + 1);
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
        state = State.MENU;
        stateTime = 0;
    }

    // ------------------------------------------------------------------ menu

    static double cardX(int i) {
        return CARD_X0 + (i % CARD_COLS) * (CARD_W + CARD_GAP);
    }

    static double cardY(int i) {
        return CARD_Y0 + (i / CARD_COLS) * CARD_PITCH_Y;
    }

    int cardAt(double x, double y) {
        for (int i = 0; i < Levels.NAMES.length; i++) {
            double cx = cardX(i), cy = cardY(i);
            if (x >= cx && x <= cx + CARD_W && y >= cy && y <= cy + CARD_H) return i;
        }
        return -1;
    }

    boolean overCampaign(double x, double y) {
        return x >= CAMP_X && x <= CAMP_X + CAMP_W && y >= CAMP_Y && y <= CAMP_Y + CAMP_H;
    }

    // ------------------------------------------------------------------ cycle de vie

    void startCampaign() {
        campaign = true;
        beginRun(0);
    }

    void startSingle(int index) {
        campaign = false;
        beginRun(index);
    }

    private void beginRun(int index) {
        lives = 3;
        score = 0;
        newRecord = false;
        loadLevel(index);
    }

    void loadLevel(int index) {
        level = index;
        bricks.clear();
        powerUps.clear();
        balls.clear();
        particles.clear();
        beams.clear();
        remaining = 0;
        goldTotal = 0;
        String[] rows = Levels.DATA[index];
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < COLS; c++) {
                char ch = c < rows[r].length() ? rows[r].charAt(c) : '.';
                if (ch == '.') continue;
                Brick b = new Brick(c, r, OFFX + c * (BW + GAP), OFFY + r * (BH + GAP), ch);
                bricks.add(b);
                if (!b.indestructible()) remaining++;
                if (b.kind == Brick.Kind.GOLD) goldTotal++;
            }
        }
        goldRemaining = goldTotal;
        endedByGold = false;
        baseSpeed = Math.min(MAX_SPEED - 150, 430 + index * 28);
        ballSpeed = baseSpeed;
        combo = 0;
        wideTimer = slowTimer = fireTimer = stickyTimer = 0;
        buttonCooldown = 0;
        paddleTargetW = PADDLE_BASE_W;
        paddleW = PADDLE_BASE_W;
        spawnStuckBall();
        state = State.READY;
        stateTime = 0;
    }

    private void spawnStuckBall() {
        Ball b = new Ball();
        b.stuck = true;
        b.stickOffset = 0;
        b.x = paddleX;
        b.y = PADDLE_Y - BALL_R - 1;
        balls.add(b);
    }

    /** Premier lancement (ou apres une balle perdue) : angle legerement aleatoire. */
    private void launch() {
        for (Ball b : balls) {
            if (!b.stuck) continue;
            b.stuck = false;
            double a = (rnd.nextDouble() - 0.5) * 0.5;
            b.dx = Math.sin(a);
            b.dy = -Math.cos(a);
        }
        play(Sfx.LAUNCH);
        state = State.PLAYING;
    }

    /** Relance les balles collees : l'angle depend de l'endroit ou elles se sont collees. */
    private void releaseStuck() {
        boolean any = false;
        for (Ball b : balls) {
            if (!b.stuck) continue;
            double rel = clamp(b.stickOffset / (paddleW / 2), -1, 1);
            double angle = rel * 1.05;
            b.dx = Math.sin(angle);
            b.dy = -Math.cos(angle);
            b.stuck = false;
            b.y = PADDLE_Y - BALL_R - 1;
            any = true;
        }
        if (any) play(Sfx.LAUNCH);
    }

    private void loseLife() {
        lives--;
        combo = 0;
        shake = 14;
        powerUps.clear();
        wideTimer = slowTimer = fireTimer = stickyTimer = 0;
        paddleTargetW = PADDLE_BASE_W;
        if (lives <= 0) {
            state = State.GAME_OVER;
            stateTime = 0;
            play(Sfx.GAME_OVER);
            finalizeScore();
        } else {
            play(Sfx.LIFE_LOST);
            ballSpeed = Math.max(baseSpeed, ballSpeed * 0.9);
            spawnStuckBall();
            state = State.READY;
            stateTime = 0;
        }
    }

    /** Niveau fini : s'il y a de l'or, il suffit de l'avoir tout ramasse. */
    private boolean isCleared() {
        return goldTotal > 0 ? goldRemaining <= 0 : remaining <= 0;
    }

    private void levelClear() {
        endedByGold = goldTotal > 0;
        balls.clear();
        powerUps.clear();
        levelBonus = lives * 100;
        score += levelBonus;
        for (int i = 0; i < 6; i++) {
            spawn(80 + rnd.nextDouble() * (W - 160), 120 + rnd.nextDouble() * 200, 25, rnd.nextDouble() * 360, 260);
        }
        stateTime = 0;
        play(Sfx.LEVEL_CLEAR);
        if (campaign && level >= Levels.NAMES.length - 1) {
            state = State.WIN;
            finalizeScore();
        } else {
            state = State.LEVEL_CLEAR;
            if (!campaign) finalizeScore();
        }
    }

    private void finalizeScore() {
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

    int multiplier() {
        return Math.min(8, 1 + combo / 4);
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
            movePaddle(dt);
            for (Ball b : balls) {
                if (b.stuck) follow(b);
            }
        }

        if (state == State.PLAYING) {
            if (wideTimer > 0) {
                wideTimer -= dt;
                if (wideTimer <= 0) paddleTargetW = PADDLE_BASE_W;
            }
            slowTimer = Math.max(0, slowTimer - dt);
            fireTimer = Math.max(0, fireTimer - dt);
            if (stickyTimer > 0) {
                stickyTimer -= dt;
                if (stickyTimer <= 0) releaseStuck(); // fin du bonus : les balles collees repartent
            }

            for (int i = balls.size() - 1; i >= 0; i--) {
                Ball b = balls.get(i);
                stepBall(b, dt);
                if (b.dead) balls.remove(i);
            }
            updatePowerUps(dt);

            if (isCleared()) levelClear();
            else if (balls.isEmpty()) loseLife();
        }
    }

    private void follow(Ball b) {
        double half = paddleW / 2 - BALL_R * 0.5;
        b.stickOffset = clamp(b.stickOffset, -half, half);
        b.x = paddleX + b.stickOffset;
        b.y = PADDLE_Y - BALL_R - 1;
        b.clearTrail();
    }

    private void movePaddle(double dt) {
        double old = paddleX;
        if (left != right) {
            paddleX += (right ? 1 : -1) * KEY_SPEED * dt;
        } else if (!Double.isNaN(mouseX)) {
            paddleX += (mouseX - paddleX) * Math.min(1, dt * 45);
        }
        paddleW += (paddleTargetW - paddleW) * Math.min(1, dt * 10);
        double half = paddleW / 2;
        paddleX = clamp(paddleX, half, W - half);
        paddleVx = dt > 0 ? (paddleX - old) / dt : 0;
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
            collidePaddle(b);
            collideBricks(b);
        }
        b.pushTrail();
    }

    private void collideWalls(Ball b) {
        boolean bounced = false;
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
        if (bounced) play(Sfx.WALL);
        if (b.y - BALL_R > H) b.dead = true;
    }

    private void collidePaddle(Ball b) {
        if (b.dy <= 0) return;
        if (b.y + BALL_R < PADDLE_Y) return;
        if (b.y > PADDLE_Y + PADDLE_H * 0.6) return; // deja passee sous la raquette
        double half = paddleW / 2;
        if (Math.abs(b.x - paddleX) > half + BALL_R * 0.5) return;

        if (stickyTimer > 0) { // balle collante : elle reste sur la raquette jusqu'au clic
            b.stuck = true;
            b.stickOffset = b.x - paddleX;
            b.y = PADDLE_Y - BALL_R - 1;
            b.clearTrail();
            combo = 0;
            play(Sfx.STICKY);
            return;
        }

        double rel = clamp((b.x - paddleX) / half, -1, 1);
        rel = clamp(rel + clamp(paddleVx / 2500.0, -0.25, 0.25), -1, 1); // effet de la raquette
        double angle = rel * 1.05; // ~60 degres max
        b.dx = Math.sin(angle);
        b.dy = -Math.cos(angle);
        b.y = PADDLE_Y - BALL_R;
        combo = 0;
        ballSpeed = Math.min(MAX_SPEED, ballSpeed + 5);
        spawn(b.x, PADDLE_Y, 6, 210, 140);
        play(Sfx.PADDLE);
    }

    private void collideBricks(Ball b) {
        boolean fire = fireTimer > 0;
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
            hitBrick(k);
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
        combo++;
        ballSpeed = Math.min(MAX_SPEED, ballSpeed + 2);
        if (k.hp <= 0) {
            destroy(k);
        } else {
            score += 2 * multiplier();
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
        score += points * multiplier();
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
            apply(k.bonus); // bonus immediat, rien ne tombe
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
        beams.add(new Beam(true, x));
        shake = Math.max(shake, 8);
        play(Sfx.LIGHTNING);
        for (double y = TOP + 20; y < PADDLE_Y; y += 55) spawn(x, y, 3, 55, 170);
        for (Brick o : bricks) {
            if (o.alive && !o.indestructible() && o.col == k.col) destroy(o);
        }
    }

    /** Roche : detruit toute la ligne (les briques indestructibles resistent). */
    private void rockslide(Brick k) {
        double y = k.y + BH / 2;
        beams.add(new Beam(false, y));
        shake = Math.max(shake, 8);
        play(Sfx.ROCK);
        for (double x = 40; x < W; x += 80) spawn(x, y, 3, 28, 170);
        for (Brick o : bricks) {
            if (o.alive && !o.indestructible() && o.row == k.row) destroy(o);
        }
    }

    private void dropPowerUp(Brick k) {
        PowerUp.Type[] all = PowerUp.Type.values();
        PowerUp.Type t = all[rnd.nextInt(all.length)];
        if (t == PowerUp.Type.LIFE && rnd.nextDouble() < 0.6) t = PowerUp.Type.WIDE; // la vie reste rare
        powerUps.add(new PowerUp(t, k.x + BW / 2, k.y + BH / 2));
    }

    // ------------------------------------------------------------------ bonus

    private void updatePowerUps(double dt) {
        for (int i = powerUps.size() - 1; i >= 0; i--) {
            PowerUp p = powerUps.get(i);
            p.y += 170 * dt;
            p.t += dt;
            boolean caught = p.y + 10 >= PADDLE_Y && p.y - 10 <= PADDLE_Y + PADDLE_H
                    && Math.abs(p.x - paddleX) <= paddleW / 2 + 16;
            if (caught) {
                apply(p.type);
                powerUps.remove(i);
            } else if (p.y > H + 20) {
                powerUps.remove(i);
            }
        }
    }

    private void apply(PowerUp.Type type) {
        score += 50;
        spawn(paddleX, PADDLE_Y, 16, type.hue, 220);
        play(Sfx.POWERUP);
        switch (type) {
        case WIDE:
            wideTimer = 12;
            paddleTargetW = PADDLE_WIDE_W;
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
            fireTimer = 6;
            break;
        case STICKY:
            stickyTimer = 15;
            break;
        case LIFE:
            lives = Math.min(6, lives + 1);
            break;
        }
    }

    // ------------------------------------------------------------------ utilitaires

    /** Normalise la direction et evite les trajectoires quasi horizontales. */
    private static void fixDirection(Ball b) {
        double len = Math.hypot(b.dx, b.dy);
        if (len < 1e-9) {
            b.dx = 0;
            b.dy = -1;
            return;
        }
        b.dx /= len;
        b.dy /= len;
        if (Math.abs(b.dy) < 0.22) {
            b.dy = (b.dy < 0 ? -1 : 1) * 0.22;
            double s = b.dx < 0 ? -1 : 1;
            b.dx = s * Math.sqrt(1 - b.dy * b.dy);
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
