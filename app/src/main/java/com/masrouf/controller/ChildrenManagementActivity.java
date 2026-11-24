package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.BudgetAllocationReceiver;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.GiftEnvelopeData;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.OperationResult;
import com.example.masrouf1.R;
import com.example.masrouf1.db.BudgetHistoryRow;
import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ChildProfileEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Parent-only household ledger, child accounts, and aggregate budget history. */
public final class ChildrenManagementActivity extends AppCompatActivity {
    private RecyclerView recycler;
    private MasroufViewModel viewModel;
    private ChildAdapter adapter;
    private long parentId;
    private boolean authorizedParent;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(SessionStore.role(this)!=AppRole.PARENT) { finish(); return; }
        setContentView(R.layout.activity_children_management); parentId=SessionStore.userId(this);
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        recycler=findViewById(R.id.children_management_list); recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter=new ChildAdapter(new ChildActions() {
            @Override public void give(ChildSpendingSummary child) { showGiveMoneyDialog(child); }
            @Override public void reset(ChildSpendingSummary child) { showResetPasswordDialog(child); }
            @Override public void history(ChildSpendingSummary child,LinearLayout target) { loadHistory(child,target); }
        }); recycler.setAdapter(adapter);
        View create=findViewById(R.id.child_management_create);create.setEnabled(false);create.setOnClickListener(v->{if(authorizedParent)showAddChildDialog();});
        viewModel.loadUser(parentId);
        viewModel.user().observe(this,profileState->{
            if(profileState==null||profileState.loading)return;
            UserEntity parent=profileState.data;
            if(parent==null||!(AppRole.PARENT.value().equalsIgnoreCase(parent.role)||"admin".equalsIgnoreCase(parent.role))){finish();return;}
            authorizedParent=true;create.setEnabled(true);loadChildren();
        });
    }

    @Override protected void onResume() { super.onResume(); if(adapter!=null&&authorizedParent) loadChildren(); }

    private void showAddChildDialog() {
        View form=LayoutInflater.from(this).inflate(R.layout.dialog_add_child,null,false);
        EditText name=form.findViewById(R.id.dialog_child_name),email=form.findViewById(R.id.dialog_child_email);
        EditText password=form.findViewById(R.id.dialog_child_password),phone=form.findViewById(R.id.dialog_child_phone),allocation=form.findViewById(R.id.dialog_child_allocation);
        TextView error=form.findViewById(R.id.dialog_child_error);
        email.addTextChangedListener(new FormValidationTextWatcher(error));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.add_child).setView(form)
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String childName=name.getText().toString().trim(),childEmail=email.getText().toString();
            String secret=password.getText().toString(),parentPhone=phone.getText().toString().trim(); long initial;
            try { String text=allocation.getText().toString().trim(); initial=text.isEmpty()?0:MoneyInput.toMilli(text); }
            catch(NumberFormatException exception) { error.setText(R.string.money_invalid); return; }
            if(childName.isEmpty()||parentPhone.isEmpty()||initial<0) { error.setText(R.string.child_creation_invalid); return; }
            if(!InputValidation.isValidGmail(childEmail)) { error.setText(R.string.email_error); return; }
            if(!InputValidation.isValidPassword(secret)) { error.setText(InputValidation.passwordError(this,secret)); return; }
            createChildAccount(dialog,error,childName,childEmail,secret,parentPhone,initial);
        }));
        dialog.show();
    }

    private void createChildAccount(AlertDialog dialog,TextView error,String childName,String childEmail,String secret,String parentPhone,long initial) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        UserEntity child=new UserEntity();child.name=childName;child.email=childEmail;child.role=AppRole.CHILD.value();child.accountType="child";child.onboarded=true;child.balanceMilli=0;child.createdAt=System.currentTimeMillis();
        ChildProfileEntity profile=new ChildProfileEntity();profile.parentUserId=parentId;profile.nickname=childName;profile.parentPhone=parentPhone;profile.allocatedTotalMilli=initial;profile.createdAt=System.currentTimeMillis();
        BudgetEntity initialBudget=null;
        if(initial>0){initialBudget=new BudgetEntity();initialBudget.name="Dotation familiale";initialBudget.monthlyLimitMilli=initial;initialBudget.status="ACTIVE";setCurrentPeriod(initialBudget);}
        List<CategoryEntity> categories=defaultChildCategories();BudgetEntity childBudget=initialBudget;
        viewModel.<OperationResult<Long>>request(repository->repository.createChildAccount(child,profile,childBudget,categories,secret),state->{
            if(isFinishing()||isDestroyed())return;OperationResult<Long> result=state.data;
            if(result==null||!result.succeeded()||result.value==null||result.value<=0){
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);String failure=result==null?"save":result.error;
                if("duplicate".equals(failure))error.setText(R.string.account_exists);
                else if("password".equals(failure))error.setText(InputValidation.passwordError(this,secret));
                else if("gmail".equals(failure))error.setText(R.string.email_error);
                else if("parent".equals(failure))error.setText(R.string.transaction_saved_error);
                else if(failure!=null&&failure.contains("child allocation exceeds"))error.setText(initial>0?R.string.parent_allocation_cap:R.string.transaction_saved_error);
                else error.setText(initial>0?R.string.parent_budget_required:R.string.child_creation_invalid);
                return;
            }
            dialog.dismiss();showCredentials(childEmail,secret);loadChildren();
        });
    }

    private void showCredentials(String email,String password) {
        String message=getString(R.string.child_credentials_message,email,password);
        new AlertDialog.Builder(this).setTitle(R.string.child_credentials_title).setMessage(message)
                .setPositiveButton(android.R.string.ok,null).setCancelable(false).show();
    }

    private List<CategoryEntity> defaultChildCategories() {
        ArrayList<CategoryEntity> items=new ArrayList<>();
        String[] labels={"Jeux","Friandises","Fournitures"}; String[] colors={"#E9B66B","#DC7770","#2E7D43"};
        for(int i=0;i<labels.length;i++) { CategoryEntity row=new CategoryEntity(); row.name=labels[i]; row.colorHex=colors[i]; row.type="expense"; row.isEssential=i==2; items.add(row); }
        return items;
    }

    private void showGiveMoneyDialog(ChildSpendingSummary child) {
        View form=LayoutInflater.from(this).inflate(R.layout.dialog_give_money,null,false);
        EditText amountInput=form.findViewById(R.id.dialog_gift_amount),noteInput=form.findViewById(R.id.dialog_gift_note);
        TextView error=form.findViewById(R.id.dialog_gift_error);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(getString(R.string.give_money_title,child.nickname)).setView(form)
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            long amount; try { amount=MoneyInput.toMilli(amountInput.getText().toString()); if(amount<=0) throw new NumberFormatException(); }
            catch(NumberFormatException exception) { error.setText(R.string.money_invalid); return; }
            String note=noteInput.getText().toString().trim(); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            inspectChildEnvelope(dialog,error,child,amount,note);
        }));
        dialog.show();
    }

    private void inspectChildEnvelope(AlertDialog dialog,TextView error,ChildSpendingSummary child,long amount,String note) {
        viewModel.<GiftEnvelopeData>request(repository->repository.giftEnvelope(parentId),state->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);GiftEnvelopeData envelope=state.data;
            if(envelope==null||!envelope.ready||envelope.budget==null){error.setText(R.string.parent_budget_required);return;}
            if(amount>envelope.remainingMilli){
                new AlertDialog.Builder(this).setTitle(R.string.donate_money)
                        .setMessage(getString(R.string.budget_envelope_warning,CurrencyFormatter.formatMilli(amount),CurrencyFormatter.formatMilli(envelope.remainingMilli)))
                        .setNegativeButton(android.R.string.cancel,null)
                        .setPositiveButton(android.R.string.yes,(d,w)->commitGift(dialog,error,child,amount,note)).show();
            }else commitGift(dialog,error,child,amount,note);
        });
    }

    private void commitGift(AlertDialog dialog,TextView error,ChildSpendingSummary child,long amount,String note) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        long occurredAt=System.currentTimeMillis();
        viewModel.<Integer>request(repository->repository.giveMoney(parentId,child.childProfileId,amount,note,occurredAt),state->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);Integer result=state.data;
            if(result!=null&&result==0){dialog.dismiss();Toast.makeText(this,R.string.family_gift_saved,Toast.LENGTH_SHORT).show();sendAllowanceNotice(child.childUserId,amount);loadChildren();return;}
            if(result!=null&&result==-2)error.setText(R.string.parent_allocation_cap);else if(result!=null&&result==-3)error.setText(R.string.parent_budget_required);else error.setText(R.string.transaction_saved_error);
        });
    }

    private void showResetPasswordDialog(ChildSpendingSummary child) {
        EditText password=new EditText(this); password.setSingleLine(true); password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); password.setHint(R.string.child_temporary_password);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(R.string.reset_child_password).setMessage(R.string.password_rules).setView(password)
                .setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String secret=password.getText().toString(); if(!InputValidation.isValidPassword(secret)) { password.setError(InputValidation.passwordError(this,secret)); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            viewModel.<OperationResult<Boolean>>request(repository->repository.resetChildPassword(child.childProfileId,parentId,secret),result->{
                if(result.data==null||!result.data.succeeded()){dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);Toast.makeText(this,R.string.transaction_saved_error,Toast.LENGTH_SHORT).show();return;}
                long profileId=child.childProfileId;dialog.dismiss();
                viewModel.<String>request(repository->repository.childEmail(parentId,profileId),emailResult->{if(emailResult.data!=null)showCredentials(emailResult.data,secret);Toast.makeText(this,R.string.password_reset_saved,Toast.LENGTH_SHORT).show();});
            });
        })); dialog.show();
    }

    private void loadHistory(ChildSpendingSummary child,LinearLayout target) {
        viewModel.<List<BudgetHistoryRow>>request(repository->repository.childHistory(parentId,child.childUserId),state->{
                List<BudgetHistoryRow> result=state.data==null?new ArrayList<>():state.data;
                target.removeAllViews();
                if(result.isEmpty()) { TextView empty=new TextView(this); empty.setText(R.string.history_none); empty.setTextColor(getColor(R.color.muted_green)); target.addView(empty); return; }
                SimpleDateFormat month=new SimpleDateFormat("MMM yyyy",Locale.getDefault());
                for(BudgetHistoryRow row:result) {
                    int used=row.allocatedMilli<=0?0:MoneyMath.percentOf(row.spentMilli,row.allocatedMilli);
                    TextView line=new TextView(this); line.setText(getString(R.string.history_month_row,month.format(row.periodStart),CurrencyFormatter.formatMilli(row.allocatedMilli),CurrencyFormatter.formatMilli(row.spentMilli),CurrencyFormatter.formatMilli(MoneyMath.subtract(row.allocatedMilli,row.spentMilli)),used));
                    line.setTextColor(getColor(R.color.ink)); line.setTextSize(12); line.setPadding(0,dp(5),0,dp(5)); target.addView(line);
                }
        });
    }

    private void loadChildren() {
        if(!authorizedParent)return;
        viewModel.<List<ChildSpendingSummary>>request(repository->repository.familyChildren(parentId),state->{if(state.data!=null&&adapter!=null)adapter.submit(state.data);});
    }

    private void sendAllowanceNotice(long childUserId,long amountMilli) {
        Intent notice=new Intent(this,BudgetAllocationReceiver.class).setAction(BudgetAllocationReceiver.ACTION_CHILD_ALLOCATION)
                .putExtra(BudgetAllocationReceiver.EXTRA_CHILD_USER_ID,childUserId)
                .putExtra(BudgetAllocationReceiver.EXTRA_AMOUNT_MILLI,amountMilli);
        sendBroadcast(notice);
    }

    private static void setCurrentPeriod(BudgetEntity budget) {
        Calendar start=Calendar.getInstance(); start.set(Calendar.DAY_OF_MONTH,1); start.set(Calendar.HOUR_OF_DAY,0); start.set(Calendar.MINUTE,0); start.set(Calendar.SECOND,0); start.set(Calendar.MILLISECOND,0);
        Calendar end=(Calendar)start.clone(); end.add(Calendar.MONTH,1); end.add(Calendar.MILLISECOND,-1);
        budget.periodStart=start.getTimeInMillis(); budget.periodEnd=end.getTimeInMillis();
    }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }

    private interface ChildActions {
        void give(ChildSpendingSummary child);
        void reset(ChildSpendingSummary child);
        void history(ChildSpendingSummary child,LinearLayout target);
    }

    private static final class ChildAdapter extends RecyclerView.Adapter<ChildHolder> {
        private final List<ChildSpendingSummary> data=new ArrayList<>(); private final ChildActions actions;
        ChildAdapter(ChildActions actions) { this.actions=actions; }
        void submit(List<ChildSpendingSummary> rows) { data.clear(); data.addAll(rows); notifyDataSetChanged(); }
        @Override public ChildHolder onCreateViewHolder(ViewGroup parent,int viewType) { return new ChildHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_child_management,parent,false)); }
        @Override public void onBindViewHolder(ChildHolder holder,int position) {
            ChildSpendingSummary child=data.get(position); holder.name.setText(child.nickname);
            holder.ratio.setText(holder.itemView.getContext().getString(R.string.child_budget_ratio,CurrencyFormatter.formatMilli(child.spentMilli),CurrencyFormatter.formatMilli(child.allocatedTotalMilli)));
            holder.available.setText(holder.itemView.getContext().getString(R.string.child_available_summary,CurrencyFormatter.formatMilli(child.availableMilli())));
            holder.balance.setText(holder.itemView.getContext().getString(R.string.child_balance_summary,CurrencyFormatter.formatMilli(child.balanceMilli)));
            int percent=child.allocatedTotalMilli<=0?0:Math.min(999,Math.max(0,MoneyMath.percentOf(child.spentMilli,child.allocatedTotalMilli)));
            holder.percent.setText(holder.itemView.getContext().getString(R.string.consumed_percent,percent)); holder.progress.setProgress(Math.min(100,percent));
            int tint=percent>100?R.color.coral:percent>=80?R.color.amber:R.color.emerald;
            holder.progress.setProgressTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getColor(tint)));
            holder.give.setOnClickListener(v->actions.give(child)); holder.reset.setOnClickListener(v->actions.reset(child));
            holder.history.setVisibility(View.GONE); holder.history.removeAllViews(); holder.toggle.setOnClickListener(v->{
                boolean show=holder.history.getVisibility()!=View.VISIBLE; holder.history.setVisibility(show?View.VISIBLE:View.GONE);
                if(show) actions.history(child,holder.history);
            });
        }
        @Override public int getItemCount() { return data.size(); }
    }

    private static final class ChildHolder extends RecyclerView.ViewHolder {
        final TextView name,ratio,available,percent,balance; final ProgressBar progress; final View give,reset,toggle; final LinearLayout history;
        ChildHolder(View view) { super(view); name=view.findViewById(R.id.child_row_name); ratio=view.findViewById(R.id.child_row_ratio); available=view.findViewById(R.id.child_row_available); percent=view.findViewById(R.id.child_row_percent); balance=view.findViewById(R.id.child_row_balance); progress=view.findViewById(R.id.child_row_progress); give=view.findViewById(R.id.child_row_give); reset=view.findViewById(R.id.child_row_reset); toggle=view.findViewById(R.id.child_row_history_toggle); history=view.findViewById(R.id.child_row_history); }
    }
}
