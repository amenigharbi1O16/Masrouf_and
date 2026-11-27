/*
 * ============================================================================
 * FICHIER : BudgetDao.java
 * MISSION : Ancienne façade asynchrone d'accès aux opérations budgétaires de MasroufDao.
 * IMPLÉMENTE : Exécuteur mono-thread IO + Handler(Looper.getMainLooper()) avec rappel Result<T>.
 * EFFET À L'ÉCRAN : Aucun affichage direct ; permettait aux anciens contrôleurs de sauvegarder hors thread UI.
 * COMMUNICATIONS : Délègue vers MasroufDatabase.getInstance(...).masroufDao() ; audit : les écrans courants
 *                  passent désormais par MasroufViewModel + MasroufRepository.
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Context;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Façade asynchrone historique encapsulant les transactions budgétaires atomiques de MasroufDao.
 */
public final class BudgetDao {
    // Thread d'arrière-plan unique sérialisant les écritures SQLite sans bloquer l'UI.
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    /** Callback asynchrone livré sur le thread principal Android. */
    public interface Result<T> { void complete(T value); }
    // Instance Singleton de la base Room.
    private final MasroufDatabase database;

    /** Initialise la façade sur le contexte d'application pour éviter les fuites mémoire. */
    public BudgetDao(Context context) { database=MasroufDatabase.getInstance(context.getApplicationContext()); }

    /** Crée un nouveau budget et ses enveloppes catégories hors thread UI puis renvoie son ID SQLite. */
    public void insertBudgetWithEnvelopes(BudgetEntity budget,List<BudgetLineEntity> envelopes,Result<Long> callback) {
        IO.execute(()->{
            long id=-1L; try { id=database.masroufDao().insertBudgetWithEnvelopes(budget,envelopes); } catch(RuntimeException ignored) { }
            long result=id; new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }

    /** Met à jour un budget existant et réconcilie ses enveloppes ; retourne true si réussi. */
    public void updateBudgetWithEnvelopes(BudgetEntity budget,List<BudgetLineEntity> envelopes,Result<Boolean> callback) {
        IO.execute(()->{
            boolean saved=false; try { saved=database.masroufDao().updateBudgetWithEnvelopes(budget,envelopes); } catch(RuntimeException ignored) { }
            boolean result=saved; new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }

    /** Remplace uniquement les enveloppes internes d'un budget Enfant sans modifier son plafond global. */
    public void replaceChildBudgetEnvelopes(long budgetId,long childUserId,List<BudgetLineEntity> envelopes,Result<Boolean> callback) {
        IO.execute(()->{
            boolean saved=false; try { saved=database.masroufDao().replaceChildBudgetEnvelopes(budgetId,childUserId,envelopes); } catch(RuntimeException ignored) { }
            boolean result=saved; new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result));
        });
    }
}
