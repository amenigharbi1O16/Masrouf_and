/*
 * ============================================================================
 * FICHIER : AnalyticsActivity.java
 * MISSION : Activity de redirection rétrocompatible ouvrant l'onglet central « Analyses » dans MainActivity.
 * IMPLÉMENTE : Hérite d'AppCompatActivity et redirige aussitôt via Intent explicite avec EXTRA_CENTRAL_TAB="analytics".
 * EFFET À L'ÉCRAN : Aucun layout propre ; bascule immédiatement l'utilisateur sur AnalyticsFragment hébergé dans MainActivity.
 * COMMUNICATIONS : Déclarée dans AndroidManifest.xml ; appelle MainActivity avec CLEAR_TOP | SINGLE_TOP puis finish().
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity-tremplin de compatibilité déclarée dans AndroidManifest.xml.
 */
public final class AnalyticsActivity extends AppCompatActivity {
    /** Redirige vers MainActivity sur l'onglet "analytics" (AnalyticsFragment) puis ferme cette Activity tremplin. */
    @Override protected void onCreate(Bundle state){
        // Appelle l'initialisation système d'AppCompatActivity.
        super.onCreate(state);
        // Envoie un Intent explicite vers MainActivity en ciblant l'onglet "analytics" sans recréer MainActivity.
        startActivity(new Intent(this,MainActivity.class).putExtra(MainActivity.EXTRA_CENTRAL_TAB,"analytics").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        // Retire immédiatement AnalyticsActivity de la pile arrière.
        finish();
    }
}
