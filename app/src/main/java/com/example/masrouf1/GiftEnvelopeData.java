package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

/** Parent's current child allowance envelope before a transfer. */
public final class GiftEnvelopeData {
    public final BudgetEntity budget;
    public final long remainingMilli;
    public final boolean ready;
    public GiftEnvelopeData(BudgetEntity budget,long remainingMilli,boolean ready){this.budget=budget;this.remainingMilli=remainingMilli;this.ready=ready;}
}
