package com.masrouf.controller;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.R;

abstract class TransactionDetailActivity extends BaseFeatureActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showPage(titleResource());
        LinearLayout detail=new LinearLayout(this); detail.setGravity(Gravity.CENTER_VERTICAL); detail.setOrientation(LinearLayout.VERTICAL);
        TextView title=new TextView(this); title.setText(getIntent().getStringExtra("TITLE")); title.setTextColor(getColor(R.color.forest)); title.setTextSize(22); title.setTypeface(null,android.graphics.Typeface.BOLD); detail.addView(title);
        long amount=getIntent().getLongExtra("AMOUNT",0L); TextView value=new TextView(this); value.setText(CurrencyFormatter.formatMilli(amount)); value.setTextColor(getColor(R.color.coral)); value.setTextSize(18); value.setPadding(0,dp(12),0,0); detail.addView(value);
        content.addView(detail,new android.widget.FrameLayout.LayoutParams(-1,-1));
    }
    protected abstract int titleResource();
}
