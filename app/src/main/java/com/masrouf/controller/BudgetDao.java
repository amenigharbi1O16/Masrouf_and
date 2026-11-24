package com.masrouf.controller;

import android.content.Context;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Asynchronous MVC data-access facade over Room's atomic budget transactions. */
public final class BudgetDao {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    public interface Result<T> { void complete(T value); }
    private final MasroufDatabase database;
    public BudgetDao(Context context) { database=MasroufDatabase.getInstance(context.getApplicationContext()); }

    public void insertBudgetWithEnvelopes(BudgetEntity budget,List<BudgetLineEntity> envelopes,Result<Long> callback) {
        IO.execute(()->{
            long id=-1L; try { id=database.masroufDao().insertBudgetWithEnvelopes(budget,envelopes); } catch(RuntimeException ignored) { }
            long result=id; new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }
    public void updateBudgetWithEnvelopes(BudgetEntity budget,List<BudgetLineEntity> envelopes,Result<Boolean> callback) {
        IO.execute(()->{
            boolean saved=false; try { saved=database.masroufDao().updateBudgetWithEnvelopes(budget,envelopes); } catch(RuntimeException ignored) { }
            boolean result=saved; new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }
    public void replaceChildBudgetEnvelopes(long budgetId,long childUserId,List<BudgetLineEntity> envelopes,Result<Boolean> callback) {
        IO.execute(()->{
            boolean saved=false; try { saved=database.masroufDao().replaceChildBudgetEnvelopes(budgetId,childUserId,envelopes); } catch(RuntimeException ignored) { }
            boolean result=saved; new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }
}
