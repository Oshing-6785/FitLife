package com.oshing.fitlife.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.oshing.fitlife.R;
import com.oshing.fitlife.ui.routines.model.TrashRow;

import java.util.List;

public class TrashAdapter extends ArrayAdapter<TrashRow> {

    private final LayoutInflater inflater;

    public TrashAdapter(@NonNull Context context, @NonNull List<TrashRow> rows) {
        super(context, 0, rows);
        inflater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = convertView;
        if (v == null) v = inflater.inflate(R.layout.row_trash_item, parent, false);

        TrashRow row = getItem(position);
        if (row == null) return v;

        ImageView iv = v.findViewById(R.id.ivTypeIcon);
        TextView tvTitle = v.findViewById(R.id.tvTitle);
        TextView tvSubtitle = v.findViewById(R.id.tvSubtitle);

        tvTitle.setText(safe(row.title));
        tvSubtitle.setText(safe(row.subtitle));
        tvSubtitle.setVisibility(isEmpty(row.subtitle) ? View.GONE : View.VISIBLE);

        // Use built in icons
        if (row.type == TrashRow.TYPE_ROUTINE) {
            iv.setImageResource(android.R.drawable.ic_menu_my_calendar);
        } else if (row.type == TrashRow.TYPE_EXERCISE) {
            iv.setImageResource(android.R.drawable.ic_menu_edit);
        } else {
            iv.setImageResource(android.R.drawable.ic_menu_agenda);
        }

        return v;
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
