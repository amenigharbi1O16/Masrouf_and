/*
 * ============================================================================
 * FICHIER : AppRole.java
 * MISSION : Énumération officielle des profils utilisateurs supportés par Masrouf Android natif.
 * IMPLÉMENTE : Enum AppRole avec les 3 valeurs réelles : PARENT, USER et CHILD (aucun rôle Admin distinct).
 * EFFET À L'ÉCRAN : Conditionne le choix du Side Drawer (AppDrawer vs AdminDrawer), les onglets/boutons autorisés
 *                   sur HomeFragment / SettingsFragment / AnalyticsRenderer et les vérifications de sécurité SectionActivity.
 * COMMUNICATIONS : Lu depuis UserEntity.role, SessionStore.role() et propagé à MainActivity, HomeFragment et Repository.
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Énumération des trois profils réels supportés par Masrouf Android natif :
 * PARENT ("parent"), USER ("user" = utilisateur normal) et CHILD ("child").
 * Aucun rôle Admin distinct n'existe dans l'application.
 */
public enum AppRole {
    // PARENT : gère son foyer et ses enfants ; USER : compte personnel ; CHILD : compte enfant supervisé.
    PARENT("parent"), USER("user"), CHILD("child");

    // Chaîne canonique stockée dans la colonne SQLite users.role et SharedPreferences.
    private final String value;

    /** Initialise la constante d'énumération avec sa clé texte SQLite correspondante. */
    AppRole(String value) { this.value = value; }

    /**
     * Retourne la chaîne stockée en base SQLite (table users.role).
     * @return "parent", "user" ou "child".
     */
    public String value() { return value; }

    /**
     * Convertit de façon sûre une chaîne issue de SQLite ou d'un Intent vers un AppRole typé.
     * Note d'audit : l'ancien libellé "admin" redirige vers PARENT (absence de rôle Admin système distinct).
     * @param value chaîne brute lue en base ou en préférence.
     * @return AppRole correspondant, ou USER par repli sécurisé.
     */
    public static AppRole from(String value) {
        // Rétrocompatibilité avec l'ancien nommage "admin" -> redirigé vers le rôle PARENT du foyer.
        if("admin".equalsIgnoreCase(value)) return PARENT;
        // Parcourt les trois rôles officiels en ignorant la casse.
        for (AppRole role : values()) if (role.value.equalsIgnoreCase(value)) return role;
        // Valeur par défaut sécurisée si la chaîne est nulle ou inconnue.
        return USER;
    }
}
