package com.example.masrouf1;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.ProductCatalogRow;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.button.MaterialButton;
import com.masrouf.controller.AddProductActivity;
import com.masrouf.controller.ComparePriceActivity;
import com.masrouf.controller.ProductDetailsActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Room-backed searchable product catalogue with comparable price observations. */
public final class ProductsFragment extends DashboardSectionFragment {
    private final List<ProductCatalogRow> allProducts=new ArrayList<>();
    private ProductCatalogAdapter adapter;
    private ChipGroup filters;
    private EditText search;
    private TextView empty;

    @Override protected int pageTitle(){return R.string.nav_products;}
    @Override protected void createContent(LinearLayout content){
        View root=LayoutInflater.from(requireContext()).inflate(R.layout.fragment_products,content,false);content.addView(root);
        search=root.findViewById(R.id.product_search);filters=root.findViewById(R.id.product_category_filters);empty=root.findViewById(R.id.product_catalog_empty);
        RecyclerView list=root.findViewById(R.id.product_catalog_list);list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter=new ProductCatalogAdapter(product->startActivity(new Intent(requireContext(),ProductDetailsActivity.class).putExtra("PRODUCT_ID",product.id)));list.setAdapter(adapter);
        ((MaterialButton)root.findViewById(R.id.product_catalog_add)).setOnClickListener(v->startActivity(new Intent(requireContext(),AddProductActivity.class)));
        ((MaterialButton)root.findViewById(R.id.product_catalog_compare)).setOnClickListener(v->startActivity(new Intent(requireContext(),ComparePriceActivity.class)));
        search.addTextChangedListener(new TextWatcher(){@Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}@Override public void onTextChanged(CharSequence s,int start,int before,int count){applyFilter();}@Override public void afterTextChanged(Editable value){}});
        filters.setOnCheckedStateChangeListener((group,checkedIds)->applyFilter());
    }

    @Override public void onResume(){super.onResume();loadCatalog();}

    private void loadCatalog(){
        long userId=signedInUserId();
        viewModel.<CatalogSnapshot>request(repository->new CatalogSnapshot(repository.productCatalog(userId),repository.categories(userId)),state->{
            if(!isAdded()||getView()==null)return;
            if(state.failed||state.data==null){allProducts.clear();adapter.submit(allProducts);empty.setText(R.string.dashboard_read_error);empty.setVisibility(View.VISIBLE);return;}
            allProducts.clear();allProducts.addAll(state.data.products);buildFilters(state.data.categories);applyFilter();
        });
    }

    private void buildFilters(List<CategoryEntity> categories){
        filters.removeAllViews();Chip all=new Chip(requireContext());all.setId(View.generateViewId());all.setText(R.string.filter_all);all.setCheckable(true);all.setTag(0L);filters.addView(all);filters.check(all.getId());
        for(CategoryEntity category:categories){Chip chip=new Chip(requireContext());chip.setId(View.generateViewId());chip.setText(category.name);chip.setCheckable(true);chip.setTag(category.id);filters.addView(chip);}
    }

    private void applyFilter(){
        if(adapter==null||search==null)return;String query=search.getText()==null?"":search.getText().toString().trim().toLowerCase(Locale.ROOT);
        long categoryId=0;int selected=filters.getCheckedChipId();View chip=selected==View.NO_ID?null:filters.findViewById(selected);if(chip!=null&&chip.getTag() instanceof Long)categoryId=(Long)chip.getTag();
        ArrayList<ProductCatalogRow> visible=new ArrayList<>();
        for(ProductCatalogRow row:allProducts){boolean matchesCategory=categoryId==0||row.categoryId==categoryId;String name=row.name==null?"":row.name.toLowerCase(Locale.ROOT);String brand=row.brand==null?"":row.brand.toLowerCase(Locale.ROOT);boolean matchesText=query.isEmpty()||name.contains(query)||brand.contains(query);if(matchesCategory&&matchesText)visible.add(row);}
        adapter.submit(visible);empty.setText(allProducts.isEmpty()?R.string.products_empty:R.string.products_filter_empty);empty.setVisibility(visible.isEmpty()?View.VISIBLE:View.GONE);
    }

    private static final class CatalogSnapshot {
        final List<ProductCatalogRow> products;final List<CategoryEntity> categories;
        CatalogSnapshot(List<ProductCatalogRow> products,List<CategoryEntity> categories){this.products=products;this.categories=categories;}
    }
}
