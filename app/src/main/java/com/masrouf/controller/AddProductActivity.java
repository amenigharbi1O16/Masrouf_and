package com.masrouf.controller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.masrouf1.BudgetNotificationManager;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.ProductFormData;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class AddProductActivity extends BaseFeatureActivity implements PhotoSourceBottomSheetDialog.Listener {
    private MasroufViewModel viewModel;
    private EditText name,brand,price,quantity,store;
    private Spinner categorySpinner,unitSpinner;
    private TextView unitPrice,date;
    private MaterialButton save;
    private List<CategoryEntity> categories=new ArrayList<>();
    private BudgetEntity budget;
    private String photoPath;
    private long occurredAt=System.currentTimeMillis();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showPage(R.string.add_purchase_product);viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        android.view.View form=getLayoutInflater().inflate(R.layout.activity_add_product,content,false); content.addView(form);
        name=findViewById(R.id.product_name); brand=findViewById(R.id.product_brand); price=findViewById(R.id.product_price);
        quantity=findViewById(R.id.product_quantity); store=findViewById(R.id.product_store); unitPrice=findViewById(R.id.product_unit_price);
        categorySpinner=findViewById(R.id.product_category); unitSpinner=findViewById(R.id.product_unit); date=findViewById(R.id.product_date); save=findViewById(R.id.save_product);
        unitSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.measure_units)));
        FormPriceTextWatcher watcher=new FormPriceTextWatcher(price,quantity,unitPrice); price.addTextChangedListener(watcher); quantity.addTextChangedListener(watcher); watcher.afterTextChanged(null);
        setDateLabel(); date.setOnClickListener(v->chooseDate());
        findViewById(R.id.product_photo).setOnClickListener(v->new PhotoSourceBottomSheetDialog().show(getSupportFragmentManager(),"product-photo"));
        getSupportFragmentManager().setFragmentResultListener("masrouf-photo-selected",this,(key,result)->photoPath=result.getString("photoPath"));
        save.setOnClickListener(v->savePurchase()); loadFormData();
    }
    private void loadFormData() {
        long userId=SessionStore.userId(this);
        viewModel.<ProductFormData>request(repository->repository.productForm(userId),state->{if(state.data==null)return;
            ArrayList<CategoryEntity> productCategories=new ArrayList<>();for(CategoryEntity item:state.data.categories)if("product".equalsIgnoreCase(item.type))productCategories.add(item);
            if(productCategories.isEmpty())productCategories.addAll(state.data.categories);
            categories=productCategories;budget=state.data.budget;ArrayList<String> labels=new ArrayList<>();for(CategoryEntity row:categories)labels.add(row.name);categorySpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));
        });
    }
    private void chooseDate() {
        Calendar c=Calendar.getInstance(); c.setTimeInMillis(occurredAt);
        new DatePickerDialog(this,(picker,year,month,day)->{ Calendar selected=Calendar.getInstance(); selected.set(year,month,day,12,0,0); selected.set(Calendar.MILLISECOND,0); occurredAt=selected.getTimeInMillis(); setDateLabel(); },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void setDateLabel() { date.setText(new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(occurredAt)); }
    private void savePurchase() {
        if(categories.isEmpty()) { Toast.makeText(this,R.string.select_category_first,Toast.LENGTH_SHORT).show(); return; }
        if(budget==null) { Toast.makeText(this,R.string.budget_required_for_entry,Toast.LENGTH_LONG).show(); return; }
        String productName=name.getText().toString().trim(); if(productName.isEmpty()) { name.setError(getString(R.string.product_name)); return; }
        long total,quantityMilli;
        try { total=MoneyInput.toMilli(price.getText().toString()); quantityMilli=MoneyInput.toMilli(quantity.getText().toString()); if(total<=0||quantityMilli<=0||quantityMilli>Integer.MAX_VALUE) throw new NumberFormatException(); }
        catch(NumberFormatException ex) { price.setError(getString(R.string.money_invalid)); return; }
        long perUnit=MoneyMath.pricePerUnitMilli(total,quantityMilli); if(perUnit<=0) { price.setError(getString(R.string.money_invalid)); return; }
        CategoryEntity category=categories.get(categorySpinner.getSelectedItemPosition()); long userId=SessionStore.userId(this);
        ProductEntity product=new ProductEntity(); product.categoryId=category.id; product.name=productName; product.brand=brand.getText().toString().trim(); product.unit=unitSpinner.getSelectedItem().toString(); product.photoPath=photoPath; product.currency="TND"; product.currentPriceMilli=perUnit; product.updatedAt=occurredAt;
        PurchaseEntity purchase=new PurchaseEntity(); purchase.userId=userId; purchase.occurredAt=occurredAt; purchase.totalMilli=total; purchase.storeName=store.getText().toString().trim(); purchase.receiptPath=photoPath;
        PurchaseItemEntity item=new PurchaseItemEntity(); item.description=productName; item.quantityMilli=(int)quantityMilli; item.unit=product.unit; item.unitPriceMilli=perUnit;
        save.setEnabled(false);
        long budgetId=budget.id;
        viewModel.<BudgetEntity>request(repository->{repository.savePurchase(product,purchase,item,userId,budgetId);return repository.budget(budgetId,userId);},state->{
            if(state.data==null){save.setEnabled(true);Toast.makeText(this,R.string.transaction_saved_error,Toast.LENGTH_SHORT).show();return;}
            BudgetEntity updated=state.data;BudgetNotificationManager.reportBudgetProgress(this,updated.spentMilli,updated.monthlyLimitMilli,(int)(updated.id&0x7fffffff));
            Intent next=new Intent(this,ProductDetailsActivity.class).putExtra("PRODUCT_ID",product.id).putExtra("PURCHASE_ID",purchase.id);startActivity(next);finish();
        });
    }
    @Override public void onPhotoSelected(String path) { photoPath=path; }
}
