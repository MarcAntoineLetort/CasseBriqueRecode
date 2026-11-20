package cassebrique;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Ecran "Informations" : controles, regles, bonus et malus, briques speciales, comptage des points.
 * Les valeurs (points, durees, vies...) sont lues dans Rules : changer une regle met le texte a jour.
 * Les lignes sont comprimees si elles depassent la largeur disponible : rien n'est jamais rogne.
 */
final class InfoScreen {
    private InfoScreen() {
    }

    private static final String[] TITLES = {
        "Contr\u00f4les", "R\u00e8gles du jeu", "Bonus et malus", "Briques sp\u00e9ciales (1/2)", "Briques sp\u00e9ciales (2/2)",
        "Comptage des points" };

    private static final double PANEL_TEXT = 405; // largeur de texte dans un panneau de 440

    static void draw(GraphicsContext gc, Game g, double t) {
        gc.setFill(Color.rgb(4, 5, 13, 0.55));
        gc.fillRect(0, 0, Game.W, Game.H);
        Theme.center(gc, "INFORMATIONS", Game.W / 2, 34, Theme.BIG, Color.hsb(210, 0.45, 1));
        Theme.center(gc, TITLES[g.infoPage], Game.W / 2, 72, Theme.MED, Theme.GOLD);

        switch (g.infoPage) {
        case 0:
            controls(gc);
            break;
        case 1:
            rules(gc);
            break;
        case 2:
            bonuses(gc, t);
            break;
        case 3:
            bricks(gc, t, 0);
            break;
        case 4:
            bricks(gc, t, 1);
            break;
        default:
            points(gc);
            break;
        }

        // navigation
        Renderer.button(gc, Game.BACK_X, Game.NAV_Y, Game.BACK_W, Game.PAGE_H, g.hoverBack);
        Theme.center(gc, "< Retour", Game.BACK_X + Game.BACK_W / 2, Game.NAV_Y + Game.PAGE_H / 2, Theme.SMALL_BOLD, Theme.WHITE);
        Renderer.arrow(gc, "<", Game.PREV_X, Game.NAV_Y, g.infoPage > 0, g.hoverPrev);
        Renderer.arrow(gc, ">", Game.NEXT_X, Game.NAV_Y, g.infoPage < Game.INFO_PAGES - 1, g.hoverNext);
        Theme.center(gc, "Page " + (g.infoPage + 1) + " / " + Game.INFO_PAGES, Game.W / 2, Game.NAV_Y + Game.PAGE_H / 2,
                Theme.SMALL, Theme.SOFT);
    }

    private static void panel(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(Color.rgb(120, 170, 255, 0.10));
        gc.fillRoundRect(x, y, w, h, 16, 16);
        gc.setStroke(Color.rgb(160, 190, 255, 0.40));
        gc.setLineWidth(2);
        gc.strokeRoundRect(x, y, w, h, 16, 16);
    }

    private static void heading(GraphicsContext gc, String s, double x, double y) {
        Theme.left(gc, s, x, y, Theme.MED, Theme.GOLD);
    }

    private static void text(GraphicsContext gc, String s, double x, double y, double step, double maxW) {
        Theme.lines(gc, s, x, y, step, Theme.SMALL, Theme.WHITE, maxW);
    }

    // ------------------------------------------------------------------ pages

    private static void controls(GraphicsContext gc) {
        panel(gc, 30, 100, 440, 300);
        heading(gc, "1 JOUEUR", 50, 126);
        text(gc, "D\u00e9placer la raquette :\nsouris, fl\u00e8ches gauche / droite ou Q / D.\n\n"
                + "Lancer la balle, la relancer (balle\ncollante) ou tirer une \u00e9toile filante :\nclic ou ESPACE.\n\n"
                + "Si le curseur sort de la fen\u00eatre,\nla raquette va jusqu'au bord.",
                50, 160, 21, PANEL_TEXT);

        panel(gc, 490, 100, 440, 300);
        heading(gc, "2 JOUEURS (clavier)", 510, 126);
        gc.setFill(Theme.PAPER);
        gc.fillRoundRect(510, 146, 190, 26, 13, 13);
        Theme.center(gc, "BLANC - gauche", 605, 159, Theme.SMALL_BOLD, Theme.INK);
        text(gc, "Monter : Z (ou W)\nDescendre : S\nRelancer (collante) / \u00e9toile : ESPACE", 510, 192, 21, PANEL_TEXT);
        gc.setFill(Theme.INK);
        gc.fillRoundRect(510, 274, 190, 26, 13, 13);
        gc.setStroke(Color.web("#f4f4fb"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(510, 274, 190, 26, 13, 13);
        Theme.center(gc, "NOIR - droite", 605, 287, Theme.SMALL_BOLD, Theme.PAPER);
        text(gc, "Monter : fl\u00e8che haut\nDescendre : fl\u00e8che bas\nRelancer (collante) / \u00e9toile : ENTR\u00c9E", 510, 318, 21, PANEL_TEXT);

        panel(gc, 30, 416, 900, 150);
        heading(gc, "PARTOUT", 50, 440);
        text(gc, "P ou \u00c9chap : pause (elle rappelle les contr\u00f4les)  -  M : retour au menu depuis la pause\n"
                + "N : son activ\u00e9 / coup\u00e9  -  F11 (ou F) : plein \u00e9cran  -  la fen\u00eatre peut \u00eatre redimensionn\u00e9e\n"
                + "Dans le menu : fl\u00e8ches gauche / droite pour changer de page, Tab pour changer d'onglet (1 / 2 joueurs).\n"
                + "R\u00e9seau : chacun joue \u00e0 la souris sur son ordinateur (clic : relancer) ; l'h\u00f4te est BLANC, l'invit\u00e9 est NOIR.",
                50, 474, 25, 860);
    }

    private static void rules(GraphicsContext gc) {
        panel(gc, 30, 100, 440, 470);
        heading(gc, "SOLO", 50, 126);
        text(gc, "\u2022 Vous avez " + Rules.LIVES + " balles (vies) : une balle\n   perdue = une vie en moins.\n"
                + "\u2022 Campagne : les niveaux solo s'encha\u00eenent,\n   le score se cumule.\n"
                + "\u2022 Niveau avec briques d'or : il s'arr\u00eate d\u00e8s\n   que tout l'or est pris.\n"
                + "\u2022 Chaque brique touch\u00e9e sans toucher la\n   raquette augmente le multiplicateur\n   (voir \u00ab Comptage des points \u00bb).\n"
                + "\u2022 L'angle de renvoi d\u00e9pend de l'endroit\n   touch\u00e9 sur la raquette.\n"
                + "\u2022 Un record est gard\u00e9 par niveau et pour\n   la campagne.",
                50, 160, 22, PANEL_TEXT);

        panel(gc, 490, 100, 440, 470);
        heading(gc, "DUEL", 510, 126);
        text(gc, "\u2022 Chacun a sa balle et " + Rules.LIVES + " vies. Apr\u00e8s un\n   d\u00e9compte de " + Rules.sec(Rules.SERVE_DELAY)
                + ", la balle part toute seule.\n"
                + "\u2022 Ma balle sort de MON c\u00f4t\u00e9 : je perds une\n   vie (sauf s'il me reste d'autres balles).\n"
                + "\u2022 Ma balle sort du c\u00f4t\u00e9 ADVERSE : c'est\n   l'adversaire qui perd " + Rules.DUEL_PENALTY + " points.\n"
                + "\u2022 Plus de vie = d\u00e9faite, m\u00eame avec plus\n   de points.\n"
                + "\u2022 Sinon, quand le niveau est fini : le plus\n   de points gagne (balles restantes :\n   +" + Rules.BALL_BONUS + " chacune).\n"
                + "\u2022 Les points vont au propri\u00e9taire de la\n   balle. Scores cach\u00e9s pendant la partie,\n   r\u00e9v\u00e9l\u00e9s dans le tableau final.",
                510, 160, 22, PANEL_TEXT);
    }

    private static void bonuses(GraphicsContext gc, double t) {
        String[][] items = {
            { "WIDE", "Raquette large", Rules.sec(Rules.WIDE_TIME) },
            { "MULTI", "Multi-balles", "chaque balle en donne trois" },
            { "SLOW", "Ralenti", "balles plus lentes, " + Rules.sec(Rules.SLOW_TIME) },
            { "FIRE", "Boule de feu", "traverse les briques, " + Rules.sec(Rules.FIRE_TIME) },
            { "STICKY", "Balle collante", "reste sur la raquette, clic pour\nrelancer (" + Rules.sec(Rules.STICKY_TIME) + ")" },
            { "WALL", "Mur de secours", "derri\u00e8re la raquette, " + Rules.sec(Rules.WALL_TIME) },
            { "GHOSTS", "Fant\u00f4mes", "un par seconde pendant " + Rules.sec(Rules.GHOST_TIME) + ",\nchacun casse une brique puis dispara\u00eet" },
            { "STAR", "\u00c9toile filante", Rules.sec(Rules.STAR_TIME) + " pour un clic : explose au contact\nde la 1re brique (9 briques cass\u00e9es)" },
            { "LAVA", "Balle de lave", "casse les 8 briques autour de chaque\nbrique touch\u00e9e, " + Rules.sec(Rules.LAVA_TIME) },
            { "LIFE", "Balle en plus", "une vie suppl\u00e9mentaire (" + Rules.MAX_LIVES + " maximum)" },
            { "SHRINK", "MALUS : raquette r\u00e9duite", Rules.sec(Rules.SHRINK_TIME) },
            { "NOBREAK", "MALUS : balle inoffensive", "ne casse rien pendant " + Rules.sec(Rules.NOBREAK_TIME) },
            { "DRUNK", "MALUS : ivresse", "balle plus lente qui serpente, rebond au\nhasard sur la raquette, " + Rules.sec(Rules.DRUNK_TIME) } };
        for (int i = 0; i < items.length; i++) {
            double x = i < 7 ? 40 : 500;
            double y = 116 + (i < 7 ? i : i - 7) * 56;
            PowerUp.Type type = PowerUp.Type.valueOf(items[i][0]);
            Art.capsule(gc, type, x + 30, y, t);
            Theme.leftFit(gc, items[i][1], x + 66, y - 12, Theme.SMALL_BOLD, type.bad ? Theme.RED : Theme.WHITE, 360);
            Theme.lines(gc, items[i][2], x + 66, y + 8, 17, Theme.TINY, Theme.SOFT, 360);
        }
        // note en bas, sur toute la largeur (elle ne depasse plus de l'ecran)
        text(gc, "Une brique marqu\u00e9e d'un symbole donne son effet d\u00e8s qu'on la touche.\n"
                + "Les m\u00eames effets tombent aussi au hasard des briques normales :\n"
                + "capsule color\u00e9e = bonus, capsule sombre bord\u00e9e de rouge = malus.\n"
                + "Les effets s'appliquent au joueur qui les re\u00e7oit (et \u00e0 ses balles). La balle inoffensive annule le feu et la lave.",
                40, 502, 22, 880);
    }

    private static void brick(GraphicsContext gc, char c, double x, double y, double t) {
        Brick b = new Brick(0, 0, x, y, Game.SOLO_BW, Game.SOLO_BH, c);
        if (c == 'T') b.phaseSolid = true;
        Art.brick(gc, b, t);
    }

    private static void bricks(GraphicsContext gc, double t, int page) {
        Object[][] all = {
            { '2', "Normale", "1 \u00e0 3 coups : la couleur indique la r\u00e9sistance" },
            { '#', "Acier", "indestructible" },
            { 'X', "Explosive", "d\u00e9truit les 8 briques voisines" },
            { 'G', "Or", "vaut " + Rules.GOLD_POINTS + " points ; le niveau s'arr\u00eate quand tout\nl'or est pris (derni\u00e8re : +" + Rules.LAST_GOLD_SOLO
                    + ", duel +" + Rules.LAST_GOLD_DUEL + ")" },
            { 'Z', "Foudre", "d\u00e9truit toute sa colonne" },
            { 'R', "Roche", "d\u00e9truit toute sa ligne" },
            { 'B', "Bouton", "fait basculer les briques-interrupteurs" },
            { 'T', "Interrupteur", "solide ou fant\u00f4me (contour discret) :\nchange \u00e0 chaque pression d'un bouton" },
            { 'I', "Givre", "les 8 voisines sont incassables " + Rules.sec(Rules.FROST_TIME) },
            { 'H', "Temps", "les briques d\u00e9truites (sauf or et temps)\nr\u00e9apparaissent" },
            { 'N', "Nuit", "tout devient noir " + Rules.sec(Rules.NIGHT_TIME) + " : seuls la balle,\nla raquette et les effets restent visibles" },
            { 'M', "Mobile", "indestructible, patrouille entre ses balises ;\nrecule si elle devait \u00e9craser la balle" },
            { 'P', "T\u00e9l\u00e9portation", "traversable : la balle ressort d'un\nautre t\u00e9l\u00e9porteur, avec son \u00e9lan" },
            { 'a', "Arc-en-ciel", "comme la t\u00e9l\u00e9portation, mais les deux\nbriques utilis\u00e9es disparaissent (arc-en-ciel)" },
            { 'y', "Malus jaune (duel)", "t\u00eate de mort : une fois cass\u00e9e, un malus\npour l'ADVERSAIRE (jaune vif, pas l'or !)" },
            { 'x', "\u00c9change (duel)", "noir et blanc : les balles changent de\ncamp ; qui n'en a plus attend " + Rules.sec(Rules.SWAP_DELAY) },
            { 'w', "Bonus / malus", "effet imm\u00e9diat au contact (voir page\n\u00ab Bonus et malus \u00bb)" } };
        int from = page == 0 ? 0 : 10, to = page == 0 ? 10 : all.length;
        int n = to - from, perCol = (n + 1) / 2;
        for (int i = 0; i < n; i++) {
            double x = i < perCol ? 30 : 490;
            double y = 112 + (i < perCol ? i : i - perCol) * 88;
            Object[] it = all[from + i];
            brick(gc, (Character) it[0], x, y, t);
            Theme.leftFit(gc, (String) it[1], x + 76, y + 4, Theme.SMALL_BOLD, Theme.WHITE, 360);
            Theme.lines(gc, (String) it[2], x + 76, y + 26, 18, Theme.TINY, Theme.SOFT, 360);
        }
    }

    private static void points(GraphicsContext gc) {
        panel(gc, 30, 100, 440, 470);
        heading(gc, "POINTS GAGN\u00c9S", 50, 126);
        text(gc, "Brique normale : " + Rules.BRICK_POINTS + " x r\u00e9sistance (1 \u00e0 3)\n"
                + "Coup qui n'ach\u00e8ve pas une brique\nr\u00e9sistante : " + Rules.DAMAGE_POINTS + " points\n"
                + "Brique d'or : " + Rules.GOLD_POINTS + " points\n"
                + "-> ces points sont MULTIPLI\u00c9S par le\n    multiplicateur (voir \u00e0 droite).\n\n"
                + "Jamais multipli\u00e9s :\n"
                + "Derni\u00e8re brique d'or : +" + Rules.LAST_GOLD_SOLO + " (duel : +" + Rules.LAST_GOLD_DUEL + ")\n"
                + "Bonus ramass\u00e9 : +" + Rules.CATCH_BONUS + "\n"
                + "Balle restante en fin de partie : +" + Rules.BALL_BONUS + "\n"
                + "Duel, balle adverse sortie chez moi : -" + Rules.DUEL_PENALTY,
                50, 160, 24, PANEL_TEXT);

        panel(gc, 490, 100, 440, 470);
        heading(gc, "MULTIPLICATEUR", 510, 126);
        text(gc, "Il MULTIPLIE les points des briques :\nx2 = points doubl\u00e9s (ce n'est pas \"+1\").\n"
                + "x1 au d\u00e9part, x2 apr\u00e8s " + Rules.COMBO_STEP + " briques touch\u00e9es\nd'affil\u00e9e, x3 apr\u00e8s " + (2 * Rules.COMBO_STEP)
                + ", etc. jusqu'\u00e0 x" + Rules.MAX_MULTIPLIER + ".\n"
                + "Il retombe \u00e0 x1 d\u00e8s que votre raquette\ntouche votre balle, ou si vous la perdez.",
                510, 154, 20, PANEL_TEXT);
        heading(gc, "TABLEAU FINAL", 510, 300);
        Theme.left(gc, "Les lignes apparaissent une \u00e0 une :", 510, 326, Theme.SMALL, Theme.SOFT);
        String[] rows = { "Briques cass\u00e9es par la balle", "Briques sp\u00e9ciales et effets", "Briques d'or",
            "Derni\u00e8re brique d'or", "Bonus ramass\u00e9s", "Balles restantes", "Balle adverse perdue (duel)" };
        for (int i = 0; i < rows.length; i++) {
            double y = 358 + i * 30;
            if (i % 2 == 0) {
                gc.setFill(Color.rgb(255, 255, 255, 0.06));
                gc.fillRoundRect(506, y - 14, 408, 28, 10, 10);
            }
            gc.setFill(Theme.GOLD);
            gc.fillOval(516, y - 10, 20, 20);
            Theme.center(gc, String.valueOf(i + 1), 526, y, Theme.TINY, Theme.INK);
            Theme.leftFit(gc, rows[i], 548, y, Theme.SMALL, Theme.WHITE, 350);
        }
    }
}
