/*
 * ============================================================================
 * FICHIER : ManageCategoriesData.java
 * MISSION : Conteneur des données de gestion des catégories pour ManageCategoriesActivity et CustomCategoryDialogFragment.
 * IMPLÉMENTE : DTO immuable séparant catalogue autorisé, catégories personnalisées et sélections actives du compte.
 * EFFET À L'ÉCRAN : Permet d'afficher les boutons « Sélectionner / ✓ Sélectionné » et d'appliquer les restrictions CHILD.
 * COMMUNICATIONS : Retourné par MasroufRepository -> lu par ManageCategoriesActivity et CustomCategoryDialogFragment.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;

import java.util.List;

/**
 * Données de configuration des catégories chargées pour ManageCategoriesActivity.
 */
public final class ManageCategoriesData {
    // Indique si le compte connecté est un profil CHILD (limité aux catégories sélectionnées par son Parent).
    public final boolean childMode;
    // Liste des catégories système accessibles à cet utilisateur.
    public final List<SystemCategoryEntity> catalogCategories;
    // Catégories manuelles propres créées par l'utilisateur via CustomCategoryDialogFragment.
    public final List<CategoryEntity> customCategories;
    // Identifiants de system_categories et categories personnalisées actuellement actifs dans user_categories.
    public final List<Long> selectedCatalogIds;
    public final List<Long> selectedCustomCategoryIds;

    /** Initialise le modèle de gestion multi-catégories pour l'utilisateur courant. */
    public ManageCategoriesData(boolean childMode,
                                List<SystemCategoryEntity> catalogCategories,
                                List<CategoryEntity> customCategories,
                                List<Long> selectedCatalogIds,
                                List<Long> selectedCustomCategoryIds) {
        this.childMode = childMode;
        this.catalogCategories = catalogCategories;
        this.customCategories = customCategories;
        this.selectedCatalogIds = selectedCatalogIds;
        this.selectedCustomCategoryIds = selectedCustomCategoryIds;
    }
}
