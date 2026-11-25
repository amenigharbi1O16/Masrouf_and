package com.example.masrouf1.ui;

import com.example.masrouf1.R;

/** Resolves the icon name stored with each Room category to a local vector asset. */
public final class CategoryIconResources {
    private CategoryIconResources() { }
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
            // New icon mappings
            case "laptop-outline": return R.drawable.ic_laptop;
            case "card-outline": return R.drawable.ic_receipt;
            case "gift-outline": return R.drawable.ic_trophy;
            case "people-outline": return R.drawable.ic_people;
            case "heart-outline": return R.drawable.ic_health;
            default: return productFallback(type);
        }
    }
    private static int productFallback(String type){return "product".equalsIgnoreCase(type)?R.drawable.ic_bag:R.drawable.ic_receipt;}
}
