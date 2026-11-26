package com.example.masrouf1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 6-month rounded pill bar chart matching the React Native PillBarChart specification:
 * subtle rounded resting pills when 0, dark forest pill fill proportional to maximum spend
 * (coral red when monthly spend exceeds that month's allocated budget), and month axis labels.
 */
public final class PillBarChart extends View {
    public static final class BarItem {
        public final String monthKey;
        public final String shortLabel;
        public final long spentMilli;
        public final long allocatedMilli;
        public final boolean isCurrent;

        public BarItem(String monthKey, String shortLabel, long spentMilli, long allocatedMilli, boolean isCurrent) {
            this.monthKey = monthKey;
            this.shortLabel = shortLabel == null ? "" : shortLabel;
            this.spentMilli = Math.max(0L, spentMilli);
            this.allocatedMilli = Math.max(0L, allocatedMilli);
            this.isCurrent = isCurrent;
        }
    }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<BarItem> items = new ArrayList<>();
    private final float density;
    private final int forest, coral, muted, emptyPillColor;

    public PillBarChart(Context context) {
        this(context, null);
    }

    public PillBarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        forest = context.getColor(R.color.forest);
        coral = context.getColor(R.color.coral);
        muted = context.getColor(R.color.muted_green);
        emptyPillColor = Color.rgb(203, 212, 207);
    }

    public void setBarItems(List<BarItem> barItems) {
        items.clear();
        if (barItems != null) items.addAll(barItems);
        requestLayout();
        invalidate();
    }

    /** Backwards-compatible numeric series setter. */
    public void setMilliValues(List<Long> amounts) {
        items.clear();
        if (amounts != null) {
            for (int i = 0; i < amounts.size(); i++) {
                long v = amounts.get(i) == null ? 0L : amounts.get(i);
                items.add(new BarItem(String.valueOf(i), "", v, 0L, i == amounts.size() - 1));
            }
        }
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int desiredHeight = (int) (182 * density);
        setMeasuredDimension(width, resolveSize(desiredHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items.isEmpty()) return;

        float left = getPaddingLeft() + 8f * density;
        float right = getWidth() - getPaddingRight() - 8f * density;
        float labelSpace = hasLabels() ? 26f * density : 8f * density;
        float top = getPaddingTop() + 14f * density;
        float bottom = getHeight() - getPaddingBottom() - labelSpace;
        float maxBarHeight = Math.max(24f * density, bottom - top);

        long maximum = 0L;
        for (BarItem item : items) {
            maximum = Math.max(maximum, item.spentMilli);
        }

        float slot = (right - left) / items.size();
        float barWidth = Math.min(26f * density, slot * 0.46f);
        float zeroPillHeight = Math.min(8f * density, maxBarHeight * 0.1f);
        float minNonZeroHeight = Math.min(22f * density, maxBarHeight * 0.18f);

        for (int i = 0; i < items.size(); i++) {
            BarItem item = items.get(i);
            float center = left + slot * (i + 0.5f);
            float barLeft = center - barWidth / 2f;
            float barRight = center + barWidth / 2f;
            float radius = barWidth / 2f;

            if (item.spentMilli <= 0L) {
                paint.setColor(emptyPillColor);
                RectF emptyPill = new RectF(barLeft, bottom - zeroPillHeight, barRight, bottom);
                canvas.drawRoundRect(emptyPill, zeroPillHeight / 2f, zeroPillHeight / 2f, paint);
            } else {
                float fraction = maximum <= 0L ? 0f : Math.max(0f, Math.min(1f, item.spentMilli / (float) maximum));
                float height = Math.max(minNonZeroHeight, maxBarHeight * fraction);
                boolean exceeded = item.allocatedMilli > 0L && item.spentMilli > item.allocatedMilli;
                paint.setColor(exceeded ? coral : forest);
                RectF bar = new RectF(barLeft, bottom - height, barRight, bottom);
                canvas.drawRoundRect(bar, radius, radius, paint);
            }

            if (!item.shortLabel.isEmpty()) {
                paint.setColor(item.isCurrent ? forest : muted);
                paint.setTextSize(11.5f * density);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, item.isCurrent ? Typeface.BOLD : Typeface.NORMAL));
                float labelWidth = paint.measureText(item.shortLabel);
                canvas.drawText(item.shortLabel, center - labelWidth / 2f, bottom + 18f * density, paint);
            }
        }
    }

    private boolean hasLabels() {
        for (BarItem item : items) {
            if (!item.shortLabel.isEmpty()) return true;
        }
        return false;
    }
}
