package com.oshing.fitlife.ui.delegation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oshing.fitlife.R;

import java.util.List;

public class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.VH> {

    public interface Action {
        void onAction(RequestModel r);
    }

    private final List<RequestModel> requests;
    private final Action onAccept;
    private final Action onReject;

    public RequestsAdapter(List<RequestModel> requests, Action onAccept, Action onReject) {
        this.requests = requests;
        this.onAccept = onAccept;
        this.onReject = onReject;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_friend_request, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        RequestModel r = requests.get(position);

        h.tvName.setText(r.getFromName() == null ? "User" : r.getFromName());
        h.tvEmail.setText(r.getFromEmail() == null ? "" : r.getFromEmail());

        h.btnAccept.setOnClickListener(v -> onAccept.onAction(r));
        h.btnReject.setOnClickListener(v -> onReject.onAction(r));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail;
        Button btnAccept, btnReject;

        VH(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvName);
            tvEmail = v.findViewById(R.id.tvEmail);
            btnAccept = v.findViewById(R.id.btnAccept);
            btnReject = v.findViewById(R.id.btnReject);
        }
    }
}
