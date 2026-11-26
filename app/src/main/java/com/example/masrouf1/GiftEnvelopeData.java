package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

/** Parent's current Enfant(s) category envelope & monthly total headroom before a transfer. */
public final class GiftEnvelopeData {
    public final BudgetEntity budget;
    public final long remainingMilli;
    public final boolean ready;
    public final long categoryAllocatedMilli;
    public final long categorySpentMilli;
    public final long monthlyRemainingMilli;
    public final boolean hasEnfantEnvelope;

    public GiftEnvelopeData(BudgetEntity budget, long remainingMilli, boolean ready) {
        this(budget, remainingMilli, ready, Math.max(0L, remainingMilli), 0L,
                budget == null ? 0L : Math.max(0L, budget.monthlyLimitMilli - budget.spentMilli), remainingMilli > 0L);
    }

    public GiftEnvelopeData(BudgetEntity budget,
                            long remainingMilli,
                            boolean ready,
                            long categoryAllocatedMilli,
                            long categorySpentMilli,
                            long monthlyRemainingMilli,
                            boolean hasEnfantEnvelope) {
        this.budget = budget;
        this.remainingMilli = remainingMilli;
        this.ready = ready;
        this.categoryAllocatedMilli = Math.max(0L, categoryAllocatedMilli);
        this.categorySpentMilli = Math.max(0L, categorySpentMilli);
        this.monthlyRemainingMilli = monthlyRemainingMilli;
        this.hasEnfantEnvelope = hasEnfantEnvelope;
    }

    public boolean willExceedCategory(long amountMilli) {
        return amountMilli > 0L && (!hasEnfantEnvelope || amountMilli > remainingMilli);
    }

    public boolean willExceedMonthly(long amountMilli) {
        return budget != null && amountMilli > 0L && amountMilli > monthlyRemainingMilli;
    }
}
