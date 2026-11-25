package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.R;
import com.example.masrouf1.RewardsActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * ACTIVITÉ : CongratulationsActivity
 * RÔLE : Écran de félicitations affiché après qu'un utilisateur a terminé
 *         une période budgétaire avec succès ou a atteint un objectif.
 *
 * PARAMÈTRES ATTENDUS EN EXTRA :
 *   - "POINTS_EARNED" (int) : points gagnés lors de cette période
 *   - "SAVINGS_MILLI" (long) : montant économisé en millimes (optionnel)
 *   - "MESSAGE" (String) : message personnalisé (optionnel)
 *
 * CORRESPONDANCE REACT NATIVE : src/app/felicitations.tsx
 */
public final class CongratulationsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_congratulations);

        // Récupération des paramètres passés par l'appelant
        int pointsEarned = getIntent().getIntExtra("POINTS_EARNED", 0);
        long savingsMilli = getIntent().getLongExtra("SAVINGS_MILLI", 0L);
        String customMessage = getIntent().getStringExtra("MESSAGE");

        // Message personnalisé si fourni
        if (customMessage != null && !customMessage.isEmpty()) {
            TextView messageView = findViewById(R.id.congrats_message);
            if (messageView != null) messageView.setText(customMessage);
        }

        // Carte des points gagnés (si > 0)
        MaterialCardView pointsCard = findViewById(R.id.congrats_points_card);
        TextView pointsValue = findViewById(R.id.congrats_points_value);
        if (pointsCard != null && pointsEarned > 0) {
            pointsCard.setVisibility(View.VISIBLE);
            if (pointsValue != null) pointsValue.setText(String.valueOf(pointsEarned));
        }

        // Carte des économies (si > 0)
        MaterialCardView savingsCard = findViewById(R.id.congrats_savings_card);
        TextView savingsValue = findViewById(R.id.congrats_savings_value);
        if (savingsCard != null && savingsMilli > 0) {
            savingsCard.setVisibility(View.VISIBLE);
            if (savingsValue != null) savingsValue.setText(CurrencyFormatter.formatMilli(savingsMilli));
        }

        // Bouton Retour à l'accueil
        MaterialButton homeButton = findViewById(R.id.congrats_home_button);
        if (homeButton != null) {
            homeButton.setOnClickListener(v -> goHome());
        }

        // Bouton Voir mes points
        MaterialButton pointsButton = findViewById(R.id.congrats_points_button);
        if (pointsButton != null) {
            pointsButton.setOnClickListener(v -> {
                startActivity(new Intent(this, RewardsActivity.class)
                        .putExtra("USER_ID", SessionStore.userId(this)));
                finish();
            });
        }
    }

    /** Retour à l'écran d'accueil en vidant la pile de navigation. */
    private void goHome() {
        Intent intent = new Intent(this, com.example.masrouf1.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
