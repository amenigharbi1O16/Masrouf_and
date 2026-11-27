/*
 * ============================================================================
 * FICHIER : BudgetRolloverWorker.java
 * MISSION : Tâche de fond WorkManager exécutée quotidiennement pour clôturer
 *           automatiquement tout budget actif dont la date de fin periodEnd est
 *           dépassée, attribuer les points de fidélité en cas de respect du
 *           plafond et ouvrir la période mensuelle suivante.
 * IMPLÉMENTE : Sous-classe de androidx.work.Worker exécutant findExpiredBudgets()
 *              puis la transaction atomique rolloverBudget() sur MasroufDao,
 *              avec bascule multi-mois si l'appareil est resté éteint longtemps.
 * EFFET À L'ÉCRAN : Aucun directement — déclenche des notifications système
 *                   (budgetClosed et pointsAwarded) visibles dans la barre
 *                   d'état et NotificationsActivity.
 * COMMUNICATIONS :
 *   ← MasroufApplication.onCreate (planification périodique toutes les 24 h)
 *   → MasroufDatabase / MasroufDao (findExpiredBudgets, rolloverBudget)
 *   → BudgetNotificationManager (budgetClosed, pointsAwarded)
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

import java.util.Calendar;
import java.util.List;

/**
 * Worker WorkManager exécuté quotidiennement hors ligne pour clôturer les budgets expirés et reconduire le mois suivant.
 * Planifié dans MasroufApplication.onCreate() ; appelle MasroufDao.findExpiredBudgets() puis rolloverBudget(),
 * et déclenche les notifications locales budgetClosed / pointsAwarded via BudgetNotificationManager.
 */
public final class BudgetRolloverWorker extends Worker {
    public BudgetRolloverWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    /** Parcourt tous les budgets actifs expirés et crée de manière atomique la période mensuelle suivante. */
    @NonNull @Override public Result doWork() {
        try {
            long now = System.currentTimeMillis();
            MasroufDatabase database = MasroufDatabase.getInstance(getApplicationContext());
            // Recherche tous les budgets encore ACTIVE dont la date de fin periodEnd est dépassée.
            List<BudgetEntity> expired = database.masroufDao().findExpiredBudgets(now);
            for (BudgetEntity budget : expired) {
                Calendar start = Calendar.getInstance(); start.setTimeInMillis(budget.periodEnd);
                Calendar end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
                // Avance mois par mois si plusieurs périodes se sont écoulées sans ouverture de l'application.
                while (end.getTimeInMillis() <= now) {
                    start = end;
                    end = (Calendar) start.clone(); end.add(Calendar.MONTH, 1);
                }
                Context context=getApplicationContext();
                // Transaction atomique : clôture l'ancien budget, attribue éventuellement 100 points et crée la nouvelle période.
                boolean updated = database.masroufDao().rolloverBudget(
                        budget.id, start.getTimeInMillis(), end.getTimeInMillis(), now - 92L * 24L * 60L * 60L * 1000L,now,
                        context.getString(R.string.points_awarded_title),context.getString(R.string.points_awarded_message));
                if (updated) {
                    int notificationId=(int)(budget.id&0x7fffffff);
                    BudgetNotificationManager.budgetClosed(context,notificationId,budget.userId,budget.id,budget.periodStart);
                    // Notifie l'obtention de points uniquement si la limite budgétaire a été respectée.
                    if(budget.spentMilli<=budget.monthlyLimitMilli) BudgetNotificationManager.pointsAwarded(context,notificationId+1,"budget-close:"+budget.id+":"+budget.periodStart);
                }
            }
            return Result.success();
        } catch (Exception exception) {
            return Result.retry();
        }
    }
}
