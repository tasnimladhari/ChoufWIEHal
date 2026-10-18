package com.choufwie.hal.ui;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.choufwie.hal.R;
import com.choufwie.hal.ai.AiService;
import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.ActivityMainBinding;
import com.choufwie.hal.util.Prefs;
import com.choufwie.hal.util.Utils;

/** Hosts the 5 tabs + the always-visible SOS button. */
public class MainActivity extends AppCompatActivity {
    /** The logged-in user (kept fresh when the mode changes). */
    public static User user;
    private ActivityMainBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(null); // no fragment restoration: we rebuild from the saved session
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        long uid = Prefs.userId(this);
        Utils.bg(() -> {
            User u = AppDatabase.get(this).users().byId(uid);
            Utils.ui(() -> {
                if (u == null) { logout(); return; }
                user = u;
                setup();
            });
            new AiService(this).warmUp(); // pre-load the LLM so the first answer is fast
        });
    }

    private void setup() {
        b.nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment f = id == R.id.nav_home ? new HomeFragment()
                    : id == R.id.nav_plan ? new PlanFragment()
                    : id == R.id.nav_checkin ? new CheckInFragment()
                    : id == R.id.nav_insights ? new InsightsFragment()
                    : new ToolkitFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, f).commit();
            return true;
        });
        b.nav.setSelectedItemId(R.id.nav_home);
        b.fabSos.setOnClickListener(v -> startActivity(new Intent(this, BreathingActivity.class)));
    }

    private void logout() {
        Prefs.setUserId(this, -1);
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}
