/*
 * ============================================================================
 * FICHIER : FormValidationTextWatcher.java
 * MISSION : Écouteur de frappe (TextWatcher explicite) validant en temps réel
 *           une adresse Gmail saisie dans LoginActivity ou RegisterActivity
 *           et exposant les helpers statiques de contrôle InputValidation.
 * IMPLÉMENTE : android.text.TextWatcher avec délégation statique vers
 *              InputValidation (isStrictGmail, isValidPassword, etc.) et
 *              mise à jour immédiate de couleur/texte du TextView feedback.
 * EFFET À L'ÉCRAN : Affiche dynamiquement « Gmail valide » en vert émeraude
 *                   ou le message d'erreur Gmail en corail sous le champ email.
 * COMMUNICATIONS :
 *   ← LoginActivity, RegisterActivity (addTextChangedListener)
 *   → InputValidation (regex Gmail + règles de complexité mot de passe)
 * ============================================================================
 */
package com.masrouf.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.example.masrouf1.R;

import java.util.regex.Pattern;

/**
 * Écouteur de frappe (TextWatcher explicite) utilisé par LoginActivity et RegisterActivity
 * pour valider en temps réel la saisie d'une adresse Gmail et exposer les règles InputValidation.
 * Effet concret à l'écran : met à jour le TextView feedback (vert émeraude si valide, corail sinon).
 */
public final class FormValidationTextWatcher implements TextWatcher {
    /** Expression régulière Gmail partagée depuis InputValidation. */
    public static final Pattern GMAIL_PATTERN=InputValidation.GMAIL_PATTERN;
    /** TextView affichant le retour visuel de validation en direct. */
    private final TextView feedback;

    /** @param feedback TextView recevant le message d'état Gmail en direct. */
    public FormValidationTextWatcher(TextView feedback) { this.feedback = feedback; }
    /** Vérifie que l'adresse correspond au format Gmail strict. */
    public static boolean isStrictGmail(String value) { return InputValidation.isStrictGmail(value); }
    /** Alias conservé pour compatibilité ascendante. */
    public static boolean isGmail(String value) { return isStrictGmail(value); }
    /** Contrôle la longueur minimale du mot de passe. */
    public static boolean hasMinimumLength(String password) { return InputValidation.hasMinimumLength(password); }
    /** Contrôle la présence d'au moins une majuscule. */
    public static boolean hasUppercase(String password) { return InputValidation.hasUppercase(password); }
    /** Contrôle la présence d'au moins une minuscule. */
    public static boolean hasLowercase(String password) { return InputValidation.hasLowercase(password); }
    /** Contrôle la présence d'au moins un chiffre. */
    public static boolean hasNumber(String password) { return InputValidation.hasNumber(password); }
    /** Contrôle la présence d'au moins un caractère spécial. */
    public static boolean hasSpecialCharacter(String password) { return InputValidation.hasSpecialCharacter(password); }
    /** Contrôle l'ensemble des 5 règles de sécurité du mot de passe. */
    public static boolean isValidPassword(String password) { return InputValidation.isValidPassword(password); }
    /** Hook TextWatcher inutilisé avant modification du texte. */
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    /** Recalcule la validité à chaque caractère saisi/supprimé et colore le retour utilisateur. */
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        String value = s == null ? "" : s.toString();
        if (value.isEmpty()) feedback.setText("");
        else {
            boolean valid = isStrictGmail(value);
            feedback.setText(valid ? R.string.gmail_valid : R.string.email_error);
            feedback.setTextColor(feedback.getResources().getColor(valid ? R.color.emerald : R.color.coral, feedback.getContext().getTheme()));
        }
    }
    /** Hook TextWatcher inutilisé après modification du texte. */
    @Override public void afterTextChanged(Editable s) { }
}
