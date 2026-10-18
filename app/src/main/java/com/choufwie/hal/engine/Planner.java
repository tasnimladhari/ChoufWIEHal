package com.choufwie.hal.engine;

import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.util.Utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 3: adaptive planner (greedy priority allocation).
 *  Student : priority = coef * difficulty / (1 + daysLeft); daily study budget is cut when risk is high.
 *  Graduate: daily job-search routine with a CAP on applications, interview prep scheduled backwards, rest day.
 *  Both    : protected movement, recovery and sleep blocks.
 */
public final class Planner {
    private Planner() {}

    public static class Block {
        public final String title;
        public final int minutes;
        public final String kind;
        Block(String t, int m, String k) { title = t; minutes = m; kind = k; }
    }

    public static class DayPlan {
        public String label;
        public final List<Block> blocks = new ArrayList<>();
    }

    public static List<DayPlan> week(String mode, List<Commitment> cs, int risk) {
        List<DayPlan> out = new ArrayList<>();
        for (int d = 0; d < 7; d++) {
            DayPlan dp = new DayPlan();
            dp.label = Utils.dayLabel(d);
            if (Utils.STUDENT.equals(mode)) student(dp, cs, risk, d); else graduate(dp, cs, risk, d);
            dp.blocks.add(new Block("Move your body (walk, stretch, sport)", 20, "HABIT"));
            if (risk >= 65) dp.blocks.add(new Block("Recovery: breathing + a short walk", 20, "REST"));
            dp.blocks.add(new Block("Wind-down: screens off, lights out before 23:30", 0, "REST"));
            out.add(dp);
        }
        return out;
    }

    private static void student(DayPlan dp, List<Commitment> cs, int risk, int d) {
        float factor = risk >= 65 ? 0.65f : risk >= 50 ? 0.85f : 1f;
        int budget = Math.round(240 * factor);
        List<Commitment> open = new ArrayList<>();
        for (Commitment c : cs) {
            int left = Utils.daysUntil(c.dueDate) - d;
            if (c.grade < 0 && left >= 0 && left <= 21) open.add(c);
        }
        final int dd = d;
        Collections.sort(open, (a, b) -> Float.compare(priority(b, dd), priority(a, dd)));
        List<Commitment> top = open.subList(0, Math.min(3, open.size()));

        float total = 0;
        for (Commitment c : top) total += priority(c, d);
        if (d == 0 && factor < 1f)
            dp.blocks.add(new Block("Study load lightened by " + Math.round((1 - factor) * 100) + "% to protect you from burnout", 0, "REST"));
        for (Commitment c : top) {
            int left = Utils.daysUntil(c.dueDate) - d;
            if (left == 0) { dp.blocks.add(new Block("Exam day · " + c.title + ": light review only", 30, "STUDY")); continue; }
            int minutes = Math.round(budget * priority(c, d) / total / 25f) * 25; // Pomodoro-sized chunks
            if (minutes > 0) dp.blocks.add(new Block("Study · " + c.title, minutes, "STUDY"));
        }
        if (top.isEmpty()) dp.blocks.add(new Block("No deadline in sight: free revision or rest", 0, "REST"));
    }

    private static float priority(Commitment c, int d) {
        return (c.weight * c.difficulty) / (1f + Utils.daysUntil(c.dueDate) - d);
    }

    private static void graduate(DayPlan dp, List<Commitment> cs, int risk, int d) {
        if (LocalDate.now().plusDays(d).getDayOfWeek() == DayOfWeek.SUNDAY) {
            dp.blocks.add(new Block("Job search is OFF today. Rest counts as progress.", 0, "REST"));
            return;
        }
        int apps = risk >= 65 ? 1 : risk >= 50 ? 2 : 3; // daily cap: no doom-applying
        dp.blocks.add(new Block("Send " + apps + " tailored application" + (apps > 1 ? "s" : ""), apps * 40, "JOB"));
        dp.blocks.add(new Block("Skill building / portfolio", 45, "JOB"));
        dp.blocks.add(new Block("Networking: 1 message to someone in your field", 15, "JOB"));
        for (Commitment c : cs) {
            int left = Utils.daysUntil(c.dueDate) - d;
            if ("INTERVIEW".equals(c.type)) {
                if (left == 0) dp.blocks.add(new Block("Interview today · " + c.title + ": warm-up + 2-min breathing", 20, "JOB"));
                else if (left >= 1 && left <= 3) dp.blocks.add(new Block("Prepare · " + c.title + " (D-" + left + ")", 60, "JOB"));
            } else if ("TECH_TEST".equals(c.type) && left >= 1 && left <= 3) {
                dp.blocks.add(new Block("Practice · " + c.title + " (D-" + left + ")", 60, "JOB"));
            } else if ("APPLICATION".equals(c.type) && left >= 0 && left <= 1) {
                dp.blocks.add(new Block("Deadline · " + c.title, 30, "JOB"));
            } else if ("FOLLOW_UP".equals(c.type) && left == 0) {
                dp.blocks.add(new Block("Follow up · " + c.title, 15, "JOB"));
            }
        }
        dp.blocks.add(new Block("Job search stops at 18:00", 0, "REST"));
    }
}
