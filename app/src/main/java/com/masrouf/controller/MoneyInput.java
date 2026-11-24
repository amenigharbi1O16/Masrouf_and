package com.masrouf.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MoneyInput {
    private MoneyInput() { }
    static long toMilli(String value) {
        String normalized=value==null?"":value.trim().replace(',','.');
        if(normalized.isEmpty()) throw new NumberFormatException("empty");
        return new BigDecimal(normalized).setScale(3,RoundingMode.UNNECESSARY).movePointRight(3).longValueExact();
    }
}
