package com.example.masrouf1;

import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.masrouf1.db.CategorySpendRow;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/** Monthly statistics based on the signed-in account's Room ledger. */
public final class AnalyticsFragment extends DashboardSectionFragment {
    private PillBarChart chart;private LinearLayout rows;private TextView comparison;
    @Override protected int pageTitle(){return R.string.nav_analytics;}
    @Override protected void createContent(LinearLayout content){
        content.addView(text(getString(R.string.monthly_spending_analysis)),new LinearLayout.LayoutParams(-1,dp(48)));
        comparison=text(getString(R.string.analytics_comparison_loading));content.addView(comparison,new LinearLayout.LayoutParams(-1,dp(52)));
        chart=new PillBarChart(requireContext());content.addView(chart,new LinearLayout.LayoutParams(-1,dp(160)));
        rows=new LinearLayout(requireContext());rows.setOrientation(LinearLayout.VERTICAL);content.addView(rows);
    }
    @Override protected void observeContent(){
        Calendar current=Calendar.getInstance();current.set(Calendar.DAY_OF_MONTH,1);current.set(Calendar.HOUR_OF_DAY,0);current.set(Calendar.MINUTE,0);current.set(Calendar.SECOND,0);current.set(Calendar.MILLISECOND,0);
        long currentFrom=current.getTimeInMillis();current.add(Calendar.MONTH,1);long currentTo=current.getTimeInMillis();current.add(Calendar.MONTH,-2);long previousFrom=current.getTimeInMillis();current.add(Calendar.MONTH,1);long previousTo=current.getTimeInMillis();
        long owner=signedInUserId();viewModel.loadMonthlySpending(owner,currentFrom,currentTo);
        viewModel.<MonthlyComparison>request(repository->repository.monthlyComparison(owner,currentFrom,currentTo,previousFrom,previousTo),state->{
            if(!isAdded())return;if(state.failed||state.data==null){comparison.setText(R.string.analytics_comparison_unavailable);return;}
            MonthlyComparison value=state.data;comparison.setText(value.percentChange==null
                    ?getString(R.string.analytics_comparison_no_previous,CurrencyFormatter.formatMilli(value.currentMilli),CurrencyFormatter.formatMilli(value.previousMilli))
                    :getString(R.string.analytics_comparison,CurrencyFormatter.formatMilli(value.currentMilli),CurrencyFormatter.formatMilli(value.previousMilli),value.percentChange));
        });
        viewModel.monthlySpending().observe(getViewLifecycleOwner(),state->{
            if(state.loading)return;rows.removeAllViews();
            if(state.failed||state.data==null){chart.setMilliValues(new ArrayList<>());rows.addView(text(getString(R.string.history_none)));return;}
            Map<Long,CategoryEntity> categories=new HashMap<>();for(CategoryEntity item:state.data.categories)categories.put(item.id,item);
            ArrayList<Long> values=new ArrayList<>();long essentials=0,leisure=0;
            for(CategorySpendRow item:state.data.amounts){
                values.add(item.amountMilli);CategoryEntity category=categories.get(item.categoryId);
                String name=category==null?getString(R.string.category_name):category.name;
                rows.addView(text(name+" · "+CurrencyFormatter.formatMilli(item.amountMilli)),new LinearLayout.LayoutParams(-1,dp(42)));
                if(category!=null){if(category.isEssential)essentials=safeAdd(essentials,item.amountMilli);else leisure=safeAdd(leisure,item.amountMilli);}
            }
            chart.setMilliValues(values);
            if(!state.data.amounts.isEmpty()){
                boolean classificationAvailable=!state.data.categories.isEmpty();
                if(classificationAvailable){rows.addView(text(getString(R.string.analytics_essentials,CurrencyFormatter.formatMilli(essentials))),new LinearLayout.LayoutParams(-1,dp(42)));rows.addView(text(getString(R.string.analytics_leisure,CurrencyFormatter.formatMilli(leisure))),new LinearLayout.LayoutParams(-1,dp(42)));}
            }else rows.addView(text(getString(R.string.history_none)),new LinearLayout.LayoutParams(-1,dp(50)));
        });
    }
    private long safeAdd(long left,long right){try{return Math.addExact(left,right);}catch(ArithmeticException overflow){return Long.MAX_VALUE;}}
}
