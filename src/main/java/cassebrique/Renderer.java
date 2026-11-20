package cassebrique;

import java.util.Random;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.TextAlignment;

/** Dessin du jeu. Travaille dans le repere logique 960x640, mis a l'echelle de la fenetre. */
final class Renderer {
    private final Backgrounds backgrounds = new Backgrounds();

    void render(GraphicsContext gc, Game g, double w, double h) {
        gc.setFill(Theme.OUTSIDE);
        gc.fillRect(0, 0, w, h);

        double s = Math.min(w / Game.W, h / Game.H);
        gc.save();
        gc.translate((w - Game.W * s) / 2, (h - Game.H * s) / 2);
        gc.scale(s, s);

        boolean menuLike = g.state == Game.State.MENU || g.state == Game.State.INFO || g.state == Game.State.NET;
        boolean duel = g.twoPlayer && !menuLike;
        Art.setSpriteScale(s);
        backgrounds.draw(gc, g.time, menuLike ? 0 : g.theme, duel, s);

        switch (g.state) {
        case MENU:
            drawMenu(gc, g);
            break;
        case INFO:
            InfoScreen.draw(gc, g, g.time);
            break;
        case NET:
            NetScreen.draw(gc, g);
            break;
        case RESULTS:
            gc.save();
            if (g.shake > 0.1) gc.translate((Math.random() - 0.5) * g.shake, (Math.random() - 0.5) * g.shake);
            drawResults(gc, g);
            drawParticles(gc, g);
            gc.restore();
            break;
        default:
            drawPlay(gc, g, duel);
            break;
        }
        gc.restore();
        // bandes noires autour de la zone de jeu (plus economique qu'un clip du Canvas a chaque image)
        double ox = (w - Game.W * s) / 2, oy = (h - Game.H * s) / 2;
        gc.setFill(Theme.OUTSIDE);
        if (ox > 0.5) {
            gc.fillRect(0, 0, ox, h);
            gc.fillRect(w - ox, 0, ox, h);
        }
        if (oy > 0.5) {
            gc.fillRect(0, 0, w, oy);
            gc.fillRect(0, h - oy, w, oy);
        }
    }

    // ------------------------------------------------------------------ partie

    private void drawPlay(GraphicsContext gc, Game g, boolean duel) {
        gc.save();
        if (g.shake > 0.1) {
            gc.translate((Math.random() - 0.5) * g.shake, (Math.random() - 0.5) * g.shake);
        }
        gc.setStroke(duel ? Color.web("#16213a") : Color.web("#33407f"));
        gc.setLineWidth(2);
        gc.strokeLine(0, Game.TOP, Game.W, Game.TOP);

        for (Brick k : g.bricks) {
            if (k.alive) Art.brick(gc, k, g.time);
        }
        boolean night = g.nightTimer > 0;
        if (night) drawNight(gc, g);

        for (Beam b : g.beams) drawBeam(gc, b);
        for (Rainbow rb : g.rainbows) drawRainbow(gc, rb);
        for (Paddle p : g.paddles) {
            if (p.wallTimer > 0) drawWall(gc, g, p);
        }
        for (Paddle p : g.paddles) drawPaddle(gc, g, p);
        if (!night) { // en pleine nuit, les bonus qui tombent sont invisibles
            for (PowerUp u : g.powerUps) Art.capsule(gc, u.type, u.x, u.y + Math.sin(u.t * 8) * 1.5, u.t);
        }
        for (Ghost gh : g.ghosts) drawGhost(gc, gh, g.time);
        for (Star st : g.stars) drawStar(gc, st);
        for (Ball b : g.balls) drawBall(gc, g, b);
        drawParticles(gc, g);
        gc.restore();

        drawHud(gc, g);
        drawOverlay(gc, g);
    }

    /** Nuit : tout est noir sauf raquettes, balles, effets et briques qui viennent de disparaitre. */
    /** Arc-en-ciel droit entre deux briques arc-en-ciel : bandes colorees qui s'effacent. */
    private void drawRainbow(GraphicsContext gc, Rainbow r) {
        double f = r.life / Rainbow.MAX_LIFE;                    // 1 -> 0
        double grow = Math.min(1, (1 - f) * 6);                  // le trait se deploie vite
        double a = Math.min(1, f * 2.2);
        double dx = r.x2 - r.x1, dy = r.y2 - r.y1, len = Math.hypot(dx, dy);
        if (len < 1) return;
        double nx = -dy / len, ny = dx / len; // normale
        double ex = r.x1 + dx * grow, ey = r.y1 + dy * grow;
        int bands = 7;
        double bw = 3.2 * (0.6 + 0.4 * f);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.BUTT);
        gc.setGlobalAlpha(a * 0.28);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(bands * bw + 8);
        gc.strokeLine(r.x1, r.y1, ex, ey);
        gc.setGlobalAlpha(a);
        gc.setLineWidth(bw + 0.6);
        for (int i = 0; i < bands; i++) {
            double off = (i - (bands - 1) / 2.0) * bw;
            gc.setStroke(Color.hsb(i * 50.0, 0.9, 1));
            gc.strokeLine(r.x1 + nx * off, r.y1 + ny * off, ex + nx * off, ey + ny * off);
        }
        gc.setGlobalAlpha(1);
    }

    private void drawNight(GraphicsContext gc, Game g) {
        double elapsed = Rules.NIGHT_TIME - g.nightTimer;
        double a = 0.97 * Math.min(1, elapsed / 0.4) * Math.min(1, g.nightTimer / 0.8);
        gc.setFill(Color.rgb(0, 0, 4, a));
        gc.fillRect(-20, -20, Game.W + 40, Game.H + 40);
        for (double[] d : g.deaths) { // on voit brievement les briques qui se detruisent
            double f = Math.max(0, Math.min(1, d[5] / 0.5));
            gc.setFill(Color.hsb(Art.hue(d[4]), 0.7, 1, 0.85 * f));
            gc.fillRoundRect(d[0], d[1], d[2], d[3], 8, 8);
        }
    }

    private void drawBeam(GraphicsContext gc, Beam b) {
        double a = Math.max(0, Math.min(1, b.life / b.maxLife));
        Random r = new Random(b.seed);
        int n = 26;
        double[] xs = new double[n + 1], ys = new double[n + 1];
        if (b.vertical) {
            for (int i = 0; i <= n; i++) {
                ys[i] = b.a + (b.b - b.a) * i / n;
                xs[i] = b.pos + ((i == 0 || i == n) ? 0 : (r.nextDouble() - 0.5) * 22);
            }
            gc.setStroke(Color.rgb(255, 230, 80, a * 0.35));
            gc.setLineWidth(16);
            gc.strokePolyline(xs, ys, n + 1);
            gc.setStroke(Color.rgb(255, 255, 255, a));
            gc.setLineWidth(3);
            gc.strokePolyline(xs, ys, n + 1);
        } else {
            gc.setFill(Color.rgb(160, 115, 70, a * 0.40));
            gc.fillRect(b.a, b.pos - b.thick / 2, b.b - b.a, b.thick);
            for (int i = 0; i <= n; i++) {
                xs[i] = b.a + (b.b - b.a) * i / n;
                ys[i] = b.pos + ((i == 0 || i == n) ? 0 : (r.nextDouble() - 0.5) * 14);
            }
            gc.setStroke(Color.rgb(70, 45, 28, a));
            gc.setLineWidth(3);
            gc.strokePolyline(xs, ys, n + 1);
        }
    }

    /** Mur de secours derriere la raquette. */
    private void drawWall(GraphicsContext gc, Game g, Paddle p) {
        double wf = p.wallFace();
        double a0 = Math.min(wf, wf + p.toward * 6);
        double alpha = p.wallTimer < 2 ? 0.5 + 0.5 * Math.sin(g.time * 24) : 1.0; // clignote avant de disparaitre
        Color glow = Color.hsb(300, 0.6, 1, 0.25 * alpha);
        Color core = Color.hsb(300, 0.5, 1, 0.9 * alpha);
        if (p.vertical) {
            gc.setFill(glow);
            gc.fillRect(a0 - 5, Game.TOP, 16, Game.H - Game.TOP);
            gc.setFill(core);
            gc.fillRect(a0, Game.TOP, 6, Game.H - Game.TOP);
        } else {
            gc.setFill(glow);
            gc.fillRect(0, a0 - 5, Game.W, 16);
            gc.setFill(core);
            gc.fillRect(0, a0, Game.W, 6);
        }
    }

    private void drawPaddle(GraphicsContext gc, Game g, Paddle p) {
        double x = p.rectX(), y = p.rectY(), w = p.rectW(), h = p.rectH();
        double arc = Paddle.T;
        boolean fire = p.fireTimer > 0;
        Color glow, body, light, edge;
        if (g.twoPlayer) {
            if (p.index == 0) { // blanc
                glow = Color.rgb(255, 255, 255, 0.35);
                body = Theme.PAPER;
                light = Color.rgb(120, 120, 140, 0.35);
                edge = Theme.INK;
            } else {            // noir
                glow = Color.rgb(255, 255, 255, 0.30); // halo clair : le noir ressort sur le fond sombre
                body = Theme.INK;
                light = Color.rgb(255, 255, 255, 0.25);
                edge = Color.web("#f4f4fb");
            }
        } else {
            glow = fire ? Color.rgb(255, 150, 60, 0.20) : Color.rgb(90, 160, 255, 0.18);
            body = fire ? Color.web("#ffb060") : Color.web("#9cc2ff");
            light = Color.rgb(255, 255, 255, 0.35);
            edge = fire ? Color.web("#c9722f") : Color.web("#4f7fd6");
        }
        gc.setFill(glow);
        gc.fillRoundRect(x - 6, y - 6, w + 12, h + 12, arc + 6, arc + 6);
        double ep = g.twoPlayer ? 3 : 2;
        gc.setFill(edge);
        gc.fillRoundRect(x - ep, y - ep, w + 2 * ep, h + 2 * ep, arc + ep, arc + ep);
        gc.setFill(body);
        gc.fillRoundRect(x, y, w, h, arc, arc);
        gc.setFill(light);
        if (p.vertical) gc.fillRoundRect(x + 3, y + 6, 4, h - 12, 4, 4);
        else gc.fillRoundRect(x + 6, y + 3, w - 12, 4, 4, 4);

        if (fire && g.twoPlayer) { // liseret orange
            gc.setStroke(Color.web("#ff9a3c"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x - 3, y - 3, w + 6, h + 6, arc + 3, arc + 3);
        }
        if (p.shrinkTimer > 0) { // malus : contour rouge qui pulse
            gc.setStroke(Color.rgb(255, 50, 50, 0.55 + 0.4 * Math.sin(g.time * 10)));
            gc.setLineWidth(3);
            gc.strokeRoundRect(x - 4, y - 4, w + 8, h + 8, arc + 4, arc + 4);
        }
        if (p.lavaTimer > 0) { // balle de lave prete : liseret rouge-orange qui braise
            gc.setStroke(Color.rgb(255, 90 + (int) (60 * Math.sin(g.time * 9)), 20, 0.85));
            gc.setLineWidth(2.5);
            gc.strokeRoundRect(x - 5, y - 5, w + 10, h + 10, arc + 5, arc + 5);
        }
        if (p.drunkTimer > 0) { // ivresse : contour dont la couleur tourne
            gc.setStroke(Color.hsb(Art.hue(g.time * 260), 0.7, 1, 0.9));
            gc.setLineWidth(3);
            gc.setLineDashes(5, 4);
            gc.setLineDashOffset(-g.time * 30);
            gc.strokeRoundRect(x - 6, y - 6, w + 12, h + 12, arc + 6, arc + 6);
            gc.setLineDashes((double[]) null);
        }
        if (p.stickyTimer > 0) { // face gluante
            double s0 = p.toward > 0 ? p.face - 3 : p.face - 2;
            gc.setFill(Color.hsb(60, 0.8, 1, 0.95));
            if (p.vertical) gc.fillRoundRect(s0, y + 2, 5, h - 4, 5, 5);
            else gc.fillRoundRect(x + 2, s0, w - 4, 5, 5, 5);
        }
        if (p.starTimer > 0) { // aura doree : etoile filante prete, un clic la lance
            double pulse = 0.5 + 0.5 * Math.sin(g.time * 8);
            gc.setStroke(Color.rgb(255, 215, 70, 0.40 + 0.40 * pulse));
            gc.setLineWidth(3);
            gc.strokeRoundRect(x - 8, y - 8, w + 16, h + 16, arc + 8, arc + 8);
            gc.setStroke(Color.rgb(255, 240, 160, 0.15 + 0.25 * pulse));
            gc.setLineWidth(5);
            gc.strokeRoundRect(x - 14, y - 14, w + 28, h + 28, arc + 14, arc + 14);
            gc.setFill(Color.rgb(255, 225, 90, 0.95));
            double sx = p.vertical ? p.face - p.toward * 22 : p.pos;
            double sy = p.vertical ? p.pos : p.face - p.toward * 22;
            Art.star(gc, sx, sy, 8, 3.6, g.time * 2);
        }
    }

    private void drawBall(GraphicsContext gc, Game g, Ball b) {
        Paddle o = g.paddles[Math.min(b.owner, g.paddles.length - 1)];
        boolean fire = o.fireTimer > 0;
        boolean soft = o.nobreakTimer > 0;
        boolean lava = o.lavaTimer > 0;
        boolean drunk = o.drunkTimer > 0;
        boolean duel = g.twoPlayer;
        boolean black = duel && b.owner == 1;

        Color trail, glow, body, ring = null;
        if (soft) {
            trail = Color.rgb(140, 147, 168, 1);
            glow = Color.rgb(255, 60, 60, 0.18);
            body = Color.web("#8c93a8");
            ring = Color.rgb(255, 60, 60, 0.6 + 0.4 * Math.sin(g.time * 12));
        } else if (lava) {
            trail = Color.rgb(255, 80, 20, 1);
            glow = Color.rgb(255, 70, 0, 0.28 + 0.12 * Math.sin(g.time * 14));
            body = Color.web("#e0300d");
            ring = duel ? (black ? Color.web("#f4f4fb") : Theme.INK) : Color.rgb(255, 160, 40, 0.9);
        } else if (fire) {
            trail = Color.rgb(255, 140, 40, 1);
            glow = Color.rgb(255, 120, 30, 0.18);
            body = Color.web("#ffd27a");
            if (duel) ring = black ? Color.web("#f4f4fb") : Theme.INK;
        } else if (duel) {
            trail = black ? Color.rgb(150, 130, 255, 1) : Color.rgb(255, 255, 255, 1);
            glow = black ? Color.rgb(200, 190, 255, 0.28) : Color.rgb(255, 255, 255, 0.30);
            body = black ? Theme.INK : Theme.PAPER;
            ring = black ? Color.web("#f4f4fb") : Theme.INK;
        } else {
            trail = Color.rgb(150, 200, 255, 1);
            glow = Color.rgb(170, 210, 255, 0.15);
            body = Color.WHITE;
        }

        int n = b.trailSize();
        gc.setFill(trail);
        for (int i = 0; i < n; i++) {
            double f = (i + 1) / (double) n;
            double r = Game.BALL_R * (0.35 + 0.65 * f);
            gc.setGlobalAlpha(f * 0.32);
            gc.fillOval(b.trailX(i) - r, b.trailY(i) - r, r * 2, r * 2);
        }
        gc.setGlobalAlpha(1);
        double r = Game.BALL_R;
        gc.setFill(glow);
        gc.fillOval(b.x - r * 2.2, b.y - r * 2.2, r * 4.4, r * 4.4);
        if (ring != null) {
            gc.setFill(ring);
            gc.fillOval(b.x - r - 2, b.y - r - 2, r * 2 + 4, r * 2 + 4);
        }
        gc.setFill(body);
        gc.fillOval(b.x - r, b.y - r, r * 2, r * 2);
        if (lava && !soft) { // fissures incandescentes qui tournent
            gc.setStroke(Color.rgb(255, 225, 90, 0.95));
            gc.setLineWidth(1.4);
            double an = g.time * 3;
            for (int i = 0; i < 3; i++) {
                double a2 = an + i * 2.094;
                gc.strokeLine(b.x, b.y, b.x + Math.cos(a2) * r * 0.85, b.y + Math.sin(a2) * r * 0.85);
            }
        }
        if (drunk) { // trois etincelles qui tournent autour de la balle
            for (int i = 0; i < 3; i++) {
                double a2 = g.time * 8 + i * 2.094;
                gc.setFill(Color.hsb(Art.hue(g.time * 200 + i * 120), 0.8, 1, 0.95));
                gc.fillOval(b.x + Math.cos(a2) * r * 2 - 2, b.y + Math.sin(a2) * r * 2 - 2, 4, 4);
            }
        }
    }

    /** Petit fantome extraterrestre. */
    private void drawGhost(GraphicsContext gc, Ghost gh, double time) {
        double x = gh.x, y = gh.y;
        double wob = Math.sin(time * 10 + gh.t * 6) * 1.5;
        gc.setFill(Color.rgb(150, 255, 175, 0.22));
        gc.fillOval(x - 15, y - 15, 30, 30);
        gc.setStroke(Color.web("#b5ffc4"));
        gc.setLineWidth(2);
        gc.strokeLine(x, y - 8, x, y - 13);
        gc.setFill(Color.web("#d6ffe0"));
        gc.fillOval(x - 2, y - 16, 4, 4);
        gc.setFill(Color.web("#b5ffc4"));
        gc.fillOval(x - 8, y - 9, 16, 16);
        gc.fillRect(x - 8, y - 1, 16, 8);
        for (int i = 0; i < 3; i++) gc.fillOval(x - 8 + i * 5.4, y + 3 + (i % 2 == 0 ? wob : -wob) * 0.5, 5.4, 6);
        gc.setFill(Color.web("#10241a"));
        gc.fillOval(x - 6, y - 4, 5, 7);
        gc.fillOval(x + 1, y - 4, 5, 7);
        gc.setFill(Color.WHITE);
        gc.fillOval(x - 4.5, y - 3, 2, 2);
        gc.fillOval(x + 2.5, y - 3, 2, 2);
    }

    private void drawStar(GraphicsContext gc, Star s) {
        double len = Math.hypot(s.vx, s.vy);
        double ux = len > 0 ? s.vx / len : 0, uy = len > 0 ? s.vy / len : -1;
        for (int i = 1; i <= 7; i++) { // traine
            double f = 1 - i / 8.0;
            gc.setFill(Color.rgb(255, 210, 80, 0.45 * f));
            double r = 7 * f;
            gc.fillOval(s.x - ux * i * 8 - r, s.y - uy * i * 8 - r, r * 2, r * 2);
        }
        gc.setFill(Color.rgb(255, 235, 140, 0.25));
        gc.fillOval(s.x - 16, s.y - 16, 32, 32);
        gc.setFill(Color.rgb(255, 225, 80, 1));
        Art.star(gc, s.x, s.y, 10, 4.4, s.t * 9);
    }

    private void drawParticles(GraphicsContext gc, Game g) {
        for (int i = 0, n = g.particles.size(); i < n; i++) {
            Particle p = g.particles.get(i);
            gc.setGlobalAlpha(Math.max(0, Math.min(1, p.life / p.maxLife)));
            gc.setFill(Art.hueColor(p.hue));
            gc.fillRect(p.x, p.y, p.size, p.size);
        }
        gc.setGlobalAlpha(1);
    }

    // ------------------------------------------------------------------ HUD

    private void drawHud(GraphicsContext gc, Game g) {
        if (g.twoPlayer) drawHudDuel(gc, g);
        else drawHudSolo(gc, g);

        if (g.goldTotal > 0) {
            String t = "OR  " + (g.goldTotal - g.goldRemaining) + " / " + g.goldTotal;
            if (g.twoPlayer) Theme.center(gc, t, Game.W / 2, Game.H - 14, Theme.MED, Color.web("#fff0a0"));
            else Theme.right(gc, t, Game.W - 20, Game.H - 14, Theme.MED, Theme.GOLD);
        }
        if (g.muted) Theme.right(gc, "SON COUP\u00c9 (N)", Game.W - 20, 49, Theme.TINY, Color.web("#8a94c8"));
    }

    private void drawHudSolo(GraphicsContext gc, Game g) {
        Paddle p = g.paddles[0];
        Theme.left(gc, "SCORE  " + g.score(), 20, 20, Theme.HUD, Theme.WHITE);
        int best = g.campaign ? g.highScore : g.levelBest[g.level];
        Theme.left(gc, "RECORD  " + best, 20, 42, Theme.TINY, Color.web("#8a94c8"));
        int m = g.multiplier(0);
        if (m > 1) Theme.left(gc, "x" + m, 250, 24, Theme.HUD, Color.hsb(50, 0.9, 1));
        Theme.center(gc, "NIVEAU " + Levels.number(g.level) + "  -  " + Levels.get(g.level).name, Game.W / 2, 28,
                Theme.SMALL, Theme.SOFT);

        for (int i = 0; i < p.lives; i++) {
            double x = Game.W - 30 - i * 24;
            gc.setFill(Color.rgb(170, 210, 255, 0.2));
            gc.fillOval(x - 11, 17, 22, 22);
            gc.setFill(Color.WHITE);
            gc.fillOval(x - 7, 21, 14, 14);
        }

        double y = timers(gc, p, 20, Game.H - 14);
        timerBar(gc, "RALENTI", g.slowTimer, Rules.SLOW_TIME, 20, y, 270, 0.8);
    }

    /** En duel les scores restent caches jusqu'au tableau final ; on ne voit que les balles restantes. */
    private void drawHudDuel(GraphicsContext gc, Game g) {
        gc.setFill(Color.rgb(10, 12, 30, 0.82));
        gc.fillRect(0, 0, Game.W, Game.TOP);
        pill(gc, g, 0, 20, "BLANC");
        pill(gc, g, 1, Game.W - 20 - 200, "NOIR");
        Theme.center(gc, "NIVEAU " + Levels.number(g.level) + "  -  " + Levels.get(g.level).name, Game.W / 2, 20,
                Theme.SMALL, Theme.SOFT);
        Theme.center(gc, g.netPlay ? "DUEL EN R\u00c9SEAU" : "DUEL", Game.W / 2, 40, Theme.TINY, Color.web("#8a94c8"));

        timers(gc, g.paddles[0], 20, Game.H - 14);
        timers(gc, g.paddles[1], Game.W - 20 - 192, Game.H - 14);
        timerBar(gc, "RALENTI", g.slowTimer, Rules.SLOW_TIME, Game.W / 2 - 96, Game.H - 40, 270, 0.8);
    }

    private void pill(GraphicsContext gc, Game g, int player, double x, String name) {
        boolean white = player == 0;
        gc.setFill(white ? Theme.PAPER : Theme.INK);
        gc.fillRoundRect(x, 12, 200, 32, 16, 16);
        gc.setStroke(white ? Theme.INK : Color.web("#f4f4fb"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, 12, 200, 32, 16, 16);
        Theme.left(gc, name, x + 14, 29, Theme.HUD, white ? Theme.INK : Theme.PAPER);
        if (g.netPlay && player == g.localIdx) {
            Theme.left(gc, "(vous)", x + 14 + (white ? 74 : 62), 31, Theme.TINY, white ? Color.web("#555a78") : Color.web("#aab4ee"));
        }
        Paddle p = g.paddles[player];
        for (int i = 0; i < p.lives; i++) { // balles restantes
            double bx = x + 186 - i * 18;
            gc.setFill(white ? Theme.INK : Theme.PAPER);
            gc.fillOval(bx - 7, 22, 14, 14);
        }
    }

    /** Barres de duree des effets d'une raquette ; renvoie le y suivant disponible. */
    private double timers(GraphicsContext gc, Paddle p, double x, double y) {
        y = timerBar(gc, "LARGE", p.wideTimer, Rules.WIDE_TIME, x, y, 140, 0.8);
        y = timerBar(gc, "PETIT", p.shrinkTimer, Rules.SHRINK_TIME, x, y, 0, 0.9);
        y = timerBar(gc, "COLLANT", p.stickyTimer, Rules.STICKY_TIME, x, y, 60, 0.8);
        y = timerBar(gc, "FEU", p.fireTimer, Rules.FIRE_TIME, x, y, 25, 0.8);
        y = timerBar(gc, "MUR", p.wallTimer, Rules.WALL_TIME, x, y, 300, 0.6);
        y = timerBar(gc, "FANT\u00d4MES", p.ghostTimer, Rules.GHOST_TIME, x, y, 150, 0.6);
        y = timerBar(gc, "\u00c9TOILE", p.starTimer, Rules.STAR_TIME, x, y, 45, 0.8);
        y = timerBar(gc, "LAVE", p.lavaTimer, Rules.LAVA_TIME, x, y, 12, 0.9);
        y = timerBar(gc, "INOFFENSIF", p.nobreakTimer, Rules.NOBREAK_TIME, x, y, 0, 0.9);
        y = timerBar(gc, "IVRESSE", p.drunkTimer, Rules.DRUNK_TIME, x, y, 290, 0.8);
        return y;
    }

    private double timerBar(GraphicsContext gc, String label, double t, double max, double x, double y,
            double hue, double sat) {
        if (t <= 0) return y;
        Theme.left(gc, label, x, y, Theme.TINY, Color.hsb(hue, 0.5, 1));
        gc.setFill(Color.rgb(255, 255, 255, 0.15));
        gc.fillRoundRect(x + 92, y - 4, 100, 8, 8, 8);
        gc.setFill(Color.hsb(hue, sat, 1));
        gc.fillRoundRect(x + 92, y - 4, 100 * Math.min(1, t / max), 8, 8, 8);
        return y - 20;
    }

    // ------------------------------------------------------------------ menu

    private void drawMenu(GraphicsContext gc, Game g) {
        Theme.center(gc, "CASSE-BRIQUE", Game.W / 2, 50, Theme.TITLE, Color.hsb(Art.hue(200 + Math.sin(g.time) * 40), 0.7, 1));

        boolean duelTab = g.menuTab == 1;
        boolean connected = g.net.connected();
        boolean guest = connected && !g.net.isHost();
        if (!duelTab) {
            button(gc, Game.CAMP_X, Game.CAMP_Y, Game.CAMP_W, Game.CAMP_H, g.hoverCampaign);
            Theme.center(gc, "CAMPAGNE  (ESPACE)", Game.W / 2, Game.CAMP_Y + 16, Theme.MED, Theme.WHITE);
            Theme.center(gc, "niveaux solo encha\u00een\u00e9s - record : " + g.highScore, Game.W / 2, Game.CAMP_Y + 35, Theme.TINY, Theme.GOLD);
        } else if (!connected) {
            button(gc, Game.CAMP_X, Game.CAMP_Y, Game.CAMP_W, Game.CAMP_H, g.hoverCampaign);
            Theme.center(gc, "JOUER EN R\u00c9SEAU", Game.W / 2, Game.CAMP_Y + 16, Theme.MED, Theme.WHITE);
            Theme.center(gc, "2 ordinateurs, \u00e0 la souris", Game.W / 2, Game.CAMP_Y + 35, Theme.TINY, Theme.SOFT);
        } else {
            gc.setFill(Color.rgb(125, 255, 154, 0.12));
            gc.fillRoundRect(Game.CAMP_X - 60, Game.CAMP_Y, Game.CAMP_W + 120, Game.CAMP_H, 14, 14);
            gc.setStroke(Color.rgb(125, 255, 154, 0.6));
            gc.setLineWidth(2);
            gc.strokeRoundRect(Game.CAMP_X - 60, Game.CAMP_Y, Game.CAMP_W + 120, Game.CAMP_H, 14, 14);
            Theme.center(gc, guest ? "Connect\u00e9 : vous \u00eates le joueur NOIR" : "Connect\u00e9 : vous \u00eates le joueur BLANC",
                    Game.W / 2, Game.CAMP_Y + 16, Theme.MED, Theme.GREEN);
            Theme.center(gc, guest ? "l'h\u00f4te choisit le niveau..." : "choisissez un niveau pour lancer la partie",
                    Game.W / 2, Game.CAMP_Y + 35, Theme.TINY, Theme.WHITE);
        }

        // onglets
        String[] tabs = { "1 JOUEUR", "2 JOUEURS" };
        for (int t = 0; t < 2; t++) {
            double tx = t == 0 ? Game.TAB0_X : Game.TAB1_X;
            boolean active = g.menuTab == t, hover = g.hoverTab == t;
            gc.setFill(active ? Color.rgb(120, 170, 255, 0.40) : Color.rgb(120, 170, 255, hover ? 0.22 : 0.08));
            gc.fillRoundRect(tx, Game.TAB_Y, Game.TAB_W, Game.TAB_H, 14, 14);
            gc.setStroke(active ? Color.web("#cfe0ff") : Color.rgb(160, 190, 255, hover ? 0.9 : 0.4));
            gc.setLineWidth(active ? 3 : 2);
            gc.strokeRoundRect(tx, Game.TAB_Y, Game.TAB_W, Game.TAB_H, 14, 14);
            Theme.center(gc, tabs[t], tx + Game.TAB_W / 2, Game.TAB_Y + Game.TAB_H / 2, Theme.MED,
                    active ? Theme.WHITE : Theme.SOFT);
        }

        int[] idx = g.tabLevels();
        int first = g.menuPage() * Game.PER_PAGE;
        for (int local = 0; local < Game.PER_PAGE && first + local < idx.length; local++) {
            int i = idx[first + local];
            Levels.Level lv = Levels.get(i);
            double cx = Game.cardX(local), cy = Game.cardY(local);
            boolean locked = guest && lv.twoPlayer;
            boolean hover = g.hoverCard == i && !locked;
            double hu = lv.twoPlayer ? 0 : (200 + i * 29) % 360;
            double sat = lv.twoPlayer ? 0.0 : 0.5; // les duels sont en blanc / gris
            if (locked) gc.setGlobalAlpha(0.45);
            gc.setFill(Color.hsb(hu, sat, 0.9, hover ? 0.38 : 0.16));
            gc.fillRoundRect(cx, cy, Game.CARD_W, Game.CARD_H, 12, 12);
            gc.setStroke(Color.hsb(hu, sat, 1, hover ? 1.0 : 0.55));
            gc.setLineWidth(hover ? 3 : 2);
            gc.strokeRoundRect(cx, cy, Game.CARD_W, Game.CARD_H, 12, 12);

            Theme.left(gc, "NIVEAU " + Levels.number(i), cx + 14, cy + 17, Theme.TINY, Color.hsb(hu, sat * 0.8, 1));
            Theme.leftFit(gc, lv.name, cx + 14, cy + 40, Theme.MED, Theme.WHITE, Game.CARD_W - 24);
            if (lv.twoPlayer) {
                Theme.left(gc, "Blanc contre Noir", cx + 14, cy + 61, Theme.TINY, Color.web("#9aa3d0"));
            } else {
                boolean has = g.levelBest[i] > 0;
                Theme.left(gc, "Meilleur : " + (has ? String.valueOf(g.levelBest[i]) : "-"), cx + 14, cy + 61,
                        Theme.TINY, has ? Theme.GOLD : Color.web("#6a74a8"));
            }
            gc.setGlobalAlpha(1);
        }

        if (g.pageCount() > 1) {
            arrow(gc, "<", Game.PREV_X, Game.PAGE_Y, g.menuPage() > 0, g.hoverPrev);
            arrow(gc, ">", Game.NEXT_X, Game.PAGE_Y, g.menuPage() < g.pageCount() - 1, g.hoverNext);
            Theme.center(gc, "Page " + (g.menuPage() + 1) + " / " + g.pageCount(), Game.W / 2,
                    Game.PAGE_Y + Game.PAGE_H / 2, Theme.SMALL, Theme.SOFT);
        }

        if (!g.netNotice.isEmpty() && g.time - g.netNoticeTime < 7) {
            Theme.center(gc, g.netNotice, Game.W / 2, 522, Theme.SMALL_BOLD, Color.web("#ffd24a"));
        } else if (duelTab && !connected) {
            Theme.center(gc, "Clic sur un niveau : duel \u00e0 deux sur ce clavier", Game.W / 2, 522, Theme.SMALL, Theme.SOFT);
        }
        button(gc, Game.INFO_X, Game.INFO_Y, Game.INFO_W, Game.INFO_H, g.hoverInfoBtn);
        Theme.center(gc, "INFORMATIONS", Game.W / 2, Game.INFO_Y + Game.INFO_H / 2, Theme.MED, Theme.WHITE);
        if (connected) {
            button(gc, Game.DISC_X, Game.DISC_Y, Game.DISC_W, Game.DISC_H, g.hoverDisc);
            Theme.center(gc, "D\u00e9connecter", Game.DISC_X + Game.DISC_W / 2, Game.DISC_Y + Game.DISC_H / 2, Theme.SMALL_BOLD, Theme.WHITE);
        }
    }

    static void button(GraphicsContext gc, double x, double y, double w, double h, boolean hover) {
        gc.setFill(hover ? Color.rgb(120, 170, 255, 0.35) : Color.rgb(120, 170, 255, 0.16));
        gc.fillRoundRect(x, y, w, h, 14, 14);
        gc.setStroke(Color.web(hover ? "#cfe0ff" : "#6f8de0"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, h, 14, 14);
    }

    static void arrow(GraphicsContext gc, String label, double x, double y, boolean enabled, boolean hover) {
        double a = enabled ? (hover ? 0.4 : 0.18) : 0.06;
        gc.setFill(Color.rgb(120, 170, 255, a));
        gc.fillRoundRect(x, y, Game.PAGE_W, Game.PAGE_H, 10, 10);
        gc.setStroke(Color.rgb(160, 190, 255, enabled ? (hover ? 1.0 : 0.6) : 0.2));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, Game.PAGE_W, Game.PAGE_H, 10, 10);
        Theme.center(gc, label, x + Game.PAGE_W / 2, y + Game.PAGE_H / 2, Theme.MED,
                enabled ? Theme.WHITE : Color.web("#4a5280"));
    }

    // ------------------------------------------------------------------ ecrans

    private void dim(GraphicsContext gc, double a) {
        gc.setFill(Color.rgb(4, 5, 13, a));
        gc.fillRect(0, 0, Game.W, Game.H);
    }

    private Color blink(Game g, Color c) {
        double a = 0.45 + 0.55 * (0.5 + 0.5 * Math.sin(g.time * 4));
        return Color.color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }

    private void drawOverlay(GraphicsContext gc, Game g) {
        switch (g.state) {
        case READY:
            if (g.stateTime < 1.8) {
                double a = Math.min(1, (1.8 - g.stateTime));
                Theme.center(gc, "NIVEAU " + Levels.number(g.level), Game.W / 2, 400, Theme.BIG, Color.color(1, 1, 1, a));
                Theme.center(gc, Levels.get(g.level).name, Game.W / 2, 445, Theme.MED, Color.color(0.67, 0.71, 0.93, a));
            }
            Theme.center(gc, "Clic / ESPACE pour lancer", Game.W / 2, Game.PADDLE_Y - 60, Theme.SMALL, blink(g, Theme.SOFT));
            break;
        case PLAYING:
            drawPlayingHints(gc, g);
            break;
        case PAUSED:
            drawPause(gc, g);
            break;
        case LEVEL_CLEAR:
            dim(gc, 0.55);
            Theme.center(gc, g.endedByGold ? "TOUT L'OR EST RAMASS\u00c9 !" : "NIVEAU TERMIN\u00c9 !", Game.W / 2, 250,
                    Theme.BIG, Color.hsb(140, 0.6, 1));
            Theme.center(gc, "Balles restantes : +" + g.levelBonus, Game.W / 2, 305, Theme.MED, Theme.WHITE);
            Theme.center(gc, "Score : " + g.score(), Game.W / 2, 340, Theme.MED, Theme.WHITE);
            Theme.center(gc, "ESPACE pour le niveau suivant", Game.W / 2, 420, Theme.SMALL, blink(g, Theme.SOFT));
            break;
        default:
            break;
        }
    }

    /** Pendant la partie : decompte du duel en grand, aide au lancement, etoile prete. */
    private void drawPlayingHints(GraphicsContext gc, Game g) {
        if (g.twoPlayer) {
            Paddle a = g.paddles[0], b = g.paddles[1];
            if (a.serveTimer > 0 && b.serveTimer > 0 && Math.abs(a.serveTimer - b.serveTimer) < 0.05) {
                countdown(gc, g, a.serveTimer, Game.W / 2, Game.H / 2 - 10, null);
            } else {
                if (a.serveTimer > 0) countdown(gc, g, a.serveTimer, Game.W * 0.27, Game.H / 2 - 10, a);
                if (b.serveTimer > 0) countdown(gc, g, b.serveTimer, Game.W * 0.73, Game.H / 2 - 10, b);
            }
            for (Paddle p : g.paddles) {
                double hx = p.index == 0 ? Game.W * 0.27 : Game.W * 0.73;
                if (g.netPlay && p.index != g.localIdx) continue; // en reseau, on n'affiche que ses propres consignes
                if (p.serveTimer <= 0 && hasStuck(g, p)) {
                    Theme.center(gc, g.netPlay ? "Clic : relancer" : (p.index == 0 ? "ESPACE : relancer" : "ENTR\u00c9E : relancer"),
                            hx, Game.H / 2 + 100, Theme.MED, blink(g, Color.WHITE));
                } else if (p.starTimer > 0 && p.serveTimer <= 0) {
                    Theme.center(gc, g.netPlay ? "Clic : \u00e9toile filante"
                            : (p.index == 0 ? "ESPACE : \u00e9toile filante" : "ENTR\u00c9E : \u00e9toile filante"), hx,
                            Game.H / 2 + 100, Theme.SMALL, blink(g, Color.rgb(255, 225, 90)));
                }
            }
        } else {
            Paddle p = g.paddles[0];
            if (hasStuck(g, p)) {
                Theme.center(gc, "Clic / ESPACE pour relancer", Game.W / 2, Game.PADDLE_Y - 60, Theme.SMALL, blink(g, Theme.SOFT));
            } else if (p.starTimer > 0) {
                Theme.center(gc, "Clic / ESPACE : \u00e9toile filante", Game.W / 2, Game.PADDLE_Y - 60, Theme.SMALL,
                        blink(g, Color.rgb(255, 225, 90)));
            }
        }
    }

    private boolean hasStuck(Game g, Paddle p) {
        for (Ball b : g.balls) {
            if (b.stuck && b.owner == p.index) return true;
        }
        return false;
    }

    /** Gros chiffre du decompte (3, 2, 1) qui se reduit a chaque seconde. */
    private void countdown(GraphicsContext gc, Game g, double timer, double cx, double cy, Paddle p) {
        int n = (int) Math.ceil(timer);
        double frac = timer - Math.floor(timer);
        if (frac == 0) frac = 1;
        double alpha = 0.35 + 0.65 * frac;
        Color fill = p == null ? Color.WHITE : (p.index == 0 ? Theme.PAPER : Theme.INK);
        Color line = p == null ? Theme.INK : (p.index == 0 ? Theme.INK : Color.web("#f4f4fb"));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Theme.HUGE);
        gc.setLineWidth(8);
        gc.setStroke(Color.color(line.getRed(), line.getGreen(), line.getBlue(), alpha));
        gc.setFill(Color.color(fill.getRed(), fill.getGreen(), fill.getBlue(), alpha));
        gc.strokeText(String.valueOf(n), cx, cy);
        gc.fillText(String.valueOf(n), cx, cy);
    }

    private void drawPause(GraphicsContext gc, Game g) {
        dim(gc, 0.72);
        Theme.center(gc, "PAUSE", Game.W / 2, 110, Theme.BIG, Theme.WHITE);
        gc.setFill(Color.rgb(120, 170, 255, 0.12));
        gc.fillRoundRect(110, 160, 740, 360, 18, 18);
        gc.setStroke(Color.rgb(160, 190, 255, 0.5));
        gc.setLineWidth(2);
        gc.strokeRoundRect(110, 160, 740, 360, 18, 18);

        if (!g.twoPlayer) {
            Theme.center(gc, "CONTR\u00d4LES - 1 JOUEUR", Game.W / 2, 190, Theme.MED, Theme.GOLD);
            Theme.left(gc, "D\u00e9placer la raquette", 150, 240, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.left(gc, "Souris, ou fl\u00e8ches gauche / droite, ou Q / D", 150, 264, Theme.SMALL, Theme.SOFT);
            Theme.left(gc, "Lancer, relancer (balle collante), tirer l'\u00e9toile filante", 150, 314, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.left(gc, "Clic ou ESPACE", 150, 338, Theme.SMALL, Theme.SOFT);
            Theme.left(gc, "Raquette : la balle repart selon l'endroit touch\u00e9", 150, 388, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.left(gc, "Touchez-la pr\u00e8s du bord pour un tir en biais", 150, 412, Theme.SMALL, Theme.SOFT);
        } else if (g.netPlay) {
            Theme.center(gc, "CONTR\u00d4LES - DUEL EN R\u00c9SEAU", Game.W / 2, 190, Theme.MED, Theme.GOLD);
            Theme.left(gc, "Vous \u00eates le joueur " + (g.localIdx == 0 ? "BLANC (gauche)" : "NOIR (droite)"), 150, 240, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.left(gc, "D\u00e9placer la raquette", 150, 290, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.left(gc, "Souris (haut / bas), ou fl\u00e8ches haut / bas, ou Z / S", 150, 314, Theme.SMALL, Theme.SOFT);
            Theme.left(gc, "Relancer (balle collante), tirer l'\u00e9toile filante", 150, 358, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.left(gc, "Clic ou ESPACE", 150, 382, Theme.SMALL, Theme.SOFT);
            Theme.left(gc, "La pause s'applique aux deux joueurs ; M quitte la partie pour les deux.", 150, 432, Theme.SMALL, Theme.SOFT);
        } else {
            Theme.center(gc, "CONTR\u00d4LES - 2 JOUEURS", Game.W / 2, 190, Theme.MED, Theme.GOLD);
            gc.setFill(Theme.PAPER);
            gc.fillRoundRect(140, 214, 320, 30, 15, 15);
            Theme.center(gc, "BLANC (gauche)", 300, 229, Theme.SMALL_BOLD, Theme.INK);
            gc.setFill(Theme.INK);
            gc.fillRoundRect(500, 214, 320, 30, 15, 15);
            gc.setStroke(Color.web("#f4f4fb"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(500, 214, 320, 30, 15, 15);
            Theme.center(gc, "NOIR (droite)", 660, 229, Theme.SMALL_BOLD, Theme.PAPER);
            Theme.left(gc, "Monter : Z (ou W)", 160, 272, Theme.SMALL, Theme.WHITE);
            Theme.left(gc, "Descendre : S", 160, 298, Theme.SMALL, Theme.WHITE);
            Theme.left(gc, "Relancer / \u00e9toile : ESPACE", 160, 324, Theme.SMALL, Theme.WHITE);
            Theme.left(gc, "Monter : fl\u00e8che haut", 520, 272, Theme.SMALL, Theme.WHITE);
            Theme.left(gc, "Descendre : fl\u00e8che bas", 520, 298, Theme.SMALL, Theme.WHITE);
            Theme.left(gc, "Relancer / \u00e9toile : ENTR\u00c9E", 520, 324, Theme.SMALL, Theme.WHITE);
            Theme.center(gc, "Chacun a sa balle et " + Rules.LIVES + " vies ; elle part toute seule apr\u00e8s le d\u00e9compte.", Game.W / 2, 378, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.center(gc, "Balle sortie de votre c\u00f4t\u00e9 : une vie en moins.", Game.W / 2, 402, Theme.SMALL, Theme.SOFT);
            Theme.center(gc, "Balle sortie du c\u00f4t\u00e9 adverse : l'adversaire perd " + Rules.DUEL_PENALTY + " points.", Game.W / 2, 424, Theme.SMALL, Theme.SOFT);
        }
        Theme.center(gc, "P ou \u00c9chap : reprendre     M : menu     N : son     F11 : plein \u00e9cran", Game.W / 2, 480,
                Theme.SMALL_BOLD, Theme.WHITE);
    }

    // ------------------------------------------------------------------ tableau des scores

    private double ease(double x) {
        double c = Math.max(0, Math.min(1, x));
        return 1 - (1 - c) * (1 - c);
    }

    private String fmt(int v) {
        return String.valueOf(v);
    }

    private void drawResults(GraphicsContext gc, Game g) {
        boolean duel = g.twoPlayer;
        gc.setFill(Color.rgb(4, 5, 13, 0.82));
        gc.fillRect(0, 0, Game.W, Game.H);

        Theme.center(gc, g.resultTitle, Game.W / 2, 50, Theme.BIG, Theme.GOLD);
        if (!g.resultNote.isEmpty()) {
            if (g.eliminatedMask != 0) Theme.center(gc, g.resultNote, Game.W / 2, 88, Theme.SMALL_BOLD, Color.rgb(255, 150, 150));
            else Theme.center(gc, g.resultNote, Game.W / 2, 88, Theme.SMALL, Theme.SOFT);
        }

        int n = g.resLines;
        double rowH = duel ? 42 : 46;
        double y0 = duel ? 160 : 135;
        double bx = duel ? 110 : 190, bw = duel ? 740 : 580;

        if (duel) {
            gc.setFill(Theme.PAPER);
            gc.fillRoundRect(120, 106, 230, 32, 16, 16);
            Theme.center(gc, "BLANC", 235, 122, Theme.HUD, Theme.INK);
            gc.setFill(Theme.INK);
            gc.fillRoundRect(610, 106, 230, 32, 16, 16);
            gc.setStroke(Color.web("#f4f4fb"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(610, 106, 230, 32, 16, 16);
            Theme.center(gc, "NOIR", 725, 122, Theme.HUD, Theme.PAPER);
        }

        for (int i = 0; i < n && i < g.resRevealed; i++) {
            double ti = Game.RES_START + i * Game.RES_STEP;
            double age = g.stateTime - ti;
            double y = y0 + i * rowH;
            double prog = ease(age / 0.55);
            gc.setFill(Color.rgb(255, 255, 255, 0.05 + 0.20 * Math.max(0, 1 - age / 0.6)));
            gc.fillRoundRect(bx, y - 18, bw, 36, 10, 10);
            if (duel) {
                Theme.center(gc, Game.CAT_NAMES[i], Game.W / 2, y, Theme.SMALL, Theme.SOFT);
                value(gc, g.paddles[0].pts[i], prog, 345, y, TextAlignment.RIGHT);
                value(gc, g.paddles[1].pts[i], prog, 615, y, TextAlignment.LEFT);
            } else {
                Theme.left(gc, Game.CAT_NAMES[i], bx + 24, y, Theme.SMALL, Theme.SOFT);
                value(gc, g.paddles[0].pts[i], prog, bx + bw - 24, y, TextAlignment.RIGHT);
            }
        }

        if (g.resTotalShown) {
            double age = g.stateTime - g.resultsTotalTime();
            double prog = ease(age / 0.9);
            double pop = 1 + 0.25 * Math.max(0, 1 - age / 0.35);
            double y = y0 + n * rowH + 22;
            gc.setStroke(Color.rgb(255, 210, 74, 0.8));
            gc.setLineWidth(2);
            gc.strokeLine(bx, y - 28, bx + bw, y - 28);
            if (duel) {
                Theme.center(gc, "TOTAL", Game.W / 2, y, Theme.HUD, Theme.GOLD);
                bigValue(gc, g.paddles[0].score, prog, pop, 345, y, TextAlignment.RIGHT);
                bigValue(gc, g.paddles[1].score, prog, pop, 615, y, TextAlignment.LEFT);
            } else {
                Theme.left(gc, "TOTAL", bx + 24, y, Theme.HUD, Theme.GOLD);
                bigValue(gc, g.paddles[0].score, prog, pop, bx + bw - 24, y, TextAlignment.RIGHT);
            }
        }

        if (g.resFinalShown) {
            double pulse = 0.5 + 0.5 * Math.sin(g.time * 6);
            double y = y0 + n * rowH + 92;
            if (duel) {
                String t = g.winner == 0 ? "BLANC GAGNE !" : (g.winner == 1 ? "NOIR GAGNE !" : "\u00c9GALIT\u00c9 !");
                Color c = g.winner == 1 ? Color.web("#f4f4fb") : Color.hsb(50, 0.5 + 0.3 * pulse, 1);
                if (g.winner >= 0) {
                    gc.setFill(g.winner == 0 ? Theme.PAPER : Theme.INK);
                    gc.fillRoundRect(Game.W / 2 - 230, y - 32, 460, 64, 32, 32);
                    gc.setStroke(g.winner == 0 ? Theme.INK : Color.web("#f4f4fb"));
                    gc.setLineWidth(3);
                    gc.strokeRoundRect(Game.W / 2 - 230, y - 32, 460, 64, 32, 32);
                    c = g.winner == 0 ? Theme.INK : Theme.PAPER;
                }
                Theme.center(gc, t, Game.W / 2, y, Theme.BIG, c);
            } else if (g.newRecord) {
                Theme.center(gc, "NOUVEAU RECORD !", Game.W / 2, y, Theme.BIG, Color.hsb(50, 0.45 + 0.4 * pulse, 1));
            } else {
                int best = g.campaign ? g.highScore : g.levelBest[g.level];
                Theme.center(gc, "Record : " + best, Game.W / 2, y, Theme.MED, Theme.SOFT);
            }
        }

        if (!g.resultsAllShown()) Theme.center(gc, "ESPACE : passer", Game.W / 2, 612, Theme.SMALL, Theme.DIM);
        else Theme.center(gc, "ESPACE : continuer", Game.W / 2, 612, Theme.SMALL, blink(g, Theme.SOFT));

        if (g.elimBannerActive()) drawElimination(gc, g);
    }

    /** Duel : "X n'a plus de balles" - arrive en dernier, avec flash rouge, zoom et secousse. */
    private void drawElimination(GraphicsContext gc, Game g) {
        double age = g.stateTime - g.resultsElimStart();
        double fade = Math.max(0, Math.min(1, (g.resultsElimEnd() - g.stateTime) / 0.3)); // disparait en fondu
        gc.save();
        gc.setGlobalAlpha(fade);
        double zoom = 1 + 2.4 * (1 - ease(age / 0.4));
        double flash = Math.max(0, 1 - age / 0.6);
        gc.setFill(Color.rgb(255, 0, 20, 0.40 * flash));
        gc.fillRect(-20, -20, Game.W + 40, Game.H + 40);
        gc.setFill(Color.rgb(70, 0, 10, 0.94));
        gc.fillRect(-20, 262, Game.W + 40, 160);
        double pulse = 0.5 + 0.5 * Math.sin(g.time * 9);
        gc.setStroke(Color.rgb(255, 70, 70, 0.6 + 0.4 * pulse));
        gc.setLineWidth(4);
        gc.strokeLine(-20, 262, Game.W + 20, 262);
        gc.strokeLine(-20, 422, Game.W + 20, 422);

        int m = g.eliminatedMask;
        String text = m == 1 ? "BLANC N'A PLUS DE BALLES !" : (m == 2 ? "NOIR N'A PLUS DE BALLES !" : "PLUS DE BALLES POUR LES DEUX !");
        gc.save();
        gc.translate(Game.W / 2, 332);
        gc.scale(zoom, zoom);
        Theme.center(gc, text, 0, 0, Theme.BIG, Color.WHITE);
        gc.restore();

        String sub;
        if (m == 3) {
            sub = "Plus de balles pour personne : le plus de points l'emporte";
        } else {
            int loser = m == 1 ? 0 : 1;
            boolean morePoints = g.paddles[loser].score > g.paddles[1 - loser].score;
            sub = morePoints ? "D\u00e9faite ! Sans balle on perd, m\u00eame avec plus de points" : "D\u00e9faite : plus aucune balle";
        }
        double a = ease((age - 0.3) / 0.4);
        Theme.center(gc, sub, Game.W / 2, 388, Theme.MED, Color.rgb(255, 205, 205, a));
        gc.restore();
    }

    private void value(GraphicsContext gc, int finalVal, double prog, double x, double y, TextAlignment a) {
        int v = (int) Math.round(finalVal * prog);
        Color c = v < 0 ? Theme.RED : (v == 0 ? Theme.DIM : Theme.WHITE);
        Theme.text(gc, fmt(v), x, y, Theme.MED, c, a);
    }

    private void bigValue(GraphicsContext gc, int finalVal, double prog, double pop, double x, double y, TextAlignment a) {
        int v = (int) Math.round(finalVal * prog);
        Color c = v < 0 ? Theme.RED : Theme.GOLD;
        gc.save();
        gc.translate(x, y);
        gc.scale(pop, pop);
        Theme.text(gc, fmt(v), 0, 0, Theme.BIG, c, a);
        gc.restore();
    }
}
