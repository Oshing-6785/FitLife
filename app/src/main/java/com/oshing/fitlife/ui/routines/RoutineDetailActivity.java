package com.oshing.fitlife.ui.routines;

import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;
import com.oshing.fitlife.utils.SessionManager;

public class RoutineDetailActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "SHAKE_TEST";

    private TextView tvRoutineName, tvRoutineDay, tvRoutineTime, tvRoutineGoal, tvRoutineNotes;
    private DatabaseHelper dbHelper;

    private int routineId = -1;
    private int userId = -1;

    // Sensor
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_detail);

        tvRoutineName = findViewById(R.id.tvRoutineName);
        tvRoutineDay = findViewById(R.id.tvRoutineDay);
        tvRoutineTime = findViewById(R.id.tvRoutineTime);
        tvRoutineGoal = findViewById(R.id.tvRoutineGoal);
        tvRoutineNotes = findViewById(R.id.tvRoutineNotes);

        dbHelper = new DatabaseHelper(this);

        routineId = getIntent().getIntExtra("routine_id", -1);

        // prefer SessionManager extra as fallback
        userId = SessionManager.getUserId(this);
        if (userId == -1) {
            userId = getIntent().getIntExtra("user_id", -1);
        }

        if (routineId == -1) {
            finish();
            return;
        }

        if (isRoutineDeleted(routineId)) {
            Toast.makeText(this, "This routine is in Trash. Restore it first.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadRoutine(routineId);

        // SENSOR SETUP
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager != null
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                : null;

        shakeDetector = new ShakeDetector(this::onShakeRestoreTriggered);

        if (accelerometer == null) {
            Toast.makeText(this, "Accelerometer not available", Toast.LENGTH_LONG).show();
        }

        //  FALLBACK
        tvRoutineName.setOnLongClickListener(v -> {
            Toast.makeText(this, "Restore trigger", Toast.LENGTH_SHORT).show();
            onShakeRestoreTriggered();
            return true;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            shakeDetector.onAccelerometer(
                    event.values[0],
                    event.values[1],
                    event.values[2]
            );
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // LOAD ROUTINE
    private void loadRoutine(int routineId) {
        Cursor cursor = dbHelper.getRoutineById(routineId);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {

                    String routineName = safe(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_NAME)));
                    String routineDay = safe(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_DAY_OF_WEEK)));
                    String routineTime = safe(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_TIME)));
                    String routineGoal = safe(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_GOAL)));
                    String routineNotes = safe(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_NOTES)));

                    tvRoutineName.setText(routineName);
                    tvRoutineDay.setText("Day: " + routineDay);
                    tvRoutineTime.setText("Time: " + routineTime);
                    tvRoutineGoal.setText("Goal: " + routineGoal);
                    tvRoutineNotes.setText("Notes: " + routineNotes);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private boolean isRoutineDeleted(int routineId) {
        Cursor c = dbHelper.getRoutineById(routineId);
        if (c == null) return false;
        try {
            if (!c.moveToFirst()) return false;

            try {
                int isDeleted = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_IS_DELETED));
                return isDeleted == 1;
            } catch (Exception ignored) {
                return false;
            }
        } finally {
            c.close();
        }
    }

    // RESTORE LOGIC
    private void onShakeRestoreTriggered() {
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        int restoredId = dbHelper.restoreMostRecentlyDeletedRoutine(userId);

        if (restoredId == -1) {
            Toast.makeText(this, "Trash is empty", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Restored a routine from Trash ✅", Toast.LENGTH_SHORT).show();

            // optional: go back to list so user sees it immediately
            Intent i = new Intent(this, RoutineListActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
            finish();
        }
    }

    // SHAKE DETECTOR (BEST EFFORT)
    private static class ShakeDetector {

        interface OnShakeListener { void onShake(); }

        private static final float THRESHOLD_G = 1.3f; // device-friendly
        private static final int SLOP_MS = 700;

        private long lastTime = 0;
        private final OnShakeListener listener;

        ShakeDetector(OnShakeListener l) {
            listener = l;
        }

        void onAccelerometer(float x, float y, float z) {

            float gX = x / 9.81f;
            float gY = y / 9.81f;
            float gZ = z / 9.81f;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            Log.d(TAG, "gForce=" + gForce);

            if (gForce > THRESHOLD_G) {
                long now = System.currentTimeMillis();
                if (now - lastTime < SLOP_MS) return;
                lastTime = now;
                listener.onShake();
            }
        }
    }
}
