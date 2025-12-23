package com.oshing.fitlife.ui.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.oshing.fitlife.R;
import com.oshing.fitlife.ui.MainActivity;
import com.oshing.fitlife.ui.auth.LoginActivity;
import com.oshing.fitlife.ui.checklist.GlobalChecklistActivity;
import com.oshing.fitlife.ui.delegation.DelegationHomeActivity;
import com.oshing.fitlife.ui.routines.RoutineListActivity;
import com.oshing.fitlife.ui.routines.TrashActivity;
import com.oshing.fitlife.utils.SessionManager;

public class DashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;

    // clickable cards
    private MaterialCardView btnMyRoutines;
    private MaterialCardView btnGlobalChecklist;
    private MaterialCardView btnDelegation;
    private MaterialCardView btnTrash;

    // MaterialButton
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SessionManager.isSessionValid(this)) {
            startActivity(new Intent(DashboardActivity.this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);

        btnMyRoutines = findViewById(R.id.btnMyRoutines);
        btnGlobalChecklist = findViewById(R.id.btnGlobalChecklist);
        btnDelegation = findViewById(R.id.btnDelegation);
        btnTrash = findViewById(R.id.btnTrash);
        btnLogout = findViewById(R.id.btnLogout);

        String email = SessionManager.getUserEmail(this);
        if (email == null || email.trim().isEmpty()) email = "User";

        tvWelcome.setText(getString(R.string.welcome_user, email));

        enableCardClick(btnMyRoutines);
        enableCardClick(btnGlobalChecklist);
        enableCardClick(btnDelegation);
        enableCardClick(btnTrash);

        btnMyRoutines.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, RoutineListActivity.class))
        );

        btnGlobalChecklist.setOnClickListener(v -> {
            int userId = SessionManager.getUserId(DashboardActivity.this);
            if (userId == -1) {
                Toast.makeText(this, "Session missing. Please login again.", Toast.LENGTH_SHORT).show();
                SessionManager.clearSession(DashboardActivity.this);
                startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                finish();
                return;
            }

            Intent intent = new Intent(DashboardActivity.this, GlobalChecklistActivity.class);
            intent.putExtra("user_id", userId);
            startActivity(intent);
        });

        //  DELEGATION / FRIENDS / MESSAGES
        btnDelegation.setOnClickListener(v -> {
            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            if (fbUser == null) {
                Toast.makeText(this, "Please login again to enable Delegation (Firebase).", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
                return;
            }
            startActivity(new Intent(DashboardActivity.this, DelegationHomeActivity.class));
        });

        btnTrash.setOnClickListener(v ->
                startActivity(new Intent(DashboardActivity.this, TrashActivity.class))
        );

        btnLogout.setOnClickListener(v -> showLogoutConfirm());
    }

    private void enableCardClick(MaterialCardView card) {
        if (card == null) return;
        card.setClickable(true);
        card.setFocusable(true);
        card.setForeground(getResources().getDrawable(android.R.drawable.list_selector_background, getTheme()));
    }

    private void showLogoutConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Logout?")
                .setMessage("You will need to login again to access your data.")
                .setPositiveButton("Logout", (d, w) -> {
                    SessionManager.clearSession(DashboardActivity.this);
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
