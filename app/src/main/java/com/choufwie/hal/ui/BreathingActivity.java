package com.choufwie.hal.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;

import com.choufwie.hal.R;
import com.choufwie.hal.databinding.ActivityBreathingBinding;

import java.util.Locale;

/** SOS: a 2-minute guided breathing (inhale 4 - hold 4 - exhale 6) + the 5-4-3-2-1 grounding exercise. */
public class BreathingActivity extends AppCompatActivity {
    private ActivityBreathingBinding b;
    private final Handler h = new Handler(Looper.getMainLooper());
    private long end;
    private int step = 0;
    private boolean running = true;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityBreathingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        end = SystemClock.elapsedRealtime() + 120_000;
        b.btnClose.setOnClickListener(v -> finish());
        b.btnGround.setOnClickListener(v -> Ui.message(this, "5-4-3-2-1 grounding", getString(R.string.grounding)));
        tick();
    }

    private void tick() {
        if (!running) return;
        long left = end - SystemClock.elapsedRealtime();
        if (left <= 0) {
            b.phase.setText("Well done 💜");
            b.timeLeft.setText("0:00");
            return;
        }
        b.timeLeft.setText(String.format(Locale.ENGLISH, "%d:%02d", left / 60000, (left / 1000) % 60));
        String[] names = {"Inhale… 4", "Hold… 4", "Exhale… 6"};
        long[] dur = {4000, 4000, 6000};
        b.phase.setText(names[step]);
        if (step == 0) b.circle.animate().scaleX(1.5f).scaleY(1.5f).setDuration(4000).start();
        if (step == 2) b.circle.animate().scaleX(1f).scaleY(1f).setDuration(6000).start();
        long d = dur[step];
        step = (step + 1) % 3;
        h.postDelayed(this::tick, d);
    }

    @Override
    protected void onDestroy() {
        running = false;
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
