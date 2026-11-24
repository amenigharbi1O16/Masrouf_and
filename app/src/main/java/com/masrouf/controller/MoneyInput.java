package com.masrouf.controller;

import com.example.masrouf1.MoneyUtils;

final class MoneyInput {
    private MoneyInput() { }
    static long toMilli(String value) {
        return MoneyUtils.toMilli(value);
    }
}
