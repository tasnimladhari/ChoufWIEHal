package com.choufwie.hal.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.choufwie.hal.R;
import com.choufwie.hal.util.Prefs;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.function.Consumer;

/** Small UI helpers shared by all screens. */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, int v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }

    public static int levelColor(int score) {
        return score < 35 ? 0xFF3FA66B : score < 65 ? 0xFFE8A317 : 0xFFD64545;
    }

    public static TextView line(Context c, String text, boolean bold) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(15);
        t.setPadding(0, dp(c, 3), 0, dp(c, 3));
        if (bold) t.setTypeface(null, Typeface.BOLD);
        return t;
    }

    public static View card(Context c, String title, String sub, View.OnClickListener click) {
        MaterialCardView cv = new MaterialCardView(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(c, 8);
        cv.setLayoutParams(lp);
        cv.setRadius(dp(c, 14));
        cv.setCardElevation(dp(c, 2));
        LinearLayout in = new LinearLayout(c);
        in.setOrientation(LinearLayout.VERTICAL);
        in.setPadding(dp(c, 14), dp(c, 10), dp(c, 14), dp(c, 10));
        TextView s = line(c, sub, false);
        s.setTextSize(13);
        in.addView(line(c, title, true));
        in.addView(s);
        cv.addView(in);
        if (click != null) cv.setOnClickListener(click);
        return cv;
    }

    /** Label + coloured progress bar (0..1). */
    public static void bar(LinearLayout parent, String label, float v) {
        Context c = parent.getContext();
        TextView t = line(c, label + " · " + Math.round(v * 100) + "%", false);
        t.setTextSize(13);
        ProgressBar pb = new ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(Math.round(v * 100));
        pb.setProgressTintList(ColorStateList.valueOf(levelColor(Math.round(v * 100))));
        parent.addView(t);
        parent.addView(pb, new LinearLayout.LayoutParams(-1, dp(c, 10)));
    }

    public static void message(Context c, String title, String msg) {
        new MaterialAlertDialogBuilder(c).setTitle(title).setMessage(msg).setPositiveButton("OK", null).show();
    }

    public static void input(Context c, String title, String hint, int inputType, Consumer<String> ok) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setInputType(inputType);
        if ((inputType & InputType.TYPE_TEXT_FLAG_MULTI_LINE) != 0) { e.setMinLines(4); e.setGravity(Gravity.TOP); }
        FrameLayout box = new FrameLayout(c);
        box.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), 0);
        box.addView(e);
        new MaterialAlertDialogBuilder(c).setTitle(title).setView(box)
                .setPositiveButton("OK", (d, w) -> ok.accept(e.getText().toString().trim()))
                .setNegativeButton("Cancel", null).show();
    }

    /** Safety dialog: supportive, non-diagnostic, points to humans. */
    public static void crisis(Context c) {
        new MaterialAlertDialogBuilder(c).setTitle("You are not alone 💜")
                .setMessage(R.string.crisis_text)
                .setPositiveButton("I will reach out", null)
                .setNeutralButton("Message my trusted person", (d, w) -> sms(c, "Hey, I'm having a really hard moment. Can we talk?"))
                .show();
    }

    public static void sms(Context c, String body) {
        String phone = Prefs.str(c, "contactPhone", "");
        if (phone.isEmpty()) { Toast.makeText(c, "Save a trusted contact first (Toolkit → Support circle).", Toast.LENGTH_LONG).show(); return; }
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone));
        i.putExtra("sms_body", body);
        try { c.startActivity(i); } catch (ActivityNotFoundException e) { Toast.makeText(c, "No messaging app found.", Toast.LENGTH_SHORT).show(); }
    }
}
