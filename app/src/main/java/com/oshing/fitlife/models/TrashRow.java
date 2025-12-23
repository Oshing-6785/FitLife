package com.oshing.fitlife.ui.routines.model;

public class TrashRow {

    public static final int TYPE_ROUTINE = 1;
    public static final int TYPE_EXERCISE = 2;
    public static final int TYPE_CHECKLIST = 3;

    public int type;
    public int id;
    public String title;
    public String subtitle;
    public long deletedAt;

    public TrashRow(int type, int id, String title, String subtitle, long deletedAt) {
        this.type = type;
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.deletedAt = deletedAt;
    }
}
