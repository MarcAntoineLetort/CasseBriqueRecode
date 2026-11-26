package cassebrique;

import java.net.URL;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import javafx.scene.media.AudioClip;

/**
 * Lecture des effets sonores. Tolerant : fichier ou module media absent = pas de son, pas d'erreur.
 * Le nombre de sons simultanes est limite par le moteur audio : quand il est atteint, il coupe des sons en cours.
 * On lui donne donc des priorites (les sons importants ne sont jamais sacrifies) et on evite de saturer
 * avec les bruits de briques les plus anodins.
 */
final class Sounds {
    private final Map<Sfx, AudioClip> clips = new EnumMap<Sfx, AudioClip>(Sfx.class);
    private final long[] last = new long[Sfx.values().length];
    private final long[] recentLow = new long[3]; // dates des derniers sons de priorite 0
    private int lowIdx;
    private final Random rnd = new Random();

    Sounds() {
        for (Sfx s : Sfx.values()) {
            try {
                URL url = Sounds.class.getResource("sounds/" + s.fileName());
                if (url != null) clips.put(s, new AudioClip(url.toExternalForm()));
            } catch (Throwable t) {
                // son indisponible : on continue sans
            }
        }
    }

    void play(Sfx s) {
        AudioClip c = clips.get(s);
        if (c == null) return;
        long now = System.nanoTime();
        if (now - last[s.ordinal()] < 35000000L) return; // anti-saturation (reactions en chaine)
        if (s.priority == 0) { // au plus 3 petits bruits de brique par tranche de 70 ms
            if (now - recentLow[lowIdx] < 70000000L) return;
            recentLow[lowIdx] = now;
            lowIdx = (lowIdx + 1) % recentLow.length;
        }
        last[s.ordinal()] = now;
        double rate = 1.0;
        if (s == Sfx.BRICK_HIT || s == Sfx.BRICK_BREAK || s == Sfx.PADDLE || s == Sfx.WALL) {
            rate = 0.94 + rnd.nextDouble() * 0.12; // legere variation, evite la lassitude
        } else if (s == Sfx.GHOST) {
            rate = 0.8 + rnd.nextDouble() * 0.5;   // les fantomes "chantent" un peu differemment a chaque fois
        }
        try {
            c.play(s.volume, 0, rate, 0, s.priority);
        } catch (Throwable t) {
            // ignore
        }
    }
}
