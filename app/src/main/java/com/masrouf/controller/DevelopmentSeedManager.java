package com.masrouf.controller;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Inserts only missing development identities; no existing row or password is modified. */
public final class DevelopmentSeedManager {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private DevelopmentSeedManager() { }

    public static void seedIfDebug(Context context) {
        ensureSeeded(context,null);
    }
    public static void ensureSeeded(Context context,Runnable completion) {
        Context app=context.getApplicationContext();
        if((app.getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)==0){if(completion!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(completion);return;}
        IO.execute(()->{
          try {
            MasroufDatabase database=MasroufDatabase.getInstance(app);
            database.runInTransaction(()->{
                com.example.masrouf1.db.MasroufDao dao=database.masroufDao();
                UserEntity parent=dao.findUserByEmail("admin.masrouf@gmail.com");
                if(parent==null){parent=user("Parent Masrouf","admin.masrouf@gmail.com",AppRole.PARENT.value(),"family","120000$PBKDF2WithHmacSHA1$3rJC+91N10fAaigivpaeLA==$8yhgu0LsqUYEplTjeI8ZJsqBSuj7lXb4Chvk65Lw1Rg=");parent.id=dao.insert(parent);}
                UserEntity standard=dao.findUserByEmail("user.masrouf@gmail.com");
                if(standard==null){standard=user("Utilisateur Masrouf","user.masrouf@gmail.com",AppRole.USER.value(),"personal","120000$PBKDF2WithHmacSHA1$tpLKuOcBeqLQutp8HvxgQw==$1zVYGUlG73pKgP/YnziA4FVXfVyO7F+RhI2DYqW1ZVY=");dao.insert(standard);}
                UserEntity child=dao.findUserByEmail("enfant.masrouf@gmail.com");
                if(child==null&&parent.id>0&&isParent(parent.role)){
                    child=user("Enfant Masrouf","enfant.masrouf@gmail.com",AppRole.CHILD.value(),"family","120000$PBKDF2WithHmacSHA1$l2WpRyR2Uuj0npLrPr1zIQ==$Mss2DFDLRd37z+8gFcocYwtQpf8jWTVi4O3rLfklPVc=");child.householdId=parent.householdId;child.id=dao.insertChildUser(child);
                    ChildProfileEntity profile=new ChildProfileEntity();profile.parentUserId=parent.id;profile.childUserId=child.id;profile.nickname=child.name;profile.parentPhone=parent.parentPhone;profile.allocatedTotalMilli=0;profile.createdAt=System.currentTimeMillis();dao.insert(profile);
                }
            });
          } finally {if(completion!=null)new android.os.Handler(android.os.Looper.getMainLooper()).post(completion);}
        });
    }
    private static UserEntity user(String name,String email,String role,String type,String passwordHash){UserEntity row=new UserEntity();row.householdId=System.currentTimeMillis();row.name=name;row.email=email;row.role=role;row.accountType=type;row.passwordHash=passwordHash;row.onboarded=true;row.balanceMilli=0;row.points=0;row.createdAt=System.currentTimeMillis();return row;}
    private static boolean isParent(String role){return AppRole.PARENT.value().equalsIgnoreCase(role)||"admin".equalsIgnoreCase(role);}
}
