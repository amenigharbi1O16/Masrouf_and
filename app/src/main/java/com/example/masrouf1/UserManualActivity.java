/*
 * ============================================================================
 * FICHIER : UserManualActivity.java
 * MISSION : Activity passerelle déclenchant l'ouverture du manuel utilisateur PDF embarqué.
 * IMPLÉMENTE : Hérite d'AppCompatActivity et lance immédiatement un Intent implicite ACTION_VIEW.
 * EFFET À L'ÉCRAN : Aucune interface propre affichée ; ouvre la visionneuse PDF externe avec manuel_utilisation.pdf puis se ferme.
 * COMMUNICATIONS : Ouverte depuis SettingsFragment -> appelle SystemIntents.openManual(this) (FileProvider) -> finish().
 * ============================================================================
 */
package com.example.masrouf1;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity tremplin ouvrant le manuel d'utilisation PDF embarqué via SystemIntents.openManual (FileProvider),
 * puis fermant immédiatement cet écran pour laisser l'utilisateur dans la visionneuse PDF externe.
 */
public final class UserManualActivity extends AppCompatActivity {
    /**
     * Déclenche l'ouverture du manuel PDF dès la création de l'Activity et termine celle-ci aussitôt.
     * @param state état sauvegardé éventuel (inutilisé ici).
     */
    @Override protected void onCreate(Bundle state){
        // Appelle l'initialisation système d'AppCompatActivity.
        super.onCreate(state);
        // Copie res/raw/manuel_utilisation.pdf en cache et lance Intent.ACTION_VIEW via FileProvider.
        SystemIntents.openManual(this);
        // Ferme l'Activity tremplin pour ne pas laisser d'écran blanc dans la pile de navigation.
        finish();
    }
}
