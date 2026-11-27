/*
 * ============================================================================
 * FICHIER : CategoryPresentationRow.java
 * MISSION : Projection SQL combinant une catégorie personnelle (categories) à son icône/clé catalogue (system_categories).
 * IMPLÉMENTE : POJO retourné par MasroufDao.selectedCategoryPresentations(userId).
 * EFFET À L'ÉCRAN : Sert à afficher les noms traduits (FR/EN/AR), icônes vectorielles et couleurs des catégories
 *                   dans SettingsFragment, AllocateBudgetActivity, AddExpenseActivity et AddProductActivity.
 * COMMUNICATIONS : Retourné par MasroufDao -> MasroufRepository -> écrans formulaires, Paramètres et BudgetNotificationManager.
 * ============================================================================
 */
package com.example.masrouf1.db;

/**
 * Identité d'une catégorie utilisateur jointe à sa définition catalogue (si
 * catégorie système).
 */
public final class CategoryPresentationRow {
    // Identifiant SQLite dans la table categories.
    public long categoryId;
    // Nom brut enregistré dans categories.
    public String name;
    // Code couleur hexadécimal associé à la catégorie.
    public String colorHex;
    // Type de catégorie : "product" ou "expense".
    public String type;
    // Clé système ("food", "transport", etc.) ou null pour une catégorie
    // personnalisée manuelle.
    public String catalogKey;
    // Nom d'icône catalogue résolu par CategoryIconResources.resolve().
    public String iconName;
    // Pourcentage d'allocation configuré sur la fiche utilisateur.
    public int allocationPercent;
    // Pourcentage conseillé par défaut issu de system_categories (5 % si
    // personnalisé).
    public int recommendedPercent;
}
