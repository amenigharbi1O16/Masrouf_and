package com.example.masrouf1.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.GoalEntity;
import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.example.masrouf1.db.MasroufEntities.RewardEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

@Database(entities = {UserEntity.class, ChildProfileEntity.class, CategoryEntity.class, ProductEntity.class,
        BudgetEntity.class, BudgetLineEntity.class, PurchaseEntity.class, PurchaseItemEntity.class,
        TransactionEntity.class, GoalEntity.class, RewardEntity.class, NotificationEntity.class,
        PriceHistoryEntity.class}, version = 7, exportSchema = false)
public abstract class MasroufDatabase extends RoomDatabase {
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN passwordHash TEXT");
            db.execSQL("ALTER TABLE users ADD COLUMN onboarded INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE users ADD COLUMN balanceMilli INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE categories ADD COLUMN type TEXT NOT NULL DEFAULT 'expense'");
            db.execSQL("ALTER TABLE categories ADD COLUMN allocationPercent INTEGER NOT NULL DEFAULT 0");
        }
    };
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN accountType TEXT NOT NULL DEFAULT 'personal'");
            db.execSQL("ALTER TABLE users ADD COLUMN parentPhone TEXT");
            db.execSQL("ALTER TABLE child_profiles ADD COLUMN allocatedTotalMilli INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE child_profiles ADD COLUMN parentPhone TEXT");
        }
    };
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE money_transactions ADD COLUMN categoryId INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_money_transactions_categoryId ON money_transactions(categoryId)");
        }
    };
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE products ADD COLUMN brand TEXT DEFAULT ''");
            db.execSQL("ALTER TABLE products ADD COLUMN unit TEXT DEFAULT 'pièce'");
            db.execSQL("ALTER TABLE products ADD COLUMN photoPath TEXT");
            db.execSQL("ALTER TABLE purchase_items ADD COLUMN unit TEXT DEFAULT 'pièce'");
        }
    };
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE money_transactions ADD COLUMN note TEXT");
        }
    };
    private static volatile MasroufDatabase instance;
    public abstract MasroufDao masroufDao();

    public static MasroufDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (MasroufDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), MasroufDatabase.class, "masrouf.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7).build();
                }
            }
        }
        return instance;
    }
}
