package com.example.masrouf1;

import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.TransactionRow;
import java.util.List;

/** Role-filtered dashboard projection assembled from Room records by the repository. */
public final class HomeDashboardData {
    public final UserEntity user;public final BudgetEntity budget;public final List<CategoryEntity> categories;public final List<TransactionRow> recent;
    public final ChildProfileEntity childProfile;public final List<ChildSpendingSummary> children;public final long childAllocations,parentSpent;
    public HomeDashboardData(UserEntity user,BudgetEntity budget,List<CategoryEntity> categories,List<TransactionRow> recent,ChildProfileEntity childProfile,List<ChildSpendingSummary> children,long childAllocations,long parentSpent){
        this.user=user;this.budget=budget;this.categories=categories;this.recent=recent;this.childProfile=childProfile;this.children=children;this.childAllocations=childAllocations;this.parentSpent=parentSpent;
    }
}
