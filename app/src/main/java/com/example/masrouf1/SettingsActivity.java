/*
 * ============================================================================
 * FICHIER : SettingsActivity.java
 * MISSION : Activity-tremplin de compatibilité routant toute navigation directe vers l'onglet Paramètres de MainActivity.
 * IMPLÉMENTE : Hérite d'AppCompatActivity ; dispatche un Intent explicite avec EXTRA_CENTRAL_TAB="settings".
 * EFFET À L'ÉCRAN : Aucun layout propre ; ouvre directement SettingsFragment dans MainActivity.
 * COMMUNICATIONS : Déclarée dans AndroidManifest.xml -> lance MainActivity avec CLEAR_TOP | SINGLE_TOP -> finish().
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity-tremplin de compatibilité déclarée dans AndroidManifest.xml.
 */
public final class SettingsActivity extends AppCompatActivity {
    /** Transfère immédiatement l'utilisateur sur l'onglet Paramètres de MainActivity et ferme ce relais. */
    @Override
    protected void onCreate(Bundle state) {
        // Initialise l'Activity système.
        super.onCreate(state);
        // Déclenche MainActivity en demandant l'affichage de l'onglet "settings" sans dupliquer l'instance.
        startActivity(new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_CENTRAL_TAB, "settings")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        // Retire SettingsActivity de l'historique de navigation.
        finish();
    }
}
