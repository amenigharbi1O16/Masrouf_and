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
import com.example.masrouf1.db.MasroufEntities.SettingEntity;
import com.example.masrouf1.db.MasroufEntities.GoalEntity;
import com.example.masrouf1.db.MasroufEntities.MoneyRequestEntity;
import com.example.masrouf1.db.MasroufEntities.PointsLogEntity;
import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MoneyRequestSummary;
import com.example.masrouf1.db.ProductCatalogRow;
import com.masrouf.controller.InputValidation;
import com.masrouf.controller.PasswordHasher;
import com.example.masrouf1.AppRole;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

/** Repository boundary for dashboard screens; all records remain in the local Room database. */
public final class MasroufRepository {
    private final MasroufDao dao;
    private final Context appContext;
    public MasroufRepository(Context context){appContext=context.getApplicationContext();dao=MasroufDatabase.getInstance(appContext).masroufDao();}
    public UserEntity user(long userId){return dao.findUser(userId);}
    public LiveData<UserEntity> observeUser(long userId){return dao.observeUser(userId);}
    public List<CategoryEntity> categories(long userId){return dao.categoriesForUser(userId);}
    public List<SystemCategoryEntity> onboardingCategories(long userId){
        UserEntity owner=requireUserExists(userId);
        if("child".equalsIgnoreCase(owner.role)){
            if(dao.childProfileForUser(userId)==null)throw new SecurityException("child account has no family link");
            return dao.allowedSystemCategoriesForChild(userId);
        }
        return dao.systemCategories();
    }
    public List<Long> selectedCatalogIds(long userId){requireUserExists(userId);return dao.selectedCatalogIds(userId);}
    public SettingEntity systemSettings(){return dao.systemSettings();}
    public LiveData<List<CategoryEntity>> observeCategories(long userId){return dao.observeCategoriesForUser(userId);}
    public List<CategorySpendRow> categorySpending(long userId,long from,long to){return dao.categorySpendingBetween(userId,from,to);}
    public MonthlyComparison monthlyComparison(long userId,long currentFrom,long currentTo,long previousFrom,long previousTo){
        requireUserExists(userId);return new MonthlyComparison(dao.totalSpendingBetween(userId,currentFrom,currentTo),dao.totalSpendingBetween(userId,previousFrom,previousTo));
    }
    public LiveData<List<CategorySpendRow>> observeCategorySpending(long userId,long from,long to){return dao.observeCategorySpendingBetween(userId,from,to);}
    public List<ProductEntity> products(long userId){return dao.productsForUser(userId);}
    public List<ProductCatalogRow> productCatalog(long userId){requireUserExists(userId);return dao.productCatalog(userId);}
    public LiveData<List<ProductEntity>> observeProducts(long userId){return dao.observeProductsForUser(userId);}
    public List<PriceHistoryEntity> priceHistory(long userId,long productId){return dao.priceHistory(userId,productId);}
    public LiveData<List<PriceHistoryEntity>> observePriceHistory(long userId,long productId){return dao.observePriceHistory(userId,productId);}
    public List<TransactionRow> transactions(long userId,long from,long to){return dao.transactionsBetween(userId,from,to);}
    public TransactionEntity transactionForUser(long userId,long transactionId){requireUserExists(userId);TransactionEntity row=dao.findTransaction(transactionId);return row!=null&&row.userId==userId?row:null;}
    public LiveData<List<TransactionRow>> observeTransactions(long userId,long from,long to){return dao.observeTransactionsBetween(userId,from,to);}
    public LiveData<List<NotificationEntity>> observeNotifications(long userId){return dao.observeNotificationsForUser(userId);}
    public void markNotificationRead(long notificationId,long userId){dao.markNotificationRead(notificationId,userId);}
    public void markAllNotificationsRead(long userId){requireUserExists(userId);dao.markAllNotificationsRead(userId);}
    public List<MoneyRequestEntity> moneyRequestsForParent(long parentId){requireParent(parentId);return dao.moneyRequestsForParent(parentId);}
    public List<MoneyRequestSummary> moneyRequestSummariesForParent(long parentId){requireParent(parentId);return dao.moneyRequestSummariesForParent(parentId);}
    public List<MoneyRequestEntity> moneyRequestsForChild(long childId){UserEntity child=requireUserExists(childId);if(!AppRole.CHILD.value().equalsIgnoreCase(child.role))throw new SecurityException("child role required");return dao.moneyRequestsForChild(childId);}
    public long requestMoney(long childId,long amountMilli,String reason){
        UserEntity child=requireUserExists(childId);if(!AppRole.CHILD.value().equalsIgnoreCase(child.role)||amountMilli<=0)return -1L;
        ChildProfileEntity profile=dao.childProfileForUser(childId);if(profile==null)return -1L;
        MoneyRequestEntity request=new MoneyRequestEntity();request.childUserId=childId;request.parentUserId=profile.parentUserId;
        request.amountMilli=amountMilli;request.reason=reason==null?"":reason.trim();request.createdAt=System.currentTimeMillis();
        String title=appContext.getString(R.string.money_request_notification_title);
        String message=appContext.getString(R.string.money_request_notification_body,child.name,CurrencyFormatter.formatMilli(amountMilli));
        long requestId=dao.createMoneyRequest(request,title,message);
        if(requestId>0)BudgetNotificationManager.moneyRequestReceived(appContext,(int)(requestId&0x7fffffff),"money-request:"+requestId,message);
        return requestId;
    }
    public int resolveMoneyRequest(long parentId,long requestId,boolean approve,String responseNote){
        requireParent(parentId);MoneyRequestEntity request=dao.findMoneyRequest(requestId);
        if(request==null||request.parentUserId!=parentId)return -1;
        long now=System.currentTimeMillis();String status=approve?appContext.getString(R.string.money_request_accepted):appContext.getString(R.string.money_request_rejected);
        String message=approve?appContext.getString(R.string.money_request_accepted_body,CurrencyFormatter.formatMilli(request.amountMilli)):
                appContext.getString(R.string.money_request_rejected_body);
        int result=dao.resolveMoneyRequest(requestId,parentId,approve,responseNote,now,status,message);
        if(result==0)BudgetNotificationManager.moneyRequestResolved(appContext,(int)(requestId&0x7fffffff),"money-request-response:"+requestId+":"+(approve?"accepted":"rejected"),status,message);
        return result;
    }
    public GoalEntity primaryGoal(long userId){requireUserExists(userId);return dao.primaryGoal(userId);}
    public TransactionRow latestAllowanceReceived(long userId){requireUserExists(userId);return dao.latestAllowanceReceived(userId);}
    public long totalAllowanceReceived(long userId){requireUserExists(userId);return dao.totalAllowanceReceived(userId);}
    public List<PointsLogEntity> pointsHistory(long userId){requireUserExists(userId);return dao.pointsHistory(userId);}
    public List<com.example.masrouf1.db.MasroufEntities.RewardEntity> rewardsForUser(long userId){
        UserEntity user=requireUserExists(userId);if(!AppRole.CHILD.value().equalsIgnoreCase(user.role))return Collections.emptyList();
        ChildProfileEntity profile=dao.childProfileForUser(userId);return profile==null?Collections.emptyList():dao.rewardsForChild(profile.id);
    }
    public boolean redeemReward(long childUserId,long rewardId){
        UserEntity child=requireUserExists(childUserId);if(!AppRole.CHILD.value().equalsIgnoreCase(child.role))throw new SecurityException("child role required");
        return dao.redeemReward(childUserId,rewardId,System.currentTimeMillis(),appContext.getString(R.string.reward_redeemed_title),appContext.getString(R.string.reward_redeemed_body));
    }
    public UserEntity authenticate(String email,String password){
        String normalizedEmail=InputValidation.normalizeGmail(email);
        if(!InputValidation.isStrictGmail(normalizedEmail)||password==null||password.isEmpty())return null;
        UserEntity candidate=dao.findUserByEmail(normalizedEmail);return candidate!=null&&PasswordHasher.verify(password,candidate.passwordHash)?candidate:null;
    }
    public OperationResult<Long> register(String name,String email,String password,String role,String accountType){
        email=InputValidation.normalizeGmail(email);
        if(!InputValidation.isStrictGmail(email))return OperationResult.failure("gmail");
        if(!InputValidation.isValidPassword(password))return OperationResult.failure("password");
        if(name==null||name.trim().isEmpty())return OperationResult.failure("name");
        if(!AppRole.USER.value().equals(role)&&!AppRole.PARENT.value().equals(role))return OperationResult.failure("role");
        if(dao.findUserByEmail(email)!=null)return OperationResult.failure("duplicate");
        UserEntity user=new UserEntity();user.householdId=System.currentTimeMillis();user.name=name.trim();user.email=email;user.role=role;user.accountType=accountType;
        user.passwordHash=PasswordHasher.hash(password);user.onboarded=false;user.categoriesSelected=false;user.balanceMilli=0;user.createdAt=System.currentTimeMillis();
        try{return OperationResult.success(dao.insertNewUser(user));}
        catch(RuntimeException duplicate){return OperationResult.failure("duplicate");}
    }
    public boolean saveOnboarding(long userId,List<SystemCategoryEntity> categories){return dao.finishOnboarding(userId,categories);}
    public boolean deleteExpenseTransaction(long transactionId,long userId){
        requireUserExists(userId);TransactionEntity row=dao.findTransaction(transactionId);
        return row!=null&&row.userId==userId&&dao.deleteExpenseTransaction(transactionId);
    }
    public OperationResult<Long> createChildAccount(UserEntity child,ChildProfileEntity profile,BudgetEntity initialBudget,List<CategoryEntity> categories,String password){
        if(profile==null)return OperationResult.failure("parent");
        UserEntity parent=dao.findUser(profile.parentUserId);
        if(parent==null||!(AppRole.PARENT.value().equalsIgnoreCase(parent.role)||"admin".equalsIgnoreCase(parent.role)))return OperationResult.failure("parent");
        if(child==null||!AppRole.CHILD.value().equals(child.role))return OperationResult.failure("role");
        child.email=InputValidation.normalizeGmail(child.email);
        if(!InputValidation.isStrictGmail(child.email))return OperationResult.failure("gmail");
        if(!InputValidation.isValidPassword(password))return OperationResult.failure("password");
        if(dao.findUserByEmail(child.email)!=null)return OperationResult.failure("duplicate");
        child.householdId=parent.householdId;child.passwordHash=PasswordHasher.hash(password);child.onboarded=false;child.categoriesSelected=false;
        try{long id=dao.createChildAccount(child,profile,initialBudget,categories);return id>0?OperationResult.success(id):OperationResult.failure("parent");}
        catch(RuntimeException failure){String message=failure.getMessage();return OperationResult.failure(message==null?"save":message);}
    }
    public OperationResult<Boolean> resetChildPassword(long profileId,long parentId,String password){
        try{requireParent(parentId);if(!InputValidation.isValidPassword(password))return OperationResult.failure("password");
            return dao.resetChildPassword(profileId,parentId,PasswordHasher.hash(password))>0?OperationResult.success(true):OperationResult.failure("parent");
        }catch(RuntimeException failure){return OperationResult.failure("parent");}
    }
    public String childEmail(long parentId,long profileId){requireParent(parentId);return dao.childEmailForProfile(profileId,parentId);}
    /**
     * Vérifie l'ancien mot de passe puis met à jour le hash en base si la vérification réussit.
     * Retourne OperationResult.failure("wrong_password") si l'ancien MDP est incorrect.
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
     * Retourne l'historique chronologique des achats d'un produit pour l'utilisateur connecté.
     * Utilisé par ProductPriceHistoryActivity.
     */
    public List<PurchaseHistoryRow> purchaseHistoryForProduct(long productId, long userId) {
        requireUserExists(userId);
        return dao.productPurchaseHistory(userId, productId);
    }

    public OperationResult<Boolean> updateEmail(long userId,String email){
        email=InputValidation.normalizeGmail(email);
        if(!InputValidation.isStrictGmail(email))return OperationResult.failure("gmail");
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
        return product==null?new ProductDetailsData(null,Collections.emptyList(),Collections.emptyList()):
                new ProductDetailsData(product,dao.priceHistory(userId,productId),dao.productPurchaseHistory(userId,productId));
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
    public int giveMoney(long parentId,long profileId,long amountMilli,String note,long occurredAt){
        requireParent(parentId);String title=appContext.getString(R.string.notification_gift_received);
        String message=appContext.getString(R.string.allowance_received,CurrencyFormatter.formatMilli(amountMilli));
        int result=dao.giveMoneyToChild(profileId,parentId,amountMilli,note,occurredAt,title,message,true);
        if(result==0){ChildProfileEntity profile=dao.childProfile(profileId);if(profile!=null){TransactionRow receipt=dao.latestAllowanceReceived(profile.childUserId);if(receipt!=null)BudgetNotificationManager.allowanceReceived(appContext,(int)(receipt.id&0x7fffffff),profile.childUserId,amountMilli,"allowance:"+receipt.id);}}
        return result;
    }
    public String parentPhone(long childUserId){
        UserEntity child=requireUserExists(childUserId);if(!"child".equalsIgnoreCase(child.role))return "";
        String phone=dao.parentPhoneForChild(childUserId);return phone==null?"":phone;
    }
    public int unreadNotificationCount(long userId){requireUserExists(userId);return dao.unreadNotificationCount(userId);}
    public List<BudgetLineEntity> budgetLines(long budgetId,long userId){requireUserExists(userId);BudgetEntity budget=dao.findBudget(budgetId);return budget!=null&&budget.userId==userId?dao.budgetLines(budgetId):Collections.emptyList();}
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
        long periodStart=budget==null?monthStartMillis():budget.periodStart;
        long periodEndExclusive=budget==null?nextMonthStartMillis(periodStart):safeExclusiveEnd(budget.periodEnd);
        GoalEntity goal=childMode?dao.primaryGoal(userId):null;
        TransactionRow latestAllowance=childMode?dao.latestAllowanceReceived(userId):null;
        long totalAllowance=childMode?dao.totalAllowanceReceived(userId):0L;
        int transactionCount=parentMode?dao.familyTransactionCount(userId,periodStart,periodEndExclusive):dao.transactionCount(userId,periodStart,periodEndExclusive);
        int pendingRequests=parentMode?dao.pendingMoneyRequestCount(userId):0;
        Map<Long,Long> envelopes=new HashMap<>();
        if(budget!=null)for(BudgetLineEntity line:dao.budgetLines(budget.id))envelopes.put(line.categoryId,line.limitMilli);
        Map<Long,Long> spending=new HashMap<>();
        for(CategorySpendRow row:dao.categorySpendingBetween(userId,periodStart,periodEndExclusive))spending.put(row.categoryId,row.amountMilli);
        long percentBase=budget==null?0:budget.monthlyLimitMilli;
        ArrayList<HomeCategoryProgress> progress=new ArrayList<>();
        for(CategoryPresentationRow row:dao.selectedCategoryPresentations(userId)){
            long envelope=envelopes.containsKey(row.categoryId)?envelopes.get(row.categoryId):0;
            long spent=spending.containsKey(row.categoryId)?spending.get(row.categoryId):0;
            int planned=percentBase<=0?0:Math.max(0,MoneyMath.percentOf(envelope,percentBase));
            int consumed=envelope<=0?(spent>0?100:0):Math.max(0,MoneyMath.percentOf(spent,envelope));
            progress.add(new HomeCategoryProgress(row.categoryId,row.name,row.colorHex,row.type,row.catalogKey,row.iconName,envelope,spent,planned,consumed));
        }
        SettingEntity settings=dao.systemSettings();
        return new HomeDashboardData(user,budget,categories,recent,profile,children,allocated,parentSpent,progress,settings,goal,latestAllowance,totalAllowance,transactionCount,pendingRequests);
    }
    private static long monthStartMillis(){Calendar month=Calendar.getInstance();month.set(Calendar.DAY_OF_MONTH,1);month.set(Calendar.HOUR_OF_DAY,0);month.set(Calendar.MINUTE,0);month.set(Calendar.SECOND,0);month.set(Calendar.MILLISECOND,0);return month.getTimeInMillis();}
    private static long nextMonthStartMillis(long start){Calendar next=Calendar.getInstance();next.setTimeInMillis(start);next.add(Calendar.MONTH,1);return next.getTimeInMillis();}
    private static long safeExclusiveEnd(long inclusiveEnd){return inclusiveEnd==Long.MAX_VALUE?Long.MAX_VALUE:inclusiveEnd+1;}
}
