/*
 * ============================================================================
 * FICHIER : BaseFeatureActivity.java
 * MISSION : Classe mère abstraite fournissant une coque d'écran commune aux formulaires et fiches secondaires.
 * IMPLÉMENTE : Hérite d'AppCompatActivity, gonfle activity_section.xml (toolbar + carte blanche extensible + pied hors ligne).
 * EFFET À L'ÉCRAN : Affiche le titre de page dans la barre du haut et héberge le contenu métier dans FrameLayout #section_content.
 * COMMUNICATIONS : Étendue par AddExpenseActivity, AddProductActivity, ComparePriceActivity, ProductDetailsActivity,
 *                  TransactionDetailsActivity, TransactionDetailActivity, ConfirmationActivity et MoneyRequestActivity.
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.MainActivity;
import com.example.masrouf1.R;

/**
 * Classe mère abstraite partagée par les Activities secondaires de com.masrouf.controller.
 */
abstract class BaseFeatureActivity extends AppCompatActivity {
    // Conteneur FrameLayout central dans lequel chaque écran enfant gonfle ou ajoute ses propres vues.
    protected FrameLayout content;

    /**
     * Charge le gabarit activity_section.xml, définit le titre en haut et mémorise le conteneur content.
     * @param title identifiant R.string.* du titre de l'écran.
     */
    protected void showPage(int title) {
        // Monte le layout XML partagé avec ConstraintLayout + MaterialToolbar.
        setContentView(R.layout.activity_section);
        // Applique le titre localisé sur la barre supérieure.
        ((TextView)findViewById(R.id.section_title)).setText(title);
        // Prépare la référence du conteneur central où les sous-classes insèrent leur contenu.
        content=findViewById(R.id.section_content);
    }

    /** Convertit une dimension en dp vers pixels réels pour les sous-classes créant des vues Java. */
    protected int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }

    /**
     * Gestionnaire XML onClick (android:onClick="goHome") du bouton retour/accueil ramenant à MainActivity.
     * @param ignored vue cliquée transmise par Android.
     */
    public void goHome(View ignored) { startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish(); }
}
