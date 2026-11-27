/*
 * ============================================================================
 * FICHIER : FamilyAnalyticsData.java
 * MISSION : Conteneur d'agrégats financiers familiaux réservé exclusivement au profil PARENT.
 * IMPLÉMENTE : DTO immuable regroupant résumés enfants, courbes mensuelles et enveloppe globale foyer.
 * EFFET À L'ÉCRAN : Alimente les graphiques PillBarChartView et ChildrenBudgetAnalyticsView de FamilyBudgetsAnalyticsActivity.
 * COMMUNICATIONS : Assemblé par MasroufRepository -> lu par FamilyBudgetsAnalyticsActivity via MasroufViewModel.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.ChildSpendingSummary;
import java.util.List;

/**
 * Agrégats familiaux réservés au profil Parent.
 */
public final class FamilyAnalyticsData {
    // Liste des résumés agrégés (alloué/dépensé/reste) par enfant du foyer.
    public final List<ChildSpendingSummary> children;
    // Série chronologique mensuelle des dotations et dépenses du foyer.
    public final List<FamilyMonthSpending> months;
    // Plafond mensuel global du budget actif du parent (millimes).
    public final long globalBudgetMilli;
    // Part cumulée versée aux enfants sur la période active (millimes).
    public final long childAllocationsMilli;
    // Dépenses personnelles propres du parent hors dotations enfants (millimes).
    public final long parentSpendingMilli;

    /**
     * Construit le snapshot analytique familial consommé par les vues du parent.
     */
    public FamilyAnalyticsData(List<ChildSpendingSummary> children,List<FamilyMonthSpending> months,long globalBudgetMilli,long childAllocationsMilli,long parentSpendingMilli){
        this.children=children;this.months=months;this.globalBudgetMilli=globalBudgetMilli;this.childAllocationsMilli=childAllocationsMilli;this.parentSpendingMilli=parentSpendingMilli;
    }
}
