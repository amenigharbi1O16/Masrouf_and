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
import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.ProductCatalogRow;
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

    @Test public void repeatedPurchaseReusesProductAndBuildsPriceHistory() {
        MasroufRepository repo=new MasroufRepository(context,database);
        assertTrue(dao.finishOnboarding(user.id,Arrays.asList(dao.systemCategoryByKey("food"),dao.systemCategoryByKey("other"))));
        CategoryEntity food=dao.selectedCategoryForCatalog(user.id,dao.systemCategoryByKey("food").id);

        BudgetEntity budget=new BudgetEntity();budget.userId=user.id;budget.name="Nov";budget.monthlyLimitMilli=500_000L;
        budget.periodStart=1L;budget.periodEnd=Long.MAX_VALUE;budget.status="ACTIVE";
        long budgetId=repo.createBudget(budget,Collections.emptyList());

        ProductEntity p1=new ProductEntity();p1.categoryId=food.id;p1.name="Huile";p1.brand="Safi";p1.unit="L";p1.currentPriceMilli=18_000L;p1.currency="TND";p1.updatedAt=100L;
        PurchaseEntity pu1=new PurchaseEntity();pu1.userId=user.id;pu1.occurredAt=100L;pu1.totalMilli=18_000L;pu1.storeName="Carrefour";
        PurchaseItemEntity pi1=new PurchaseItemEntity();pi1.description="Huile";pi1.quantityMilli=1_000;pi1.unit="L";pi1.unitPriceMilli=18_000L;
        assertTrue(repo.savePurchase(p1,pu1,pi1,user.id,budgetId)>0L);

        ProductEntity p2=new ProductEntity();p2.categoryId=food.id;p2.name="Huile";p2.brand="Safi";p2.unit="L";p2.currentPriceMilli=16_500L;p2.currency="TND";p2.updatedAt=200L;
        PurchaseEntity pu2=new PurchaseEntity();pu2.userId=user.id;pu2.occurredAt=200L;pu2.totalMilli=16_500L;pu2.storeName="Monoprix";
        PurchaseItemEntity pi2=new PurchaseItemEntity();pi2.description="Huile";pi2.quantityMilli=1_000;pi2.unit="L";pi2.unitPriceMilli=16_500L;
        assertTrue(repo.savePurchase(p2,pu2,pi2,user.id,budgetId)>0L);

        List<ProductCatalogRow> catalog=repo.productCatalog(user.id);
        assertEquals(1,catalog.size());
        assertEquals(16_500L,catalog.get(0).currentPriceMilli);
        assertEquals(Long.valueOf(18_000L),catalog.get(0).priorPriceMilli);
        assertEquals(2,repo.purchaseHistoryForProduct(catalog.get(0).id,user.id).size());
    }

    @Test public void analyticsComputesRealDataAcrossNormalUserChildAndParentWithoutCrossAccountLeakage() {
        MasroufRepository repo=new MasroufRepository(context,database);
        SystemCategoryEntity sysFood=dao.systemCategoryByKey("food");
        SystemCategoryEntity sysBills=dao.systemCategoryByKey("bills");
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(sysFood,sysBills)));
        assertTrue(dao.finishOnboarding(user.id,Arrays.asList(sysFood,sysBills)));
        assertTrue(dao.finishOnboarding(child.id,Arrays.asList(sysFood)));

        long start=BudgetPeriodMath.startOfMonthMillis(2026,java.util.Calendar.NOVEMBER);
        long end=BudgetPeriodMath.endOfMonthMillis(2026,java.util.Calendar.NOVEMBER);

        // Personal user with purchases and direct expenses
        CategoryEntity uFood=dao.selectedCategoryForCatalog(user.id,sysFood.id);
        CategoryEntity uBills=dao.selectedCategoryForCatalog(user.id,sysBills.id);
        BudgetEntity uBudget=new BudgetEntity();uBudget.userId=user.id;uBudget.name="Nov";uBudget.monthlyLimitMilli=1_000_000L;
        uBudget.periodStart=start;uBudget.periodEnd=end;uBudget.status="ACTIVE";
        BudgetLineEntity uFoodLine=new BudgetLineEntity();uFoodLine.categoryId=uFood.id;uFoodLine.limitMilli=200_000L;uFoodLine.plannedPercentBps=2000;
        BudgetLineEntity uBillsLine=new BudgetLineEntity();uBillsLine.categoryId=uBills.id;uBillsLine.limitMilli=100_000L;uBillsLine.plannedPercentBps=1000;
        long uBudgetId=repo.createBudget(uBudget,Arrays.asList(uFoodLine,uBillsLine));

        ProductEntity milk=new ProductEntity();milk.categoryId=uFood.id;milk.name="Lait";milk.unit="L";milk.currentPriceMilli=12_000L;milk.currency="TND";milk.updatedAt=start+1_000L;
        PurchaseEntity mPur=new PurchaseEntity();mPur.userId=user.id;mPur.occurredAt=start+1_000L;mPur.totalMilli=12_000L;mPur.storeName="Magasin";
        PurchaseItemEntity mItem=new PurchaseItemEntity();mItem.description="Lait";mItem.quantityMilli=1_000;mItem.unit="L";mItem.unitPriceMilli=12_000L;
        assertTrue(repo.savePurchase(milk,mPur,mItem,user.id,uBudgetId)>0L);

        TransactionEntity elec=new TransactionEntity();elec.userId=user.id;elec.budgetId=uBudgetId;elec.categoryId=uBills.id;
        elec.kind="expense";elec.label="STEG";elec.amountMilli=90_000L;elec.occurredAt=start+2_000L;
        assertTrue(repo.saveExpense(elec,user.id)>0L);

        AnalyticsSummaryData userAnalytics=repo.loadAnalytics(user.id,uBudgetId);
        assertEquals(AppRole.USER,userAnalytics.role);
        assertEquals(102_000L,userAnalytics.currentSpentMilli);
        assertEquals(12_000L,userAnalytics.purchasesTotalMilli);
        assertEquals(90_000L,userAnalytics.expensesTotalMilli);
        assertEquals(6,userAnalytics.sixMonthHistory.size());
        // Sorted ascending by % consumed: Food (6%) before Bills (90%)
        assertEquals(2,userAnalytics.categoryStats.size());
        assertEquals(uFood.id,userAnalytics.categoryStats.get(0).categoryId);
        assertEquals(uBills.id,userAnalytics.categoryStats.get(1).categoryId);
        assertNotNull(userAnalytics.attentionCategory);
        assertEquals(uBills.id,userAnalytics.attentionCategory.categoryId);

        // Child has strict own data only
        CategoryEntity cFood=dao.selectedCategoryForCatalog(child.id,sysFood.id);
        BudgetEntity cBudget=new BudgetEntity();cBudget.userId=child.id;cBudget.name="ChildNov";cBudget.monthlyLimitMilli=50_000L;
        cBudget.periodStart=start;cBudget.periodEnd=end;cBudget.status="ACTIVE";
        long cBudgetId=repo.createBudget(cBudget,Collections.emptyList());
        TransactionEntity snack=new TransactionEntity();snack.userId=child.id;snack.budgetId=cBudgetId;snack.categoryId=cFood.id;
        snack.kind="expense";snack.label="Gouter";snack.amountMilli=15_000L;snack.occurredAt=start+5_000L;
        assertTrue(repo.saveExpense(snack,child.id)>0L);

        AnalyticsSummaryData childAnalytics=repo.loadAnalytics(child.id,cBudgetId);
        assertEquals(AppRole.CHILD,childAnalytics.role);
        assertEquals(15_000L,childAnalytics.currentSpentMilli);
        assertTrue(childAnalytics.childrenProgress.isEmpty());

        // Parent sees household analytics + aggregate child progress (never child item details)
        BudgetEntity pBudget=new BudgetEntity();pBudget.userId=parent.id;pBudget.name="FamilyNov";pBudget.monthlyLimitMilli=2_000_000L;
        pBudget.periodStart=start;pBudget.periodEnd=end;pBudget.status="ACTIVE";
        long pBudgetId=repo.createBudget(pBudget,Collections.emptyList());
        AnalyticsSummaryData parentAnalytics=repo.loadAnalytics(parent.id,pBudgetId);
        assertEquals(AppRole.PARENT,parentAnalytics.role);
        assertEquals(1,parentAnalytics.childrenProgress.size());
        assertEquals(15_000L,parentAnalytics.childrenProgress.get(0).spentMilli);
    }

    @Test public void notificationsFireOnThresholdsOverrunBudgetUpdatesAndChildToParentWithoutDuplicates() {
        MasroufRepository repo=new MasroufRepository(context,database);
        SystemCategoryEntity sysFood=dao.systemCategoryByKey("food");
        assertTrue(dao.finishOnboarding(parent.id,Arrays.asList(sysFood)));
        assertTrue(dao.finishOnboarding(user.id,Arrays.asList(sysFood)));
        assertTrue(dao.finishOnboarding(child.id,Arrays.asList(sysFood)));

        long start=BudgetPeriodMath.startOfMonthMillis(2026,java.util.Calendar.NOVEMBER);
        long end=BudgetPeriodMath.endOfMonthMillis(2026,java.util.Calendar.NOVEMBER);

        // User 100 DT budget + 80 DT category envelope -> 80%, 100%, overrun + deduplication
        CategoryEntity uFood=dao.selectedCategoryForCatalog(user.id,sysFood.id);
        BudgetEntity uBudget=new BudgetEntity();uBudget.userId=user.id;uBudget.name="UserNov";uBudget.monthlyLimitMilli=100_000L;
        uBudget.periodStart=start;uBudget.periodEnd=end;uBudget.status="ACTIVE";
        BudgetLineEntity uLine=new BudgetLineEntity();uLine.categoryId=uFood.id;uLine.limitMilli=80_000L;uLine.plannedPercentBps=8000;
        long uBudgetId=repo.createBudget(uBudget,Collections.singletonList(uLine));

        // Expense 1: 80 DT (triggers 80% budget + category 80% caution + budget_created already present)
        TransactionEntity t1=new TransactionEntity();t1.userId=user.id;t1.budgetId=uBudgetId;t1.categoryId=uFood.id;
        t1.kind="expense";t1.label="Course1";t1.amountMilli=80_000L;t1.occurredAt=start+1_000L;
        assertTrue(repo.saveExpense(t1,user.id)>0L);

        // Expense 2: 20 DT -> reaches exact 100 DT (100% milestone!) and category > 100%
        TransactionEntity t2=new TransactionEntity();t2.userId=user.id;t2.budgetId=uBudgetId;t2.categoryId=uFood.id;
        t2.kind="expense";t2.label="Course2";t2.amountMilli=20_000L;t2.occurredAt=start+2_000L;
        assertTrue(repo.saveExpense(t2,user.id)>0L);

        // Expense 3: 15 DT -> exceeds 100% (115 DT / 100 DT red overrun!)
        TransactionEntity t3=new TransactionEntity();t3.userId=user.id;t3.budgetId=uBudgetId;t3.categoryId=uFood.id;
        t3.kind="expense";t3.label="Course3";t3.amountMilli=15_000L;t3.occurredAt=start+3_000L;
        assertTrue(repo.saveExpense(t3,user.id)>0L);

        // Expense 4: 5 DT -> repeat while already over: MUST NOT duplicate budget_over notification
        TransactionEntity t4=new TransactionEntity();t4.userId=user.id;t4.budgetId=uBudgetId;t4.categoryId=uFood.id;
        t4.kind="expense";t4.label="Course4";t4.amountMilli=5_000L;t4.occurredAt=start+4_000L;
        assertTrue(repo.saveExpense(t4,user.id)>0L);

        List<NotificationEntity> uNotifs=dao.notificationsForUser(user.id);
        int createdCnt=0,cautionCnt=0,limit100Cnt=0,overCnt=0,importantCnt=0;
        for(NotificationEntity n:uNotifs) {
            if("budget_created".equals(n.kind)) createdCnt++;
            if("budget_caution".equals(n.kind)) cautionCnt++;
            if("budget_100".equals(n.kind)) limit100Cnt++;
            if("budget_over".equals(n.kind)) overCnt++;
            if("important_expense".equals(n.kind)) importantCnt++;
        }
        assertEquals(1,createdCnt);
        assertEquals(1,cautionCnt);
        assertEquals(1,limit100Cnt);
        assertEquals(1,overCnt);
        assertEquals(1,importantCnt);

        // Child spends 55 DT against 50 DT budget (single important overrun purchase): Parent MUST receive alerts, Personal User MUST NOT!
        CategoryEntity cFood=dao.selectedCategoryForCatalog(child.id,sysFood.id);
        BudgetEntity cBudget=new BudgetEntity();cBudget.userId=child.id;cBudget.name="ChildNov";cBudget.monthlyLimitMilli=50_000L;
        cBudget.periodStart=start;cBudget.periodEnd=end;cBudget.status="ACTIVE";
        long cBudgetId=repo.createBudget(cBudget,Collections.emptyList());

        ProductEntity p=new ProductEntity();p.categoryId=cFood.id;p.name="Chaussures";p.unit="pièce";p.currentPriceMilli=55_000L;p.currency="TND";p.updatedAt=start+5_000L;
        PurchaseEntity pu=new PurchaseEntity();pu.userId=child.id;pu.occurredAt=start+5_000L;pu.totalMilli=55_000L;pu.storeName="Mall";
        PurchaseItemEntity pi=new PurchaseItemEntity();pi.description="Chaussures";pi.quantityMilli=1_000;pi.unit="pièce";pi.unitPriceMilli=55_000L;
        assertTrue(repo.savePurchase(p,pu,pi,child.id,cBudgetId)>0L);

        List<NotificationEntity> pNotifs=dao.notificationsForUser(parent.id);
        boolean parentGotChildOver=false, parentGotChildImportant=false;
        for(NotificationEntity n:pNotifs) {
            if("child_budget_over".equals(n.kind)) parentGotChildOver=true;
            if("child_important_tx".equals(n.kind)) parentGotChildImportant=true;
        }
        assertTrue(parentGotChildOver);
        assertTrue(parentGotChildImportant);

        // Personal user must NEVER receive parent/child family notifications
        for(NotificationEntity n:dao.notificationsForUser(user.id)) {
            assertFalse(n.kind!=null&&n.kind.startsWith("child_"));
        }
    }

    private UserEntity createUser(String email,String role,long balance,long householdId) {
        UserEntity row=new UserEntity();row.name=role;row.email=email;row.role=role;
        row.accountType="child".equals(role)?"family":"personal";row.passwordHash="test-hash";
        row.balanceMilli=balance;row.points=0;row.onboarded=false;row.categoriesSelected=false;row.createdAt=1L;
        row.householdId=householdId;row.id=dao.insertNewUser(row);return row;
    }
}
