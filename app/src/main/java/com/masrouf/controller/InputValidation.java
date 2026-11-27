/*
 * ============================================================================
 * FICHIER : InputValidation.java
 * MISSION : Référentiel unique centralisant les règles de validation Gmail stricte et mot de passe robuste (5 règles).
 * IMPLÉMENTE : Utilitaire statique de expressions régulières et formateurs de messages d'erreur localisés.
 * EFFET À L'ÉCRAN : Commande l'activation des boutons et l'affichage vert/rouge des checklists de règles
 *                   dans RegisterActivity, ChangePasswordActivity, LoginActivity, ChildrenManagementActivity et SettingsFragment.
 * COMMUNICATIONS : Testé unitairement dans InputValidationTest et appelé par MasroufRepository.
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Context;
import android.text.TextUtils;

import com.example.masrouf1.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Référentiel unique des règles de validation d'adresse Gmail stricte et de robustesse du mot de passe (5 critères). */
public final class InputValidation {
    // Regex stricte imposant lettres/chiffres, séparateurs non consécutifs, et domaine exact @gmail.com.
    public static final Pattern GMAIL_PATTERN=Pattern.compile("^[a-z0-9]+(?:[._-][a-z0-9]+)*@gmail\\.com$");
    private static final Pattern GMAIL=GMAIL_PATTERN;
    // Motifs vérifiant majuscule, minuscule, chiffre et caractère spécial.
    private static final Pattern UPPER=Pattern.compile("[A-Z]");
    private static final Pattern LOWER=Pattern.compile("[a-z]");
    private static final Pattern DIGIT=Pattern.compile("[0-9]");
    private static final Pattern SYMBOL=Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private InputValidation() { }

    /** Normalise l'adresse saisie (trim + minuscules) pour l'unicité SQLite. */
    public static String normalizeGmail(String value) { return value==null?"":value.trim().toLowerCase(Locale.ROOT); }
    /** @return true uniquement si la chaîne normalisée correspond au format strict *@gmail.com sans points consécutifs. */
    public static boolean isStrictGmail(String value) { return GMAIL.matcher(normalizeGmail(value)).matches(); }
    /** Alias de compatibilité déléguant à isStrictGmail(). */
    public static boolean isValidGmail(String value) { return isStrictGmail(value); }
    /** Vérifie au moins 8 caractères. */
    public static boolean hasMinimumLength(String password) { return password!=null&&password.length()>=8; }
    /** Vérifie la présence d'au moins une lettre majuscule ASCII. */
    public static boolean hasUppercase(String password) { return password!=null&&UPPER.matcher(password).find(); }
    /** Vérifie la présence d'au moins une lettre minuscule ASCII. */
    public static boolean hasLowercase(String password) { return password!=null&&LOWER.matcher(password).find(); }
    /** Vérifie la présence d'au moins un chiffre. */
    public static boolean hasNumber(String password) { return password!=null&&DIGIT.matcher(password).find(); }
    /** Vérifie la présence d'au moins un symbole non alphanumérique. */
    public static boolean hasSpecialCharacter(String password) { return password!=null&&SYMBOL.matcher(password).find(); }
    /** @return tableau de 5 booléens [longueur>=8, majuscule, minuscule, chiffre, symbole]. */
    public static boolean[] passwordRules(String value) {
        String password=value==null?"":value;
        return new boolean[]{hasMinimumLength(password),hasUppercase(password),hasLowercase(password),hasNumber(password),hasSpecialCharacter(password)};
    }
    /** @return true uniquement lorsque les 5 critères de sécurité du mot de passe sont respectés. */
    public static boolean isValidPassword(String value) { for(boolean rule:passwordRules(value))if(!rule)return false;return true; }
    /** Évalue chaque règle de complexité du mot de passe (utilisé par ChangePasswordActivity). */
    public static boolean[] evaluatePasswordRules(String value) { return passwordRules(value); }
    /** Retourne les 5 identifiants de chaînes localisées décrivant les critères du mot de passe. */
    public static int[] passwordRuleStringIds(){return new int[]{R.string.password_length,R.string.password_uppercase,R.string.password_lowercase,R.string.password_number,R.string.password_symbol};}
    /** Collecte les identifiants R.string des règles non encore satisfaites par la chaîne candidate. */
    public static List<Integer> missingPasswordRuleIds(String value) {
        boolean[] valid=passwordRules(value);int[] ids=passwordRuleStringIds();
        ArrayList<Integer> missing=new ArrayList<>();for(int i=0;i<valid.length;i++)if(!valid[i])missing.add(ids[i]);return missing;
    }
    /** Compose le message d'erreur détaillé énumérant les critères manquants. */
    public static String passwordError(Context context,String value) {
        ArrayList<String> missing=new ArrayList<>();for(int id:missingPasswordRuleIds(value))missing.add(context.getString(id));
        return context.getString(R.string.password_error_specific, TextUtils.join(", ",missing));
    }
}
