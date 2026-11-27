/*
 * ============================================================================
 * FICHIER : EditPurchaseActivity.java
 * MISSION : Écran d'aperçu en lecture seule d'un achat produit sélectionné depuis l'Accueil.
 * IMPLÉMENTE : Sous-classe concrète de TransactionDetailActivity déclarée dans AndroidManifest.xml.
 * EFFET À L'ÉCRAN : Affiche le titre « Ajouter un achat / produit », le nom et le montant de l'achat.
 * COMMUNICATIONS : Ouvert via Intent explicite (extra "TRANSACTION_ID") depuis HomeFragment ;
 *                  délègue l'affichage et le chargement Room à TransactionDetailActivity.
 * ============================================================================
 */
package com.masrouf.controller;

import com.example.masrouf1.R;

/** Affiche l'aperçu d'un achat sélectionné (titre "Ajouter un achat / produit", vue lecture seule héritée). */
public final class EditPurchaseActivity extends TransactionDetailActivity {
    /**
     * Fournit la ressource titre adaptée aux achats de produits pour BaseFeatureActivity.showPage().
     * @return identifiant R.string.add_purchase_product.
     */
    @Override protected int titleResource() { return R.string.add_purchase_product; }
}
