package cassebrique;

import java.util.ArrayList;
import java.util.List;

/**
 * Tous les niveaux. Pour en ajouter : une ligne solo(...) ou duel(...) a la fin de la liste,
 * le menu cree les pages tout seul. (Les records sont memorises par nom de niveau : le nom doit etre unique.)
 *
 * Niveaux solo : 14 colonnes.
 * Niveaux duel : briques verticales ; 9 lignes remplissent tout le terrain de haut en bas
 *   (la balle ne peut passer chez l'autre que si le niveau laisse volontairement des vides) ; jusqu'a 27 colonnes.
 *
 *  '.' vide
 *  '1' '2' '3' brique normale (nombre de coups)
 *  '#' acier indestructible          'X' explosive (detruit les 8 voisines)
 *  'G' or (si le niveau en contient : il se termine quand tout l'or est pris)
 *  'Z' foudre (detruit sa colonne)   'R' roche (detruit sa ligne)
 *  'B' bouton (fait basculer les briques 'T'/'t')
 *  'T' brique indestructible visible au depart, 't' cachee au depart (elles alternent a chaque bouton)
 *  'I' givre (les 8 voisines deviennent incassables 10 s)
 *  'H' temps (les briques detruites, sauf or et temps, reapparaissent)
 *  'N' nuit (tout devient noir 8 s)
 *  'P' teleportation (traversable, incassable ; la balle ressort d'une autre brique P)
 *  'M' brique mobile indestructible ; 'o' balise invisible : la brique mobile fait des allers-retours entre sa
 *      position de depart et les balises alignees avec elle (ligne ou colonne), de proche en proche
 *  Bonus directs au contact : 'w' large  'm' multi-balles  's' ralenti  'f' boule de feu  'c' collante
 *                             'u' mur de secours  '+' vie  'g' fantomes  'e' etoile filante
 *  Malus directs au contact : 'r' raquette reduite  'n' balle inoffensive
 */
final class Levels {
    private Levels() {
    }

    static final class Level {
        final String name;
        final boolean twoPlayer;
        final String[] rows;

        Level(String name, boolean twoPlayer, String[] rows) {
            this.name = name;
            this.twoPlayer = twoPlayer;
            this.rows = rows;
        }
    }

    private static final List<Level> ALL = new ArrayList<Level>();

    static Level get(int i) {
        return ALL.get(i);
    }

    static int count() {
        return ALL.size();
    }

    /** Premier niveau solo (debut de la campagne). */
    static int firstSolo() {
        return nextSolo(-1);
    }

    /** Niveau solo suivant (la campagne saute les duels), ou -1 s'il n'y en a plus. */
    static int nextSolo(int from) {
        for (int i = from + 1; i < ALL.size(); i++) {
            if (!ALL.get(i).twoPlayer) return i;
        }
        return -1;
    }

    private static void solo(String name, String... rows) {
        ALL.add(new Level(name, false, rows));
    }

    private static void duel(String name, String... rows) {
        ALL.add(new Level(name, true, rows));
    }

    static {
        solo("Premiers pas",
            "11111111111111",
            "11111111111111",
            "11111111111111",
            "..1111111111..");
        solo("R\u00e9sistantes",
            "22222222222222",
            "11111111111111",
            "11111111111111",
            "..2222222222..");
        solo("Pyramide",
            "......11......",
            ".....1221.....",
            "....123321....",
            "...12333321...",
            "..1233333321..",
            ".123333333321.");
        solo("Explosives",
            "1X1X1X1X1X1X1X",
            "11111111111111",
            "2222XX22XX2222",
            "11111111111111",
            "X1X1X1X1X1X1X1");
        solo("Forteresse",
            "##############",
            "#222222222222#",
            "#2XXXXXXXXXX2#",
            "#222222222222#");
        solo("Damier",
            "1.2.3.1.2.3.1.",
            ".3.1.2.3.1.2.3",
            "1.2.3.1.2.3.1.",
            ".3.1.2.3.1.2.3",
            "1.2.3.1.2.3.1.");
        solo("Colonnes",
            "11111111111111",
            "3#3#3#3#3#3#3#",
            "3#3#3#3#3#3#3#",
            "3X3X3X3X3X3X3X");
        solo("Le grand mur",
            "##############",
            "#222222222222#",
            "#3X3X3X3X3X33#",
            "#333333333333#",
            "22222222222222");
        solo("Foudre et roche",
            "11111111111111",
            "12222222222221",
            "1233Z33333R321",
            "12333333333321",
            "12222222222221",
            "11111111111111");
        solo("Chasse \u00e0 l'or",
            "G222222222222G",
            "#111111111111#",
            "2G2G2G2G2G2G2G",
            "11111111111111",
            "..3333GG3333..");
        solo("Interrupteurs",
            "B............B",
            "TTTT......TTTT",
            "22222222222222",
            "3333G33G333333",
            "tt.tt.tt.tt.tt");
        solo("Bonus garantis",
            "w.m.s.f.c.+...",
            "22222222222222",
            "3Z3333RR3333Z3",
            "G1X1G1X1G1X1G1",
            "11111111111111");
        solo("Pi\u00e8ges et mur",
            "1r1u1m1n1u1r1w",
            "22222222222222",
            "3333X33X333333",
            "22222222222222",
            "1G1G1G1G1G1G1G");
        solo("Givre",
            "11111111111111",
            "2I22222222I222",
            "33333333333333",
            "3I3333X33333I3",
            "22222222222222",
            "1G1G1G1G1G1G1G");
        solo("Nuit et horloge",
            "11111111111111",
            "11N11111H11N11",
            "22222222222222",
            "3333Z33333R333",
            "22222222222222",
            "11111111111111");
        solo("T\u00e9l\u00e9portation",
            "P............P",
            "11111111111111",
            "22222222222222",
            "3333P333P33333",
            "22222222222222",
            "G.G.G.G.G.G.G.");
        solo("Mur mobile",
            "11111111111111",
            "2M..........o2",
            "33333333333333",
            "3333X333X33333",
            "22222222222222");
        solo("Fant\u00f4mes et \u00e9toiles",
            "g.e.g.e.g.e.g.",
            "22222222222222",
            "33333333333333",
            "22222222222222",
            "11111111111111");

        duel("Premier duel",
            "12333333321",
            "12333333321",
            "12333333321",
            "12333333321",
            "12333333321",
            "12333333321",
            "12333333321",
            "12333333321",
            "12333333321");
        duel("Duel explosif",
            "1111X222X1111",
            "1222222222221",
            "12X3333333X21",
            "123333Z333321",
            "1233333333321",
            "123333Z333321",
            "12X3333333X21",
            "1222222222221",
            "1111X222X1111");
        duel("Duel dor\u00e9",
            "G1222222222221G",
            "113333333333311",
            "1B3333TTT3333B1",
            "1233333R3333321",
            "123333tttt33321",
            "1233333R3333321",
            "1B3333TTT3333B1",
            "113333333333311",
            "G1222222222221G");
        duel("Br\u00e8che",
            "22222222222222222",
            "1N11111I11111H111",
            "11111111111111111",
            "P...............P",
            ".................",
            "P...............P",
            "11111111111111111",
            "1111g111e1111g111",
            "22222222222222222");
        duel("Duel mobile",
            "333333333333333",
            "2M.........o222",
            "111111111111111",
            "222222222222222",
            "33333X3X3333333",
            "222222222222222",
            "111111111111111",
            "22o.........M22",
            "333333333333333");
    }
}
