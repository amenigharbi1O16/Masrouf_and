package com.example.masrouf1;

import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;

import java.util.Collections;
import java.util.List;

/** Owner-scoped categories, presentations, and active budget needed to create a purchase or expense. */
public final class ProductFormData {
    public final BudgetEntity budget;
    public final List<CategoryEntity> categories;
    public final List<CategoryPresentationRow> presentations;

    public ProductFormData(BudgetEntity budget, List<CategoryEntity> categories) {
        this(budget, categories, Collections.emptyList());
    }

    public ProductFormData(BudgetEntity budget, List<CategoryEntity> categories,
                           List<CategoryPresentationRow> presentations) {
        this.budget = budget;
        this.categories = categories == null ? Collections.emptyList() : categories;
        this.presentations = presentations == null ? Collections.emptyList() : presentations;
    }
}
