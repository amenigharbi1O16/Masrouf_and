package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.masrouf1.LocaleController;
import com.example.masrouf1.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.Locale;

public final class WelcomeActivity extends AppCompatActivity {
    private LinearLayout welcomePhase, slidePhase, accountPhase;
    private ViewPager2 pager;
    private MaterialButton next;
    private TabLayoutMediator mediator;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(SessionStore.userId(this)>0) { startActivity(new Intent(this,com.example.masrouf1.MainActivity.class)); finish(); return; }
        setContentView(R.layout.activity_welcome);
        welcomePhase = findViewById(R.id.phase_welcome); slidePhase = findViewById(R.id.phase_slides); accountPhase = findViewById(R.id.phase_select_type);
        pager = findViewById(R.id.welcome_pager); next = findViewById(R.id.slide_next);
        buildLanguages();
        findViewById(R.id.start_welcome).setOnClickListener(v -> showPhase2());
        findViewById(R.id.open_login).setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.personal_card).setOnClickListener(v -> openRegister("personal"));
        findViewById(R.id.family_card).setOnClickListener(v -> openRegister("family"));
        pager.setAdapter(new WelcomeSlideAdapter());
        mediator = new TabLayoutMediator((TabLayout) findViewById(R.id.welcome_tabs), pager, (tab, position) -> { });
        mediator.attach();
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { next.setText(position==2?R.string.start:R.string.welcome_next); }
        });
        next.setOnClickListener(v -> {
            if (pager.getCurrentItem() < 2) pager.setCurrentItem(pager.getCurrentItem() + 1, true);
            else showPhase3();
        });
    }

    private void showPhase2() { welcomePhase.setVisibility(View.GONE); slidePhase.setVisibility(View.VISIBLE); }
    private void showPhase3() { slidePhase.setVisibility(View.GONE); accountPhase.setVisibility(View.VISIBLE); }
    private void openRegister(String type) {
        Intent intent = new Intent(this, RegisterActivity.class); intent.putExtra("TYPE", type); startActivity(intent);
    }

    private void buildLanguages() {
        LinearLayout row = findViewById(R.id.welcome_languages);
        for (String tag : new String[]{"fr", "ar", "en"}) {
            TextView language = new TextView(this); language.setText(tag.toUpperCase(Locale.ROOT)); language.setGravity(Gravity.CENTER);
            language.setTextColor(getColor(R.color.emerald)); language.setTextSize(11); language.setPadding(8, 8, 8, 8);
            language.setOnClickListener(v -> LocaleController.apply(tag)); row.addView(language);
        }
    }

    private final class WelcomeSlideAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<WelcomeSlideAdapter.Holder> {
        private final int[] titleIds = {R.string.slide_one_title, R.string.slide_two_title, R.string.slide_three_title};
        private final int[] bodyIds = {R.string.slide_one_body, R.string.slide_two_body, R.string.slide_three_body};
        private final String[] icons = {"↗", "◉", "✦"};
        @Override public Holder onCreateViewHolder(ViewGroup parent, int type) {
            LinearLayout page = new LinearLayout(WelcomeActivity.this); page.setGravity(Gravity.CENTER); page.setOrientation(LinearLayout.VERTICAL); page.setPadding(20, 24, 20, 24);
            TextView icon = new TextView(WelcomeActivity.this); icon.setGravity(Gravity.CENTER); icon.setTextColor(getColor(R.color.emerald)); icon.setTextSize(76);
            page.addView(icon, new LinearLayout.LayoutParams(-1, dp(130)));
            TextView title = new TextView(WelcomeActivity.this); title.setGravity(Gravity.CENTER); title.setTextColor(getColor(R.color.forest)); title.setTextSize(24); title.setTypeface(null, android.graphics.Typeface.BOLD);
            page.addView(title, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView body = new TextView(WelcomeActivity.this); body.setGravity(Gravity.CENTER); body.setTextColor(getColor(R.color.muted_green)); body.setTextSize(15); body.setPadding(8, 12, 8, 12);
            page.addView(body, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
            return new Holder(page, icon, title, body);
        }
        @Override public void onBindViewHolder(Holder holder, int position) {
            holder.icon.setText(icons[position]); holder.title.setText(titleIds[position]); holder.body.setText(bodyIds[position]);
        }
        @Override public int getItemCount() { return titleIds.length; }
        final class Holder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final TextView icon, title, body;
            Holder(View view, TextView icon, TextView title, TextView body) { super(view); this.icon=icon; this.title=title; this.body=body; }
        }
    }
    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    @Override protected void onDestroy() { if (mediator != null) mediator.detach(); super.onDestroy(); }
}
