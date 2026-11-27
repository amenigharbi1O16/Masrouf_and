/*
 * ============================================================================
 * FICHIER : MoneyRequestSummary.java
 * MISSION : Projection SQL joignant une demande d'argent enfant (money_requests) au nom du demandeur (users).
 * IMPLÉMENTE : POJO retourné par MasroufDao.moneyRequestSummariesForParent(parentUserId).
 * EFFET À L'ÉCRAN : Sert au rendu de chaque ligne de demande d'argent en attente (prénom, montant, motif, statut)
 *                   dans l'écran parent ChildrenManagementActivity avec boutons Accepter / Refuser.
 * COMMUNICATIONS : Retourné par MasroufDao -> MasroufRepository -> ChildrenManagementActivity.
 * ============================================================================
 */
package com.example.masrouf1.db;

/**
 * Projection SQL jointe (money_requests + users) affichée chez le Parent dans
 * ChildrenManagementActivity.
 */
public final class MoneyRequestSummary {
    // Identifiant de la demande d'argent (money_requests.id).
    public long id;
    // Identifiant du compte enfant émetteur.
    public long childUserId;
    // Prénom/nom complet de l'enfant affiché sur la carte de validation parentale.
    public String childName;
    // Montant sollicité par l'enfant (en millimes).
    public long amountMilli;
    // Justification saisie ou dictée par l'enfant.
    public String reason;
    // Statut courant de la demande : "pending", "accepted" ou "rejected".
    public String status;
    // Date/heure de création en millisecondes.
    public long createdAt;
}
