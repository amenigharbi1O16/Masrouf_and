package com.example.masrouf1;

import android.os.SystemClock;
import android.view.View;

/** Explicit click listener that prevents accidental double submissions. */
public abstract class SafeClickListener implements View.OnClickListener {
    private static final long DEFAULT_INTERVAL_MS = 650L;
    private final long intervalMs;
    private long lastClickAt;

    protected SafeClickListener() { this(DEFAULT_INTERVAL_MS); }

    protected SafeClickListener(long intervalMs) { this.intervalMs = intervalMs; }

    @Override public final void onClick(View view) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastClickAt < intervalMs) return;
        lastClickAt = now;
        onSafeClick(view);
    }

    protected abstract void onSafeClick(View view);
}
