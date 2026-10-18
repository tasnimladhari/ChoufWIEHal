package com.choufwie.hal.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.choufwie.hal.R;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
/**
 * Jannet the fortune-teller pops up (with a bounce) every time the app predicts or "reads" something.
 * Usage: JannetDialog d = new JannetDialog(ctx, "Title", "loading text"); ... d.say("final text");
 */
public class JannetDialog {
    private final AlertDialog dlg;
    private final TextView msg;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Runnable typer;

    public JannetDialog(Context c, String title, String firstText) {
        View v = LayoutInflater.from(c).inflate(R.layout.dialog_jannet, null);

        WebView img = v.findViewById(R.id.jannet_img);

        img.getSettings().setJavaScriptEnabled(true);
        img.setWebViewClient(new WebViewClient());


        String html =
                "<html><body style='margin:0;padding:0;background:transparent;'>"
                        + "<div class='tenor-gif-embed' data-postid='25347941' "
                        + "data-share-method='host' data-width='100%'></div>"
                        + "<script async src='https://tenor.com/embed.js'></script>"
                        + "</body></html>";


        img.loadDataWithBaseURL(
                "https://tenor.com/",
                html,
                "text/html",
                "UTF-8",
                null
        );

        msg = v.findViewById(R.id.jannet_msg);
        msg.setText(firstText);
        dlg = new MaterialAlertDialogBuilder(c).setView(v).setPositiveButton("Merci Jannet 💜", null).create();
        dlg.setOnDismissListener(x -> { if (typer != null) h.removeCallbacks(typer); });
        dlg.show();
        img.setScaleX(0f);
        img.setScaleY(0f);
        img.animate().scaleX(1f).scaleY(1f).setDuration(500).setInterpolator(new OvershootInterpolator()).start();
    }

    /** Typewriter effect: Jannet "speaks". */
    public void say(String text) {
        if (!dlg.isShowing()) return;
        if (typer != null) h.removeCallbacks(typer);
        msg.setText("");
        final int[] i = {0};
        typer = new Runnable() {
            @Override public void run() {
                if (!dlg.isShowing()) return;
                i[0] = Math.min(text.length(), i[0] + 2);
                msg.setText(text.substring(0, i[0]));
                if (i[0] < text.length()) h.postDelayed(this, 18);
            }
        };
        h.post(typer);
    }
}
