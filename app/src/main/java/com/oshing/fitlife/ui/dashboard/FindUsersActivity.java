package com.oshing.fitlife.ui.delegation;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.oshing.fitlife.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FindUsersActivity extends AppCompatActivity {

    private EditText etSearchEmail;
    private RecyclerView recyclerUsers;

    private FirebaseFirestore firestore;
    private FirebaseUser fbUser;

    private String myUid;
    private String myName = "User";
    private String myEmail = "";

    private UsersAdapter adapter;
    private final List<UserModel> users = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_users);

        etSearchEmail = findViewById(R.id.etSearchEmail);
        recyclerUsers = findViewById(R.id.recyclerUsers);

        firestore = FirebaseFirestore.getInstance();
        fbUser = FirebaseAuth.getInstance().getCurrentUser();

        if (fbUser == null) {
            Toast.makeText(this, "Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        myUid = fbUser.getUid();
        myEmail = (fbUser.getEmail() == null) ? "" : fbUser.getEmail().trim().toLowerCase(Locale.ROOT);

        adapter = new UsersAdapter(users, myUid, this::sendFriendRequest);
        recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerUsers.setAdapter(adapter);

        findViewById(R.id.btnSearch).setOnClickListener(v -> searchUser());

        // Load my profile
        loadMyProfileThenUsers();
    }

    private void loadMyProfileThenUsers() {
        firestore.collection("users").document(myUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String n = doc.getString("name");
                        String e = doc.getString("email");
                        if (n != null && !n.trim().isEmpty()) myName = n.trim();
                        if (e != null && !e.trim().isEmpty()) myEmail = e.trim().toLowerCase(Locale.ROOT);
                    }
                    loadAllUsers();
                })
                .addOnFailureListener(e -> loadAllUsers());
    }

    private void loadAllUsers() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    users.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        if (doc.getId().equals(myUid)) continue;

                        UserModel u = doc.toObject(UserModel.class);
                        if (u == null) u = new UserModel();

                        u.setUid(doc.getId());

                        if (u.getEmail() == null) u.setEmail("");
                        if (u.getName() == null) u.setName("User");

                        users.add(u);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load users: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void searchUser() {
        String email = etSearchEmail.getText().toString().trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty()) {
            Toast.makeText(this, "Enter email to search", Toast.LENGTH_SHORT).show();
            return;
        }

        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    users.clear();
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        if (doc.getId().equals(myUid)) continue;

                        UserModel u = doc.toObject(UserModel.class);
                        if (u == null) u = new UserModel();
                        u.setUid(doc.getId());

                        if (u.getEmail() == null) u.setEmail("");
                        if (u.getName() == null) u.setName("User");

                        users.add(u);
                    }
                    adapter.notifyDataSetChanged();

                    if (users.isEmpty()) {
                        Toast.makeText(this, "No user found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Search failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void sendFriendRequest(UserModel target) {
        if (target == null || target.getUid() == null || target.getUid().trim().isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show();
            return;
        }

        String targetUid = target.getUid().trim();

        if (targetUid.equals(myUid)) {
            Toast.makeText(this, "You can’t add yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> incoming = new HashMap<>();
        incoming.put("fromUid", myUid);
        incoming.put("fromName", myName);
        incoming.put("fromEmail", myEmail == null ? "" : myEmail.toLowerCase(Locale.ROOT));
        incoming.put("sentAt", System.currentTimeMillis());

        Map<String, Object> outgoing = new HashMap<>();
        outgoing.put("toUid", targetUid);
        outgoing.put("toName", target.getName() == null ? "User" : target.getName());
        outgoing.put("toEmail", target.getEmail() == null ? "" : target.getEmail().toLowerCase(Locale.ROOT));
        outgoing.put("sentAt", System.currentTimeMillis());

        // 1.Write incoming request under TARGET user
        firestore.collection("users")
                .document(targetUid)
                .collection("incoming_requests")
                .document(myUid)
                .set(incoming)
                .addOnSuccessListener(unused ->

                        // 2.Write outgoing request under ME
                        firestore.collection("users")
                                .document(myUid)
                                .collection("outgoing_requests")
                                .document(targetUid)
                                .set(outgoing)
                                .addOnSuccessListener(unused2 ->
                                        Toast.makeText(this, "Friend request sent", Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Outgoing write denied: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                )

                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Incoming write denied: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
