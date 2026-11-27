/*
 * ============================================================================
 * FICHIER : MoneyRequestActivity.java
 * MISSION : Écran réservé exclusivement au profil Enfant (AppRole.CHILD) pour
 *           envoyer une demande d'argent à son parent associé (montant + motif
 *           avec dictée vocale inline) et suivre l'historique des demandes
 *           (en attente, acceptées ou refusées).
 * IMPLÉMENTE : Sous-classe de BaseFeatureActivity avec contrôle d'accès strict
 *              sur AppRole.CHILD, validation MoneyInput, VoiceFieldHelper sur
 *              les deux champs et RecyclerView alimenté par MoneyRequestAdapter.
 * EFFET À L'ÉCRAN : Formulaire beige avec boutons micro, message de confirmation
 *                   ou d'erreur et liste chronologique des demandes envoyées.
 * COMMUNICATIONS :
 *   ← HomeFragment (bouton « Demander de l'argent » visible uniquement en mode Child)
 *   → MasroufViewModel / MasroufRepository (requestMoney, moneyRequestsForChild)
 *   → VoiceFieldHelper (dictée vocale montant et raison)
 *   → MoneyRequestAdapter (affichage des cartes de demandes)
 * ============================================================================
 */
package com.masrouf.controller;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.ui.AlertBannerView;
import com.google.android.material.button.MaterialButton;

/**
 * Écran réservé exclusivement au profil Enfant (AppRole.CHILD) : garde d'accès au onCreate().
 * Mission : saisir et envoyer une demande d'argent vers son Parent (MasroufRepository.requestMoney)
 * avec saisie vocale assistée (VoiceFieldHelper), et afficher l'historique via MoneyRequestAdapter.
 * Effet à l'écran : affiche confirmation émeraude/corail et actualise la liste des statuts (pending/accepted/rejected).
 */
public final class MoneyRequestActivity extends BaseFeatureActivity {
    private MasroufViewModel viewModel;
    private VoiceFieldHelper voiceFieldHelper;
    private EditText amount, reason;
    private TextView feedback, empty;
    private AlertBannerView voiceBanner;
    private MaterialButton submit;
    private MoneyRequestAdapter adapter;
    private long childId;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (SessionStore.role(this) != AppRole.CHILD || SessionStore.userId(this) <= 0) {
            finish();
            return;
        }
        childId = SessionStore.userId(this);
        showPage(R.string.money_request_page_title);
        View form = getLayoutInflater().inflate(R.layout.activity_money_request, content, false);
        content.addView(form);

        amount = form.findViewById(R.id.request_amount);
        reason = form.findViewById(R.id.request_reason);
        feedback = form.findViewById(R.id.request_feedback);
        empty = form.findViewById(R.id.request_empty);
        voiceBanner = form.findViewById(R.id.request_voice_banner);
        submit = form.findViewById(R.id.request_submit);

        voiceFieldHelper = new VoiceFieldHelper(this, this, voiceBanner);
        voiceFieldHelper.bindField(form.findViewById(R.id.request_amount_mic), amount);
        voiceFieldHelper.bindField(form.findViewById(R.id.request_reason_mic), reason);

        RecyclerView history = form.findViewById(R.id.request_history);
        history.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MoneyRequestAdapter();
        history.setAdapter(adapter);
        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        submit.setOnClickListener(v -> sendRequest());
        refreshHistory();
    }

    @Override
    protected void onDestroy() {
        if (voiceFieldHelper != null) voiceFieldHelper.destroy();
        super.onDestroy();
    }

    private void sendRequest() {
        final long amountMilli;
        try {
            amountMilli = MoneyInput.toMilli(amount.getText() == null ? "" : amount.getText().toString());
            if (amountMilli <= 0) throw new NumberFormatException();
        } catch (NumberFormatException invalid) {
            amount.setError(getString(R.string.money_invalid));
            return;
        }
        String why = reason.getText() == null ? "" : reason.getText().toString().trim();
        if (why.isEmpty()) {
            reason.setError(getString(R.string.money_request_reason_hint));
            return;
        }
        submit.setEnabled(false);
        feedback.setVisibility(View.GONE);
        viewModel.<Long>request(repository -> repository.requestMoney(childId, amountMilli, why), state -> {
            submit.setEnabled(true);
            feedback.setVisibility(View.VISIBLE);
            if (state.data != null && state.data > 0) {
                feedback.setText(R.string.money_request_sent);
                feedback.setTextColor(getColor(R.color.emerald));
                amount.setText(null);
                reason.setText(null);
                refreshHistory();
            } else {
                feedback.setText(R.string.money_request_failed);
                feedback.setTextColor(getColor(R.color.coral));
            }
        });
    }

    private void refreshHistory() {
        viewModel.<java.util.List<com.example.masrouf1.db.MasroufEntities.MoneyRequestEntity>>request(
                repository -> repository.moneyRequestsForChild(childId),
                state -> {
                    if (state.loading) return;
                    java.util.List<com.example.masrouf1.db.MasroufEntities.MoneyRequestEntity> requests = state.data;
                    adapter.submit(requests);
                    empty.setVisibility(state.failed || requests == null || requests.isEmpty() ? View.VISIBLE : View.GONE);
                    if (state.failed) empty.setText(R.string.dashboard_read_error);
                });
    }
}
