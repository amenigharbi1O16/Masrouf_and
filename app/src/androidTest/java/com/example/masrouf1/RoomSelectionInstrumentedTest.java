package com.example.masrouf1;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.masrouf1.db.MasroufDao;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Isolated Room tests; the named test database is removed without touching masrouf.db. */
@RunWith(AndroidJUnit4.class)
public final class RoomSelectionInstrumentedTest {
    private static final String TEST_DATABASE="masrouf-room-selection-test.db";
    private Context context;
    private MasroufDatabase database;
    private MasroufDao dao;
    private UserEntity parent,user,child;

    @Before public void setUp() {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(TEST_DATABASE);
        openDatabase();
        for(SystemCategoryEntity category:SystemCategoryCatalog.defaults())dao.insertSystemCategory(category);
        parent=createUser("parent.room@gmail.com","parent",0L,1L);
        user=createUser("user.room@gmail.com","user",0L,2L);
        child=createUser("child.room@gmail.com","child",5_000L,parent.householdId);
        ChildProfileEntity profile=new ChildProfileEntity();profile.parentUserId=parent.id;profile.childUserId=child.id;
        profile.nickname="Child";profile.createdAt=1L;dao.insert(profile);
    }

    private void openDatabase() {
        database=Room.databaseBuilder(context,MasroufDatabase.class,TEST_DATABASE).allowMainThreadQueries().build();
        dao=database.masroufDao();
    }

    @After public void tearDown() {
        if(database!=null)database.close();
        if(context!=null)context.deleteDatabase(TEST_DATABASE);
    }

    @Test public void catalogIsNotSelectionAndConfirmationPersistsIndependently() {
        assertEquals(16,dao.systemCategories().size());
        assertFalse(dao.findUser(parent.id).categoriesSelected);
        assertTrue(dao.selectedCatalogIds(parent.id).isEmpty());

        database.close();openDatabase();
        assertFalse(dao.findUser(parent.id).categoriesSelected);
        assertTrue(dao.selectedCatalogIds(parent.id).isEmpty());

        SystemCategoryEntity food=dao.systemCategoryByKey("food");
        SystemCategoryEntity other=dao.systemCategoryByKey("other");
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(food,other)));
        assertTrue(dao.finishOnboarding(user.id,Arrays.asList(other)));
        assertEquals(Arrays.asList(food.id,other.id),dao.selectedCatalogIds(parent.id));
        assertEquals(Arrays.asList(other.id),dao.selectedCatalogIds(user.id));

        database.close();openDatabase();
        assertTrue(dao.findUser(parent.id).categoriesSelected);
        assertEquals(Arrays.asList(food.id,other.id),dao.selectedCatalogIds(parent.id));
        assertEquals(Arrays.asList(other.id),dao.selectedCatalogIds(user.id));
    }

    @Test public void childChoicesAreLimitedToParentAndFailedOrRepeatedConfirmationIsSafe() {
        SystemCategoryEntity food=dao.systemCategoryByKey("food");
        SystemCategoryEntity other=dao.systemCategoryByKey("other");
        SystemCategoryEntity health=dao.systemCategoryByKey("health");
        assertFalse(dao.finishOnboarding(parent.id,java.util.Collections.emptyList()));
        assertFalse(dao.findUser(parent.id).categoriesSelected);
        assertTrue(dao.selectedCatalogIds(parent.id).isEmpty());

        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(food,other)));
        List<SystemCategoryEntity> allowed=dao.allowedSystemCategoriesForChild(child.id);
        assertTrue(allowed.stream().anyMatch(row->row.id==food.id));
        assertTrue(allowed.stream().anyMatch(row->row.id==other.id));
        assertFalse(allowed.stream().anyMatch(row->row.id==health.id));
        assertFalse(dao.finishOnboarding(child.id,Arrays.asList(health,other)));
        assertFalse(dao.findUser(child.id).categoriesSelected);
        assertTrue(dao.selectedCatalogIds(child.id).isEmpty());
        assertTrue(dao.finishOnboarding(child.id,Arrays.asList(food,other)));

        List<Long> saved=dao.selectedCatalogIds(parent.id);
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(health,other)));
        assertEquals(saved,dao.selectedCatalogIds(parent.id));
    }

    @Test public void expenseAndDeletionUpdateBudgetEnvelopeAndChildBalanceAtomically() {
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(dao.systemCategoryByKey("food"),dao.systemCategoryByKey("other"))));
        assertTrue(dao.finishOnboarding(child.id,Arrays.asList(dao.systemCategoryByKey("food"),dao.systemCategoryByKey("other"))));
        CategoryEntity food=dao.selectedCategoryForCatalog(child.id,dao.systemCategoryByKey("food").id);
        assertNotNull(food);

        BudgetEntity budget=new BudgetEntity();budget.userId=child.id;budget.name="Test";budget.monthlyLimitMilli=5_000L;
        budget.status="ACTIVE";budget.periodStart=1L;budget.periodEnd=Long.MAX_VALUE;budget.id=dao.insert(budget);
        BudgetLineEntity line=new BudgetLineEntity();line.budgetId=budget.id;line.categoryId=food.id;line.limitMilli=5_000L;dao.insert(line);
        TransactionEntity expense=new TransactionEntity();expense.userId=child.id;expense.budgetId=budget.id;
        expense.categoryId=food.id;expense.kind="expense";expense.label="test";expense.amountMilli=2_500L;expense.occurredAt=2L;
        long transactionId=dao.recordExpense(expense);

        assertEquals(2_500L,dao.findBudget(budget.id).spentMilli);
        assertEquals(2_500L,dao.budgetLineForCategory(budget.id,food.id).spentMilli);
        assertEquals(2_500L,dao.findUser(child.id).balanceMilli);
        assertTrue(dao.deleteExpenseTransaction(transactionId));
        assertFalse(dao.deleteExpenseTransaction(transactionId));
        assertEquals(0L,dao.findBudget(budget.id).spentMilli);
        assertEquals(0L,dao.budgetLineForCategory(budget.id,food.id).spentMilli);
        assertEquals(5_000L,dao.findUser(child.id).balanceMilli);
    }

    @Test public void purchasePersistsProductItemPriceHistoryAndLedgerTogether() {
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(dao.systemCategoryByKey("food"),dao.systemCategoryByKey("other"))));
        assertTrue(dao.finishOnboarding(child.id,Arrays.asList(dao.systemCategoryByKey("food"),dao.systemCategoryByKey("other"))));
        CategoryEntity food=dao.selectedCategoryForCatalog(child.id,dao.systemCategoryByKey("food").id);
        BudgetEntity budget=new BudgetEntity();budget.userId=child.id;budget.name="Test";budget.monthlyLimitMilli=5_000L;
        budget.status="ACTIVE";budget.periodStart=1L;budget.periodEnd=Long.MAX_VALUE;budget.id=dao.insert(budget);
        BudgetLineEntity line=new BudgetLineEntity();line.budgetId=budget.id;line.categoryId=food.id;line.limitMilli=5_000L;dao.insert(line);

        ProductEntity product=new ProductEntity();product.categoryId=food.id;product.name="Lait";product.unit="L";
        product.currentPriceMilli=2_500L;product.currency="TND";product.updatedAt=2L;
        PurchaseEntity purchase=new PurchaseEntity();purchase.userId=child.id;purchase.occurredAt=2L;purchase.totalMilli=2_500L;purchase.storeName="Marché";
        PurchaseItemEntity item=new PurchaseItemEntity();item.description="Lait";item.quantityMilli=1_000;item.unit="L";item.unitPriceMilli=2_500L;
        long purchaseId=dao.recordPurchase(product,purchase,item,budget.id);

        assertTrue(purchaseId>0);assertEquals(2_500L,dao.findBudget(budget.id).spentMilli);
        assertEquals(2_500L,dao.budgetLineForCategory(budget.id,food.id).spentMilli);
        assertEquals(2_500L,dao.findUser(child.id).balanceMilli);
        List<PriceHistoryEntity> history=dao.priceHistory(child.id,product.id);
        assertEquals(1,history.size());assertEquals(2_500L,history.get(0).priceMilli);
        assertEquals(1,dao.recentSpendingTransactions(child.id).size());
    }

    @Test public void budgetOverlapRejectedAndPlannedPercentBpsPersistsWithoutForcing100Percent() {
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(dao.systemCategoryByKey("food"),dao.systemCategoryByKey("bills"))));
        CategoryEntity food=dao.selectedCategoryForCatalog(parent.id,dao.systemCategoryByKey("food").id);
        CategoryEntity bills=dao.selectedCategoryForCatalog(parent.id,dao.systemCategoryByKey("bills").id);

        long novStart=BudgetPeriodMath.parseIsoDateStartMillis("2026-11-01");
        long novEnd=BudgetPeriodMath.parseIsoDateEndMillis("2026-11-30");
        BudgetEntity nov=new BudgetEntity();nov.userId=parent.id;nov.name="Nov";nov.monthlyLimitMilli=1_000_000L;
        nov.periodStart=novStart;nov.periodEnd=novEnd;nov.status="ACTIVE";

        BudgetLineEntity foodLine=new BudgetLineEntity();foodLine.categoryId=food.id;foodLine.limitMilli=250_000L;foodLine.plannedPercentBps=2500;
        BudgetLineEntity billsLine=new BudgetLineEntity();billsLine.categoryId=bills.id;billsLine.limitMilli=150_000L;billsLine.plannedPercentBps=1500;
        long novId=dao.insertBudgetWithEnvelopes(nov,Arrays.asList(foodLine,billsLine));
        assertTrue(novId>0L);

        List<BudgetLineEntity> loaded=dao.budgetLines(novId);
        assertEquals(2,loaded.size());
        assertEquals(2500,dao.budgetLineForCategory(novId,food.id).plannedPercentBps);
        assertEquals(1500,dao.budgetLineForCategory(novId,bills.id).plannedPercentBps);

        BudgetEntity overlapping=new BudgetEntity();overlapping.userId=parent.id;overlapping.name="Overlap";
        overlapping.monthlyLimitMilli=500_000L;
        overlapping.periodStart=BudgetPeriodMath.parseIsoDateStartMillis("2026-11-15");
        overlapping.periodEnd=BudgetPeriodMath.parseIsoDateEndMillis("2026-12-10");
        overlapping.status="ACTIVE";
        assertEquals(-2L,dao.insertBudgetWithEnvelopes(overlapping,Collections.emptyList()));

        BudgetEntity dec=new BudgetEntity();dec.userId=parent.id;dec.name="Dec";
        dec.monthlyLimitMilli=800_000L;
        dec.periodStart=BudgetPeriodMath.parseIsoDateStartMillis("2026-12-01");
        dec.periodEnd=BudgetPeriodMath.parseIsoDateEndMillis("2026-12-31");
        dec.status="ACTIVE";
        assertTrue(dao.insertBudgetWithEnvelopes(dec,Collections.emptyList())>0L);
    }

    @Test public void manageCategoriesUpdatesActiveEnvelopesAndPreservesTransactionsAndClosedBudgets() {
        SystemCategoryEntity sysFood=dao.systemCategoryByKey("food");
        SystemCategoryEntity sysBills=dao.systemCategoryByKey("bills");
        SystemCategoryEntity sysTransport=dao.systemCategoryByKey("transport");
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(sysFood,sysBills)));
        CategoryEntity food=dao.selectedCategoryForCatalog(parent.id,sysFood.id);
        CategoryEntity bills=dao.selectedCategoryForCatalog(parent.id,sysBills.id);

        BudgetEntity closedOct=new BudgetEntity();closedOct.userId=parent.id;closedOct.name="Oct";closedOct.monthlyLimitMilli=500_000L;
        closedOct.periodStart=BudgetPeriodMath.parseIsoDateStartMillis("2026-10-01");
        closedOct.periodEnd=BudgetPeriodMath.parseIsoDateEndMillis("2026-10-31");
        closedOct.status="CLOSED";
        BudgetLineEntity octBills=new BudgetLineEntity();octBills.categoryId=bills.id;octBills.limitMilli=100_000L;octBills.spentMilli=40_000L;octBills.plannedPercentBps=2000;
        long octId=dao.insertBudgetWithEnvelopes(closedOct,Collections.singletonList(octBills));

        BudgetEntity activeNov=new BudgetEntity();activeNov.userId=parent.id;activeNov.name="Nov";activeNov.monthlyLimitMilli=1_000_000L;
        activeNov.periodStart=BudgetPeriodMath.parseIsoDateStartMillis("2026-11-01");
        activeNov.periodEnd=BudgetPeriodMath.parseIsoDateEndMillis("2026-11-30");
        activeNov.status="ACTIVE";
        BudgetLineEntity novFood=new BudgetLineEntity();novFood.categoryId=food.id;novFood.limitMilli=300_000L;novFood.spentMilli=10_000L;novFood.plannedPercentBps=3000;
        BudgetLineEntity novBills=new BudgetLineEntity();novBills.categoryId=bills.id;novBills.limitMilli=200_000L;novBills.spentMilli=50_000L;novBills.plannedPercentBps=2000;
        long novId=dao.insertBudgetWithEnvelopes(activeNov,Arrays.asList(novFood,novBills));

        TransactionEntity tx=new TransactionEntity();tx.userId=parent.id;tx.budgetId=novId;tx.categoryId=bills.id;
        tx.kind="expense";tx.label="STEG";tx.amountMilli=50_000L;tx.occurredAt=BudgetPeriodMath.parseIsoDateStartMillis("2026-11-05");
        long txId=dao.recordExpense(tx);
        assertTrue(txId>0L);

        assertTrue(dao.updateUserCategorySelections(
                parent.id,
                Arrays.asList(sysFood.id,sysTransport.id),
                Collections.<Long>emptyList(),
                BudgetPeriodMath.parseIsoDateStartMillis("2026-11-01")));
        List<CategoryEntity> nowSelected=dao.selectedCategoriesForUser(parent.id);
        assertEquals(2,nowSelected.size());

        // Historical transaction and closed October budget must remain untouched
        assertNotNull(dao.findBudget(octId));
        assertNotNull(dao.budgetLineForCategory(octId,bills.id));
        assertEquals(1,dao.recentSpendingTransactions(parent.id).size());

        // Active November keeps bills line with spend=50_000 (no data loss) and adds new transport envelope
        CategoryEntity transport=dao.selectedCategoryForCatalog(parent.id,sysTransport.id);
        assertNotNull(transport);
        assertNotNull(dao.budgetLineForCategory(novId,transport.id));
        assertNotNull(dao.budgetLineForCategory(novId,bills.id));
    }

    @Test public void accountIsolationAndEnfantsCategoryWorkIndependentlyPerUser() {
        UserEntity personal=createUser("personal@example.com","user",0L,2L);
        SystemCategoryEntity enfants=dao.systemCategoryByKey("children");
        assertNotNull(enfants);
        assertEquals("expense",enfants.type);
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(enfants,dao.systemCategoryByKey("food"))));
        assertTrue(dao.finishOnboarding(personal.id,Collections.singletonList(dao.systemCategoryByKey("bills"))));

        assertEquals(2,dao.selectedCatalogIds(parent.id).size());
        assertEquals(1,dao.selectedCatalogIds(personal.id).size());
        assertEquals(dao.systemCategoryByKey("bills").id,(long)dao.selectedCatalogIds(personal.id).get(0));

        BudgetEntity parentBudget=new BudgetEntity();parentBudget.userId=parent.id;parentBudget.name="Foyer";
        parentBudget.monthlyLimitMilli=2_000_000L;
        parentBudget.periodStart=BudgetPeriodMath.parseIsoDateStartMillis("2026-11-01");
        parentBudget.periodEnd=BudgetPeriodMath.parseIsoDateEndMillis("2026-11-30");
        parentBudget.status="ACTIVE";
        assertTrue(dao.insertBudgetWithEnvelopes(parentBudget,Collections.emptyList())>0L);

        assertEquals(1,dao.budgetsForUser(parent.id).size());
        assertEquals(0,dao.budgetsForUser(personal.id).size());
    }

    @Test public void deleteCustomCategoryRemovesUnusedAndProtectsUsedCategory() {
        CategoryEntity unused=new CategoryEntity();unused.userId=parent.id;unused.name="Café";unused.colorHex="#2E7D43";unused.type="expense";unused.allocationPercent=0;
        assertTrue(dao.addCategoryWithAllocation(unused));
        CategoryEntity used=new CategoryEntity();used.userId=parent.id;used.name="Taxi";used.colorHex="#2E7D43";used.type="expense";used.allocationPercent=0;
        assertTrue(dao.addCategoryWithAllocation(used));

        long unusedId=0L, usedId=0L;
        for(CategoryEntity c:dao.categoriesForUser(parent.id)) {
            if("Café".equals(c.name)) unusedId=c.id;
            if("Taxi".equals(c.name)) usedId=c.id;
        }
        assertTrue(unusedId>0L && usedId>0L);

        BudgetEntity b=new BudgetEntity();b.userId=parent.id;b.name="Test";b.monthlyLimitMilli=100_000L;
        b.periodStart=1L;b.periodEnd=Long.MAX_VALUE;b.status="ACTIVE";
        long bId=dao.insert(b);

        TransactionEntity tx=new TransactionEntity();tx.userId=parent.id;tx.budgetId=bId;tx.categoryId=usedId;
        tx.kind="expense";tx.label="Course";tx.amountMilli=12_000L;tx.occurredAt=100L;
        dao.insert(tx);

        assertTrue(dao.deleteCustomCategory(parent.id,unusedId));
        assertFalse(dao.deleteCustomCategory(parent.id,usedId));
    }

    @Test public void allThreeUserTypesCanSaveAnyPositiveBudgetAndUpdateProfileName() {
        MasroufRepository repo=new MasroufRepository(context,database);
        long start=BudgetPeriodMath.parseIsoDateStartMillis("2026-11-01");
        long end=BudgetPeriodMath.parseIsoDateEndMillis("2026-11-30");

        // Personal user
        BudgetEntity personalBudget=new BudgetEntity();personalBudget.userId=user.id;personalBudget.name="Personal";
        personalBudget.monthlyLimitMilli=1_563_000L;personalBudget.periodStart=start;personalBudget.periodEnd=end;personalBudget.status="ACTIVE";
        assertTrue(repo.createBudget(personalBudget,Collections.emptyList())>0L);

        // Parent (even with existing child allocations or large/small amounts, only > 0 applies)
        assertTrue(repo.updateName(parent.id,"Amira Updated").succeeded());
        assertEquals("Amira Updated",dao.findUser(parent.id).name);
        BudgetEntity parentBudget=new BudgetEntity();parentBudget.userId=parent.id;parentBudget.name="Family";
        parentBudget.monthlyLimitMilli=1_000L;parentBudget.periodStart=start;parentBudget.periodEnd=end;parentBudget.status="ACTIVE";
        long parentBudgetId=repo.createBudget(parentBudget,Collections.emptyList());
        assertTrue(parentBudgetId>0L);
        parentBudget.id=parentBudgetId;
        parentBudget.monthlyLimitMilli=25_000_000L;
        assertTrue(repo.updateBudget(parentBudget,Collections.emptyList()));
        assertEquals(25_000_000L,dao.findBudget(parentBudgetId).monthlyLimitMilli);

        // Child user can also define, update, and load allocation for any positive budget > 0
        BudgetEntity childBudget=new BudgetEntity();childBudget.userId=child.id;childBudget.name="Child";
        childBudget.monthlyLimitMilli=500_000_000L;childBudget.periodStart=start;childBudget.periodEnd=end;childBudget.status="ACTIVE";
        long childBudgetId=repo.createBudget(childBudget,Collections.emptyList());
        assertTrue(childBudgetId>0L);
        assertEquals(500_000_000L,dao.findBudget(childBudgetId).monthlyLimitMilli);
        assertTrue(repo.loadBudgetAllocation(child.id,false,true,childBudgetId,start).authorized);
        assertNotNull(repo.loadSettingsData(parent.id).activeBudget);
    }

    private UserEntity createUser(String email,String role,long balance,long householdId) {
        UserEntity row=new UserEntity();row.name=role;row.email=email;row.role=role;
        row.accountType="child".equals(role)?"family":"personal";row.passwordHash="test-hash";
        row.balanceMilli=balance;row.points=0;row.onboarded=false;row.categoriesSelected=false;row.createdAt=1L;
        row.householdId=householdId;row.id=dao.insertNewUser(row);return row;
    }
}
