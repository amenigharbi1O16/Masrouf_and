/*
 * ============================================================================
 * FICHIER : DevelopmentSeedManager.java
 * MISSION : Pré-insérer en mode Debug (sans écraser l'existant) trois comptes de démonstration pour les tests.
 * IMPLÉMENTE : Transaction Room idempotente sur Exécuteur IO (admin.masrouf@gmail.com = PARENT,
 *              user.masrouf@gmail.com = USER, enfant.masrouf@gmail.com = CHILD relié à parent).
 * EFFET À L'ÉCRAN : Aucun affichage direct ; permet de se connecter immédiatement en démonstration Debug.
 * COMMUNICATIONS : Appelé dans MasroufApplication.onCreate() et LoginActivity.performLogin().
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufDao;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Initialise uniquement en build Debug trois comptes de test s'ils sont absents.
 */
public final class DevelopmentSeedManager {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final long FAMILY_ID=1L;
    private DevelopmentSeedManager() { }

    /** Déclenche l'initialisation silencieuse au lancement de MasroufApplication (builds Debug uniquement). */
    public static void seedIfDebug(Context context) { ensureSeeded(context,null); }

    /**
     * Vérifie sur l'exécuteur IO qu'aucun compte par défaut n'est manquant dans SQLite en build Debug,
     * insère les comptes manquants en transaction Room sans écraser les données existantes, puis exécute completion sur le thread UI.
     */
    public static void ensureSeeded(Context context,Runnable completion) {
        Context app=context.getApplicationContext();
        // N'exécute jamais le peuplement sur une compilation Release destinée à la production.
        if((app.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0){postCompletion(completion);return;}
        IO.execute(()->{
            try {
                MasroufDatabase database=MasroufDatabase.getInstance(app);
                database.runInTransaction(()->seed(database.masroufDao()));
            } catch(RuntimeException failure) {
                android.util.Log.e("MasroufSeed","Development seed initialization failed",failure);
            } finally { postCompletion(completion); }
        });
    }

    /** Insère uniquement les comptes de test absents (Parent, User, Enfant + lien child_profiles). */
    private static void seed(MasroufDao dao) {
        // 1. Compte foyer PARENT (nom historique admin.masrouf@gmail.com mais role = "parent").
        UserEntity parent=dao.findUserByEmail("admin.masrouf@gmail.com");
        if(parent==null) {
            parent=user("Parent Administrateur","admin.masrouf@gmail.com",AppRole.PARENT.value(),"family",
                    "120000$PBKDF2WithHmacSHA1$3rJC+91N10fAaigivpaeLA==$8yhgu0LsqUYEplTjeI8ZJsqBSuj7lXb4Chvk65Lw1Rg=",150,FAMILY_ID);
            parent.id=insertStable(dao,parent,1L);
        }

        // 2. Compte personnel indépendant USER.
        UserEntity standard=dao.findUserByEmail("user.masrouf@gmail.com");
        if(standard==null) {
            standard=user("Utilisateur Masrouf","user.masrouf@gmail.com",AppRole.USER.value(),"personal",
                    "120000$PBKDF2WithHmacSHA1$tpLKuOcBeqLQutp8HvxgQw==$xckqpB1vlEZaCJtUm4BQfV/dmd/92HQiJFH4PFP18/c=",50,2L);
            standard.id=insertStable(dao,standard,2L);
        }

        // 3. Compte CHILD rattaché au même foyer householdId que parent.
        UserEntity child=dao.findUserByEmail("enfant.masrouf@gmail.com");
        if(child==null&&isParent(parent.role)) {
            child=user("Enfant Masrouf","enfant.masrouf@gmail.com",AppRole.CHILD.value(),"family",
                    "120000$PBKDF2WithHmacSHA1$l2WpRyR2Uuj0npLrPr1zIQ==$Mss2DFDLRd37z+8gFcocYwtQpf8jWTVi4O3rLfklPVc=",0,parent.householdId);
            child.id=insertStable(dao,child,3L);
        }
        // Crée le lien parent-enfant dans child_profiles si absent.
        if(child!=null&&isParent(parent.role)&&"child".equalsIgnoreCase(child.role)
                &&dao.childProfileForUser(child.id)==null) {
            ChildProfileEntity profile=new ChildProfileEntity();profile.parentUserId=parent.id;
            profile.childUserId=child.id;profile.nickname=child.name;profile.parentPhone=parent.parentPhone;
            profile.allocatedTotalMilli=0;profile.createdAt=System.currentTimeMillis();dao.insert(profile);
        }
    }

    /** Réutilise preferredId si libre dans SQLite, sinon laisse l'auto-incrément assigner un nouvel ID. */
    private static long insertStable(MasroufDao dao,UserEntity user,long preferredId) {
        user.id=dao.findUser(preferredId)==null?preferredId:0L;
        return dao.insertNewUser(user);
    }

    /** Construit une entité UserEntity vierge (onboarding requis à la première connexion). */
    private static UserEntity user(String name,String email,String role,String type,String passwordHash,int points,long householdId) {
        UserEntity row=new UserEntity();row.householdId=householdId;row.name=name;row.email=email;row.role=role;
        row.accountType=type;row.passwordHash=passwordHash;row.onboarded=false;row.categoriesSelected=false;
        row.balanceMilli=0;row.points=points;row.createdAt=System.currentTimeMillis();return row;
    }

    private static boolean isParent(String role) {
        return AppRole.PARENT.value().equalsIgnoreCase(role)||"admin".equalsIgnoreCase(role);
    }

    private static void postCompletion(Runnable completion) {
        if(completion!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(completion);
    }
}
