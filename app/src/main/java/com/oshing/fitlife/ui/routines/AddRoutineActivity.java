package com.oshing.fitlife.ui.routines;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;
import com.oshing.fitlife.ui.exercises.ExerciseListActivity;
import com.oshing.fitlife.utils.SessionManager;

import java.util.Calendar;
import java.util.Locale;

public class AddRoutineActivity extends AppCompatActivity {

    private AutoCompleteTextView etRoutineName;
    private Spinner spinnerGoal;
    private ChipGroup chipGroupDays;
    private TextView tvSelectedTime;
    private TextInputEditText etNotes;
    private Button btnSaveRoutine;
    private Button btnManageExercises;

    private TextInputLayout tilRoutineName;
    private TextInputLayout tilTime;
    private TextInputLayout tilNotes;
    private TextView tvTitle;

    private DatabaseHelper dbHelper;

    private String selectedTime = "";
    private boolean isEdit = false;
    private int routineId = -1;

    private static final String[] DAYS = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_routine);

        dbHelper = new DatabaseHelper(this);

        tvTitle = findViewById(R.id.tvAddRoutineTitle);

        tilRoutineName = findViewById(R.id.tilRoutineName);
        tilTime = findViewById(R.id.tilTime);
        tilNotes = findViewById(R.id.tilNotes);

        etRoutineName = findViewById(R.id.etRoutineName);
        spinnerGoal = findViewById(R.id.spinnerGoal);
        chipGroupDays = findViewById(R.id.chipGroupDays);
        tvSelectedTime = findViewById(R.id.tvSelectedTime);
        etNotes = findViewById(R.id.etNotes);
        btnSaveRoutine = findViewById(R.id.btnSaveRoutine);

        btnManageExercises = findViewById(R.id.btnManageExercises);
        btnManageExercises.setVisibility(View.GONE);

        setupRoutineNameSuggestions();
        setupGoalSpinner();
        setupDaysChipsFixedOrder();
        setupTimePickerUx();

        isEdit = getIntent().getBooleanExtra("is_edit", false);
        routineId = getIntent().getIntExtra("routine_id", -1);

        if (isEdit) {
            tvTitle.setText("Edit Routine");
            btnSaveRoutine.setText("Update Routine");
            loadRoutineForEdit(routineId);

            btnManageExercises.setVisibility(View.VISIBLE);
            btnManageExercises.setOnClickListener(v -> {
                Intent i = new Intent(AddRoutineActivity.this, ExerciseListActivity.class);
                i.putExtra("routine_id", routineId);
                startActivity(i);
            });
        } else {
            tvTitle.setText(getString(R.string.add_routine_title));
            btnSaveRoutine.setText(getString(R.string.add_routine_save_btn));

            btnManageExercises.setVisibility(View.GONE);

            autoSelectTodayChipFixedOrder();
            tvSelectedTime.setText(getString(R.string.add_routine_time_hint));
        }

        btnSaveRoutine.setOnClickListener(v -> {
            clearErrors();
            if (!validateForm()) return;

            if (isEdit) updateRoutine();
            else saveRoutine();
        });

        etRoutineName.addTextChangedListener(new SimpleTextWatcher(() -> tilRoutineName.setError(null)));
        etNotes.addTextChangedListener(new SimpleTextWatcher(() -> tilNotes.setError(null)));

        tvSelectedTime.setOnClickListener(v -> openTimePickerSafe());
        tilTime.setEndIconOnClickListener(v -> openTimePickerSafe());

        tilTime.setOnClickListener(v -> openTimePickerSafe());
        if (tilTime.getEditText() != null) {
            tilTime.getEditText().setOnClickListener(v -> openTimePickerSafe());
        }
    }

    private void openTimePickerSafe() {
        tilTime.setError(null);
        openTimePicker();
    }

    // Setup
    private void setupRoutineNameSuggestions() {
        String[] suggestions = new String[]{
                "Full Body", "Push Day", "Pull Day", "Leg Day",
                "Upper Body", "Lower Body", "Cardio Session", "Core & Abs"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                suggestions
        );
        etRoutineName.setAdapter(adapter);
        etRoutineName.setThreshold(1);
    }

    private void setupGoalSpinner() {
        String[] goals = new String[]{
                "General Fitness", "Fat Loss", "Muscle Gain",
                "Strength", "Endurance", "Rehabilitation"
        };

        ArrayAdapter<String> goalAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                goals
        );
        goalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGoal.setAdapter(goalAdapter);
    }

    private void setupDaysChipsFixedOrder() {
        chipGroupDays.removeAllViews();

        for (String d : DAYS) {
            Chip chip = new Chip(this);
            chip.setText(d);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setId(View.generateViewId());
            chipGroupDays.addView(chip);
        }
    }

    private void autoSelectTodayChipFixedOrder() {
        int todayIdx = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1; // 0..6
        if (todayIdx < 0 || todayIdx > 6) todayIdx = 0;
        selectDayChip(DAYS[todayIdx]);
    }

    private void setupTimePickerUx() {
        tilTime.setEndIconOnClickListener(v -> openTimePickerSafe());
    }

    private void openTimePicker() {
        Calendar now = Calendar.getInstance();

        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);

        if (!selectedTime.isEmpty() && selectedTime.contains(":")) {
            try {
                String[] parts = selectedTime.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            } catch (Exception ignored) { }
        }

        TimePickerDialog dialog = new TimePickerDialog(
                AddRoutineActivity.this,
                (view, hourOfDay, min) -> {
                    selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, min);
                    tvSelectedTime.setText(selectedTime);
                },
                hour,
                minute,
                true
        );
        dialog.show();
    }

    // Validation + helpers
    private void clearErrors() {
        tilRoutineName.setError(null);
        tilTime.setError(null);
        tilNotes.setError(null);
    }

    private boolean validateForm() {
        String name = safeText(etRoutineName);
        String day = getSelectedDay();
        String notes = (etNotes.getText() == null) ? "" : etNotes.getText().toString().trim();

        boolean ok = true;

        if (name.isEmpty()) {
            tilRoutineName.setError("Routine name is required");
            ok = false;
        }

        if (day == null) {
            Toast.makeText(this, "Please select a day", Toast.LENGTH_SHORT).show();
            ok = false;
        }

        String timeText = tvSelectedTime.getText() == null ? "" : tvSelectedTime.getText().toString();
        if (selectedTime.isEmpty() || timeText.equals(getString(R.string.add_routine_time_hint))) {
            tilTime.setError("Please pick a time");
            ok = false;
        }

        if (notes.length() > 250) {
            tilNotes.setError("Keep notes under 250 characters");
            ok = false;
        }

        return ok;
    }

    private String safeText(AutoCompleteTextView v) {
        return v.getText() == null ? "" : v.getText().toString().trim();
    }

    private String getSelectedDay() {
        int checkedId = chipGroupDays.getCheckedChipId();
        if (checkedId == View.NO_ID) return null;

        View v = chipGroupDays.findViewById(checkedId);
        if (!(v instanceof Chip)) return null;

        Chip chip = (Chip) v;
        CharSequence t = chip.getText();
        return t == null ? null : t.toString();
    }

    private void selectDayChip(String dayText) {
        if (dayText == null) return;

        for (int i = 0; i < chipGroupDays.getChildCount(); i++) {
            View v = chipGroupDays.getChildAt(i);
            if (v instanceof Chip) {
                Chip chip = (Chip) v;
                if (dayText.equalsIgnoreCase(chip.getText().toString())) {
                    chip.setChecked(true);
                    return;
                }
            }
        }
    }

    private int getSpinnerIndex(Spinner spinner, String value) {
        if (value == null) return 0;
        for (int i = 0; i < spinner.getCount(); i++) {
            Object item = spinner.getItemAtPosition(i);
            if (item != null && value.equalsIgnoreCase(item.toString())) {
                return i;
            }
        }
        return 0;
    }

    // Load existing routine
    private void loadRoutineForEdit(int routineId) {
        if (routineId == -1) {
            Toast.makeText(this, "Invalid routine to edit", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int sessionUserId = SessionManager.getUserId(this);
        if (sessionUserId == -1) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Cursor c = null;
        try {
            c = dbHelper.getRoutineById(routineId);
            if (c == null || !c.moveToFirst()) {
                Toast.makeText(this, "Routine not found", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            int dbUserId = c.getInt(c.getColumnIndexOrThrow("user_id"));
            if (dbUserId != sessionUserId) {
                Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Block editing deleted routines
            try {
                int isDeleted = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_IS_DELETED));
                if (isDeleted == 1) {
                    Toast.makeText(this, "This routine is in Trash. Restore it first.", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            } catch (Exception ignored) { }

            String name = c.getString(c.getColumnIndexOrThrow("name"));
            String day = c.getString(c.getColumnIndexOrThrow("day_of_week"));
            String time = c.getString(c.getColumnIndexOrThrow("time"));
            String goal = c.getString(c.getColumnIndexOrThrow("goal"));
            String notes = c.getString(c.getColumnIndexOrThrow("notes"));

            if (name != null) etRoutineName.setText(name);
            spinnerGoal.setSelection(getSpinnerIndex(spinnerGoal, goal));
            selectDayChip(day);

            selectedTime = (time != null) ? time : "";
            tvSelectedTime.setText(selectedTime.isEmpty()
                    ? getString(R.string.add_routine_time_hint)
                    : selectedTime);

            if (notes != null) etNotes.setText(notes);

        } catch (Exception e) {
            Toast.makeText(this, "Failed to load routine", Toast.LENGTH_SHORT).show();
            finish();
        } finally {
            if (c != null) c.close();
        }
    }

    // Save / Update
    private void saveRoutine() {
        String name = safeText(etRoutineName);
        String day = getSelectedDay();
        String goal = (String) spinnerGoal.getSelectedItem();
        String notes = etNotes.getText() == null ? "" : etNotes.getText().toString().trim();

        int userId = SessionManager.getUserId(this);
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        long result = dbHelper.addRoutine(userId, name, day, selectedTime, goal, notes);
        if (result == -1) {
            Toast.makeText(this, "Failed to save routine", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Routine saved", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void updateRoutine() {
        if (routineId == -1) {
            Toast.makeText(this, "Invalid routine to update", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = safeText(etRoutineName);
        String day = getSelectedDay();
        String goal = (String) spinnerGoal.getSelectedItem();
        String notes = etNotes.getText() == null ? "" : etNotes.getText().toString().trim();

        int userId = SessionManager.getUserId(this);
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        int rows = dbHelper.updateRoutine(routineId, userId, name, day, selectedTime, goal, notes);
        if (rows > 0) {
            Toast.makeText(this, "Routine updated", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to update routine", Toast.LENGTH_SHORT).show();
        }
    }

    // TextWatcher helper
    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable after;

        SimpleTextWatcher(Runnable after) { this.after = after; }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void afterTextChanged(Editable s) { if (after != null) after.run(); }
    }
}
