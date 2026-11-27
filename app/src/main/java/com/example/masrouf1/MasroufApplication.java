/*
 * ============================================================================
 * FICHIER : MasroufApplication.java
 * MISSION : Point d'entrée global de l'application Android. Exécuté avant
 *           toute Activity pour initialiser l'infrastructure transverse :
 *           comptes de démo, canal de notifications, alarme mensuelle et
 *           tâche périodique WorkManager de clôture budgétaire.
 * IMPLÉMENTE : Application.onCreate() avec quatre initialisations séquentielles :
 *              1) DevelopmentSeedManager.seedIfDebug (pré-peuplement Debug)
 *              2) BudgetNotificationManager.createChannel (canal Android 8+)
 *              3) BudgetEvaluationService.scheduleNextMonth (alarme AlarmManager)
 *              4) WorkManager PeriodicWorkRequest quotidienne (BudgetRolloverWorker)
 * EFFET À L'ÉCRAN : Aucun directement — prépare les services de fond.
 * COMMUNICATIONS :
 *   → DevelopmentSeedManager (injection des comptes test si Debug)
 *   → BudgetNotificationManager (création du canal "budget_channel")
 *   → BudgetEvaluationService (alarme mensuelle de clôture)
 *   → BudgetRolloverWorker (vérification quotidienne des budgets expirés)
 * ============================================================================
 */
package com.example.masrouf1;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;
import com.masrouf.controller.DevelopmentSeedManager;

/**
 * Point d'entrée global de l'application Android exécuté avant la création de toute Activity.
 * Déclare le pré-peuplement Debug (DevelopmentSeedManager), enregistre le canal de notification
 * "budget_channel" (BudgetNotificationManager), planifie l'alarme mensuelle de clôture (BudgetEvaluationService)
 * et démarre la tâche périodique WorkManager quotidienne (BudgetRolloverWorker).
 */
public final class MasroufApplication extends Application {
    /** Initialise l'infrastructure globale (notifications, alarme mensuelle et worker de bascule budgétaire). */
    @Override public void onCreate() {
        super.onCreate();
        // Injecte les comptes de démonstration uniquement en mode Debug si la base est vide.
        DevelopmentSeedManager.seedIfDebug(this);
        // Crée le canal de notifications "budget_channel" requis sur Android 8+ (API 26+).
        BudgetNotificationManager.createChannel(this);
        // Programme l'alarme mensuelle du service au premier plan pour la clôture des budgets.
        com.masrouf.service.BudgetEvaluationService.scheduleNextMonth(this);
        // Planifie une tâche périodique quotidienne WorkManager pour vérifier les budgets expirés hors ligne.
        PeriodicWorkRequest rollover = new PeriodicWorkRequest.Builder(BudgetRolloverWorker.class, 1, TimeUnit.DAYS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("masrouf-budget-rollover", ExistingPeriodicWorkPolicy.KEEP, rollover);
    }
}
