package cassebrique;

/** Bonus qui tombe quand une brique est detruite (ou bonus direct d'une brique bonus). */
final class PowerUp {
    enum Type {
        WIDE("L", 140), MULTI("M", 200), SLOW("S", 270), FIRE("F", 25), STICKY("C", 60), LIFE("+", 350);

        final String label;
        final double hue;

        Type(String label, double hue) {
            this.label = label;
            this.hue = hue;
        }
    }

    final Type type;
    double x, y, t;

    PowerUp(Type type, double x, double y) {
        this.type = type;
        this.x = x;
        this.y = y;
    }
}
