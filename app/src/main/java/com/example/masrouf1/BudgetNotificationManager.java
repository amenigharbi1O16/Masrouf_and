package com.example.masrouf1;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.media.AudioAttributes;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BudgetNotificationManager {
    public static final String CHANNEL_ID = "budget_channel";
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private BudgetNotificationManager() { }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, context.getString(R.string.budget_this_month), NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(context.getString(R.string.budget_hint));
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0,220,120,220});
            Uri sound=Uri.parse("android.resource://"+context.getPackageName()+"/"+R.raw.achievement);
            AudioAttributes attributes=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
            channel.setSound(sound,attributes);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    public static void reportBudgetProgress(Context context, long spentMilli, long limitMilli, int notificationId) {
        if (limitMilli <= 0) return;
        if (spentMilli >= limitMilli) post(context, notificationId, R.string.notification_budget_over, MainActivity.class);
        else if (MoneyMath.atLeastPercent(spentMilli,limitMilli,80)) post(context, notificationId, R.string.notification_budget_caution, MainActivity.class);
    }

    public static void budgetClosed(Context context, int notificationId) {
        post(context, notificationId, R.string.notification_budget_closed, MainActivity.class);
    }

    public static void pointsAwarded(Context context,int notificationId) {
        post(context,notificationId,R.string.points_awarded_message,RewardsActivity.class);
    }

    public static void giftReceived(Context context, int notificationId) {
        post(context, notificationId, R.string.notification_gift_received, RewardsActivity.class);
    }

    public static void allowanceReceived(Context context,int notificationId,long childUserId,long amountMilli) {
        if(childUserId<=0||amountMilli<=0)return;
        createChannel(context);
        String message=context.getString(R.string.allowance_received,CurrencyFormatter.formatMilli(amountMilli));
        String title=context.getString(R.string.notification_gift_received);Context app=context.getApplicationContext();
        IO.execute(()->{try{NotificationEntity row=new NotificationEntity();row.userId=childUserId;row.title=title;row.message=message;row.kind="gift_received";row.createdAt=System.currentTimeMillis();row.read=false;MasroufDatabase.getInstance(app).masroufDao().insert(row);}catch(RuntimeException ignored){}});
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return;
        Intent intent=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending=PendingIntent.getActivity(context,notificationId,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification=new NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_leaf).setContentTitle(context.getString(R.string.app_name))
                .setContentText(message).setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH).setVibrate(new long[]{0,220,120,220}).setAutoCancel(true).setContentIntent(pending);
        Uri sound=Uri.parse("android.resource://"+context.getPackageName()+"/"+R.raw.achievement);
        notification.setSound(sound);
        try { NotificationManagerCompat.from(context).notify(notificationId,notification.build()); }
        catch(SecurityException ignored) { }
    }

    private static void post(Context context, int notificationId, int messageId, Class<? extends Activity> destination) {
        createChannel(context);
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        Intent intent = new Intent(context, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if(destination==MainActivity.class)intent.putExtra(MainActivity.EXTRA_CENTRAL_TAB,"analytics");
        PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_leaf)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(messageId))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(context.getString(messageId)))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);
        try { NotificationManagerCompat.from(context).notify(notificationId, notification.build()); }
        catch (SecurityException ignored) { /* Permission can be revoked while work is running. */ }
    }
}
