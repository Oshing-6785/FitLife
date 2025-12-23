package com.oshing.fitlife.ui.delegation;

import android.content.Intent;
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
import java.util.List;

public class FriendsListActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private FriendsAdapter adapter;

    private FirebaseFirestore firestore;
    private String myUid;

    private final List<FriendModel> friends = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends_list);

        recycler = findViewById(R.id.recyclerFriends);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        firestore = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getUid();

        if (myUid == null) {
            Toast.makeText(this, "Session expired", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        adapter = new FriendsAdapter(friends, friend -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("friend_uid", friend.getFriendUid());
            i.putExtra("friend_name", friend.getName()); // optional
            startActivity(i);
        });

        recycler.setAdapter(adapter);

        loadFriends();
    }

    private void loadFriends() {
        // users/{myUid}/friends/{friendUid}
        firestore.collection("users")
                .document(myUid)
                .collection("friends")
                .get()
                .addOnSuccessListener(q -> {
                    friends.clear();

                    if (q.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "No friends yet", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : q.getDocuments()) {
                        String friendUid = doc.getId();

                        //fallback
                        if (friendUid == null || friendUid.trim().isEmpty()) {
                            String fieldUid = doc.getString("friendUid");
                            if (fieldUid != null && !fieldUid.trim().isEmpty()) {
                                friendUid = fieldUid;
                            }
                        }

                        if (friendUid != null && !friendUid.trim().isEmpty()) {
                            loadFriendProfile(friendUid);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load friends failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadFriendProfile(String friendUid) {
        firestore.collection("users")
                .document(friendUid)
                .get()
                .addOnSuccessListener(doc -> {
                    FriendModel f = new FriendModel();
                    f.setFriendUid(friendUid);

                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String email = doc.getString("email");
                        if (name != null && !name.trim().isEmpty()) f.setName(name);
                        else f.setName("User");

                        if (email != null) f.setEmail(email);
                        else f.setEmail("");
                    } else {
                        f.setName("User");
                        f.setEmail("");
                    }

                    friends.add(f);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    // add UID
                    FriendModel f = new FriendModel();
                    f.setFriendUid(friendUid);
                    f.setName("User");
                    f.setEmail("");
                    friends.add(f);
                    adapter.notifyDataSetChanged();
                });
    }
}
