package com.example.masrouf1;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Seven destinations for personal and child accounts. */
public final class AppDrawer {
    public static final String[] DESTINATIONS = {"home", "products", "calendar", "analytics", "rewards", "notifications", "settings"};
    private static final int[] LABELS = {R.string.nav_home, R.string.nav_products, R.string.nav_calendar,
            R.string.nav_analytics, R.string.nav_rewards, R.string.nav_notifications, R.string.nav_settings};

    private AppDrawer() { }
    public static void populate(MainActivity activity, LinearLayout menu) {
        addItems(activity, menu, DESTINATIONS, LABELS);
    }
    static void addItems(MainActivity activity, LinearLayout menu, String[] destinations, int[] labels) {
        menu.removeAllViews();
        for (int i = 0; i < destinations.length; i++) {
            final String destination = destinations[i];
            TextView item = new TextView(activity);
            item.setText(labels[i]); item.setTextColor(activity.getColor(R.color.ink)); item.setTextSize(15f);
            item.setGravity(Gravity.CENTER_VERTICAL); item.setPaddingRelative(16, 14, 12, 14);
            item.setMinHeight((int) (52 * activity.getResources().getDisplayMetrics().density));
            item.setBackgroundResource(R.drawable.drawer_item_background);
            item.setOnClickListener(v -> activity.onDrawerDestination(destination));
            menu.addView(item, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
    }
}
