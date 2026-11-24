package com.example.masrouf1;

import java.math.BigInteger;

/** Integer-millime comparison between the current and previous calendar month. */
public final class MonthlyComparison {
    public final long currentMilli,previousMilli,differenceMilli;
    public final Integer percentChange;
    public MonthlyComparison(long currentMilli,long previousMilli){
        this.currentMilli=currentMilli;this.previousMilli=previousMilli;this.differenceMilli=currentMilli-previousMilli;
        this.percentChange=previousMilli<=0?null:BigInteger.valueOf(differenceMilli).multiply(BigInteger.valueOf(100))
                .divide(BigInteger.valueOf(previousMilli)).intValue();
    }
}
