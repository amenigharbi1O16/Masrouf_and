/*
 * ============================================================================
 * FICHIER : ConfirmationActivity.java
 * MISSION : Écran récapitulatif affiché après l'enregistrement réussi d'une dépense ou d'un achat.
 * IMPLÉMENTE : Hérite de BaseFeatureActivity et construit 100 % de ses vues par programmation en Java (sans XML propre).
 * EFFET À L'ÉCRAN : Affiche au centre de la carte un symbole « ✓ » vert géant, le libellé de l'opération,
 *                   son montant formaté en DT et le bouton « Retour à l'accueil » (goHome).
 * COMMUNICATIONS : Lancé via Intent explicite avec Extras "LABEL" (String) et "AMOUNT" (long millimes) ;
 *                  le clic sur le bouton appelle BaseFeatureActivity.goHome() -> MainActivity.
 * ============================================================================
 */
package com.masrouf.controller;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.R;
import com.google.android.material.button.MaterialButton;

/**
 * Écran de confirmation post-enregistrement héritant de BaseFeatureActivity (100 % vues Java).
 */
public final class ConfirmationActivity extends BaseFeatureActivity {
    /**
     * Crée dynamiquement l'interface Java centrée et affiche les données reçues dans l'Intent.
     * @param state état sauvegardé Android éventuel.
     */
    @Override protected void onCreate(Bundle state){
        // Charge la coque commune activity_section.xml avec le titre "Confirmation".
        super.onCreate(state);showPage(R.string.transaction_confirmation);
        // Crée un conteneur vertical centré avec marge interne de 24 dp pour les éléments Java.
        LinearLayout page=new LinearLayout(this);page.setGravity(Gravity.CENTER);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(24),dp(24),dp(24));
        // Coche verte géante "✓" (52sp) symbolisant le succès de l'écriture SQLite.
        TextView mark=new TextView(this);mark.setText("✓");mark.setTextSize(52);mark.setTextColor(getColor(R.color.emerald));mark.setGravity(Gravity.CENTER);page.addView(mark,new LinearLayout.LayoutParams(-1,dp(72)));
        // Libellé de la dépense ou du produit transmis via l'extra "LABEL".
        TextView label=new TextView(this);label.setText(getIntent().getStringExtra("LABEL"));label.setTextColor(getColor(R.color.forest));label.setTextSize(20);label.setGravity(Gravity.CENTER);page.addView(label);
        // Montant formaté en DT à partir de l'extra "AMOUNT" (millimes).
        TextView amount=new TextView(this);amount.setText(CurrencyFormatter.formatMilli(getIntent().getLongExtra("AMOUNT",0)));amount.setTextColor(getColor(R.color.emerald));amount.setTextSize(26);amount.setGravity(Gravity.CENTER);page.addView(amount);
        // Bouton MaterialButton renvoyant l'utilisateur vers MainActivity via goHome().
        MaterialButton done=new MaterialButton(this);done.setText(R.string.go_home);done.setOnClickListener(v->goHome(v));page.addView(done,new LinearLayout.LayoutParams(-1,dp(54)));
        // Injecte l'arborescence Java créée dans le FrameLayout content de BaseFeatureActivity.
        content.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-1));
    }
}
