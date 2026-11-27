/*
 * ============================================================================
 * FICHIER : ProductsActivity.java
 * MISSION : Activity-relais rétrocompatible routant vers l'onglet « Produits » de MainActivity.
 * IMPLÉMENTE : Hérite d'AppCompatActivity et transfère immédiatement via Intent explicite.
 * EFFET À L'ÉCRAN : N'affiche aucun écran propre ; ouvre directement ProductsFragment dans MainActivity.
 * COMMUNICATIONS : Déclarée dans AndroidManifest.xml -> lance MainActivity(EXTRA_CENTRAL_TAB="products") -> finish().
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Activity-tremplin de compatibilité déclarée dans AndroidManifest.xml.
 */
public final class ProductsActivity extends AppCompatActivity {
    /** Redirige vers l'onglet central Produits dans MainActivity puis termine l'Activity tremplin. */
    @Override protected void onCreate(Bundle state){
        // Initialise l'Activity hôte système.
        super.onCreate(state);
        // Demande à MainActivity existante d'afficher l'onglet "products".
        startActivity(new Intent(this,MainActivity.class).putExtra(MainActivity.EXTRA_CENTRAL_TAB,"products").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));
        // Retire ProductsActivity du back-stack.
        finish();
    }
}
