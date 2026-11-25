package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;

import java.util.List;

/** Account-scoped category catalog + custom category selections for ManageCategoriesActivity. */
public final class ManageCategoriesData {
    public final boolean childMode;
    public final List<SystemCategoryEntity> catalogCategories;
    public final List<CategoryEntity> customCategories;
    public final List<Long> selectedCatalogIds;
    public final List<Long> selectedCustomCategoryIds;

    public ManageCategoriesData(boolean childMode,
                                List<SystemCategoryEntity> catalogCategories,
                                List<CategoryEntity> customCategories,
                                List<Long> selectedCatalogIds,
                                List<Long> selectedCustomCategoryIds) {
        this.childMode = childMode;
        this.catalogCategories = catalogCategories;
        this.customCategories = customCategories;
        this.selectedCatalogIds = selectedCatalogIds;
        this.selectedCustomCategoryIds = selectedCustomCategoryIds;
    }
}
