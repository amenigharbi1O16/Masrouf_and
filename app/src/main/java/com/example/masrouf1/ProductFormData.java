package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import java.util.List;

/** Owner-scoped categories and active budget needed to create a purchase. */
public final class ProductFormData {
    public final BudgetEntity budget;
    public final List<CategoryEntity> categories;
    public ProductFormData(BudgetEntity budget,List<CategoryEntity> categories){this.budget=budget;this.categories=categories;}
}
