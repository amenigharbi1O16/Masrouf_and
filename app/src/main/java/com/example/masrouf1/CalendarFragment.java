package com.example.masrouf1;

import android.widget.LinearLayout;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/** Persistent calendar tab showing this month's locally recorded operations. */
public final class CalendarFragment extends DashboardSectionFragment {
    private LinearLayout rows;
    @Override protected int pageTitle(){return R.string.nav_calendar;}
    @Override protected void createContent(LinearLayout content){rows=new LinearLayout(requireContext());rows.setOrientation(LinearLayout.VERTICAL);content.addView(rows);}
    @Override protected void observeContent(){viewModel.transactions().observe(getViewLifecycleOwner(),state->{if(state.loading)return;rows.removeAllViews();if(state.failed||state.data==null||state.data.isEmpty()){rows.addView(text(getString(R.string.history_none)));return;}DateFormat date=DateFormat.getDateInstance(DateFormat.MEDIUM);for(com.example.masrouf1.db.TransactionRow item:state.data){String line=date.format(new Date(item.occurredAt))+" · "+item.title+" · "+CurrencyFormatter.formatMilli(item.amountMilli);rows.addView(text(line),new LinearLayout.LayoutParams(-1,dp(54)));}});Calendar month=Calendar.getInstance();month.set(Calendar.DAY_OF_MONTH,1);month.set(Calendar.HOUR_OF_DAY,0);month.set(Calendar.MINUTE,0);month.set(Calendar.SECOND,0);month.set(Calendar.MILLISECOND,0);long from=month.getTimeInMillis();month.add(Calendar.MONTH,1);viewModel.loadTransactions(signedInUserId(),from,month.getTimeInMillis()-1);}
}
