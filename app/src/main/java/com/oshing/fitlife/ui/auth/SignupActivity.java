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
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout tilName, tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPassword, etConfirmPassword;

    private Button btnSignup;
    private android.widget.TextView tvGoLogin;

    private DatabaseHelper dbHelper;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private CharSequence originalBtnText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        dbHelper = new DatabaseHelper(this);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // wrappers
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnSignup = findViewById(R.id.btnSignup);
        tvGoLogin = findViewById(R.id.tvGoLogin);

        originalBtnText = btnSignup.getText();

        btnSignup.setOnClickListener(v -> handleSignup());

        if (tvGoLogin != null) {
            tvGoLogin.setOnClickListener(v -> {
                Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
                finish();
            });
        }
    }

    private void handleSignup() {
        clearErrors();

        String name = getText(etName).trim();
        String email = getText(etEmail).trim().toLowerCase(Locale.ROOT);
        String password = getText(etPassword).trim();
        String confirm = getText(etConfirmPassword).trim();

        boolean ok = true;

        if (name.isEmpty()) {
            setNameError("Name is required");
            ok = false;
        } else if (name.length() < 2) {
            setNameError("Enter a valid name");
            ok = false;
        }

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

        if (confirm.isEmpty()) {
            setConfirmError("Confirm your password");
            ok = false;
        } else if (!password.equals(confirm)) {
            setConfirmError("Passwords do not match");
            ok = false;
        }

        if (!ok) return;

        setLoading(true);

        // 1.Create Firebase Auth account
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        setLoading(false);
                        Toast.makeText(this, "Signup failed (no user)", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String uid = firebaseUser.getUid();
                    long now = System.currentTimeMillis();

                    // 2.Save profile
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("name", name);
                    data.put("email", email);
                    data.put("createdAt", now);
                    data.put("updatedAt", now);

                    firestore.collection("users").document(uid)
                            .set(data)
                            .addOnSuccessListener(unused -> {
                                // 3.Create local SQLite mapping
                                int localUserId = ensureLocalUserExists(name, email);
                                if (localUserId <= 0) {
                                    setLoading(false);
                                    Toast.makeText(this, "Local user mapping failed", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                setLoading(false);
                                Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show();

                                // Force login again
                                firebaseAuth.signOut();

                                Intent i = new Intent(SignupActivity.this, LoginActivity.class);
                                i.putExtra("prefill_email", email);
                                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        setEmailError("Email is already registered");
                    } else {
                        Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int ensureLocalUserExists(String name, String email) {
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

    private void setLoading(boolean loading) {
        btnSignup.setEnabled(!loading);
        if (etName != null) etName.setEnabled(!loading);
        if (etEmail != null) etEmail.setEnabled(!loading);
        if (etPassword != null) etPassword.setEnabled(!loading);
        if (etConfirmPassword != null) etConfirmPassword.setEnabled(!loading);

        if (loading) {
            btnSignup.setText("Creating...");
        } else {
            btnSignup.setText(originalBtnText);
        }
    }

    private void clearErrors() {
        if (tilName != null) tilName.setError(null);
        if (tilEmail != null) tilEmail.setError(null);
        if (tilPassword != null) tilPassword.setError(null);
        if (tilConfirmPassword != null) tilConfirmPassword.setError(null);
    }

    private void setNameError(String msg) {
        if (tilName != null) tilName.setError(msg);
        else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setEmailError(String msg) {
        if (tilEmail != null) tilEmail.setError(msg);
        else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setPasswordError(String msg) {
        if (tilPassword != null) tilPassword.setError(msg);
        else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setConfirmError(String msg) {
        if (tilConfirmPassword != null) tilConfirmPassword.setError(msg);
        else Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String getText(TextInputEditText et) {
        return (et == null || et.getText() == null) ? "" : et.getText().toString();
    }
}
