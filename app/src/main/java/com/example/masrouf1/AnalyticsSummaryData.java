package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;

import java.util.Collections;
import java.util.List;

/**
 * Complete Room-backed analytics snapshot supporting Personal Users, Children (own isolated ledger),
 * and Parents (household overview, category breakdown, 6-month consolidated history, and privacy-safe child stats).
 */
public final class AnalyticsSummaryData {
    public static final class CategoryStat {
        public final long categoryId;
        public final String name;
        public final String catalogKey;
        public final String iconName;
        public final String colorHex;
        public final long allocatedMilli;
        public final long spentMilli;
        public final boolean hasBudget;
        public final int plannedPercent;
        public final double consumptionPct;
        public final double shareOfTotal;

        public CategoryStat(long categoryId, String name, String catalogKey, String iconName, String colorHex,
                            long allocatedMilli, long spentMilli, boolean hasBudget, int plannedPercent,
                            double consumptionPct, double shareOfTotal) {
            this.categoryId = categoryId;
            this.name = name;
            this.catalogKey = catalogKey;
            this.iconName = iconName;
            this.colorHex = colorHex;
            this.allocatedMilli = allocatedMilli;
            this.spentMilli = spentMilli;
            this.hasBudget = hasBudget;
            this.plannedPercent = plannedPercent;
            this.consumptionPct = consumptionPct;
            this.shareOfTotal = shareOfTotal;
        }
    }

    public static final class ChildAnalyticsItem {
        public final long childUserId;
        public final String nickname;
        public final String email;
        public final long allocatedMilli;
        public final long spentMilli;
        public final long remainingMilli;
        public final int percent;
        public final List<PillBarChart.BarItem> sixMonthHistory;

        public ChildAnalyticsItem(long childUserId, String nickname, String email, long allocatedMilli,
                                  long spentMilli, long remainingMilli, int percent,
                                  List<PillBarChart.BarItem> sixMonthHistory) {
            this.childUserId = childUserId;
            this.nickname = nickname;
            this.email = email;
            this.allocatedMilli = allocatedMilli;
            this.spentMilli = spentMilli;
            this.remainingMilli = remainingMilli;
            this.percent = percent;
            this.sixMonthHistory = sixMonthHistory == null ? Collections.emptyList() : sixMonthHistory;
        }
    }

    public final AppRole role;
    public final BudgetEntity budget;
    public final long currentSpentMilli;
    public final long previousSpentMilli;
    public final long prevWindowStartMillis;
    public final long prevWindowEndMillis;
    public final long purchasesTotalMilli;
    public final long expensesTotalMilli;
    public final long avgDailySpendMilli;
    public final int daysElapsed;
    public final int totalDays;
    public final int daysRemaining;
    public final long projectedTotalMilli;
    public final boolean paceSustainable;
    public final long projectedDiffMilli;
    public final List<CategoryStat> categoryStats;
    public final CategoryStat attentionCategory;
    public final List<PillBarChart.BarItem> sixMonthHistory;
    public final List<TransactionRow> periodTransactions;
    public final List<ChildAnalyticsItem> childrenProgress;

    public AnalyticsSummaryData(
            AppRole role,
            BudgetEntity budget,
            long currentSpentMilli,
            long previousSpentMilli,
            long prevWindowStartMillis,
            long prevWindowEndMillis,
            long purchasesTotalMilli,
            long expensesTotalMilli,
            long avgDailySpendMilli,
            int daysElapsed,
            int totalDays,
            int daysRemaining,
            long projectedTotalMilli,
            boolean paceSustainable,
            long projectedDiffMilli,
            List<CategoryStat> categoryStats,
            CategoryStat attentionCategory,
            List<PillBarChart.BarItem> sixMonthHistory,
            List<TransactionRow> periodTransactions,
            List<ChildAnalyticsItem> childrenProgress) {
        this.role = role == null ? AppRole.USER : role;
        this.budget = budget;
        this.currentSpentMilli = currentSpentMilli;
        this.previousSpentMilli = previousSpentMilli;
        this.prevWindowStartMillis = prevWindowStartMillis;
        this.prevWindowEndMillis = prevWindowEndMillis;
        this.purchasesTotalMilli = purchasesTotalMilli;
        this.expensesTotalMilli = expensesTotalMilli;
        this.avgDailySpendMilli = avgDailySpendMilli;
        this.daysElapsed = daysElapsed;
        this.totalDays = totalDays;
        this.daysRemaining = daysRemaining;
        this.projectedTotalMilli = projectedTotalMilli;
        this.paceSustainable = paceSustainable;
        this.projectedDiffMilli = projectedDiffMilli;
        this.categoryStats = categoryStats == null ? Collections.emptyList() : categoryStats;
        this.attentionCategory = attentionCategory;
        this.sixMonthHistory = sixMonthHistory == null ? Collections.emptyList() : sixMonthHistory;
        this.periodTransactions = periodTransactions == null ? Collections.emptyList() : periodTransactions;
        this.childrenProgress = childrenProgress == null ? Collections.emptyList() : childrenProgress;
    }
}
