package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.LocaleController;
import com.example.masrouf1.MainActivity;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.databinding.ActivityLoginBinding;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

public final class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private MasroufViewModel viewModel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this)).get(MasroufViewModel.class);

        // Wire back button
        ImageView backBtn = findViewById(R.id.login_back);
        if (backBtn != null) backBtn.setOnClickListener(v -> finish());

        // Wire language switcher (3 TextViews: lang_fr, lang_ar, lang_en)
        wireLangTab(R.id.lang_fr, "fr");
        wireLangTab(R.id.lang_ar, "ar");
        wireLangTab(R.id.lang_en, "en");
        highlightActiveLang();

        if (getIntent().hasExtra("EMAIL")) binding.loginEmail.setText(getIntent().getStringExtra("EMAIL"));
        binding.loginEmail.addTextChangedListener(new FormValidationTextWatcher(binding.loginEmailFeedback));
        binding.loginSubmit.setOnClickListener(new LoginClickListener(this));
        binding.loginToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class).putExtra("TYPE", "personal")));
    }

    private void wireLangTab(int viewId, String tag) {
        TextView tv = findViewById(viewId);
        if (tv == null) return;
        tv.setOnClickListener(v -> {
            LocaleController.apply(tag);
        });
    }

    private void highlightActiveLang() {
        String currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (currentTag.isEmpty()) currentTag = java.util.Locale.getDefault().getLanguage();
        String activeLang = currentTag.length() >= 2 ? currentTag.substring(0, 2).toLowerCase(java.util.Locale.ROOT) : "fr";

        int[][] langPairs = new int[0][];
        // Map view id -> lang tag
        int[] ids = {R.id.lang_fr, R.id.lang_ar, R.id.lang_en};
        String[] tags = {"fr", "ar", "en"};
        for (int i = 0; i < ids.length; i++) {
            TextView tv = findViewById(ids[i]);
            if (tv == null) continue;
            boolean selected = tags[i].equals(activeLang);
            tv.setBackground(selected ? getDrawable(R.drawable.bg_lang_selected) : null);
            tv.setTextColor(getColor(selected ? R.color.alabaster : R.color.muted_green));
            if (selected) tv.setTypeface(null, android.graphics.Typeface.BOLD);
            else tv.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    public void performLogin() {
        String address = InputValidation.normalizeGmail(binding.loginEmail.getText().toString());
        String secret = binding.loginPassword.getText().toString();
        if (!InputValidation.isStrictGmail(address) || secret.isEmpty()) { showError(); return; }
        if (!binding.loginEmail.getText().toString().equals(address)) binding.loginEmail.setText(address);
        binding.loginSubmit.setEnabled(false);
        DevelopmentSeedManager.ensureSeeded(this, () -> viewModel.<UserEntity>request(
                repository -> repository.authenticate(address, secret),
                state -> {
                    binding.loginSubmit.setEnabled(true);
                    UserEntity user = state.data;
                    if (user == null) { showError(); return; }
                    binding.loginErrorCard.setVisibility(View.GONE);
                    SessionStore.save(this, user);
                    boolean completedOnboarding = user.categoriesSelected && user.onboarded;
                    Intent target = new Intent(this, completedOnboarding ? MainActivity.class : OnboardingActivity.class);
                    target.putExtra("USER_ID", user.id);
                    if (completedOnboarding) {
                        target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    }
                    startActivity(target);
                    finish();
                }));
    }

    private void showError() { binding.loginErrorCard.setVisibility(View.VISIBLE); }
}
