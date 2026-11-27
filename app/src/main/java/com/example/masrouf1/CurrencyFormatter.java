/*
 * ============================================================================
 * FICHIER : CurrencyFormatter.java
 * MISSION : Point d'entrée unique de formatage monétaire pour les écrans et notifications.
 * IMPLÉMENTE : Classe utilitaire statique non instanciable déléguant à MoneyUtils.formatMilli().
 * EFFET À L'ÉCRAN : Uniformise l'affichage de chaque montant en dinars tunisiens (ex. "1 250,000 DT")
 *                   sur toutes les cartes, graphiques, notifications et dialogues.
 * COMMUNICATIONS : Appelé par HomeFragment, BudgetCardView, AnalyticsRenderer, NotificationsActivity,
 *                  BudgetNotificationManager et tous les adaptateurs affichant un prix.
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Façade statique de formatage monétaire utilisée dans toute l'UI et les notifications.
 */
public final class CurrencyFormatter {
    // Constructeur privé : appel exclusivement statique via CurrencyFormatter.formatMilli(...).
    private CurrencyFormatter() { }

    /**
     * Convertit un montant entier en millimes vers sa représentation texte formatée avec devise.
     * @param milli montant entier en millimes (1 DT = 1 000 millimes).
     * @return chaîne formatée avec 3 décimales fixes et suffixe " DT".
     */
    public static String formatMilli(long milli) { return MoneyUtils.formatMilli(milli); }
}
