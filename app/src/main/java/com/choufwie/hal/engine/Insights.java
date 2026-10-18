package com.choufwie.hal.engine;

import com.choufwie.hal.data.Entities.CheckIn;
import com.choufwie.hal.util.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Weekly insights: the PATTERNS are computed in code (reliable); the LLM only phrases them. */
public final class Insights {
    private Insights() {}

    public static class Result { public String facts, fallback; }

    public static Result build(List<CheckIn> list) {
        Result r = new Result();
        int n = list.size();
        if (n < 3) {
            r.facts = "Not enough check-ins yet (" + n + ").";
            r.fallback = "I need at least 3 check-ins to spot your patterns. Come back in a few days, or load the demo data in Settings.";
            return r;
        }
        long now = System.currentTimeMillis();
        float recent = 0, before = 0, low = 0, high = 0;
        int nr = 0, nb = 0, nl = 0, nh = 0, moves = 0;
        Map<String, Integer> stressors = new HashMap<>();
        for (CheckIn c : list) {
            if (now - c.timestamp <= 7 * Utils.DAY) { recent += c.stress; nr++; if (c.movement) moves++; }
            else { before += c.stress; nb++; }
            if (c.sleepHours < 6) { low += c.stress; nl++; } else { high += c.stress; nh++; }
            if (c.stressor != null && !"none".equals(c.stressor)) stressors.merge(c.stressor, 1, Integer::sum);
        }
        StringBuilder f = new StringBuilder(), fb = new StringBuilder();
        if (nr > 0) {
            f.append(String.format(Locale.ENGLISH, "Average stress over the last 7 days: %.1f/10", recent / nr));
            fb.append(String.format(Locale.ENGLISH, "• Your average stress this week is %.1f/10", recent / nr));
            if (nb > 0) {
                f.append(String.format(Locale.ENGLISH, " (previous week: %.1f/10)", before / nb));
                fb.append(recent / nr > before / nb + 0.5 ? ", higher than last week." : recent / nr < before / nb - 0.5 ? ", lower than last week. 👏" : ", similar to last week.");
            } else fb.append(".");
            f.append(". ");
            fb.append("\n");
        }
        if (nl > 0 && nh > 0) {
            f.append(String.format(Locale.ENGLISH, "On days with under 6 h of sleep stress averaged %.1f/10 versus %.1f/10 otherwise. ", low / nl, high / nh));
            fb.append(String.format(Locale.ENGLISH, "• On short-sleep days your stress is %.1f/10 versus %.1f/10 on other days.\n", low / nl, high / nh));
        }
        String top = null; int best = 0;
        for (Map.Entry<String, Integer> e : stressors.entrySet()) if (e.getValue() > best) { best = e.getValue(); top = e.getKey(); }
        if (top != null) {
            f.append("Most frequent stressor: ").append(top.replace('_', ' ')).append(". ");
            fb.append("• Your most frequent stressor: ").append(top.replace('_', ' ')).append(".\n");
        }
        f.append("Movement logged on ").append(moves).append(" of the last ").append(nr).append(" check-ins.");
        fb.append("• You moved your body on ").append(moves).append(" of your last ").append(nr).append(" check-ins.\nYa3tik essa7a for tracking: awareness is the first step.");
        r.facts = f.toString();
        r.fallback = fb.toString();
        return r;
    }
}
