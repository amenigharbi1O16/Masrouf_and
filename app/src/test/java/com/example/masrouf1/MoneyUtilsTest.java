package com.example.masrouf1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class MoneyUtilsTest {
    @Test public void parsesWholeAndMillimeAmountsWithoutFloatingPoint() {
        assertEquals(1_250_000L, MoneyUtils.toMilli("1 250,000"));
        assertEquals(25_750L, MoneyUtils.toMilli("25,750"));
        assertEquals(0L, MoneyUtils.toMilli("0,000"));
    }

    @Test public void formatsWithFrenchGroupingAndExactlyThreeDecimals() {
        assertEquals("1 250,000 DT", MoneyUtils.formatMilli(1_250_000L));
        assertEquals("25,750 DT", MoneyUtils.formatMilli(25_750L));
        assertEquals("0,000 DT", MoneyUtils.formatMilli(0L));
    }

    @Test public void rejectsPrecisionLossAndOverflow() {
        assertThrows(() -> MoneyUtils.toMilli("0,0001"));
        assertThrows(() -> MoneyUtils.toMilli("9223372036854775,808"));
    }

    private static void assertThrows(Runnable action) {
        try { action.run(); fail("Expected invalid money input to be rejected"); }
        catch (ArithmeticException | NumberFormatException expected) { }
    }
}
