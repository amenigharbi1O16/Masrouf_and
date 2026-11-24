package com.example.masrouf1.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/** Botanical budget hero card. Financial values are supplied by the Room-backed screen model. */
public final class BudgetCardView extends MaterialCardView {
    private final TextView period,remaining,spent,total;
    private final ProgressBar progress;
    private final MaterialButton allocationAction;

    public BudgetCardView(Context context){this(context,null);}
    public BudgetCardView(Context context,@Nullable AttributeSet attrs){this(context,attrs,com.google.android.material.R.attr.materialCardViewStyle);}
    public BudgetCardView(Context context,@Nullable AttributeSet attrs,int defStyleAttr){
        super(context,attrs,defStyleAttr);setCardBackgroundColor(getContext().getColor(R.color.forest));setRadius(getResources().getDimension(R.dimen.radius_budget_hero));setCardElevation(0);setStrokeWidth(0);setClickable(true);setFocusable(true);
        LinearLayout content=new LinearLayout(context);content.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(18),dp(20),dp(18));
        period=new TextView(context);period.setId(R.id.home_budget_period);period.setTextColor(getContext().getColor(R.color.mint_accent));period.setTextSize(12);period.setMaxLines(2);content.addView(period);
        TextView label=new TextView(context);label.setText(R.string.remaining_to_live);label.setTextColor(getContext().getColor(R.color.mint_accent));label.setTextSize(13);LinearLayout.LayoutParams labelParams=new LinearLayout.LayoutParams(-1,-2);labelParams.topMargin=dp(10);content.addView(label,labelParams);
        remaining=new TextView(context);remaining.setId(R.id.home_remaining);remaining.setTextColor(getContext().getColor(R.color.alabaster));remaining.setTextSize(32);remaining.setTypeface(null,android.graphics.Typeface.BOLD);remaining.setSingleLine(true);content.addView(remaining);
        progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setId(R.id.home_budget_progress);progress.setMax(100);progress.setProgress(0);progress.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.muted_green)));progress.setProgressTintList(ColorStateList.valueOf(getContext().getColor(R.color.mint_accent)));LinearLayout.LayoutParams progressParams=new LinearLayout.LayoutParams(-1,dp(8));progressParams.topMargin=dp(14);content.addView(progress,progressParams);
        LinearLayout values=new LinearLayout(context);values.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);values.setGravity(Gravity.CENTER_VERTICAL);values.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams valuesParams=new LinearLayout.LayoutParams(-1,-2);valuesParams.topMargin=dp(12);
        spent=new TextView(context);spent.setId(R.id.home_spent);spent.setTextColor(getContext().getColor(R.color.alabaster));spent.setTextSize(12);values.addView(spent,new LinearLayout.LayoutParams(0,-2,1));
        total=new TextView(context);total.setId(R.id.home_total);total.setTextColor(getContext().getColor(R.color.mint_accent));total.setTextSize(12);total.setGravity(Gravity.END);values.addView(total);content.addView(values,valuesParams);
        allocationAction=new MaterialButton(context,null,com.google.android.material.R.attr.materialButtonTonalStyle);allocationAction.setId(R.id.home_partition_child_budget);allocationAction.setText(R.string.partition_child_budget);allocationAction.setTextColor(getContext().getColor(R.color.forest));allocationAction.setBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.mint_light)));allocationAction.setCornerRadius(dp(16));allocationAction.setVisibility(GONE);LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(-1,dp(48));actionParams.topMargin=dp(12);content.addView(allocationAction,actionParams);
        addView(content,new LayoutParams(-1,-2));
        setBudgetUnavailable(getContext().getString(R.string.limit_label));
    }
    public void setBudgetUnavailable(String totalLabel){
        period.setText(null);period.setVisibility(GONE);remaining.setText(R.string.budget_value_unavailable);
        spent.setText(R.string.spent_value_unavailable);total.setText(getContext().getString(R.string.budget_total_unavailable,totalLabel));progress.setProgress(0);
    }
    public void setBudgetData(String periodLabel,long totalMilli,long spentMilli,String totalLabel,int warningFrom,int dangerFrom){
        long remainingMilli=MoneyMath.subtract(totalMilli,spentMilli);int percent=totalMilli<=0?(spentMilli>0?100:0):Math.max(0,MoneyMath.percentOf(spentMilli,totalMilli));
        period.setText(periodLabel==null?"":periodLabel);period.setVisibility(periodLabel==null||periodLabel.trim().isEmpty()?GONE:VISIBLE);
        remaining.setText(CurrencyFormatter.formatMilli(remainingMilli));spent.setText(getContext().getString(R.string.spent_label)+"  "+CurrencyFormatter.formatMilli(spentMilli));total.setText((totalLabel==null?getContext().getString(R.string.limit_label):totalLabel)+"  "+CurrencyFormatter.formatMilli(totalMilli));progress.setProgress(Math.min(100,percent));
        int tint=percent>=dangerFrom?R.color.coral:percent>=warningFrom?R.color.amber:R.color.mint_accent;progress.setProgressTintList(ColorStateList.valueOf(getContext().getColor(tint)));
    }
    public void setAllocationActionVisible(boolean visible){allocationAction.setVisibility(visible?VISIBLE:GONE);}
    public void setAllocationActionClickListener(OnClickListener listener){allocationAction.setOnClickListener(listener);}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
}
