package com.oshing.fitlife.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.oshing.fitlife.R;
import com.oshing.fitlife.ui.auth.LoginActivity;
import com.oshing.fitlife.ui.auth.SignupActivity;
import com.oshing.fitlife.ui.dashboard.DashboardActivity;
import com.oshing.fitlife.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private Button btnLogin;
    private Button btnSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. session valid go  dashboard
        if (SessionManager.isSessionValid(this)) {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
            return; // do not show landing UI
        }

        // 2.No valid session
        setContentView(R.layout.activity_main);

        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LoginActivity.class))
        );

        btnSignup.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SignupActivity.class))
        );
    }
}
