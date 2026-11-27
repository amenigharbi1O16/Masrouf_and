/*
 * ============================================================================
 * FICHIER : TransactionRow.java
 * MISSION : Projection SQL allégée d'une transaction financière issue de la table money_transactions.
 * IMPLÉMENTE : POJO Room utilisé pour afficher efficacement les listes chronologiques d'opérations.
 * EFFET À L'ÉCRAN : Alimente les cartes de transactions récentes sur HomeFragment, le journal du mois
 *                   dans CalendarFragment et l'historique récent dans AnalyticsRenderer.
 * COMMUNICATIONS : Retourné par MasroufDao -> MasroufRepository -> HomeFragment, CalendarFragment, AnalyticsRenderer.
 * ============================================================================
 */
package com.example.masrouf1.db;

/**
 * Projection SQL allégée d'une ligne financière (money_transactions).
 */
public final class TransactionRow {
    // Identifiant unique de la ligne (money_transactions.id) transmis ensuite via EXTRA "TRANSACTION_ID".
    public long id;
    // Libellé affiché à l'écran (nom de dépense ou magasin · produit).
    public String title;
    // Montant de l'opération en millimes entiers.
    public long amountMilli;
    // Date/heure de réalisation en millisecondes Unix.
    public long occurredAt;
    // Catégorie technique de mouvement : "expense", "purchase", "donation", "allowance_received" ou "family_allocation".
    public String kind;
}
