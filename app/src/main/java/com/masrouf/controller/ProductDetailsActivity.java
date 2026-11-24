package com.masrouf.controller;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.DataState;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.ProductDetailsData;
import com.example.masrouf1.PriceChartView;
import com.example.masrouf1.R;

import java.io.File;

public final class ProductDetailsActivity extends BaseFeatureActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);showPage(R.string.product_details);LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);page.setPadding(dp(20),dp(16),dp(20),dp(16));
        ImageView photo=new ImageView(this);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);page.addView(photo,new LinearLayout.LayoutParams(-1,dp(160)));
        TextView title=new TextView(this);title.setTextColor(getColor(R.color.forest));title.setTextSize(22);title.setGravity(Gravity.CENTER_VERTICAL);page.addView(title,new LinearLayout.LayoutParams(-1,dp(58)));
        PriceChartView chart=new PriceChartView(this);LinearLayout.LayoutParams chartParams=new LinearLayout.LayoutParams(-1,dp(230));page.addView(chart,chartParams);content.addView(page,new android.widget.FrameLayout.LayoutParams(-1,-1));
        long id=getIntent().getLongExtra("PRODUCT_ID",0),userId=SessionStore.userId(this);MasroufViewModel vm=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        vm.<ProductDetailsData>request(repository->repository.productDetails(userId,id),result->{if(result.data==null){title.setText(R.string.history_none);return;}ProductDetailsData data=result.data;if(data.product!=null){title.setText(data.product.name+(data.product.brand==null||data.product.brand.isEmpty()?"":" · "+data.product.brand)+"\n"+CurrencyFormatter.formatMilli(data.product.currentPriceMilli)+" / "+data.product.unit);if(data.product.photoPath!=null&&new File(data.product.photoPath).exists())photo.setImageBitmap(BitmapFactory.decodeFile(data.product.photoPath));}else title.setText(R.string.history_none);chart.setHistory(data.history);});}
}
