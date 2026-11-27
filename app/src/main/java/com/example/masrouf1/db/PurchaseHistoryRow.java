/*
 * ============================================================================
 * FICHIER : PurchaseHistoryRow.java
 * MISSION : Projection SQL jointe (purchases + purchase_items) décrivant un achat antérieur d'un produit.
 * IMPLÉMENTE : POJO Room alimenté par MasroufDao.productPurchaseHistory(userId, productId).
 * EFFET À L'ÉCRAN : Affiche chaque ligne d'achat (date, magasin, quantité + unité, prix unitaire et total)
 *                   dans ProductDetailsActivity (PurchaseHistoryAdapter) et ProductPriceHistoryActivity (PriceHistoryAdapter).
 * COMMUNICATIONS : Retourné par MasroufDao -> MasroufRepository -> adapters d'historique produit.
 * ============================================================================
 */
package com.example.masrouf1.db;

/** Ligne d'historique d'un achat pour un produit donné, déjà filtrée par propriétaire authentifié. */
public final class PurchaseHistoryRow {
    // Identifiant unique de l'en-tête d'achat (purchases.id).
    public long purchaseId;
    // Date/heure de l'achat en millisecondes depuis l'époque Unix.
    public long occurredAt;
    // Montant total payé sur cet achat en millimes.
    public long totalMilli;
    // Nom du magasin saisi lors de l'achat (facultatif).
    public String storeName;
    // Quantité achetée exprimée en millièmes (ex. 1500 = 1,5 kg/L/pièce).
    public int quantityMilli;
    // Unité de mesure choisie ("pièce", "kg", "g", "L", etc.).
    public String unit;
    // Prix unitaire calculé au moment de cet achat (en millimes).
    public long unitPriceMilli;
    // Libellé/description de la ligne d'achat.
    public String description;
}
