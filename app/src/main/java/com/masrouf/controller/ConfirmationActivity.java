package com.masrouf.controller;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.R;
import com.google.android.material.button.MaterialButton;

public final class ConfirmationActivity extends BaseFeatureActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);showPage(R.string.transaction_confirmation);LinearLayout page=new LinearLayout(this);page.setGravity(Gravity.CENTER);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(24),dp(24),dp(24),dp(24));
        TextView mark=new TextView(this);mark.setText("✓");mark.setTextSize(52);mark.setTextColor(getColor(R.color.emerald));mark.setGravity(Gravity.CENTER);page.addView(mark,new LinearLayout.LayoutParams(-1,dp(72)));
        TextView label=new TextView(this);label.setText(getIntent().getStringExtra("LABEL"));label.setTextColor(getColor(R.color.forest));label.setTextSize(20);label.setGravity(Gravity.CENTER);page.addView(label);
        TextView amount=new TextView(this);amount.setText(CurrencyFormatter.formatMilli(getIntent().getLongExtra("AMOUNT",0)));amount.setTextColor(getColor(R.color.emerald));amount.setTextSize(26);amount.setGravity(Gravity.CENTER);page.addView(amount);
        MaterialButton done=new MaterialButton(this);done.setText(R.string.go_home);done.setOnClickListener(v->goHome(v));page.addView(done,new LinearLayout.LayoutParams(-1,dp(54)));content.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-1));}
}
