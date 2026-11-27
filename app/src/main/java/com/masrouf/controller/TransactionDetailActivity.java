/*
 * ============================================================================
 * FICHIER : TransactionDetailActivity.java
 * MISSION : Classe mère abstraite de consultation rapide (lecture seule) d'une dépense ou d'un achat depuis Accueil.
 * IMPLÉMENTE : Hérite de BaseFeatureActivity et génère les TextView titre + montant par programmation en Java.
 * EFFET À L'ÉCRAN : Affiche le libellé en gras forêt (22sp) et le montant formaté en rouge corail (18sp)
 *                   sans exposer ni champ de modification ni bouton de suppression.
 * COMMUNICATIONS : Étendue uniquement par EditExpenseActivity et EditPurchaseActivity (extras "TITLE" et "AMOUNT").
 * ============================================================================
 */
package com.masrouf.controller;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.R;

/**
 * Classe mère abstraite de consultation rapide d'une transaction depuis l'accueil.
 */
abstract class TransactionDetailActivity extends BaseFeatureActivity {
    /**
     * Construit dynamiquement les deux TextView Java (libellé + montant) à partir des Extras de l'Intent.
     */
    @Override protected void onCreate(Bundle state) {
        // Charge la coque commune activity_section.xml avec le titre fourni par la sous-classe.
        super.onCreate(state); showPage(titleResource());
        // Crée un LinearLayout vertical centré verticalement à injecter dans content.
        LinearLayout detail=new LinearLayout(this); detail.setGravity(Gravity.CENTER_VERTICAL); detail.setOrientation(LinearLayout.VERTICAL);
        // Crée le TextView du libellé transmis via l'extra "TITLE" et l'ajoute au conteneur.
        TextView title=new TextView(this); title.setText(getIntent().getStringExtra("TITLE")); title.setTextColor(getColor(R.color.forest)); title.setTextSize(22); title.setTypeface(null,android.graphics.Typeface.BOLD); detail.addView(title);
        // Lit le montant en millimes depuis l'extra "AMOUNT", le formate en DT et l'ajoute sous le titre.
        long amount=getIntent().getLongExtra("AMOUNT",0L); TextView value=new TextView(this); value.setText(CurrencyFormatter.formatMilli(amount)); value.setTextColor(getColor(R.color.coral)); value.setTextSize(18); value.setPadding(0,dp(12),0,0); detail.addView(value);
        // Monte la vue Java complète dans le FrameLayout content de BaseFeatureActivity.
        content.addView(detail,new android.widget.FrameLayout.LayoutParams(-1,-1));
    }

    /** Retourne l'identifiant R.string.* du titre de page défini par EditExpenseActivity ou EditPurchaseActivity. */
    protected abstract int titleResource();
}
