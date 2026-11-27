/*
 * ============================================================================
 * FICHIER : ProductsFragment.java
 * MISSION : Onglet central « Produits » affichant le catalogue personnel des
 *           articles achetés persistés dans SQLite Room. Offre la recherche
 *           texte en temps réel, la dictée vocale inline (FR/AR/EN/Darija),
 *           le filtrage par puces ChipGroup (catégories de type "product"),
 *           le tri alphabétique ou par prix croissant et l'ajout d'achat.
 * IMPLÉMENTE : Sous-classe de DashboardSectionFragment s'appuyant sur
 *              fragment_products.xml, RecyclerView + ProductCatalogAdapter,
 *              VoiceFieldHelper, TextWatcher et ChipGroup dynamique.
 * EFFET À L'ÉCRAN : Bannière de période active (« Changer > »), champ recherche
 *                   avec bouton micro, bouton « Ajouter un achat », puces de
 *                   catégories, compteur + bouton tri et liste de cartes produit.
 * COMMUNICATIONS :
 *   ← DashboardSectionFragment (barre supérieure ☰ + cloche + profil)
 *   → MasroufViewModel / MasroufRepository (productCatalog, categories, budget)
 *   → ProductCatalogAdapter (liaison des lignes produit cliquables)
 *   → AddProductActivity (bouton « Ajouter un achat »)
 *   → ProductDetailsActivity (clic sur une fiche produit)
 *   → MainActivity.openCentralTab("calendar") (clic sur la bannière période)
 *   → VoiceFieldHelper (dictée vocale dans la barre de recherche)
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.ProductCatalogRow;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.masrouf.controller.AddProductActivity;
import com.masrouf.controller.ProductDetailsActivity;
import com.masrouf.controller.VoiceFieldHelper;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Onglet central « Produits » affichant le catalogue personnel persisté dans Room :
 * recherche texte + dictée vocale inline, puces ChipGroup de filtrage par catégorie produit,
 * tri alphabétique ou par prix et accès aux fiches ProductDetailsActivity.
 */
/**
 * Room-backed searchable product catalog ("Produits") aligned with React Native:
 * - Active period pill banner ("Changer >")
 * - Warm beige search card with inline multilingual voice search
 * - Prominent left-aligned "Ajouter un achat" button before empty-state / catalog list ("Comparer un prix" removed)
 * - Category filter pills (product categories only) & A-Z / Price sorting
 */
public final class ProductsFragment extends DashboardSectionFragment {
    private EditText search;
    private ImageButton searchMic;
    private ChipGroup filters;
    private TextView empty, emptySub, periodLabel, countView, sortLabel;
    private View emptyCard;
    private RecyclerView listView;
    private ProductCatalogAdapter adapter;
    private VoiceFieldHelper voiceFieldHelper;
    private final List<ProductCatalogRow> allProducts = new ArrayList<>();
    private boolean sortByPrice = false;

    @Override
    protected int pageTitle() {
        return R.string.nav_products;
    }

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        voiceFieldHelper = new VoiceFieldHelper(requireContext(), this, null);
    }

    @Override
    public void onDestroy() {
        if (voiceFieldHelper != null) {
            voiceFieldHelper.destroy();
        }
        super.onDestroy();
    }

    @Override
    protected void createContent(LinearLayout content) {
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.fragment_products, content, false);
        content.addView(root);

        TextInputLayout searchBox = root.findViewById(R.id.product_search);
        search = root.findViewById(R.id.product_search_input);
        if (search == null && searchBox != null) {
            search = searchBox.getEditText();
        }
        searchMic = root.findViewById(R.id.product_search_mic);
        filters = root.findViewById(R.id.product_category_filters);
        empty = root.findViewById(R.id.product_catalog_empty);
        emptySub = root.findViewById(R.id.product_catalog_empty_sub);
        emptyCard = root.findViewById(R.id.product_catalog_empty_card);
        periodLabel = root.findViewById(R.id.product_period_label);
        countView = root.findViewById(R.id.product_catalog_count);
        sortLabel = root.findViewById(R.id.product_sort_label);

        listView = root.findViewById(R.id.product_catalog_list);
        listView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProductCatalogAdapter(product -> startActivity(
                new Intent(requireContext(), ProductDetailsActivity.class)
                        .putExtra("PRODUCT_ID", product.id)));
        listView.setAdapter(adapter);

        ((MaterialButton) root.findViewById(R.id.product_catalog_add)).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), AddProductActivity.class)));

        View periodBanner = root.findViewById(R.id.product_period_banner);
        if (periodBanner != null) {
            periodBanner.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).openCentralTab("calendar");
                }
            });
        }

        View sortToggle = root.findViewById(R.id.product_sort_toggle);
        if (sortToggle != null) {
            sortToggle.setOnClickListener(v -> {
                sortByPrice = !sortByPrice;
                if (sortLabel != null) {
                    sortLabel.setText(sortByPrice ? R.string.products_sort_price : R.string.products_sort_alpha);
                }
                applyFilter();
            });
        }

        if (search != null) {
            search.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    applyFilter();
                }

                @Override
                public void afterTextChanged(Editable s) { }
            });
            voiceFieldHelper.bindField(searchMic, search, (target, text, lang) -> applyFilter());
        }

        filters.setOnCheckedStateChangeListener((group, checked) -> applyFilter());
    }

    @Override
    protected void observeContent() {
        loadProducts();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewModel != null && getView() != null) {
            loadProducts();
        }
    }

    private void loadProducts() {
        long userId = signedInUserId();
        viewModel.<CatalogSnapshot>request(
                repository -> new CatalogSnapshot(
                        repository.productCatalog(userId),
                        repository.categories(userId),
                        repository.currentOrActiveBudget(userId)),
                state -> {
                    if (!isAdded() || getView() == null) return;
                    if (state.failed || state.data == null) {
                        allProducts.clear();
                        adapter.submit(allProducts);
                        empty.setText(R.string.dashboard_read_error);
                        if (emptySub != null) emptySub.setVisibility(View.GONE);
                        if (emptyCard != null) emptyCard.setVisibility(View.VISIBLE);
                        if (listView != null) listView.setVisibility(View.GONE);
                        return;
                    }
                    renderPeriodBanner(state.data.activeBudget);
                    allProducts.clear();
                    allProducts.addAll(state.data.products);
                    buildFilters(state.data.categories);
                    applyFilter();
                });
    }

    private void renderPeriodBanner(@Nullable BudgetEntity budget) {
        if (periodLabel == null || !isAdded()) return;
        Locale locale = Locale.getDefault();
        java.util.Calendar cal = java.util.Calendar.getInstance();
        long start = budget != null ? budget.periodStart
                : BudgetPeriodMath.startOfMonthMillis(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH));
        long end = budget != null ? budget.periodEnd
                : BudgetPeriodMath.endOfMonthMillis(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH));
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, locale);
        String range = df.format(new Date(start)) + " – " + df.format(new Date(end));
        periodLabel.setText(getString(R.string.products_active_period_prefix, range));
    }

    /** Reconstruit dynamiquement les puces ChipGroup de filtrage pour les seules catégories de type "product". */
    private void buildFilters(List<CategoryEntity> categories) {
        if (filters == null) return;
        long previousSelected = 0L;
        int checkedId = filters.getCheckedChipId();
        if (checkedId != View.NO_ID) {
            View cur = filters.findViewById(checkedId);
            if (cur != null && cur.getTag() instanceof Long) {
                previousSelected = (Long) cur.getTag();
            }
        }
        filters.removeAllViews();
        Chip all = createFilterChip(getString(R.string.filter_all), 0L);
        filters.addView(all);
        int idToCheck = all.getId();
        for (CategoryEntity category : categories) {
            if (!"product".equalsIgnoreCase(category.type)) continue;
            Chip chip = createFilterChip(category.name, category.id);
            filters.addView(chip);
            if (category.id == previousSelected) {
                idToCheck = chip.getId();
            }
        }
        filters.check(idToCheck);
    }

    /** Instancie en Java une puce Material Chip cochable mémorisant l'ID de catégorie dans {@code setTag()}. */
    private Chip createFilterChip(String label, long categoryId) {
        Chip chip = new Chip(requireContext());
        chip.setId(View.generateViewId());
        chip.setText(label);
        chip.setCheckable(true);
        chip.setTag(categoryId);
        chip.setChipBackgroundColor(ColorStateList.valueOf(requireContext().getColor(R.color.form_beige)));
        chip.setCheckedIconVisible(true);
        chip.setCheckedIconTint(ColorStateList.valueOf(requireContext().getColor(R.color.emerald)));
        chip.setChipStrokeColor(ColorStateList.valueOf(requireContext().getColor(R.color.input_border)));
        chip.setChipStrokeWidth(1f * getResources().getDisplayMetrics().density);
        chip.setTextColor(requireContext().getColor(R.color.black));
        return chip;
    }

    /** Applique le filtre texte (nom/marque), le filtre catégorie et le tri (A-Z ou prix) puis rafraîchit ProductCatalogAdapter. */
    private void applyFilter() {
        if (adapter == null || search == null) return;
        String query = search.getText() == null ? "" : search.getText().toString().trim().toLowerCase(Locale.ROOT);
        long categoryId = 0;
        int selected = filters.getCheckedChipId();
        View chip = selected == View.NO_ID ? null : filters.findViewById(selected);
        if (chip != null && chip.getTag() instanceof Long) categoryId = (Long) chip.getTag();

        ArrayList<ProductCatalogRow> visible = new ArrayList<>();
        for (ProductCatalogRow row : allProducts) {
            boolean matchesCategory = categoryId == 0 || row.categoryId == categoryId;
            String name = row.name == null ? "" : row.name.toLowerCase(Locale.ROOT);
            String brand = row.brand == null ? "" : row.brand.toLowerCase(Locale.ROOT);
            boolean matchesText = query.isEmpty() || name.contains(query) || brand.contains(query);
            if (matchesCategory && matchesText) visible.add(row);
        }

        if (sortByPrice) {
            Collections.sort(visible, (a, b) -> Long.compare(a.currentPriceMilli, b.currentPriceMilli));
        } else {
            Collections.sort(visible, Comparator.comparing(
                    row -> row.name == null ? "" : row.name.toLowerCase(Locale.ROOT)));
        }

        adapter.submit(visible);
        if (countView != null && isAdded()) {
            countView.setText(getString(
                    visible.size() == 1 ? R.string.products_count_sort : R.string.products_count_sort_plural,
                    visible.size()));
        }

        boolean isEmpty = visible.isEmpty();
        empty.setText(allProducts.isEmpty() ? R.string.products_empty_title : R.string.products_filter_empty);
        if (emptySub != null) {
            emptySub.setVisibility(allProducts.isEmpty() && isEmpty ? View.VISIBLE : View.GONE);
        }
        if (emptyCard != null) {
            emptyCard.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        } else {
            empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (listView != null) {
            listView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    private static final class CatalogSnapshot {
        final List<ProductCatalogRow> products;
        final List<CategoryEntity> categories;
        final BudgetEntity activeBudget;

        CatalogSnapshot(List<ProductCatalogRow> products, List<CategoryEntity> categories, BudgetEntity activeBudget) {
            this.products = products;
            this.categories = categories;
            this.activeBudget = activeBudget;
        }
    }
}
