package com.oshing.fitlife.ui.delegation;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.oshing.fitlife.R;
import com.oshing.fitlife.data.DatabaseHelper;
import com.oshing.fitlife.utils.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView recycler;
    private EditText etMessage;
    private ImageButton btnSend, btnChecklist;
    private MaterialToolbar toolbarChat;

    private FirebaseFirestore firestore;
    private DatabaseHelper dbHelper;

    private String myUid, friendUid, friendName, chatId;

    private ChatAdapter adapter;
    private int lastCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        friendUid = getIntent().getStringExtra("friend_uid");
        friendName = getIntent().getStringExtra("friend_name");

        if (friendUid == null || friendUid.trim().isEmpty()) {
            Toast.makeText(this, "Missing friend id", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        friendUid = friendUid.trim();

        myUid = FirebaseAuth.getInstance().getUid();
        if (myUid == null) {
            Toast.makeText(this, "Please login again (Firebase user missing)", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        chatId = buildChatId(myUid, friendUid);

        firestore = FirebaseFirestore.getInstance();
        dbHelper = new DatabaseHelper(this);

        toolbarChat = findViewById(R.id.toolbarChat);
        recycler = findViewById(R.id.recyclerChat);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        btnChecklist = findViewById(R.id.btnChecklist);

        if (toolbarChat != null) {
            toolbarChat.setTitle((friendName == null || friendName.trim().isEmpty()) ? "Chat" : friendName.trim());
            toolbarChat.setNavigationOnClickListener(v -> finish());
        } else {
            setTitle((friendName == null || friendName.trim().isEmpty()) ? "Chat" : friendName.trim());
        }

        adapter = new ChatAdapter(myUid, friendName);

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        recycler.setLayoutManager(lm);
        recycler.setAdapter(adapter);

        ensureChatDocThenListen();

        btnSend.setOnClickListener(v -> sendText());
        btnChecklist.setOnClickListener(v -> showChecklistPickerAndSend());
    }

    private String buildChatId(String a, String b) {
        return (a.compareTo(b) < 0) ? (a + "_" + b) : (b + "_" + a);
    }

    private void ensureChatDocThenListen() {
        long now = System.currentTimeMillis();

        Map<String, Object> participants = new HashMap<>();
        participants.put(myUid, true);
        participants.put(friendUid, true);

        Map<String, Object> names = new HashMap<>();
        names.put(myUid, "Me");
        names.put(friendUid, (friendName == null || friendName.trim().isEmpty()) ? "User" : friendName.trim());

        Map<String, Object> chatDoc = new HashMap<>();
        chatDoc.put("participants", participants);
        chatDoc.put("names", names);
        chatDoc.put("updatedAt", now);
        chatDoc.put("lastMessage", "");
        chatDoc.put("lastTimestamp", now);

        firestore.collection("chats")
                .document(chatId)
                .set(chatDoc, SetOptions.merge())
                .addOnSuccessListener(unused -> listenMessages())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Chat init failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void listenMessages() {
        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) {
                        Toast.makeText(this, "Listen failed: " + err.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    List<ChatMessage> list = snap.toObjects(ChatMessage.class);
                    adapter.setMessages(list);

                    if (list.size() > lastCount) {
                        recycler.scrollToPosition(Math.max(adapter.getItemCount() - 1, 0));
                        lastCount = list.size();
                    }
                });
    }

    private void sendText() {
        String text = etMessage.getText() == null ? "" : etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        etMessage.setText("");
        sendMessage(text, "TEXT");
    }

    private void showChecklistPickerAndSend() {
        int localUserId = SessionManager.getUserId(this);
        if (localUserId <= 0) {
            Toast.makeText(this, "Local session missing. Please login again.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<DatabaseHelper.ChecklistSummary> items = dbHelper.getActiveChecklistSummaries(localUserId);
        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "No checklist items found", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> labels = new ArrayList<>();
        for (DatabaseHelper.ChecklistSummary it : items) {
            String prefix = it.isChecked ? "✅ " : "⬜ ";
            labels.add(prefix + it.name);
        }

        new AlertDialog.Builder(this)
                .setTitle("Send checklist item")
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    DatabaseHelper.ChecklistSummary item = items.get(which);
                    String msg = item.isChecked
                            ? "I’m bringing " + item.name
                            : "Please bring " + item.name;

                    sendMessage(msg, "CHECKLIST");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendMessage(String text, String type) {
        long now = System.currentTimeMillis();

        Map<String, Object> unhide = new HashMap<>();
        unhide.put("hidden." + myUid, false);
        unhide.put("hidden." + friendUid, false);

        firestore.collection("chats").document(chatId).update(unhide);

        Map<String, Object> data = new HashMap<>();
        data.put("fromUid", myUid);
        data.put("toUid", friendUid);
        data.put("text", text == null ? "" : text);
        data.put("type", type == null ? "TEXT" : type);
        data.put("timestamp", now);

        Map<String, Object> chatUpdate = new HashMap<>();
        chatUpdate.put("lastMessage", text == null ? "" : text);
        chatUpdate.put("lastTimestamp", now);
        chatUpdate.put("updatedAt", now);

        Map<String, Object> names = new HashMap<>();
        names.put(myUid, "Me");
        names.put(friendUid, (friendName == null || friendName.trim().isEmpty()) ? "User" : friendName.trim());
        chatUpdate.put("names", names);

        firestore.collection("chats").document(chatId).set(chatUpdate, SetOptions.merge());

        firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(data)
                .addOnFailureListener(err ->
                        Toast.makeText(this, "Send failed: " + err.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
