package com.example.masrouf1.db;

import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;

import java.util.ArrayList;
import java.util.List;

/** Canonical seed data for the app-wide category catalog. */
public final class SystemCategoryCatalog {
    private SystemCategoryCatalog() { }

    public static List<SystemCategoryEntity> defaults() {
        ArrayList<SystemCategoryEntity> rows = new ArrayList<>();
        // ── Keep original IDs 1–10 for backward compatibility ───────────────
        // ── Catégories de produits (product) ────────────────────────────────
        rows.add(row(1,  "food",       "Alimentation",               "product", "#2E7D43", "cart-outline",             25, false));
        rows.add(row(7,  "clothing",   "Vêtements et chaussures",    "product", "#4A6572", "shirt-outline",             5, false));
        rows.add(row(11, "hygiene",    "Hygiène et soins personnels","product", "#85B8A5", "medkit-outline",             5, false));
        rows.add(row(12, "home_equip", "Maison et équipement",       "product", "#A58C67", "home-outline",              5, false));
        rows.add(row(13, "computer",   "Matériel informatique",      "product", "#4A6572", "laptop-outline",            5, false));
        rows.add(row(4,  "health",     "Santé",                      "product", "#DC7770", "medkit-outline",           10, false));
        // ── Catégories de dépenses (expense) ────────────────────────────────
        rows.add(row(3,  "transport",  "Transport",                  "expense", "#61746D", "bus-outline",              10, false));
        rows.add(row(8,  "bills",      "Factures",                   "expense", "#D35400", "receipt-outline",           5, false));
        rows.add(row(6,  "education",  "Études",                     "expense", "#A58C67", "school-outline",           10, false));
        rows.add(row(5,  "leisure",    "Loisirs et sorties",         "expense", "#85B8A5", "game-controller-outline",  10, false));
        rows.add(row(14, "children",   "Enfants",                    "expense", "#DC7770", "people-outline",            5, false));
        rows.add(row(15, "family_aid", "Aide à la famille",          "expense", "#E9B66B", "heart-outline",             5, false));
        rows.add(row(9,  "savings",    "Épargne",                    "expense", "#27AE60", "wallet-outline",            5, false));
        rows.add(row(16, "donations",  "Dons et charité",            "expense", "#27AE60", "gift-outline",              3, false));
        rows.add(row(17, "banking",    "Frais bancaires",            "expense", "#7F8C8D", "card-outline",              2, false));
        rows.add(row(10, "other",      "Autres",                     "expense", "#7F8C8D", "ellipsis-horizontal-outline", 0, true));
        return rows;
    }

    public static int titleResource(String key) {
        switch (key == null ? "" : key) {
            case "food":        return R.string.category_food;
            case "housing":     return R.string.category_home;
            case "clothing":    return R.string.category_clothing;
            case "hygiene":     return R.string.category_hygiene;
            case "home_equip":  return R.string.category_home_equip;
            case "computer":    return R.string.category_computer;
            case "health":      return R.string.category_health;
            case "transport":   return R.string.category_transport;
            case "bills":       return R.string.category_bills;
            case "education":   return R.string.category_school;
            case "leisure":     return R.string.category_leisure;
            case "children":    return R.string.category_children;
            case "family_aid":  return R.string.category_family_aid;
            case "savings":     return R.string.category_savings;
            case "donations":   return R.string.category_donations;
            case "banking":     return R.string.category_banking;
            default:            return R.string.category_other;
        }
    }

    private static SystemCategoryEntity row(long id, String key, String name, String type,
                                            String color, String icon, int recommended, boolean protectedCategory) {
        SystemCategoryEntity row = new SystemCategoryEntity();
        row.id = id;
        row.key = key;
        row.name = name;
        row.type = type;
        row.colorHex = color;
        row.iconName = icon;
        row.recommendedPercent = recommended;
        row.protectedCategory = protectedCategory;
        return row;
    }
}
