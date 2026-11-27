/*
 * ============================================================================
 * FICHIER : MonthlySpending.java
 * MISSION : Modèle composite combinant la liste des catégories de l'utilisateur et leurs montants dépensés mensuels.
 * IMPLÉMENTE : DTO immuable publié par MediatorLiveData dans MasroufViewModel.publishMonthly().
 * EFFET À L'ÉCRAN : Permet aux observateurs LiveData d'afficher en une seule fois les libellés de catégories
 *                   et leurs sommes dépensées correspondantes.
 * COMMUNICATIONS : Assemblé dans MasroufViewModel à partir des flux MasroufRepository.observeCategories et observeCategorySpending.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.CategorySpendRow;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import java.util.List;

/**
 * Paire de listes combinées par MasroufViewModel.loadMonthlySpending(...) via MediatorLiveData.
 */
public final class MonthlySpending {
    // Liste des catégories du compte (fournit noms et couleurs).
    public final List<CategoryEntity> categories;
    // Cumuls dépensés par categoryId sur la fenêtre mensuelle demandée (en millimes).
    public final List<CategorySpendRow> amounts;

    /**
     * Crée le snapshot combiné catégories + dépenses prêt à être consommé par l'UI.
     */
    public MonthlySpending(List<CategoryEntity> categories,List<CategorySpendRow> amounts){this.categories=categories;this.amounts=amounts;}
}
