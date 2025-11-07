package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class DonutProgressView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    private int progress = 0;        // 0..100
    private float strokeWidth = 22f; // dp ηταν 22 πριν
    private int trackColor = 0xFFD1D5DB;    // γκρι
    private int progressColor = 0xFF3B82F6; // μπλε

    public DonutProgressView(Context c) { super(c); init(); }
    public DonutProgressView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public DonutProgressView(Context c, @Nullable AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        setLayerType(LAYER_TYPE_HARDWARE, null);
        updatePaints();
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private void updatePaints() {
        trackPaint.setStrokeWidth(dp(strokeWidth));
        trackPaint.setColor(trackColor);
        progressPaint.setStrokeWidth(dp(strokeWidth));
        progressPaint.setColor(progressColor);
        invalidate();
    }

    public void setProgress(int pct) { // 0..100
        progress = Math.max(0, Math.min(100, pct));
        invalidate();
    }
    public void setTrackColor(int c) { trackColor = c; updatePaints(); }
    public void setProgressColor(int c) { progressColor = c; updatePaints(); }
    public void setStrokeWidthDp(float w) { strokeWidth = w; updatePaints(); }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        float pad = dp(strokeWidth) / 2f + dp(1);
        oval.set(pad, pad, w - pad, h - pad);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // υπόλοιπο κύκλου
        canvas.drawArc(oval, 0, 360, false, trackPaint);
        // progress από την κορυφή (-90°)
        float sweep = 360f * progress / 100f;
        canvas.drawArc(oval, -90, sweep, false, progressPaint);
    }
}
