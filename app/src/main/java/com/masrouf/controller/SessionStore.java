/*
 * ============================================================================
 * FICHIER : SessionStore.java
 * MISSION : Persister et relire localement la session de l'utilisateur connecté dans SharedPreferences.
 * IMPLÉMENTE : Utilitaire statique s'appuyant sur "masrouf_preferences" (MODE_PRIVATE).
 * EFFET À L'ÉCRAN : Aucun affichage direct, mais conditionne le routage (Welcome -> Onboarding -> MainActivity)
 *                   et l'adaptation du Dashboard/Drawer selon le profil (USER, PARENT, CHILD).
 * COMMUNICATIONS : Écrit par LoginActivity, OnboardingActivity, HomeFragment, AllocateBudgetActivity ;
 *                  lu par toutes les Activities/Fragments protégés et vidé lors de la déconnexion.
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Context;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

/**
 * Gestionnaire de session persistée localement via SharedPreferences ("masrouf_preferences", MODE_PRIVATE).
 */
public final class SessionStore {
    // Constructeur privé empêchant l'instanciation de cet utilitaire purement statique.
    private SessionStore() { }

    /**
     * Enregistre les informations essentielles de session après une connexion réussie dans LoginActivity.
     * @param context contexte Android utilisé pour ouvrir le fichier de préférences privées.
     * @param user entité UserEntity authentifiée depuis SQLite Room.
     */
    public static void save(Context context, UserEntity user) {
        // Écrit de façon asynchrone (.apply()) l'ID utilisateur, son rôle normalisé et ses drapeaux d'onboarding.
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit()
                .putLong("user_id", user.id).putString("role", user.role==null?AppRole.USER.value():user.role)
                .putBoolean("onboarded",user.onboarded).putBoolean("categories_selected",user.categoriesSelected).apply();
    }

    /**
     * Lit l'identifiant du compte actuellement connecté.
     * @return ID SQLite du compte connecté, ou 0L si aucune session n'est active.
     */
    public static long userId(Context context) {
        return context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).getLong("user_id", 0L);
    }

    /**
     * Lit le profil de l'utilisateur courant et le convertit en valeur sûre de l'énumération AppRole.
     * @return profil AppRole (USER, PARENT ou CHILD).
     */
    public static AppRole role(Context context) {
        return AppRole.from(context.getSharedPreferences("masrouf_preferences",Context.MODE_PRIVATE).getString("role",AppRole.USER.value()));
    }

    /**
     * Indique si l'utilisateur courant a déjà validé sa sélection initiale de catégories (Étape 2/3 d'onboarding).
     */
    public static boolean categoriesSelected(Context context){return context.getSharedPreferences("masrouf_preferences",Context.MODE_PRIVATE).getBoolean("categories_selected",false);}

    /**
     * Marque localement la fin de l'assistant OnboardingActivity afin d'éviter son réaffichage au prochain démarrage.
     */
    public static void markOnboardingComplete(Context context){context.getSharedPreferences("masrouf_preferences",Context.MODE_PRIVATE).edit().putBoolean("onboarded",true).putBoolean("categories_selected",true).apply();}

    /**
     * Efface la session courante lors du clic sur « Déconnexion » (conserve l'historique SQLite intact).
     */
    public static void clear(Context context) {
        // Supprime uniquement les clés de session de SharedPreferences sans toucher à selected_budget_id ni à Room.
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit().remove("user_id").remove("role").remove("onboarded").remove("categories_selected").apply();
    }

    /**
     * Retourne l'identifiant du budget sélectionné manuellement par cet utilisateur (ex. depuis Calendrier).
     * @return budgetId mémorisé pour cet utilisateur, ou 0L pour laisser le budget actif courant par défaut.
     */
    public static long selectedBudgetId(Context context, long userId) {
        // Un userId invalide n'a jamais de préférence de budget associée.
        if (userId <= 0L) return 0L;
        return context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE)
                .getLong("selected_budget_id_" + userId, 0L);
    }

    /**
     * Mémorise le budget choisi comme référence pour cet utilisateur précis (clé isolée par userId).
     */
    public static void setSelectedBudgetId(Context context, long userId, long budgetId) {
        // Ignore tout identifiant utilisateur nul ou négatif.
        if (userId <= 0L) return;
        // Borne budgetId à >= 0L avant d'écrire la clé propre au compte dans SharedPreferences.
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit()
                .putLong("selected_budget_id_" + userId, Math.max(0L, budgetId)).apply();
    }
}
