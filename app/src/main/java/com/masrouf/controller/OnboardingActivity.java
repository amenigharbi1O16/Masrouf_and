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
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OnboardingActivity — Post-login 3-step onboarding:
 *
 *  Step 1 (01/03): Welcome + hero card + sample transactions → "Sélection des catégories"
 *  Step 2 (02/03): Category selection per-user (Produits / Dépenses) → "Vérifier les N catégories"
 *  Step 3 (03/03): Confirmation — shows selected categories + budget tip → "Accéder à l'accueil"
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        userId = getIntent().getLongExtra("USER_ID", SessionStore.userId(this));
        viewModel = new androidx.lifecycle.ViewModelProvider(
                this, new MasroufViewModelFactory(this)).get(MasroufViewModel.class);

        // Bind views
        stepOne = findViewById(R.id.onboarding_step_one);
        stepTwo = findViewById(R.id.onboarding_step_two);
        stepThree = findViewById(R.id.onboarding_step_three);
        stepLabel = findViewById(R.id.onboarding_step_label);
        titleBar = findViewById(R.id.onboarding_title_bar);
        backBtn = findViewById(R.id.onboarding_back);
        nextBtn = findViewById(R.id.onboarding_next);
        modifyBtn = findViewById(R.id.onboarding_modify_selection);
        saveProgress = findViewById(R.id.onboarding_save_progress);
        categoryLoading = findViewById(R.id.onboarding_category_loading);
        sampleTransactions = findViewById(R.id.onboarding_sample_transactions);
        categoryList = findViewById(R.id.onboarding_category_list);
        categoryCountLabel = findViewById(R.id.onboarding_category_count);
        categoryCountBadge = findViewById(R.id.category_count_badge);
        summaryList = findViewById(R.id.onboarding_summary_list);
        selectionSummaryText = findViewById(R.id.onboarding_selection_summary_text);

        // Category RecyclerView
        categoryList.setLayoutManager(new LinearLayoutManager(this));

        // Navigation
        backBtn.setOnClickListener(v -> handleBack());
        nextBtn.setOnClickListener(v -> advance());
        modifyBtn.setOnClickListener(v -> {
            step = 2;
            renderStep();
        });

        // Load user and categories
        loadData();

        // Initial render
        renderStep();
    }

    // ─── Data Loading ─────────────────────────────────────────────────────────

    private void loadData() {
        categoryLoading.setVisibility(View.VISIBLE);
        viewModel.<OnboardingData>request(
                repository -> {
                    UserEntity user = repository.user(userId);
                    if (user == null) throw new IllegalStateException("Unknown account");
                    List<SystemCategoryEntity> cats = repository.onboardingCategories(userId);
                    List<Long> selectedIds = repository.selectedCatalogIds(userId);
                    return new OnboardingData(user, cats, selectedIds);
                },
                state -> {
                    categoryLoading.setVisibility(View.GONE);
                    if (state.data == null) {
                        Toast.makeText(this, R.string.category_selection_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    currentUser = state.data.user;
                    availableCategories = state.data.categories;

                    // Restore previously selected keys
                    Set<Long> selectedIdSet = new HashSet<>(state.data.selectedCatalogIds);
                    for (SystemCategoryEntity cat : availableCategories) {
                        if (selectedIdSet.contains(cat.id)) selectedKeys.add(cat.key);
                    }

                    // Build adapter
                    buildCategoryAdapter();
                    updateCategoryCount();

                    // Build step-1 sample transactions
                    buildSampleTransactions();

                    // If already onboarded, skip to step 1 for review
                    renderStep();
                }
        );
    }

    private void buildSampleTransactions() {
        if (sampleTransactions == null) return;
        sampleTransactions.removeAllViews();
        // Sample rows matching the screenshot
        String[][] samples = {
                {"food",       getString(R.string.category_food),     "−42.500 DT", getString(R.string.onboarding_sample_desc1)},
                {"food",       getString(R.string.onboarding_sample_product), "−14.500 DT", getString(R.string.onboarding_sample_desc2)},
                {"transport",  getString(R.string.category_transport), "−35.000 DT", getString(R.string.onboarding_sample_desc3)},
        };
        for (String[] row : samples) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_summary_category, sampleTransactions, false);
            ImageView icon = itemView.findViewById(R.id.summary_category_icon);
            TextView name = itemView.findViewById(R.id.summary_category_name);
            TextView type = itemView.findViewById(R.id.summary_category_type);

            if (name != null) name.setText(row[1]);
            if (type != null) type.setText(row[3]); // description under name
            SystemCategoryEntity catEntity = findCategoryByKey(row[0]);
            if (catEntity != null && icon != null) {
                icon.setImageResource(CategoryIconResources.resolve(catEntity.iconName, catEntity.type));
            }
            sampleTransactions.addView(itemView);
        }
    }

    // ─── Category Adapter ─────────────────────────────────────────────────────

    private void buildCategoryAdapter() {
        categoryAdapter = new CategorySelectionAdapter(availableCategories, selectedKeys, key -> {
            if (selectedKeys.contains(key)) {
                selectedKeys.remove(key);
            } else {
                selectedKeys.add(key);
            }
            updateCategoryCount();
            refreshNextButton();
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

    private void handleBack() {
        if (step > 1) {
            step--;
            renderStep();
        } else {
            finish();
        }
    }

    private void renderStep() {
        stepOne.setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        stepTwo.setVisibility(step == 2 ? View.VISIBLE : View.GONE);
        stepThree.setVisibility(step == 3 ? View.VISIBLE : View.GONE);

        // Back button visible from step 2
        if (backBtn != null) backBtn.setVisibility(step > 1 ? View.VISIBLE : View.INVISIBLE);

        // Step label (e.g. "01 / 03 · BIENVENUE DANS MASROUF")
        switch (step) {
            case 1:
                stepLabel.setText(getString(R.string.onboarding_step_label_1));
                titleBar.setText(R.string.onboarding_discover);
                nextBtn.setText(R.string.onboarding_select_categories_btn);
                modifyBtn.setVisibility(View.GONE);
                break;
            case 2:
                stepLabel.setText(getString(R.string.onboarding_step_label_2));
                titleBar.setText(R.string.onboarding_your_categories);
                updateCategoryCount();
                modifyBtn.setVisibility(View.GONE);
                break;
            case 3:
                stepLabel.setText(getString(R.string.onboarding_step_label_3));
                titleBar.setText(R.string.onboarding_your_selection);
                nextBtn.setText(R.string.go_home);
                modifyBtn.setVisibility(View.VISIBLE);
                break;
        }

        refreshNextButton();
    }

    private void refreshNextButton() {
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
            TextView name = itemView.findViewById(R.id.summary_category_name);
            TextView type = itemView.findViewById(R.id.summary_category_type);

            if (icon != null) {
                icon.setImageResource(CategoryIconResources.resolve(cat.iconName, cat.type));
            }
            if (name != null) {
                name.setText(SystemCategoryCatalog.titleResource(cat.key) != 0
                        ? getString(SystemCategoryCatalog.titleResource(cat.key))
                        : cat.name);
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
        nextBtn.setEnabled(false);
        nextBtn.setText(R.string.onboarding_saving);
        saveProgress.setVisibility(View.VISIBLE);

        // Build list of selected SystemCategoryEntity objects
        ArrayList<SystemCategoryEntity> chosen = new ArrayList<>();
        for (SystemCategoryEntity cat : availableCategories) {
            if (selectedKeys.contains(cat.key)) chosen.add(cat);
        }

        viewModel.<Boolean>request(
                repository -> repository.saveOnboarding(userId, chosen),
                result -> {
                    if (!Boolean.TRUE.equals(result.data)) {
                        saving = false;
                        saveProgress.setVisibility(View.GONE);
                        nextBtn.setEnabled(true);
                        nextBtn.setText(R.string.go_home);
                        Toast.makeText(this, R.string.category_selection_failed, Toast.LENGTH_LONG).show();
                        return;
                    }
                    SessionStore.markOnboardingComplete(this);
                    startActivity(new Intent(this, com.example.masrouf1.MainActivity.class)
                            .putExtra("USER_ID", userId));
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
     * Adapter that shows categories in two sections:
     *   "Catégories de produits" → all type=product
     *   "Catégories de dépenses" → all type=expense
     *
     * Each row: icon | name | "Sélectionner" / "Sélectionné" toggle
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
            // Products section
            boolean hasProducts = false;
            for (SystemCategoryEntity c : cats) {
                if ("product".equalsIgnoreCase(c.type)) {
                    if (!hasProducts) {
                        items.add("PRODUCTS_HEADER");
                        hasProducts = true;
                    }
                    items.add(c);
                }
            }
            // Expenses section
            boolean hasExpenses = false;
            for (SystemCategoryEntity c : cats) {
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

                // Name
                int nameRes = SystemCategoryCatalog.titleResource(cat.key);
                h.name.setText(nameRes != 0
                        ? holder.itemView.getContext().getString(nameRes)
                        : cat.name);

                // Toggle button
                boolean isSelected = selected.contains(cat.key);
                if (isSelected) {
                    h.toggle.setText(R.string.selected_btn);
                    h.toggle.setTextColor(holder.itemView.getContext().getColor(R.color.muted_green));
                } else {
                    h.toggle.setText(R.string.select_btn);
                    h.toggle.setTextColor(holder.itemView.getContext().getColor(R.color.emerald));
                }

                // Divider: skip — no item_divider id in this layout

                h.itemView.setOnClickListener(v -> {
                    listener.onToggle(cat.key);
                    notifyItemChanged(position);
                });
                h.toggle.setOnClickListener(v -> {
                    listener.onToggle(cat.key);
                    notifyItemChanged(position);
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
            final ImageView icon;
            final TextView name, toggle;
            ItemHolder(View v) {
                super(v);
                icon = v.findViewById(R.id.item_category_icon);
                name = v.findViewById(R.id.item_category_name);
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
