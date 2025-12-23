package com.oshing.fitlife.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.List;

public class ShareUtils {

    //  CHECKLIST SHARE MESSAGE
    public static String buildChecklistHelpMessage(List<String> pendingItems) {
        if (pendingItems == null || pendingItems.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Hey! Can you help me bring these items for exercise?\n\n");

        for (String item : pendingItems) {
            if (item != null && !item.trim().isEmpty()) {
                sb.append("• ").append(item.trim()).append("\n");
            }
        }

        sb.append("\nThanks 🙏");
        return sb.toString();
    }

    //  ROUTINE SHARE MESSAGE
    public static String buildRoutineHelpMessage(List<String> pendingRoutines) {
        if (pendingRoutines == null || pendingRoutines.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("Hey! Can you help me with my exercise routine?\n\n");
        sb.append("I still need to complete:\n\n");

        for (String routine : pendingRoutines) {
            if (routine != null && !routine.trim().isEmpty()) {
                sb.append("• ").append(routine.trim()).append("\n");
            }
        }

        sb.append("\nThanks 🙏");
        return sb.toString();
    }

    //  SHARE VIA ANY APP
    public static void shareToAnyApp(Context context, String message) {
        if (context == null || message == null || message.trim().isEmpty()) return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, message);

        context.startActivity(Intent.createChooser(intent, "Send using"));
    }

    //  SHARE VIA SMS
    public static void shareViaSms(Context context, String phoneNumber, String message) {
        if (context == null) return;
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return;
        if (message == null || message.trim().isEmpty()) return;

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("smsto:" + Uri.encode(phoneNumber.trim())));
        intent.putExtra("sms_body", message);

        context.startActivity(intent);
    }
}
