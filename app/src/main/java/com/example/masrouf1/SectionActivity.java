package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public abstract class SectionActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (requiresParentRole() && AppRole.from(getSharedPreferences("masrouf_preferences", MODE_PRIVATE)
                .getString("role", AppRole.USER.value())) != AppRole.PARENT) {
            Toast.makeText(this, R.string.nav_children, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        setContentView(R.layout.activity_section);
        ((TextView) findViewById(R.id.section_title)).setText(titleResource());
        View content = createSectionContent();
        if (content != null) ((FrameLayout) findViewById(R.id.section_content)).addView(content,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    }

    protected boolean requiresParentRole() { return false; }
    protected abstract int titleResource();
    protected abstract View createSectionContent();

    public void goHome(View ignored) {
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    protected TextView bodyText(String text) {
        TextView body = new TextView(this); body.setText(text); body.setTextColor(getColor(R.color.muted_green));
        body.setTextSize(15f); body.setGravity(Gravity.CENTER_VERTICAL); return body;
    }
}
