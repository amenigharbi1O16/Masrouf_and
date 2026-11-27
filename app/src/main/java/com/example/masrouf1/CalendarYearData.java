/*
 * ============================================================================
 * FICHIER : CalendarYearData.java
 * MISSION : Conteneur annuel complet des données financières pour l'onglet Calendrier & Historique.
 * IMPLÉMENTE : DTO immuable + sous-classe BudgetPeriodSummary regroupant les 12 mois d'une année.
 * EFFET À L'ÉCRAN : Alimente la grille 12 mois, les périodes chevauchantes et le journal du mois de CalendarFragment.
 * COMMUNICATIONS : Construit par MasroufRepository -> consommé par CalendarFragment.renderYear().
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;

import java.util.List;
import java.util.Map;

/**
 * Modèle annuel agrégé pour CalendarFragment.
 */
public final class CalendarYearData {
    // Année affichée (ex. 2026).
    public final int year;
    // Tableaux de taille 12 (0 = janvier..11 = décembre) pour dépenses et allocations mensuelles en millimes.
    public final long[] monthlySpentMilli;
    public final long[] monthlyAllocatedMilli;
    // Périodes budgétaires recoupant chaque mois (clé = index mois 0..11).
    public final Map<Integer, List<BudgetPeriodSummary>> monthBudgets;
    // Identifiant du budget actuellement choisi comme référence pour ce compte.
    public final long selectedBudgetId;
    // Transactions financières enregistrées pour chaque mois de l'année.
    public final Map<Integer, List<TransactionRow>> monthTransactions;

    /** Initialise toutes les structures calendaires annuelles prêtes pour l'affichage. */
    public CalendarYearData(int year,
                            long[] monthlySpentMilli,
                            long[] monthlyAllocatedMilli,
                            Map<Integer, List<BudgetPeriodSummary>> monthBudgets,
                            long selectedBudgetId,
                            Map<Integer, List<TransactionRow>> monthTransactions) {
        this.year = year;
        this.monthlySpentMilli = monthlySpentMilli;
        this.monthlyAllocatedMilli = monthlyAllocatedMilli;
        this.monthBudgets = monthBudgets;
        this.selectedBudgetId = selectedBudgetId;
        this.monthTransactions = monthTransactions;
    }

    /** Paire liant une entité BudgetEntity au total réellement dépensé calculé sur ses bornes. */
    public static final class BudgetPeriodSummary {
        // Entité budgétaire SQLite (dates, plafond, statut ACTIVE/CLOSED).
        public final BudgetEntity budget;
        // Total des dépenses effectivement constatées dans l'intervalle [periodStart..periodEnd].
        public final long actualSpentMilli;

        /** Construit le résumé d'une période budgétaire pour la carte item_calendar_budget_period. */
        public BudgetPeriodSummary(BudgetEntity budget, long actualSpentMilli) {
            this.budget = budget;
            this.actualSpentMilli = actualSpentMilli;
        }
    }
}
