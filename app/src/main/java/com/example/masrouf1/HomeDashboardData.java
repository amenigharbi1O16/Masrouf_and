package com.example.masrouf1;

import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.MasroufEntities.SettingEntity;
import com.example.masrouf1.db.MasroufEntities.GoalEntity;
import com.example.masrouf1.db.TransactionRow;
import java.util.List;

/** Role-filtered dashboard projection assembled from Room records by the repository. */
public final class HomeDashboardData {
    public final UserEntity user;public final BudgetEntity budget;public final List<CategoryEntity> categories;public final List<TransactionRow> recent;
    public final ChildProfileEntity childProfile;public final List<ChildSpendingSummary> children;public final long childAllocations,parentSpent;
    public final List<HomeCategoryProgress> categoryProgress; public final SettingEntity settings;
    public final GoalEntity goal; public final TransactionRow latestAllowance; public final long totalAllowanceMilli; public final int transactionCount,pendingRequestCount;
    public HomeDashboardData(UserEntity user,BudgetEntity budget,List<CategoryEntity> categories,List<TransactionRow> recent,ChildProfileEntity childProfile,List<ChildSpendingSummary> children,long childAllocations,long parentSpent,List<HomeCategoryProgress> categoryProgress,SettingEntity settings,GoalEntity goal,TransactionRow latestAllowance,long totalAllowanceMilli,int transactionCount,int pendingRequestCount){
        this.user=user;this.budget=budget;this.categories=categories;this.recent=recent;this.childProfile=childProfile;this.children=children;this.childAllocations=childAllocations;this.parentSpent=parentSpent;this.categoryProgress=categoryProgress;this.settings=settings;this.goal=goal;this.latestAllowance=latestAllowance;this.totalAllowanceMilli=totalAllowanceMilli;this.transactionCount=transactionCount;this.pendingRequestCount=pendingRequestCount;
    }
}
