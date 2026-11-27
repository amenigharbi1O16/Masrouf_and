/*
 * ============================================================================
 * FICHIER : CalendarActivity.java
 * MISSION : Activity de redirection rétrocompatible ouvrant l'onglet central « Calendrier » dans MainActivity.
 * IMPLÉMENTE : Hérite d'AppCompatActivity et lance MainActivity avec EXTRA_CENTRAL_TAB="calendar".
 * EFFET À L'ÉCRAN : Aucun écran propre ; affiche directement CalendarFragment au sein de MainActivity.
 * COMMUNICATIONS : Déclarée dans AndroidManifest.xml -> redirige vers MainActivity puis finish().
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity-tremplin de compatibilité déclarée dans AndroidManifest.xml.
 */
public final class CalendarActivity extends AppCompatActivity {
    /** Transfère la navigation vers l'onglet "calendar" de MainActivity puis termine immédiatement l'activité. */
    @Override protected void onCreate(Bundle state){
        // Initialise la super-classe Android.
        super.onCreate(state);
        // Navigue vers MainActivity sur l'onglet Calendrier en réutilisant l'instance existante.
        startActivity(new Intent(this,MainActivity.class).putExtra(MainActivity.EXTRA_CENTRAL_TAB,"calendar").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        // Ferme aussitôt cette Activity relais.
        finish();
    }
}
