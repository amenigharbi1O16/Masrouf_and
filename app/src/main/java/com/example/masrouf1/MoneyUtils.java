package com.example.masrouf1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Money conversion and display helpers. All persisted values are integer millimes. */
public final class MoneyUtils {
    private MoneyUtils() { }

    /** Parses a dinar amount with at most three decimals into integer millimes. */
    public static long toMilli(String amount) {
        String normalized = amount == null ? "" : amount.trim()
                .replace("\u00a0", "").replace("\u202f", "").replace("\u2009", "")
                .replace(" ", "").replace(',', '.');
        if (normalized.isEmpty()) throw new NumberFormatException("Amount is empty");
        return new BigDecimal(normalized).setScale(3, RoundingMode.UNNECESSARY)
                .movePointRight(3).longValueExact();
    }

    /** Formats millimes in French notation with grouping and exactly three decimals. */
    public static String formatMilli(long amountMilli) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRANCE);
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0.000", symbols);
        format.setGroupingUsed(true);
        format.setRoundingMode(RoundingMode.UNNECESSARY);
        return format.format(BigDecimal.valueOf(amountMilli, 3)) + " DT";
    }
}
