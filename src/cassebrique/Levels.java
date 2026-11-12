package cassebrique;

/**
 * Les niveaux : 14 colonnes max.
 *  '.' vide
 *  '1' '2' '3' brique normale (nombre de coups)
 *  '#' acier indestructible          'X' explosive (detruit les 8 voisines)
 *  'G' or (si le niveau en contient : il se termine quand tout l'or est pris)
 *  'Z' foudre (detruit sa colonne)   'R' roche (detruit sa ligne)
 *  'B' bouton (fait basculer les briques 'T'/'t')
 *  'T' brique indestructible visible au depart, 't' cachee au depart (elles alternent a chaque bouton)
 *  'w' large  'm' multi-balles  's' ralenti  'f' boule de feu  'c' collante  '+' vie : bonus direct au contact
 * Pour ajouter un niveau : ajouter un nom et une grille ici, rien d'autre.
 */
final class Levels {
    private Levels() {
    }

    static final String[] NAMES = {
        "Premiers pas",
        "R\u00e9sistantes",
        "Pyramide",
        "Explosives",
        "Forteresse",
        "Damier",
        "Colonnes",
        "Le grand mur",
        "Foudre et roche",
        "Chasse \u00e0 l'or",
        "Interrupteurs",
        "Bonus garantis"
    };

    static final String[][] DATA = {
        { // 1
            "11111111111111",
            "11111111111111",
            "11111111111111",
            "..1111111111.." },
        { // 2
            "22222222222222",
            "11111111111111",
            "11111111111111",
            "..2222222222.." },
        { // 3
            "......11......",
            ".....1221.....",
            "....123321....",
            "...12333321...",
            "..1233333321..",
            ".123333333321." },
        { // 4
            "1X1X1X1X1X1X1X",
            "11111111111111",
            "2222XX22XX2222",
            "11111111111111",
            "X1X1X1X1X1X1X1" },
        { // 5
            "##############",
            "#222222222222#",
            "#2XXXXXXXXXX2#",
            "#222222222222#" },
        { // 6
            "1.2.3.1.2.3.1.",
            ".3.1.2.3.1.2.3",
            "1.2.3.1.2.3.1.",
            ".3.1.2.3.1.2.3",
            "1.2.3.1.2.3.1." },
        { // 7
            "11111111111111",
            "3#3#3#3#3#3#3#",
            "3#3#3#3#3#3#3#",
            "3X3X3X3X3X3X3X" },
        { // 8
            "##############",
            "#222222222222#",
            "#3X3X3X3X3X33#",
            "#333333333333#",
            "22222222222222" },
        { // 9 : foudre colonne 4, roche ligne 2
            "11111111111111",
            "12222222222221",
            "1233Z33333R321",
            "12333333333321",
            "12222222222221",
            "11111111111111" },
        { // 10 : l'or termine le niveau
            "G222222222222G",
            "#111111111111#",
            "2G2G2G2G2G2G2G",
            "11111111111111",
            "..3333GG3333.." },
        { // 11 : boutons et briques interrupteurs
            "B............B",
            "TTTT......TTTT",
            "22222222222222",
            "3333G33G333333",
            "tt.tt.tt.tt.tt" },
        { // 12 : tout ensemble
            "w.m.s.ff.c.+.w",
            "22222222222222",
            "3Z3333RR3333Z3",
            "G1X1G1X1G1X1G1",
            "11111111111111" }
    };
}
