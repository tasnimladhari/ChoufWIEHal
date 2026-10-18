package com.choufwie.hal.util;

import android.content.Context;
import android.content.SharedPreferences;

/** Tiny SharedPreferences wrapper (session, LLM settings, trusted contact, login lockout). */
public final class Prefs {
    private Prefs() {}

    public static final String DEFAULT_HOST = "http://10.0.2.2:11434"; // emulator -> your PC. On a real phone use the PC's LAN IP.
    public static final String DEFAULT_MODEL = "llama3.2:3b";

    private static SharedPreferences sp(Context c) { return c.getApplicationContext().getSharedPreferences("hal_prefs", Context.MODE_PRIVATE); }

    public static long userId(Context c) { return sp(c).getLong("userId", -1); }
    public static void setUserId(Context c, long id) { sp(c).edit().putLong("userId", id).apply(); }

    public static String str(Context c, String k, String def) { return sp(c).getString(k, def); }
    public static void putStr(Context c, String k, String v) { sp(c).edit().putString(k, v).apply(); }

    public static long lng(Context c, String k, long def) { return sp(c).getLong(k, def); }
    public static void putLng(Context c, String k, long v) { sp(c).edit().putLong(k, v).apply(); }

    public static int integer(Context c, String k, int def) { return sp(c).getInt(k, def); }
    public static void putInt(Context c, String k, int v) { sp(c).edit().putInt(k, v).apply(); }

    public static String host(Context c) { return str(c, "llmHost", DEFAULT_HOST); }
    public static String model(Context c) { return str(c, "llmModel", DEFAULT_MODEL); }
    public static void setLlm(Context c, String host, String model) {
        sp(c).edit().putString("llmHost", host.trim()).putString("llmModel", model.trim()).apply();
    }
}
