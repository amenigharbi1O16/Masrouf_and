package com.example.masrouf1;

/** Combined child allocation and spending for one calendar month, in millimes. */
public final class FamilyMonthSpending {
    public final long periodStart;
    public long allocatedMilli;
    public long spentMilli;
    public FamilyMonthSpending(long periodStart){this.periodStart=periodStart;}
}
