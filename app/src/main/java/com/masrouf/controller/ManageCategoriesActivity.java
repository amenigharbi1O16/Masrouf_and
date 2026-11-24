package com.masrouf.controller;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.R;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;

import java.util.ArrayList;
import java.util.List;

public final class ManageCategoriesActivity extends BaseFeatureActivity {
    private HomeCategoryAdapter adapter;
    private MasroufViewModel viewModel;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showPage(R.string.your_categories);
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        LinearLayout page=new LinearLayout(this); page.setGravity(Gravity.CENTER); page.setOrientation(LinearLayout.VERTICAL);
        getSupportFragmentManager().setFragmentResultListener("masrouf-categories-updated",this,(key,result)->load());
        RecyclerView list=new RecyclerView(this); list.setLayoutManager(new GridLayoutManager(this,1)); adapter=new HomeCategoryAdapter(); list.setAdapter(adapter); page.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        content.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-1));
        viewModel.categories().observe(this,dataState->{if(dataState!=null&&dataState.data!=null&&adapter!=null)adapter.submit(dataState.data);});
    }
    @Override protected void onResume() { super.onResume(); load(); }
    private void load() {
        if(adapter==null) return;
        viewModel.loadCategories(SessionStore.userId(this));
    }
}
