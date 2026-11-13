package cassebrique;

/** Petit fantome extraterrestre envoye par la raquette (bonus "fantomes"). */
final class Ghost {
    final int owner;
    double x, y, vx, vy, t;

    Ghost(int owner, double x, double y, double vx, double vy) {
        this.owner = owner;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }
}
