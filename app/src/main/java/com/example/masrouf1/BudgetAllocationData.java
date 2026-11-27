/*
 * ============================================================================
 * FICHIER : BudgetAllocationData.java
 * MISSION : Conteneur de données Room complet alimentant l'écran de répartition budgétaire (% / DT).
 * IMPLÉMENTE : DTO immuable combinant droits d'accès, catégories actives, enveloppes enregistrées et dépenses M-1.
 * EFFET À L'ÉCRAN : Permet à AllocateBudgetActivity de pré-remplir chaque ligne item_allocation_row.xml.
 * COMMUNICATIONS : Retourné par MasroufRepository -> consommé par AllocateBudgetActivity.loadData().
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Snapshot de données Room destiné à l'écran de répartition budgétaire (AllocateBudgetActivity).
 */
public final class BudgetAllocationData {
    // Indique si l'utilisateur courant est autorisé à modifier cette répartition budgétaire.
    public final boolean authorized;
    // Catégories actives du compte et leurs métadonnées catalogue localisées.
    public final List<CategoryEntity> categories;
    public final List<CategoryPresentationRow> presentations;
    // Budget cible, profil enfant éventuel et lignes d'enveloppes existantes en base.
    public final BudgetEntity budget;
    public final ChildProfileEntity childProfile;
    public final List<BudgetLineEntity> lines;
    // Dépenses réelles de la période précédente par categoryId (sert à la pré-répartition intelligente).
    public final Map<Long,Long> previousSpending;

    /** Constructeur de compatibilité sans liste presentations. */
    public BudgetAllocationData(boolean authorized,List<CategoryEntity> categories,BudgetEntity budget,ChildProfileEntity childProfile,List<BudgetLineEntity> lines,Map<Long,Long> previousSpending){
        this(authorized,categories,Collections.emptyList(),budget,childProfile,lines,previousSpending);
    }

    /** Constructeur complet initialisant toutes les données nécessaires au calcul des enveloppes. */
    public BudgetAllocationData(boolean authorized,List<CategoryEntity> categories,List<CategoryPresentationRow> presentations,BudgetEntity budget,ChildProfileEntity childProfile,List<BudgetLineEntity> lines,Map<Long,Long> previousSpending){
        this.authorized=authorized;this.categories=categories;this.presentations=presentations==null?Collections.emptyList():presentations;this.budget=budget;this.childProfile=childProfile;this.lines=lines;this.previousSpending=previousSpending;
    }
}
