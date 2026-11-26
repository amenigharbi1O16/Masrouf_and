package com.example.masrouf1;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class BudgetPeriodMathTest {

    @Test
    public void validatesIsoDatesAndLeapYearDays() {
        assertTrue(BudgetPeriodMath.isValidIsoDate("2026-11-01"));
        assertTrue(BudgetPeriodMath.isValidIsoDate("2024-02-29"));
        assertFalse(BudgetPeriodMath.isValidIsoDate("2026-02-29"));
        assertFalse(BudgetPeriodMath.isValidIsoDate("2026-02-30"));
        assertFalse(BudgetPeriodMath.isValidIsoDate("2026-13-01"));
        assertFalse(BudgetPeriodMath.isValidIsoDate("01/11/2026"));

        assertEquals(30, BudgetPeriodMath.daysInMonth(2026, Calendar.NOVEMBER));
        assertEquals(29, BudgetPeriodMath.daysInMonth(2024, Calendar.FEBRUARY));
        assertEquals(28, BudgetPeriodMath.daysInMonth(2026, Calendar.FEBRUARY));
    }

    @Test
    public void formatsDefaultMonthBoundsAndDaysInclusive() {
        long start = BudgetPeriodMath.startOfMonthMillis(2026, Calendar.NOVEMBER);
        long end = BudgetPeriodMath.endOfMonthMillis(2026, Calendar.NOVEMBER);
        assertEquals("2026-11-01", BudgetPeriodMath.formatIsoDate(start));
        assertEquals("2026-11-30", BudgetPeriodMath.formatIsoDate(end));
        assertEquals(30, BudgetPeriodMath.daysBetweenInclusive(start, end));
    }

    @Test
    public void detectsPeriodOverlapAndDisjointBoundaries() {
        long novStart = BudgetPeriodMath.parseIsoDateStartMillis("2026-11-01");
        long novEnd = BudgetPeriodMath.parseIsoDateEndMillis("2026-11-30");
        long decStart = BudgetPeriodMath.parseIsoDateStartMillis("2026-12-01");
        long decEnd = BudgetPeriodMath.parseIsoDateEndMillis("2026-12-31");
        long midStart = BudgetPeriodMath.parseIsoDateStartMillis("2026-11-15");
        long midEnd = BudgetPeriodMath.parseIsoDateEndMillis("2026-12-10");

        assertFalse(BudgetPeriodMath.periodsOverlap(novStart, novEnd, decStart, decEnd));
        assertTrue(BudgetPeriodMath.periodsOverlap(novStart, novEnd, midStart, midEnd));
        assertTrue(BudgetPeriodMath.periodsOverlap(decStart, decEnd, midStart, midEnd));
        assertTrue(BudgetPeriodMath.periodsOverlap(novStart, novEnd, novEnd, novEnd));
    }

    @Test
    public void distinguishesPastVsCurrentOrFuturePeriods() {
        Calendar nov15 = Calendar.getInstance();
        nov15.clear();
        nov15.set(2026, Calendar.NOVEMBER, 15, 10, 30, 0);

        assertTrue(BudgetPeriodMath.isPastMonth(2026, Calendar.OCTOBER, nov15));
        assertFalse(BudgetPeriodMath.isPastMonth(2026, Calendar.NOVEMBER, nov15));
        assertFalse(BudgetPeriodMath.isPastMonth(2026, Calendar.DECEMBER, nov15));

        long octEnd = BudgetPeriodMath.parseIsoDateEndMillis("2026-10-31");
        long novEnd = BudgetPeriodMath.parseIsoDateEndMillis("2026-11-30");
        long nov14End = BudgetPeriodMath.parseIsoDateEndMillis("2026-11-14");
        long nov15End = BudgetPeriodMath.parseIsoDateEndMillis("2026-11-15");

        assertTrue(BudgetPeriodMath.isPastPeriodEnd(octEnd, nov15));
        assertTrue(BudgetPeriodMath.isPastPeriodEnd(nov14End, nov15));
        assertFalse(BudgetPeriodMath.isPastPeriodEnd(nov15End, nov15));
        assertFalse(BudgetPeriodMath.isPastPeriodEnd(novEnd, nov15));
    }

    @Test
    public void convertsBasisPointsAndAmountsWithoutDrift() {
        long total = 1_200_000L; // 1200.000 DT
        int bps = BudgetPeriodMath.parsePercentToBps("12,5");
        assertEquals(1250, bps);
        assertEquals("12.5", BudgetPeriodMath.formatPercentFromBps(bps));

        long amount = BudgetPeriodMath.amountFromPercentBps(total, bps);
        assertEquals(150_000L, amount);
        assertEquals(1250, BudgetPeriodMath.percentBpsFromAmount(total, amount));

        assertEquals(420_000L, BudgetPeriodMath.amountFromPercentBps(total, 3500));
        assertEquals("35", BudgetPeriodMath.formatPercentFromBps(3500));

        try {
            BudgetPeriodMath.parsePercentToBps("-5");
            fail("Expected negative percentage to be rejected");
        } catch (NumberFormatException expected) { }
    }

    @Test
    public void parsesAnyPositiveBudgetAmountWithoutArbitraryCapsOrDecimalErrors() {
        assertEquals(1_563_000L, BudgetPeriodMath.parsePositiveBudgetMilli("1563"));
        assertEquals(1_563_500L, BudgetPeriodMath.parsePositiveBudgetMilli("1563,5"));
        assertEquals(1_563_500L, BudgetPeriodMath.parsePositiveBudgetMilli("1 563.50 DT"));
        assertEquals(100_333L, BudgetPeriodMath.parsePositiveBudgetMilli("100.3334"));
        assertEquals(1L, BudgetPeriodMath.parsePositiveBudgetMilli("0.001"));
        assertEquals(999_999_999_000L, BudgetPeriodMath.parsePositiveBudgetMilli("999999999"));
        assertEquals(0L, BudgetPeriodMath.parseNonNegativeBudgetMilli(""));

        try {
            BudgetPeriodMath.parsePositiveBudgetMilli("0");
            fail("Expected zero budget to be rejected");
        } catch (NumberFormatException expected) { }
        try {
            BudgetPeriodMath.parsePositiveBudgetMilli("-10");
            fail("Expected negative budget to be rejected");
        } catch (NumberFormatException expected) { }
    }

    @Test
    public void computesEqualPreviousComparisonWindowAndCompactAmounts() {
        long novStart = BudgetPeriodMath.parseIsoDateStartMillis("2026-11-01");
        long novEnd = BudgetPeriodMath.parseIsoDateEndMillis("2026-11-30");
        long prevStart = BudgetPeriodMath.previousWindowStartMillis(novStart, novEnd);
        long prevEnd = BudgetPeriodMath.previousWindowEndMillis(novStart);

        assertEquals("2026-10-02", BudgetPeriodMath.formatIsoDate(prevStart));
        assertEquals("2026-10-31", BudgetPeriodMath.formatIsoDate(prevEnd));
        assertEquals(30, BudgetPeriodMath.daysBetweenInclusive(prevStart, prevEnd));

        assertEquals("0", BudgetPeriodMath.compactDinars(0L));
        assertEquals("28", BudgetPeriodMath.compactDinars(28_000L));
        assertEquals("12.5", BudgetPeriodMath.compactDinars(12_500L));
        assertEquals("Nov", BudgetPeriodMath.shortMonthLabel(Calendar.NOVEMBER, java.util.Locale.ENGLISH));
    }
}
