package com.choufwie.hal.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
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
import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.Commitment;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.FragmentPlanBinding;
import com.choufwie.hal.engine.Planner;
import com.choufwie.hal.engine.Snapshot;
import com.choufwie.hal.util.Utils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/** Student: subjects + coefficients. Graduate: interviews + deadlines. Below: the adaptive 7-day plan. */
public class PlanFragment extends Fragment {
    private FragmentPlanBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentPlanBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private boolean student() { return Utils.STUDENT.equals(MainActivity.user.mode); }

    private void refresh() {
        Context app = requireContext().getApplicationContext();
        User u = MainActivity.user;
        b.header.setText(student() ? "My subjects & exams" : "My job-hunt plan");
        b.btnAdd.setText(student() ? "+ Add subject / exam" : "+ Add interview / deadline");
        b.btnAdd.setOnClickListener(x -> showForm(null));
        Utils.bg(() -> {
            Snapshot s = Snapshot.load(app, u);
            Utils.ui(() -> { if (b != null) render(s); });
        });
    }

    private void render(Snapshot s) {
        Context ctx = requireContext();
        boolean st = student();
        b.riskNote.setText("The plan adapts to your burnout risk (now " + s.risk.score + "/100 · " + s.risk.level + ").");
        b.list.removeAllViews();
        if (s.commitments.isEmpty()) b.list.addView(Ui.line(ctx, "Nothing yet. Add your first one, or load demo data from Settings.", false));
        for (Commitment c : s.commitments) {
            String sub = (st ? "Coef " : "Importance ") + c.weight + " · difficulty " + c.difficulty
                    + " · due " + Utils.fmt(c.dueDate, "EEE d MMM")
                    + (c.grade >= 0 ? " · grade " + c.grade + "/20" : "")
                    + (c.focusMinutes > 0 ? " · focused " + c.focusMinutes + " min" : "");
            b.list.addView(Ui.card(ctx, c.title + "  (" + c.type.toLowerCase().replace('_', ' ') + ")", sub, v -> showForm(c)));
        }
        b.weekPlan.removeAllViews();
        for (Planner.DayPlan d : s.plan) {
            TextView h = Ui.line(ctx, d.label, true);
            h.setPadding(0, Ui.dp(ctx, 12), 0, 0);
            b.weekPlan.addView(h);
            for (Planner.Block bl : d.blocks)
                b.weekPlan.addView(Ui.line(ctx, "• " + bl.title + (bl.minutes > 0 ? " — " + bl.minutes + " min" : ""), false));
        }
    }

    /** Add (c == null) or edit/delete a commitment. */
    private void showForm(@Nullable Commitment c) {
        Context ctx = requireContext();
        boolean st = student();
        User u = MainActivity.user;
        View v = getLayoutInflater().inflate(R.layout.dialog_commitment, null);
        EditText etTitle = v.findViewById(R.id.etTitle);
        Spinner spType = v.findViewById(R.id.spType), spWeight = v.findViewById(R.id.spWeight), spDiff = v.findViewById(R.id.spDiff);
        Button btnDate = v.findViewById(R.id.btnDate);
        EditText etGrade = v.findViewById(R.id.etGrade);
        ((TextView) v.findViewById(R.id.lblWeight)).setText(st ? "Coefficient (1–5)" : "Importance to you (1–5)");

        List<String> types = st ? Arrays.asList("EXAM", "PROJECT", "THESIS") : Arrays.asList("INTERVIEW", "APPLICATION", "TECH_TEST", "FOLLOW_UP");
        List<String> nums = Arrays.asList("1", "2", "3", "4", "5");
        spType.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, types));
        spWeight.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, nums));
        spDiff.setAdapter(new ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, nums));
        etGrade.setVisibility(st ? View.VISIBLE : View.GONE);
        etGrade.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        long[] due = {c != null ? c.dueDate : Utils.toMillis(LocalDate.now().plusDays(3))};
        btnDate.setText("Due: " + Utils.fmt(due[0], "EEE d MMM yyyy"));
        btnDate.setOnClickListener(x -> {
            LocalDate d = Utils.toDate(due[0]);
            new DatePickerDialog(ctx, (dp, y, m, day) -> {
                due[0] = Utils.toMillis(LocalDate.of(y, m + 1, day));
                btnDate.setText("Due: " + Utils.fmt(due[0], "EEE d MMM yyyy"));
            }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        if (c != null) {
            etTitle.setText(c.title);
            spType.setSelection(Math.max(0, types.indexOf(c.type)));
            spWeight.setSelection(c.weight - 1);
            spDiff.setSelection(c.difficulty - 1);
            if (c.grade >= 0) etGrade.setText(String.valueOf(c.grade));
        } else {
            spWeight.setSelection(2);
            spDiff.setSelection(2);
        }

        MaterialAlertDialogBuilder bd = new MaterialAlertDialogBuilder(ctx)
                .setTitle(c == null ? "Add" : "Edit").setView(v)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (dlg, w) -> {
                    String title = etTitle.getText().toString().trim();
                    if (title.isEmpty()) { Toast.makeText(ctx, "Give it a title.", Toast.LENGTH_SHORT).show(); return; }
                    Commitment n = c != null ? c : new Commitment();
                    n.userId = u.id;
                    n.title = title;
                    n.type = types.get(spType.getSelectedItemPosition());
                    n.weight = spWeight.getSelectedItemPosition() + 1;
                    n.difficulty = spDiff.getSelectedItemPosition() + 1;
                    n.dueDate = due[0];
                    n.grade = -1;
                    String g = etGrade.getText().toString().trim().replace(',', '.');
                    if (st && !g.isEmpty()) {
                        try { n.grade = Math.max(0, Math.min(20, Double.parseDouble(g))); } catch (NumberFormatException ignored) { }
                    }
                    Context app = ctx.getApplicationContext();
                    Utils.bg(() -> {
                        if (n.id == 0) AppDatabase.get(app).commitments().insert(n);
                        else AppDatabase.get(app).commitments().update(n);
                        Utils.ui(() -> { if (b != null) refresh(); });
                    });
                });
        if (c != null) bd.setNeutralButton("Delete", (dlg, w) -> {
            Context app = ctx.getApplicationContext();
            Utils.bg(() -> {
                AppDatabase.get(app).commitments().delete(c);
                Utils.ui(() -> { if (b != null) refresh(); });
            });
        });
        AlertDialog d = bd.create();
        d.show();
    }
}
