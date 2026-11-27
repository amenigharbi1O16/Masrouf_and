/*
 * ============================================================================
 * FICHIER : BudgetPeriodMath.java
 * MISSION : Utilitaires purs de calcul de périodes budgétaires, bornes
 *           calendaires exactes (début/fin de journée et de mois en ms),
 *           validation/formatage ISO (YYYY-MM-DD), conversion points de base
 *           (basis points bps : 0..10000) et parsing de montants budgétaires.
 * IMPLÉMENTE : Classe statique sans dépendance Android Context, utilisant
 *              Calendar, BigDecimal, BigInteger et Pattern ISO_DATE pour garantir
 *              des tests unitaires JVM déterministes (BudgetPeriodMathTest).
 * EFFET À L'ÉCRAN : Formate les plages de dates ("01–30 NOV 2026"), calcule les
 *                   fenêtres précédentes d'analyse, les montants d'enveloppes
 *                   et les étiquettes compactes ("28", "12.5") du PillBarChart.
 * COMMUNICATIONS :
 *   ← CalendarFragment, BudgetPeriodActivity, AllocateBudgetActivity
 *   ← MasroufRepository (loadCalendarYear, loadAnalytics, clôture de période)
 *   ← AnalyticsRenderer (étiquettes de plages et graphiques)
 *   ← BudgetPeriodMathTest (suite de tests unitaires JUnit)
 * ============================================================================
 */
package com.example.masrouf1;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure date-range, calendar bounds, and independent percent/amount math helpers. */
public final class BudgetPeriodMath {
    private static final Pattern ISO_DATE = Pattern.compile("^(\\d{4})-(\\d{2})-(\\d{2})$");
    private static final BigInteger TEN_THOUSAND = BigInteger.valueOf(10_000L);

    private BudgetPeriodMath() { }

    public static int daysInMonth(int year, int monthZeroBased) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, monthZeroBased);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }

    public static long startOfDayMillis(int year, int monthZeroBased, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, monthZeroBased, dayOfMonth, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    public static long endOfDayMillis(int year, int monthZeroBased, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, monthZeroBased, dayOfMonth, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    public static long startOfMonthMillis(int year, int monthZeroBased) {
        return startOfDayMillis(year, monthZeroBased, 1);
    }

    public static long endOfMonthMillis(int year, int monthZeroBased) {
        return endOfDayMillis(year, monthZeroBased, daysInMonth(year, monthZeroBased));
    }

    public static long todayStartMillis() {
        Calendar now = Calendar.getInstance();
        return startOfDayMillis(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
    }

    public static boolean isPastMonth(int year, int monthZeroBased, Calendar now) {
        int cy = now.get(Calendar.YEAR);
        int cm = now.get(Calendar.MONTH);
        return year < cy || (year == cy && monthZeroBased < cm);
    }

    public static boolean isPastPeriodEnd(long periodEndMillis, Calendar today) {
        long todayStart = startOfDayMillis(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH));
        return periodEndMillis < todayStart;
    }

    public static boolean periodsOverlap(long s1, long e1, long s2, long e2) {
        return s1 <= e2 && e1 >= s2;
    }

    /** Parses strict YYYY-MM-DD into local 00:00:00.000 millis, or throws IllegalArgumentException. */
    public static long parseIsoDateStartMillis(String iso) {
        int[] ymd = parseIsoParts(iso);
        return startOfDayMillis(ymd[0], ymd[1] - 1, ymd[2]);
    }

    /** Parses strict YYYY-MM-DD into local 23:59:59.999 millis, or throws IllegalArgumentException. */
    public static long parseIsoDateEndMillis(String iso) {
        int[] ymd = parseIsoParts(iso);
        return endOfDayMillis(ymd[0], ymd[1] - 1, ymd[2]);
    }

    public static int[] parseIsoParts(String iso) {
        if (iso == null) throw new IllegalArgumentException("Invalid ISO date");
        String trimmed = iso.trim();
        Matcher m = ISO_DATE.matcher(trimmed);
        if (!m.matches()) throw new IllegalArgumentException("Invalid ISO date");
        int y = Integer.parseInt(m.group(1));
        int mo = Integer.parseInt(m.group(2));
        int d = Integer.parseInt(m.group(3));
        if (mo < 1 || mo > 12) throw new IllegalArgumentException("Invalid month");
        int maxDay = daysInMonth(y, mo - 1);
        if (d < 1 || d > maxDay) throw new IllegalArgumentException("Invalid day");
        return new int[]{y, mo, d};
    }

    public static boolean isValidIsoDate(String iso) {
        try {
            parseIsoParts(iso);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static String formatIsoDate(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return String.format(Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    public static String formatIsoDate(int year, int monthZeroBased, int dayOfMonth) {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, dayOfMonth);
    }

    public static int daysBetweenInclusive(long startMillis, long endMillis) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(startMillis);
        long s = startOfDayMillis(a.get(Calendar.YEAR), a.get(Calendar.MONTH), a.get(Calendar.DAY_OF_MONTH));
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(endMillis);
        long e = startOfDayMillis(b.get(Calendar.YEAR), b.get(Calendar.MONTH), b.get(Calendar.DAY_OF_MONTH));
        long diff = (e - s) / 86_400_000L;
        return Math.max(1, (int) diff + 1);
    }

    public static String formatRange(long startMillis, long endMillis, Locale locale) {
        Calendar a = Calendar.getInstance();
        a.setTimeInMillis(startMillis);
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(endMillis);
        String[] shortMonths = new DateFormatSymbols(locale).getShortMonths();
        String monA = shortMonths[a.get(Calendar.MONTH)].toUpperCase(locale);
        String monB = shortMonths[b.get(Calendar.MONTH)].toUpperCase(locale);
        int dayA = a.get(Calendar.DAY_OF_MONTH);
        int dayB = b.get(Calendar.DAY_OF_MONTH);
        int yearA = a.get(Calendar.YEAR);
        int yearB = b.get(Calendar.YEAR);
        if (yearA == yearB && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)) {
            return String.format(locale, "%02d–%02d %s %d", dayA, dayB, monB, yearB);
        }
        return String.format(locale, "%02d %s – %02d %s %d", dayA, monA, dayB, monB, yearB);
    }

    /** Converts basis points (0..10000 -> 0%..100%) to millimes half-up. */
    public static long amountFromPercentBps(long totalMilli, int bps) {
        if (totalMilli <= 0 || bps <= 0) return 0L;
        BigInteger num = BigInteger.valueOf(totalMilli).multiply(BigInteger.valueOf(bps));
        BigInteger[] qr = num.divideAndRemainder(TEN_THOUSAND);
        if (qr[1].shiftLeft(1).compareTo(TEN_THOUSAND) >= 0) qr[0] = qr[0].add(BigInteger.ONE);
        return qr[0].longValueExact();
    }

    /** Converts amountMilli out of totalMilli to basis points (1% = 100 bps, 12.5% = 1250 bps). */
    public static int percentBpsFromAmount(long totalMilli, long amountMilli) {
        if (totalMilli <= 0 || amountMilli <= 0) return 0;
        BigInteger num = BigInteger.valueOf(amountMilli).multiply(TEN_THOUSAND);
        BigInteger den = BigInteger.valueOf(totalMilli);
        BigInteger[] qr = num.divideAndRemainder(den);
        if (qr[1].shiftLeft(1).compareTo(den) >= 0) qr[0] = qr[0].add(BigInteger.ONE);
        BigInteger max = BigInteger.valueOf(Integer.MAX_VALUE);
        return qr[0].compareTo(max) > 0 ? Integer.MAX_VALUE : qr[0].intValue();
    }

    /** Parses decimal percentage (e.g. "12.5" or "12,5") to basis points (e.g. 1250). */
    public static int parsePercentToBps(String raw) {
        if (raw == null) throw new NumberFormatException("empty");
        String norm = raw.trim().replace(",", ".").replace("%", "").replace(" ", "");
        if (norm.isEmpty()) return 0;
        BigDecimal bd = new BigDecimal(norm);
        if (bd.signum() < 0) throw new NumberFormatException("negative");
        BigDecimal scaled = bd.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
        if (scaled.compareTo(BigDecimal.valueOf(1_000_000L)) > 0) throw new NumberFormatException("overflow");
        return scaled.intValueExact();
    }

    /** Formate un entier en points de base (10000 = 100 %) vers une chaîne lisible ("25", "12.5", "3.25"). */
    public static String formatPercentFromBps(int bps) {
        int safe = Math.max(0, bps);
        if (safe % 100 == 0) return String.valueOf(safe / 100);
        if (safe % 10 == 0) return String.format(Locale.US, "%.1f", safe / 100.0);
        return String.format(Locale.US, "%.2f", safe / 100.0);
    }

    /**
     * Convertit une chaîne monétaire (espaces, virgule, point, suffixe "DT") en millimes entiers arrondis HALF_UP.
     */
    public static long parseSignedOrUnsignedBudgetMilli(String raw) {
        if (raw == null) throw new NumberFormatException("empty");
        String normalized = raw.trim()
                .replace("\u00a0", "")
                .replace("\u202f", "")
                .replace("\u2009", "")
                .replace("DT", "")
                .replace("dt", "")
                .replace(" ", "")
                .replace(',', '.');
        if (normalized.isEmpty()) throw new NumberFormatException("empty");
        BigDecimal bd = new BigDecimal(normalized);
        return bd.multiply(BigDecimal.valueOf(1_000L))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    /** Parses a budget total string and verifies only that it is strictly greater than zero (> 0). */
    public static long parsePositiveBudgetMilli(String raw) {
        long milli = parseSignedOrUnsignedBudgetMilli(raw);
        if (milli <= 0L) throw new NumberFormatException("must be > 0");
        return milli;
    }

    public static long parseNonNegativeBudgetMilli(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return 0L;
        long milli = parseSignedOrUnsignedBudgetMilli(trimmed);
        if (milli < 0L) throw new NumberFormatException("negative");
        return milli;
    }

    /** Inclusive start (00:00:00.000) of the equal-duration window immediately preceding [startMillis..endMillis]. */
    public static long previousWindowStartMillis(long startMillis, long endMillis) {
        int days = daysBetweenInclusive(startMillis, endMillis);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startMillis);
        Calendar prevStart = Calendar.getInstance();
        prevStart.clear();
        prevStart.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        prevStart.set(Calendar.MILLISECOND, 0);
        prevStart.add(Calendar.DAY_OF_MONTH, -days);
        return prevStart.getTimeInMillis();
    }

    /** Inclusive end (23:59:59.999) of the day immediately preceding startMillis. */
    public static long previousWindowEndMillis(long startMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startMillis);
        Calendar prevEnd = Calendar.getInstance();
        prevEnd.clear();
        prevEnd.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
        prevEnd.set(Calendar.MILLISECOND, 999);
        prevEnd.add(Calendar.DAY_OF_MONTH, -1);
        return prevEnd.getTimeInMillis();
    }

    /** Short localized capitalized month label without trailing period (e.g. "Juin", "Juil", "Nov"). */
    public static String shortMonthLabel(int monthZeroBased, Locale locale) {
        Locale safe = locale == null ? Locale.getDefault() : locale;
        String[] shorts = new DateFormatSymbols(safe).getShortMonths();
        if (monthZeroBased < 0 || monthZeroBased >= shorts.length) return "";
        String raw = shorts[monthZeroBased].replace(".", "").trim();
        if (raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase(safe) + raw.substring(1);
    }

    /** Compact numeric display (e.g. "0", "28", "50", or "12.5") matching RN pill chart legend rows. */
    public static String compactDinars(long milli) {
        if (milli <= 0L) return "0";
        long whole = milli / 1_000L;
        long rem = Math.abs(milli % 1_000L);
        if (rem == 0L) return String.valueOf(whole);
        BigDecimal bd = BigDecimal.valueOf(milli, 3).stripTrailingZeros();
        return bd.toPlainString();
    }
}
