/*
 * ============================================================================
 * FICHIER : ProductFormData.java
 * MISSION : Charge utile initialisant les écrans de saisie de dépense, achat produit et comparateur de prix.
 * IMPLÉMENTE : DTO immuable combinant le budget actif, les catégories sélectionnées et leurs métadonnées d'icônes.
 * EFFET À L'ÉCRAN : Permet aux Spinners de AddExpenseActivity / AddProductActivity / ComparePriceActivity
 *                   d'afficher les noms localisés et aux bannières de signaler l'absence de budget actif.
 * COMMUNICATIONS : Chargé par MasroufRepository -> transmis par MasroufViewModel -> formulaires produits/dépenses.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;

import java.util.Collections;
import java.util.List;

/**
 * Données nécessaires à l'ouverture des formulaires AddProductActivity, AddExpenseActivity et ComparePriceActivity.
 */
public final class ProductFormData {
    // Budget actif auquel imputer la dépense ou l'achat (null si aucun budget n'est défini).
    public final BudgetEntity budget;
    // Catégories autorisées pour le compte connecté.
    public final List<CategoryEntity> categories;
    // Présentations enrichies (catalogKey, iconName) permettant de localiser les noms système en FR/EN/AR.
    public final List<CategoryPresentationRow> presentations;

    /** Constructeur de compatibilité sans présentations catalogue. */
    public ProductFormData(BudgetEntity budget, List<CategoryEntity> categories) {
        this(budget, categories, Collections.emptyList());
    }

    /** Constructeur principal garantissant des listes non nulles. */
    public ProductFormData(BudgetEntity budget, List<CategoryEntity> categories,
                           List<CategoryPresentationRow> presentations) {
        this.budget = budget;
        this.categories = categories == null ? Collections.emptyList() : categories;
        this.presentations = presentations == null ? Collections.emptyList() : presentations;
    }
}
