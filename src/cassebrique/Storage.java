package cassebrique;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Sauvegarde (records par niveau, son coupe) dans un simple fichier : ~/.cassebrique/save.properties.
 * Remplace java.util.prefs qui ecrit dans le registre Windows (d'ou l'avertissement "erreur 5").
 */
final class Storage {
    private static final File FILE = new File(System.getProperty("user.home"), ".cassebrique/save.properties");

    private Storage() {
    }

    static void load(Game g) {
        InputStream in = null;
        try {
            in = new FileInputStream(FILE);
            Properties p = new Properties();
            p.load(in);
            g.highScore = parse(p.getProperty("highScore"));
            for (int i = 0; i < g.levelBest.length; i++) g.levelBest[i] = parse(p.getProperty("best." + Levels.get(i).name));
            g.muted = "true".equals(p.getProperty("muted"));
        } catch (IOException e) {
            // pas encore de sauvegarde : valeurs par defaut
        } finally {
            close(in);
        }
    }

    static void save(Game g) {
        OutputStream out = null;
        try {
            File dir = FILE.getParentFile();
            if (!dir.exists()) dir.mkdirs();
            Properties p = new Properties();
            p.setProperty("highScore", String.valueOf(g.highScore));
            for (int i = 0; i < g.levelBest.length; i++) p.setProperty("best." + Levels.get(i).name, String.valueOf(g.levelBest[i]));
            p.setProperty("muted", String.valueOf(g.muted));
            out = new FileOutputStream(FILE);
            p.store(out, "Casse-Brique");
        } catch (IOException e) {
            // sauvegarde impossible : tant pis, le jeu continue
        } finally {
            close(out);
        }
    }

    private static int parse(String s) {
        try {
            return s == null ? 0 : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void close(java.io.Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
