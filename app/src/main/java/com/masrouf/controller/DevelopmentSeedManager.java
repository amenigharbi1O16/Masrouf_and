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

/** Inserts only missing development identities; existing balances, points, and hashes are never reset. */
public final class DevelopmentSeedManager {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private static final long FAMILY_ID=1L;
    private DevelopmentSeedManager() { }

    public static void seedIfDebug(Context context) { ensureSeeded(context,null); }

    public static void ensureSeeded(Context context,Runnable completion) {
        Context app=context.getApplicationContext();
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

    private static void seed(MasroufDao dao) {
        UserEntity parent=dao.findUserByEmail("admin.masrouf@gmail.com");
        if(parent==null) {
            parent=user("Parent Administrateur","admin.masrouf@gmail.com",AppRole.PARENT.value(),"family",
                    "120000$PBKDF2WithHmacSHA1$3rJC+91N10fAaigivpaeLA==$8yhgu0LsqUYEplTjeI8ZJsqBSuj7lXb4Chvk65Lw1Rg=",150,FAMILY_ID);
            parent.id=insertStable(dao,parent,1L);
        }

        UserEntity standard=dao.findUserByEmail("user.masrouf@gmail.com");
        if(standard==null) {
            standard=user("Utilisateur Masrouf","user.masrouf@gmail.com",AppRole.USER.value(),"personal",
                    "120000$PBKDF2WithHmacSHA1$tpLKuOcBeqLQutp8HvxgQw==$xckqpB1vlEZaCJtUm4BQfV/dmd/92HQiJFH4PFP18/c=",50,2L);
            standard.id=insertStable(dao,standard,2L);
        }

        UserEntity child=dao.findUserByEmail("enfant.masrouf@gmail.com");
        if(child==null&&isParent(parent.role)) {
            child=user("Enfant Masrouf","enfant.masrouf@gmail.com",AppRole.CHILD.value(),"family",
                    "120000$PBKDF2WithHmacSHA1$l2WpRyR2Uuj0npLrPr1zIQ==$Mss2DFDLRd37z+8gFcocYwtQpf8jWTVi4O3rLfklPVc=",0,parent.householdId);
            child.id=insertStable(dao,child,3L);
        }
        if(child!=null&&isParent(parent.role)&&"child".equalsIgnoreCase(child.role)
                &&dao.childProfileForUser(child.id)==null) {
            ChildProfileEntity profile=new ChildProfileEntity();profile.parentUserId=parent.id;
            profile.childUserId=child.id;profile.nickname=child.name;profile.parentPhone=parent.parentPhone;
            profile.allocatedTotalMilli=0;profile.createdAt=System.currentTimeMillis();dao.insert(profile);
        }
    }

    private static long insertStable(MasroufDao dao,UserEntity user,long preferredId) {
        user.id=dao.findUser(preferredId)==null?preferredId:0L;
        return dao.insertNewUser(user);
    }

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
