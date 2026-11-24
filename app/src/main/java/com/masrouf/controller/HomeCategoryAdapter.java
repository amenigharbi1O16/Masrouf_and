package com.masrouf.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;

import java.util.ArrayList;
import java.util.List;

final class HomeCategoryAdapter extends RecyclerView.Adapter<HomeCategoryAdapter.Holder> {
    private final List<CategoryEntity> items=new ArrayList<>();
    void submit(List<CategoryEntity> categories) { items.clear(); if(categories!=null) items.addAll(categories); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type) { return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_home_category,parent,false)); }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position) {
        CategoryEntity category=items.get(position); holder.name.setText(category.name); holder.percent.setText(category.allocationPercent+"%"); holder.progress.setProgress(Math.min(100,category.allocationPercent));
    }
    @Override public int getItemCount() { return items.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name,percent; final ProgressBar progress;
        Holder(View view) { super(view); name=view.findViewById(R.id.home_category_name); percent=view.findViewById(R.id.home_category_percent); progress=view.findViewById(R.id.home_category_progress); }
    }
}
