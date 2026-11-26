package com.example.masrouf1;

import android.content.Context;
import androidx.lifecycle.LiveData;

import com.example.masrouf1.db.CategorySpendRow;
import com.example.masrouf1.db.BudgetHistoryRow;
import com.example.masrouf1.db.PurchaseHistoryRow;
import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufDao;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.TransactionRow;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.UserCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.SettingEntity;
import com.example.masrouf1.db.MasroufEntities.GoalEntity;
import com.example.masrouf1.db.MasroufEntities.MoneyRequestEntity;
import com.example.masrouf1.db.MasroufEntities.PointsLogEntity;
import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MoneyRequestSummary;
import com.example.masrouf1.db.ProductCatalogRow;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.masrouf.controller.InputValidation;
import com.masrouf.controller.PasswordHasher;
import com.example.masrouf1.AppRole;

import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Repository boundary for dashboard screens; all records remain in the local
 * Room database.
 */
public final class MasroufRepository {
    private final MasroufDao dao;
    private final Context appContext;

    public MasroufRepository(Context context) {
        this(context, MasroufDatabase.getInstance(context.getApplicationContext()));
    }

    public MasroufRepository(Context context, MasroufDatabase database) {
        appContext = context.getApplicationContext();
        dao = database.masroufDao();
    }

    public UserEntity user(long userId) {
        return dao.findUser(userId);
    }

    public LiveData<UserEntity> observeUser(long userId) {
        return dao.observeUser(userId);
    }

    public List<CategoryEntity> categories(long userId) {
        List<CategoryEntity> selected = dao.selectedCategoriesForUser(userId);
        return selected.isEmpty() ? dao.categoriesForUser(userId) : selected;
    }

    public List<SystemCategoryEntity> onboardingCategories(long userId) {
        UserEntity owner = requireUserExists(userId);
        if ("child".equalsIgnoreCase(owner.role)) {
            if (dao.childProfileForUser(userId) == null)
                throw new SecurityException("child account has no family link");
            return dao.allowedSystemCategoriesForChild(userId);
        }
        return dao.systemCategories();
    }

    public List<Long> selectedCatalogIds(long userId) {
        requireUserExists(userId);
        return dao.selectedCatalogIds(userId);
    }

    public SettingEntity systemSettings() {
        return dao.systemSettings();
    }

    public LiveData<List<CategoryEntity>> observeCategories(long userId) {
        return dao.observeCategoriesForUser(userId);
    }

    public List<CategorySpendRow> categorySpending(long userId, long from, long to) {
        return dao.categorySpendingBetween(userId, from, to);
    }

    public MonthlyComparison monthlyComparison(long userId, long currentFrom, long currentTo, long previousFrom,
            long previousTo) {
        requireUserExists(userId);
        return new MonthlyComparison(dao.totalSpendingBetween(userId, currentFrom, currentTo),
                dao.totalSpendingBetween(userId, previousFrom, previousTo));
    }

    public LiveData<List<CategorySpendRow>> observeCategorySpending(long userId, long from, long to) {
        return dao.observeCategorySpendingBetween(userId, from, to);
    }

    public List<ProductEntity> products(long userId) {
        return dao.productsForUser(userId);
    }

    public List<ProductCatalogRow> productCatalog(long userId) {
        requireUserExists(userId);
        return dao.productCatalog(userId);
    }

    public LiveData<List<ProductEntity>> observeProducts(long userId) {
        return dao.observeProductsForUser(userId);
    }

    public List<PriceHistoryEntity> priceHistory(long userId, long productId) {
        return dao.priceHistory(userId, productId);
    }

    public LiveData<List<PriceHistoryEntity>> observePriceHistory(long userId, long productId) {
        return dao.observePriceHistory(userId, productId);
    }

    public List<TransactionRow> transactions(long userId, long from, long to) {
        return dao.transactionsBetween(userId, from, to);
    }

    public TransactionEntity transactionForUser(long userId, long transactionId) {
        requireUserExists(userId);
        TransactionEntity row = dao.findTransaction(transactionId);
        return row != null && row.userId == userId ? row : null;
    }

    public LiveData<List<TransactionRow>> observeTransactions(long userId, long from, long to) {
        return dao.observeTransactionsBetween(userId, from, to);
    }

    public LiveData<List<NotificationEntity>> observeNotifications(long userId) {
        return dao.observeNotificationsForUser(userId);
    }

    public void markNotificationRead(long notificationId, long userId) {
        dao.markNotificationRead(notificationId, userId);
    }

    public void markAllNotificationsRead(long userId) {
        requireUserExists(userId);
        dao.markAllNotificationsRead(userId);
    }

    public List<MoneyRequestEntity> moneyRequestsForParent(long parentId) {
        requireParent(parentId);
        return dao.moneyRequestsForParent(parentId);
    }

    public List<MoneyRequestSummary> moneyRequestSummariesForParent(long parentId) {
        requireParent(parentId);
        return dao.moneyRequestSummariesForParent(parentId);
    }

    public List<MoneyRequestEntity> moneyRequestsForChild(long childId) {
        UserEntity child = requireUserExists(childId);
        if (!AppRole.CHILD.value().equalsIgnoreCase(child.role))
            throw new SecurityException("child role required");
        return dao.moneyRequestsForChild(childId);
    }

    public long requestMoney(long childId, long amountMilli, String reason) {
        UserEntity child = requireUserExists(childId);
        if (!AppRole.CHILD.value().equalsIgnoreCase(child.role) || amountMilli <= 0)
            return -1L;
        ChildProfileEntity profile = dao.childProfileForUser(childId);
        if (profile == null)
            return -1L;
        MoneyRequestEntity request = new MoneyRequestEntity();
        request.childUserId = childId;
        request.parentUserId = profile.parentUserId;
        request.amountMilli = amountMilli;
        request.reason = reason == null ? "" : reason.trim();
        request.createdAt = System.currentTimeMillis();
        String title = appContext.getString(R.string.money_request_notification_title);
        String message = appContext.getString(R.string.money_request_notification_body, child.name,
                CurrencyFormatter.formatMilli(amountMilli));
        long requestId = dao.createMoneyRequest(request, title, message);
        if (requestId > 0)
            BudgetNotificationManager.moneyRequestReceived(appContext, (int) (requestId & 0x7fffffff),
                    "money-request:" + requestId, message);
        return requestId;
    }

    public int resolveMoneyRequest(long parentId, long requestId, boolean approve, String responseNote) {
        requireParent(parentId);
        MoneyRequestEntity request = dao.findMoneyRequest(requestId);
        if (request == null || request.parentUserId != parentId)
            return -1;
        long now = System.currentTimeMillis();
        String status = approve ? appContext.getString(R.string.money_request_accepted)
                : appContext.getString(R.string.money_request_rejected);
        String message = approve
                ? appContext.getString(R.string.money_request_accepted_body,
                        CurrencyFormatter.formatMilli(request.amountMilli))
                : appContext.getString(R.string.money_request_rejected_body);
        int result = dao.resolveMoneyRequest(requestId, parentId, approve, responseNote, now, status, message);
        if (result == 0)
            BudgetNotificationManager.moneyRequestResolved(appContext, (int) (requestId & 0x7fffffff),
                    "money-request-response:" + requestId + ":" + (approve ? "accepted" : "rejected"), status, message);
        return result;
    }

    public GoalEntity primaryGoal(long userId) {
        requireUserExists(userId);
        return dao.primaryGoal(userId);
    }

    public TransactionRow latestAllowanceReceived(long userId) {
        requireUserExists(userId);
        return dao.latestAllowanceReceived(userId);
    }

    public long totalAllowanceReceived(long userId) {
        requireUserExists(userId);
        return dao.totalAllowanceReceived(userId);
    }

    public List<PointsLogEntity> pointsHistory(long userId) {
        requireUserExists(userId);
        return dao.pointsHistory(userId);
    }

    public List<com.example.masrouf1.db.MasroufEntities.RewardEntity> rewardsForUser(long userId) {
        UserEntity user = requireUserExists(userId);
        if (!AppRole.CHILD.value().equalsIgnoreCase(user.role))
            return Collections.emptyList();
        ChildProfileEntity profile = dao.childProfileForUser(userId);
        return profile == null ? Collections.emptyList() : dao.rewardsForChild(profile.id);
    }

    public boolean redeemReward(long childUserId, long rewardId) {
        UserEntity child = requireUserExists(childUserId);
        if (!AppRole.CHILD.value().equalsIgnoreCase(child.role))
            throw new SecurityException("child role required");
        return dao.redeemReward(childUserId, rewardId, System.currentTimeMillis(),
                appContext.getString(R.string.reward_redeemed_title),
                appContext.getString(R.string.reward_redeemed_body));
    }

    public UserEntity authenticate(String email, String password) {
        String normalizedEmail = InputValidation.normalizeGmail(email);
        if (!InputValidation.isStrictGmail(normalizedEmail) || password == null || password.isEmpty())
            return null;
        UserEntity candidate = dao.findUserByEmail(normalizedEmail);
        return candidate != null && PasswordHasher.verify(password, candidate.passwordHash) ? candidate : null;
    }

    public OperationResult<Long> register(String name, String email, String password, String role, String accountType) {
        email = InputValidation.normalizeGmail(email);
        if (!InputValidation.isStrictGmail(email))
            return OperationResult.failure("gmail");
        if (!InputValidation.isValidPassword(password))
            return OperationResult.failure("password");
        if (name == null || name.trim().isEmpty())
            return OperationResult.failure("name");
        if (!AppRole.USER.value().equals(role) && !AppRole.PARENT.value().equals(role))
            return OperationResult.failure("role");
        if (dao.findUserByEmail(email) != null)
            return OperationResult.failure("duplicate");
        UserEntity user = new UserEntity();
        user.householdId = System.currentTimeMillis();
        user.name = name.trim();
        user.email = email;
        user.role = role;
        user.accountType = accountType;
        user.passwordHash = PasswordHasher.hash(password);
        user.onboarded = false;
        user.categoriesSelected = false;
        user.balanceMilli = 0;
        user.createdAt = System.currentTimeMillis();
        try {
            return OperationResult.success(dao.insertNewUser(user));
        } catch (RuntimeException duplicate) {
            return OperationResult.failure("duplicate");
        }
    }

    public boolean saveOnboarding(long userId, List<SystemCategoryEntity> categories) {
        return dao.finishOnboarding(userId, categories);
    }

    public boolean deleteExpenseTransaction(long transactionId, long userId) {
        requireUserExists(userId);
        TransactionEntity row = dao.findTransaction(transactionId);
        return row != null && row.userId == userId && dao.deleteExpenseTransaction(transactionId);
    }

    public OperationResult<Long> createChildAccount(UserEntity child, ChildProfileEntity profile,
            BudgetEntity initialBudget, List<CategoryEntity> categories, String password) {
        if (profile == null)
            return OperationResult.failure("parent");
        UserEntity parent = dao.findUser(profile.parentUserId);
        if (parent == null
                || !(AppRole.PARENT.value().equalsIgnoreCase(parent.role) || "admin".equalsIgnoreCase(parent.role)))
            return OperationResult.failure("parent");
        if (child == null || !AppRole.CHILD.value().equals(child.role))
            return OperationResult.failure("role");
        child.email = InputValidation.normalizeGmail(child.email);
        if (!InputValidation.isStrictGmail(child.email))
            return OperationResult.failure("gmail");
        if (!InputValidation.isValidPassword(password))
            return OperationResult.failure("password");
        if (dao.findUserByEmail(child.email) != null)
            return OperationResult.failure("duplicate");
        child.householdId = parent.householdId;
        child.passwordHash = PasswordHasher.hash(password);
        child.onboarded = false;
        child.categoriesSelected = false;
        try {
            long id = dao.createChildAccount(child, profile, initialBudget, categories);
            if (id > 0L && initialBudget != null && initialBudget.id > 0L) {
                BudgetNotificationManager.dispatchBudgetChangeSync(appContext, dao, initialBudget, true);
            }
            return id > 0 ? OperationResult.success(id) : OperationResult.failure("parent");
        } catch (RuntimeException failure) {
            String message = failure.getMessage();
            return OperationResult.failure(message == null ? "save" : message);
        }
    }

    public OperationResult<Boolean> resetChildPassword(long profileId, long parentId, String password) {
        try {
            requireParent(parentId);
            if (!InputValidation.isValidPassword(password))
                return OperationResult.failure("password");
            return dao.resetChildPassword(profileId, parentId, PasswordHasher.hash(password)) > 0
                    ? OperationResult.success(true)
                    : OperationResult.failure("parent");
        } catch (RuntimeException failure) {
            return OperationResult.failure("parent");
        }
    }

    public String childEmail(long parentId, long profileId) {
        requireParent(parentId);
        return dao.childEmailForProfile(profileId, parentId);
    }

    /**
     * Vérifie l'ancien mot de passe puis met à jour le hash en base si la
     * vérification réussit.
     * Retourne OperationResult.failure("wrong_password") si l'ancien MDP est
     * incorrect.
     * Utilisé par ChangePasswordActivity.
     */
    public OperationResult<Boolean> changePassword(long userId, String currentPassword, String newPassword) {
        UserEntity user = requireUserExists(userId);
        // Vérification de l'ancien mot de passe via hash sécurisé
        if (!com.masrouf.controller.PasswordHasher.verify(currentPassword, user.passwordHash)) {
            return OperationResult.failure("wrong_password");
        }
        if (!InputValidation.isValidPassword(newPassword)) {
            return OperationResult.failure("policy");
        }
        String newHash = com.masrouf.controller.PasswordHasher.hash(newPassword);
        try {
            return dao.updatePasswordHash(userId, newHash) > 0
                    ? OperationResult.success(true)
                    : OperationResult.failure("save");
        } catch (RuntimeException e) {
            return OperationResult.failure("save");
        }
    }

    /**
     * Retourne l'historique chronologique des achats d'un produit pour
     * l'utilisateur connecté.
     * Utilisé par ProductPriceHistoryActivity.
     */
    public List<PurchaseHistoryRow> purchaseHistoryForProduct(long productId, long userId) {
        requireUserExists(userId);
        return dao.productPurchaseHistory(userId, productId);
    }

    public OperationResult<Boolean> updateName(long userId, String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty())
            return OperationResult.failure("name");
        try {
            return dao.updateName(userId, trimmed) > 0
                    ? OperationResult.success(true)
                    : OperationResult.failure("save");
        } catch (RuntimeException failure) {
            return OperationResult.failure("save");
        }
    }

    public OperationResult<Boolean> updateEmail(long userId, String email) {
        email = InputValidation.normalizeGmail(email);
        if (!InputValidation.isStrictGmail(email))
            return OperationResult.failure("gmail");
        UserEntity existing = dao.findUserByEmail(email);
        if (existing != null && existing.id != userId)
            return OperationResult.failure("duplicate");
        try {
            return dao.updateEmail(userId, email) > 0 ? OperationResult.success(true) : OperationResult.failure("save");
        } catch (RuntimeException duplicate) {
            return OperationResult.failure("duplicate");
        }
    }

    public long createBudget(BudgetEntity budget, List<BudgetLineEntity> lines) {
        requireUserExists(budget.userId);
        long id = dao.insertBudgetWithEnvelopes(budget, lines);
        if (id > 0L) {
            budget.id = id;
            BudgetNotificationManager.dispatchBudgetChangeSync(appContext, dao, budget, true);
        }
        return id;
    }

    public boolean updateBudget(BudgetEntity budget, List<BudgetLineEntity> lines) {
        requireUserExists(budget.userId);
        boolean ok = dao.updateBudgetWithEnvelopes(budget, lines);
        if (ok) {
            BudgetNotificationManager.dispatchBudgetChangeSync(appContext, dao, budget, false);
        }
        return ok;
    }

    public boolean replaceChildEnvelopes(long budgetId, long childId, List<BudgetLineEntity> lines) {
        UserEntity child = requireUserExists(childId);
        if (!"child".equalsIgnoreCase(child.role))
            throw new SecurityException("child role required");
        boolean ok = dao.replaceChildBudgetEnvelopes(budgetId, childId, lines);
        if (ok) {
            int sig = 17;
            if (lines != null) {
                for (BudgetLineEntity l : lines) {
                    sig = 31 * sig + (int) l.categoryId;
                    sig = 31 * sig + (int) (l.limitMilli & 0xffffffffL);
                }
            }
            BudgetNotificationManager.dispatchChildEnvelopesUpdatedSync(appContext, dao, childId, budgetId, sig);
        }
        return ok;
    }

    public ProductFormData productForm(long userId) {
        return productForm(userId, 0L);
    }

    public ProductFormData productForm(long userId, long preferredBudgetId) {
        requireUserExists(userId);
        List<CategoryEntity> selected = dao.selectedCategoriesForUser(userId);
        if (selected.isEmpty())
            selected = dao.categoriesForUser(userId);
        List<CategoryPresentationRow> presentations = dao.selectedCategoryPresentations(userId);
        return new ProductFormData(resolveSelectedOrCurrentBudget(userId, preferredBudgetId), selected, presentations);
    }

    public BudgetEntity resolveBudgetForDateOrPreferred(long userId, long occurredAt, long preferredBudgetId) {
        requireUserExists(userId);
        if (occurredAt > 0L) {
            BudgetEntity byDate = dao.currentBudgetAt(userId, occurredAt);
            if (byDate != null) return byDate;
        }
        return resolveSelectedOrCurrentBudget(userId, preferredBudgetId);
    }

    public BudgetEntity activeBudget(long userId) {
        requireUserExists(userId);
        return currentOrActiveBudget(userId);
    }

    public BudgetEntity currentOrActiveBudget(long userId) {
        requireUserExists(userId);
        BudgetEntity current = dao.currentBudgetAt(userId, System.currentTimeMillis());
        if (current != null)
            return current;
        BudgetEntity active = dao.activeBudgetForUser(userId);
        if (active != null)
            return active;
        List<BudgetEntity> all = dao.budgetsForUser(userId);
        return all.isEmpty() ? null : all.get(all.size() - 1);
    }

    public BudgetEntity resolveSelectedOrCurrentBudget(long userId, long preferredBudgetId) {
        requireUserExists(userId);
        if (preferredBudgetId > 0L) {
            BudgetEntity chosen = dao.findBudget(preferredBudgetId);
            if (chosen != null && chosen.userId == userId)
                return chosen;
        }
        return currentOrActiveBudget(userId);
    }

    public List<BudgetEntity> budgetsForUser(long userId) {
        requireUserExists(userId);
        return dao.budgetsForUser(userId);
    }

    public BudgetEntity findOverlappingBudget(long userId, long startMillis, long endMillis, long excludeBudgetId) {
        requireUserExists(userId);
        return dao.findOverlappingBudget(userId, startMillis, endMillis, excludeBudgetId);
    }

    public ManageCategoriesData loadManageCategories(long userId) {
        UserEntity owner = requireUserExists(userId);
        boolean child = "child".equalsIgnoreCase(owner.role);
        List<SystemCategoryEntity> catalog = child ? onboardingCategories(userId) : dao.systemCategories();
        List<Long> selectedCatalogIds = dao.selectedCatalogIds(userId);
        List<UserCategoryEntity> links = dao.userCategoryLinks(userId);
        java.util.HashSet<Long> customIds = new java.util.HashSet<>();
        ArrayList<CategoryEntity> customCats = new ArrayList<>();
        ArrayList<Long> selectedCustomIds = new ArrayList<>();
        java.util.HashSet<Long> activeCatIds = new java.util.HashSet<>();
        for (UserCategoryEntity link : links) {
            if (link.active)
                activeCatIds.add(link.categoryId);
            if (link.catalogId == null)
                customIds.add(link.categoryId);
        }
        for (CategoryEntity c : dao.categoriesForUser(userId)) {
            if (customIds.contains(c.id)) {
                customCats.add(c);
                if (activeCatIds.contains(c.id))
                    selectedCustomIds.add(c.id);
            }
        }
        return new ManageCategoriesData(child, catalog, customCats, selectedCatalogIds, selectedCustomIds);
    }

    public boolean saveManagedCategories(long userId, List<Long> catalogIds, List<Long> customIds) {
        requireUserExists(userId);
        return dao.updateUserCategorySelections(userId, catalogIds, customIds, BudgetPeriodMath.todayStartMillis());
    }

    public boolean activateProposedCatalogCategory(long userId, long catalogId, int allocationPercent) {
        requireUserExists(userId);
        if (catalogId <= 0L) return false;
        ManageCategoriesData data = loadManageCategories(userId);
        java.util.LinkedHashSet<Long> catalogs = new java.util.LinkedHashSet<>(data.selectedCatalogIds);
        catalogs.add(catalogId);
        boolean ok = dao.updateUserCategorySelections(
                userId,
                new ArrayList<>(catalogs),
                data.selectedCustomCategoryIds,
                BudgetPeriodMath.todayStartMillis());
        if (!ok) return false;
        CategoryEntity linked = dao.selectedCategoryForCatalog(userId, catalogId);
        if (linked != null && allocationPercent >= 0) {
            int clampedPct = Math.max(0, Math.min(100, allocationPercent));
            linked.allocationPercent = clampedPct;
            dao.update(linked);
            BudgetEntity activeBudget = currentOrActiveBudget(userId);
            if (activeBudget != null && activeBudget.monthlyLimitMilli > 0L) {
                BudgetLineEntity line = dao.budgetLineForCategory(activeBudget.id, linked.id);
                long limit = MoneyMath.portion(activeBudget.monthlyLimitMilli, clampedPct);
                if (line == null) {
                    line = new BudgetLineEntity();
                    line.budgetId = activeBudget.id;
                    line.categoryId = linked.id;
                    line.limitMilli = limit;
                    line.spentMilli = 0L;
                    line.plannedPercentBps = clampedPct * 100;
                    dao.insert(line);
                } else {
                    line.limitMilli = limit;
                    line.plannedPercentBps = clampedPct * 100;
                    dao.updateBudgetLines(java.util.Collections.singletonList(line));
                }
            }
        }
        return true;
    }

    public boolean deleteCustomCategory(long userId, long categoryId) {
        requireUserExists(userId);
        return dao.deleteCustomCategory(userId, categoryId);
    }

    public CalendarYearData loadCalendarYear(long userId, int year, long selectedBudgetId) {
        requireUserExists(userId);
        long yearStart = BudgetPeriodMath.startOfMonthMillis(year, 0);
        long yearEnd = BudgetPeriodMath.endOfMonthMillis(year, 11);
        List<BudgetEntity> overlappingYear = dao.budgetsOverlappingForUser(userId, yearStart, yearEnd);
        List<TransactionRow> yearTx = dao.transactionsBetween(userId, yearStart, yearEnd);
        long[] monthlySpent = new long[12];
        long[] monthlyAllocated = new long[12];
        Map<Integer, List<CalendarYearData.BudgetPeriodSummary>> monthBudgets = new HashMap<>();
        Map<Integer, List<TransactionRow>> monthTransactions = new HashMap<>();
        for (int m = 0; m < 12; m++) {
            monthBudgets.put(m, new ArrayList<>());
            monthTransactions.put(m, new ArrayList<>());
        }
        Calendar txCal = Calendar.getInstance();
        for (TransactionRow tx : yearTx) {
            txCal.setTimeInMillis(tx.occurredAt);
            if (txCal.get(Calendar.YEAR) != year)
                continue;
            int m = txCal.get(Calendar.MONTH);
            if (m >= 0 && m < 12) {
                monthTransactions.get(m).add(tx);
                if ("expense".equalsIgnoreCase(tx.kind) || "donation".equalsIgnoreCase(tx.kind)
                        || "purchase".equalsIgnoreCase(tx.kind)) {
                    monthlySpent[m] = MoneyMath.add(monthlySpent[m], tx.amountMilli);
                }
            }
        }
        Map<Long, Long> budgetSpendCache = new HashMap<>();
        for (int m = 0; m < 12; m++) {
            long mStart = BudgetPeriodMath.startOfMonthMillis(year, m);
            long mEnd = BudgetPeriodMath.endOfMonthMillis(year, m);
            List<CalendarYearData.BudgetPeriodSummary> list = monthBudgets.get(m);
            for (BudgetEntity b : overlappingYear) {
                if (BudgetPeriodMath.periodsOverlap(b.periodStart, b.periodEnd, mStart, mEnd)) {
                    monthlyAllocated[m] = MoneyMath.add(monthlyAllocated[m], b.monthlyLimitMilli);
                    Long cached = budgetSpendCache.get(b.id);
                    if (cached == null) {
                        long total = dao.totalSpendingBetween(userId, b.periodStart, safeExclusiveEnd(b.periodEnd));
                        cached = total;
                        budgetSpendCache.put(b.id, cached);
                    }
                    list.add(new CalendarYearData.BudgetPeriodSummary(b, cached));
                }
            }
        }
        BudgetEntity resolved = resolveSelectedOrCurrentBudget(userId, selectedBudgetId);
        return new CalendarYearData(year, monthlySpent, monthlyAllocated, monthBudgets,
                resolved == null ? 0L : resolved.id, monthTransactions);
    }

    public BudgetEntity budget(long budgetId, long ownerId) {
        requireUserExists(ownerId);
        BudgetEntity value = dao.findBudget(budgetId);
        return value != null && value.userId == ownerId ? value : null;
    }

    public ProductDetailsData productDetails(long userId, long productId) {
        requireUserExists(userId);
        ProductEntity product = dao.findProductForUser(productId, userId);
        return product == null ? new ProductDetailsData(null, Collections.emptyList(), Collections.emptyList())
                : new ProductDetailsData(product, dao.priceHistory(userId, productId),
                        dao.productPurchaseHistory(userId, productId));
    }

    public Long latestUnitPrice(long userId, String name, String brand, String unit) {
        requireUserExists(userId);
        return dao.latestUnitPrice(userId, name, brand, unit);
    }

    public BudgetLineEntity envelope(long userId, long budgetId, long categoryId) {
        requireUserExists(userId);
        BudgetEntity budget = dao.findBudget(budgetId);
        return budget != null && budget.userId == userId ? dao.budgetLineForCategory(budgetId, categoryId) : null;
    }

    public long savePurchase(ProductEntity product, PurchaseEntity purchase, PurchaseItemEntity item, long userId,
            long budgetId) {
        requireUserExists(userId);
        if (purchase.userId != userId)
            throw new SecurityException("purchase owner");
        long purchaseId = dao.recordPurchase(product, purchase, item, budgetId);
        if (purchaseId > 0L) {
            BudgetNotificationManager.dispatchSpendingAlertsSync(
                    appContext,
                    dao,
                    userId,
                    budgetId,
                    product.categoryId,
                    purchase.totalMilli,
                    product.name,
                    true,
                    purchaseId,
                    purchase.occurredAt);
        }
        return purchaseId;
    }

    public long saveExpense(TransactionEntity expense, long userId) {
        requireUserExists(userId);
        if (expense.userId != userId)
            throw new SecurityException("expense owner");
        long txId = dao.recordExpense(expense);
        if (txId > 0L) {
            BudgetNotificationManager.dispatchSpendingAlertsSync(
                    appContext,
                    dao,
                    userId,
                    expense.budgetId,
                    expense.categoryId,
                    expense.amountMilli,
                    expense.label,
                    false,
                    txId,
                    expense.occurredAt);
        }
        return txId;
    }

    public List<CategoryEntity> categoriesForExpense(long userId) {
        requireUserExists(userId);
        List<CategoryEntity> selected = dao.selectedCategoriesForUser(userId);
        return selected.isEmpty() ? dao.categoriesForUser(userId) : selected;
    }

    public int allocatedPercent(long userId, String type) {
        requireUserExists(userId);
        return dao.allocatedPercent(userId, type);
    }

    public CategoryAllocationData categoryAllocation(long userId, String type) {
        requireUserExists(userId);
        return new CategoryAllocationData(dao.allocatedPercent(userId, type), currentOrActiveBudget(userId));
    }

    public boolean addCategory(CategoryEntity category, long userId) {
        requireUserExists(userId);
        if (category.userId != userId)
            throw new SecurityException("category owner");
        return dao.addCategoryWithAllocation(category);
    }

    public List<ChildSpendingSummary> familyChildren(long parentId) {
        requireParent(parentId);
        return dao.childSpendingSummaries(parentId);
    }

    public List<BudgetHistoryRow> childHistory(long parentId, long childId) {
        requireParent(parentId);
        return dao.childBudgetHistory(childId, parentId);
    }

    public GiftEnvelopeData giftEnvelope(long parentId) {
        requireParent(parentId);
        BudgetEntity budget = dao.activeBudgetForUser(parentId);
        if (budget == null)
            return new GiftEnvelopeData(null, 0, false);
        long categoryId = dao.ensureChildrenCategory(parentId);
        BudgetLineEntity line = dao.budgetLineForCategory(budget.id, categoryId);
        long spent = dao.categoryExpenseBetween(parentId, categoryId, budget.periodStart, budget.periodEnd);
        long remaining = line == null ? 0 : MoneyMath.subtract(line.limitMilli, spent);
        return new GiftEnvelopeData(budget, remaining, true);
    }

    public int giveMoney(long parentId, long profileId, long amountMilli, String note, long occurredAt) {
        requireParent(parentId);
        String title = appContext.getString(R.string.notification_gift_received);
        String message = appContext.getString(R.string.allowance_received, CurrencyFormatter.formatMilli(amountMilli));
        int result = dao.giveMoneyToChild(profileId, parentId, amountMilli, note, occurredAt, title, message, true);
        if (result == 0) {
            ChildProfileEntity profile = dao.childProfile(profileId);
            if (profile != null) {
                TransactionRow receipt = dao.latestAllowanceReceived(profile.childUserId);
                if (receipt != null)
                    BudgetNotificationManager.allowanceReceived(appContext, (int) (receipt.id & 0x7fffffff),
                            profile.childUserId, amountMilli, "allowance:" + receipt.id);
            }
        }
        return result;
    }

    public String parentPhone(long childUserId) {
        UserEntity child = requireUserExists(childUserId);
        if (!"child".equalsIgnoreCase(child.role))
            return "";
        String phone = dao.parentPhoneForChild(childUserId);
        return phone == null ? "" : phone;
    }

    public int unreadNotificationCount(long userId) {
        requireUserExists(userId);
        return dao.unreadNotificationCount(userId);
    }

    public List<BudgetLineEntity> budgetLines(long budgetId, long userId) {
        requireUserExists(userId);
        BudgetEntity budget = dao.findBudget(budgetId);
        return budget != null && budget.userId == userId ? dao.budgetLines(budgetId) : Collections.emptyList();
    }

    public DrawerBadgeData parentDrawerBadges(long parentId) {
        requireParent(parentId);
        return new DrawerBadgeData(dao.countChildren(parentId), dao.unreadNotificationCount(parentId));
    }

    public BudgetAllocationData loadBudgetAllocation(long userId, boolean childMode, boolean editMode, long budgetId,
            long periodStart) {
        UserEntity owner = requireUserExists(userId);
        boolean isChild = "child".equalsIgnoreCase(owner.role);
        if (childMode && !isChild)
            return new BudgetAllocationData(false, Collections.emptyList(), Collections.emptyList(), null, null,
                    Collections.emptyList(), Collections.emptyMap());
        List<CategoryPresentationRow> presentations = dao.selectedCategoryPresentations(userId);
        List<CategoryEntity> categories = dao.selectedCategoriesForUser(userId);
        if (categories.isEmpty())
            categories = dao.categoriesForUser(userId);
        BudgetEntity budget = null;
        ChildProfileEntity profile = null;
        if (childMode) {
            profile = dao.childProfileForUser(userId);
            budget = dao.findBudget(budgetId);
            if (profile == null || budget == null || budget.userId != userId)
                return new BudgetAllocationData(false, categories, presentations, null, profile,
                        Collections.emptyList(), Collections.emptyMap());
        } else if (editMode) {
            budget = dao.findBudget(budgetId);
            if (budget == null || budget.userId != userId)
                return new BudgetAllocationData(false, categories, presentations, null, null, Collections.emptyList(),
                        Collections.emptyMap());
        }
        List<BudgetLineEntity> lines = budget == null ? Collections.emptyList() : dao.budgetLines(budget.id);
        if (!lines.isEmpty()) {
            java.util.HashSet<Long> included = new java.util.HashSet<>();
            for (CategoryEntity c : categories)
                included.add(c.id);
            ArrayList<CategoryEntity> merged = new ArrayList<>(categories);
            for (BudgetLineEntity line : lines) {
                if (!included.contains(line.categoryId) && line.spentMilli > 0) {
                    CategoryEntity extra = dao.findCategoryForUser(line.categoryId, userId);
                    if (extra != null) {
                        merged.add(extra);
                        included.add(extra.id);
                    }
                }
            }
            categories = merged;
        }
        Map<Long, Long> history = Collections.emptyMap();
        if (lines.isEmpty() && periodStart > 0) {
            Calendar prior = Calendar.getInstance();
            prior.setTimeInMillis(periodStart);
            prior.add(Calendar.MONTH, -1);
            List<CategorySpendRow> spent = dao.categorySpendingBetween(userId, prior.getTimeInMillis(), periodStart);
            Map<Long, Long> amounts = new java.util.HashMap<>();
            for (CategorySpendRow row : spent)
                amounts.put(row.categoryId, row.amountMilli);
            history = amounts;
        }
        return new BudgetAllocationData(true, categories, presentations, budget, profile, lines, history);
    }

    public FamilyAnalyticsData familyAnalytics(long parentId) {
        requireParent(parentId);
        List<ChildSpendingSummary> children = dao.childSpendingSummaries(parentId);
        TreeMap<Long, FamilyMonthSpending> months = new TreeMap<>();
        long global = 0, parentSpent = 0;
        BudgetEntity budget = currentOrActiveBudget(parentId);
        if (budget != null) {
            global = budget.monthlyLimitMilli;
            parentSpent = dao.personalSpendingBetween(parentId, budget.periodStart, budget.periodEnd);
        }
        for (ChildSpendingSummary child : children)
            for (BudgetHistoryRow row : dao.childBudgetHistory(child.childUserId, parentId)) {
                Calendar month = Calendar.getInstance();
                month.setTimeInMillis(row.periodStart);
                month.set(Calendar.DAY_OF_MONTH, 1);
                month.set(Calendar.HOUR_OF_DAY, 0);
                month.set(Calendar.MINUTE, 0);
                month.set(Calendar.SECOND, 0);
                month.set(Calendar.MILLISECOND, 0);
                long key = month.getTimeInMillis();
                FamilyMonthSpending point = months.get(key);
                if (point == null) {
                    point = new FamilyMonthSpending(key);
                    months.put(key, point);
                }
                point.spentMilli = MoneyMath.add(point.spentMilli, row.spentMilli);
                point.allocatedMilli = MoneyMath.add(point.allocatedMilli, row.allocatedMilli);
            }
        return new FamilyAnalyticsData(children, new ArrayList<>(months.values()), global,
                dao.totalChildAllocations(parentId), parentSpent);
    }

    private UserEntity requireUserExists(long userId) {
        UserEntity user = dao.findUser(userId);
        if (user == null)
            throw new SecurityException("unknown user");
        return user;
    }

    private void requireParent(long userId) {
        UserEntity parent = dao.findUser(userId);
        if (parent == null || !("parent".equalsIgnoreCase(parent.role) || "admin".equalsIgnoreCase(parent.role)))
            throw new SecurityException("parent role required");
    }

    public HomeDashboardData loadHome(long userId, boolean parentMode, boolean childMode) {
        return loadHome(userId, parentMode, childMode, 0L);
    }

    public HomeDashboardData loadHome(long userId, boolean parentMode, boolean childMode, long preferredBudgetId) {
        UserEntity user = dao.findUser(userId);
        BudgetEntity budget = resolveSelectedOrCurrentBudget(userId, preferredBudgetId);
        List<CategoryEntity> categories = dao.selectedCategoriesForUser(userId);
        if (categories.isEmpty())
            categories = dao.categoriesForUser(userId);
        List<TransactionRow> recent = dao.recentSpendingTransactions(userId);
        ChildProfileEntity profile = childMode ? dao.childProfileForUser(userId) : null;
        List<ChildSpendingSummary> children = parentMode ? dao.childSpendingSummaries(userId) : Collections.emptyList();
        long allocated = parentMode ? dao.totalChildAllocations(userId) : 0;
        long parentSpent = parentMode && budget != null
                ? dao.personalSpendingBetween(userId, budget.periodStart, budget.periodEnd)
                : 0;
        long periodStart = budget == null ? monthStartMillis() : budget.periodStart;
        long periodEndExclusive = budget == null ? nextMonthStartMillis(periodStart)
                : safeExclusiveEnd(budget.periodEnd);
        GoalEntity goal = childMode ? dao.primaryGoal(userId) : null;
        TransactionRow latestAllowance = childMode ? dao.latestAllowanceReceived(userId) : null;
        long totalAllowance = childMode ? dao.totalAllowanceReceived(userId) : 0L;
        int transactionCount = parentMode ? dao.familyTransactionCount(userId, periodStart, periodEndExclusive)
                : dao.transactionCount(userId, periodStart, periodEndExclusive);
        int pendingRequests = parentMode ? dao.pendingMoneyRequestCount(userId) : 0;
        Map<Long, BudgetLineEntity> envelopes = new HashMap<>();
        if (budget != null)
            for (BudgetLineEntity line : dao.budgetLines(budget.id))
                envelopes.put(line.categoryId, line);
        Map<Long, Long> spending = new HashMap<>();
        for (CategorySpendRow row : dao.categorySpendingBetween(userId, periodStart, periodEndExclusive))
            spending.put(row.categoryId, row.amountMilli);
        long percentBase = budget == null ? 0 : budget.monthlyLimitMilli;
        ArrayList<HomeCategoryProgress> progress = new ArrayList<>();
        List<CategoryPresentationRow> presentations = dao.selectedCategoryPresentations(userId);
        if (!presentations.isEmpty()) {
            for (CategoryPresentationRow row : presentations) {
                BudgetLineEntity line = envelopes.get(row.categoryId);
                long envelope = line == null ? 0L : line.limitMilli;
                long spent = spending.containsKey(row.categoryId) ? spending.get(row.categoryId) : 0L;
                int plannedBps = line != null && line.plannedPercentBps > 0
                        ? line.plannedPercentBps
                        : (percentBase <= 0 ? 0 : BudgetPeriodMath.percentBpsFromAmount(percentBase, envelope));
                int planned = (plannedBps + 50) / 100;
                int consumed = envelope <= 0 ? (spent > 0 ? 100 : 0) : Math.max(0, MoneyMath.percentOf(spent, envelope));
                progress.add(new HomeCategoryProgress(row.categoryId, row.name, row.colorHex, row.type, row.catalogKey,
                        row.iconName, envelope, spent, planned, plannedBps, consumed));
            }
        } else {
            for (CategoryEntity cat : categories) {
                BudgetLineEntity line = envelopes.get(cat.id);
                long envelope = line == null ? 0L : line.limitMilli;
                long spent = spending.containsKey(cat.id) ? spending.get(cat.id) : 0L;
                int plannedBps = line != null && line.plannedPercentBps > 0
                        ? line.plannedPercentBps
                        : (percentBase <= 0 ? 0 : BudgetPeriodMath.percentBpsFromAmount(percentBase, envelope));
                int planned = (plannedBps + 50) / 100;
                int consumed = envelope <= 0 ? (spent > 0 ? 100 : 0) : Math.max(0, MoneyMath.percentOf(spent, envelope));
                progress.add(new HomeCategoryProgress(cat.id, cat.name, cat.colorHex, cat.type, null,
                        "ellipse-outline", envelope, spent, planned, plannedBps, consumed));
            }
        }
        SettingEntity settings = dao.systemSettings();
        return new HomeDashboardData(user, budget, categories, recent, profile, children, allocated, parentSpent,
                progress, settings, goal, latestAllowance, totalAllowance, transactionCount, pendingRequests);
    }

    public SettingsData loadSettingsData(long userId) {
        UserEntity owner = requireUserExists(userId);
        List<CategoryPresentationRow> presentations = dao.selectedCategoryPresentations(userId);
        List<CategoryEntity> rawCats = dao.selectedCategoriesForUser(userId);
        if (rawCats.isEmpty())
            rawCats = dao.categoriesForUser(userId);
        BudgetEntity budget = currentOrActiveBudget(userId);
        boolean isChild = AppRole.CHILD.value().equalsIgnoreCase(owner.role);
        ChildProfileEntity childProfile = isChild ? dao.childProfileForUser(userId) : null;
        long totalAllowance = isChild ? dao.totalAllowanceReceived(userId) : 0L;
        String parentPhoneStr = isChild ? parentPhone(userId) : "";
        return new SettingsData(owner, presentations, rawCats, budget, childProfile, totalAllowance, parentPhoneStr);
    }

    /**
     * Builds the full role-specific analytics snapshot strictly from persisted Room records:
     * - USER: personal budget synthesis, 7 insights, equal-window comparison, 6-month pill chart, sorted categories, purchase/expense breakdown
     * - CHILD: strictly isolated own budget, expenses, purchases & 6-month consumption trend
     * - PARENT: household monthly gauge, category breakdown with overrun badges, 6-month parent+household chart, and privacy-safe child stats
     */
    public AnalyticsSummaryData loadAnalytics(long userId, long preferredBudgetId) {
        UserEntity user = requireUserExists(userId);
        AppRole role = AppRole.from(user.role);
        BudgetEntity budget = resolveSelectedOrCurrentBudget(userId, preferredBudgetId);
        long nowMillis = System.currentTimeMillis();

        Calendar refCal = Calendar.getInstance();
        if (budget != null) {
            refCal.setTimeInMillis(budget.periodStart);
        }
        int targetYear = refCal.get(Calendar.YEAR);
        int targetMonthZero = refCal.get(Calendar.MONTH);
        Locale locale = Locale.getDefault();

        // Determine active analysis window
        long periodStart = budget != null ? budget.periodStart
                : BudgetPeriodMath.startOfMonthMillis(targetYear, targetMonthZero);
        long periodEnd = budget != null ? budget.periodEnd
                : BudgetPeriodMath.endOfMonthMillis(targetYear, targetMonthZero);
        long periodEndExcl = safeExclusiveEnd(periodEnd);

        // Equal-duration previous window
        long prevStart = BudgetPeriodMath.previousWindowStartMillis(periodStart, periodEnd);
        long prevEnd = BudgetPeriodMath.previousWindowEndMillis(periodStart);
        long prevEndExcl = safeExclusiveEnd(prevEnd);

        List<TransactionEntity> curTxns = dao.spendingTransactionsBetween(userId, periodStart, periodEndExcl);
        List<TransactionRow> curRows = new ArrayList<>(curTxns.size());
        long curSpent = 0L, purchasesTotal = 0L, expensesTotal = 0L;
        for (TransactionEntity tx : curTxns) {
            curSpent = MoneyMath.add(curSpent, tx.amountMilli);
            if ("purchase".equalsIgnoreCase(tx.kind)) {
                purchasesTotal = MoneyMath.add(purchasesTotal, tx.amountMilli);
            } else {
                expensesTotal = MoneyMath.add(expensesTotal, tx.amountMilli);
            }
            TransactionRow tr = new TransactionRow();
            tr.id = tx.id;
            tr.title = tx.label;
            tr.amountMilli = tx.amountMilli;
            tr.occurredAt = tx.occurredAt;
            tr.kind = tx.kind;
            curRows.add(tr);
        }
        long prevSpent = dao.totalSpendingBetween(userId, prevStart, prevEndExcl);

        // Pace calculations matching RN analyses.tsx lines 167-196
        int totalDays = BudgetPeriodMath.daysBetweenInclusive(periodStart, periodEnd);
        long todayStart = BudgetPeriodMath.todayStartMillis();
        long startDay = BudgetPeriodMath.startOfDayMillis(refCal.get(Calendar.YEAR), refCal.get(Calendar.MONTH), refCal.get(Calendar.DAY_OF_MONTH));
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(periodEnd);
        long endDay = BudgetPeriodMath.startOfDayMillis(endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH));

        int daysElapsed;
        int daysRemaining;
        if (todayStart < startDay) {
            daysElapsed = 0;
            daysRemaining = totalDays;
        } else if (todayStart > endDay) {
            daysElapsed = totalDays;
            daysRemaining = 0;
        } else {
            daysElapsed = BudgetPeriodMath.daysBetweenInclusive(startDay, todayStart);
            daysRemaining = Math.max(0, (int) ((endDay - todayStart) / 86_400_000L));
        }
        long avgDailyMilli = daysElapsed > 0 ? Math.round(curSpent / (double) daysElapsed) : 0L;
        long projectedTotalMilli = MoneyMath.add(curSpent, Math.round(avgDailyMilli * (double) daysRemaining));
        long limitMilli = budget == null ? 0L : budget.monthlyLimitMilli;
        boolean paceSustainable = limitMilli <= 0L || projectedTotalMilli <= limitMilli;
        long projectedDiffMilli = Math.abs(projectedTotalMilli - limitMilli);

        // Category breakdown
        Map<Long, String> catNameMap = new HashMap<>();
        Map<Long, String> catKeyMap = new HashMap<>();
        Map<Long, String> catIconMap = new HashMap<>();
        Map<Long, String> catColorMap = new HashMap<>();
        for (CategoryEntity c : dao.categoriesForUser(userId)) {
            catNameMap.put(c.id, c.name);
            catColorMap.put(c.id, c.colorHex);
        }
        for (CategoryPresentationRow p : dao.selectedCategoryPresentations(userId)) {
            catNameMap.put(p.categoryId, p.name);
            catKeyMap.put(p.categoryId, p.catalogKey);
            catIconMap.put(p.categoryId, p.iconName);
            catColorMap.put(p.categoryId, p.colorHex);
        }
        Map<Long, BudgetLineEntity> envelopes = new HashMap<>();
        if (budget != null) {
            for (BudgetLineEntity line : dao.budgetLines(budget.id)) {
                envelopes.put(line.categoryId, line);
            }
        }
        Map<Long, Long> spentByCat = new HashMap<>();
        for (CategorySpendRow row : dao.categorySpendingBetween(userId, periodStart, periodEndExcl)) {
            spentByCat.put(row.categoryId, MoneyMath.add(
                    spentByCat.containsKey(row.categoryId) ? spentByCat.get(row.categoryId) : 0L,
                    row.amountMilli));
        }
        java.util.LinkedHashSet<Long> catIds = new java.util.LinkedHashSet<>(spentByCat.keySet());
        for (BudgetLineEntity line : envelopes.values()) {
            if (line.limitMilli > 0L) catIds.add(line.categoryId);
        }

        ArrayList<AnalyticsSummaryData.CategoryStat> categoryStats = new ArrayList<>();
        for (Long cid : catIds) {
            if (cid == null || cid <= 0L) continue;
            long spent = spentByCat.containsKey(cid) ? spentByCat.get(cid) : 0L;
            BudgetLineEntity env = envelopes.get(cid);
            long allocated = env == null ? 0L : env.limitMilli;
            if (spent <= 0L && allocated <= 0L) continue;
            boolean hasBudget = allocated > 0L;
            int plannedPct = (limitMilli > 0L && allocated > 0L)
                    ? (int) Math.round((allocated * 100.0) / limitMilli)
                    : 0;
            double consumptionPct = hasBudget ? (spent * 100.0) / allocated : 0.0;
            double shareOfTotal = curSpent > 0L ? (spent * 100.0) / curSpent : 0.0;
            String rawName = catNameMap.containsKey(cid) ? catNameMap.get(cid) : "";
            String resolvedName = catKeyMap.get(cid) != null
                    ? appContext.getString(SystemCategoryCatalog.titleResource(catKeyMap.get(cid)))
                    : rawName;
            categoryStats.add(new AnalyticsSummaryData.CategoryStat(
                    cid, resolvedName, catKeyMap.get(cid), catIconMap.get(cid), catColorMap.get(cid),
                    allocated, spent, hasBudget, plannedPct, consumptionPct, shareOfTotal));
        }

        if (role == AppRole.PARENT) {
            // Parent view sorts by spent descending, then planned descending (matching budget-analyses.tsx:186-189)
            categoryStats.sort((a, b) -> {
                if (b.spentMilli != a.spentMilli) return Long.compare(b.spentMilli, a.spentMilli);
                return Long.compare(b.allocatedMilli, a.allocatedMilli);
            });
        } else {
            // Personal & Child views sort budgeted ascending by consumptionPct, then unbudgeted ascending by shareOfTotal (matching analyses.tsx:288-299)
            categoryStats.sort((a, b) -> {
                if (a.hasBudget && b.hasBudget) return Double.compare(a.consumptionPct, b.consumptionPct);
                if (!a.hasBudget && !b.hasBudget) return Double.compare(a.shareOfTotal, b.shareOfTotal);
                return a.hasBudget ? -1 : 1;
            });
        }

        // Attention category detection (analyses.tsx:303-327)
        AnalyticsSummaryData.CategoryStat attentionCategory = null;
        if (curSpent > 0L && !categoryStats.isEmpty()) {
            for (AnalyticsSummaryData.CategoryStat stat : categoryStats) {
                if (stat.hasBudget && stat.consumptionPct > 100.0) {
                    if (attentionCategory == null || stat.consumptionPct > attentionCategory.consumptionPct) {
                        attentionCategory = stat;
                    }
                }
            }
            if (attentionCategory == null) {
                for (AnalyticsSummaryData.CategoryStat stat : categoryStats) {
                    if (stat.hasBudget && stat.consumptionPct >= 80.0) {
                        if (attentionCategory == null || stat.consumptionPct > attentionCategory.consumptionPct) {
                            attentionCategory = stat;
                        }
                    }
                }
            }
            if (attentionCategory == null) {
                for (AnalyticsSummaryData.CategoryStat stat : categoryStats) {
                    if (!stat.hasBudget && stat.shareOfTotal >= 35.0) {
                        if (attentionCategory == null || stat.shareOfTotal > attentionCategory.shareOfTotal) {
                            attentionCategory = stat;
                        }
                    }
                }
            }
        }

        // 6-month history for the active user role
        List<BudgetEntity> allUserBudgets = dao.budgetsForUser(userId);
        List<PillBarChart.BarItem> sixMonthHistory = buildSixMonthHistory(
                userId, role, targetYear, targetMonthZero, locale, allUserBudgets,
                role == AppRole.CHILD ? dao.childProfileForUser(userId) : null);

        // Parent-specific aggregate child stats (no child transaction/product leak!)
        ArrayList<AnalyticsSummaryData.ChildAnalyticsItem> childrenProgress = new ArrayList<>();
        if (role == AppRole.PARENT) {
            List<ChildProfileEntity> profiles = dao.childProfilesForParent(userId);
            for (ChildProfileEntity cp : profiles) {
                UserEntity childUser = dao.findUser(cp.childUserId);
                if (childUser == null) continue;
                List<BudgetEntity> childBudgets = dao.budgetsForUser(cp.childUserId);
                BudgetEntity childPeriodBudget = findMonthBudget(childBudgets, targetYear, targetMonthZero);
                long childAlloc = childPeriodBudget != null && childPeriodBudget.monthlyLimitMilli > 0L
                        ? childPeriodBudget.monthlyLimitMilli
                        : (cp.allocatedTotalMilli > 0L ? cp.allocatedTotalMilli : Math.max(0L, childUser.balanceMilli));
                long childMonthStart = childPeriodBudget != null ? childPeriodBudget.periodStart
                        : BudgetPeriodMath.startOfMonthMillis(targetYear, targetMonthZero);
                long childMonthEndExcl = safeExclusiveEnd(childPeriodBudget != null ? childPeriodBudget.periodEnd
                        : BudgetPeriodMath.endOfMonthMillis(targetYear, targetMonthZero));
                long childSpent = dao.totalSpendingBetween(cp.childUserId, childMonthStart, childMonthEndExcl);
                long childRem = Math.max(0L, MoneyMath.subtract(childAlloc, childSpent));
                int childPct = childAlloc > 0L ? Math.max(0, MoneyMath.percentOf(childSpent, childAlloc)) : 0;
                List<PillBarChart.BarItem> chHist = buildSixMonthHistory(
                        cp.childUserId, AppRole.CHILD, targetYear, targetMonthZero, locale, childBudgets, cp);
                childrenProgress.add(new AnalyticsSummaryData.ChildAnalyticsItem(
                        cp.childUserId,
                        cp.nickname != null && !cp.nickname.trim().isEmpty() ? cp.nickname.trim() : childUser.name,
                        childUser.email == null ? "" : childUser.email,
                        childAlloc,
                        childSpent,
                        childRem,
                        childPct,
                        chHist));
            }
        }

        return new AnalyticsSummaryData(
                role,
                budget,
                curSpent,
                prevSpent,
                prevStart,
                prevEnd,
                purchasesTotal,
                expensesTotal,
                avgDailyMilli,
                daysElapsed,
                totalDays,
                daysRemaining,
                projectedTotalMilli,
                paceSustainable,
                projectedDiffMilli,
                categoryStats,
                attentionCategory,
                sixMonthHistory,
                curRows,
                childrenProgress);
    }

    private List<PillBarChart.BarItem> buildSixMonthHistory(
            long userId,
            AppRole role,
            int endYear,
            int endMonthZero,
            Locale locale,
            List<BudgetEntity> userBudgets,
            ChildProfileEntity childProfile) {
        ArrayList<PillBarChart.BarItem> list = new ArrayList<>(6);
        for (int i = 5; i >= 0; i--) {
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(Calendar.YEAR, endYear);
            cal.set(Calendar.MONTH, endMonthZero);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, -i);
            int y = cal.get(Calendar.YEAR);
            int mZero = cal.get(Calendar.MONTH);
            long mStart = BudgetPeriodMath.startOfMonthMillis(y, mZero);
            long mEnd = BudgetPeriodMath.endOfMonthMillis(y, mZero);
            long mEndExcl = safeExclusiveEnd(mEnd);
            String mKey = String.format(Locale.US, "%04d-%02d", y, mZero + 1);
            String shortLbl = BudgetPeriodMath.shortMonthLabel(mZero, locale);

            long spent = dao.totalSpendingBetween(userId, mStart, mEndExcl);
            if (role == AppRole.PARENT) {
                // Include family_allocation entries only when not already matched by recorded Enfants expense
                long allocTotal = dao.childAllocationsBetween(userId, mStart, mEndExcl);
                if (spent == 0L && allocTotal > 0L) {
                    spent = allocTotal;
                }
            }
            BudgetEntity mb = findMonthBudget(userBudgets, y, mZero);
            long allocated = mb != null ? mb.monthlyLimitMilli : 0L;
            if (role == AppRole.CHILD && allocated == 0L && i == 0 && childProfile != null) {
                allocated = childProfile.allocatedTotalMilli;
            }
            list.add(new PillBarChart.BarItem(mKey, shortLbl, spent, allocated, i == 0));
        }
        return list;
    }

    private static BudgetEntity findMonthBudget(List<BudgetEntity> budgets, int year, int monthZero) {
        if (budgets == null || budgets.isEmpty()) return null;
        long mStart = BudgetPeriodMath.startOfMonthMillis(year, monthZero);
        long mEnd = BudgetPeriodMath.endOfMonthMillis(year, monthZero);
        for (BudgetEntity b : budgets) {
            if (BudgetPeriodMath.periodsOverlap(b.periodStart, b.periodEnd, mStart, mEnd)) {
                return b;
            }
        }
        return null;
    }

    private static long monthStartMillis() {
        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.set(Calendar.HOUR_OF_DAY, 0);
        month.set(Calendar.MINUTE, 0);
        month.set(Calendar.SECOND, 0);
        month.set(Calendar.MILLISECOND, 0);
        return month.getTimeInMillis();
    }

    private static long nextMonthStartMillis(long start) {
        Calendar next = Calendar.getInstance();
        next.setTimeInMillis(start);
        next.add(Calendar.MONTH, 1);
        return next.getTimeInMillis();
    }

    private static long safeExclusiveEnd(long inclusiveEnd) {
        return inclusiveEnd == Long.MAX_VALUE ? Long.MAX_VALUE : inclusiveEnd + 1;
    }
}
