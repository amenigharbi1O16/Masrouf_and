/*
 * ============================================================================
 * FICHIER : ProductDetailsData.java
 * MISSION : Conteneur immuable regroupant la fiche complète d'un produit et ses statistiques de prix.
 * IMPLÉMENTE : DTO calculant à la construction min, moyenne (BigInteger exact) et max sur price_history.
 * EFFET À L'ÉCRAN : Alimente la fiche produit, la courbe PriceChartView et la liste PurchaseHistoryAdapter
 *                   dans ProductDetailsActivity.
 * COMMUNICATIONS : Assemblé par MasroufRepository -> transmis par MasroufViewModel -> ProductDetailsActivity.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.PurchaseHistoryRow;
import java.math.BigInteger;
import java.util.List;

/**
 * Fiche détaillée d'un produit (ProductDetailsActivity) et statistiques de prix calculées.
 */
public final class ProductDetailsData {
    // Fiche principale du produit (nom, marque, unité, photo, dernier prix).
    public final ProductEntity product;
    // Points chronologiques d'évolution du prix unitaire pour tracer la courbe PriceChartView.
    public final List<PriceHistoryEntity> history;
    // Liste des achats antérieurs de ce produit pour ce compte utilisateur.
    public final List<PurchaseHistoryRow> purchases;
    // Indicateurs statistiques (en millimes) calculés sur history (null si aucun historique).
    public final Long minPriceMilli,averagePriceMilli,maxPriceMilli;

    /**
     * Initialise la fiche et calcule exactement les bornes min / moyenne / max en BigInteger sans débordement.
     */
    public ProductDetailsData(ProductEntity product,List<PriceHistoryEntity> history,List<PurchaseHistoryRow> purchases){
        this.product=product;this.history=history;this.purchases=purchases;
        // Aucun calcul statistique si l'historique des prix est vide ou absent.
        if(history==null||history.isEmpty()){minPriceMilli=null;averagePriceMilli=null;maxPriceMilli=null;}
        else {
            // Initialise les accumulateurs pour le balayage de tous les relevés price_history.
            long min=Long.MAX_VALUE,max=Long.MIN_VALUE;BigInteger total=BigInteger.ZERO;
            for(PriceHistoryEntity row:history){min=Math.min(min,row.priceMilli);max=Math.max(max,row.priceMilli);total=total.add(BigInteger.valueOf(row.priceMilli));}
            // Enregistre minimum, maximum et moyenne entière exacte.
            minPriceMilli=min;maxPriceMilli=max;averagePriceMilli=total.divide(BigInteger.valueOf(history.size())).longValueExact();
        }
    }
}
