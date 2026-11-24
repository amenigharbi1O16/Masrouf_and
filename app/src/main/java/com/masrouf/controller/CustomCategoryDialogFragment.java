package com.masrouf.controller;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.masrouf1.R;
import com.example.masrouf1.CategoryAllocationData;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.google.android.material.tabs.TabLayout;


/** Product and expense allocations are selected through mutually exclusive tabs. */
public final class CustomCategoryDialogFragment extends DialogFragment {
    private String selectedType="product";
    private int usedPercent;
    private MasroufViewModel viewModel;
    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(requireContext())).get(MasroufViewModel.class);
        LinearLayout form=new LinearLayout(requireContext());form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(22),dp(8),dp(22),0);
        TabLayout tabs=new TabLayout(requireContext());tabs.addTab(tabs.newTab().setText(R.string.category_type_products));tabs.addTab(tabs.newTab().setText(R.string.category_type_expenses));form.addView(tabs,new LinearLayout.LayoutParams(-1,dp(48)));
        EditText name=new EditText(requireContext());name.setHint(R.string.category_name);form.addView(name,new LinearLayout.LayoutParams(-1,dp(54)));
        EditText allocation=new EditText(requireContext());allocation.setHint(R.string.allocation_percent);allocation.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);form.addView(allocation,new LinearLayout.LayoutParams(-1,dp(54)));
        TextView cap=new TextView(requireContext());cap.setPadding(0,dp(5),0,dp(4));form.addView(cap);
        TextView error=new TextView(requireContext());error.setTextColor(requireContext().getColor(R.color.coral));form.addView(error);
        AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle(R.string.add_category).setView(form).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){public void onTabSelected(TabLayout.Tab tab){selectedType=tab.getPosition()==0?"product":"expense";loadUsed(cap,allocation,dialog);}public void onTabUnselected(TabLayout.Tab tab){}public void onTabReselected(TabLayout.Tab tab){}});
        allocation.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){refreshCap(allocation,cap,dialog);}public void afterTextChanged(Editable e){}});
        dialog.setOnShowListener(ignored->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->save(name,allocation,cap,error,dialog));loadUsed(cap,allocation,dialog);});return dialog;
    }
    private void loadUsed(TextView cap,EditText allocation,AlertDialog dialog){long userId=SessionStore.userId(requireContext());String kind=selectedType;viewModel.<CategoryAllocationData>request(repository->repository.categoryAllocation(userId,kind),state->{if(!isAdded())return;CategoryAllocationData result=state.data;usedPercent=result==null?0:result.usedPercent;if(result==null||result.budget==null){cap.setText(R.string.category_budget_required);cap.setTextColor(requireContext().getColor(R.color.coral));if(dialog!=null&&dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);}else refreshCap(allocation,cap,dialog);});}
    private void refreshCap(EditText allocation,TextView cap,AlertDialog dialog){if(cap==null)return;int requested=0;try{requested=Integer.parseInt(allocation.getText().toString().trim());}catch(RuntimeException ignored){}int sum=usedPercent+requested;cap.setText(getString(R.string.allocation_cap_live,usedPercent,requested,Math.max(0,100-sum)));cap.setTextColor(requireContext().getColor(sum>100?R.color.coral:R.color.muted_green));if(dialog!=null&&dialog.isShowing())dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(sum<=100&&requested>0);}
    private void save(EditText name,EditText allocation,TextView cap,TextView error,AlertDialog dialog){String label=name.getText().toString().trim();int percent;try{percent=Integer.parseInt(allocation.getText().toString().trim());}catch(NumberFormatException ex){error.setText(R.string.allocation_invalid);return;}if(label.isEmpty()||percent<1||percent+usedPercent>100){error.setText(percent+usedPercent>100?getString(R.string.allocation_overflow):getString(R.string.allocation_invalid));return;}
        long userId=SessionStore.userId(requireContext());CategoryEntity category=new CategoryEntity();category.userId=userId;category.name=label;category.type=selectedType;category.allocationPercent=percent;category.colorHex="#2E7D43";category.isEssential=false;dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        viewModel.<Boolean>request(repository->repository.addCategory(category,userId),state->{if(!isAdded())return;if(Boolean.TRUE.equals(state.data)){android.widget.Toast.makeText(requireContext(),R.string.category_saved,android.widget.Toast.LENGTH_SHORT).show();getParentFragmentManager().setFragmentResult("masrouf-categories-updated",new Bundle());dialog.dismiss();}else{error.setText(R.string.allocation_overflow);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);}});
    }
    private int dp(int value){return(int)(value*getResources().getDisplayMetrics().density+.5f);}
}
