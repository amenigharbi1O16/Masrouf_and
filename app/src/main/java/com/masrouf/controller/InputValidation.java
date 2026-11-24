package com.masrouf.controller;

import android.content.Context;
import android.text.TextUtils;

import com.example.masrouf1.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Single source of truth for account email and password policy. */
public final class InputValidation {
    private static final Pattern GMAIL=Pattern.compile("^[a-z0-9._%+-]+@gmail\\.com$");
    private static final Pattern UPPER=Pattern.compile("[A-Z]");
    private static final Pattern LOWER=Pattern.compile("[a-z]");
    private static final Pattern DIGIT=Pattern.compile("[0-9]");
    private static final Pattern SYMBOL=Pattern.compile("[^A-Za-z0-9\\s]");
    private InputValidation() { }

    public static boolean isValidGmail(String value) { return value!=null&&GMAIL.matcher(value).matches(); }
    public static boolean[] passwordRules(String value) {
        String password=value==null?"":value;
        return new boolean[]{password.length()>=8,UPPER.matcher(password).find(),LOWER.matcher(password).find(),DIGIT.matcher(password).find(),SYMBOL.matcher(password).find()};
    }
    public static boolean isValidPassword(String value) { for(boolean rule:passwordRules(value))if(!rule)return false;return true; }
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
