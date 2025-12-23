package com.oshing.fitlife.ui.exercises;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class SwipeGestureDetector implements View.OnTouchListener {

    public interface OnSwipeListener {
        void onSwipeLeft(MotionEvent e2);
        void onSwipeRight(MotionEvent e2);
    }

    private final GestureDetector detector;

    public SwipeGestureDetector(Context context, OnSwipeListener listener) {
        detector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {

            private static final int SWIPE_THRESHOLD = 120;
            private static final int SWIPE_VELOCITY_THRESHOLD = 120;

            @Override
            public boolean onDown(MotionEvent e) {
                // important: continue to receive events
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null || listener == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) listener.onSwipeRight(e2);
                        else listener.onSwipeLeft(e2);
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return detector.onTouchEvent(event);
    }
}
