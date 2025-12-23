package com.oshing.fitlife.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.oshing.fitlife.utils.HashUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {


    //  DATABASE CONFIG
    private static final String DATABASE_NAME = "fitlife.db";

    private static final int DATABASE_VERSION = 9;


    //  TABLE NAMES
    public static final String TABLE_USERS = "users";
    public static final String TABLE_ROUTINES = "routines";
    public static final String TABLE_EXERCISES = "exercises";

    public static final String TABLE_CHECKLIST = "checklist_items";

    //  USERS COLUMNS
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_NAME = "name";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD = "password";

    //  ROUTINES COLUMNS
    public static final String COL_ROUTINE_ID = "id";
    public static final String COL_ROUTINE_USER_ID = "user_id";
    public static final String COL_ROUTINE_NAME = "name";
    public static final String COL_ROUTINE_DAY_OF_WEEK = "day_of_week";
    public static final String COL_ROUTINE_TIME = "time";
    public static final String COL_ROUTINE_GOAL = "goal";
    public static final String COL_ROUTINE_NOTES = "notes";
    public static final String COL_ROUTINE_IS_DONE = "is_done"; // 0/1

    // Soft delete support
    public static final String COL_ROUTINE_IS_DELETED = "is_deleted"; // 0/1
    public static final String COL_ROUTINE_DELETED_AT = "deleted_at"; // epoch millis

    //  EXERCISES COLUMNS
    public static final String COL_EX_ID = "id";
    public static final String COL_EX_ROUTINE_ID = "routine_id";
    public static final String COL_EX_NAME = "name";
    public static final String COL_EX_SETS = "sets";
    public static final String COL_EX_REPS = "reps";
    public static final String COL_EX_EQUIPMENT = "equipment";
    public static final String COL_EX_INSTRUCTIONS = "instructions";

    // Exercise image support
    public static final String COL_EX_IMAGE_URI = "image_uri";

    public static final String COL_EX_IS_COMPLETED = "is_completed";

    // NEW exercise columns (v7)
    public static final String COL_EX_TYPE = "type";
    public static final String COL_EX_WEIGHT = "weight";
    public static final String COL_EX_REST_SECONDS = "rest_seconds";
    public static final String COL_EX_DURATION_MIN = "duration_min";

    // NEW exercise soft delete columns (v8)
    public static final String COL_EX_IS_DELETED = "is_deleted"; // 0/1
    public static final String COL_EX_DELETED_AT = "deleted_at"; // epoch millis


    // CHECKLIST COLUMNS
    public static final String COL_CHECK_ID = "id";
    public static final String COL_CHECK_USER_ID = "user_id";
    public static final String COL_CHECK_NAME = "name";
    public static final String COL_CHECK_IS_CHECKED = "is_checked"; // 0/1
    public static final String COL_CHECK_CREATED_AT = "created_at";

    // Soft delete support (checklist)
    public static final String COL_CHECK_IS_DELETED = "is_deleted"; // 0/1
    public static final String COL_CHECK_DELETED_AT = "deleted_at"; // epoch millis

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //  CREATE TABLES
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_USERS + " ("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_USER_NAME + " TEXT, "
                + COL_USER_EMAIL + " TEXT UNIQUE, "
                + COL_USER_PASSWORD + " TEXT"
                + ");";

        String CREATE_ROUTINES_TABLE = "CREATE TABLE " + TABLE_ROUTINES + " ("
                + COL_ROUTINE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_ROUTINE_USER_ID + " INTEGER, "
                + COL_ROUTINE_NAME + " TEXT, "
                + COL_ROUTINE_DAY_OF_WEEK + " TEXT, "
                + COL_ROUTINE_TIME + " TEXT, "
                + COL_ROUTINE_GOAL + " TEXT, "
                + COL_ROUTINE_NOTES + " TEXT, "
                + COL_ROUTINE_IS_DONE + " INTEGER DEFAULT 0, "
                + COL_ROUTINE_IS_DELETED + " INTEGER DEFAULT 0, "
                + COL_ROUTINE_DELETED_AT + " INTEGER"
                + ");";

        String CREATE_EXERCISES_TABLE = "CREATE TABLE " + TABLE_EXERCISES + " ("
                + COL_EX_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_EX_ROUTINE_ID + " INTEGER, "
                + COL_EX_NAME + " TEXT, "
                + COL_EX_SETS + " INTEGER, "
                + COL_EX_REPS + " INTEGER, "
                + COL_EX_EQUIPMENT + " TEXT, "
                + COL_EX_INSTRUCTIONS + " TEXT, "
                + COL_EX_IMAGE_URI + " TEXT, "
                + COL_EX_TYPE + " TEXT, "
                + COL_EX_WEIGHT + " REAL, "
                + COL_EX_REST_SECONDS + " INTEGER, "
                + COL_EX_DURATION_MIN + " INTEGER, "
                + COL_EX_IS_COMPLETED + " INTEGER DEFAULT 0, "
                + COL_EX_IS_DELETED + " INTEGER DEFAULT 0, "
                + COL_EX_DELETED_AT + " INTEGER"
                + ");";

        String CREATE_CHECKLIST_TABLE = "CREATE TABLE " + TABLE_CHECKLIST + " ("
                + COL_CHECK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CHECK_USER_ID + " INTEGER, "
                + COL_CHECK_NAME + " TEXT NOT NULL, "
                + COL_CHECK_IS_CHECKED + " INTEGER DEFAULT 0, "
                + COL_CHECK_CREATED_AT + " INTEGER, "
                + COL_CHECK_IS_DELETED + " INTEGER DEFAULT 0, "
                + COL_CHECK_DELETED_AT + " INTEGER"
                + ");";

        db.execSQL(CREATE_USERS_TABLE);
        db.execSQL(CREATE_ROUTINES_TABLE);
        db.execSQL(CREATE_EXERCISES_TABLE);
        db.execSQL(CREATE_CHECKLIST_TABLE);
    }

    //  SAFE UPGRADE STRATEGY
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ROUTINES
                        + " ADD COLUMN " + COL_ROUTINE_IS_DONE + " INTEGER DEFAULT 0");
            } catch (Exception ignored) { }
        }

        if (oldVersion < 4) {
            try {
                db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CHECKLIST + " ("
                        + COL_CHECK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + COL_CHECK_USER_ID + " INTEGER, "
                        + COL_CHECK_NAME + " TEXT NOT NULL, "
                        + COL_CHECK_IS_CHECKED + " INTEGER DEFAULT 0, "
                        + COL_CHECK_CREATED_AT + " INTEGER"
                        + ");");
            } catch (Exception ignored) { }
        }

        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_ROUTINES
                        + " ADD COLUMN " + COL_ROUTINE_IS_DELETED + " INTEGER DEFAULT 0");
            } catch (Exception ignored) { }

            try {
                db.execSQL("ALTER TABLE " + TABLE_ROUTINES
                        + " ADD COLUMN " + COL_ROUTINE_DELETED_AT + " INTEGER");
            } catch (Exception ignored) { }
        }

        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_CHECKLIST
                        + " ADD COLUMN " + COL_CHECK_IS_DELETED + " INTEGER DEFAULT 0");
            } catch (Exception ignored) { }

            try {
                db.execSQL("ALTER TABLE " + TABLE_CHECKLIST
                        + " ADD COLUMN " + COL_CHECK_DELETED_AT + " INTEGER");
            } catch (Exception ignored) { }
        }

        if (oldVersion < 7) {
            try { db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_TYPE + " TEXT"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_WEIGHT + " REAL"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_REST_SECONDS + " INTEGER"); } catch (Exception ignored) {}
            try { db.execSQL("ALTER TABLE " + TABLE_EXERCISES + " ADD COLUMN " + COL_EX_DURATION_MIN + " INTEGER"); } catch (Exception ignored) {}
        }

        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES
                        + " ADD COLUMN " + COL_EX_IS_DELETED + " INTEGER DEFAULT 0");
            } catch (Exception ignored) { }

            try {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES
                        + " ADD COLUMN " + COL_EX_DELETED_AT + " INTEGER");
            } catch (Exception ignored) { }
        }

        // image_uri exists for anyone upgrading from older DBs
        if (oldVersion < 9) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_EXERCISES
                        + " ADD COLUMN " + COL_EX_IMAGE_URI + " TEXT");
            } catch (Exception ignored) { }
        }

        try {
            db.execSQL("UPDATE " + TABLE_EXERCISES +
                    " SET " + COL_EX_IS_COMPLETED + " = 0 WHERE " + COL_EX_IS_COMPLETED + " IS NULL");
        } catch (Exception ignored) { }

        try {
            db.execSQL("UPDATE " + TABLE_EXERCISES +
                    " SET " + COL_EX_IS_DELETED + " = 0 WHERE " + COL_EX_IS_DELETED + " IS NULL");
        } catch (Exception ignored) { }
    }

    // ✅ DELEGATION / LOCAL USERS
    public static class LocalUser {
        public int id;
        public String name;
        public String email;

        public LocalUser(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }

    public LocalUser getLocalUserByEmail(String email) {
        if (email == null) return null;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(
                    TABLE_USERS,
                    new String[]{COL_USER_ID, COL_USER_NAME, COL_USER_EMAIL},
                    COL_USER_EMAIL + " = ?",
                    new String[]{email.trim()},
                    null, null, null,
                    "1"
            );

            if (c != null && c.moveToFirst()) {
                int id = c.getInt(c.getColumnIndexOrThrow(COL_USER_ID));
                String name = safe(c.getString(c.getColumnIndexOrThrow(COL_USER_NAME)));
                String em = safe(c.getString(c.getColumnIndexOrThrow(COL_USER_EMAIL)));
                return new LocalUser(id, name, em);
            }
        } finally {
            if (c != null) c.close();
        }
        return null;
    }

    public List<LocalUser> getAllLocalUsers() {
        List<LocalUser> users = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(
                    TABLE_USERS,
                    new String[]{COL_USER_ID, COL_USER_NAME, COL_USER_EMAIL},
                    null,
                    null,
                    null, null,
                    COL_USER_NAME + " COLLATE NOCASE ASC"
            );

            if (c != null && c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndexOrThrow(COL_USER_ID));
                    String name = safe(c.getString(c.getColumnIndexOrThrow(COL_USER_NAME)));
                    String email = safe(c.getString(c.getColumnIndexOrThrow(COL_USER_EMAIL)));
                    users.add(new LocalUser(id, name, email));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return users;
    }

    // CHECKLIST SUMMARY (used by Chat / Delegation)
    public static class ChecklistSummary {
        public int id;
        public String name;
        public boolean isChecked;

        public ChecklistSummary(int id, String name, boolean isChecked) {
            this.id = id;
            this.name = name;
            this.isChecked = isChecked;
        }
    }

    //  USER / AUTH HELPERS
    public long registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();

        String hashedPassword = HashUtils.sha256(password);

        ContentValues values = new ContentValues();
        values.put(COL_USER_NAME, name);
        values.put(COL_USER_EMAIL, email);
        values.put(COL_USER_PASSWORD, hashedPassword);

        return db.insert(TABLE_USERS, null, values);
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USER_EMAIL + " = ?",
                new String[]{email},
                null, null, null
        );

        boolean exists = (cursor != null && cursor.moveToFirst());
        if (cursor != null) cursor.close();
        return exists;
    }

    public int authenticateUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        String hashedPassword = HashUtils.sha256(password);

        Cursor cursor = db.query(
                TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USER_EMAIL + " = ? AND " + COL_USER_PASSWORD + " = ?",
                new String[]{email, hashedPassword},
                null, null, null
        );

        int userId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COL_USER_ID));
        }
        if (cursor != null) cursor.close();
        return userId;
    }

    // ROUTINES

    public long addRoutine(int userId, String name, String day, String time, String goal, String notes) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ROUTINE_USER_ID, userId);
        values.put(COL_ROUTINE_NAME, name);
        values.put(COL_ROUTINE_DAY_OF_WEEK, day);
        values.put(COL_ROUTINE_TIME, time);
        values.put(COL_ROUTINE_GOAL, goal);
        values.put(COL_ROUTINE_NOTES, notes);
        values.put(COL_ROUTINE_IS_DONE, 0);
        values.put(COL_ROUTINE_IS_DELETED, 0);
        values.putNull(COL_ROUTINE_DELETED_AT);

        return db.insert(TABLE_ROUTINES, null, values);
    }

    public int updateRoutine(int routineId,
                             int userId,
                             String name,
                             String day,
                             String time,
                             String goal,
                             String notes) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ROUTINE_NAME, name);
        values.put(COL_ROUTINE_DAY_OF_WEEK, day);
        values.put(COL_ROUTINE_TIME, time);
        values.put(COL_ROUTINE_GOAL, goal);
        values.put(COL_ROUTINE_NOTES, notes);

        return db.update(
                TABLE_ROUTINES,
                values,
                COL_ROUTINE_ID + " = ? AND " + COL_ROUTINE_USER_ID + " = ? AND (" + COL_ROUTINE_IS_DELETED + " IS NULL OR " + COL_ROUTINE_IS_DELETED + " = 0)",
                new String[]{String.valueOf(routineId), String.valueOf(userId)}
        );
    }

    public Cursor getRoutineById(int routineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_ROUTINES,
                null,
                COL_ROUTINE_ID + " = ?",
                new String[]{String.valueOf(routineId)},
                null, null, null
        );
    }

    public int deleteRoutine(int routineId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ROUTINE_IS_DELETED, 1);
        values.put(COL_ROUTINE_DELETED_AT, System.currentTimeMillis());

        return db.update(
                TABLE_ROUTINES,
                values,
                COL_ROUTINE_ID + " = ?",
                new String[]{String.valueOf(routineId)}
        );
    }

    public int restoreRoutine(int routineId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_ROUTINE_IS_DELETED, 0);
        values.putNull(COL_ROUTINE_DELETED_AT);

        return db.update(
                TABLE_ROUTINES,
                values,
                COL_ROUTINE_ID + " = ? AND " + COL_ROUTINE_USER_ID + " = ?",
                new String[]{String.valueOf(routineId), String.valueOf(userId)}
        );
    }

    public boolean restoreRoutineById(int routineId, int userId) {
        return restoreRoutine(routineId, userId) > 0;
    }

    public boolean hardDeleteRoutine(int routineId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_EXERCISES, COL_EX_ROUTINE_ID + " = ?", new String[]{String.valueOf(routineId)});

            int deleted = db.delete(
                    TABLE_ROUTINES,
                    COL_ROUTINE_ID + " = ? AND " + COL_ROUTINE_USER_ID + " = ?",
                    new String[]{String.valueOf(routineId), String.valueOf(userId)}
            );

            db.setTransactionSuccessful();
            return deleted > 0;
        } finally {
            db.endTransaction();
        }
    }

    public int hardDeleteRoutine(int routineId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_EXERCISES, COL_EX_ROUTINE_ID + " = ?", new String[]{String.valueOf(routineId)});
            int deleted = db.delete(TABLE_ROUTINES, COL_ROUTINE_ID + " = ?", new String[]{String.valueOf(routineId)});
            db.setTransactionSuccessful();
            return deleted;
        } finally {
            db.endTransaction();
        }
    }

    public Cursor getDeletedRoutinesCursor(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_ROUTINES,
                null,
                COL_ROUTINE_USER_ID + " = ? AND " + COL_ROUTINE_IS_DELETED + " = 1",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_ROUTINE_DELETED_AT + " DESC"
        );
    }

    public List<ChecklistSummary> getActiveChecklistSummaries(int userId) {
        List<ChecklistSummary> out = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;

        try {
            c = db.query(
                    TABLE_CHECKLIST,
                    new String[]{COL_CHECK_ID, COL_CHECK_NAME, COL_CHECK_IS_CHECKED},
                    COL_CHECK_USER_ID + " = ? AND (" +
                            COL_CHECK_IS_DELETED + " IS NULL OR " +
                            COL_CHECK_IS_DELETED + " = 0)",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COL_CHECK_CREATED_AT + " DESC"
            );

            if (c != null && c.moveToFirst()) {
                do {
                    int id = c.getInt(c.getColumnIndexOrThrow(COL_CHECK_ID));
                    String name = safe(c.getString(c.getColumnIndexOrThrow(COL_CHECK_NAME)));
                    boolean checked = c.getInt(c.getColumnIndexOrThrow(COL_CHECK_IS_CHECKED)) == 1;
                    out.add(new ChecklistSummary(id, name, checked));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }

        return out;
    }

    public List<Integer> getDeletedRoutineIdsForUserSorted(int userId) {
        List<Integer> ids = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_ROUTINES,
                    new String[]{COL_ROUTINE_ID},
                    COL_ROUTINE_USER_ID + " = ? AND " + COL_ROUTINE_IS_DELETED + " = 1",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COL_ROUTINE_DELETED_AT + " DESC"
            );
            if (c != null && c.moveToFirst()) {
                do {
                    ids.add(c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_ID)));
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return ids;
    }

    public List<String> getDeletedRoutineDisplayListForUserSorted(int userId) {
        List<String> out = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_ROUTINES,
                    new String[]{COL_ROUTINE_NAME, COL_ROUTINE_DAY_OF_WEEK, COL_ROUTINE_TIME, COL_ROUTINE_GOAL},
                    COL_ROUTINE_USER_ID + " = ? AND " + COL_ROUTINE_IS_DELETED + " = 1",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COL_ROUTINE_DELETED_AT + " DESC"
            );

            if (c != null && c.moveToFirst()) {
                do {
                    String name = safe(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_NAME)));
                    String day = safe(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_DAY_OF_WEEK)));
                    String time = safe(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_TIME)));
                    String goal = safe(c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_GOAL)));
                    out.add(name + " (" + day + " @ " + time + ") - " + goal);
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    public int getMostRecentlyDeletedRoutineId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_ROUTINES,
                    new String[]{COL_ROUTINE_ID},
                    COL_ROUTINE_USER_ID + " = ? AND " + COL_ROUTINE_IS_DELETED + " = 1",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COL_ROUTINE_DELETED_AT + " DESC",
                    "1"
            );
            if (c != null && c.moveToFirst()) {
                return c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_ID));
            }
        } finally {
            if (c != null) c.close();
        }
        return -1;
    }

    public boolean toggleRoutineDone(int routineId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int current = 0;
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_ROUTINES,
                    new String[]{COL_ROUTINE_IS_DONE},
                    COL_ROUTINE_ID + " = ? AND " + COL_ROUTINE_USER_ID + " = ?",
                    new String[]{String.valueOf(routineId), String.valueOf(userId)},
                    null, null, null
            );

            if (c != null && c.moveToFirst()) {
                current = c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_IS_DONE));
            } else {
                return false;
            }
        } finally {
            if (c != null) c.close();
        }

        int newValue = (current == 1) ? 0 : 1;

        ContentValues values = new ContentValues();
        values.put(COL_ROUTINE_IS_DONE, newValue);

        int rows = db.update(
                TABLE_ROUTINES,
                values,
                COL_ROUTINE_ID + " = ? AND " + COL_ROUTINE_USER_ID + " = ?",
                new String[]{String.valueOf(routineId), String.valueOf(userId)}
        );

        return rows > 0 && newValue == 1;
    }

    private static class RoutineRow {
        int id;
        String name, day, time, goal, notes;
        int isDone;
        int isDeleted;

        RoutineRow(int id, String name, String day, String time, String goal, String notes, int isDone, int isDeleted) {
            this.id = id;
            this.name = name;
            this.day = day;
            this.time = time;
            this.goal = goal;
            this.notes = notes;
            this.isDone = isDone;
            this.isDeleted = isDeleted;
        }
    }

    private int todayIndex() {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1;
    }

    private int dayToIndex(String day) {
        if (day == null) return 0;
        String d = day.toLowerCase(Locale.ROOT);
        if (d.startsWith("sun")) return 0;
        if (d.startsWith("mon")) return 1;
        if (d.startsWith("tue")) return 2;
        if (d.startsWith("wed")) return 3;
        if (d.startsWith("thu")) return 4;
        if (d.startsWith("fri")) return 5;
        if (d.startsWith("sat")) return 6;
        return 0;
    }

    private int timeToMinutes(String time) {
        if (time == null || !time.matches("\\d{2}:\\d{2}")) return 0;
        return Integer.parseInt(time.substring(0, 2)) * 60 + Integer.parseInt(time.substring(3, 5));
    }

    private List<RoutineRow> getRoutinesForUserSorted(int userId) {
        List<RoutineRow> rows = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.query(
                TABLE_ROUTINES,
                null,
                COL_ROUTINE_USER_ID + " = ? AND (" + COL_ROUTINE_IS_DELETED + " IS NULL OR " + COL_ROUTINE_IS_DELETED + " = 0)",
                new String[]{String.valueOf(userId)},
                null, null, null
        );

        if (c != null && c.moveToFirst()) {
            do {
                int isDone = 0;
                try { isDone = c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_IS_DONE)); } catch (Exception ignored) {}

                int isDeleted = 0;
                try { isDeleted = c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_IS_DELETED)); } catch (Exception ignored) {}

                rows.add(new RoutineRow(
                        c.getInt(c.getColumnIndexOrThrow(COL_ROUTINE_ID)),
                        c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_NAME)),
                        c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_DAY_OF_WEEK)),
                        c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_TIME)),
                        c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_GOAL)),
                        c.getString(c.getColumnIndexOrThrow(COL_ROUTINE_NOTES)),
                        isDone,
                        isDeleted
                ));
            } while (c.moveToNext());
        }
        if (c != null) c.close();

        int today = todayIndex();
        rows.sort((a, b) -> {
            int da = (dayToIndex(a.day) - today + 7) % 7;
            int dbb = (dayToIndex(b.day) - today + 7) % 7;
            if (da != dbb) return da - dbb;
            return timeToMinutes(a.time) - timeToMinutes(b.time);
        });

        return rows;
    }

    public List<String> getRoutineDisplayListForUserSorted(int userId) {
        List<String> out = new ArrayList<>();
        for (RoutineRow r : getRoutinesForUserSorted(userId)) {
            String prefix = (r.isDone == 1) ? "✅ " : "";
            String name = safe(r.name);
            String day = safe(r.day);
            String time = safe(r.time);
            String goal = safe(r.goal);
            out.add(prefix + name + " (" + day + " @ " + time + ") - " + goal);
        }
        return out;
    }

    public List<Integer> getRoutineIdsForUserSorted(int userId) {
        List<Integer> ids = new ArrayList<>();
        for (RoutineRow r : getRoutinesForUserSorted(userId)) ids.add(r.id);
        return ids;
    }

    public int restoreMostRecentlyDeletedRoutine(int userId) {
        int routineId = getMostRecentlyDeletedRoutineId(userId);
        if (routineId == -1) return -1;

        int rows = restoreRoutine(routineId, userId);
        return (rows > 0) ? routineId : -1;
    }

    // GLOBAL CHECKLIST

    public long addChecklistItem(int userId, String name) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_CHECK_USER_ID, userId);
        values.put(COL_CHECK_NAME, name);
        values.put(COL_CHECK_IS_CHECKED, 0);
        values.put(COL_CHECK_CREATED_AT, System.currentTimeMillis());
        values.put(COL_CHECK_IS_DELETED, 0);
        values.putNull(COL_CHECK_DELETED_AT);

        return db.insert(TABLE_CHECKLIST, null, values);
    }

    public int updateChecklistItemName(int itemId, int userId, String newName) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_CHECK_NAME, newName);

        return db.update(
                TABLE_CHECKLIST,
                values,
                COL_CHECK_ID + " = ? AND " + COL_CHECK_USER_ID + " = ? AND (" + COL_CHECK_IS_DELETED + " IS NULL OR " + COL_CHECK_IS_DELETED + " = 0)",
                new String[]{String.valueOf(itemId), String.valueOf(userId)}
        );
    }

    public boolean toggleChecklistItemChecked(int itemId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int current = 0;
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_CHECKLIST,
                    new String[]{COL_CHECK_IS_CHECKED},
                    COL_CHECK_ID + " = ? AND " + COL_CHECK_USER_ID + " = ? AND (" + COL_CHECK_IS_DELETED + " IS NULL OR " + COL_CHECK_IS_DELETED + " = 0)",
                    new String[]{String.valueOf(itemId), String.valueOf(userId)},
                    null, null, null
            );

            if (c != null && c.moveToFirst()) {
                current = c.getInt(c.getColumnIndexOrThrow(COL_CHECK_IS_CHECKED));
            } else {
                return false;
            }
        } finally {
            if (c != null) c.close();
        }

        int newValue = (current == 1) ? 0 : 1;

        ContentValues values = new ContentValues();
        values.put(COL_CHECK_IS_CHECKED, newValue);

        int rows = db.update(
                TABLE_CHECKLIST,
                values,
                COL_CHECK_ID + " = ? AND " + COL_CHECK_USER_ID + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(userId)}
        );

        return rows > 0 && newValue == 1;
    }

    public int deleteChecklistItem(int itemId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_CHECK_IS_DELETED, 1);
        values.put(COL_CHECK_DELETED_AT, System.currentTimeMillis());

        return db.update(
                TABLE_CHECKLIST,
                values,
                COL_CHECK_ID + " = ? AND " + COL_CHECK_USER_ID + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(userId)}
        );
    }

    public Cursor getChecklistItemsCursor(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        return db.query(
                TABLE_CHECKLIST,
                null,
                COL_CHECK_USER_ID + " = ? AND (" + COL_CHECK_IS_DELETED + " IS NULL OR " + COL_CHECK_IS_DELETED + " = 0)",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_CHECK_CREATED_AT + " DESC"
        );
    }

    public int clearCompletedChecklistItems(int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_CHECK_IS_DELETED, 1);
        values.put(COL_CHECK_DELETED_AT, System.currentTimeMillis());

        return db.update(
                TABLE_CHECKLIST,
                values,
                COL_CHECK_USER_ID + " = ? AND " + COL_CHECK_IS_CHECKED + " = 1 AND (" + COL_CHECK_IS_DELETED + " IS NULL OR " + COL_CHECK_IS_DELETED + " = 0)",
                new String[]{String.valueOf(userId)}
        );
    }

    public int getMostRecentlyDeletedChecklistItemId(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_CHECKLIST,
                    new String[]{COL_CHECK_ID},
                    COL_CHECK_USER_ID + " = ? AND " + COL_CHECK_IS_DELETED + " = 1",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COL_CHECK_DELETED_AT + " DESC",
                    "1"
            );
            if (c != null && c.moveToFirst()) {
                return c.getInt(c.getColumnIndexOrThrow(COL_CHECK_ID));
            }
        } finally {
            if (c != null) c.close();
        }
        return -1;
    }

    public int restoreChecklistItem(int itemId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_CHECK_IS_DELETED, 0);
        values.putNull(COL_CHECK_DELETED_AT);

        return db.update(
                TABLE_CHECKLIST,
                values,
                COL_CHECK_ID + " = ? AND " + COL_CHECK_USER_ID + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(userId)}
        );
    }

    public int restoreMostRecentlyDeletedChecklistItem(int userId) {
        int itemId = getMostRecentlyDeletedChecklistItemId(userId);
        if (itemId == -1) return -1;

        int rows = restoreChecklistItem(itemId, userId);
        return (rows > 0) ? itemId : -1;
    }

    //  OLD METHODS FOR SHAKE DELEGATION
    public ArrayList<String> getUncheckedGlobalChecklistItems() {
        ArrayList<String> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(
                    TABLE_CHECKLIST,
                    new String[]{COL_CHECK_NAME},
                    COL_CHECK_IS_CHECKED + " = 0 AND (" + COL_CHECK_IS_DELETED + " IS NULL OR " + COL_CHECK_IS_DELETED + " = 0)",
                    null,
                    null, null,
                    COL_CHECK_CREATED_AT + " DESC"
            );

            if (c != null && c.moveToFirst()) {
                do {
                    String name = c.getString(c.getColumnIndexOrThrow(COL_CHECK_NAME));
                    if (name != null && !name.trim().isEmpty()) items.add(name.trim());
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return items;
    }

    public ArrayList<String> getUncheckedGlobalChecklistItems(int userId) {
        ArrayList<String> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(
                    TABLE_CHECKLIST,
                    new String[]{COL_CHECK_NAME},
                    COL_CHECK_USER_ID + " = ? AND " + COL_CHECK_IS_CHECKED + " = 0 AND (" + COL_CHECK_IS_DELETED + " IS NULL OR " + COL_CHECK_IS_DELETED + " = 0)",
                    new String[]{String.valueOf(userId)},
                    null, null,
                    COL_CHECK_CREATED_AT + " DESC"
            );

            if (c != null && c.moveToFirst()) {
                do {
                    String name = c.getString(c.getColumnIndexOrThrow(COL_CHECK_NAME));
                    if (name != null && !name.trim().isEmpty()) items.add(name.trim());
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return items;
    }

    // ignore deleted exercises too
    public ArrayList<String> getUncheckedItemsForRoutine(int routineId) {
        ArrayList<String> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = null;
        try {
            c = db.query(
                    TABLE_EXERCISES,
                    new String[]{COL_EX_NAME},
                    COL_EX_ROUTINE_ID + " = ? AND (" + COL_EX_IS_COMPLETED + " IS NULL OR " + COL_EX_IS_COMPLETED + " = 0) AND ("
                            + COL_EX_IS_DELETED + " IS NULL OR " + COL_EX_IS_DELETED + " = 0)",
                    new String[]{String.valueOf(routineId)},
                    null, null,
                    COL_EX_NAME + " ASC"
            );

            if (c != null && c.moveToFirst()) {
                do {
                    String name = c.getString(c.getColumnIndexOrThrow(COL_EX_NAME));
                    if (name != null && !name.trim().isEmpty()) items.add(name.trim());
                } while (c.moveToNext());
            }
        } finally {
            if (c != null) c.close();
        }
        return items;
    }

    // EXERCISES
    public long addExercise(int routineId,
                            String name,
                            int sets,
                            int reps,
                            String equipment,
                            String instructions,
                            String imageUri,
                            String type,
                            double weight,
                            int restSeconds,
                            int durationMin) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EX_ROUTINE_ID, routineId);
        values.put(COL_EX_NAME, name);
        values.put(COL_EX_SETS, sets);
        values.put(COL_EX_REPS, reps);
        values.put(COL_EX_EQUIPMENT, equipment);
        values.put(COL_EX_INSTRUCTIONS, instructions);
        values.put(COL_EX_IMAGE_URI, imageUri);
        values.put(COL_EX_TYPE, type);
        values.put(COL_EX_WEIGHT, weight);
        values.put(COL_EX_REST_SECONDS, restSeconds);
        values.put(COL_EX_DURATION_MIN, durationMin);
        values.put(COL_EX_IS_COMPLETED, 0);
        values.put(COL_EX_IS_DELETED, 0);
        values.putNull(COL_EX_DELETED_AT);

        return db.insert(TABLE_EXERCISES, null, values);
    }

    public long addExercise(int routineId,
                            String name,
                            int sets,
                            int reps,
                            String equipment,
                            String instructions,
                            String imageUri) {
        return addExercise(routineId, name, sets, reps, equipment, instructions, imageUri,
                "Strength", 0.0, 0, 0);
    }

    public int updateExercise(int exerciseId,
                              int routineId,
                              String name,
                              int sets,
                              int reps,
                              String equipment,
                              String instructions,
                              String imageUri,
                              String type,
                              double weight,
                              int restSeconds,
                              int durationMin) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EX_NAME, name);
        values.put(COL_EX_SETS, sets);
        values.put(COL_EX_REPS, reps);
        values.put(COL_EX_EQUIPMENT, equipment);
        values.put(COL_EX_INSTRUCTIONS, instructions);
        values.put(COL_EX_IMAGE_URI, imageUri);
        values.put(COL_EX_TYPE, type);
        values.put(COL_EX_WEIGHT, weight);
        values.put(COL_EX_REST_SECONDS, restSeconds);
        values.put(COL_EX_DURATION_MIN, durationMin);

        return db.update(
                TABLE_EXERCISES,
                values,
                COL_EX_ID + " = ? AND " + COL_EX_ROUTINE_ID + " = ? AND (" + COL_EX_IS_DELETED + " IS NULL OR " + COL_EX_IS_DELETED + " = 0)",
                new String[]{String.valueOf(exerciseId), String.valueOf(routineId)}
        );
    }

    public int updateExercise(int exerciseId,
                              int routineId,
                              String name,
                              int sets,
                              int reps,
                              String equipment,
                              String instructions,
                              String imageUri) {
        return updateExercise(exerciseId, routineId, name, sets, reps, equipment, instructions, imageUri,
                "Strength", 0.0, 0, 0);
    }

    public Cursor getExercisesByRoutineCursor(int routineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_EXERCISES,
                null,
                COL_EX_ROUTINE_ID + " = ? AND (" + COL_EX_IS_DELETED + " IS NULL OR " + COL_EX_IS_DELETED + " = 0)",
                new String[]{String.valueOf(routineId)},
                null, null,
                COL_EX_NAME + " COLLATE NOCASE ASC"
        );
    }

    public Cursor getExercisesByRoutineCursorSearch(int routineId, String query) {
        SQLiteDatabase db = this.getReadableDatabase();

        String q = (query == null) ? "" : query.trim();
        if (q.isEmpty()) return getExercisesByRoutineCursor(routineId);

        String like = "%" + q + "%";

        return db.query(
                TABLE_EXERCISES,
                null,
                COL_EX_ROUTINE_ID + " = ? AND (" + COL_EX_IS_DELETED + " IS NULL OR " + COL_EX_IS_DELETED + " = 0) AND ("
                        + COL_EX_NAME + " LIKE ? OR "
                        + COL_EX_EQUIPMENT + " LIKE ? OR "
                        + COL_EX_TYPE + " LIKE ?"
                        + ")",
                new String[]{String.valueOf(routineId), like, like, like},
                null, null,
                COL_EX_NAME + " COLLATE NOCASE ASC"
        );
    }

    public Cursor getExerciseById(int exerciseId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_EXERCISES,
                null,
                COL_EX_ID + " = ?",
                new String[]{String.valueOf(exerciseId)},
                null, null, null,
                "1"
        );
    }

    public boolean toggleExerciseCompleted(int exerciseId) {
        SQLiteDatabase db = this.getWritableDatabase();

        int current = 0;
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_EXERCISES,
                    new String[]{COL_EX_IS_COMPLETED},
                    COL_EX_ID + " = ?",
                    new String[]{String.valueOf(exerciseId)},
                    null, null, null,
                    "1"
            );
            if (c != null && c.moveToFirst()) {
                try { current = c.getInt(c.getColumnIndexOrThrow(COL_EX_IS_COMPLETED)); }
                catch (Exception ignored) { current = 0; }
            }
        } finally {
            if (c != null) c.close();
        }

        int newValue = (current == 1) ? 0 : 1;

        ContentValues values = new ContentValues();
        values.put(COL_EX_IS_COMPLETED, newValue);

        int rows = db.update(
                TABLE_EXERCISES,
                values,
                COL_EX_ID + " = ? AND (" + COL_EX_IS_DELETED + " IS NULL OR " + COL_EX_IS_DELETED + " = 0)",
                new String[]{String.valueOf(exerciseId)}
        );

        return rows > 0 && newValue == 1;
    }

    public int deleteExercise(int exerciseId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EX_IS_DELETED, 1);
        values.put(COL_EX_DELETED_AT, System.currentTimeMillis());

        return db.update(
                TABLE_EXERCISES,
                values,
                COL_EX_ID + " = ?",
                new String[]{String.valueOf(exerciseId)}
        );
    }

    public int restoreExercise(int exerciseId) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_EX_IS_DELETED, 0);
        values.putNull(COL_EX_DELETED_AT);

        return db.update(
                TABLE_EXERCISES,
                values,
                COL_EX_ID + " = ?",
                new String[]{String.valueOf(exerciseId)}
        );
    }

    public int hardDeleteExercise(int exerciseId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(
                TABLE_EXERCISES,
                COL_EX_ID + " = ?",
                new String[]{String.valueOf(exerciseId)}
        );
    }

    public int getMostRecentlyDeletedExerciseIdForRoutine(int routineId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = null;
        try {
            c = db.query(
                    TABLE_EXERCISES,
                    new String[]{COL_EX_ID},
                    COL_EX_ROUTINE_ID + " = ? AND " + COL_EX_IS_DELETED + " = 1",
                    new String[]{String.valueOf(routineId)},
                    null, null,
                    COL_EX_DELETED_AT + " DESC",
                    "1"
            );
            if (c != null && c.moveToFirst()) {
                return c.getInt(c.getColumnIndexOrThrow(COL_EX_ID));
            }
        } finally {
            if (c != null) c.close();
        }
        return -1;
    }

    public int restoreMostRecentlyDeletedExerciseForRoutine(int routineId) {
        int exId = getMostRecentlyDeletedExerciseIdForRoutine(routineId);
        if (exId == -1) return -1;

        int rows = restoreExercise(exId);
        return (rows > 0) ? exId : -1;
    }

    // TRASH SUPPORT — USER SAFE
    public Cursor getDeletedExercisesCursorForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String sql =
                "SELECT e." + COL_EX_ID + " AS " + COL_EX_ID + ", " +
                        "e." + COL_EX_NAME + " AS " + COL_EX_NAME + ", " +
                        "e." + COL_EX_DELETED_AT + " AS " + COL_EX_DELETED_AT + ", " +
                        "r." + COL_ROUTINE_NAME + " AS " + COL_ROUTINE_NAME + " " +
                        "FROM " + TABLE_EXERCISES + " e " +
                        "JOIN " + TABLE_ROUTINES + " r ON r." + COL_ROUTINE_ID + " = e." + COL_EX_ROUTINE_ID + " " +
                        "WHERE r." + COL_ROUTINE_USER_ID + " = ? " +
                        "AND e." + COL_EX_IS_DELETED + " = 1 " +
                        "ORDER BY e." + COL_EX_DELETED_AT + " DESC";

        return db.rawQuery(sql, new String[]{String.valueOf(userId)});
    }

    public boolean restoreExerciseForUser(int exerciseId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        String sql =
                "UPDATE " + TABLE_EXERCISES + " " +
                        "SET " + COL_EX_IS_DELETED + " = 0, " + COL_EX_DELETED_AT + " = NULL " +
                        "WHERE " + COL_EX_ID + " = ? " +
                        "AND " + COL_EX_ROUTINE_ID + " IN (SELECT " + COL_ROUTINE_ID +
                        " FROM " + TABLE_ROUTINES +
                        " WHERE " + COL_ROUTINE_USER_ID + " = ?)";

        db.execSQL(sql, new Object[]{exerciseId, userId});

        Cursor c = null;
        try {
            c = db.rawQuery("SELECT changes()", null);
            if (c != null && c.moveToFirst()) return c.getInt(0) > 0;
        } finally {
            if (c != null) c.close();
        }
        return false;
    }

    public boolean hardDeleteExerciseForUser(int exerciseId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();

        String sql =
                "DELETE FROM " + TABLE_EXERCISES + " " +
                        "WHERE " + COL_EX_ID + " = ? " +
                        "AND " + COL_EX_ROUTINE_ID + " IN (SELECT " + COL_ROUTINE_ID +
                        " FROM " + TABLE_ROUTINES +
                        " WHERE " + COL_ROUTINE_USER_ID + " = ?)";

        db.execSQL(sql, new Object[]{exerciseId, userId});

        Cursor c = null;
        try {
            c = db.rawQuery("SELECT changes()", null);
            if (c != null && c.moveToFirst()) return c.getInt(0) > 0;
        } finally {
            if (c != null) c.close();
        }
        return false;
    }

    public Cursor getDeletedChecklistCursorForUser(int userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.query(
                TABLE_CHECKLIST,
                new String[]{COL_CHECK_ID, COL_CHECK_NAME, COL_CHECK_DELETED_AT},
                COL_CHECK_USER_ID + " = ? AND " + COL_CHECK_IS_DELETED + " = 1",
                new String[]{String.valueOf(userId)},
                null, null,
                COL_CHECK_DELETED_AT + " DESC"
        );
    }

    public int hardDeleteChecklistItem(int itemId, int userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(
                TABLE_CHECKLIST,
                COL_CHECK_ID + " = ? AND " + COL_CHECK_USER_ID + " = ?",
                new String[]{String.valueOf(itemId), String.valueOf(userId)}
        );
    }

    // helper
    private String safe(String s) {
        return s == null ? "" : s;
    }
}
