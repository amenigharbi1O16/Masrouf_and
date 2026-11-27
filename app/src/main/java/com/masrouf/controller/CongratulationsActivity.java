/*
 * ============================================================================
 * FICHIER : CongratulationsActivity.java
 * MISSION : Écran de célébration affiché à l'utilisateur lorsqu'une période budgétaire est clôturée avec succès.
 * IMPLÉMENTE : AppCompatActivity associée au layout activity_congratulations.xml.
 * EFFET À L'ÉCRAN : Affiche message personnalisé, carte des points gagnés (si > 0), carte des économies (si > 0),
 *                   et boutons « Retour à l'accueil » et « Voir mes points » (RewardsActivity).
 * COMMUNICATIONS : Ouverte par NotificationsActivity au clic sur une notification "budget_closed" ;
 *                  lit les Extras "POINTS_EARNED", "SAVINGS_MILLI" et "MESSAGE".
 * ============================================================================
 */
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
 * ACTIVITÉ : CongratulationsActivity — Écran de félicitations post-clôture budgétaire.
 */
public final class CongratulationsActivity extends AppCompatActivity {

    /**
     * Gonfle activity_congratulations.xml, lit les extras facultatifs et câble la navigation vers Accueil ou Récompenses.
     */
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

    /** Retour à l'écran d'accueil en vidant les activités intermédiaires de la pile de navigation. */
    private void goHome() {
        Intent intent = new Intent(this, com.example.masrouf1.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
