package com.oshing.fitlife.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oshing.fitlife.R;
import com.oshing.fitlife.models.ChecklistItem;

import java.util.ArrayList;
import java.util.List;

public class ChecklistAdapter extends RecyclerView.Adapter<ChecklistAdapter.VH> {

    public interface Listener {
        void onToggle(ChecklistItem item);
        void onEdit(ChecklistItem item);
    }

    private final Listener listener;
    private final List<ChecklistItem> items = new ArrayList<>();

    public ChecklistAdapter(List<ChecklistItem> initialItems, Listener listener) {
        if (initialItems != null) items.addAll(initialItems);
        this.listener = listener;
    }

    public void setItems(List<ChecklistItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public ChecklistItem getItem(int position) {
        return items.get(position);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_checklist, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChecklistItem item = items.get(position);

        h.tvName.setText(item.getName());
        h.cbChecked.setOnCheckedChangeListener(null);
        h.cbChecked.setChecked(item.isChecked());

        applyCheckedStyle(h.itemView, h.tvName, item.isChecked());

        h.cbChecked.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // handles DB update
            listener.onToggle(item);
        });

        h.itemView.setOnClickListener(v -> listener.onEdit(item));
    }

    private void applyCheckedStyle(View row, TextView tv, boolean checked) {
        if (checked) {
            tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            tv.setAlpha(0.65f);
            row.setAlpha(0.75f);
        } else {
            tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            tv.setAlpha(1.0f);
            row.setAlpha(1.0f);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbChecked;
        TextView tvName;

        VH(@NonNull View itemView) {
            super(itemView);
            cbChecked = itemView.findViewById(R.id.cbChecklistChecked);
            tvName = itemView.findViewById(R.id.tvChecklistName);
        }
    }
}
