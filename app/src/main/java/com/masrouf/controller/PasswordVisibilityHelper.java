package com.masrouf.controller;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

import com.example.masrouf1.R;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Enforces consistent password visibility behavior across every password field:
 * - Hidden (`textPassword`) displays a closed eye icon (`ic_eye_closed`).
 * - Visible (`text` / `textVisiblePassword`) displays an open eye icon (`ic_eye_open`).
 * - Toggling preserves entered text, cursor selection, typeface, and styling.
 */
public final class PasswordVisibilityHelper {
    private PasswordVisibilityHelper() {}

    /**
     * Recursively attaches the password visibility toggle to every password
     * {@link TextInputLayout} inside {@code root}.
     */
    public static void attachAll(View root) {
        if (root == null) return;
        if (root instanceof TextInputLayout) {
            TextInputLayout layout = (TextInputLayout) root;
            EditText editText = layout.getEditText();
            if (editText != null && (isPasswordInput(editText)
                    || layout.getEndIconMode() == TextInputLayout.END_ICON_PASSWORD_TOGGLE)) {
                attach(layout, editText);
            }
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                attachAll(group.getChildAt(i));
            }
        }
    }

    /** Attaches toggle behavior to a single {@link TextInputLayout} and its {@link EditText}. */
    public static void attach(TextInputLayout layout, EditText editText) {
        if (layout == null || editText == null) return;
        layout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
        layout.setEndIconCheckable(false);
        layout.setEndIconTintList(ColorStateList.valueOf(
                ContextCompat.getColor(layout.getContext(), R.color.black)));
        syncIcon(layout, isPasswordVisible(editText));
        layout.setEndIconOnClickListener(v -> toggle(layout, editText));
    }

    /** Returns {@code true} when the password text is currently readable in plain text. */
    public static boolean isPasswordVisible(EditText editText) {
        if (editText == null) return false;
        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            return false;
        }
        int cls = editText.getInputType() & InputType.TYPE_MASK_CLASS;
        int variation = editText.getInputType() & InputType.TYPE_MASK_VARIATION;
        return !(cls == InputType.TYPE_CLASS_TEXT && variation == InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    /** Toggles visibility while preserving text, cursor position/selection, and typeface. */
    public static void toggle(TextInputLayout layout, EditText editText) {
        if (layout == null || editText == null) return;
        boolean currentlyVisible = isPasswordVisible(editText);
        setPasswordVisible(layout, editText, !currentlyVisible);
    }

    /** Explicitly sets password visibility state and updates the end icon immediately. */
    public static void setPasswordVisible(TextInputLayout layout, EditText editText, boolean visible) {
        if (editText == null) return;
        int selectionStart = editText.getSelectionStart();
        int selectionEnd = editText.getSelectionEnd();
        Typeface typeface = editText.getTypeface();
        Editable currentText = editText.getText();
        String preserved = currentText == null ? "" : currentText.toString();

        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            editText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        if (editText.getText() == null || !preserved.equals(editText.getText().toString())) {
            editText.setText(preserved);
        }
        if (typeface != null) {
            editText.setTypeface(typeface);
        }
        int length = editText.getText() == null ? 0 : editText.getText().length();
        if (selectionStart >= 0 && selectionEnd >= 0) {
            editText.setSelection(Math.min(selectionStart, length), Math.min(selectionEnd, length));
        } else if (editText.hasFocus()) {
            editText.setSelection(length);
        }
        syncIcon(layout, visible);
    }

    /** Syncs the end icon drawable and accessibility description with the current state. */
    public static void syncIcon(TextInputLayout layout, boolean visible) {
        if (layout == null) return;
        layout.setEndIconDrawable(visible ? R.drawable.ic_eye_open : R.drawable.ic_eye_closed);
        layout.setEndIconVisible(true);
        layout.setEndIconContentDescription(layout.getContext().getString(
                visible ? R.string.hide_password : R.string.show_password));
    }

    private static boolean isPasswordInput(EditText editText) {
        if (editText.getTransformationMethod() instanceof PasswordTransformationMethod) {
            return true;
        }
        int cls = editText.getInputType() & InputType.TYPE_MASK_CLASS;
        int variation = editText.getInputType() & InputType.TYPE_MASK_VARIATION;
        return cls == InputType.TYPE_CLASS_TEXT && (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD);
    }
}
