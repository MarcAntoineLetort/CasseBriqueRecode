package cassebrique;

import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** Couleurs, polices et petites fonctions de texte partagees par tous les ecrans. */
final class Theme {
    private Theme() {
    }

    static final Color OUTSIDE = Color.web("#04050d");
    static final Color WHITE = Color.web("#e8ecff");
    static final Color SOFT = Color.web("#aab4ee");
    static final Color DIM = Color.web("#7a84b8");
    static final Color GOLD = Color.web("#ffd24a");
    static final Color INK = Color.web("#14141c");   // le "noir" du joueur noir
    static final Color PAPER = Color.web("#ffffff"); // le "blanc" du joueur blanc
    static final Color RED = Color.web("#ff6b6b");
    static final Color GREEN = Color.web("#7dff9a");

    static final Font HUGE = Font.font("System", FontWeight.BOLD, 160);
    static final Font TITLE = Font.font("System", FontWeight.BOLD, 76);
    static final Font BIG = Font.font("System", FontWeight.BOLD, 44);
    static final Font HUD = Font.font("System", FontWeight.BOLD, 22);
    static final Font MED = Font.font("System", FontWeight.BOLD, 20);
    static final Font SMALL = Font.font("System", FontWeight.NORMAL, 16);
    static final Font SMALL_BOLD = Font.font("System", FontWeight.BOLD, 16);
    static final Font TINY = Font.font("System", FontWeight.BOLD, 13);

    static void text(GraphicsContext gc, String s, double x, double y, Font f, Color c, TextAlignment a) {
        gc.setTextAlign(a);
        gc.setTextBaseline(VPos.CENTER);
        gc.setFont(f);
        gc.setFill(c);
        gc.fillText(s, x, y);
    }

    static void left(GraphicsContext gc, String s, double x, double y, Font f, Color c) {
        text(gc, s, x, y, f, c, TextAlignment.LEFT);
    }

    static void right(GraphicsContext gc, String s, double x, double y, Font f, Color c) {
        text(gc, s, x, y, f, c, TextAlignment.RIGHT);
    }

    static void center(GraphicsContext gc, String s, double x, double y, Font f, Color c) {
        text(gc, s, x, y, f, c, TextAlignment.CENTER);
    }

    /** Texte sur plusieurs lignes (separees par \n), aligne a gauche. */
    static void lines(GraphicsContext gc, String s, double x, double y, double step, Font f, Color c) {
        String[] parts = s.split("\n");
        for (int i = 0; i < parts.length; i++) left(gc, parts[i], x, y + i * step, f, c);
    }
}
