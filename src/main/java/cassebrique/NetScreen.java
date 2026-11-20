package cassebrique;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/** Ecrans "Jouer en reseau" : choix heberger / rejoindre, attente d'un joueur, saisie de l'adresse. */
final class NetScreen {
    private NetScreen() {
    }

    static void draw(GraphicsContext gc, Game g) {
        gc.setFill(Color.rgb(4, 5, 13, 0.55));
        gc.fillRect(0, 0, Game.W, Game.H);
        Theme.center(gc, "JOUER EN R\u00c9SEAU", Game.W / 2, 50, Theme.BIG, Color.hsb(210, 0.45, 1));

        switch (g.netScreen) {
        case 1:
            host(gc, g);
            break;
        case 2:
            join(gc, g);
            break;
        default:
            choice(gc, g);
            break;
        }

        Renderer.button(gc, Game.BACK_X, Game.NAV_Y, Game.BACK_W, Game.PAGE_H, g.hoverBack);
        Theme.center(gc, "< Retour", Game.BACK_X + Game.BACK_W / 2, Game.NAV_Y + Game.PAGE_H / 2, Theme.SMALL_BOLD, Theme.WHITE);
    }

    private static void bigButton(GraphicsContext gc, double y, boolean hover, String title, String sub) {
        Renderer.button(gc, Game.NB_X, y, Game.NB_W, Game.NB_H, hover);
        Theme.center(gc, title, Game.W / 2, y + 22, Theme.MED, Theme.WHITE);
        Theme.center(gc, sub, Game.W / 2, y + 42, Theme.TINY, Theme.SOFT);
    }

    private static void choice(GraphicsContext gc, Game g) {
        Theme.center(gc, "Deux ordinateurs, deux souris : l'h\u00f4te est BLANC (gauche), l'invit\u00e9 est NOIR (droite).",
                Game.W / 2, 118, Theme.SMALL, Theme.WHITE);
        Theme.center(gc, "Vous devez avoir la m\u00eame version du jeu sur les deux ordinateurs.", Game.W / 2, 144, Theme.SMALL, Theme.SOFT);
        bigButton(gc, Game.NB0_Y, g.hoverNetBtn == 0, "H\u00c9BERGER UNE PARTIE", "vous \u00eates BLANC");
        bigButton(gc, Game.NB1_Y, g.hoverNetBtn == 1, "REJOINDRE UNE PARTIE", "vous \u00eates NOIR");

        Theme.center(gc, "M\u00eame r\u00e9seau (maison, bureau, wifi) : l'invit\u00e9 tape l'adresse affich\u00e9e chez l'h\u00f4te.",
                Game.W / 2, 380, Theme.SMALL, Theme.WHITE);
        Theme.center(gc, "Par Internet : l'h\u00f4te ouvre le port TCP " + NetSession.PORT + " de sa box vers son ordinateur,",
                Game.W / 2, 408, Theme.SMALL, Theme.SOFT);
        Theme.center(gc, "ou bien vous utilisez un r\u00e9seau priv\u00e9 virtuel gratuit (Tailscale, ZeroTier, Radmin VPN).",
                Game.W / 2, 432, Theme.SMALL, Theme.SOFT);
        Theme.center(gc, "Si Windows demande d'autoriser le jeu dans le pare-feu, acceptez (r\u00e9seaux priv\u00e9s).",
                Game.W / 2, 466, Theme.SMALL, Theme.SOFT);
    }

    private static void host(GraphicsContext gc, Game g) {
        int dots = 1 + (int) (g.time * 2) % 3;
        StringBuilder sb = new StringBuilder("En attente d'un joueur");
        for (int i = 0; i < dots; i++) sb.append('.');
        Theme.center(gc, sb.toString(), Game.W / 2, 140, Theme.BIG, Theme.WHITE);
        Theme.center(gc, "Donnez-lui cette adresse :", Game.W / 2, 210, Theme.SMALL, Theme.SOFT);

        gc.setFill(Color.rgb(120, 170, 255, 0.14));
        gc.fillRoundRect(150, 232, 660, 64, 16, 16);
        gc.setStroke(Color.rgb(160, 190, 255, 0.6));
        gc.setLineWidth(2);
        gc.strokeRoundRect(150, 232, 660, 64, 16, 16);
        Theme.leftFit(gc, g.net.localAddresses, 170, 264, Theme.BIG, Theme.GOLD, 620);
        Theme.center(gc, "port " + NetSession.PORT + " (d\u00e9j\u00e0 inclus : l'invit\u00e9 n'a que l'adresse \u00e0 taper)", Game.W / 2, 322,
                Theme.TINY, Theme.SOFT);
        Theme.center(gc, "Plusieurs adresses ? Prenez celle qui ressemble \u00e0 celle de l'autre ordinateur (m\u00eames premiers chiffres),",
                Game.W / 2, 392, Theme.SMALL, Theme.SOFT);
        Theme.center(gc, "ou votre adresse Internet / VPN si vous jouez \u00e0 distance.", Game.W / 2, 416, Theme.SMALL, Theme.SOFT);
        if (!g.net.message.isEmpty()) Theme.center(gc, g.net.message, Game.W / 2, 470, Theme.SMALL_BOLD, Theme.RED);
    }

    private static void join(GraphicsContext gc, Game g) {
        Theme.center(gc, "Adresse de l'h\u00f4te", Game.W / 2, 140, Theme.BIG, Theme.WHITE);
        Theme.center(gc, "celle affich\u00e9e sur l'ordinateur qui h\u00e9berge la partie (ex. 192.168.1.20)", Game.W / 2, 196, Theme.SMALL, Theme.SOFT);

        gc.setFill(Color.rgb(255, 255, 255, 0.10));
        gc.fillRoundRect(Game.IN_X, Game.IN_Y, Game.IN_W, Game.IN_H, 12, 12);
        gc.setStroke(Color.web("#cfe0ff"));
        gc.setLineWidth(2);
        gc.strokeRoundRect(Game.IN_X, Game.IN_Y, Game.IN_W, Game.IN_H, 12, 12);
        boolean caret = (int) (g.time * 2) % 2 == 0;
        Theme.leftFit(gc, g.netInput + (caret ? "|" : ""), Game.IN_X + 14, Game.IN_Y + Game.IN_H / 2, Theme.BIG, Theme.WHITE,
                Game.IN_W - 28);

        boolean busy = g.net.status == NetSession.Status.CONNECTING;
        Renderer.button(gc, Game.GO_X, Game.GO_Y, Game.GO_W, Game.GO_H, g.hoverNetBtn == 0 && !busy);
        Theme.center(gc, busy ? "Connexion..." : "SE CONNECTER (Entr\u00e9e)", Game.W / 2, Game.GO_Y + Game.GO_H / 2, Theme.MED, Theme.WHITE);
        Theme.center(gc, "Ctrl+V pour coller l'adresse", Game.W / 2, 390, Theme.SMALL, Theme.SOFT);
        if (!g.net.message.isEmpty()) {
            boolean bad = g.net.status == NetSession.Status.IDLE;
            Theme.center(gc, g.net.message, Game.W / 2, 440, Theme.SMALL_BOLD, bad ? Theme.RED : Theme.WHITE);
        }
    }
}
