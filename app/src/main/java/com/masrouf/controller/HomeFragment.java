package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.DataState;
import com.example.masrouf1.HomeDashboardData;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.R;
import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Role-aware home: child history remains private; parents receive SQL aggregates only. */
public final class HomeFragment extends Fragment {
    private long userId,childAllocation;
    private MasroufViewModel dashboardViewModel;
    private View root;
    private HomeCategoryAdapter categoriesAdapter;
    private RecentTransactionsAdapter transactionsAdapter;
    private BudgetEntity currentBudget;
    private boolean childMode,parentMode;

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle savedState) {
        super.onViewCreated(view,savedState); root=view;
        userId=SessionStore.userId(requireContext()); dashboardViewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(requireContext())).get(MasroufViewModel.class);
        AppRole role=SessionStore.role(requireContext()); childMode=role==AppRole.CHILD; parentMode=role==AppRole.PARENT;
        RecyclerView categories=view.findViewById(R.id.home_categories); categories.setLayoutManager(new LinearLayoutManager(requireContext()));
        categoriesAdapter=new HomeCategoryAdapter(); categories.setAdapter(categoriesAdapter);
        RecyclerView recent=view.findViewById(R.id.home_recent_transactions); recent.setLayoutManager(new LinearLayoutManager(requireContext()));
        transactionsAdapter=new RecentTransactionsAdapter(this::openTransaction); recent.setAdapter(transactionsAdapter);
        recent.setNestedScrollingEnabled(false); categories.setNestedScrollingEnabled(false);
        ((TextView)view.findViewById(R.id.home_period)).setText(periodLabel());
        view.findViewById(R.id.home_period).setContentDescription(periodLabel());
        view.findViewById(R.id.home_calendar_link).setOnClickListener(v->((com.example.masrouf1.MainActivity)requireActivity()).openCentralTab("calendar"));
        view.findViewById(R.id.home_open_drawer).setOnClickListener(v->((com.example.masrouf1.MainActivity)requireActivity()).openMenu());
        view.findViewById(R.id.home_budget_card).setOnClickListener(v->openBudget());
        view.findViewById(R.id.home_define_budget).setOnClickListener(v->openBudget());
        view.findViewById(R.id.home_partition_child_budget).setOnClickListener(v->openChildAllocation());
        view.findViewById(R.id.home_add_category).setOnClickListener(v->new CustomCategoryDialogFragment().show(getParentFragmentManager(),"add-category"));
        view.findViewById(R.id.home_add_purchase).setOnClickListener(v->startActivity(new Intent(requireContext(),AddProductActivity.class)));
        view.findViewById(R.id.home_add_expense).setOnClickListener(v->startActivity(new Intent(requireContext(),AddExpenseActivity.class)));
        view.findViewById(R.id.home_manage_categories).setOnClickListener(v->startActivity(new Intent(requireContext(),ManageCategoriesActivity.class)));
        view.findViewById(R.id.home_manage_children).setOnClickListener(v->startActivity(new Intent(requireContext(),ChildrenManagementActivity.class)));
        getParentFragmentManager().setFragmentResultListener("masrouf-categories-updated",this,(key,result)->loadData());
        view.findViewById(R.id.home_children_tracking).setVisibility(parentMode?View.VISIBLE:View.GONE);
        view.findViewById(R.id.home_manage_children).setVisibility(parentMode?View.VISIBLE:View.GONE);
        dashboardViewModel.home().observe(getViewLifecycleOwner(),state->{
            if(state.loading||state.data==null||root==null)return;
            HomeDashboardData data=state.data;String name=data.user==null||data.user.name==null?"":data.user.name;
            ((TextView)root.findViewById(R.id.home_greeting)).setText(name.isEmpty()?getString(R.string.greeting_hello):getString(R.string.greeting_hello)+", "+name);
            currentBudget=data.budget;childAllocation=data.childProfile==null?0:data.childProfile.allocatedTotalMilli;
            renderBudget(data.budget,data.childAllocations,data.parentSpent);categoriesAdapter.submit(data.categories);transactionsAdapter.submit(data.recent);
            if(parentMode)renderChildren(data.children);
        });
    }

    @Override public void onResume() { super.onResume(); if(root!=null) loadData(); }

    private void loadData() {
        if(isAdded()&&dashboardViewModel!=null)dashboardViewModel.loadHome(userId,parentMode,childMode);
    }

    private void renderBudget(BudgetEntity budget,long childrenAllocated,long parentSpent) {
        long total=childMode?childAllocation:(parentMode?(budget==null?0:Math.max(0,MoneyMath.subtract(budget.monthlyLimitMilli,childrenAllocated))):(budget==null?0:budget.monthlyLimitMilli));
        if(childMode&&total<=0&&budget!=null) total=budget.monthlyLimitMilli;
        long spent=parentMode?parentSpent:(budget==null?0:budget.spentMilli), remaining=MoneyMath.subtract(total,spent);
        ((TextView)root.findViewById(R.id.home_remaining)).setText(CurrencyFormatter.formatMilli(remaining));
        ((TextView)root.findViewById(R.id.home_spent)).setText(getString(R.string.spent_label)+"  "+CurrencyFormatter.formatMilli(spent));
        ((TextView)root.findViewById(R.id.home_total)).setText((parentMode?getString(R.string.parent_personal_share):getString(R.string.limit_label))+"  "+CurrencyFormatter.formatMilli(total));
        boolean empty=budget==null||total<=0;
        root.findViewById(R.id.home_no_budget).setVisibility(empty?View.VISIBLE:View.GONE);
        root.findViewById(R.id.home_define_budget).setVisibility(childMode?View.GONE:View.VISIBLE);
        root.findViewById(R.id.home_no_budget_title).setVisibility(childMode?View.GONE:View.VISIBLE);
        root.findViewById(R.id.home_child_no_budget_message).setVisibility(childMode&&empty?View.VISIBLE:View.GONE);
        root.findViewById(R.id.home_partition_child_budget).setVisibility(childMode&&total>0&&budget!=null?View.VISIBLE:View.GONE);
        ProgressBar bar=root.findViewById(R.id.home_budget_progress);
        int percentage=total<=0?0:Math.min(100,Math.max(0,MoneyMath.percentOf(spent,total))); bar.setProgress(percentage);
        MaterialCardView alert=root.findViewById(R.id.home_threshold_card); TextView label=root.findViewById(R.id.home_threshold_text);
        if(total>0&&spent>=total) { alert.setCardBackgroundColor(requireContext().getColor(R.color.coral)); label.setText(R.string.budget_over_100); label.setTextColor(requireContext().getColor(R.color.alabaster)); bar.setProgressTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.coral))); }
        else if(total>0&&MoneyMath.atLeastPercent(spent,total,80)) { alert.setCardBackgroundColor(requireContext().getColor(R.color.amber)); label.setText(R.string.budget_alert_80); label.setTextColor(requireContext().getColor(R.color.forest)); bar.setProgressTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.amber))); }
        else { alert.setCardBackgroundColor(requireContext().getColor(R.color.mint)); label.setText(R.string.healthy_budget); label.setTextColor(requireContext().getColor(R.color.forest)); bar.setProgressTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.mint))); }
    }

    private void openBudget() {
        if(childMode) { showChildBudgetDialog(); return; }
        Intent intent=new Intent(requireContext(),BudgetPeriodActivity.class);
        if(currentBudget!=null) intent.putExtra("MODE","edit"); startActivity(intent);
    }

    private void showChildBudgetDialog() {
        androidx.appcompat.app.AlertDialog.Builder dialog=new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setMessage(R.string.child_budget_locked).setNegativeButton(android.R.string.ok,null);
        if(childAllocation>0&&currentBudget!=null) dialog.setPositiveButton(R.string.partition_child_budget,(d,w)->openChildAllocation());
        dialog.show();
    }

    private void openChildAllocation() {
        if(childAllocation<=0||currentBudget==null) { showChildBudgetDialog(); return; }
        startActivity(new Intent(requireContext(),AllocateBudgetActivity.class).putExtra("IS_CHILD",true).putExtra("BUDGET_ID",currentBudget.id));
    }

    private void renderChildren(List<ChildSpendingSummary> children) {
        LinearLayout rows=root.findViewById(R.id.home_children_rows); rows.removeAllViews();
        if(children.isEmpty()) { TextView empty=new TextView(requireContext()); empty.setText(R.string.children_empty); empty.setTextColor(requireContext().getColor(R.color.muted_green)); rows.addView(empty); return; }
        for(ChildSpendingSummary child:children) {
            MaterialCardView card=new MaterialCardView(requireContext()); card.setCardBackgroundColor(requireContext().getColor(R.color.cream)); card.setRadius(dp(16)); card.setStrokeWidth(dp(1)); card.setStrokeColor(requireContext().getColor(R.color.card_border));
            LinearLayout summary=new LinearLayout(requireContext()); summary.setOrientation(LinearLayout.VERTICAL); summary.setPadding(dp(12),dp(10),dp(12),dp(10));
            TextView name=new TextView(requireContext()); name.setText(child.nickname); name.setTextColor(requireContext().getColor(R.color.forest)); name.setTextSize(15); name.setTypeface(null,1); summary.addView(name);
            TextView ratio=new TextView(requireContext()); ratio.setText(getString(R.string.child_budget_ratio,CurrencyFormatter.formatMilli(child.spentMilli),CurrencyFormatter.formatMilli(child.allocatedTotalMilli))); ratio.setTextColor(requireContext().getColor(R.color.ink)); summary.addView(ratio);
            ProgressBar progress=new ProgressBar(requireContext(),null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); int percent=child.allocatedTotalMilli<=0?0:Math.min(100,Math.max(0,MoneyMath.percentOf(child.spentMilli,child.allocatedTotalMilli))); progress.setProgress(percent); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(child.spentMilli>child.allocatedTotalMilli?R.color.coral:R.color.emerald))); progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(requireContext().getColor(R.color.mint)));
            LinearLayout.LayoutParams progressParams=new LinearLayout.LayoutParams(-1,dp(7)); progressParams.topMargin=dp(6); summary.addView(progress,progressParams);
            LinearLayout footer=new LinearLayout(requireContext()); footer.setGravity(android.view.Gravity.CENTER_VERTICAL); footer.setOrientation(LinearLayout.HORIZONTAL);
            TextView available=new TextView(requireContext()); available.setText(getString(R.string.child_available_summary,CurrencyFormatter.formatMilli(child.availableMilli()))); available.setTextColor(requireContext().getColor(R.color.emerald)); footer.addView(available,new LinearLayout.LayoutParams(0,-2,1));
            TextView consumed=new TextView(requireContext()); consumed.setText(getString(R.string.consumed_percent,child.allocatedTotalMilli<=0?0:MoneyMath.percentOf(child.spentMilli,child.allocatedTotalMilli))); consumed.setTextColor(requireContext().getColor(R.color.muted_green)); footer.addView(consumed); summary.addView(footer);
            card.addView(summary); LinearLayout.LayoutParams cardParams=new LinearLayout.LayoutParams(-1,-2); cardParams.topMargin=dp(9); rows.addView(card,cardParams);
        }
    }

    private void openTransaction(TransactionRow row) {
        Class<?> destination="purchase".equals(row.kind)?EditPurchaseActivity.class:EditExpenseActivity.class;
        startActivity(new Intent(requireContext(),destination).putExtra("TRANSACTION_ID",row.id).putExtra("TITLE",row.title).putExtra("AMOUNT",row.amountMilli));
    }

    private String periodLabel() {
        Calendar c=Calendar.getInstance(); int day=c.getActualMaximum(Calendar.DAY_OF_MONTH);
        String month=new DateFormatSymbols(Locale.FRENCH).getMonths()[c.get(Calendar.MONTH)].toUpperCase(Locale.ROOT);
        String shortMonth=month.length()>3?month.substring(0,3):month;
        return String.format(Locale.ROOT,"%s %d · DU 01 AU %02d %s",month,c.get(Calendar.YEAR),day,shortMonth);
    }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
}
