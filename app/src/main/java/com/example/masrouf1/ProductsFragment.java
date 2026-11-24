package com.example.masrouf1;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.google.android.material.button.MaterialButton;
import com.masrouf.controller.AddProductActivity;
import com.masrouf.controller.ComparePriceActivity;
import com.masrouf.controller.ProductDetailsActivity;

import java.util.List;

/** Persistent products tab with Room-backed prices and unit-price history. */
public final class ProductsFragment extends DashboardSectionFragment {
    private PriceChartView chart;private LinearLayout rows;
    @Override protected int pageTitle(){return R.string.nav_products;}
    @Override protected void createContent(LinearLayout content){
        content.addView(text(getString(R.string.product_price_history)),new LinearLayout.LayoutParams(-1,dp(48)));
        chart=new PriceChartView(requireContext());content.addView(chart,new LinearLayout.LayoutParams(-1,dp(220)));
        rows=new LinearLayout(requireContext());rows.setOrientation(LinearLayout.VERTICAL);content.addView(rows,new LinearLayout.LayoutParams(-1,-2));
        MaterialButton add=new MaterialButton(requireContext());add.setText(R.string.add_purchase_product);add.setAllCaps(false);add.setOnClickListener(v->startActivity(new Intent(requireContext(),AddProductActivity.class)));content.addView(add,new LinearLayout.LayoutParams(-1,dp(50)));
        MaterialButton compare=new MaterialButton(requireContext());compare.setText(R.string.compare_price_title);compare.setAllCaps(false);compare.setOnClickListener(v->startActivity(new Intent(requireContext(),ComparePriceActivity.class)));content.addView(compare,new LinearLayout.LayoutParams(-1,dp(50)));
    }
    @Override protected void observeContent(){
        viewModel.history().observe(getViewLifecycleOwner(),state->{if(state.loading)return;chart.setHistory(state.data);});
        viewModel.products().observe(getViewLifecycleOwner(),state->{if(state.loading)return;rows.removeAllViews();List<ProductEntity> products=state.data;
            if(state.failed||products==null||products.isEmpty()){rows.addView(text(getString(R.string.history_none)));chart.setHistory(null);return;}
            for(ProductEntity item:products){TextView row=text(item.name+" · "+CurrencyFormatter.formatMilli(item.currentPriceMilli)+" / "+item.unit);row.setTextColor(requireContext().getColor(R.color.forest));row.setPadding(dp(8),0,dp(8),0);row.setOnClickListener(v->startActivity(new Intent(requireContext(),ProductDetailsActivity.class).putExtra("PRODUCT_ID",item.id)));rows.addView(row,new LinearLayout.LayoutParams(-1,dp(48)));}
            viewModel.loadHistory(signedInUserId(),products.get(0).id);
        });
        viewModel.loadProducts(signedInUserId());
    }
}
