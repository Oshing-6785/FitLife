package com.oshing.fitlife.ui.delegation;

public class FriendModel {

    private String friendUid;
    private String name;
    private String email;

    // empty constructor for Firestore
    public FriendModel() {
    }

    // UID
    public String getFriendUid() {
        return friendUid;
    }

    public void setFriendUid(String friendUid) {
        this.friendUid = friendUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
