package com.choufwie.hal.engine;

import android.content.Context;

import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.CheckIn;
import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.util.Security;
import com.choufwie.hal.util.Utils;

/** SIMULATED data for the live demo (say so in the pitch!): a week of check-ins + commitments. Call off the UI thread. */
public final class DemoSeeder {
    private DemoSeeder() {}

    public static void seed(Context c, User u) {
        AppDatabase db = AppDatabase.get(c);
        db.commitments().clear(u.id);
        db.checkIns().clear(u.id);
        long now = System.currentTimeMillis();
        boolean student = Utils.STUDENT.equals(u.mode);

        double[] sleep = {7.5, 7, 6.5, 6, 5.5, 5, 4.5};
        int[] energy = {4, 4, 3, 3, 2, 2, 2};
        int[] stress;
        String[] events, stressors, emotions, texts;

        if (student) {
            add(db, u, "Big Data", "EXAM", 3, 4, 3, -1);
            add(db, u, "Cryptography", "EXAM", 2, 3, 4, -1);
            add(db, u, "Networks", "EXAM", 3, 3, 5, -1);
            add(db, u, "Web Dev", "EXAM", 2, 2, 9, 17);
            add(db, u, "English", "EXAM", 1, 1, 8, 16);
            stress = new int[]{3, 3, 4, 5, 6, 7, 8};
            events = new String[]{"none", "none", "none", "none", "none", "none", "none"};
            stressors = new String[]{"none", "workload", "exams", "sleep", "exams", "exams", "exams"};
            emotions = new String[]{"calm", "calm", "anxious", "tired", "anxious", "overwhelmed", "overwhelmed"};
            texts = new String[]{"Journée calme, j'ai bien avancé en cours.", "Ça va, un peu de retard sur les révisions.",
                    "Examen de Big Data dans quelques jours, je commence à stresser.", "Ma nrakezch barcha, mochkla fi nawm.",
                    "3andi 3 examens fi 4 jours, 9ala9.", "Ma9drach nrakez, mazelt ma bdit chay.", "Je suis épuisée et j'ai peur de tout rater."};
        } else {
            u.jobSearchStart = now - 70 * Utils.DAY;
            db.users().update(u);
            add(db, u, "Interview · Company A", "INTERVIEW", 5, 4, 2, -1);
            add(db, u, "Tech test · Company B", "TECH_TEST", 4, 4, 4, -1);
            add(db, u, "Application · Company C", "APPLICATION", 3, 2, 1, -1);
            add(db, u, "Follow-up · Company D", "FOLLOW_UP", 2, 1, 3, -1);
            stress = new int[]{4, 4, 6, 6, 5, 7, 8};
            events = new String[]{"none", "interview_done", "rejection", "no_response", "none", "rejection", "none"};
            stressors = new String[]{"career", "uncertainty", "rejection", "uncertainty", "comparison", "rejection", "career"};
            emotions = new String[]{"hopeful", "anxious", "sad", "tired", "tired", "sad", "anxious"};
            texts = new String[]{"Candidature envoyée, je garde espoir.", "Premier entretien passé, je ne sais pas encore.",
                    "Encore un refus aujourd'hui.", "Aucune réponse depuis deux semaines.",
                    "Je me compare aux autres sur LinkedIn, ça fatigue.", "Un autre refus. Je commence à douter.",
                    "Ma9drach nkamel, 3andi entretien jeudi w 9ala9."};
        }

        for (int i = 0; i < 7; i++) {
            CheckIn ci = new CheckIn();
            ci.userId = u.id;
            ci.timestamp = now - (6 - i) * Utils.DAY - (i == 6 ? 3_600_000L : 0);
            ci.textEnc = Security.encrypt(texts[i]);
            ci.sleepHours = sleep[i];
            ci.energy = energy[i];
            ci.stress = stress[i];
            ci.emotion = emotions[i];
            ci.stressor = stressors[i];
            ci.event = events[i];
            ci.movement = i % 3 == 0;
            ci.water = i % 2 == 0;
            db.checkIns().insert(ci);
        }
    }

    private static void add(AppDatabase db, User u, String title, String type, int w, int d, int inDays, double grade) {
        Commitment c = new Commitment();
        c.userId = u.id;
        c.title = title;
        c.type = type;
        c.weight = w;
        c.difficulty = d;
        c.dueDate = Utils.toMillis(java.time.LocalDate.now().plusDays(inDays));
        c.grade = grade;
        db.commitments().insert(c);
    }
}
