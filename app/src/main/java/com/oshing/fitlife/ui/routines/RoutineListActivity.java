package com.oshing.fitlife.ui.routines;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.oshing.fitlife.R;
import com.oshing.fitlife.adapters.RoutineAdapter;
import com.oshing.fitlife.data.DatabaseHelper;
import com.oshing.fitlife.utils.SessionManager;
import com.oshing.fitlife.utils.ShareUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RoutineListActivity extends AppCompatActivity implements SensorEventListener {

    private MaterialToolbar toolbar;

    private MaterialButton btnAddRoutine;
    private ListView lvRoutines;
    private TextView tvEmptyState;
    private TextView tvHint;
    private TextInputEditText etRoutineSearch;

    private TextView tvRoutineCount;
    private MaterialButton btnShareRoutineHelp;

    private MaterialButtonToggleGroup toggleRoutineFilter;

    private DatabaseHelper dbHelper;
    private RoutineAdapter routineAdapter;

    private final List<Integer> allRoutineIds = new ArrayList<>();
    private final List<Integer> visibleRoutineIds = new ArrayList<>();

    private GestureDetector gestureDetector;

    private String currentQuery = "";
    private FilterMode currentFilter = FilterMode.ALL;

    private enum FilterMode { TODAY, UPCOMING, ALL }

    //  SHAKE DETECTION
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float lastAccel = SensorManager.GRAVITY_EARTH;
    private float currentAccel = SensorManager.GRAVITY_EARTH;
    private float accel = 0.0f;

    private long lastShakeTimeMs = 0L;

    private static final float SHAKE_THRESHOLD = 12.0f;
    private static final long SHAKE_COOLDOWN_MS = 1200L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_list);

        if (!SessionManager.isSessionValid(this)) {
            finish();
            return;
        }

        dbHelper = new DatabaseHelper(this);

        toolbar = findViewById(R.id.toolbar);

        btnAddRoutine = findViewById(R.id.btnAddRoutine);
        lvRoutines = findViewById(R.id.lvRoutines);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvHint = findViewById(R.id.tvHint);
        etRoutineSearch = findViewById(R.id.etRoutineSearch);
        tvRoutineCount = findViewById(R.id.tvRoutineCount);
        toggleRoutineFilter = findViewById(R.id.toggleRoutineFilter);

        btnShareRoutineHelp = findViewById(R.id.btnShareRoutineHelp);

        // toolbar back
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        btnAddRoutine.setOnClickListener(v ->
                startActivity(new Intent(this, AddRoutineActivity.class))
        );

        btnShareRoutineHelp.setOnClickListener(v -> shareUnfinishedRoutines());

        setupSwipeGesture();
        setupSearch();
        setupFilterButtons();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadFromDbThenApplyFilters();

        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) sensorManager.unregisterListener(this);
    }

    //  SHARE HELP
    private void shareUnfinishedRoutines() {
        int userId = SessionManager.getUserId(this);

        List<String> displayList = dbHelper.getRoutineDisplayListForUserSorted(userId);

        ArrayList<String> pending = new ArrayList<>();
        for (String s : displayList) {
            if (s == null) continue;
            if (!s.startsWith("✅")) pending.add(s.trim());
        }

        if (pending.isEmpty()) {
            Toast.makeText(this, "No pending routines to share 👍", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = ShareUtils.buildRoutineHelpMessage(pending);

        new AlertDialog.Builder(this)
                .setTitle("Request help")
                .setItems(new String[]{"Send via any app", "Send via SMS"}, (dialog, which) -> {
                    if (which == 0) {
                        ShareUtils.shareToAnyApp(this, message);
                    } else {
                        showSmsPhoneDialogAndSend(message);
                    }
                })
                .show();
    }

    private void showSmsPhoneDialogAndSend(String message) {
        final android.widget.EditText etPhone = new android.widget.EditText(this);
        etPhone.setHint("Enter phone number (e.g., 98XXXXXXXX)");

        new AlertDialog.Builder(this)
                .setTitle("Send via SMS")
                .setMessage("Enter receiver phone number:")
                .setView(etPhone)
                .setPositiveButton("Send", (d, w) -> {
                    String phone = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();
                    if (phone.isEmpty()) {
                        Toast.makeText(this, "Phone number required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    ShareUtils.shareViaSms(this, phone, message);
                })
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .show();
    }

    //  SHAKE CALLBACKS
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null) return;
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        lastAccel = currentAccel;
        currentAccel = (float) Math.sqrt(x * x + y * y + z * z);
        float delta = currentAccel - lastAccel;
        accel = accel * 0.9f + delta;

        long now = System.currentTimeMillis();
        if (accel > SHAKE_THRESHOLD && (now - lastShakeTimeMs) > SHAKE_COOLDOWN_MS) {
            lastShakeTimeMs = now;
            handleShakeRestore();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void handleShakeRestore() {
        int userId = SessionManager.getUserId(this);

        int restoredId = dbHelper.restoreMostRecentlyDeletedRoutine(userId);

        if (restoredId == -1) {
            Toast.makeText(this, "Trash is empty", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Restored a routine from Trash ✅", Toast.LENGTH_SHORT).show();
            reloadFromDbThenApplyFilters();
        }
    }

    //  SEARCH + FILTERS
    private void setupSearch() {
        etRoutineSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s == null ? "" : s.toString();
                applyFilters();
            }
        });
    }

    private void setupFilterButtons() {
        toggleRoutineFilter.check(R.id.btnFilterAll);
        currentFilter = FilterMode.ALL;

        toggleRoutineFilter.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            if (checkedId == R.id.btnFilterToday) currentFilter = FilterMode.TODAY;
            else if (checkedId == R.id.btnFilterUpcoming) currentFilter = FilterMode.UPCOMING;
            else currentFilter = FilterMode.ALL;

            applyFilters();
        });
    }

    private void reloadFromDbThenApplyFilters() {
        int userId = SessionManager.getUserId(this);

        allRoutineIds.clear();
        allRoutineIds.addAll(dbHelper.getRoutineIdsForUserSorted(userId));

        applyFilters();
    }

    private void applyFilters() {
        visibleRoutineIds.clear();

        int userId = SessionManager.getUserId(this);
        List<Integer> sortedIds = allRoutineIds;
        List<String> displayList = dbHelper.getRoutineDisplayListForUserSorted(userId);

        String q = currentQuery.trim().toLowerCase(Locale.ROOT);
        int todayIdx = todayIndex();

        for (int i = 0; i < sortedIds.size(); i++) {
            int routineId = sortedIds.get(i);

            String display = (i < displayList.size() && displayList.get(i) != null) ? displayList.get(i) : "";

            boolean matchesSearch = q.isEmpty() || display.toLowerCase(Locale.ROOT).contains(q);
            if (!matchesSearch) continue;

            int dayIdx = extractDayIndexFromDisplay(display);
            boolean matchesFilter = matchesFilterMode(dayIdx, todayIdx);

            if (matchesFilter) visibleRoutineIds.add(routineId);
        }

        updateList();
        updateCounter(displayList, sortedIds, visibleRoutineIds);
    }

    private boolean matchesFilterMode(int dayIdx, int todayIdx) {
        if (currentFilter == FilterMode.ALL) return true;

        int diff = (dayIdx - todayIdx + 7) % 7;

        if (currentFilter == FilterMode.TODAY) return diff == 0;
        if (currentFilter == FilterMode.UPCOMING) return diff >= 1;
        return true;
    }

    //  LIST + COUNTER
    private void updateList() {
        int userId = SessionManager.getUserId(this);

        if (visibleRoutineIds.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            lvRoutines.setVisibility(View.GONE);
            if (routineAdapter != null) routineAdapter.setRoutineIds(new ArrayList<>());
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        lvRoutines.setVisibility(View.VISIBLE);

        if (routineAdapter == null) {
            routineAdapter = new RoutineAdapter(this, dbHelper, userId, visibleRoutineIds);
            lvRoutines.setAdapter(routineAdapter);
        } else {
            routineAdapter.setRoutineIds(visibleRoutineIds);
        }

        lvRoutines.setOnItemClickListener((p, v, pos, id) ->
                openEditRoutine(visibleRoutineIds.get(pos))
        );

        lvRoutines.setOnItemLongClickListener((p, v, pos, id) -> {
            showRoutineOptionsDialog(visibleRoutineIds.get(pos));
            return true;
        });

        lvRoutines.setOnTouchListener((v, e) -> gestureDetector.onTouchEvent(e));
    }

    private void updateCounter(List<String> displayList, List<Integer> sortedIds, List<Integer> visibleIds) {
        int totalVisible = visibleIds.size();
        int completedVisible = 0;

        for (int i = 0; i < sortedIds.size(); i++) {
            int rid = sortedIds.get(i);
            if (!visibleIds.contains(rid)) continue;

            String display = (i < displayList.size() ? displayList.get(i) : null);
            if (display != null && display.startsWith("✅")) completedVisible++;
        }

        tvRoutineCount.setText("Completed: " + completedVisible + " / " + totalVisible);
    }

    //  DAY PARSING HELPERS
    private int extractDayIndexFromDisplay(String display) {
        try {
            int open = display.indexOf('(');
            int close = display.indexOf(')');
            if (open == -1 || close == -1 || close <= open) return 0;

            String inside = display.substring(open + 1, close); // "Mon @ 07:30"
            String[] parts = inside.split("@");
            if (parts.length == 0) return 0;

            String day = parts[0].trim();
            return dayToIndex(day);
        } catch (Exception e) {
            return 0;
        }
    }

    private int todayIndex() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
    }

    private int dayToIndex(String day) {
        if (day == null) return 0;
        String d = day.toLowerCase(Locale.ROOT);
        if (d.startsWith("sun")) return 0;
        if (d.startsWith("mon")) return 1;
        if (d.startsWith("tue")) return 2;
        if (d.startsWith("wed")) return 3;
        if (d.startsWith("thu")) return 4;
        if (d.startsWith("fri")) return 5;
        if (d.startsWith("sat")) return 6;
        return 0;
    }

    //  SWIPE
    private void setupSwipeGesture() {
        gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    private static final int SWIPE_THRESHOLD = 140;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 250;

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {

                        if (e1 == null || e2 == null) return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        if (Math.abs(diffX) > Math.abs(diffY)
                                && Math.abs(diffX) > SWIPE_THRESHOLD
                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                            int pos = lvRoutines.pointToPosition((int) e1.getX(), (int) e1.getY());
                            if (pos == ListView.INVALID_POSITION) return false;

                            int routineId = visibleRoutineIds.get(pos);

                            if (diffX < 0) confirmDeleteRoutine(routineId);
                            else toggleRoutineDone(routineId);

                            return true;
                        }
                        return false;
                    }
                });
    }

    //  ACTIONS
    private void showRoutineOptionsDialog(int routineId) {
        String[] options = {"Edit", "Move to Trash", "Cancel"};

        new AlertDialog.Builder(this)
                .setTitle("Routine options")
                .setItems(options, (d, w) -> {
                    if (w == 0) openEditRoutine(routineId);
                    else if (w == 1) confirmDeleteRoutine(routineId);
                })
                .show();
    }

    private void confirmDeleteRoutine(int routineId) {
        new AlertDialog.Builder(this)
                .setTitle("Move routine to Trash?")
                .setMessage("You can restore it later by shaking your phone.")
                .setPositiveButton("Move to Trash", (d, w) -> {
                    dbHelper.deleteRoutine(routineId);
                    reloadFromDbThenApplyFilters();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleRoutineDone(int routineId) {
        int userId = SessionManager.getUserId(this);
        boolean done = dbHelper.toggleRoutineDone(routineId, userId);

        Toast.makeText(this,
                done ? "Marked as done ✅" : "Marked as undone ❌",
                Toast.LENGTH_SHORT).show();

        reloadFromDbThenApplyFilters();
    }

    private void openEditRoutine(int routineId) {
        Intent i = new Intent(this, AddRoutineActivity.class);
        i.putExtra("is_edit", true);
        i.putExtra("routine_id", routineId);
        startActivity(i);
    }
}
