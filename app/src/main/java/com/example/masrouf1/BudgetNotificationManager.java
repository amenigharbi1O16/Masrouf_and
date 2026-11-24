package com.example.masrouf1;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.NotificationEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local notifications backed by idempotent Room event keys. */
public final class BudgetNotificationManager {
    public static final String CHANNEL_ID="budget_channel";
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private BudgetNotificationManager(){}

    public static void createChannel(Context context){
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.O)return;
        NotificationChannel channel=new NotificationChannel(CHANNEL_ID,context.getString(R.string.budget_this_month),NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.budget_hint));channel.enableVibration(true);channel.setVibrationPattern(new long[]{0,220,120,220});
        NotificationManager manager=context.getSystemService(NotificationManager.class);if(manager!=null)manager.createNotificationChannel(channel);
    }

    public static void reportBudgetProgress(Context context,long spentMilli,long limitMilli,long userId,long budgetId,long periodStart){
        if(limitMilli<=0||userId<=0||budgetId<=0)return;
        boolean exceeded=spentMilli>limitMilli;boolean caution=!exceeded&&MoneyMath.atLeastPercent(spentMilli,limitMilli,80);if(!exceeded&&!caution)return;
        String type=exceeded?"over":"caution";String key="budget:"+budgetId+":"+periodStart+":"+type;
        int messageId=exceeded?R.string.notification_budget_over:R.string.notification_budget_caution;
        persistAndPost(context,userId,key,exceeded?"budget_over":"budget_caution",context.getString(messageId),
                (int)(budgetId&0x7fffffff),NotificationsActivity.class);
    }

    public static void budgetClosed(Context context,int notificationId,long userId,long budgetId,long periodStart){
        String key="budget-close:"+budgetId+":"+periodStart;String message=context.getString(R.string.notification_budget_closed);
        persistAndPost(context,userId,key,"budget_closed",message,notificationId,NotificationsActivity.class);
    }

    public static void pointsAwarded(Context context,int notificationId,String eventKey){
        postKnownEvent(context,notificationId,"points:"+eventKey,context.getString(R.string.points_awarded_title),context.getString(R.string.points_awarded_message),RewardsActivity.class);
    }

    public static void allowanceReceived(Context context,int notificationId,long childUserId,long amountMilli,String eventKey){
        if(childUserId<=0||amountMilli<=0||eventKey==null||eventKey.trim().isEmpty())return;
        postKnownEvent(context,notificationId,eventKey,context.getString(R.string.notification_gift_received),
                context.getString(R.string.allowance_received,CurrencyFormatter.formatMilli(amountMilli)),NotificationsActivity.class);
    }

    public static void moneyRequestReceived(Context context,int notificationId,String eventKey,String message){
        postKnownEvent(context,notificationId,eventKey,context.getString(R.string.money_request_notification_title),message,NotificationsActivity.class);
    }

    public static void moneyRequestResolved(Context context,int notificationId,String eventKey,String title,String message){
        postKnownEvent(context,notificationId,eventKey,title,message,NotificationsActivity.class);
    }

    private static void persistAndPost(Context context,long userId,String eventKey,String kind,String message,int id,Class<? extends Activity> destination){
        Context app=context.getApplicationContext();IO.execute(()->{
            try{NotificationEntity row=new NotificationEntity();row.userId=userId;row.title=app.getString(R.string.app_name);row.message=message;row.kind=kind;row.createdAt=System.currentTimeMillis();row.eventKey=eventKey;
                long inserted=MasroufDatabase.getInstance(app).masroufDao().insertNotificationOnce(row);if(inserted>0)postKnownEvent(app,id,eventKey,app.getString(R.string.app_name),message,destination);
            }catch(RuntimeException ignored){ }
        });
    }

    private static void postKnownEvent(Context context,int notificationId,String eventKey,String title,String message,Class<? extends Activity> destination){
        if(eventKey==null||eventKey.isEmpty())return;
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        String preferenceKey="native:"+eventKey;android.content.SharedPreferences prefs=context.getSharedPreferences("masrouf_notification_delivery",Context.MODE_PRIVATE);
        if(prefs.getBoolean(preferenceKey,false))return;createChannel(context);
        Intent intent=new Intent(context,destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending=PendingIntent.getActivity(context,notificationId,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification=new NotificationCompat.Builder(context,CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_leaf)
                .setContentTitle(title).setContentText(message).setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(new NotificationCompat.Builder(context,CHANNEL_ID).setSmallIcon(R.drawable.ic_notification_leaf).setContentTitle(context.getString(R.string.app_name)).setContentText(context.getString(R.string.notification_private_preview)).build())
                .setAutoCancel(true).setContentIntent(pending);
        try{NotificationManagerCompat.from(context).notify(notificationId,notification.build());prefs.edit().putBoolean(preferenceKey,true).apply();}
        catch(SecurityException ignored){ }
    }
}
