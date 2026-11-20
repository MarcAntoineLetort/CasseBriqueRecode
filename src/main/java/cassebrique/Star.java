package cassebrique;

/** Etoile filante (bonus) : explose au contact de la premiere brique. */
final class Star {
    final int owner;
    double x, y, vx, vy, t;

    Star(int owner, double x, double y, double vx, double vy) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }
}
