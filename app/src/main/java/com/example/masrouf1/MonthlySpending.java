package com.example.masrouf1;

import com.example.masrouf1.db.CategorySpendRow;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import java.util.List;

/** Room aggregate and category labels for one reporting period. */
public final class MonthlySpending {
    public final List<CategoryEntity> categories;
    public final List<CategorySpendRow> amounts;
    public MonthlySpending(List<CategoryEntity> categories,List<CategorySpendRow> amounts){this.categories=categories;this.amounts=amounts;}
}
