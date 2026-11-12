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

    private final LinearGradient bg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#10173d")), new Stop(1, Color.web("#04050d")));

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

        gc.setFill(bg);
        gc.fillRect(0, 0, Game.W, Game.H);
        drawStars(gc, g.time);

        if (g.state == Game.State.MENU) {
            drawMenu(gc, g);
        } else {
            gc.save();
            if (g.shake > 0.1) {
                gc.translate((Math.random() - 0.5) * g.shake, (Math.random() - 0.5) * g.shake);
            }
            gc.setStroke(Color.web("#33407f"));
            gc.setLineWidth(2);
            gc.strokeLine(0, Game.TOP, Game.W, Game.TOP);

            for (Brick k : g.bricks) {
                if (k.alive) drawBrick(gc, k, g.time);
            }
            for (Beam b : g.beams) drawBeam(gc, b);
            drawPaddle(gc, g);
            for (PowerUp p : g.powerUps) drawPowerUp(gc, p);
            for (Ball b : g.balls) drawBall(gc, b, g.fireTimer > 0);
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
        case BONUS: return Color.hsb(k.bonus.hue, 0.8, 0.95);
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
        case BONUS:
            gc.setStroke(Color.rgb(255, 255, 255, 0.45 + 0.35 * Math.sin(t * 5)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, 8, 8);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(fMed);
            gc.setFill(Color.web("#10142e"));
            gc.fillText(k.bonus.label, cx, cy + 1);
            break;
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
                ys[i] = Game.TOP + (Game.PADDLE_Y - Game.TOP) * i / n;
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
            gc.fillRect(0, b.pos - Game.BH / 2, Game.W, Game.BH);
            for (int i = 0; i <= n; i++) {
                xs[i] = Game.W * i / n;
                ys[i] = b.pos + ((i == 0 || i == n) ? 0 : (r.nextDouble() - 0.5) * 14);
            }
            gc.setStroke(Color.rgb(70, 45, 28, a));
            gc.setLineWidth(3);
            gc.strokePolyline(xs, ys, n + 1);
        }
    }

    private void drawPaddle(GraphicsContext gc, Game g) {
        double w = g.paddleW, x = g.paddleX - w / 2, y = Game.PADDLE_Y, h = Game.PADDLE_H;
        boolean fire = g.fireTimer > 0;
        boolean sticky = g.stickyTimer > 0;
        gc.setFill(fire ? Color.rgb(255, 150, 60, 0.20) : Color.rgb(90, 160, 255, 0.18));
        gc.fillRoundRect(x - 6, y - 6, w + 12, h + 12, 20, 20);
        gc.setFill(fire ? Color.web("#c9722f") : Color.web("#4f7fd6"));
        gc.fillRoundRect(x, y + 2, w, h, 12, 12);
        gc.setFill(fire ? Color.web("#ffb060") : Color.web("#9cc2ff"));
        gc.fillRoundRect(x, y, w, h - 2, 12, 12);
        gc.setFill(Color.rgb(255, 255, 255, 0.35));
        gc.fillRoundRect(x + 6, y + 2, w - 12, 4, 4, 4);
        if (sticky) { // dessus gluant
            gc.setFill(Color.hsb(60, 0.8, 1, 0.9));
            gc.fillRoundRect(x + 2, y - 3, w - 4, 5, 5, 5);
        }
    }

    private void drawBall(GraphicsContext gc, Ball b, boolean fire) {
        int n = b.trailSize();
        for (int i = 0; i < n; i++) {
            double f = (i + 1) / (double) n;
            double r = Game.BALL_R * (0.35 + 0.65 * f);
            gc.setFill(fire ? Color.rgb(255, 140, 40, f * 0.35) : Color.rgb(150, 200, 255, f * 0.30));
            gc.fillOval(b.trailX(i) - r, b.trailY(i) - r, r * 2, r * 2);
        }
        double r = Game.BALL_R;
        gc.setFill(fire ? Color.rgb(255, 120, 30, 0.18) : Color.rgb(170, 210, 255, 0.15));
        gc.fillOval(b.x - r * 2.2, b.y - r * 2.2, r * 4.4, r * 4.4);
        gc.setFill(fire ? Color.web("#ffd27a") : Color.WHITE);
        gc.fillOval(b.x - r, b.y - r, r * 2, r * 2);
    }

    private void drawPowerUp(GraphicsContext gc, PowerUp p) {
        double bob = Math.sin(p.t * 8) * 1.5;
        Color c = Color.hsb(p.type.hue, 0.8, 0.95);
        gc.setFill(Color.rgb(255, 255, 255, 0.12));
        gc.fillRoundRect(p.x - 21, p.y - 15 + bob, 42, 30, 30, 30);
        gc.setFill(c);
        gc.fillRoundRect(p.x - 16, p.y - 10 + bob, 32, 20, 20, 20);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(fTiny);
        gc.setFill(Color.web("#10142e"));
        gc.fillText(p.type.label, p.x, p.y + bob + 1);
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
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(fHud);
        gc.setFill(WHITE);
        gc.fillText("SCORE  " + g.score, 20, 20);
        gc.setFont(fTiny);
        gc.setFill(Color.web("#8a94c8"));
        long best = g.campaign ? g.highScore : g.levelBest[g.level];
        gc.fillText("RECORD  " + best, 20, 42);

        int m = g.multiplier();
        if (m > 1) {
            gc.setFont(fHud);
            gc.setFill(Color.hsb(50, 0.9, 1));
            gc.fillText("x" + m, 250, 24);
        }

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(fSmall);
        gc.setFill(SOFT);
        gc.fillText("NIVEAU " + (g.level + 1) + "  -  " + Levels.NAMES[g.level], Game.W / 2, 28);

        for (int i = 0; i < g.lives; i++) {
            double x = Game.W - 30 - i * 24;
            gc.setFill(Color.rgb(170, 210, 255, 0.2));
            gc.fillOval(x - 11, 17, 22, 22);
            gc.setFill(Color.WHITE);
            gc.fillOval(x - 7, 21, 14, 14);
        }

        double y = Game.H - 14;
        y = timerBar(gc, "LARGE", g.wideTimer, 12, 20, y, 140);
        y = timerBar(gc, "RALENTI", g.slowTimer, 8, 20, y, 270);
        y = timerBar(gc, "COLLANT", g.stickyTimer, 15, 20, y, 60);
        timerBar(gc, "FEU", g.fireTimer, 6, 20, y, 25);

        gc.setTextAlign(TextAlignment.RIGHT);
        double ry = Game.H - 14;
        if (g.goldTotal > 0) {
            gc.setFont(fMed);
            gc.setFill(GOLD);
            gc.fillText("OR  " + (g.goldTotal - g.goldRemaining) + " / " + g.goldTotal, Game.W - 20, ry);
            ry -= 24;
        }
        if (g.muted) {
            gc.setFont(fTiny);
            gc.setFill(Color.web("#8a94c8"));
            gc.fillText("SON COUP\u00c9 (S)", Game.W - 20, ry);
        }
    }

    private double timerBar(GraphicsContext gc, String label, double t, double max, double x, double y, double hue) {
        if (t <= 0) return y;
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFont(fTiny);
        gc.setFill(Color.hsb(hue, 0.6, 1));
        gc.fillText(label, x, y);
        gc.setFill(Color.rgb(255, 255, 255, 0.15));
        gc.fillRoundRect(x + 70, y - 4, 100, 8, 8, 8);
        gc.setFill(Color.hsb(hue, 0.8, 1));
        gc.fillRoundRect(x + 70, y - 4, 100 * Math.min(1, t / max), 8, 8, 8);
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
        center(gc, "niveaux encha\u00een\u00e9s - record : " + g.highScore, Game.CAMP_Y + 38, fTiny, GOLD);

        center(gc, "ou choisissez directement un niveau", 195, fSmall, SOFT);

        for (int i = 0; i < Levels.NAMES.length; i++) {
            double cx = Game.cardX(i), cy = Game.cardY(i);
            boolean hover = g.hoverCard == i;
            double hu = (200 + i * 29) % 360;
            gc.setFill(Color.hsb(hu, 0.5, 0.9, hover ? 0.38 : 0.16));
            gc.fillRoundRect(cx, cy, Game.CARD_W, Game.CARD_H, 12, 12);
            gc.setStroke(Color.hsb(hu, 0.5, 1, hover ? 1.0 : 0.55));
            gc.setLineWidth(hover ? 3 : 2);
            gc.strokeRoundRect(cx, cy, Game.CARD_W, Game.CARD_H, 12, 12);

            gc.setTextAlign(TextAlignment.LEFT);
            gc.setTextBaseline(VPos.CENTER);
            gc.setFont(fTiny);
            gc.setFill(Color.hsb(hu, 0.4, 1));
            gc.fillText("NIVEAU " + (i + 1), cx + 14, cy + 18);
            gc.setFont(fMed);
            gc.setFill(WHITE);
            gc.fillText(Levels.NAMES[i], cx + 14, cy + 42);
            gc.setFont(fTiny);
            gc.setFill(g.levelBest[i] > 0 ? GOLD : Color.web("#6a74a8"));
            gc.fillText("Meilleur : " + (g.levelBest[i] > 0 ? String.valueOf(g.levelBest[i]) : "-"), cx + 14, cy + 64);
        }

        center(gc, "Souris ou fl\u00e8ches / Q-D : raquette   |   Clic / ESPACE : lancer   |   P : pause   |   S : son   |   F11 : plein \u00e9cran",
                520, fTiny, SOFT);
        center(gc, "Bonus : L = large   M = multi-balles   S = ralenti   F = feu   C = collante   + = vie", 542, fTiny, SOFT);
        center(gc, "Les briques bonus (lettre) activent leur bonus d\u00e8s qu'on les touche", 564, fTiny, SOFT);
        center(gc, "\u00c9clair = colonne   Roche = ligne   Or = niveau fini quand tout l'or est pris   Bouton = fait basculer les briques cyan",
                586, fTiny, SOFT);
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
                center(gc, Levels.NAMES[g.level], 445, fMed, Color.color(0.67, 0.71, 0.93, a));
            }
            center(gc, "Clic / ESPACE pour lancer", Game.PADDLE_Y - 60, fSmall, blink(g, SOFT));
            break;
        case PLAYING:
            for (Ball b : g.balls) {
                if (b.stuck) {
                    center(gc, "Clic / ESPACE pour relancer", Game.PADDLE_Y - 60, fSmall, blink(g, SOFT));
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
            dim(gc, 0.45);
            center(gc, g.endedByGold ? "TOUT L'OR EST RAMASS\u00c9 !" : "NIVEAU TERMIN\u00c9 !", 250, fBig, Color.hsb(140, 0.6, 1));
            center(gc, "Bonus vies : +" + g.levelBonus, 305, fMed, WHITE);
            center(gc, "Score : " + g.score, 340, fMed, WHITE);
            if (g.newRecord) center(gc, "Nouveau record !", 375, fMed, Color.hsb(50, 0.8, 1));
            center(gc, g.campaign ? "ESPACE pour le niveau suivant" : "ESPACE pour revenir au menu", 420, fSmall, blink(g, SOFT));
            break;
        case GAME_OVER:
            dim(gc, 0.7);
            center(gc, "PARTIE TERMIN\u00c9E", 240, fBig, Color.hsb(5, 0.7, 1));
            center(gc, "Score : " + g.score, 305, fHud, WHITE);
            if (g.newRecord) center(gc, "Nouveau record !", 345, fMed, Color.hsb(50, 0.8, 1));
            center(gc, "ESPACE pour revenir au menu", 410, fSmall, blink(g, SOFT));
            break;
        case WIN:
            dim(gc, 0.6);
            center(gc, "VICTOIRE !", 230, fTitle, Color.hsb(hue(g.time * 80), 0.7, 1));
            center(gc, "Score final : " + g.score, 310, fHud, WHITE);
            if (g.newRecord) center(gc, "Nouveau record !", 350, fMed, Color.hsb(50, 0.8, 1));
            center(gc, "ESPACE pour revenir au menu", 420, fSmall, blink(g, SOFT));
            break;
        default:
            break;
        }
    }
}
