package com.example.masrouf1.db;

import androidx.room.Entity;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/** Room's normalized offline data model; catalogue rows and per-user selections are separate. */
public final class MasroufEntities {
    private MasroufEntities() { }

    @Entity(tableName = "users", indices = {@Index(value = {"householdId"}), @Index(value = {"email"}, unique = true)})
    public static class UserEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long householdId;
        public String name;
        public String email;
        public String role;
        @ColumnInfo(defaultValue = "'personal'") public String accountType;
        public String parentPhone;
        public String passwordHash;
        @ColumnInfo(defaultValue = "0") public boolean onboarded;
        @ColumnInfo(defaultValue = "0") public boolean categoriesSelected;
        @ColumnInfo(defaultValue = "0") public long balanceMilli;
        public int points;
        public long createdAt;
    }

    @Entity(tableName = "child_profiles", foreignKeys = {
            @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "parentUserId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "childUserId", onDelete = ForeignKey.CASCADE)},
            indices = {@Index("parentUserId"), @Index(value = {"parentUserId", "allocationPeriodStart"}), @Index(value = {"childUserId"}, unique = true)})
    public static class ChildProfileEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long parentUserId;
        public long childUserId;
        public String nickname;
        @ColumnInfo(defaultValue = "0") public long allocatedTotalMilli;
        @ColumnInfo(defaultValue = "0") public long allocationPeriodStart;
        public String parentPhone;
        public long weeklyLimitMilli;
        public long createdAt;
    }

    @Entity(tableName = "categories", foreignKeys = @ForeignKey(entity = UserEntity.class,
            parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE), indices = @Index("userId"))
    public static class CategoryEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public String name;
        public String colorHex;
        public boolean isEssential;
        @ColumnInfo(defaultValue = "'expense'") public String type;
        @ColumnInfo(defaultValue = "0") public int allocationPercent;
    }

    @Entity(tableName = "products", foreignKeys = @ForeignKey(entity = CategoryEntity.class,
            parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.CASCADE), indices = @Index("categoryId"))
    public static class ProductEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long categoryId;
        public String name;
        @ColumnInfo(defaultValue = "''") public String brand;
        @ColumnInfo(defaultValue = "'pièce'") public String unit;
        public String photoPath;
        public String barcode;
        public long currentPriceMilli;
        public String currency;
        public long updatedAt;
    }

    @Entity(tableName = "budgets", foreignKeys = @ForeignKey(entity = UserEntity.class,
            parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE), indices = @Index("userId"))
    public static class BudgetEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public String name;
        public long monthlyLimitMilli;
        public long spentMilli;
        public long lastMonthSummaryMilli;
        public long periodStart;
        public long periodEnd;
        public String status;
        public int pointsAwarded;
    }

    @Entity(tableName = "budget_lines", foreignKeys = {
            @ForeignKey(entity = BudgetEntity.class, parentColumns = "id", childColumns = "budgetId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.CASCADE)},
            indices = {@Index("budgetId"), @Index("categoryId")})
    public static class BudgetLineEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long budgetId;
        public long categoryId;
        public long limitMilli;
        public long spentMilli;
        @ColumnInfo(defaultValue = "0") public int plannedPercentBps;
    }

    @Entity(tableName = "purchases", foreignKeys = @ForeignKey(entity = UserEntity.class,
            parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE), indices = @Index("userId"))
    public static class PurchaseEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public long occurredAt;
        public long totalMilli;
        public String storeName;
        public String receiptPath;
    }

    @Entity(tableName = "purchase_items", foreignKeys = {
            @ForeignKey(entity = PurchaseEntity.class, parentColumns = "id", childColumns = "purchaseId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = ProductEntity.class, parentColumns = "id", childColumns = "productId", onDelete = ForeignKey.CASCADE)},
            indices = {@Index("purchaseId"), @Index("productId")})
    public static class PurchaseItemEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long purchaseId;
        public long productId;
        public String description;
        public int quantityMilli;
        @ColumnInfo(defaultValue = "'pièce'") public String unit;
        public long unitPriceMilli;
    }

    @Entity(tableName = "money_transactions", foreignKeys = {
            @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = BudgetEntity.class, parentColumns = "id", childColumns = "budgetId", onDelete = ForeignKey.CASCADE)},
            indices = {@Index("userId"), @Index("budgetId"), @Index("occurredAt"), @Index("categoryId")})
    public static class TransactionEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public long budgetId;
        @ColumnInfo(defaultValue = "0") public long categoryId;
        public String kind;
        public String label;
        public String note;
        public long amountMilli;
        public long occurredAt;
    }

    @Entity(tableName = "goals", foreignKeys = @ForeignKey(entity = UserEntity.class,
            parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE), indices = @Index("userId"))
    public static class GoalEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public String title;
        public long targetMilli;
        public long savedMilli;
        public long dueAt;
    }

    @Entity(tableName = "rewards", foreignKeys = @ForeignKey(entity = ChildProfileEntity.class,
            parentColumns = "id", childColumns = "childProfileId", onDelete = ForeignKey.CASCADE), indices = @Index("childProfileId"))
    public static class RewardEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long childProfileId;
        public String title;
        public int pointsCost;
        public boolean redeemed;
        public long createdAt;
    }

    @Entity(tableName = "notifications", foreignKeys = @ForeignKey(entity = UserEntity.class,
            parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE), indices = {@Index("userId"), @Index("createdAt"), @Index(value = {"userId", "eventKey"}, unique = true)})
    public static class NotificationEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public String title;
        public String message;
        public String kind;
        public long createdAt;
        public boolean read;
        public String eventKey;
    }

    @Entity(tableName = "money_requests", foreignKeys = {
            @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "childUserId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "parentUserId", onDelete = ForeignKey.CASCADE)},
            indices = {@Index(value = {"parentUserId", "status", "createdAt"}), @Index(value = {"childUserId", "createdAt"})})
    public static class MoneyRequestEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long childUserId;
        public long parentUserId;
        public long amountMilli;
        public String reason;
        @NonNull @ColumnInfo(defaultValue = "'pending'") public String status = "pending";
        public long createdAt;
        public long updatedAt;
        public String responseNote;
    }

    @Entity(tableName = "points_log", foreignKeys = @ForeignKey(entity = UserEntity.class,
            parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
            indices = {@Index("userId"), @Index("createdAt"), @Index(value = {"userId", "eventKey"}, unique = true)})
    public static class PointsLogEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        @NonNull public String eventKey;
        public int points;
        @NonNull public String reason;
        public long createdAt;
    }

    @Entity(tableName = "price_history", foreignKeys = @ForeignKey(entity = ProductEntity.class,
            parentColumns = "id", childColumns = "productId", onDelete = ForeignKey.CASCADE), indices = {@Index("productId"), @Index("observedAt")})
    public static class PriceHistoryEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long productId;
        public long priceMilli;
        public long observedAt;
        public String storeName;
    }

    /** App-wide catalog; rows here are available options, never a user's selection. */
    @Entity(tableName = "system_categories", indices = @Index(value = {"key"}, unique = true))
    public static class SystemCategoryEntity {
        @PrimaryKey public long id;
        public String key;
        public String name;
        public String type;
        public String colorHex;
        public String iconName;
        public int recommendedPercent;
        @ColumnInfo(defaultValue = "0") public boolean protectedCategory;
    }

    /** Explicit user-to-category selection relation. CategoryEntity remains the ledger-facing row. */
    @Entity(tableName = "user_categories", foreignKeys = {
            @ForeignKey(entity = UserEntity.class, parentColumns = "id", childColumns = "userId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = CategoryEntity.class, parentColumns = "id", childColumns = "categoryId", onDelete = ForeignKey.CASCADE),
            @ForeignKey(entity = SystemCategoryEntity.class, parentColumns = "id", childColumns = "catalogId", onDelete = ForeignKey.SET_NULL)},
            indices = {@Index(value = {"userId", "categoryId"}, unique = true), @Index("categoryId"), @Index("catalogId")})
    public static class UserCategoryEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        public long userId;
        public long categoryId;
        public Long catalogId;
        @ColumnInfo(defaultValue = "1") public boolean active;
    }

    /** Single global configuration row (id=1), with Room defaults for fresh and migrated stores. */
    @Entity(tableName = "settings")
    public static class SettingEntity {
        @PrimaryKey(autoGenerate = true) public long id;
        @ColumnInfo(defaultValue = "100") public int pointsPerPeriod;
        @ColumnInfo(defaultValue = "10") public int pointsPerCategory;
        @ColumnInfo(defaultValue = "50") public int pointsMonthlyBonus;
        @ColumnInfo(defaultValue = "20") public int pointsStreakBonus;
        @ColumnInfo(defaultValue = "3") public int streakLength;
        @ColumnInfo(defaultValue = "0") public int thresholdBeginner;
        @ColumnInfo(defaultValue = "100") public int thresholdSaver;
        @ColumnInfo(defaultValue = "300") public int thresholdExpert;
        @ColumnInfo(defaultValue = "79") public int greenUntil;
        @ColumnInfo(defaultValue = "80") public int orangeFrom;
        @ColumnInfo(defaultValue = "100") public int redFrom;
    }
}
