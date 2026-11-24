package com.example.masrouf1.db;

/** A purchase row for one product, already scoped to its authenticated owner. */
public final class PurchaseHistoryRow {
    public long purchaseId;
    public long occurredAt;
    public long totalMilli;
    public String storeName;
    public int quantityMilli;
    public String unit;
    public long unitPriceMilli;
    public String description;
}
