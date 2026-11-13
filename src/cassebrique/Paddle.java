package cassebrique;

/**
 * Une raquette. Horizontale en bas de l'ecran (solo) ou verticale a gauche / a droite (duel).
 * Toute la geometrie est exprimee avec :
 *  - pos    : position le long de la raquette (x si horizontale, y si verticale)
 *  - face   : coordonnee de la face qui touche la balle (y si horizontale, x si verticale)
 *  - toward : sens (+1 / -1) dans lequel la balle arrive sur la raquette, sur l'axe perpendiculaire
 */
final class Paddle {
    static final double T = 14;  // epaisseur
    static final int CATS = 7;   // lignes du tableau des scores (voir Game.CAT_*)

    final int index;      // 0 = solo ou joueur blanc (gauche), 1 = joueur noir (droite)
    final boolean vertical;
    final double face;
    final int toward;
    final double baseLen;

    double pos, len, targetLen, vel;
    int lives = 3;        // balles restantes
    int score, combo;
    final int[] pts = new int[CATS]; // points par categorie (tableau final)
    boolean keyNeg, keyPos;          // gauche/haut et droite/bas

    // Effets (secondes restantes)
    double wideTimer, shrinkTimer, stickyTimer, fireTimer, wallTimer, nobreakTimer;
    double ghostTimer, ghostNext, starTimer;
    double serveTimer;    // duel : decompte avant de pouvoir lancer sa balle
    int serveSecond;

    Paddle(int index, boolean vertical, double face, int toward, double baseLen, double pos) {
        this.index = index;
        this.vertical = vertical;
        this.face = face;
        this.toward = toward;
        this.baseLen = baseLen;
        this.pos = pos;
        this.len = baseLen;
        this.targetLen = baseLen;
    }

    void award(int cat, int amount) {
        pts[cat] += amount;
        score += amount;
    }

    void updateTargetLen() {
        targetLen = baseLen * (wideTimer > 0 ? 1.64 : 1.0) * (shrinkTimer > 0 ? 0.55 : 1.0);
    }

    void resetEffects() {
        wideTimer = shrinkTimer = stickyTimer = fireTimer = wallTimer = nobreakTimer = 0;
        ghostTimer = ghostNext = starTimer = 0;
    }

    /** Nouveau niveau de la campagne : on garde les points et les balles restantes. */
    void resetForLevel(double newPos) {
        resetEffects();
        pos = newPos;
        len = baseLen;
        targetLen = baseLen;
        combo = 0;
        vel = 0;
        serveTimer = 0;
        keyNeg = false;
        keyPos = false;
    }

    private double minAcross() {
        return Math.min(face, face + toward * T);
    }

    double rectX() {
        return vertical ? minAcross() : pos - len / 2;
    }

    double rectY() {
        return vertical ? pos - len / 2 : minAcross();
    }

    double rectW() {
        return vertical ? T : len;
    }

    double rectH() {
        return vertical ? len : T;
    }

    /** Face avant du mur de secours (bonus "mur"), derriere la raquette. */
    double wallFace() {
        return face + toward * (T + 8);
    }
}
