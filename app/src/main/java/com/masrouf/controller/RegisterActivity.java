package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.LocaleController;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.OperationResult;
import com.example.masrouf1.R;
import java.util.Locale;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * RegisterActivity — matches screenshot design exactly:
 *  - Language toggle (FR/AR/EN) at top right
 *  - "SUIVI DE BUDGET FAMILIAL" subtitle
 *  - Account type toggle (Personal / Famille)
 *  - Blue info banner when Famille selected
 *  - Form: name, email, password, confirm
 *  - "Créer un compte" primary button
 *  - "Se connecter" secondary button
 */
public final class RegisterActivity extends AppCompatActivity {

    private MasroufViewModel viewModel;
    private String accountType;

    // Views
    private TextView tabPersonal, tabFamily;
    private MaterialCardView familyInfoBanner;
    private TextInputEditText editName, editEmail, editPassword, editConfirm;
    private TextInputLayout layoutName, layoutEmail, layoutPassword, layoutConfirm;
    private TextView emailFeedback, errorText;
    private MaterialButton submitBtn, toLoginBtn;
    private LinearLayout passwordRulesPanel;
    private TextView ruleLength, ruleUppercase, ruleLowercase, ruleNumber, ruleSymbol;
    private ImageView backBtn;

    private final int[] ruleTextIds = InputValidation.passwordRuleStringIds();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this)).get(MasroufViewModel.class);

        // Get account type from intent
        accountType = getIntent().getStringExtra("TYPE");
        if (accountType == null) accountType = "personal";

        // Bind views
        backBtn = findViewById(R.id.register_back);
        tabPersonal = findViewById(R.id.tab_personal);
        tabFamily = findViewById(R.id.tab_family);
        familyInfoBanner = findViewById(R.id.family_info_banner);
        editName = findViewById(R.id.register_name);
        editEmail = findViewById(R.id.register_email);
        editPassword = findViewById(R.id.register_password);
        editConfirm = findViewById(R.id.register_confirm);
        emailFeedback = findViewById(R.id.register_email_feedback);
        errorText = findViewById(R.id.register_error);
        submitBtn = findViewById(R.id.register_submit);
        toLoginBtn = findViewById(R.id.register_to_login);
        passwordRulesPanel = findViewById(R.id.password_rules_panel);
        ruleLength = findViewById(R.id.rule_length);
        ruleUppercase = findViewById(R.id.rule_uppercase);
        ruleLowercase = findViewById(R.id.rule_lowercase);
        ruleNumber = findViewById(R.id.rule_number);
        ruleSymbol = findViewById(R.id.rule_symbol);

        // Back
        backBtn.setOnClickListener(v -> finish());
        PasswordVisibilityHelper.attachAll(findViewById(android.R.id.content));

        // Language toggles
        buildLanguageButtons();

        // Apply initial account type UI
        applyAccountType(accountType);

        // Account type tabs
        tabPersonal.setOnClickListener(v -> applyAccountType("personal"));
        tabFamily.setOnClickListener(v -> applyAccountType("family"));

        // Email validation watcher
        editEmail.addTextChangedListener(new FormValidationTextWatcher(emailFeedback));

        // Password rules watcher
        editPassword.addTextChangedListener(new PasswordRulesWatcher());

        // Submit availability watchers
        watchField(editName);
        watchField(editEmail);
        watchField(editConfirm);

        submitBtn.setEnabled(false);
        refreshPasswordRules("");
        refreshSubmit();

        // Submit
        submitBtn.setOnClickListener(v -> register());

        // Go to login
        toLoginBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void applyAccountType(String type) {
        accountType = type;
        boolean isFamily = "family".equals(type);

        // Update tab appearance
        tabPersonal.setBackgroundResource(isFamily ? 0 : R.drawable.bg_tab_selected);
        tabPersonal.setTextColor(getColor(isFamily ? R.color.muted_green : R.color.forest));
        tabPersonal.setTypeface(null, isFamily ? android.graphics.Typeface.NORMAL : android.graphics.Typeface.BOLD);

        tabFamily.setBackgroundResource(isFamily ? R.drawable.bg_tab_selected : 0);
        tabFamily.setTextColor(getColor(isFamily ? R.color.forest : R.color.muted_green));
        tabFamily.setTypeface(null, isFamily ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        // Show/hide family info banner
        familyInfoBanner.setVisibility(isFamily ? View.VISIBLE : View.GONE);
    }

    private void register() {
        String fullName = editName.getText() == null ? "" : editName.getText().toString().trim();
        String email = InputValidation.normalizeGmail(editEmail.getText() == null ? "" : editEmail.getText().toString());
        String password = editPassword.getText() == null ? "" : editPassword.getText().toString();
        String confirm = editConfirm.getText() == null ? "" : editConfirm.getText().toString();

        if (fullName.isEmpty()) { errorText.setText(R.string.full_name); return; }
        if (!InputValidation.isStrictGmail(email)) { errorText.setText(R.string.email_error); return; }
        if (!InputValidation.isValidPassword(password)) { errorText.setText(InputValidation.passwordError(this, password)); return; }
        if (!password.equals(confirm)) { errorText.setText(R.string.passwords_differ); return; }

        // Update email field if normalized
        if (!editEmail.getText().toString().equals(email)) editEmail.setText(email);

        submitBtn.setEnabled(false);
        errorText.setText("");

        String role = "family".equals(accountType) ? AppRole.PARENT.value() : AppRole.USER.value();
        viewModel.<OperationResult<Long>>request(
                repository -> repository.register(fullName, email, password, role, accountType),
                state -> {
                    submitBtn.setEnabled(true);
                    OperationResult<Long> outcome = state.data;
                    if (outcome == null || !outcome.succeeded() || outcome.value == null) {
                        String failure = outcome == null ? "save" : outcome.error;
                        if ("duplicate".equals(failure)) errorText.setText(R.string.account_exists);
                        else if ("password".equals(failure)) errorText.setText(InputValidation.passwordError(this, password));
                        else errorText.setText(R.string.email_error);
                        return;
                    }
                    Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class).putExtra("EMAIL", email));
                    finish();
                }
        );
    }

    private void refreshSubmit() {
        String name = editName.getText() == null ? "" : editName.getText().toString().trim();
        String email = editEmail.getText() == null ? "" : editEmail.getText().toString();
        String pwd = editPassword.getText() == null ? "" : editPassword.getText().toString();
        String confirm = editConfirm.getText() == null ? "" : editConfirm.getText().toString();
        boolean ready = !name.isEmpty()
                && InputValidation.isStrictGmail(email)
                && InputValidation.isValidPassword(pwd)
                && pwd.equals(confirm);
        submitBtn.setEnabled(ready);
    }

    private void refreshPasswordRules(String value) {
        boolean[] valid = InputValidation.passwordRules(value);
        TextView[] rows = {ruleLength, ruleUppercase, ruleLowercase, ruleNumber, ruleSymbol};
        if (rows[0] == null) return;
        for (int i = 0; i < rows.length; i++) {
            if (rows[i] == null) continue;
            rows[i].setText((valid[i] ? "✓  " : "○  ") + getString(ruleTextIds[i]));
            rows[i].setTextColor(getColor(valid[i] ? R.color.emerald : R.color.muted_green));
        }
        // Show password rules panel when typing
        boolean hasInput = !value.isEmpty();
        if (passwordRulesPanel != null) {
            passwordRulesPanel.setVisibility(hasInput ? View.VISIBLE : View.GONE);
        }
    }

    private void watchField(TextInputEditText field) {
        if (field == null) return;
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { refreshSubmit(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void buildLanguageButtons() {
        // Determine active language
        String currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (currentTag.isEmpty()) currentTag = java.util.Locale.getDefault().getLanguage();
        String activeLang = currentTag.length() >= 2 ? currentTag.substring(0, 2).toLowerCase(java.util.Locale.ROOT) : "fr";

        // Find lang buttons by ID, attach click listeners, and style active one
        int[] ids = {R.id.lang_fr, R.id.lang_ar, R.id.lang_en};
        String[] tags = {"fr", "ar", "en"};
        for (int i = 0; i < ids.length; i++) {
            TextView tv = findViewById(ids[i]);
            if (tv == null) continue;
            final String tag = tags[i];
            boolean selected = tag.equals(activeLang);
            tv.setBackground(selected ? getDrawable(R.drawable.bg_lang_selected) : null);
            tv.setTextColor(getColor(selected ? R.color.alabaster : R.color.muted_green));
            tv.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            tv.setOnClickListener(v -> LocaleController.apply(tag));
        }
    }

    private final class PasswordRulesWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
        @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
            refreshPasswordRules(s == null ? "" : s.toString());
            refreshSubmit();
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}
