/*
 * ============================================================================
 * FICHIER : CategoryIconResources.java
 * MISSION : Table de correspondance statique résolvant le nom d'icône texte
 *           stocké en base SQLite (ex. "cart-outline", "bus-outline") vers la
 *           ressource drawable vectorielle Android locale (R.drawable.ic_*).
 * IMPLÉMENTE : Méthode statique resolve(iconName, type) avec solution de repli
 *              productFallback() selon le type ("product" -> ic_bag, sinon ic_receipt).
 * EFFET À L'ÉCRAN : Affiche l'icône vectorielle appropriée dans les cartes de
 *                   catégories, puces de réglages, onboarding et analyses.
 * COMMUNICATIONS :
 *   ← CategoryProgressCardView, SettingsFragment, AnalyticsRenderer,
 *     OnboardingActivity, ManageCategoriesActivity, CategorySelectionAdapter
 *   ← SystemCategoryCatalog (iconName persistés dans system_categories)
 * ============================================================================
 */
package com.example.masrouf1.ui;

import com.example.masrouf1.R;

/** Table de correspondance statique entre la clé iconName en base SQLite et la ressource drawable vectorielle locale. */
public final class CategoryIconResources {
    /** Empêche l'instanciation de la classe utilitaire. */
    private CategoryIconResources() { }
    /** Associe un identifiant d'icône Ionicons stocké dans Room à sa ressource drawable vectorielle locale. */
    public static int resolve(String iconName,String type) {
        if(iconName==null) return productFallback(type);
        switch(iconName) {
            case "cart-outline": return R.drawable.ic_cart;
            case "home-outline": return R.drawable.ic_home;
            case "bus-outline": return R.drawable.ic_bus;
            case "medkit-outline": return R.drawable.ic_health;
            case "game-controller-outline": return R.drawable.ic_leisure;
            case "school-outline": return R.drawable.ic_school;
            case "shirt-outline": return R.drawable.ic_clothing;
            case "receipt-outline": return R.drawable.ic_receipt;
            case "wallet-outline": return R.drawable.ic_wallet;
            case "ellipsis-horizontal-outline": return R.drawable.ic_more;
            // Correspondances des icônes ajoutées lors de l'extension du catalogue système.
            case "laptop-outline": return R.drawable.ic_laptop;
            case "card-outline": return R.drawable.ic_receipt;
            case "gift-outline": return R.drawable.ic_trophy;
            case "people-outline": return R.drawable.ic_people;
            case "heart-outline": return R.drawable.ic_health;
            default: return productFallback(type);
        }
    }
    /** Retourne l'icône par défaut selon la nature de la catégorie ("product" vs "expense"). */
    private static int productFallback(String type){return "product".equalsIgnoreCase(type)?R.drawable.ic_bag:R.drawable.ic_receipt;}
}
