package com.example.masrouf1.db;

/** Aggregate-only monthly budget record used for a child's four-month history. */
public final class BudgetHistoryRow {
    public long periodStart;
    public long allocatedMilli;
    public long spentMilli;
    public int pointsAwarded;
}
