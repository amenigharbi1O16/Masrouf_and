package com.masrouf.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.example.masrouf1.R;

import java.util.regex.Pattern;

public final class FormValidationTextWatcher implements TextWatcher {
    public static final Pattern GMAIL_PATTERN=InputValidation.GMAIL_PATTERN;
    private final TextView feedback;
    public FormValidationTextWatcher(TextView feedback) { this.feedback = feedback; }
    public static boolean isStrictGmail(String value) { return InputValidation.isStrictGmail(value); }
    public static boolean isGmail(String value) { return isStrictGmail(value); }
    public static boolean hasMinimumLength(String password) { return InputValidation.hasMinimumLength(password); }
    public static boolean hasUppercase(String password) { return InputValidation.hasUppercase(password); }
    public static boolean hasLowercase(String password) { return InputValidation.hasLowercase(password); }
    public static boolean hasNumber(String password) { return InputValidation.hasNumber(password); }
    public static boolean hasSpecialCharacter(String password) { return InputValidation.hasSpecialCharacter(password); }
    public static boolean isValidPassword(String password) { return InputValidation.isValidPassword(password); }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        String value = s == null ? "" : s.toString();
        if (value.isEmpty()) feedback.setText("");
        else {
            boolean valid = isStrictGmail(value);
            feedback.setText(valid ? R.string.gmail_valid : R.string.email_error);
            feedback.setTextColor(feedback.getResources().getColor(valid ? R.color.emerald : R.color.coral, feedback.getContext().getTheme()));
        }
    }
    @Override public void afterTextChanged(Editable s) { }
}
