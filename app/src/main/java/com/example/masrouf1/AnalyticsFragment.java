package com.example.masrouf1;

import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/** Persistent monthly analysis tab, populated from Room transaction aggregates. */
public final class AnalyticsFragment extends DashboardSectionFragment {
    private PillBarChart chart;private LinearLayout rows;
    @Override protected int pageTitle(){return R.string.nav_analytics;}
    @Override protected void createContent(LinearLayout content){content.addView(text(getString(R.string.monthly_spending_analysis)),new LinearLayout.LayoutParams(-1,dp(48)));chart=new PillBarChart(requireContext());content.addView(chart,new LinearLayout.LayoutParams(-1,dp(160)));rows=new LinearLayout(requireContext());rows.setOrientation(LinearLayout.VERTICAL);content.addView(rows);}
    @Override protected void observeContent(){viewModel.monthlySpending().observe(getViewLifecycleOwner(),state->{if(state.loading)return;rows.removeAllViews();if(state.failed||state.data==null){chart.setMilliValues(new ArrayList<>());rows.addView(text(getString(R.string.history_none)));return;}java.util.Map<Long,String> names=new java.util.HashMap<>();for(com.example.masrouf1.db.MasroufEntities.CategoryEntity item:state.data.categories)names.put(item.id,item.name);ArrayList<Long> values=new ArrayList<>();for(com.example.masrouf1.db.CategorySpendRow item:state.data.amounts){values.add(item.amountMilli);rows.addView(text((names.containsKey(item.categoryId)?names.get(item.categoryId):getString(R.string.category_name))+" · "+CurrencyFormatter.formatMilli(item.amountMilli)),new LinearLayout.LayoutParams(-1,dp(42)));}chart.setMilliValues(values);if(values.isEmpty())rows.addView(text(getString(R.string.history_none)),new LinearLayout.LayoutParams(-1,dp(50)));});Calendar month=Calendar.getInstance();month.set(Calendar.DAY_OF_MONTH,1);month.set(Calendar.HOUR_OF_DAY,0);month.set(Calendar.MINUTE,0);month.set(Calendar.SECOND,0);month.set(Calendar.MILLISECOND,0);long from=month.getTimeInMillis();month.add(Calendar.MONTH,1);viewModel.loadMonthlySpending(signedInUserId(),from,month.getTimeInMillis()-1);}
}
