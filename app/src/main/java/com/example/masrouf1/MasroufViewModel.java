package com.example.masrouf1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.MutableLiveData;
import android.os.Handler;
import android.os.Looper;

import com.example.masrouf1.db.CategorySpendRow;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.TransactionRow;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Callable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** MVVM boundary: exposes Room LiveData through repositories and survives configuration changes. */
public final class MasroufViewModel extends ViewModel {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final MasroufRepository repository;
    private final MediatorLiveData<DataState<List<ProductEntity>>> products=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<List<CategoryEntity>>> categories=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<List<PriceHistoryEntity>>> history=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<MonthlySpending>> monthlySpending=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<List<TransactionRow>>> transactions=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<UserEntity>> user=new MediatorLiveData<>();
    private final MutableLiveData<DataState<HomeDashboardData>> home=new MutableLiveData<>();
    private final MutableLiveData<Set<String>> categorySelection=new MutableLiveData<>();
    private final MediatorLiveData<DataState<List<NotificationEntity>>> notifications=new MediatorLiveData<>();
    private LiveData<List<ProductEntity>> productSource;
    private LiveData<List<CategoryEntity>> categoriesSource;
    private LiveData<List<PriceHistoryEntity>> historySource;
    private LiveData<List<CategoryEntity>> categorySource;
    private LiveData<List<CategorySpendRow>> spendingSource;
    private LiveData<List<TransactionRow>> transactionSource;
    private LiveData<UserEntity> userSource;
    private LiveData<List<NotificationEntity>> notificationSource;
    private List<CategoryEntity> latestCategories;
    private List<CategorySpendRow> latestSpending;

    /** Typed one-shot repository requests for forms and secondary screens. */
    public interface RequestCallback<T> { void onResult(DataState<T> result); }
    public interface RepositoryRequest<T> { T load(MasroufRepository repository) throws Exception; }
    private final Handler mainHandler=new Handler(Looper.getMainLooper());
    private volatile boolean cleared;

    MasroufViewModel(MasroufRepository repository){this.repository=repository;}
    public LiveData<DataState<List<ProductEntity>>> products(){return products;}
    public LiveData<DataState<List<CategoryEntity>>> categories(){return categories;}
    public LiveData<DataState<List<PriceHistoryEntity>>> history(){return history;}
    public LiveData<DataState<MonthlySpending>> monthlySpending(){return monthlySpending;}
    public LiveData<DataState<List<TransactionRow>>> transactions(){return transactions;}
    public LiveData<DataState<UserEntity>> user(){return user;}
    public LiveData<DataState<HomeDashboardData>> home(){return home;}
    public LiveData<Set<String>> categorySelection(){return categorySelection;}
    public Set<String> currentCategorySelection(){Set<String> value=categorySelection.getValue();return value==null?Collections.emptySet():Collections.unmodifiableSet(new HashSet<>(value));}
    public void initializeCategorySelection(Set<String> defaults){if(categorySelection.getValue()==null)categorySelection.setValue(Collections.unmodifiableSet(new HashSet<>(defaults)));}
    public void toggleCategorySelection(String key){if(key==null)return;Set<String> next=new HashSet<>(currentCategorySelection());if(!next.add(key))next.remove(key);categorySelection.setValue(Collections.unmodifiableSet(next));}
    public LiveData<DataState<List<NotificationEntity>>> notifications(){return notifications;}

    public void loadProducts(long userId){products.setValue(DataState.loading());if(productSource!=null)products.removeSource(productSource);productSource=repository.observeProducts(userId);products.addSource(productSource,rows->products.setValue(DataState.success(rows)));}
    public void loadCategories(long userId){categories.setValue(DataState.loading());if(categoriesSource!=null)categories.removeSource(categoriesSource);categoriesSource=repository.observeCategories(userId);categories.addSource(categoriesSource,rows->categories.setValue(DataState.success(rows)));}
    public void loadHistory(long userId,long productId){history.setValue(DataState.loading());if(historySource!=null)history.removeSource(historySource);historySource=repository.observePriceHistory(userId,productId);history.addSource(historySource,rows->history.setValue(DataState.success(rows)));}
    public void loadMonthlySpending(long userId,long from,long to){
        monthlySpending.setValue(DataState.loading());latestCategories=null;latestSpending=null;
        if(categorySource!=null)monthlySpending.removeSource(categorySource);if(spendingSource!=null)monthlySpending.removeSource(spendingSource);
        categorySource=repository.observeCategories(userId);spendingSource=repository.observeCategorySpending(userId,from,to);
        monthlySpending.addSource(categorySource,rows->{latestCategories=rows;publishMonthly();});
        monthlySpending.addSource(spendingSource,rows->{latestSpending=rows;publishMonthly();});
    }
    private void publishMonthly(){if(latestCategories!=null&&latestSpending!=null)monthlySpending.setValue(DataState.success(new MonthlySpending(latestCategories,latestSpending)));}
    public void loadTransactions(long userId,long from,long to){transactions.setValue(DataState.loading());if(transactionSource!=null)transactions.removeSource(transactionSource);transactionSource=repository.observeTransactions(userId,from,to);transactions.addSource(transactionSource,rows->transactions.setValue(DataState.success(rows)));}
    public void loadUser(long userId){user.setValue(DataState.loading());if(userSource!=null)user.removeSource(userSource);userSource=repository.observeUser(userId);user.addSource(userSource,value->user.setValue(value==null?DataState.failure():DataState.success(value)));}
    public void loadHome(long userId,boolean parentMode,boolean childMode){home.setValue(DataState.loading());IO.execute(()->{try{home.postValue(DataState.success(repository.loadHome(userId,parentMode,childMode)));}catch(RuntimeException failure){home.postValue(DataState.failure());}});}
    public void loadNotifications(long userId){notifications.setValue(DataState.loading());if(notificationSource!=null)notifications.removeSource(notificationSource);notificationSource=repository.observeNotifications(userId);notifications.addSource(notificationSource,rows->notifications.setValue(DataState.success(rows)));}
    public void markNotificationRead(long notificationId,long userId){IO.execute(()->{try{repository.markNotificationRead(notificationId,userId);}catch(RuntimeException ignored){}});}
    public void markAllNotificationsRead(long userId){IO.execute(()->{try{repository.markAllNotificationsRead(userId);}catch(RuntimeException ignored){}});}

    public <T> void request(RepositoryRequest<T> request,RequestCallback<T> callback){
        IO.execute(()->{
            DataState<T> result;
            try{result=DataState.success(request.load(repository));}
            catch(Exception failure){result=DataState.failure();}
            DataState<T> completed=result;
            mainHandler.post(()->{if(!cleared)callback.onResult(completed);});
        });
    }
    @Override protected void onCleared(){cleared=true;super.onCleared();}
}
