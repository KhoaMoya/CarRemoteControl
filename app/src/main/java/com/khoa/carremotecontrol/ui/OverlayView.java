package com.khoa.carremotecontrol.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import java.util.LinkedList;
import java.util.List;

public class OverlayView extends View {
    private final List<DrawCallback> callbacks = new LinkedList();

    public interface DrawCallback {
        void drawCallback(Canvas canvas);
    }

    public OverlayView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public void addCallback(DrawCallback drawCallback) {
        this.callbacks.add(drawCallback);
    }

    public synchronized void draw(Canvas canvas) {
        super.draw(canvas);
        for (DrawCallback drawCallback : this.callbacks) {
            drawCallback.drawCallback(canvas);
        }
    }
}
