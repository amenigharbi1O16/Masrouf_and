package com.example.masrouf1;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

/** Exact helpers for values stored as integer millimes. Floating point is reserved for drawing only. */
public final class MoneyMath {
    private static final BigInteger THOUSAND = BigInteger.valueOf(1_000L);
    private static final BigInteger HUNDRED = BigInteger.valueOf(100L);
    private MoneyMath() { }

    /** Converts a total and a quantity expressed in thousandths to millimes per unit, half-up. */
    public static long pricePerUnitMilli(long totalMilli, long quantityMilli) {
        if (totalMilli < 0 || quantityMilli <= 0) throw new ArithmeticException("Invalid price or quantity");
        BigInteger numerator = BigInteger.valueOf(totalMilli).multiply(THOUSAND);
        BigInteger denominator = BigInteger.valueOf(quantityMilli);
        BigInteger[] division = numerator.divideAndRemainder(denominator);
        if (division[1].shiftLeft(1).compareTo(denominator) >= 0) division[0] = division[0].add(BigInteger.ONE);
        return division[0].longValueExact();
    }

    /** True only when the increase is strictly greater than ten percent. */
    public static boolean increasedMoreThanTenPercent(long current, long previous) {
        return previous > 0 && BigInteger.valueOf(current).subtract(BigInteger.valueOf(previous))
                .multiply(BigInteger.TEN).compareTo(BigInteger.valueOf(previous)) > 0;
    }

    /** Signed percent change rounded to the nearest integer, with ties away from zero. */
    public static int percentChangeRounded(long current, long previous) {
        if (previous <= 0) return 0;
        BigInteger delta = BigInteger.valueOf(current).subtract(BigInteger.valueOf(previous));
        BigInteger value = divideHalfAwayFromZero(delta.multiply(HUNDRED), BigInteger.valueOf(previous));
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) return Integer.MAX_VALUE;
        if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) return Integer.MIN_VALUE;
        return value.intValue();
    }

    /** Amount as a rounded whole percent of a positive allocation, saturating at int bounds. */
    public static int percentOf(long amount, long allocation) {
        if (allocation <= 0) return amount > 0 ? Integer.MAX_VALUE : 0;
        BigInteger value = divideHalfAwayFromZero(BigInteger.valueOf(amount).multiply(HUNDRED), BigInteger.valueOf(allocation));
        if (value.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) return Integer.MAX_VALUE;
        if (value.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) return Integer.MIN_VALUE;
        return value.intValue();
    }

    public static boolean atLeastPercent(long amount, long allocation, int thresholdPercent) {
        if (allocation <= 0) return amount > 0;
        return BigInteger.valueOf(amount).multiply(HUNDRED)
                .compareTo(BigInteger.valueOf(allocation).multiply(BigInteger.valueOf(thresholdPercent))) >= 0;
    }

    public static long add(long firstMilli,long secondMilli){return Math.addExact(firstMilli,secondMilli);}
    public static long subtract(long firstMilli,long secondMilli){return Math.subtractExact(firstMilli,secondMilli);}
    public static long portion(long totalMilli,int percent){
        return BigInteger.valueOf(totalMilli).multiply(BigInteger.valueOf(percent)).divide(HUNDRED).longValueExact();
    }
    /** Total millimes represented by a unit price and a quantity in thousandths, rounded half-up. */
    public static long totalForQuantity(long unitPriceMilli,long quantityMilli){
        if(unitPriceMilli<0||quantityMilli<0)throw new ArithmeticException("Invalid price or quantity");
        return divideHalfAwayFromZero(BigInteger.valueOf(unitPriceMilli).multiply(BigInteger.valueOf(quantityMilli)),THOUSAND).longValueExact();
    }

    /** Integer largest-remainder allocation. The result sums exactly to total. */
    public static List<Long> allocate(long total, List<Long> weights) {
        ArrayList<Long> result = new ArrayList<>(weights.size());
        if (weights.isEmpty()) return result;
        BigInteger totalValue = BigInteger.valueOf(Math.max(0L, total));
        BigInteger weightSum = BigInteger.ZERO;
        for (Long weight : weights) weightSum = weightSum.add(BigInteger.valueOf(Math.max(0L, weight == null ? 0L : weight)));
        if (weightSum.signum() == 0) {
            for (int i = 0; i < weights.size(); i++) result.add(0L);
            return result;
        }
        BigInteger[] remainders = new BigInteger[weights.size()];
        long assigned = 0;
        for (int i = 0; i < weights.size(); i++) {
            BigInteger product = totalValue.multiply(BigInteger.valueOf(Math.max(0L, weights.get(i) == null ? 0L : weights.get(i))));
            BigInteger[] part = product.divideAndRemainder(weightSum);
            long value = part[0].longValueExact();
            result.add(value);
            remainders[i] = part[1];
            assigned = Math.addExact(assigned, value);
        }
        long remaining = totalValue.longValueExact() - assigned;
        while (remaining > 0) {
            int best = -1;
            for (int i = 0; i < remainders.length; i++)
                if (remainders[i] != null && (best < 0 || remainders[i].compareTo(remainders[best]) > 0)) best = i;
            if (best < 0) break;
            result.set(best, Math.addExact(result.get(best), 1L));
            remainders[best] = null;
            remaining--;
        }
        return result;
    }

    public static long threeQuarterAverage(long current, long average) {
        BigInteger numerator = BigInteger.valueOf(Math.max(0L, current)).multiply(BigInteger.valueOf(3L))
                .add(BigInteger.valueOf(Math.max(0L, average)));
        return numerator.divide(BigInteger.valueOf(4L)).longValueExact();
    }

    private static BigInteger divideHalfAwayFromZero(BigInteger numerator, BigInteger denominator) {
        BigInteger[] division = numerator.abs().divideAndRemainder(denominator.abs());
        if (division[1].shiftLeft(1).compareTo(denominator.abs()) >= 0) division[0] = division[0].add(BigInteger.ONE);
        return numerator.signum() * denominator.signum() < 0 ? division[0].negate() : division[0];
    }
}
