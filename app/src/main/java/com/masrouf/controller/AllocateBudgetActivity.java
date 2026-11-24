package com.masrouf.controller;

import android.os.Bundle;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.BudgetAllocationData;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Separates total budget creation from category envelope allocation. */
public final class AllocateBudgetActivity extends AppCompatActivity {
    private MasroufViewModel viewModel;
    private long userId,totalMilli,start,end,budgetId;
    private boolean childMode,editMode,loading=true;
    private BudgetEntity budget;
    private List<BudgetLineEntity> savedLines=new ArrayList<>();
    private List<CategoryEntity> categories=new ArrayList<>();
    private final Map<Long,Long> oldLines=new HashMap<>();
    private final List<AllocationEntry> entries=new ArrayList<>();
    private LinearLayout rows;
    private TextView totalInfo,sumInfo,unallocatedInfo;
    private View overflowBanner;
    private MaterialButton save;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        childMode=getIntent().getBooleanExtra("IS_CHILD",false); userId=SessionStore.userId(this);
        if(childMode && SessionStore.role(this)!=AppRole.CHILD) { finish(); return; }
        if(!childMode && SessionStore.role(this)==AppRole.CHILD) { Toast.makeText(this,R.string.child_budget_locked,Toast.LENGTH_LONG).show(); finish(); return; }
        setContentView(R.layout.activity_allocate_budget);
        rows=findViewById(R.id.allocate_category_rows); totalInfo=findViewById(R.id.allocate_total_row); sumInfo=findViewById(R.id.allocate_sum_row); unallocatedInfo=findViewById(R.id.allocate_unallocated_row);
        overflowBanner=findViewById(R.id.allocate_overflow_banner); save=findViewById(R.id.allocate_save);
        ((TextView)findViewById(R.id.allocate_title)).setText(childMode?R.string.partition_child_budget:R.string.your_categories);
        findViewById(R.id.allocate_child_locked).setVisibility(childMode?View.VISIBLE:View.GONE);
        if(!childMode) {
            start=getIntent().getLongExtra("START",0L); end=getIntent().getLongExtra("END",0L); totalMilli=getIntent().getLongExtra("TOTAL",0L);
            budgetId=getIntent().getLongExtra("BUDGET_ID",0L); editMode=budgetId>0;
        } else budgetId=getIntent().getLongExtra("BUDGET_ID",0L);
        save.setOnClickListener(v->persistAllocations());
        loadData();
    }

    private void loadData() {
        long requestedStart=start;
        viewModel.<BudgetAllocationData>request(repository->repository.loadBudgetAllocation(userId,childMode,editMode,budgetId,requestedStart),state->{
            BudgetAllocationData data=state.data;
            if(data==null||!data.authorized){Toast.makeText(this,childMode?R.string.no_child_budget:R.string.transaction_saved_error,Toast.LENGTH_LONG).show();finish();return;}
            List<CategoryEntity> loaded=data.categories;BudgetEntity loadedBudget=data.budget;
            if(childMode){
                if(data.childProfile==null||loadedBudget==null){Toast.makeText(this,R.string.no_child_budget,Toast.LENGTH_LONG).show();finish();return;}
                totalMilli=data.childProfile.allocatedTotalMilli>0?data.childProfile.allocatedTotalMilli:loadedBudget.monthlyLimitMilli;start=loadedBudget.periodStart;end=loadedBudget.periodEnd;
            }else if(editMode&&loadedBudget!=null){totalMilli=loadedBudget.monthlyLimitMilli;start=loadedBudget.periodStart;end=loadedBudget.periodEnd;}
            if(totalMilli<=0||loaded.isEmpty()){Toast.makeText(this,loaded.isEmpty()?R.string.select_category_first:R.string.budget_total_required,Toast.LENGTH_LONG).show();finish();return;}
            List<BudgetLineEntity> lines=data.lines;for(BudgetLineEntity line:lines)oldLines.put(line.categoryId,line.limitMilli);
            categories=loaded;budget=loadedBudget;savedLines=new ArrayList<>(lines);
            ((TextView)findViewById(R.id.allocate_total_row)).setText(getString(R.string.allocation_total_row)+"  "+CurrencyFormatter.formatMilli(totalMilli));
            Map<Long,Long> initial=oldLines.isEmpty()?computeAutomatedBudgetAllocations(categories,data.previousSpending,totalMilli):new HashMap<>(oldLines);
            renderRows(initial);loading=false;recalculate();
        });
    }

    /** M-1 actual spending wins; otherwise catalogue weights are normalized to exactly 100%. */
    public static Map<Long,Long> computeAutomatedBudgetAllocations(List<CategoryEntity> categories,Map<Long,Long> previousSpend,long totalMilli) {
        HashMap<Long,Long> result=new HashMap<>(); if(categories.isEmpty()) return result;
        boolean hasHistory=false; for(CategoryEntity category:categories) if(previousSpend.getOrDefault(category.id,0L)>0) hasHistory=true;
        if(hasHistory) {
            ArrayList<Long> weights=new ArrayList<>(categories.size());
            for(CategoryEntity category:categories) weights.add(Math.max(0L,previousSpend.getOrDefault(category.id,0L)));
            List<Long> allocation=MoneyMath.allocate(totalMilli,weights);
            for(int i=0;i<categories.size();i++) result.put(categories.get(i).id,allocation.get(i));
            return result;
        }
        int[] percentages=defaultPercentages(categories); ArrayList<Long> weights=new ArrayList<>(percentages.length);
        for(int percentage:percentages) weights.add((long)percentage);
        List<Long> allocation=MoneyMath.allocate(totalMilli,weights);
        for(int i=0;i<categories.size();i++) result.put(categories.get(i).id,allocation.get(i));
        return result;
    }

    private static int[] defaultPercentages(List<CategoryEntity> categories) {
        int[] weights=new int[categories.size()]; int weightTotal=0;
        for(int i=0;i<categories.size();i++) { weights[i]=defaultWeight(categories.get(i).name); weightTotal+=weights[i]; }
        int[] result=new int[categories.size()]; int sum=0;
        for(int i=0;i<result.length;i++) { result[i]=(weights[i]*100)/weightTotal; sum+=result[i]; }
        for(int i=0;sum<100;i=(i+1)%result.length) { result[i]++; sum++; }
        return result;
    }
    private static int defaultWeight(String name) {
        String value=name==null?"":name.toLowerCase(Locale.ROOT);
        if(value.contains("course")||value.contains("grocer")) return 35;
        if(value.contains("maison")||value.contains("home")) return 20;
        if(value.contains("transport")) return 15;
        if(value.contains("sant")||value.contains("health")) return 10;
        if(value.contains("loisir")||value.contains("leisure")) return 10;
        if(value.contains("éduc")||value.contains("educ")) return 10;
        if(value.contains("jeux")||value.contains("game")) return 20;
        if(value.contains("friand")||value.contains("snack")) return 15;
        if(value.contains("fournitur")||value.contains("school")) return 25;
        return 10;
    }

    private void renderRows(Map<Long,Long> initial) {
        rows.removeAllViews(); entries.clear();
        for(CategoryEntity category:categories) {
            LinearLayout line=new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL); line.setOrientation(LinearLayout.HORIZONTAL); line.setPadding(dp(12),dp(5),dp(8),dp(5));
            TextView name=new TextView(this); name.setText(category.name); name.setTextColor(getColor(R.color.forest)); name.setTextSize(15);
            line.addView(name,new LinearLayout.LayoutParams(0,dp(54),1));
            EditText amount=new EditText(this); amount.setSingleLine(true); amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); amount.setHint("0,000"); amount.setGravity(Gravity.END);
            long value=initial.getOrDefault(category.id,0L); amount.setText(CurrencyFormatter.formatMilli(value).replace(" DT",""));
            line.addView(amount,new LinearLayout.LayoutParams(dp(130),dp(54)));
            rows.addView(line,new LinearLayout.LayoutParams(-1,dp(64)));
            AllocationEntry entry=new AllocationEntry(category,amount); entries.add(entry);
            amount.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
                @Override public void onTextChanged(CharSequence s,int start,int before,int count) { if(!loading) recalculate(); }
                @Override public void afterTextChanged(Editable s) { }
            });
        }
    }

    private void recalculate() {
        long sum=0; boolean invalid=false;
        for(AllocationEntry entry:entries) {
            try { String text=entry.amount.getText().toString().trim(); if(!text.isEmpty()) sum=MoneyMath.add(sum,MoneyInput.toMilli(text)); }
            catch(NumberFormatException|ArithmeticException exception) { invalid=true; }
        }
        long unallocated=MoneyMath.subtract(totalMilli,sum);
        sumInfo.setText(getString(R.string.allocation_sum_row)+"  "+CurrencyFormatter.formatMilli(sum));
        unallocatedInfo.setText(getString(R.string.allocation_unassigned_row)+"  "+CurrencyFormatter.formatMilli(unallocated));
        boolean overflow=invalid||sum>totalMilli; overflowBanner.setVisibility(overflow?View.VISIBLE:View.GONE); save.setEnabled(!overflow);
    }

    private void persistAllocations() {
        ArrayList<BudgetLineEntity> lines=new ArrayList<>(); long sum=0;
        try {
            for(AllocationEntry entry:entries) {
                String raw=entry.amount.getText().toString().trim(); long amount=raw.isEmpty()?0:MoneyInput.toMilli(raw);
                if(amount<0) throw new NumberFormatException(); sum=MoneyMath.add(sum,amount);
                BudgetLineEntity line=new BudgetLineEntity(); line.categoryId=entry.category.id; line.limitMilli=amount;
                BudgetLineEntity previous=null;
                for(BudgetLineEntity old:savedLines) if(old.categoryId==entry.category.id) previous=old;
                line.spentMilli=previous==null?0:previous.spentMilli; lines.add(line);
            }
        } catch(NumberFormatException|ArithmeticException exception) { Toast.makeText(this,R.string.money_invalid,Toast.LENGTH_SHORT).show(); return; }
        if(sum>totalMilli) { recalculate(); return; }
        save.setEnabled(false);
        if(childMode) {
            viewModel.<Boolean>request(repository->repository.replaceChildEnvelopes(budgetId,userId,lines),state->finishToHome(Boolean.TRUE.equals(state.data)));return;
        }
        if(budget==null) { budget=new BudgetEntity(); budget.userId=userId; budget.name="Budget mensuel"; budget.spentMilli=0; budget.status="ACTIVE"; }
        budget.monthlyLimitMilli=totalMilli; budget.periodStart=start; budget.periodEnd=end; budget.status="ACTIVE";
        if(editMode)viewModel.<Boolean>request(repository->repository.updateBudget(budget,lines),state->finishToHome(Boolean.TRUE.equals(state.data)));
        else viewModel.<Long>request(repository->repository.createBudget(budget,lines),state->finishToHome(state.data!=null&&state.data>0));
    }

    private void finishToHome(boolean saved) {
        if(!saved) { save.setEnabled(true); Toast.makeText(this,R.string.transaction_saved_error,Toast.LENGTH_SHORT).show(); return; }
        Toast.makeText(this,R.string.budget_saved,Toast.LENGTH_SHORT).show();
        Intent home=new Intent(this,com.example.masrouf1.MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home); finish();
    }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
    private static final class AllocationEntry {
        final CategoryEntity category; final EditText amount;
        AllocationEntry(CategoryEntity category,EditText amount) { this.category=category; this.amount=amount; }
    }
}
