package com.masrouf.controller;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Initial family/user setup. System catalog rows remain separate from confirmed user selections. */
public final class OnboardingActivity extends AppCompatActivity {
    private int step=1;
    private long userId;
    private LinearLayout first,second,third,summary;
    private TextView stepLabel;
    private TextView categoryCount;
    private MaterialButton next;
    private ProgressBar categoryLoading,saveProgress;
    private RecyclerView categoryList;
    private CategoryAdapter adapter;
    private MasroufViewModel viewModel;
    private UserEntity currentUser;
    private List<SystemCategoryEntity> availableCategories=new ArrayList<>();
    private List<CategoryEntity> existingCategories=new ArrayList<>();
    private boolean saving;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);setContentView(R.layout.activity_onboarding);
        userId=getIntent().getLongExtra("USER_ID",SessionStore.userId(this));
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        first=findViewById(R.id.onboarding_step_one);second=findViewById(R.id.onboarding_step_two);third=findViewById(R.id.onboarding_step_three);
        summary=findViewById(R.id.onboarding_summary_list);stepLabel=findViewById(R.id.onboarding_step_label);next=findViewById(R.id.onboarding_next);
        categoryCount=findViewById(R.id.onboarding_category_count);categoryLoading=findViewById(R.id.onboarding_category_loading);saveProgress=findViewById(R.id.onboarding_save_progress);
        categoryList=findViewById(R.id.onboarding_category_list);categoryList.setLayoutManager(new GridLayoutManager(this,getResources().getConfiguration().screenWidthDp>=480?2:1));
        next.setEnabled(false);next.setOnClickListener(v->advance());renderStep();loadSelections();
    }

    private void loadSelections() {
        categoryLoading.setVisibility(View.VISIBLE);
        viewModel.<OnboardingSnapshot>request(repository->{
            UserEntity user=repository.user(userId);
            if(user==null)throw new IllegalStateException("Unknown account");
            return new OnboardingSnapshot(user,repository.onboardingCategories(userId),repository.selectedCatalogIds(userId),repository.categories(userId));
        },state->{
            categoryLoading.setVisibility(View.GONE);
            OnboardingSnapshot snapshot=state.data;
            if(snapshot==null){next.setEnabled(false);Toast.makeText(this,R.string.category_selection_failed,Toast.LENGTH_LONG).show();return;}
            currentUser=snapshot.user;availableCategories=snapshot.available;existingCategories=snapshot.existing;
            ArrayList<CategoryOption> options=new ArrayList<>();Set<String> selectedKeys=new HashSet<>();
            Set<Long> selectedIds=new HashSet<>(snapshot.selectedCatalogIds);
            for(SystemCategoryEntity category:availableCategories){
                options.add(new CategoryOption(category.id,category.key,getString(SystemCategoryCatalog.titleResource(category.key)),
                        category.colorHex,category.type,category.recommendedPercent,category.iconName,category.protectedCategory));
                if(selectedIds.contains(category.id))selectedKeys.add(category.key);
            }
            viewModel.initializeCategorySelection(selectedKeys);
            adapter=new CategoryAdapter(options,key->viewModel.toggleCategorySelection(key));
            categoryList.setAdapter(adapter);
            viewModel.categorySelection().observe(this,keys->{
                if(adapter==null||keys==null)return;
                adapter.setSelectedKeys(keys);categoryCount.setText(getResources().getQuantityString(R.plurals.category_selection_count,keys.size(),keys.size()));
                boolean valid=keys.size()>0&&adapter.hasProtectedFallbackSelected();
                next.setEnabled(step!=2||valid);
            });
            categoryLoading.setVisibility(View.GONE);
            if(!availableCategories.isEmpty())next.setEnabled(step==1||!viewModel.currentCategorySelection().isEmpty());
            if(availableCategories.isEmpty())Toast.makeText(this,R.string.category_selection_failed,Toast.LENGTH_LONG).show();
        });
    }

    private void advance() {
        if(currentUser==null||adapter==null||saving)return;
        if(step==1) {
            if(currentUser.categoriesSelected){renderSummary();step=3;}else step=2;
            renderStep();return;
        }
        if(step==2) {
            if(viewModel.currentCategorySelection().isEmpty()){Toast.makeText(this,R.string.categories_required,Toast.LENGTH_SHORT).show();return;}
            if(!adapter.hasProtectedFallbackSelected()){Toast.makeText(this,R.string.category_other_required,Toast.LENGTH_LONG).show();return;}
            saveAndOpenHome();return;
        }
        saveAndOpenHome();
    }

    private void renderStep() {
        first.setVisibility(step==1?View.VISIBLE:View.GONE);second.setVisibility(step==2?View.VISIBLE:View.GONE);third.setVisibility(step==3?View.VISIBLE:View.GONE);
        stepLabel.setText(step+" / 3");next.setText(step==1?R.string.choose_categories:step==2?R.string.onboarding_confirm_selection:R.string.go_home);
        if(step==2&&adapter!=null){Set<String> keys=viewModel.currentCategorySelection();next.setEnabled(!keys.isEmpty()&&adapter.hasProtectedFallbackSelected());}
    }

    private void renderSummary() {
        summary.removeAllViews();
        if(currentUser!=null&&currentUser.categoriesSelected) {
            for(CategoryEntity category:existingCategories)addSummaryRow(category.name);
            if(existingCategories.isEmpty())addSummaryRow(getString(R.string.categories_confirmed_summary));
            return;
        }
        Set<String> selected=viewModel.currentCategorySelection();for(CategoryOption option:availableOptions())if(selected.contains(option.key))addSummaryRow(option.name);
    }

    private void addSummaryRow(String name) {
        TextView row=new TextView(this);row.setText("✓  "+name);row.setTextColor(getColor(R.color.emerald));row.setTextSize(16);
        row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(8,12,8,12);summary.addView(row,new LinearLayout.LayoutParams(-1,52));
    }

    private void saveAndOpenHome() {
        saving=true;next.setEnabled(false);next.setText(R.string.onboarding_saving);saveProgress.setVisibility(View.VISIBLE);
        ArrayList<SystemCategoryEntity> chosen=new ArrayList<>();
        if(currentUser!=null&&!currentUser.categoriesSelected){
            Set<String> selectedKeys=viewModel.currentCategorySelection();
            for(SystemCategoryEntity category:availableCategories)if(selectedKeys.contains(category.key))chosen.add(category);
        }
        viewModel.<Boolean>request(repository->repository.saveOnboarding(userId,chosen),result->{
            if(!Boolean.TRUE.equals(result.data)){
                saving=false;saveProgress.setVisibility(View.GONE);next.setEnabled(true);next.setText(step==2?R.string.onboarding_confirm_selection:R.string.go_home);Toast.makeText(this,R.string.category_selection_failed,Toast.LENGTH_LONG).show();return;
            }
            SessionStore.markOnboardingComplete(this);
            startActivity(new android.content.Intent(this,com.example.masrouf1.MainActivity.class).putExtra("USER_ID",userId));finish();
        });
    }

    private List<CategoryOption> availableOptions(){return adapter==null?new ArrayList<>():adapter.options();}

    private static final class OnboardingSnapshot {
        final UserEntity user;final List<SystemCategoryEntity> available;final List<Long> selectedCatalogIds;final List<CategoryEntity> existing;
        OnboardingSnapshot(UserEntity user,List<SystemCategoryEntity> available,List<Long> selectedCatalogIds,List<CategoryEntity> existing){
            this.user=user;this.available=available;this.selectedCatalogIds=selectedCatalogIds;this.existing=existing;
        }
    }
}
