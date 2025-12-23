package com.oshing.fitlife.ui.exercises;

import android.database.Cursor;
import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.imageview.ShapeableImageView;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;

public class ExerciseAdapter extends BaseAdapter {

    private Cursor cursor;
    private final LayoutInflater inflater;

    public ExerciseAdapter(@NonNull android.content.Context context, Cursor cursor) {
        this.inflater = LayoutInflater.from(context);
        this.cursor = cursor;
    }

    public void swapCursor(Cursor newCursor) {
        if (cursor == newCursor) return;
        cursor = newCursor;
        notifyDataSetChanged();
    }

    public void close() {
        if (cursor != null && !cursor.isClosed()) cursor.close();
        cursor = null;
    }

    @Override
    public int getCount() {
        return (cursor == null || cursor.isClosed()) ? 0 : cursor.getCount();
    }

    @Override
    public Object getItem(int position) {
        if (cursor == null || cursor.isClosed()) return null;
        if (!cursor.moveToPosition(position)) return null;
        return cursor;
    }

    @Override
    public long getItemId(int position) {
        if (cursor == null || cursor.isClosed()) return 0;
        if (!cursor.moveToPosition(position)) return 0;
        return cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EX_ID));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_exercise, parent, false);
            h = new ViewHolder(convertView);
            convertView.setTag(h);
        } else {
            h = (ViewHolder) convertView.getTag();
        }

        if (cursor == null || cursor.isClosed()) return convertView;
        if (!cursor.moveToPosition(position)) return convertView;

        String name = safe(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EX_NAME)));

        int sets = 0;
        int reps = 0;
        try { sets = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EX_SETS)); } catch (Exception ignored) {}
        try { reps = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EX_REPS)); } catch (Exception ignored) {}

        int done = 0;
        try { done = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EX_IS_COMPLETED)); }
        catch (Exception ignored) { done = 0; }

        String imageUri = "";
        try { imageUri = safe(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_EX_IMAGE_URI))); }
        catch (Exception ignored) { imageUri = ""; }

        // bind text
        h.tvName.setText(name);
        h.tvMeta.setText("Sets: " + sets + "   Reps: " + reps);

        if (done == 1) {
            h.tvDone.setVisibility(View.VISIBLE);
            h.tvName.setPaintFlags(h.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvName.setAlpha(0.75f);
            h.tvMeta.setAlpha(0.65f);
            convertView.setAlpha(0.90f);
        } else {
            h.tvDone.setVisibility(View.GONE);
            h.tvName.setPaintFlags(h.tvName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            h.tvName.setAlpha(1f);
            h.tvMeta.setAlpha(0.85f);
            convertView.setAlpha(1f);
        }

        //reset thumbnail
        h.ivThumb.setImageResource(R.drawable.ic_image_placeholder);

        if (!imageUri.trim().isEmpty()) {
            try {
                Uri uri = Uri.parse(imageUri);
                h.ivThumb.setImageURI(uri);

                // If load fails
                if (h.ivThumb.getDrawable() == null) {
                    h.ivThumb.setImageResource(R.drawable.ic_image_placeholder);
                }
            } catch (Exception ignored) {
                h.ivThumb.setImageResource(R.drawable.ic_image_placeholder);
            }
        }

        return convertView;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    static class ViewHolder {
        ShapeableImageView ivThumb;
        TextView tvName, tvMeta, tvDone;

        ViewHolder(View root) {
            ivThumb = root.findViewById(R.id.ivExerciseThumb);
            tvName = root.findViewById(R.id.tvExerciseName);
            tvMeta = root.findViewById(R.id.tvExerciseMeta);
            tvDone = root.findViewById(R.id.tvExerciseDoneBadge);
        }
    }
}
