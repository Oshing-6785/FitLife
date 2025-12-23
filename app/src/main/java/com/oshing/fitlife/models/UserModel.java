package com.oshing.fitlife.ui.delegation;

public class UserModel {
    private String uid;
    private String name;
    private String email;

    public UserModel() {
    }

    public UserModel(String uid, String name, String email) {
        this.uid = uid;
        this.name = name;
        this.email = email;
    }

    public String getUid() { return uid; }
    public String getName() { return name; }
    public String getEmail() { return email; }

    public void setUid(String uid) { this.uid = uid; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
}
