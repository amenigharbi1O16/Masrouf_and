package com.masrouf.controller;

import android.content.Context;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

public final class SessionStore {
    private SessionStore() { }
    public static void save(Context context, UserEntity user) {
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit()
                .putLong("user_id", user.id).putString("role", user.role==null?AppRole.USER.value():user.role)
                .putBoolean("onboarded",user.onboarded).putBoolean("categories_selected",user.categoriesSelected).apply();
    }
    public static long userId(Context context) {
        return context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).getLong("user_id", 0L);
    }
    public static AppRole role(Context context) {
        return AppRole.from(context.getSharedPreferences("masrouf_preferences",Context.MODE_PRIVATE).getString("role",AppRole.USER.value()));
    }
    public static boolean categoriesSelected(Context context){return context.getSharedPreferences("masrouf_preferences",Context.MODE_PRIVATE).getBoolean("categories_selected",false);}
    public static void markOnboardingComplete(Context context){context.getSharedPreferences("masrouf_preferences",Context.MODE_PRIVATE).edit().putBoolean("onboarded",true).putBoolean("categories_selected",true).apply();}
    public static void clear(Context context) {
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit().remove("user_id").remove("role").remove("onboarded").remove("categories_selected").apply();
    }
    public static long selectedBudgetId(Context context, long userId) {
        if (userId <= 0L) return 0L;
        return context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE)
                .getLong("selected_budget_id_" + userId, 0L);
    }
    public static void setSelectedBudgetId(Context context, long userId, long budgetId) {
        if (userId <= 0L) return;
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit()
                .putLong("selected_budget_id_" + userId, Math.max(0L, budgetId)).apply();
    }
}
