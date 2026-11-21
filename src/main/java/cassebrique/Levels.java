package cassebrique;

import java.util.ArrayList;
import java.util.List;

/**
 * Tous les niveaux. Pour en ajouter : une ligne solo(...) ou duel(...) a la fin de la liste,
 * le menu cree les pages tout seul. (Les records sont memorises par nom de niveau : le nom doit etre unique.)
 *
 * Niveaux solo : solo(...) = 14 colonnes (15 au maximum), avec une marge au-dessus et sur les cotes.
 *   soloFull(...) = les briques occupent tout l'espace : collees au plafond et aux deux murs, quel que soit le
 *   nombre de colonnes (la largeur des briques s'adapte). Pour laisser des espaces, mettre des '.'.
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
 *  'y' malus jaune a tete de mort : une fois cassee, applique un malus au joueur ADVERSAIRE (en solo : a soi)
 *  'a' arc-en-ciel (comme la teleportation, mais les deux briques utilisees disparaissent ; il en faut 2 ou plus)
 *  'x' echange (duel) : au contact, les balles noires et blanches changent de proprietaire
 *  'M' brique mobile indestructible ; 'o' balise invisible : la brique mobile fait des allers-retours entre sa
 *      position de depart et les balises alignees avec elle (ligne ou colonne), de proche en proche
 *  Bonus directs au contact : 'w' large  'm' multi-balles  's' ralenti  'f' boule de feu  'c' collante
 *                             'u' mur de secours  '+' vie  'g' fantomes  'e' etoile filante  'l' balle de lave
 *  Malus directs au contact : 'r' raquette reduite  'n' balle inoffensive  'v' ivresse
 */
final class Levels {
    private Levels() {
    }

    static final class Level {
        final String name;
        final boolean twoPlayer;
        final boolean full;   // solo : grille collee au plafond et aux murs
        final String[] rows;

        Level(String name, boolean twoPlayer, boolean full, String[] rows) {
            this.name = name;
            this.twoPlayer = twoPlayer;
            this.full = full;
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

    /** Numero du niveau dans sa categorie (1 joueur ou 2 joueurs), a partir de 1. */
    static int number(int i) {
        int n = 0;
        for (int k = 0; k <= i; k++) {
            if (ALL.get(k).twoPlayer == ALL.get(i).twoPlayer) n++;
        }
        return n;
    }

    /** Indices des niveaux d'une categorie, dans l'ordre. */
    static int[] indices(boolean twoPlayer) {
        int n = 0;
        for (Level l : ALL) {
            if (l.twoPlayer == twoPlayer) n++;
        }
        int[] r = new int[n];
        int j = 0;
        for (int k = 0; k < ALL.size(); k++) {
            if (ALL.get(k).twoPlayer == twoPlayer) r[j++] = k;
        }
        return r;
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
        ALL.add(new Level(name, false, false, rows));
    }

    private static void soloFull(String name, String... rows) {
        ALL.add(new Level(name, false, true, rows));
    }

    private static void duel(String name, String... rows) {
        ALL.add(new Level(name, true, false, rows));
    }

    static {
        solo("Premiers pas",
            "11111111111111",
            "1G1111111111G1",
            "11111111111111",
            "..1111111111..");
        solo("R\u00e9sistantes",
            "22222222222222",
            "1s111GGGG111+1",
            "111111+r111111",
            "..2222222222..");
        solo("Pyramide",
            "w.....11.....s",
            ".....1221.....",
            "....12GG21....",
            "...12333321...",
            "..1233GG3321..",
            ".123333333321.");
        solo("Explosives",
            "1X1X1X1X1X1X1X",
            "11G11111111G11",
            "2222XX22XX2222",
            "1111G1111G1111",
            "X1X1X1X1X1X1X1");
        solo("Forteresse",
            "##############",
            "#22G222222G22#",
            "#2XXXXXXXXXX2#",
            "#222222222222#");
        solo("Damier",
            "1.2.G.1.2.3.1.",
            ".3.1.2.3.G.2.3",
            "1.2.G.1.2.3.1.",
            ".3.1.2.3.G.2.3",
            "1.2.G.1.2.3.1.");
        solo("Colonnes",
            "1G1m11GG11c1G1",
			"3#3#3#33#3#3#3",
			"3#3#3#XX#3#3#3",
			"3X3X3X33X3X3X3");
        solo("Le grand mur",
            "##############",
            "#222222222222#",
            "#3X3X3X3X3X33#",
            "#333333333333#",
            "22222222222222");
        solo("Foudre et roche",
            "11111111111111",
			"1G22wG22G+22Z1",
			"123333333R3321",
			"1233R333333321",
			"1Z2222222222G1",
			"11111111111111");
        solo("Chasse \u00e0 l'or",
            "G222222222222G",
            "#111111111111#",
            "2G2G2G2G2G2G2G",
            "11111111111111",
            "..3333GG3333..");
        soloFull("Interrupteurs",
            "vG.l......e.Gc",
            "TTTT......TTTT",
            "G222X2222X222G",
            "X3R3G3333G3I3X",
            "Bt+tt.tt.tt+tB");
		soloFull("Enfer et paradis",
			"G1N11G#mG1c1eG",
			"31v1I1n#111l13",
			"Gr1r11#+w1111G",
			"I111111#1111w1",
			"11111r#f1+111g",
			"2n22221#2222u2");
		solo("L'envahisseur",
			"gn22w3XX3s22ng",
			".gu22g11g22ug.",
			"..gn22ew22ng..",
			"..gu2222ug...",
			"....gZ++Zg....",
			".....gggg.....");
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
        soloFull("T\u00e9l\u00e9portation",
        	"P.............P",
        	"G.G.G.G.G.G.G.G",
            "1+11m1II1u11a1",
            "22g2r2sw2m2v22",
            "###############",
            "n333P3333X333l",
            "2a2w22+s22u2+2"
            );
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

        soloFull("Fortifié",
            "222222222222222",
            "1G11111111111G1",
            "3.3.3.3.3.3.3.3",
            "l1111111111111v",
            "222X2222222X222",
            "1#+#11e#r11#+#1");
        soloFull("Arc-en-ciel",
        	"G.G.G.G.G.G.G.",
            "a............a",
            "11111111111111",
            "22222222222222",
            "33+3a3XX3a3+33",
            "22w22222222w22",
            "a............a");
        soloFull("Lave et ivresse",
            "22222222222222",
            "3G3333333333G3",
            "2R2222222222R2",
            "+11+11ww11+11+",
            "l.v.l.v.l.v.l.");

        duel("Premier duel",
            "1233G+G3321",
			"1+33333332w",
			"12333333321",
			"12333333u21",
			"12333G33321",
			"12333G33321",
			"12u33333321",
			"12333333321",
			"wG333333G+1",
			"1233G+G3321");
        duel("Duel explosif",
            "G111XG2GX111w",
			"1222222222221",
			"12Xu3G3G3+X21",
			"123333Z333321",
			"e2+333G333+2e",
			"123333Z333321",
			"12X+3G3G3uX21",
			"1222222222221",
			"w111XG2GX111G");
        duel("Duel dor\u00e9",
            "G1222222222221G",
            "w1333333333331w",
            "1B3333TTT3333B1",
            "1233333R3333321",
            "+23333tttt3332+",
            "1233333R3333321",
            "1B3333TTT3333B1",
            "113333333333311",
            "G1222222222221G");
        duel("Br\u00e8che",
            "3G22H223G322H22G3",
			"111111+333+111111",
			"11I1s11III11s1I11",
			"P...............P",
			".................",
			"P...............P",
			"11I1r11III11r1I11",
			"+111111222111111+",
			"3N223G22G22G322N3");
        duel("Duel mobile",
            "33G333333333g33",
			"22M.........o22",
			"1111r11111w1111",
			"22X223333322+22",
			"33333GGGGG33333",
			"22+223333322X22",
			"1111w11m11r1111",
			"22o.........M22",
			"33g333333333G33");
        duel("Sabotage glac\u00e9",
            "1I22222GIG22222Ic",
			"RI2r11222221112Z1",
			"1I2G+11111111G2Iu",
			"1I21113Gn+r1112I1",
			"1I2X1333H3331X2I1",
			"1I2111r+1G31112I1",
			"uI2G11111111+G2I1",
			"1Z21111333111r2IR",
			"cI22G223f322G22I1");
		duel("Chacun son tour",
			"tG2222Z3GIG3Z22222T",
			"t1111G2111112G1G11T",
			"Bn1111211G11211e11T",
			"t1+1112111112111g1T",
			"t11111211111211111T",
			"t1g1112GrfrG2111+1T",
			"t11e1123333321111nB",
			"t11G1123XXX3211111T",
			"t11111233G3321111GT");
		duel("Les envahisseurs",
			"....g2Re2g....",
			"...gu2332wg...",
			"..gs123321+g..",
			".g+1#w33n#1ng.",
			"gc11g2XX2g11cg",
			".gn1#n33w#1+g.",
			"..g+123321sg..",
			"...gw2332ug...",
			"....g2eR2g....");
		duel("Surprise",
			"+112GP1111111N111333G1w111X",
			"33####1111111G11c3X311rZ11X",
			"1111111#R#G#11111333111g11X",
			"I+1111G#33m1P3#+11111111112",
			"2m11111#GH33n11#####e1111s2",
			"2s1111e#####11n33HG#11111m2",
			"21111111111+#3P1m33#G1111+I",
			"X11g11133311111#G#R#1111111",
			"X11Zr113X3c11G1111111####33",
			"X111w1G333111I1111111PG211+");
		duel("Alternance",
			"3G11111T111G1f1G111T11111G3",
			"3311yX2T11111R11111T2Xy1133",
			"u111112T11G11B11G11T211111u",
			".I11G12ttttttttttttt21G11I.",
			"..I11111111rGIGr11111111I..",
			".I1111ctttttttttttttc1111I.",
			"wX111Z1T11m11X11m11T1Z111Xw",
			"11+B111T11G11111G11T111B+11",
			"g11+11eT11G11G11G11Te11+11g");
        duel("Malus jaunes",
            "1y22G3G22y1",
            "12333333321",
            "123y333y321",
            "123333l3321",
            "12G33y33G21",
            "123333v3321",
            "123y333y321",
            "12333333321",
            "1y22G3G22y1");
        duel("\u00c9change",
            "1222222222221",
            "123333x333321",
            "12G3333333G21",
            "1233x33x33321",
            "e2+3333333+2e",
            "1233x33x33321",
            "12G3333333G21",
            "123333x333321",
            "1222222222221");
        duel("Arc-en-ciel duel",
            "G122222a22221G",
            "13333333333331",
            "1y3I3333333I31",
            "133333x3333331",
            "a2+3333333+2.a",
            "133333x3333331",
            "1y3I3333333I31",
            "13333333333331",
            "G122222a22221G");
    }
}
