package cassebrique;

/**
 * Point d'entree pour le programme distribue (jpackage / jar) : avec Java 11+ une classe qui herite de
 * Application ne peut pas toujours etre lancee directement hors d'un module ; ce petit lanceur contourne le probleme.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
