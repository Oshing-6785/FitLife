package com.oshing.fitlife.ui.checklist;

import android.content.DialogInterface;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.oshing.fitlife.R;
import com.oshing.fitlife.adapters.ChecklistAdapter;
import com.oshing.fitlife.data.DatabaseHelper;
import com.oshing.fitlife.models.ChecklistItem;
import com.oshing.fitlife.utils.ShareUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GlobalChecklistActivity extends AppCompatActivity implements SensorEventListener {

    private DatabaseHelper db;
    private RecyclerView rv;
    private FloatingActionButton fabAdd;

    private TextView tvChecklistCount;
    private com.google.android.material.button.MaterialButton btnClearCompleted;
    private com.google.android.material.button.MaterialButton btnShareHelp;

    private TextInputEditText etSearchChecklist;

    private ChecklistAdapter adapter;

    private int userId = -1;

    private final List<ChecklistItem> allItems = new ArrayList<>();
    private final List<ChecklistItem> visibleItems = new ArrayList<>();

    private String currentQuery = "";

    // SHAKE RESTORE
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
        setContentView(R.layout.activity_global_checklist);

        db = new DatabaseHelper(this);

        userId = getIntent().getIntExtra("user_id", -1);
        if (userId == -1) {
            finish();
            return;
        }

        rv = findViewById(R.id.rvChecklist);
        fabAdd = findViewById(R.id.fabAddChecklist);
        tvChecklistCount = findViewById(R.id.tvChecklistCount);
        btnClearCompleted = findViewById(R.id.btnClearCompleted);
        btnShareHelp = findViewById(R.id.btnShareHelp);
        etSearchChecklist = findViewById(R.id.etSearchChecklist);

        rv.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChecklistAdapter(new ArrayList<>(), new ChecklistAdapter.Listener() {
            @Override
            public void onToggle(ChecklistItem item) {
                db.toggleChecklistItemChecked(item.getId(), userId);
                reloadFromDbThenApplyFilter();
            }

            @Override
            public void onEdit(ChecklistItem item) {
                showEditDialog(item);
            }
        });
        rv.setAdapter(adapter);

        ItemTouchHelper ith = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                return 0.75f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                return defaultValue * 3;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                ChecklistItem item = adapter.getItem(pos);

                if (direction == ItemTouchHelper.RIGHT) {
                    db.toggleChecklistItemChecked(item.getId(), userId);
                    reloadFromDbThenApplyFilter();
                } else {
                    showDeleteConfirm(item);
                }
            }
        });
        ith.attachToRecyclerView(rv);

        fabAdd.setOnClickListener(v -> showAddDialog());
        btnClearCompleted.setOnClickListener(v -> showClearCompletedConfirm());

        // Share/Request Help click
        btnShareHelp.setOnClickListener(v -> handleShareHelpChecklist());

        etSearchChecklist.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = (s == null) ? "" : s.toString();
                applyFilterAndRefresh();
            }
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        reloadFromDbThenApplyFilter();
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
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void handleShakeRestore() {
        int restoredId = db.restoreMostRecentlyDeletedChecklistItem(userId);

        if (restoredId == -1) {
            Toast.makeText(this, "Trash is empty", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Restored checklist item ✅", Toast.LENGTH_SHORT).show();
            reloadFromDbThenApplyFilter();
        }
    }

    //  SHARE / REQUEST HELP
    private void handleShareHelpChecklist() {
        ArrayList<String> pending = db.getUncheckedGlobalChecklistItems(userId);

        if (pending == null || pending.isEmpty()) {
            Toast.makeText(this, "No pending items to share 👍", Toast.LENGTH_SHORT).show();
            return;
        }

        String message = ShareUtils.buildChecklistHelpMessage(pending);

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
        final EditText etPhone = new EditText(this);
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

    private void reloadFromDbThenApplyFilter() {
        allItems.clear();
        allItems.addAll(listFromCursor(db.getChecklistItemsCursor(userId)));
        applyFilterAndRefresh();
    }

    private void applyFilterAndRefresh() {
        visibleItems.clear();

        String q = currentQuery == null ? "" : currentQuery.trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            visibleItems.addAll(allItems);
        } else {
            for (ChecklistItem item : allItems) {
                String name = item.getName() == null ? "" : item.getName();
                if (name.toLowerCase(Locale.ROOT).contains(q)) {
                    visibleItems.add(item);
                }
            }
        }

        adapter.setItems(visibleItems);
        updateFooterCounts(visibleItems);
    }

    private void updateFooterCounts(List<ChecklistItem> list) {
        int total = list.size();
        int completed = 0;
        for (ChecklistItem item : list) {
            if (item.isChecked()) completed++;
        }
        tvChecklistCount.setText("Completed: " + completed + " / " + total);

        btnClearCompleted.setEnabled(completed > 0);
        btnClearCompleted.setAlpha(completed > 0 ? 1.0f : 0.5f);
    }

    private List<ChecklistItem> listFromCursor(Cursor c) {
        List<ChecklistItem> list = new ArrayList<>();
        if (c == null) return list;

        try {
            if (c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_ID));
                    int uid = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_USER_ID));
                    String name = c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_NAME));
                    int checkedInt = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_IS_CHECKED));
                    long createdAt = c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_CREATED_AT));

                    boolean checked = (checkedInt == 1);
                    list.add(new ChecklistItem(id, uid, name, checked, createdAt));

                } while (c.moveToNext());
            }
        } finally {
            c.close();
        }

        return list;
    }

    private void showAddDialog() {
        EditText et = new EditText(this);
        et.setHint("e.g., Water Bottle");

        new AlertDialog.Builder(this)
                .setTitle("Add checklist item")
                .setView(et)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty()) {
                        db.addChecklistItem(userId, name);
                        reloadFromDbThenApplyFilter();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showEditDialog(ChecklistItem item) {
        EditText et = new EditText(this);
        et.setText(item.getName());

        new AlertDialog.Builder(this)
                .setTitle("Edit item")
                .setView(et)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = et.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        db.updateChecklistItemName(item.getId(), userId, newName);
                        reloadFromDbThenApplyFilter();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> reloadFromDbThenApplyFilter())
                .show();
    }

    private void showDeleteConfirm(ChecklistItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Move to Trash?")
                .setMessage(item.getName() + "\n\nShake phone to restore.")
                .setPositiveButton("Move", (dialog, which) -> {
                    db.deleteChecklistItem(item.getId(), userId);
                    reloadFromDbThenApplyFilter();
                })
                .setNegativeButton("Cancel", (dialog, which) -> reloadFromDbThenApplyFilter())
                .setOnCancelListener(DialogInterface::dismiss)
                .show();
    }

    private void showClearCompletedConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Move completed to Trash?")
                .setMessage("This will move all checked items to Trash.\nShake to restore the latest one.")
                .setPositiveButton("Move", (dialog, which) -> {
                    db.clearCompletedChecklistItems(userId);
                    reloadFromDbThenApplyFilter();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
