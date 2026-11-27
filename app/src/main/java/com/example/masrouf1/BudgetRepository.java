/*
 * ============================================================================
 * FICHIER : BudgetRepository.java
 * MISSION : Ancienne façade d'écriture directe d'une dépense conservée dans le package principal.
 * IMPLÉMENTE : Exécuteur IO + notification différée via BudgetNotificationManager + Handler UI.
 * EFFET À L'ÉCRAN : Aucun affichage direct ; retourne true/false au callback sur le thread principal.
 * COMMUNICATIONS : Utilise MasroufDatabase.getInstance(app).masroufDao().recordExpense(...) ;
 *                  les écrans actuels utilisent plutôt MasroufRepository via MasroufViewModel.
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Façade historique asynchrone conservée dans com.example.masrouf1 pour enregistrer une dépense atomiquement.
 */
public final class BudgetRepository {
    // Exécuteur mono-thread empêchant toute écriture SQLite sur le thread principal Android.
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private BudgetRepository() { }

    /** Rappel retournant true si l'écriture Room s'est effectuée sans exception. */
    public interface Result { void complete(boolean saved); }

    /** Enregistre une dépense sans callback de résultat UI. */
    public static void recordExpense(Context context, TransactionEntity transaction) {
        recordExpense(context, transaction, saved -> { });
    }

    /**
     * Insère transaction dans SQLite via MasroufDao.recordExpense puis déclenche les alertes seuils,
     * et reposte le booléen de succès sur le thread UI (Looper.getMainLooper()).
     */
    public static void recordExpense(Context context, TransactionEntity transaction, Result callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            boolean saved=false;
            try {
                // Ouvre la base Room singleton et exécute l'insertion transactionnelle.
                MasroufDatabase database = MasroufDatabase.getInstance(app);
                database.masroufDao().recordExpense(transaction);
                // Relit le budget mis à jour et vérifie les seuils de notifications.
                BudgetEntity budget = database.masroufDao().findBudget(transaction.budgetId);
                if (budget != null) BudgetNotificationManager.reportBudgetProgress(app,
                        budget.spentMilli, budget.monthlyLimitMilli,transaction.userId,budget.id,budget.periodStart);
                saved=true;
            } catch(RuntimeException ignored) { }
            // Renvoie le statut final sur le thread principal pour mettre à jour l'interface appelante.
            boolean result=saved;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }
}
