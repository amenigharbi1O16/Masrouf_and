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

public final class ProductCatalogAdapter extends RecyclerView.Adapter<ProductCatalogAdapter.Holder> {
    public interface Listener { void onProductSelected(ProductCatalogRow product); }
    private final Listener listener;
    private final List<ProductCatalogRow> rows=new ArrayList<>();
    public ProductCatalogAdapter(Listener listener){this.listener=listener;}
    public void submit(List<ProductCatalogRow> values){rows.clear();if(values!=null)rows.addAll(values);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_catalog,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        ProductCatalogRow row=rows.get(position);android.content.Context context=holder.itemView.getContext();
        holder.name.setText(row.name==null?"":row.name);
        String maker=row.brand==null||row.brand.trim().isEmpty()?context.getString(R.string.no_brand_recorded):row.brand;
        String category=row.categoryName==null?context.getString(R.string.category_name):row.categoryName;
        holder.details.setText(context.getString(R.string.product_catalog_meta,maker,category,row.unit==null?"":row.unit));
        String store=row.storeName==null||row.storeName.trim().isEmpty()?context.getString(R.string.no_store_recorded):row.storeName;
        holder.price.setText(context.getString(R.string.product_catalog_price,CurrencyFormatter.formatMilli(row.currentPriceMilli),store));
        if(row.priorPriceMilli==null){holder.change.setText(R.string.price_trend_new);holder.change.setTextColor(context.getColor(R.color.muted_green));}
        else {int change=MoneyMath.percentChangeRounded(row.currentPriceMilli,row.priorPriceMilli);if(MoneyMath.increasedMoreThanTenPercent(row.currentPriceMilli,row.priorPriceMilli)){holder.change.setText(context.getString(R.string.price_trend_up,change));holder.change.setTextColor(context.getColor(R.color.coral));}
            else if(row.currentPriceMilli<=row.priorPriceMilli){holder.change.setText(context.getString(R.string.price_trend_down,Math.max(0,-change)));holder.change.setTextColor(context.getColor(R.color.emerald));}
            else {holder.change.setText(context.getString(R.string.price_trend_stable,change));holder.change.setTextColor(context.getColor(R.color.muted_green));}}
        if(row.photoPath!=null&&new File(row.photoPath).isFile()){holder.image.setImageBitmap(BitmapFactory.decodeFile(row.photoPath));}else holder.image.setImageResource(R.drawable.ic_bag);
        holder.itemView.setOnClickListener(v->{if(listener!=null)listener.onProductSelected(row);});
    }
    @Override public int getItemCount(){return rows.size();}
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name,details,price,change;final ImageView image;
        Holder(View root){super(root);name=root.findViewById(R.id.product_catalog_name);details=root.findViewById(R.id.product_catalog_brand_category);price=root.findViewById(R.id.product_catalog_price_store);change=root.findViewById(R.id.product_catalog_change);image=root.findViewById(R.id.product_catalog_image);}
    }
}
