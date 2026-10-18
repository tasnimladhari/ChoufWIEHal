package com.choufwie.hal.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.choufwie.hal.data.AppDatabase;
import com.choufwie.hal.data.Entities.User;
import com.choufwie.hal.databinding.ActivityRegisterBinding;
import com.choufwie.hal.util.Prefs;
import com.choufwie.hal.util.Security;
import com.choufwie.hal.util.Utils;

import java.util.Locale;

/** Local-only account: nothing is sent anywhere. The chosen MODE is saved and reused at every login. */
public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        // Image comes from activity_register.xml (@drawable/choufwie_icon_transparent)
        b.btnRegister.setOnClickListener(v -> register());
    }

    private void register() {
        String name = b.etName.getText().toString().trim();
        String username = b.etUser.getText().toString().trim().toLowerCase(Locale.ROOT);
        String pw = b.etPass.getText().toString();
        if (name.isEmpty() || username.length() < 3) { toast("Enter your name and a username (3+ characters)."); return; }
        if (pw.length() < 6) { toast("Password: at least 6 characters."); return; }
        String mode = b.rbGrad.isChecked() ? Utils.GRADUATE : Utils.STUDENT;

        Utils.bg(() -> {
            AppDatabase db = AppDatabase.get(this);
            if (db.users().byUsername(username) != null) { Utils.ui(() -> toast("This username is already taken.")); return; }
            User u = new User();
            u.name = name;
            u.username = username;
            u.salt = Security.newSalt();
            u.passHash = Security.hash(pw, u.salt);
            u.mode = mode;
            u.createdAt = System.currentTimeMillis();
            u.jobSearchStart = Utils.GRADUATE.equals(mode) ? u.createdAt : 0;
            long id = db.users().insert(u);
            Utils.ui(() -> {
                Prefs.setUserId(this, id);
                Intent i = new Intent(this, MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            });
        });
    }

    private void toast(String m) { Toast.makeText(this, m, Toast.LENGTH_SHORT).show(); }
}