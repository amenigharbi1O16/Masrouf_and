/*
 * ============================================================================
 * FICHIER : FormPriceTextWatcher.java
 * MISSION : Écouteur de saisie (TextWatcher explicite) calculant automatiquement le prix unitaire en temps réel.
 * IMPLÉMENTE : android.text.TextWatcher observateur des champs prix total et quantité.
 * EFFET À L'ÉCRAN : Actualise instantanément le TextView de résultat ("Prix unitaire : X,XXX DT" ou "Non disponible").
 * COMMUNICATIONS : Branché dans AddProductActivity et ComparePriceActivity ; utilise MoneyInput et MoneyMath.
 * ============================================================================
 */
package com.masrouf.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MoneyMath;

/**
 * Listener TextWatcher explicite branché sur les champs prix et quantité (AddProductActivity, ComparePriceActivity).
 */
public final class FormPriceTextWatcher implements TextWatcher {
    // Références vers le champ montant total, le champ quantité et la zone texte de restitution.
    private final EditText totalPrice;
    private final EditText quantity;
    private final TextView result;

    /**
     * Lie cet écouteur aux trois vues concernées par le calcul unitaire.
     * @param totalPrice champ montant ; @param quantity champ quantité ; @param result zone texte du prix unitaire affiché.
     */
    public FormPriceTextWatcher(EditText totalPrice, EditText quantity, TextView result) {
        this.totalPrice=totalPrice; this.quantity=quantity; this.result=result;
    }

    /** Hook requis par TextWatcher avant modification (inutilisé). */
    @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
    /** Hook requis par TextWatcher pendant modification (traitement effectué dans afterTextChanged). */
    @Override public void onTextChanged(CharSequence s,int start,int before,int count) { }

    /**
     * Exécuté après chaque frappe dans prix ou quantité : calcule total/quantité en millimes et rafraîchit l'écran.
     */
    @Override public void afterTextChanged(Editable editable) {
        try {
            // Convertit les saisies textuelles (avec virgule/point) en millimes entiers exacts.
            long price=MoneyInput.toMilli(totalPrice.getText().toString());
            long quantityMilli=MoneyInput.toMilli(quantity.getText().toString());
            // Affiche le prix unitaire exact uniquement lorsque prix > 0 et quantité > 0.
            if(price>0&&quantityMilli>0) result.setText(result.getContext().getString(
                    com.example.masrouf1.R.string.unit_price_result,
                    CurrencyFormatter.formatMilli(MoneyMath.pricePerUnitMilli(price,quantityMilli))));
            // Sinon affiche le libellé d'attente "Non calculé".
            else result.setText(com.example.masrouf1.R.string.unit_price_not_available);
        } catch(RuntimeException ignored) {
            // Toute saisie incomplète ou non numérique (ex. "." seul) remet proprement "Non calculé".
            result.setText(com.example.masrouf1.R.string.unit_price_not_available);
        }
    }
}
