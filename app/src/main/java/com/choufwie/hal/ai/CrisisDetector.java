package com.choufwie.hal.ai;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Safety net that NEVER depends on the LLM: plain keyword matching (English, French, Arabic script, Tunisian "arabizi").
 * Deliberately sensitive: a false alarm only shows a kind message; a miss could cost much more.
 */
public final class CrisisDetector {
    private CrisisDetector() {}

    private static final String[] KEYS = {
            // English
            "kill myself", "end my life", "want to die", "wanna die", "suicide", "suicidal", "hurt myself", "harm myself",
            "no reason to live", "better off without me", "don't want to be here anymore",
            // French
            "me tuer", "en finir avec la vie", "envie de mourir", "veux mourir", "veux plus vivre", "suicid", "me faire du mal",
            "plus envie de vivre", "disparaitre pour toujours",
            // Arabic script
            "انتحار", "نقتل روحي", "نموت", "نحب نموت", "نقتل نفسي", "اقتل نفسي", "أريد أن أموت", "اريد ان اموت",
            // Tunisian arabizi
            "nheb nmout", "nheb nmout", "n7eb nmout", "nokTol rou7i", "no9tol rou7i", "no9tel rou7i", "nekhtel rou7i",
            "ma3adch nheb n3ich", "ma3adch n7eb n3ich", "n7eb nmout"
    };

    public static boolean isCrisis(String text) {
        if (text == null) return false;
        String t = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").replace('’', '\'');
        for (String k : KEYS) {
            String kk = Normalizer.normalize(k.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            if (t.contains(kk)) return true;
        }
        return false;
    }
}
