package com.qwibbler.salarm.salah.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class DraggableFloatingActionButton extends FloatingActionButton implements View.OnTouchListener {

    private float dX, dY;
    private float downRawX, downRawY;
    private boolean isDragging;
    private final int touchSlop;

    public DraggableFloatingActionButton(@NonNull Context context) {
        this(context, null);
    }

    public DraggableFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DraggableFloatingActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        setOnTouchListener(this);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        ViewParent parentView = v.getParent();
        if (parentView == null) return false;

        View parent = (View) parentView;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dX = v.getX() - event.getRawX();
                dY = v.getY() - event.getRawY();
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                isDragging = false;
                parentView.requestDisallowInterceptTouchEvent(true);
                return true;

            case MotionEvent.ACTION_MOVE:
                float distance = (float) Math.hypot(event.getRawX() - downRawX, event.getRawY() - downRawY);
                if (distance > touchSlop) {
                    isDragging = true;
                }
                if (isDragging) {
                    parentView.requestDisallowInterceptTouchEvent(true);

                    float currX = event.getRawX() + dX;
                    float currY = event.getRawY() + dY;

                    float maxX = parent.getWidth() - v.getWidth();
                    float maxY = parent.getHeight() - v.getHeight();

                    float clampedX = Math.max(0, Math.min(currX, maxX));
                    float clampedY = Math.max(0, Math.min(currY, maxY));

                    v.setX(clampedX);
                    v.setY(clampedY);
                }
                return true;

            case MotionEvent.ACTION_UP:
                parentView.requestDisallowInterceptTouchEvent(false);
                if (!isDragging) {
                    performClick();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                parentView.requestDisallowInterceptTouchEvent(false);
                return true;

            default:
                return false;
        }
    }
}
