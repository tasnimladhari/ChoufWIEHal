package com.choufwie.hal.engine;

import com.choufwie.hal.data.Entities.CheckIn;
import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 2: transparent burnout-risk score (a heuristic, NOT a clinical tool).
 *
 *  score = 100 * ( 0.35*stress + 0.25*sleepDebt + 0.30*pressure + 0.10*trend )
 *
 *  stress    : average AI stress of the last 7 days / 10
 *  sleepDebt : (7.5h - average sleep) / 3.5, clamped 0..1
 *  pressure  : student  = sum( coef/5 * difficulty/5 / (1 + daysLeft) )   -> exam load
 *              graduate = 0.6*jobSearchStrain + 0.4*interview/deadline load
 *  trend     : stress of last 3 check-ins vs the ones before
 *
 * The forecast re-runs the same formula for the next 7 days (deadlines get closer).
 */
public final class RiskEngine {
    private RiskEngine() {}

    public static class Result {
        public int score;
        public String level;
        public float stressAvg, sleepDebt, pressure, trend, strain;
        public double avgSleep;
        public int[] forecast = new int[7];
        public int peakDay;
        public String peakReason;
        public boolean hasData;
    }

    public static String levelOf(int s) { return s < 35 ? "Low" : s < 65 ? "Moderate" : "High"; }

    public static Result compute(String mode, List<Commitment> cs, List<CheckIn> checkIns, long jobSearchStart) {
        Result r = new Result();
        long now = System.currentTimeMillis();
        boolean grad = Utils.GRADUATE.equals(mode);

        List<CheckIn> w = new ArrayList<>();
        for (CheckIn c : checkIns) if (c.timestamp >= now - 7 * Utils.DAY) w.add(c);
        r.hasData = !w.isEmpty();

        float stressSum = 0, sleepSum = 0;
        for (CheckIn c : w) { stressSum += c.stress; sleepSum += c.sleepHours; }
        r.stressAvg = w.isEmpty() ? 0.3f : stressSum / w.size() / 10f;
        r.avgSleep = w.isEmpty() ? 7.5 : sleepSum / w.size();
        r.sleepDebt = Utils.clamp((float) ((7.5 - r.avgSleep) / 3.5));

        int n = w.size();
        r.trend = 0;
        if (n >= 4) {
            float recent = avgStress(w.subList(n - 3, n)), before = avgStress(w.subList(0, n - 3));
            r.trend = Utils.clamp((recent - before) / 5f);
        }
        r.strain = grad ? strain(checkIns, jobSearchStart, now) : 0;

        int peak = 0, max = -1;
        for (int d = 0; d < 7; d++) {
            float p = pressure(cs, d);
            float third = grad ? 0.6f * r.strain + 0.4f * p : p;
            int s = Math.min(100, Math.round(100 * (0.35f * r.stressAvg + 0.25f * r.sleepDebt + 0.30f * third + 0.10f * r.trend)));
            r.forecast[d] = s;
            if (d == 0) r.pressure = p;
            if (s > max) { max = s; peak = d; }
        }
        r.score = r.forecast[0];
        r.level = levelOf(r.score);
        r.peakDay = peak;

        int count = 0;
        for (Commitment c : cs) {
            int left = Utils.daysUntil(c.dueDate);
            if (c.grade < 0 && left >= peak && left <= peak + 2) count++;
        }
        r.peakReason = count > 0
                ? count + " deadline" + (count > 1 ? "s" : "") + " around " + Utils.dayLabel(peak).toLowerCase()
                : "stress and sleep pressure building up";
        return r;
    }

    private static float avgStress(List<CheckIn> l) {
        float s = 0;
        for (CheckIn c : l) s += c.stress;
        return l.isEmpty() ? 0 : s / l.size();
    }

    private static float pressure(List<Commitment> cs, int offset) {
        float sum = 0;
        for (Commitment c : cs) {
            if (c.grade >= 0) continue; // already done
            int left = Utils.daysUntil(c.dueDate) - offset;
            if (left < 0 || left > 14) continue;
            sum += (c.weight / 5f) * (c.difficulty / 5f) / (1f + left);
        }
        return Utils.clamp(sum / 1.2f);
    }

    /** Graduate mode: rejections / silence in the last 14 days + how long the search has lasted. */
    private static float strain(List<CheckIn> list, long start, long now) {
        int rej = 0;
        for (CheckIn c : list)
            if (now - c.timestamp <= 14 * Utils.DAY && ("rejection".equals(c.event) || "no_response".equals(c.event))) rej++;
        float weeks = start > 0 ? (now - start) / (7f * Utils.DAY) : 0;
        return Utils.clamp(0.6f * Math.min(1f, rej / 8f) + 0.4f * Math.min(1f, weeks / 12f));
    }
}
