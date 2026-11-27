/*
 * ============================================================================
 * FICHIER : GmailValidationWatcher.java
 * MISSION : Écouteur de saisie (TextWatcher) contrôlant en direct la conformité stricte d'une adresse Gmail.
 * IMPLÉMENTE : android.text.TextWatcher relié aux règles centralisées de InputValidation.isStrictGmail().
 * EFFET À L'ÉCRAN : Met à jour instantanément un TextView indicateur sous le champ email
 *                   (vert émeraude si valide, rouge corail si invalide, vide si effacé).
 * COMMUNICATIONS : Branché dans SettingsFragment (settings_email_input) et utilisable sur tout formulaire email.
 * ============================================================================
 */
package com.example.masrouf1;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.masrouf.controller.InputValidation;

/**
 * Listener TextWatcher explicite branché sur le champ email de SettingsFragment (settings_email_input).
 */
public final class GmailValidationWatcher implements TextWatcher {
    // TextView affichant le message de retour en direct sous le champ d'adresse e-mail.
    private final TextView feedback;

    /** @param feedback TextView recevant le statut de validation Gmail en direct. */
    public GmailValidationWatcher(TextView feedback) { this.feedback = feedback; }

    /** Méthode imposée par l'interface TextWatcher avant modification (aucun traitement nécessaire ici). */
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    /**
     * Exécutée à chaque caractère frappé ou collé : vérifie la syntaxe Gmail et colore le retour visuel.
     */
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Convertit la séquence courante en String sûre (chaîne vide si null).
        String email = s == null ? "" : s.toString();
        // Vide le message tant que l'utilisateur n'a rien saisi.
        if (email.isEmpty()) feedback.setText("");
        else {
            // Vérifie le format strict @gmail.com via InputValidation.
            boolean valid = InputValidation.isStrictGmail(email);
            // Affiche le libellé localisé "gmail_valid" ou "gmail_invalid".
            feedback.setText(valid ? R.string.gmail_valid : R.string.gmail_invalid);
            // Colore en vert émeraude (valide) ou rouge corail (invalide) selon le résultat.
            feedback.setTextColor(feedback.getResources().getColor(valid ? R.color.emerald : R.color.coral, feedback.getContext().getTheme()));
        }
    }

    /** Méthode imposée par l'interface TextWatcher après modification (aucun traitement nécessaire ici). */
    @Override public void afterTextChanged(Editable s) { }
}
