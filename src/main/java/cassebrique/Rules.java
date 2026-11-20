package cassebrique;

/**
 * Toutes les regles chiffrees du jeu au meme endroit : points, multiplicateur, vies, durees des effets.
 * Il suffit de changer une valeur ici : le jeu ET les textes de l'ecran "Informations" s'adaptent.
 */
final class Rules {
    private Rules() {
    }

    // ---- Points
    static final int BRICK_POINTS = 10;      // brique normale : BRICK_POINTS x resistance (1 a 3)
    static final int DAMAGE_POINTS = 2;      // coup qui n'acheve pas une brique resistante (0 = pas de points)
    static final int GOLD_POINTS = 150;      // brique d'or
    static final int LAST_GOLD_SOLO = 500;   // derniere brique d'or du niveau, en solo
    static final int LAST_GOLD_DUEL = 200;   // derniere brique d'or du niveau, en duel
    static final int CATCH_BONUS = 50;       // bonus ramasse (pas les malus)
    static final int BALL_BONUS = 150;       // par balle restante a la fin (un peu plus que la penalite du duel)
    static final int DUEL_PENALTY = 100;     // duel : points perdus quand la balle adverse sort chez moi
    static final int LIFE_POINTS = 0;        // (reserve)

    // ---- Multiplicateur (s'applique aux points des briques : normales, resistantes touchees, or)
    static final int COMBO_STEP = 4;         // briques touchees d'affilee pour gagner +1 au multiplicateur
    static final int MAX_MULTIPLIER = 8;

    // ---- Vies et hasard
    static final int LIVES = 3;              // balles de depart
    static final int MAX_LIVES = 6;
    static final double DROP_CHANCE = 0.14;  // chance qu'une brique normale detruite laisse tomber un bonus

    // ---- Durees (secondes)
    static final double WIDE_TIME = 12;
    static final double SHRINK_TIME = 10;
    static final double SLOW_TIME = 8;
    static final double FIRE_TIME = 6;
    static final double STICKY_TIME = 15;
    static final double WALL_TIME = 8;
    static final double GHOST_TIME = 5;
    static final double STAR_TIME = 8;
    static final double NOBREAK_TIME = 5;
    static final double NIGHT_TIME = 8;
    static final double FROST_TIME = 10;
    static final double LAVA_TIME = 6;       // bonus balle de lave
    static final double DRUNK_TIME = 6;      // malus ivresse
    static final double SWAP_DELAY = 3;      // duel : decompte du joueur qui se retrouve sans balle apres un echange
    static final double SWAP_COOLDOWN = 0.5; // duel : delai minimal entre deux echanges
    static final double SERVE_DELAY = 3;     // duel : decompte avant le depart de la balle
    static final double RESPAWN_DELAY = 2;   // duel : decompte apres une balle perdue

    // ---- Tableau des scores (duel)
    static final double ELIM_BANNER_DELAY = 1.2; // pause entre le total et la banniere rouge "n'a plus de balles"
    static final double ELIM_BANNER_TIME = 2.0;  // duree d'affichage de la banniere rouge (puis le gagnant apparait)

    static int lastGold(boolean duel) {
        return duel ? LAST_GOLD_DUEL : LAST_GOLD_SOLO;
    }

    /** "12 s" (entier si possible). */
    static String sec(double s) {
        return (s == Math.rint(s) ? String.valueOf((int) s) : String.valueOf(s)) + " s";
    }
}
