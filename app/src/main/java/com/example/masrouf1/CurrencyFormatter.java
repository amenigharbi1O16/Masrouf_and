package com.example.masrouf1;

/** Formats integer millimes as dinars with exactly three decimal places. */
public final class CurrencyFormatter {
    private CurrencyFormatter() { }
    public static String formatMilli(long milli) { return MoneyUtils.formatMilli(milli); }
}
