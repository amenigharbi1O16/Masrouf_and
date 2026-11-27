/*
 * ============================================================================
 * FICHIER : ChildSpendingSummary.java
 * MISSION : Projection SQL agrégée d'un compte Enfant garantissant la confidentialité de ses achats détaillés.
 * IMPLÉMENTE : POJO retourné par MasroufDao.childSpendingSummaries(parentUserId).
 * EFFET À L'ÉCRAN : Permet d'afficher au Parent le prénom, l'email, les dotations reçues, le cumul dépensé
 *                   et le reste disponible de chaque enfant dans ChildrenManagementActivity et AnalyticsRenderer.
 * COMMUNICATIONS : Produit par MasroufDao -> MasroufRepository -> ChildrenManagementActivity, PillBarChartView, AnalyticsRenderer.
 * ============================================================================
 */
package com.example.masrouf1.db;

import com.example.masrouf1.MoneyMath;

/**
 * Projection SQL agrégée et respectueuse de la confidentialité d'un compte
 * Enfant.
 */
public final class ChildSpendingSummary {
    // Identifiant de la fiche child_profiles et identifiant utilisateur associé du
    // compte enfant.
    public long childProfileId;
    public long childUserId;
    // Prénom/surnom, Gmail de connexion et numéro de contact parental affichés sur
    // la carte enfant.
    public String nickname;
    public String email;
    public String parentPhone;
    // Date de création du profil enfant (millisecondes Unix).
    public long createdAt;
    // Montant total alloué sur la période active, montant cumulé dépensé et solde
    // actuel (millimes).
    public long allocatedTotalMilli;
    public long spentMilli;
    public long balanceMilli;

    /**
     * Calcule de façon exacte le reste théorique disponible pour l'enfant (alloué -
     * dépensé) via MoneyMath.
     * 
     * @return montant restant en millimes (peut être négatif en cas de dépassement
     *         budgétaire).
     */
    public long availableMilli() {
        return MoneyMath.subtract(allocatedTotalMilli, spentMilli);
    }
}
