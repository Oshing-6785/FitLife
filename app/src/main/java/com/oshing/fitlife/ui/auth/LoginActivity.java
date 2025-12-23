package com.oshing.fitlife.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;
import com.oshing.fitlife.ui.dashboard.DashboardActivity;
import com.oshing.fitlife.utils.SessionManager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;

    private Button btnLoginSubmit;

    private android.widget.TextView tvForgotPassword;
    private android.widget.TextView tvGoRegister;

    private DatabaseHelper dbHelper;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private CharSequence originalBtnText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        String prefill = getIntent().getStringExtra("prefill_email");
        if (prefill != null && etEmail != null) etEmail.setText(prefill);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new DatabaseHelper(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // New Material wrappers
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);

        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvGoRegister = findViewById(R.id.tvGoRegister);

        originalBtnText = btnLoginSubmit.getText();

        btnLoginSubmit.setOnClickListener(v -> handleLogin());

        // Optional links
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        }

        if (tvGoRegister != null) {
            tvGoRegister.setOnClickListener(v -> {
                // leave as toast so app doesn't crash.
                try {
                    // Change this class name if yours is different:
                    Class<?> clazz = Class.forName("com.oshing.fitlife.ui.auth.RegisterActivity");
                    startActivity(new Intent(LoginActivity.this, clazz));
                } catch (Exception e) {
                    Toast.makeText(this, "Register screen not added yet", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleLogin() {
        clearErrors();

        String email = getText(etEmail).trim().toLowerCase(Locale.ROOT);
        String password = getText(etPassword).trim();

        boolean ok = true;

        if (email.isEmpty()) {
            setEmailError("Email is required");
            ok = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setEmailError("Enter a valid email");
            ok = false;
        }

        if (password.isEmpty()) {
            setPasswordError("Password is required");
            ok = false;
        } else if (password.length() < 6) {
            setPasswordError("Password must be at least 6 characters");
            ok = false;
        }

        if (!ok) return;

        setLoading(true);

        // single source of truth for login
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        setLoading(false);
                        Toast.makeText(this, "Login failed (no Firebase user)", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    onFirebaseLoginSuccess(user);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);

                    if (e instanceof FirebaseAuthInvalidUserException) {
                        setEmailError("Account not found. Please register.");
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        setPasswordError("Invalid email or password");
                    } else {
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleForgotPassword() {
        clearErrors();

        String email = getText(etEmail).trim().toLowerCase(Locale.ROOT);

        if (email.isEmpty()) {
            setEmailError("Enter your email first");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setEmailError("Enter a valid email");
            return;
        }

        setLoading(true);

        firebaseAuth.sendPasswordResetEmail(email)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(this, "Password reset link sent to your email", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Reset failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void onFirebaseLoginSuccess(FirebaseUser firebaseUser) {
        final String uid = firebaseUser.getUid();
        final String email = (firebaseUser.getEmail() != null)
                ? firebaseUser.getEmail().trim().toLowerCase(Locale.ROOT)
                : "";

        if (email.isEmpty()) {
            setLoading(false);
            Toast.makeText(this, "Firebase account has no email", Toast.LENGTH_SHORT).show();
            return;
        }

        final String fallbackName = deriveNameFromEmail(email);

        // upsert Firestore profile
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("email", email);
        data.put("name", fallbackName);
        data.put("updatedAt", System.currentTimeMillis());

        firestore.collection("users")
                .document(uid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    int localUserId = ensureLocalUserExists(email, fallbackName);
                    if (localUserId <= 0) {
                        setLoading(false);
                        Toast.makeText(this, "Local user mapping failed", Toast.LENGTH_LONG).show();
                        return;
                    }

                    SessionManager.saveLoginSession(this, localUserId, email);

                    setLoading(false);
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    finish();
                })
                .addOnFailureListener(err -> {
                    setLoading(false);
                    Toast.makeText(this, "Firestore error: " + err.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private int ensureLocalUserExists(String email, String name) {
        DatabaseHelper.LocalUser existing = dbHelper.getLocalUserByEmail(email);
        if (existing != null) return existing.id;

        String randomPassword = UUID.randomUUID().toString();
        long inserted = dbHelper.registerUser(name, email, randomPassword);

        if (inserted > 0) {
            DatabaseHelper.LocalUser created = dbHelper.getLocalUserByEmail(email);
            if (created != null) return created.id;
        }

        DatabaseHelper.LocalUser retry = dbHelper.getLocalUserByEmail(email);
        return (retry != null) ? retry.id : -1;
    }

    private String deriveNameFromEmail(String email) {
        try {
            String prefix = email.split("@")[0];
            if (prefix == null || prefix.trim().isEmpty()) return "User";
            prefix = prefix.replace(".", " ").replace("_", " ").trim();
            if (prefix.isEmpty()) return "User";
            return prefix.substring(0, 1).toUpperCase(Locale.ROOT) + prefix.substring(1);
        } catch (Exception e) {
            return "User";
        }
    }

    private void setLoading(boolean loading) {
        btnLoginSubmit.setEnabled(!loading);
        if (etEmail != null) etEmail.setEnabled(!loading);
        if (etPassword != null) etPassword.setEnabled(!loading);

        if (loading) {
            btnLoginSubmit.setText("Please wait...");
        } else {
            btnLoginSubmit.setText(originalBtnText);
        }
    }

    private void clearErrors() {
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
    }

    private void setEmailError(String msg) {
        if (tilEmail != null) {
            tilEmail.setError(msg);
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void setPasswordError(String msg) {
        if (tilPassword != null) {
            tilPassword.setError(msg);
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private String getText(TextInputEditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString();
    }
}
