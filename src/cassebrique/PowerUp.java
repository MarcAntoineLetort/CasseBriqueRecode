package cassebrique;

/** Bonus / malus qui tombe quand une brique est detruite (ou effet direct d'une brique bonus). */
final class PowerUp {
    enum Type {
        WIDE("L", 140, false, 1.0),
        MULTI("M", 200, false, 1.0),
        SLOW("S", 270, false, 1.0),
        FIRE("F", 25, false, 1.0),
        STICKY("C", 60, false, 1.0),
        WALL("=", 300, false, 1.0),
        LIFE("+", 350, false, 0.4),
        SHRINK("-", 0, true, 0.8),        // malus : raquette reduite
        NOBREAK("\u00d7", 0, true, 0.8);  // malus : la balle ne casse rien

        final String label;
        final double hue;
        final boolean bad;
        final double weight; // frequence relative dans les bonus aleatoires

        Type(String label, double hue, boolean bad, double weight) {
            this.label = label;
            this.hue = hue;
            this.bad = bad;
            this.weight = weight;
        }
    }

    final Type type;
    double x, y, vx, vy, t;

    PowerUp(Type type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
