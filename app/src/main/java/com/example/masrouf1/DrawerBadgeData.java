/*
 * ============================================================================
 * FICHIER : DrawerBadgeData.java
 * MISSION : Paire de compteurs numériques affichés sous forme de pastilles dans le tiroir latéral PARENT (AdminDrawer).
 * IMPLÉMENTE : DTO immuable retourné de manière asynchrone par MasroufRepository.parentDrawerBadges(userId).
 * EFFET À L'ÉCRAN : Met à jour les pastilles blanches de comptage devant les entrées « Enfants » et « Notifications ».
 * COMMUNICATIONS : Produit par MasroufRepository -> MasroufViewModel.request() -> consommé par AdminDrawer.refreshBadges().
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Compteurs dynamiques affichés sous forme de pastilles dans le tiroir Parent (AdminDrawer).
 */
public final class DrawerBadgeData {
    // Nombre total de comptes enfants rattachés au parent connecté.
    public final int children;
    // Nombre de notifications non lues (read = 0) pour ce parent dans SQLite.
    public final int unreadNotifications;

    /**
     * Construit le jeu de compteurs de badges du menu latéral familial.
     * @param children effectif d'enfants gérés par le foyer.
     * @param unreadNotifications nombre d'alertes non encore marquées comme lues.
     */
    public DrawerBadgeData(int children,int unreadNotifications){this.children=children;this.unreadNotifications=unreadNotifications;}
}
