package com.example.masrouf1;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.masrouf1.db.MasroufDao;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.example.masrouf1.db.MasroufEntities.SettingEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local and in-app Room notification dispatcher backed by unique deterministic event keys per recipient.
 */
public final class BudgetNotificationManager {
    public static final String CHANNEL_ID = "budget_channel";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final long IMPORTANT_FIXED_THRESHOLD_MILLI = 100_000L; // 100 DT
    private static final long IMPORTANT_MIN_RELATIVE_MILLI = 10_000L; // 10 DT floor for 25% check

    private BudgetNotificationManager() { }

    public static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.budget_this_month),
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.budget_hint));
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 220, 120, 220});
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.createNotificationChannel(channel);
    }

    /** Evaluates budget thresholds (80%, 100%, >100%), category overruns, important spends, and parent child-alerts synchronously in DB. */
    public static void dispatchSpendingAlertsSync(
            Context context,
            MasroufDao dao,
            long userId,
            long budgetId,
            long categoryId,
            long txAmountMilli,
            String itemLabel,
            boolean isPurchase,
            long recordId,
            long nowMillis) {
        if (context == null || dao == null || userId <= 0L || budgetId <= 0L) return;
        UserEntity actor = dao.findUser(userId);
        BudgetEntity budget = dao.findBudget(budgetId);
        if (actor == null || budget == null) return;

        Context app = context.getApplicationContext();
        long timestamp = nowMillis > 0L ? nowMillis : System.currentTimeMillis();
        SettingEntity settings = dao.systemSettings();
        int warnPct = settings == null ? 80 : Math.max(1, settings.orangeFrom);
        int dangerPct = settings == null ? 100 : Math.max(warnPct, settings.redFrom);

        long safeEnd = budget.periodEnd == Long.MAX_VALUE ? Long.MAX_VALUE : budget.periodEnd + 1L;
        long totalSpent = Math.max(budget.spentMilli, dao.totalSpendingBetween(userId, budget.periodStart, safeEnd));
        long limit = budget.monthlyLimitMilli;
        int pctUsed = limit > 0L ? Math.max(0, MoneyMath.percentOf(totalSpent, limit)) : 0;

        // 1. User / Child own budget threshold alerts (80%, 100% exact, >100% red overrun)
        if (limit > 0L) {
            if (totalSpent > limit) {
                long overMilli = MoneyMath.subtract(totalSpent, limit);
                String key = "budget:" + budget.id + ":" + budget.periodStart + ":cap" + limit + ":over";
                String title = app.getString(R.string.notif_budget_over_title);
                String msg = app.getString(
                        R.string.notif_budget_over_body,
                        CurrencyFormatter.formatMilli(totalSpent),
                        CurrencyFormatter.formatMilli(limit),
                        CurrencyFormatter.formatMilli(overMilli));
                persistSyncAndPost(app, dao, userId, key, "budget_over", title, msg, timestamp,
                        stableNotifId(key), NotificationsActivity.class);
            } else if (totalSpent == limit || pctUsed >= dangerPct) {
                String key = "budget:" + budget.id + ":" + budget.periodStart + ":cap" + limit + ":limit100";
                String title = app.getString(R.string.notif_budget_100_title);
                String msg = app.getString(
                        R.string.notif_budget_100_body,
                        CurrencyFormatter.formatMilli(limit),
                        CurrencyFormatter.formatMilli(totalSpent));
                persistSyncAndPost(app, dao, userId, key, "budget_100", title, msg, timestamp,
                        stableNotifId(key), NotificationsActivity.class);
            } else if (MoneyMath.atLeastPercent(totalSpent, limit, warnPct)) {
                long remaining = Math.max(0L, MoneyMath.subtract(limit, totalSpent));
                String key = "budget:" + budget.id + ":" + budget.periodStart + ":cap" + limit + ":caution" + warnPct;
                String title = app.getString(R.string.notif_budget_80_title, pctUsed);
                String msg = app.getString(
                        R.string.notif_budget_80_body,
                        CurrencyFormatter.formatMilli(totalSpent),
                        CurrencyFormatter.formatMilli(limit),
                        pctUsed,
                        CurrencyFormatter.formatMilli(remaining));
                persistSyncAndPost(app, dao, userId, key, "budget_caution", title, msg, timestamp,
                        stableNotifId(key), NotificationsActivity.class);
            }
        }

        // 2. Category envelope threshold alerts (80% / >100% / unallocated Enfant(s) overrun)
        if (categoryId > 0L) {
            BudgetLineEntity line = dao.budgetLineForCategory(budget.id, categoryId);
            long catSpent = Math.max(line == null ? 0L : line.spentMilli,
                    dao.categoryTotalSpendBetween(userId, categoryId, budget.periodStart, safeEnd));
            if (line != null && line.limitMilli > 0L) {
                String catDisplay = resolveCategoryName(app, dao, userId, categoryId);
                int catPct = Math.max(0, MoneyMath.percentOf(catSpent, line.limitMilli));
                if (catSpent > line.limitMilli) {
                    String key = "cat:" + budget.id + ":" + categoryId + ":cap" + line.limitMilli + ":over";
                    String title = app.getString(R.string.notif_cat_over_title, catDisplay);
                    String msg = app.getString(
                            R.string.notif_cat_over_body,
                            catDisplay,
                            CurrencyFormatter.formatMilli(catSpent),
                            CurrencyFormatter.formatMilli(line.limitMilli));
                    persistSyncAndPost(app, dao, userId, key, "category_over", title, msg, timestamp,
                            stableNotifId(key), NotificationsActivity.class);
                } else if (MoneyMath.atLeastPercent(catSpent, line.limitMilli, warnPct)) {
                    String key = "cat:" + budget.id + ":" + categoryId + ":cap" + line.limitMilli + ":caution" + warnPct;
                    String title = app.getString(R.string.notif_cat_80_title, catDisplay, catPct);
                    String msg = app.getString(
                            R.string.notif_cat_80_body,
                            catDisplay,
                            CurrencyFormatter.formatMilli(catSpent),
                            CurrencyFormatter.formatMilli(line.limitMilli));
                    persistSyncAndPost(app, dao, userId, key, "category_caution", title, msg, timestamp,
                            stableNotifId(key), NotificationsActivity.class);
                }
            } else if (catSpent > 0L && (AppRole.PARENT.value().equalsIgnoreCase(actor.role) || "admin".equalsIgnoreCase(actor.role))) {
                CategoryEntity childrenCat = dao.childrenCategory(userId);
                if (childrenCat != null && childrenCat.id == categoryId) {
                    String catDisplay = resolveCategoryName(app, dao, userId, categoryId);
                    String key = "cat:" + budget.id + ":" + categoryId + ":unalloc:over";
                    String title = app.getString(R.string.notif_cat_over_title, catDisplay);
                    String msg = app.getString(
                            R.string.notif_cat_unalloc_over_body,
                            catDisplay,
                            CurrencyFormatter.formatMilli(catSpent));
                    persistSyncAndPost(app, dao, userId, key, "category_over", title, msg, timestamp,
                            stableNotifId(key), NotificationsActivity.class);
                }
            }
        }

        // 3. Important single purchase or expense event
        boolean important = isImportantAmount(txAmountMilli, limit);
        if (important && recordId > 0L) {
            String safeLabel = (itemLabel == null || itemLabel.trim().isEmpty())
                    ? app.getString(isPurchase ? R.string.add_purchase_product : R.string.add_expense)
                    : itemLabel.trim();
            String key = "important:" + (isPurchase ? "purchase:" : "expense:") + recordId;
            String title = app.getString(isPurchase ? R.string.notif_important_purchase_title : R.string.notif_important_expense_title);
            String msg = app.getString(
                    isPurchase ? R.string.notif_important_purchase_body : R.string.notif_important_expense_body,
                    safeLabel,
                    CurrencyFormatter.formatMilli(txAmountMilli));
            persistSyncAndPost(app, dao, userId, key, isPurchase ? "important_purchase" : "important_expense",
                    title, msg, timestamp, stableNotifId(key), NotificationsActivity.class);
        }

        // 4. Child -> Parent family alerts (scoped strictly to profile.parentUserId)
        if (AppRole.CHILD.value().equalsIgnoreCase(actor.role)) {
            ChildProfileEntity profile = dao.childProfileForUser(userId);
            if (profile != null && profile.parentUserId > 0L) {
                String childName = (profile.nickname != null && !profile.nickname.trim().isEmpty())
                        ? profile.nickname.trim()
                        : (actor.name == null ? "" : actor.name.trim());
                long childLimit = Math.max(limit, profile.allocatedTotalMilli);
                int childPct = childLimit > 0L ? Math.max(0, MoneyMath.percentOf(totalSpent, childLimit)) : 0;
                if (childLimit > 0L) {
                    if (totalSpent > childLimit) {
                        long overMilli = MoneyMath.subtract(totalSpent, childLimit);
                        String key = "child-budget:" + userId + ":" + budget.id + ":" + budget.periodStart + ":cap" + childLimit + ":over";
                        String title = app.getString(R.string.notif_child_over_title, childName);
                        String msg = app.getString(
                                R.string.notif_child_over_body,
                                childName,
                                CurrencyFormatter.formatMilli(totalSpent),
                                CurrencyFormatter.formatMilli(childLimit),
                                CurrencyFormatter.formatMilli(overMilli));
                        persistSyncAndPost(app, dao, profile.parentUserId, key, "child_budget_over", title, msg,
                                timestamp, stableNotifId(key), NotificationsActivity.class);
                    } else if (totalSpent == childLimit || childPct >= dangerPct) {
                        String key = "child-budget:" + userId + ":" + budget.id + ":" + budget.periodStart + ":cap" + childLimit + ":limit100";
                        String title = app.getString(R.string.notif_child_100_title, childName);
                        String msg = app.getString(
                                R.string.notif_child_100_body,
                                childName,
                                CurrencyFormatter.formatMilli(childLimit));
                        persistSyncAndPost(app, dao, profile.parentUserId, key, "child_budget_100", title, msg,
                                timestamp, stableNotifId(key), NotificationsActivity.class);
                    } else if (MoneyMath.atLeastPercent(totalSpent, childLimit, warnPct)) {
                        String key = "child-budget:" + userId + ":" + budget.id + ":" + budget.periodStart + ":cap" + childLimit + ":caution" + warnPct;
                        String title = app.getString(R.string.notif_child_80_title, childName, childPct);
                        String msg = app.getString(
                                R.string.notif_child_80_body,
                                childName,
                                CurrencyFormatter.formatMilli(totalSpent),
                                CurrencyFormatter.formatMilli(childLimit));
                        persistSyncAndPost(app, dao, profile.parentUserId, key, "child_budget_caution", title, msg,
                                timestamp, stableNotifId(key), NotificationsActivity.class);
                    }
                }
                if (isImportantAmount(txAmountMilli, childLimit) && recordId > 0L) {
                    String key = "child-important:" + userId + ":" + (isPurchase ? "purchase:" : "expense:") + recordId;
                    String title = app.getString(R.string.notif_child_important_title, childName);
                    String msg = app.getString(
                            R.string.notif_child_important_body,
                            childName,
                            CurrencyFormatter.formatMilli(txAmountMilli));
                    persistSyncAndPost(app, dao, profile.parentUserId, key, "child_important_tx", title, msg,
                            timestamp, stableNotifId(key), NotificationsActivity.class);
                }
            }
        }
    }

    /** Persists a budget creation or update notification with a unique cap-scoped event key. */
    public static void dispatchBudgetChangeSync(Context context, MasroufDao dao, BudgetEntity budget, boolean created) {
        if (context == null || dao == null || budget == null || budget.userId <= 0L || budget.id <= 0L) return;
        Context app = context.getApplicationContext();
        String range = BudgetPeriodMath.formatRange(budget.periodStart, budget.periodEnd, Locale.getDefault());
        String key = (created ? "budget-created:" : "budget-updated:")
                + budget.id + ":" + budget.periodStart + ":" + budget.periodEnd + ":" + budget.monthlyLimitMilli;
        String title = app.getString(created ? R.string.notif_budget_created_title : R.string.notif_budget_updated_title);
        String msg = created
                ? app.getString(R.string.notif_budget_created_body, CurrencyFormatter.formatMilli(budget.monthlyLimitMilli), range)
                : app.getString(R.string.notif_budget_updated_body, range, CurrencyFormatter.formatMilli(budget.monthlyLimitMilli));
        persistSyncAndPost(app, dao, budget.userId, key, created ? "budget_created" : "budget_updated",
                title, msg, System.currentTimeMillis(), stableNotifId(key), NotificationsActivity.class);
    }

    /** Persists a notification when child category allocations are adjusted. */
    public static void dispatchChildEnvelopesUpdatedSync(Context context, MasroufDao dao, long childUserId, long budgetId, int signatureHash) {
        if (context == null || dao == null || childUserId <= 0L || budgetId <= 0L) return;
        BudgetEntity budget = dao.findBudget(budgetId);
        if (budget == null) return;
        Context app = context.getApplicationContext();
        String range = BudgetPeriodMath.formatRange(budget.periodStart, budget.periodEnd, Locale.getDefault());
        String key = "child-envelopes:" + childUserId + ":" + budgetId + ":" + signatureHash;
        String title = app.getString(R.string.notif_child_envelopes_updated_title);
        String msg = app.getString(R.string.notif_child_envelopes_updated_body, range);
        persistSyncAndPost(app, dao, childUserId, key, "budget_updated",
                title, msg, System.currentTimeMillis(), stableNotifId(key), NotificationsActivity.class);
    }

    public static void reportBudgetProgress(Context context, long spentMilli, long limitMilli, long userId, long budgetId, long periodStart) {
        if (limitMilli <= 0 || userId <= 0 || budgetId <= 0) return;
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                MasroufDao dao = MasroufDatabase.getInstance(app).masroufDao();
                dispatchSpendingAlertsSync(app, dao, userId, budgetId, 0L, 0L, "", false, 0L, System.currentTimeMillis());
            } catch (RuntimeException ignored) { }
        });
    }

    public static void budgetClosed(Context context, int notificationId, long userId, long budgetId, long periodStart) {
        String key = "budget-close:" + budgetId + ":" + periodStart;
        String message = context.getString(R.string.notification_budget_closed);
        persistAndPost(context, userId, key, "budget_closed", context.getString(R.string.app_name),
                message, notificationId, NotificationsActivity.class);
    }

    public static void pointsAwarded(Context context, int notificationId, String eventKey) {
        postKnownEvent(context, notificationId, "points:" + eventKey,
                context.getString(R.string.points_awarded_title),
                context.getString(R.string.points_awarded_message),
                RewardsActivity.class);
    }

    public static void allowanceReceived(Context context, int notificationId, long childUserId, long amountMilli, String eventKey) {
        if (childUserId <= 0 || amountMilli <= 0 || eventKey == null || eventKey.trim().isEmpty()) return;
        postKnownEvent(context, notificationId, eventKey,
                context.getString(R.string.notification_gift_received),
                context.getString(R.string.allowance_received, CurrencyFormatter.formatMilli(amountMilli)),
                NotificationsActivity.class);
    }

    public static void moneyRequestReceived(Context context, int notificationId, String eventKey, String message) {
        postKnownEvent(context, notificationId, eventKey,
                context.getString(R.string.money_request_notification_title),
                message, NotificationsActivity.class);
    }

    public static void moneyRequestResolved(Context context, int notificationId, String eventKey, String title, String message) {
        postKnownEvent(context, notificationId, eventKey, title, message, NotificationsActivity.class);
    }

    public static boolean isImportantAmount(long amountMilli, long budgetLimitMilli) {
        if (amountMilli <= 0L) return false;
        if (amountMilli >= IMPORTANT_FIXED_THRESHOLD_MILLI) return true;
        return budgetLimitMilli > 0L
                && amountMilli >= IMPORTANT_MIN_RELATIVE_MILLI
                && MoneyMath.atLeastPercent(amountMilli, budgetLimitMilli, 25);
    }

    private static String resolveCategoryName(Context app, MasroufDao dao, long userId, long categoryId) {
        List<CategoryPresentationRow> presentations = dao.selectedCategoryPresentations(userId);
        for (CategoryPresentationRow row : presentations) {
            if (row.categoryId == categoryId) {
                return row.catalogKey == null
                        ? (row.name == null ? app.getString(R.string.category_name) : row.name)
                        : app.getString(SystemCategoryCatalog.titleResource(row.catalogKey));
            }
        }
        CategoryEntity raw = dao.findCategoryForUser(categoryId, userId);
        return raw != null && raw.name != null ? raw.name : app.getString(R.string.category_name);
    }

    private static void persistAndPost(Context context, long userId, String eventKey, String kind,
                                       String title, String message, int id, Class<? extends Activity> destination) {
        Context app = context.getApplicationContext();
        IO.execute(() -> {
            try {
                MasroufDao dao = MasroufDatabase.getInstance(app).masroufDao();
                persistSyncAndPost(app, dao, userId, eventKey, kind, title, message, System.currentTimeMillis(), id, destination);
            } catch (RuntimeException ignored) { }
        });
    }

    private static void persistSyncAndPost(Context app, MasroufDao dao, long userId, String eventKey, String kind,
                                           String title, String message, long createdAt, int id,
                                           Class<? extends Activity> destination) {
        NotificationEntity row = new NotificationEntity();
        row.userId = userId;
        row.title = title;
        row.message = message;
        row.kind = kind;
        row.createdAt = createdAt;
        row.eventKey = eventKey;
        long inserted = dao.insertNotificationOnce(row);
        if (inserted > 0L) {
            postKnownEvent(app, id, eventKey, title, message, destination);
        }
    }

    private static int stableNotifId(String key) {
        return key == null ? 1 : (key.hashCode() & 0x7fffffff);
    }

    private static void postKnownEvent(Context context, int notificationId, String eventKey, String title,
                                       String message, Class<? extends Activity> destination) {
        if (eventKey == null || eventKey.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        String preferenceKey = "native:" + eventKey;
        android.content.SharedPreferences prefs = context.getSharedPreferences("masrouf_notification_delivery", Context.MODE_PRIVATE);
        if (prefs.getBoolean(preferenceKey, false)) return;
        createChannel(context);
        Intent intent = new Intent(context, destination).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_leaf)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification_leaf)
                        .setContentTitle(context.getString(R.string.app_name))
                        .setContentText(context.getString(R.string.notification_private_preview))
                        .build())
                .setAutoCancel(true)
                .setContentIntent(pending);
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification.build());
            prefs.edit().putBoolean(preferenceKey, true).apply();
        } catch (SecurityException ignored) { }
    }
}
