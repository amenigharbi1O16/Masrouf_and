package com.masrouf.controller;

import android.content.Context;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

public final class SessionStore {
    private SessionStore() { }
    public static void save(Context context, UserEntity user) {
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit()
                .putLong("user_id", user.id).putString("role", user.role==null?AppRole.USER.value():user.role).apply();
    }
    public static long userId(Context context) {
        return context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).getLong("user_id", 0L);
    }
    public static AppRole role(Context context) {
        return AppRole.from(context.getSharedPreferences("masrouf_preferences",Context.MODE_PRIVATE).getString("role",AppRole.USER.value()));
    }
    public static void clear(Context context) {
        context.getSharedPreferences("masrouf_preferences", Context.MODE_PRIVATE).edit().remove("user_id").remove("role").apply();
    }
}
