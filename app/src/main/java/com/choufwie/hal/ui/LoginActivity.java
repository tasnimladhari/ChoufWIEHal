package com.choufwie.hal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.ActivityLoginBinding;
import com.choufwie.hal.util.Prefs;
import com.choufwie.hal.util.Security;
import com.choufwie.hal.util.Utils;

import java.util.Locale;

/** Launcher screen. If a session exists, go straight to the app (mode is already saved on the user). */
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Prefs.userId(this) > 0) { go(); return; }
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.linkRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));









































        b.btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        long now = System.currentTimeMillis();
        long lock = Prefs.lng(this, "lockUntil", 0);
        if (now < lock) {
            Toast.makeText(this, "Too many attempts. Try again in " + ((lock - now) / 60000 + 1) + " min.", Toast.LENGTH_LONG).show();
            return;
        }
        String name = b.etUser.getText().toString().trim().toLowerCase(Locale.ROOT);
        String pw = b.etPass.getText().toString();
        Utils.bg(() -> {
            User u = AppDatabase.get(this).users().byUsername(name);
            boolean ok = u != null && Security.verify(pw, u.salt, u.passHash);
            Utils.ui(() -> {
                if (ok) {
                    Prefs.putInt(this, "fails", 0);
                    Prefs.setUserId(this, u.id);
                    go();
                } else {
                    int f = Prefs.integer(this, "fails", 0) + 1;
                    if (f >= 5) { // basic brute-force protection
                        Prefs.putLng(this, "lockUntil", System.currentTimeMillis() + 5 * 60_000L);
                        f = 0;
                        Toast.makeText(this, "Too many attempts. Locked for 5 minutes.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Wrong username or password.", Toast.LENGTH_SHORT).show();
                    }
                    Prefs.putInt(this, "fails", f);
                }
            });
        });
    }

    private void go() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
