package com.choufwie.hal.util;

import android.os.Handler;
import android.os.Looper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Threading + date helpers shared by every layer. */
public final class Utils {
    private Utils() {}

    public static final String STUDENT = "STUDENT";
    public static final String GRADUATE = "GRADUATE";
    public static final long DAY = 86_400_000L;

    private static final ExecutorService IO = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    /** Run off the UI thread (database, network, LLM). */
    public static void bg(Runnable r) { IO.execute(r); }

    /** Run on the UI thread. */
    public static void ui(Runnable r) { MAIN.post(r); }

    public static float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    public static long toMillis(LocalDate d) { return d.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(); }

    public static LocalDate toDate(long ms) { return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate(); }

    public static int daysUntil(long ms) { return (int) ChronoUnit.DAYS.between(LocalDate.now(), toDate(ms)); }

    public static String fmt(long ms, String pattern) {
        return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH));
    }

    /** 0 -> "Today", 1 -> "Tomorrow", else the weekday name. */
    public static String dayLabel(int offset) {
        if (offset == 0) return "Today";
        if (offset == 1) return "Tomorrow";
        return LocalDate.now().plusDays(offset).getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    /** Short weekday ("Mon") for chart labels. */
    public static String dow(int offset) {
        return LocalDate.now().plusDays(offset).getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }
}
