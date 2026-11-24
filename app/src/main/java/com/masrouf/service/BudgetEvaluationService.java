package com.masrouf.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.masrouf1.BudgetNotificationManager;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Monthly budget evaluator. Work is offline and idempotent with the daily WorkManager worker. */
public final class BudgetEvaluationService extends Service {
    public static final String ACTION_EVALUATE="com.masrouf.action.EVALUATE_BUDGETS";
    private static final String SERVICE_CHANNEL="budget_evaluation_service";
    private static final int SERVICE_NOTIFICATION_ID=8817;
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();

    public static void scheduleNextMonth(Context context) {
        Calendar next=Calendar.getInstance(); next.set(Calendar.DAY_OF_MONTH,1); next.add(Calendar.MONTH,1);
        next.set(Calendar.HOUR_OF_DAY,0); next.set(Calendar.MINUTE,10); next.set(Calendar.SECOND,0); next.set(Calendar.MILLISECOND,0);
        Intent intent=new Intent(context,BudgetEvaluationService.class).setAction(ACTION_EVALUATE);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
        PendingIntent operation=Build.VERSION.SDK_INT>=26?PendingIntent.getForegroundService(context,8818,intent,flags):PendingIntent.getService(context,8818,intent,flags);
        AlarmManager alarm=(AlarmManager)context.getSystemService(ALARM_SERVICE);
        if(alarm!=null) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),operation);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        createServiceChannel();
        startForeground(SERVICE_NOTIFICATION_ID,serviceNotification());
        scheduleNextMonth(this);
        IO.execute(()->evaluate(startId));
        return START_NOT_STICKY;
    }

    private void evaluate(int startId) {
        try {
            long now=System.currentTimeMillis();
            MasroufDatabase database=MasroufDatabase.getInstance(getApplicationContext());
            List<BudgetEntity> expired=database.masroufDao().findExpiredBudgets(now);
            for(BudgetEntity budget:expired) {
                Calendar start=Calendar.getInstance(); start.setTimeInMillis(budget.periodEnd+1);
                Calendar end=(Calendar)start.clone(); end.add(Calendar.MONTH,1);
                while(end.getTimeInMillis()<=now) { start=end; end=(Calendar)start.clone(); end.add(Calendar.MONTH,1); }
                boolean respect=budget.spentMilli<=budget.monthlyLimitMilli;
                boolean updated=database.masroufDao().rolloverBudget(budget.id,start.getTimeInMillis(),end.getTimeInMillis(),now-120L*24L*60L*60L*1000L);
                if(updated) {
                    int notificationId=(int)(budget.id&0x7fffffff);
                    BudgetNotificationManager.budgetClosed(getApplicationContext(),notificationId);
                    if(respect) BudgetNotificationManager.pointsAwarded(getApplicationContext(),notificationId+1);
                }
            }
        } catch(RuntimeException ignored) { }
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelfResult(startId);
    }

    private void createServiceChannel() {
        if(Build.VERSION.SDK_INT<26) return;
        NotificationChannel channel=new NotificationChannel(SERVICE_CHANNEL,getString(R.string.budget_evaluation_title),NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(getString(R.string.budget_evaluation_running));
        NotificationManager manager=getSystemService(NotificationManager.class); if(manager!=null) manager.createNotificationChannel(channel);
    }

    private Notification serviceNotification() {
        return new NotificationCompat.Builder(this,SERVICE_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_leaf)
                .setContentTitle(getString(R.string.budget_evaluation_title))
                .setContentText(getString(R.string.budget_evaluation_running))
                .setOngoing(true).setOnlyAlertOnce(true).setSilent(true).build();
    }

    @Nullable @Override public IBinder onBind(Intent intent) { return null; }
}
