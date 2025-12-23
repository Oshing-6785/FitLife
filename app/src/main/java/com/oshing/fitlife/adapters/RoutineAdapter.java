package com.oshing.fitlife.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.imageview.ShapeableImageView;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;

import java.util.ArrayList;
import java.util.List;

public class RoutineAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private final DatabaseHelper db;
    private final int userId;

    private final List<Integer> routineIds = new ArrayList<>();

    public RoutineAdapter(@NonNull Context context,
                          @NonNull DatabaseHelper db,
                          int userId,
                          @NonNull List<Integer> routineIds) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.db = db;
        this.userId = userId;
        this.routineIds.clear();
        this.routineIds.addAll(routineIds);
    }

    public void setRoutineIds(@NonNull List<Integer> newIds) {
        routineIds.clear();
        routineIds.addAll(newIds);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return routineIds.size();
    }

    @Override
    public Integer getItem(int position) {
        return routineIds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return routineIds.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_routine, parent, false);
            h = new ViewHolder(convertView);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        int routineId = routineIds.get(position);

        Cursor c = db.getRoutineById(routineId);

        String name = "";
        String day = "";
        String time = "";
        String goal = "";
        String notes = "";
        int isDone = 0;
        int isDeleted = 0;

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    name = safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_NAME)));
                    day = safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_DAY_OF_WEEK)));
                    time = safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_TIME)));
                    goal = safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_GOAL)));
                    notes = safe(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_NOTES)));

                    try {
                        isDone = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_IS_DONE));
                    } catch (Exception ignored) {
                        isDone = 0;
                    }

                    try {
                        isDeleted = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COL_ROUTINE_IS_DELETED));
                    } catch (Exception ignored) {
                        isDeleted = 0;
                    }
                }
            } finally {
                c.close();
            }
        }


        if (h.ivIcon != null) {
            h.ivIcon.setImageResource(R.drawable.ic_routine);
        }

        // Deleted placeholder
        if (isDeleted == 1) {
            h.tvName.setText("(Deleted routine)");
            h.tvSchedule.setText("Moved to Trash");
            h.tvGoal.setText("Goal: -");
            h.tvNotes.setText("Notes: -");
            h.tvDoneBadge.setVisibility(View.GONE);

            clearDoneStyle(convertView, h);
            fadeRow(convertView, h, true);
            return convertView;
        }

        // Normal row
        h.tvName.setText(name.isEmpty() ? "(Untitled routine)" : name);

        String schedule = buildSchedule(day, time);
        h.tvSchedule.setText(schedule);

        h.tvGoal.setText(goal.isEmpty() ? "Goal: -" : ("Goal: " + goal));
        h.tvNotes.setText(notes.isEmpty() ? "Notes: -" : ("Notes: " + notes));

        boolean done = (isDone == 1);
        h.tvDoneBadge.setVisibility(done ? View.VISIBLE : View.GONE);

        if (done) {
            applyDoneStyle(convertView, h);
        } else {
            clearDoneStyle(convertView, h);
        }

        fadeRow(convertView, h, false);
        return convertView;
    }

    private String buildSchedule(String day, String time) {
        boolean hasDay = day != null && !day.trim().isEmpty();
        boolean hasTime = time != null && !time.trim().isEmpty();

        if (!hasDay && !hasTime) return "Schedule: not set";
        if (hasDay && !hasTime) return day.trim();
        if (!hasDay) return time.trim();
        return (day.trim() + " @ " + time.trim());
    }

    private void applyDoneStyle(View row, ViewHolder h) {
        h.tvName.setPaintFlags(h.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        h.tvName.setAlpha(0.75f);
        h.tvSchedule.setAlpha(0.65f);
        h.tvGoal.setAlpha(0.65f);
        h.tvNotes.setAlpha(0.65f);

        row.setAlpha(0.92f);
    }

    private void clearDoneStyle(View row, ViewHolder h) {
        h.tvName.setPaintFlags(h.tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

        h.tvName.setAlpha(1.0f);
        h.tvSchedule.setAlpha(1.0f);
        h.tvGoal.setAlpha(1.0f);
        h.tvNotes.setAlpha(1.0f);

        row.setAlpha(1.0f);
    }

    private void fadeRow(View row, ViewHolder h, boolean faded) {
        if (faded) {
            h.tvName.setAlpha(0.55f);
            h.tvSchedule.setAlpha(0.50f);
            h.tvGoal.setAlpha(0.50f);
            h.tvNotes.setAlpha(0.50f);
            row.setAlpha(0.85f);
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    static class ViewHolder {
        ShapeableImageView ivIcon;
        TextView tvName;
        TextView tvDoneBadge;
        TextView tvSchedule;
        TextView tvGoal;
        TextView tvNotes;

        ViewHolder(View root) {
            try {
                ivIcon = root.findViewById(R.id.ivRoutineIcon);
            } catch (Exception ignored) {
                ivIcon = null;
            }

            tvName = root.findViewById(R.id.tvRoutineName);
            tvDoneBadge = root.findViewById(R.id.tvDoneBadge);
            tvSchedule = root.findViewById(R.id.tvRoutineSchedule);
            tvGoal = root.findViewById(R.id.tvRoutineGoal);
            tvNotes = root.findViewById(R.id.tvRoutineNotes);
        }
    }
}
