package com.example.masrouf1;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.masrouf1.db.TransactionRow;
import com.google.android.material.button.MaterialButton;
import com.masrouf.controller.TransactionDetailsActivity;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Monthly local ledger calendar; day markers and totals are derived from Room rows only. */
public final class CalendarFragment extends DashboardSectionFragment {
    private final Calendar month=Calendar.getInstance();
    private int selectedDay;
    private List<TransactionRow> monthTransactions=new ArrayList<>();
    private TextView monthTitle,selectedDate,dayTotal,state;
    private LinearLayout weekdays,days,dayRows;

    @Override protected int pageTitle(){return R.string.nav_calendar;}
    @Override protected void createContent(LinearLayout content){
        month.set(Calendar.DAY_OF_MONTH,1);selectedDay=Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        View root=LayoutInflater.from(requireContext()).inflate(R.layout.fragment_calendar,content,false);content.addView(root);
        monthTitle=root.findViewById(R.id.calendar_month_title);selectedDate=root.findViewById(R.id.calendar_selected_date);dayTotal=root.findViewById(R.id.calendar_day_total);
        weekdays=root.findViewById(R.id.calendar_weekdays);days=root.findViewById(R.id.calendar_days);dayRows=root.findViewById(R.id.calendar_day_transactions);state=root.findViewById(R.id.calendar_state);
        ((MaterialButton)root.findViewById(R.id.calendar_previous)).setOnClickListener(v->{month.add(Calendar.MONTH,-1);selectedDay=1;loadMonth();});
        ((MaterialButton)root.findViewById(R.id.calendar_next)).setOnClickListener(v->{month.add(Calendar.MONTH,1);selectedDay=1;loadMonth();});
        renderWeekdays();renderMonthTitle();
    }
    @Override protected void observeContent(){
        viewModel.transactions().observe(getViewLifecycleOwner(),result->{
            if(result.loading){state.setVisibility(View.GONE);getView().findViewById(R.id.calendar_loading).setVisibility(View.VISIBLE);return;}
            getView().findViewById(R.id.calendar_loading).setVisibility(View.GONE);
            if(result.failed||result.data==null){monthTransactions=new ArrayList<>();state.setText(R.string.dashboard_read_error);state.setVisibility(View.VISIBLE);renderCalendar();renderSelectedDay();return;}
            monthTransactions=result.data;state.setText(monthTransactions.isEmpty()?R.string.calendar_no_transactions:R.string.calendar_transactions_hint);state.setVisibility(monthTransactions.isEmpty()?View.VISIBLE:View.GONE);renderCalendar();renderSelectedDay();
        });
        loadMonth();
    }
    @Override public void onResume(){super.onResume();if(isAdded()&&viewModel!=null)loadMonth();}

    private void loadMonth(){
        renderMonthTitle();Calendar start=(Calendar)month.clone();start.set(Calendar.DAY_OF_MONTH,1);setStartOfDay(start);
        Calendar end=(Calendar)start.clone();end.add(Calendar.MONTH,1);end.add(Calendar.MILLISECOND,-1);
        if(getView()!=null)getView().findViewById(R.id.calendar_loading).setVisibility(View.VISIBLE);
        viewModel.loadTransactions(signedInUserId(),start.getTimeInMillis(),end.getTimeInMillis());
    }
    private void renderMonthTitle(){if(monthTitle!=null)monthTitle.setText(new SimpleDateFormat("MMMM yyyy",Locale.getDefault()).format(month.getTime()));}
    private void renderWeekdays(){
        weekdays.removeAllViews();SimpleDateFormat format=new SimpleDateFormat("EE",Locale.getDefault());Calendar date=Calendar.getInstance();date.set(2024,Calendar.JANUARY,1,12,0,0);
        for(int i=0;i<7;i++){TextView label=calendarText(format.format(date.getTime()),12);label.setTextColor(requireContext().getColor(R.color.muted_green));label.setTypeface(null,android.graphics.Typeface.BOLD);weekdays.addView(label,new LinearLayout.LayoutParams(0,dp(36),1));date.add(Calendar.DAY_OF_MONTH,1);}
    }
    private void renderCalendar(){
        if(days==null||!isAdded())return;days.removeAllViews();renderMonthTitle();
        Map<Integer,Long> totals=new HashMap<>();Map<Integer,Integer> counts=new HashMap<>();
        Calendar txDate=Calendar.getInstance();for(TransactionRow tx:monthTransactions){txDate.setTimeInMillis(tx.occurredAt);int key=txDate.get(Calendar.DAY_OF_MONTH);totals.put(key,MoneyMath.add(totals.containsKey(key)?totals.get(key):0L,tx.amountMilli));counts.put(key,(counts.containsKey(key)?counts.get(key):0)+1);}
        Calendar first=(Calendar)month.clone();first.set(Calendar.DAY_OF_MONTH,1);int offset=(first.get(Calendar.DAY_OF_WEEK)-Calendar.MONDAY+7)%7;int length=month.getActualMaximum(Calendar.DAY_OF_MONTH);int cells=((offset+length+6)/7)*7;
        LinearLayout row=null;for(int cell=0;cell<cells;cell++){
            if(cell%7==0){row=new LinearLayout(requireContext());row.setOrientation(LinearLayout.HORIZONTAL);days.addView(row,new LinearLayout.LayoutParams(-1,dp(50)));}
            LinearLayout cellBox=new LinearLayout(requireContext());cellBox.setGravity(Gravity.CENTER);cellBox.setOrientation(LinearLayout.VERTICAL);int day=cell-offset+1;
            if(day<1||day>length){row.addView(cellBox,new LinearLayout.LayoutParams(0,-1,1));continue;}
            TextView number=calendarText(String.valueOf(day),14);number.setTextColor(requireContext().getColor(day==selectedDay?R.color.forest:R.color.ink));number.setTypeface(null,day==selectedDay?android.graphics.Typeface.BOLD:android.graphics.Typeface.NORMAL);cellBox.addView(number);
            if(counts.containsKey(day)){TextView marker=calendarText("•",11);marker.setTextColor(requireContext().getColor(R.color.emerald));cellBox.addView(marker);cellBox.setContentDescription(getString(R.string.calendar_day_accessibility,day,CurrencyFormatter.formatMilli(totals.get(day))));}
            else cellBox.setContentDescription(String.valueOf(day));
            if(day==selectedDay){GradientDrawable selected=new GradientDrawable();selected.setColor(requireContext().getColor(R.color.mint_light));selected.setCornerRadius(dp(14));cellBox.setBackground(selected);}
            cellBox.setOnClickListener(v->{selectedDay=day;renderCalendar();renderSelectedDay();});row.addView(cellBox,new LinearLayout.LayoutParams(0,-1,1));
        }
    }
    private void renderSelectedDay(){
        if(!isAdded())return;Calendar selected=(Calendar)month.clone();selected.set(Calendar.DAY_OF_MONTH,selectedDay);setStartOfDay(selected);long start=selected.getTimeInMillis();selected.add(Calendar.DAY_OF_MONTH,1);long end=selected.getTimeInMillis();
        List<TransactionRow> rows=new ArrayList<>();long total=0;for(TransactionRow tx:monthTransactions)if(tx.occurredAt>=start&&tx.occurredAt<end){rows.add(tx);total=MoneyMath.add(total,tx.amountMilli);}
        selectedDate.setText(DateFormat.getDateInstance(DateFormat.FULL,Locale.getDefault()).format(new Date(start)));dayTotal.setText(getString(R.string.calendar_day_total,CurrencyFormatter.formatMilli(total)));dayRows.removeAllViews();
        if(rows.isEmpty()){TextView empty=calendarText(getString(R.string.calendar_day_empty),14);empty.setTextColor(requireContext().getColor(R.color.muted_green));dayRows.addView(empty);return;}
        for(TransactionRow tx:rows){String label=tx.title==null||tx.title.trim().isEmpty()?getString(R.string.transaction_name):tx.title;TextView line=calendarText(label+" · "+CurrencyFormatter.formatMilli(tx.amountMilli),14);line.setTextColor(requireContext().getColor(R.color.ink));line.setPadding(dp(8),dp(8),dp(8),dp(8));line.setOnClickListener(v->startActivity(new Intent(requireContext(),TransactionDetailsActivity.class).putExtra("TRANSACTION_ID",tx.id)));dayRows.addView(line,new LinearLayout.LayoutParams(-1,-2));}
    }
    private TextView calendarText(String value,int size){TextView text=new TextView(requireContext());text.setText(value);text.setTextSize(size);text.setGravity(Gravity.CENTER);text.setMaxLines(2);return text;}
    private void setStartOfDay(Calendar value){value.set(Calendar.HOUR_OF_DAY,0);value.set(Calendar.MINUTE,0);value.set(Calendar.SECOND,0);value.set(Calendar.MILLISECOND,0);}
}
