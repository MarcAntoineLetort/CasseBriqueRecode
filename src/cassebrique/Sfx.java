package cassebrique;

/**
 * Liste des effets sonores. Chaque son correspond au fichier
 * src/cassebrique/sounds/<nom en minuscules>.wav (ex. BRICK_HIT -> brick_hit.wav).
 * Pour changer un son : remplacer le fichier du meme nom par un vrai fichier .wav.
 * Un fichier absent = son simplement muet.
 */
enum Sfx {
    LAUNCH(0.6),      // lancement de la balle
    PADDLE(0.7),      // rebond sur la raquette
    WALL(0.35),       // rebond sur un mur
    BRICK_HIT(0.6),   // brique touchee mais pas detruite
    BRICK_BREAK(0.7), // brique detruite
    STEEL(0.6),       // brique indestructible touchee
    EXPLOSION(0.9),   // brique explosive
    LIGHTNING(0.9),   // brique foudre
    ROCK(0.9),        // brique roche
    POWERUP(0.7),     // bonus recupere / active
    GOLD(0.8),        // brique d'or
    BUTTON(0.7),      // brique bouton
    STICKY(0.6),      // balle collee a la raquette
    LIFE_LOST(0.9),   // balle perdue
    LEVEL_CLEAR(0.9), // niveau termine
    GAME_OVER(0.9),   // partie perdue
    MENU_CLICK(0.6);  // clic dans le menu

    final double volume;

    Sfx(double volume) {
        this.volume = volume;
    }

    String fileName() {
        return name().toLowerCase() + ".wav";
    }
}
