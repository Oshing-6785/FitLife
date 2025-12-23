package com.oshing.fitlife.ui.delegation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.oshing.fitlife.R;

public class DelegationHomeActivity extends AppCompatActivity {

    private Button btnFindUsers, btnRequests, btnFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delegation_home);

        btnFindUsers = findViewById(R.id.btnFindUsers);
        btnRequests = findViewById(R.id.btnRequests);
        btnFriends = findViewById(R.id.btnFriends);

        btnFindUsers.setOnClickListener(v ->
                startActivity(new Intent(this, FindUsersActivity.class))
        );

        btnRequests.setOnClickListener(v ->
                startActivity(new Intent(this, FriendRequestsActivity.class))
        );

        btnFriends.setOnClickListener(v ->
                startActivity(new Intent(this, FriendsListActivity.class))
        );
    }
}
