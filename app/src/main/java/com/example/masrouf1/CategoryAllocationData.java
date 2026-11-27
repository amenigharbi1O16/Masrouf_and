/*
 * ============================================================================
 * FICHIER : CategoryAllocationData.java
 * MISSION : Conteneur immuable résumant la part (%) déjà allouée sur le budget actif avant ajout d'une catégorie.
 * IMPLÉMENTE : DTO léger combinant un pourcentage cumulé et l'entité BudgetEntity active.
 * EFFET À L'ÉCRAN : Alimente la jauge de pourcentage disponible/alloué dans la modale CustomCategoryDialogFragment.
 * COMMUNICATIONS : Retourné par MasroufRepository -> lu par CustomCategoryDialogFragment lors de l'ouverture du dialogue.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

/**
 * Résultat d'une lecture Room vérifiant le taux (%) déjà alloué sur le budget actif.
 */
public final class CategoryAllocationData {
    // Somme actuelle des pourcentages alloués (en %) sur les catégories existantes.
    public final int usedPercent;
    // Budget actif sur lequel une enveloppe automatique sera éventuellement créée (null s'il n'y en a pas).
    public final BudgetEntity budget;

    /**
     * Construit l'état courant d'allocation budgétaire pour CustomCategoryDialogFragment.
     * @param usedPercent pourcentage déjà consommé (0..100).
     * @param budget entité BudgetEntity active ou null.
     */
    public CategoryAllocationData(int usedPercent,BudgetEntity budget){this.usedPercent=usedPercent;this.budget=budget;}
}
