package com.example.facialscanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class FaceOverlayView extends View {
    private Rect faceBounds;
    private Paint paint;
    private Status status = Status.NONE;

    public enum Status { NONE, REGISTERING, MATCHED, UNKNOWN }

    public FaceOverlayView(Context context) { super(context); init(); }
    public FaceOverlayView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6f);
    }

    public void setFace(Rect bounds, Status status) {
        this.faceBounds = bounds;
        this.status = status;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (faceBounds != null) {
            switch (status) {
                case REGISTERING: paint.setColor(Color.BLUE); break;
                case MATCHED:     paint.setColor(Color.GREEN); break;
                case UNKNOWN:     paint.setColor(Color.RED); break;
                default:          paint.setColor(Color.TRANSPARENT);
            }
            canvas.drawRect(faceBounds, paint);
        }
    }
}
