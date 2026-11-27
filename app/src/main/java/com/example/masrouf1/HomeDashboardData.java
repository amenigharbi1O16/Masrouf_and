/*
 * ============================================================================
 * FICHIER : HomeDashboardData.java
 * MISSION : Agrégat principal chargé en une seule passe Room pour dessiner tout le tableau de bord Accueil.
 * IMPLÉMENTE : DTO immuable combinant compte, budget actif, enveloppes, transactions récentes et données rôle (PARENT/CHILD).
 * EFFET À L'ÉCRAN : Alimente chaque zone de HomeFragment (BudgetCardView, résumé profil, carte Enfant, liste catégories, récent).
 * COMMUNICATIONS : Assemblé hors UI thread par MasroufRepository.loadHome(...) -> publié par MasroufViewModel.home().
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.MasroufEntities.SettingEntity;
import com.example.masrouf1.db.MasroufEntities.GoalEntity;
import com.example.masrouf1.db.TransactionRow;
import java.util.List;

/**
 * Snapshot complet du tableau de bord Accueil (HomeFragment), assemblé par MasroufRepository.loadHome().
 */
public final class HomeDashboardData {
    // Utilisateur connecté, son budget sélectionné/actif, ses catégories et jusqu'à 5 transactions récentes.
    public final UserEntity user;public final BudgetEntity budget;public final List<CategoryEntity> categories;public final List<TransactionRow> recent;
    // Fiche enfant (si CHILD), liste des enfants (si PARENT), sommes allouées aux enfants et dépensées par le parent.
    public final ChildProfileEntity childProfile;public final List<ChildSpendingSummary> children;public final long childAllocations,parentSpent;
    // Progression calculée par enveloppe de catégorie (affichée par CategoryProgressCardView).
    public final List<HomeCategoryProgress> categoryProgress;
    // Seuils globaux couleur/alertes (greenUntil 79 %, orangeFrom 80 %, redFrom 100 %).
    public final SettingEntity settings;
    // Premier objectif d'épargne éventuel (goals) et dernière dotation reçue (profil CHILD).
    public final GoalEntity goal; public final TransactionRow latestAllowance;
    // Total historique reçu (CHILD), nombre de transactions sur la période et demandes d'argent en attente (PARENT).
    public final long totalAllowanceMilli; public final int transactionCount,pendingRequestCount;

    /** Initialise toutes les sections du tableau de bord pour un rendu atomique sans requêtes fragmentées. */
    public HomeDashboardData(UserEntity user,BudgetEntity budget,List<CategoryEntity> categories,List<TransactionRow> recent,ChildProfileEntity childProfile,List<ChildSpendingSummary> children,long childAllocations,long parentSpent,List<HomeCategoryProgress> categoryProgress,SettingEntity settings,GoalEntity goal,TransactionRow latestAllowance,long totalAllowanceMilli,int transactionCount,int pendingRequestCount){
        this.user=user;this.budget=budget;this.categories=categories;this.recent=recent;this.childProfile=childProfile;this.children=children;this.childAllocations=childAllocations;this.parentSpent=parentSpent;this.categoryProgress=categoryProgress;this.settings=settings;this.goal=goal;this.latestAllowance=latestAllowance;this.totalAllowanceMilli=totalAllowanceMilli;this.transactionCount=transactionCount;this.pendingRequestCount=pendingRequestCount;
    }
}
