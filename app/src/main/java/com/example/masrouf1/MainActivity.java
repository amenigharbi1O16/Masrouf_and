package com.example.masrouf1;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.os.LocaleListCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.masrouf.controller.SessionStore;
import com.masrouf.controller.VoiceTransactionDialogFragment;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Personal-account, child, and parent dashboard host. */
public final class MainActivity extends AppCompatActivity {
    public static final String EXTRA_CENTRAL_TAB = "com.example.masrouf1.CENTRAL_TAB";
    private static final ExecutorService ACCOUNT_GATE = Executors.newSingleThreadExecutor();
    private DrawerLayout drawer;
    private View drawerPanel;
    private LinearLayout activeDrawerMenu;
    private AppRole activeRole = AppRole.USER;
    private int activeTab = R.id.tab_home;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer != null && drawerPanel != null && drawer.isDrawerOpen(drawerPanel)) {
                    drawer.closeDrawer(drawerPanel);
                    return;
                }
                if (activeTab != R.id.tab_home) {
                    openCentralTab("home");
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
        long userId = SessionStore.userId(this);
        if (userId == 0L) {
            openWelcome();
            return;
        }
        FrameLayout shell = new FrameLayout(this);
        View dashboard = LayoutInflater.from(this).inflate(R.layout.activity_dashboard_host, shell, false);
        shell.addView(dashboard, new FrameLayout.LayoutParams(-1, -1));
        TextView loading = new TextView(this);
        loading.setText(R.string.app_name);
        loading.setTextColor(getColor(R.color.forest));
        loading.setGravity(android.view.Gravity.CENTER);
        loading.setBackgroundColor(getColor(R.color.cream));
        shell.addView(loading, new FrameLayout.LayoutParams(-1, -1));
        setContentView(shell);
        ACCOUNT_GATE.execute(() -> {
            UserEntity account;
            try {
                account = MasroufDatabase.getInstance(getApplicationContext()).masroufDao().findUser(userId);
            } catch (RuntimeException failure) {
                account = null;
            }
            UserEntity verified = account;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (verified == null) {
                    SessionStore.clear(this);
                    openWelcome();
                    return;
                }
                SessionStore.save(this, verified);
                if (!verified.categoriesSelected || !verified.onboarded) {
                    startActivity(new Intent(this, com.masrouf.controller.OnboardingActivity.class)
                            .putExtra("USER_ID", verified.id));
                    finish();
                    return;
                }
                loading.setVisibility(View.GONE);
                initializeDashboard(savedInstanceState, verified);
            });
        });
    }

    private void openWelcome() {
        startActivity(new Intent(this, com.masrouf.controller.WelcomeActivity.class));
        finish();
    }

    private void initializeDashboard(Bundle savedInstanceState, UserEntity verified) {
        drawer = findViewById(R.id.user_drawer);
        activeRole = SessionStore.role(this);
        View personalPanel = findViewById(R.id.personal_drawer_panel), adminPanel = findViewById(R.id.admin_drawer_panel);
        View drawerContainer = findViewById(R.id.drawer_container);
        drawerPanel = drawerContainer != null ? drawerContainer : (activeRole == AppRole.PARENT ? adminPanel : personalPanel);

        if (savedInstanceState == null) {
            activeTab = tabIdForName(getIntent().getStringExtra(EXTRA_CENTRAL_TAB));
            showCentralTab(activeTab);
        } else {
            FragmentManager manager = getSupportFragmentManager();
            for (Fragment fragment : manager.getFragments()) {
                if (fragment.isVisible()) {
                    activeTab = tabForFragment(fragment);
                    break;
                }
            }
        }

        String activeDest = destinationForTab(activeTab);
        if (activeRole == AppRole.PARENT) {
            personalPanel.setVisibility(View.GONE);
            adminPanel.setVisibility(View.VISIBLE);
            int width = Math.min((int) (getResources().getDisplayMetrics().widthPixels * .88f),
                    (int) (360 * getResources().getDisplayMetrics().density + .5f));
            ViewGroup.LayoutParams params = adminPanel.getLayoutParams();
            if (params != null) {
                params.width = width;
                adminPanel.setLayoutParams(params);
            }
            activeDrawerMenu = findViewById(R.id.admin_drawer_menu_items);
            AdminDrawer.populate(this, activeDrawerMenu, activeDest);
            TextView adminName = findViewById(R.id.admin_drawer_user_name);
            if (adminName != null && verified.name != null && !verified.name.trim().isEmpty()) {
                adminName.setText(verified.name.trim());
                adminName.setVisibility(View.VISIBLE);
            }
        } else {
            adminPanel.setVisibility(View.GONE);
            personalPanel.setVisibility(View.VISIBLE);
            activeDrawerMenu = findViewById(R.id.drawer_menu_items);
            AppDrawer.populate(this, activeDrawerMenu, activeDest);
            TextView personalName = findViewById(R.id.personal_drawer_user_name);
            if (personalName != null && verified.name != null && !verified.name.trim().isEmpty()) {
                personalName.setText(verified.name.trim());
                personalName.setVisibility(View.VISIBLE);
            }
        }

        TextView badge = findViewById(R.id.personal_drawer_badge);
        badge.setText(activeRole == AppRole.PARENT ? R.string.drawer_parent
                : activeRole == AppRole.CHILD ? R.string.drawer_child : R.string.drawer_personal);
        buildLanguageButtons(activeRole == AppRole.PARENT ? R.id.admin_language_buttons : R.id.personal_language_buttons,
                activeRole == AppRole.PARENT);

        findViewById(activeRole == AppRole.PARENT ? R.id.admin_logout : R.id.personal_logout).setOnClickListener(v -> {
            SessionStore.clear(this);
            Intent welcome = new Intent(this, com.masrouf.controller.WelcomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(welcome);
        });

        FloatingActionButton voice = findViewById(R.id.voice_transaction_button);
        voice.setOnClickListener(v -> new VoiceTransactionDialogFragment().show(getSupportFragmentManager(), "voice-transaction"));
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            android.content.SharedPreferences permissionState = getSharedPreferences("masrouf_notification_permission", MODE_PRIVATE);
            if (!permissionState.getBoolean("requested", false)) {
                permissionState.edit().putBoolean("requested", true).apply();
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 210);
            }
        }
    }

    public void openMenu() {
        drawer.openDrawer(drawerPanel);
    }

    public void openCentralTab(String destination) {
        int tab = tabIdForName(destination);
        if (drawer != null && drawer.isDrawerOpen(drawerPanel)) drawer.closeDrawer(drawerPanel);
        showCentralTab(tab);
    }

    private int tabIdForName(String name) {
        switch (name == null ? "home" : name) {
            case "products": return R.id.tab_products;
            case "calendar": return R.id.tab_calendar;
            case "analytics": return R.id.tab_analytics;
            case "settings": return R.id.tab_settings;
            default: return R.id.tab_home;
        }
    }

    private String destinationForTab(int tabId) {
        if (tabId == R.id.tab_products) return "products";
        if (tabId == R.id.tab_calendar) return "calendar";
        if (tabId == R.id.tab_analytics) return "analytics";
        if (tabId == R.id.tab_settings) return "settings";
        return "home";
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String tab = intent.getStringExtra(EXTRA_CENTRAL_TAB);
        if (tab != null) openCentralTab(tab);
    }

    private void showCentralTab(int tabId) {
        FragmentManager manager = getSupportFragmentManager();
        String tag = "central-tab-" + tabId;
        Fragment target = manager.findFragmentByTag(tag);
        if (target == null) {
            if (tabId == R.id.tab_home) target = new com.masrouf.controller.HomeFragment();
            else if (tabId == R.id.tab_products) target = new ProductsFragment();
            else if (tabId == R.id.tab_calendar) target = new CalendarFragment();
            else if (tabId == R.id.tab_analytics) target = new AnalyticsFragment();
            else target = new SettingsFragment();
        }
        FragmentTransaction transaction = manager.beginTransaction().setReorderingAllowed(true);
        for (Fragment fragment : manager.getFragments()) {
            if (fragment.getId() == R.id.dashboard_fragment_host) transaction.hide(fragment);
        }
        if (target.isAdded()) transaction.show(target);
        else transaction.add(R.id.dashboard_fragment_host, target, tag);
        transaction.commit();
        activeTab = tabId;
        refreshDrawerSelection(destinationForTab(tabId));
    }

    private void refreshDrawerSelection(String destination) {
        if (activeDrawerMenu == null) return;
        if (activeRole == AppRole.PARENT) AdminDrawer.updateSelection(activeDrawerMenu, destination);
        else AppDrawer.updateSelection(activeDrawerMenu, destination);
    }

    private int tabForFragment(Fragment fragment) {
        if (fragment instanceof com.masrouf.controller.HomeFragment) return R.id.tab_home;
        if (fragment instanceof ProductsFragment) return R.id.tab_products;
        if (fragment instanceof CalendarFragment) return R.id.tab_calendar;
        if (fragment instanceof AnalyticsFragment) return R.id.tab_analytics;
        return R.id.tab_settings;
    }

    private void buildLanguageButtons(int rowId, boolean darkDrawer) {
        LinearLayout row = findViewById(rowId);
        if (row == null) return;
        row.removeAllViews();
        String currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (currentTag.isEmpty()) currentTag = Locale.getDefault().getLanguage();
        String activeLang = currentTag.length() >= 2 ? currentTag.substring(0, 2).toLowerCase(Locale.ROOT) : "fr";

        float density = getResources().getDisplayMetrics().density;
        for (String tag : new String[]{"fr", "ar", "en"}) {
            boolean selected = tag.equals(activeLang);
            MaterialButton button = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setText(tag.toUpperCase(Locale.ROOT));
            button.setAllCaps(false);
            button.setTextSize(12);
            button.setCornerRadius((int) (16 * density));
            button.setStrokeWidth((int) (1 * density));
            if (darkDrawer) {
                button.setBackgroundTintList(ColorStateList.valueOf(getColor(selected ? R.color.emerald : R.color.muted_green)));
                button.setTextColor(getColor(R.color.alabaster));
                button.setStrokeColor(ColorStateList.valueOf(getColor(selected ? R.color.mint : R.color.mint_accent)));
            } else {
                button.setBackgroundTintList(ColorStateList.valueOf(getColor(selected ? R.color.forest : R.color.muted_green)));
                button.setTextColor(getColor(R.color.alabaster));
                button.setStrokeColor(ColorStateList.valueOf(getColor(selected ? R.color.forest : R.color.muted_green)));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, (int) (40 * density), 1);
            params.setMargins(3, 0, 3, 0);
            button.setOnClickListener(v -> LocaleController.apply(tag));
            row.addView(button, params);
        }
    }

    /** Keeps the role drawer destinations available to parent and child profile hosts. */
    public void onDrawerDestination(String destination) {
        if (drawer != null && drawer.isDrawerOpen(drawerPanel)) drawer.closeDrawer(drawerPanel);
        if ("home".equals(destination) || "products".equals(destination)
                || "calendar".equals(destination) || "settings".equals(destination)) {
            openCentralTab(destination);
            return;
        }
        if ("analytics".equals(destination) && SessionStore.role(this) != AppRole.PARENT) {
            openCentralTab(destination);
            return;
        }
        Class<?> screen;
        switch (destination) {
            case "children":
                screen = com.masrouf.controller.ChildrenManagementActivity.class;
                break;
            case "analytics":
                screen = SessionStore.role(this) == AppRole.PARENT
                        ? FamilyBudgetsAnalyticsActivity.class : AnalyticsActivity.class;
                break;
            case "rewards":
                screen = RewardsActivity.class;
                break;
            case "notifications":
                screen = NotificationsActivity.class;
                break;
            default:
                return;
        }
        startActivity(new Intent(this, screen).putExtra("USER_ID", SessionStore.userId(this)));
    }
}
