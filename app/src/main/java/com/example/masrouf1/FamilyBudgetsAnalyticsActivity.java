package com.example.masrouf1;

import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.db.ChildSpendingSummary;
import com.masrouf.controller.SessionStore;

import java.util.ArrayList;

/** Parent-only aggregate charts for children and the four latest budget periods. */
public final class FamilyBudgetsAnalyticsActivity extends AppCompatActivity {
    private PillBarChartView pills;
    private ChildrenBudgetAnalyticsView trend;
    private MasroufViewModel viewModel;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(SessionStore.role(this)!=AppRole.PARENT) { finish(); return; }
        setContentView(R.layout.activity_family_budgets_analytics);
        pills=findViewById(R.id.family_child_pills); trend=findViewById(R.id.family_monthly_trend);
        com.google.android.material.appbar.MaterialToolbar toolbar=findViewById(R.id.family_analytics_toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back); toolbar.setNavigationOnClickListener(v->finish());
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        pills.setOnChildTapListener(this::showChildDetails); loadAggregates();
    }

    private void loadAggregates() {
        long parentId=SessionStore.userId(this);
        viewModel.<FamilyAnalyticsData>request(repository->repository.familyAnalytics(parentId),state->{
            if(isFinishing()||isDestroyed())return;
            if(state.data==null){finish();return;}
            FamilyAnalyticsData data=state.data;ArrayList<ChildrenBudgetAnalyticsView.Point> points=new ArrayList<>();
            for(FamilyMonthSpending month:data.months)points.add(new ChildrenBudgetAnalyticsView.Point(month.periodStart,month.spentMilli,month.allocatedMilli));
            pills.setChildren(data.children);trend.setPoints(points);
            long personal=Math.max(0,MoneyMath.subtract(data.globalBudgetMilli,data.childAllocationsMilli));
            ((android.widget.TextView)findViewById(R.id.family_global_summary)).setText(getString(R.string.family_budget_summary,CurrencyFormatter.formatMilli(data.globalBudgetMilli),CurrencyFormatter.formatMilli(data.childAllocationsMilli),CurrencyFormatter.formatMilli(MoneyMath.subtract(personal,data.parentSpendingMilli))));
        });
    }

    private void showChildDetails(ChildSpendingSummary child) {
        String message=getString(R.string.child_chart_details,child.nickname,CurrencyFormatter.formatMilli(child.allocatedTotalMilli),CurrencyFormatter.formatMilli(child.spentMilli),CurrencyFormatter.formatMilli(child.availableMilli()));
        new AlertDialog.Builder(this).setTitle(child.nickname).setMessage(message).setPositiveButton(android.R.string.ok,null).show();
    }
}
