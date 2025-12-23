package com.oshing.fitlife.models;

public class ChecklistItem {

    private int id;
    private int userId;
    private String name;
    private boolean checked;
    private long createdAt;

    public ChecklistItem(int id, int userId, String name, boolean checked, long createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.checked = checked;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public boolean isChecked() {
        return checked;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
