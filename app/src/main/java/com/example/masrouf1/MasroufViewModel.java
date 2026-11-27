/*
 * ============================================================================
 * FICHIER : MasroufViewModel.java
 * MISSION : ViewModel central de l'architecture MVVM. Fait le pont entre
 *           les écrans (Activities/Fragments) et le MasroufRepository.
 *           Expose des LiveData réactives encapsulées dans DataState
 *           (LOADING / SUCCESS / ERROR) et survit aux changements de
 *           configuration (rotation d'écran).
 * IMPLÉMENTE : ViewModel avec MediatorLiveData combinant plusieurs sources
 *              Room, ExecutorService mono-thread pour les opérations I/O,
 *              Handler(Looper.getMainLooper()) pour le retour sur le thread UI,
 *              interface générique RequestCallback<T> pour les requêtes ponctuelles.
 * EFFET À L'ÉCRAN : Aucun directement — alimente les observateurs LiveData
 *                   des Fragments HomeFragment, ProductsFragment, CalendarFragment,
 *                   AnalyticsFragment, SettingsFragment et les formulaires.
 * COMMUNICATIONS :
 *   ← MasroufRepository (toutes les opérations de données)
 *   → HomeFragment, ProductsFragment, CalendarFragment, AnalyticsFragment,
 *     SettingsFragment, AddExpenseActivity, AddProductActivity, etc.
 *   ← MasroufViewModelFactory (instanciation avec injection du Repository)
 * ============================================================================
 */
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

/**
 * ViewModel central (architecture MVVM) : relie l'interface utilisateur au MasroufRepository,
 * expose des LiveData observées par les Fragments/Activities et survit aux rotations d'écran.
 */
/** MVVM boundary: exposes Room LiveData through repositories and survives configuration changes. */
public final class MasroufViewModel extends ViewModel {
    // Exécuteur mono-thread dédié aux lectures/écritures hors thread principal (UI).
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final MasroufRepository repository;
    // Flux réactifs observables encapsulés dans DataState (LOADING / SUCCESS / ERROR).
    private final MediatorLiveData<DataState<List<ProductEntity>>> products=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<List<CategoryEntity>>> categories=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<List<PriceHistoryEntity>>> history=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<MonthlySpending>> monthlySpending=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<List<TransactionRow>>> transactions=new MediatorLiveData<>();
    private final MediatorLiveData<DataState<UserEntity>> user=new MediatorLiveData<>();
    private final MutableLiveData<DataState<HomeDashboardData>> home=new MutableLiveData<>();
    private final MutableLiveData<Set<String>> categorySelection=new MutableLiveData<>();
    private final MediatorLiveData<DataState<List<NotificationEntity>>> notifications=new MediatorLiveData<>();
    // Références sur les sources Room actives pour pouvoir les détacher avant un rechargement.
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
    // Handler rattaché au Looper principal pour renvoyer le résultat d'une requête asynchrone sur le thread UI.
    private final Handler mainHandler=new Handler(Looper.getMainLooper());
    private volatile boolean cleared;

    MasroufViewModel(MasroufRepository repository){this.repository=repository;}
    public LiveData<DataState<List<ProductEntity>>> products(){return products;}
    public LiveData<DataState<List<CategoryEntity>>> categories(){return categories;}
    public LiveData<DataState<List<PriceHistoryEntity>>> history(){return history;}
    public LiveData<DataState<MonthlySpending>> monthlySpending(){return monthlySpending;}
    public LiveData<DataState<List<TransactionRow>>> transactions(){return transactions;}
    /** Retourne le flux réactif (LiveData) de l'état de l'utilisateur courant. */
    public LiveData<DataState<UserEntity>> user(){return user;}
    /** Retourne le flux réactif complet du tableau de bord Accueil (budget, catégories, transactions, enfants). */
    public LiveData<DataState<HomeDashboardData>> home(){return home;}
    /** Retourne le flux réactif de sélection des catégories pendant l'onboarding. */
    public LiveData<Set<String>> categorySelection(){return categorySelection;}
    /** Retourne une copie immuable de la sélection actuelle de clés de catégories. */
    public Set<String> currentCategorySelection(){Set<String> value=categorySelection.getValue();return value==null?Collections.emptySet():Collections.unmodifiableSet(new HashSet<>(value));}
    /** Initialise les catégories présélectionnées une seule fois si aucun état n'existe encore. */
    public void initializeCategorySelection(Set<String> defaults){if(categorySelection.getValue()==null)categorySelection.setValue(Collections.unmodifiableSet(new HashSet<>(defaults)));}
    /** Bascule la sélection d'une catégorie (ajout ou retrait) lors de l'étape d'onboarding. */
    public void toggleCategorySelection(String key){if(key==null)return;Set<String> next=new HashSet<>(currentCategorySelection());if(!next.add(key))next.remove(key);categorySelection.setValue(Collections.unmodifiableSet(next));}
    /** Retourne le flux réactif des notifications persistées pour l'utilisateur courant. */
    public LiveData<DataState<List<NotificationEntity>>> notifications(){return notifications;}

    // Observe en temps réel la liste des produits de l'utilisateur depuis Room.
    public void loadProducts(long userId){products.setValue(DataState.loading());if(productSource!=null)products.removeSource(productSource);productSource=repository.observeProducts(userId);products.addSource(productSource,rows->products.setValue(DataState.success(rows)));}
    // Observe les catégories actives de l'utilisateur.
    public void loadCategories(long userId){categories.setValue(DataState.loading());if(categoriesSource!=null)categories.removeSource(categoriesSource);categoriesSource=repository.observeCategories(userId);categories.addSource(categoriesSource,rows->categories.setValue(DataState.success(rows)));}
    // Observe l'historique des prix d'un produit donné.
    public void loadHistory(long userId,long productId){history.setValue(DataState.loading());if(historySource!=null)history.removeSource(historySource);historySource=repository.observePriceHistory(userId,productId);history.addSource(historySource,rows->history.setValue(DataState.success(rows)));}
    // Combine deux sources LiveData (catégories + totaux dépensés) pour construire le résumé mensuel.
    public void loadMonthlySpending(long userId,long from,long to){
        monthlySpending.setValue(DataState.loading());latestCategories=null;latestSpending=null;
        if(categorySource!=null)monthlySpending.removeSource(categorySource);if(spendingSource!=null)monthlySpending.removeSource(spendingSource);
        categorySource=repository.observeCategories(userId);spendingSource=repository.observeCategorySpending(userId,from,to);
        monthlySpending.addSource(categorySource,rows->{latestCategories=rows;publishMonthly();});
        monthlySpending.addSource(spendingSource,rows->{latestSpending=rows;publishMonthly();});
    }
    private void publishMonthly(){if(latestCategories!=null&&latestSpending!=null)monthlySpending.setValue(DataState.success(new MonthlySpending(latestCategories,latestSpending)));}
    // Observe les transactions récentes dans la fenêtre [from, to].
    public void loadTransactions(long userId,long from,long to){transactions.setValue(DataState.loading());if(transactionSource!=null)transactions.removeSource(transactionSource);transactionSource=repository.observeTransactions(userId,from,to);transactions.addSource(transactionSource,rows->transactions.setValue(DataState.success(rows)));}
    // Observe la fiche utilisateur courante (solde, points, statut onboarding).
    public void loadUser(long userId){user.setValue(DataState.loading());if(userSource!=null)user.removeSource(userSource);userSource=repository.observeUser(userId);user.addSource(userSource,value->user.setValue(value==null?DataState.failure():DataState.success(value)));}
    // Charge de manière asynchrone l'ensemble des données du tableau de bord Accueil.
    public void loadHome(long userId,boolean parentMode,boolean childMode){loadHome(userId,parentMode,childMode,0L);}
    public void loadHome(long userId,boolean parentMode,boolean childMode,long preferredBudgetId){home.setValue(DataState.loading());IO.execute(()->{try{home.postValue(DataState.success(repository.loadHome(userId,parentMode,childMode,preferredBudgetId)));}catch(RuntimeException failure){home.postValue(DataState.failure());}});}
    // Observe les notifications locales stockées en base Room.
    public void loadNotifications(long userId){notifications.setValue(DataState.loading());if(notificationSource!=null)notifications.removeSource(notificationSource);notificationSource=repository.observeNotifications(userId);notifications.addSource(notificationSource,rows->notifications.setValue(DataState.success(rows)));}
    public void markNotificationRead(long notificationId,long userId){IO.execute(()->{try{repository.markNotificationRead(notificationId,userId);}catch(RuntimeException ignored){}});}
    public void markAllNotificationsRead(long userId){IO.execute(()->{try{repository.markAllNotificationsRead(userId);}catch(RuntimeException ignored){}});}

    /** Exécute une opération ponctuelle sur le Repository hors UI thread et renvoie le résultat au callback si le ViewModel est encore actif. */
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
