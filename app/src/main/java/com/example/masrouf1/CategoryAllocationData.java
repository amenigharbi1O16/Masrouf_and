package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

/** Allocation cap and active budget for a new category in one Room read. */
public final class CategoryAllocationData {
    public final int usedPercent;
    public final BudgetEntity budget;
    public CategoryAllocationData(int usedPercent,BudgetEntity budget){this.usedPercent=usedPercent;this.budget=budget;}
}
