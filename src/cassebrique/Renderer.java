package cassebrique;

import java.util.Random;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Dessin du jeu. Travaille dans le repere logique 960x640, mis a l'echelle de la fenetre. */
final class Renderer {
    private static final Color OUTSIDE = Color.web("#04050d");
    private static final Color WHITE = Color.web("#e8ecff");
    private static final Color SOFT = Color.web("#aab4ee");
    private static final Color GOLD = Color.web("#ffd24a");
    private static final Color INK = Color.web("#14141c");   // "noir" du joueur noir
    private static final Color PAPER = Color.web("#ffffff"); // "blanc" du joueur blanc

    private final LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#10173d")), new Stop(1, Color.web("#04050d")));
    // Fond gris moyen du duel : le blanc et le noir y ressortent tous les deux
    private final LinearGradient duelBg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#8d92a3")), new Stop(1, Color.web("#686d7f")));

    private final Font fTitle = Font.font("System", FontWeight.BOLD, 76);
    private final Font fBig = Font.font("System", FontWeight.BOLD, 44);
    private final Font fHud = Font.font("System", FontWeight.BOLD, 22);
    private final Font fMed = Font.font("System", FontWeight.BOLD, 20);
    private final Font fSmall = Font.font("System", FontWeight.NORMAL, 16);
    private final Font fTiny = Font.font("System", FontWeight.BOLD, 13);

    private final double[] starX = new double[90], starY = new double[90], starS = new double[90];

    Renderer() {
        Random r = new Random(42);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = r.nextDouble() * Game.W;
            starY[i] = r.nextDouble() * Game.H;
            starS[i] = 0.5 + r.nextDouble() * 1.5;
        }
    }

    void render(GraphicsContext gc, Game g, double w, double h) {
        gc.setFill(OUTSIDE);
        gc.fillRect(0, 0, w, h);

        double s = Math.min(w / Game.W, h / Game.H);
        gc.save();
        gc.translate((w - Game.W * s) / 2, (h - Game.H * s) / 2);
        gc.scale(s, s);
        gc.beginPath();
        gc.rect(0, 0, Game.W, Game.H);
        gc.clip();

        boolean duel = g.twoPlayer && g.state != Game.State.MENU;
        gc.setFill(duel ? duelBg : bg);
        gc.fillRect(0, 0, Game.W, Game.H);
        if (!duel) drawStars(gc, g.time);

        if (g.state == Game.State.MENU) {
            drawMenu(gc, g);
        } else {
            gc.save();
            if (g.shake > 0.1) {
                gc.translate((Math.random() - 0.5) * g.shake, (Math.random() - 0.5) * g.shake);
            }
            gc.setStroke(duel ? Color.web("#2a2f45") : Color.web("#33407f"));
            gc.setLineWidth(2);
            gc.strokeLine(0, Game.TOP, Game.W, Game.TOP);

            for (Brick k : g.bricks) {
                if (k.alive) drawBrick(gc, k, g.time);
            }
            for (Beam b : g.beams) drawBeam(gc, b);
            for (Paddle p : g.paddles) {
                if (p.wallTimer > 0) drawWall(gc, g, p);
            }
            for (Paddle p : g.paddles) drawPaddle(gc, g, p);
            for (PowerUp u : g.powerUps) drawPowerUp(gc, u);
            for (Ball b : g.balls) drawBall(gc, g, b);
            drawParticles(gc, g);
            gc.restore();

            drawHud(gc, g);
            drawOverlay(gc, g);
        }
        gc.restore();
    }

    // ------------------------------------------------------------------ decor

    private void drawStars(GraphicsContext gc, double t) {
        for (int i = 0; i < starX.length; i++) {
            double y = (starY[i] + t * 6 * starS[i]) % Game.H;
            double a = 0.25 + 0.5 * Math.abs(Math.sin(t * starS[i] + i));
            gc.setFill(Color.rgb(255, 255, 255, a));
            gc.fillOval(starX[i], y, starS[i], starS[i]);
        }
    }

    private Color baseColor(Brick k) {
        switch (k.kind) {
        case STEEL: return Color.web("#7b8394");
        case EXPLOSIVE: return Color.hsb(8, 0.85, 0.95);
        case GOLD: return Color.web("#ffd24a");
        case LIGHTNING: return Color.web("#2a3270");
        case ROCK: return Color.web("#8a6a4a");
        case BUTTON: return Color.web("#4a4f66");
        case PHASE: return Color.web("#3fb8c9");
        case BONUS: return k.bonus.bad ? Color.web("#3a1018") : Color.hsb(k.bonus.hue, 0.8, 0.95);
        default: return Color.hsb(k.hue(), 0.75, 0.95);
        }
    }

    private void drawBrick(GraphicsContext gc, Brick k, double t) {
        double x = k.x, y = k.y, w = Game.BW, h = Game.BH;
        double cx = x + w / 2, cy = y + h / 2 - 1;

        if (k.kind == Brick.Kind.PHASE && !k.phaseSolid) { // fantome : indique ou la brique reapparaitra
            gc.setStroke(Color.rgb(63, 184, 201, 0.30 + Math.min(0.4, k.flash * 2)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, 8, 8);
            return;
        }

        Color base = baseColor(k);
        if (k.flash > 0) base = base.interpolate(Color.WHITE, Math.min(1, k.flash * 6));
        gc.setFill(base.darker().darker());
        gc.fillRoundRect(x, y + 2, w, h, 8, 8);
        gc.setFill(base);
        gc.fillRoundRect(x, y, w, h - 2, 8, 8);
        gc.setFill(Color.rgb(255, 255, 255, 0.28));
        gc.fillRoundRect(x + 3, y + 2, w - 6, 6, 6, 6);

        switch (k.kind) {
        case EXPLOSIVE:
            gc.setFill(Color.web("#3b0a00"));
            gc.fillOval(cx - 7, cy - 7, 14, 14);
            gc.setFill(Color.YELLOW);
            gc.fillOval(cx - 3, cy - 3, 6, 6);
            break;
        case STEEL:
            gc.setFill(Color.rgb(40, 45, 60, 0.7));
            gc.fillOval(x + 5, y + 5, 4, 4);
            gc.fillOval(x + w - 9, y + 5, 4, 4);
            gc.fillOval(x + 5, y + h - 11, 4, 4);
            gc.fillOval(x + w - 9, y + h - 11, 4, 4);
            break;
        case GOLD: {
            gc.setStroke(Color.web("#a87a00"));
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(x + 0.75, y + 0.75, w - 1.5, h - 3.5, 8, 8);
            double sp = (t * 1.3 + k.col * 0.37 + k.row * 0.71) % 3.0; // petit scintillement
            if (sp < 0.4) {
                double sx = x + 6 + (w - 12) * (sp / 0.4);
                gc.setStroke(Color.rgb(255, 255, 255, 0.9));
                gc.setLineWidth(2);
                gc.strokeLine(sx - 4, cy, sx + 4, cy);
                gc.strokeLine(sx, cy - 4, sx, cy + 4);
            }
            break;
        }
        case LIGHTNING:
            gc.setStroke(Color.web("#ffe44d"));
            gc.setLineWidth(3);
            gc.strokePolyline(new double[] { cx - 4, cx + 3, cx - 3, cx + 4 },
                    new double[] { cy - 8, cy - 2, cy + 1, cy + 8 }, 4);
            break;
        case ROCK:
            gc.setFill(Color.web("#4a3322"));
            gc.fillPolygon(new double[] { cx - 9, cx - 3, cx + 7, cx + 10, cx + 4, cx - 6 },
                    new double[] { cy + 2, cy - 7, cy - 6, cy + 2, cy + 8, cy + 8 }, 6);
            gc.fillPolygon(new double[] { x + 6, x + 14, x + 14 }, new double[] { cy, cy - 5, cy + 5 }, 3);
            gc.fillPolygon(new double[] { x + w - 6, x + w - 14, x + w - 14 }, new double[] { cy, cy - 5, cy + 5 }, 3);
            break;
        case BUTTON:
            gc.setFill(Color.web("#262a3c"));
            gc.fillOval(cx - 10, cy - 8, 20, 17);
            gc.setFill(k.flash > 0 ? Color.web("#ff8a8a") : Color.web("#c0222c"));
            gc.fillOval(cx - 7, cy - 6, 14, 12);
            break;
        case PHASE:
            gc.setStroke(Color.rgb(255, 255, 255, 0.55));
            gc.setLineWidth(2);
            gc.strokeLine(x + 8, y + h - 7, x + 20, y + 5);
            gc.strokeLine(x + 24, y + h - 7, x + 36, y + 5);
            gc.strokeLine(x + 40, y + h - 7, x + 52, y + 5);
            break;
        case BONUS: {
            boolean bad = k.bonus.bad;
            double a = 0.45 + 0.35 * Math.sin(t * 5);
            gc.setStroke(bad ? Color.rgb(255, 70, 70, 0.5 + 0.4 * Math.sin(t * 6)) : Color.rgb(255, 255, 255, a));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, 8, 8);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(fMed);
            gc.setFill(bad ? Color.web("#ff7070") : Color.web("#10142e"));
            gc.fillText(k.bonus.label, cx, cy + 1);
            break;
        }
        default:
            break;
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
            gc.fillRect(b.a, b.pos - Game.BH / 2, b.b - b.a, Game.BH);
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
                glow = Color.rgb(255, 255, 255, 0.30);
                body = PAPER;
                light = Color.rgb(120, 120, 140, 0.35);
                edge = INK;
            } else {            // noir
                glow = Color.rgb(0, 0, 0, 0.28);
                body = INK;
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
        gc.setFill(edge);
        gc.fillRoundRect(x - 2, y - 2, w + 4, h + 4, arc + 2, arc + 2);
        gc.setFill(body);
        gc.fillRoundRect(x, y, w, h, arc, arc);
        gc.setFill(light);
        if (p.vertical) gc.fillRoundRect(x + 3, y + 6, 4, h - 12, 4, 4);
        else gc.fillRoundRect(x + 6, y + 3, w - 12, 4, 4, 4);

        if (fire && g.twoPlayer) { // lisere orange
            gc.setStroke(Color.web("#ff9a3c"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x - 3, y - 3, w + 6, h + 6, arc + 3, arc + 3);
        }
        if (p.shrinkTimer > 0) { // malus : contour rouge qui pulse
            gc.setStroke(Color.rgb(255, 50, 50, 0.55 + 0.4 * Math.sin(g.time * 10)));
            gc.setLineWidth(3);
            gc.strokeRoundRect(x - 4, y - 4, w + 8, h + 8, arc + 4, arc + 4);
        }
        if (p.stickyTimer > 0) { // face gluante
            double s0 = p.toward > 0 ? p.face - 3 : p.face - 2;
            gc.setFill(Color.hsb(60, 0.8, 1, 0.95));
            if (p.vertical) gc.fillRoundRect(s0, y + 2, 5, h - 4, 5, 5);
            else gc.fillRoundRect(x + 2, s0, w - 4, 5, 5, 5);
        }
    }

    private void drawBall(GraphicsContext gc, Game g, Ball b) {
        Paddle o = g.paddles[Math.min(b.owner, g.paddles.length - 1)];
        boolean fire = o.fireTimer > 0;
        boolean soft = o.nobreakTimer > 0;
        boolean duel = g.twoPlayer;
        boolean black = duel && b.owner == 1;

        Color trail, glow, body, ring = null;
        if (soft) {
            trail = Color.rgb(140, 147, 168, 1);
            glow = Color.rgb(255, 60, 60, 0.18);
            body = Color.web("#8c93a8");
            ring = Color.rgb(255, 60, 60, 0.6 + 0.4 * Math.sin(g.time * 12));
        } else if (fire) {
            trail = Color.rgb(255, 140, 40, 1);
            glow = Color.rgb(255, 120, 30, 0.18);
            body = Color.web("#ffd27a");
        } else if (duel) {
            trail = black ? Color.rgb(20, 20, 28, 1) : Color.rgb(255, 255, 255, 1);
            glow = black ? Color.rgb(0, 0, 0, 0.18) : Color.rgb(255, 255, 255, 0.28);
            body = black ? INK : PAPER;
            ring = black ? Color.web("#f4f4fb") : INK;
        } else {
            trail = Color.rgb(150, 200, 255, 1);
            glow = Color.rgb(170, 210, 255, 0.15);
            body = Color.WHITE;
        }

        int n = b.trailSize();
        for (int i = 0; i < n; i++) {
            double f = (i + 1) / (double) n;
            double r = Game.BALL_R * (0.35 + 0.65 * f);
            gc.setFill(Color.color(trail.getRed(), trail.getGreen(), trail.getBlue(), f * 0.32));
            gc.fillOval(b.trailX(i) - r, b.trailY(i) - r, r * 2, r * 2);
        }
        double r = Game.BALL_R;
        gc.setFill(glow);
        gc.fillOval(b.x - r * 2.2, b.y - r * 2.2, r * 4.4, r * 4.4);
        if (ring != null) {
            gc.setFill(ring);
            gc.fillOval(b.x - r - 2, b.y - r - 2, r * 2 + 4, r * 2 + 4);
        }
        gc.setFill(body);
        gc.fillOval(b.x - r, b.y - r, r * 2, r * 2);
    }

    private void drawPowerUp(GraphicsContext gc, PowerUp u) {
        double bob = Math.sin(u.t * 8) * 1.5;
        gc.setFill(Color.rgb(255, 255, 255, 0.12));
        gc.fillRoundRect(u.x - 21, u.y - 15 + bob, 42, 30, 30, 30);
        if (u.type.bad) { // malus : fond sombre, contour rouge
            gc.setFill(Color.web("#3a1018"));
            gc.fillRoundRect(u.x - 16, u.y - 10 + bob, 32, 20, 20, 20);
            gc.setStroke(Color.rgb(255, 70, 70, 0.7 + 0.3 * Math.sin(u.t * 10)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(u.x - 16, u.y - 10 + bob, 32, 20, 20, 20);
        } else {
            gc.setFill(Color.hsb(u.type.hue, 0.8, 0.95));
            gc.fillRoundRect(u.x - 16, u.y - 10 + bob, 32, 20, 20, 20);
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(fTiny);
        gc.setFill(u.type.bad ? Color.web("#ff7070") : Color.web("#10142e"));
        gc.fillText(u.type.label, u.x, u.y + bob + 1);
    }

    private void drawParticles(GraphicsContext gc, Game g) {
        for (Particle p : g.particles) {
            double a = Math.max(0, Math.min(1, p.life / p.maxLife));
            gc.setFill(Color.hsb(hue(p.hue), 0.7, 1.0, a));
            gc.fillRect(p.x, p.y, p.size, p.size);
        }
    }

    private static double hue(double h) {
        return ((h % 360) + 360) % 360;
    }

    // ------------------------------------------------------------------ HUD

    private void drawHud(GraphicsContext gc, Game g) {
        gc.setTextBaseline(VPos.CENTER);
        if (g.twoPlayer) drawHudDuel(gc, g);
        else drawHudSolo(gc, g);

        // Compteur d'or et son coupe
        gc.setTextAlign(TextAlignment.RIGHT);
        if (g.goldTotal > 0) {
            gc.setFont(fMed);
            gc.setFill(g.twoPlayer ? Color.web("#fff0a0") : GOLD);
            String t = "OR  " + (g.goldTotal - g.goldRemaining) + " / " + g.goldTotal;
            if (g.twoPlayer) {
                gc.setTextAlign(TextAlignment.CENTER);
                strokeText(gc, t, Game.W / 2, Game.H - 14);
            } else {
                gc.fillText(t, Game.W - 20, Game.H - 14);
            }
        }
        if (g.muted) {
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFont(fTiny);
            gc.setFill(Color.web("#8a94c8"));
            gc.fillText("SON COUP\u00c9 (N)", Game.W - 20, 49);
        }
    }

    /** Texte avec lisere sombre : lisible sur le fond gris du duel. */
    private void strokeText(GraphicsContext gc, String t, double x, double y) {
        gc.setStroke(Color.rgb(20, 22, 40, 0.9));
        gc.setLineWidth(3);
        gc.strokeText(t, x, y);
        gc.fillText(t, x, y);
    }

    private void drawHudSolo(GraphicsContext gc, Game g) {
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(fHud);
        gc.setFill(WHITE);
        gc.fillText("SCORE  " + g.score(), 20, 20);
        gc.setFont(fTiny);
        gc.setFill(Color.web("#8a94c8"));
        int best = g.campaign ? g.highScore : g.levelBest[g.level];
        gc.fillText("RECORD  " + best, 20, 42);

        int m = g.multiplier(0);
        if (m > 1) {
            gc.setFont(fHud);
            gc.setFill(Color.hsb(50, 0.9, 1));
            gc.fillText("x" + m, 250, 24);
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(fSmall);
        gc.setFill(SOFT);
        gc.fillText("NIVEAU " + (g.level + 1) + "  -  " + Levels.get(g.level).name, Game.W / 2, 28);

        for (int i = 0; i < g.lives; i++) {
            double x = Game.W - 30 - i * 24;
            gc.setFill(Color.rgb(170, 210, 255, 0.2));
            gc.fillOval(x - 11, 17, 22, 22);
            gc.setFill(Color.WHITE);
            gc.fillOval(x - 7, 21, 14, 14);
        }

        double y = timers(gc, g.paddles[0], 20, Game.H - 14);
        timerBar(gc, "RALENTI", g.slowTimer, 8, 20, y, 270, 0.8);
    }

    private void drawHudDuel(GraphicsContext gc, Game g) {
        gc.setFill(Color.rgb(10, 12, 30, 0.80)); // bandeau sombre derriere les scores
        gc.fillRect(0, 0, Game.W, Game.TOP);

        pill(gc, g, 0, 20, "BLANC");
        pill(gc, g, 1, Game.W - 20 - 200, "NOIR");

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(fSmall);
        gc.setFill(SOFT);
        gc.fillText("NIVEAU " + (g.level + 1) + "  -  " + Levels.get(g.level).name, Game.W / 2, 20);
        gc.setFont(fTiny);
        gc.setFill(Color.web("#8a94c8"));
        gc.fillText("DUEL", Game.W / 2, 40);

        timers(gc, g.paddles[0], 20, Game.H - 14);
        timers(gc, g.paddles[1], Game.W - 20 - 192, Game.H - 14);
        timerBar(gc, "RALENTI", g.slowTimer, 8, Game.W / 2 - 96, Game.H - 40, 270, 0.8);
    }

    private void pill(GraphicsContext gc, Game g, int player, double x, String name) {
        boolean white = player == 0;
        gc.setFill(white ? PAPER : INK);
        gc.fillRoundRect(x, 12, 200, 32, 16, 16);
        gc.setStroke(white ? INK : Color.web("#f4f4fb"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, 12, 200, 32, 16, 16);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(fHud);
        gc.setFill(white ? INK : PAPER);
        gc.fillText(name + "  " + g.paddles[player].score, x + 14, 29);
        int m = g.multiplier(player);
        if (m > 1) {
            gc.setFont(fHud);
            gc.setFill(Color.hsb(50, 0.9, 1));
            gc.setTextAlign(white ? TextAlignment.LEFT : TextAlignment.RIGHT);
            gc.fillText("x" + m, white ? x + 210 : x - 10, 29);
        }
    }

    /** Barres de duree des effets d'une raquette ; renvoie le y suivant disponible. */
    private double timers(GraphicsContext gc, Paddle p, double x, double y) {
        y = timerBar(gc, "LARGE", p.wideTimer, 12, x, y, 140, 0.8);
        y = timerBar(gc, "PETIT", p.shrinkTimer, 10, x, y, 0, 0.9);
        y = timerBar(gc, "COLLANT", p.stickyTimer, 15, x, y, 60, 0.8);
        y = timerBar(gc, "FEU", p.fireTimer, 6, x, y, 25, 0.8);
        y = timerBar(gc, "MUR", p.wallTimer, 8, x, y, 300, 0.6);
        y = timerBar(gc, "INOFFENSIF", p.nobreakTimer, 5, x, y, 0, 0.9);
        return y;
    }

    private double timerBar(GraphicsContext gc, String label, double t, double max, double x, double y,
            double hue, double sat) {
        if (t <= 0) return y;
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(fTiny);
        gc.setFill(Color.hsb(hue, 0.5, 1));
        gc.fillText(label, x, y);
        gc.setFill(Color.rgb(255, 255, 255, 0.15));
        gc.fillRoundRect(x + 92, y - 4, 100, 8, 8, 8);
        gc.setFill(Color.hsb(hue, sat, 1));
        gc.fillRoundRect(x + 92, y - 4, 100 * Math.min(1, t / max), 8, 8, 8);
        return y - 20;
    }

    // ------------------------------------------------------------------ menu

    private void drawMenu(GraphicsContext gc, Game g) {
        center(gc, "CASSE-BRIQUE", 72, fTitle, Color.hsb(hue(200 + Math.sin(g.time) * 40), 0.7, 1));

        boolean hc = g.hoverCampaign;
        gc.setFill(hc ? Color.rgb(120, 170, 255, 0.35) : Color.rgb(120, 170, 255, 0.16));
        gc.fillRoundRect(Game.CAMP_X, Game.CAMP_Y, Game.CAMP_W, Game.CAMP_H, 14, 14);
        gc.setStroke(Color.web(hc ? "#cfe0ff" : "#6f8de0"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(Game.CAMP_X, Game.CAMP_Y, Game.CAMP_W, Game.CAMP_H, 14, 14);
        center(gc, "CAMPAGNE  (ESPACE)", Game.CAMP_Y + 18, fMed, WHITE);
        center(gc, "niveaux solo encha\u00een\u00e9s - record : " + g.highScore, Game.CAMP_Y + 38, fTiny, GOLD);

        center(gc, "ou choisissez directement un niveau", 195, fSmall, SOFT);

        int first = g.menuPage * Game.PER_PAGE;
        for (int local = 0; local < Game.PER_PAGE && first + local < Levels.count(); local++) {
            int i = first + local;
            Levels.Level lv = Levels.get(i);
            double cx = Game.cardX(local), cy = Game.cardY(local);
            boolean hover = g.hoverCard == i;
            double hu = lv.twoPlayer ? 0 : (200 + i * 29) % 360;
            double sat = lv.twoPlayer ? 0.0 : 0.5; // les duels sont en gris/blanc
            gc.setFill(Color.hsb(hu, sat, 0.9, hover ? 0.38 : 0.16));
            gc.fillRoundRect(cx, cy, Game.CARD_W, Game.CARD_H, 12, 12);
            gc.setStroke(Color.hsb(hu, sat, 1, hover ? 1.0 : 0.55));
            gc.setLineWidth(hover ? 3 : 2);
            gc.strokeRoundRect(cx, cy, Game.CARD_W, Game.CARD_H, 12, 12);

            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(fTiny);
            gc.setFill(Color.hsb(hu, sat * 0.8, 1));
            gc.fillText("NIVEAU " + (i + 1), cx + 14, cy + 18);
            if (lv.twoPlayer) {
                gc.setFill(PAPER);
                gc.fillRoundRect(cx + Game.CARD_W - 92, cy + 9, 80, 18, 9, 9);
                gc.setFill(INK);
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText("2 JOUEURS", cx + Game.CARD_W - 52, cy + 18);
                gc.setTextAlign(TextAlignment.LEFT);
            }
            gc.setFont(fMed);
            gc.setFill(WHITE);
            gc.fillText(lv.name, cx + 14, cy + 42);
            gc.setFont(fTiny);
            if (lv.twoPlayer) {
                gc.setFill(Color.web("#9aa3d0"));
                gc.fillText("Blanc contre Noir", cx + 14, cy + 64);
            } else {
                gc.setFill(g.levelBest[i] > 0 ? GOLD : Color.web("#6a74a8"));
                gc.fillText("Meilleur : " + (g.levelBest[i] > 0 ? String.valueOf(g.levelBest[i]) : "-"), cx + 14, cy + 64);
            }
        }

        if (g.pageCount() > 1) {
            arrow(gc, "<", Game.PREV_X, g.menuPage > 0, g.hoverPrev);
            arrow(gc, ">", Game.NEXT_X, g.menuPage < g.pageCount() - 1, g.hoverNext);
            center(gc, "Page " + (g.menuPage + 1) + " / " + g.pageCount(), Game.PAGE_Y + Game.PAGE_H / 2, fSmall, SOFT);
        }

        center(gc, "Souris ou fl\u00e8ches / Q-D : raquette   |   Clic / ESPACE : lancer   |   P : pause   |   N : son   |   F11 : plein \u00e9cran",
                530, fTiny, SOFT);
        center(gc, "Bonus : L = large   M = multi-balles   S = ralenti   F = feu   C = collante   = = mur de secours   + = vie",
                548, fTiny, SOFT);
        center(gc, "Malus (fond sombre) : - = raquette r\u00e9duite   \u00d7 = balle inoffensive   |   Les briques-lettres agissent d\u00e8s qu'on les touche",
                566, fTiny, SOFT);
        center(gc, "\u00c9clair = colonne   Roche = ligne   Or = niveau fini quand tout l'or est pris   Bouton = fait basculer les briques cyan",
                584, fTiny, SOFT);
        center(gc, "Duel : Blanc (gauche) Z/S + ESPACE   |   Noir (droite) fl\u00e8ches haut/bas + ENTR\u00c9E   |   balle rat\u00e9e = points perdus",
                602, fTiny, SOFT);
        center(gc, "Menu : fl\u00e8ches gauche/droite ou boutons pour changer de page", 620, fTiny, Color.web("#7a84b8"));
    }

    private void arrow(GraphicsContext gc, String label, double x, boolean enabled, boolean hover) {
        double a = enabled ? (hover ? 0.4 : 0.18) : 0.06;
        gc.setFill(Color.rgb(120, 170, 255, a));
        gc.fillRoundRect(x, Game.PAGE_Y, Game.PAGE_W, Game.PAGE_H, 10, 10);
        gc.setStroke(Color.rgb(160, 190, 255, enabled ? (hover ? 1.0 : 0.6) : 0.2));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, Game.PAGE_Y, Game.PAGE_W, Game.PAGE_H, 10, 10);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(fMed);
        gc.setFill(enabled ? WHITE : Color.web("#4a5280"));
        gc.fillText(label, x + Game.PAGE_W / 2, Game.PAGE_Y + Game.PAGE_H / 2);
    }

    // ------------------------------------------------------------------ ecrans

    private void dim(GraphicsContext gc, double a) {
        gc.setFill(Color.rgb(4, 5, 13, a));
        gc.fillRect(0, 0, Game.W, Game.H);
    }

    private void center(GraphicsContext gc, String s, double y, Font f, Color c) {
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(f);
        gc.setFill(c);
        gc.fillText(s, Game.W / 2, y);
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
                center(gc, "NIVEAU " + (g.level + 1), 400, fBig, Color.color(1, 1, 1, a));
                center(gc, Levels.get(g.level).name, 445, fMed, Color.color(0.67, 0.71, 0.93, a));
            }
            if (g.twoPlayer) {
                boolean white = g.server == 0;
                String who = white ? "BLANC sert : ESPACE" : "NOIR sert : ENTR\u00c9E";
                Color c = white ? PAPER : INK;
                gc.setFill(Color.rgb(white ? 20 : 240, white ? 22 : 240, white ? 40 : 250, 0.8));
                gc.fillRoundRect(Game.W / 2 - 150, Game.H - 86, 300, 34, 17, 17);
                center(gc, who, Game.H - 69, fMed, blink(g, c));
            } else {
                center(gc, "Clic / ESPACE pour lancer", Game.PADDLE_Y - 60, fSmall, blink(g, SOFT));
            }
            break;
        case PLAYING:
            for (Ball b : g.balls) {
                if (b.stuck) {
                    String t = g.twoPlayer ? "ESPACE (blanc) / ENTR\u00c9E (noir) pour relancer" : "Clic / ESPACE pour relancer";
                    center(gc, t, g.twoPlayer ? Game.H - 69 : Game.PADDLE_Y - 60, fSmall, blink(g, g.twoPlayer ? WHITE : SOFT));
                    break;
                }
            }
            break;
        case PAUSED:
            dim(gc, 0.65);
            center(gc, "PAUSE", 280, fBig, WHITE);
            center(gc, "P ou clic pour reprendre", 335, fSmall, SOFT);
            center(gc, "M : retour au menu", 365, fSmall, SOFT);
            break;
        case LEVEL_CLEAR:
            dim(gc, 0.55);
            if (g.twoPlayer) {
                String title = g.winner == 0 ? "BLANC GAGNE !" : (g.winner == 1 ? "NOIR GAGNE !" : "\u00c9GALIT\u00c9 !");
                center(gc, title, 250, fBig, Color.hsb(50, 0.5, 1));
                center(gc, "Blanc : " + g.paddles[0].score + "     Noir : " + g.paddles[1].score, 315, fHud, WHITE);
                center(gc, "ESPACE pour revenir au menu", 390, fSmall, blink(g, SOFT));
                break;
            }
            center(gc, g.endedByGold ? "TOUT L'OR EST RAMASS\u00c9 !" : "NIVEAU TERMIN\u00c9 !", 250, fBig, Color.hsb(140, 0.6, 1));
            center(gc, "Bonus vies : +" + g.levelBonus, 305, fMed, WHITE);
            center(gc, "Score : " + g.score(), 340, fMed, WHITE);
            if (g.newRecord) center(gc, "Nouveau record !", 375, fMed, Color.hsb(50, 0.8, 1));
            center(gc, g.campaign ? "ESPACE pour le niveau suivant" : "ESPACE pour revenir au menu", 420, fSmall, blink(g, SOFT));
            break;
        case GAME_OVER:
            dim(gc, 0.7);
            center(gc, "PARTIE TERMIN\u00c9E", 240, fBig, Color.hsb(5, 0.7, 1));
            center(gc, "Score : " + g.score(), 305, fHud, WHITE);
            if (g.newRecord) center(gc, "Nouveau record !", 345, fMed, Color.hsb(50, 0.8, 1));
            center(gc, "ESPACE pour revenir au menu", 410, fSmall, blink(g, SOFT));
            break;
        case WIN:
            dim(gc, 0.6);
            center(gc, "VICTOIRE !", 230, fTitle, Color.hsb(hue(g.time * 80), 0.7, 1));
            center(gc, "Score final : " + g.score(), 310, fHud, WHITE);
            if (g.newRecord) center(gc, "Nouveau record !", 350, fMed, Color.hsb(50, 0.8, 1));
            center(gc, "ESPACE pour revenir au menu", 420, fSmall, blink(g, SOFT));
            break;
        default:
            break;
        }
    }
}
