/*
 * ============================================================================
 * FICHIER : ProductCatalogRow.java
 * MISSION : Projection SQL du catalogue personnel de produits scopée par utilisateur authentifié.
 * IMPLÉMENTE : POJO retourné par MasroufDao.productCatalog(userId) via une sous-requête sur price_history.
 * EFFET À L'ÉCRAN : Alimente chaque carte de ProductCatalogAdapter dans ProductsFragment (photo, nom, marque,
 *                   catégorie, dernier prix unitaire, dernier magasin et tendance de prix vs achat précédent).
 * COMMUNICATIONS : Produit par MasroufDao -> MasroufRepository -> ProductsFragment -> ProductCatalogAdapter.
 * ============================================================================
 */
package com.example.masrouf1.db;

/**
 * Projection SQL du catalogue produits scopée par utilisateur authentifié.
 */
public final class ProductCatalogRow {
    // Identifiant du produit (products.id) passé vers ProductDetailsActivity.
    public long id;
    // Catégorie produit associée (categories.id), utilisée pour le filtrage par
    // puces ChipGroup.
    public long categoryId;
    // Nom du produit affiché en titre de carte.
    public String name;
    // Marque facultative du produit (souvent utilisée pour différencier les
    // articles homonymes).
    public String brand;
    // Unité de mesure ("pièce", "kg", "g", "L", etc.).
    public String unit;
    // Libellé de la catégorie affichée en badge.
    public String categoryName;
    // Nom du dernier magasin relevé dans price_history.
    public String storeName;
    // Chemin fichier local de la photo prise par caméra/galerie (décodée via
    // BitmapFactory).
    public String photoPath;
    // Dernier prix unitaire enregistré (en millimes).
    public long currentPriceMilli;
    // Avant-dernier prix unitaire relevé (null s'il n'y a qu'un seul achat),
    // utilisé pour calculer l'écart %.
    public Long priorPriceMilli;
}
