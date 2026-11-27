/*
 * ============================================================================
 * FICHIER : ComparePriceActivity.java
 * MISSION : Comparateur de prix avant achat permettant d'évaluer l'inflation produit et l'impact budgétaire.
 * IMPLÉMENTE : Hérite de BaseFeatureActivity et utilise activity_compare_price.xml + FormPriceTextWatcher.
 * EFFET À L'ÉCRAN : Calcule le prix unitaire à la frappe, affiche le diagnostic (+>10 % corail / économie émeraude /
 *                   hausse légère ambre), simule le reste de l'enveloppe catégorie et enregistre l'achat si validé.
 * COMMUNICATIONS : MasroufViewModel -> MasroufRepository.latestUnitPrice / envelope / savePurchase -> ConfirmationActivity.
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.masrouf1.BudgetNotificationManager;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.ProductFormData;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran « Comparer avant d'acheter » (BaseFeatureActivity + activity_compare_price.xml).
 */
public final class ComparePriceActivity extends BaseFeatureActivity {
    private MasroufViewModel viewModel;
    private EditText name,brand,price,quantity,store;
    private Spinner unit,category;
    private TextView perUnit,indicator,impact;
    private List<CategoryEntity> categories=new ArrayList<>(); private BudgetEntity budget;
    private long unitMilli,totalMilli,quantityMilli; private Long previous;

    /** Initialise les Spinners, écouteurs de calcul unitaire et de comparaison Room en direct. */
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state); showPage(R.string.compare_price_title);viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class); content.addView(getLayoutInflater().inflate(R.layout.activity_compare_price,content,false));
        name=findViewById(R.id.compare_name); brand=findViewById(R.id.compare_brand); price=findViewById(R.id.compare_price); quantity=findViewById(R.id.compare_quantity); store=findViewById(R.id.compare_store);
        unit=findViewById(R.id.compare_unit); category=findViewById(R.id.compare_category); perUnit=findViewById(R.id.compare_unit_price); indicator=findViewById(R.id.price_indicator); impact=findViewById(R.id.envelope_impact);
        // Remplit le Spinner des unités de mesure depuis arrays.xml (measure_units).
        ArrayList<String> unitChoices=new ArrayList<>();unitChoices.add(getString(R.string.select_unit));java.util.Collections.addAll(unitChoices,getResources().getStringArray(R.array.measure_units));
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, R.layout.item_masrouf_spinner, unitChoices);
        unitAdapter.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);
        unit.setAdapter(unitAdapter);
        unit.setPopupBackgroundResource(R.drawable.bg_dropdown_beige);
        // Initialise le Spinner des catégories avec l'invite en position 0.
        ArrayAdapter<String> catPlaceholder = new ArrayAdapter<>(this, R.layout.item_masrouf_spinner, new String[]{getString(R.string.select_product_category)});
        catPlaceholder.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);
        category.setAdapter(catPlaceholder);
        category.setPopupBackgroundResource(R.drawable.bg_dropdown_beige);
        // Branche FormPriceTextWatcher pour calculer instantanément prix / quantité.
        FormPriceTextWatcher watcher=new FormPriceTextWatcher(price,quantity,perUnit); price.addTextChangedListener(watcher); quantity.addTextChangedListener(watcher);
        // Chaque saisie sur nom/marque/prix/quantité ou changement de Spinner relance la comparaison en base.
        TextWatcher queryWatcher=new android.text.TextWatcher(){ public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){scheduleComparison();} public void afterTextChanged(android.text.Editable e){} };
        name.addTextChangedListener(queryWatcher); brand.addTextChangedListener(queryWatcher); price.addTextChangedListener(queryWatcher); quantity.addTextChangedListener(queryWatcher);
        unit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){ public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){scheduleComparison();} });
        category.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int pos,long id){loadEnvelopeImpact();}});
        findViewById(R.id.compare_save).setOnClickListener(v->savePurchase()); loadFormData();
    }

    /** Charge les catégories produit autorisées et le budget actif du compte courant. */
    private void loadFormData(){long userId=SessionStore.userId(this);long pref=SessionStore.selectedBudgetId(this,userId);viewModel.<ProductFormData>request(repository->repository.productForm(userId,pref),state->{if(state.data==null)return;
        java.util.HashMap<Long,String> keys=new java.util.HashMap<>();for(com.example.masrouf1.db.CategoryPresentationRow p:state.data.presentations)if(p.catalogKey!=null)keys.put(p.categoryId,p.catalogKey);
        ArrayList<CategoryEntity> products=new ArrayList<>();for(CategoryEntity c:state.data.categories)if("product".equalsIgnoreCase(c.type))products.add(c);if(products.isEmpty())products.addAll(state.data.categories);budget=state.data.budget;categories=products;ArrayList<String> labels=new ArrayList<>();labels.add(getString(R.string.select_product_category));for(CategoryEntity c:products){String k=keys.get(c.id);labels.add(k!=null?getString(com.example.masrouf1.db.SystemCategoryCatalog.titleResource(k)):c.name);}ArrayAdapter<String> catAdapter=new ArrayAdapter<>(this,R.layout.item_masrouf_spinner,labels);catAdapter.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);category.setAdapter(catAdapter);});}

    /** Interroge Room pour obtenir le dernier prix unitaire payé sur ce couple (nom, marque, unité). */
    private void scheduleComparison(){ String productName=name==null?"":name.getText().toString().trim(); String maker=brand==null?"":brand.getText().toString().trim(); if(productName.isEmpty()||unit==null||unit.getSelectedItem()==null||unit.getSelectedItemPosition()<=0)return; String selectedUnit=unit.getSelectedItem().toString(); long userId=SessionStore.userId(this);
        viewModel.<Long>request(repository->repository.latestUnitPrice(userId,productName,maker,selectedUnit),state->{previous=state.data;updatePriceIndicator();loadEnvelopeImpact();}); }

    /** Compare le prix unitaire saisi au précédent connu et colorie l'indicateur (corail / émeraude / ambre). */
    private void updatePriceIndicator(){ try { totalMilli=MoneyInput.toMilli(price.getText().toString()); quantityMilli=MoneyInput.toMilli(quantity.getText().toString()); if(totalMilli<=0||quantityMilli<=0)throw new NumberFormatException(); unitMilli=MoneyMath.pricePerUnitMilli(totalMilli,quantityMilli); } catch(RuntimeException e){unitMilli=0;return;}
        if(previous==null){ indicator.setText(R.string.no_previous_price); indicator.setTextColor(getColor(R.color.muted_green)); return; }
        int change=MoneyMath.percentChangeRounded(unitMilli,previous);
        if(MoneyMath.increasedMoreThanTenPercent(unitMilli,previous)){indicator.setText(getString(R.string.price_inflation,CurrencyFormatter.formatMilli(previous),CurrencyFormatter.formatMilli(unitMilli),change));indicator.setTextColor(getColor(R.color.coral));}
        else if(unitMilli<=previous){int saving=change==Integer.MIN_VALUE?Integer.MAX_VALUE:-change;indicator.setText(getString(R.string.price_saving,CurrencyFormatter.formatMilli(previous),CurrencyFormatter.formatMilli(unitMilli),saving));indicator.setTextColor(getColor(R.color.emerald));}
        else {indicator.setText(getString(R.string.price_change_small,change));indicator.setTextColor(getColor(R.color.amber));}
    }

    /** Simule le solde de l'enveloppe catégorie avant et après cet achat éventuel. */
    private void loadEnvelopeImpact(){if(categories.isEmpty()||budget==null||category.getSelectedItemPosition()<=0){impact.setText(null);return;}long categoryId=categories.get(category.getSelectedItemPosition()-1).id;long userId=SessionStore.userId(this),budgetId=budget.id;viewModel.<BudgetLineEntity>request(repository->repository.envelope(userId,budgetId,categoryId),state->{BudgetLineEntity line=state.data;long left=line==null?0:MoneyMath.subtract(line.limitMilli,line.spentMilli);long after=MoneyMath.subtract(left,totalMilli);impact.setText(getString(R.string.envelope_impact_value,CurrencyFormatter.formatMilli(left),CurrencyFormatter.formatMilli(after)));impact.setTextColor(getColor(after<0?R.color.coral:R.color.muted_green));});}

    /** Valide tous les champs puis enregistre l'achat transactionnellement et ouvre ConfirmationActivity. */
    private void savePurchase(){
        String itemName=name.getText().toString().trim(); if(itemName.isEmpty()){name.setError(getString(R.string.product_name_required));name.requestFocus();return;}
        if(categories.isEmpty()){Toast.makeText(this,R.string.no_categories_available,Toast.LENGTH_SHORT).show();return;}
        if(category.getSelectedItemPosition()<=0){Toast.makeText(this,R.string.select_category_first,Toast.LENGTH_SHORT).show();return;}
        if(unit.getSelectedItemPosition()<=0){Toast.makeText(this,R.string.select_unit_required,Toast.LENGTH_SHORT).show();return;}
        if(budget==null){Toast.makeText(this,R.string.budget_required_for_entry,Toast.LENGTH_SHORT).show();return;}
        try{totalMilli=MoneyInput.toMilli(price.getText().toString());quantityMilli=MoneyInput.toMilli(quantity.getText().toString());if(totalMilli<=0||quantityMilli<=0||quantityMilli>Integer.MAX_VALUE)throw new NumberFormatException();unitMilli=MoneyMath.pricePerUnitMilli(totalMilli,quantityMilli);if(unitMilli<=0)throw new NumberFormatException();}catch(RuntimeException e){price.setError(getString(R.string.money_invalid));return;}
        CategoryEntity selected=categories.get(category.getSelectedItemPosition()-1); ProductEntity product=new ProductEntity(); product.name=itemName;product.brand=brand.getText().toString().trim();product.unit=unit.getSelectedItem().toString();product.categoryId=selected.id;product.currentPriceMilli=unitMilli;product.currency="TND";product.updatedAt=System.currentTimeMillis();
        PurchaseEntity purchase=new PurchaseEntity();purchase.userId=SessionStore.userId(this);purchase.occurredAt=System.currentTimeMillis();purchase.totalMilli=totalMilli;purchase.storeName=store.getText().toString().trim();
        PurchaseItemEntity item=new PurchaseItemEntity();item.description=itemName;item.quantityMilli=(int)quantityMilli;item.unit=product.unit;item.unitPriceMilli=unitMilli;
        MaterialButton button=findViewById(R.id.compare_save);button.setEnabled(false);long ownerId=purchase.userId,budgetId=budget.id,amountToConfirm=totalMilli;viewModel.<BudgetEntity>request(repository->{repository.savePurchase(product,purchase,item,ownerId,budgetId);return repository.budget(budgetId,ownerId);},state->{BudgetEntity updated=state.data;if(updated==null){button.setEnabled(true);Toast.makeText(this,R.string.transaction_saved_error,Toast.LENGTH_SHORT).show();return;}startActivity(new Intent(this,ConfirmationActivity.class).putExtra("LABEL",itemName).putExtra("AMOUNT",amountToConfirm).putExtra("KIND","purchase"));finish();});
    }
}
