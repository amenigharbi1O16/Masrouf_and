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

    private UserEntity createUser(String email,String role,long balance,long householdId) {
        UserEntity row=new UserEntity();row.name=role;row.email=email;row.role=role;
        row.accountType="child".equals(role)?"family":"personal";row.passwordHash="test-hash";
        row.balanceMilli=balance;row.points=0;row.onboarded=false;row.categoriesSelected=false;row.createdAt=1L;
        row.householdId=householdId;row.id=dao.insertNewUser(row);return row;
    }
}
