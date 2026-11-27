/*
 * ============================================================================
 * FICHIER : OnboardingActivity.java
 * MISSION : Assistant de prise en main en 3 étapes affiché lors de la première
 *           connexion tant que les catégories initiales n'ont pas été choisies :
 *           Étape 1 (Découverte + exemples), Étape 2 (Sélection personnalisée
 *           des catégories autorisées), Étape 3 (Récapitulatif et validation).
 * IMPLÉMENTE : AppCompatActivity (activity_onboarding.xml) avec état restauré
 *              sur rotation (STEP + SELECTED_KEYS), RecyclerView à en-têtes
 *              (CategorySelectionAdapter) respectant les restrictions enfant
 *              et écriture atomique finishOnboarding() dans Room.
 * EFFET À L'ÉCRAN : Indicateur de progression 01/03 à 03/03, cartes d'exemples,
 *                   liste interactive de sélection Produits/Dépenses et bouton
 *                   final « Accéder à l'accueil ».
 * COMMUNICATIONS :
 *   ← LoginActivity, MainActivity (gate de vérification compte)
 *   → MasroufViewModel / MasroufRepository (onboardingCategories, saveOnboarding)
 *   → SessionStore (save, markOnboardingComplete, clear si abandon étape 1)
 *   → MainActivity (lancement avec CLEAR_TASK une fois terminé)
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.SystemCategoryEntity;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.example.masrouf1.ui.CategoryIconResources;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parcours d'accueil en 3 étapes après la première connexion :
 * 1) Découverte avec exemples ; 2) Sélection des catégories autorisées pour ce compte ;
 * 3) Récapitulatif puis écriture atomique dans user_categories / categories (finishOnboarding).
 */
/**
 * OnboardingActivity – Post-login 3-step onboarding:
 *
 *  Step 1 (01/03): Welcome + hero card + sample transactions → "Sélection des catégories"
 *  Step 2 (02/03): Category selection per-user (Produits / Dépenses) → "Vérifier les N catégories"
 *  Step 3 (03/03): Confirmation – shows selected categories + budget tip → "Accéder à l'accueil"
 *
 * Per-user: each user's selections saved to user_categories table (not shared).
 * categoriesSelected + onboarded flags set on UserEntity after Step 3 confirmation.
 */
public final class OnboardingActivity extends AppCompatActivity {

    // ─── State ────────────────────────────────────────────────────────────────
    private int step = 1;
    private long userId;
    private UserEntity currentUser;
    private List<SystemCategoryEntity> availableCategories = new ArrayList<>();
    private final Set<String> selectedKeys = new HashSet<>();
    private boolean saving = false;

    // ─── Views ────────────────────────────────────────────────────────────────
    private LinearLayout stepOne, stepTwo, stepThree;
    private TextView stepLabel, titleBar;
    private ImageView backBtn;
    private MaterialButton nextBtn, modifyBtn;
    private ProgressBar saveProgress, categoryLoading;

    // Step 1 views
    private LinearLayout sampleTransactions;

    // Step 2 views
    private RecyclerView categoryList;
    private TextView categoryCountLabel, categoryCountBadge;
    private CategorySelectionAdapter categoryAdapter;

    // Step 3 views
    private LinearLayout summaryList;
    private TextView selectionSummaryText;

    // ─── ViewModel ────────────────────────────────────────────────────────────
    private MasroufViewModel viewModel;

    private static final String STATE_STEP = "onboarding_step";
    private static final String STATE_SELECTED_KEYS = "onboarding_selected_keys";
    private boolean restoredSelectionState = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        userId = getIntent().getLongExtra("USER_ID", SessionStore.userId(this));
        if (userId <= 0L) {
            startActivity(new Intent(this, WelcomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }
        if (savedInstanceState != null) {
            step = savedInstanceState.getInt(STATE_STEP, 1);
            ArrayList<String> restored = savedInstanceState.getStringArrayList(STATE_SELECTED_KEYS);
            if (restored != null) {
                selectedKeys.addAll(restored);
                restoredSelectionState = true;
            }
        }
        viewModel = new androidx.lifecycle.ViewModelProvider(
                this, new MasroufViewModelFactory(this)).get(MasroufViewModel.class);

        // Bind views
        stepOne   = findViewById(R.id.onboarding_step_one);
        stepTwo   = findViewById(R.id.onboarding_step_two);
        stepThree = findViewById(R.id.onboarding_step_three);
        stepLabel = findViewById(R.id.onboarding_step_label);
        titleBar  = findViewById(R.id.onboarding_title_bar);
        backBtn   = findViewById(R.id.onboarding_back);
        nextBtn   = findViewById(R.id.onboarding_next);
        modifyBtn = findViewById(R.id.onboarding_modify_selection);
        saveProgress   = findViewById(R.id.onboarding_save_progress);
        categoryLoading = findViewById(R.id.onboarding_category_loading);
        sampleTransactions = findViewById(R.id.onboarding_sample_transactions);
        categoryList    = findViewById(R.id.onboarding_category_list);
        categoryCountLabel = findViewById(R.id.onboarding_category_count);
        categoryCountBadge = findViewById(R.id.category_count_badge);
        summaryList = findViewById(R.id.onboarding_summary_list);
        selectionSummaryText = findViewById(R.id.onboarding_selection_summary_text);

        // Category RecyclerView
        if (categoryList != null) {
            categoryList.setLayoutManager(new LinearLayoutManager(this));
        }

        // Navigation
        if (backBtn != null) backBtn.setOnClickListener(v -> handleBack());
        if (nextBtn != null) nextBtn.setOnClickListener(v -> advance());
        if (modifyBtn != null) {
            modifyBtn.setOnClickListener(v -> {
                step = 2;
                renderStep();
            });
        }

        // Load user and categories
        loadData();

        // Initial render
        renderStep();
    }

    // ─── Data Loading ─────────────────────────────────────────────────────────

    private void loadData() {
        if (categoryLoading != null) categoryLoading.setVisibility(View.VISIBLE);
        viewModel.<OnboardingData>request(
                repository -> {
                    UserEntity user = repository.user(userId);
                    if (user == null) throw new IllegalStateException("Unknown account");
                    List<SystemCategoryEntity> cats = repository.onboardingCategories(userId);
                    List<Long> selectedIds = repository.selectedCatalogIds(userId);
                    return new OnboardingData(user, cats, selectedIds);
                },
                state -> {
                    if (categoryLoading != null) categoryLoading.setVisibility(View.GONE);
                    if (state.data == null) {
                        Toast.makeText(this, R.string.category_selection_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    currentUser = state.data.user;
                    availableCategories = state.data.categories;

                    // Restore previously selected keys (per-user) unless mid-flow state already restored
                    if (!restoredSelectionState) {
                        selectedKeys.clear();
                        Set<Long> selectedIdSet = new HashSet<>(state.data.selectedCatalogIds);
                        for (SystemCategoryEntity cat : availableCategories) {
                            if (selectedIdSet.contains(cat.id)) selectedKeys.add(cat.key);
                        }
                    }

                    // Build adapter
                    buildCategoryAdapter();
                    updateCategoryCount();

                    // Build step-1 sample transactions
                    buildSampleTransactions();

                    // If restored directly into step 3, populate summary cards too
                    if (step == 3) {
                        buildSummary();
                    }

                    // Re-render with loaded data
                    renderStep();
                }
        );
    }

    private void buildSampleTransactions() {
        if (sampleTransactions == null) return;
        sampleTransactions.removeAllViews();
        // Sample rows matching the screenshot
        String[][] samples = {
                {"food",      getString(R.string.category_food),     "−42.500 DT", getString(R.string.onboarding_sample_desc1)},
                {"food",      getString(R.string.onboarding_sample_product), "−14.500 DT", getString(R.string.onboarding_sample_desc2)},
                {"transport", getString(R.string.category_transport), "−35.000 DT", getString(R.string.onboarding_sample_desc3)},
        };
        for (String[] row : samples) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_summary_category, sampleTransactions, false);
            ImageView icon = itemView.findViewById(R.id.summary_category_icon);
            TextView name  = itemView.findViewById(R.id.summary_category_name);
            TextView type  = itemView.findViewById(R.id.summary_category_type);

            if (name != null) name.setText(row[1]);
            if (type != null) type.setText(row[3]);
            SystemCategoryEntity catEntity = findCategoryByKey(row[0]);
            if (catEntity != null && icon != null) {
                icon.setImageResource(CategoryIconResources.resolve(catEntity.iconName, catEntity.type));
            }
            sampleTransactions.addView(itemView);
        }
    }

    // ─── Category Adapter ─────────────────────────────────────────────────────

    private void buildCategoryAdapter() {
        if (categoryList == null) return;
        categoryAdapter = new CategorySelectionAdapter(availableCategories, selectedKeys, key -> {
            // Toggle selection (per-user, not shared)
            if (selectedKeys.contains(key)) {
                selectedKeys.remove(key);
            } else {
                selectedKeys.add(key);
            }
            updateCategoryCount();
            refreshNextButton();
            // Refresh the whole adapter so Select/Selected state updates correctly
            categoryAdapter.notifyDataSetChanged();
        });
        categoryList.setAdapter(categoryAdapter);
    }

    private void updateCategoryCount() {
        int count = selectedKeys.size();
        if (categoryCountLabel != null) {
            categoryCountLabel.setText(getString(R.string.categories_selected_label));
        }
        if (categoryCountBadge != null) {
            categoryCountBadge.setText(String.valueOf(count));
        }
        if (nextBtn != null && step == 2) {
            nextBtn.setText(getString(R.string.onboarding_verify_categories, count));
        }
    }

    // ─── Step navigation ──────────────────────────────────────────────────────

    private void advance() {
        if (saving) return;
        switch (step) {
            case 1:
                step = 2;
                renderStep();
                break;
            case 2:
                if (selectedKeys.isEmpty()) {
                    Toast.makeText(this, R.string.categories_required, Toast.LENGTH_SHORT).show();
                    return;
                }
                step = 3;
                renderStep();
                buildSummary();
                break;
            case 3:
                saveAndGoHome();
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step);
        outState.putStringArrayList(STATE_SELECTED_KEYS, new ArrayList<>(selectedKeys));
    }

    private void handleBack() {
        if (step > 1) {
            step--;
            renderStep();
        } else {
            if (currentUser == null || !currentUser.categoriesSelected) {
                SessionStore.clear(this);
            }
            finish();
        }
    }

    private void renderStep() {
        if (stepOne != null)   stepOne.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        if (stepTwo != null)   stepTwo.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        if (stepThree != null) stepThree.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        // Back button visible from step 2
        if (backBtn != null) backBtn.setVisibility(step > 1 ? View.VISIBLE : View.INVISIBLE);

        if (nextBtn == null) return;

        switch (step) {
            case 1:
                if (stepLabel != null) stepLabel.setText(getString(R.string.onboarding_step_label_1));
                if (titleBar != null)  titleBar.setText(R.string.onboarding_discover);
                nextBtn.setText(R.string.onboarding_select_categories_btn);
                nextBtn.setTextColor(getColor(R.color.alabaster));
                if (modifyBtn != null) modifyBtn.setVisibility(View.GONE);
                break;
            case 2:
                if (stepLabel != null) stepLabel.setText(getString(R.string.onboarding_step_label_2));
                if (titleBar != null)  titleBar.setText(R.string.onboarding_your_categories);
                updateCategoryCount();
                if (modifyBtn != null) modifyBtn.setVisibility(View.GONE);
                nextBtn.setTextColor(getColor(R.color.alabaster));
                break;
            case 3:
                if (stepLabel != null) stepLabel.setText(getString(R.string.onboarding_step_label_3));
                if (titleBar != null)  titleBar.setText(R.string.onboarding_your_selection);
                nextBtn.setText(R.string.go_home);
                nextBtn.setTextColor(getColor(R.color.alabaster));
                if (modifyBtn != null) {
                    modifyBtn.setVisibility(View.VISIBLE);
                    modifyBtn.setTextColor(getColor(R.color.alabaster));
                }
                break;
        }

        refreshNextButton();
    }

    private void refreshNextButton() {
        if (nextBtn == null) return;
        if (step == 2) {
            int count = selectedKeys.size();
            nextBtn.setText(getString(R.string.onboarding_verify_categories, count));
            nextBtn.setEnabled(count > 0);
            nextBtn.setAlpha(count > 0 ? 1f : 0.6f);
        } else {
            nextBtn.setEnabled(true);
            nextBtn.setAlpha(1f);
        }
    }

    // ─── Step 3: Summary ──────────────────────────────────────────────────────

    private void buildSummary() {
        if (summaryList == null) return;
        summaryList.removeAllViews();

        int count = selectedKeys.size();
        if (selectionSummaryText != null) {
            selectionSummaryText.setText(getResources().getQuantityString(
                    R.plurals.categories_selected_summary, count, count));
        }

        for (SystemCategoryEntity cat : availableCategories) {
            if (!selectedKeys.contains(cat.key)) continue;
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_summary_category, summaryList, false);
            ImageView icon = itemView.findViewById(R.id.summary_category_icon);
            TextView name  = itemView.findViewById(R.id.summary_category_name);
            TextView type  = itemView.findViewById(R.id.summary_category_type);

            if (icon != null) {
                icon.setImageResource(CategoryIconResources.resolve(cat.iconName, cat.type));
            }
            int nameRes = SystemCategoryCatalog.titleResource(cat.key);
            if (name != null) {
                name.setText(nameRes != 0 ? getString(nameRes) : cat.name);
            }
            if (type != null) {
                type.setText("product".equalsIgnoreCase(cat.type)
                        ? getString(R.string.type_articles_produits)
                        : getString(R.string.type_depenses_services));
            }
            summaryList.addView(itemView);
        }
    }

    // ─── Save & navigate ──────────────────────────────────────────────────────

    private void saveAndGoHome() {
        if (saving) return;
        saving = true;
        if (nextBtn != null) {
            nextBtn.setEnabled(false);
            nextBtn.setText(R.string.onboarding_saving);
        }
        if (saveProgress != null) saveProgress.setVisibility(View.VISIBLE);

        // Build list of selected SystemCategoryEntity objects (this user's choices only)
        ArrayList<SystemCategoryEntity> chosen = new ArrayList<>();
        for (SystemCategoryEntity cat : availableCategories) {
            if (selectedKeys.contains(cat.key)) chosen.add(cat);
        }

        // Déclenche la transaction Room finale et bascule vers MainActivity sans possibilité de retour sur l'onboarding.
        viewModel.<Boolean>request(
                repository -> repository.saveOnboarding(userId, chosen),
                result -> {
                    if (!Boolean.TRUE.equals(result.data)) {
                        saving = false;
                        if (saveProgress != null) saveProgress.setVisibility(View.GONE);
                        if (nextBtn != null) {
                            nextBtn.setEnabled(true);
                            nextBtn.setText(R.string.go_home);
                        }
                        Toast.makeText(this, R.string.category_selection_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Mark this user's onboarding complete in session and preserve exact profile/role
                    if (currentUser != null) {
                        currentUser.onboarded = true;
                        currentUser.categoriesSelected = true;
                        SessionStore.save(this, currentUser);
                    }
                    SessionStore.markOnboardingComplete(this);
                    // Route to MainActivity — it reads the user's role and shows the correct dashboard
                    Intent home = new Intent(this, com.example.masrouf1.MainActivity.class);
                    home.putExtra("USER_ID", userId);
                    home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(home);
                    finish();
                }
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private SystemCategoryEntity findCategoryByKey(String key) {
        for (SystemCategoryEntity cat : availableCategories) {
            if (cat.key.equals(key)) return cat;
        }
        return null;
    }

    // ─── Category selection adapter ───────────────────────────────────────────

    /**
     * Shows categories in two sections:
     *   "Catégories de produits" → all type=product
     *   "Catégories de dépenses" → all type=expense
     *
     * Each row: icon | name | "Sélectionner" / "Sélectionné" toggle
     * Select/Selected state is dynamic — changes immediately on tap.
     */
    private static final class CategorySelectionAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        interface OnToggle { void onToggle(String key); }

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM   = 1;

        private final List<Object> items = new ArrayList<>(); // String (header) or SystemCategoryEntity
        private final Set<String> selected;
        private final OnToggle listener;

        CategorySelectionAdapter(List<SystemCategoryEntity> cats,
                                 Set<String> selected,
                                 OnToggle listener) {
            this.selected = selected;
            this.listener = listener;
            build(cats);
        }

        private void build(List<SystemCategoryEntity> cats) {
            items.clear();
            List<String> preferredOrder = java.util.Arrays.asList(
                    "food", "clothing", "hygiene", "home_equip", "computer", "health",
                    "housing", "transport", "bills", "education", "leisure", "children",
                    "family_aid", "savings", "donations", "banking", "other");
            List<SystemCategoryEntity> sorted = new ArrayList<>(cats);
            java.util.Collections.sort(sorted, (a, b) -> {
                int ia = preferredOrder.indexOf(a.key);
                int ib = preferredOrder.indexOf(b.key);
                if (ia < 0) ia = "other".equals(a.key) ? 999 : 500;
                if (ib < 0) ib = "other".equals(b.key) ? 999 : 500;
                return Integer.compare(ia, ib);
            });
            // Products section first
            boolean hasProducts = false;
            for (SystemCategoryEntity c : sorted) {
                if ("product".equalsIgnoreCase(c.type)) {
                    if (!hasProducts) {
                        items.add("PRODUCTS_HEADER");
                        hasProducts = true;
                    }
                    items.add(c);
                }
            }
            // Expenses section second
            boolean hasExpenses = false;
            for (SystemCategoryEntity c : sorted) {
                if ("expense".equalsIgnoreCase(c.type)) {
                    if (!hasExpenses) {
                        items.add("EXPENSES_HEADER");
                        hasExpenses = true;
                    }
                    items.add(c);
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position) instanceof String ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public int getItemCount() { return items.size(); }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            if (viewType == TYPE_HEADER) {
                View v = inflater.inflate(R.layout.item_category_section_header, parent, false);
                return new HeaderHolder(v);
            } else {
                View v = inflater.inflate(R.layout.item_onboarding_category, parent, false);
                return new ItemHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderHolder) {
                String tag = (String) items.get(position);
                boolean isProducts = "PRODUCTS_HEADER".equals(tag);
                ((HeaderHolder) holder).label.setText(
                        isProducts
                                ? holder.itemView.getContext().getString(R.string.onboarding_section_products)
                                : holder.itemView.getContext().getString(R.string.onboarding_section_expenses));
            } else if (holder instanceof ItemHolder) {
                SystemCategoryEntity cat = (SystemCategoryEntity) items.get(position);
                ItemHolder h = (ItemHolder) holder;

                // Icon
                h.icon.setImageResource(CategoryIconResources.resolve(cat.iconName, cat.type));

                // Name (use localized string if available)
                int nameRes = SystemCategoryCatalog.titleResource(cat.key);
                h.name.setText(nameRes != 0
                        ? holder.itemView.getContext().getString(nameRes)
                        : cat.name);

                // Toggle button: dynamic "Sélectionner" <-> "Sélectionné" ("Select" <-> "Selected")
                boolean isSelected = selected.contains(cat.key);
                if (h.rowContainer != null) {
                    h.rowContainer.setBackgroundColor(holder.itemView.getContext().getColor(
                            isSelected ? R.color.mint_light : android.R.color.transparent));
                }
                if (isSelected) {
                    h.toggle.setText(R.string.selected_btn);
                    h.toggle.setBackgroundResource(R.drawable.bg_lang_selected);
                    h.toggle.setTextColor(holder.itemView.getContext().getColor(R.color.alabaster));
                } else {
                    h.toggle.setText(R.string.select_btn);
                    h.toggle.setBackgroundResource(R.drawable.bg_btn_select);
                    h.toggle.setTextColor(holder.itemView.getContext().getColor(R.color.alabaster));
                }

                // Click on row OR on toggle button — both toggle the selection
                // Use getBindingAdapterPosition() to avoid stale position from lambda
                h.itemView.setOnClickListener(v -> {
                    int pos = h.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Object item = items.get(pos);
                    if (!(item instanceof SystemCategoryEntity)) return;
                    listener.onToggle(((SystemCategoryEntity) item).key);
                });
                h.toggle.setOnClickListener(v -> {
                    int pos = h.getBindingAdapterPosition();
                    if (pos == RecyclerView.NO_POSITION) return;
                    Object item = items.get(pos);
                    if (!(item instanceof SystemCategoryEntity)) return;
                    listener.onToggle(((SystemCategoryEntity) item).key);
                });
            }
        }

        static class HeaderHolder extends RecyclerView.ViewHolder {
            final TextView label;
            HeaderHolder(View v) {
                super(v);
                label = (TextView) v;
            }
        }

        static class ItemHolder extends RecyclerView.ViewHolder {
            final View rowContainer;
            final ImageView icon;
            final TextView name, toggle;
            ItemHolder(View v) {
                super(v);
                rowContainer = v.findViewById(R.id.item_category_row);
                icon   = v.findViewById(R.id.item_category_icon);
                name   = v.findViewById(R.id.item_category_name);
                toggle = v.findViewById(R.id.item_category_toggle);
            }
        }
    }

    // ─── Data snapshot ────────────────────────────────────────────────────────

    private static final class OnboardingData {
        final UserEntity user;
        final List<SystemCategoryEntity> categories;
        final List<Long> selectedCatalogIds;
        OnboardingData(UserEntity user, List<SystemCategoryEntity> categories, List<Long> selectedCatalogIds) {
            this.user = user;
            this.categories = categories;
            this.selectedCatalogIds = selectedCatalogIds;
        }
    }
}
