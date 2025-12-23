package com.oshing.fitlife.ui.delegation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oshing.fitlife.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatsListAdapter extends RecyclerView.Adapter<ChatsListAdapter.VH> {

    public interface OnChatClick { void onChatClick(ChatListItem item); }
    public interface OnChatLongClick { void onChatLongClick(ChatListItem item); }

    private final Context context;
    private final OnChatClick click;
    private final OnChatLongClick longClick;

    private final List<ChatListItem> items = new ArrayList<>();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatsListAdapter(@NonNull Context context,
                            @NonNull OnChatClick click,
                            @NonNull OnChatLongClick longClick) {
        this.context = context;
        this.click = click;
        this.longClick = longClick;
    }

    public void submit(List<ChatListItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_chat_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ChatListItem item = items.get(position);

        String name = safe(item.friendName);
        String last = safe(item.lastMessage);

        h.tvName.setText(name.isEmpty() ? "User" : name);
        h.tvLastMessage.setText(last.isEmpty() ? "No messages yet" : last);

        // Avatar initial
        String initial = (name.isEmpty() ? "U" : name.substring(0, 1).toUpperCase(Locale.ROOT));
        h.tvAvatar.setText(initial);

        // Time
        if (item.timestamp > 0) {
            h.tvTime.setText(timeFmt.format(new Date(item.timestamp)));
            h.tvTime.setVisibility(View.VISIBLE);
        } else {
            h.tvTime.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> click.onChatClick(item));
        h.itemView.setOnLongClickListener(v -> {
            longClick.onChatLongClick(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvLastMessage, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
