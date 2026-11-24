package com.example.masrouf1;

import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.masrouf.controller.SessionStore;

/** Seven family destinations with count badges and native vector navigation icons. */
public final class AdminDrawer {
    public static final String[] DESTINATIONS={"home","children","products","calendar","analytics","notifications","settings"};
    private static final int[] LABELS={R.string.nav_home,R.string.nav_children,R.string.nav_products,R.string.nav_calendar_history,R.string.nav_budget_analytics,R.string.nav_notifications,R.string.nav_settings};
    private static final int[] ICONS={R.drawable.ic_home,R.drawable.ic_people,R.drawable.ic_bag,R.drawable.ic_calendar,R.drawable.ic_pie_chart,R.drawable.ic_notifications,R.drawable.ic_settings};
    private AdminDrawer() { }

    public static void populate(MainActivity activity,LinearLayout menu) {
        menu.removeAllViews(); TextView childrenBadge=null,notificationsBadge=null;
        for(int i=0;i<DESTINATIONS.length;i++) {
            String destination=DESTINATIONS[i];
            LinearLayout row=new LinearLayout(activity); row.setGravity(Gravity.CENTER_VERTICAL); row.setPaddingRelative(dp(activity,13),0,dp(activity,10),0);
            row.setMinimumHeight(dp(activity,52)); row.setBackgroundResource(R.drawable.drawer_item_background);
            ImageView icon=new ImageView(activity); icon.setImageResource(ICONS[i]); icon.setColorFilter(activity.getColor(R.color.mint));
            row.addView(icon,new LinearLayout.LayoutParams(dp(activity,23),dp(activity,23)));
            TextView label=new TextView(activity); label.setText(LABELS[i]); label.setTextColor(activity.getColor(R.color.alabaster)); label.setTextSize(14.5f);
            LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(0,-2,1); labelParams.setMarginStart(dp(activity,13)); row.addView(label,labelParams);
            TextView badge=new TextView(activity); badge.setGravity(Gravity.CENTER); badge.setTextColor(activity.getColor(R.color.forest)); badge.setTextSize(10); badge.setTypeface(null,android.graphics.Typeface.BOLD);
            badge.setPadding(dp(activity,7),dp(activity,3),dp(activity,7),dp(activity,3)); badge.setBackgroundResource(R.drawable.badge_background); badge.setVisibility(android.view.View.GONE);
            row.addView(badge,new LinearLayout.LayoutParams(-2,-2));
            if("children".equals(destination)) childrenBadge=badge;
            if("notifications".equals(destination)) notificationsBadge=badge;
            row.setOnClickListener(v->activity.onDrawerDestination(destination));
            LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2); rowParams.bottomMargin=dp(activity,3); menu.addView(row,rowParams);
        }
        TextView childCount=childrenBadge,noticeCount=notificationsBadge;
        long userId=SessionStore.userId(activity);
        MasroufViewModel viewModel=new androidx.lifecycle.ViewModelProvider(activity,new MasroufViewModelFactory(activity)).get(MasroufViewModel.class);
        viewModel.<DrawerBadgeData>request(repository->repository.parentDrawerBadges(userId),state->{
            if(activity.isFinishing()||activity.isDestroyed()||state.data==null) return;
            if(childCount!=null&&state.data.children>0) { childCount.setText(String.valueOf(state.data.children)); childCount.setVisibility(android.view.View.VISIBLE); }
            if(noticeCount!=null&&state.data.unreadNotifications>0) { noticeCount.setText(String.valueOf(state.data.unreadNotifications)); noticeCount.setVisibility(android.view.View.VISIBLE); }
        });
    }
    private static int dp(MainActivity activity,int value) { return (int)(value*activity.getResources().getDisplayMetrics().density+.5f); }
}
