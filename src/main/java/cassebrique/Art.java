package cassebrique;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

/** Dessin partage des briques et des symboles de bonus (jeu et ecran d'informations). */
final class Art {
    private Art() {
    }

    static double hue(double h) {
        return ((h % 360) + 360) % 360;
    }

    private static final Color[] HUES = new Color[360];

    static {
        for (int i = 0; i < 360; i++) HUES[i] = Color.hsb(i, 0.7, 1.0);
    }

    /** Couleur saturee mise en cache pour les particules (la transparence se regle avec setGlobalAlpha). */
    static Color hueColor(double h) {
        return HUES[(int) hue(h) % 360];
    }

    static Color baseColor(Brick k) {
        switch (k.kind) {
        case STEEL: return Color.web("#7b8394");
        case EXPLOSIVE: return Color.hsb(8, 0.85, 0.95);
        case GOLD: return Color.web("#ffd24a");
        case LIGHTNING: return Color.web("#2a3270");
        case ROCK: return Color.web("#8a6a4a");
        case BUTTON: return Color.web("#3b4056");
        case FROST: return Color.web("#6fc3e8");
        case TIME: return Color.web("#6a4cc2");
        case NIGHT: return Color.web("#151a3f");
        case MOVER: return Color.web("#8a8f9e");
        case MALUS: return Color.web("#f2e41c");
        case SWAP: return Color.web("#f4f4fb");
        case BONUS: return k.bonus.bad ? Color.web("#3a1018") : Color.hsb(k.bonus.hue, 0.8, 0.95);
        default: return Color.hsb(k.hue(), 0.75, 0.95);
        }
    }

    // ------------------------------------------------------------------ briques

    private static final java.util.Map<String, Image> SPRITES = new java.util.HashMap<String, Image>();
    private static final double SPR_PAD = 3;
    private static double sprScale = 1;
    private static int sprEpoch;
    private static boolean renderingSprite;

    /** Briques dont l'aspect ne change pas d'une image a l'autre : dessinees une fois, puis simplement recopiees. */
    private static boolean fixedLook(Brick.Kind kind) {
        switch (kind) {
        case NORMAL: case STEEL: case EXPLOSIVE: case GOLD: case BONUS: case LIGHTNING: case ROCK:
        case FROST: case TIME: case NIGHT:
            return true;
        default:
            return false;
        }
    }

    /** Adapte la definition des sprites de briques a la taille de la fenetre (les refait seulement si elle change vraiment). */
    static void setSpriteScale(double winScale) {
        double sc = Backgrounds.scaleClass(winScale) == 0 ? 1.0 : (Backgrounds.scaleClass(winScale) == 1 ? 1.5 : 2.0);
        if (sc != sprScale) {
            sprScale = sc;
            sprEpoch++;
            SPRITES.clear();
        }
    }

    private static Image sprite(Brick k) {
        if (k.fxSprite != null && k.fxSpriteHp == k.hp && k.fxEpoch == sprEpoch) return (Image) k.fxSprite;
        String key = k.kind + "|" + k.hp + "|" + k.bonus + "|" + (int) (k.w * 10) + "|" + (int) (k.h * 10);
        Image im = SPRITES.get(key);
        if (im == null) {
            Canvas c = new Canvas(Math.ceil((k.w + 2 * SPR_PAD) * sprScale), Math.ceil((k.h + 2 * SPR_PAD) * sprScale));
            GraphicsContext g2 = c.getGraphicsContext2D();
            g2.scale(sprScale, sprScale);
            g2.translate(SPR_PAD - k.x, SPR_PAD - k.y);
            renderingSprite = true;
            drawLive(g2, k, 0);
            renderingSprite = false;
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            im = c.snapshot(sp, null);
            SPRITES.put(key, im);
        }
        k.fxSprite = im;
        k.fxSpriteHp = k.hp;
        k.fxEpoch = sprEpoch;
        return im;
    }

    static void brick(GraphicsContext gc, Brick k, double t) {
        if (k.flash <= 0 && k.frost <= 0 && fixedLook(k.kind)) {
            gc.drawImage(sprite(k), k.x - SPR_PAD, k.y - SPR_PAD, k.w + 2 * SPR_PAD, k.h + 2 * SPR_PAD);
            if (k.kind == Brick.Kind.GOLD) goldSparkle(gc, k, t);
            return;
        }
        drawLive(gc, k, t);
    }

    private static void goldSparkle(GraphicsContext gc, Brick k, double t) {
        double sp = (t * 1.3 + k.col * 0.37 + k.row * 0.71) % 3.0; // scintillement
        if (sp >= 0.4) return;
        double f = sp / 0.4;
        double cx = k.x + k.w / 2, cy = k.y + k.h / 2 - 1;
        double sx = k.w >= k.h ? k.x + 6 + (k.w - 12) * f : cx;
        double sy = k.w >= k.h ? cy : k.y + 6 + (k.h - 12) * f;
        gc.setStroke(Color.rgb(255, 255, 255, 0.9));
        gc.setLineWidth(2);
        gc.strokeLine(sx - 4, sy, sx + 4, sy);
        gc.strokeLine(sx, sy - 4, sx, sy + 4);
    }

    private static void drawLive(GraphicsContext gc, Brick k, double t) {
        if (k.kind == Brick.Kind.PHASE) {
            phase(gc, k, t);
            return;
        }
        if (k.kind == Brick.Kind.TELEPORT) {
            teleport(gc, k, t);
            return;
        }
        if (k.kind == Brick.Kind.RAINBOW) {
            rainbowBrick(gc, k, t);
            return;
        }
        double x = k.x, y = k.y, w = k.w, h = k.h;
        double cx = x + w / 2, cy = y + h / 2 - 1;
        double arc = Math.min(8, Math.min(w, h) / 2.5);

        if (k.fxBase == null || k.fxHp != k.hp) { // la couleur ne change que si les points de vie changent
            Color b0 = baseColor(k);
            k.fxBase = b0;
            k.fxDark = b0.darker().darker();
            k.fxHp = k.hp;
        }
        Color base = (Color) k.fxBase, dark = (Color) k.fxDark;
        if (k.flash > 0) {
            base = base.interpolate(Color.WHITE, Math.min(1, k.flash * 6));
            dark = base.darker().darker();
        }
        gc.setFill(dark);
        gc.fillRoundRect(x, y + 2, w, h, arc, arc);
        gc.setFill(base);
        gc.fillRoundRect(x, y, w, h - 2, arc, arc);
        gc.setFill(Color.rgb(255, 255, 255, 0.28));
        if (w >= h) gc.fillRoundRect(x + 3, y + 2, w - 6, 6, 6, 6);
        else gc.fillRoundRect(x + 2, y + 3, 5, h - 8, 5, 5);

        icon(gc, k, base, cx, cy, t);
        if (k.frost > 0) frostOverlay(gc, k, t);
    }

    private static void icon(GraphicsContext gc, Brick k, Color base, double cx, double cy, double t) {
        double x = k.x, y = k.y, w = k.w, h = k.h;
        double arc = Math.min(8, Math.min(w, h) / 2.5);
        switch (k.kind) {
        case EXPLOSIVE:
            gc.setFill(Color.web("#3b0a00"));
            gc.fillOval(cx - 7, cy - 7, 14, 14);
            gc.setFill(Color.YELLOW);
            gc.fillOval(cx - 3, cy - 3, 6, 6);
            break;
        case STEEL:
            gc.setFill(Color.rgb(40, 45, 60, 0.7));
            gc.fillOval(x + 4, y + 4, 4, 4);
            gc.fillOval(x + w - 8, y + 4, 4, 4);
            gc.fillOval(x + 4, y + h - 10, 4, 4);
            gc.fillOval(x + w - 8, y + h - 10, 4, 4);
            break;
        case GOLD: {
            gc.setStroke(Color.web("#a87a00"));
            gc.setLineWidth(1.5);
            gc.strokeRoundRect(x + 0.75, y + 0.75, w - 1.5, h - 3.5, arc, arc);
            if (!renderingSprite) goldSparkle(gc, k, t);
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
            if (w >= 40) { // fleches : detruit toute la ligne
                gc.fillPolygon(new double[] { x + 6, x + 14, x + 14 }, new double[] { cy, cy - 5, cy + 5 }, 3);
                gc.fillPolygon(new double[] { x + w - 6, x + w - 14, x + w - 14 }, new double[] { cy, cy - 5, cy + 5 }, 3);
            }
            break;
        case BUTTON: { // gros bouton rouge sur une plaque a liseres jaune et noir
            boolean pressed = k.flash > 0;
            gc.setStroke(Color.web("#e8c030"));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
            gc.setStroke(Color.web("#1a1c28"));
            gc.setLineDashes(4, 4);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
            gc.setLineDashes((double[]) null);
            double r = 7;
            double off = pressed ? 2 : 0;
            gc.setFill(Color.web("#a3a9c2"));
            gc.fillOval(cx - r - 2, cy - r - 2, 2 * r + 4, 2 * r + 4);
            gc.setFill(Color.web("#1d2030"));
            gc.fillOval(cx - r - 0.5, cy - r - 0.5, 2 * r + 1, 2 * r + 1);
            gc.setFill(pressed ? Color.web("#8f1018") : Color.web("#e8303c"));
            gc.fillOval(cx - r + 1, cy - r + 1 + off, 2 * r - 2, 2 * r - 2);
            if (!pressed) {
                gc.setFill(Color.rgb(255, 255, 255, 0.6));
                gc.fillOval(cx - r * 0.5, cy - r * 0.7, r * 0.7, r * 0.45);
            }
            break;
        }
        case FROST: { // flocon
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            for (int i = 0; i < 3; i++) {
                double a = Math.toRadians(i * 60 + 90);
                gc.strokeLine(cx - Math.cos(a) * 8, cy - Math.sin(a) * 8, cx + Math.cos(a) * 8, cy + Math.sin(a) * 8);
            }
            gc.setFill(Color.WHITE);
            gc.fillOval(cx - 2, cy - 2, 4, 4);
            break;
        }
        case TIME: { // horloge avec fleche de retour
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(2);
            gc.strokeOval(cx - 8, cy - 8, 16, 16);
            gc.strokeLine(cx, cy, cx, cy - 5);
            gc.strokeLine(cx, cy, cx + 4, cy + 1);
            gc.setFill(Color.WHITE);
            gc.fillPolygon(new double[] { cx - 11, cx - 5, cx - 11 }, new double[] { cy - 6, cy - 5, cy - 1 }, 3);
            break;
        }
        case NIGHT: { // croissant de lune
            gc.setFill(Color.web("#ffe9a8"));
            gc.fillOval(cx - 7, cy - 7, 14, 14);
            gc.setFill(base);
            gc.fillOval(cx - 2, cy - 9, 13, 13);
            gc.setFill(Color.rgb(255, 255, 255, 0.8));
            gc.fillOval(cx + 4, cy + 2, 2, 2);
            break;
        }
        case MOVER: { // bandes de chantier et fleches
            gc.save();
            gc.beginPath();
            gc.rect(x, y, w, h - 2);
            gc.clip();
            gc.setStroke(Color.rgb(255, 200, 30, 0.9));
            gc.setLineWidth(5);
            for (double d = -h; d < w + h; d += 14) gc.strokeLine(x + d, y + h, x + d + h, y);
            gc.restore();
            gc.setFill(Color.rgb(35, 38, 52, 0.92));
            gc.fillRoundRect(cx - 10, cy - 7, 20, 14, 8, 8);
            gc.setFill(Color.WHITE);
            gc.fillPolygon(new double[] { cx - 8, cx - 3, cx - 3 }, new double[] { cy, cy - 4, cy + 4 }, 3);
            gc.fillPolygon(new double[] { cx + 8, cx + 3, cx + 3 }, new double[] { cy, cy - 4, cy + 4 }, 3);
            break;
        }
        case MALUS: { // jaune vif, barres noires, tete de mort et contour rouge : impossible a confondre avec l'or
            gc.setFill(Color.web("#161616"));
            if (w >= h) {
                gc.fillRect(x + 3, y + 4, 4, h - 9);
                gc.fillRect(x + w - 7, y + 4, 4, h - 9);
            } else {
                gc.fillRect(x + 4, y + 3, w - 8, 4);
                gc.fillRect(x + 4, y + h - 9, w - 8, 4);
            }
            gc.fillOval(cx - 6.5, cy - 8, 13, 12);
            gc.fillRoundRect(cx - 3.5, cy + 2, 7, 6, 2, 2);
            gc.setFill(base);
            gc.fillOval(cx - 4.7, cy - 4.5, 3.8, 4.4);
            gc.fillOval(cx + 0.9, cy - 4.5, 3.8, 4.4);
            gc.fillPolygon(new double[] { cx, cx - 1.2, cx + 1.2 }, new double[] { cy - 0.5, cy + 1.8, cy + 1.8 }, 3);
            gc.setStroke(base);
            gc.setLineWidth(1);
            gc.strokeLine(cx - 1.2, cy + 4, cx - 1.2, cy + 8);
            gc.strokeLine(cx + 1.2, cy + 4, cx + 1.2, cy + 8);
            gc.setStroke(Color.rgb(255, 40, 40, 0.55 + 0.4 * Math.sin(t * 6)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
            break;
        }
        case SWAP: { // moitie blanche / moitie noire et fleches croisees
            boolean wide = w >= h;
            gc.setFill(Color.web("#14141c"));
            if (wide) {
                gc.fillRoundRect(x + w / 2, y, w / 2, h - 2, arc, arc);
                gc.fillRect(x + w / 2, y, arc, h - 2);
            } else {
                gc.fillRoundRect(x, y + h / 2, w, h / 2 - 2, arc, arc);
                gc.fillRect(x, y + h / 2, w, arc);
            }
            gc.setStroke(Color.web("#8a8fa8"));
            gc.setLineWidth(1.5);
            if (wide) {
                arrow(gc, cx - 9, cy - 4, cx + 9, cy - 4, 4);
                arrow(gc, cx + 9, cy + 4, cx - 9, cy + 4, 4);
            } else {
                arrow(gc, cx - 4, cy + 9, cx - 4, cy - 9, 4);
                arrow(gc, cx + 4, cy - 9, cx + 4, cy + 9, 4);
            }
            gc.setStroke(Color.rgb(255, 255, 255, 0.35 + 0.3 * Math.sin(t * 4)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
            if (k.flash > 0) {
                gc.setFill(Color.rgb(255, 220, 80, Math.min(0.8, k.flash * 2.5)));
                gc.fillRoundRect(x, y, w, h - 2, arc, arc);
            }
            break;
        }
        case BONUS:
            powerIcon(gc, k.bonus, cx, cy, 7, k.bonus.bad ? Color.web("#ff7a7a") : Color.web("#10142e"));
            if (k.bonus.bad) {
                gc.setStroke(Color.rgb(255, 70, 70, 0.5 + 0.4 * Math.sin(t * 6)));
            } else {
                gc.setStroke(Color.rgb(255, 255, 255, 0.45 + 0.35 * Math.sin(t * 5)));
            }
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
            break;
        default:
            break;
        }
    }

    private static void arrow(GraphicsContext gc, double x1, double y1, double x2, double y2, double head) {
        gc.strokeLine(x1, y1, x2, y2);
        double a = Math.atan2(y2 - y1, x2 - x1);
        gc.strokeLine(x2, y2, x2 - Math.cos(a - 0.6) * head, y2 - Math.sin(a - 0.6) * head);
        gc.strokeLine(x2, y2, x2 - Math.cos(a + 0.6) * head, y2 - Math.sin(a + 0.6) * head);
    }

    /** Brique arc-en-ciel : portail aux anneaux multicolores et petit arc-en-ciel au centre. */
    private static void rainbowBrick(GraphicsContext gc, Brick k, double t) {
        double x = k.x, y = k.y, w = k.w, h = k.h;
        double cx = x + w / 2, cy = y + h / 2;
        double arc = Math.min(10, Math.min(w, h) / 2.2);
        gc.setFill(Color.rgb(255, 255, 255, 0.14));
        gc.fillRoundRect(x, y, w, h, arc, arc);
        gc.setStroke(Color.hsb(hue(t * 90), 0.55, 1, 0.95));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 2, arc, arc);
        for (int i = 0; i < 2; i++) {
            double ph = (t * 0.8 + i / 2.0) % 1.0;
            double rw = (w / 2 - 3) * (1 - ph), rh = (h / 2 - 3) * (1 - ph);
            gc.setStroke(Color.hsb(hue(t * 120 + i * 180), 0.5, 1, 0.2 + 0.7 * (1 - ph)));
            gc.strokeOval(cx - rw, cy - rh, rw * 2, rh * 2);
        }
        double r0 = Math.min(w, h) * 0.36;
        gc.setLineWidth(2);
        for (int i = 0; i < 4; i++) {
            double r = r0 - i * 2.1;
            if (r <= 1) break;
            gc.setStroke(Color.hsb(i * 48, 0.85, 1));
            gc.strokeArc(cx - r, cy - r + r0 * 0.45, 2 * r, 2 * r, 0, 180, ArcType.OPEN);
        }
    }

    /** Brique-interrupteur : "hologramme" qui scintille quand elle est solide, contour discret quand elle est cachee. */
    private static void phase(GraphicsContext gc, Brick k, double t) {
        double x = k.x, y = k.y, w = k.w, h = k.h;
        double arc = Math.min(8, Math.min(w, h) / 2.5);
        if (!k.phaseSolid) {
            gc.setStroke(Color.rgb(63, 184, 201, 0.30 + Math.min(0.4, k.flash * 2)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
            return;
        }
        double pulse = 0.5 + 0.5 * Math.sin(t * 3 + k.col * 0.6 + k.row * 0.4);
        gc.setFill(Color.rgb(63, 184, 201, 0.30 + 0.18 * pulse));
        gc.fillRoundRect(x, y, w, h - 2, arc, arc);
        // hachures diagonales coupees a la main : un seul trace, sans clip (le clip du Canvas est tres couteux)
        gc.setStroke(Color.rgb(200, 250, 255, 0.35));
        gc.setLineWidth(2);
        double off = (t * 18) % 12, hh = h - 2;
        gc.beginPath();
        for (double d = -hh; d < w; d += 12) {
            double x0 = x + d + off, y0 = y + hh, x1 = x0 + hh, y1 = y;
            if (x0 < x) {
                y0 -= x - x0;
                x0 = x;
            }
            if (x1 > x + w) {
                y1 += x1 - (x + w);
                x1 = x + w;
            }
            if (x1 - x0 < 1 || y0 - y1 < 1) continue;
            gc.moveTo(x0, y0);
            gc.lineTo(x1, y1);
        }
        gc.stroke();
        gc.setStroke(Color.rgb(190, 248, 255, 0.95));
        gc.setLineWidth(2);
        gc.setLineDashes(6, 4);
        gc.setLineDashOffset(-t * 16);
        gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
        gc.setLineDashes((double[]) null);
        if (k.flash > 0) {
            gc.setFill(Color.rgb(255, 255, 255, Math.min(0.7, k.flash * 3)));
            gc.fillRoundRect(x, y, w, h - 2, arc, arc);
        }
    }

    /** Teleporteur : portail aux anneaux qui convergent. */
    private static void teleport(GraphicsContext gc, Brick k, double t) {
        double x = k.x, y = k.y, w = k.w, h = k.h;
        double cx = x + w / 2, cy = y + h / 2;
        double arc = Math.min(10, Math.min(w, h) / 2.2);
        gc.setFill(Color.rgb(150, 60, 220, 0.22));
        gc.fillRoundRect(x, y, w, h, arc, arc);
        gc.setStroke(Color.rgb(205, 130, 255, 0.9));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 2, arc, arc);
        for (int i = 0; i < 3; i++) {
            double ph = (t * 0.9 + i / 3.0) % 1.0;
            double rw = (w / 2 - 3) * (1 - ph), rh = (h / 2 - 3) * (1 - ph);
            gc.setStroke(Color.rgb(235, 180, 255, 0.15 + 0.75 * (1 - ph)));
            gc.setLineWidth(2);
            gc.strokeOval(cx - rw, cy - rh, rw * 2, rh * 2);
        }
    }

    private static void frostOverlay(GraphicsContext gc, Brick k, double t) {
        double x = k.x, y = k.y, w = k.w, h = k.h;
        double arc = Math.min(8, Math.min(w, h) / 2.5);
        double a = k.frost < 2 ? 0.30 + 0.25 * Math.sin(t * 20) : 0.55; // clignote avant de fondre
        gc.setFill(Color.rgb(205, 238, 255, a));
        gc.fillRoundRect(x, y, w, h - 2, arc, arc);
        gc.setStroke(Color.rgb(255, 255, 255, Math.min(0.9, a + 0.3)));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x + 1, y + 1, w - 2, h - 4, arc, arc);
        gc.setLineWidth(1);
        gc.strokeLine(x + w * 0.2, y + 3, x + w * 0.45, y + h * 0.5);
        gc.strokeLine(x + w * 0.45, y + h * 0.5, x + w * 0.35, y + h - 5);
        gc.strokeLine(x + w * 0.7, y + 3, x + w * 0.6, y + h * 0.45);
    }

    // ------------------------------------------------------------------ bonus / malus

    /** Capsule d'un bonus qui tombe (ou legende de l'ecran d'informations). */
    static void capsule(GraphicsContext gc, PowerUp.Type type, double cx, double cy, double t) {
        gc.setFill(Color.rgb(255, 255, 255, 0.12));
        gc.fillRoundRect(cx - 21, cy - 15, 42, 30, 30, 30);
        if (type.bad) { // malus : fond sombre, contour rouge qui pulse
            gc.setFill(Color.web("#3a1018"));
            gc.fillRoundRect(cx - 16, cy - 10, 32, 20, 20, 20);
            gc.setStroke(Color.rgb(255, 70, 70, 0.7 + 0.3 * Math.sin(t * 10)));
            gc.setLineWidth(2);
            gc.strokeRoundRect(cx - 16, cy - 10, 32, 20, 20, 20);
            powerIcon(gc, type, cx, cy, 6.5, Color.web("#ff7a7a"));
        } else {
            gc.setFill(Color.hsb(type.hue, 0.8, 0.95));
            gc.fillRoundRect(cx - 16, cy - 10, 32, 20, 20, 20);
            powerIcon(gc, type, cx, cy, 6.5, Color.web("#10142e"));
        }
    }

    /** Symbole d'un bonus, centre en (cx, cy), de demi-taille s. */
    static void powerIcon(GraphicsContext gc, PowerUp.Type type, double cx, double cy, double s, Color ink) {
        gc.setFill(ink);
        gc.setStroke(ink);
        gc.setLineWidth(2);
        switch (type) {
        case WIDE: // double fleche
            gc.strokeLine(cx - s, cy, cx + s, cy);
            gc.fillPolygon(new double[] { cx - s - 1, cx - s + 4, cx - s + 4 }, new double[] { cy, cy - 4, cy + 4 }, 3);
            gc.fillPolygon(new double[] { cx + s + 1, cx + s - 4, cx + s - 4 }, new double[] { cy, cy - 4, cy + 4 }, 3);
            break;
        case MULTI: // trois balles
            gc.fillOval(cx - 3, cy - s, 6, 6);
            gc.fillOval(cx - s, cy + 0.5, 6, 6);
            gc.fillOval(cx + s - 6, cy + 0.5, 6, 6);
            break;
        case SLOW: // horloge
            gc.strokeOval(cx - s + 1, cy - s + 1, 2 * s - 2, 2 * s - 2);
            gc.setLineWidth(1.6);
            gc.strokeLine(cx, cy, cx, cy - s * 0.55);
            gc.strokeLine(cx, cy, cx + s * 0.45, cy + s * 0.1);
            break;
        case FIRE: // flamme
            gc.fillPolygon(new double[] { cx, cx + s * 0.65, cx + s * 0.55, cx, cx - s * 0.55, cx - s * 0.65 },
                    new double[] { cy - s, cy - s * 0.1, cy + s * 0.65, cy + s, cy + s * 0.65, cy - s * 0.1 }, 6);
            gc.setFill(Color.rgb(255, 255, 255, 0.7));
            gc.fillOval(cx - s * 0.25, cy + s * 0.1, s * 0.5, s * 0.6);
            break;
        case STICKY: // balle sur une barre gluante
            gc.fillOval(cx - 3, cy - s, 6, 6);
            gc.fillRoundRect(cx - s, cy, 2 * s, 4, 3, 3);
            gc.fillOval(cx - s * 0.6, cy + 2, 3, 6);
            gc.fillOval(cx + s * 0.2, cy + 2, 3, 7);
            break;
        case WALL: // mur de briques
            gc.fillRect(cx - s, cy - 5, s - 1, 4);
            gc.fillRect(cx + 1, cy - 5, s - 1, 4);
            gc.fillRect(cx - s, cy + 1, s * 0.5 - 1, 4);
            gc.fillRect(cx - s * 0.5 + 1, cy + 1, s - 2, 4);
            gc.fillRect(cx + s * 0.5 + 1, cy + 1, s * 0.5 - 1, 4);
            break;
        case LIFE: // coeur
            gc.fillOval(cx - s, cy - s * 0.8, s, s);
            gc.fillOval(cx, cy - s * 0.8, s, s);
            gc.fillPolygon(new double[] { cx - s + 0.4, cx + s - 0.4, cx },
                    new double[] { cy - s * 0.15, cy - s * 0.15, cy + s * 0.95 }, 3);
            break;
        case GHOSTS: // fantome
            gc.fillOval(cx - s * 0.75, cy - s, s * 1.5, s * 1.5);
            gc.fillRect(cx - s * 0.75, cy - s * 0.25, s * 1.5, s * 1.0);
            gc.fillOval(cx - s * 0.75, cy + s * 0.45, s * 0.55, s * 0.6);
            gc.fillOval(cx - s * 0.27, cy + s * 0.45, s * 0.55, s * 0.6);
            gc.fillOval(cx + s * 0.2, cy + s * 0.45, s * 0.55, s * 0.6);
            gc.setFill(Color.WHITE);
            gc.fillOval(cx - s * 0.45, cy - s * 0.45, s * 0.4, s * 0.55);
            gc.fillOval(cx + s * 0.05, cy - s * 0.45, s * 0.4, s * 0.55);
            break;
        case STAR: // etoile
            star(gc, cx, cy, s * 1.05, s * 0.45, -Math.PI / 2);
            break;
        case SHRINK: // fleches qui se rapprochent
            gc.fillPolygon(new double[] { cx - s, cx - s, cx - 2 }, new double[] { cy - 4.5, cy + 4.5, cy }, 3);
            gc.fillPolygon(new double[] { cx + s, cx + s, cx + 2 }, new double[] { cy - 4.5, cy + 4.5, cy }, 3);
            break;
        case LAVA: // boule entouree de rayons
            gc.fillOval(cx - s * 0.5, cy - s * 0.5, s, s);
            gc.setLineWidth(1.8);
            for (int i = 0; i < 8; i++) {
                double an = i * Math.PI / 4;
                gc.strokeLine(cx + Math.cos(an) * s * 0.75, cy + Math.sin(an) * s * 0.75,
                        cx + Math.cos(an) * s * 1.2, cy + Math.sin(an) * s * 1.2);
            }
            break;
        case DRUNK: { // spirale de vertige
            double[] xs = new double[26], ys = new double[26];
            for (int i = 0; i < 26; i++) {
                double an = i * 0.42, r = s * (0.12 + i * 0.037);
                xs[i] = cx + Math.cos(an) * r;
                ys[i] = cy + Math.sin(an) * r;
            }
            gc.setLineWidth(1.6);
            gc.strokePolyline(xs, ys, 26);
            break;
        }
        case NOBREAK: // sens interdit
            gc.strokeOval(cx - s + 1, cy - s + 1, 2 * s - 2, 2 * s - 2);
            gc.strokeLine(cx - s * 0.65, cy + s * 0.65, cx + s * 0.65, cy - s * 0.65);
            break;
        default:
            break;
        }
    }

    /** Polygone en etoile a cinq branches. */
    static void star(GraphicsContext gc, double cx, double cy, double outer, double inner, double rot) {
        double[] xs = new double[10], ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double r = (i % 2 == 0) ? outer : inner;
            double a = rot + i * Math.PI / 5;
            xs[i] = cx + Math.cos(a) * r;
            ys[i] = cy + Math.sin(a) * r;
        }
        gc.fillPolygon(xs, ys, 10);
    }
}
