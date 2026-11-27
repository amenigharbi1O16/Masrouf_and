/*
 * ============================================================================
 * FICHIER : AddProductActivity.java
 * MISSION : Écran formulaire d'enregistrement d'un achat produit : nom, marque,
 *           photo facultative (caméra ou galerie via PhotoSourceBottomSheetDialog),
 *           catégorie produit, prix total, quantité, unité de mesure, calcul en
 *           temps réel du prix unitaire, magasin et date (DatePickerDialog).
 * IMPLÉMENTE : Sous-classe de BaseFeatureActivity gonflant activity_add_product.xml,
 *              avec FormPriceTextWatcher, VoiceFieldHelper sur 4 champs, calcul
 *              exact en millimes (MoneyMath.pricePerUnitMilli) et enregistrement
 *              atomique multi-tables dans Room.
 * EFFET À L'ÉCRAN : Formulaire complet avec aperçu instantané du prix unitaire,
 *                   bannière d'absence de budget, boutons micro par champ et
 *                   sélecteur d'unité / catégorie.
 * COMMUNICATIONS :
 *   ← HomeFragment, ProductsFragment (bouton « Ajouter un achat » / voix)
 *   → PhotoSourceBottomSheetDialog (caméra/galerie via FragmentResult)
 *   → FormPriceTextWatcher (calcul temps réel du prix unitaire)
 *   → VoiceFieldHelper (dictée nom, marque, prix, magasin)
 *   → MasroufViewModel / MasroufRepository (productForm, savePurchase)
 *   → ConfirmationActivity (confirmation après écriture SQLite)
 * ============================================================================
 */
package com.masrouf.controller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.ProductFormData;
import com.example.masrouf1.R;
import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.example.masrouf1.ui.AlertBannerView;
import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Formulaire d'ajout d'un achat produit : saisit nom, marque, photo optionnelle (PhotoSourceBottomSheetDialog),
 * catégorie produit, prix total, quantité et unité, calcule en direct le prix unitaire (FormPriceTextWatcher)
 * et met à jour atomiquement products, purchases, purchase_items, price_history, budget_lines et budgets.
 */
public final class AddProductActivity extends BaseFeatureActivity implements PhotoSourceBottomSheetDialog.Listener {
    private static final String STATE_CATEGORY_ID = "ap_cat_id";
    private static final String STATE_DATE_MILLIS = "ap_date";

    private MasroufViewModel viewModel;
    private EditText name, brand, price, quantity, store;
    private Spinner categorySpinner, unitSpinner;
    private TextView unitPrice, date;
    private MaterialButton save, defineBudgetBtn;
    private AlertBannerView budgetBanner, validationBanner;

    private List<CategoryEntity> categories = new ArrayList<>();
    private final Map<Long, String> catalogKeyByCategoryId = new HashMap<>();
    private BudgetEntity budget;
    private String photoPath;
    private long occurredAt;
    private long selectedCategoryId = -1L;
    private boolean bindingCategories = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        showPage(R.string.add_purchase_product);
        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this))
                .get(MasroufViewModel.class);
        View form = getLayoutInflater().inflate(R.layout.activity_add_product, content, false);
        content.addView(form);

        name = findViewById(R.id.product_name);
        brand = findViewById(R.id.product_brand);
        price = findViewById(R.id.product_price);
        quantity = findViewById(R.id.product_quantity);
        store = findViewById(R.id.product_store);
        unitPrice = findViewById(R.id.product_unit_price);
        categorySpinner = findViewById(R.id.product_category);
        unitSpinner = findViewById(R.id.product_unit);
        date = findViewById(R.id.product_date);
        save = findViewById(R.id.save_product);
        budgetBanner = findViewById(R.id.product_budget_banner);
        defineBudgetBtn = findViewById(R.id.product_define_budget_btn);
        validationBanner = findViewById(R.id.product_validation_banner);

        if (state != null) {
            selectedCategoryId = state.getLong(STATE_CATEGORY_ID, -1L);
            occurredAt = state.getLong(STATE_DATE_MILLIS, 0L);
        }
        if (occurredAt <= 0L) {
            occurredAt = System.currentTimeMillis();
        }
        updateDateDisplay();

        ArrayList<String> unitChoices = new ArrayList<>();
        unitChoices.add(getString(R.string.select_unit));
        java.util.Collections.addAll(unitChoices, getResources().getStringArray(R.array.measure_units));
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, R.layout.item_masrouf_spinner, unitChoices);
        unitAdapter.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);
        unitSpinner.setAdapter(unitAdapter);
        unitSpinner.setPopupBackgroundResource(R.drawable.bg_dropdown_beige);

        ArrayAdapter<String> initCatAdapter = new ArrayAdapter<>(this, R.layout.item_masrouf_spinner,
                new String[]{getString(R.string.select_product_category)});
        initCatAdapter.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);
        categorySpinner.setAdapter(initCatAdapter);
        categorySpinner.setPopupBackgroundResource(R.drawable.bg_dropdown_beige);

        voiceFieldHelper = new VoiceFieldHelper(this, this, validationBanner);
        voiceFieldHelper.bindField(findViewById(R.id.product_name_mic), name);
        voiceFieldHelper.bindField(findViewById(R.id.product_brand_mic), brand);
        voiceFieldHelper.bindField(findViewById(R.id.product_price_mic), price);
        voiceFieldHelper.bindField(findViewById(R.id.product_store_mic), store);

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (bindingCategories) return;
                if (position > 0 && position - 1 < categories.size()) {
                    selectedCategoryId = categories.get(position - 1).id;
                    clearValidationBanner();
                } else {
                    selectedCategoryId = -1L;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) clearValidationBanner();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        FormPriceTextWatcher watcher = new FormPriceTextWatcher(price, quantity, unitPrice);
        price.addTextChangedListener(watcher);
        quantity.addTextChangedListener(watcher);
        watcher.afterTextChanged(null);

        date.setOnClickListener(v -> chooseDate());
        defineBudgetBtn.setOnClickListener(v -> startActivity(new Intent(this, BudgetPeriodActivity.class)));
        findViewById(R.id.product_photo).setOnClickListener(
                v -> new PhotoSourceBottomSheetDialog().show(getSupportFragmentManager(), "product-photo"));
        getSupportFragmentManager().setFragmentResultListener("masrouf-photo-selected", this,
                (key, result) -> photoPath = result.getString("photoPath"));

        if (getIntent().hasExtra("VOICE_LABEL")) name.setText(getIntent().getStringExtra("VOICE_LABEL"));
        if (getIntent().hasExtra("VOICE_AMOUNT")) price.setText(getIntent().getStringExtra("VOICE_AMOUNT"));
        save.setOnClickListener(v -> savePurchase());
        loadFormData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadFormData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(STATE_CATEGORY_ID, selectedCategoryId);
        outState.putLong(STATE_DATE_MILLIS, occurredAt);
    }

    private void loadFormData() {
        long userId = SessionStore.userId(this);
        long preferredId = SessionStore.selectedBudgetId(this, userId);
        viewModel.<ProductFormData>request(repository -> repository.productForm(userId, preferredId), state -> {
            if (isFinishing() || isDestroyed() || state.data == null) return;
            catalogKeyByCategoryId.clear();
            for (CategoryPresentationRow row : state.data.presentations) {
                if (row.catalogKey != null) catalogKeyByCategoryId.put(row.categoryId, row.catalogKey);
            }

            ArrayList<CategoryEntity> productCategories = new ArrayList<>();
            for (CategoryEntity item : state.data.categories) {
                if ("product".equalsIgnoreCase(item.type)) productCategories.add(item);
            }
            if (productCategories.isEmpty()) productCategories.addAll(state.data.categories);

            categories = productCategories;
            budget = state.data.budget;
            updateNoBudgetBanner();

            ArrayList<String> labels = new ArrayList<>();
            labels.add(getString(R.string.select_product_category));
            for (CategoryEntity row : categories) {
                labels.add(localizedCategoryName(row));
            }

            bindingCategories = true;
            ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, R.layout.item_masrouf_spinner, labels);
            catAdapter.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);
            categorySpinner.setAdapter(catAdapter);
            categorySpinner.setPopupBackgroundResource(R.drawable.bg_dropdown_beige);
            int restorePos = 0;
            if (selectedCategoryId > 0L) {
                for (int i = 0; i < categories.size(); i++) {
                    if (categories.get(i).id == selectedCategoryId) {
                        restorePos = i + 1;
                        break;
                    }
                }
            } else if (categories.size() == 1) {
                restorePos = 1;
                selectedCategoryId = categories.get(0).id;
            }
            categorySpinner.setSelection(restorePos, false);
            bindingCategories = false;
        });
    }

    private String localizedCategoryName(CategoryEntity category) {
        String key = catalogKeyByCategoryId.get(category.id);
        return key != null ? getString(SystemCategoryCatalog.titleResource(key)) : category.name;
    }

    private void updateNoBudgetBanner() {
        boolean missingBudget = budget == null;
        budgetBanner.setVisibility(missingBudget ? View.VISIBLE : View.GONE);
        defineBudgetBtn.setVisibility(missingBudget ? View.VISIBLE : View.GONE);
        if (missingBudget) {
            budgetBanner.setAlert(AlertBannerView.Tone.WARNING,
                    getString(R.string.form_no_budget_title),
                    getString(R.string.form_no_budget_detail));
        }
    }

    private void chooseDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(occurredAt > 0L ? occurredAt : System.currentTimeMillis());
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth, 12, 0, 0);
            chosen.set(Calendar.MILLISECOND, 0);
            occurredAt = chosen.getTimeInMillis();
            date.setError(null);
            clearValidationBanner();
            updateDateDisplay();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateDateDisplay() {
        date.setText(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(occurredAt));
    }

    private void savePurchase() {
        clearValidationBanner();
        String itemName = name.getText() == null ? "" : name.getText().toString().trim();
        if (itemName.isEmpty()) {
            name.setError(getString(R.string.product_name_required));
            name.requestFocus();
            showValidationError(R.string.product_name_required);
            return;
        }

        if (categories.isEmpty()) {
            showValidationError(R.string.no_categories_available);
            return;
        }

        CategoryEntity category = resolveSelectedCategory();
        if (category == null) {
            showValidationError(R.string.select_category_first);
            categorySpinner.requestFocus();
            return;
        }

        long totalMilli, quantityMilli, unitMilli;
        try {
            totalMilli = MoneyInput.toMilli(price.getText() == null ? "" : price.getText().toString());
            if (totalMilli <= 0L) throw new NumberFormatException();
        } catch (RuntimeException ex) {
            price.setError(getString(R.string.money_invalid));
            price.requestFocus();
            showValidationError(R.string.money_invalid);
            return;
        }

        try {
            quantityMilli = MoneyInput.toMilli(quantity.getText() == null ? "" : quantity.getText().toString());
            if (quantityMilli <= 0L || quantityMilli > Integer.MAX_VALUE) throw new NumberFormatException();
            unitMilli = MoneyMath.pricePerUnitMilli(totalMilli, quantityMilli);
            if (unitMilli <= 0L) throw new NumberFormatException();
        } catch (RuntimeException ex) {
            quantity.setError(getString(R.string.quantity_invalid));
            quantity.requestFocus();
            showValidationError(R.string.quantity_invalid);
            return;
        }

        if (unitSpinner.getSelectedItemPosition() <= 0 || unitSpinner.getSelectedItem() == null) {
            showValidationError(R.string.select_unit_required);
            unitSpinner.requestFocus();
            return;
        }

        if (occurredAt <= 0L) {
            date.setError(getString(R.string.date_required));
            showValidationError(R.string.date_required);
            return;
        }

        ProductEntity product = new ProductEntity();
        product.name = itemName;
        product.brand = brand.getText() == null ? "" : brand.getText().toString().trim();
        product.unit = unitSpinner.getSelectedItem().toString();
        product.categoryId = category.id;
        product.photoPath = photoPath;
        product.currentPriceMilli = unitMilli;
        product.currency = "TND";
        product.updatedAt = occurredAt;

        long ownerId = SessionStore.userId(this);
        long preferredId = SessionStore.selectedBudgetId(this, ownerId);
        PurchaseEntity purchase = new PurchaseEntity();
        purchase.userId = ownerId;
        purchase.occurredAt = occurredAt;
        purchase.totalMilli = totalMilli;
        purchase.storeName = store.getText() == null ? "" : store.getText().toString().trim();

        PurchaseItemEntity item = new PurchaseItemEntity();
        item.description = itemName;
        item.quantityMilli = (int) quantityMilli;
        item.unit = product.unit;
        item.unitPriceMilli = unitMilli;

        save.setEnabled(false);
        viewModel.<Long>request(repository -> {
            BudgetEntity targetBudget = repository.resolveBudgetForDateOrPreferred(ownerId, occurredAt, preferredId);
            if (targetBudget == null) return -3L;
            budget = targetBudget;
            long id = repository.savePurchase(product, purchase, item, ownerId, targetBudget.id);
            return id > 0L ? targetBudget.id : -1L;
        }, state -> {
            if (isFinishing() || isDestroyed()) return;
            Long result = state.data;
            if (result != null && result == -3L) {
                save.setEnabled(true);
                budget = null;
                updateNoBudgetBanner();
                Toast.makeText(this, R.string.budget_required_for_entry, Toast.LENGTH_LONG).show();
                return;
            }
            if (state.failed || result == null || result <= 0L) {
                save.setEnabled(true);
                Toast.makeText(this, R.string.transaction_saved_error, Toast.LENGTH_SHORT).show();
                return;
            }
            startActivity(new Intent(this, ConfirmationActivity.class)
                    .putExtra("LABEL", itemName)
                    .putExtra("AMOUNT", totalMilli)
                    .putExtra("KIND", "purchase"));
            finish();
        });
    }

    private CategoryEntity resolveSelectedCategory() {
        int pos = categorySpinner.getSelectedItemPosition();
        if (pos > 0 && pos - 1 < categories.size()) {
            return categories.get(pos - 1);
        }
        if (selectedCategoryId > 0L) {
            for (CategoryEntity c : categories) {
                if (c.id == selectedCategoryId) return c;
            }
        }
        return null;
    }

    private void showValidationError(int msgRes) {
        String text = getString(msgRes);
        validationBanner.setVisibility(View.VISIBLE);
        validationBanner.setAlert(AlertBannerView.Tone.DANGER, getString(R.string.app_name), text);
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void clearValidationBanner() {
        if (validationBanner != null) {
            validationBanner.setVisibility(View.GONE);
        }
    }

    private VoiceFieldHelper voiceFieldHelper;

    @Override
    protected void onDestroy() {
        if (voiceFieldHelper != null) voiceFieldHelper.destroy();
        super.onDestroy();
    }

    @Override
    public void onPhotoSelected(String path) {
        photoPath = path;
    }
}
