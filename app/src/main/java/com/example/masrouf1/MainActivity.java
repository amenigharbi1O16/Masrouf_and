package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.masrouf.controller.SessionStore;
import com.masrouf.controller.VoiceTransactionDialogFragment;

/** Personal-account dashboard host. */
public final class MainActivity extends AppCompatActivity {
    public static final String EXTRA_CENTRAL_TAB="com.example.masrouf1.CENTRAL_TAB";
    private DrawerLayout drawer;
    private View drawerPanel;
    private BottomNavigationView bottomNavigation;
    private int activeTab=R.id.tab_home;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SessionStore.userId(this) == 0L) {
            startActivity(new Intent(this, com.masrouf.controller.WelcomeActivity.class)); finish(); return;
        }
        setContentView(R.layout.activity_dashboard_host);
        bottomNavigation=findViewById(R.id.dashboard_bottom_navigation);
        drawer=findViewById(R.id.user_drawer);
        AppRole role=SessionStore.role(this);
        View personalPanel=findViewById(R.id.personal_drawer_panel),adminPanel=findViewById(R.id.admin_drawer_panel);
        View drawerContainer=findViewById(R.id.drawer_container);
        drawerPanel = drawerContainer != null ? drawerContainer : (role == AppRole.PARENT ? adminPanel : personalPanel);
        if(role==AppRole.PARENT) {
            personalPanel.setVisibility(View.GONE); adminPanel.setVisibility(View.VISIBLE);
            int width=Math.min((int)(getResources().getDisplayMetrics().widthPixels*.88f),(int)(360*getResources().getDisplayMetrics().density+.5f));
            ViewGroup.LayoutParams params=adminPanel.getLayoutParams(); if(params!=null){params.width=width; adminPanel.setLayoutParams(params);}
            AdminDrawer.populate(this,findViewById(R.id.admin_drawer_menu_items));
        } else {
            adminPanel.setVisibility(View.GONE); personalPanel.setVisibility(View.VISIBLE);
            AppDrawer.populate(this,findViewById(R.id.drawer_menu_items));
        }
        TextView badge=findViewById(R.id.personal_drawer_badge);
        badge.setText(role==AppRole.PARENT?R.string.drawer_parent:role==AppRole.CHILD?R.string.drawer_child:R.string.drawer_personal);
        buildLanguageButtons(role==AppRole.PARENT?R.id.admin_language_buttons:R.id.personal_language_buttons);
        findViewById(role==AppRole.PARENT?R.id.admin_logout:R.id.personal_logout).setOnClickListener(v->{
            com.masrouf.controller.SessionStore.clear(this);
            Intent welcome=new Intent(this,com.masrouf.controller.WelcomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(welcome);
        });
        if(savedInstanceState==null){activeTab=tabIdForName(getIntent().getStringExtra(EXTRA_CENTRAL_TAB));showCentralTab(activeTab);bottomNavigation.setSelectedItemId(activeTab);}
        else {
            FragmentManager manager=getSupportFragmentManager();
            for(Fragment fragment:manager.getFragments())if(fragment.isVisible()){activeTab=tabForFragment(fragment);break;}
            bottomNavigation.setSelectedItemId(activeTab);
        }
        bottomNavigation.setOnItemSelectedListener(item->{showCentralTab(item.getItemId());return true;});
        FloatingActionButton voice = findViewById(R.id.voice_transaction_button);
        voice.setOnClickListener(v -> new VoiceTransactionDialogFragment().show(getSupportFragmentManager(), "voice-transaction"));
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},210);
    }

    public void openMenu() { drawer.openDrawer(drawerPanel); }
    public void openCentralTab(String destination) {
        int tab=tabIdForName(destination);
        if(drawer!=null&&drawer.isDrawerOpen(drawerPanel))drawer.closeDrawer(drawerPanel);
        if(bottomNavigation!=null)bottomNavigation.setSelectedItemId(tab);else showCentralTab(tab);
    }

    private int tabIdForName(String name){switch(name==null?"home":name){case "products":return R.id.tab_products;case "calendar":return R.id.tab_calendar;case "analytics":return R.id.tab_analytics;case "settings":return R.id.tab_settings;default:return R.id.tab_home;}}

    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);String tab=intent.getStringExtra(EXTRA_CENTRAL_TAB);if(tab!=null)openCentralTab(tab);}

    private void showCentralTab(int tabId) {
        FragmentManager manager=getSupportFragmentManager();String tag="central-tab-"+tabId;Fragment target=manager.findFragmentByTag(tag);
        if(target==null){
            if(tabId==R.id.tab_home)target=new com.masrouf.controller.HomeFragment();
            else if(tabId==R.id.tab_products)target=new ProductsFragment();
            else if(tabId==R.id.tab_calendar)target=new CalendarFragment();
            else if(tabId==R.id.tab_analytics)target=new AnalyticsFragment();
            else target=new SettingsFragment();
        }
        FragmentTransaction transaction=manager.beginTransaction().setReorderingAllowed(true);
        for(Fragment fragment:manager.getFragments())if(fragment.getId()==R.id.dashboard_fragment_host)transaction.hide(fragment);
        if(target.isAdded())transaction.show(target);else transaction.add(R.id.dashboard_fragment_host,target,tag);
        transaction.commit();activeTab=tabId;
    }

    private int tabForFragment(Fragment fragment){
        if(fragment instanceof com.masrouf.controller.HomeFragment)return R.id.tab_home;
        if(fragment instanceof ProductsFragment)return R.id.tab_products;
        if(fragment instanceof CalendarFragment)return R.id.tab_calendar;
        if(fragment instanceof AnalyticsFragment)return R.id.tab_analytics;
        return R.id.tab_settings;
    }

    @Override public void onBackPressed(){
        if(drawer!=null && drawerPanel!=null && drawer.isDrawerOpen(drawerPanel)){drawer.closeDrawer(drawerPanel);return;}
        if(activeTab!=R.id.tab_home){openCentralTab("home");return;}
        super.onBackPressed();
    }
    private void buildLanguageButtons(int rowId) {
        LinearLayout row=findViewById(rowId);
        for(String tag:new String[]{"fr","ar","en"}) {
            MaterialButton button=new MaterialButton(this); button.setText(tag.toUpperCase(java.util.Locale.ROOT)); button.setAllCaps(false); button.setTextSize(12); button.setCornerRadius((int)(16*getResources().getDisplayMetrics().density));
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,(int)(42*getResources().getDisplayMetrics().density),1); params.setMargins(3,0,3,0);
            button.setOnClickListener(v->AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))); row.addView(button,params);
        }
    }

    /** Keeps the role drawer destinations available to parent and child profile hosts. */
    public void onDrawerDestination(String destination) {
        if(drawer!=null && drawer.isDrawerOpen(drawerPanel)) drawer.closeDrawer(drawerPanel);
        if("home".equals(destination)||"products".equals(destination)||"calendar".equals(destination)||"settings".equals(destination)) {openCentralTab(destination);return;}
        if("analytics".equals(destination)&&SessionStore.role(this)!=AppRole.PARENT){openCentralTab(destination);return;}
        Class<?> screen;
        switch(destination) {
            case "children": screen=com.masrouf.controller.ChildrenManagementActivity.class; break;
            case "analytics": screen=SessionStore.role(this)==AppRole.PARENT?FamilyBudgetsAnalyticsActivity.class:AnalyticsActivity.class; break;
            case "rewards": screen=RewardsActivity.class; break;
            case "notifications": screen=NotificationsActivity.class; break;
            default: return;
        }
        startActivity(new Intent(this,screen).putExtra("USER_ID",SessionStore.userId(this)));
    }
}
