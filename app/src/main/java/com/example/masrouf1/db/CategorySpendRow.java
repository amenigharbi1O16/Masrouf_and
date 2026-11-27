/*
 * ============================================================================
 * FICHIER : CategorySpendRow.java
 * MISSION : Projection SQL Room transportant le cumul des dépenses par catégorie sur une période.
 * IMPLÉMENTE : POJO cible des requêtes @Query de MasroufDao (categorySpendingBetween / observeCategorySpendingBetween).
 * EFFET À L'ÉCRAN : Permet de remplir les barres de consommation et pourcentages de chaque catégorie dans l'UI.
 * COMMUNICATIONS : Retourné par MasroufDao -> MasroufRepository -> MasroufViewModel (MonthlySpending) / HomeFragment.
 * ============================================================================
 */
package com.example.masrouf1.db;

/**
 * Projection SQL en lecture seule retournée par
 * MasroufDao.observeCategorySpending(...).
 */
public final class CategorySpendRow {
    // Identifiant SQLite de la catégorie (categories.id).
    public long categoryId;
    // Somme des dépenses, dons et achats imputés à cette catégorie sur la période
    // (en millimes).
    public long amountMilli;
}
