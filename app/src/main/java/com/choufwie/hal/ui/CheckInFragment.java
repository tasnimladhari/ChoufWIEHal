package com.choufwie.hal.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.choufwie.hal.ai.AiService;
import com.choufwie.hal.ai.Analysis;
import com.choufwie.hal.ai.CrisisDetector;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.FragmentCheckinBinding;
import com.choufwie.hal.engine.Snapshot;
import com.choufwie.hal.util.Utils;

/**
 * Daily check-in (~30 s). Pipeline:
 *  crisis check (keywords) -> stage 1 LLM extraction -> save (AES-encrypted text) -> stage 2/3 risk + plan -> stage 4 Jannet.
 */
public class CheckInFragment extends Fragment {
    private FragmentCheckinBinding b;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        b = FragmentCheckinBinding.inflate(i, c, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        b.btnSubmit.setOnClickListener(x -> submit());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }

    private void submit() {
        String text = b.entry.getText().toString().trim();
        if (text.isEmpty()) { Toast.makeText(requireContext(), "Write a few words first 💜", Toast.LENGTH_SHORT).show(); return; }
        double sleep = b.sleep.getValue();
        int energy = (int) b.energy.getValue();
        boolean move = b.cbMove.isChecked(), water = b.cbWater.isChecked();
        Context ctx = requireContext();
        User u = MainActivity.user;

        if (CrisisDetector.isCrisis(text)) {            // never depends on the LLM
            Analysis a = new Analysis();
            a.stress = 9;
            a.emotion = "overwhelmed";
            Utils.bg(() -> Snapshot.saveCheckIn(ctx.getApplicationContext(), u, text, sleep, energy, move, water, a));
            Ui.crisis(ctx);
            reset();
            return;
        }

        b.btnSubmit.setEnabled(false);
        JannetDialog d = new JannetDialog(ctx, "Jannet reads your check-in 🔮", "🔮 Jannet is reading the cards…");
        Utils.bg(() -> {
            Context app = ctx.getApplicationContext();
            AiService ai = new AiService(app);
            Analysis a = ai.analyze(u.mode, text, sleep, energy);          // stage 1
            Snapshot.saveCheckIn(app, u, text, sleep, energy, move, water, a);
            Snapshot s = Snapshot.load(app, u);                            // stages 2 + 3
            String say = ai.jannet(s.facts(a), text, s.fallback(a));       // stage 4
            Utils.ui(() -> {
                d.say(say);
                if (b != null) { reset(); b.btnSubmit.setEnabled(true); }
            });
        });
    }

    private void reset() {
        if (b != null) b.entry.setText("");
    }
}
