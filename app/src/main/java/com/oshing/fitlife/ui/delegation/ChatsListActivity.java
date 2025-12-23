package com.oshing.fitlife.ui.delegation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.oshing.fitlife.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ChatsListActivity extends AppCompatActivity
        implements ChatsListAdapter.OnChatClick, ChatsListAdapter.OnChatLongClick {

    private RecyclerView recycler;
    private ChatsListAdapter adapter;

    private FirebaseFirestore firestore;
    private String myUid;

    private MaterialToolbar toolbarChats;
    private TextView tvEmptyChats;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chats_list);

        myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) {
            Toast.makeText(this, "Please login again (Firebase user missing)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        firestore = FirebaseFirestore.getInstance();

        toolbarChats = findViewById(R.id.toolbarChats);
        tvEmptyChats = findViewById(R.id.tvEmptyChats);

        if (toolbarChats != null) {
            toolbarChats.setTitle("Chats");
            toolbarChats.setNavigationOnClickListener(v -> finish());
        }

        recycler = findViewById(R.id.recyclerChats);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ChatsListAdapter(this, this, this);
        recycler.setAdapter(adapter);

        loadChats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadChats(); // refresh ChatActivity
    }

    private void loadChats() {
        firestore.collection("chats")
                .whereEqualTo("participants." + myUid, true)
                .get()
                .addOnSuccessListener(chatsSnap -> {
                    List<ChatListItem> items = new ArrayList<>();

                    for (QueryDocumentSnapshot chatDoc : chatsSnap) {

                        Map<String, Object> hidden = (Map<String, Object>) chatDoc.get("hidden");
                        if (hidden != null && Boolean.TRUE.equals(hidden.get(myUid))) {
                            continue;
                        }

                        String chatId = chatDoc.getId();

                        Map<String, Object> participants = (Map<String, Object>) chatDoc.get("participants");
                        String friendUid = extractFriendUid(participants, myUid);
                        if (friendUid == null || friendUid.trim().isEmpty()) continue;

                        ChatListItem item = new ChatListItem();
                        item.chatId = chatId;
                        item.friendUid = friendUid;

                        item.lastMessage = chatDoc.getString("lastMessage");
                        Long ts = chatDoc.getLong("lastTimestamp");
                        item.timestamp = (ts == null ? 0L : ts);
                        if (item.lastMessage == null) item.lastMessage = "";

                        Map<String, Object> names = (Map<String, Object>) chatDoc.get("names");
                        if (names != null) {
                            Object nameObj = names.get(friendUid);
                            if (nameObj != null) item.friendName = String.valueOf(nameObj);
                        }
                        if (item.friendName == null || item.friendName.trim().isEmpty()) {
                            item.friendName = friendUid; // fallback
                        }

                        items.add(item);
                    }

                    Collections.sort(items, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                    adapter.submit(items);

                    // Empty state
                    if (tvEmptyChats != null) {
                        tvEmptyChats.setVisibility(items.isEmpty() ? TextView.VISIBLE : TextView.GONE);
                    }
                    recycler.setVisibility(items.isEmpty() ? RecyclerView.GONE : RecyclerView.VISIBLE);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Load chats failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String extractFriendUid(Map<String, Object> participants, String myUid) {
        if (participants == null || participants.isEmpty()) return null;
        for (String uid : participants.keySet()) {
            if (uid != null && !uid.equals(myUid)) return uid;
        }
        return null;
    }

    @Override
    public void onChatClick(ChatListItem item) {
        Intent i = new Intent(this, ChatActivity.class);
        i.putExtra("friend_uid", item.friendUid);
        i.putExtra("friend_name", item.friendName);
        startActivity(i);
    }

    @Override
    public void onChatLongClick(ChatListItem item) {
        confirmHideChat(item);
    }

    private void confirmHideChat(ChatListItem item) {
        String title = (item.friendName != null && !item.friendName.trim().isEmpty())
                ? item.friendName
                : "this chat";

        new AlertDialog.Builder(this)
                .setTitle("Hide chat")
                .setMessage("Hide chat with " + title + " from your chat list?\n\nYou can restore it by sending/receiving a new message.")
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Hide", (d, w) -> hideChat(item))
                .show();
    }

    private void hideChat(ChatListItem item) {
        firestore.collection("chats")
                .document(item.chatId)
                .update("hidden." + myUid, true)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Chat hidden", Toast.LENGTH_SHORT).show();
                    loadChats();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Hide failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
