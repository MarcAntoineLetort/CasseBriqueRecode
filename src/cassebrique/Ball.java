package cassebrique;

/** Une balle : position, direction unitaire (dx, dy) et petite trainee. */
final class Ball {
    static final int TRAIL = 14;

    double x, y, dx, dy;
    boolean stuck, dead;
    double stickOffset; // position de la balle collee, relative au centre de la raquette

    private final double[] tx = new double[TRAIL];
    private final double[] ty = new double[TRAIL];
    private int head, size;

    void pushTrail() {
        tx[head] = x;
        ty[head] = y;
        head = (head + 1) % TRAIL;
        if (size < TRAIL) size++;
    }

    void clearTrail() {
        size = 0;
    }

    int trailSize() {
        return size;
    }

    /** i = 0 : le plus ancien point. */
    double trailX(int i) {
        return tx[((head - size + i) % TRAIL + TRAIL) % TRAIL];
    }

    double trailY(int i) {
        return ty[((head - size + i) % TRAIL + TRAIL) % TRAIL];
    }
}
