package com.example.masrouf1;

import com.example.masrouf1.db.ChildSpendingSummary;
import java.util.List;

/** Parent-only family aggregates assembled from Room records. */
public final class FamilyAnalyticsData {
    public final List<ChildSpendingSummary> children;
    public final List<FamilyMonthSpending> months;
    public final long globalBudgetMilli;
    public final long childAllocationsMilli;
    public final long parentSpendingMilli;
    public FamilyAnalyticsData(List<ChildSpendingSummary> children,List<FamilyMonthSpending> months,long globalBudgetMilli,long childAllocationsMilli,long parentSpendingMilli){
        this.children=children;this.months=months;this.globalBudgetMilli=globalBudgetMilli;this.childAllocationsMilli=childAllocationsMilli;this.parentSpendingMilli=parentSpendingMilli;
    }
}
