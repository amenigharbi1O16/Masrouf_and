/*
 * ============================================================================
 * FICHIER : EditExpenseActivity.java
 * MISSION : Écran d'aperçu en lecture seule d'une dépense sélectionnée depuis l'Accueil.
 * IMPLÉMENTE : Sous-classe concrète de TransactionDetailActivity déclarée dans AndroidManifest.xml.
 * EFFET À L'ÉCRAN : Affiche l'intitulé « Ajouter une dépense », le titre et le montant formaté de l'opération.
 * COMMUNICATIONS : Ouvert via Intent explicite (extra "TRANSACTION_ID") depuis HomeFragment ;
 *                  délègue toute l'interface et la requête Room à la classe mère TransactionDetailActivity.
 * ============================================================================
 */
package com.masrouf.controller;

import com.example.masrouf1.R;

/** Affiche l'aperçu d'une dépense sélectionnée (titre "Ajouter une dépense", vue lecture seule héritée). */
public final class EditExpenseActivity extends TransactionDetailActivity {
    /**
     * Spécifie la ressource titre affichée dans la barre supérieure pour ce type d'écriture.
     * @return identifiant R.string.add_expense.
     */
    @Override protected int titleResource() { return R.string.add_expense; }
}
