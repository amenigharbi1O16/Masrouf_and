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

    private void openRegister(String type) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("TYPE", type);
        startActivity(intent);
    }

    private void buildLanguageButtons() {
        LinearLayout row = findViewById(R.id.choose_languages);
        if (row == null) return;
        row.removeAllViews();
        String[] tags = {"en", "fr", "ar"};
        String[] labels = {"EN", "FR", "عربي"};
        for (int i = 0; i < tags.length; i++) {
            final String tag = tags[i];
            final String label = labels[i];
            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(13f);
            tv.setPadding(dp(10), dp(6), dp(10), dp(6));
            tv.setTextColor(getColor(R.color.muted_green));
            tv.setOnClickListener(v -> {
                LocaleController.apply(tag);
                // setApplicationLocales() automatically recreates — no manual recreate() needed
            });
            row.addView(tv);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
