/*
 * ============================================================================
 * FICHIER : FamilyMonthSpending.java
 * MISSION : Modèle UI mensuel consolidant les dotations familiales et dépenses pour un mois donné du foyer PARENT.
 * IMPLÉMENTE : Objet de données mutable rempli lors de l'agrégation analytique familiale.
 * EFFET À L'ÉCRAN : Alimente les barres mensuelles de PillBarChartView et FamilyBudgetsAnalyticsActivity / AnalyticsRenderer.
 * COMMUNICATIONS : Instancié par MasroufRepository dans le calcul de FamilyAnalyticsData.
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Cumul mensuel d'un foyer sur un mois donné (début de période, dotations allouées et dépenses réelles en millimes).
 */
public final class FamilyMonthSpending {
    // Timestamp du 1er jour du mois analysé (00:00:00).
    public final long periodStart;
    // Total alloué sur ce mois pour l'enveloppe familiale / les enfants (millimes).
    public long allocatedMilli;
    // Total réellement dépensé sur ce mois par le foyer (millimes).
    public long spentMilli;

    /**
     * Initialise le résumé mensuel pour un mois calendaire donné (montants démarrant à 0).
     * @param periodStart timestamp du début de mois.
     */
    public FamilyMonthSpending(long periodStart){this.periodStart=periodStart;}
}
