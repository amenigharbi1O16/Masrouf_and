/*
 * ============================================================================
 * FICHIER : LocaleController.java
 * MISSION : Appliquer dynamiquement à chaud la langue sélectionnée (Français, Arabe ou Anglais).
 * IMPLÉMENTE : Utilitaire statique encapsulant AndroidX AppCompatDelegate.setApplicationLocales().
 * EFFET À L'ÉCRAN : Recrée automatiquement l'Activity active avec les ressources values/values-ar/values-en
 *                   et bascule l'interface en sens RTL dès que la langue arabe ("ar") est choisie.
 * COMMUNICATIONS : Appelé au clic sur les pilules FR/AR/EN dans WelcomeActivity, ChooseAccountTypeActivity,
 *                  LoginActivity, RegisterActivity, MainActivity (Drawers) et SettingsFragment.
 * ============================================================================
 */
package com.example.masrouf1;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Contrôleur utilitaire appliquant dynamiquement la langue choisie ("fr", "ar", "en").
 */
public final class LocaleController {
    // Constructeur privé : appel exclusivement via LocaleController.apply(tag).
    private LocaleController() { }

    /**
     * Applique la langue via AndroidX AppCompatDelegate sur l'ensemble de l'application (API 24+).
     * @param languageTag code BCP-47 court ("fr", "ar" ou "en").
     */
    public static void apply(String languageTag) {
        // Convertit le tag en LocaleListCompat et laisse AppCompat rafraîchir les écrans ouverts.
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag));
    }
}
