package com.example.masrouf1.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.lifecycle.LiveData;

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
import com.example.masrouf1.MoneyMath;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Dao
public interface MasroufDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(UserEntity row);
    @Insert(onConflict = OnConflictStrategy.ABORT) long insertNewUser(UserEntity row);
    @Insert(onConflict = OnConflictStrategy.ABORT) long insertChildUser(UserEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(ChildProfileEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(CategoryEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(ProductEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(BudgetEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(BudgetLineEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(PurchaseEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(PurchaseItemEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(TransactionEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(GoalEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(RewardEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(NotificationEntity row);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertNotificationOnce(NotificationEntity row);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insert(MoneyRequestEntity row);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insert(PointsLogEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(PriceHistoryEntity row);
    @Insert(onConflict = OnConflictStrategy.ABORT) long insertSystemCategory(SystemCategoryEntity row);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertUserCategory(UserCategoryEntity row);
    @Insert(onConflict = OnConflictStrategy.IGNORE) long insertSetting(SettingEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long[] insertBudgetLines(List<BudgetLineEntity> rows);
    @Update int update(BudgetEntity row);
    @Update int update(ChildProfileEntity row);
    @Update int update(ProductEntity row);
    @Update int update(CategoryEntity row);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1") UserEntity findUser(long id);
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1") LiveData<UserEntity> observeUser(long id);
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1") UserEntity findUserByEmail(String email);
    @Query("UPDATE users SET email = :email WHERE id = :userId") int updateEmail(long userId,String email);
    @Query("UPDATE users SET name = :name WHERE id = :userId") int updateName(long userId,String name);
    /** Met à jour le hash du mot de passe d'un utilisateur (utilisé par ChangePasswordActivity). */
    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = :userId") int updatePasswordHash(long userId,String passwordHash);
    @Query("UPDATE users SET onboarded = :onboarded WHERE id = :userId") int updateOnboarding(long userId, boolean onboarded);
    @Query("UPDATE users SET onboarded = 1, categoriesSelected = 1 WHERE id = :userId") int completeOnboardingAndCategorySelection(long userId);
    @Query("UPDATE users SET onboarded = 1 WHERE id = :userId") int completeGeneralOnboarding(long userId);
    @Query("SELECT * FROM system_categories ORDER BY id") List<SystemCategoryEntity> systemCategories();
    @Query("SELECT * FROM system_categories WHERE id = :id LIMIT 1") SystemCategoryEntity systemCategory(long id);
    @Query("SELECT * FROM system_categories WHERE `key` = :key LIMIT 1") SystemCategoryEntity systemCategoryByKey(String key);
    @Query("SELECT * FROM system_categories sc WHERE sc.protectedCategory = 1 OR sc.id IN (SELECT uc.catalogId FROM user_categories uc INNER JOIN child_profiles cp ON cp.parentUserId = uc.userId WHERE cp.childUserId = :childUserId AND uc.active = 1 AND uc.catalogId IS NOT NULL) ORDER BY sc.id") List<SystemCategoryEntity> allowedSystemCategoriesForChild(long childUserId);
    @Query("SELECT catalogId FROM user_categories WHERE userId = :userId AND active = 1 AND catalogId IS NOT NULL ORDER BY catalogId") List<Long> selectedCatalogIds(long userId);
    @Query("SELECT categoryId FROM user_categories WHERE userId = :userId AND active = 1 ORDER BY categoryId") List<Long> selectedCategoryIds(long userId);
    @Query("SELECT categories.* FROM categories INNER JOIN user_categories ON user_categories.categoryId = categories.id WHERE user_categories.userId = :userId AND user_categories.catalogId = :catalogId LIMIT 1") CategoryEntity selectedCategoryForCatalog(long userId,long catalogId);
    @Query("SELECT * FROM categories WHERE userId = :userId AND name = :name COLLATE NOCASE LIMIT 1") CategoryEntity categoryForUserByName(long userId,String name);
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1") SettingEntity systemSettings();
    @Query("SELECT * FROM goals WHERE userId = :userId ORDER BY dueAt, id LIMIT 1") GoalEntity primaryGoal(long userId);
    @Query("SELECT * FROM budgets WHERE userId = :userId AND status = 'ACTIVE' ORDER BY periodStart DESC LIMIT 1") BudgetEntity activeBudgetForUser(long userId);
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY periodStart ASC, id ASC") List<BudgetEntity> budgetsForUser(long userId);
    @Query("SELECT * FROM budgets WHERE userId = :userId AND periodStart <= :rangeEnd AND periodEnd >= :rangeStart ORDER BY periodStart ASC, id ASC") List<BudgetEntity> budgetsOverlappingForUser(long userId, long rangeStart, long rangeEnd);
    @Query("SELECT * FROM budgets WHERE userId = :userId AND periodStart <= :now AND periodEnd >= :now ORDER BY CASE WHEN status = 'ACTIVE' THEN 0 ELSE 1 END, periodStart DESC, id DESC LIMIT 1") BudgetEntity currentBudgetAt(long userId, long now);
    @Query("SELECT * FROM budgets WHERE userId = :userId AND (:excludeId <= 0 OR id != :excludeId) AND periodStart <= :end AND periodEnd >= :start LIMIT 1") BudgetEntity findOverlappingBudget(long userId, long start, long end, long excludeId);
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name COLLATE NOCASE") List<CategoryEntity> categoriesForUser(long userId);
    @Query("SELECT c.* FROM categories c INNER JOIN user_categories uc ON uc.categoryId = c.id WHERE uc.userId = :userId AND uc.active = 1 ORDER BY c.name COLLATE NOCASE") List<CategoryEntity> selectedCategoriesForUser(long userId);
    @Query("SELECT * FROM categories WHERE id = :categoryId AND userId = :userId LIMIT 1") CategoryEntity findCategoryForUser(long categoryId, long userId);
    @Query("SELECT c.id AS categoryId, c.name AS name, c.colorHex AS colorHex, c.type AS type, sc.`key` AS catalogKey, sc.iconName AS iconName, c.allocationPercent AS allocationPercent, COALESCE(sc.recommendedPercent, 5) AS recommendedPercent FROM categories c INNER JOIN user_categories uc ON uc.categoryId = c.id LEFT JOIN system_categories sc ON sc.id = uc.catalogId WHERE uc.userId = :userId AND uc.active = 1 ORDER BY c.name COLLATE NOCASE") List<CategoryPresentationRow> selectedCategoryPresentations(long userId);
    @Query("SELECT * FROM user_categories WHERE userId = :userId") List<UserCategoryEntity> userCategoryLinks(long userId);
    @Query("UPDATE user_categories SET active = :active, catalogId = :catalogId WHERE id = :id") int updateUserCategoryLink(long id, boolean active, Long catalogId);
    @Query("UPDATE users SET categoriesSelected = 1 WHERE id = :userId") int markCategoriesSelected(long userId);
    @Query("DELETE FROM budget_lines WHERE id = :lineId") int deleteBudgetLine(long lineId);
    @Query("SELECT COUNT(*) FROM products WHERE categoryId = :categoryId") int countProductsInCategory(long categoryId);
    @Query("SELECT COUNT(*) FROM money_transactions WHERE categoryId = :categoryId") int countTransactionsInCategory(long categoryId);
    @Query("SELECT COUNT(*) FROM user_categories WHERE categoryId = :categoryId AND catalogId IS NOT NULL") int countCatalogLinkForCategory(long categoryId);
    @Query("DELETE FROM categories WHERE id = :categoryId AND userId = :userId") int deleteCategoryRow(long categoryId, long userId);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND categoryId = :categoryId AND kind IN ('expense','donation','purchase') AND occurredAt >= :from AND occurredAt < :to") long categoryTotalSpendBetween(long userId, long categoryId, long from, long to);
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name COLLATE NOCASE") LiveData<List<CategoryEntity>> observeCategoriesForUser(long userId);
    @Query("SELECT * FROM budget_lines WHERE budgetId = :budgetId ORDER BY categoryId") List<BudgetLineEntity> budgetLines(long budgetId);
    @Query("SELECT COALESCE(SUM(limitMilli),0) FROM budget_lines WHERE budgetId = :budgetId") long allocatedEnvelopes(long budgetId);
    @Query("DELETE FROM budget_lines WHERE budgetId = :budgetId") int deleteBudgetLines(long budgetId);
    @Query("SELECT * FROM child_profiles WHERE childUserId = :childUserId LIMIT 1") ChildProfileEntity childProfileForUser(long childUserId);
    @Query("SELECT COALESCE(SUM(allocatedTotalMilli),0) FROM child_profiles WHERE parentUserId = :parentUserId AND allocationPeriodStart = (SELECT periodStart FROM budgets WHERE userId = :parentUserId AND status = 'ACTIVE' ORDER BY periodStart DESC LIMIT 1)") long totalChildAllocations(long parentUserId);
    @Query("SELECT COUNT(*) FROM child_profiles WHERE parentUserId = :parentUserId") int countChildren(long parentUserId);
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND read = 0") int unreadNotificationCount(long userId);
    @Query("SELECT COUNT(*) FROM money_requests WHERE parentUserId = :parentUserId AND status = 'pending'") int pendingMoneyRequestCount(long parentUserId);
    @Query("SELECT * FROM money_requests WHERE id = :requestId LIMIT 1") MoneyRequestEntity findMoneyRequest(long requestId);
    @Query("SELECT * FROM money_requests WHERE parentUserId = :parentUserId ORDER BY CASE status WHEN 'pending' THEN 0 ELSE 1 END, createdAt DESC") List<MoneyRequestEntity> moneyRequestsForParent(long parentUserId);
    @Query("SELECT r.id, r.childUserId, u.name AS childName, r.amountMilli, r.reason, r.status, r.createdAt FROM money_requests r INNER JOIN users u ON u.id = r.childUserId WHERE r.parentUserId = :parentUserId ORDER BY CASE r.status WHEN 'pending' THEN 0 ELSE 1 END, r.createdAt DESC") List<MoneyRequestSummary> moneyRequestSummariesForParent(long parentUserId);
    @Query("SELECT * FROM money_requests WHERE childUserId = :childUserId ORDER BY createdAt DESC LIMIT 20") List<MoneyRequestEntity> moneyRequestsForChild(long childUserId);
    @Query("UPDATE money_requests SET status = :status, updatedAt = :updatedAt, responseNote = :responseNote WHERE id = :requestId AND parentUserId = :parentUserId AND status = 'pending'") int updateMoneyRequestStatus(long requestId,long parentUserId,String status,long updatedAt,String responseNote);
    @Query("SELECT * FROM points_log WHERE userId = :userId ORDER BY createdAt DESC, id DESC LIMIT 50") List<PointsLogEntity> pointsHistory(long userId);
    @Query("SELECT * FROM rewards WHERE childProfileId = :profileId ORDER BY createdAt DESC, id DESC") List<RewardEntity> rewardsForChild(long profileId);
    @Query("SELECT * FROM rewards WHERE id = :rewardId LIMIT 1") RewardEntity findReward(long rewardId);
    @Query("UPDATE rewards SET redeemed = 1 WHERE id = :rewardId AND childProfileId = :profileId AND redeemed = 0") int markRewardRedeemed(long rewardId,long profileId);
    @Query("UPDATE users SET points = points - :cost WHERE id = :userId AND points >= :cost") int subtractPoints(long userId,int cost);
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC, id DESC") LiveData<List<NotificationEntity>> observeNotificationsForUser(long userId);
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC, id DESC") List<NotificationEntity> notificationsForUser(long userId);
    @Query("UPDATE notifications SET read = 1 WHERE id = :notificationId AND userId = :userId") int markNotificationRead(long notificationId,long userId);
    @Query("UPDATE notifications SET read = 1 WHERE userId = :userId") int markAllNotificationsRead(long userId);
    @Query("SELECT * FROM categories WHERE userId = :userId AND name = 'Enfants' COLLATE NOCASE LIMIT 1") CategoryEntity childrenCategory(long userId);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND categoryId = :categoryId AND kind IN ('expense','donation') AND occurredAt >= :from AND occurredAt <= :to") long categoryExpenseBetween(long userId,long categoryId,long from,long to);
    @Query("SELECT * FROM budget_lines WHERE budgetId = :budgetId AND categoryId = :categoryId LIMIT 1") BudgetLineEntity budgetLineForCategory(long budgetId,long categoryId);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND kind IN ('expense','donation','purchase') AND occurredAt >= :from AND occurredAt <= :to AND categoryId NOT IN (SELECT id FROM categories WHERE userId = :userId AND name = 'Enfants' COLLATE NOCASE)") long personalSpendingBetween(long userId,long from,long to);
    @Query("SELECT budgets.periodStart, budgets.monthlyLimitMilli AS allocatedMilli, budgets.spentMilli, budgets.pointsAwarded FROM budgets INNER JOIN child_profiles ON child_profiles.childUserId = budgets.userId WHERE budgets.userId = :childUserId AND child_profiles.parentUserId = :parentUserId ORDER BY budgets.periodStart DESC LIMIT 4") List<BudgetHistoryRow> childBudgetHistory(long childUserId,long parentUserId);
    @Query("SELECT users.email FROM users INNER JOIN child_profiles ON child_profiles.childUserId = users.id WHERE child_profiles.id = :profileId AND child_profiles.parentUserId = :parentId LIMIT 1") String childEmailForProfile(long profileId,long parentId);
    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = (SELECT childUserId FROM child_profiles WHERE id = :profileId AND parentUserId = :parentId)") int resetChildPassword(long profileId,long parentId,String passwordHash);
    @Query("UPDATE child_profiles SET allocatedTotalMilli = CASE WHEN allocationPeriodStart = :periodStart THEN allocatedTotalMilli + :amountMilli ELSE :amountMilli END, allocationPeriodStart = :periodStart WHERE id = :profileId") int addChildAllocation(long profileId,long amountMilli,long periodStart);
    @Query("UPDATE users SET balanceMilli = balanceMilli + :amountMilli WHERE id = :childUserId") int addChildBalance(long childUserId,long amountMilli);
    @Query("UPDATE users SET balanceMilli = MAX(0, balanceMilli - :amountMilli) WHERE id = :childUserId") int subtractChildBalance(long childUserId,long amountMilli);
    @Query("SELECT * FROM child_profiles WHERE parentUserId = :parentUserId ORDER BY nickname COLLATE NOCASE") List<ChildProfileEntity> childProfilesForParent(long parentUserId);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :parentUserId AND kind = 'family_allocation' AND occurredAt >= :from AND occurredAt < :to") long childAllocationsBetween(long parentUserId,long from,long to);
    @Query("SELECT * FROM money_transactions WHERE userId = :userId AND kind IN ('expense','donation','purchase') AND occurredAt >= :from AND occurredAt < :to ORDER BY occurredAt DESC, id DESC") List<TransactionEntity> spendingTransactionsBetween(long userId,long from,long to);
    @Query("SELECT child_profiles.parentPhone FROM child_profiles WHERE child_profiles.childUserId = :childUserId LIMIT 1") String parentPhoneForChild(long childUserId);
    @Query("SELECT child_profiles.id AS childProfileId, users.id AS childUserId, child_profiles.nickname AS nickname, users.email AS email, child_profiles.parentPhone AS parentPhone, child_profiles.createdAt AS createdAt, CASE WHEN child_profiles.allocationPeriodStart = (SELECT periodStart FROM budgets WHERE userId = :parentUserId AND status = 'ACTIVE' ORDER BY periodStart DESC LIMIT 1) THEN child_profiles.allocatedTotalMilli ELSE 0 END AS allocatedTotalMilli, COALESCE(SUM(money_transactions.amountMilli),0) AS spentMilli, users.balanceMilli AS balanceMilli FROM child_profiles INNER JOIN users ON users.id = child_profiles.childUserId LEFT JOIN budgets ON budgets.userId = users.id AND budgets.status = 'ACTIVE' LEFT JOIN money_transactions ON money_transactions.userId = users.id AND money_transactions.kind IN ('expense','donation','purchase') AND money_transactions.occurredAt >= budgets.periodStart AND money_transactions.occurredAt <= budgets.periodEnd WHERE child_profiles.parentUserId = :parentUserId AND EXISTS (SELECT 1 FROM users parent WHERE parent.id = :parentUserId AND lower(parent.role) IN ('parent','admin')) GROUP BY child_profiles.id, users.id, budgets.id ORDER BY child_profiles.nickname COLLATE NOCASE")
    List<ChildSpendingSummary> childSpendingSummaries(long parentUserId);
    @Query("SELECT categoryId, COALESCE(SUM(amountMilli),0) AS amountMilli FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt < :to AND kind IN ('expense','donation','purchase') AND categoryId > 0 GROUP BY categoryId")
    List<CategorySpendRow> categorySpendingBetween(long userId,long from,long to);
    @Query("SELECT categoryId, COALESCE(SUM(amountMilli),0) AS amountMilli FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt < :to AND kind IN ('expense','donation','purchase') AND categoryId > 0 GROUP BY categoryId")
    LiveData<List<CategorySpendRow>> observeCategorySpendingBetween(long userId,long from,long to);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt < :to AND kind IN ('expense','donation','purchase')")
    long totalSpendingBetween(long userId,long from,long to);
    @Query("SELECT * FROM child_profiles WHERE id = :profileId LIMIT 1") ChildProfileEntity childProfile(long profileId);
    @Query("UPDATE child_profiles SET allocatedTotalMilli = :allocatedTotalMilli, allocationPeriodStart = :periodStart WHERE id = :profileId") int setChildAllocation(long profileId,long allocatedTotalMilli,long periodStart);
    @Query("SELECT COALESCE(SUM(allocationPercent), 0) FROM categories WHERE userId = :userId AND type = :type") int allocatedPercent(long userId, String type);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId ORDER BY occurredAt DESC LIMIT 5") List<TransactionRow> recentTransactions(long userId);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt <= :to ORDER BY occurredAt DESC") List<TransactionRow> transactionsBetween(long userId,long from,long to);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt <= :to ORDER BY occurredAt DESC") LiveData<List<TransactionRow>> observeTransactionsBetween(long userId,long from,long to);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId AND kind IN ('expense','donation','purchase') ORDER BY occurredAt DESC LIMIT 5") List<TransactionRow> recentSpendingTransactions(long userId);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId AND kind = 'allowance_received' ORDER BY occurredAt DESC LIMIT 1") TransactionRow latestAllowanceReceived(long userId);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND kind = 'allowance_received'") long totalAllowanceReceived(long userId);
    @Query("SELECT COUNT(*) FROM money_transactions WHERE userId = :userId AND kind IN ('expense','donation','purchase') AND occurredAt >= :from AND occurredAt < :to") int transactionCount(long userId,long from,long to);
    @Query("SELECT COUNT(*) FROM money_transactions WHERE kind IN ('expense','donation','purchase') AND occurredAt >= :from AND occurredAt < :to AND (userId = :parentId OR userId IN (SELECT childUserId FROM child_profiles WHERE parentUserId = :parentId))") int familyTransactionCount(long parentId,long from,long to);
    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1") BudgetEntity findBudget(long id);
    @Query("SELECT * FROM money_transactions WHERE id = :id LIMIT 1") TransactionEntity findTransaction(long id);
    @Query("DELETE FROM money_transactions WHERE id = :id AND userId = :userId") int deleteTransactionById(long id,long userId);
    @Query("SELECT products.* FROM products INNER JOIN categories ON categories.id = products.categoryId WHERE products.id = :id AND categories.userId = :userId LIMIT 1") ProductEntity findProductForUser(long id,long userId);
    @Query("SELECT products.* FROM products INNER JOIN categories ON categories.id = products.categoryId WHERE categories.userId = :userId ORDER BY products.updatedAt DESC, products.id DESC") LiveData<List<ProductEntity>> observeProductsForUser(long userId);
    @Query("SELECT products.* FROM products INNER JOIN categories ON categories.id = products.categoryId WHERE categories.userId = :userId ORDER BY products.updatedAt DESC, products.id DESC") List<ProductEntity> productsForUser(long userId);
    @Query("SELECT p.* FROM products p INNER JOIN categories c ON c.id = p.categoryId WHERE c.userId = :userId AND p.categoryId = :categoryId AND lower(trim(p.name)) = lower(trim(:name)) AND lower(trim(coalesce(p.brand,''))) = lower(trim(coalesce(:brand,''))) AND lower(trim(p.unit)) = lower(trim(:unit)) ORDER BY p.updatedAt DESC, p.id DESC LIMIT 1")
    ProductEntity findMatchingProductForUser(long userId,long categoryId,String name,String brand,String unit);
    @Query("SELECT p.id, p.categoryId, p.name, p.brand, p.unit, p.photoPath, c.name AS categoryName, p.currentPriceMilli, (SELECT h.storeName FROM price_history h WHERE h.productId = p.id ORDER BY h.observedAt DESC, h.id DESC LIMIT 1) AS storeName, (SELECT h.priceMilli FROM price_history h WHERE h.productId = p.id ORDER BY h.observedAt DESC, h.id DESC LIMIT 1 OFFSET 1) AS priorPriceMilli FROM products p INNER JOIN categories c ON c.id = p.categoryId WHERE c.userId = :userId ORDER BY p.updatedAt DESC, p.id DESC")
    List<ProductCatalogRow> productCatalog(long userId);
    @Query("SELECT price_history.* FROM price_history INNER JOIN products ON products.id = price_history.productId INNER JOIN categories ON categories.id = products.categoryId WHERE price_history.productId = :productId AND categories.userId = :userId ORDER BY price_history.observedAt") List<PriceHistoryEntity> priceHistory(long userId,long productId);
    @Query("SELECT purchases.id AS purchaseId, purchases.occurredAt AS occurredAt, purchases.totalMilli AS totalMilli, purchases.storeName AS storeName, purchase_items.quantityMilli AS quantityMilli, purchase_items.unit AS unit, purchase_items.unitPriceMilli AS unitPriceMilli, purchase_items.description AS description FROM purchase_items INNER JOIN purchases ON purchases.id = purchase_items.purchaseId INNER JOIN products ON products.id = purchase_items.productId INNER JOIN categories ON categories.id = products.categoryId WHERE purchases.userId = :userId AND categories.userId = :userId AND purchase_items.productId = :productId ORDER BY purchases.occurredAt DESC, purchases.id DESC") List<PurchaseHistoryRow> productPurchaseHistory(long userId,long productId);
    @Query("SELECT price_history.* FROM price_history INNER JOIN products ON products.id = price_history.productId INNER JOIN categories ON categories.id = products.categoryId WHERE price_history.productId = :productId AND categories.userId = :userId ORDER BY price_history.observedAt") LiveData<List<PriceHistoryEntity>> observePriceHistory(long userId,long productId);
    @Query("SELECT pi.unitPriceMilli FROM purchase_items pi INNER JOIN purchases pu ON pu.id=pi.purchaseId INNER JOIN products pr ON pr.id=pi.productId INNER JOIN categories c ON c.id=pr.categoryId WHERE pu.userId=:userId AND c.userId=:userId AND lower(trim(pr.name))=lower(trim(:name)) AND lower(trim(coalesce(pr.brand,'')))=lower(trim(:brand)) AND pi.unit=:unit ORDER BY pu.occurredAt DESC LIMIT 1") Long latestUnitPrice(long userId,String name,String brand,String unit);
    @Query("SELECT COUNT(*) FROM categories WHERE id = :categoryId AND userId = :userId") int categoryOwnedByUser(long categoryId,long userId);
    @Query("SELECT * FROM budgets WHERE status = 'ACTIVE' AND periodEnd <= :now") List<BudgetEntity> findExpiredBudgets(long now);
    @Query("SELECT users.name FROM users INNER JOIN child_profiles ON child_profiles.childUserId = users.id WHERE child_profiles.parentUserId = :parentUserId ORDER BY users.name") List<String> childNames(long parentUserId);
    @Query("SELECT SUM(amountMilli) FROM money_transactions WHERE userId = :userId AND kind IN ('expense','donation','purchase') AND occurredAt >= :historyStart GROUP BY strftime('%Y-%m', occurredAt / 1000, 'unixepoch')")
    List<Long> monthlySpendingTotals(long userId, long historyStart);
    default Long averageMonthlySpendMilli(long userId, long historyStart) {
        List<Long> monthlyTotals=monthlySpendingTotals(userId,historyStart);
        if(monthlyTotals.isEmpty()) return null;
        BigInteger sum=BigInteger.ZERO; for(Long total:monthlyTotals) sum=sum.add(BigInteger.valueOf(total==null?0L:total));
        return sum.divide(BigInteger.valueOf(monthlyTotals.size())).longValueExact();
    }

    @Transaction
    default long createMoneyRequest(MoneyRequestEntity request,String notificationTitle,String notificationMessage) {
        if(request==null||request.amountMilli<=0||request.childUserId<=0)return -1L;
        UserEntity child=findUser(request.childUserId);ChildProfileEntity profile=childProfileForUser(request.childUserId);
        if(child==null||profile==null||!"child".equalsIgnoreCase(child.role))return -1L;
        request.parentUserId=profile.parentUserId;request.status="pending";
        if(request.createdAt<=0)request.createdAt=System.currentTimeMillis();request.updatedAt=request.createdAt;
        long requestId=insert(request);if(requestId<=0)return -1L;
        NotificationEntity notification=new NotificationEntity();notification.userId=profile.parentUserId;
        notification.title=notificationTitle;notification.message=notificationMessage;notification.kind="money_request";
        notification.createdAt=request.createdAt;notification.eventKey="money-request:"+requestId;
        insertNotificationOnce(notification);
        return requestId;
    }

    @Transaction
    default int resolveMoneyRequest(long requestId,long parentUserId,boolean approve,String responseNote,
                                   long now,String notificationTitle,String notificationMessage) {
        MoneyRequestEntity request=findMoneyRequest(requestId);UserEntity parent=findUser(parentUserId);
        if(request==null||request.parentUserId!=parentUserId||!"pending".equalsIgnoreCase(request.status)
                ||parent==null||!("parent".equalsIgnoreCase(parent.role)||"admin".equalsIgnoreCase(parent.role)))return -1;
        ChildProfileEntity profile=childProfileForUser(request.childUserId);
        if(profile==null||profile.parentUserId!=parentUserId)return -1;
        if(approve){int transfer=giveMoneyToChild(profile.id,parentUserId,request.amountMilli,request.reason,now,notificationTitle,notificationMessage,false);if(transfer!=0)return transfer;}
        String status=approve?"accepted":"rejected";
        if(updateMoneyRequestStatus(requestId,parentUserId,status,now,responseNote)==0)
            throw new IllegalStateException("Money request changed during approval");
        NotificationEntity notification=new NotificationEntity();notification.userId=request.childUserId;
        notification.title=notificationTitle;notification.message=notificationMessage;notification.kind="money_request_"+status;
        notification.createdAt=now;notification.eventKey="money-request-response:"+requestId+":"+status;
        insertNotificationOnce(notification);
        return 0;
    }

    @Transaction
    default boolean awardPointsOnce(long userId,String eventKey,int points,String reason,long now,
                                   String notificationTitle,String notificationMessage) {
        if(userId<=0||eventKey==null||eventKey.trim().isEmpty()||points<=0||findUser(userId)==null)return false;
        PointsLogEntity log=new PointsLogEntity();log.userId=userId;log.eventKey=eventKey;log.points=points;
        log.reason=reason==null?"":reason;log.createdAt=now;
        if(insert(log)<=0)return false;
        if(addPoints(userId,points)==0)throw new IllegalStateException("Points owner disappeared");
        NotificationEntity notification=new NotificationEntity();notification.userId=userId;notification.title=notificationTitle;
        notification.message=notificationMessage;notification.kind="points_awarded";notification.createdAt=now;
        notification.eventKey="points:"+eventKey;insertNotificationOnce(notification);
        return true;
    }

    @Transaction
    default boolean redeemReward(long childUserId,long rewardId,long now,String notificationTitle,String notificationMessage){
        UserEntity child=findUser(childUserId);ChildProfileEntity profile=childProfileForUser(childUserId);RewardEntity reward=findReward(rewardId);
        if(child==null||!"child".equalsIgnoreCase(child.role)||profile==null||reward==null||reward.childProfileId!=profile.id
                ||reward.redeemed||reward.pointsCost<0||child.points<reward.pointsCost)return false;
        if(markRewardRedeemed(rewardId,profile.id)==0) return false;
        if(reward.pointsCost>0&&subtractPoints(childUserId,reward.pointsCost)==0)throw new IllegalStateException("Reward points changed");
        PointsLogEntity log=new PointsLogEntity();log.userId=childUserId;log.eventKey="reward-redeemed:"+rewardId;
        log.points=-reward.pointsCost;log.reason="reward-redeemed";log.createdAt=now;
        if(insert(log)<=0)throw new IllegalStateException("Reward was already logged");
        NotificationEntity notification=new NotificationEntity();notification.userId=childUserId;notification.title=notificationTitle;
        notification.message=notificationMessage;notification.kind="reward_redeemed";notification.createdAt=now;
        notification.eventKey="reward-notification:"+rewardId;insertNotificationOnce(notification);
        return true;
    }

    @Query("UPDATE budgets SET status = 'CLOSED', lastMonthSummaryMilli = spentMilli, pointsAwarded = pointsAwarded + :points WHERE id = :budgetId")
    int closeAndSummarize(long budgetId, int points);
    @Query("UPDATE users SET points = points + :points WHERE id = :userId") int addPoints(long userId, int points);
    @Query("UPDATE budgets SET spentMilli = spentMilli + :amountMilli WHERE id = :budgetId") int addBudgetSpend(long budgetId, long amountMilli);
    @Query("UPDATE budget_lines SET spentMilli = spentMilli + :amountMilli WHERE budgetId = :budgetId AND categoryId = :categoryId") int addEnvelopeSpend(long budgetId,long categoryId,long amountMilli);
    @Query("UPDATE budgets SET spentMilli = spentMilli - :amountMilli WHERE id = :budgetId AND spentMilli >= :amountMilli") int subtractBudgetSpend(long budgetId,long amountMilli);
    @Query("UPDATE budget_lines SET spentMilli = spentMilli - :amountMilli WHERE budgetId = :budgetId AND categoryId = :categoryId AND spentMilli >= :amountMilli") int subtractEnvelopeSpend(long budgetId,long categoryId,long amountMilli);
    @Query("UPDATE budgets SET periodStart = :start, periodEnd = :end, monthlyLimitMilli = :newLimit, spentMilli = 0, status = 'ACTIVE' WHERE id = :budgetId")
    int startNextPeriod(long budgetId, long start, long end, long newLimit);
    @Update int updateBudgetLines(List<BudgetLineEntity> rows);

    @Transaction
    default long ensureChildrenCategory(long parentUserId) {
        CategoryEntity category=childrenCategory(parentUserId);
        if(category==null) { category=new CategoryEntity(); category.userId=parentUserId; category.name="Enfants"; category.type="expense"; category.colorHex="#E9B66B"; category.isEssential=true; category.allocationPercent=0; category.id=insert(category); }
        BudgetEntity budget=activeBudgetForUser(parentUserId);
        if(budget!=null && budgetLineForCategory(budget.id,category.id)==null) { BudgetLineEntity line=new BudgetLineEntity(); line.budgetId=budget.id; line.categoryId=category.id; insert(line); }
        return category.id;
    }

    @Transaction
    default long createChildAccount(UserEntity child,ChildProfileEntity profile,BudgetEntity initialBudget,List<CategoryEntity> categories) {
        UserEntity parent=findUser(profile.parentUserId);
        if(parent==null||!("parent".equalsIgnoreCase(parent.role)||"admin".equalsIgnoreCase(parent.role))||!"child".equalsIgnoreCase(child.role)) throw new IllegalArgumentException("invalid child account owner");
        ensureChildrenCategory(profile.parentUserId);
        BudgetEntity parentBudget=activeBudgetForUser(profile.parentUserId);
        if(parentBudget!=null)profile.allocationPeriodStart=parentBudget.periodStart;
        if(profile.allocatedTotalMilli>0) {
            if(parentBudget==null||MoneyMath.add(totalChildAllocations(profile.parentUserId),profile.allocatedTotalMilli)>parentBudget.monthlyLimitMilli) throw new IllegalArgumentException("child allocation exceeds household budget");
            initialBudget.periodStart=parentBudget.periodStart; initialBudget.periodEnd=parentBudget.periodEnd;
        }
        child.onboarded=false;child.categoriesSelected=false;
        if(profile.allocatedTotalMilli>0 && child.balanceMilli<profile.allocatedTotalMilli) child.balanceMilli=profile.allocatedTotalMilli;
        long childId=insertChildUser(child); profile.childUserId=childId; insert(profile);
        // Parent-created default suggestions are not a child's confirmed selections; only the onboarding transaction may select them.
        if(initialBudget!=null) { initialBudget.userId=childId; insert(initialBudget); }
        return childId;
    }

    @Transaction
    default boolean updateChildFamilyAllocation(long profileId,long allocatedTotalMilli,long periodStart,long periodEnd) {
        ChildProfileEntity profile=childProfile(profileId); if(profile==null || allocatedTotalMilli<=0) return false;
        BudgetEntity parentBudget=activeBudgetForUser(profile.parentUserId);
        if(parentBudget==null)return false;
        long currentAllocation=profile.allocationPeriodStart==parentBudget.periodStart?profile.allocatedTotalMilli:0L;
        if(MoneyMath.add(MoneyMath.subtract(totalChildAllocations(profile.parentUserId),currentAllocation),allocatedTotalMilli)>parentBudget.monthlyLimitMilli) return false;
        BudgetEntity budget=activeBudgetForUser(profile.childUserId);
        if(budget!=null && allocatedEnvelopes(budget.id)>allocatedTotalMilli) return false;
        if(setChildAllocation(profileId,allocatedTotalMilli,parentBudget.periodStart)==0) return false;
        if(budget==null) {
            budget=new BudgetEntity(); budget.userId=profile.childUserId; budget.name="Dotation familiale";
            budget.monthlyLimitMilli=allocatedTotalMilli; budget.periodStart=periodStart; budget.periodEnd=periodEnd; budget.status="ACTIVE";
            insert(budget);
        } else {
            budget.monthlyLimitMilli=allocatedTotalMilli; budget.periodStart=periodStart; budget.periodEnd=periodEnd; budget.status="ACTIVE";
            update(budget);
        }
        return true;
    }

    /** Records a pocket-money transfer and its parent-side expense atomically. */
    @Transaction
    default int giveMoneyToChild(long profileId,long parentUserId,long amountMilli,String note,long occurredAt) {
        return giveMoneyToChild(profileId,parentUserId,amountMilli,note,occurredAt,"","",false);
    }

    @Transaction
    default int giveMoneyToChild(long profileId,long parentUserId,long amountMilli,String note,long occurredAt,
                                 String notificationTitle,String notificationMessage,boolean notifyChild) {
        ChildProfileEntity profile=childProfile(profileId); UserEntity parent=findUser(parentUserId);
        if(profile==null||profile.parentUserId!=parentUserId||parent==null||!("parent".equalsIgnoreCase(parent.role)||"admin".equalsIgnoreCase(parent.role))||amountMilli<=0) return -1;
        BudgetEntity parentBudget=activeBudgetForUser(parentUserId);
        if(parentBudget==null||MoneyMath.add(totalChildAllocations(parentUserId),amountMilli)>parentBudget.monthlyLimitMilli) return -2;
        UserEntity childUser=findUser(profile.childUserId); if(childUser==null) return -1;
        long categoryId=ensureChildrenCategory(parentUserId);
        insertFamilyAllocation(parentUserId,parentBudget.id,categoryId,profile.nickname,amountMilli,note,occurredAt);
        TransactionEntity expense=new TransactionEntity(); expense.userId=parentUserId; expense.budgetId=parentBudget.id;
        expense.categoryId=categoryId; expense.kind="expense"; expense.label="Dotation · "+profile.nickname;
        expense.amountMilli=amountMilli; expense.occurredAt=occurredAt; recordExpense(expense);
        long newChildTotal=profile.allocationPeriodStart==parentBudget.periodStart?MoneyMath.add(profile.allocatedTotalMilli,amountMilli):amountMilli;
        if(addChildAllocation(profileId,amountMilli,parentBudget.periodStart)==0||addChildBalance(profile.childUserId,amountMilli)==0) return -1;
        BudgetEntity childBudget=activeBudgetForUser(profile.childUserId);
        if(childBudget==null) {
            childBudget=new BudgetEntity(); childBudget.userId=profile.childUserId; childBudget.name="Dotation familiale";
            childBudget.monthlyLimitMilli=newChildTotal; childBudget.periodStart=parentBudget.periodStart; childBudget.periodEnd=parentBudget.periodEnd; childBudget.status="ACTIVE";
            insert(childBudget);
        } else { childBudget.monthlyLimitMilli=newChildTotal; update(childBudget); }
        TransactionEntity received=new TransactionEntity();received.userId=profile.childUserId;received.budgetId=childBudget.id;
        received.kind="allowance_received";received.label="";received.note=note;received.amountMilli=amountMilli;received.occurredAt=occurredAt;
        long receivedId=insert(received);
        if(notifyChild&&receivedId>0){NotificationEntity notification=new NotificationEntity();notification.userId=profile.childUserId;
            notification.title=notificationTitle;notification.message=notificationMessage;notification.kind="gift_received";
            notification.createdAt=occurredAt;notification.eventKey="allowance:"+receivedId;insertNotificationOnce(notification);}
        return 0;
    }

    /** Ledger representation of the family_allocation record using the existing offline transaction table. */
    default long insertFamilyAllocation(long parentUserId,long budgetId,long categoryId,String childName,long amountMilli,String note,long occurredAt) {
        TransactionEntity row=new TransactionEntity(); row.userId=parentUserId; row.budgetId=budgetId; row.categoryId=categoryId; row.kind="family_allocation";
        row.label=childName+" · "+(note==null||note.trim().isEmpty()?"Argent de poche":note.trim()); row.amountMilli=amountMilli; row.occurredAt=occurredAt;
        return insert(row);
    }

    @Transaction
    default long insertBudgetWithEnvelopes(BudgetEntity budget,List<BudgetLineEntity> lines) {
        if(budget==null||budget.monthlyLimitMilli<=0||budget.periodEnd<budget.periodStart) return -1L;
        if(findOverlappingBudget(budget.userId,budget.periodStart,budget.periodEnd,0L)!=null) return -2L;
        long sum=0; for(BudgetLineEntity line:lines) sum=MoneyMath.add(sum,line.limitMilli);
        if(sum>budget.monthlyLimitMilli) return -1L;
        for(BudgetLineEntity line:lines) if(categoryOwnedByUser(line.categoryId,budget.userId)==0) return -1L;
        long id=insert(budget); for(BudgetLineEntity line:lines) line.budgetId=id;
        if(!lines.isEmpty()) insertBudgetLines(lines); return id;
    }

    @Transaction
    default boolean updateBudgetWithEnvelopes(BudgetEntity budget,List<BudgetLineEntity> lines) {
        if(budget==null||budget.monthlyLimitMilli<=0||budget.periodEnd<budget.periodStart) return false;
        if(findOverlappingBudget(budget.userId,budget.periodStart,budget.periodEnd,budget.id)!=null) return false;
        long sum=0; for(BudgetLineEntity line:lines) sum=MoneyMath.add(sum,line.limitMilli);
        BudgetEntity current=findBudget(budget.id);
        if(current==null||current.userId!=budget.userId||sum>budget.monthlyLimitMilli) return false;
        for(BudgetLineEntity line:lines) if(categoryOwnedByUser(line.categoryId,budget.userId)==0) return false;
        if(update(budget)==0) return false;
        deleteBudgetLines(budget.id); for(BudgetLineEntity line:lines) line.budgetId=budget.id;
        if(!lines.isEmpty()) insertBudgetLines(lines); return true;
    }

    @Transaction
    default boolean replaceChildBudgetEnvelopes(long budgetId,long childUserId,List<BudgetLineEntity> lines) {
        BudgetEntity budget=findBudget(budgetId);
        if(budget==null || budget.userId!=childUserId) return false;
        long sum=0; for(BudgetLineEntity line:lines) sum=MoneyMath.add(sum,line.limitMilli);
        if(sum>budget.monthlyLimitMilli) return false;
        for(BudgetLineEntity line:lines) if(categoryOwnedByUser(line.categoryId,childUserId)==0) return false;
        deleteBudgetLines(budgetId); for(BudgetLineEntity line:lines) line.budgetId=budgetId;
        if(!lines.isEmpty()) insertBudgetLines(lines); return true;
    }

    /** Summarizes, awards gamification points, and recalculates the next limit atomically. */
    @Transaction
    default boolean rolloverBudget(long budgetId, long nextStart, long nextEnd, long historyStart) {
        return rolloverBudget(budgetId,nextStart,nextEnd,historyStart,System.currentTimeMillis(),"Budget closed","Points were added for this period.");
    }

    @Transaction
    default boolean rolloverBudget(long budgetId,long nextStart,long nextEnd,long historyStart,String pointsTitle,String pointsMessage) {
        return rolloverBudget(budgetId,nextStart,nextEnd,historyStart,System.currentTimeMillis(),pointsTitle,pointsMessage);
    }

    @Transaction
    default boolean rolloverBudget(long budgetId,long nextStart,long nextEnd,long historyStart,long now,String pointsTitle,String pointsMessage) {
        BudgetEntity budget = findBudget(budgetId);
        if (budget == null || !"ACTIVE".equals(budget.status) || budget.periodEnd>now) return false;
        int points = budget.spentMilli <= budget.monthlyLimitMilli ? 100 : 0;
        if (closeAndSummarize(budget.id, points) == 0) return false;
        if (points > 0) awardPointsOnce(budget.userId,"budget-close:"+budget.id+":"+budget.periodStart,
                points,"budget-period-completed",System.currentTimeMillis(),pointsTitle,pointsMessage);
        Long average = averageMonthlySpendMilli(budget.userId, historyStart);
        long nextLimit = budget.monthlyLimitMilli;
        ChildProfileEntity child=childProfileForUser(budget.userId);
        if(child!=null) nextLimit=child.allocatedTotalMilli;
        else if (average != null && average > 0) nextLimit = Math.max(1_000L, MoneyMath.threeQuarterAverage(budget.monthlyLimitMilli,average));
        UserEntity owner=findUser(budget.userId);
        if(owner!=null&&("parent".equalsIgnoreCase(owner.role)||"admin".equalsIgnoreCase(owner.role))) nextLimit=Math.max(nextLimit,totalChildAllocations(budget.userId));
        reallocateEnvelopesByActualSpend(budget.id,nextLimit);
        return startNextPeriod(budget.id, nextStart, nextEnd, nextLimit) > 0;
    }

    default void reallocateEnvelopesByActualSpend(long budgetId,long nextTotal) {
        List<BudgetLineEntity> lines=budgetLines(budgetId); if(lines.isEmpty()) return;
        boolean useActual=false; for(BudgetLineEntity line:lines) if(line.spentMilli>0) useActual=true;
        List<Long> weights=new ArrayList<>(lines.size());
        for(BudgetLineEntity line:lines) weights.add(Math.max(0L,useActual?line.spentMilli:line.limitMilli));
        List<Long> allocation=MoneyMath.allocate(nextTotal,weights);
        for(int i=0;i<lines.size();i++) { lines.get(i).limitMilli=allocation.get(i); lines.get(i).spentMilli=0; }
        updateBudgetLines(lines);
    }

    /** Stores an expense and updates its budget in one SQLite transaction. */
    @Transaction
    default long recordExpense(TransactionEntity transaction) {
        if(transaction==null)throw new SecurityException("Transaction is required");
        BudgetEntity ownerBudget=findBudget(transaction.budgetId);
        if(transaction.id!=0||transaction.amountMilli<=0||ownerBudget==null||ownerBudget.userId!=transaction.userId
                ||!("expense".equalsIgnoreCase(transaction.kind)||"donation".equalsIgnoreCase(transaction.kind))
                ||(transaction.categoryId>0&&categoryOwnedByUser(transaction.categoryId,transaction.userId)==0))
            throw new SecurityException("Transaction ownership or amount is invalid");
        MoneyMath.add(ownerBudget.spentMilli,transaction.amountMilli);
        BudgetLineEntity envelope=transaction.categoryId>0?budgetLineForCategory(transaction.budgetId,transaction.categoryId):null;
        if(envelope!=null)MoneyMath.add(envelope.spentMilli,transaction.amountMilli);
        boolean child=childProfileForUser(transaction.userId)!=null;
        UserEntity childUser=child?findUser(transaction.userId):null;
        if(child&&childUser==null)throw new SecurityException("Child account is invalid");
        long transactionId = insert(transaction);
        if(transactionId<=0||addBudgetSpend(transaction.budgetId,transaction.amountMilli)==0)throw new IllegalStateException("Budget spend was not recorded");
        if(envelope!=null&&addEnvelopeSpend(transaction.budgetId,transaction.categoryId,transaction.amountMilli)==0)throw new IllegalStateException("Envelope spend was not recorded");
        if(child&&subtractChildBalance(transaction.userId,transaction.amountMilli)==0)throw new IllegalStateException("Child balance update failed");
        return transactionId;
    }

    /** Deletes a child or personal expense once and reverses its ledger effects in the same transaction. */
    @Transaction
    default boolean deleteExpenseTransaction(long id) {
        TransactionEntity row=findTransaction(id);
        if(row==null||!("expense".equalsIgnoreCase(row.kind)||"donation".equalsIgnoreCase(row.kind))||row.amountMilli<=0)return false;
        BudgetEntity budget=findBudget(row.budgetId);
        if(budget==null||budget.userId!=row.userId||budget.spentMilli<row.amountMilli)return false;
        BudgetLineEntity envelope=row.categoryId>0?budgetLineForCategory(row.budgetId,row.categoryId):null;
        if(envelope!=null&&envelope.spentMilli<row.amountMilli)return false;
        boolean child=childProfileForUser(row.userId)!=null;
        if(child){UserEntity childUser=findUser(row.userId);if(childUser==null)return false;MoneyMath.add(childUser.balanceMilli,row.amountMilli);}
        if(deleteTransactionById(id,row.userId)==0)return false;
        if(subtractBudgetSpend(row.budgetId,row.amountMilli)==0)throw new IllegalStateException("Budget rollback failed");
        if(envelope!=null&&subtractEnvelopeSpend(row.budgetId,row.categoryId,row.amountMilli)==0)throw new IllegalStateException("Envelope rollback failed");
        if(child&&addChildBalance(row.userId,row.amountMilli)==0)throw new IllegalStateException("Child balance restoration failed");
        return true;
    }

    @Transaction
    default long recordPurchase(ProductEntity product, PurchaseEntity purchase, PurchaseItemEntity item, long budgetId) {
        if(purchase!=null&&item!=null&&purchase.totalMilli<=0&&item.quantityMilli>0&&item.unitPriceMilli>0)
            purchase.totalMilli=MoneyMath.totalForQuantity(item.unitPriceMilli,item.quantityMilli);
        BudgetEntity ownerBudget=findBudget(budgetId);
        if(product==null||purchase==null||item==null||product.id!=0||purchase.id!=0||item.id!=0||purchase.totalMilli<=0||item.quantityMilli<=0||item.unitPriceMilli<=0
                ||ownerBudget==null||ownerBudget.userId!=purchase.userId||categoryOwnedByUser(product.categoryId,purchase.userId)==0)
            throw new SecurityException("Purchase ownership or amount is invalid");
        MoneyMath.add(ownerBudget.spentMilli,purchase.totalMilli);
        BudgetLineEntity envelope=budgetLineForCategory(budgetId,product.categoryId);
        if(envelope!=null)MoneyMath.add(envelope.spentMilli,purchase.totalMilli);
        boolean child=childProfileForUser(purchase.userId)!=null;
        UserEntity childUser=child?findUser(purchase.userId):null;
        if(child&&childUser==null)throw new SecurityException("Child account is invalid");
        ProductEntity existingProduct = findMatchingProductForUser(
                purchase.userId, product.categoryId, product.name, product.brand, product.unit);
        long productId;
        if(existingProduct!=null) {
            existingProduct.currentPriceMilli=item.unitPriceMilli>0?item.unitPriceMilli:product.currentPriceMilli;
            existingProduct.updatedAt=Math.max(existingProduct.updatedAt, product.updatedAt);
            if(product.photoPath!=null&&!product.photoPath.trim().isEmpty()) existingProduct.photoPath=product.photoPath;
            update(existingProduct);
            productId=existingProduct.id;
        } else {
            productId = insert(product);
        }
        product.id=productId;
        if(purchase.totalMilli<=0) purchase.totalMilli = MoneyMath.totalForQuantity(item.unitPriceMilli,item.quantityMilli);
        long purchaseId = insert(purchase);
        purchase.id=purchaseId;
        item.productId = productId; item.purchaseId = purchaseId;
        insert(item);
        PriceHistoryEntity history=new PriceHistoryEntity(); history.productId=productId; history.priceMilli=item.unitPriceMilli;
        history.observedAt=purchase.occurredAt; history.storeName=purchase.storeName; insert(history);
        if(addBudgetSpend(budgetId, purchase.totalMilli)==0)throw new IllegalStateException("Budget spend was not recorded");
        TransactionEntity transaction = new TransactionEntity();
        transaction.userId = purchase.userId; transaction.budgetId = budgetId; transaction.kind = "purchase";
        transaction.categoryId=product.categoryId;
        transaction.label = purchase.storeName == null || purchase.storeName.isEmpty() ? product.name : purchase.storeName + " · " + product.name;
        transaction.amountMilli = purchase.totalMilli; transaction.occurredAt = purchase.occurredAt;
        insert(transaction);
        if(envelope!=null&&addEnvelopeSpend(budgetId,product.categoryId,purchase.totalMilli)==0)throw new IllegalStateException("Envelope spend was not recorded");
        if(child&&subtractChildBalance(purchase.userId,purchase.totalMilli)==0)throw new IllegalStateException("Child balance update failed");
        return purchaseId;
    }

    @Transaction
    default boolean finishOnboarding(long userId, List<SystemCategoryEntity> selections) {
        UserEntity user=findUser(userId);
        if(user==null)return false;
        // Idempotent confirmation and completion of general onboarding must not duplicate or replace selections.
        if(user.categoriesSelected)return completeGeneralOnboarding(userId)>0;
        if(selections==null||selections.isEmpty())return false;
        java.util.HashSet<Long> ids=new java.util.HashSet<>();
        for(SystemCategoryEntity selected:selections) {
            if(selected==null||selected.id<=0||!ids.add(selected.id))return false;
            SystemCategoryEntity canonical=systemCategory(selected.id);
            if(canonical==null)return false;
        }
        boolean child="child".equalsIgnoreCase(user.role);
        if(child) {
            if(childProfileForUser(userId)==null)return false;
            java.util.HashSet<Long> allowed=new java.util.HashSet<>();
            for(SystemCategoryEntity option:allowedSystemCategoriesForChild(userId))allowed.add(option.id);
            if(!allowed.containsAll(ids))return false;
        }
        for(Long id:ids) {
            SystemCategoryEntity canonical=systemCategory(id);
            CategoryEntity category=selectedCategoryForCatalog(userId,id);
            if(category==null)category=categoryForUserByName(userId,canonical.name);
            if(category==null) {
                category=new CategoryEntity();category.userId=userId;category.name=canonical.name;
                category.colorHex=canonical.colorHex;category.type=canonical.type;
                category.isEssential=canonical.protectedCategory;category.allocationPercent=canonical.recommendedPercent;
                category.id=insert(category);
            }
            UserCategoryEntity relation=new UserCategoryEntity();relation.userId=userId;relation.categoryId=category.id;
            relation.catalogId=id;relation.active=true;insertUserCategory(relation);
        }
        if(completeOnboardingAndCategorySelection(userId)==0)throw new IllegalStateException("User onboarding state could not be updated");
        if("parent".equalsIgnoreCase(user.role)||"admin".equalsIgnoreCase(user.role))ensureChildrenCategory(userId);
        return true;
    }

    @Transaction
    default boolean addCategoryWithAllocation(CategoryEntity category) {
        int safePct=Math.max(0,Math.min(100,category.allocationPercent));
        category.allocationPercent=safePct;
        BudgetEntity budget=activeBudgetForUser(category.userId);
        if(budget!=null) {
            long envelopeLimit=MoneyMath.portion(budget.monthlyLimitMilli,safePct);
            if(MoneyMath.add(allocatedEnvelopes(budget.id),envelopeLimit)>budget.monthlyLimitMilli) {
                long remaining=Math.max(0L,MoneyMath.subtract(budget.monthlyLimitMilli,allocatedEnvelopes(budget.id)));
                envelopeLimit=Math.min(envelopeLimit,remaining);
            }
            long categoryId=insert(category);
            if(categoryId<=0)return false;
            UserCategoryEntity relation=new UserCategoryEntity();relation.userId=category.userId;relation.categoryId=categoryId;relation.catalogId=null;relation.active=true;insertUserCategory(relation);
            BudgetLineEntity line=new BudgetLineEntity(); line.budgetId=budget.id; line.categoryId=categoryId;
            line.limitMilli=envelopeLimit; line.spentMilli=0;
            line.plannedPercentBps=budget.monthlyLimitMilli>0?(int)((envelopeLimit*10_000L)/budget.monthlyLimitMilli):(safePct*100);
            insert(line);
            return true;
        }
        long categoryId=insert(category);
        if(categoryId<=0)return false;
        UserCategoryEntity relation=new UserCategoryEntity();relation.userId=category.userId;relation.categoryId=categoryId;relation.catalogId=null;relation.active=true;insertUserCategory(relation);
        return true;
    }

    @Transaction
    default boolean updateUserCategorySelections(long userId, List<Long> selectedCatalogIds,
                                                 List<Long> selectedCustomCategoryIds, long todayStartMillis) {
        UserEntity user=findUser(userId);
        if(user==null) return false;
        List<Long> safeCatalogIds=selectedCatalogIds==null?java.util.Collections.emptyList():selectedCatalogIds;
        List<Long> safeCustomIds=selectedCustomCategoryIds==null?java.util.Collections.emptyList():selectedCustomCategoryIds;
        java.util.LinkedHashSet<Long> catalogSet=new java.util.LinkedHashSet<>(safeCatalogIds);
        java.util.LinkedHashSet<Long> customSet=new java.util.LinkedHashSet<>(safeCustomIds);
        if(catalogSet.isEmpty() && customSet.isEmpty()) return false;

        boolean child="child".equalsIgnoreCase(user.role);
        if(child) {
            if(childProfileForUser(userId)==null) return false;
            java.util.HashSet<Long> allowed=new java.util.HashSet<>();
            for(SystemCategoryEntity option:allowedSystemCategoriesForChild(userId)) allowed.add(option.id);
            if(!allowed.containsAll(catalogSet)) return false;
        }

        List<UserCategoryEntity> existingLinks=userCategoryLinks(userId);
        java.util.Map<Long,UserCategoryEntity> linkByCategoryId=new java.util.HashMap<>();
        java.util.HashSet<Long> previouslyActiveCategoryIds=new java.util.HashSet<>();
        for(UserCategoryEntity link:existingLinks) {
            linkByCategoryId.put(link.categoryId,link);
            if(link.active) previouslyActiveCategoryIds.add(link.categoryId);
        }

        java.util.LinkedHashSet<Long> targetActiveCategoryIds=new java.util.LinkedHashSet<>();
        java.util.Map<Long,Integer> weightByCategoryId=new java.util.HashMap<>();

        for(Long catalogId:catalogSet) {
            if(catalogId==null||catalogId<=0) return false;
            SystemCategoryEntity canonical=systemCategory(catalogId);
            if(canonical==null) return false;
            CategoryEntity category=selectedCategoryForCatalog(userId,catalogId);
            if(category==null) category=categoryForUserByName(userId,canonical.name);
            if(category==null) {
                category=new CategoryEntity();
                category.userId=userId;category.name=canonical.name;
                category.colorHex=canonical.colorHex;category.type=canonical.type;
                category.isEssential=canonical.protectedCategory;
                category.allocationPercent=canonical.recommendedPercent;
                category.id=insert(category);
            }
            UserCategoryEntity existing=linkByCategoryId.get(category.id);
            if(existing!=null) updateUserCategoryLink(existing.id,true,catalogId);
            else {
                UserCategoryEntity rel=new UserCategoryEntity();
                rel.userId=userId;rel.categoryId=category.id;rel.catalogId=catalogId;rel.active=true;
                insertUserCategory(rel);
            }
            targetActiveCategoryIds.add(category.id);
            weightByCategoryId.put(category.id,category.allocationPercent>0?category.allocationPercent:Math.max(1,canonical.recommendedPercent));
        }

        for(Long customCatId:customSet) {
            if(customCatId==null||customCatId<=0) continue;
            CategoryEntity custom=findCategoryForUser(customCatId,userId);
            if(custom==null) continue;
            UserCategoryEntity existing=linkByCategoryId.get(custom.id);
            if(existing!=null) updateUserCategoryLink(existing.id,true,existing.catalogId);
            else {
                UserCategoryEntity rel=new UserCategoryEntity();
                rel.userId=userId;rel.categoryId=custom.id;rel.catalogId=null;rel.active=true;
                insertUserCategory(rel);
            }
            targetActiveCategoryIds.add(custom.id);
            weightByCategoryId.put(custom.id,custom.allocationPercent>0?custom.allocationPercent:5);
        }

        java.util.HashSet<Long> deactivatedCategoryIds=new java.util.HashSet<>();
        for(UserCategoryEntity link:existingLinks) {
            if(link.active && !targetActiveCategoryIds.contains(link.categoryId)) {
                updateUserCategoryLink(link.id,false,link.catalogId);
                deactivatedCategoryIds.add(link.categoryId);
            }
        }

        ArrayList<Long> newlyAddedCategoryIds=new ArrayList<>();
        for(Long categoryId:targetActiveCategoryIds) {
            if(!previouslyActiveCategoryIds.contains(categoryId)) newlyAddedCategoryIds.add(categoryId);
        }

        // Update active/future non-closed budgets without altering historical budgets or transactions.
        List<BudgetEntity> budgets=budgetsForUser(userId);
        for(BudgetEntity budget:budgets) {
            if("CLOSED".equalsIgnoreCase(budget.status) || budget.periodEnd < todayStartMillis) continue;
            long safeEnd=budget.periodEnd==Long.MAX_VALUE?Long.MAX_VALUE:budget.periodEnd+1;
            List<BudgetLineEntity> lines=budgetLines(budget.id);
            java.util.HashSet<Long> keptLineCategories=new java.util.HashSet<>();
            long keptAllocatedSum=0L;
            for(BudgetLineEntity line:lines) {
                if(deactivatedCategoryIds.contains(line.categoryId)) {
                    long txSpent=categoryTotalSpendBetween(userId,line.categoryId,budget.periodStart,safeEnd);
                    if(line.spentMilli<=0 && txSpent<=0) {
                        deleteBudgetLine(line.id);
                        continue;
                    }
                }
                keptLineCategories.add(line.categoryId);
                keptAllocatedSum=MoneyMath.add(keptAllocatedSum,line.limitMilli);
            }

            ArrayList<Long> missingCatIds=new ArrayList<>();
            for(Long catId:targetActiveCategoryIds) {
                if(!keptLineCategories.contains(catId) && newlyAddedCategoryIds.contains(catId)) {
                    missingCatIds.add(catId);
                }
            }
            // Also add lines for any active category missing in a newly created budget when newlyAdded wasn't toggled this call
            if(missingCatIds.isEmpty()) {
                for(Long catId:targetActiveCategoryIds) {
                    if(!keptLineCategories.contains(catId)) missingCatIds.add(catId);
                }
            }
            if(!missingCatIds.isEmpty()) {
                long headroom=Math.max(0L,MoneyMath.subtract(budget.monthlyLimitMilli,keptAllocatedSum));
                int weightSum=0;
                ArrayList<Long> weights=new ArrayList<>(missingCatIds.size());
                for(Long catId:missingCatIds) {
                    int w=Math.max(1,weightByCategoryId.containsKey(catId)?weightByCategoryId.get(catId):5);
                    weightSum+=w;
                    weights.add((long)w);
                }
                long desiredPortion=MoneyMath.portion(budget.monthlyLimitMilli,Math.min(100,weightSum));
                long toDistribute=headroom>0?(desiredPortion>0?Math.min(headroom,desiredPortion):headroom):0L;
                List<Long> distributed=toDistribute>0?MoneyMath.allocate(toDistribute,weights):java.util.Collections.nCopies(missingCatIds.size(),0L);
                for(int i=0;i<missingCatIds.size();i++) {
                    long catId=missingCatIds.get(i);
                    long limitMilli=distributed.get(i);
                    BudgetLineEntity newLine=new BudgetLineEntity();
                    newLine.budgetId=budget.id;
                    newLine.categoryId=catId;
                    newLine.limitMilli=limitMilli;
                    newLine.spentMilli=0L;
                    newLine.plannedPercentBps=budget.monthlyLimitMilli>0?(int)Math.min(10_000L,(limitMilli*10_000L)/budget.monthlyLimitMilli):0;
                    insert(newLine);
                }
            }
        }
        markCategoriesSelected(userId);
        return true;
    }

    @Transaction
    default boolean deleteCustomCategory(long userId, long categoryId) {
        CategoryEntity category=findCategoryForUser(categoryId,userId);
        if(category==null||countCatalogLinkForCategory(categoryId)>0||"Enfants".equalsIgnoreCase(category.name)) return false;
        if(countProductsInCategory(categoryId)>0||countTransactionsInCategory(categoryId)>0) return false;
        return deleteCategoryRow(categoryId,userId)>0;
    }
}
