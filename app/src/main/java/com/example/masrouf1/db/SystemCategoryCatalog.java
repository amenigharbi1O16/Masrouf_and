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
        rows.add(row(1, "food", "Alimentation & Courses", "product", "#2E7D43", "cart-outline", 25, false));
        rows.add(row(2, "housing", "Logement & Charges", "expense", "#E9B66B", "home-outline", 20, false));
        rows.add(row(3, "transport", "Transport", "expense", "#61746D", "bus-outline", 10, false));
        rows.add(row(4, "health", "Santé", "expense", "#DC7770", "medkit-outline", 10, false));
        rows.add(row(5, "leisure", "Loisirs & Sorties", "expense", "#85B8A5", "game-controller-outline", 10, false));
        rows.add(row(6, "education", "Éducation & Scolarité", "expense", "#A58C67", "school-outline", 10, false));
        rows.add(row(7, "clothing", "Vêtements", "product", "#4A6572", "shirt-outline", 5, false));
        rows.add(row(8, "bills", "Factures & Abonnements", "expense", "#D35400", "receipt-outline", 5, false));
        rows.add(row(9, "savings", "Épargne & Réserve", "expense", "#27AE60", "wallet-outline", 5, false));
        rows.add(row(10, "other", "Autres dépenses", "expense", "#7F8C8D", "ellipsis-horizontal-outline", 0, true));
        return rows;
    }

    public static int titleResource(String key) {
        switch (key == null ? "" : key) {
            case "food": return R.string.category_food;
            case "housing": return R.string.category_home;
            case "transport": return R.string.category_transport;
            case "health": return R.string.category_health;
            case "leisure": return R.string.category_leisure;
            case "education": return R.string.category_school;
            case "clothing": return R.string.category_clothing;
            case "bills": return R.string.category_bills;
            case "savings": return R.string.category_savings;
            default: return R.string.category_other;
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
