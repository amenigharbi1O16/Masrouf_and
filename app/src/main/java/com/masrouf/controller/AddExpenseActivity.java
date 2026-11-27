/*
 * ============================================================================
 * FICHIER : AddExpenseActivity.java
 * MISSION : Écran formulaire permettant à tous les profils (USER, PARENT, CHILD)
 *           d'enregistrer une dépense directe (loyer, transport, facture, etc.) :
 *           libellé, montant en dinars, catégorie de dépense, date (DatePickerDialog),
 *           note facultative et dictée vocale par champ.
 * IMPLÉMENTE : Sous-classe de BaseFeatureActivity chargeant activity_add_expense.xml,
 *              résolvant le budget correspondant à la date saisie, validant les
 *              entrées via MoneyInput et redirigeant vers ConfirmationActivity.
 * EFFET À L'ÉCRAN : Carte formulaire beige chaud avec icônes micro inline,
 *                   bannière d'alerte si aucun budget n'est défini, liste
 *                   déroulante des catégories et bouton d'enregistrement.
 * COMMUNICATIONS :
 *   ← HomeFragment (bouton « Ajouter une dépense » ou saisie vocale)
 *   → MasroufViewModel / MasroufRepository (productForm, resolveBudgetForDateOrPreferred,
 *     saveExpense)
 *   → VoiceFieldHelper (dictée vocale libellé / montant / note)
 *   → BudgetPeriodActivity (bouton « Définir un budget » si absent)
 *   → ConfirmationActivity (écran récapitulatif après succès)
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
import com.example.masrouf1.ProductFormData;
import com.example.masrouf1.R;
import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
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
 * Formulaire d'ajout d'une dépense directe (hérite de BaseFeatureActivity / activity_section.xml).
 * Charge le budget actif et les catégories de type dépense, intègre la dictée vocale par champ
 * (VoiceFieldHelper) et un sélecteur de date (DatePickerDialog), puis exécute saveExpense dans Room.
 */
public final class AddExpenseActivity extends BaseFeatureActivity {
    private static final String STATE_CATEGORY_ID = "ae_cat_id";
    private static final String STATE_DATE_MILLIS = "ae_date";

    private MasroufViewModel viewModel;
    private EditText label, amount, note;
    private Spinner categorySpinner;
    private TextView date;
    private MaterialButton save, defineBudgetBtn;
    private AlertBannerView budgetBanner, validationBanner;

    private List<CategoryEntity> categories = new ArrayList<>();
    private final Map<Long, String> catalogKeyByCategoryId = new HashMap<>();
    private BudgetEntity budget;
    private long occurredAt;
    private long selectedCategoryId = -1L;
    private boolean bindingCategories = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        showPage(R.string.add_expense);
        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this))
                .get(MasroufViewModel.class);
        View form = getLayoutInflater().inflate(R.layout.activity_add_expense, content, false);
        content.addView(form);

        label = findViewById(R.id.expense_label);
        amount = findViewById(R.id.expense_amount);
        note = findViewById(R.id.expense_note);
        categorySpinner = findViewById(R.id.expense_category);
        date = findViewById(R.id.expense_date);
        save = findViewById(R.id.save_expense);
        budgetBanner = findViewById(R.id.expense_budget_banner);
        defineBudgetBtn = findViewById(R.id.expense_define_budget_btn);
        validationBanner = findViewById(R.id.expense_validation_banner);

        if (state != null) {
            selectedCategoryId = state.getLong(STATE_CATEGORY_ID, -1L);
            occurredAt = state.getLong(STATE_DATE_MILLIS, 0L);
        }
        if (occurredAt <= 0L) {
            occurredAt = System.currentTimeMillis();
        }
        updateDateDisplay();

        ArrayAdapter<String> initExpAdapter = new ArrayAdapter<>(this, R.layout.item_masrouf_spinner,
                new String[]{getString(R.string.select_expense_category)});
        initExpAdapter.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);
        categorySpinner.setAdapter(initExpAdapter);
        categorySpinner.setPopupBackgroundResource(R.drawable.bg_dropdown_beige);

        voiceFieldHelper = new VoiceFieldHelper(this, this, validationBanner);
        voiceFieldHelper.bindField(findViewById(R.id.expense_label_mic), label);
        voiceFieldHelper.bindField(findViewById(R.id.expense_amount_mic), amount);
        voiceFieldHelper.bindField(findViewById(R.id.expense_note_mic), note);
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

        if (getIntent().hasExtra("VOICE_LABEL")) label.setText(getIntent().getStringExtra("VOICE_LABEL"));
        if (getIntent().hasExtra("VOICE_AMOUNT")) amount.setText(getIntent().getStringExtra("VOICE_AMOUNT"));
        date.setOnClickListener(v -> chooseDate());
        defineBudgetBtn.setOnClickListener(v -> startActivity(new Intent(this, BudgetPeriodActivity.class)));
        save.setOnClickListener(v -> saveExpense());
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

            ArrayList<CategoryEntity> expenseCategories = new ArrayList<>();
            for (CategoryEntity item : state.data.categories) {
                if (!"product".equalsIgnoreCase(item.type)) expenseCategories.add(item);
            }
            if (expenseCategories.isEmpty()) expenseCategories.addAll(state.data.categories);

            budget = state.data.budget;
            categories = expenseCategories;
            updateNoBudgetBanner();

            ArrayList<String> labels = new ArrayList<>();
            labels.add(getString(R.string.select_expense_category));
            for (CategoryEntity row : categories) {
                labels.add(localizedCategoryName(row));
            }

            bindingCategories = true;
            ArrayAdapter<String> expCatAdapter = new ArrayAdapter<>(this, R.layout.item_masrouf_spinner, labels);
            expCatAdapter.setDropDownViewResource(R.layout.item_masrouf_spinner_dropdown);
            categorySpinner.setAdapter(expCatAdapter);
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

    private void saveExpense() {
        clearValidationBanner();
        long milli;
        try {
            milli = MoneyInput.toMilli(amount.getText() == null ? "" : amount.getText().toString());
            if (milli <= 0L) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            amount.setError(getString(R.string.money_invalid));
            amount.requestFocus();
            showValidationError(R.string.money_invalid);
            return;
        }

        if (categories.isEmpty()) {
            showValidationError(R.string.no_categories_available);
            return;
        }

        CategoryEntity selected = resolveSelectedCategory();
        if (selected == null) {
            showValidationError(R.string.select_category_first);
            categorySpinner.requestFocus();
            return;
        }

        if (occurredAt <= 0L) {
            date.setError(getString(R.string.date_required));
            showValidationError(R.string.date_required);
            return;
        }

        long ownerId = SessionStore.userId(this);
        long preferredId = SessionStore.selectedBudgetId(this, ownerId);
        String text = label.getText() == null ? "" : label.getText().toString().trim();
        String displayTitle = text.isEmpty() ? localizedCategoryName(selected) : text;

        TransactionEntity expense = new TransactionEntity();
        expense.userId = ownerId;
        expense.categoryId = selected.id;
        expense.kind = "expense";
        expense.label = displayTitle;
        expense.note = note.getText() == null ? "" : note.getText().toString().trim();
        expense.amountMilli = milli;
        expense.occurredAt = occurredAt;

        save.setEnabled(false);
        viewModel.<Long>request(repository -> {
            BudgetEntity targetBudget = repository.resolveBudgetForDateOrPreferred(ownerId, occurredAt, preferredId);
            if (targetBudget == null) return -3L;
            budget = targetBudget;
            expense.budgetId = targetBudget.id;
            return repository.saveExpense(expense, ownerId);
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
                    .putExtra("LABEL", displayTitle)
                    .putExtra("AMOUNT", milli)
                    .putExtra("KIND", "expense"));
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

    private VoiceFieldHelper voiceFieldHelper;

    @Override
    protected void onDestroy() {
        if (voiceFieldHelper != null) voiceFieldHelper.destroy();
        super.onDestroy();
    }

    private void clearValidationBanner() {
        if (validationBanner != null) {
            validationBanner.setVisibility(View.GONE);
        }
    }
}
