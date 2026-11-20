package cassebrique;

/** Effet visuel : arc-en-ciel droit entre deux briques arc-en-ciel qui viennent d'etre utilisees. */
final class Rainbow {
    final double x1, y1, x2, y2;
    static final double MAX_LIFE = 0.9;
    double life = MAX_LIFE;

    Rainbow(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
}
