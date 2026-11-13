package cassebrique;

/**
 * Liste des effets sonores. Chaque son correspond au fichier
 * src/cassebrique/sounds/<nom en minuscules>.wav (ex. BRICK_HIT -> brick_hit.wav).
 * Pour changer un son : remplacer le fichier du meme nom par un vrai fichier .wav.
 * Un fichier absent = son simplement muet.
 */
enum Sfx {
    LAUNCH(0.6),       // lancement de la balle
    PADDLE(0.7),       // rebond sur la raquette
    WALL(0.35),        // rebond sur un mur
    BRICK_HIT(0.6),    // brique touchee mais pas detruite
    BRICK_BREAK(0.7),  // brique detruite
    STEEL(0.6),        // brique indestructible / givree touchee
    EXPLOSION(0.9),    // brique explosive
    LIGHTNING(0.9),    // brique foudre
    ROCK(0.9),         // brique roche
    POWERUP(0.7),      // bonus recupere / active
    MALUS(0.8),        // malus recupere
    SHIELD(0.7),       // mur de secours active
    GOLD(0.8),         // brique d'or
    BUTTON(0.7),       // brique bouton
    STICKY(0.6),       // balle collee a la raquette
    FROST(0.8),        // brique de givre
    TIMEWARP(0.9),     // brique du temps
    NIGHT(0.9),        // brique de nuit
    TELEPORT(0.7),     // teleportation
    GHOST(0.5),        // fantome envoye
    STAR_LAUNCH(0.7),  // etoile filante lancee
    STAR_EXPLODE(0.9), // etoile filante qui explose
    LIFE_LOST(0.9),    // balle perdue de son cote
    PENALTY(0.8),      // duel : balle adverse perdue chez soi (points perdus)
    LEVEL_CLEAR(0.9),  // niveau termine
    GAME_OVER(0.9),    // partie perdue
    COUNTDOWN(0.7),    // duel : bip du decompte
    GO(0.8),           // duel : fin du decompte
    TALLY(0.6),        // tableau des scores : une ligne apparait
    TOTAL(0.9),        // tableau des scores : total
    RECORD(0.9),       // nouveau record / victoire
    ELIMINATION(1.0),  // duel : un joueur n'a plus de balles (defaite)
    MENU_CLICK(0.6);   // clic dans le menu

    final double volume;

    Sfx(double volume) {
        this.volume = volume;
    }

    String fileName() {
        return name().toLowerCase() + ".wav";
    }
}
