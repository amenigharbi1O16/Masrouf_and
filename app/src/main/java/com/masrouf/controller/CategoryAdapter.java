package com.masrouf.controller;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.R;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reusable two-column category choice adapter for onboarding and category management. */
public final class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {
    public interface SelectionListener { void onSelectionChanged(); }
    private final List<CategoryOption> options;
    private final Set<String> selected = new HashSet<>();
    private final SelectionListener listener;

    public CategoryAdapter(List<CategoryOption> options, SelectionListener listener) {
        this.options=new ArrayList<>(options); this.listener=listener;
    }
    public List<CategoryOption> selectedOptions() {
        ArrayList<CategoryOption> result=new ArrayList<>();
        for(CategoryOption option:options) if(selected.contains(option.key)) result.add(option);
        return result;
    }
    public void setSelectedKeys(Set<String> keys) { selected.clear(); selected.addAll(keys); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType) {
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category,parent,false); return new Holder(view);
    }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position) {
        CategoryOption option=options.get(position); boolean isSelected=selected.contains(option.key);
        holder.name.setText(option.name); holder.status.setText(isSelected?R.string.selected_category:R.string.choose_category);
        try { holder.icon.setTextColor(Color.parseColor(option.colorHex)); } catch(IllegalArgumentException ignored) { }
        holder.check.setText(isSelected?"✓":"");
        holder.card.setStrokeColor(holder.itemView.getContext().getColor(isSelected?R.color.emerald:R.color.card_border));
        holder.card.setStrokeWidth((int)(holder.itemView.getResources().getDisplayMetrics().density*(isSelected?2:1)+.5f));
        holder.itemView.setOnClickListener(v->{ if(isSelected) selected.remove(option.key); else selected.add(option.key); notifyItemChanged(position); if(listener!=null) listener.onSelectionChanged(); });
    }
    @Override public int getItemCount() { return options.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card; final TextView icon,name,status,check;
        Holder(View item) { super(item); card=(MaterialCardView)item; LinearLayout root=item.findViewById(R.id.category_item_root); icon=item.findViewById(R.id.category_item_icon); name=item.findViewById(R.id.category_item_name); status=item.findViewById(R.id.category_item_status); check=item.findViewById(R.id.category_item_check); }
    }
}
