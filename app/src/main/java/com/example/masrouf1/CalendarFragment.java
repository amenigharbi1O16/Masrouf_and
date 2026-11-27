/*
 * ============================================================================
 * FICHIER : CalendarFragment.java
 * MISSION : Onglet central « Calendrier » permettant d'explorer les budgets
 *           sur toute une année civile, de basculer entre les années (< YYYY >),
 *           de sélectionner un mois dans une grille 2×6, de définir un budget
 *           comme période de référence du tableau de bord ou de le modifier/consulter,
 *           et de consulter les transactions du mois sélectionné.
 * IMPLÉMENTE : Sous-classe de DashboardSectionFragment mêlant inflation XML
 *              (fragment_calendar.xml, item_calendar_month.xml,
 *              item_calendar_budget_period.xml) et création dynamique de cartes
 *              Java MaterialCardView pour les transactions mensuelles.
 * EFFET À L'ÉCRAN : En-tête année cliquable, grille des 12 mois avec barres de
 *                   progression vertes/corail, cartes des périodes budgétaires du
 *                   mois sélectionné et liste des transactions cliquables.
 * COMMUNICATIONS :
 *   ← DashboardSectionFragment (coque d'en-tête avec cloche et tiroir)
 *   → MasroufViewModel / MasroufRepository.loadCalendarYear (données annuelles)
 *   → SessionStore.setSelectedBudgetId (changement de budget de référence)
 *   → MainActivity.openCentralTab("home") (retour à l'accueil après sélection)
 *   → BudgetPeriodActivity (création, modification ou consultation lecture seule)
 *   → TransactionDetailsActivity (détail d'une transaction mensuelle)
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.masrouf1.db.TransactionRow;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.masrouf.controller.BudgetPeriodActivity;
import com.masrouf.controller.SessionStore;
import com.masrouf.controller.TransactionDetailsActivity;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Onglet central « Calendrier / Historique » : sélecteur d'année, grille annuelle des 12 mois,
 * liste des périodes budgétaires du mois sélectionné (changement de référence, modification ou consultation).
 */
/**
 * Financial Year Calendar & Historical Budget Explorer matching React Native design:
 * - Year selector (< YYYY >)
 * - 2-column 12-month grid with dynamic spend & consumption bar
 * - Overlapping budget periods for the selected month with "Select as reference",
 *   "Modify" (current/future), and "Consult" / "Closed" read-only historical view
 */
public final class CalendarFragment extends DashboardSectionFragment {
    private static final String STATE_YEAR = "cal_year";
    private static final String STATE_MONTH = "cal_month";

    private static final int[] MONTH_NAME_RESOURCES = {
            R.string.onboarding_step_label_1 // placeholder avoided below; uses DateFormatSymbols / resources
    };

    private int selectedYear;
    private int selectedMonth; // 0..11
    private CalendarYearData yearData;

    private TextView yearTitle, selectedMonthHeader, monthTotalText, stateText;
    private LinearLayout monthsGrid, budgetRows, dayRows;
    private View emptyCard, createPeriodBtn, addAnotherBtn, loadingView;

    @Override
    protected int pageTitle() {
        return R.string.nav_calendar;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Calendar now = Calendar.getInstance();
        if (savedInstanceState != null) {
            selectedYear = savedInstanceState.getInt(STATE_YEAR, now.get(Calendar.YEAR));
            selectedMonth = savedInstanceState.getInt(STATE_MONTH, now.get(Calendar.MONTH));
        } else {
            selectedYear = now.get(Calendar.YEAR);
            selectedMonth = now.get(Calendar.MONTH);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_YEAR, selectedYear);
        outState.putInt(STATE_MONTH, selectedMonth);
    }

    @Override
    protected void createContent(LinearLayout content) {
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_calendar, content, false);
        content.addView(root);
        yearTitle = root.findViewById(R.id.calendar_month_title);
        selectedMonthHeader = root.findViewById(R.id.calendar_selected_date);
        monthTotalText = root.findViewById(R.id.calendar_day_total);
        monthsGrid = root.findViewById(R.id.calendar_months_grid);
        budgetRows = root.findViewById(R.id.calendar_budget_rows);
        dayRows = root.findViewById(R.id.calendar_day_transactions);
        emptyCard = root.findViewById(R.id.calendar_empty_card);
        createPeriodBtn = root.findViewById(R.id.calendar_create_period_btn);
        addAnotherBtn = root.findViewById(R.id.calendar_add_another_btn);
        loadingView = root.findViewById(R.id.calendar_loading);
        stateText = root.findViewById(R.id.calendar_state);

        root.findViewById(R.id.calendar_previous).setOnClickListener(v -> {
            selectedYear--;
            loadYear();
        });
        root.findViewById(R.id.calendar_next).setOnClickListener(v -> {
            selectedYear++;
            loadYear();
        });

        View.OnClickListener openCreator = v -> openCreatePeriodForSelectedMonth();
        createPeriodBtn.setOnClickListener(openCreator);
        addAnotherBtn.setOnClickListener(openCreator);

        renderYearHeader();
    }

    @Override
    protected void observeContent() {
        loadYear();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && viewModel != null) {
            loadYear();
        }
    }

    private void loadYear() {
        if (!isAdded() || viewModel == null) return;
        renderYearHeader();
        if (loadingView != null) loadingView.setVisibility(View.VISIBLE);
        if (stateText != null) stateText.setVisibility(View.GONE);
        long userId = signedInUserId();
        long selectedBudgetId = SessionStore.selectedBudgetId(requireContext(), userId);
        int requestedYear = selectedYear;
        viewModel.<CalendarYearData>request(
                repository -> repository.loadCalendarYear(userId, requestedYear, selectedBudgetId),
                result -> {
                    if (!isAdded() || getView() == null) return;
                    if (loadingView != null) loadingView.setVisibility(View.GONE);
                    if (result.failed || result.data == null) {
                        if (stateText != null) {
                            stateText.setText(R.string.dashboard_read_error);
                            stateText.setVisibility(View.VISIBLE);
                        }
                        return;
                    }
                    yearData = result.data;
                    renderYearGrid();
                    renderSelectedMonthDetails();
                }
        );
    }

    private void renderYearHeader() {
        if (yearTitle != null) yearTitle.setText(String.valueOf(selectedYear));
    }

    private void renderYearGrid() {
        if (monthsGrid == null || !isAdded() || yearData == null) return;
        monthsGrid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        String[] months = localizedMonthNames();

        for (int rowIdx = 0; rowIdx < 6; rowIdx++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(10);

            for (int col = 0; col < 2; col++) {
                final int monthIdx = rowIdx * 2 + col;
                View item = inflater.inflate(R.layout.item_calendar_month, row, false);
                MaterialCardView card = item.findViewById(R.id.calendar_month_card);
                TextView nameView = item.findViewById(R.id.calendar_month_name);
                TextView spentView = item.findViewById(R.id.calendar_month_spent);
                ProgressBar progress = item.findViewById(R.id.calendar_month_progress);

                boolean active = (monthIdx == selectedMonth);
                long spentMilli = yearData.monthlySpentMilli[monthIdx];
                long allocMilli = yearData.monthlyAllocatedMilli[monthIdx];
                int ratioPct = allocMilli <= 0 ? 0 : Math.min(100, Math.max(0, MoneyMath.percentOf(spentMilli, allocMilli)));

                nameView.setText(months[monthIdx]);
                spentView.setText(formatShortDinars(spentMilli));

                card.setCardBackgroundColor(requireContext().getColor(active ? R.color.forest : R.color.alabaster));
                card.setStrokeColor(requireContext().getColor(active ? R.color.forest : R.color.card_border));
                nameView.setTextColor(requireContext().getColor(active ? R.color.alabaster : R.color.forest));
                spentView.setTextColor(requireContext().getColor(active ? R.color.mint : R.color.muted_green));
                progress.setProgress(ratioPct);
                progress.setProgressBackgroundTintList(ColorStateList.valueOf(
                        requireContext().getColor(active ? R.color.muted_green : R.color.card_border)));
                int fillColor = active
                        ? R.color.mint
                        : (allocMilli > 0 && spentMilli >= allocMilli ? R.color.coral : R.color.emerald);
                progress.setProgressTintList(ColorStateList.valueOf(requireContext().getColor(fillColor)));

                card.setOnClickListener(v -> {
                    selectedMonth = monthIdx;
                    renderYearGrid();
                    renderSelectedMonthDetails();
                });

                LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -2, 1f);
                if (col == 0) cellParams.setMarginEnd(dp(10));
                row.addView(item, cellParams);
            }
            monthsGrid.addView(row, rowParams);
        }
    }

    private void renderSelectedMonthDetails() {
        if (!isAdded() || yearData == null) return;
        String[] months = localizedMonthNames();
        if (selectedMonthHeader != null) {
            selectedMonthHeader.setText(String.format(Locale.getDefault(), "%s %d", months[selectedMonth], selectedYear));
        }

        Calendar now = Calendar.getInstance();
        boolean pastMonth = BudgetPeriodMath.isPastMonth(selectedYear, selectedMonth, now);
        List<CalendarYearData.BudgetPeriodSummary> budgets = yearData.monthBudgets.containsKey(selectedMonth)
                ? yearData.monthBudgets.get(selectedMonth)
                : Collections.emptyList();

        budgetRows.removeAllViews();
        if (budgets.isEmpty()) {
            emptyCard.setVisibility(View.VISIBLE);
            addAnotherBtn.setVisibility(View.GONE);
            TextView detail = emptyCard.findViewById(R.id.calendar_empty_detail);
            if (pastMonth) {
                detail.setText(R.string.calendar_past_month_detail);
                createPeriodBtn.setVisibility(View.GONE);
            } else {
                detail.setText(R.string.calendar_no_budget_detail);
                createPeriodBtn.setVisibility(View.VISIBLE);
            }
        } else {
            emptyCard.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            long userId = signedInUserId();
            for (CalendarYearData.BudgetPeriodSummary item : budgets) {
                View rowView = inflater.inflate(R.layout.item_calendar_budget_period, budgetRows, false);
                MaterialCardView card = rowView.findViewById(R.id.calendar_budget_card);
                TextView rangeView = rowView.findViewById(R.id.calendar_budget_range);
                TextView ratioView = rowView.findViewById(R.id.calendar_budget_ratio);
                TextView badgeView = rowView.findViewById(R.id.calendar_budget_badge);
                ProgressBar progress = rowView.findViewById(R.id.calendar_budget_progress);
                MaterialButton refBtn = rowView.findViewById(R.id.calendar_budget_select_ref);
                MaterialButton secBtn = rowView.findViewById(R.id.calendar_budget_secondary_action);

                boolean isActive = item.budget.id == yearData.selectedBudgetId;
                boolean isClosed = "CLOSED".equalsIgnoreCase(item.budget.status)
                        || BudgetPeriodMath.isPastPeriodEnd(item.budget.periodEnd, now);
                boolean exceeded = item.budget.monthlyLimitMilli > 0
                        && item.actualSpentMilli > item.budget.monthlyLimitMilli;

                rangeView.setText(BudgetPeriodMath.formatRange(item.budget.periodStart, item.budget.periodEnd, Locale.getDefault()));
                ratioView.setText(String.format(Locale.getDefault(), "%s / %s",
                        CurrencyFormatter.formatMilli(item.actualSpentMilli),
                        CurrencyFormatter.formatMilli(item.budget.monthlyLimitMilli)));
                ratioView.setTextColor(requireContext().getColor(exceeded ? R.color.coral : R.color.muted_green));

                int pct = item.budget.monthlyLimitMilli <= 0 ? 0
                        : Math.min(100, Math.max(0, MoneyMath.percentOf(item.actualSpentMilli, item.budget.monthlyLimitMilli)));
                progress.setProgress(pct);
                progress.setProgressTintList(ColorStateList.valueOf(
                        requireContext().getColor(exceeded ? R.color.coral : R.color.emerald)));

                card.setStrokeColor(requireContext().getColor(isActive ? R.color.emerald : R.color.card_border));

                if (isActive) {
                    badgeView.setText(R.string.calendar_active_badge);
                    badgeView.setTextColor(requireContext().getColor(R.color.emerald));
                    badgeView.setVisibility(View.VISIBLE);
                } else if (isClosed) {
                    badgeView.setText(R.string.calendar_closed_badge);
                    badgeView.setTextColor(requireContext().getColor(R.color.muted_green));
                    badgeView.setVisibility(View.VISIBLE);
                } else {
                    badgeView.setVisibility(View.GONE);
                }

                if (isActive) {
                    refBtn.setVisibility(View.GONE);
                } else {
                    refBtn.setVisibility(View.VISIBLE);
                    refBtn.setOnClickListener(v -> {
                        SessionStore.setSelectedBudgetId(requireContext(), userId, item.budget.id);
                        if (requireActivity() instanceof MainActivity) {
                            ((MainActivity) requireActivity()).openCentralTab("home");
                        }
                    });
                }

                if (isClosed) {
                    secBtn.setText(R.string.calendar_consult);
                    secBtn.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.forest)));
                    secBtn.setOnClickListener(v -> startActivity(
                            new Intent(requireContext(), BudgetPeriodActivity.class)
                                    .putExtra("BUDGET_ID", item.budget.id)
                                    .putExtra("READ_ONLY", true)));
                } else {
                    secBtn.setText(R.string.calendar_modify);
                    secBtn.setBackgroundTintList(ColorStateList.valueOf(requireContext().getColor(R.color.emerald)));
                    secBtn.setOnClickListener(v -> startActivity(
                            new Intent(requireContext(), BudgetPeriodActivity.class)
                                    .putExtra("MODE", "edit")
                                    .putExtra("BUDGET_ID", item.budget.id)));
                }

                budgetRows.addView(rowView);
            }
            addAnotherBtn.setVisibility(!pastMonth ? View.VISIBLE : View.GONE);
        }

        renderMonthTransactions();
    }

    /** Affiche dynamiquement en cartes MaterialCardView cliquables les transactions du mois sélectionné. */
    private void renderMonthTransactions() {
        if (!isAdded() || yearData == null || dayRows == null) return;
        List<TransactionRow> rows = yearData.monthTransactions.containsKey(selectedMonth)
                ? yearData.monthTransactions.get(selectedMonth)
                : Collections.emptyList();
        long totalMilli = yearData.monthlySpentMilli[selectedMonth];
        if (monthTotalText != null) {
            monthTotalText.setText(getString(R.string.calendar_day_total, CurrencyFormatter.formatMilli(totalMilli)));
        }
        dayRows.removeAllViews();
        if (rows.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.calendar_no_transactions);
            empty.setTextColor(requireContext().getColor(R.color.muted_green));
            empty.setTextSize(13f);
            empty.setPadding(0, dp(4), 0, dp(8));
            dayRows.addView(empty);
            return;
        }
        DateFormat fmt = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        for (TransactionRow tx : rows) {
            MaterialCardView txCard = new MaterialCardView(requireContext());
            txCard.setCardBackgroundColor(requireContext().getColor(R.color.alabaster));
            txCard.setRadius(dp(12));
            txCard.setCardElevation(0);
            txCard.setStrokeColor(requireContext().getColor(R.color.card_border));
            txCard.setStrokeWidth(dp(1));

            LinearLayout line = new LinearLayout(requireContext());
            line.setOrientation(LinearLayout.HORIZONTAL);
            line.setPadding(dp(12), dp(10), dp(12), dp(10));
            line.setGravity(android.view.Gravity.CENTER_VERTICAL);

            String title = tx.title == null || tx.title.trim().isEmpty()
                    ? getString(R.string.transaction_name)
                    : tx.title;
            TextView labelView = new TextView(requireContext());
            labelView.setText(title + " · " + fmt.format(new Date(tx.occurredAt)));
            labelView.setTextColor(requireContext().getColor(R.color.ink));
            labelView.setTextSize(13f);
            line.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView amountView = new TextView(requireContext());
            amountView.setText(CurrencyFormatter.formatMilli(tx.amountMilli));
            amountView.setTextColor(requireContext().getColor(R.color.forest));
            amountView.setTextSize(13f);
            amountView.setTypeface(null, android.graphics.Typeface.BOLD);
            line.addView(amountView);

            txCard.addView(line);
            txCard.setOnClickListener(v -> startActivity(
                    new Intent(requireContext(), TransactionDetailsActivity.class)
                            .putExtra("TRANSACTION_ID", tx.id)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(6);
            dayRows.addView(txCard, lp);
        }
    }

    /** Ouvre BudgetPeriodActivity pré-remplie avec le mois futur/courant sélectionné dans la grille. */
    private void openCreatePeriodForSelectedMonth() {
        if (BudgetPeriodMath.isPastMonth(selectedYear, selectedMonth, Calendar.getInstance())) return;
        long start = BudgetPeriodMath.startOfMonthMillis(selectedYear, selectedMonth);
        long end = BudgetPeriodMath.endOfMonthMillis(selectedYear, selectedMonth);
        startActivity(new Intent(requireContext(), BudgetPeriodActivity.class)
                .putExtra("DEFAULT_START", start)
                .putExtra("DEFAULT_END", end));
    }

    private String formatShortDinars(long milli) {
        if (milli == 0L) return getString(R.string.calendar_short_dt, "0");
        if (milli % 1_000L == 0L) return getString(R.string.calendar_short_dt, String.valueOf(milli / 1_000L));
        return CurrencyFormatter.formatMilli(milli);
    }

    private String[] localizedMonthNames() {
        String lang = Locale.getDefault().getLanguage();
        if ("fr".equalsIgnoreCase(lang)) {
            return new String[]{"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};
        }
        if ("ar".equalsIgnoreCase(lang)) {
            return new String[]{"جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
                    "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"};
        }
        return new String[]{"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
    }
}
