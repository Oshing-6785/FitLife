package com.oshing.fitlife.ui.delegation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oshing.fitlife.R;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.VH> {

    public interface Click {
        void onClick(FriendModel f);
    }

    private final List<FriendModel> friends;
    private final Click click;

    public FriendsAdapter(List<FriendModel> friends, Click click) {
        this.friends = friends;
        this.click = click;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        FriendModel f = friends.get(pos);

        String name = (f.getName() == null || f.getName().trim().isEmpty())
                ? "User"
                : f.getName().trim();

        String email = (f.getEmail() == null) ? "" : f.getEmail().trim();

        h.tvName.setText(name);
        h.tvEmail.setText(email);

        // Avatar initial
        String initial = name.length() > 0
                ? String.valueOf(Character.toUpperCase(name.charAt(0)))
                : "U";
        h.tvAvatar.setText(initial);

        h.itemView.setOnClickListener(v -> click.onClick(f));
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvAvatar;

        VH(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvFriendName);
            tvEmail = v.findViewById(R.id.tvFriendEmail);
            tvAvatar = v.findViewById(R.id.tvAvatar);
        }
    }
}
