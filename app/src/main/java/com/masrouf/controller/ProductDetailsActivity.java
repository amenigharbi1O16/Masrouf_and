package com.masrouf.controller;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.ProductDetailsData;
import com.example.masrouf1.PriceChartView;
import com.example.masrouf1.R;

import java.io.File;

public final class ProductDetailsActivity extends BaseFeatureActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);showPage(R.string.product_details);getLayoutInflater().inflate(R.layout.activity_product_details,content,true);
        ImageView photo=findViewById(R.id.product_detail_photo);TextView title=findViewById(R.id.product_detail_title),stats=findViewById(R.id.product_detail_stats),empty=findViewById(R.id.product_purchase_empty);
        PriceChartView chart=findViewById(R.id.product_price_chart);RecyclerView purchases=findViewById(R.id.product_purchase_history);purchases.setLayoutManager(new LinearLayoutManager(this));PurchaseHistoryAdapter adapter=new PurchaseHistoryAdapter();purchases.setAdapter(adapter);
        long id=getIntent().getLongExtra("PRODUCT_ID",0),userId=SessionStore.userId(this);MasroufViewModel vm=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        vm.<ProductDetailsData>request(repository->repository.productDetails(userId,id),result->{if(result.failed||result.data==null||result.data.product==null){title.setText(R.string.product_details_unavailable);stats.setText("");chart.setHistory(java.util.Collections.emptyList());adapter.submit(java.util.Collections.emptyList());empty.setVisibility(android.view.View.VISIBLE);return;}
            ProductDetailsData data=result.data;title.setText(data.product.name+(data.product.brand==null||data.product.brand.isEmpty()?"":" · "+data.product.brand)+"\n"+CurrencyFormatter.formatMilli(data.product.currentPriceMilli)+" / "+data.product.unit);
            if(data.product.photoPath!=null&&new File(data.product.photoPath).exists())photo.setImageBitmap(BitmapFactory.decodeFile(data.product.photoPath));
            if(data.minPriceMilli==null)stats.setText(R.string.price_history_empty);else stats.setText(getString(R.string.product_price_stats,CurrencyFormatter.formatMilli(data.minPriceMilli),CurrencyFormatter.formatMilli(data.averagePriceMilli),CurrencyFormatter.formatMilli(data.maxPriceMilli)));
            chart.setHistory(data.history);adapter.submit(data.purchases);empty.setVisibility(data.purchases.isEmpty()?android.view.View.VISIBLE:android.view.View.GONE);
        });}
}
