package com.oshing.fitlife.ui.exercises;

import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;

public class ExerciseListActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private int routineId;

    private ListView lvExercises;
    private View btnAddExercise;

    private MaterialToolbar toolbar;
    private View emptyContainer;
    private TextInputEditText etSearch;

    private ExerciseAdapter adapter;
    private String currentQuery = "";

    //  SHAKE TO RESTORE
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener shakeListener;

    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 700;
    private long lastShakeTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise_list);

        db = new DatabaseHelper(this);

        routineId = getIntent().getIntExtra("routine_id", -1);
        if (routineId == -1) {
            Toast.makeText(this, "Routine not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        toolbar = findViewById(R.id.toolbarExercises);
        emptyContainer = findViewById(R.id.emptyContainer);
        etSearch = findViewById(R.id.etExerciseSearch);

        if (toolbar != null) {
            toolbar.setTitle("Exercises");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        lvExercises = findViewById(R.id.lvExercises);
        btnAddExercise = findViewById(R.id.btnAddExercise);

        btnAddExercise.setOnClickListener(v -> {
            Intent i = new Intent(this, AddEditExerciseActivity.class);
            i.putExtra("routine_id", routineId);
            i.putExtra("is_edit", false);
            startActivity(i);
        });

        // Long press for options
        lvExercises.setOnItemLongClickListener((parent, view, position, id) -> {
            int exerciseId = (int) id;
            showExerciseOptions(exerciseId);
            return true;
        });

        // Swipe detector: RIGHT = DONE, LEFT = TRASH
        SwipeGestureDetector swipeDetector = new SwipeGestureDetector(
                this,
                new SwipeGestureDetector.OnSwipeListener() {

                    // LEFT swipe
                    @Override
                    public void onSwipeLeft(MotionEvent e2) {
                        int exerciseId = getExerciseIdUnderFinger(e2);
                        if (exerciseId == -1) return;

                        int rows = db.deleteExercise(exerciseId); // soft delete
                        reload();

                        if (rows > 0) {
                            Snackbar.make(lvExercises, "Moved to Trash 🗑️", Snackbar.LENGTH_LONG)
                                    .setAction("UNDO", vv -> {
                                        db.restoreExercise(exerciseId);
                                        reload();
                                    })
                                    .show();
                        }
                    }

                    // RIGHT swipe
                    @Override
                    public void onSwipeRight(MotionEvent e2) {
                        int exerciseId = getExerciseIdUnderFinger(e2);
                        if (exerciseId == -1) return;

                        boolean nowDone = db.toggleExerciseCompleted(exerciseId);
                        Toast.makeText(
                                ExerciseListActivity.this,
                                nowDone ? "Completed ✅" : "Marked incomplete ❌",
                                Toast.LENGTH_SHORT
                        ).show();
                        reload();
                    }
                }
        );

        // Forward ListView
        lvExercises.setOnTouchListener((v, event) -> {
            swipeDetector.onTouch(v, event);
            return false;
        });

        // Search filtering
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentQuery = (s == null) ? "" : s.toString();
                    reload();
                }
            });
        }

        // Init shake detector
        setupShakeToRestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();

        // register shake listener
        if (sensorManager != null && accelerometer != null && shakeListener != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // unregister shake listener
        if (sensorManager != null && shakeListener != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    private void setupShakeToRestore() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager == null) return;

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) return;

        shakeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event == null || event.values == null || event.values.length < 3) return;

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                float gX = x / SensorManager.GRAVITY_EARTH;
                float gY = y / SensorManager.GRAVITY_EARTH;
                float gZ = z / SensorManager.GRAVITY_EARTH;

                // gForce
                float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    long now = System.currentTimeMillis();
                    if (lastShakeTime + SHAKE_SLOP_TIME_MS > now) return;
                    lastShakeTime = now;

                    restoreMostRecentFromTrash();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // not needed
            }
        };
    }

    private void restoreMostRecentFromTrash() {
        int restoredId = db.restoreMostRecentlyDeletedExerciseForRoutine(routineId);

        if (restoredId != -1) {
            reload();
            Snackbar.make(lvExercises, "Restored from Trash ✅", Snackbar.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Trash is empty (nothing to restore)", Toast.LENGTH_SHORT).show();
        }
    }

    private int getExerciseIdUnderFinger(MotionEvent e2) {
        if (lvExercises == null || adapter == null || e2 == null) return -1;

        int pos = lvExercises.pointToPosition((int) e2.getX(), (int) e2.getY());
        if (pos == ListView.INVALID_POSITION) return -1;

        return (int) adapter.getItemId(pos);
    }

    private void reload() {
        Cursor c;

        String q = (currentQuery == null) ? "" : currentQuery.trim();
        if (q.isEmpty()) {
            c = db.getExercisesByRoutineCursor(routineId);
        } else {
            c = db.getExercisesByRoutineCursorSearch(routineId, q); // filters deleted
        }

        if (adapter == null) {
            adapter = new ExerciseAdapter(this, c);
            lvExercises.setAdapter(adapter);
        } else {
            adapter.swapCursor(c);
        }

        boolean empty = (c == null || c.getCount() == 0);
        if (emptyContainer != null) emptyContainer.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvExercises.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showExerciseOptions(int exerciseId) {
        String[] options = {"Edit", "Move to Trash", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Exercise options")
                .setItems(options, (d, which) -> {
                    if (which == 0) openEdit(exerciseId);
                    else if (which == 1) confirmDelete(exerciseId);
                })
                .show();
    }

    private void openEdit(int exerciseId) {
        Intent i = new Intent(this, AddEditExerciseActivity.class);
        i.putExtra("routine_id", routineId);
        i.putExtra("exercise_id", exerciseId);
        i.putExtra("is_edit", true);
        startActivity(i);
    }

    private void confirmDelete(int exerciseId) {
        new AlertDialog.Builder(this)
                .setTitle("Move to Trash?")
                .setMessage("You can restore it using UNDO or Shake.")
                .setPositiveButton("Move", (d, w) -> {
                    int rows = db.deleteExercise(exerciseId); // soft delete
                    reload();

                    if (rows > 0) {
                        Snackbar.make(lvExercises, "Moved to Trash 🗑️", Snackbar.LENGTH_LONG)
                                .setAction("UNDO", v -> {
                                    db.restoreExercise(exerciseId);
                                    reload();
                                })
                                .show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (adapter != null) adapter.close();
    }

}
