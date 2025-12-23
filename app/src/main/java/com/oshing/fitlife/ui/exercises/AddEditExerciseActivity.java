package com.oshing.fitlife.ui.exercises;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;

public class AddEditExerciseActivity extends AppCompatActivity {

    private static final String STATE_IMAGE_URI = "state_image_uri";

    private DatabaseHelper db;

    private int routineId;
    private int exerciseId = -1;
    private boolean isEdit = false;

    private TextInputLayout tilName, tilSets, tilReps, tilType, tilWeight, tilRest, tilDuration;
    private TextInputEditText etName, etSets, etReps, etEquipment, etInstructions;
    private TextInputEditText etWeight, etRestSeconds, etDurationMin;
    private MaterialAutoCompleteTextView actvType;

    private ShapeableImageView ivExercisePhoto;
    private MaterialButton btnPickExerciseImage;
    private MaterialButton btnRemoveExerciseImage;

    //  Stored image
    private String selectedImageUri = "";

    private MaterialButton btnSave;
    private MaterialButton btnCancel;

    private ActivityResultLauncher<String[]> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_exercise);

        db = new DatabaseHelper(this);

        routineId = getIntent().getIntExtra("routine_id", -1);
        isEdit = getIntent().getBooleanExtra("is_edit", false);
        exerciseId = getIntent().getIntExtra("exercise_id", -1);

        if (routineId == -1) {
            Toast.makeText(this, "Routine not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (savedInstanceState != null) {
            selectedImageUri = safe(savedInstanceState.getString(STATE_IMAGE_URI));
        }

        // form fields
        tilName = findViewById(R.id.tilExerciseName);
        tilSets = findViewById(R.id.tilSets);
        tilReps = findViewById(R.id.tilReps);

        etName = findViewById(R.id.etExerciseName);
        etSets = findViewById(R.id.etSets);
        etReps = findViewById(R.id.etReps);
        etEquipment = findViewById(R.id.etEquipment);
        etInstructions = findViewById(R.id.etInstructions);

        tilType = findViewById(R.id.tilExerciseType);
        actvType = findViewById(R.id.actvExerciseType);

        tilWeight = findViewById(R.id.tilWeight);
        tilRest = findViewById(R.id.tilRest);
        tilDuration = findViewById(R.id.tilDuration);

        etWeight = findViewById(R.id.etWeight);
        etRestSeconds = findViewById(R.id.etRestSeconds);
        etDurationMin = findViewById(R.id.etDurationMin);

        //  Image views
        ivExercisePhoto = findViewById(R.id.ivExercisePhoto);
        btnPickExerciseImage = findViewById(R.id.btnPickExerciseImage);
        btnRemoveExerciseImage = findViewById(R.id.btnRemoveExerciseImage);

        btnSave = findViewById(R.id.btnSaveExercise);
        btnCancel = findViewById(R.id.btnCancelExercise);

        setupTypeDropdown();
        setupImagePicker();

        if (isEdit) {
            setTitle("Edit Exercise");
            btnSave.setText("Update Exercise");
            loadForEdit();
        } else {
            setTitle("Add Exercise");
            btnSave.setText("Add Exercise");
            applyImageUriToPreview(); // show placeholder / restore
        }

        btnCancel.setOnClickListener(v -> finish());

        btnPickExerciseImage.setOnClickListener(v -> {
            // Uses ACTION_OPEN_DOCUMENT under the hood
            pickImageLauncher.launch(new String[]{"image/*"});
        });

        btnRemoveExerciseImage.setOnClickListener(v -> {
            selectedImageUri = "";
            applyImageUriToPreview();
        });

        btnSave.setOnClickListener(v -> {
            clearErrors();
            if (!validate()) return;

            String name = safe(etName);
            int sets = parseIntSafe(etSets);
            int reps = parseIntSafe(etReps);
            String equipment = safe(etEquipment);
            String instructions = safe(etInstructions);

            String type = safe(actvType);
            if (type.isEmpty()) type = "Strength";

            double weight = parseDoubleSafe(etWeight);
            int restSec = parseIntSafe(etRestSeconds);
            int durationMin = parseIntSafe(etDurationMin);

            // Save selected image
            String imageUri = safe(selectedImageUri);

            if (isEdit) {
                int rows = db.updateExercise(
                        exerciseId, routineId,
                        name, sets, reps, equipment, instructions, imageUri,
                        type, weight, restSec, durationMin
                );
                if (rows > 0) {
                    Toast.makeText(this, "Exercise updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                }
            } else {
                long id = db.addExercise(
                        routineId,
                        name, sets, reps, equipment, instructions, imageUri,
                        type, weight, restSec, durationMin
                );
                if (id != -1) {
                    Toast.makeText(this, "Exercise added", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Failed to add", Toast.LENGTH_SHORT).show();
                }
            }
        });

        etName.addTextChangedListener(new SimpleTextWatcher(() -> tilName.setError(null)));
        etSets.addTextChangedListener(new SimpleTextWatcher(() -> tilSets.setError(null)));
        etReps.addTextChangedListener(new SimpleTextWatcher(() -> tilReps.setError(null)));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_IMAGE_URI, selectedImageUri);
    }

    private void setupTypeDropdown() {
        String[] types = new String[]{"Strength", "Cardio", "Mobility", "Stretching", "Custom"};
        android.widget.ArrayAdapter<String> adapter =
                new android.widget.ArrayAdapter<>(this, android.R.layout.simple_list_item_1, types);

        actvType.setAdapter(adapter);
        if (safe(actvType).isEmpty()) actvType.setText("Strength", false);
    }

    private void setupImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null) return;

                    // Persist read permission so the app can load it later
                    try {
                        final int flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        getContentResolver().takePersistableUriPermission(uri, flags);
                    } catch (SecurityException ignored) {
                        // Some providers allow only READ. Try again with READ only.
                        try {
                            final int flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, flags);
                        } catch (Exception ignored2) { }
                    } catch (Exception ignored) { }

                    selectedImageUri = uri.toString();
                    applyImageUriToPreview();
                }
        );
    }

    private void applyImageUriToPreview() {
        boolean hasImage = selectedImageUri != null && !selectedImageUri.trim().isEmpty();

        if (!hasImage) {
            // Placeholder
            ivExercisePhoto.setImageResource(R.drawable.ic_image_placeholder);
            btnRemoveExerciseImage.setEnabled(false);
            btnRemoveExerciseImage.setAlpha(0.5f);
            return;
        }

        try {
            Uri uri = Uri.parse(selectedImageUri);
            ivExercisePhoto.setImageURI(uri);

            // fallback
            if (ivExercisePhoto.getDrawable() == null) {
                ivExercisePhoto.setImageResource(R.drawable.ic_image_placeholder);
                btnRemoveExerciseImage.setEnabled(false);
                btnRemoveExerciseImage.setAlpha(0.5f);
                selectedImageUri = "";
                return;
            }

            btnRemoveExerciseImage.setEnabled(true);
            btnRemoveExerciseImage.setAlpha(1f);
        } catch (Exception e) {
            ivExercisePhoto.setImageResource(R.drawable.ic_image_placeholder);
            btnRemoveExerciseImage.setEnabled(false);
            btnRemoveExerciseImage.setAlpha(0.5f);
            selectedImageUri = "";
        }
    }

    private void loadForEdit() {
        if (exerciseId == -1) return;

        Cursor c = null;
        try {
            c = db.getExerciseById(exerciseId);
            if (c == null || !c.moveToFirst()) {
                Toast.makeText(this, "Exercise not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            int dbRoutineId = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_ROUTINE_ID));
            if (dbRoutineId != routineId) {
                Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            etName.setText(safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_NAME))));
            etSets.setText(String.valueOf(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_SETS))));
            etReps.setText(String.valueOf(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_REPS))));
            etEquipment.setText(safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_EQUIPMENT))));
            etInstructions.setText(safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_INSTRUCTIONS))));

            try {
                selectedImageUri = safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_IMAGE_URI)));
            } catch (Exception ignored) {
                selectedImageUri = safe(selectedImageUri); // keep restored state if any
            }
            applyImageUriToPreview();

            String type = safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_TYPE)));
            if (type.isEmpty()) type = "Strength";
            actvType.setText(type, false);

            try {
                double w = c.getDouble(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_WEIGHT));
                if (w > 0) etWeight.setText(String.valueOf(w));
            } catch (Exception ignored) { }

            try {
                int rest = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_REST_SECONDS));
                if (rest > 0) etRestSeconds.setText(String.valueOf(rest));
            } catch (Exception ignored) { }

            try {
                int dur = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_EX_DURATION_MIN));
                if (dur > 0) etDurationMin.setText(String.valueOf(dur));
            } catch (Exception ignored) { }

        } catch (Exception e) {
            Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
            finish();
        } finally {
            if (c != null) c.close();
        }
    }

    private void clearErrors() {
        tilName.setError(null);
        tilSets.setError(null);
        tilReps.setError(null);

        tilType.setError(null);
        tilWeight.setError(null);
        tilRest.setError(null);
        tilDuration.setError(null);
    }

    private boolean validate() {
        boolean ok = true;

        String name = safe(etName);
        if (name.isEmpty()) {
            tilName.setError("Exercise name is required");
            ok = false;
        }

        int sets = parseIntSafe(etSets);
        int reps = parseIntSafe(etReps);

        if (sets < 0 || sets > 99) {
            tilSets.setError("Sets must be 0-99");
            ok = false;
        }
        if (reps < 0 || reps > 999) {
            tilReps.setError("Reps must be 0-999");
            ok = false;
        }

        double w = parseDoubleSafe(etWeight);
        if (w < 0 || w > 9999) {
            tilWeight.setError("Weight must be 0-9999");
            ok = false;
        }

        int rest = parseIntSafe(etRestSeconds);
        if (rest < 0 || rest > 3600) {
            tilRest.setError("Rest must be 0-3600 sec");
            ok = false;
        }

        int dur = parseIntSafe(etDurationMin);
        if (dur < 0 || dur > 999) {
            tilDuration.setError("Duration must be 0-999 min");
            ok = false;
        }

        return ok;
    }

    private int parseIntSafe(TextInputEditText et) {
        try {
            String s = et.getText() == null ? "" : et.getText().toString().trim();
            if (s.isEmpty()) return 0;
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    private double parseDoubleSafe(TextInputEditText et) {
        try {
            String s = et.getText() == null ? "" : et.getText().toString().trim();
            if (s.isEmpty()) return 0.0;
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String safe(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private String safe(MaterialAutoCompleteTextView actv) {
        return actv.getText() == null ? "" : actv.getText().toString().trim();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable after;
        SimpleTextWatcher(Runnable after) { this.after = after; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { if (after != null) after.run(); }
    }
}
