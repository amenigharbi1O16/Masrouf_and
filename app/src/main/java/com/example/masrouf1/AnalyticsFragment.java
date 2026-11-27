/*
 * ============================================================================
 * FICHIER : AnalyticsFragment.java
 * MISSION : Onglet central « Analyses » (accessible depuis le tiroir latéral
 *           pour USER/CHILD, ou depuis l'hôte central). Charge de manière
 *           asynchrone AnalyticsSummaryData depuis Room et délègue tout le
 *           rendu visuel programmatique à AnalyticsRenderer.
 * IMPLÉMENTE : Sous-classe de DashboardSectionFragment gérant un indicateur
 *              de chargement (ProgressBar), un message d'erreur et le
 *              conteneur vertical rempli dynamiquement selon le rôle.
 * EFFET À L'ÉCRAN : Tableau analytique complet : carte héroïque budget,
 *                   conclusions et recommandations, comparaison mensuelle,
 *                   jauge, graphique 6 mois PillBarChart, ventilation par
 *                   catégorie, rythme quotidien et détail des transactions.
 * COMMUNICATIONS :
 *   ← DashboardSectionFragment (en-tête avec menu ☰, cloche, profil)
 *   → MasroufViewModel / MasroufRepository.loadAnalytics (calculs SQLite)
 *   → AnalyticsRenderer.render (construction des cartes et graphiques Java)
 *   → MainActivity.openCentralTab("calendar") (sélecteur de période)
 *   → BudgetPeriodActivity (création/modification du budget)
 *   → TransactionDetailsActivity (ouverture d'une transaction récente)
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;
import com.masrouf.controller.BudgetPeriodActivity;
import com.masrouf.controller.SessionStore;
import com.masrouf.controller.TransactionDetailsActivity;

/**
 * Onglet central « Analyses » (adaptatif selon USER / CHILD / PARENT) héritant de DashboardSectionFragment.
 * Charge AnalyticsSummaryData hors UI thread et délègue la construction visuelle complète à AnalyticsRenderer.
 */
public final class AnalyticsFragment extends DashboardSectionFragment {
    /** Conteneur vertical recevant toutes les cartes analytiques générées par AnalyticsRenderer. */
    private LinearLayout container;
    /** Indicateur affiché pendant l'agrégation asynchrone des données Room. */
    private ProgressBar loadingView;
    /** Message d'erreur discret affiché en cas d'échec de chargement SQLite. */
    private TextView errorView;

    /** Adapte le titre d'en-tête entre « Analyses budgétaires » (Parent) et « Analyses » (User/Child). */
    @Override
    protected int pageTitle() {
        return SessionStore.role(requireContext()) == AppRole.PARENT
                ? R.string.nav_budget_analytics
                : R.string.nav_analytics;
    }

    /** Construit par programmation le ProgressBar, le TextView d'erreur et le conteneur de cartes. */
    @Override
    protected void createContent(LinearLayout content) {
        loadingView = new ProgressBar(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(28), dp(28));
        lp.topMargin = dp(12);
        lp.bottomMargin = dp(12);
        content.addView(loadingView, lp);

        errorView = text(getString(R.string.dashboard_read_error));
        errorView.setTextColor(requireContext().getColor(R.color.coral));
        errorView.setVisibility(View.GONE);
        content.addView(errorView);

        container = new LinearLayout(requireContext());
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
        cp.topMargin = dp(10);
        content.addView(container, cp);
    }

    /** Déclenche le premier chargement analytique dès la création de la vue. */
    @Override
    protected void observeContent() {
        loadAnalytics();
    }

    /** Recharge les indicateurs au retour sur l'Activity (après ajout d'une dépense ou édition du budget). */
    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && container != null) loadAnalytics();
    }

    /** Recharge les analyses lorsque l'onglet redevient visible après un show/hide dans MainActivity. */
    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded() && container != null) loadAnalytics();
    }

    /** Exécute loadAnalytics() via ViewModel puis transmet le snapshot et les callbacks à AnalyticsRenderer. */
    private void loadAnalytics() {
        long ownerId = signedInUserId();
        if (ownerId <= 0L) return;
        long preferredBudgetId = SessionStore.selectedBudgetId(requireContext(), ownerId);
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        if (errorView != null) errorView.setVisibility(View.GONE);

        viewModel.<AnalyticsSummaryData>request(
                repository -> repository.loadAnalytics(ownerId, preferredBudgetId),
                state -> {
                    if (!isAdded() || getView() == null) return;
                    if (loadingView != null) loadingView.setVisibility(View.GONE);
                    if (state.failed || state.data == null) {
                        if (errorView != null) errorView.setVisibility(View.VISIBLE);
                        return;
                    }
                    // Construction des vues analytiques avec les trois actions interactives associées.
                    AnalyticsRenderer.render(requireContext(), container, state.data, new AnalyticsRenderer.Actions() {
                        @Override
                        public void onOpenCalendar() {
                            if (requireActivity() instanceof MainActivity) {
                                ((MainActivity) requireActivity()).openCentralTab("calendar");
                            }
                        }

                        @Override
                        public void onCreateOrEditBudget(BudgetEntity currentBudget) {
                            Intent intent = new Intent(requireContext(), BudgetPeriodActivity.class);
                            if (currentBudget != null && currentBudget.id > 0L) {
                                intent.putExtra("MODE", "edit").putExtra("BUDGET_ID", currentBudget.id);
                            }
                            startActivity(intent);
                        }

                        @Override
                        public void onOpenTransaction(TransactionRow row) {
                            if (row == null) return;
                            startActivity(new Intent(requireContext(), TransactionDetailsActivity.class)
                                    .putExtra("TRANSACTION_ID", row.id));
                        }
                    });
                });
    }
}
