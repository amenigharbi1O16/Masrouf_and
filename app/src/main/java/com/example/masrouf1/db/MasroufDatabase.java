package com.example.masrouf1.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.UserCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.SettingEntity;
import com.example.masrouf1.db.MasroufEntities.MoneyRequestEntity;
import com.example.masrouf1.db.MasroufEntities.PointsLogEntity;

@Database(entities = {UserEntity.class, ChildProfileEntity.class, CategoryEntity.class, ProductEntity.class,
        BudgetEntity.class, BudgetLineEntity.class, PurchaseEntity.class, PurchaseItemEntity.class,
        TransactionEntity.class, GoalEntity.class, RewardEntity.class, NotificationEntity.class,
        PriceHistoryEntity.class, SystemCategoryEntity.class, UserCategoryEntity.class, SettingEntity.class,
        MoneyRequestEntity.class, PointsLogEntity.class}, version = 10, exportSchema = false)
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
    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE users ADD COLUMN categoriesSelected INTEGER NOT NULL DEFAULT 0");
            db.execSQL("CREATE TABLE IF NOT EXISTS system_categories (id INTEGER NOT NULL, `key` TEXT NOT NULL, name TEXT NOT NULL, type TEXT NOT NULL, colorHex TEXT NOT NULL, iconName TEXT NOT NULL, recommendedPercent INTEGER NOT NULL, protectedCategory INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_system_categories_key ON system_categories (`key`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS user_categories (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, categoryId INTEGER NOT NULL, catalogId INTEGER, active INTEGER NOT NULL DEFAULT 1, FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(catalogId) REFERENCES system_categories(id) ON UPDATE NO ACTION ON DELETE SET NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_categories_userId_categoryId ON user_categories(userId, categoryId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_user_categories_categoryId ON user_categories(categoryId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_user_categories_catalogId ON user_categories(catalogId)");
            db.execSQL("CREATE TABLE IF NOT EXISTS settings (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, pointsPerPeriod INTEGER NOT NULL DEFAULT 100, pointsPerCategory INTEGER NOT NULL DEFAULT 10, pointsMonthlyBonus INTEGER NOT NULL DEFAULT 50, pointsStreakBonus INTEGER NOT NULL DEFAULT 20, streakLength INTEGER NOT NULL DEFAULT 3, thresholdBeginner INTEGER NOT NULL DEFAULT 0, thresholdSaver INTEGER NOT NULL DEFAULT 100, thresholdExpert INTEGER NOT NULL DEFAULT 300, greenUntil INTEGER NOT NULL DEFAULT 79, orangeFrom INTEGER NOT NULL DEFAULT 80, redFrom INTEGER NOT NULL DEFAULT 100)");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS settings_singleton_insert BEFORE INSERT ON settings WHEN NEW.id <> 1 BEGIN SELECT RAISE(ABORT, 'settings is a singleton'); END");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS settings_singleton_update BEFORE UPDATE OF id ON settings WHEN NEW.id <> 1 BEGIN SELECT RAISE(ABORT, 'settings is a singleton'); END");
        }
    };
    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE notifications ADD COLUMN eventKey TEXT");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notifications_userId_eventKey ON notifications(userId, eventKey)");
            db.execSQL("CREATE TABLE IF NOT EXISTS money_requests (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, childUserId INTEGER NOT NULL, parentUserId INTEGER NOT NULL, amountMilli INTEGER NOT NULL, reason TEXT, status TEXT NOT NULL DEFAULT 'pending', createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, responseNote TEXT, FOREIGN KEY(childUserId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(parentUserId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_money_requests_parentUserId_status_createdAt ON money_requests(parentUserId, status, createdAt)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_money_requests_childUserId_createdAt ON money_requests(childUserId, createdAt)");
            db.execSQL("CREATE TABLE IF NOT EXISTS points_log (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, userId INTEGER NOT NULL, eventKey TEXT NOT NULL, points INTEGER NOT NULL, reason TEXT NOT NULL, createdAt INTEGER NOT NULL, FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_points_log_userId ON points_log(userId)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_points_log_createdAt ON points_log(createdAt)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_points_log_userId_eventKey ON points_log(userId, eventKey)");
        }
    };
    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE child_profiles ADD COLUMN allocationPeriodStart INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE child_profiles SET allocationPeriodStart = COALESCE((SELECT periodStart FROM budgets WHERE budgets.userId = child_profiles.parentUserId AND budgets.status = 'ACTIVE' ORDER BY periodStart DESC LIMIT 1), (SELECT periodStart FROM budgets WHERE budgets.userId = child_profiles.childUserId AND budgets.status = 'ACTIVE' ORDER BY periodStart DESC LIMIT 1), 0)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_child_profiles_parentUserId_allocationPeriodStart ON child_profiles(parentUserId, allocationPeriodStart)");
        }
    };
    private static final Callback REFERENCE_DATA_CALLBACK = new Callback() {
        @Override public void onOpen(SupportSQLiteDatabase db) {
            db.beginTransaction();
            try {
                for (SystemCategoryEntity row : SystemCategoryCatalog.defaults()) {
                    db.execSQL("INSERT OR IGNORE INTO system_categories (id, `key`, name, type, colorHex, iconName, recommendedPercent, protectedCategory) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                            new Object[]{row.id, row.key, row.name, row.type, row.colorHex, row.iconName, row.recommendedPercent, row.protectedCategory ? 1 : 0});
                }
                db.execSQL("INSERT OR IGNORE INTO settings (id, pointsPerPeriod, pointsPerCategory, pointsMonthlyBonus, pointsStreakBonus, streakLength, thresholdBeginner, thresholdSaver, thresholdExpert, greenUntil, orangeFrom, redFrom) VALUES (1, 100, 10, 50, 20, 3, 0, 100, 300, 79, 80, 100)");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS settings_singleton_insert BEFORE INSERT ON settings WHEN NEW.id <> 1 BEGIN SELECT RAISE(ABORT, 'settings is a singleton'); END");
                db.execSQL("CREATE TRIGGER IF NOT EXISTS settings_singleton_update BEFORE UPDATE OF id ON settings WHEN NEW.id <> 1 BEGIN SELECT RAISE(ABORT, 'settings is a singleton'); END");
                db.execSQL("INSERT OR IGNORE INTO user_categories (userId, categoryId, catalogId, active) " +
                        "SELECT c.userId, c.id, sc.id, 1 FROM categories c LEFT JOIN system_categories sc ON " +
                        "lower(trim(c.name)) = lower(trim(sc.name)) OR " +
                        "(lower(trim(c.name)) IN ('courses','food','alimentation') AND sc.`key`='food') OR " +
                        "(lower(trim(c.name)) IN ('maison','home','logement') AND sc.`key`='housing') OR " +
                        "(lower(trim(c.name)) IN ('santé','sante','health') AND sc.`key`='health') OR " +
                        "(lower(trim(c.name)) IN ('loisirs','loisirs & sorties','leisure') AND sc.`key`='leisure') OR " +
                        "(lower(trim(c.name)) IN ('école','ecole','education') AND sc.`key`='education') OR " +
                        "(lower(trim(c.name)) IN ('autres','autres dépenses','other') AND sc.`key`='other') WHERE c.userId > 0 AND lower(trim(c.name)) <> 'enfants'");
                db.execSQL("UPDATE users SET categoriesSelected=1 WHERE categoriesSelected=0 AND onboarded=1 AND EXISTS (SELECT 1 FROM categories c WHERE c.userId=users.id AND lower(trim(c.name)) <> 'enfants')");
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
    };
    private static volatile MasroufDatabase instance;
    public abstract MasroufDao masroufDao();

    public static MasroufDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (MasroufDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), MasroufDatabase.class, "masrouf.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                            .addCallback(REFERENCE_DATA_CALLBACK).build();
                }
            }
        }
        return instance;
    }

    /** Creates a verified database snapshot in the app's private no-backup directory. Run off the main thread. */
    public static File backupDatabase(Context context) throws IOException {
        MasroufDatabase room = getInstance(context.getApplicationContext());
        SupportSQLiteDatabase database = room.getOpenHelper().getWritableDatabase();
        if (database.inTransaction()) throw new IOException("Database backup cannot start inside a transaction");
        File folder = new File(context.getNoBackupFilesDir(), "database-backups");
        if (!folder.exists() && !folder.mkdirs()) throw new IOException("Cannot create private backup directory");
        File temporary = new File(folder, "masrouf.db.tmp");
        File target = new File(folder, "masrouf.db.bak");
        deleteIfExists(temporary); deleteIfExists(new File(temporary.getPath() + "-wal")); deleteIfExists(new File(temporary.getPath() + "-shm"));
        String escapedPath = temporary.getAbsolutePath().replace("'", "''");
        boolean vacuumSucceeded = false;
        try {
            database.execSQL("VACUUM INTO '" + escapedPath + "'");
            vacuumSucceeded = true;
        } catch (RuntimeException unsupportedOrBusy) {
            // Older SQLite builds lack VACUUM INTO. Checkpoint first, then copy the stable database/WAL snapshot under Room's write lock.
            deleteIfExists(temporary); deleteIfExists(new File(temporary.getPath() + "-wal")); deleteIfExists(new File(temporary.getPath() + "-shm"));
            Cursor checkpoint;
            try { checkpoint = database.query("PRAGMA wal_checkpoint(FULL)"); }
            catch (RuntimeException checkpointFailure) { throw new IOException("Could not checkpoint the database for backup", checkpointFailure); }
            int busy = 1;
            try { if (checkpoint.moveToFirst()) busy = checkpoint.getInt(0); }
            finally { checkpoint.close(); }
            if (busy != 0) throw new IOException("Database is busy; retry the backup", unsupportedOrBusy);
            try {
                room.runInTransaction(() -> {
                    try {
                        copyFile(context.getDatabasePath("masrouf.db"), temporary);
                        File wal = new File(context.getDatabasePath("masrouf.db").getPath() + "-wal");
                        if (wal.exists()) copyFile(wal, new File(temporary.getPath() + "-wal"));
                    } catch (IOException failure) { throw new IllegalStateException(failure); }
                });
            } catch (RuntimeException copyFailure) {
                deleteIfExists(temporary); deleteIfExists(new File(temporary.getPath() + "-wal"));
                throw new IOException("Could not create a consistent database backup", copyFailure);
            }
        }
        try {
            verifyBackup(temporary);
            File temporaryWal = new File(temporary.getPath() + "-wal");
            File targetWal = new File(target.getPath() + "-wal");
            deleteIfExists(target); deleteIfExists(targetWal); deleteIfExists(new File(target.getPath() + "-shm"));
            if (!temporary.renameTo(target)) copyFile(temporary, target);
            if (temporaryWal.exists() && !temporaryWal.renameTo(targetWal)) copyFile(temporaryWal, targetWal);
            deleteIfExists(temporary); deleteIfExists(temporaryWal); deleteIfExists(new File(temporary.getPath() + "-shm"));
            verifyBackup(target);
            return target;
        } catch (RuntimeException | IOException failure) {
            deleteIfExists(temporary); deleteIfExists(new File(temporary.getPath() + "-wal")); deleteIfExists(new File(temporary.getPath() + "-shm"));
            if (vacuumSucceeded) throw new IOException("Database backup verification failed", failure);
            throw failure instanceof IOException ? (IOException) failure : new IOException("Database backup verification failed", failure);
        }
    }

    private static void verifyBackup(File file) throws IOException {
        if (!file.isFile() || file.length() == 0L) throw new IOException("Database backup is empty");
        SQLiteDatabase check = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
        try (Cursor result = check.rawQuery("PRAGMA integrity_check", null)) {
            if (!result.moveToFirst() || !"ok".equalsIgnoreCase(result.getString(0))) throw new IOException("Database backup integrity check failed");
        } catch (SQLiteException failure) { throw new IOException("Database backup cannot be opened", failure); }
        finally { check.close(); }
    }

    private static void copyFile(File source, File destination) throws IOException {
        if (!source.isFile()) throw new IOException("Database file is missing");
        try (FileInputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192]; int count;
            while ((count = input.read(buffer)) >= 0) if (count > 0) output.write(buffer, 0, count);
            output.getFD().sync();
        }
    }

    private static void deleteIfExists(File file) throws IOException {
        if (file.exists() && !file.delete()) throw new IOException("Cannot replace previous backup file");
    }
}
