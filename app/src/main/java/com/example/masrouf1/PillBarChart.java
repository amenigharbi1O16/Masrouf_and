package com.example.masrouf1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Compact rounded bars used for category spending in the dashboard. */
public final class PillBarChart extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Long> values = new ArrayList<>();

    public PillBarChart(Context context) { super(context); }
    public PillBarChart(Context context, AttributeSet attrs) { super(context, attrs); }
    public void setMilliValues(List<Long> amounts) { values.clear(); if(amounts!=null) values.addAll(amounts); invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(values.isEmpty()) return;
        float slot = getWidth() / (values.size() * 2f);
        float maxWidth = Math.min(14f, slot * 0.48f);
        float top = 16f, bottom = getHeight() - 12f, trackHeight = bottom - top;
        long maximum = 0; for(long amount:values) maximum=Math.max(maximum,amount);
        paint.setColor(getResources().getColor(R.color.mint, getContext().getTheme()));
        for (int i = 0; i < values.size(); i++) {
            float center = slot * (i * 2 + 1);
            RectF track = new RectF(center - maxWidth / 2f, top, center + maxWidth / 2f, bottom);
            canvas.drawRoundRect(track, maxWidth, maxWidth, paint);
            paint.setColor(getResources().getColor(R.color.emerald, getContext().getTheme()));
            float fraction=maximum<=0?0f:Math.max(0f,Math.min(1f,values.get(i)/(float)maximum));
            float height = fraction==0f?0f:Math.max(8f, trackHeight * fraction);
            RectF bar = new RectF(center - maxWidth / 2f, bottom - height, center + maxWidth / 2f, bottom);
            canvas.drawRoundRect(bar, maxWidth, maxWidth, paint);
            paint.setColor(getResources().getColor(R.color.mint, getContext().getTheme()));
        }
    }
}
