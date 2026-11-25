package com.example.masrouf1.db;

/** Category identity joined to the selected system catalog row, if this is a system category. */
public final class CategoryPresentationRow {
    public long categoryId;
    public String name;
    public String colorHex;
    public String type;
    public String catalogKey;
    public String iconName;
    public int allocationPercent;
    public int recommendedPercent;
}
