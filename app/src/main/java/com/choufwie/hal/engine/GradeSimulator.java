package com.choufwie.hal.engine;

import com.choufwie.hal.data.Entities.Commitment;

import java.util.List;
import java.util.Locale;

/** Student toolkit: what average do I need in the remaining subjects to reach my target? Pure math on coefficients. */
public final class GradeSimulator {
    private GradeSimulator() {}

    public static String simulate(List<Commitment> cs, double target) {
        double knownSum = 0, knownCoef = 0, remCoef = 0;
        for (Commitment c : cs) {
            if (c.grade >= 0) { knownSum += c.grade * c.weight; knownCoef += c.weight; }
            else remCoef += c.weight;
        }
        if (remCoef == 0)
            return "Every subject already has a grade (or none is saved). Add the subjects still to come in the Plan tab, without a grade.";
        double current = knownCoef > 0 ? knownSum / knownCoef : 0;
        double needed = (target * (knownCoef + remCoef) - knownSum) / remCoef;
        StringBuilder sb = new StringBuilder();
        if (knownCoef > 0) sb.append(String.format(Locale.ENGLISH, "Your current weighted average: %.2f/20.\n", current));
        if (needed <= 0) {
            sb.append("You have already secured your target of ").append(target).append("! Keep your rhythm and protect your sleep.");
        } else if (needed > 20) {
            sb.append(String.format(Locale.ENGLISH, "Reaching %.2f would need %.1f/20 in the remaining subjects: not reachable. Try a slightly lower target. ", target, needed));
            sb.append("A target is a compass, not a verdict.");
        } else {
            sb.append(String.format(Locale.ENGLISH, "To reach %.2f/20 you need about %.2f/20 on average in the remaining subjects (total coef %.0f). ", target, needed, remCoef));
            sb.append(needed <= 12 ? "Very realistic."
                    : needed <= 15 ? "Realistic with steady work."
                    : needed <= 17 ? "Ambitious: plan your effort and protect your sleep."
                    : "Very ambitious: aim high, but have a plan B that doesn't cost your health.");
        }
        return sb.toString();
    }
}
