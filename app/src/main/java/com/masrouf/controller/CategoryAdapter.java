/*
 * ============================================================================
 * FICHIER : CategoryAdapter.java
 * MISSION : Adaptateur RecyclerView de sélection multi-catégories basé sur item_category.xml.
 * IMPLÉMENTE : RecyclerView.Adapter<Holder> avec protection de désélection sur les catégories essentielles/protégées.
 * EFFET À L'ÉCRAN : Anime légèrement la carte au clic (0.97 -> 1.0), colorie contour/fond en vert émeraude si sélectionnée.
 * COMMUNICATIONS : Composant conservé dans com.masrouf.controller (l'onboarding actif emploie CategorySelectionAdapter).
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.R;
import com.example.masrouf1.ui.CategoryIconResources;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adaptateur RecyclerView réutilisable (layout item_category.xml) avec animation tactile (scaleX/Y).
 */
public final class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.Holder> {
    /** Rappel émis lorsque la sélection d'une clé de catégorie bascule. */
    public interface SelectionListener { void onSelectionChanged(String categoryKey); }
    private final List<CategoryOption> options;
    private final Set<String> selected = new HashSet<>();
    private final SelectionListener listener;

    /** Initialise l'adaptateur avec une copie défensive des options disponibles. */
    public CategoryAdapter(List<CategoryOption> options, SelectionListener listener) {
        this.options=new ArrayList<>(options); this.listener=listener;
    }
    /** Retourne uniquement les CategoryOption actuellement cochées. */
    public List<CategoryOption> selectedOptions() {
        ArrayList<CategoryOption> result=new ArrayList<>();
        for(CategoryOption option:options) if(selected.contains(option.key)) result.add(option);
        return result;
    }
    /** Retourne une copie de toutes les options de la liste. */
    public List<CategoryOption> options(){return new ArrayList<>(options);}
    /** Vérifie qu'au moins une catégorie protégée (ex. "other") figure dans la sélection. */
    public boolean hasProtectedFallbackSelected(){for(CategoryOption option:selectedOptions())if(option.protectedCategory)return true;return false;}
    /** Applique un ensemble prédéfini de clés cochées puis rafraîchit toutes les cartes. */
    public void setSelectedKeys(Set<String> keys) { selected.clear(); selected.addAll(keys); notifyDataSetChanged(); }

    /** Gonfle le layout item_category.xml pour une carte catégorie. */
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType) {
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category,parent,false); return new Holder(view);
    }

    /** Lie icône colorée, état coché, accessibilité et animation tactile au clic. */
    @Override public void onBindViewHolder(@NonNull Holder holder,int position) {
        CategoryOption option=options.get(position); boolean isSelected=selected.contains(option.key);
        holder.name.setText(option.name); holder.status.setText(isSelected?R.string.selected_category:R.string.choose_category);
        holder.icon.setImageResource(CategoryIconResources.resolve(option.iconName,option.type));
        try { holder.icon.setImageTintList(ColorStateList.valueOf(Color.parseColor(option.colorHex))); } catch(IllegalArgumentException ignored) { }
        holder.check.setText(isSelected?"✓":"");
        holder.card.setStrokeColor(holder.itemView.getContext().getColor(isSelected?R.color.emerald:R.color.card_border));
        holder.card.setStrokeWidth((int)(holder.itemView.getResources().getDisplayMetrics().density*(isSelected?2:1)+.5f));
        holder.card.setCardBackgroundColor(holder.itemView.getContext().getColor(isSelected?R.color.success_surface:R.color.alabaster));
        holder.itemView.setSelected(isSelected);
        holder.itemView.setContentDescription(option.name+", "+holder.itemView.getContext().getString(isSelected?R.string.category_selected:R.string.category_choose));
        // Empêche de décocher les catégories protégées et anime un léger rebond au clic.
        holder.itemView.setOnClickListener(v->{ int current=holder.getBindingAdapterPosition();if(current==RecyclerView.NO_POSITION)return;CategoryOption clicked=options.get(current);boolean selectedNow=selected.contains(clicked.key);if(selectedNow&&clicked.protectedCategory)return;if(selectedNow)selected.remove(clicked.key);else selected.add(clicked.key);v.animate().cancel();v.setScaleX(.97f);v.setScaleY(.97f);v.animate().scaleX(1f).scaleY(1f).setDuration(150).start();notifyItemChanged(current);if(listener!=null)listener.onSelectionChanged(clicked.key); });
    }

    @Override public int getItemCount() { return options.size(); }

    /** ViewHolder mémorisant les sous-vues d'une carte item_category.xml. */
    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card; final ImageView icon; final TextView name,status,check;
        Holder(View item) { super(item); card=(MaterialCardView)item; icon=item.findViewById(R.id.category_item_icon); name=item.findViewById(R.id.category_item_name); status=item.findViewById(R.id.category_item_status); check=item.findViewById(R.id.category_item_check); }
    }
}
