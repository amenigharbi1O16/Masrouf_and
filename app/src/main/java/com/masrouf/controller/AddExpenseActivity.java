package com.masrouf.controller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.ProductFormData;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class AddExpenseActivity extends BaseFeatureActivity {
    private MasroufViewModel viewModel;
    private EditText label,amount,note;
    private Spinner categorySpinner;
    private TextView date;
    private MaterialButton save;
    private List<CategoryEntity> categories=new ArrayList<>();
    private BudgetEntity budget;
    private long occurredAt=System.currentTimeMillis();
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showPage(R.string.add_expense);viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        android.view.View form=getLayoutInflater().inflate(R.layout.activity_add_expense,content,false); content.addView(form);
        label=findViewById(R.id.expense_label); amount=findViewById(R.id.expense_amount); note=findViewById(R.id.expense_note);
        categorySpinner=findViewById(R.id.expense_category); date=findViewById(R.id.expense_date); save=findViewById(R.id.save_expense);
        if(getIntent().hasExtra("VOICE_LABEL")) label.setText(getIntent().getStringExtra("VOICE_LABEL"));
        if(getIntent().hasExtra("VOICE_AMOUNT")) amount.setText(getIntent().getStringExtra("VOICE_AMOUNT"));
        setDateLabel(); date.setOnClickListener(v->chooseDate()); save.setOnClickListener(v->saveExpense()); loadFormData();
    }
    private void loadFormData() {
        long userId=SessionStore.userId(this);
        viewModel.<ProductFormData>request(repository->repository.productForm(userId),state->{if(state.data==null)return;
            ArrayList<CategoryEntity> expenseCategories=new ArrayList<>();for(CategoryEntity item:state.data.categories)if(!"product".equalsIgnoreCase(item.type))expenseCategories.add(item);
            if(expenseCategories.isEmpty())expenseCategories.addAll(state.data.categories);
            budget=state.data.budget;categories=expenseCategories;ArrayList<String> labels=new ArrayList<>();for(CategoryEntity row:categories)labels.add(row.name);categorySpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        });
    }
    private void chooseDate() { Calendar c=Calendar.getInstance(); c.setTimeInMillis(occurredAt); new DatePickerDialog(this,(picker,y,m,d)->{ Calendar selected=Calendar.getInstance(); selected.set(y,m,d,12,0,0); selected.set(Calendar.MILLISECOND,0); occurredAt=selected.getTimeInMillis(); setDateLabel(); },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show(); }
    private void setDateLabel() { date.setText(new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(occurredAt)); }
    private void saveExpense() {
        long milli; try { milli=MoneyInput.toMilli(amount.getText().toString()); if(milli<=0) throw new NumberFormatException(); } catch(NumberFormatException ex) { amount.setError(getString(R.string.money_invalid)); return; }
        if(budget==null) { Toast.makeText(this,R.string.budget_required_for_entry,Toast.LENGTH_LONG).show(); return; }
        TransactionEntity tx=new TransactionEntity(); tx.userId=SessionStore.userId(this); tx.budgetId=budget.id;
        boolean donation="donation".equals(getIntent().getStringExtra("VOICE_KIND")); tx.kind=donation?"donation":"expense";
        String title=label.getText().toString().trim(); if(title.isEmpty()) title=donation?getString(R.string.donation):getString(R.string.add_expense);
        String noteValue=note.getText().toString().trim(); tx.note=noteValue; tx.label=donation?getString(R.string.donation_prefix)+title:title;
        tx.amountMilli=milli; tx.occurredAt=occurredAt; if(!categories.isEmpty()) tx.categoryId=categories.get(categorySpinner.getSelectedItemPosition()).id;
        save.setEnabled(false);
        long ownerId=tx.userId;
        viewModel.<Long>request(repository->repository.saveExpense(tx,ownerId),result->{if(result.data!=null&&result.data>0){Intent next=new Intent(this,ConfirmationActivity.class).putExtra("LABEL",tx.label).putExtra("AMOUNT",milli).putExtra("KIND",tx.kind);startActivity(next);finish();}else{save.setEnabled(true);Toast.makeText(this,R.string.transaction_saved_error,Toast.LENGTH_SHORT).show();}});
    }
}
