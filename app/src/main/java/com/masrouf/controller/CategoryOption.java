/*
 * ============================================================================
 * FICHIER : CategoryOption.java
 * MISSION : Modèle UI immuable décrivant une option de catégorie (icône, couleur, taux) dans les listes de sélection.
 * IMPLÉMENTE : DTO léger utilisé par CategoryAdapter et les assistants de sélection.
 * EFFET À L'ÉCRAN : Permet d'afficher le nom, la pastille couleur et l'icône d'une catégorie sélectionnable.
 * COMMUNICATIONS : Instancié avec SystemCategoryCatalog / CategoryEntity -> consommé par CategoryAdapter.
 * ============================================================================
 */
package com.masrouf.controller;

/**
 * Modèle immuable représentant un choix de catégorie dans les adaptateurs de sélection.
 */
public final class CategoryOption {
    // Clé système, nom affiché, code hexadécimal de couleur et type ("product" / "expense").
    public final String key, name, colorHex, type;
    // Pourcentage d'allocation recommandé ou configuré.
    public final int allocationPercent;
    // ID catalogue système (0 si personnalisé) et nom d'icône vectorielle résolu par CategoryIconResources.
    public final long catalogId;
    public final String iconName;
    // true pour les catégories protégées (ex. "other") qui ne peuvent pas être décochées/supprimées.
    public final boolean protectedCategory;

    /** Constructeur simplifié sans lien catalogue système ni icône spécifique. */
    public CategoryOption(String key, String name, String colorHex, String type, int allocationPercent) {
        this(0,key,name,colorHex,type,allocationPercent,"",false);
    }

    /**
     * Constructeur complet liant l'option UI à une ligne system_categories.
     * @param protectedCategory true pour "other" afin d'interdire sa désélection.
     */
    public CategoryOption(long catalogId,String key,String name,String colorHex,String type,int allocationPercent,String iconName,boolean protectedCategory) {
        this.catalogId=catalogId;this.key=key;this.name=name;this.colorHex=colorHex;this.type=type;
        this.allocationPercent=allocationPercent;this.iconName=iconName;this.protectedCategory=protectedCategory;
    }
}
