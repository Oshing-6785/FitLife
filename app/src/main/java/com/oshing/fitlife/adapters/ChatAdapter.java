package com.oshing.fitlife.ui.delegation;

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

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    private final String myUid;
    private final String friendName;

    private final List<ChatMessage> messages = new ArrayList<>();
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(@NonNull String myUid, String friendName) {
        this.myUid = myUid;
        this.friendName = (friendName == null || friendName.trim().isEmpty()) ? "Friend" : friendName.trim();
    }

    public void setMessages(List<ChatMessage> list) {
        messages.clear();
        if (list != null) messages.addAll(list);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage m = messages.get(position);
        String from = (m == null) ? null : m.fromUid;
        return myUid.equals(from) ? TYPE_ME : TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            View v = inf.inflate(R.layout.item_chat_me, parent, false);
            return new MeVH(v);
        } else {
            View v = inf.inflate(R.layout.item_chat_other, parent, false);
            return new OtherVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int pos) {
        ChatMessage m = messages.get(pos);

        String text = (m == null || m.text == null) ? "" : m.text;
        String type = (m == null || m.type == null) ? "TEXT" : m.type;
        String time = formatTime((m == null) ? 0L : m.timestamp);

        boolean isChecklist = "CHECKLIST".equalsIgnoreCase(type);

        if (h instanceof MeVH) {
            MeVH vh = (MeVH) h;
            vh.tvMessage.setText(text);
            vh.tvTime.setText(time);
            if (vh.tvType != null) {
                vh.tvType.setVisibility(isChecklist ? View.VISIBLE : View.GONE);
                vh.tvType.setText("CHECKLIST");
            }
        } else {
            OtherVH vh = (OtherVH) h;
            vh.tvSender.setText(friendName);
            vh.tvMessage.setText(text);
            vh.tvTime.setText(time);
            if (vh.tvType != null) {
                vh.tvType.setVisibility(isChecklist ? View.VISIBLE : View.GONE);
                vh.tvType.setText("CHECKLIST");
            }
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MeVH extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvType;
        MeVH(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
            tvType = v.findViewById(R.id.tvType);
        }
    }

    static class OtherVH extends RecyclerView.ViewHolder {
        TextView tvSender, tvMessage, tvTime, tvType;
        OtherVH(View v) {
            super(v);
            tvSender = v.findViewById(R.id.tvSender);
            tvMessage = v.findViewById(R.id.tvMessage);
            tvTime = v.findViewById(R.id.tvTime);
            tvType = v.findViewById(R.id.tvType);
        }
    }

    private String formatTime(long ts) {
        if (ts <= 0) return "";
        return timeFmt.format(new Date(ts));
    }
}
