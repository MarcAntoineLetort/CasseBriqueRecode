package cassebrique;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Ecran "Informations" : controles, regles, bonus et malus, briques speciales, comptage des points. */
final class InfoScreen {
    private InfoScreen() {
    }

    private static final String[] TITLES = {
        "Contr\u00f4les", "R\u00e8gles du jeu", "Bonus et malus", "Briques sp\u00e9ciales", "Comptage des points" };

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
            bricks(gc, t);
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

    // ------------------------------------------------------------------ pages

    private static void controls(GraphicsContext gc) {
        panel(gc, 30, 100, 440, 300);
        heading(gc, "1 JOUEUR", 50, 126);
        Theme.lines(gc, "D\u00e9placer la raquette :\nsouris, fl\u00e8ches gauche / droite ou Q / D.\n\n"
                + "Lancer la balle, la relancer\n(balle collante) ou tirer une \u00e9toile\nfilante : clic ou ESPACE.\n\n"
                + "Si le curseur sort de la fen\u00eatre,\nla raquette va jusqu'au bord.",
                50, 160, 21, Theme.SMALL, Theme.WHITE);

        panel(gc, 490, 100, 440, 300);
        heading(gc, "2 JOUEURS (clavier)", 510, 126);
        gc.setFill(Theme.PAPER);
        gc.fillRoundRect(510, 146, 190, 26, 13, 13);
        Theme.center(gc, "BLANC - gauche", 605, 159, Theme.SMALL_BOLD, Theme.INK);
        Theme.lines(gc, "Monter : Z (ou W)\nDescendre : S\nLancer / \u00e9toile : ESPACE", 510, 192, 21, Theme.SMALL, Theme.WHITE);
        gc.setFill(Theme.INK);
        gc.fillRoundRect(510, 274, 190, 26, 13, 13);
        gc.setStroke(Color.web("#f4f4fb"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(510, 274, 190, 26, 13, 13);
        Theme.center(gc, "NOIR - droite", 605, 287, Theme.SMALL_BOLD, Theme.PAPER);
        Theme.lines(gc, "Monter : fl\u00e8che haut\nDescendre : fl\u00e8che bas\nLancer / \u00e9toile : ENTR\u00c9E", 510, 318, 21,
                Theme.SMALL, Theme.WHITE);

        panel(gc, 30, 416, 900, 150);
        heading(gc, "PARTOUT", 50, 440);
        Theme.lines(gc, "P ou \u00c9chap : pause (la pause rappelle les contr\u00f4les)  -  M : retour au menu depuis la pause\n"
                + "N : son activ\u00e9 / coup\u00e9  -  F11 (ou F) : plein \u00e9cran  -  la fen\u00eatre peut \u00eatre redimensionn\u00e9e\n"
                + "Dans le menu : fl\u00e8ches gauche / droite pour changer de page, clic sur un niveau pour le lancer.",
                50, 474, 26, Theme.SMALL, Theme.WHITE);
    }

    private static void rules(GraphicsContext gc) {
        panel(gc, 30, 100, 440, 470);
        heading(gc, "SOLO", 50, 126);
        Theme.lines(gc, "\u2022 Vous avez 3 balles (vies) : une balle\n   perdue = une vie en moins.\n"
                + "\u2022 Campagne : les niveaux solo s'encha\u00eenent,\n   le score se cumule.\n"
                + "\u2022 Niveau avec briques d'or : il s'arr\u00eate d\u00e8s\n   que tout l'or est pris.\n"
                + "\u2022 Chaque brique touch\u00e9e sans toucher la\n   raquette augmente le multiplicateur\n   (jusqu'\u00e0 x8).\n"
                + "\u2022 L'angle de renvoi d\u00e9pend de l'endroit\n   touch\u00e9 sur la raquette.\n"
                + "\u2022 Un record est gard\u00e9 par niveau et pour\n   la campagne.",
                50, 160, 22, Theme.SMALL, Theme.WHITE);

        panel(gc, 490, 100, 440, 470);
        heading(gc, "DUEL", 510, 126);
        Theme.lines(gc, "\u2022 Chacun a sa balle et 3 vies. D\u00e9compte de\n   3 secondes avant de pouvoir lancer.\n"
                + "\u2022 Ma balle sort de MON c\u00f4t\u00e9 : je perds\n   une vie.\n"
                + "\u2022 Ma balle sort du c\u00f4t\u00e9 ADVERSE : c'est\n   l'adversaire qui perd 100 points.\n"
                + "\u2022 Plus de vie = d\u00e9faite, m\u00eame avec plus\n   de points.\n"
                + "\u2022 Sinon, quand le niveau est fini : le\n   plus de points gagne (balles restantes\n   compt\u00e9es : +150 chacune).\n"
                + "\u2022 Les points vont au propri\u00e9taire de la\n   balle. Les scores sont cach\u00e9s pendant\n   la partie, r\u00e9v\u00e9l\u00e9s \u00e0 la fin.",
                510, 160, 22, Theme.SMALL, Theme.WHITE);
    }

    private static void bonuses(GraphicsContext gc, double t) {
        String[][] items = {
            { "WIDE", "Raquette large", "12 secondes" },
            { "MULTI", "Multi-balles", "chaque balle en donne trois" },
            { "SLOW", "Ralenti", "balles plus lentes, 8 secondes" },
            { "FIRE", "Boule de feu", "traverse les briques, 6 secondes" },
            { "STICKY", "Balle collante", "reste sur la raquette, clic pour\nrelancer (15 secondes)" },
            { "WALL", "Mur de secours", "derri\u00e8re la raquette, 8 secondes" },
            { "GHOSTS", "Fant\u00f4mes", "un par seconde pendant 5 s, chacun\ncasse une brique puis dispara\u00eet" },
            { "STAR", "\u00c9toile filante", "8 s pour un clic : explose au contact\nde la 1re brique (9 briques cass\u00e9es)" },
            { "LIFE", "Balle en plus", "une vie suppl\u00e9mentaire (6 maximum)" },
            { "SHRINK", "MALUS : raquette r\u00e9duite", "10 secondes" },
            { "NOBREAK", "MALUS : balle inoffensive", "ne casse rien pendant 5 secondes" } };
        for (int i = 0; i < items.length; i++) {
            double x = i < 6 ? 40 : 500;
            double y = 120 + (i < 6 ? i : i - 6) * 62;
            PowerUp.Type type = PowerUp.Type.valueOf(items[i][0]);
            Art.capsule(gc, type, x + 30, y, t);
            Theme.left(gc, items[i][1], x + 66, y - 12, Theme.SMALL_BOLD, type.bad ? Theme.RED : Theme.WHITE);
            Theme.lines(gc, items[i][2], x + 66, y + 8, 17, Theme.TINY, Theme.SOFT);
        }
        Theme.lines(gc, "Une brique marqu\u00e9e d'un symbole donne son effet d\u00e8s qu'on la touche.\n"
                + "Les m\u00eames effets tombent aussi au hasard des briques normales :\n"
                + "capsule color\u00e9e = bonus, capsule sombre bord\u00e9e de rouge = malus.\n"
                + "Les effets s'appliquent au joueur qui les re\u00e7oit.",
                500, 440, 22, Theme.SMALL, Theme.SOFT);
    }

    private static void brick(GraphicsContext gc, char c, double x, double y, double t) {
        Brick b = new Brick(0, 0, x, y, Game.SOLO_BW, Game.SOLO_BH, c);
        if (c == 'T') b.phaseSolid = true;
        Art.brick(gc, b, t);
    }

    private static void bricks(GraphicsContext gc, double t) {
        Object[][] items = {
            { '2', "Normale", "1 \u00e0 3 coups : la couleur indique la r\u00e9sistance" },
            { '#', "Acier", "indestructible" },
            { 'X', "Explosive", "d\u00e9truit les 8 briques voisines" },
            { 'G', "Or", "valeur forte ; le niveau s'arr\u00eate quand tout\nl'or est pris (+500 pour la derni\u00e8re)" },
            { 'Z', "Foudre", "d\u00e9truit toute sa colonne" },
            { 'R', "Roche", "d\u00e9truit toute sa ligne" },
            { 'B', "Bouton", "fait basculer les briques-interrupteurs" },
            { 'T', "Interrupteur", "solide ou fant\u00f4me (contour discret) :\nchange \u00e0 chaque pression d'un bouton" },
            { 'I', "Givre", "les 8 voisines sont incassables 10 s" },
            { 'H', "Temps", "les briques d\u00e9truites (sauf or et temps)\nr\u00e9apparaissent" },
            { 'N', "Nuit", "tout devient noir 8 s : seuls la balle,\nla raquette et les effets restent visibles" },
            { 'M', "Mobile", "indestructible, patrouille entre ses balises ;\nrecule si elle devait \u00e9craser la balle" },
            { 'P', "T\u00e9l\u00e9portation", "traversable : la balle ressort d'un\nautre t\u00e9l\u00e9porteur, avec son \u00e9lan" },
            { 'w', "Bonus / malus", "effet imm\u00e9diat au contact (voir page\npr\u00e9c\u00e9dente)" } };
        for (int i = 0; i < items.length; i++) {
            double x = i < 7 ? 30 : 490;
            double y = 98 + (i < 7 ? i : i - 7) * 66;
            brick(gc, (Character) items[i][0], x, y, t);
            Theme.left(gc, (String) items[i][1], x + 76, y + 4, Theme.SMALL_BOLD, Theme.WHITE);
            Theme.lines(gc, (String) items[i][2], x + 76, y + 24, 16, Theme.TINY, Theme.SOFT);
        }
    }

    private static void points(GraphicsContext gc) {
        panel(gc, 30, 100, 440, 470);
        heading(gc, "POINTS GAGN\u00c9S", 50, 126);
        Theme.lines(gc, "Brique normale : 10 x r\u00e9sistance\nBrique d'or : 150\n"
                + "(le tout x multiplicateur, jusqu'\u00e0 x8)\n\n"
                + "Derni\u00e8re brique d'or du niveau : +500\n"
                + "Bonus ramass\u00e9 : +50\n"
                + "Balle restante en fin de partie : +150\n\n"
                + "Duel : balle adverse perdue chez moi :\n-100\n\n"
                + "Multiplicateur : +1 toutes les 4 briques\ntouch\u00e9es sans toucher sa raquette.",
                50, 160, 22, Theme.SMALL, Theme.WHITE);

        panel(gc, 490, 100, 440, 470);
        heading(gc, "TABLEAU FINAL", 510, 126);
        Theme.lines(gc, "Les lignes apparaissent une \u00e0 une :\n\n"
                + "1. Briques cass\u00e9es par la balle\n    (normales et r\u00e9sistantes, balles multiples\n    comprises)\n"
                + "2. Briques sp\u00e9ciales et effets\n    (explosion, foudre, roche, fant\u00f4mes,\n    \u00e9toile...)\n"
                + "3. Briques d'or, quelle que soit la mani\u00e8re\n"
                + "4. Derni\u00e8re brique d'or\n"
                + "5. Bonus ramass\u00e9s\n"
                + "6. Balles restantes\n"
                + "7. Balle adverse perdue (duel)\n\n"
                + "Puis le total, et \"nouveau record\".",
                510, 160, 21, Theme.SMALL, Theme.WHITE);
    }
}
