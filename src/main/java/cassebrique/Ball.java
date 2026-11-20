package cassebrique;

/** Une balle : position, direction unitaire (dx, dy) et petite trainee. */
final class Ball {
    static final int TRAIL = 14;

    double x, y, dx, dy;
    boolean stuck, dead;
    double speedNow;    // vitesse courante en px/s (transmise a l'invite)
    double speed;       // invite : vitesse recue, pour la prediction entre deux images
    double wobble;      // phase de l'oscillation (malus ivresse)
    double tpCooldown;  // apres une teleportation : evite de rebondir de brique en brique
    double stickOffset; // position de la balle collee, relative au centre de la raquette
    int owner;          // index du joueur qui l'a touchee en dernier (0 en solo)
    int lostSide = -1;  // duel : cote par lequel elle est sortie (0 = gauche, 1 = droite)

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
