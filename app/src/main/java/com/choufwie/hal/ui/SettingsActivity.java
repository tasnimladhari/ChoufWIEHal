package com.choufwie.hal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.ActivitySettingsBinding;
import com.choufwie.hal.engine.DemoSeeder;
import com.choufwie.hal.ai.OllamaProvider;
import com.choufwie.hal.util.Prefs;
import com.choufwie.hal.util.Utils;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        User u = MainActivity.user;
        b.etHost.setText(Prefs.host(this));
        b.etModel.setText(Prefs.model(this));
        refreshModeButton();

        b.btnSave.setOnClickListener(v -> { save(); toast("Saved."); });
        b.btnTest.setOnClickListener(v -> {
            save();
            String host = b.etHost.getText().toString();
            Utils.bg(() -> {
                try { OllamaProvider.ping(host); Utils.ui(() -> toast("✅ Ollama is reachable")); }
                catch (Exception e) { Utils.ui(() -> toast("❌ Can't reach Ollama: " + e.getMessage())); }
            });
        });
        b.btnMode.setOnClickListener(v -> Utils.bg(() -> {
            boolean toGrad = Utils.STUDENT.equals(u.mode);
            u.mode = toGrad ? Utils.GRADUATE : Utils.STUDENT;
            if (toGrad && u.jobSearchStart == 0) u.jobSearchStart = System.currentTimeMillis();
            AppDatabase.get(this).users().update(u); // same object as MainActivity.user
            Utils.ui(() -> { refreshModeButton(); toast(toGrad ? "Congrats, graduate 🎓 Mode switched." : "Back to student mode."); });
        }));
        b.btnDemo.setOnClickListener(v -> Utils.bg(() -> {
            DemoSeeder.seed(getApplicationContext(), u);
            Utils.ui(() -> toast("Demo data loaded (simulated)."));
        }));
        b.btnClear.setOnClickListener(v -> Utils.bg(() -> {
            AppDatabase db = AppDatabase.get(this);
            db.commitments().clear(u.id);
            db.checkIns().clear(u.id);
            Utils.ui(() -> toast("Your plan and check-ins were erased."));
        }));
        b.btnLogout.setOnClickListener(v -> {
            Prefs.setUserId(this, -1);
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        });
    }

    private void save() { Prefs.setLlm(this, b.etHost.getText().toString(), b.etModel.getText().toString()); }

    private void refreshModeButton() {
        b.btnMode.setText(Utils.STUDENT.equals(MainActivity.user.mode)
                ? "I graduated 🎓 (switch to graduate mode)" : "Back to student mode");
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_LONG).show(); }
}
