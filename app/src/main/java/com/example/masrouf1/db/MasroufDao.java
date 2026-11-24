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
    @Insert(onConflict = OnConflictStrategy.REPLACE) long insert(PriceHistoryEntity row);
    @Insert(onConflict = OnConflictStrategy.REPLACE) long[] insertBudgetLines(List<BudgetLineEntity> rows);
    @Update int update(BudgetEntity row);
    @Update int update(ChildProfileEntity row);

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1") UserEntity findUser(long id);
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1") LiveData<UserEntity> observeUser(long id);
    @Query("SELECT * FROM users WHERE email = :email COLLATE NOCASE LIMIT 1") UserEntity findUserByEmail(String email);
    @Query("UPDATE users SET email = :email WHERE id = :userId") int updateEmail(long userId,String email);
    @Query("UPDATE users SET onboarded = :onboarded WHERE id = :userId") int updateOnboarding(long userId, boolean onboarded);
    @Query("SELECT * FROM budgets WHERE userId = :userId AND status = 'ACTIVE' ORDER BY periodStart DESC LIMIT 1") BudgetEntity activeBudgetForUser(long userId);
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name COLLATE NOCASE") List<CategoryEntity> categoriesForUser(long userId);
    @Query("SELECT * FROM categories WHERE userId = :userId ORDER BY name COLLATE NOCASE") LiveData<List<CategoryEntity>> observeCategoriesForUser(long userId);
    @Query("SELECT * FROM budget_lines WHERE budgetId = :budgetId ORDER BY categoryId") List<BudgetLineEntity> budgetLines(long budgetId);
    @Query("SELECT COALESCE(SUM(limitMilli),0) FROM budget_lines WHERE budgetId = :budgetId") long allocatedEnvelopes(long budgetId);
    @Query("DELETE FROM budget_lines WHERE budgetId = :budgetId") int deleteBudgetLines(long budgetId);
    @Query("SELECT * FROM child_profiles WHERE childUserId = :childUserId LIMIT 1") ChildProfileEntity childProfileForUser(long childUserId);
    @Query("SELECT COALESCE(SUM(allocatedTotalMilli),0) FROM child_profiles WHERE parentUserId = :parentUserId") long totalChildAllocations(long parentUserId);
    @Query("SELECT COUNT(*) FROM child_profiles WHERE parentUserId = :parentUserId") int countChildren(long parentUserId);
    @Query("SELECT COUNT(*) FROM notifications WHERE userId = :userId AND read = 0") int unreadNotificationCount(long userId);
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY createdAt DESC") LiveData<List<NotificationEntity>> observeNotificationsForUser(long userId);
    @Query("UPDATE notifications SET read = 1 WHERE id = :notificationId AND userId = :userId") int markNotificationRead(long notificationId,long userId);
    @Query("SELECT * FROM categories WHERE userId = :userId AND name = 'Enfants' COLLATE NOCASE LIMIT 1") CategoryEntity childrenCategory(long userId);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND categoryId = :categoryId AND kind IN ('expense','donation') AND occurredAt >= :from AND occurredAt <= :to") long categoryExpenseBetween(long userId,long categoryId,long from,long to);
    @Query("SELECT * FROM budget_lines WHERE budgetId = :budgetId AND categoryId = :categoryId LIMIT 1") BudgetLineEntity budgetLineForCategory(long budgetId,long categoryId);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND kind IN ('expense','donation','purchase') AND occurredAt >= :from AND occurredAt <= :to AND categoryId NOT IN (SELECT id FROM categories WHERE userId = :userId AND name = 'Enfants' COLLATE NOCASE)") long personalSpendingBetween(long userId,long from,long to);
    @Query("SELECT budgets.periodStart, budgets.monthlyLimitMilli AS allocatedMilli, budgets.spentMilli, budgets.pointsAwarded FROM budgets INNER JOIN child_profiles ON child_profiles.childUserId = budgets.userId WHERE budgets.userId = :childUserId AND child_profiles.parentUserId = :parentUserId ORDER BY budgets.periodStart DESC LIMIT 4") List<BudgetHistoryRow> childBudgetHistory(long childUserId,long parentUserId);
    @Query("SELECT users.email FROM users INNER JOIN child_profiles ON child_profiles.childUserId = users.id WHERE child_profiles.id = :profileId AND child_profiles.parentUserId = :parentId LIMIT 1") String childEmailForProfile(long profileId,long parentId);
    @Query("UPDATE users SET passwordHash = :passwordHash WHERE id = (SELECT childUserId FROM child_profiles WHERE id = :profileId AND parentUserId = :parentId)") int resetChildPassword(long profileId,long parentId,String passwordHash);
    @Query("UPDATE child_profiles SET allocatedTotalMilli = allocatedTotalMilli + :amountMilli WHERE id = :profileId") int addChildAllocation(long profileId,long amountMilli);
    @Query("UPDATE users SET balanceMilli = balanceMilli + :amountMilli WHERE id = :childUserId") int addChildBalance(long childUserId,long amountMilli);
    @Query("UPDATE users SET balanceMilli = balanceMilli - :amountMilli WHERE id = :childUserId") int subtractChildBalance(long childUserId,long amountMilli);
    @Query("SELECT child_profiles.parentPhone FROM child_profiles WHERE child_profiles.childUserId = :childUserId LIMIT 1") String parentPhoneForChild(long childUserId);
    @Query("SELECT child_profiles.id AS childProfileId, users.id AS childUserId, child_profiles.nickname AS nickname, COALESCE(NULLIF(child_profiles.allocatedTotalMilli,0),budgets.monthlyLimitMilli,0) AS allocatedTotalMilli, COALESCE(SUM(money_transactions.amountMilli),0) AS spentMilli, users.balanceMilli AS balanceMilli FROM child_profiles INNER JOIN users ON users.id = child_profiles.childUserId LEFT JOIN budgets ON budgets.userId = users.id AND budgets.status = 'ACTIVE' LEFT JOIN money_transactions ON money_transactions.userId = users.id AND money_transactions.kind IN ('expense','donation','purchase') AND money_transactions.occurredAt >= budgets.periodStart AND money_transactions.occurredAt <= budgets.periodEnd WHERE child_profiles.parentUserId = :parentUserId AND EXISTS (SELECT 1 FROM users parent WHERE parent.id = :parentUserId AND lower(parent.role) IN ('parent','admin')) GROUP BY child_profiles.id, users.id, budgets.id ORDER BY child_profiles.nickname COLLATE NOCASE")
    List<ChildSpendingSummary> childSpendingSummaries(long parentUserId);
    @Query("SELECT categoryId, COALESCE(SUM(amountMilli),0) AS amountMilli FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt < :to AND kind IN ('expense','donation','purchase') AND categoryId > 0 GROUP BY categoryId")
    List<CategorySpendRow> categorySpendingBetween(long userId,long from,long to);
    @Query("SELECT categoryId, COALESCE(SUM(amountMilli),0) AS amountMilli FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt < :to AND kind IN ('expense','donation','purchase') AND categoryId > 0 GROUP BY categoryId")
    LiveData<List<CategorySpendRow>> observeCategorySpendingBetween(long userId,long from,long to);
    @Query("SELECT COALESCE(SUM(amountMilli),0) FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt < :to AND kind IN ('expense','donation','purchase')")
    long totalSpendingBetween(long userId,long from,long to);
    @Query("SELECT * FROM child_profiles WHERE id = :profileId LIMIT 1") ChildProfileEntity childProfile(long profileId);
    @Query("UPDATE child_profiles SET allocatedTotalMilli = :allocatedTotalMilli WHERE id = :profileId") int setChildAllocation(long profileId,long allocatedTotalMilli);
    @Query("SELECT COALESCE(SUM(allocationPercent), 0) FROM categories WHERE userId = :userId AND type = :type") int allocatedPercent(long userId, String type);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId ORDER BY occurredAt DESC LIMIT 5") List<TransactionRow> recentTransactions(long userId);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt <= :to ORDER BY occurredAt DESC") List<TransactionRow> transactionsBetween(long userId,long from,long to);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId AND occurredAt >= :from AND occurredAt <= :to ORDER BY occurredAt DESC") LiveData<List<TransactionRow>> observeTransactionsBetween(long userId,long from,long to);
    @Query("SELECT id, label AS title, amountMilli, occurredAt, kind FROM money_transactions WHERE userId = :userId AND kind IN ('expense','donation','purchase') ORDER BY occurredAt DESC LIMIT 5") List<TransactionRow> recentSpendingTransactions(long userId);
    @Query("SELECT * FROM budgets WHERE id = :id LIMIT 1") BudgetEntity findBudget(long id);
    @Query("SELECT products.* FROM products INNER JOIN categories ON categories.id = products.categoryId WHERE products.id = :id AND categories.userId = :userId LIMIT 1") ProductEntity findProductForUser(long id,long userId);
    @Query("SELECT products.* FROM products INNER JOIN categories ON categories.id = products.categoryId WHERE categories.userId = :userId ORDER BY products.updatedAt DESC, products.id DESC") LiveData<List<ProductEntity>> observeProductsForUser(long userId);
    @Query("SELECT products.* FROM products INNER JOIN categories ON categories.id = products.categoryId WHERE categories.userId = :userId ORDER BY products.updatedAt DESC, products.id DESC") List<ProductEntity> productsForUser(long userId);
    @Query("SELECT price_history.* FROM price_history INNER JOIN products ON products.id = price_history.productId INNER JOIN categories ON categories.id = products.categoryId WHERE price_history.productId = :productId AND categories.userId = :userId ORDER BY price_history.observedAt") List<PriceHistoryEntity> priceHistory(long userId,long productId);
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
    @Query("UPDATE budgets SET status = 'CLOSED', lastMonthSummaryMilli = spentMilli, pointsAwarded = pointsAwarded + :points WHERE id = :budgetId")
    int closeAndSummarize(long budgetId, int points);
    @Query("UPDATE users SET points = points + :points WHERE id = :userId") int addPoints(long userId, int points);
    @Query("UPDATE budgets SET spentMilli = spentMilli + :amountMilli WHERE id = :budgetId") int addBudgetSpend(long budgetId, long amountMilli);
    @Query("UPDATE budget_lines SET spentMilli = spentMilli + :amountMilli WHERE budgetId = :budgetId AND categoryId = :categoryId") int addEnvelopeSpend(long budgetId,long categoryId,long amountMilli);
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
        if(profile.allocatedTotalMilli>0) {
            BudgetEntity parentBudget=activeBudgetForUser(profile.parentUserId);
            if(parentBudget==null||MoneyMath.add(totalChildAllocations(profile.parentUserId),profile.allocatedTotalMilli)>parentBudget.monthlyLimitMilli) throw new IllegalArgumentException("child allocation exceeds household budget");
            initialBudget.periodStart=parentBudget.periodStart; initialBudget.periodEnd=parentBudget.periodEnd;
        }
        long childId=insertChildUser(child); profile.childUserId=childId; insert(profile);
        for(CategoryEntity category:categories) { category.userId=childId; insert(category); }
        if(initialBudget!=null) { initialBudget.userId=childId; insert(initialBudget); }
        return childId;
    }

    @Transaction
    default boolean updateChildFamilyAllocation(long profileId,long allocatedTotalMilli,long periodStart,long periodEnd) {
        ChildProfileEntity profile=childProfile(profileId); if(profile==null || allocatedTotalMilli<=0) return false;
        BudgetEntity parentBudget=activeBudgetForUser(profile.parentUserId);
        if(parentBudget==null||MoneyMath.add(MoneyMath.subtract(totalChildAllocations(profile.parentUserId),profile.allocatedTotalMilli),allocatedTotalMilli)>parentBudget.monthlyLimitMilli) return false;
        BudgetEntity budget=activeBudgetForUser(profile.childUserId);
        if(budget!=null && allocatedEnvelopes(budget.id)>allocatedTotalMilli) return false;
        if(setChildAllocation(profileId,allocatedTotalMilli)==0) return false;
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
        if(addChildAllocation(profileId,amountMilli)==0||addChildBalance(profile.childUserId,amountMilli)==0) return -1;
        long newChildTotal=MoneyMath.add(profile.allocatedTotalMilli,amountMilli);
        BudgetEntity childBudget=activeBudgetForUser(profile.childUserId);
        if(childBudget==null) {
            childBudget=new BudgetEntity(); childBudget.userId=profile.childUserId; childBudget.name="Dotation familiale";
            childBudget.monthlyLimitMilli=newChildTotal; childBudget.periodStart=parentBudget.periodStart; childBudget.periodEnd=parentBudget.periodEnd; childBudget.status="ACTIVE";
            insert(childBudget);
        } else { childBudget.monthlyLimitMilli=newChildTotal; update(childBudget); }
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
        long sum=0; for(BudgetLineEntity line:lines) sum=MoneyMath.add(sum,line.limitMilli);
        if(sum>budget.monthlyLimitMilli) return -1L;
        UserEntity owner=findUser(budget.userId);
        if(owner!=null&&("parent".equalsIgnoreCase(owner.role)||"admin".equalsIgnoreCase(owner.role))&&totalChildAllocations(owner.id)>budget.monthlyLimitMilli) return -1L;
        for(BudgetLineEntity line:lines) if(categoryOwnedByUser(line.categoryId,budget.userId)==0) return -1L;
        long id=insert(budget); for(BudgetLineEntity line:lines) line.budgetId=id;
        if(!lines.isEmpty()) insertBudgetLines(lines); return id;
    }

    @Transaction
    default boolean updateBudgetWithEnvelopes(BudgetEntity budget,List<BudgetLineEntity> lines) {
        long sum=0; for(BudgetLineEntity line:lines) sum=MoneyMath.add(sum,line.limitMilli);
        BudgetEntity current=findBudget(budget.id);
        if(current==null||current.userId!=budget.userId||sum>budget.monthlyLimitMilli) return false;
        UserEntity owner=findUser(budget.userId);
        if(owner!=null&&("parent".equalsIgnoreCase(owner.role)||"admin".equalsIgnoreCase(owner.role))&&totalChildAllocations(owner.id)>budget.monthlyLimitMilli) return false;
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
        BudgetEntity budget = findBudget(budgetId);
        if (budget == null || !"ACTIVE".equals(budget.status)) return false;
        int points = budget.spentMilli <= budget.monthlyLimitMilli ? 100 : 0;
        if (closeAndSummarize(budget.id, points) == 0) return false;
        if (points > 0) {
            addPoints(budget.userId, points);
            NotificationEntity log=new NotificationEntity(); log.userId=budget.userId; log.title="100 points";
            log.message="Budget respecté — points attribués à la clôture."; log.kind="points_log"; log.createdAt=System.currentTimeMillis(); insert(log);
        }
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
        BudgetEntity ownerBudget=findBudget(transaction.budgetId);
        if(transaction.amountMilli<=0||ownerBudget==null||ownerBudget.userId!=transaction.userId
                ||(transaction.categoryId>0&&categoryOwnedByUser(transaction.categoryId,transaction.userId)==0))
            throw new SecurityException("Transaction ownership or amount is invalid");
        long transactionId = insert(transaction);
        if ("expense".equalsIgnoreCase(transaction.kind) || "donation".equalsIgnoreCase(transaction.kind)) {
            addBudgetSpend(transaction.budgetId, transaction.amountMilli);
            if(transaction.categoryId>0) addEnvelopeSpend(transaction.budgetId,transaction.categoryId,transaction.amountMilli);
            if(childProfileForUser(transaction.userId)!=null) subtractChildBalance(transaction.userId,transaction.amountMilli);
        }
        return transactionId;
    }

    @Transaction
    default long recordPurchase(ProductEntity product, PurchaseEntity purchase, PurchaseItemEntity item, long budgetId) {
        if(purchase!=null&&item!=null&&purchase.totalMilli<=0&&item.quantityMilli>0&&item.unitPriceMilli>0)
            purchase.totalMilli=MoneyMath.totalForQuantity(item.unitPriceMilli,item.quantityMilli);
        BudgetEntity ownerBudget=findBudget(budgetId);
        if(product==null||purchase==null||item==null||purchase.totalMilli<=0||item.quantityMilli<=0||item.unitPriceMilli<=0
                ||ownerBudget==null||ownerBudget.userId!=purchase.userId||categoryOwnedByUser(product.categoryId,purchase.userId)==0)
            throw new SecurityException("Purchase ownership or amount is invalid");
        long productId = insert(product);
        product.id=productId;
        if(purchase.totalMilli<=0) purchase.totalMilli = MoneyMath.totalForQuantity(item.unitPriceMilli,item.quantityMilli);
        long purchaseId = insert(purchase);
        purchase.id=purchaseId;
        item.productId = productId; item.purchaseId = purchaseId;
        insert(item);
        PriceHistoryEntity history=new PriceHistoryEntity(); history.productId=productId; history.priceMilli=item.unitPriceMilli;
        history.observedAt=purchase.occurredAt; history.storeName=purchase.storeName; insert(history);
        addBudgetSpend(budgetId, purchase.totalMilli);
        TransactionEntity transaction = new TransactionEntity();
        transaction.userId = purchase.userId; transaction.budgetId = budgetId; transaction.kind = "purchase";
        transaction.categoryId=product.categoryId;
        transaction.label = purchase.storeName == null || purchase.storeName.isEmpty() ? product.name : purchase.storeName + " · " + product.name;
        transaction.amountMilli = purchase.totalMilli; transaction.occurredAt = purchase.occurredAt;
        insert(transaction);
        if(product.categoryId>0) addEnvelopeSpend(budgetId,product.categoryId,purchase.totalMilli);
        if(childProfileForUser(purchase.userId)!=null) subtractChildBalance(purchase.userId,purchase.totalMilli);
        return purchaseId;
    }

    @Transaction
    default boolean finishOnboarding(long userId, List<CategoryEntity> categories) {
        UserEntity user=findUser(userId);
        if(user==null)return false;
        for (CategoryEntity category : categories) {
            if(category.userId!=userId)return false;
            insert(category);
        }
        if(user!=null&&("parent".equalsIgnoreCase(user.role)||"admin".equalsIgnoreCase(user.role))) ensureChildrenCategory(userId);
        return updateOnboarding(userId, true) > 0;
    }

    @Transaction
    default boolean addCategoryWithAllocation(CategoryEntity category) {
        if(category.allocationPercent<0||category.allocationPercent>100)return false;
        int current = allocatedPercent(category.userId, category.type);
        if (Math.addExact(current,category.allocationPercent) > 100) return false;
        BudgetEntity budget=activeBudgetForUser(category.userId);
        if(budget==null) return false;
        long envelopeLimit=MoneyMath.portion(budget.monthlyLimitMilli,category.allocationPercent);
        if(MoneyMath.add(allocatedEnvelopes(budget.id),envelopeLimit)>budget.monthlyLimitMilli) return false;
        long categoryId=insert(category);
        BudgetLineEntity line=new BudgetLineEntity(); line.budgetId=budget.id; line.categoryId=categoryId;
        line.limitMilli=envelopeLimit; line.spentMilli=0; insert(line);
        return categoryId > 0;
    }
}
