package com.example.masrouf1;

import java.math.BigInteger;

/** Formats integer millimes as dinars with exactly three decimal places. */
public final class CurrencyFormatter {
    private CurrencyFormatter() { }
    public static String formatMilli(long milli) {
        BigInteger value = BigInteger.valueOf(milli);
        BigInteger[] parts = value.abs().divideAndRemainder(BigInteger.valueOf(1_000L));
        return (value.signum() < 0 ? "-" : "") + parts[0] + "," + String.format(java.util.Locale.ROOT, "%03d", parts[1].intValue()) + " DT";
    }
}
