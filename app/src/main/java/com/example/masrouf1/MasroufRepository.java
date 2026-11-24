package com.example.masrouf1;

import android.content.Context;
import androidx.lifecycle.LiveData;

import com.example.masrouf1.db.CategorySpendRow;
import com.example.masrouf1.db.BudgetHistoryRow;
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
import com.masrouf.controller.InputValidation;
import com.masrouf.controller.PasswordHasher;
import com.example.masrouf1.AppRole;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

/** Repository boundary for dashboard screens; all records remain in the local Room database. */
public final class MasroufRepository {
    private final MasroufDao dao;
    public MasroufRepository(Context context){dao=MasroufDatabase.getInstance(context.getApplicationContext()).masroufDao();}
    public UserEntity user(long userId){return dao.findUser(userId);}
    public LiveData<UserEntity> observeUser(long userId){return dao.observeUser(userId);}
    public List<CategoryEntity> categories(long userId){return dao.categoriesForUser(userId);}
    public LiveData<List<CategoryEntity>> observeCategories(long userId){return dao.observeCategoriesForUser(userId);}
    public List<CategorySpendRow> categorySpending(long userId,long from,long to){return dao.categorySpendingBetween(userId,from,to);}
    public LiveData<List<CategorySpendRow>> observeCategorySpending(long userId,long from,long to){return dao.observeCategorySpendingBetween(userId,from,to);}
    public List<ProductEntity> products(long userId){return dao.productsForUser(userId);}
    public LiveData<List<ProductEntity>> observeProducts(long userId){return dao.observeProductsForUser(userId);}
    public List<PriceHistoryEntity> priceHistory(long userId,long productId){return dao.priceHistory(userId,productId);}
    public LiveData<List<PriceHistoryEntity>> observePriceHistory(long userId,long productId){return dao.observePriceHistory(userId,productId);}
    public List<TransactionRow> transactions(long userId,long from,long to){return dao.transactionsBetween(userId,from,to);}
    public LiveData<List<TransactionRow>> observeTransactions(long userId,long from,long to){return dao.observeTransactionsBetween(userId,from,to);}
    public LiveData<List<NotificationEntity>> observeNotifications(long userId){return dao.observeNotificationsForUser(userId);}
    public void markNotificationRead(long notificationId,long userId){dao.markNotificationRead(notificationId,userId);}
    public UserEntity authenticate(String email,String password){
        if(!InputValidation.isValidGmail(email)||password==null||password.isEmpty())return null;
        UserEntity candidate=dao.findUserByEmail(email);return candidate!=null&&PasswordHasher.verify(password,candidate.passwordHash)?candidate:null;
    }
    public OperationResult<Long> register(String name,String email,String password,String role,String accountType){
        if(!InputValidation.isValidGmail(email))return OperationResult.failure("gmail");
        if(!InputValidation.isValidPassword(password))return OperationResult.failure("password");
        if(name==null||name.trim().isEmpty())return OperationResult.failure("name");
        if(!AppRole.USER.value().equals(role)&&!AppRole.PARENT.value().equals(role))return OperationResult.failure("role");
        if(dao.findUserByEmail(email)!=null)return OperationResult.failure("duplicate");
        UserEntity user=new UserEntity();user.householdId=System.currentTimeMillis();user.name=name.trim();user.email=email;user.role=role;user.accountType=accountType;
        user.passwordHash=PasswordHasher.hash(password);user.onboarded=false;user.balanceMilli=0;user.createdAt=System.currentTimeMillis();
        try{return OperationResult.success(dao.insertNewUser(user));}
        catch(RuntimeException duplicate){return OperationResult.failure("duplicate");}
    }
    public boolean saveOnboarding(long userId,List<CategoryEntity> categories){return dao.finishOnboarding(userId,categories);}
    public OperationResult<Long> createChildAccount(UserEntity child,ChildProfileEntity profile,BudgetEntity initialBudget,List<CategoryEntity> categories,String password){
        if(profile==null)return OperationResult.failure("parent");
        UserEntity parent=dao.findUser(profile.parentUserId);
        if(parent==null||!(AppRole.PARENT.value().equalsIgnoreCase(parent.role)||"admin".equalsIgnoreCase(parent.role)))return OperationResult.failure("parent");
        if(child==null||!AppRole.CHILD.value().equals(child.role))return OperationResult.failure("role");
        if(!InputValidation.isValidGmail(child.email))return OperationResult.failure("gmail");
        if(!InputValidation.isValidPassword(password))return OperationResult.failure("password");
        if(dao.findUserByEmail(child.email)!=null)return OperationResult.failure("duplicate");
        child.householdId=parent.householdId;child.passwordHash=PasswordHasher.hash(password);
        try{long id=dao.createChildAccount(child,profile,initialBudget,categories);return id>0?OperationResult.success(id):OperationResult.failure("parent");}
        catch(RuntimeException failure){String message=failure.getMessage();return OperationResult.failure(message==null?"save":message);}
    }
    public OperationResult<Boolean> resetChildPassword(long profileId,long parentId,String password){
        try{requireParent(parentId);if(!InputValidation.isValidPassword(password))return OperationResult.failure("password");
            return dao.resetChildPassword(profileId,parentId,PasswordHasher.hash(password))>0?OperationResult.success(true):OperationResult.failure("parent");
        }catch(RuntimeException failure){return OperationResult.failure("parent");}
    }
    public String childEmail(long parentId,long profileId){requireParent(parentId);return dao.childEmailForProfile(profileId,parentId);}
    public OperationResult<Boolean> updateEmail(long userId,String email){
        if(!InputValidation.isValidGmail(email))return OperationResult.failure("gmail");
        UserEntity existing=dao.findUserByEmail(email);if(existing!=null&&existing.id!=userId)return OperationResult.failure("duplicate");
        try{return dao.updateEmail(userId,email)>0?OperationResult.success(true):OperationResult.failure("save");}
        catch(RuntimeException duplicate){return OperationResult.failure("duplicate");}
    }
    public long createBudget(BudgetEntity budget,List<BudgetLineEntity> lines){
        UserEntity owner=requireUserExists(budget.userId);if("child".equalsIgnoreCase(owner.role))throw new SecurityException("child budget creation is parent managed");
        return dao.insertBudgetWithEnvelopes(budget,lines);
    }
    public boolean updateBudget(BudgetEntity budget,List<BudgetLineEntity> lines){
        UserEntity owner=requireUserExists(budget.userId);if("child".equalsIgnoreCase(owner.role))throw new SecurityException("child budget editing is locked");
        return dao.updateBudgetWithEnvelopes(budget,lines);
    }
    public boolean replaceChildEnvelopes(long budgetId,long childId,List<BudgetLineEntity> lines){
        UserEntity child=requireUserExists(childId);if(!"child".equalsIgnoreCase(child.role))throw new SecurityException("child role required");
        return dao.replaceChildBudgetEnvelopes(budgetId,childId,lines);
    }
    public ProductFormData productForm(long userId){
        requireUserExists(userId);
        return new ProductFormData(dao.activeBudgetForUser(userId),dao.categoriesForUser(userId));
    }
    public BudgetEntity activeBudget(long userId){requireUserExists(userId);return dao.activeBudgetForUser(userId);}
    public BudgetEntity budget(long budgetId,long ownerId){requireUserExists(ownerId);BudgetEntity value=dao.findBudget(budgetId);return value!=null&&value.userId==ownerId?value:null;}
    public ProductDetailsData productDetails(long userId,long productId){
        requireUserExists(userId);
        ProductEntity product=dao.findProductForUser(productId,userId);
        return new ProductDetailsData(product,product==null?Collections.emptyList():dao.priceHistory(userId,productId));
    }
    public Long latestUnitPrice(long userId,String name,String brand,String unit){requireUserExists(userId);return dao.latestUnitPrice(userId,name,brand,unit);}
    public BudgetLineEntity envelope(long userId,long budgetId,long categoryId){
        requireUserExists(userId);BudgetEntity budget=dao.findBudget(budgetId);
        return budget!=null&&budget.userId==userId?dao.budgetLineForCategory(budgetId,categoryId):null;
    }
    public long savePurchase(ProductEntity product,PurchaseEntity purchase,PurchaseItemEntity item,long userId,long budgetId){
        requireUserExists(userId);
        if(purchase.userId!=userId)throw new SecurityException("purchase owner");
        return dao.recordPurchase(product,purchase,item,budgetId);
    }
    public long saveExpense(TransactionEntity expense,long userId){
        requireUserExists(userId);if(expense.userId!=userId)throw new SecurityException("expense owner");
        return dao.recordExpense(expense);
    }
    public List<CategoryEntity> categoriesForExpense(long userId){requireUserExists(userId);return dao.categoriesForUser(userId);}
    public int allocatedPercent(long userId,String type){requireUserExists(userId);return dao.allocatedPercent(userId,type);}
    public CategoryAllocationData categoryAllocation(long userId,String type){requireUserExists(userId);return new CategoryAllocationData(dao.allocatedPercent(userId,type),dao.activeBudgetForUser(userId));}
    public boolean addCategory(CategoryEntity category,long userId){requireUserExists(userId);if(category.userId!=userId)throw new SecurityException("category owner");return dao.addCategoryWithAllocation(category);}
    public List<ChildSpendingSummary> familyChildren(long parentId){requireParent(parentId);return dao.childSpendingSummaries(parentId);}
    public List<BudgetHistoryRow> childHistory(long parentId,long childId){requireParent(parentId);return dao.childBudgetHistory(childId,parentId);}
    public GiftEnvelopeData giftEnvelope(long parentId){
        requireParent(parentId);BudgetEntity budget=dao.activeBudgetForUser(parentId);
        if(budget==null)return new GiftEnvelopeData(null,0,false);
        long categoryId=dao.ensureChildrenCategory(parentId);BudgetLineEntity line=dao.budgetLineForCategory(budget.id,categoryId);
        long spent=dao.categoryExpenseBetween(parentId,categoryId,budget.periodStart,budget.periodEnd);
        long remaining=line==null?0:MoneyMath.subtract(line.limitMilli,spent);
        return new GiftEnvelopeData(budget,remaining,true);
    }
    public int giveMoney(long parentId,long profileId,long amountMilli,String note,long occurredAt){requireParent(parentId);return dao.giveMoneyToChild(profileId,parentId,amountMilli,note,occurredAt);}
    public String parentPhone(long childUserId){
        UserEntity child=requireUserExists(childUserId);if(!"child".equalsIgnoreCase(child.role))return "";
        String phone=dao.parentPhoneForChild(childUserId);return phone==null?"":phone;
    }
    public DrawerBadgeData parentDrawerBadges(long parentId){requireParent(parentId);return new DrawerBadgeData(dao.countChildren(parentId),dao.unreadNotificationCount(parentId));}
    public BudgetAllocationData loadBudgetAllocation(long userId,boolean childMode,boolean editMode,long budgetId,long periodStart){
        UserEntity owner=requireUserExists(userId);boolean isChild="child".equalsIgnoreCase(owner.role);
        if(childMode!=isChild)return new BudgetAllocationData(false,Collections.emptyList(),null,null,Collections.emptyList(),Collections.emptyMap());
        List<CategoryEntity> categories=dao.categoriesForUser(userId);BudgetEntity budget=null;ChildProfileEntity profile=null;
        if(childMode){profile=dao.childProfileForUser(userId);budget=dao.findBudget(budgetId);if(profile==null||budget==null||budget.userId!=userId)return new BudgetAllocationData(false,categories,null,profile,Collections.emptyList(),Collections.emptyMap());}
        else if(editMode){budget=dao.findBudget(budgetId);if(budget==null||budget.userId!=userId)return new BudgetAllocationData(false,categories,null,null,Collections.emptyList(),Collections.emptyMap());}
        List<BudgetLineEntity> lines=budget==null?Collections.emptyList():dao.budgetLines(budget.id);Map<Long,Long> history=Collections.emptyMap();
        if(lines.isEmpty()&&periodStart>0){Calendar prior=Calendar.getInstance();prior.setTimeInMillis(periodStart);prior.add(Calendar.MONTH,-1);List<CategorySpendRow> spent=dao.categorySpendingBetween(userId,prior.getTimeInMillis(),periodStart);Map<Long,Long> amounts=new java.util.HashMap<>();for(CategorySpendRow row:spent)amounts.put(row.categoryId,row.amountMilli);history=amounts;}
        return new BudgetAllocationData(true,categories,budget,profile,lines,history);
    }
    public FamilyAnalyticsData familyAnalytics(long parentId){
        requireParent(parentId);List<ChildSpendingSummary> children=dao.childSpendingSummaries(parentId);TreeMap<Long,FamilyMonthSpending> months=new TreeMap<>();
        long global=0,parentSpent=0;BudgetEntity budget=dao.activeBudgetForUser(parentId);
        if(budget!=null){global=budget.monthlyLimitMilli;parentSpent=dao.personalSpendingBetween(parentId,budget.periodStart,budget.periodEnd);}
        for(ChildSpendingSummary child:children)for(BudgetHistoryRow row:dao.childBudgetHistory(child.childUserId,parentId)){
            Calendar month=Calendar.getInstance();month.setTimeInMillis(row.periodStart);month.set(Calendar.DAY_OF_MONTH,1);month.set(Calendar.HOUR_OF_DAY,0);month.set(Calendar.MINUTE,0);month.set(Calendar.SECOND,0);month.set(Calendar.MILLISECOND,0);
            long key=month.getTimeInMillis();FamilyMonthSpending point=months.get(key);if(point==null){point=new FamilyMonthSpending(key);months.put(key,point);}point.spentMilli=MoneyMath.add(point.spentMilli,row.spentMilli);point.allocatedMilli=MoneyMath.add(point.allocatedMilli,row.allocatedMilli);
        }
        return new FamilyAnalyticsData(children,new ArrayList<>(months.values()),global,dao.totalChildAllocations(parentId),parentSpent);
    }
    private UserEntity requireUserExists(long userId){UserEntity user=dao.findUser(userId);if(user==null)throw new SecurityException("unknown user");return user;}
    private void requireParent(long userId){UserEntity parent=dao.findUser(userId);if(parent==null||!("parent".equalsIgnoreCase(parent.role)||"admin".equalsIgnoreCase(parent.role)))throw new SecurityException("parent role required");}
    public HomeDashboardData loadHome(long userId,boolean parentMode,boolean childMode){
        UserEntity user=dao.findUser(userId);BudgetEntity budget=dao.activeBudgetForUser(userId);
        List<CategoryEntity> categories=dao.categoriesForUser(userId);List<TransactionRow> recent=dao.recentSpendingTransactions(userId);
        ChildProfileEntity profile=childMode?dao.childProfileForUser(userId):null;
        List<ChildSpendingSummary> children=parentMode?dao.childSpendingSummaries(userId):Collections.emptyList();
        long allocated=parentMode?dao.totalChildAllocations(userId):0;
        long parentSpent=parentMode&&budget!=null?dao.personalSpendingBetween(userId,budget.periodStart,budget.periodEnd):0;
        return new HomeDashboardData(user,budget,categories,recent,profile,children,allocated,parentSpent);
    }
}
