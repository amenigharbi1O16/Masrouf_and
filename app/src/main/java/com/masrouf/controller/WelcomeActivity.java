package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.masrouf1.LocaleController;
import com.example.masrouf1.R;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * WelcomeActivity — entry point with 2 phases:
 *  Phase 1: Welcome splash (logo + "Commencer" button)
 *  Phase 2: 3-slide intro carousel → routes to ChooseAccountTypeActivity
 */
public final class WelcomeActivity extends AppCompatActivity {

    private LinearLayout phaseWelcome, phaseSlides;
    private LinearLayout welcomeBottom, slideBottom;
    private ViewPager2 pager;
    private LinearLayout dotsContainer;
    private MaterialButton slideNext;
    private int currentSlide = 0;
    private static final int TOTAL_SLIDES = 3;

    // Slide data
    private final int[] slideTitleIds = {
            R.string.slide_one_title,
            R.string.slide_two_title,
            R.string.slide_three_title
    };
    private final int[] slideBodyIds = {
            R.string.slide_one_body,
            R.string.slide_two_body,
            R.string.slide_three_body
    };
    private final int[] slideBullet1Ids = {
            R.string.slide_one_bullet1,
            R.string.slide_two_bullet1,
            R.string.slide_three_bullet1
    };
    private final int[] slideBullet2Ids = {
            R.string.slide_one_bullet2,
            R.string.slide_two_bullet2,
            R.string.slide_three_bullet2
    };
    private final int[] slideIconIds = {
            R.drawable.ic_wallet,
            R.drawable.ic_pie_chart,
            R.drawable.ic_people
    };

    private static final String STATE_IN_SLIDES = "in_slides";
    private static final String STATE_SLIDE_INDEX = "slide_index";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Welcome Page is always the first screen shown on cold app launch; clear any incomplete session.
        if (savedInstanceState == null && SessionStore.userId(this) > 0
                && (!SessionStore.categoriesSelected(this) || !isSessionOnboarded())) {
            SessionStore.clear(this);
        }

        setContentView(R.layout.activity_welcome);

        phaseWelcome = findViewById(R.id.phase_welcome);
        phaseSlides = findViewById(R.id.phase_slides);
        welcomeBottom = findViewById(R.id.welcome_bottom);
        slideBottom = findViewById(R.id.slide_bottom);
        pager = findViewById(R.id.welcome_pager);
        dotsContainer = findViewById(R.id.slide_dots);
        slideNext = findViewById(R.id.slide_next);

        // Build language buttons
        buildLanguageButtons(R.id.welcome_languages);

        // Welcome phase: "Commencer" button
        MaterialButton startBtn = findViewById(R.id.start_welcome);
        startBtn.setOnClickListener(v -> showPhase2());

        // Welcome phase: "Se connecter" link (with HTML bold)
        TextView openLogin = findViewById(R.id.open_login);
        openLogin.setText(android.text.Html.fromHtml(getString(R.string.already_account_link), android.text.Html.FROM_HTML_MODE_COMPACT));
        openLogin.setOnClickListener(v -> goToLogin());

        // Slide phase: back arrow
        ImageView slideBack = findViewById(R.id.slide_back);
        slideBack.setOnClickListener(v -> {
            if (currentSlide > 0) {
                pager.setCurrentItem(currentSlide - 1, true);
            } else {
                showPhase1();
            }
        });

        // Slide phase: "Passer" skip link
        TextView slideSkip = findViewById(R.id.slide_skip);
        slideSkip.setOnClickListener(v -> goToChooseAccountType());

        // Slide "Se connecter"
        TextView slideOpenLogin = findViewById(R.id.slide_open_login);
        slideOpenLogin.setText(android.text.Html.fromHtml(getString(R.string.already_account_link), android.text.Html.FROM_HTML_MODE_COMPACT));
        slideOpenLogin.setOnClickListener(v -> goToLogin());

        // "Continuer" / "Choisissez votre type de compte" button
        slideNext.setOnClickListener(v -> {
            if (currentSlide < TOTAL_SLIDES - 1) {
                pager.setCurrentItem(currentSlide + 1, true);
            } else {
                goToChooseAccountType();
            }
        });

        // Build dots
        buildDots();

        // Setup pager
        pager.setAdapter(new SlideAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentSlide = position;
                updateDots(position);
                updateNextButton(position);
            }
        });

        if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_IN_SLIDES, false)) {
            int restoredSlide = Math.max(0, Math.min(TOTAL_SLIDES - 1, savedInstanceState.getInt(STATE_SLIDE_INDEX, 0)));
            showPhase2();
            currentSlide = restoredSlide;
            pager.setCurrentItem(restoredSlide, false);
            updateDots(restoredSlide);
            updateNextButton(restoredSlide);
        } else {
            updateNextButton(0);
            updateDots(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_IN_SLIDES, phaseSlides != null && phaseSlides.getVisibility() == View.VISIBLE);
        outState.putInt(STATE_SLIDE_INDEX, currentSlide);
    }

    private boolean isSessionOnboarded() {
        return getSharedPreferences("masrouf_preferences", MODE_PRIVATE).getBoolean("onboarded", false);
    }

    private void showPhase1() {
        phaseWelcome.setVisibility(View.VISIBLE);
        phaseSlides.setVisibility(View.GONE);
        welcomeBottom.setVisibility(View.VISIBLE);
        slideBottom.setVisibility(View.GONE);
    }

    private void showPhase2() {
        phaseWelcome.setVisibility(View.GONE);
        phaseSlides.setVisibility(View.VISIBLE);
        welcomeBottom.setVisibility(View.GONE);
        slideBottom.setVisibility(View.VISIBLE);
        currentSlide = 0;
        pager.setCurrentItem(0, false);
        updateDots(0);
        updateNextButton(0);
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    private void goToChooseAccountType() {
        startActivity(new Intent(this, ChooseAccountTypeActivity.class));
    }

    private void updateNextButton(int position) {
        if (position == TOTAL_SLIDES - 1) {
            slideNext.setText(R.string.choose_account_type_action);
        } else {
            slideNext.setText(R.string.welcome_next);
        }
    }

    private void buildDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < TOTAL_SLIDES; i++) {
            View dot = new View(this);
            int size = dp(10);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(dp(5), 0, dp(5), 0);
            dot.setLayoutParams(params);
            dot.setTag(i);
            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int active) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            if (i == active) {
                dot.setBackgroundResource(R.drawable.bg_dot_active);
            } else {
                dot.setBackgroundResource(R.drawable.bg_dot_inactive);
            }
        }
    }

    private void buildLanguageButtons(int containerId) {
        LinearLayout row = findViewById(containerId);
        if (row == null) return;
        row.removeAllViews();
        String currentTag = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (currentTag.isEmpty()) currentTag = Locale.getDefault().getLanguage();
        String activeLang = currentTag.length() >= 2 ? currentTag.substring(0, 2).toLowerCase(Locale.ROOT) : "fr";
        String[] tags = {"fr", "ar", "en"};
        String[] labels = {"FR", "AR", "EN"};
        for (int i = 0; i < tags.length; i++) {
            final String tag = tags[i];
            final String label = labels[i];
            boolean selected = tag.equals(activeLang);
            TextView tv = new TextView(this);
            tv.setText(label);
            tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextSize(13f);
            tv.setPadding(dp(12), dp(6), dp(12), dp(6));
            tv.setTextColor(getColor(selected ? R.color.alabaster : R.color.muted_green));
            tv.setBackground(getDrawable(selected ? R.drawable.bg_lang_selected : R.drawable.bg_lang_unselected));
            tv.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(dp(4));
            tv.setOnClickListener(v -> {
                LocaleController.apply(tag);
                // setApplicationLocales() automatically recreates the activity — no manual recreate() needed
            });
            row.addView(tv, params);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ─── Slide ViewPager2 adapter ────────────────────────────────────────────

    private final class SlideAdapter extends RecyclerView.Adapter<SlideAdapter.Holder> {

        @Override
        public Holder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_welcome_slide, parent, false);
            return new Holder(v);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.icon.setImageResource(slideIconIds[position]);
            holder.title.setText(slideTitleIds[position]);
            holder.body.setText(slideBodyIds[position]);
            holder.bullet1.setText(slideBullet1Ids[position]);
            holder.bullet2.setText(slideBullet2Ids[position]);
            // Last slide uses blue checkmarks
            int checkRes = position == 2 ? R.drawable.ic_check_circle_blue : R.drawable.ic_check_circle;
            holder.bullet1Icon.setImageResource(checkRes);
            holder.bullet2Icon.setImageResource(checkRes);
        }

        @Override
        public int getItemCount() { return TOTAL_SLIDES; }

        final class Holder extends RecyclerView.ViewHolder {
            final ImageView icon, bullet1Icon, bullet2Icon;
            final TextView title, body, bullet1, bullet2;
            Holder(android.view.View v) {
                super(v);
                icon = v.findViewById(R.id.slide_icon);
                title = v.findViewById(R.id.slide_title);
                body = v.findViewById(R.id.slide_body);
                bullet1 = v.findViewById(R.id.slide_bullet1);
                bullet2 = v.findViewById(R.id.slide_bullet2);
                bullet1Icon = v.findViewById(R.id.slide_bullet1_icon);
                bullet2Icon = v.findViewById(R.id.slide_bullet2_icon);
            }
        }
    }
}
