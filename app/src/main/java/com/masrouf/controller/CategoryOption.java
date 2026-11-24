package com.masrouf.controller;

public final class CategoryOption {
    public final String key, name, colorHex, type;
    public final int allocationPercent;
    public final long catalogId;
    public final String iconName;
    public final boolean protectedCategory;
    public CategoryOption(String key, String name, String colorHex, String type, int allocationPercent) {
        this(0,key,name,colorHex,type,allocationPercent,"",false);
    }
    public CategoryOption(long catalogId,String key,String name,String colorHex,String type,int allocationPercent,String iconName,boolean protectedCategory) {
        this.catalogId=catalogId;this.key=key;this.name=name;this.colorHex=colorHex;this.type=type;
        this.allocationPercent=allocationPercent;this.iconName=iconName;this.protectedCategory=protectedCategory;
    }
}
