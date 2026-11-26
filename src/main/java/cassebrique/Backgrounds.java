package cassebrique;

import java.util.Random;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

/**
 * Les decors de fond. Theme 0 = decor classique (solo : ciel etoile ; duel : nebuleuses), 1 = Mars vue de l'espace,
 * 2 = aurore boreale. Tout ce qui est fixe est dessine UNE fois dans une image au demarrage : pendant le jeu, on ne
 * fait que recopier des images et faire scintiller quelques etoiles (beaucoup moins cher que de redessiner des degrades).
 */
final class Backgrounds {
    private static final double W = Game.W, H = Game.H;
    /** Definition des images pre-calculees, selon la taille de la fenetre (1 pixel d'image = 1 pixel d'ecran, sans redimensionnement couteux). */
    private static final double[] SCALES = { 1.0, 1.5, 2.0 };

    static int scaleClass(double winScale) {
        return winScale < 1.25 ? 0 : (winScale < 1.75 ? 1 : 2);
    }

    private interface Painter {
        void paint(GraphicsContext gc);
    }

    private final LinearGradient soloBg = new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#10173d")), new Stop(1, Color.web("#04050d")));
    private final LinearGradient duelLeft = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(255, 255, 255, 0.22)), new Stop(1, Color.rgb(255, 255, 255, 0)));
    private final LinearGradient duelRight = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(20, 0, 50, 0)), new Stop(1, Color.rgb(20, 0, 50, 0.50)));

    private final Image blobBlue, blobViolet, curtainA, glowCyan, glowPink, glowGreen;
    private final Image[] duelBase = new Image[3], mars = new Image[3], aurora = new Image[3], ocean = new Image[3];

    private static final double HZ = 330; // horizon de l'ocean
    private static final int BIO = 38;
    private final double[] bioX = new double[BIO], bioY = new double[BIO], bioP = new double[BIO], bioS = new double[BIO];
    private final Color[] waveColors = new Color[9];

    private final double[] starX = new double[90], starY = new double[90], starS = new double[90];
    private final double[] star2X = new double[70], star2Y = new double[70], star2S = new double[70];

    Backgrounds() {
        Random r = new Random(42);
        for (int i = 0; i < starX.length; i++) {
            starX[i] = r.nextDouble() * W;
            starY[i] = r.nextDouble() * H;
            starS[i] = 0.5 + r.nextDouble() * 1.5;
        }
        Random r2 = new Random(7);
        for (int i = 0; i < star2X.length; i++) {
            star2X[i] = r2.nextDouble() * W;
            star2Y[i] = r2.nextDouble() * H;
            star2S[i] = 1.2 + r2.nextDouble() * 2.2;
        }
        glowCyan = sprite(13, Color.rgb(90, 255, 235, 0.95));
        glowPink = sprite(13, Color.rgb(255, 110, 230, 0.95));
        glowGreen = sprite(13, Color.rgb(150, 255, 120, 0.95));
        Random r3 = new Random(99);
        for (int i = 0; i < BIO; i++) {
            bioX[i] = 20 + r3.nextDouble() * (W - 40);
            bioY[i] = HZ + 40 + r3.nextDouble() * (H - HZ - 70);
            bioP[i] = r3.nextDouble() * 6.28;
            bioS[i] = 0.5 + r3.nextDouble() * 1.4;
        }
        for (int i = 0; i < waveColors.length; i++) {
            double f = i / 8.0;
            waveColors[i] = Color.rgb(130, 255, 240, 0.07 + 0.10 * f);
        }
        blobBlue = sprite(330, Color.rgb(90, 150, 255, 0.14));
        blobViolet = sprite(330, Color.rgb(200, 90, 255, 0.12));
        curtainA = renderSized(1200, 380, 1.0, new Painter() {
            public void paint(GraphicsContext gc) {
                paintCurtain(gc, 5, 140, 0.0);
            }
        });
    }

    // ------------------------------------------------------------------ pre-calcul

    private static Image render(double w, double h, int cls, Painter p) {
        return renderSized(w, h, SCALES[cls], p);
    }

    private Image duelBase(int cls) {
        if (duelBase[cls] == null) {
            duelBase[cls] = render(W, H, cls, new Painter() {
                public void paint(GraphicsContext gc) {
                    // degrade + les trois nebuleuses a leur place de depart, le tout dans UNE image
                    gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#25398a")), new Stop(1, Color.web("#0d1334"))));
                    gc.fillRect(0, 0, W, H);
                    blob(gc, W * 0.25, H * 0.45, 330, Color.rgb(90, 150, 255, 0.32));
                    blob(gc, W * 0.75, H * 0.55, 330, Color.rgb(200, 90, 255, 0.28));
                    blob(gc, W * 0.5, H * 0.5, 430, Color.rgb(60, 210, 230, 0.12));
                }
            });
        }
        return duelBase[cls];
    }

    private Image mars(int cls) {
        if (mars[cls] == null) {
            mars[cls] = render(W, H, cls, new Painter() {
                public void paint(GraphicsContext gc) {
                    paintMars(gc);
                }
            });
        }
        return mars[cls];
    }

    private Image ocean(int cls) {
        if (ocean[cls] == null) {
            ocean[cls] = render(W, H, cls, new Painter() {
                public void paint(GraphicsContext gc) {
                    paintOcean(gc);
                }
            });
        }
        return ocean[cls];
    }

    /** Fabrique, a raison d'une image par appel, les decors pas encore prets (appele pendant le menu : aucun a-coup en jeu). */
    void warmUp(double winScale) {
        int cls = scaleClass(winScale);
        if (mars[cls] == null) mars(cls);
        else if (aurora[cls] == null) aurora(cls);
        else if (ocean[cls] == null) ocean(cls);
        else if (duelBase[cls] == null) duelBase(cls);
    }

    private Image aurora(int cls) {
        if (aurora[cls] == null) {
            aurora[cls] = render(W, H, cls, new Painter() {
                public void paint(GraphicsContext gc) {
                    paintAurora(gc);
                }
            });
        }
        return aurora[cls];
    }

    private static Image renderSized(double w, double h, double scale, Painter p) {
        Canvas c = new Canvas(w * scale, h * scale);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.scale(scale, scale);
        p.paint(gc);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }

    private static Image sprite(final double r, final Color c) {
        return renderSized(2 * r, 2 * r, 1.0, new Painter() {
            public void paint(GraphicsContext gc) {
                blob(gc, r, r, r, c);
            }
        });
    }

    private static void blob(GraphicsContext gc, double cx, double cy, double r, Color c) {
        gc.setFill(new RadialGradient(0, 0, cx, cy, r, false, CycleMethod.NO_CYCLE,
                new Stop(0, c), new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0))));
        gc.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    /** Mars vue de l'espace : grande planete rouille dont on ne voit que l'horizon courbe, eclairee par la gauche. */
    private static void paintMars(GraphicsContext gc) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#02030a")), new Stop(1, Color.web("#0c0a16"))));
        gc.fillRect(0, 0, W, H);
        Random r = new Random(11);
        for (int i = 0; i < 280; i++) {
            double a = 0.15 + 0.6 * r.nextDouble() * r.nextDouble();
            gc.setFill(Color.rgb(255, 245, 235, a));
            double s = 0.6 + r.nextDouble() * 1.3;
            gc.fillOval(r.nextDouble() * W, r.nextDouble() * 520, s, s);
        }
        blob(gc, 40, 20, 480, Color.rgb(255, 210, 160, 0.10)); // lumiere du Soleil, en haut a gauche

        double R = 900, cx = W * 0.5, cy = 450 + R;
        gc.save();
        gc.beginPath();
        gc.arc(cx, cy, R, R, 0, 360);
        gc.closePath();
        gc.clip();
        gc.setFill(new LinearGradient(0, 0, W, 0, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#d27846")), new Stop(0.5, Color.web("#a64c2b")), new Stop(1, Color.web("#41170f"))));
        gc.fillRect(0, 400, W, 400);
        // grandes plaques sombres et claires (douces)
        for (int i = 0; i < 150; i++) {
            double x = -60 + r.nextDouble() * (W + 120), y = 440 + r.nextDouble() * 260;
            double rad = 25 + r.nextDouble() * 95;
            boolean dark = r.nextDouble() < 0.6;
            gc.save();
            gc.translate(x, y);
            gc.scale(1, 0.32 + r.nextDouble() * 0.25);
            blob(gc, 0, 0, rad, dark ? Color.rgb(70, 24, 12, 0.10 + 0.14 * r.nextDouble())
                    : Color.rgb(235, 160, 105, 0.07 + 0.10 * r.nextDouble()));
            gc.restore();
        }
        // cratères
        for (int i = 0; i < 26; i++) {
            double x = r.nextDouble() * W, y = 470 + r.nextDouble() * 170, rad = 5 + r.nextDouble() * 20;
            double sq = 0.45;
            gc.setFill(Color.rgb(60, 20, 10, 0.22));
            gc.fillOval(x - rad, y - rad * sq, rad * 2, rad * 2 * sq);
            gc.setStroke(Color.rgb(255, 200, 150, 0.30));
            gc.setLineWidth(1.4);
            gc.strokeArc(x - rad, y - rad * sq, rad * 2, rad * 2 * sq, 20, 150, javafx.scene.shape.ArcType.OPEN);
        }
        // canyon (Valles Marineris) et sa lueur
        double[] xs = new double[40], ys = new double[40];
        for (int i = 0; i < 40; i++) {
            xs[i] = 90 + i * 15;
            ys[i] = 560 + 18 * Math.sin(i * 0.33) + i * 0.7;
        }
        gc.setStroke(Color.rgb(60, 20, 10, 0.12));
        gc.setLineWidth(15);
        gc.strokePolyline(xs, ys, 40);
        gc.setStroke(Color.rgb(50, 16, 8, 0.45));
        gc.setLineWidth(5);
        gc.strokePolyline(xs, ys, 40);
        // trainees de poussiere
        for (int i = 0; i < 40; i++) {
            double x = r.nextDouble() * W, y = 450 + r.nextDouble() * 190, len = 80 + r.nextDouble() * 220;
            gc.setStroke(Color.rgb(255, 210, 170, 0.05));
            gc.setLineWidth(1 + r.nextDouble() * 2);
            gc.strokeLine(x, y, x + len, y - len * 0.03);
        }
        // assombrissement vers le limbe
        gc.setFill(new RadialGradient(0, 0, cx, cy, R, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0, 0, 0, 0.0)), new Stop(0.90, Color.rgb(0, 0, 0, 0.05)),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.5))));
        gc.fillRect(0, 400, W, 400);
        gc.restore();
        // atmosphere : halo orange puis fin liseré bleu sur le limbe
        for (int i = 0; i < 9; i++) {
            gc.setStroke(Color.rgb(255, 150, 95, 0.035));
            gc.setLineWidth(4 + i * 4);
            gc.strokeOval(cx - R, cy - R, 2 * R, 2 * R);
        }
        gc.setStroke(Color.rgb(150, 195, 255, 0.5));
        gc.setLineWidth(2.5);
        gc.strokeOval(cx - R, cy - R, 2 * R, 2 * R);
        // le tout un peu assombri pour que les briques et la balle ressortent
        gc.setFill(Color.rgb(2, 3, 10, 0.18));
        gc.fillRect(0, 0, W, H);
    }

    private static void paintAurora(GraphicsContext gc) {
        gc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#030612")), new Stop(0.65, Color.web("#08202b")), new Stop(1, Color.web("#0b2f33"))));
        gc.fillRect(0, 0, W, H);
        Random r = new Random(23);
        for (int i = 0; i < 230; i++) {
            double a = 0.12 + 0.6 * r.nextDouble() * r.nextDouble();
            gc.setFill(Color.rgb(235, 245, 255, a));
            double s = 0.6 + r.nextDouble() * 1.4;
            gc.fillOval(r.nextDouble() * W, r.nextDouble() * 480, s, s);
        }
        blob(gc, W * 0.5, 430, 520, Color.rgb(60, 255, 170, 0.10));
        gc.save(); // second rideau (turquoise / rose), fixe : seul le premier rideau est anime
        gc.translate(-110, 55);
        gc.setGlobalAlpha(0.65);
        paintCurtain(gc, 9, 178, 1.7);
        gc.restore();
        // montagnes lointaines puis proches
        double[] fx = new double[62], fy = new double[62];
        for (int i = 0; i < 62; i++) {
            fx[i] = i == 0 ? 0 : (i == 61 ? W : (i - 0.5) * W / 60);
        }
        for (int i = 1; i < 61; i++) {
            double x = fx[i];
            fy[i] = 520 + 38 * Math.sin(x * 0.011 + 1) + 24 * Math.sin(x * 0.029) + 10 * Math.sin(x * 0.083);
        }
        fy[0] = fy[1];
        fy[61] = fy[60];
        gc.setFill(Color.web("#0a1a28"));
        gc.fillPolygon(concat(fx, new double[] { W, 0 }), concat(fy, new double[] { H, H }), 64);
        // neige legere sur les cretes lointaines
        gc.setStroke(Color.rgb(170, 230, 230, 0.18));
        gc.setLineWidth(1.5);
        gc.strokePolyline(fx, fy, 62);
        for (int i = 1; i < 61; i++) {
            double x = fx[i];
            fy[i] = 578 + 26 * Math.sin(x * 0.017 + 2) + 15 * Math.sin(x * 0.05) + 6 * Math.sin(x * 0.13);
        }
        fy[0] = fy[1];
        fy[61] = fy[60];
        gc.setFill(Color.web("#050b13"));
        gc.fillPolygon(concat(fx, new double[] { W, 0 }), concat(fy, new double[] { H, H }), 64);
        // lac gele : reflet vert tres leger au pied des montagnes
        blob(gc, W * 0.5, 600, 380, Color.rgb(60, 255, 170, 0.07));
    }

    /** Ocean d'une planete inconnue, de nuit : grosse planete a anneaux, petite lune, eau sombre aux reflets roses et turquoise. */
    private static void paintOcean(GraphicsContext gc) {
        gc.setFill(new LinearGradient(0, 0, 0, HZ, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#05030f")), new Stop(0.55, Color.web("#1a0c38")), new Stop(1, Color.web("#12505c"))));
        gc.fillRect(0, 0, W, HZ);
        Random r = new Random(31);
        for (int i = 0; i < 210; i++) {
            gc.setFill(Color.rgb(240, 235, 255, 0.15 + 0.6 * r.nextDouble() * r.nextDouble()));
            double s = 0.6 + r.nextDouble() * 1.4;
            gc.fillOval(r.nextDouble() * W, r.nextDouble() * (HZ - 10), s, s);
        }
        blob(gc, 235, HZ, 430, Color.rgb(255, 120, 220, 0.20));
        blob(gc, 720, HZ, 380, Color.rgb(60, 230, 200, 0.14));

        // petite lune verte
        blob(gc, 770, 120, 95, Color.rgb(160, 255, 225, 0.16));
        gc.setFill(new RadialGradient(225, 0.5, 770, 120, 24, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#e6fff4")), new Stop(1, Color.web("#5ea896"))));
        gc.fillOval(746, 96, 48, 48);
        gc.setFill(Color.rgb(40, 90, 80, 0.35));
        gc.fillOval(758, 108, 9, 7);
        gc.fillOval(774, 122, 12, 9);
        gc.fillOval(756, 126, 6, 5);

        // aiguilles rocheuses a l'horizon
        gc.setFill(Color.web("#090418"));
        double[] spx = { 560, 590, 612, 640, 668, 690, 720, 760, 800, 845, 880 };
        double[] sph = { 22, 54, 30, 70, 26, 44, 18, 62, 28, 40, 20 };
        for (int i = 0; i < spx.length; i++) {
            gc.fillPolygon(new double[] { spx[i] - 9, spx[i], spx[i] + 9 }, new double[] { HZ + 2, HZ - sph[i], HZ + 2 }, 3);
        }

        // grosse planete annelee (anneaux inclines : moitie arriere, planete, moitie avant)
        double pcx = 235, pcy = 205, pr = 100;
        gc.save();
        gc.translate(pcx, pcy);
        gc.rotate(-18);
        double[][] rings = { { 205, 46, 7, 0.30 }, { 172, 38, 4, 0.40 }, { 232, 54, 3, 0.22 } };
        for (double[] rg : rings) {
            gc.setStroke(Color.rgb(255, 200, 250, rg[3]));
            gc.setLineWidth(rg[2]);
            gc.strokeArc(-rg[0], -rg[1], 2 * rg[0], 2 * rg[1], 0, 180, javafx.scene.shape.ArcType.OPEN);
        }
        gc.restore();
        gc.setFill(new RadialGradient(225, 0.55, pcx, pcy, pr, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffc2ff")), new Stop(0.5, Color.web("#b552d8")), new Stop(1, Color.web("#2d0d5c"))));
        gc.fillOval(pcx - pr, pcy - pr, 2 * pr, 2 * pr);
        gc.save();
        gc.beginPath();
        gc.arc(pcx, pcy, pr, pr, 0, 360);
        gc.closePath();
        gc.clip();
        for (int i = 0; i < 9; i++) { // bandes
            gc.setStroke(i % 2 == 0 ? Color.rgb(255, 225, 255, 0.16) : Color.rgb(70, 20, 120, 0.20));
            gc.setLineWidth(5 + (i % 3) * 3);
            double yy = pcy - pr + 14 + i * 22;
            gc.strokeLine(pcx - pr, yy, pcx + pr, yy + 8);
        }
        gc.setFill(new RadialGradient(0, 0, pcx - 40, pcy - 45, 2.1 * pr, false, CycleMethod.NO_CYCLE, // ombre douce, cote nuit
                new Stop(0, Color.rgb(5, 0, 25, 0)), new Stop(0.5, Color.rgb(5, 0, 25, 0)), new Stop(1, Color.rgb(5, 0, 25, 0.70))));
        gc.fillRect(pcx - pr, pcy - pr, 2 * pr, 2 * pr);
        gc.restore();
        gc.save();
        gc.translate(pcx, pcy);
        gc.rotate(-18);
        for (double[] rg : rings) { // moitie avant des anneaux, devant la planete
            gc.setStroke(Color.rgb(255, 205, 250, rg[3] + 0.28));
            gc.setLineWidth(rg[2]);
            gc.strokeArc(-rg[0], -rg[1], 2 * rg[0], 2 * rg[1], 180, 180, javafx.scene.shape.ArcType.OPEN);
        }
        gc.restore();

        // l'ocean
        gc.setFill(new LinearGradient(0, HZ, 0, H, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0e5f6b")), new Stop(0.22, Color.web("#0a3a4f")), new Stop(1, Color.web("#020c15"))));
        gc.fillRect(0, HZ, W, H - HZ);
        gc.save(); // reflet allonge de la planete
        gc.translate(pcx, HZ);
        gc.scale(0.5, 2.7);
        blob(gc, 0, 0, 125, Color.rgb(255, 150, 235, 0.30));
        gc.restore();
        gc.setStroke(Color.rgb(170, 255, 240, 0.38));
        gc.setLineWidth(1.5);
        gc.strokeLine(0, HZ, W, HZ);
    }

    private static double[] concat(double[] a, double[] b) {
        double[] r = new double[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    /** Un rideau d'aurore : de fines bandes verticales dont la hauteur et la position ondulent. */
    private static void paintCurtain(GraphicsContext gc, double step, double hueBase, double phase) {
        double cw = 1200;
        for (double x = 0; x < cw; x += step) {
            double base = 235 + 70 * Math.sin(x * 0.006 + phase) + 34 * Math.sin(x * 0.017 + 1 + phase);
            double hgt = 110 + 120 * (0.5 + 0.5 * Math.sin(x * 0.011 + 2 + phase)) * (0.6 + 0.4 * Math.sin(x * 0.043 + phase));
            double top = base - hgt, bot = base + 22;
            double edge = Math.min(1, Math.min(x, cw - x) / 140); // les bords du rideau s'effacent
            gc.setFill(new LinearGradient(0, top, 0, bot, false, CycleMethod.NO_CYCLE,
                    new Stop(0.0, Color.hsb(290, 0.6, 1, 0)),
                    new Stop(0.35, Color.hsb(hueBase + 140, 0.5, 1, 0.16 * edge)),
                    new Stop(0.72, Color.hsb(hueBase, 0.7, 1, 0.46 * edge)),
                    new Stop(0.93, Color.hsb(hueBase - 5, 0.55, 1, 0.75 * edge)),
                    new Stop(1.0, Color.hsb(hueBase - 5, 0.4, 1, 0))));
            gc.fillRect(x, top, step + 0.6, bot - top);
        }
    }

    // ------------------------------------------------------------------ dessin

    /** Dessine le decor complet (t = temps de jeu en secondes). */
    void draw(GraphicsContext gc, double t, int theme, boolean duel, double winScale) {
        int cls = scaleClass(winScale);
        switch (theme) {
        case 1:
            gc.drawImage(mars(cls), 0, 0, W, H);
            stars(gc, t, 55, 0.6);
            phobos(gc, t);
            break;
        case 2:
            gc.drawImage(aurora(cls), 0, 0, W, H);
            stars(gc, t, 55, 0.55);
            auroraCurtains(gc, t);
            break;
        case 3:
            gc.drawImage(ocean(cls), 0, 0, W, H);
            stars(gc, t, 45, 0.7, HZ - 10);
            oceanLive(gc, t);
            break;
        default:
            if (duel) classicDuel(gc, t, cls);
            else {
                gc.setFill(soloBg);
                gc.fillRect(0, 0, W, H);
                stars(gc, t, starX.length, 1.0);
            }
            break;
        }
        if (duel) campsAndLine(gc);
    }

    private void stars(GraphicsContext gc, double t, int n, double strength) {
        stars(gc, t, n, strength, H);
    }

    private void stars(GraphicsContext gc, double t, int n, double strength, double maxY) {
        gc.setFill(Color.WHITE);
        for (int i = 0; i < n; i++) {
            double y = (starY[i] * maxY / H + t * 6 * starS[i]) % maxY;
            gc.setGlobalAlpha((0.25 + 0.5 * Math.abs(Math.sin(t * starS[i] + i))) * strength);
            gc.fillOval(starX[i], y, starS[i], starS[i]);
        }
        gc.setGlobalAlpha(1);
    }

    private void classicDuel(GraphicsContext gc, double t, int cls) {
        gc.drawImage(duelBase(cls), 0, 0, W, H);
        // les nebuleuses derivent lentement : deux petits voiles par-dessus la composition fixe
        drawSprite(gc, blobBlue, W * 0.25 + Math.sin(t * 0.15) * 90, H * 0.45 + Math.cos(t * 0.11) * 60);
        drawSprite(gc, blobViolet, W * 0.75 + Math.cos(t * 0.13) * 90, H * 0.55 + Math.sin(t * 0.17) * 60);
        stars(gc, t, starX.length, 1.0);
        for (int i = 0; i < star2X.length; i++) {
            double y = (star2Y[i] + t * 18 * star2S[i]) % H;
            double a = 0.35 + 0.55 * Math.abs(Math.sin(t * star2S[i] * 1.3 + i));
            gc.setFill(i % 3 == 0 ? Color.rgb(160, 200, 255, a) : Color.rgb(255, 255, 255, a));
            gc.fillOval(star2X[i], y, star2S[i], star2S[i]);
        }
        shootingStar(gc, t);
    }

    private void drawSprite(GraphicsContext gc, Image img, double cx, double cy) {
        gc.drawImage(img, cx - img.getWidth() / 2, cy - img.getHeight() / 2);
    }

    private void shootingStar(GraphicsContext gc, double t) {
        double cyc = t / 7.0;
        double ph = (cyc - Math.floor(cyc)) * 10; // 0..1 : le trait ; ensuite pause
        if (ph >= 1) return;
        Random r = new Random((long) Math.floor(cyc) * 31 + 7);
        double sx = 100 + r.nextDouble() * 760, sy = Game.TOP + 20 + r.nextDouble() * 220;
        for (int i = 0; i < 12; i++) {
            double f = ph - i * 0.02;
            if (f < 0) continue;
            gc.setFill(Color.rgb(255, 255, 255, (1 - ph) * (1 - i / 12.0)));
            gc.fillOval(sx + f * 280 - 2, sy + f * 170 - 2, 4 - i * 0.2, 4 - i * 0.2);
        }
    }

    /** Ocean : vagues qui ondulent, scintillement sous la planete, plancton lumineux. */
    private void oceanLive(GraphicsContext gc, double t) {
        int n = 36;
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < 9; i++) {
            double f = i / 8.0;
            double y0 = HZ + 12 + (H - HZ - 30) * Math.pow(f, 1.8);
            double amp = 1.2 + 6.5 * f, k = 0.05 - 0.028 * f;
            for (int j = 0; j < n; j++) {
                double x = j * W / (n - 1);
                xs[j] = x;
                ys[j] = y0 + amp * Math.sin(x * k + t * (0.7 + f) + i * 1.7);
            }
            gc.setStroke(waveColors[i]);
            gc.setLineWidth(1 + 2.2 * f);
            gc.strokePolyline(xs, ys, n);
        }
        for (int j = 0; j < 16; j++) { // reflet qui scintille sous la planete
            double yy = HZ + 6 + j * j * 0.95;
            double hw = 10 + j * 4.2, cx = 235 + Math.sin(t * 1.3 + j) * (5 + j * 1.6);
            gc.setStroke(Color.rgb(255, 170, 240, 0.08 + 0.20 * Math.abs(Math.sin(t * 2 + j * 1.7))));
            gc.setLineWidth(1.4 + j * 0.13);
            gc.strokeLine(cx - hw, yy, cx + hw, yy);
        }
        for (int i = 0; i < BIO; i++) { // plancton phosphorescent
            double a = Math.sin(t * bioS[i] + bioP[i]);
            if (a <= 0) continue;
            gc.setGlobalAlpha(0.12 + 0.75 * a * a);
            Image g = i % 3 == 0 ? glowPink : (i % 3 == 1 ? glowCyan : glowGreen);
            gc.drawImage(g, bioX[i] + 6 * Math.sin(t * 0.3 + bioP[i]) - 13, bioY[i] - 13);
        }
        gc.setGlobalAlpha(1);
    }

    /** Phobos : petit rocher gris qui traverse tres lentement le ciel de Mars. */
    private void phobos(GraphicsContext gc, double t) {
        double x = (t * 8) % (W + 160) - 80, y = 105 + 22 * Math.sin(t * 0.07);
        gc.setFill(Color.rgb(150, 142, 134, 0.9));
        gc.fillOval(x - 13, y - 9, 26, 18);
        gc.setFill(Color.rgb(40, 34, 32, 0.45));
        gc.fillOval(x - 2, y - 8, 15, 17);
        gc.setFill(Color.rgb(70, 62, 58, 0.55));
        gc.fillOval(x - 8, y - 3, 5, 4);
        gc.fillOval(x + 3, y + 1, 4, 3);
        gc.setFill(Color.rgb(255, 255, 255, 0.18));
        gc.fillOval(x - 12, y - 8, 12, 6);
    }

    private void auroraCurtains(GraphicsContext gc, double t) {
        gc.setGlobalAlpha(0.70 + 0.25 * Math.sin(t * 0.55));
        gc.drawImage(curtainA, -60 + 50 * Math.sin(t * 0.12), 20 + 14 * Math.sin(t * 0.17));
        gc.setGlobalAlpha(1);
    }

    /** Duel : camp du blanc (lumiere), camp du noir (ombre) et ligne mediane. */
    private void campsAndLine(GraphicsContext gc) {
        gc.setFill(duelLeft);
        gc.fillRect(0, Game.TOP, 170, H - Game.TOP);
        gc.setFill(duelRight);
        gc.fillRect(W - 170, Game.TOP, 170, H - Game.TOP);
        gc.setStroke(Color.rgb(255, 255, 255, 0.14));
        gc.setLineWidth(2);
        gc.setLineDashes(10, 14);
        gc.strokeLine(W / 2, Game.TOP, W / 2, H);
        gc.setLineDashes((double[]) null);
    }
}
