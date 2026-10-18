package com.choufwie.hal.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.choufwie.hal.ai.AiService;
import com.choufwie.hal.data.Entities.CheckIn;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.FragmentInsightsBinding;
import com.choufwie.hal.engine.Insights;
import com.choufwie.hal.engine.Snapshot;
import com.choufwie.hal.util.Security;
import com.choufwie.hal.util.Utils;

import java.util.List;
import java.util.Locale;

/** Risk explanation, 7-day forecast, stress trend and weekly insights. Jannet greets once per session. */
public class InsightsFragment extends Fragment {
    private static boolean introShown = false;
    private FragmentInsightsBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentInsightsBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        b.btnWeekly.setOnClickListener(x -> weekly());
    }

    @Override
    public void onResume() {
        super.onResume();
        Context app = requireContext().getApplicationContext();
        User u = MainActivity.user;
        Utils.bg(() -> {
            Snapshot s = Snapshot.load(app, u);
            String last = "";
            if (!s.checkIns.isEmpty()) last = Security.decrypt(s.checkIns.get(s.checkIns.size() - 1).textEnc);
            String lastText = last;
            Utils.ui(() -> { if (b != null) render(s, lastText); });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void render(Snapshot s, String lastEntry) {
        Context ctx = requireContext();
        boolean grad = Utils.GRADUATE.equals(s.mode);
        b.gauge.setValue(s.risk.score);
        b.levelText.setText(s.risk.level + " risk · peak " + Utils.dayLabel(s.risk.peakDay).toLowerCase() + ": " + s.risk.peakReason);
        b.components.removeAllViews();
        Ui.bar(b.components, "Stress (last 7 days)", s.risk.stressAvg);
        Ui.bar(b.components, String.format(Locale.ENGLISH, "Sleep debt (avg %.1f h)", s.risk.avgSleep), s.risk.sleepDebt);
        Ui.bar(b.components, grad ? "Job-search strain" : "Deadline pressure", grad ? s.risk.strain : s.risk.pressure);
        Ui.bar(b.components, "Stress trend", s.risk.trend);

        float[] f = new float[7];
        String[] fl = new String[7];
        for (int i = 0; i < 7; i++) { f[i] = s.risk.forecast[i]; fl[i] = Utils.dow(i); }
        b.forecastChart.setData(f, fl);

        List<CheckIn> list = s.checkIns;
        int n = Math.min(7, list.size());
        float[] t = new float[n];
        String[] tl = new String[n];
        for (int i = 0; i < n; i++) {
            CheckIn c = list.get(list.size() - n + i);
            t[i] = c.stress * 10f;
            tl[i] = Utils.fmt(c.timestamp, "d/M");
        }
        b.trendChart.setData(t, tl);
        b.lastEntry.setText(lastEntry.isEmpty() ? "" : "🔒 Last entry (stored encrypted with AES-256-GCM, decrypted only here):\n“" + lastEntry + "”");

        if (!introShown) { // instant, deterministic Jannet greeting (no LLM wait)
            introShown = true;
            new JannetDialog(ctx, "Jannet's forecast 🔮", s.fallback(null));
        }
    }

    private void weekly() {
        Context ctx = requireContext();
        User u = MainActivity.user;
        JannetDialog d = new JannetDialog(ctx, "Your week, according to Jannet 🔮", "🔮 Jannet is reading the cards…");
        Utils.bg(() -> {
            Context app = ctx.getApplicationContext();
            Snapshot s = Snapshot.load(app, u);
            Insights.Result r = Insights.build(s.checkIns);
            String say = new AiService(app).insight(r.facts, r.fallback);
            Utils.ui(() -> d.say(say));
        });
    }
}
