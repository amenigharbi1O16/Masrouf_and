package com.masrouf.controller;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.OperationResult;
import com.example.masrouf1.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ACTIVITÉ : ChangePasswordActivity
 * RÔLE : Permet à l'utilisateur connecté de modifier son mot de passe.
 *
 * FLUX :
 * 1. L'utilisateur saisit son ancien mot de passe.
 * 2. L'utilisateur saisit et confirme le nouveau mot de passe.
 * 3. Les règles de complexité sont vérifiées en temps réel.
 * 4. Après validation : le hash est mis à jour dans la table `users` via le DAO.
 *
 * CORRESPONDANCE REACT NATIVE : src/app/changer-mot-de-passe.tsx
 */
public final class ChangePasswordActivity extends AppCompatActivity {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private TextInputEditText currentPasswordField;
    private TextInputEditText newPasswordField;
    private TextInputEditText confirmNewPasswordField;
    private MaterialCardView errorCard;
    private MaterialCardView successCard;
    private TextView errorText;
    private MaterialButton submitButton;
    private LinearLayout rulesContainer;
    private MasroufViewModel viewModel;

    // Identifiants des règles de validation de mot de passe
    private final int[] RULE_IDS = InputValidation.passwordRuleStringIds();
    private TextView[] ruleViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        // Initialisation du ViewModel pour accéder au repository
        viewModel = new androidx.lifecycle.ViewModelProvider(
                this, new MasroufViewModelFactory(this)
        ).get(MasroufViewModel.class);

        // Liaisons avec les vues XML
        currentPasswordField = findViewById(R.id.current_password);
        newPasswordField = findViewById(R.id.new_password);
        confirmNewPasswordField = findViewById(R.id.confirm_new_password);
        PasswordVisibilityHelper.attachAll(findViewById(android.R.id.content));
        errorCard = findViewById(R.id.change_password_error_card);
        successCard = findViewById(R.id.change_password_success_card);
        errorText = findViewById(R.id.change_password_error_text);
        submitButton = findViewById(R.id.change_password_submit);
        rulesContainer = findViewById(R.id.password_rules_container);

        // Flèche retour
        View backButton = findViewById(R.id.change_password_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Construction des indicateurs de règles de mot de passe
        buildPasswordRules();

        // Surveillance du nouveau mot de passe pour affichage en temps réel des règles
        newPasswordField.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                refreshRuleIndicators(s.toString());
            }
        });

        // Bouton Enregistrer
        submitButton.setOnClickListener(v -> attemptPasswordChange());
    }

    /**
     * Construction des indicateurs visuels de règles de complexité du mot de passe.
     * Chaque règle est affichée comme un texte coloré (rouge = non satisfait, vert = satisfait).
     */
    private void buildPasswordRules() {
        ruleViews = new TextView[RULE_IDS.length];
        rulesContainer.removeAllViews();
        for (int i = 0; i < RULE_IDS.length; i++) {
            TextView rule = new TextView(this);
            rule.setText(RULE_IDS[i]);
            rule.setTextSize(12f);
            rule.setTextColor(getColor(R.color.coral));
            rule.setTypeface(null, Typeface.NORMAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            params.topMargin = (int) (2 * getResources().getDisplayMetrics().density);
            rulesContainer.addView(rule, params);
            ruleViews[i] = rule;
        }
        refreshRuleIndicators("");
    }

    /**
     * Met à jour la couleur de chaque règle selon le mot de passe saisi.
     * Vert = règle satisfaite, rouge = règle non satisfaite.
     */
    private void refreshRuleIndicators(String password) {
        boolean[] satisfied = InputValidation.evaluatePasswordRules(password);
        for (int i = 0; i < ruleViews.length && i < satisfied.length; i++) {
            ruleViews[i].setTextColor(getColor(satisfied[i] ? R.color.emerald : R.color.coral));
        }
    }

    /**
     * Validation et envoi du changement de mot de passe.
     * Vérifie : ancien MDP correct, nouveau MDP valide, confirmation identique.
     */
    private void attemptPasswordChange() {
        hideMessages();
        String currentPwd = getText(currentPasswordField);
        String newPwd = getText(newPasswordField);
        String confirmPwd = getText(confirmNewPasswordField);

        // Validation des champs obligatoires
        if (currentPwd.isEmpty()) {
            showError(getString(R.string.current_password_required));
            return;
        }
        if (newPwd.isEmpty()) {
            showError(getString(R.string.new_password_required));
            return;
        }
        if (!InputValidation.isValidPassword(newPwd)) {
            showError(getString(R.string.password_rules_not_met));
            return;
        }
        if (!newPwd.equals(confirmPwd)) {
            showError(getString(R.string.passwords_dont_match));
            return;
        }
        if (newPwd.equals(currentPwd)) {
            showError(getString(R.string.password_same_as_current));
            return;
        }

        // Désactivation du bouton pendant le traitement
        submitButton.setEnabled(false);
        long userId = SessionStore.userId(this);

        // Vérification de l'ancien mot de passe et mise à jour
        viewModel.<OperationResult<Boolean>>request(
                repository -> repository.changePassword(userId, currentPwd, newPwd),
                state -> {
                    submitButton.setEnabled(true);
                    if (state.failed || state.data == null) {
                        showError(getString(R.string.auth_error));
                        return;
                    }
                    OperationResult<Boolean> result = state.data;
                    if (result.succeeded()) {
                        showSuccess();
                        // Retour automatique après 2 secondes
                        submitButton.postDelayed(this::finish, 2000);
                    } else if ("wrong_password".equals(result.error)) {
                        showError(getString(R.string.current_password_wrong));
                    } else {
                        showError(getString(R.string.change_password_failed));
                    }
                }
        );
    }

    private String getText(TextInputEditText field) {
        return field.getText() != null ? field.getText().toString() : "";
    }

    private void showError(String message) {
        errorText.setText(message);
        errorCard.setVisibility(View.VISIBLE);
        successCard.setVisibility(View.GONE);
    }

    private void showSuccess() {
        successCard.setVisibility(View.VISIBLE);
        errorCard.setVisibility(View.GONE);
    }

    private void hideMessages() {
        errorCard.setVisibility(View.GONE);
        successCard.setVisibility(View.GONE);
    }
}
