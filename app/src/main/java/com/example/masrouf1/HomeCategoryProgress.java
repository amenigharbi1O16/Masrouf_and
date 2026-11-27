/*
 * ============================================================================
 * FICHIER : HomeCategoryProgress.java
 * MISSION : Modèle UI d'une ligne de progression budgétaire par catégorie sur l'écran d'Accueil.
 * IMPLÉMENTE : DTO immuable stockant identité visuelle (couleur, icône, clé) et métriques financières exactes.
 * EFFET À L'ÉCRAN : Consommé par CategoryProgressCardView dans HomeFragment pour dessiner chaque barre de suivi.
 * COMMUNICATIONS : Calculé dans MasroufRepository.loadHome(...) -> stocké dans HomeDashboardData -> rendu par HomeFragment.
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Modèle de progression réelle d'une enveloppe catégorie affiché dans HomeFragment par CategoryProgressCardView.
 */
public final class HomeCategoryProgress {
    // Identifiant SQLite de la catégorie dans la table categories.
    public final long categoryId;
    // Nom, couleur hexadécimale, type ("product"/"expense"), clé catalogue et identifiant d'icône vectorielle.
    public final String name, colorHex, type, catalogKey, iconName;
    // Montant alloué à cette enveloppe et montant déjà dépensé sur la période (en millimes).
    public final long allocatedMilli, spentMilli;
    // Taux planifié entier (%), taux précis en points de base (10000 = 100 %) et pourcentage réel consommé.
    public final int plannedPercent, plannedPercentBps, consumedPercent;

    /** Constructeur de compatibilité calculant plannedPercentBps à partir de plannedPercent * 100. */
    public HomeCategoryProgress(long categoryId, String name, String colorHex, String type,
                                String catalogKey, String iconName, long allocatedMilli,
                                long spentMilli, int plannedPercent, int consumedPercent) {
        this(categoryId, name, colorHex, type, catalogKey, iconName, allocatedMilli, spentMilli, plannedPercent, plannedPercent * 100, consumedPercent);
    }

    /** Constructeur complet initialisant toutes les valeurs d'affichage de CategoryProgressCardView. */
    public HomeCategoryProgress(long categoryId, String name, String colorHex, String type,
                                String catalogKey, String iconName, long allocatedMilli,
                                long spentMilli, int plannedPercent, int plannedPercentBps, int consumedPercent) {
        this.categoryId = categoryId;
        this.name = name;
        this.colorHex = colorHex;
        this.type = type;
        this.catalogKey = catalogKey;
        this.iconName = iconName;
        this.allocatedMilli = allocatedMilli;
        this.spentMilli = spentMilli;
        this.plannedPercent = plannedPercent;
        this.plannedPercentBps = plannedPercentBps;
        this.consumedPercent = consumedPercent;
    }
}
