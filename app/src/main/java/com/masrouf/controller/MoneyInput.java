package com.masrouf.controller;

import com.example.masrouf1.BudgetPeriodMath;

final class MoneyInput {
    private MoneyInput() { }

    static long toMilli(String value) {
        return BudgetPeriodMath.parseSignedOrUnsignedBudgetMilli(value);
    }
}
