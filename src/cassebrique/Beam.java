package cassebrique;

/** Effet visuel d'un eclair (vertical) ou d'un eboulement (horizontal). */
final class Beam {
    final boolean vertical;
    final double pos; // x du centre de la colonne, ou y du centre de la ligne
    final double maxLife = 0.45;
    double life = 0.45;
    final long seed = System.nanoTime();

    Beam(boolean vertical, double pos) {
        this.vertical = vertical;
        this.pos = pos;
    }
}
