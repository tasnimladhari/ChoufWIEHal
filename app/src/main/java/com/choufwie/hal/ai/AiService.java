package com.choufwie.hal.ai;

import android.content.Context;

import com.choufwie.hal.util.Prefs;
import com.choufwie.hal.util.Utils;

import org.json.JSONObject;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The 4-stage pipeline's language layer. Every method returns something usable even if the LLM is offline:
 * the app NEVER shows an error instead of an answer.
 *  analyze()   stage 1: free text -> structured signals (LLM JSON, keyword fallback)
 *  jannet()    stage 4: explains the computed numbers in Jannet's voice
 *  insight()   phrases weekly patterns computed in code
 *  brainDump() / reframe(): toolkit helpers
 */
public class AiService {
    private static final List<String> EMOTIONS = Arrays.asList("calm", "hopeful", "tired", "anxious", "sad", "overwhelmed", "angry");
    private static final List<String> STRESSORS = Arrays.asList("exams", "workload", "sleep", "family_expectations", "career",
            "uncertainty", "rejection", "comparison", "money", "relationships", "other", "none");
    private static final List<String> EVENTS = Arrays.asList("none", "rejection", "no_response", "interview_done", "offer", "exam_done");

    private static final String PERSONA =
            "You are Jannet, a warm Tunisian fortune-teller who reads DATA, not stars. You support students and young graduates "
          + "under stress. Rules: reply in the same language as the user's text (English, French, or Tunisian Arabic written in Latin letters); "
          + "be kind, concrete and short (max 90 words); use ONLY the numbers given in the facts, never invent any; "
          + "never diagnose, never give medical advice, never mention medication; give exactly ONE tiny action for today; "
          + "if the user seems in danger, encourage them to talk to a trusted person or emergency services. "
          + "A light touch of Tunisian warmth (habibti, ya3tik essa7a) is welcome. No lists, no markdown.";

    private final Context ctx;

    public AiService(Context c) { ctx = c.getApplicationContext(); }

    private String llm(String system, String user, boolean json, int timeoutMs) {
        try {
            String out = OllamaProvider.chat(Prefs.host(ctx), Prefs.model(ctx), system, user, json, timeoutMs);
            return out == null || out.isEmpty() ? null : out;
        } catch (Exception e) { return null; }
    }

    /** Loads the model into memory so the first real answer is fast. Fails silently. */
    public void warmUp() { llm("Reply with OK.", "OK", false, 20_000); }

    // ---------------- stage 1 ----------------
    public Analysis analyze(String mode, String text, double sleep, int energy) {
        String sys = "Extract signals from a student/graduate's diary entry. Answer ONLY with JSON: "
                + "{\"stress\":1-10,\"emotion\":one of " + EMOTIONS + ",\"stressor\":one of " + STRESSORS
                + ",\"event\":one of " + EVENTS + "}. The text may be English, French, Arabic or Tunisian arabizi. "
                + "stress = how stressed the writer sounds (1 calm, 10 extreme). event = something that HAPPENED (rejection, unanswered application, interview done...), else none.";
        String raw = llm(sys, "Mode: " + mode + "\nSleep: " + sleep + " h\nEnergy: " + energy + "/5\nEntry: " + text, true, 25_000);
        Analysis a = heuristic(text, sleep, energy);
        if (raw == null) return a;
        try {
            JSONObject j = new JSONObject(raw);
            a.stress = Math.max(1, Math.min(10, (int) Math.round(j.optDouble("stress", a.stress))));
            a.emotion = pick(j.optString("emotion"), EMOTIONS, a.emotion);
            a.stressor = pick(j.optString("stressor"), STRESSORS, a.stressor);
            a.event = pick(j.optString("event"), EVENTS, a.event);
        } catch (Exception ignored) { /* keep heuristic */ }
        return a;
    }

    private static String pick(String v, List<String> allowed, String def) {
        String s = v == null ? "" : v.trim().toLowerCase(Locale.ROOT);
        return allowed.contains(s) ? s : def;
    }

    /** Offline fallback: keywords (EN/FR/arabizi) + sleep + energy. Crude, but never fails. */
    static Analysis heuristic(String text, double sleep, int energy) {
        String t = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        Analysis a = new Analysis();
        int s = 3;
        if (sleep < 6) s++;
        if (sleep < 5) s++;
        if (energy <= 2) s++;
        if (has(t, "stress", "9ala9", "anxi", "peur", "panique", "panic", "worried", "afraid", "inquiet", "khayef", "5ayef")) { s += 2; a.emotion = "anxious"; }
        if (has(t, "epuise", "fatigue", "tired", "exhaust", "ta3ban", "ta3eb", "ma9drach")) { s += 1; a.emotion = "tired"; }
        if (has(t, "submerge", "overwhelm", "trop", "too much", "barcha 3al")) { s += 1; a.emotion = "overwhelmed"; }
        if (has(t, "triste", "sad", "pleur", "cry", "7zin", "hzin")) a.emotion = "sad";
        if (has(t, "calme", "calm", "bien", "good", "mli7", "mlih", "happy", "content")) { s -= 1; if (a.emotion.equals("calm")) a.emotion = "calm"; }

        if (has(t, "examen", "exam", "partiel", "controle", "revision", "revise")) a.stressor = "exams";
        else if (has(t, "refus", "rejected", "rejection", "rfedhou")) a.stressor = "rejection";
        else if (has(t, "linkedin", "compare", "comparer", "comparison")) a.stressor = "comparison";
        else if (has(t, "parents", "famille", "family", "attentes", "expectations")) a.stressor = "family_expectations";
        else if (has(t, "entretien", "interview", "candidature", "application", "cv", "emploi", "job", "stage")) a.stressor = "career";
        else if (has(t, "sommeil", "sleep", "nawm", "insomnie")) a.stressor = "sleep";
        else if (has(t, "argent", "money", "flous", "loyer")) a.stressor = "money";
        else if (has(t, "retard", "deadline", "projet", "project", "workload", "charge")) a.stressor = "workload";
        else if (has(t, "sais pas", "don't know", "uncertain", "incertain", "doute", "doubt")) a.stressor = "uncertainty";
        else if (sleep < 5.5) a.stressor = "sleep";

        if (has(t, "refus", "rejected", "rejection", "rfedhou", "pas retenu", "unfortunately")) a.event = "rejection";
        else if (has(t, "aucune reponse", "no response", "no answer", "pas de reponse", "ghost", "silence")) a.event = "no_response";
        else if (has(t, "entretien passe", "interview went", "had an interview", "passe un entretien", "premier entretien")) a.event = "interview_done";

        a.stress = Math.max(1, Math.min(10, s));
        return a;
    }

    private static boolean has(String t, String... ks) {
        for (String k : ks) if (t.contains(k)) return true;
        return false;
    }

    // ---------------- stage 4 + toolkit ----------------
    /** Jannet explains the computed numbers. facts = what the engine computed; fallback = deterministic message. */
    public String jannet(String facts, String userText, String fallback) {
        String user = "FACTS (computed by the app, do not change them):\n" + facts
                + (userText == null || userText.isEmpty() ? "" : "\n\nWhat the user just wrote:\n" + userText)
                + "\n\nAnswer as Jannet.";
        String r = llm(PERSONA, user, false, 45_000);
        return r != null ? r : fallback;
    }

    public String insight(String facts, String fallback) {
        String r = llm(PERSONA, "These weekly PATTERNS were computed from the user's check-ins:\n" + facts
                + "\n\nPresent them kindly in Jannet's voice, 2-3 sentences plus one small suggestion.", false, 45_000);
        return r != null ? r : fallback;
    }

    public String brainDump(String text) {
        String r = llm(PERSONA, "The user dumped their thoughts:\n" + text
                + "\n\nSort them into: Urgent today / Important this week / Can wait / Let go. "
                + "Then name the ONE first step (max 10 minutes). Plain sentences, no markdown.", false, 60_000);
        return r != null ? r : brainFallback(text);
    }

    static String brainFallback(String text) {
        String[] parts = text.split("[\\n.;!?]+");
        StringBuilder sb = new StringBuilder("Jannet is offline, so here is a simple sort. Your thoughts, one by one:\n\n");
        int n = 0;
        for (String p : parts) {
            String q = p.trim();
            if (q.length() < 3) continue;
            sb.append(++n).append(". ").append(q).append("\n");
            if (n == 8) break;
        }
        sb.append("\nNow pick the ONE that would feel lightest to finish, and give it 10 minutes. The rest can wait. Ya3tik essa7a 💜");
        return sb.toString();
    }

    public String reframe(String text) {
        String r = llm(PERSONA, "The user says:\n" + text
                + "\n\nOffer a gentler, realistic angle (not toxic positivity): validate the feeling, "
                + "separate the event from their worth, and suggest one small next step.", false, 60_000);
        return r != null ? r : "That sounds heavy, and it makes sense that it hurts. One hard moment is data about a situation, not a verdict on you. "
                + "Write down one thing you can learn from it, then do one small kind thing for yourself today. Ya3tik essa7a 💜";
    }
}
