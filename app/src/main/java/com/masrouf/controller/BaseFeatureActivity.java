package com.masrouf.controller;

import android.content.Intent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.MainActivity;
import com.example.masrouf1.R;

abstract class BaseFeatureActivity extends AppCompatActivity {
    protected FrameLayout content;
    protected void showPage(int title) {
        setContentView(R.layout.activity_section); ((TextView)findViewById(R.id.section_title)).setText(title);
        content=findViewById(R.id.section_content);
    }
    protected int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
    public void goHome(View ignored) { startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish(); }
}
