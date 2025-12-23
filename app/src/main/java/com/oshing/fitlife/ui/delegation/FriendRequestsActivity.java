package com.oshing.fitlife.ui.delegation;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.oshing.fitlife.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendRequestsActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private RequestsAdapter adapter;

    private FirebaseFirestore firestore;
    private String myUid;

    private final List<RequestModel> requests = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_requests);

        recycler = findViewById(R.id.recyclerRequests);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getUid();

        if (myUid == null) {
            Toast.makeText(this, "Session expired. Login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new RequestsAdapter(requests, this::acceptRequest, this::rejectRequest);
        recycler.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIncomingRequests(); // refresh when screen opens
    }

    private void loadIncomingRequests() {
        firestore.collection("users")
                .document(myUid)
                .collection("incoming_requests")
                .get()
                .addOnSuccessListener(query -> {
                    requests.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        RequestModel r = doc.toObject(RequestModel.class);
                        if (r != null) {
                            r.setFromUid(doc.getId()); // docId = sender uid
                            requests.add(r);
                        }
                    }
                    adapter.notifyDataSetChanged();

                    if (requests.isEmpty()) {
                        Toast.makeText(this, "No friend requests", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load requests: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void acceptRequest(RequestModel r) {
        String fromUid = r.getFromUid();

        Map<String, Object> meForThem = new HashMap<>();
        meForThem.put("friendUid", myUid);
        meForThem.put("since", System.currentTimeMillis());

        Map<String, Object> themForMe = new HashMap<>();
        themForMe.put("friendUid", fromUid);
        themForMe.put("since", System.currentTimeMillis());

        firestore.runBatch(batch -> {
            // add friends
            batch.set(
                    firestore.collection("users")
                            .document(myUid)
                            .collection("friends")
                            .document(fromUid),
                    themForMe
            );

            batch.set(
                    firestore.collection("users")
                            .document(fromUid)
                            .collection("friends")
                            .document(myUid),
                    meForThem
            );

            // remove requests
            batch.delete(
                    firestore.collection("users")
                            .document(myUid)
                            .collection("incoming_requests")
                            .document(fromUid)
            );

            batch.delete(
                    firestore.collection("users")
                            .document(fromUid)
                            .collection("outgoing_requests")
                            .document(myUid)
            );
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Friend added", Toast.LENGTH_SHORT).show();
            loadIncomingRequests();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Accept failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }

    private void rejectRequest(RequestModel r) {
        String fromUid = r.getFromUid();

        firestore.runBatch(batch -> {
            batch.delete(
                    firestore.collection("users")
                            .document(myUid)
                            .collection("incoming_requests")
                            .document(fromUid)
            );

            batch.delete(
                    firestore.collection("users")
                            .document(fromUid)
                            .collection("outgoing_requests")
                            .document(myUid)
            );
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show();
            loadIncomingRequests();
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Reject failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
        );
    }
}
