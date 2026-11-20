package cassebrique;

/** Une brique. Les briques mobiles bougent : x et y changent. */
final class Brick {
    enum Kind { NORMAL, STEEL, EXPLOSIVE, GOLD, BONUS, LIGHTNING, ROCK, BUTTON, PHASE, FROST, TIME, NIGHT, MOVER, TELEPORT, MALUS, RAINBOW, SWAP }

    private static final double[] HUES = { 190, 285, 28 };

    final int col, row;
    double x, y;
    final double w, h;
    final Kind kind;
    final PowerUp.Type bonus; // seulement pour Kind.BONUS
    final int maxHp;
    int hp;
    boolean alive = true;
    boolean phaseSolid; // Kind.PHASE : true = visible et solide
    double flash;
    Object fxBase, fxDark; // couleurs mises en cache par Art (evite des allocations a chaque image)
    int fxHp = -1;
    Object fxSprite;       // image pre-calculee de l'aspect fixe de la brique (Art)
    int fxSpriteHp = -1;
    int fxEpoch = -1;
    double frost;       // secondes restantes pendant lesquelles la brique est incassable (givre)

    // Brique mobile : indices des points de passage (precedent, courant, cible)
    int wpPrev = -1, wpCur = -1, wpTo = -1;
    double pause;

    /** Les caracteres des niveaux sont decrits dans Levels.java. */
    Brick(int col, int row, double x, double y, double w, double h, char c) {
        this.col = col;
        this.row = row;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        PowerUp.Type b = null;
        Kind k = Kind.NORMAL;
        int hitPoints = 1;
        switch (c) {
        case '1': case '2': case '3': hitPoints = c - '0'; break;
        case '#': k = Kind.STEEL; break;
        case 'X': k = Kind.EXPLOSIVE; break;
        case 'G': k = Kind.GOLD; break;
        case 'Z': k = Kind.LIGHTNING; break;
        case 'R': k = Kind.ROCK; break;
        case 'B': k = Kind.BUTTON; break;
        case 'T': k = Kind.PHASE; phaseSolid = true; break;
        case 't': k = Kind.PHASE; phaseSolid = false; break;
        case 'I': k = Kind.FROST; break;
        case 'H': k = Kind.TIME; break;
        case 'N': k = Kind.NIGHT; break;
        case 'M': k = Kind.MOVER; break;
        case 'P': k = Kind.TELEPORT; break;
        case 'y': k = Kind.MALUS; break;
        case 'a': k = Kind.RAINBOW; break;
        case 'x': k = Kind.SWAP; break;
        case 'l': k = Kind.BONUS; b = PowerUp.Type.LAVA; break;
        case 'v': k = Kind.BONUS; b = PowerUp.Type.DRUNK; break;
        case 'w': k = Kind.BONUS; b = PowerUp.Type.WIDE; break;
        case 'm': k = Kind.BONUS; b = PowerUp.Type.MULTI; break;
        case 's': k = Kind.BONUS; b = PowerUp.Type.SLOW; break;
        case 'f': k = Kind.BONUS; b = PowerUp.Type.FIRE; break;
        case 'c': k = Kind.BONUS; b = PowerUp.Type.STICKY; break;
        case 'u': k = Kind.BONUS; b = PowerUp.Type.WALL; break;
        case '+': k = Kind.BONUS; b = PowerUp.Type.LIFE; break;
        case 'g': k = Kind.BONUS; b = PowerUp.Type.GHOSTS; break;
        case 'e': k = Kind.BONUS; b = PowerUp.Type.STAR; break;
        case 'r': k = Kind.BONUS; b = PowerUp.Type.SHRINK; break;
        case 'n': k = Kind.BONUS; b = PowerUp.Type.NOBREAK; break;
        default: break;
        }
        this.kind = k;
        this.bonus = b;
        this.hp = hitPoints;
        this.maxHp = hitPoints;
    }

    /** Ne peut jamais etre detruite par la balle ni par un effet. */
    boolean indestructible() {
        return kind == Kind.STEEL || kind == Kind.BUTTON || kind == Kind.PHASE
                || kind == Kind.MOVER || kind == Kind.TELEPORT || kind == Kind.RAINBOW || kind == Kind.SWAP;
    }

    /** Incassable pour le moment : indestructible, ou givree. */
    boolean unbreakable() {
        return indestructible() || frost > 0;
    }

    /** Presente pour les collisions (les teleporteurs et les interrupteurs caches se traversent). */
    boolean isSolid() {
        return alive && kind != Kind.TELEPORT && kind != Kind.RAINBOW && (kind != Kind.PHASE || phaseSolid);
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
        case FROST: return 195;
        case TIME: return 265;
        case NIGHT: return 245;
        case MOVER: return 50;
        case TELEPORT: return 285;
        case MALUS: return 62;
        case RAINBOW: return 320;
        case SWAP: return 0;
        case BONUS: return bonus.hue;
        default: return HUES[Math.min(Math.max(hp, 1), 3) - 1];
        }
    }
}
