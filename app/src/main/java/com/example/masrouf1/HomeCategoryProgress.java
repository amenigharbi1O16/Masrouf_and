package com.example.masrouf1;

/** Room-backed values needed to render one selected category's real budget envelope. */
public final class HomeCategoryProgress {
    public final long categoryId;
    public final String name, colorHex, type, catalogKey, iconName;
    public final long allocatedMilli, spentMilli;
    public final int plannedPercent, consumedPercent;

    public HomeCategoryProgress(long categoryId, String name, String colorHex, String type,
                                String catalogKey, String iconName, long allocatedMilli,
                                long spentMilli, int plannedPercent, int consumedPercent) {
        this.categoryId = categoryId;
        this.name = name;
        this.colorHex = colorHex;
        this.type = type;
        this.catalogKey = catalogKey;
        this.iconName = iconName;
        this.allocatedMilli = allocatedMilli;
        this.spentMilli = spentMilli;
        this.plannedPercent = plannedPercent;
        this.consumedPercent = consumedPercent;
    }
}
