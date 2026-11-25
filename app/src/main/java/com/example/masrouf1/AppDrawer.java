package com.example.masrouf1;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.masrouf.controller.SessionStore;

/**
 * COMPOSANT : AppDrawer
 * RÔLE : Construit le tiroir latéral pour les comptes personnels et enfants.
 * Affiche les 7 destinations avec icônes vectorielles, badge de notifications non lues
 * et mise en évidence de la destination active, aligné sur AppDrawer.tsx.
 */
public final class AppDrawer {
    public static final String[] DESTINATIONS = {"home", "products", "calendar", "analytics", "rewards", "notifications", "settings"};
    private static final int[] LABELS = {R.string.nav_home, R.string.nav_products, R.string.nav_calendar,
            R.string.nav_analytics, R.string.nav_rewards, R.string.nav_notifications, R.string.nav_settings};
    private static final int[] ICONS = {R.drawable.ic_home, R.drawable.ic_bag, R.drawable.ic_calendar,
            R.drawable.ic_pie_chart, R.drawable.ic_trophy, R.drawable.ic_notifications, R.drawable.ic_settings};

    private AppDrawer() { }

    public static void populate(MainActivity activity, LinearLayout menu) {
        populate(activity, menu, "home");
    }

    public static void populate(MainActivity activity, LinearLayout menu, String activeDestination) {
        addItems(activity, menu, DESTINATIONS, LABELS, ICONS, activeDestination);
    }

    static void addItems(MainActivity activity, LinearLayout menu, String[] destinations, int[] labels, int[] icons) {
        addItems(activity, menu, destinations, labels, icons, "home");
    }

    static void addItems(MainActivity activity, LinearLayout menu, String[] destinations, int[] labels, int[] icons, String activeDestination) {
        menu.removeAllViews();
        TextView notificationsBadge = null;

        for (int i = 0; i < destinations.length; i++) {
            final String destination = destinations[i];

            LinearLayout row = new LinearLayout(activity);
            row.setTag(destination);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPaddingRelative(dp(activity, 13), 0, dp(activity, 10), 0);
            row.setMinimumHeight(dp(activity, 50));

            ImageView icon = new ImageView(activity);
            icon.setImageResource(icons[i]);
            icon.setColorFilter(activity.getColor(R.color.forest));
            row.addView(icon, new LinearLayout.LayoutParams(dp(activity, 22), dp(activity, 22)));

            TextView label = new TextView(activity);
            label.setText(labels[i]);
            label.setTextColor(activity.getColor(R.color.ink));
            label.setTextSize(14.5f);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
            labelParams.setMarginStart(dp(activity, 13));
            row.addView(label, labelParams);

            TextView badge = new TextView(activity);
            badge.setGravity(Gravity.CENTER);
            badge.setTextColor(activity.getColor(R.color.alabaster));
            badge.setTextSize(10f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(dp(activity, 7), dp(activity, 3), dp(activity, 7), dp(activity, 3));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(activity.getColor(R.color.emerald));
            badgeBg.setCornerRadius(dp(activity, 12));
            badge.setBackground(badgeBg);
            badge.setVisibility(View.GONE);
            row.addView(badge, new LinearLayout.LayoutParams(-2, -2));
            if ("notifications".equals(destination)) {
                notificationsBadge = badge;
            }

            row.setOnClickListener(v -> activity.onDrawerDestination(destination));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(activity, 3);
            menu.addView(row, rowParams);
        }

        updateSelection(menu, activeDestination);

        // Chargement asynchrone du compteur de notifications non lues (badge)
        final TextView noticeBadgeView = notificationsBadge;
        long userId = SessionStore.userId(activity);
        if (noticeBadgeView != null && userId > 0L) {
            MasroufViewModel viewModel = new androidx.lifecycle.ViewModelProvider(
                    activity, new MasroufViewModelFactory(activity)).get(MasroufViewModel.class);
            viewModel.<Integer>request(repository -> repository.unreadNotificationCount(userId), state -> {
                if (activity.isFinishing() || activity.isDestroyed() || state.data == null) return;
                if (state.data > 0) {
                    noticeBadgeView.setText(String.valueOf(state.data));
                    noticeBadgeView.setVisibility(View.VISIBLE);
                } else {
                    noticeBadgeView.setVisibility(View.GONE);
                }
            });
        }
    }

    /** Met à jour visuellement la ligne correspondant à l'onglet actif. */
    public static void updateSelection(LinearLayout menu, String activeDestination) {
        if (menu == null) return;
        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            boolean active = activeDestination != null && activeDestination.equals(row.getTag());
            if (active) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(menu.getContext().getColor(R.color.mint));
                bg.setCornerRadius(dp(menu, 14));
                row.setBackground(bg);
            } else {
                row.setBackgroundResource(R.drawable.drawer_item_background);
            }
            if (row.getChildCount() >= 2 && row.getChildAt(0) instanceof ImageView && row.getChildAt(1) instanceof TextView) {
                ((ImageView) row.getChildAt(0)).setColorFilter(
                        menu.getContext().getColor(active ? R.color.emerald : R.color.forest));
                TextView label = (TextView) row.getChildAt(1);
                label.setTextColor(menu.getContext().getColor(active ? R.color.forest : R.color.ink));
                label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }

    /** Surcharge de compatibilité pour l'appel sans icônes. */
    static void addItems(MainActivity activity, LinearLayout menu, String[] destinations, int[] labels) {
        addItems(activity, menu, destinations, labels, ICONS, "home");
    }

    private static int dp(MainActivity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + .5f);
    }

    private static int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + .5f);
    }
}
