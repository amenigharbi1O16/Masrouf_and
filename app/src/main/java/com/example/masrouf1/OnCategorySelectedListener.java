/*
 * ============================================================================
 * FICHIER : OnCategorySelectedListener.java
 * MISSION : Contrat de rappel (listener explicite) prévu pour signaler la sélection d'une catégorie dans une liste.
 * IMPLÉMENTE : Interface Java fonctionnelle (méthode unique onCategorySelected).
 * EFFET À L'ÉCRAN : Aucun rendu direct ; sert de passerelle événementielle entre un sélecteur et son écran parent.
 * COMMUNICATIONS : Audit — interface conservée dans com.example.masrouf1 ; les écrans actifs (OnboardingActivity,
 *                  ManageCategoriesActivity, CustomCategoryDialogFragment) emploient leurs interfaces internes dédiées.
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Interface de rappel explicite prévue pour notifier la sélection d'une catégorie utilisateur.
 */
public interface OnCategorySelectedListener {
    /**
     * Appelée lorsqu'une catégorie est sélectionnée par l'utilisateur.
     * @param categoryId identifiant/clé technique de la catégorie ;
     * @param label libellé localisé affiché.
     */
    void onCategorySelected(String categoryId, String label);
}
