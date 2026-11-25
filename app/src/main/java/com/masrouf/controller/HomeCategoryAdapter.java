package com.masrouf.controller;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.HomeCategoryProgress;
import com.example.masrouf1.R;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.example.masrouf1.ui.CategoryProgressCardView;

import java.util.ArrayList;
import java.util.List;

/** Displays actual Room envelopes and spending; recommended catalog percentages are never used. */
final class HomeCategoryAdapter extends RecyclerView.Adapter<HomeCategoryAdapter.Holder> {
    interface OnCategoryClickListener { void onCategoryClick(HomeCategoryProgress row); }
    private final List<HomeCategoryProgress> items=new ArrayList<>();
    private final OnCategoryClickListener listener;
    HomeCategoryAdapter() { this(null); }
    HomeCategoryAdapter(OnCategoryClickListener listener) { this.listener = listener; }
    void submitProgress(List<HomeCategoryProgress> rows){items.clear();if(rows!=null)items.addAll(rows);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
        CategoryProgressCardView card=new CategoryProgressCardView(parent.getContext());
        RecyclerView.LayoutParams params=new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin=(int)(parent.getResources().getDisplayMetrics().density*8+.5f);card.setLayoutParams(params);return new Holder(card);
    }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        HomeCategoryProgress row=items.get(position);
        String name=row.catalogKey==null?row.name:holder.itemView.getContext().getString(SystemCategoryCatalog.titleResource(row.catalogKey));
        holder.card.setProgressData(name,row.iconName,row.type,row.colorHex,row.allocatedMilli,row.spentMilli,row.plannedPercent,row.plannedPercentBps,row.consumedPercent);
        if(listener!=null) holder.card.setOnClickListener(v->listener.onCategoryClick(row));
    }
    @Override public int getItemCount(){return items.size();}
    static final class Holder extends RecyclerView.ViewHolder {
        final CategoryProgressCardView card;
        Holder(View view){super(view);card=(CategoryProgressCardView)view;}
    }
}
