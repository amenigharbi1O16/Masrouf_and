/*
 * ============================================================================
 * FICHIER : CustomCategoryDialogFragment.java
 * MISSION : Boîte de dialogue modale permettant soit d'activer une catégorie
 *           système du catalogue non encore suivie, soit de créer une nouvelle
 *           catégorie personnalisée (nom avec dictée vocale, pastille couleur
 *           botanique et pourcentage d'allocation sur 100 %).
 * IMPLÉMENTE : DialogFragment gonflant dialog_add_category.xml avec onglets
 *              Produit/Dépense, RadioGroup Proposée/Manuelle, jauge % temps
 *              réel, VoiceFieldHelper et notification via Listener + FragmentResult.
 * EFFET À L'ÉCRAN : Fenêtre beige chaleureuse avec sélecteur Produit/Dépense,
 *                   liste des catégories proposées, palette de 8 pastilles de
 *                   couleurs et barre de progression d'allocation.
 * COMMUNICATIONS :
 *   ← ManageCategoriesActivity, HomeFragment (clic « Ajouter une catégorie »)
 *   → MasroufViewModel / MasroufRepository (loadManageCategories, categoryAllocation,
 *     activateProposedCatalogCategory, addCategory)
 *   → VoiceFieldHelper (dictée vocale du nom de catégorie)
 *   → CategoryIconResources, SystemCategoryCatalog
 * ============================================================================
 */
package com.masrouf.controller;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.masrouf1.CategoryAllocationData;
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
 * Boîte de dialogue modale permettant d'activer une catégorie système non sélectionnée ou de créer
 * une catégorie personnalisée (avec dictée vocale du nom, pastille couleur botanique et % d'allocation).
 */
/**
 * Add Category Modal aligned with React Native CustomCategoryModal.tsx:
 * - Warm off-white/beige background, zero grey, zero purple, crisp black text +
 * white-on-green buttons
 * - Product vs Expense tabs (no Material purple TabLayout)
 * - Source mode RadioGroup: Proposed unselected catalog categories OR manual
 * name + inline voice dictation
 * - Non-purple botanical accent palette & dynamic allocation % gauge
 */
public final class CustomCategoryDialogFragment extends DialogFragment {
    public interface Listener {
        void onCategoryCreated();
    }

    private static final String[] BOTANICAL_SWATCHES = {
            "#2B7A4B", "#1E5C45", "#0F766E", "#D97706",
            "#B45309", "#DC2626", "#0284C7", "#374151"
    };

    private MasroufViewModel viewModel;
    private VoiceFieldHelper voiceFieldHelper;
    private long userId;

    private String selectedType = "product";
    private boolean manualMode = false;
    private int usedPercent = 0;
    private String selectedColorHex = BOTANICAL_SWATCHES[0];
    @Nullable
    private SystemCategoryEntity chosenProposed;

    private ManageCategoriesData categoriesData;

    private AlertBannerView banner;
    private TextView tabProduct, tabExpense, proposedHeader, noProposedText, allocSummary;
    private RadioGroup sourceGroup;
    private RadioButton radioProposed, radioManual;
    private View proposedContainer, manualContainer;
    private LinearLayout proposedList, colorRow;
    private EditText nameInput, allocInput;
    private ImageButton nameMic;
    private ProgressBar allocProgress;
    private MaterialButton saveBtn, cancelBtn;
    private boolean userEditedAlloc = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = SessionStore.userId(requireContext());
        viewModel = new ViewModelProvider(this, new MasroufViewModelFactory(requireContext()))
                .get(MasroufViewModel.class);
        voiceFieldHelper = new VoiceFieldHelper(requireContext(), this, null);
    }

    @Override
    public void onDestroy() {
        if (voiceFieldHelper != null)
            voiceFieldHelper.destroy();
        super.onDestroy();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle state) {
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null, false);

        banner = root.findViewById(R.id.add_cat_banner);
        tabProduct = root.findViewById(R.id.add_cat_tab_product);
        tabExpense = root.findViewById(R.id.add_cat_tab_expense);
        sourceGroup = root.findViewById(R.id.add_cat_source_group);
        radioProposed = root.findViewById(R.id.add_cat_radio_proposed);
        radioManual = root.findViewById(R.id.add_cat_radio_manual);
        proposedContainer = root.findViewById(R.id.add_cat_proposed_container);
        manualContainer = root.findViewById(R.id.add_cat_manual_container);
        proposedHeader = root.findViewById(R.id.add_cat_proposed_header);
        proposedList = root.findViewById(R.id.add_cat_proposed_list);
        noProposedText = root.findViewById(R.id.add_cat_no_proposed);
        nameInput = root.findViewById(R.id.add_cat_name_input);
        nameMic = root.findViewById(R.id.add_cat_name_mic);
        colorRow = root.findViewById(R.id.add_cat_color_row);
        allocInput = root.findViewById(R.id.add_cat_alloc_input);
        allocSummary = root.findViewById(R.id.add_cat_alloc_summary);
        allocProgress = root.findViewById(R.id.add_cat_alloc_progress);
        saveBtn = root.findViewById(R.id.add_cat_save);
        cancelBtn = root.findViewById(R.id.add_cat_cancel);

        voiceFieldHelper.setStatusBanner(banner);
        voiceFieldHelper.bindField(nameMic, nameInput);

        root.findViewById(R.id.add_cat_close).setOnClickListener(v -> dismiss());
        cancelBtn.setOnClickListener(v -> dismiss());

        tabProduct.setOnClickListener(v -> switchType("product"));
        tabExpense.setOnClickListener(v -> switchType("expense"));

        sourceGroup.setOnCheckedChangeListener((group, checkedId) -> {
            manualMode = checkedId == R.id.add_cat_radio_manual;
            updateSourceVisibility();
            if (banner != null)
                banner.setVisibility(View.GONE);
        });

        allocInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userEditedAlloc = true;
                updateAllocationPreview();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        saveBtn.setOnClickListener(v -> saveCategory());

        renderColorSwatches();
        renderTypeTabs();
        updateSourceVisibility();
        loadModalData();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.bg_card_form_beige);
        }
        return dialog;
    }

    private void loadModalData() {
        viewModel.<ModalPayload>request(repository -> new ModalPayload(
                repository.loadManageCategories(userId),
                repository.categoryAllocation(userId, selectedType)), state -> {
                    if (!isAdded() || state.loading)
                        return;
                    if (state.data != null) {
                        categoriesData = state.data.categoriesData;
                        CategoryAllocationData allocData = state.data.allocationData;
                        usedPercent = allocData == null ? 0 : Math.max(0, allocData.usedPercent);
                    }
                    rebuildProposedList();
                    updateAllocationPreview();
                });
    }

    private void switchType(String type) {
        if (type.equals(selectedType))
            return;
        selectedType = type;
        chosenProposed = null;
        userEditedAlloc = false;
        if (banner != null)
            banner.setVisibility(View.GONE);
        renderTypeTabs();
        viewModel.<CategoryAllocationData>request(
                repository -> repository.categoryAllocation(userId, selectedType),
                state -> {
                    if (!isAdded() || state.loading || state.data == null)
                        return;
                    usedPercent = Math.max(0, state.data.usedPercent);
                    rebuildProposedList();
                    updateAllocationPreview();
                });
    }

    private void renderTypeTabs() {
        boolean isProduct = "product".equals(selectedType);
        tabProduct.setBackgroundResource(isProduct ? R.drawable.bg_btn_select : 0);
        tabProduct.setTextColor(requireContext().getColor(isProduct ? R.color.alabaster : R.color.black));

        tabExpense.setBackgroundResource(!isProduct ? R.drawable.bg_btn_select : 0);
        tabExpense.setTextColor(requireContext().getColor(!isProduct ? R.color.alabaster : R.color.black));
    }

    private void updateSourceVisibility() {
        proposedContainer.setVisibility(manualMode ? View.GONE : View.VISIBLE);
        manualContainer.setVisibility(manualMode ? View.VISIBLE : View.GONE);
    }

    private void rebuildProposedList() {
        proposedList.removeAllViews();
        List<SystemCategoryEntity> unselected = new ArrayList<>();
        if (categoriesData != null && categoriesData.catalogCategories != null) {
            Set<Long> selectedIds = new HashSet<>(categoriesData.selectedCatalogIds);
            for (SystemCategoryEntity sys : categoriesData.catalogCategories) {
                if (!selectedIds.contains(sys.id) && selectedType.equalsIgnoreCase(sys.type)) {
                    unselected.add(sys);
                }
            }
        }
        proposedHeader.setText(getString(R.string.add_cat_proposed_label, unselected.size()));
        if (unselected.isEmpty()) {
            noProposedText.setVisibility(View.VISIBLE);
            chosenProposed = null;
            if (!manualMode) {
                manualMode = true;
                radioManual.setChecked(true);
                updateSourceVisibility();
            }
            return;
        }
        noProposedText.setVisibility(View.GONE);
        if (chosenProposed == null || !unselected.contains(chosenProposed)) {
            chosenProposed = unselected.get(0);
            if (!userEditedAlloc
                    && (allocInput.getText() == null || allocInput.getText().toString().trim().isEmpty())) {
                allocInput.setText(String.valueOf(Math.max(1, chosenProposed.recommendedPercent)));
                userEditedAlloc = false;
            }
        }

        for (SystemCategoryEntity item : unselected) {
            boolean selected = chosenProposed != null && chosenProposed.id == item.id;
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackgroundResource(selected ? R.drawable.bg_chip_green_tint : R.drawable.bg_preview_card);
            int pad = dp(12);
            card.setPadding(pad, dp(10), pad, dp(10));
            card.setClickable(true);
            card.setFocusable(true);

            ImageView icon = new ImageView(requireContext());
            icon.setImageResource(CategoryIconResources.resolve(item.iconName, item.type));
            icon.setImageTintList(ColorStateList.valueOf(requireContext().getColor(R.color.forest)));
            card.addView(icon, new LinearLayout.LayoutParams(dp(22), dp(22)));

            TextView nameView = new TextView(requireContext());
            int nameRes = SystemCategoryCatalog.titleResource(item.key);
            nameView.setText(nameRes != 0 ? getString(nameRes) : item.name);
            nameView.setTextColor(requireContext().getColor(R.color.black));
            nameView.setTextSize(14f);
            nameView.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, -2, 1f);
            nameLp.setMarginStart(dp(10));
            card.addView(nameView, nameLp);

            TextView pctBadge = new TextView(requireContext());
            pctBadge.setText(item.recommendedPercent + "%");
            pctBadge.setTextColor(requireContext().getColor(R.color.black));
            pctBadge.setTextSize(12f);
            pctBadge.setTypeface(null, Typeface.BOLD);
            card.addView(pctBadge);

            card.setOnClickListener(v -> {
                chosenProposed = item;
                if (!userEditedAlloc) {
                    allocInput.setText(String.valueOf(Math.max(1, item.recommendedPercent)));
                    userEditedAlloc = false;
                }
                rebuildProposedList();
                updateAllocationPreview();
            });

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
            rowLp.bottomMargin = dp(6);
            proposedList.addView(card, rowLp);
        }
    }

    private void renderColorSwatches() {
        colorRow.removeAllViews();
        for (String hex : BOTANICAL_SWATCHES) {
            View swatch = new View(requireContext());
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(Color.parseColor(hex));
            boolean active = hex.equalsIgnoreCase(selectedColorHex);
            circle.setStroke(dp(active ? 3 : 1),
                    requireContext().getColor(active ? R.color.black : R.color.input_border));
            swatch.setBackground(circle);
            swatch.setOnClickListener(v -> {
                selectedColorHex = hex;
                renderColorSwatches();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(30), dp(30));
            lp.setMarginEnd(dp(8));
            colorRow.addView(swatch, lp);
        }
    }

    private void updateAllocationPreview() {
        int draft = parseAllocPercent();
        int total = Math.max(0, usedPercent + Math.max(0, draft));
        int available = Math.max(0, 100 - total);
        allocSummary.setText(getString(R.string.add_cat_alloc_summary, total, available));
        allocProgress.setProgress(Math.min(100, total));
    }

    private int parseAllocPercent() {
        String raw = allocInput.getText() == null ? "" : allocInput.getText().toString().trim();
        if (raw.isEmpty())
            return 0;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void saveCategory() {
        if (banner != null)
            banner.setVisibility(View.GONE);
        int percent = parseAllocPercent();
        if (percent < 0 || percent > 100) {
            showAlert(AlertBannerView.Tone.WARNING, getString(R.string.allocation_invalid));
            return;
        }

        if (!manualMode) {
            if (chosenProposed == null) {
                showAlert(AlertBannerView.Tone.WARNING, getString(R.string.add_cat_pick_proposed_err));
                return;
            }
            final long catalogId = chosenProposed.id;
            final int finalPct = percent;
            saveBtn.setEnabled(false);
            viewModel.<Boolean>request(
                    repository -> repository.activateProposedCatalogCategory(userId, catalogId, finalPct),
                    state -> {
                        if (state.loading)
                            return;
                        saveBtn.setEnabled(true);
                        if (Boolean.TRUE.equals(state.data)) {
                            notifySaved();
                            dismiss();
                        } else {
                            showAlert(AlertBannerView.Tone.DANGER, getString(R.string.transaction_saved_error));
                        }
                    });
            return;
        }

        String trimmed = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (trimmed.isEmpty()) {
            showAlert(AlertBannerView.Tone.WARNING, getString(R.string.category_name));
            nameInput.requestFocus();
            return;
        }

        CategoryEntity category = new CategoryEntity();
        category.userId = userId;
        category.name = trimmed;
        category.type = selectedType;
        category.colorHex = selectedColorHex;
        category.isEssential = false;
        category.allocationPercent = percent;

        saveBtn.setEnabled(false);
        viewModel.<Boolean>request(repository -> repository.addCategory(category, userId), state -> {
            if (state.loading)
                return;
            saveBtn.setEnabled(true);
            if (Boolean.TRUE.equals(state.data)) {
                notifySaved();
                dismiss();
            } else {
                showAlert(AlertBannerView.Tone.DANGER, getString(R.string.transaction_saved_error));
            }
        });
    }

    private void showAlert(AlertBannerView.Tone tone, String msg) {
        if (banner != null) {
            banner.setVisibility(View.VISIBLE);
            banner.setAlert(tone, getString(R.string.add_category), msg);
        }
    }

    private void notifySaved() {
        if (getActivity() instanceof Listener) {
            ((Listener) getActivity()).onCategoryCreated();
        }
        getParentFragmentManager().setFragmentResult("masrouf-categories-updated", Bundle.EMPTY);
        Toast.makeText(requireContext(), R.string.categories_updated_success, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + .5f);
    }

    private static final class ModalPayload {
        final ManageCategoriesData categoriesData;
        final CategoryAllocationData allocationData;

        ModalPayload(ManageCategoriesData categoriesData, CategoryAllocationData allocationData) {
            this.categoriesData = categoriesData;
            this.allocationData = allocationData;
        }
    }
}
