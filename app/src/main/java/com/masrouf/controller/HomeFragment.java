package com.masrouf.controller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.BudgetPeriodMath;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.HomeCategoryProgress;
import com.example.masrouf1.HomeDashboardData;
import com.example.masrouf1.MainActivity;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.R;
import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.SettingEntity;
import com.example.masrouf1.db.TransactionRow;
import com.example.masrouf1.ui.AlertBannerView;
import com.example.masrouf1.ui.BudgetCardView;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Role-aware home: child history remains private; parents receive SQL aggregates only. */
public final class HomeFragment extends Fragment {
    private long userId, childAllocation;
    private MasroufViewModel dashboardViewModel;
    private View root;
    private HomeCategoryAdapter categoriesAdapter;
    private RecentTransactionsAdapter transactionsAdapter;
    private BudgetEntity currentBudget;
    private boolean childMode, parentMode;

    private final ActivityResultLauncher<Intent> manageCategoriesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String msg = result.getData().getStringExtra("EXTRA_MESSAGE");
                    showFeedbackBanner(
                            getString(R.string.manage_categories_title),
                            msg == null || msg.isEmpty()
                                    ? getString(R.string.categories_updated_success)
                                    : msg);
                }
                if (root != null) {
                    loadData();
                }
            });

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedState) {
        super.onViewCreated(view, savedState);
        root = view;
        userId = SessionStore.userId(requireContext());
        dashboardViewModel = new androidx.lifecycle.ViewModelProvider(
                this, new MasroufViewModelFactory(requireContext())).get(MasroufViewModel.class);
        AppRole role = SessionStore.role(requireContext());
        childMode = role == AppRole.CHILD;
        parentMode = role == AppRole.PARENT;

        RecyclerView categories = view.findViewById(R.id.home_categories);
        categories.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoriesAdapter = new HomeCategoryAdapter(this::onCategoryClicked);
        categories.setAdapter(categoriesAdapter);

        RecyclerView recent = view.findViewById(R.id.home_recent_transactions);
        recent.setLayoutManager(new LinearLayoutManager(requireContext()));
        transactionsAdapter = new RecentTransactionsAdapter(this::openTransaction);
        recent.setAdapter(transactionsAdapter);
        recent.setNestedScrollingEnabled(false);
        categories.setNestedScrollingEnabled(false);

        ((TextView) view.findViewById(R.id.home_period)).setText(periodLabel(null));
        view.findViewById(R.id.home_period).setContentDescription(periodLabel(null));
        view.findViewById(R.id.home_calendar_link).setOnClickListener(
                v -> ((MainActivity) requireActivity()).openCentralTab("calendar"));
        view.findViewById(R.id.home_open_drawer).setOnClickListener(
                v -> ((MainActivity) requireActivity()).openMenu());
        view.findViewById(R.id.home_budget_card).setOnClickListener(v -> openBudget());
        view.findViewById(R.id.home_define_budget).setOnClickListener(v -> openBudget());
        view.findViewById(R.id.home_partition_child_budget).setOnClickListener(v -> openChildAllocation());
        view.findViewById(R.id.home_add_category).setOnClickListener(
                v -> new CustomCategoryDialogFragment().show(getParentFragmentManager(), "add-category"));
        view.findViewById(R.id.home_add_purchase).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), AddProductActivity.class)));
        view.findViewById(R.id.home_add_expense).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), AddExpenseActivity.class)));

        View.OnClickListener openManageCategories = v ->
                manageCategoriesLauncher.launch(new Intent(requireContext(), ManageCategoriesActivity.class));
        view.findViewById(R.id.home_manage_categories).setOnClickListener(openManageCategories);
        view.findViewById(R.id.home_choose_categories_btn).setOnClickListener(openManageCategories);
        view.findViewById(R.id.home_allocate_repartition).setOnClickListener(v -> openRepartition());

        view.findViewById(R.id.home_manage_children).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), ChildrenManagementActivity.class)));
        getParentFragmentManager().setFragmentResultListener("masrouf-categories-updated", this, (key, result) -> {
            showFeedbackBanner(getString(R.string.manage_categories_title), getString(R.string.categories_updated_success));
            loadData();
        });
        view.findViewById(R.id.home_children_tracking).setVisibility(parentMode ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.home_manage_children).setVisibility(parentMode ? View.VISIBLE : View.GONE);
        view.findViewById(R.id.home_request_money).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), MoneyRequestActivity.class)));

        dashboardViewModel.home().observe(getViewLifecycleOwner(), state -> {
            if (root == null) return;
            root.findViewById(R.id.home_loading_state).setVisibility(state.loading ? View.VISIBLE : View.GONE);
            root.findViewById(R.id.home_error_state).setVisibility(state.failed ? View.VISIBLE : View.GONE);
            if (state.loading || state.failed || state.data == null) return;

            HomeDashboardData data = state.data;
            String name = data.user == null || data.user.name == null ? "" : data.user.name;
            ((TextView) root.findViewById(R.id.home_greeting)).setText(
                    name.isEmpty() ? getString(R.string.greeting_hello) : getString(R.string.greeting_hello) + ", " + name);

            currentBudget = data.budget;
            childAllocation = data.childProfile == null ? 0L : data.childProfile.allocatedTotalMilli;
            if (currentBudget != null) {
                SessionStore.setSelectedBudgetId(requireContext(), userId, currentBudget.id);
            }

            ((TextView) root.findViewById(R.id.home_period)).setText(periodLabel(data.budget));
            root.findViewById(R.id.home_period).setContentDescription(periodLabel(data.budget));
            renderBudget(data.budget, data.childAllocations, data.parentSpent,
                    data.user == null ? 0L : data.user.balanceMilli, data.settings);

            boolean noCategories = data.categoryProgress == null || data.categoryProgress.isEmpty();
            categoriesAdapter.submitProgress(data.categoryProgress);
            root.findViewById(R.id.home_categories).setVisibility(noCategories ? View.GONE : View.VISIBLE);
            root.findViewById(R.id.home_categories_empty_card).setVisibility(noCategories ? View.VISIBLE : View.GONE);

            boolean showRepartitionBtn = data.budget != null && data.budget.monthlyLimitMilli > 0L && !noCategories
                    && (!childMode || childAllocation > 0L);
            root.findViewById(R.id.home_allocate_repartition).setVisibility(showRepartitionBtn ? View.VISIBLE : View.GONE);

            transactionsAdapter.submit(data.recent);
            root.findViewById(R.id.home_transactions_empty).setVisibility(
                    data.recent == null || data.recent.isEmpty() ? View.VISIBLE : View.GONE);

            TextView summary = root.findViewById(R.id.home_summary);
            if (parentMode) {
                long familySpent = data.parentSpent;
                for (ChildSpendingSummary child : data.children) {
                    familySpent = MoneyMath.add(familySpent, child.spentMilli);
                }
                long householdRemaining = data.budget == null
                        ? 0L : MoneyMath.subtract(data.budget.monthlyLimitMilli, familySpent);
                summary.setText(getString(R.string.dashboard_family_summary,
                        CurrencyFormatter.formatMilli(familySpent),
                        CurrencyFormatter.formatMilli(householdRemaining),
                        data.children.size(),
                        data.transactionCount,
                        data.pendingRequestCount));
                summary.setVisibility(View.VISIBLE);
            } else if (!childMode) {
                summary.setText(getString(R.string.dashboard_personal_summary,
                        CurrencyFormatter.formatMilli(data.budget == null ? 0L : data.budget.spentMilli),
                        data.transactionCount));
                summary.setVisibility(View.VISIBLE);
            } else {
                summary.setVisibility(View.GONE);
            }
            if (childMode) renderChildSummary(data);
            if (parentMode) renderChildren(data.children);
        });

        consumeActivitySuccessFeedback();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (root != null) {
            consumeActivitySuccessFeedback();
            loadData();
        }
    }

    public void showFeedbackBanner(@Nullable String message) {
        showFeedbackBanner(getString(R.string.manage_categories_title), message);
    }

    public void showFeedbackBanner(@Nullable String title, @Nullable String message) {
        if (root == null || !isAdded()) return;
        AlertBannerView banner = root.findViewById(R.id.home_category_feedback);
        if (banner == null) return;
        if (message == null || message.trim().isEmpty()) {
            banner.setVisibility(View.GONE);
            banner.setAlert(AlertBannerView.Tone.NONE, "", "");
            return;
        }
        String resolvedTitle = (title == null || title.trim().isEmpty())
                ? getString(R.string.manage_categories_title)
                : title;
        banner.setVisibility(View.VISIBLE);
        banner.setAlert(AlertBannerView.Tone.SUCCESS, resolvedTitle, message);
        banner.postDelayed(() -> {
            if (isAdded() && banner.isAttachedToWindow()) {
                banner.setVisibility(View.GONE);
                banner.setAlert(AlertBannerView.Tone.NONE, "", "");
            }
        }, 4500);
    }

    private void consumeActivitySuccessFeedback() {
        if (!isAdded() || getActivity() == null || getActivity().getIntent() == null) return;
        Intent intent = getActivity().getIntent();
        if (intent.hasExtra("EXTRA_SUCCESS_MESSAGE")) {
            String msg = intent.getStringExtra("EXTRA_SUCCESS_MESSAGE");
            String title = intent.getStringExtra("EXTRA_SUCCESS_TITLE");
            intent.removeExtra("EXTRA_SUCCESS_MESSAGE");
            intent.removeExtra("EXTRA_SUCCESS_TITLE");
            if (msg != null && !msg.trim().isEmpty()) {
                showFeedbackBanner(title, msg);
            }
        }
    }

    private void loadData() {
        if (isAdded() && dashboardViewModel != null) {
            long selectedId = SessionStore.selectedBudgetId(requireContext(), userId);
            dashboardViewModel.loadHome(userId, parentMode, childMode, selectedId);
        }
    }

    private void renderBudget(BudgetEntity budget, long childrenAllocated, long parentSpent,
                              long childBalance, SettingEntity settings) {
        long spent = parentMode ? parentSpent : (budget == null ? 0L : budget.spentMilli);
        long total;
        if (childMode) {
            long budgetLimit = budget == null ? 0L : Math.max(0L, budget.monthlyLimitMilli);
            long ledgerTotal = MoneyMath.add(Math.max(0L, spent), Math.max(0L, childBalance));
            total = Math.max(budgetLimit, ledgerTotal);
        } else if (parentMode) {
            total = budget == null ? 0L : Math.max(0L, MoneyMath.subtract(budget.monthlyLimitMilli, childrenAllocated));
        } else {
            total = budget == null ? 0L : budget.monthlyLimitMilli;
        }
        if (childMode) spent = Math.max(0L, spent);
        long remaining = MoneyMath.subtract(total, spent);
        int greenUntil = settings == null ? 79 : Math.max(0, settings.greenUntil);
        int warningFrom = settings == null ? 80 : Math.max(greenUntil + 1, settings.orangeFrom);
        int dangerFrom = settings == null ? 100 : Math.max(warningFrom, settings.redFrom);

        BudgetCardView budgetCard = root.findViewById(R.id.home_budget_card);
        String totalLabel = parentMode ? getString(R.string.parent_personal_share) : getString(R.string.limit_label);
        if (budget == null && total <= 0L) {
            budgetCard.setBudgetUnavailable(totalLabel);
        } else {
            budgetCard.setBudgetData(periodLabel(budget), total, spent, totalLabel, warningFrom, dangerFrom);
        }
        budgetCard.setAllocationActionVisible(childMode && total > 0L && budget != null);
        boolean empty = budget == null || total <= 0L;
        root.findViewById(R.id.home_no_budget).setVisibility(empty ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.home_define_budget).setVisibility(View.VISIBLE);
        root.findViewById(R.id.home_no_budget_title).setVisibility(View.VISIBLE);
        root.findViewById(R.id.home_child_no_budget_message).setVisibility(View.GONE);
        root.findViewById(R.id.home_partition_child_budget).setVisibility(childMode && total > 0L && budget != null ? View.VISIBLE : View.GONE);

        AlertBannerView alert = root.findViewById(R.id.home_threshold_card);
        if ((total <= 0L && spent > 0L) || (total > 0L && MoneyMath.atLeastPercent(spent, total, dangerFrom))) {
            long overage = total <= 0L ? spent : Math.max(0L, remaining == Long.MIN_VALUE ? Long.MAX_VALUE : -remaining);
            alert.setAlert(AlertBannerView.Tone.DANGER, getString(R.string.budget_over_100),
                    getString(R.string.budget_status_danger_detail, CurrencyFormatter.formatMilli(overage)));
        } else if (total > 0L && MoneyMath.atLeastPercent(spent, total, warningFrom)) {
            alert.setAlert(AlertBannerView.Tone.WARNING, getString(R.string.budget_alert_80),
                    getString(R.string.budget_status_warning_detail, Math.max(0, MoneyMath.percentOf(spent, total))));
        } else if (total > 0L) {
            alert.setAlert(AlertBannerView.Tone.SUCCESS, getString(R.string.healthy_budget),
                    getString(R.string.budget_status_healthy_detail));
        } else {
            alert.setAlert(AlertBannerView.Tone.NONE, "", "");
        }
    }

    private void onCategoryClicked(HomeCategoryProgress item) {
        if (currentBudget != null && currentBudget.monthlyLimitMilli > 0L) {
            openRepartition();
        } else {
            openBudget();
        }
    }

    private void openRepartition() {
        if (currentBudget == null) {
            openBudget();
            return;
        }
        if (childMode && childAllocation > 0L && currentBudget.monthlyLimitMilli <= childAllocation) {
            openChildAllocation();
            return;
        }
        boolean closed = "CLOSED".equalsIgnoreCase(currentBudget.status)
                || BudgetPeriodMath.isPastPeriodEnd(currentBudget.periodEnd, Calendar.getInstance());
        startActivity(new Intent(requireContext(), AllocateBudgetActivity.class)
                .putExtra("BUDGET_ID", currentBudget.id)
                .putExtra("START", currentBudget.periodStart)
                .putExtra("END", currentBudget.periodEnd)
                .putExtra("TOTAL", currentBudget.monthlyLimitMilli)
                .putExtra("READ_ONLY", closed));
    }

    private void openBudget() {
        Intent intent = new Intent(requireContext(), BudgetPeriodActivity.class);
        if (currentBudget != null) {
            intent.putExtra("MODE", "edit").putExtra("BUDGET_ID", currentBudget.id);
        }
        startActivity(intent);
    }

    private void showChildBudgetDialog() {
        androidx.appcompat.app.AlertDialog.Builder dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setMessage(R.string.child_budget_locked)
                .setNegativeButton(android.R.string.ok, null);
        if (childAllocation > 0L && currentBudget != null) {
            dialog.setPositiveButton(R.string.partition_child_budget, (d, w) -> openChildAllocation());
        }
        dialog.show();
    }

    private void openChildAllocation() {
        if (childAllocation <= 0L || currentBudget == null) {
            showChildBudgetDialog();
            return;
        }
        startActivity(new Intent(requireContext(), AllocateBudgetActivity.class)
                .putExtra("IS_CHILD", true)
                .putExtra("BUDGET_ID", currentBudget.id));
    }

    private void renderChildren(List<ChildSpendingSummary> children) {
        LinearLayout rows = root.findViewById(R.id.home_children_rows);
        rows.removeAllViews();
        if (children.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.children_empty);
            empty.setTextColor(requireContext().getColor(R.color.muted_green));
            rows.addView(empty);
            return;
        }
        for (ChildSpendingSummary child : children) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardBackgroundColor(requireContext().getColor(R.color.cream));
            card.setRadius(dp(16));
            card.setStrokeWidth(dp(1));
            card.setStrokeColor(requireContext().getColor(R.color.card_border));

            LinearLayout summary = new LinearLayout(requireContext());
            summary.setOrientation(LinearLayout.VERTICAL);
            summary.setPadding(dp(12), dp(10), dp(12), dp(10));

            TextView name = new TextView(requireContext());
            name.setText(child.nickname);
            name.setTextColor(requireContext().getColor(R.color.forest));
            name.setTextSize(15);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            summary.addView(name);

            TextView ratio = new TextView(requireContext());
            ratio.setText(getString(R.string.child_budget_ratio,
                    CurrencyFormatter.formatMilli(child.spentMilli),
                    CurrencyFormatter.formatMilli(child.allocatedTotalMilli)));
            ratio.setTextColor(requireContext().getColor(R.color.ink));
            summary.addView(ratio);

            ProgressBar progress = new ProgressBar(requireContext(), null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(100);
            int percent = child.allocatedTotalMilli <= 0L ? 0
                    : Math.min(100, Math.max(0, MoneyMath.percentOf(child.spentMilli, child.allocatedTotalMilli)));
            progress.setProgress(percent);
            progress.setProgressTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(child.spentMilli > child.allocatedTotalMilli ? R.color.coral : R.color.emerald)));
            progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.mint)));
            LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(7));
            progressParams.topMargin = dp(6);
            summary.addView(progress, progressParams);

            LinearLayout footer = new LinearLayout(requireContext());
            footer.setGravity(android.view.Gravity.CENTER_VERTICAL);
            footer.setOrientation(LinearLayout.HORIZONTAL);
            TextView available = new TextView(requireContext());
            available.setText(getString(R.string.child_available_summary,
                    CurrencyFormatter.formatMilli(child.availableMilli())));
            available.setTextColor(requireContext().getColor(R.color.emerald));
            footer.addView(available, new LinearLayout.LayoutParams(0, -2, 1));

            TextView consumed = new TextView(requireContext());
            consumed.setText(getString(R.string.consumed_percent,
                    child.allocatedTotalMilli <= 0L ? 0 : MoneyMath.percentOf(child.spentMilli, child.allocatedTotalMilli)));
            consumed.setTextColor(requireContext().getColor(R.color.muted_green));
            footer.addView(consumed);
            summary.addView(footer);

            card.addView(summary);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.topMargin = dp(9);
            rows.addView(card, cardParams);
        }
    }

    private void renderChildSummary(HomeDashboardData data) {
        root.findViewById(R.id.home_child_summary).setVisibility(View.VISIBLE);
        String latest = data.latestAllowance == null
                ? getString(R.string.no_recent_allowance)
                : CurrencyFormatter.formatMilli(data.latestAllowance.amountMilli) + " · "
                        + DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                                .format(new Date(data.latestAllowance.occurredAt));
        ((TextView) root.findViewById(R.id.home_child_received)).setText(
                getString(R.string.child_received_summary, CurrencyFormatter.formatMilli(data.totalAllowanceMilli), latest));
        TextView goalLabel = root.findViewById(R.id.home_child_goal);
        ProgressBar goalProgress = root.findViewById(R.id.home_child_goal_progress);
        if (data.goal == null) {
            goalLabel.setText(R.string.child_goal_missing);
            goalProgress.setProgress(0);
        } else {
            goalLabel.setText(getString(R.string.child_goal_summary,
                    data.goal.title == null ? getString(R.string.savings_goal) : data.goal.title,
                    CurrencyFormatter.formatMilli(data.goal.savedMilli),
                    CurrencyFormatter.formatMilli(data.goal.targetMilli)));
            int progress = data.goal.targetMilli <= 0L ? 0
                    : Math.min(100, Math.max(0, MoneyMath.percentOf(data.goal.savedMilli, data.goal.targetMilli)));
            goalProgress.setProgress(progress);
        }
    }

    private void openTransaction(TransactionRow row) {
        Class<?> destination = "purchase".equals(row.kind) ? EditPurchaseActivity.class : EditExpenseActivity.class;
        startActivity(new Intent(requireContext(), destination)
                .putExtra("TRANSACTION_ID", row.id)
                .putExtra("TITLE", row.title)
                .putExtra("AMOUNT", row.amountMilli));
    }

    private String periodLabel(BudgetEntity budget) {
        if (budget == null) return getString(R.string.no_budget_period);
        return BudgetPeriodMath.formatRange(budget.periodStart, budget.periodEnd, Locale.getDefault());
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
