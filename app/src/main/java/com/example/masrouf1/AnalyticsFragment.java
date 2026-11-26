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
 * Role-aware Analytics central tab backed strictly by persisted Room data across all three user roles:
 * Normal User, Child (own ledger only), and Parent (household, categories, 6m trend & privacy-safe child stats).
 */
public final class AnalyticsFragment extends DashboardSectionFragment {
    private LinearLayout container;
    private ProgressBar loadingView;
    private TextView errorView;

    @Override
    protected int pageTitle() {
        return SessionStore.role(requireContext()) == AppRole.PARENT
                ? R.string.nav_budget_analytics
                : R.string.nav_analytics;
    }

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

    @Override
    protected void observeContent() {
        loadAnalytics();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && container != null) loadAnalytics();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && isAdded() && container != null) loadAnalytics();
    }

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
