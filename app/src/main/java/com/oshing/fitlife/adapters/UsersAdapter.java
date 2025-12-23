package com.oshing.fitlife.ui.delegation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oshing.fitlife.R;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

    public interface OnAddFriendClick {
        void onAddFriend(UserModel user);
    }

    private final List<UserModel> users;
    private final String myUid;
    private final OnAddFriendClick listener;

    // Trackers
    private final Set<String> requestedUids = new HashSet<>();

    public UsersAdapter(List<UserModel> users, String myUid, OnAddFriendClick listener) {
        this.users = users;
        this.myUid = myUid;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        UserModel u = users.get(position);

        String uid = (u.getUid() == null) ? "" : u.getUid();

        h.tvName.setText(u.getName() == null ? "User" : u.getName());
        h.tvEmail.setText(u.getEmail() == null ? "" : u.getEmail());

        boolean isMe = !uid.isEmpty() && uid.equals(myUid);
        boolean alreadyRequested = !uid.isEmpty() && requestedUids.contains(uid);

        if (isMe) {
            h.btnAdd.setEnabled(false);
            h.btnAdd.setText("Me");
        } else if (alreadyRequested) {
            h.btnAdd.setEnabled(false);
            h.btnAdd.setText("Requested");
        } else {
            h.btnAdd.setEnabled(true);
            h.btnAdd.setText("Add");
            h.btnAdd.setOnClickListener(v -> {
                if (listener != null) {
                    // prevent double taps
                    requestedUids.add(uid);
                    notifyItemChanged(h.getAdapterPosition());
                    listener.onAddFriend(u);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        Button btnAdd;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            btnAdd = itemView.findViewById(R.id.btnAddFriend);
        }
    }
}
