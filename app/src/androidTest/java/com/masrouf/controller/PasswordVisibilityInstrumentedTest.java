/*
 * ============================================================================
 * FICHIER : PasswordVisibilityInstrumentedTest.java
 * MISSION : Tests instrumentés UI vérifiant le comportement exact de
 *           PasswordVisibilityHelper sur tous les écrans contenant un mot de
 *           passe (Login, Register, ChangePassword, dialogue d'ajout enfant) :
 *           œil fermé par défaut, œil ouvert si visible, préservation du texte
 *           et du curseur, et vérification pixel-à-pixel de la ressource drawable.
 * IMPLÉMENTE : Suite @RunWith(AndroidJUnit4.class) utilisant ActivityScenario
 *              et comparaison de silhouettes bitmap sur TextInputLayout.
 * EFFET À L'ÉCRAN : Lance temporairement les écrans concernés sur appareil/émulateur
 *                   pendant l'exécution de connectedAndroidTest.
 * COMMUNICATIONS :
 *   → PasswordVisibilityHelper, LoginActivity, RegisterActivity,
 *     ChangePasswordActivity
 * ============================================================================
 */
package com.masrouf.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.text.method.PasswordTransformationMethod;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.masrouf1.R;
import com.google.android.material.textfield.TextInputLayout;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies closed-eye by default (hidden password), open-eye when revealed,
 * immediate icon synchronization, and preserved text + cursor position across all forms.
 */
@RunWith(AndroidJUnit4.class)
public final class PasswordVisibilityInstrumentedTest {

    @Test
    public void loginPasswordToggleUsesClosedEyeWhenHiddenAndOpenEyeWhenVisible() {
        try (ActivityScenario<LoginActivity> scenario = ActivityScenario.launch(LoginActivity.class)) {
            scenario.onActivity(activity -> verifyPasswordField(
                    activity, activity.findViewById(R.id.login_password)));
        }
    }

    @Test
    public void registerPasswordAndConfirmToggleIndependentlyPreservingTextAndCursor() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), RegisterActivity.class);
        intent.putExtra("TYPE", "personal");
        try (ActivityScenario<RegisterActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                verifyPasswordField(activity, activity.findViewById(R.id.register_password));
                verifyPasswordField(activity, activity.findViewById(R.id.register_confirm));
            });
        }
    }

    @Test
    public void changePasswordAllThreeFieldsToggleAccurately() {
        try (ActivityScenario<ChangePasswordActivity> scenario = ActivityScenario.launch(ChangePasswordActivity.class)) {
            scenario.onActivity(activity -> {
                verifyPasswordField(activity, activity.findViewById(R.id.current_password));
                verifyPasswordField(activity, activity.findViewById(R.id.new_password));
                verifyPasswordField(activity, activity.findViewById(R.id.confirm_new_password));
            });
        }
    }

    @Test
    public void addChildDialogPasswordFieldTogglesAccurately() {
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context themed = new ContextThemeWrapper(
                    ApplicationProvider.getApplicationContext(), R.style.Theme_Masrouf1);
            View form = LayoutInflater.from(themed).inflate(R.layout.dialog_add_child, null, false);
            PasswordVisibilityHelper.attachAll(form);
            verifyPasswordField(themed, form.findViewById(R.id.dialog_child_password));
        });
    }

    private void verifyPasswordField(Context context, EditText field) {
        assertNotNull(field);
        TextInputLayout layout = findParentLayout(field);
        assertNotNull(layout);
        View eyeIcon = layout.findViewById(com.google.android.material.R.id.text_input_end_icon);
        assertNotNull(eyeIcon);

        // Hidden by default -> closed eye
        assertFalse(PasswordVisibilityHelper.isPasswordVisible(field));
        assertTrue(field.getTransformationMethod() instanceof PasswordTransformationMethod);
        assertEquals(context.getString(R.string.show_password), layout.getEndIconContentDescription());
        assertIconEquals("Expected closed eye icon when hidden by default",
                context, layout.getEndIconDrawable(), R.drawable.ic_eye_closed);
        assertFalse("Must NOT display open eye icon when hidden",
                countDiffPixels(context, layout.getEndIconDrawable(), R.drawable.ic_eye_open) == 0);

        // Type password and move cursor inside the string
        field.setText("Pass123!@");
        field.setSelection(4);

        // Tap eye -> revealed + open eye + preserved text & cursor
        eyeIcon.performClick();
        assertTrue(PasswordVisibilityHelper.isPasswordVisible(field));
        assertFalse(field.getTransformationMethod() instanceof PasswordTransformationMethod);
        assertEquals("Pass123!@", field.getText().toString());
        assertEquals(4, field.getSelectionStart());
        assertEquals(4, field.getSelectionEnd());
        assertEquals(context.getString(R.string.hide_password), layout.getEndIconContentDescription());
        assertIconEquals("Expected open eye icon when password is visible",
                context, layout.getEndIconDrawable(), R.drawable.ic_eye_open);
        assertFalse("Must NOT display closed eye icon when visible",
                countDiffPixels(context, layout.getEndIconDrawable(), R.drawable.ic_eye_closed) == 0);

        // Tap eye again -> hidden + closed eye + preserved text & cursor
        eyeIcon.performClick();
        assertFalse(PasswordVisibilityHelper.isPasswordVisible(field));
        assertTrue(field.getTransformationMethod() instanceof PasswordTransformationMethod);
        assertEquals("Pass123!@", field.getText().toString());
        assertEquals(4, field.getSelectionStart());
        assertEquals(4, field.getSelectionEnd());
        assertEquals(context.getString(R.string.show_password), layout.getEndIconContentDescription());
        assertIconEquals("Expected closed eye icon after re-hiding password",
                context, layout.getEndIconDrawable(), R.drawable.ic_eye_closed);
    }

    private void assertIconEquals(String msg, Context context, android.graphics.drawable.Drawable actual, int expectedResId) {
        int diff = countDiffPixels(context, actual, expectedResId);
        int oppositeResId = expectedResId == R.drawable.ic_eye_closed
                ? R.drawable.ic_eye_open : R.drawable.ic_eye_closed;
        int oppositeDiff = countDiffPixels(context, actual, oppositeResId);
        assertTrue(msg + " (diff=" + diff + ", oppositeDiff=" + oppositeDiff
                + ", cls=" + (actual == null ? "null" : actual.getClass().getSimpleName()) + ")",
                diff <= 80 && oppositeDiff > diff * 2);
    }

    private int countDiffPixels(Context context, android.graphics.drawable.Drawable actual, int expectedResId) {
        android.graphics.drawable.Drawable expected = androidx.core.content.ContextCompat.getDrawable(context, expectedResId);
        assertNotNull(actual);
        assertNotNull(expected);
        android.graphics.Bitmap bActual = toMaskBitmap(actual);
        android.graphics.Bitmap bExpected = toMaskBitmap(expected);
        int diff = 0;
        for (int y = 0; y < bActual.getHeight(); y++) {
            for (int x = 0; x < bActual.getWidth(); x++) {
                if (bActual.getPixel(x, y) != bExpected.getPixel(x, y)) diff++;
            }
        }
        return diff;
    }

    private android.graphics.Bitmap toMaskBitmap(android.graphics.drawable.Drawable drawable) {
        int w = Math.max(48, drawable.getIntrinsicWidth());
        int h = Math.max(48, drawable.getIntrinsicHeight());
        android.graphics.Bitmap raw = android.graphics.Bitmap.createBitmap(
                w, h, android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(raw);
        android.graphics.Rect savedBounds = new android.graphics.Rect(drawable.getBounds());
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        drawable.setBounds(savedBounds);

        // Normalize to binary alpha silhouette (ignores wrapper tint nuances) and verify non-empty
        android.graphics.Bitmap mask = android.graphics.Bitmap.createBitmap(
                w, h, android.graphics.Bitmap.Config.ARGB_8888);
        int nonEmpty = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean opaque = (raw.getPixel(x, y) >>> 24) > 32;
                if (opaque) nonEmpty++;
                mask.setPixel(x, y, opaque ? android.graphics.Color.BLACK : android.graphics.Color.TRANSPARENT);
            }
        }
        assertTrue("Rendered eye drawable should not be empty", nonEmpty > 0);
        return mask;
    }

    private TextInputLayout findParentLayout(View view) {
        View current = view;
        while (current != null && current.getParent() instanceof View) {
            current = (View) current.getParent();
            if (current instanceof TextInputLayout) {
                return (TextInputLayout) current;
            }
        }
        return null;
    }
}
