package com.example.masrouf1;

import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Room-backed inputs for the budget allocation editor. */
public final class BudgetAllocationData {
    public final boolean authorized;
    public final List<CategoryEntity> categories;
    public final List<CategoryPresentationRow> presentations;
    public final BudgetEntity budget;
    public final ChildProfileEntity childProfile;
    public final List<BudgetLineEntity> lines;
    public final Map<Long,Long> previousSpending;
    public BudgetAllocationData(boolean authorized,List<CategoryEntity> categories,BudgetEntity budget,ChildProfileEntity childProfile,List<BudgetLineEntity> lines,Map<Long,Long> previousSpending){
        this(authorized,categories,Collections.emptyList(),budget,childProfile,lines,previousSpending);
    }
    public BudgetAllocationData(boolean authorized,List<CategoryEntity> categories,List<CategoryPresentationRow> presentations,BudgetEntity budget,ChildProfileEntity childProfile,List<BudgetLineEntity> lines,Map<Long,Long> previousSpending){
        this.authorized=authorized;this.categories=categories;this.presentations=presentations==null?Collections.emptyList():presentations;this.budget=budget;this.childProfile=childProfile;this.lines=lines;this.previousSpending=previousSpending;
    }
}
