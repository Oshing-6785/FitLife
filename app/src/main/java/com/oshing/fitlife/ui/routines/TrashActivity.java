package com.oshing.fitlife.ui.routines;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.ChipGroup;
import com.oshing.fitlife.R;
import com.oshing.fitlife.adapters.TrashAdapter;
import com.oshing.fitlife.data.DatabaseHelper;
import com.oshing.fitlife.ui.routines.model.TrashRow;
import com.oshing.fitlife.utils.SessionManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrashActivity extends AppCompatActivity {

    private static final int FILTER_ALL = 0;

    private ListView lvTrash;
    private LinearLayout emptyContainerTrash;

    private ChipGroup chipGroupTrash;

    private DatabaseHelper db;

    private final List<TrashRow> allRows = new ArrayList<>();
    private final List<TrashRow> filteredRows = new ArrayList<>();
    private TrashAdapter adapter;

    private int currentFilter = FILTER_ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        lvTrash = findViewById(R.id.lvTrash);
        emptyContainerTrash = findViewById(R.id.emptyContainerTrash);
        chipGroupTrash = findViewById(R.id.chipGroupTrash);

        db = new DatabaseHelper(this);

        adapter = new TrashAdapter(this, filteredRows);
        lvTrash.setAdapter(adapter);

        lvTrash.setOnItemClickListener((p, v, pos, id) -> {
            TrashRow row = filteredRows.get(pos);
            restoreItem(row);
        });

        lvTrash.setOnItemLongClickListener((p, v, pos, id) -> {
            TrashRow row = filteredRows.get(pos);
            confirmPermanentDelete(row);
            return true;
        });

        chipGroupTrash.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipTrashAll) currentFilter = FILTER_ALL;
            else if (checkedId == R.id.chipTrashRoutines) currentFilter = TrashRow.TYPE_ROUTINE;
            else if (checkedId == R.id.chipTrashExercises) currentFilter = TrashRow.TYPE_EXERCISE;
            else if (checkedId == R.id.chipTrashChecklist) currentFilter = TrashRow.TYPE_CHECKLIST;

            applyFilter();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadTrash();
    }

    private void reloadTrash() {
        int userId = SessionManager.getUserId(this);
        if (userId == -1) {
            Toast.makeText(this, "Session expired. Login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        allRows.clear();

        // 1) Deleted routines
        Cursor cr = null;
        try {
            cr = db.getDeletedRoutinesCursor(userId);
            if (cr != null && cr.moveToFirst()) {
                do {
                    int rid = cr.getInt(cr.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_ID));
                    String name = safe(cr.getString(cr.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_NAME)));
                    String day = safe(cr.getString(cr.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_DAY_OF_WEEK)));
                    String time = safe(cr.getString(cr.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_TIME)));
                    String goal = safe(cr.getString(cr.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_GOAL)));

                    long deletedAt = 0L;
                    try { deletedAt = cr.getLong(cr.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_DELETED_AT)); } catch (Exception ignored) {}

                    String subtitle = day + " @ " + time + "\nGoal: " + goal;

                    allRows.add(new TrashRow(
                            TrashRow.TYPE_ROUTINE,
                            rid,
                            name,
                            subtitle,
                            deletedAt
                    ));
                } while (cr.moveToNext());
            }
        } finally {
            if (cr != null) cr.close();
        }

        // 2.Deleted exercises
        Cursor ce = null;
        try {
            ce = db.getDeletedExercisesCursorForUser(userId);
            if (ce != null && ce.moveToFirst()) {
                do {
                    int exId = ce.getInt(ce.getColumnIndexOrThrow(DatabaseHelper.COL_EX_ID));
                    String exName = safe(ce.getString(ce.getColumnIndexOrThrow(DatabaseHelper.COL_EX_NAME)));
                    String routineName = safe(ce.getString(ce.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_NAME)));

                    long deletedAt = 0L;
                    try { deletedAt = ce.getLong(ce.getColumnIndexOrThrow(DatabaseHelper.COL_EX_DELETED_AT)); } catch (Exception ignored) {}

                    allRows.add(new TrashRow(
                            TrashRow.TYPE_EXERCISE,
                            exId,
                            exName,
                            "Routine: " + routineName,
                            deletedAt
                    ));
                } while (ce.moveToNext());
            }
        } finally {
            if (ce != null) ce.close();
        }

        // 3. Deleted checklist items
        Cursor cc = null;
        try {
            cc = db.getDeletedChecklistCursorForUser(userId);
            if (cc != null && cc.moveToFirst()) {
                do {
                    int cid = cc.getInt(cc.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_ID));
                    String name = safe(cc.getString(cc.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_NAME)));

                    long deletedAt = 0L;
                    try { deletedAt = cc.getLong(cc.getColumnIndexOrThrow(DatabaseHelper.COL_CHECK_DELETED_AT)); } catch (Exception ignored) {}

                    allRows.add(new TrashRow(
                            TrashRow.TYPE_CHECKLIST,
                            cid,
                            name,
                            "Checklist item",
                            deletedAt
                    ));
                } while (cc.moveToNext());
            }
        } finally {
            if (cc != null) cc.close();
        }

        // newest first
        Collections.sort(allRows, (a, b) -> Long.compare(b.deletedAt, a.deletedAt));

        applyFilter();
    }

    private void applyFilter() {
        filteredRows.clear();

        for (TrashRow r : allRows) {
            if (currentFilter == FILTER_ALL || r.type == currentFilter) {
                filteredRows.add(r);
            }
        }

        adapter.notifyDataSetChanged();

        boolean empty = filteredRows.isEmpty();
        emptyContainerTrash.setVisibility(empty ? View.VISIBLE : View.GONE);
        lvTrash.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void restoreItem(TrashRow row) {
        int userId = SessionManager.getUserId(this);
        if (userId == -1) return;

        boolean ok = false;

        if (row.type == TrashRow.TYPE_ROUTINE) {
            ok = db.restoreRoutineById(row.id, userId);
        } else if (row.type == TrashRow.TYPE_EXERCISE) {
            ok = db.restoreExerciseForUser(row.id, userId);
        } else if (row.type == TrashRow.TYPE_CHECKLIST) {
            ok = db.restoreChecklistItem(row.id, userId) > 0;
        }

        Toast.makeText(this, ok ? "Restored ✅" : "Restore failed", Toast.LENGTH_SHORT).show();
        if (ok) reloadTrash();
    }

    private void confirmPermanentDelete(TrashRow row) {
        new AlertDialog.Builder(this)
                .setTitle("Delete permanently?")
                .setMessage("This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    int userId = SessionManager.getUserId(this);
                    if (userId == -1) return;

                    boolean ok = false;

                    if (row.type == TrashRow.TYPE_ROUTINE) {
                        ok = db.hardDeleteRoutine(row.id, userId);
                    } else if (row.type == TrashRow.TYPE_EXERCISE) {
                        ok = db.hardDeleteExerciseForUser(row.id, userId);
                    } else if (row.type == TrashRow.TYPE_CHECKLIST) {
                        ok = db.hardDeleteChecklistItem(row.id, userId) > 0;
                    }

                    Toast.makeText(this, ok ? "Deleted permanently" : "Delete failed", Toast.LENGTH_SHORT).show();
                    if (ok) reloadTrash();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
