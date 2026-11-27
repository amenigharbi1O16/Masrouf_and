/*
 * ============================================================================
 * FICHIER : UserDao.java
 * MISSION : Façade asynchrone historique de type MVC-DAO encapsulant les
 *           opérations utilisateurs et familiales sur MasroufDatabase (connexion,
 *           inscription, onboarding, création de compte enfant, don d'argent,
 *           réinitialisation mot de passe, mise à jour email).
 * IMPLÉMENTE : Classe contrôleur exécutant les appels SQLite Room hors thread
 *              principal via ExecutorService et renvoyant les résultats au
 *              thread UI via android.os.Handler(Looper.getMainLooper()).
 * EFFET À L'ÉCRAN : Aucun directement — transmet les résultats via callbacks
 *                   Result<T> / AuthResult. Les écrans récents privilégient
 *                   MasroufViewModel -> MasroufRepository -> MasroufDao.
 * COMMUNICATIONS :
 *   → MasroufDatabase / MasroufDao (requêtes et transactions SQLite)
 *   → PasswordHasher, InputValidation, BudgetNotificationManager
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Context;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.BudgetNotificationManager;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Façade asynchrone historique de type MVC-DAO encapsulant les opérations comptes/famille sur MasroufDatabase.
 * Exécute chaque requête hors thread principal via IO (ExecutorService) et rappelle le thread UI via Handler.
 * Note architecturale : les écrans actifs passent aujourd'hui par MasroufViewModel -> MasroufRepository -> MasroufDao.
 */
public final class UserDao {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private final MasroufDatabase database;
    private final Context appContext;

    /** Rappel générique retournant le résultat et un code d'erreur éventuel. */
    public interface Result<T> { void complete(T value, String error); }
    /** Rappel spécialisé de connexion retournant UserEntity ou null si échec. */
    public interface AuthResult { void complete(UserEntity user); }

    /** @param context contexte Android utilisé pour obtenir le singleton MasroufDatabase. */
    public UserDao(Context context) { appContext=context.getApplicationContext(); database = MasroufDatabase.getInstance(appContext); }

    /** Vérifie l'email Gmail normalisé et le mot de passe PBKDF2 hors thread UI puis renvoie UserEntity. */
    public void authenticate(String email, String password, AuthResult callback) {
        IO.execute(() -> {
            UserEntity user = null;
            try {
                String normalizedEmail=InputValidation.normalizeGmail(email);
                UserEntity candidate = InputValidation.isStrictGmail(normalizedEmail) ? database.masroufDao().findUserByEmail(normalizedEmail) : null;
                if (candidate != null && PasswordHasher.verify(password, candidate.passwordHash)) user = candidate;
            } catch (RuntimeException ignored) { }
            UserEntity result = user;
            android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
            main.post(() -> callback.complete(result));
        });
    }

    public void register(String name, String email, String password, Result<Long> callback) {
        register(name,email,password,AppRole.USER.value(),"personal",callback);
    }

    public void register(String name,String email,String password,String role,String type,Result<Long> callback) {
        IO.execute(() -> {
            Long id = null; String error = null;
            try {
                String normalizedEmail=InputValidation.normalizeGmail(email);
                if (!InputValidation.isStrictGmail(normalizedEmail)) throw new IllegalArgumentException("gmail");
                if (!InputValidation.isValidPassword(password)) throw new IllegalArgumentException("password");
                if (name==null||name.trim().isEmpty()) throw new IllegalArgumentException("name");
                if (!AppRole.USER.value().equals(role)&&!AppRole.PARENT.value().equals(role)) throw new IllegalArgumentException("role");
                if (database.masroufDao().findUserByEmail(normalizedEmail) != null) throw new IllegalArgumentException("duplicate");
                UserEntity user = new UserEntity(); user.householdId = System.currentTimeMillis();
                user.name = name.trim(); user.email = normalizedEmail; user.role = role;
                user.accountType=type;
                user.passwordHash = PasswordHasher.hash(password); user.onboarded = false; user.categoriesSelected = false;
                user.balanceMilli = 0; user.createdAt = System.currentTimeMillis();
                id = database.masroufDao().insertNewUser(user);
            } catch (Exception exception) { error = exception.getMessage() == null ? "save" : exception.getMessage(); }
            Long result = id; String failure = error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.complete(result, failure));
        });
    }

    public void load(long userId, Result<UserEntity> callback) {
        IO.execute(() -> {
            UserEntity user = null; String error = null;
            try { user = database.masroufDao().findUser(userId); }
            catch (RuntimeException exception) { error = exception.getMessage(); }
            UserEntity result = user; String failure = error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.complete(result, failure));
        });
    }

    public void saveOnboarding(long userId, List<SystemCategoryEntity> categories, Result<Boolean> callback) {
        IO.execute(() -> {
            boolean success = false; String error = null;
            try {
                success = database.masroufDao().finishOnboarding(userId, categories);
            } catch (RuntimeException exception) { error = exception.getMessage(); }
            boolean result = success; String failure = error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.complete(result, failure));
        });
    }

    public void createChildAccount(UserEntity child,ChildProfileEntity profile,BudgetEntity initialBudget,List<CategoryEntity> categories,String plainPassword,Result<Long> callback) {
        IO.execute(()->{
            Long id=null; String error=null;
            try {
                UserEntity parent=profile==null?null:database.masroufDao().findUser(profile.parentUserId);
                if(parent==null||!AppRole.PARENT.value().equalsIgnoreCase(parent.role)&&!"admin".equalsIgnoreCase(parent.role)) throw new SecurityException("parent");
                if(child==null||!AppRole.CHILD.value().equals(child.role)) throw new SecurityException("role");
                child.email=InputValidation.normalizeGmail(child.email);
                if(!InputValidation.isStrictGmail(child.email)) throw new IllegalArgumentException("gmail");
                if(!InputValidation.isValidPassword(plainPassword)) throw new IllegalArgumentException("password");
                if(database.masroufDao().findUserByEmail(child.email)!=null) throw new IllegalArgumentException("duplicate");
                child.passwordHash=PasswordHasher.hash(plainPassword);
                id=database.masroufDao().createChildAccount(child,profile,initialBudget,categories);
                if(id<=0) { id=null; error="parent"; }
            } catch(Exception exception) { error=exception.getMessage()==null?"save":exception.getMessage(); }
            Long result=id; String failure=error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result,failure));
        });
    }

    public void giveMoneyToChild(long profileId,long parentId,long amountMilli,String note,long occurredAt,Result<Integer> callback) {
        IO.execute(()->{
            int result=-3; String error=null;
            try {
                result=database.masroufDao().giveMoneyToChild(profileId,parentId,amountMilli,note,occurredAt);
                if(result==0) { com.example.masrouf1.db.MasroufEntities.BudgetEntity budget=database.masroufDao().activeBudgetForUser(parentId); if(budget!=null) BudgetNotificationManager.reportBudgetProgress(appContext,budget.spentMilli,budget.monthlyLimitMilli,parentId,budget.id,budget.periodStart); }
            }
            catch(Exception exception) { error=exception.getMessage()==null?"save":exception.getMessage(); }
            int value=result; String failure=error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(value,failure));
        });
    }

    public void resetChildPassword(long profileId,long parentId,String temporaryPassword,Result<Boolean> callback) {
        IO.execute(()->{
            boolean saved=false; String error=null;
            try { UserEntity parent=database.masroufDao().findUser(parentId); if(parent==null||!AppRole.PARENT.value().equalsIgnoreCase(parent.role)&&!"admin".equalsIgnoreCase(parent.role)) throw new SecurityException("parent"); if(!InputValidation.isValidPassword(temporaryPassword)) throw new IllegalArgumentException("password"); saved=database.masroufDao().resetChildPassword(profileId,parentId,PasswordHasher.hash(temporaryPassword))>0; }
            catch(Exception exception) { error=exception.getMessage()==null?"save":exception.getMessage(); }
            boolean result=saved; String failure=error;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result,failure));
        });
    }

    public boolean updateOnboarding(long userId, boolean onboarded) {
        return database.masroufDao().updateOnboarding(userId, onboarded) > 0;
    }

    public void updateEmail(long userId,String email,Result<Boolean> callback) {
        IO.execute(()->{boolean saved=false;String error=null;try{String normalizedEmail=InputValidation.normalizeGmail(email);if(!InputValidation.isStrictGmail(normalizedEmail))throw new IllegalArgumentException("gmail");UserEntity existing=database.masroufDao().findUserByEmail(normalizedEmail);if(existing!=null&&existing.id!=userId)throw new IllegalArgumentException("duplicate");saved=database.masroufDao().updateEmail(userId,normalizedEmail)>0;}catch(Exception exception){error=exception.getMessage()==null?"save":exception.getMessage();}boolean result=saved;String failure=error;new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.complete(result,failure));});
    }
}
