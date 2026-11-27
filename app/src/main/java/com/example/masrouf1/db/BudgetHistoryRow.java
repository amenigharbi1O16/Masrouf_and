/*
 * ============================================================================
 * FICHIER : BudgetHistoryRow.java
 * MISSION : Projection SQL agrégée représentant une période mensuelle passée/active d'un enfant (4 derniers mois).
 * IMPLÉMENTE : POJO alimenté par Room depuis MasroufDao.childBudgetHistory(childUserId, parentUserId).
 * EFFET À L'ÉCRAN : Sert à afficher l'historique budgétaire sur 4 mois dans la carte enfant de ChildrenManagementActivity
 *                   et dans AnalyticsRenderer, sans exposer le détail individuel des achats de l'enfant.
 * COMMUNICATIONS : Retourné par MasroufDao -> regroupé par MasroufRepository -> lu par ChildrenManagementActivity.
 * ============================================================================
 */
package com.example.masrouf1.db;

/** Projection agrégée utilisée pour afficher l'historique budgétaire sur 4 mois d'un compte enfant. */
public final class BudgetHistoryRow {
    // Timestamp (ms) marquant le début du mois budgétaire concerné.
    public long periodStart;
    // Dotation mensuelle allouée à l'enfant sur cette période (en millimes).
    public long allocatedMilli;
    // Montant total consommé par l'enfant sur cette période (en millimes).
    public long spentMilli;
    // Nombre de points de fidélité gagnés à la clôture de cette période.
    public int pointsAwarded;
}
