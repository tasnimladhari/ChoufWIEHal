package com.choufwie.hal.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.choufwie.hal.R;
import com.choufwie.hal.ai.AiService;
import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.data.Entities.Win;
import com.choufwie.hal.databinding.FragmentToolkitBinding;
import com.choufwie.hal.engine.GradeSimulator;
import com.choufwie.hal.util.Prefs;
import com.choufwie.hal.util.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Stress management + resilience tools. Everything AI-related is announced by Jannet. */
public class ToolkitFragment extends Fragment {
    private FragmentToolkitBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentToolkitBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        Context ctx = requireContext();
        b.btnSos.setOnClickListener(x -> startActivity(new android.content.Intent(ctx, BreathingActivity.class)));
        b.btnFocus.setOnClickListener(x -> focus());
        b.btnBrain.setOnClickListener(x -> brainDump());
        b.btnReframe.setOnClickListener(x -> reframe());
        b.btnWins.setOnClickListener(x -> wins());
        b.btnWhatIf.setOnClickListener(x -> whatIf());
        b.btnLearn.setOnClickListener(x -> learn());
        b.btnCircle.setOnClickListener(x -> circle());
    }

    @Override
    public void onResume() {
        super.onResume();
        b.btnWhatIf.setVisibility(Utils.STUDENT.equals(MainActivity.user.mode) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    // ---------- AI tools (Jannet pops up) ----------
    private void brainDump() {
        Context ctx = requireContext();
        Ui.input(ctx, "Brain dump: write everything on your mind", "Exams, worries, to-dos… any language",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, text -> {
                    if (text.isEmpty()) return;
                    JannetDialog d = new JannetDialog(ctx, "Let's sort this out, habibti 🔮", "🔮 Jannet is sorting your thoughts…");
                    Utils.bg(() -> {
                        String r = new AiService(ctx).brainDump(text);
                        Utils.ui(() -> d.say(r));
                    });
                });
    }

    private void reframe() {
        Context ctx = requireContext();
        Ui.input(ctx, "What's weighing on you?", "A bad grade, a rejection, a hard day…",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE, text -> {
                    if (text.isEmpty()) return;
                    JannetDialog d = new JannetDialog(ctx, "Jannet offers another angle 🔮", "🔮 Jannet is thinking…");
                    Utils.bg(() -> {
                        String r = new AiService(ctx).reframe(text);
                        Utils.ui(() -> d.say(r));
                    });
                });
    }

    private void whatIf() {
        Context ctx = requireContext();
        User u = MainActivity.user;
        Ui.input(ctx, "Target weighted average (/20)", "e.g. 16.5",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, s -> {
                    try {
                        double target = Double.parseDouble(s.replace(',', '.'));
                        Utils.bg(() -> {
                            List<Commitment> cs = AppDatabase.get(ctx).commitments().all(u.id);
                            String r = GradeSimulator.simulate(cs, target);
                            Utils.ui(() -> new JannetDialog(ctx, "Jannet does the math 🧮", r).say(r));
                        });
                    } catch (NumberFormatException e) {
                        Toast.makeText(ctx, "Enter a number like 16.5", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ---------- focus timer ----------
    private void focus() {
        Context ctx = requireContext();
        User u = MainActivity.user;
        Utils.bg(() -> {
            List<Commitment> cs = AppDatabase.get(ctx).commitments().all(u.id);
            Utils.ui(() -> { if (isAdded()) showFocus(cs); });
        });
    }

    private void showFocus(List<Commitment> cs) {
        Context ctx = requireContext();
        View v = getLayoutInflater().inflate(R.layout.dialog_pomodoro, null);
        Spinner sp = v.findViewById(R.id.spSubject);
        TextView t = v.findViewById(R.id.timerText);
        Button start = v.findViewById(R.id.btnStart);
        List<String> names = new ArrayList<>();
        names.add("General focus");
        for (Commitment c : cs) names.add(c.title);
        sp.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, names));
        CountDownTimer[] timer = new CountDownTimer[1];
        AlertDialog d = new MaterialAlertDialogBuilder(ctx).setTitle("Focus timer · 25 min").setView(v)
                .setNegativeButton("Close", null).create();
        d.setOnDismissListener(x -> { if (timer[0] != null) timer[0].cancel(); });
        start.setOnClickListener(x -> {
            start.setEnabled(false);
            int pos = sp.getSelectedItemPosition();
            timer[0] = new CountDownTimer(25 * 60_000L, 1000) {
                @Override public void onTick(long ms) { t.setText(String.format(Locale.ENGLISH, "%02d:%02d", ms / 60000, (ms / 1000) % 60)); }
                @Override public void onFinish() {
                    t.setText("Done! 🎉");
                    if (pos > 0) { long id = cs.get(pos - 1).id; Utils.bg(() -> AppDatabase.get(ctx).commitments().addFocus(id, 25)); }
                    Toast.makeText(ctx, "Session done. Take 5 minutes!", Toast.LENGTH_LONG).show();
                }
            }.start();
        });
        d.show();
    }

    // ---------- small wins ----------
    private void wins() {
        Context ctx = requireContext();
        User u = MainActivity.user;
        Utils.bg(() -> {
            List<Win> ws = AppDatabase.get(ctx).wins().all(u.id);
            Set<String> days = new HashSet<>();
            for (Win w : ws) days.add(w.day);
            LocalDate d = LocalDate.now();
            if (!days.contains(d.toString())) d = d.minusDays(1);
            int streak = 0;
            while (days.contains(d.toString())) { streak++; d = d.minusDays(1); }
            StringBuilder sb = new StringBuilder();
            sb.append(streak > 0 ? "🔥 Streak: " + streak + " day" + (streak > 1 ? "s" : "") : "No streak yet. One small win starts it. No pressure.").append("\n\n");
            for (int i = 0; i < Math.min(5, ws.size()); i++) sb.append("• ").append(ws.get(i).text).append("\n");
            Utils.ui(() -> {
                if (!isAdded()) return;
                new MaterialAlertDialogBuilder(ctx).setTitle("3 good things / small wins ✨").setMessage(sb.toString())
                        .setPositiveButton("Add a win", (dlg, w) -> Ui.input(ctx, "Today I'm proud of…", "Even something tiny counts",
                                InputType.TYPE_CLASS_TEXT, text -> {
                                    if (text.isEmpty()) return;
                                    Utils.bg(() -> {
                                        Win win = new Win();
                                        win.userId = u.id;
                                        win.day = LocalDate.now().toString();
                                        win.text = text;
                                        AppDatabase.get(ctx).wins().insert(win);
                                        Utils.ui(() -> { if (isAdded()) wins(); });
                                    });
                                }))
                        .setNegativeButton("Close", null).show();
            });
        });
    }

    // ---------- awareness + support ----------
    private void learn() {
        Context ctx = requireContext();
        String[] titles = getResources().getStringArray(R.array.learn_titles);
        String[] texts = getResources().getStringArray(R.array.learn_texts);
        new MaterialAlertDialogBuilder(ctx).setTitle("Learn cards 📚")
                .setItems(titles, (d, i) -> Ui.message(ctx, titles[i], texts[i])).show();
    }

    private void circle() {
        Context ctx = requireContext();
        View v = getLayoutInflater().inflate(R.layout.dialog_contact, null);
        EditText n = v.findViewById(R.id.etName), p = v.findViewById(R.id.etPhone);
        n.setText(Prefs.str(ctx, "contactName", ""));
        p.setText(Prefs.str(ctx, "contactPhone", ""));
        Runnable save = () -> {
            Prefs.putStr(ctx, "contactName", n.getText().toString().trim());
            Prefs.putStr(ctx, "contactPhone", p.getText().toString().trim());
        };
        new MaterialAlertDialogBuilder(ctx).setTitle("Support circle 🤝").setView(v)
                .setPositiveButton("Save", (d, w) -> save.run())
                .setNeutralButton("Message them", (d, w) -> { save.run(); Ui.sms(ctx, "Hey, I'm having a rough moment. Can we talk?"); })
                .setNegativeButton("Cancel", null).show();
    }
}
