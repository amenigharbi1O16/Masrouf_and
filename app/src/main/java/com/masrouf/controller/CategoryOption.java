package com.masrouf.controller;

public final class CategoryOption {
    public final String key, name, colorHex, type;
    public final int allocationPercent;
    public CategoryOption(String key, String name, String colorHex, String type, int allocationPercent) {
        this.key=key; this.name=name; this.colorHex=colorHex; this.type=type; this.allocationPercent=allocationPercent;
    }
}
