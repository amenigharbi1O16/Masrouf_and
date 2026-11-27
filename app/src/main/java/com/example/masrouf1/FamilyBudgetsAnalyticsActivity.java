/*
 * ============================================================================
 * FICHIER : FamilyBudgetsAnalyticsActivity.java
 * MISSION : Écran d'analyses budgétaires familiales strictement réservé au profil PARENT.
 * IMPLÉMENTE : AppCompatActivity (layout activity_family_budgets_analytics.xml) avec garde de rôle au onCreate().
 * EFFET À L'ÉCRAN : Affiche un spinner de chargement puis injecte dans family_analytics_container le rendu Java
 *                   complet généré par AnalyticsRenderer (jauge foyer, enveloppes, 6 mois et barres enfants).
 * COMMUNICATIONS : Ouvert via AdminDrawer ; interroge MasroufRepository.loadAnalytics() via MasroufViewModel
 *                  et redirige au clic vers MainActivity ("calendar") ou BudgetPeriodActivity.
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;
import com.masrouf.controller.BudgetPeriodActivity;
import com.masrouf.controller.SessionStore;

/**
 * Écran d'analyses budgétaires familiales réservé exclusivement au profil Parent.
 */
public final class FamilyBudgetsAnalyticsActivity extends AppCompatActivity {
    private MasroufViewModel viewModel;
    private LinearLayout container;
    private ProgressBar loadingView;
    private TextView errorView;

    /** Vérifie le rôle PARENT, configure la barre d'outils MaterialToolbar et initialise MasroufViewModel. */
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        // Bloque tout utilisateur non-PARENT tentant d'ouvrir directement cet écran familial.
        if (SessionStore.role(this) != AppRole.PARENT) {
            finish();
            return;
        }
        setContentView(R.layout.activity_family_budgets_analytics);
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.family_analytics_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadingView = findViewById(R.id.family_analytics_loading);
        errorView = findViewById(R.id.family_analytics_error);
        container = findViewById(R.id.family_analytics_container);
        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this))
                .get(MasroufViewModel.class);
    }

    /** Recharge systématiquement les agrégats familiaux à chaque retour sur l'écran. */
    @Override
    protected void onResume() {
        super.onResume();
        loadAggregates();
    }

    /** Lit AnalyticsSummaryData sur le thread IO puis confie toute la construction visuelle à AnalyticsRenderer. */
    private void loadAggregates() {
        long parentId = SessionStore.userId(this);
        long preferredId = SessionStore.selectedBudgetId(this, parentId);
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        if (errorView != null) errorView.setVisibility(View.GONE);

        viewModel.<AnalyticsSummaryData>request(
                repository -> repository.loadAnalytics(parentId, preferredId),
                state -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (loadingView != null) loadingView.setVisibility(View.GONE);
                    if (state.failed || state.data == null) {
                        if (errorView != null) errorView.setVisibility(View.VISIBLE);
                        return;
                    }
                    // Délègue le rendu complet et les actions de navigation vers Calendrier ou BudgetPeriodActivity.
                    AnalyticsRenderer.render(this, container, state.data, new AnalyticsRenderer.Actions() {
                        @Override
                        public void onOpenCalendar() {
                            startActivity(new Intent(FamilyBudgetsAnalyticsActivity.this, MainActivity.class)
                                    .putExtra(MainActivity.EXTRA_CENTRAL_TAB, "calendar")
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                        }

                        @Override
                        public void onCreateOrEditBudget(BudgetEntity currentBudget) {
                            Intent intent = new Intent(FamilyBudgetsAnalyticsActivity.this, BudgetPeriodActivity.class);
                            if (currentBudget != null && currentBudget.id > 0L) {
                                intent.putExtra("MODE", "edit").putExtra("BUDGET_ID", currentBudget.id);
                            }
                            startActivity(intent);
                        }

                        @Override
                        public void onOpenTransaction(TransactionRow row) { }
                    });
                });
    }
}
