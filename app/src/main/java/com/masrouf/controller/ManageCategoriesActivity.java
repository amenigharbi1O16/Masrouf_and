package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.ManageCategoriesData;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.example.masrouf1.ui.AlertBannerView;
import com.example.masrouf1.ui.CategoryIconResources;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Category Management screen (`Gérer` / `gerer-categories.tsx`):
 * - Displays all Product and Expense categories in two separated sections (including Enfants)
 * - Dynamic button states (`Sélectionner` <-> `✓ Sélectionné`)
 * - Allows adding new custom categories and safely deleting unused custom categories
 * - Saves changes transactionally per account without erasing purchases, expenses, or past budgets
 */
public final class ManageCategoriesActivity extends AppCompatActivity {
    private MasroufViewModel viewModel;
    private long userId;

    private TextView countBadge;
    private AlertBannerView warningBanner;
    private View loadingSpinner;
    private RecyclerView listView;
    private MaterialButton saveBtn, cancelBtn, addCustomBtn;

    private final List<SystemCategoryEntity> catalogCategories = new ArrayList<>();
    private final List<CategoryEntity> customCategories = new ArrayList<>();
    private final Set<Long> selectedCatalogIds = new HashSet<>();
    private final Set<Long> selectedCustomIds = new HashSet<>();
    private final Set<Long> knownCustomIds = new HashSet<>();
    private boolean initializedSelection = false;

    private ManageCategoriesAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        userId = SessionStore.userId(this);
        if (userId <= 0L) {
            finish();
            return;
        }
        setContentView(R.layout.activity_manage_categories);

        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this))
                .get(MasroufViewModel.class);

        countBadge = findViewById(R.id.manage_cats_count_badge);
        warningBanner = findViewById(R.id.manage_cats_warning);
        loadingSpinner = findViewById(R.id.manage_cats_loading);
        listView = findViewById(R.id.manage_cats_list);
        saveBtn = findViewById(R.id.manage_cats_save);
        cancelBtn = findViewById(R.id.manage_cats_cancel);
        addCustomBtn = findViewById(R.id.manage_cats_add_custom);

        findViewById(R.id.manage_cats_back).setOnClickListener(v -> finish());
        cancelBtn.setOnClickListener(v -> finish());

        listView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ManageCategoriesAdapter();
        listView.setAdapter(adapter);

        addCustomBtn.setOnClickListener(v ->
                new CustomCategoryDialogFragment().show(getSupportFragmentManager(), "add-category"));

        getSupportFragmentManager().setFragmentResultListener("masrouf-categories-updated", this,
                (key, bundle) -> loadCategories(true));

        saveBtn.setOnClickListener(v -> saveSelection());

        loadCategories(false);
    }

    private void loadCategories(boolean addedCustom) {
        loadingSpinner.setVisibility(View.VISIBLE);
        viewModel.<ManageCategoriesData>request(
                repository -> repository.loadManageCategories(userId),
                state -> {
                    loadingSpinner.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed() || state.data == null) return;

                    ManageCategoriesData data = state.data;
                    catalogCategories.clear();
                    if (data.catalogCategories != null) {
                        catalogCategories.addAll(data.catalogCategories);
                    }
                    customCategories.clear();
                    if (data.customCategories != null) {
                        customCategories.addAll(data.customCategories);
                    }

                    if (!initializedSelection) {
                        selectedCatalogIds.clear();
                        if (data.selectedCatalogIds != null) {
                            selectedCatalogIds.addAll(data.selectedCatalogIds);
                        }
                        selectedCustomIds.clear();
                        if (data.selectedCustomCategoryIds != null) {
                            selectedCustomIds.addAll(data.selectedCustomCategoryIds);
                        }
                        knownCustomIds.clear();
                        for (CategoryEntity c : customCategories) {
                            knownCustomIds.add(c.id);
                        }
                        initializedSelection = true;
                    } else if (addedCustom) {
                        // Auto-select newly added custom categories while keeping current unsaved toggles
                        for (CategoryEntity c : customCategories) {
                            if (!knownCustomIds.contains(c.id)) {
                                selectedCustomIds.add(c.id);
                                knownCustomIds.add(c.id);
                            }
                        }
                        // Prune any custom category IDs that were deleted
                        HashSet<Long> validCustom = new HashSet<>();
                        for (CategoryEntity c : customCategories) validCustom.add(c.id);
                        selectedCustomIds.retainAll(validCustom);
                    }

                    rebuildRows();
                    updateCounterAndWarning();
                });
    }

    private void rebuildRows() {
        List<RowItem> rows = new ArrayList<>();
        List<RowItem> productRows = new ArrayList<>();
        List<RowItem> expenseRows = new ArrayList<>();

        for (SystemCategoryEntity sys : catalogCategories) {
            RowItem item = RowItem.catalog(sys);
            if ("product".equalsIgnoreCase(sys.type)) {
                productRows.add(item);
            } else {
                expenseRows.add(item);
            }
        }
        for (CategoryEntity custom : customCategories) {
            RowItem item = RowItem.custom(custom);
            if ("product".equalsIgnoreCase(custom.type)) {
                productRows.add(item);
            } else {
                expenseRows.add(item);
            }
        }

        if (!productRows.isEmpty()) {
            rows.add(RowItem.header(getString(R.string.onboarding_section_products)));
            rows.addAll(productRows);
        }
        if (!expenseRows.isEmpty()) {
            rows.add(RowItem.header(getString(R.string.onboarding_section_expenses)));
            rows.addAll(expenseRows);
        }
        adapter.submit(rows);
    }

    private void updateCounterAndWarning() {
        int count = selectedCatalogIds.size() + selectedCustomIds.size();
        countBadge.setText(String.valueOf(count));
        saveBtn.setText(getString(R.string.save_categories_count, count));
        if (count == 0) {
            warningBanner.setVisibility(View.VISIBLE);
            warningBanner.setAlert(AlertBannerView.Tone.WARNING,
                    getString(R.string.verifier_ce_budget),
                    getString(R.string.select_category_first));
            saveBtn.setEnabled(false);
        } else {
            warningBanner.setVisibility(View.GONE);
            warningBanner.setAlert(AlertBannerView.Tone.NONE, "", "");
            saveBtn.setEnabled(true);
        }
    }

    private void confirmDeleteCustom(CategoryEntity custom) {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.delete_custom_cat_confirm, custom.name))
                .setNegativeButton(R.string.cancel_btn, null)
                .setPositiveButton(R.string.delete_action, (d, w) ->
                        viewModel.<Boolean>request(
                                repository -> repository.deleteCustomCategory(userId, custom.id),
                                state -> {
                                    if (isFinishing() || isDestroyed()) return;
                                    if (Boolean.TRUE.equals(state.data)) {
                                        selectedCustomIds.remove(custom.id);
                                        knownCustomIds.remove(custom.id);
                                        loadCategories(false);
                                    } else {
                                        warningBanner.setVisibility(View.VISIBLE);
                                        warningBanner.setAlert(AlertBannerView.Tone.WARNING,
                                                getString(R.string.verifier_ce_budget),
                                                getString(R.string.delete_custom_cat_used_error));
                                    }
                                }))
                .show();
    }

    private void saveSelection() {
        int count = selectedCatalogIds.size() + selectedCustomIds.size();
        if (count == 0) {
            updateCounterAndWarning();
            return;
        }
        saveBtn.setEnabled(false);
        loadingSpinner.setVisibility(View.VISIBLE);

        ArrayList<Long> catalogList = new ArrayList<>(selectedCatalogIds);
        ArrayList<Long> customList = new ArrayList<>(selectedCustomIds);
        viewModel.<Boolean>request(
                repository -> repository.saveManagedCategories(userId, catalogList, customList),
                state -> {
                    loadingSpinner.setVisibility(View.GONE);
                    if (isFinishing() || isDestroyed()) return;
                    if (Boolean.TRUE.equals(state.data)) {
                        String successMsg = getString(R.string.categories_updated_success);
                        Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK, new Intent().putExtra("EXTRA_MESSAGE", successMsg));
                        finish();
                    } else {
                        saveBtn.setEnabled(true);
                        Toast.makeText(this, R.string.transaction_saved_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private static final class RowItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_ITEM = 1;

        final int viewType;
        final String headerTitle;
        final SystemCategoryEntity catalog;
        final CategoryEntity custom;

        private RowItem(int viewType, String headerTitle, SystemCategoryEntity catalog, CategoryEntity custom) {
            this.viewType = viewType;
            this.headerTitle = headerTitle;
            this.catalog = catalog;
            this.custom = custom;
        }

        static RowItem header(String title) {
            return new RowItem(TYPE_HEADER, title, null, null);
        }

        static RowItem catalog(SystemCategoryEntity sys) {
            return new RowItem(TYPE_ITEM, null, sys, null);
        }

        static RowItem custom(CategoryEntity cat) {
            return new RowItem(TYPE_ITEM, null, null, cat);
        }
    }

    private final class ManageCategoriesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final List<RowItem> items = new ArrayList<>();

        void submit(List<RowItem> rows) {
            items.clear();
            items.addAll(rows);
            notifyDataSetChanged();
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).viewType;
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == RowItem.TYPE_HEADER) {
                TextView tv = new TextView(parent.getContext());
                int padH = dp(4);
                int padTop = dp(14);
                int padBottom = dp(6);
                tv.setPadding(padH, padTop, padH, padBottom);
                tv.setTextColor(parent.getContext().getColor(R.color.forest));
                tv.setTextSize(15f);
                tv.setTypeface(null, android.graphics.Typeface.BOLD);
                return new HeaderVH(tv);
            }
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_category, parent, false);
            return new ItemVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            RowItem row = items.get(position);
            if (holder instanceof HeaderVH) {
                ((HeaderVH) holder).title.setText(row.headerTitle);
                return;
            }
            ItemVH vh = (ItemVH) holder;
            if (row.catalog != null) {
                SystemCategoryEntity sys = row.catalog;
                vh.name.setText(SystemCategoryCatalog.titleResource(sys.key));
                vh.icon.setImageResource(CategoryIconResources.resolve(sys.iconName, sys.type));
                vh.deleteBtn.setVisibility(View.GONE);
                vh.deleteBtn.setOnClickListener(null);

                boolean selected = selectedCatalogIds.contains(sys.id);
                applyToggleStyle(vh, selected);
                View.OnClickListener toggleListener = v -> {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    if (selectedCatalogIds.contains(sys.id)) {
                        selectedCatalogIds.remove(sys.id);
                    } else {
                        selectedCatalogIds.add(sys.id);
                    }
                    notifyItemChanged(pos);
                    updateCounterAndWarning();
                };
                vh.itemView.setOnClickListener(toggleListener);
                vh.toggle.setOnClickListener(toggleListener);
            } else if (row.custom != null) {
                CategoryEntity cat = row.custom;
                vh.name.setText(cat.name);
                vh.icon.setImageResource(CategoryIconResources.resolve(null, cat.type));
                vh.deleteBtn.setVisibility(View.VISIBLE);
                vh.deleteBtn.setOnClickListener(v -> confirmDeleteCustom(cat));

                boolean selected = selectedCustomIds.contains(cat.id);
                applyToggleStyle(vh, selected);
                View.OnClickListener toggleListener = v -> {
                    int pos = vh.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    if (selectedCustomIds.contains(cat.id)) {
                        selectedCustomIds.remove(cat.id);
                    } else {
                        selectedCustomIds.add(cat.id);
                    }
                    notifyItemChanged(pos);
                    updateCounterAndWarning();
                };
                vh.itemView.setOnClickListener(toggleListener);
                vh.toggle.setOnClickListener(toggleListener);
            }
        }

        private void applyToggleStyle(ItemVH vh, boolean isSelected) {
            if (vh.rowContainer != null) {
                vh.rowContainer.setBackgroundColor(getColor(
                        isSelected ? R.color.mint_light : android.R.color.transparent));
            }
            if (isSelected) {
                vh.toggle.setText(R.string.selected_btn);
                vh.toggle.setBackgroundResource(R.drawable.bg_lang_selected);
                vh.toggle.setTextColor(getColor(R.color.alabaster));
            } else {
                vh.toggle.setText(R.string.select_btn);
                vh.toggle.setBackgroundResource(R.drawable.bg_btn_select);
                vh.toggle.setTextColor(getColor(R.color.alabaster));
            }
        }
    }

    private static final class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title;
        HeaderVH(TextView title) {
            super(title);
            this.title = title;
        }
    }

    private static final class ItemVH extends RecyclerView.ViewHolder {
        final LinearLayout rowContainer;
        final ImageView icon;
        final TextView name;
        final ImageView deleteBtn;
        final TextView toggle;

        ItemVH(View v) {
            super(v);
            rowContainer = v.findViewById(R.id.item_category_row);
            icon = v.findViewById(R.id.item_category_icon);
            name = v.findViewById(R.id.item_category_name);
            deleteBtn = v.findViewById(R.id.item_category_delete);
            toggle = v.findViewById(R.id.item_category_toggle);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
