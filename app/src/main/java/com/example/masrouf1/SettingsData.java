package com.example.masrouf1;

import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

import java.util.Collections;
import java.util.List;

/** Complete payload for SettingsFragment so every section renders immediately without submenus. */
public final class SettingsData {
    public final UserEntity user;
    public final List<CategoryPresentationRow> categories;
    public final List<CategoryEntity> rawCategories;
    public final BudgetEntity activeBudget;
    public final ChildProfileEntity childProfile;
    public final long totalAllowanceMilli;
    public final long childSpentMilli;
    public final String parentPhone;

    public SettingsData(UserEntity user,
                        List<CategoryPresentationRow> categories,
                        List<CategoryEntity> rawCategories,
                        BudgetEntity activeBudget,
                        ChildProfileEntity childProfile,
                        long totalAllowanceMilli,
                        String parentPhone) {
        this(user, categories, rawCategories, activeBudget, childProfile, totalAllowanceMilli,
                activeBudget == null ? 0L : activeBudget.spentMilli, parentPhone);
    }

    public SettingsData(UserEntity user,
                        List<CategoryPresentationRow> categories,
                        List<CategoryEntity> rawCategories,
                        BudgetEntity activeBudget,
                        ChildProfileEntity childProfile,
                        long totalAllowanceMilli,
                        long childSpentMilli,
                        String parentPhone) {
        this.user = user;
        this.categories = categories == null ? Collections.emptyList() : categories;
        this.rawCategories = rawCategories == null ? Collections.emptyList() : rawCategories;
        this.activeBudget = activeBudget;
        this.childProfile = childProfile;
        this.totalAllowanceMilli = totalAllowanceMilli;
        this.childSpentMilli = Math.max(0L, childSpentMilli);
        this.parentPhone = parentPhone == null ? "" : parentPhone;
    }
}
