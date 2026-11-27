/*
 * ============================================================================
 * FICHIER : AnalyticsSummaryData.java
 * MISSION : Objet de transfert immuable (DTO) transportant l'instantané complet
 *           des indicateurs analytiques calculés depuis Room pour un compte
 *           (Normal User, Child ou Parent).
 * IMPLÉMENTE : Classe finale immuable contenant deux sous-classes statiques :
 *              CategoryStat (ventilation par catégorie) et ChildAnalyticsItem
 *              (statistiques agrégées confidentielles d'un enfant).
 * EFFET À L'ÉCRAN : Aucun directement — modèle de données affiché par
 *                   AnalyticsRenderer dans AnalyticsFragment et
 *                   FamilyBudgetsAnalyticsActivity.
 * COMMUNICATIONS :
 *   ← MasroufRepository.loadAnalytics (instanciation depuis SQLite)
 *   → AnalyticsFragment, FamilyBudgetsAnalyticsActivity (via ViewModel)
 *   → AnalyticsRenderer (lecture de tous les champs pour rendu visuel)
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;

import java.util.Collections;
import java.util.List;

/**
 * Instantané complet d'analyses budgétaires lu depuis SQLite Room pour AnalyticsFragment (USER / CHILD)
 * et FamilyBudgetsAnalyticsActivity (PARENT), puis rendu par AnalyticsRenderer.
 * Regroupe comparatif mensuel, rythme journalier, projection de fin de mois, ventilation par catégorie,
 * historique 6 mois et progression agrégée confidentielle des enfants pour le Parent.
 */
public final class AnalyticsSummaryData {
    /** Statistiques d'une catégorie sur la période (part du total, budget alloué et % consommé). */
    public static final class CategoryStat {
        public final long categoryId;
        public final String name;
        public final String catalogKey;
        public final String iconName;
        public final String colorHex;
        public final long allocatedMilli;
        public final long spentMilli;
        public final boolean hasBudget;
        public final int plannedPercent;
        public final double consumptionPct;
        public final double shareOfTotal;

        /** Construit la statistique immuable d'une catégorie donnée. */
        public CategoryStat(long categoryId, String name, String catalogKey, String iconName, String colorHex,
                            long allocatedMilli, long spentMilli, boolean hasBudget, int plannedPercent,
                            double consumptionPct, double shareOfTotal) {
            this.categoryId = categoryId;
            this.name = name;
            this.catalogKey = catalogKey;
            this.iconName = iconName;
            this.colorHex = colorHex;
            this.allocatedMilli = allocatedMilli;
            this.spentMilli = spentMilli;
            this.hasBudget = hasBudget;
            this.plannedPercent = plannedPercent;
            this.consumptionPct = consumptionPct;
            this.shareOfTotal = shareOfTotal;
        }
    }

    /** Progression agrégée d'un enfant (sans exposer ses lignes de transactions privées au parent). */
    public static final class ChildAnalyticsItem {
        public final long childUserId;
        public final String nickname;
        public final String email;
        public final long allocatedMilli;
        public final long spentMilli;
        public final long remainingMilli;
        public final int percent;
        public final List<PillBarChart.BarItem> sixMonthHistory;

        /** Construit l'élément de suivi analytique d'un enfant pour la vue Parent. */
        public ChildAnalyticsItem(long childUserId, String nickname, String email, long allocatedMilli,
                                  long spentMilli, long remainingMilli, int percent,
                                  List<PillBarChart.BarItem> sixMonthHistory) {
            this.childUserId = childUserId;
            this.nickname = nickname;
            this.email = email;
            this.allocatedMilli = allocatedMilli;
            this.spentMilli = spentMilli;
            this.remainingMilli = remainingMilli;
            this.percent = percent;
            this.sixMonthHistory = sixMonthHistory == null ? Collections.emptyList() : sixMonthHistory;
        }
    }

    /** Profil du titulaire (USER, CHILD ou PARENT). */
    public final AppRole role;
    /** Budget actif ou sélectionné pour la période analysée (peut être null). */
    public final BudgetEntity budget;
    /** Total dépensé sur la période courante (en millimes). */
    public final long currentSpentMilli;
    /** Total dépensé sur la fenêtre précédente de durée équivalente (en millimes). */
    public final long previousSpentMilli;
    /** Horodatage de début de la fenêtre de comparaison précédente. */
    public final long prevWindowStartMillis;
    /** Horodatage de fin de la fenêtre de comparaison précédente. */
    public final long prevWindowEndMillis;
    /** Sous-total des achats de produits sur la période (en millimes). */
    public final long purchasesTotalMilli;
    /** Sous-total des dépenses directes sur la période (en millimes). */
    public final long expensesTotalMilli;
    /** Dépense moyenne quotidienne constatée depuis le début de la période. */
    public final long avgDailySpendMilli;
    /** Nombre de jours écoulés dans la période courante. */
    public final int daysElapsed;
    /** Durée totale de la période budgétaire en jours. */
    public final int totalDays;
    /** Nombre de jours restants avant la clôture de la période. */
    public final int daysRemaining;
    /** Dépense totale projetée en fin de période selon le rythme actuel. */
    public final long projectedTotalMilli;
    /** Vrai si la projection reste inférieure ou égale au plafond budgétaire. */
    public final boolean paceSustainable;
    /** Écart positif entre projection et budget (économies prévues ou dépassement estimé). */
    public final long projectedDiffMilli;
    /** Liste triée des statistiques de consommation par catégorie. */
    public final List<CategoryStat> categoryStats;
    /** Catégorie la plus à risque ou ayant dépassé son enveloppe (null si tout va bien). */
    public final CategoryStat attentionCategory;
    /** Série des 6 derniers mois pour le graphique PillBarChart. */
    public final List<PillBarChart.BarItem> sixMonthHistory;
    /** Transactions réelles de la période affichées sous le graphique (USER/CHILD). */
    public final List<TransactionRow> periodTransactions;
    /** Progression agrégée de chaque enfant (remplie uniquement pour le rôle PARENT). */
    public final List<ChildAnalyticsItem> childrenProgress;

    /** Initialise l'instantané complet d'analyse en normalisant les listes éventuellement nulles. */
    public AnalyticsSummaryData(
            AppRole role,
            BudgetEntity budget,
            long currentSpentMilli,
            long previousSpentMilli,
            long prevWindowStartMillis,
            long prevWindowEndMillis,
            long purchasesTotalMilli,
            long expensesTotalMilli,
            long avgDailySpendMilli,
            int daysElapsed,
            int totalDays,
            int daysRemaining,
            long projectedTotalMilli,
            boolean paceSustainable,
            long projectedDiffMilli,
            List<CategoryStat> categoryStats,
            CategoryStat attentionCategory,
            List<PillBarChart.BarItem> sixMonthHistory,
            List<TransactionRow> periodTransactions,
            List<ChildAnalyticsItem> childrenProgress) {
        this.role = role == null ? AppRole.USER : role;
        this.budget = budget;
        this.currentSpentMilli = currentSpentMilli;
        this.previousSpentMilli = previousSpentMilli;
        this.prevWindowStartMillis = prevWindowStartMillis;
        this.prevWindowEndMillis = prevWindowEndMillis;
        this.purchasesTotalMilli = purchasesTotalMilli;
        this.expensesTotalMilli = expensesTotalMilli;
        this.avgDailySpendMilli = avgDailySpendMilli;
        this.daysElapsed = daysElapsed;
        this.totalDays = totalDays;
        this.daysRemaining = daysRemaining;
        this.projectedTotalMilli = projectedTotalMilli;
        this.paceSustainable = paceSustainable;
        this.projectedDiffMilli = projectedDiffMilli;
        this.categoryStats = categoryStats == null ? Collections.emptyList() : categoryStats;
        this.attentionCategory = attentionCategory;
        this.sixMonthHistory = sixMonthHistory == null ? Collections.emptyList() : sixMonthHistory;
        this.periodTransactions = periodTransactions == null ? Collections.emptyList() : periodTransactions;
        this.childrenProgress = childrenProgress == null ? Collections.emptyList() : childrenProgress;
    }
}
