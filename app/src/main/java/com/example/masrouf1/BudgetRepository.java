package com.example.masrouf1;

import android.content.Context;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Background entry point for atomic expense writes and threshold notifications. */
public final class BudgetRepository {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private BudgetRepository() { }

    public interface Result { void complete(boolean saved); }

    public static void recordExpense(Context context, TransactionEntity transaction) {
        recordExpense(context, transaction, saved -> { });
    }

    public static void recordExpense(Context context, TransactionEntity transaction, Result callback) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            boolean saved=false;
            try {
                MasroufDatabase database = MasroufDatabase.getInstance(app);
                database.masroufDao().recordExpense(transaction);
                BudgetEntity budget = database.masroufDao().findBudget(transaction.budgetId);
                if (budget != null) BudgetNotificationManager.reportBudgetProgress(app,
                        budget.spentMilli, budget.monthlyLimitMilli, (int) (budget.id & 0x7fffffff));
                saved=true;
            } catch(RuntimeException ignored) { }
            boolean result=saved;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }
}
