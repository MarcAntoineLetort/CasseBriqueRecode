package cassebrique;

/** Une brique (donnees seulement). */
final class Brick {
    enum Kind { NORMAL, STEEL, EXPLOSIVE, GOLD, BONUS, LIGHTNING, ROCK, BUTTON, PHASE }

    private static final double[] HUES = { 190, 285, 28 };

    final int col, row;
    final double x, y;
    final Kind kind;
    final PowerUp.Type bonus; // seulement pour Kind.BONUS
    final int maxHp;
    int hp;
    boolean alive = true;
    boolean phaseSolid; // seulement pour Kind.PHASE : true = visible et solide
    double flash;

    /**
     * Caracteres des niveaux :
     * '1' '2' '3' normale (resistance) | '#' acier | 'X' explosive | 'G' or
     * 'Z' foudre (colonne) | 'R' roche (ligne) | 'B' bouton
     * 'T' brique-interrupteur visible au depart | 't' brique-interrupteur cachee au depart
     * 'w' 'm' 's' 'f' 'c' '+' : bonus direct (large, multi, ralenti, feu, collante, vie)
     */
    Brick(int col, int row, double x, double y, char c) {
        this.col = col;
        this.row = row;
        this.x = x;
        this.y = y;
        PowerUp.Type b = null;
        Kind k = Kind.NORMAL;
        int h = 1;
        switch (c) {
        case '1': case '2': case '3': h = c - '0'; break;
        case '#': k = Kind.STEEL; break;
        case 'X': k = Kind.EXPLOSIVE; break;
        case 'G': k = Kind.GOLD; break;
        case 'Z': k = Kind.LIGHTNING; break;
        case 'R': k = Kind.ROCK; break;
        case 'B': k = Kind.BUTTON; break;
        case 'T': k = Kind.PHASE; phaseSolid = true; break;
        case 't': k = Kind.PHASE; phaseSolid = false; break;
        case 'w': k = Kind.BONUS; b = PowerUp.Type.WIDE; break;
        case 'm': k = Kind.BONUS; b = PowerUp.Type.MULTI; break;
        case 's': k = Kind.BONUS; b = PowerUp.Type.SLOW; break;
        case 'f': k = Kind.BONUS; b = PowerUp.Type.FIRE; break;
        case 'c': k = Kind.BONUS; b = PowerUp.Type.STICKY; break;
        case '+': k = Kind.BONUS; b = PowerUp.Type.LIFE; break;
        default: break;
        }
        this.kind = k;
        this.bonus = b;
        this.hp = h;
        this.maxHp = h;
    }

    /** Ne peut jamais etre detruite par la balle ni par un effet. */
    boolean indestructible() {
        return kind == Kind.STEEL || kind == Kind.BUTTON || kind == Kind.PHASE;
    }

    /** Brique presente pour les collisions. */
    boolean isSolid() {
        return alive && (kind != Kind.PHASE || phaseSolid);
    }

    double hue() {
        switch (kind) {
        case EXPLOSIVE: return 8;
        case GOLD: return 48;
        case LIGHTNING: return 55;
        case ROCK: return 28;
        case BUTTON: return 0;
        case PHASE: return 185;
        case STEEL: return 215;
        case BONUS: return bonus.hue;
        default: return HUES[Math.min(Math.max(hp, 1), 3) - 1];
        }
    }
}
