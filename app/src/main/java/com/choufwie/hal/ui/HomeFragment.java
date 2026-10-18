package com.choufwie.hal.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.choufwie.hal.R;
import com.choufwie.hal.ai.AiService;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.FragmentHomeBinding;
import com.choufwie.hal.engine.DemoSeeder;
import com.choufwie.hal.engine.Planner;
import com.choufwie.hal.engine.Snapshot;
import com.choufwie.hal.util.Utils;

import java.time.LocalDate;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private FragmentHomeBinding b;
    private static boolean greeted = false; // Jannet says hello once per app launch
    private int memeIdx = 0;
    private static class Task { String t; boolean d; Task(String t, boolean d) { this.t = t; this.d = d; } }
    private final List<Task> tasks = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentHomeBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        Context ctx = requireContext();

        android.webkit.WebView gif = v.findViewById(R.id.tenorGif);

        gif.getSettings().setJavaScriptEnabled(true);
        gif.setWebViewClient(new android.webkit.WebViewClient());

        gif.loadUrl("file:///android_asset/choufli.html");

        String[] quotes = getResources().getStringArray(R.array.quotes);
        b.quote.setText("“" + quotes[LocalDate.now().getDayOfYear() % quotes.length] + "”");
        showMeme();
        b.btnMeme.setOnClickListener(x -> { memeIdx++; showMeme(); });
        b.btnSettings.setOnClickListener(x -> startActivity(new Intent(ctx, SettingsActivity.class)));
        b.btnForecast.setOnClickListener(x -> forecast());
        b.btnDemo.setOnClickListener(x -> Utils.bg(() -> {
            DemoSeeder.seed(ctx.getApplicationContext(), MainActivity.user);
            Utils.ui(this::load);

        }));
        b.btnAddTask.setOnClickListener(x -> addTask());
        b.etTask.setOnEditorActionListener((tv, action, e) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { addTask(); return true; }
            return false;
        });
        b.btnClearDone.setOnClickListener(x -> {
            tasks.removeIf(t -> t.d);
            saveTasks();
            renderTasks();
        });


    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }


    private void showMeme() {
        android.webkit.WebView gif = b.memeImg;

        gif.getSettings().setJavaScriptEnabled(true);
        gif.setWebViewClient(new android.webkit.WebViewClient());

        gif.loadDataWithBaseURL(
                "https://tenor.com/",
                "<html><body style='margin:0;padding:0;'>"
                        + "<div class='tenor-gif-embed' "
                        + "data-postid='23053224' "
                        + "data-share-method='host' "
                        + "data-aspect-ratio='1.76596' "
                        + "data-width='100%'></div>"
                        + "<script async src='https://tenor.com/embed.js'></script>"
                        + "</body></html>",
                "text/html",
                "UTF-8",
                null
        );

        b.memeCaption.setText("Me at 4AM promising Jannet i will start studying at 7am");
    }


    private void load() {
        Context ctx = requireContext().getApplicationContext();
        User u = MainActivity.user;
        Utils.bg(() -> {
            Snapshot s = Snapshot.load(ctx, u);
            Utils.ui(() -> { if (b != null) render(s, u); });
        });
    }

    private void render(Snapshot s, User u) {
        Context ctx = requireContext();
        b.hello.setText("Salut " + u.name + " 👋");
        b.modeChip.setText(Utils.STUDENT.equals(u.mode) ? "🎓 Student mode" : "💼 Graduate mode");
        b.gauge.setValue(s.risk.score);
        b.levelText.setText(s.risk.level + " risk · peak " + Utils.dayLabel(s.risk.peakDay).toLowerCase()
                + " (" + s.risk.forecast[s.risk.peakDay] + "/100)");
        b.demoCard.setVisibility(s.checkIns.isEmpty() && s.commitments.isEmpty() ? View.VISIBLE : View.GONE);
        loadTasks(s);
        renderTasks();
        if (!greeted) {
            greeted = true;
            String[] g = getResources().getStringArray(R.array.jannet_greetings);
            new JannetDialog(ctx, "Jannet vous salue 🔮", g[LocalDate.now().getDayOfYear() % g.length]);
        }
    }

    /** Jannet reads the forecast aloud (LLM phrasing, deterministic fallback). */
    private void forecast() {
        Context ctx = requireContext();
        User u = MainActivity.user;
        JannetDialog d = new JannetDialog(ctx, "Jannet's forecast 🔮", "🔮 Jannet is reading the cards…");
        Utils.bg(() -> {
            Snapshot s = Snapshot.load(ctx.getApplicationContext(), u);
            String say = new AiService(ctx).jannet(s.facts(null), "", s.fallback(null));
            Utils.ui(() -> d.say(say));
        });
    }
    // ---------- To-do list ----------
    private SharedPreferences prefs() {
        return requireContext().getSharedPreferences("todo", Context.MODE_PRIVATE);
    }

    private String todayKey() { return "plan_" + LocalDate.now(); }

    private void loadTasks(Snapshot s) {
        SharedPreferences p = prefs();
        String key = todayKey();

        // drop lists from previous days
        SharedPreferences.Editor ed = p.edit();
        for (String k : new ArrayList<>(p.getAll().keySet()))
            if (k.startsWith("plan_") && !k.equals(key)) ed.remove(k);
        ed.apply();

        tasks.clear();
        String raw = p.getString(key, null);
        if (raw == null) {
            // first open today: seed from Jannet's plan
            for (Planner.Block bl : s.plan.get(0).blocks)
                tasks.add(new Task(bl.title + (bl.minutes > 0 ? " — " + bl.minutes + " min" : ""), false));
            saveTasks();
        } else {
            try {
                JSONArray arr = new JSONArray(raw);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    tasks.add(new Task(o.getString("t"), o.getBoolean("d")));
                }
            } catch (Exception ignored) { }
        }
    }

    private void saveTasks() {
        try {
            JSONArray arr = new JSONArray();
            for (Task t : tasks) arr.put(new JSONObject().put("t", t.t).put("d", t.d));
            prefs().edit().putString(todayKey(), arr.toString()).apply();
        } catch (Exception ignored) { }
    }

    private void addTask() {
        if (b == null) return;
        String text = b.etTask.getText().toString().trim();
        if (text.isEmpty()) return;
        tasks.add(new Task(text, false));
        b.etTask.setText("");
        saveTasks();
        renderTasks();
    }

    private void renderTasks() {
        if (b == null) return;
        Context ctx = requireContext();
        b.planToday.removeAllViews();
        for (Task t : tasks) {
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            CheckBox cb = new CheckBox(ctx);
            cb.setText(t.t);
            cb.setChecked(t.d);
            applyStrike(cb, t.d);
            cb.setOnCheckedChangeListener((btn, checked) -> {
                t.d = checked;
                applyStrike(cb, checked);
                saveTasks();
            });
            row.addView(cb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            ImageButton del = new ImageButton(ctx);
            del.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            del.setBackground(null);
            del.setContentDescription("Delete task");
            del.setOnClickListener(x -> {
                tasks.remove(t);
                saveTasks();
                renderTasks();
            });
            row.addView(del);

            b.planToday.addView(row);
        }
    }

    private void applyStrike(CheckBox cb, boolean done) {
        if (done) cb.setPaintFlags(cb.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else cb.setPaintFlags(cb.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        cb.setAlpha(done ? 0.5f : 1f);
    }
}
