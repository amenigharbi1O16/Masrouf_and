package com.example.masrouf1;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.masrouf.controller.SessionStore;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

/** Displays only notifications persisted for the signed-in account in Room. */
public final class NotificationsActivity extends SectionActivity {
    private LinearLayout rows;
    private MasroufViewModel viewModel;
    private long userId;
    @Override protected int titleResource(){return R.string.page_notifications;}
    @Override protected View createSectionContent(){
        rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);
        userId=SessionStore.userId(this);viewModel=new ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        viewModel.notifications().observe(this,state->{if(state.loading)return;rows.removeAllViews();List<NotificationEntity> notifications=state.data;if(state.failed||notifications==null||notifications.isEmpty()){rows.addView(bodyText(getString(R.string.notifications_empty)));return;}DateFormat date=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT);for(NotificationEntity item:notifications){TextView row=bodyText(item.title+"\n"+(item.message==null?"":item.message)+"\n"+date.format(new Date(item.createdAt)));row.setPadding(dp(12),dp(8),dp(12),dp(8));row.setTextColor(getColor(item.read?R.color.muted_green:R.color.forest));row.setOnClickListener(v->{if(!item.read)viewModel.markNotificationRead(item.id,userId);});LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.bottomMargin=dp(8);rows.addView(row,params);}});
        viewModel.loadNotifications(userId);return rows;
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
}
