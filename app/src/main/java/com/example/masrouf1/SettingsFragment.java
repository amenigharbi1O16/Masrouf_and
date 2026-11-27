/*
 * ============================================================================
 * FICHIER : SettingsFragment.java
 * MISSION : Onglet central « Réglages » unifié regroupant tous les paramètres
 *           du compte sur un seul écran sans sous-menus cachés : édition nom et
 *           adresse Gmail (avec validation temps réel), carte dotation pour
 *           l'Enfant, puces des catégories suivies, accès rapides (budget,
 *           catégories, calendrier, enfants/analyses ou récompenses, mot de passe,
 *           manuel d'utilisation, partage résumé, sauvegarde SQLite locale),
 *           communication (appel/SMS parent ou support), sélecteur de langue
 *           FR/AR/EN et déconnexion.
 * IMPLÉMENTE : Fragment utilisant fragment_settings.xml, item_settings_row.xml,
 *              GmailValidationWatcher, ExecutorService de sauvegarde Room et
 *              construction dynamique de puces de catégories avec retour à la ligne.
 * EFFET À L'ÉCRAN : Formulaire profil, badges catégories, liste d'actions
 *                   configurables selon le rôle (USER / CHILD / PARENT), boutons
 *                   de langue et bouton de déconnexion.
 * COMMUNICATIONS :
 *   ← MainActivity (onglet central tab_settings + ouverture du tiroir)
 *   → MasroufViewModel / MasroufRepository (loadSettingsData, updateName, updateEmail)
 *   → MasroufDatabase.backupDatabase (sauvegarde locale masrouf.db.bak)
 *   → SystemIntents (dialHousehold, shareReceipt, shareText)
 *   → LocaleController.apply (changement de langue FR/AR/EN)
 *   → BudgetPeriodActivity, ManageCategoriesActivity, ChildrenManagementActivity,
 *     FamilyBudgetsAnalyticsActivity, RewardsActivity, ChangePasswordActivity,
 *     UserManualActivity, WelcomeActivity
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufDatabase;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.example.masrouf1.ui.AlertBannerView;
import com.example.masrouf1.ui.CategoryIconResources;
import com.google.android.material.button.MaterialButton;
import com.masrouf.controller.BudgetPeriodActivity;
import com.masrouf.controller.ChangePasswordActivity;
import com.masrouf.controller.ChildrenManagementActivity;
import com.masrouf.controller.InputValidation;
import com.masrouf.controller.ManageCategoriesActivity;
import com.masrouf.controller.SessionStore;
import com.masrouf.controller.WelcomeActivity;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Onglet central « Réglages » unifié : modification profil (nom, Gmail) et mot de passe,
 * résumé des catégories actives, préférences (gestion enfants/dotations pour Parent),
 * Intents implicites (appel, SMS, manuel PDF), langue FR/AR/EN, sauvegarde SQLite locale et déconnexion.
 */
/**
 * Complete self-contained Settings (`Réglages`) screen displaying every profile
 * control,
 * followed categories summary, preferences, communication actions, language
 * picker,
 * backup, and sign-out immediately on the page with zero hidden submenus.
 */
public final class SettingsFragment extends Fragment {
    private static final ExecutorService BACKUP_EXECUTOR = Executors.newSingleThreadExecutor();

    private MasroufViewModel viewModel;
    private long userId;
    private AppRole role;
    private String parentPhone = "";
    private SettingsData currentData;
    private boolean profileInitialized = false;

    private EditText nameInput, emailInput;
    private TextView eyebrowView, emailFeedback, categoriesCountBadge, categoriesEmptyNote, childAllocValue;
    private View childAllocCard;
    private LinearLayout chipsContainer, prefRows, commRows, langRow;
    private MaterialButton saveNameBtn, saveEmailBtn, logoutBtn;
    private AlertBannerView profileFeedback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userId = SessionStore.userId(requireContext());
        role = SessionStore.role(requireContext());
        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(requireContext()))
                .get(MasroufViewModel.class);

        View headerBar = view.findViewById(R.id.settings_header_bar);
        if (requireActivity() instanceof MainActivity) {
            headerBar.setVisibility(View.VISIBLE);
            view.findViewById(R.id.settings_open_drawer).setOnClickListener(
                    v -> ((MainActivity) requireActivity()).openMenu());
        } else {
            headerBar.setVisibility(View.GONE);
        }

        eyebrowView = view.findViewById(R.id.settings_account_eyebrow);
        nameInput = view.findViewById(R.id.settings_name_input);
        saveNameBtn = view.findViewById(R.id.settings_save_name_btn);
        emailInput = view.findViewById(R.id.settings_email_input);
        emailFeedback = view.findViewById(R.id.settings_email_feedback);
        saveEmailBtn = view.findViewById(R.id.settings_save_email_btn);
        profileFeedback = view.findViewById(R.id.settings_profile_feedback);
        childAllocCard = view.findViewById(R.id.settings_child_allocation_card);
        childAllocValue = view.findViewById(R.id.settings_child_allocation_value);
        categoriesCountBadge = view.findViewById(R.id.settings_categories_count_badge);
        chipsContainer = view.findViewById(R.id.settings_categories_chips_container);
        categoriesEmptyNote = view.findViewById(R.id.settings_categories_empty_note);
        prefRows = view.findViewById(R.id.settings_preference_rows);
        commRows = view.findViewById(R.id.settings_communication_rows);
        langRow = view.findViewById(R.id.settings_language_buttons);
        logoutBtn = view.findViewById(R.id.settings_logout_btn);

        eyebrowView.setText(role == AppRole.PARENT
                ? R.string.settings_identity_eyebrow_family
                : (role == AppRole.CHILD
                        ? R.string.settings_identity_eyebrow_child
                        : R.string.settings_identity_eyebrow_personal));

        emailInput.addTextChangedListener(new GmailValidationWatcher(emailFeedback));
        saveNameBtn.setOnClickListener(v -> saveName());
        saveEmailBtn.setOnClickListener(v -> saveEmail());

        view.findViewById(R.id.settings_manage_cats_link).setOnClickListener(
                v -> startActivity(new Intent(requireContext(), ManageCategoriesActivity.class)));

        logoutBtn.setOnClickListener(v -> {
            SessionStore.clear(requireContext());
            Intent welcome = new Intent(requireContext(), WelcomeActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(welcome);
        });

        renderLanguagePills();
        renderActionRows(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSettings();
    }

    private void loadSettings() {
        if (!isAdded() || userId <= 0L)
            return;
        viewModel.<SettingsData>request(
                repository -> repository.loadSettingsData(userId),
                state -> {
                    if (!isAdded() || state.data == null || state.data.user == null)
                        return;
                    currentData = state.data;
                    parentPhone = state.data.parentPhone == null ? "" : state.data.parentPhone;

                    if (!profileInitialized) {
                        nameInput.setText(state.data.user.name == null ? "" : state.data.user.name);
                        emailInput.setText(state.data.user.email == null ? "" : state.data.user.email);
                        profileInitialized = true;
                    }

                    if (role == AppRole.CHILD) {
                        long childBudget = Math.max(
                                state.data.childProfile != null ? state.data.childProfile.allocatedTotalMilli : 0L,
                                Math.max(state.data.activeBudget != null ? state.data.activeBudget.monthlyLimitMilli : 0L,
                                        state.data.totalAllowanceMilli));
                        long receivedTotal = Math.max(state.data.totalAllowanceMilli, childBudget);
                        long spentTotal = state.data.childSpentMilli;
                        long balanceNow = Math.max(0L, state.data.user.balanceMilli);
                        childAllocCard.setVisibility(View.VISIBLE);
                        childAllocValue.setText(CurrencyFormatter.formatMilli(childBudget));
                        TextView childAllocDetails = getView() == null ? null : getView().findViewById(R.id.settings_child_allocation_details);
                        if (childAllocDetails != null) {
                            childAllocDetails.setText(getString(R.string.settings_child_parent_budget_details,
                                    CurrencyFormatter.formatMilli(receivedTotal),
                                    CurrencyFormatter.formatMilli(spentTotal),
                                    CurrencyFormatter.formatMilli(balanceNow)));
                            childAllocDetails.setTextColor(requireContext().getColor(
                                    childBudget > 0L && spentTotal > childBudget ? R.color.danger_text : R.color.ink));
                        }
                    } else {
                        childAllocCard.setVisibility(View.GONE);
                    }

                    renderCategoryChips(state.data.categories, state.data.rawCategories);
                    renderActionRows(state.data.activeBudget);
                });
    }

    /** Valide localement le nom saisi et l'enregistre en base Room via MasroufRepository.updateName(). */
    private void saveName() {
        String value = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (value.isEmpty()) {
            profileFeedback.setVisibility(View.VISIBLE);
            profileFeedback.setAlert(AlertBannerView.Tone.WARNING,
                    getString(R.string.nav_settings),
                    getString(R.string.settings_name_invalid));
            return;
        }
        saveNameBtn.setEnabled(false);
        viewModel.<OperationResult<Boolean>>request(
                repository -> repository.updateName(userId, value),
                result -> {
                    if (!isAdded())
                        return;
                    saveNameBtn.setEnabled(true);
                    if (result.data != null && result.data.succeeded()) {
                        profileFeedback.setVisibility(View.VISIBLE);
                        profileFeedback.setAlert(AlertBannerView.Tone.SUCCESS,
                                getString(R.string.nav_settings),
                                getString(R.string.settings_name_saved));
                        Toast.makeText(requireContext(), R.string.settings_name_saved, Toast.LENGTH_SHORT).show();
                    } else {
                        profileFeedback.setVisibility(View.VISIBLE);
                        profileFeedback.setAlert(AlertBannerView.Tone.DANGER,
                                getString(R.string.nav_settings),
                                getString(R.string.transaction_saved_error));
                    }
                });
    }

    /** Normalise l'adresse Gmail, contrôle son unicité via Room et rafraîchit le retour visuel en direct. */
    private void saveEmail() {
        String value = InputValidation.normalizeGmail(
                emailInput.getText() == null ? "" : emailInput.getText().toString());
        if (!InputValidation.isStrictGmail(value)) {
            emailFeedback.setText(R.string.email_error);
            emailFeedback.setTextColor(requireContext().getColor(R.color.coral));
            return;
        }
        if (!emailInput.getText().toString().equals(value)) {
            emailInput.setText(value);
        }
        saveEmailBtn.setEnabled(false);
        viewModel.<OperationResult<Boolean>>request(
                repository -> repository.updateEmail(userId, value),
                result -> {
                    if (!isAdded())
                        return;
                    saveEmailBtn.setEnabled(true);
                    OperationResult<Boolean> outcome = result.data;
                    if (outcome != null && outcome.succeeded()) {
                        emailFeedback.setText(R.string.email_saved);
                        emailFeedback.setTextColor(requireContext().getColor(R.color.emerald));
                        profileFeedback.setVisibility(View.VISIBLE);
                        profileFeedback.setAlert(AlertBannerView.Tone.SUCCESS,
                                getString(R.string.nav_settings),
                                getString(R.string.email_saved));
                    } else {
                        int errRes = outcome != null && "duplicate".equals(outcome.error)
                                ? R.string.email_already_used
                                : R.string.email_error;
                        emailFeedback.setText(errRes);
                        emailFeedback.setTextColor(requireContext().getColor(R.color.coral));
                    }
                });
    }

    private void renderCategoryChips(List<CategoryPresentationRow> presentations, List<CategoryEntity> rawCats) {
        chipsContainer.removeAllViews();
        int count = presentations != null && !presentations.isEmpty()
                ? presentations.size()
                : (rawCats == null ? 0 : rawCats.size());
        categoriesCountBadge.setText(getString(R.string.settings_followed_count, count));
        if (count == 0) {
            categoriesEmptyNote.setVisibility(View.VISIBLE);
            return;
        }
        categoriesEmptyNote.setVisibility(View.GONE);

        int maxWidth = Math.max(dp(240), getResources().getDisplayMetrics().widthPixels - dp(72));
        LinearLayout currentRow = createChipRow();
        chipsContainer.addView(currentRow);
        int currentRowWidth = 0;

        if (presentations != null && !presentations.isEmpty()) {
            for (CategoryPresentationRow row : presentations) {
                String label = row.catalogKey != null && !row.catalogKey.trim().isEmpty()
                        ? getString(SystemCategoryCatalog.titleResource(row.catalogKey))
                        : (row.name == null ? "" : row.name);
                View chip = buildCategoryChip(label, CategoryIconResources.resolve(row.iconName, row.type));
                chip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int w = chip.getMeasuredWidth() + dp(8);
                if (currentRowWidth > 0 && currentRowWidth + w > maxWidth) {
                    currentRow = createChipRow();
                    chipsContainer.addView(currentRow);
                    currentRowWidth = 0;
                }
                currentRow.addView(chip);
                currentRowWidth += w;
            }
        } else {
            for (CategoryEntity cat : rawCats) {
                View chip = buildCategoryChip(cat.name == null ? "" : cat.name,
                        CategoryIconResources.resolve(null, cat.type));
                chip.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                int w = chip.getMeasuredWidth() + dp(8);
                if (currentRowWidth > 0 && currentRowWidth + w > maxWidth) {
                    currentRow = createChipRow();
                    chipsContainer.addView(currentRow);
                    currentRowWidth = 0;
                }
                currentRow.addView(chip);
                currentRowWidth += w;
            }
        }
    }

    private LinearLayout createChipRow() {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        row.setLayoutParams(lp);
        return row;
    }

    private View buildCategoryChip(String label, int iconRes) {
        LinearLayout chip = new LinearLayout(requireContext());
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPaddingRelative(dp(10), dp(6), dp(12), dp(6));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(requireContext().getColor(R.color.cream));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), requireContext().getColor(R.color.card_border));
        chip.setBackground(bg);

        ImageView icon = new ImageView(requireContext());
        icon.setImageResource(iconRes);
        icon.setColorFilter(requireContext().getColor(R.color.forest));
        chip.addView(icon, new LinearLayout.LayoutParams(dp(15), dp(15)));

        TextView tv = new TextView(requireContext());
        tv.setText(label);
        tv.setTextColor(requireContext().getColor(R.color.forest));
        tv.setTextSize(12f);
        tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
        tp.setMarginStart(dp(6));
        chip.addView(tv, tp);

        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-2, -2);
        cp.setMarginEnd(dp(8));
        chip.setLayoutParams(cp);
        return chip;
    }

    private void renderActionRows(@Nullable BudgetEntity activeBudget) {
        if (!isAdded())
            return;
        prefRows.removeAllViews();
        commRows.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        String budgetDetail = activeBudget != null && activeBudget.monthlyLimitMilli > 0L
                ? CurrencyFormatter.formatMilli(activeBudget.monthlyLimitMilli)
                        + " · "
                        + BudgetPeriodMath.formatRange(activeBudget.periodStart, activeBudget.periodEnd,
                                Locale.getDefault())
                : getString(R.string.settings_budget_detail_none);

        addRow(inflater, prefRows, R.drawable.ic_wallet,
                getString(R.string.budget_this_month), budgetDetail, v -> {
                    Intent intent = new Intent(requireContext(), BudgetPeriodActivity.class);
                    if (activeBudget != null) {
                        intent.putExtra("MODE", "edit").putExtra("BUDGET_ID", activeBudget.id);
                    }
                    startActivity(intent);
                });

        addRow(inflater, prefRows, R.drawable.ic_bag,
                getString(R.string.manage_categories_title),
                getString(R.string.settings_categories_detail),
                v -> startActivity(new Intent(requireContext(), ManageCategoriesActivity.class)));

        addRow(inflater, prefRows, R.drawable.ic_calendar,
                getString(R.string.nav_calendar),
                getString(R.string.settings_calendar_detail),
                v -> {
                    if (requireActivity() instanceof MainActivity) {
                        ((MainActivity) requireActivity()).openCentralTab("calendar");
                    } else {
                        startActivity(new Intent(requireContext(), MainActivity.class)
                                .putExtra(MainActivity.EXTRA_CENTRAL_TAB, "calendar")
                                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
                    }
                });

        if (role == AppRole.PARENT) {
            addRow(inflater, prefRows, R.drawable.ic_people,
                    getString(R.string.page_children),
                    getString(R.string.settings_children_detail),
                    v -> startActivity(new Intent(requireContext(), ChildrenManagementActivity.class)));
            addRow(inflater, prefRows, R.drawable.ic_pie_chart,
                    getString(R.string.nav_budget_analytics),
                    getString(R.string.settings_family_analytics_detail),
                    v -> startActivity(new Intent(requireContext(), FamilyBudgetsAnalyticsActivity.class)));
        } else {
            addRow(inflater, prefRows, R.drawable.ic_trophy,
                    getString(R.string.nav_rewards),
                    getString(R.string.settings_points_detail),
                    v -> startActivity(new Intent(requireContext(), RewardsActivity.class)));
        }

        addRow(inflater, prefRows, R.drawable.ic_lock,
                getString(R.string.change_password_button),
                getString(R.string.settings_password_detail),
                v -> startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));

        addRow(inflater, prefRows, R.drawable.ic_receipt,
                getString(R.string.open_manual),
                getString(R.string.settings_manual_detail),
                v -> startActivity(new Intent(requireContext(), UserManualActivity.class)));

        addRow(inflater, prefRows, R.drawable.ic_more,
                getString(R.string.share_budget_summary),
                getString(R.string.share_summary_chooser),
                v -> shareBudgetSummary());

        addRow(inflater, prefRows, R.drawable.ic_shield,
                getString(R.string.create_local_backup),
                getString(R.string.settings_backup_detail),
                v -> triggerBackup(v));

        boolean isChild = role == AppRole.CHILD;
        addRow(inflater, commRows, R.drawable.ic_people,
                getString(isChild ? R.string.call_parent : R.string.call_support),
                null,
                v -> {
                    if (isChild)
                        SystemIntents.dialHousehold(requireActivity(), parentPhone);
                    else
                        SystemIntents.dialHousehold(requireActivity());
                });

        addRow(inflater, commRows, R.drawable.ic_notifications,
                getString(isChild ? R.string.message_parent : R.string.share_receipt),
                null,
                v -> {
                    if (isChild)
                        SystemIntents.shareReceipt(requireActivity(), parentPhone);
                    else
                        SystemIntents.shareReceipt(requireActivity());
                });
    }

    private void addRow(LayoutInflater inflater, LinearLayout container, int iconRes,
            String title, @Nullable String detail, View.OnClickListener clickListener) {
        View row = inflater.inflate(R.layout.item_settings_row, container, false);
        ImageView icon = row.findViewById(R.id.settings_row_icon);
        TextView titleView = row.findViewById(R.id.settings_row_title);
        TextView detailView = row.findViewById(R.id.settings_row_detail);
        icon.setImageResource(iconRes);
        titleView.setText(title);
        if (detail != null && !detail.trim().isEmpty()) {
            detailView.setText(detail);
            detailView.setVisibility(View.VISIBLE);
        } else {
            detailView.setVisibility(View.GONE);
        }
        row.setOnClickListener(clickListener);
        container.addView(row);
    }

    private void triggerBackup(View anchor) {
        anchor.setEnabled(false);
        android.content.Context appCtx = requireContext().getApplicationContext();
        BACKUP_EXECUTOR.execute(() -> {
            boolean created = false;
            try {
                MasroufDatabase.backupDatabase(appCtx);
                created = true;
            } catch (Exception failure) {
                android.util.Log.e("MasroufBackup", "Local database backup failed", failure);
            }
            boolean success = created;
            if (!isAdded())
                return;
            requireActivity().runOnUiThread(() -> {
                if (!isAdded())
                    return;
                anchor.setEnabled(true);
                Toast.makeText(requireContext(),
                        success ? R.string.backup_created : R.string.backup_failed,
                        Toast.LENGTH_LONG).show();
            });
        });
    }

    private void shareBudgetSummary() {
        String userName = currentData != null && currentData.user != null && currentData.user.name != null
                ? currentData.user.name : getString(R.string.app_name);
        int roleRes = role == AppRole.PARENT ? R.string.settings_identity_eyebrow_family
                : (role == AppRole.CHILD ? R.string.settings_identity_eyebrow_child
                : R.string.settings_identity_eyebrow_personal);
        String roleText = getString(roleRes);
        long allocMilli = currentData != null && currentData.activeBudget != null
                ? currentData.activeBudget.monthlyLimitMilli : 0L;
        long spentMilli = currentData != null && currentData.activeBudget != null
                ? currentData.activeBudget.spentMilli : 0L;
        long remMilli = Math.max(0L, MoneyMath.subtract(allocMilli, spentMilli));
        String body = getString(
                R.string.share_summary_body,
                userName,
                roleText,
                CurrencyFormatter.formatMilli(allocMilli),
                CurrencyFormatter.formatMilli(spentMilli),
                CurrencyFormatter.formatMilli(remMilli));
        SystemIntents.shareText(requireActivity(), getString(R.string.app_name), body);
    }

    private void renderLanguagePills() {
        langRow.removeAllViews();
        String currentTag = AppCompatDelegate.getApplicationLocales().toLanguageTags();
        if (currentTag.isEmpty())
            currentTag = Locale.getDefault().getLanguage();
        String activeLang = currentTag.length() >= 2
                ? currentTag.substring(0, 2).toLowerCase(Locale.ROOT)
                : "fr";

        String[] langTags = { "fr", "ar", "en" };
        String[] langLabels = { "Français", "العربية", "English" };
        for (int i = 0; i < langTags.length; i++) {
            final String tag = langTags[i];
            boolean selected = tag.equals(activeLang);
            MaterialButton btn = new MaterialButton(requireContext());
            btn.setText(langLabels[i]);
            btn.setAllCaps(false);
            btn.setTextSize(13f);
            btn.setCornerRadius(dp(14));
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    requireContext().getColor(selected ? R.color.forest : R.color.mint_light)));
            btn.setTextColor(requireContext().getColor(selected ? R.color.alabaster : R.color.forest));
            btn.setTypeface(null, selected ? Typeface.BOLD : Typeface.NORMAL);
            btn.setOnClickListener(v -> LocaleController.apply(tag));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(48), 1f);
            if (i > 0)
                lp.setMarginStart(dp(6));
            langRow.addView(btn, lp);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
