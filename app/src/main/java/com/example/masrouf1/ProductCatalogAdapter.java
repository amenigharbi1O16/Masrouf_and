/*
 * ============================================================================
 * FICHIER : ProductCatalogAdapter.java
 * MISSION : Adaptateur RecyclerView affichant les produits personnels dans l'onglet Produits.
 * IMPLÉMENTE : RecyclerView.Adapter<Holder> sur item_product_catalog.xml + Listener de sélection.
 * EFFET À L'ÉCRAN : Dessine photo locale (ou ic_bag par repli), nom, marque/catégorie/unité, dernier prix/magasin
 *                   et indicateur coloré de variation de prix (+ > 10 % corail, baisse émeraude, stable gris).
 * COMMUNICATIONS : Piloté par ProductsFragment.applyFilter() ; clic -> ProductDetailsActivity.
 * ============================================================================
 */
package com.example.masrouf1;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.db.ProductCatalogRow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adaptateur RecyclerView du catalogue produits utilisé par ProductsFragment (layout item_product_catalog.xml).
 */
public final class ProductCatalogAdapter extends RecyclerView.Adapter<ProductCatalogAdapter.Holder> {
    /** Rappel invoqué lorsqu'un utilisateur touche une carte produit pour ouvrir sa fiche détaillée. */
    public interface Listener { void onProductSelected(ProductCatalogRow product); }
    private final Listener listener;
    // Liste filtrée et triée des lignes produits actuellement affichées.
    private final List<ProductCatalogRow> rows=new ArrayList<>();

    /** Initialise l'adaptateur avec son listener de clic produit. */
    public ProductCatalogAdapter(Listener listener){this.listener=listener;}

    /** Met à jour la liste visible après filtrage/tri puis notifie RecyclerView. */
    public void submit(List<ProductCatalogRow> values){rows.clear();if(values!=null)rows.addAll(values);notifyDataSetChanged();}

    /** Gonfle le layout item_product_catalog.xml pour créer chaque carte réutilisable. */
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_catalog,parent,false));}

    /** Lie nom, métadonnées, prix, tendance colorée, photo et écouteur de clic sur la carte position. */
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        ProductCatalogRow row=rows.get(position);android.content.Context context=holder.itemView.getContext();
        // Affiche le nom du produit.
        holder.name.setText(row.name==null?"":row.name);
        // Compose la ligne "marque · catégorie · unité" avec valeurs localisées de repli si vides.
        String maker=row.brand==null||row.brand.trim().isEmpty()?context.getString(R.string.no_brand_recorded):row.brand;
        String category=row.categoryName==null?context.getString(R.string.category_name):row.categoryName;
        holder.details.setText(context.getString(R.string.product_catalog_meta,maker,category,row.unit==null?"":row.unit));
        // Affiche le dernier prix unitaire formaté accompagné du nom de magasin.
        String store=row.storeName==null||row.storeName.trim().isEmpty()?context.getString(R.string.no_store_recorded):row.storeName;
        holder.price.setText(context.getString(R.string.product_catalog_price,CurrencyFormatter.formatMilli(row.currentPriceMilli),store));
        // Calcule et colorie la tendance entre l'avant-dernier prix et le prix courant sans flottants.
        if(row.priorPriceMilli==null){holder.change.setText(R.string.price_trend_new);holder.change.setTextColor(context.getColor(R.color.muted_green));}
        else {int change=MoneyMath.percentChangeRounded(row.currentPriceMilli,row.priorPriceMilli);if(MoneyMath.increasedMoreThanTenPercent(row.currentPriceMilli,row.priorPriceMilli)){holder.change.setText(context.getString(R.string.price_trend_up,change));holder.change.setTextColor(context.getColor(R.color.coral));}
            else if(row.currentPriceMilli<=row.priorPriceMilli){holder.change.setText(context.getString(R.string.price_trend_down,Math.max(0,-change)));holder.change.setTextColor(context.getColor(R.color.emerald));}
            else {holder.change.setText(context.getString(R.string.price_trend_stable,change));holder.change.setTextColor(context.getColor(R.color.muted_green));}}
        // Charge la miniature photo depuis cache/photos si disponible, sinon affiche l'icône sac vectorielle.
        if(row.photoPath!=null&&new File(row.photoPath).isFile()){holder.image.setImageBitmap(BitmapFactory.decodeFile(row.photoPath));}else holder.image.setImageResource(R.drawable.ic_bag);
        // Branche l'écouteur de clic ouvrant ProductDetailsActivity.
        holder.itemView.setOnClickListener(v->{if(listener!=null)listener.onProductSelected(row);});
    }

    @Override public int getItemCount(){return rows.size();}

    /** ViewHolder conservant les vues d'une carte item_product_catalog.xml. */
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name,details,price,change;final ImageView image;
        Holder(View root){super(root);name=root.findViewById(R.id.product_catalog_name);details=root.findViewById(R.id.product_catalog_brand_category);price=root.findViewById(R.id.product_catalog_price_store);change=root.findViewById(R.id.product_catalog_change);image=root.findViewById(R.id.product_catalog_image);}
    }
}
