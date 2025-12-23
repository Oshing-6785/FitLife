package com.oshing.fitlife.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "FitLifePrefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_LOGIN_TIME = "login_time";

    private static final long SESSION_DURATION = 24 * 60 * 60 * 1000L; // 24 hours

    public static void saveLoginSession(Context context, int userId, String email) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();

        prefs.edit()
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .putLong(KEY_LOGIN_TIME, now)
                .apply();
    }

    public static boolean isSessionValid(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        int userId = prefs.getInt(KEY_USER_ID, -1);
        long loginTime = prefs.getLong(KEY_LOGIN_TIME, -1);

        if (userId == -1 || loginTime == -1) {
            return false; // No session
        }

        long now = System.currentTimeMillis();

        // clear session
        if (now - loginTime > SESSION_DURATION) {
            clearSession(context);
            return false;
        }

        return true;
    }

    public static void clearSession(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    public static int getUserId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_USER_ID, -1);
    }

    public static String getUserEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USER_EMAIL, null);
    }
}
