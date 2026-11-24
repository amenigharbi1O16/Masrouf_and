package com.masrouf.controller;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.R;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public final class OnboardingActivity extends AppCompatActivity {
    private int step=1;
    private long userId;
    private LinearLayout first,second,third,summary;
    private TextView stepLabel;
    private MaterialButton next;
    private CategoryAdapter adapter;
    private MasroufViewModel viewModel;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_onboarding);
        userId=getIntent().getLongExtra("USER_ID",SessionStore.userId(this));viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        first=findViewById(R.id.onboarding_step_one); second=findViewById(R.id.onboarding_step_two); third=findViewById(R.id.onboarding_step_three);
        summary=findViewById(R.id.onboarding_summary_list); stepLabel=findViewById(R.id.onboarding_step_label); next=findViewById(R.id.onboarding_next);
        RecyclerView categories=findViewById(R.id.onboarding_category_list); categories.setLayoutManager(new GridLayoutManager(this,2));
        List<CategoryOption> presets=presets(); adapter=new CategoryAdapter(presets,()->{}); categories.setAdapter(adapter);
        next.setOnClickListener(v->advance()); renderStep();
    }

    private void advance() {
        if(step==1) { step=2; renderStep(); return; }
        if(step==2) {
            if(adapter.selectedOptions().isEmpty()) { Toast.makeText(this,R.string.categories_required,Toast.LENGTH_SHORT).show(); return; }
            renderSummary(); step=3; renderStep(); return;
        }
        saveAndOpenHome();
    }
    private void renderStep() {
        first.setVisibility(step==1?View.VISIBLE:View.GONE); second.setVisibility(step==2?View.VISIBLE:View.GONE); third.setVisibility(step==3?View.VISIBLE:View.GONE);
        stepLabel.setText(step+" / 3"); next.setText(step==1?R.string.choose_categories:step==2?R.string.welcome_next:R.string.go_home);
    }
    private void renderSummary() {
        summary.removeAllViews();
        for(CategoryOption option:adapter.selectedOptions()) {
            TextView row=new TextView(this); row.setText("✓  "+option.name); row.setTextColor(getColor(R.color.emerald)); row.setTextSize(16);
            row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(8,12,8,12); summary.addView(row,new LinearLayout.LayoutParams(-1,52));
        }
    }
    private void saveAndOpenHome() {
        next.setEnabled(false); next.setText(R.string.onboarding_saving);
        ArrayList<CategoryEntity> chosen=new ArrayList<>();
        for(CategoryOption option:adapter.selectedOptions()) {
            CategoryEntity row=new CategoryEntity(); row.userId=userId; row.name=option.name; row.colorHex=option.colorHex; row.type=option.type; row.allocationPercent=option.allocationPercent; row.isEssential=false; chosen.add(row);
        }
        viewModel.<Boolean>request(repository->repository.saveOnboarding(userId,chosen),result->{
            boolean saved=Boolean.TRUE.equals(result.data);
            if(!saved) { next.setEnabled(true); next.setText(R.string.go_home); Toast.makeText(this,R.string.auth_error,Toast.LENGTH_SHORT).show(); return; }
            startActivity(new android.content.Intent(this,com.example.masrouf1.MainActivity.class).putExtra("USER_ID",userId)); finish();
        });
    }
    private List<CategoryOption> presets() {
        ArrayList<CategoryOption> list=new ArrayList<>();
        list.add(new CategoryOption("food",getString(R.string.category_food),"#2E7D43","expense",0));
        list.add(new CategoryOption("home",getString(R.string.category_home),"#E9B66B","expense",0));
        list.add(new CategoryOption("transport",getString(R.string.category_transport),"#61746D","expense",0));
        list.add(new CategoryOption("health",getString(R.string.category_health),"#DC7770","expense",0));
        list.add(new CategoryOption("leisure",getString(R.string.category_leisure),"#85B8A5","expense",0));
        list.add(new CategoryOption("school",getString(R.string.category_school),"#A58C67","expense",0));
        return list;
    }
}
