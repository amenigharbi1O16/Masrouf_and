package com.example.masrouf1;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

import java.util.Calendar;
import java.util.List;

/** Daily offline check that closes expired periods and prepares the next budget month. */
public final class BudgetRolloverWorker extends Worker {
    public BudgetRolloverWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        try {
            long now = System.currentTimeMillis();
            MasroufDatabase database = MasroufDatabase.getInstance(getApplicationContext());
            List<BudgetEntity> expired = database.masroufDao().findExpiredBudgets(now);
            for (BudgetEntity budget : expired) {
                Calendar start = Calendar.getInstance(); start.setTimeInMillis(budget.periodEnd);
                Calendar end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
                while (end.getTimeInMillis() <= now) {
                    start = end;
                    end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
                }
                boolean updated = database.masroufDao().rolloverBudget(
                        budget.id, start.getTimeInMillis(), end.getTimeInMillis(), now - 92L * 24L * 60L * 60L * 1000L);
                if (updated) {
                    int notificationId=(int)(budget.id&0x7fffffff);
                    BudgetNotificationManager.budgetClosed(getApplicationContext(),notificationId);
                    if(budget.spentMilli<=budget.monthlyLimitMilli) { BudgetNotificationManager.pointsAwarded(getApplicationContext(),notificationId+1); AchievementPlayer.play(getApplicationContext()); }
                }
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }
}
