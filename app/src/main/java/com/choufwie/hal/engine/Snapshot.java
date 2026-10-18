package com.choufwie.hal.engine;

import android.content.Context;

import com.choufwie.hal.ai.Analysis;
import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.CheckIn;
import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.util.Security;
import com.choufwie.hal.util.Utils;

import java.util.List;
import java.util.Locale;

/** One consistent picture of the user's situation: data + risk + forecast + plan. Load it OFF the UI thread. */
public class Snapshot {
    public String mode;
    public List<Commitment> commitments;
    public List<CheckIn> checkIns;
    public RiskEngine.Result risk;
    public List<Planner.DayPlan> plan;

    public static Snapshot load(Context c, User u) {
        AppDatabase db = AppDatabase.get(c);
        Snapshot s = new Snapshot();
        s.mode = u.mode;
        s.commitments = db.commitments().all(u.id);
        s.checkIns = db.checkIns().since(u.id, System.currentTimeMillis() - 14 * Utils.DAY);
        s.risk = RiskEngine.compute(u.mode, s.commitments, s.checkIns, u.jobSearchStart);
        s.plan = Planner.week(u.mode, s.commitments, s.risk.score);
        return s;
    }

    public static void saveCheckIn(Context c, User u, String text, double sleep, int energy, boolean move, boolean water, Analysis a) {
        CheckIn ci = new CheckIn();
        ci.userId = u.id;
        ci.timestamp = System.currentTimeMillis();
        ci.textEnc = Security.encrypt(text);   // AES-256-GCM, key in Android Keystore
        ci.sleepHours = sleep;
        ci.energy = energy;
        ci.movement = move;
        ci.water = water;
        ci.stress = a.stress;
        ci.emotion = a.emotion;
        ci.stressor = a.stressor;
        ci.event = a.event;
        AppDatabase.get(c).checkIns().insert(ci);
    }

    /** Facts handed to the LLM so it explains real numbers instead of inventing. */
    public String facts(Analysis a) {
        boolean grad = Utils.GRADUATE.equals(mode);
        StringBuilder f = new StringBuilder();
        f.append("User is a ").append(grad ? "recent graduate looking for a job" : "university student").append(". ");
        f.append("Burnout risk ").append(risk.score).append("/100 (").append(risk.level).append("). ");
        f.append(String.format(Locale.ENGLISH, "Components: stress %.0f%%, sleep debt %.0f%%, %s %.0f%%, trend %.0f%%. ",
                risk.stressAvg * 100, risk.sleepDebt * 100, grad ? "job-search strain" : "deadline pressure",
                (grad ? risk.strain : risk.pressure) * 100, risk.trend * 100));
        f.append("Forecast peak: ").append(Utils.dayLabel(risk.peakDay)).append(" (").append(risk.forecast[risk.peakDay])
                .append("/100) because of ").append(risk.peakReason).append(". ");
        if (a != null)
            f.append("Latest check-in: stress ").append(a.stress).append("/10, emotion ").append(a.emotion)
                    .append(", stressor ").append(a.stressor).append(", event ").append(a.event).append(". ");
        f.append(risk.score >= 65 ? "Plan adjusted: workload cut by 35% and a recovery block added."
                : risk.score >= 50 ? "Plan adjusted: workload cut by 15%."
                : "Plan unchanged: she is in a safe zone.");
        return f.toString();
    }

    /** Deterministic message used instantly (no LLM) or when the LLM is offline. */
    public String fallback(Analysis a) {
        String action;
        String st = a == null ? "none" : a.stressor, ev = a == null ? "none" : a.event;
        if ("rejection".equals(ev)) action = "Write down one thing you learned from this, then stop applying for today.";
        else if ("no_response".equals(ev)) action = "Pick a date to follow up, then let it go until then.";
        else if ("exams".equals(st)) action = "Do a 2-minute breathing reset, then choose ONE subject for the next 25 minutes.";
        else if ("sleep".equals(st) || risk.sleepDebt > 0.4f) action = "Put your phone away 30 minutes earlier tonight.";
        else if ("family_expectations".equals(st)) action = "Tell one trusted person how much pressure you feel. You don't carry it alone.";
        else action = "Drink some water and stretch for 2 minutes.";
        return "Jannet reads your data, not the stars: your burnout risk is " + risk.level.toLowerCase() + " (" + risk.score + "/100). "
                + "Your forecast peaks " + Utils.dayLabel(risk.peakDay).toLowerCase() + " (" + risk.forecast[risk.peakDay] + "/100): " + risk.peakReason + ".\n\n"
                + "Tiny action: " + action + " Ya3tik essa7a! 💜";
    }
}
