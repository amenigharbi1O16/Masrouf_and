/*
 * ============================================================================
 * FICHIER : MoneyMath.java
 * MISSION : Arithmétique monétaire exacte sur entiers longs et BigInteger,
 *           éliminant toute erreur d'arrondi flottant. Fournit les opérations
 *           de base (addition, soustraction, portion, pourcentage), le calcul
 *           de prix unitaire, la détection de hausse >10 %, et la répartition
 *           par la méthode du plus fort reste (allocate).
 * IMPLÉMENTE : Classe utilitaire statique avec BigInteger pour la précision,
 *              Math.addExact/subtractExact pour la détection de dépassement,
 *              et arrondi demi-éloigné de zéro (half-away-from-zero).
 * EFFET À L'ÉCRAN : Utilisé partout pour les calculs de barres de progression,
 *                   badges de pourcentage, alertes prix, répartitions budget.
 * COMMUNICATIONS :
 *   ← AddProductActivity, FormPriceTextWatcher (calcul prix unitaire)
 *   ← AllocateBudgetActivity (répartition des enveloppes)
 *   ← BudgetNotificationManager (seuils 70%/100%)
 *   ← HomeFragment, CalendarFragment, AnalyticsFragment (affichage %)
 *   ← MasroufDao, MasroufRepository (opérations atomiques)
 * ============================================================================
 */
package com.example.masrouf1;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Arithmétique monétaire exacte sur entiers (BigInteger / Math.*Exact) évitant toute erreur d'arrondi flottant. */
/** Exact helpers for values stored as integer millimes. Floating point is reserved for drawing only. */
public final class MoneyMath {
    private static final BigInteger THOUSAND = BigInteger.valueOf(1_000L);
    private static final BigInteger HUNDRED = BigInteger.valueOf(100L);
    private MoneyMath() { }

    /**
     * Calcule le prix unitaire exact en millimes à partir du prix total et de la quantité en millièmes (arrondi demi-supérieur).
     * Utilisé par AddProductActivity, FormPriceTextWatcher et MasroufRepository pour l'historique des prix.
     *
     * @param totalMilli Coût total payé en millimes.
     * @param quantityMilli Quantité exprimée en millièmes (1 unité = 1000).
     * @return Prix unitaire en millimes.
     */
    public static long pricePerUnitMilli(long totalMilli, long quantityMilli) {
        if (totalMilli < 0 || quantityMilli <= 0) throw new ArithmeticException("Invalid price or quantity");
        BigInteger numerator = BigInteger.valueOf(totalMilli).multiply(THOUSAND);
        BigInteger denominator = BigInteger.valueOf(quantityMilli);
        BigInteger[] division = numerator.divideAndRemainder(denominator);
        if (division[1].shiftLeft(1).compareTo(denominator) >= 0) division[0] = division[0].add(BigInteger.ONE);
        return division[0].longValueExact();
    }

    /**
     * Vérifie si un prix courant dépasse le prix précédent de strictement plus de 10 % (alerte inflation).
     *
     * @param current Nouveau prix en millimes.
     * @param previous Prix de référence en millimes.
     * @return true si l'augmentation est > 10 %, déclenchant la bannière ou notification d'alerte prix.
     */
    public static boolean increasedMoreThanTenPercent(long current, long previous) {
        return previous > 0 && BigInteger.valueOf(current).subtract(BigInteger.valueOf(previous))
                .multiply(BigInteger.TEN).compareTo(BigInteger.valueOf(previous)) > 0;
    }

    /**
     * Calcule la variation en pourcentage signée arrondie à l'entier le plus proche (demi éloigné de zéro).
     *
     * @param current Valeur actuelle en millimes.
     * @param previous Valeur précédente en millimes.
     * @return Pourcentage signé (+ ou -), saturé aux bornes de int.
     */
    public static int percentChangeRounded(long current, long previous) {
        if (previous <= 0) return 0;
        BigInteger delta = BigInteger.valueOf(current).subtract(BigInteger.valueOf(previous));
        BigInteger value = divideHalfAwayFromZero(delta.multiply(HUNDRED), BigInteger.valueOf(previous));
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) return Integer.MAX_VALUE;
        if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) return Integer.MIN_VALUE;
        return value.intValue();
    }

    /**
     * Calcule le pourcentage consommé d'une enveloppe ou d'un budget (barres de progression et badges).
     *
     * @param amount Montant dépensé en millimes.
     * @param allocation Montant alloué en millimes.
     * @return Pourcentage entier arrondi.
     */
    public static int percentOf(long amount, long allocation) {
        if (allocation <= 0) return amount > 0 ? Integer.MAX_VALUE : 0;
        BigInteger value = divideHalfAwayFromZero(BigInteger.valueOf(amount).multiply(HUNDRED), BigInteger.valueOf(allocation));
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) return Integer.MAX_VALUE;
        if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) return Integer.MIN_VALUE;
        return value.intValue();
    }

    /**
     * Teste exactement (sans flottants) si amount / allocation atteint au moins thresholdPercent %.
     * Utilisé par BudgetNotificationManager et BudgetEvaluationService pour les seuils 70 % et 100 %.
     */
    public static boolean atLeastPercent(long amount, long allocation, int thresholdPercent) {
        if (allocation <= 0) return amount > 0;
        return BigInteger.valueOf(amount).multiply(HUNDRED)
                .compareTo(BigInteger.valueOf(allocation).multiply(BigInteger.valueOf(thresholdPercent))) >= 0;
    }

    /** Addition exacte de deux montants en millimes avec détection de dépassement. */
    public static long add(long firstMilli,long secondMilli){return Math.addExact(firstMilli,secondMilli);}
    /** Soustraction exacte de deux montants en millimes avec détection de dépassement. */
    public static long subtract(long firstMilli,long secondMilli){return Math.subtractExact(firstMilli,secondMilli);}
    /** Calcule une fraction entière en pourcentage d'un montant en millimes. */
    public static long portion(long totalMilli,int percent){
        return BigInteger.valueOf(totalMilli).multiply(BigInteger.valueOf(percent)).divide(HUNDRED).longValueExact();
    }
    /** Calcule le coût total exact (prix unitaire x quantité en millièmes), arrondi demi-éloigné de zéro. */
    public static long totalForQuantity(long unitPriceMilli,long quantityMilli){
        if(unitPriceMilli<0||quantityMilli<0)throw new ArithmeticException("Invalid price or quantity");
        return divideHalfAwayFromZero(BigInteger.valueOf(unitPriceMilli).multiply(BigInteger.valueOf(quantityMilli)),THOUSAND).longValueExact();
    }

    /**
     * Répartit exactement un budget total selon des poids (méthode du plus fort reste) sans perte ni création de millimes.
     * Utilisé lors de l'onboarding et de la répartition par catégories dans AllocateBudgetActivity.
     *
     * @param total Enveloppe globale à distribuer en millimes.
     * @param weights Liste des poids relatifs par catégorie.
     * @return Liste d'allocations entières dont la somme vaut exactement total.
     */
    public static List<Long> allocate(long total, List<Long> weights) {
        ArrayList<Long> result = new ArrayList<>(weights.size());
        if (weights.isEmpty()) return result;
        BigInteger totalValue = BigInteger.valueOf(Math.max(0L, total));
        BigInteger weightSum = BigInteger.ZERO;
        for (Long weight : weights) weightSum = weightSum.add(BigInteger.valueOf(Math.max(0L, weight == null ? 0L : weight)));
        if (weightSum.signum() == 0) {
            for (int i = 0; i < weights.size(); i++) result.add(0L);
            return result;
        }
        BigInteger[] remainders = new BigInteger[weights.size()];
        long assigned = 0;
        for (int i = 0; i < weights.size(); i++) {
            BigInteger product = totalValue.multiply(BigInteger.valueOf(Math.max(0L, weights.get(i) == null ? 0L : weights.get(i))));
            BigInteger[] part = product.divideAndRemainder(weightSum);
            long value = part[0].longValueExact();
            result.add(value);
            remainders[i] = part[1];
            assigned = Math.addExact(assigned, value);
        }
        long remaining = totalValue.longValueExact() - assigned;
        while (remaining > 0) {
            int best = -1;
            for (int i = 0; i < remainders.length; i++)
                if (remainders[i] != null && (best < 0 || remainders[i].compareTo(remainders[best]) > 0)) best = i;
            if (best < 0) break;
            result.set(best, Math.addExact(result.get(best), 1L));
            remainders[best] = null;
            remaining--;
        }
        return result;
    }

    public static long threeQuarterAverage(long current, long average) {
        BigInteger numerator = BigInteger.valueOf(Math.max(0L, current)).multiply(BigInteger.valueOf(3L))
                .add(BigInteger.valueOf(Math.max(0L, average)));
        return numerator.divide(BigInteger.valueOf(4L)).longValueExact();
    }

    private static BigInteger divideHalfAwayFromZero(BigInteger numerator, BigInteger denominator) {
        BigInteger[] division = numerator.abs().divideAndRemainder(denominator.abs());
        if (division[1].shiftLeft(1).compareTo(denominator.abs()) >= 0) division[0] = division[0].add(BigInteger.ONE);
        return numerator.signum() * denominator.signum() < 0 ? division[0].negate() : division[0];
    }
}
