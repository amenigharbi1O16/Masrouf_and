package com.masrouf.controller;

import android.content.Context;
import android.text.TextUtils;

import com.example.masrouf1.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Single source of truth for account email and password policy. */
public final class InputValidation {
    public static final Pattern GMAIL_PATTERN=Pattern.compile("^[a-z0-9]+(?:[._-][a-z0-9]+)*@gmail\\.com$");
    private static final Pattern GMAIL=GMAIL_PATTERN;
    private static final Pattern UPPER=Pattern.compile("[A-Z]");
    private static final Pattern LOWER=Pattern.compile("[a-z]");
    private static final Pattern DIGIT=Pattern.compile("[0-9]");
    private static final Pattern SYMBOL=Pattern.compile("[^\\p{L}\\p{N}\\s]");
    private InputValidation() { }

    public static String normalizeGmail(String value) { return value==null?"":value.trim().toLowerCase(Locale.ROOT); }
    public static boolean isStrictGmail(String value) { return GMAIL.matcher(normalizeGmail(value)).matches(); }
    /** Compatibility alias; all email checks delegate to the strict Gmail rule. */
    public static boolean isValidGmail(String value) { return isStrictGmail(value); }
    public static boolean hasMinimumLength(String password) { return password!=null&&password.length()>=8; }
    public static boolean hasUppercase(String password) { return password!=null&&UPPER.matcher(password).find(); }
    public static boolean hasLowercase(String password) { return password!=null&&LOWER.matcher(password).find(); }
    public static boolean hasNumber(String password) { return password!=null&&DIGIT.matcher(password).find(); }
    public static boolean hasSpecialCharacter(String password) { return password!=null&&SYMBOL.matcher(password).find(); }
    public static boolean[] passwordRules(String value) {
        String password=value==null?"":value;
        return new boolean[]{hasMinimumLength(password),hasUppercase(password),hasLowercase(password),hasNumber(password),hasSpecialCharacter(password)};
    }
    public static boolean isValidPassword(String value) { for(boolean rule:passwordRules(value))if(!rule)return false;return true; }
    /**
     * Évalue chaque règle de complexité du mot de passe et retourne un tableau booléen.
     * Chaque index correspond à une règle (longueur, majuscule, minuscule, chiffre, symbole).
     * Utilisé par ChangePasswordActivity pour l'affichage en temps réel.
     */
    public static boolean[] evaluatePasswordRules(String value) { return passwordRules(value); }
    public static int[] passwordRuleStringIds(){return new int[]{R.string.password_length,R.string.password_uppercase,R.string.password_lowercase,R.string.password_number,R.string.password_symbol};}
    public static List<Integer> missingPasswordRuleIds(String value) {
        boolean[] valid=passwordRules(value);int[] ids=passwordRuleStringIds();
        ArrayList<Integer> missing=new ArrayList<>();for(int i=0;i<valid.length;i++)if(!valid[i])missing.add(ids[i]);return missing;
    }
    public static String passwordError(Context context,String value) {
        ArrayList<String> missing=new ArrayList<>();for(int id:missingPasswordRuleIds(value))missing.add(context.getString(id));
        return context.getString(R.string.password_error_specific, TextUtils.join(", ",missing));
    }
}
