/*
 * ============================================================================
 * FICHIER : MoneyInput.java
 * MISSION : Parseur interne convertissant les montants textuels saisis dans les formulaires en millimes entiers.
 * IMPLÉMENTE : Façade package-private statique autour de BudgetPeriodMath.parseSignedOrUnsignedBudgetMilli().
 * EFFET À L'ÉCRAN : Permet aux écrans d'ajout de dépense/produit et au calcul du prix unitaire de lire virgules,
 *                   points ou suffixes "DT" sans erreur d'arrondi flottant.
 * COMMUNICATIONS : Appelé par AddExpenseActivity, AddProductActivity, ComparePriceActivity et FormPriceTextWatcher.
 * ============================================================================
 */
package com.masrouf.controller;

import com.example.masrouf1.BudgetPeriodMath;

/**
 * Utilitaire de conversion texte -> millimes entiers utilisé par les contrôleurs de formulaires.
 */
final class MoneyInput {
    // Constructeur privé : appel exclusivement statique.
    private MoneyInput() { }

    /**
     * Convertit une saisie utilisateur en dinars (ex. "12.5" ou "12,500") en millimes entiers exacts.
     * @param value texte brut issu d'un EditText.
     * @return montant converti en millimes (1 DT = 1 000).
     */
    static long toMilli(String value) {
        // Délègue au parseur BigDecimal exact gérant espaces, virgules et suffixe "DT".
        return BudgetPeriodMath.parseSignedOrUnsignedBudgetMilli(value);
    }
}
