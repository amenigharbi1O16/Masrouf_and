package com.masrouf.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.example.masrouf1.R;

public final class FormValidationTextWatcher implements TextWatcher {
    private final TextView feedback;
    public FormValidationTextWatcher(TextView feedback) { this.feedback = feedback; }
    public static boolean isGmail(String value) { return InputValidation.isValidGmail(value); }
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        String value = s == null ? "" : s.toString();
        if (value.isEmpty()) feedback.setText("");
        else {
            boolean valid = isGmail(value);
            feedback.setText(valid ? R.string.gmail_valid : R.string.email_error);
            feedback.setTextColor(feedback.getResources().getColor(valid ? R.color.emerald : R.color.coral, feedback.getContext().getTheme()));
        }
    }
    @Override public void afterTextChanged(Editable s) { }
}
