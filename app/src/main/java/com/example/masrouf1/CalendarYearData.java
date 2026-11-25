package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;

import java.util.List;
import java.util.Map;

/** Database-derived yearly overview and per-month overlapping budget summaries for CalendarFragment. */
public final class CalendarYearData {
    public final int year;
    public final long[] monthlySpentMilli;
    public final long[] monthlyAllocatedMilli;
    public final Map<Integer, List<BudgetPeriodSummary>> monthBudgets;
    public final long selectedBudgetId;
    public final Map<Integer, List<TransactionRow>> monthTransactions;

    public CalendarYearData(int year,
                            long[] monthlySpentMilli,
                            long[] monthlyAllocatedMilli,
                            Map<Integer, List<BudgetPeriodSummary>> monthBudgets,
                            long selectedBudgetId,
                            Map<Integer, List<TransactionRow>> monthTransactions) {
        this.year = year;
        this.monthlySpentMilli = monthlySpentMilli;
        this.monthlyAllocatedMilli = monthlyAllocatedMilli;
        this.monthBudgets = monthBudgets;
        this.selectedBudgetId = selectedBudgetId;
        this.monthTransactions = monthTransactions;
    }

    public static final class BudgetPeriodSummary {
        public final BudgetEntity budget;
        public final long actualSpentMilli;

        public BudgetPeriodSummary(BudgetEntity budget, long actualSpentMilli) {
            this.budget = budget;
            this.actualSpentMilli = actualSpentMilli;
        }
    }
}
