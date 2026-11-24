package com.masrouf.controller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/** Captures a user's pay-cycle dates and total; allocation is a separate next step. */
public final class BudgetPeriodActivity extends AppCompatActivity {
    private MasroufViewModel viewModel;
    private final SimpleDateFormat dateFormat=new SimpleDateFormat("dd/MM/yyyy",Locale.ROOT);
    private EditText startInput,endInput,totalInput;
    private TextView error;
    private BudgetEntity editingBudget;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(SessionStore.role(this)==AppRole.CHILD) { Toast.makeText(this,R.string.child_budget_locked,Toast.LENGTH_LONG).show(); finish(); return; }
        setContentView(R.layout.activity_budget_period);
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        startInput=findViewById(R.id.budget_start_date); endInput=findViewById(R.id.budget_end_date); totalInput=findViewById(R.id.budget_total_input); error=findViewById(R.id.budget_period_error);
        startInput.setOnClickListener(v->showDatePicker(startInput)); endInput.setOnClickListener(v->showDatePicker(endInput));
        findViewById(R.id.budget_period_continue).setOnClickListener(v->continueToAllocation());
        if(getIntent().getStringExtra("MODE")!=null) loadExisting();
    }

    private void loadExisting() {
        long userId=SessionStore.userId(this);
        viewModel.<BudgetEntity>request(repository->repository.activeBudget(userId),state->{
            BudgetEntity budget=state.data;
            if(budget!=null) {
                editingBudget=budget; startInput.setText(dateFormat.format(new Date(budget.periodStart)));
                Calendar last=Calendar.getInstance(); last.setTimeInMillis(budget.periodEnd-1); endInput.setText(dateFormat.format(last.getTime()));
                totalInput.setText(CurrencyFormatter.formatMilli(budget.monthlyLimitMilli).replace(" DT",""));
            }
        });
    }

    private void showDatePicker(EditText target) {
        Calendar calendar=Calendar.getInstance();
        try { Date parsed=dateFormat.parse(target.getText().toString()); if(parsed!=null) calendar.setTime(parsed); } catch(ParseException ignored) { }
        new DatePickerDialog(this,(picker,year,month,day)->{
            Calendar selected=Calendar.getInstance(); selected.set(year,month,day); target.setText(dateFormat.format(selected.getTime()));
        },calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void continueToAllocation() {
        if(startInput.getText().toString().trim().isEmpty()||endInput.getText().toString().trim().isEmpty()){error.setText(R.string.date_required);return;}
        Date start,end; long total;
        try { start=dateFormat.parse(startInput.getText().toString().trim()); end=dateFormat.parse(endInput.getText().toString().trim()); total=MoneyInput.toMilli(totalInput.getText().toString()); }
        catch(Exception exception) { error.setText(R.string.money_invalid); return; }
        if(start==null||end==null||end.before(start)) { error.setText(R.string.date_order_error); return; }
        if(total<=0) { error.setText(R.string.budget_total_required); return; }
        Calendar endOfDay=Calendar.getInstance(); endOfDay.setTime(end); endOfDay.set(Calendar.HOUR_OF_DAY,23); endOfDay.set(Calendar.MINUTE,59); endOfDay.set(Calendar.SECOND,59); endOfDay.set(Calendar.MILLISECOND,999);
        Intent next=new Intent(this,AllocateBudgetActivity.class)
                .putExtra("START",start.getTime()).putExtra("END",endOfDay.getTimeInMillis()).putExtra("TOTAL",total);
        if(editingBudget!=null) next.putExtra("BUDGET_ID",editingBudget.id);
        startActivity(next);
    }
}
