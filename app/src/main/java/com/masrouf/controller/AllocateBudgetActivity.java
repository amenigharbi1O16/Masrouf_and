/*
 * ============================================================================
 * FICHIER : AllocateBudgetActivity.java
 * MISSION : Écran de répartition du budget par enveloppes de catégories :
 *           pré-remplit les lignes depuis les dépenses du mois précédent (M-1)
 *           ou les pourcentages recommandés du catalogue, synchronise de façon
 *           bidirectionnelle pourcentage (%) et montant (DT) sans boucle
 *           infinie, autorise une réserve non allouée (< 100 %), mais bloque
 *           tout dépassement (> 100 %) avant de sauvegarder le budget et ses
 *           BudgetLineEntity dans SQLite Room.
 * IMPLÉMENTE : AppCompatActivity (activity_allocate_budget.xml) gérant les modes
 *              création, modification, répartition enfant (IS_CHILD) et lecture
 *              seule (READ_ONLY pour budgets clôturés), avec arithmétique en
 *              millimes et points de base (bps).
 * EFFET À L'ÉCRAN : Bandeau récapitulatif Total / Somme allouée / Non alloué,
 *                   boutons « Gérer » et « Répartir 100 % », lignes éditables
 *                   par catégorie et bannière d'erreur si dépassement > 100 %.
 * COMMUNICATIONS :
 *   ← BudgetPeriodActivity, HomeFragment (IS_CHILD), CalendarFragment
 *   → ManageCategoriesActivity (via ActivityResultLauncher pour ajouter/retirer)
 *   → MasroufViewModel / MasroufRepository (loadBudgetAllocation, createBudget,
 *     updateBudget, replaceChildEnvelopes)
 *   → MainActivity (redirection accueil avec bannière de confirmation)
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.BudgetAllocationData;
import com.example.masrouf1.BudgetPeriodMath;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MainActivity;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.R;
import com.example.masrouf1.db.CategoryPresentationRow;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.BudgetLineEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.SystemCategoryCatalog;
import com.example.masrouf1.ui.AlertBannerView;
import com.example.masrouf1.ui.CategoryIconResources;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Écran de répartition du budget par catégorie : pré-remplit depuis le mois précédent (M-1) ou le catalogue,
 * synchronise de façon bidirectionnelle pourcentage (%) et montant (DT) en millimes/points de base (bps),
 * autorise une somme < 100 % mais bloque tout dépassement > 100 % avant enregistrement dans Room.
 */
/**
 * Budget Repartition Screen matching React Native (`allocation-budget.tsx`):
 * - Pre-distributes across selected categories using M-1 spend or predefined catalog percentages
 * - Supports bidirectional independent editing of Percentage (%) and Amount (DT)
 * - Unallocated funds (total < 100%) are allowed; sum > total is blocked cleanly
 * - Saves both `limitMilli` and `plannedPercentBps` in Room SQLite and updates selected budget
 */
public final class AllocateBudgetActivity extends AppCompatActivity {
    private MasroufViewModel viewModel;
    private long userId, totalMilli, start, end, budgetId;
    private boolean childMode, editMode, readOnly;
    private boolean loading = true;
    private boolean syncingInputs = false;

    private BudgetEntity budget;
    private List<BudgetLineEntity> savedLines = new ArrayList<>();
    private List<CategoryEntity> categories = new ArrayList<>();
    private final Map<Long, CategoryPresentationRow> presentationMap = new HashMap<>();
    private final Map<Long, BudgetLineEntity> existingLinesMap = new HashMap<>();
    private final Map<Long, long[]> pendingDraftEdits = new HashMap<>();
    private Map<Long, Long> previousSpending = new HashMap<>();

    private final List<AllocationEntry> entries = new ArrayList<>();

    private LinearLayout rowsContainer;
    private TextView titleView, periodEyebrow, totalInfo, sumInfo, unallocatedInfo, childLockedText, overflowText;
    private AlertBannerView statusBanner;
    private View overflowBanner, actionsRow;
    private MaterialButton manageCatsBtn, autoDistributeBtn, saveBtn;

    private final ActivityResultLauncher<Intent> manageCategoriesLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (!loading && !readOnly) {
                    captureCurrentDrafts();
                    loading = true;
                    loadData(true);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this))
                .get(MasroufViewModel.class);

        childMode = getIntent().getBooleanExtra("IS_CHILD", false);
        userId = SessionStore.userId(this);
        if (userId <= 0L) {
            finish();
            return;
        }
        if (childMode && SessionStore.role(this) != AppRole.CHILD) {
            finish();
            return;
        }

        setContentView(R.layout.activity_allocate_budget);

        findViewById(R.id.allocate_back).setOnClickListener(v -> finish());
        titleView = findViewById(R.id.allocate_title);
        periodEyebrow = findViewById(R.id.allocate_period_eyebrow);
        totalInfo = findViewById(R.id.allocate_total_row);
        sumInfo = findViewById(R.id.allocate_sum_row);
        unallocatedInfo = findViewById(R.id.allocate_unallocated_row);
        childLockedText = findViewById(R.id.allocate_child_locked);
        actionsRow = findViewById(R.id.allocate_actions_row);
        manageCatsBtn = findViewById(R.id.allocate_manage_categories_btn);
        autoDistributeBtn = findViewById(R.id.allocate_auto_distribute_btn);
        rowsContainer = findViewById(R.id.allocate_category_rows);
        statusBanner = findViewById(R.id.allocate_status_banner);
        overflowBanner = findViewById(R.id.allocate_overflow_banner);
        overflowText = findViewById(R.id.allocate_overflow_text);
        saveBtn = findViewById(R.id.allocate_save);

        readOnly = getIntent().getBooleanExtra("READ_ONLY", false);
        if (!childMode) {
            start = getIntent().getLongExtra("START", 0L);
            end = getIntent().getLongExtra("END", 0L);
            totalMilli = getIntent().getLongExtra("TOTAL", 0L);
            budgetId = getIntent().getLongExtra("BUDGET_ID", 0L);
            editMode = budgetId > 0L;
        } else {
            budgetId = getIntent().getLongExtra("BUDGET_ID", 0L);
        }

        titleView.setText(childMode ? R.string.partition_child_budget : R.string.allocate_budget_title);
        childLockedText.setVisibility(childMode ? View.VISIBLE : View.GONE);

        manageCatsBtn.setOnClickListener(v -> {
            if (readOnly) return;
            manageCategoriesLauncher.launch(new Intent(this, ManageCategoriesActivity.class));
        });
        autoDistributeBtn.setOnClickListener(v -> {
            if (!readOnly) applyAutoDistribute100();
        });
        saveBtn.setOnClickListener(v -> {
            if (readOnly) {
                finish();
            } else {
                persistAllocations();
            }
        });

        loadData(false);
    }

    private void loadData(boolean preservePendingDrafts) {
        long requestedStart = start;
        viewModel.<BudgetAllocationData>request(
                repository -> repository.loadBudgetAllocation(userId, childMode, editMode, budgetId, requestedStart),
                state -> {
                    if (isFinishing() || isDestroyed()) return;
                    BudgetAllocationData data = state.data;
                    if (data == null || !data.authorized) {
                        Toast.makeText(this, childMode ? R.string.no_child_budget : R.string.transaction_saved_error,
                                Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    List<CategoryEntity> loadedCats = data.categories;
                    BudgetEntity loadedBudget = data.budget;
                    if (childMode) {
                        if (data.childProfile == null || loadedBudget == null) {
                            Toast.makeText(this, R.string.no_child_budget, Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        totalMilli = data.childProfile.allocatedTotalMilli > 0
                                ? data.childProfile.allocatedTotalMilli
                                : loadedBudget.monthlyLimitMilli;
                        start = loadedBudget.periodStart;
                        end = loadedBudget.periodEnd;
                    } else if (editMode && loadedBudget != null) {
                        if (totalMilli <= 0L) totalMilli = loadedBudget.monthlyLimitMilli;
                        if (start <= 0L) start = loadedBudget.periodStart;
                        if (end <= 0L) end = loadedBudget.periodEnd;
                        if ("CLOSED".equalsIgnoreCase(loadedBudget.status)
                                || BudgetPeriodMath.isPastPeriodEnd(loadedBudget.periodEnd, Calendar.getInstance())) {
                            readOnly = true;
                        }
                    }

                    if (totalMilli <= 0L) {
                        Toast.makeText(this, R.string.budget_total_required, Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }

                    budget = loadedBudget;
                    categories = loadedCats == null ? new ArrayList<>() : loadedCats;
                    savedLines = data.lines == null ? new ArrayList<>() : new ArrayList<>(data.lines);
                    previousSpending = data.previousSpending == null ? new HashMap<>() : data.previousSpending;

                    presentationMap.clear();
                    if (data.presentations != null) {
                        for (CategoryPresentationRow row : data.presentations) {
                            presentationMap.put(row.categoryId, row);
                        }
                    }

                    existingLinesMap.clear();
                    for (BudgetLineEntity line : savedLines) {
                        existingLinesMap.put(line.categoryId, line);
                    }

                    if (start > 0L && end >= start) {
                        periodEyebrow.setText(getString(
                                R.string.allocate_period_eyebrow,
                                BudgetPeriodMath.formatRange(start, end, Locale.getDefault()),
                                BudgetPeriodMath.daysBetweenInclusive(start, end)));
                    }
                    totalInfo.setText(CurrencyFormatter.formatMilli(totalMilli));
                    configureModeButtons();

                    Map<Long, long[]> initialValues = buildInitialAllocations(preservePendingDrafts);
                    pendingDraftEdits.clear();
                    renderRows(initialValues);
                    loading = false;
                    recalculate();
                });
    }

    private void configureModeButtons() {
        if (readOnly) {
            actionsRow.setVisibility(View.GONE);
            saveBtn.setText(R.string.back);
            statusBanner.setAlert(AlertBannerView.Tone.WARNING,
                    getString(R.string.period_closed_banner_title),
                    getString(R.string.period_closed_banner_detail));
        } else {
            actionsRow.setVisibility(View.VISIBLE);
            manageCatsBtn.setVisibility(childMode ? View.GONE : View.VISIBLE);
            saveBtn.setText(childMode
                    ? R.string.save_allocation_cta
                    : (editMode ? R.string.save_allocation_cta : R.string.create_my_budget_cta));
        }
    }

    private void captureCurrentDrafts() {
        pendingDraftEdits.clear();
        for (AllocationEntry entry : entries) {
            long amt = parseAmountOrZero(entry.amountInput.getText().toString());
            int bps = parsePercentOrZero(entry.percentInput.getText().toString(), amt);
            pendingDraftEdits.put(entry.category.id, new long[]{amt, bps});
        }
    }

    /**
     * Computes initial [amountMilli, pctBps] pairs:
     * - If returning from Gérer, preserves in-progress edits (and initializes new categories cleanly at 0)
     * - If editing existing lines, preserves each line's amount + plannedPercentBps
     * - On new creation, uses M-1 spending ratios if available, otherwise DB catalog/custom initial percentages
     */
    private Map<Long, long[]> buildInitialAllocations(boolean usePendingDrafts) {
        Map<Long, long[]> map = new HashMap<>();
        if (usePendingDrafts && !pendingDraftEdits.isEmpty()) {
            for (CategoryEntity cat : categories) {
                long[] savedDraft = pendingDraftEdits.get(cat.id);
                map.put(cat.id, savedDraft != null ? savedDraft : new long[]{0L, 0L});
            }
            return map;
        }

        if (!existingLinesMap.isEmpty()) {
            long oldLineSum = 0L;
            for (CategoryEntity cat : categories) {
                BudgetLineEntity line = existingLinesMap.get(cat.id);
                if (line != null) oldLineSum = MoneyMath.add(oldLineSum, Math.max(0L, line.limitMilli));
            }
            boolean scaleDownToFitNewTotal = oldLineSum > totalMilli && budget != null && budget.monthlyLimitMilli != totalMilli;
            for (CategoryEntity cat : categories) {
                BudgetLineEntity line = existingLinesMap.get(cat.id);
                if (line != null) {
                    int bps = line.plannedPercentBps > 0
                            ? line.plannedPercentBps
                            : BudgetPeriodMath.percentBpsFromAmount(
                                    budget != null && budget.monthlyLimitMilli > 0L ? budget.monthlyLimitMilli : totalMilli,
                                    line.limitMilli);
                    long amt = scaleDownToFitNewTotal
                            ? BudgetPeriodMath.amountFromPercentBps(totalMilli, Math.min(10_000, bps))
                            : line.limitMilli;
                    map.put(cat.id, new long[]{amt, bps});
                } else {
                    map.put(cat.id, new long[]{0L, 0L});
                }
            }
            return map;
        }

        // Check M-1 history first
        boolean hasHistory = false;
        for (CategoryEntity cat : categories) {
            Long prev = previousSpending.get(cat.id);
            if (prev != null && prev > 0L) {
                hasHistory = true;
                break;
            }
        }
        if (hasHistory) {
            ArrayList<Long> weights = new ArrayList<>(categories.size());
            for (CategoryEntity cat : categories) {
                Long prev = previousSpending.get(cat.id);
                weights.add(prev == null ? 0L : Math.max(0L, prev));
            }
            List<Long> allocated = MoneyMath.allocate(totalMilli, weights);
            for (int i = 0; i < categories.size(); i++) {
                long amt = allocated.get(i);
                int bps = BudgetPeriodMath.percentBpsFromAmount(totalMilli, amt);
                map.put(categories.get(i).id, new long[]{amt, bps});
            }
            return map;
        }

        // New budget without M-1 history: apply each category's DB predefined initial percentage
        int[] defaultPcts = new int[categories.size()];
        int pctSum = 0;
        for (int i = 0; i < categories.size(); i++) {
            int p = resolvePredefinedPercent(categories.get(i));
            defaultPcts[i] = p;
            pctSum += p;
        }

        if (pctSum > 100) {
            // Normalize cleanly if combined predefined weights exceed 100%
            ArrayList<Long> weights = new ArrayList<>(categories.size());
            for (int p : defaultPcts) weights.add((long) Math.max(1, p));
            List<Long> allocated = MoneyMath.allocate(totalMilli, weights);
            for (int i = 0; i < categories.size(); i++) {
                long amt = allocated.get(i);
                int bps = BudgetPeriodMath.percentBpsFromAmount(totalMilli, amt);
                map.put(categories.get(i).id, new long[]{amt, bps});
            }
        } else {
            for (int i = 0; i < categories.size(); i++) {
                int bps = Math.max(0, defaultPcts[i]) * 100;
                long amt = BudgetPeriodMath.amountFromPercentBps(totalMilli, bps);
                map.put(categories.get(i).id, new long[]{amt, bps});
            }
        }
        return map;
    }

    /** Full-budget automatic distribution (100%) using M-1 history or catalog initial weights. */
    private void applyAutoDistribute100() {
        if (entries.isEmpty() || totalMilli <= 0L) return;

        boolean hasHistory = false;
        for (AllocationEntry entry : entries) {
            Long prev = previousSpending.get(entry.category.id);
            if (prev != null && prev > 0L) {
                hasHistory = true;
                break;
            }
        }

        ArrayList<Long> weights = new ArrayList<>(entries.size());
        for (AllocationEntry entry : entries) {
            if (hasHistory) {
                Long prev = previousSpending.get(entry.category.id);
                weights.add(prev == null ? 0L : Math.max(0L, prev));
            } else {
                weights.add((long) Math.max(1, resolvePredefinedPercent(entry.category)));
            }
        }

        List<Long> distributed = MoneyMath.allocate(totalMilli, weights);
        syncingInputs = true;
        for (int i = 0; i < entries.size(); i++) {
            AllocationEntry entry = entries.get(i);
            long amt = distributed.get(i);
            int bps = BudgetPeriodMath.percentBpsFromAmount(totalMilli, amt);
            entry.amountInput.setText(formatPlainMilli(amt));
            entry.percentInput.setText(BudgetPeriodMath.formatPercentFromBps(bps));
        }
        syncingInputs = false;
        recalculate();
    }

    private void renderRows(Map<Long, long[]> initialValues) {
        rowsContainer.removeAllViews();
        entries.clear();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (CategoryEntity category : categories) {
            View rowView = inflater.inflate(R.layout.item_allocation_row, rowsContainer, false);
            ImageView icon = rowView.findViewById(R.id.alloc_cat_icon);
            TextView name = rowView.findViewById(R.id.alloc_cat_name);
            TextView hint = rowView.findViewById(R.id.alloc_cat_default_hint);
            EditText percentInput = rowView.findViewById(R.id.alloc_cat_percent_input);
            EditText amountInput = rowView.findViewById(R.id.alloc_cat_amount_input);

            CategoryPresentationRow pres = presentationMap.get(category.id);
            String iconName = pres != null ? pres.iconName : null;
            icon.setImageResource(CategoryIconResources.resolve(iconName, category.type));
            name.setText(localizedCategoryName(category, pres));

            int predefinedPct = resolvePredefinedPercent(category);
            if (predefinedPct > 0) {
                hint.setText(getString(R.string.allocate_default_pct_info, predefinedPct));
                hint.setVisibility(View.VISIBLE);
            } else {
                hint.setText("product".equalsIgnoreCase(category.type)
                        ? R.string.type_articles_produits
                        : R.string.type_depenses_services);
            }

            long[] pair = initialValues.get(category.id);
            long initAmt = pair == null ? 0L : Math.max(0L, pair[0]);
            int initBps = pair == null ? 0 : (int) Math.max(0L, pair[1]);

            syncingInputs = true;
            if (initAmt > 0L || initBps > 0) {
                amountInput.setText(formatPlainMilli(initAmt));
                percentInput.setText(BudgetPeriodMath.formatPercentFromBps(initBps));
            } else {
                amountInput.setText("");
                percentInput.setText("");
            }
            syncingInputs = false;

            if (readOnly) {
                percentInput.setEnabled(false);
                amountInput.setEnabled(false);
            } else {
                percentInput.setEnabled(true);
                amountInput.setEnabled(true);
            }

            AllocationEntry entry = new AllocationEntry(category, percentInput, amountInput);
            entries.add(entry);

            percentInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (loading || syncingInputs || readOnly) return;
                    onPercentEdited(entry, s == null ? "" : s.toString());
                }
                @Override public void afterTextChanged(Editable s) { }
            });

            amountInput.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (loading || syncingInputs || readOnly) return;
                    onAmountEdited(entry, s == null ? "" : s.toString());
                }
                @Override public void afterTextChanged(Editable s) { }
            });

            rowsContainer.addView(rowView);
        }
    }

    private void onPercentEdited(AllocationEntry entry, String rawPercent) {
        syncingInputs = true;
        String trimmed = rawPercent.trim();
        if (trimmed.isEmpty()) {
            entry.amountInput.setText("");
            entry.percentInvalid = false;
            entry.amountInvalid = false;
        } else {
            try {
                int bps = BudgetPeriodMath.parsePercentToBps(trimmed);
                long computedMilli = BudgetPeriodMath.amountFromPercentBps(totalMilli, bps);
                entry.amountInput.setText(formatPlainMilli(computedMilli));
                entry.percentInvalid = false;
                entry.amountInvalid = false;
            } catch (NumberFormatException | ArithmeticException ex) {
                entry.percentInvalid = true;
            }
        }
        syncingInputs = false;
        recalculate();
    }

    private void onAmountEdited(AllocationEntry entry, String rawAmount) {
        syncingInputs = true;
        String trimmed = rawAmount.trim();
        if (trimmed.isEmpty()) {
            entry.percentInput.setText("");
            entry.amountInvalid = false;
            entry.percentInvalid = false;
        } else {
            try {
                long milli = MoneyInput.toMilli(trimmed);
                if (milli < 0L) throw new NumberFormatException("negative");
                int bps = BudgetPeriodMath.percentBpsFromAmount(totalMilli, milli);
                entry.percentInput.setText(BudgetPeriodMath.formatPercentFromBps(bps));
                entry.amountInvalid = false;
                entry.percentInvalid = false;
            } catch (NumberFormatException | ArithmeticException ex) {
                entry.amountInvalid = true;
            }
        }
        syncingInputs = false;
        recalculate();
    }

    private void recalculate() {
        long sumMilli = 0L;
        int sumPctBps = 0;
        boolean invalid = false;

        for (AllocationEntry entry : entries) {
            if (entry.amountInvalid || entry.percentInvalid) {
                invalid = true;
                continue;
            }
            try {
                String rawAmt = entry.amountInput.getText().toString().trim();
                long amt = rawAmt.isEmpty() ? 0L : MoneyInput.toMilli(rawAmt);
                if (amt < 0L) {
                    invalid = true;
                } else {
                    sumMilli = MoneyMath.add(sumMilli, amt);
                }
            } catch (NumberFormatException | ArithmeticException ex) {
                invalid = true;
            }

            try {
                String rawPct = entry.percentInput.getText().toString().trim();
                int bps = rawPct.isEmpty() ? 0 : BudgetPeriodMath.parsePercentToBps(rawPct);
                sumPctBps += Math.max(0, bps);
            } catch (NumberFormatException | ArithmeticException ex) {
                invalid = true;
            }
        }

        long unallocated = MoneyMath.subtract(totalMilli, sumMilli);
        sumInfo.setText(getString(
                R.string.allocate_sum_with_pct,
                CurrencyFormatter.formatMilli(sumMilli),
                BudgetPeriodMath.formatPercentFromBps(sumPctBps)));
        unallocatedInfo.setText(CurrencyFormatter.formatMilli(unallocated));
        unallocatedInfo.setTextColor(getColor(unallocated < 0L ? R.color.coral : R.color.emerald));

        boolean overflow = invalid || sumMilli > totalMilli;
        if (overflow) {
            statusBanner.setAlert(AlertBannerView.Tone.NONE, "", "");
            overflowBanner.setVisibility(View.VISIBLE);
            overflowText.setText(invalid
                    ? getString(R.string.money_invalid)
                    : getString(R.string.allocate_overflow_detail, CurrencyFormatter.formatMilli(sumMilli - totalMilli)));
            saveBtn.setEnabled(readOnly);
        } else {
            overflowBanner.setVisibility(View.GONE);
            if (!readOnly) {
                statusBanner.setAlert(AlertBannerView.Tone.SUCCESS,
                        getString(R.string.allocate_unassigned_ok_title),
                        getString(R.string.allocate_unassigned_ok_detail));
            }
            saveBtn.setEnabled(true);
        }
    }

    private void persistAllocations() {
        ArrayList<BudgetLineEntity> lines = new ArrayList<>();
        long sum = 0L;
        try {
            for (AllocationEntry entry : entries) {
                String rawAmt = entry.amountInput.getText().toString().trim();
                String rawPct = entry.percentInput.getText().toString().trim();

                int bps = rawPct.isEmpty() ? 0 : BudgetPeriodMath.parsePercentToBps(rawPct);
                long amount;
                if (!rawAmt.isEmpty()) {
                    amount = MoneyInput.toMilli(rawAmt);
                } else if (bps > 0) {
                    amount = BudgetPeriodMath.amountFromPercentBps(totalMilli, bps);
                } else {
                    amount = 0L;
                }
                if (amount < 0L) throw new NumberFormatException("negative");
                if (rawPct.isEmpty() && amount > 0L) {
                    bps = BudgetPeriodMath.percentBpsFromAmount(totalMilli, amount);
                }

                sum = MoneyMath.add(sum, amount);
                BudgetLineEntity line = new BudgetLineEntity();
                line.categoryId = entry.category.id;
                line.limitMilli = amount;
                line.plannedPercentBps = Math.max(0, bps);

                BudgetLineEntity previous = existingLinesMap.get(entry.category.id);
                line.spentMilli = previous == null ? 0L : previous.spentMilli;
                lines.add(line);
            }
        } catch (NumberFormatException | ArithmeticException ex) {
            Toast.makeText(this, R.string.money_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        if (sum > totalMilli) {
            recalculate();
            return;
        }

        saveBtn.setEnabled(false);
        if (childMode) {
            viewModel.<Boolean>request(
                    repository -> repository.replaceChildEnvelopes(budgetId, userId, lines),
                    state -> {
                        if (Boolean.TRUE.equals(state.data)) {
                            SessionStore.setSelectedBudgetId(this, userId, budgetId);
                        }
                        finishToHome(Boolean.TRUE.equals(state.data), false);
                    });
            return;
        }

        if (budget == null) {
            budget = new BudgetEntity();
            budget.userId = userId;
            budget.name = getString(R.string.monthly_budget_amount);
            budget.spentMilli = 0L;
            budget.status = "ACTIVE";
        }
        budget.monthlyLimitMilli = totalMilli;
        budget.periodStart = start;
        budget.periodEnd = end;
        budget.status = "ACTIVE";

        if (editMode) {
            viewModel.<Boolean>request(
                    repository -> repository.updateBudget(budget, lines),
                    state -> {
                        boolean ok = Boolean.TRUE.equals(state.data);
                        if (ok) SessionStore.setSelectedBudgetId(this, userId, budget.id);
                        finishToHome(ok, false);
                    });
        } else {
            viewModel.<Long>request(
                    repository -> repository.createBudget(budget, lines),
                    state -> {
                        long createdId = state.data == null ? -1L : state.data;
                        if (createdId == -2L) {
                            finishToHome(false, true);
                            return;
                        }
                        boolean ok = createdId > 0L;
                        if (ok) SessionStore.setSelectedBudgetId(this, userId, createdId);
                        finishToHome(ok, false);
                    });
        }
    }

    private void finishToHome(boolean saved, boolean overlapConflict) {
        if (!saved) {
            saveBtn.setEnabled(true);
            if (overlapConflict) {
                statusBanner.setAlert(AlertBannerView.Tone.DANGER,
                        getString(R.string.period_overlap_title),
                        getString(R.string.period_overlap_save_error));
                Toast.makeText(this, R.string.period_overlap_save_error, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, R.string.transaction_saved_error, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        String msg = getString(R.string.budget_repartition_saved);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Intent home = new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_CENTRAL_TAB, "home")
                .putExtra("EXTRA_SUCCESS_TITLE", getString(R.string.allocate_budget_title))
                .putExtra("EXTRA_SUCCESS_MESSAGE", msg)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(home);
        finish();
    }

    private int resolvePredefinedPercent(CategoryEntity category) {
        CategoryPresentationRow pres = presentationMap.get(category.id);
        if (pres != null) {
            if (pres.recommendedPercent > 0) return pres.recommendedPercent;
            if (pres.allocationPercent > 0) return pres.allocationPercent;
        }
        return Math.max(0, category.allocationPercent);
    }

    private String localizedCategoryName(CategoryEntity cat, @Nullable CategoryPresentationRow pres) {
        if (pres != null && pres.catalogKey != null && !pres.catalogKey.trim().isEmpty()) {
            return getString(SystemCategoryCatalog.titleResource(pres.catalogKey));
        }
        return cat.name == null ? "" : cat.name;
    }

    private String formatPlainMilli(long milli) {
        return CurrencyFormatter.formatMilli(milli)
                .replace(" DT", "")
                .replace(" ", "")
                .replace(',', '.');
    }

    private long parseAmountOrZero(String raw) {
        try {
            String t = raw == null ? "" : raw.trim();
            return t.isEmpty() ? 0L : Math.max(0L, MoneyInput.toMilli(t));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private int parsePercentOrZero(String raw, long amountFallback) {
        try {
            String t = raw == null ? "" : raw.trim();
            if (!t.isEmpty()) return BudgetPeriodMath.parsePercentToBps(t);
        } catch (Exception ignored) { }
        return BudgetPeriodMath.percentBpsFromAmount(totalMilli, amountFallback);
    }

    /** Public helper kept compatible with unit tests & M-1 historical calculation. */
    public static Map<Long, Long> computeAutomatedBudgetAllocations(List<CategoryEntity> categories,
                                                                    Map<Long, Long> previousSpend,
                                                                    long totalMilli) {
        HashMap<Long, Long> result = new HashMap<>();
        if (categories == null || categories.isEmpty() || totalMilli <= 0L) return result;
        boolean hasHistory = false;
        for (CategoryEntity category : categories) {
            Long prev = previousSpend == null ? null : previousSpend.get(category.id);
            if (prev != null && prev > 0L) {
                hasHistory = true;
                break;
            }
        }
        if (hasHistory) {
            ArrayList<Long> weights = new ArrayList<>(categories.size());
            for (CategoryEntity category : categories) {
                Long prev = previousSpend == null ? null : previousSpend.get(category.id);
                weights.add(prev == null ? 0L : Math.max(0L, prev));
            }
            List<Long> allocation = MoneyMath.allocate(totalMilli, weights);
            for (int i = 0; i < categories.size(); i++) {
                result.put(categories.get(i).id, allocation.get(i));
            }
            return result;
        }

        ArrayList<Long> weights = new ArrayList<>(categories.size());
        for (CategoryEntity cat : categories) {
            int w = cat.allocationPercent > 0 ? cat.allocationPercent : defaultWeightFallback(cat.name);
            weights.add((long) Math.max(1, w));
        }
        List<Long> allocation = MoneyMath.allocate(totalMilli, weights);
        for (int i = 0; i < categories.size(); i++) {
            result.put(categories.get(i).id, allocation.get(i));
        }
        return result;
    }

    private static int defaultWeightFallback(String name) {
        String value = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (value.contains("aliment") || value.contains("course") || value.contains("food") || value.contains("grocer")) return 33;
        if (value.contains("facture") || value.contains("bill")) return 14;
        if (value.contains("transport")) return 10;
        if (value.contains("sant") || value.contains("health")) return 5;
        if (value.contains("maison") || value.contains("home")) return 5;
        if (value.contains("loisir") || value.contains("leisure")) return 5;
        if (value.contains("vêt") || value.contains("cloth")) return 4;
        if (value.contains("hyg")) return 4;
        if (value.contains("étud") || value.contains("educ")) return 4;
        if (value.contains("épargn") || value.contains("sav")) return 4;
        return 3;
    }

    private static final class AllocationEntry {
        final CategoryEntity category;
        final EditText percentInput;
        final EditText amountInput;
        boolean percentInvalid;
        boolean amountInvalid;

        AllocationEntry(CategoryEntity category, EditText percentInput, EditText amountInput) {
            this.category = category;
            this.percentInput = percentInput;
            this.amountInput = amountInput;
        }
    }
}
