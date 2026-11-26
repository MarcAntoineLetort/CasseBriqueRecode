package cassebrique;

/**
 * Liste des effets sonores. Chaque son correspond au fichier
 * src/cassebrique/sounds/<nom en minuscules>.wav (ex. BRICK_HIT -> brick_hit.wav).
 * Pour changer un son : remplacer le fichier du meme nom par un vrai fichier .wav.
 * Un fichier absent = son simplement muet.
 */
enum Sfx {
    LAUNCH(0.6, 1),       // lancement de la balle
    PADDLE(0.7, 1),       // rebond sur la raquette
    WALL(0.35, 0),        // rebond sur un mur
    BRICK_HIT(0.6, 0),    // brique touchee mais pas detruite
    BRICK_BREAK(0.7, 0),  // brique detruite
    STEEL(0.6, 0),        // brique indestructible / givree touchee
    EXPLOSION(0.9, 2),    // brique explosive
    LIGHTNING(0.9, 2),    // brique foudre
    ROCK(0.9, 2),         // brique roche
    POWERUP(0.7, 2),      // bonus recupere / active
    MALUS(0.8, 2),        // malus recupere
    SHIELD(0.7, 2),       // mur de secours active
    GOLD(0.8, 2),         // brique d'or
    BUTTON(0.7, 1),       // brique bouton
    STICKY(0.6, 1),       // balle collee a la raquette
    FROST(0.8, 1),        // brique de givre
    TIMEWARP(0.9, 2),     // brique du temps
    NIGHT(0.9, 2),        // brique de nuit
    TELEPORT(0.7, 1),     // teleportation
    GHOST(1.0, 2),        // fantome envoye / fantome qui casse une brique (bien present)
    STAR_LAUNCH(0.7, 1),  // etoile filante lancee
    STAR_EXPLODE(0.9, 1), // etoile filante qui explose
    LIFE_LOST(0.9, 2),    // balle perdue de son cote
    PENALTY(0.8, 2),      // duel : balle adverse perdue chez soi (points perdus)
    LEVEL_CLEAR(0.9, 3),  // niveau termine
    GAME_OVER(0.9, 3),    // partie perdue
    COUNTDOWN(0.7, 2),    // duel : bip du decompte
    GO(0.8, 2),           // duel : fin du decompte
    TALLY(0.6, 2),        // tableau des scores : une ligne apparait
    TOTAL(0.9, 3),        // tableau des scores : total
    RECORD(0.9, 3),       // nouveau record / victoire
    ELIMINATION(1.0, 3),  // duel : un joueur n'a plus de balles (defaite)
    RAINBOW(0.9, 2),      // brique arc-en-ciel utilisee
    SWAP(0.9, 2),         // brique d'echange : les balles changent de camp
    LAVA(0.9, 2),         // balle de lave : explosion autour de la brique touchee
    MENU_CLICK(0.6, 1);   // clic dans le menu

    final double volume;
    /** Priorite : quand beaucoup de sons jouent en meme temps, JavaFX coupe d'abord les moins prioritaires. */
    final int priority;

    Sfx(double volume, int priority) {
        this.volume = volume;
        this.priority = priority;
    }

    String fileName() {
        return name().toLowerCase() + ".wav";
    }
}
