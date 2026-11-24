package com.example.masrouf1;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;

import com.masrouf.controller.InputValidation;

/** Named TextWatcher used by the account settings email field. */
public final class GmailValidationWatcher implements TextWatcher {
    private final TextView feedback;

    public GmailValidationWatcher(TextView feedback) { this.feedback = feedback; }

    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        String email = s == null ? "" : s.toString();
        if (email.isEmpty()) feedback.setText("");
        else {
            boolean valid = InputValidation.isValidGmail(email);
            feedback.setText(valid ? R.string.gmail_valid : R.string.gmail_invalid);
            feedback.setTextColor(feedback.getResources().getColor(valid ? R.color.emerald : R.color.coral, feedback.getContext().getTheme()));
        }
    }
    @Override public void afterTextChanged(Editable s) { }
}
