package cassebrique;

/** Effet visuel d'un eclair (vertical) ou d'un eboulement (horizontal). */
final class Beam {
    final boolean vertical;
    final double pos;    // x du centre de la colonne, ou y du centre de la ligne
    final double a, b;   // debut et fin le long de l'axe (y pour un eclair, x pour un eboulement)
    final double thick;  // epaisseur de la bande (eboulement)
    final double maxLife = 0.45;
    double life = 0.45;
    final long seed = System.nanoTime();

    Beam(boolean vertical, double pos, double a, double b, double thick) {
        this.vertical = vertical;
        this.pos = pos;
        this.a = a;
        this.b = b;
        this.thick = thick;
    }
}
