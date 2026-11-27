/*
 * ============================================================================
 * FICHIER : MoneyUtils.java
 * MISSION : Utilitaires de conversion et formatage monétaire exact en dinars
 *           tunisiens. Convertit les saisies textuelles en millimes entiers
 *           (toMilli) et formate les millimes pour affichage ("1 250,000 DT").
 * IMPLÉMENTE : Classe utilitaire statique utilisant BigDecimal pour la
 *              conversion exacte (movePointRight(3)) et DecimalFormat avec
 *              symboles Locale.FRANCE pour le formatage tunisien.
 * EFFET À L'ÉCRAN : Format monétaire visible sur chaque TextView affichant
 *                   un montant dans toute l'application (budgets, dépenses,
 *                   produits, demandes d'argent, notifications).
 * COMMUNICATIONS :
 *   ← Tous les formulaires de saisie (AddExpenseActivity, AddProductActivity,
 *     BudgetPeriodActivity, MoneyRequestActivity, VoiceParser)
 *   ← Tous les écrans d'affichage (HomeFragment, CalendarFragment, etc.)
 *   ← CurrencyFormatter (délègue à formatMilli pour l'affichage)
 * ============================================================================
 */
package com.example.masrouf1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Utilitaires de conversion et formatage monétaire exact en dinars tunisiens (millimes -> "1 250,000 DT") sans flottants. */
/** Money conversion and display helpers. All persisted values are integer millimes. */
public final class MoneyUtils {
    private MoneyUtils() { }

    /**
     * Convertit une chaîne saisie en dinars (ex. "12,5" ou "1 250.000") en millimes entiers exacts.
     * Appelée par les formulaires (dépense, produit, budget, demande) et le parseur vocal.
     *
     * @param amount Montant textuel avec au plus 3 décimales (virgule ou point acceptés, espaces nettoyés).
     * @return Valeur entière exacte en millimes (1 DT = 1000 millimes).
     * @throws NumberFormatException Si la chaîne est vide ou invalide.
     */
    public static long toMilli(String amount) {
        String normalized = amount == null ? "" : amount.trim()
                .replace("\u00a0", "").replace("\u202f", "").replace("\u2009", "")
                .replace(" ", "").replace(',', '.');
        if (normalized.isEmpty()) throw new NumberFormatException("Amount is empty");
        return new BigDecimal(normalized).setScale(3, RoundingMode.UNNECESSARY)
                .movePointRight(3).longValueExact();
    }

    /**
     * Formate un entier en millimes selon l'affichage tunisien français ("1 250,000 DT") pour les vues TextView.
     *
     * @param amountMilli Montant en millimes stocké en base Room.
     * @return Chaîne formatée avec séparateur de milliers espace, virgule, 3 décimales et suffixe " DT".
     */
    public static String formatMilli(long amountMilli) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.000", symbols);
        format.setGroupingUsed(true);
        format.setRoundingMode(RoundingMode.UNNECESSARY);
        return format.format(BigDecimal.valueOf(amountMilli, 3)) + " DT";
    }
}
