/*
 * ============================================================================
 * FICHIER : ChooseAccountTypeActivity.java
 * MISSION : Écran intermédiaire d'inscription permettant à l'utilisateur de
 *           choisir entre un compte Personnel (rôle USER) et un compte Famille
 *           (rôle PARENT), avec sélecteur de langue FR / AR / EN et lien
 *           vers la connexion.
 * IMPLÉMENTE : AppCompatActivity gonflant activity_choose_account_type.xml,
 *              pilules de langue créées en Java (buildLanguageButtons) et
 *              Intent explicite avec l'extra "TYPE" vers RegisterActivity.
 * EFFET À L'ÉCRAN : Deux cartes cliquables (« Personnel » et « Famille »)
 *                   détaillant les fonctionnalités, flèche retour et boutons FR/AR/EN.
 * COMMUNICATIONS :
 *   ← WelcomeActivity (bouton « Créer un compte »)
 *   → RegisterActivity (extra "TYPE" = "personal" ou "family")
 *   → LoginActivity (lien « Se connecter »)
 *   → LocaleController.apply (changement immédiat de langue)
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.LocaleController;
import com.example.masrouf1.R;

/**
 * Écran de choix du type de compte (Personnel -> rôle USER, Famille -> rôle PARENT)
 * transmettant l'extra "TYPE" vers RegisterActivity via un Intent explicite.
 */
/**
 * ChooseAccountTypeActivity — Account type selection screen.
 * Shows Personal vs Family account cards with feature lists.
 * Navigates to RegisterActivity with TYPE extra.
 */
public final class ChooseAccountTypeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_account_type);

        // Back button
        ImageView backBtn = findViewById(R.id.choose_back);
        backBtn.setOnClickListener(v -> finish());

        // Language buttons
        buildLanguageButtons();

        // Personal card
        findViewById(R.id.personal_card).setOnClickListener(v -> openRegister("personal"));

        // Family card
        findViewById(R.id.family_card).setOnClickListener(v -> openRegister("family"));

        // Login link
        TextView openLogin = findViewById(R.id.choose_open_login);
        openLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
        });
    }

    /** Ouvre RegisterActivity en lui passant le type sélectionné ("personal" ou "family") via l'extra "TYPE". */
    private void openRegister(String type) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("TYPE", type);
        startActivity(intent);
    }

    /** Construit dynamiquement en Java les pilules de sélection de langue FR / AR / EN et relance la locale au clic. */
    private void buildLanguageButtons() {
        LinearLayout row = findViewById(R.id.choose_languages);
        if (row == null) return;
        row.removeAllViews();
        String currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (currentTag.isEmpty()) currentTag = java.util.Locale.getDefault().getLanguage();
        String activeLang = currentTag.length() >= 2 ? currentTag.substring(0, 2).toLowerCase(java.util.Locale.ROOT) : "fr";
        String[] tags = {"fr", "ar", "en"};
        String[] labels = {"FR", "AR", "EN"};
        for (int i = 0; i < tags.length; i++) {
            final String tag = tags[i];
            final String label = labels[i];
            boolean selected = tag.equals(activeLang);
            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(13f);
            tv.setPadding(dp(12), dp(6), dp(12), dp(6));
            tv.setTextColor(getColor(selected ? R.color.alabaster : R.color.muted_green));
            tv.setBackground(getDrawable(selected ? R.drawable.bg_lang_selected : R.drawable.bg_lang_unselected));
            tv.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(dp(4));
            tv.setOnClickListener(v -> {
                LocaleController.apply(tag);
                // setApplicationLocales() automatically recreates — no manual recreate() needed
            });
            row.addView(tv, params);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
