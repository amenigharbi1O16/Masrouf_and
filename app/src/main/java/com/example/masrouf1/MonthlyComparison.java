/*
 * ============================================================================
 * FICHIER : MonthlyComparison.java
 * MISSION : Calculer avec exactitude entière la comparaison de dépense entre mois courant et mois précédent.
 * IMPLÉMENTE : DTO analytique utilisant BigInteger pour calculer le pourcentage d'écart sans perte flottante.
 * EFFET À L'ÉCRAN : Permet d'afficher la progression ou baisse mensuelle (+X % / -X % et différence en DT).
 * COMMUNICATIONS : Construit par MasroufRepository.monthlyComparison(...) depuis MasroufDao.totalSpendingBetween().
 * ============================================================================
 */
package com.example.masrouf1;

import java.math.BigInteger;

/**
 * Comparaison arithmétique exacte (en millimes) entre le mois courant et le mois précédent.
 */
public final class MonthlyComparison {
    // Dépense du mois courant, dépense du mois précédent, et différence signée (current - previous) en millimes.
    public final long currentMilli,previousMilli,differenceMilli;
    // Variation en pourcentage entier par rapport au mois précédent (null si aucune dépense précédente <= 0).
    public final Integer percentChange;

    /**
     * Calcule l'écart absolu et le taux de variation relatif dès la construction de l'objet.
     * @param currentMilli dépense cumulée de la période courante (millimes).
     * @param previousMilli dépense cumulée de la période précédente (millimes).
     */
    public MonthlyComparison(long currentMilli,long previousMilli){
        // Stocke les deux bornes de comparaison et calcule la variation nette en millimes.
        this.currentMilli=currentMilli;this.previousMilli=previousMilli;this.differenceMilli=currentMilli-previousMilli;
        // Évite toute division par zéro : calcule (écart * 100 / previous) en arithmétique exacte BigInteger.
        this.percentChange=previousMilli<=0?null:BigInteger.valueOf(differenceMilli).multiply(BigInteger.valueOf(100))
                .divide(BigInteger.valueOf(previousMilli)).intValue();
    }
}
