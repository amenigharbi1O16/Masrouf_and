/*
 * ============================================================================
 * FICHIER : SettingsData.java
 * MISSION : Conteneur de données (DTO immuable) regroupant l'état complet de l'écran Paramètres.
 * IMPLÉMENTE : Aucune interface (objet de transfert pur entre MasroufRepository et SettingsFragment).
 * EFFET À L'ÉCRAN : Alimente la carte identité, les catégories suivies, la carte dotation Enfant et les actions de SettingsFragment.
 * COMMUNICATIONS : Construit par MasroufRepository.loadSettingsData(), transporté par MasroufViewModel, consommé par SettingsFragment.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

import java.util.Collections;
import java.util.List;

/**
 * Charge utile complète transmise à SettingsFragment en une seule requête
 * asynchrone.
 */
public final class SettingsData {
    // Fiche de l'utilisateur connecté (nom, Gmail, rôle, solde, points).
    public final UserEntity user;
    // Catégories actives avec leur clé catalogue et icône pour afficher les puces
    // dans Paramètres.
    public final List<CategoryPresentationRow> categories;
    // Liste brute des catégories SQLite de secours si aucune présentation catalogue
    // n'est liée.
    public final List<CategoryEntity> rawCategories;
    // Budget actif courant servant notamment au calcul du résumé partagé et du
    // budget Enfant.
    public final BudgetEntity activeBudget;
    // Profil enfant associé lorsque le compte connecté possède le rôle CHILD (null
    // sinon).
    public final ChildProfileEntity childProfile;
    // Total historique d'argent de poche reçu par l'enfant (en millimes).
    public final long totalAllowanceMilli;
    // Montant total déjà dépensé par l'enfant sur son budget actif (en millimes).
    public final long childSpentMilli;
    // Numéro de téléphone du parent utilisé pour les raccourcis Appeler / SMS du
    // profil CHILD.
    public final String parentPhone;

    /**
     * Constructeur de compatibilité à 7 paramètres déduisant childSpentMilli depuis
     * activeBudget.spentMilli.
     */
    public SettingsData(UserEntity user,
            List<CategoryPresentationRow> categories,
            List<CategoryEntity> rawCategories,
            BudgetEntity activeBudget,
            ChildProfileEntity childProfile,
            long totalAllowanceMilli,
            String parentPhone) {
        // Délègue au constructeur complet en prenant spentMilli du budget actif ou 0 si
        // aucun budget n'existe.
        this(user, categories, rawCategories, activeBudget, childProfile, totalAllowanceMilli,
                activeBudget == null ? 0L : activeBudget.spentMilli, parentPhone);
    }

    /**
     * Constructeur principal initialisant tous les champs avec sécurisation
     * anti-null sur les listes et textes.
     */
    public SettingsData(UserEntity user,
            List<CategoryPresentationRow> categories,
            List<CategoryEntity> rawCategories,
            BudgetEntity activeBudget,
            ChildProfileEntity childProfile,
            long totalAllowanceMilli,
            long childSpentMilli,
            String parentPhone) {
        // Stocke l'entité utilisateur courante.
        this.user = user;
        // Remplace toute liste nulle par une liste vide immuable pour éviter les
        // NullPointerException côté UI.
        this.categories = categories == null ? Collections.emptyList() : categories;
        // Garantit une liste brute non nulle pour l'affichage de repli des catégories.
        this.rawCategories = rawCategories == null ? Collections.emptyList() : rawCategories;
        // Conserve la référence du budget actif.
        this.activeBudget = activeBudget;
        // Conserve la fiche enfant (null pour USER et PARENT).
        this.childProfile = childProfile;
        // Stocke la somme totale d'argent de poche reçu.
        this.totalAllowanceMilli = totalAllowanceMilli;
        // Garantit un montant dépensé positif ou nul.
        this.childSpentMilli = Math.max(0L, childSpentMilli);
        // Normalise un numéro parental null vers une chaîne vide.
        this.parentPhone = parentPhone == null ? "" : parentPhone;
    }
}
