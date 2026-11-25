package com.masrouf.controller;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.BudgetPeriodMath;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.ui.AlertBannerView;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Dedicated Budget Period & Calendar Selection page matching React Native (`budget-mois.tsx`):
 * - Defaults to 1st day -> last day of current month (or passed month from CalendarFragment)
 * - Complete interactive calendar UI with month navigation + start/end range selection
 * - Blocks past periods for new budget creation, keeps historical periods read-only for consultation
 * - Validates ISO format, chronological order, positive amount, and overlapping/duplicate periods
 */
public final class BudgetPeriodActivity extends AppCompatActivity {
    private static final String STATE_START = "bp_start";
    private static final String STATE_END = "bp_end";
    private static final String STATE_TOTAL = "bp_total";
    private static final String STATE_CAL_YEAR = "bp_cal_year";
    private static final String STATE_CAL_MONTH = "bp_cal_month";
    private static final String STATE_PICK_START = "bp_pick_start";

    private MasroufViewModel viewModel;
    private long userId;
    private long requestedBudgetId;
    private boolean editMode;
    private boolean readOnly;
    private boolean pickingStart = true;
    private boolean programmaticTextUpdate = false;

    private int calendarYear;
    private int calendarMonth; // 0..11

    private BudgetEntity editingBudget;
    private List<BudgetEntity> userBudgets = new ArrayList<>();

    private EditText startInput, endInput, totalInput;
    private TextView titleView, errorText, calMonthLabel;
    private AlertBannerView alertBanner, topInfoBanner, daysBanner;
    private MaterialButton editExistingBtn, toggleCalBtn, modeStartBtn, modeEndBtn, continueBtn, cancelBtn;
    private View calendarCard;
    private LinearLayout calWeekdays, calDays;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_period);

        userId = SessionStore.userId(this);
        if (userId <= 0L) {
            finish();
            return;
        }

        viewModel = new androidx.lifecycle.ViewModelProvider(this, new MasroufViewModelFactory(this))
                .get(MasroufViewModel.class);

        titleView = findViewById(R.id.budget_period_title);
        startInput = findViewById(R.id.budget_start_date);
        endInput = findViewById(R.id.budget_end_date);
        totalInput = findViewById(R.id.budget_total_input);
        errorText = findViewById(R.id.budget_period_error);
        alertBanner = findViewById(R.id.budget_period_alert_banner);
        topInfoBanner = findViewById(R.id.budget_period_top_info);
        daysBanner = findViewById(R.id.budget_period_days_banner);
        editExistingBtn = findViewById(R.id.budget_period_edit_existing_btn);
        toggleCalBtn = findViewById(R.id.budget_toggle_calendar_btn);
        calendarCard = findViewById(R.id.budget_calendar_card);
        modeStartBtn = findViewById(R.id.calendar_pick_mode_start);
        modeEndBtn = findViewById(R.id.calendar_pick_mode_end);
        calMonthLabel = findViewById(R.id.budget_cal_month_label);
        calWeekdays = findViewById(R.id.budget_cal_weekdays);
        calDays = findViewById(R.id.budget_cal_days);
        continueBtn = findViewById(R.id.budget_period_continue);
        cancelBtn = findViewById(R.id.budget_period_cancel);

        findViewById(R.id.budget_period_back).setOnClickListener(v -> finish());
        cancelBtn.setOnClickListener(v -> finish());

        requestedBudgetId = getIntent().getLongExtra("BUDGET_ID", 0L);
        editMode = "edit".equalsIgnoreCase(getIntent().getStringExtra("MODE")) || requestedBudgetId > 0L;
        readOnly = getIntent().getBooleanExtra("READ_ONLY", false);

        Calendar now = Calendar.getInstance();
        long defaultStart = getIntent().getLongExtra("DEFAULT_START",
                BudgetPeriodMath.startOfMonthMillis(now.get(Calendar.YEAR), now.get(Calendar.MONTH)));
        long defaultEnd = getIntent().getLongExtra("DEFAULT_END",
                BudgetPeriodMath.endOfMonthMillis(now.get(Calendar.YEAR), now.get(Calendar.MONTH)));

        if (savedInstanceState != null) {
            setIsoDatesSilently(
                    savedInstanceState.getString(STATE_START, BudgetPeriodMath.formatIsoDate(defaultStart)),
                    savedInstanceState.getString(STATE_END, BudgetPeriodMath.formatIsoDate(defaultEnd)));
            totalInput.setText(savedInstanceState.getString(STATE_TOTAL, ""));
            calendarYear = savedInstanceState.getInt(STATE_CAL_YEAR, now.get(Calendar.YEAR));
            calendarMonth = savedInstanceState.getInt(STATE_CAL_MONTH, now.get(Calendar.MONTH));
            pickingStart = savedInstanceState.getBoolean(STATE_PICK_START, true);
        } else {
            setIsoDatesSilently(
                    BudgetPeriodMath.formatIsoDate(defaultStart),
                    BudgetPeriodMath.formatIsoDate(defaultEnd));
            Calendar seedCal = Calendar.getInstance();
            seedCal.setTimeInMillis(defaultStart);
            calendarYear = seedCal.get(Calendar.YEAR);
            calendarMonth = seedCal.get(Calendar.MONTH);
        }

        renderWeekdays();
        updatePickerModeButtons();

        toggleCalBtn.setOnClickListener(v -> {
            if (readOnly) return;
            boolean show = calendarCard.getVisibility() != View.VISIBLE;
            calendarCard.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) renderInteractiveCalendar();
        });
        findViewById(R.id.budget_start_cal_btn).setOnClickListener(v -> focusCalendarEndpoint(true));
        findViewById(R.id.budget_end_cal_btn).setOnClickListener(v -> focusCalendarEndpoint(false));
        startInput.setOnClickListener(v -> focusCalendarEndpoint(true));
        endInput.setOnClickListener(v -> focusCalendarEndpoint(false));

        modeStartBtn.setOnClickListener(v -> focusCalendarEndpoint(true));
        modeEndBtn.setOnClickListener(v -> focusCalendarEndpoint(false));

        findViewById(R.id.budget_cal_prev_month).setOnClickListener(v -> {
            if (calendarMonth == 0) {
                calendarMonth = 11;
                calendarYear--;
            } else {
                calendarMonth--;
            }
            renderInteractiveCalendar();
        });
        findViewById(R.id.budget_cal_next_month).setOnClickListener(v -> {
            if (calendarMonth == 11) {
                calendarMonth = 0;
                calendarYear++;
            } else {
                calendarMonth++;
            }
            renderInteractiveCalendar();
        });

        TextWatcher liveWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!programmaticTextUpdate) {
                    errorText.setVisibility(View.GONE);
                    refreshLivePeriodState(false);
                    renderInteractiveCalendar();
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        };
        startInput.addTextChangedListener(liveWatcher);
        endInput.addTextChangedListener(liveWatcher);
        totalInput.addTextChangedListener(liveWatcher);

        continueBtn.setOnClickListener(v -> handleContinue());

        loadBudgetsFromDb();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_START, startInput.getText().toString().trim());
        outState.putString(STATE_END, endInput.getText().toString().trim());
        outState.putString(STATE_TOTAL, totalInput.getText().toString().trim());
        outState.putInt(STATE_CAL_YEAR, calendarYear);
        outState.putInt(STATE_CAL_MONTH, calendarMonth);
        outState.putBoolean(STATE_PICK_START, pickingStart);
    }

    private void loadBudgetsFromDb() {
        long targetId = requestedBudgetId;
        boolean loadActiveFallback = editMode && targetId <= 0L;
        viewModel.<LoadedPeriodState>request(repository -> {
            List<BudgetEntity> all = repository.budgetsForUser(userId);
            BudgetEntity target = null;
            if (targetId > 0L) {
                target = repository.budget(targetId, userId);
            } else if (loadActiveFallback) {
                target = repository.currentOrActiveBudget(userId);
            }
            return new LoadedPeriodState(all, target);
        }, state -> {
            if (isFinishing() || isDestroyed() || state.data == null) return;
            userBudgets = state.data.all == null ? Collections.emptyList() : state.data.all;
            BudgetEntity target = state.data.target;
            if (editMode && target != null) {
                editingBudget = target;
                setIsoDatesSilently(
                        BudgetPeriodMath.formatIsoDate(target.periodStart),
                        BudgetPeriodMath.formatIsoDate(target.periodEnd));
                String rawTotal = CurrencyFormatter.formatMilli(target.monthlyLimitMilli)
                        .replace(" DT", "").replace(" ", "").replace(',', '.');
                programmaticTextUpdate = true;
                totalInput.setText(rawTotal);
                programmaticTextUpdate = false;

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(target.periodStart);
                calendarYear = c.get(Calendar.YEAR);
                calendarMonth = c.get(Calendar.MONTH);

                if ("CLOSED".equalsIgnoreCase(target.status)
                        || BudgetPeriodMath.isPastPeriodEnd(target.periodEnd, Calendar.getInstance())) {
                    readOnly = true;
                }
            }
            applyModeUi();
            refreshLivePeriodState(false);
            renderInteractiveCalendar();
        });
    }

    private void applyModeUi() {
        if (readOnly) {
            titleView.setText(R.string.consult_budget_title);
            startInput.setEnabled(false);
            endInput.setEnabled(false);
            totalInput.setEnabled(false);
            toggleCalBtn.setVisibility(View.GONE);
            calendarCard.setVisibility(View.GONE);
            continueBtn.setText(R.string.view_allocations_readonly);
            alertBanner.setAlert(AlertBannerView.Tone.WARNING,
                    getString(R.string.period_closed_banner_title),
                    getString(R.string.period_closed_banner_detail));
        } else if (editingBudget != null) {
            titleView.setText(R.string.edit_budget_title);
            continueBtn.setText(R.string.allocate_by_category_cta);
        } else {
            titleView.setText(R.string.create_budget_title);
            continueBtn.setText(R.string.allocate_by_category_cta);
        }
    }

    private void focusCalendarEndpoint(boolean pickStart) {
        if (readOnly) return;
        pickingStart = pickStart;
        calendarCard.setVisibility(View.VISIBLE);
        String targetIso = (pickStart ? startInput : endInput).getText().toString().trim();
        if (BudgetPeriodMath.isValidIsoDate(targetIso)) {
            int[] ymd = BudgetPeriodMath.parseIsoParts(targetIso);
            calendarYear = ymd[0];
            calendarMonth = ymd[1] - 1;
        }
        updatePickerModeButtons();
        renderInteractiveCalendar();
    }

    private void updatePickerModeButtons() {
        modeStartBtn.setBackgroundTintList(ColorStateList.valueOf(
                getColor(pickingStart ? R.color.forest : R.color.mint_light)));
        modeStartBtn.setTextColor(getColor(pickingStart ? R.color.alabaster : R.color.forest));
        modeEndBtn.setBackgroundTintList(ColorStateList.valueOf(
                getColor(!pickingStart ? R.color.forest : R.color.mint_light)));
        modeEndBtn.setTextColor(getColor(!pickingStart ? R.color.alabaster : R.color.forest));
    }

    private void renderWeekdays() {
        calWeekdays.removeAllViews();
        SimpleDateFormat fmt = new SimpleDateFormat("EE", Locale.getDefault());
        Calendar date = Calendar.getInstance();
        date.set(2024, Calendar.JANUARY, 1, 12, 0, 0); // Monday
        for (int i = 0; i < 7; i++) {
            TextView tv = new TextView(this);
            tv.setText(fmt.format(date.getTime()));
            tv.setTextSize(12f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(getColor(R.color.muted_green));
            calWeekdays.addView(tv, new LinearLayout.LayoutParams(0, dp(32), 1f));
            date.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void renderInteractiveCalendar() {
        if (calDays == null) return;
        calDays.removeAllViews();
        if (calMonthLabel != null) {
            calMonthLabel.setText(String.format(Locale.getDefault(), "%s %d",
                    localizedMonthName(calendarMonth), calendarYear));
        }

        long selectedStart = BudgetPeriodMath.isValidIsoDate(startInput.getText().toString())
                ? BudgetPeriodMath.parseIsoDateStartMillis(startInput.getText().toString()) : -1L;
        long selectedEnd = BudgetPeriodMath.isValidIsoDate(endInput.getText().toString())
                ? BudgetPeriodMath.parseIsoDateEndMillis(endInput.getText().toString()) : -1L;
        long todayStart = BudgetPeriodMath.todayStartMillis();

        Calendar first = Calendar.getInstance();
        first.clear();
        first.set(calendarYear, calendarMonth, 1, 0, 0, 0);
        int offset = (first.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
        int daysInMonth = BudgetPeriodMath.daysInMonth(calendarYear, calendarMonth);
        int totalCells = ((offset + daysInMonth + 6) / 7) * 7;

        long editingId = editingBudget == null ? 0L : editingBudget.id;

        LinearLayout row = null;
        for (int cell = 0; cell < totalCells; cell++) {
            if (cell % 7 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                calDays.addView(row, new LinearLayout.LayoutParams(-1, dp(42)));
            }
            LinearLayout cellBox = new LinearLayout(this);
            cellBox.setOrientation(LinearLayout.VERTICAL);
            cellBox.setGravity(Gravity.CENTER);
            int day = cell - offset + 1;
            if (day < 1 || day > daysInMonth) {
                row.addView(cellBox, new LinearLayout.LayoutParams(0, -1, 1f));
                continue;
            }

            long dayStart = BudgetPeriodMath.startOfDayMillis(calendarYear, calendarMonth, day);
            long dayEnd = BudgetPeriodMath.endOfDayMillis(calendarYear, calendarMonth, day);
            boolean isStart = selectedStart >= 0 && dayStart == selectedStart;
            boolean isEnd = selectedEnd >= 0 && dayEnd == selectedEnd;
            boolean inRange = selectedStart >= 0 && selectedEnd >= selectedStart
                    && dayStart >= selectedStart && dayEnd <= selectedEnd;
            boolean isToday = dayStart == todayStart;

            boolean occupiedByOtherBudget = false;
            for (BudgetEntity b : userBudgets) {
                if (editingId > 0 && b.id == editingId) continue;
                if (BudgetPeriodMath.periodsOverlap(dayStart, dayEnd, b.periodStart, b.periodEnd)) {
                    occupiedByOtherBudget = true;
                    break;
                }
            }

            TextView dayLabel = new TextView(this);
            dayLabel.setText(String.valueOf(day));
            dayLabel.setTextSize(13f);
            dayLabel.setGravity(Gravity.CENTER);

            if (isStart || isEnd) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(getColor(R.color.forest));
                bg.setCornerRadius(dp(12));
                cellBox.setBackground(bg);
                dayLabel.setTextColor(getColor(R.color.alabaster));
                dayLabel.setTypeface(null, Typeface.BOLD);
            } else if (inRange) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(getColor(R.color.mint_light));
                bg.setCornerRadius(dp(10));
                cellBox.setBackground(bg);
                dayLabel.setTextColor(getColor(R.color.forest));
                dayLabel.setTypeface(null, Typeface.BOLD);
            } else if (isToday) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.TRANSPARENT);
                bg.setStroke(dp(1), getColor(R.color.emerald));
                bg.setCornerRadius(dp(12));
                cellBox.setBackground(bg);
                dayLabel.setTextColor(getColor(R.color.emerald));
                dayLabel.setTypeface(null, Typeface.BOLD);
            } else {
                cellBox.setBackground(null);
                dayLabel.setTextColor(getColor(occupiedByOtherBudget ? R.color.coral : R.color.ink));
            }
            cellBox.addView(dayLabel);

            if (occupiedByOtherBudget) {
                View dot = new View(this);
                GradientDrawable dotBg = new GradientDrawable();
                dotBg.setShape(GradientDrawable.OVAL);
                dotBg.setColor(getColor(isStart || isEnd ? R.color.mint : R.color.amber));
                dot.setBackground(dotBg);
                LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(dp(5), dp(5));
                dpParams.topMargin = dp(2);
                cellBox.addView(dot, dpParams);
            }

            final int pickedDay = day;
            cellBox.setOnClickListener(v -> onCalendarDayClicked(calendarYear, calendarMonth, pickedDay));
            row.addView(cellBox, new LinearLayout.LayoutParams(0, -1, 1f));
        }
    }

    private void onCalendarDayClicked(int year, int month, int day) {
        if (readOnly) return;
        String pickedIso = BudgetPeriodMath.formatIsoDate(year, month, day);
        String curStart = startInput.getText().toString().trim();
        String curEnd = endInput.getText().toString().trim();

        if (pickingStart) {
            if (BudgetPeriodMath.isValidIsoDate(curEnd) && pickedIso.compareTo(curEnd) > 0) {
                setIsoDatesSilently(pickedIso, pickedIso);
            } else {
                setIsoDatesSilently(pickedIso, curEnd);
            }
            pickingStart = false;
        } else {
            if (BudgetPeriodMath.isValidIsoDate(curStart) && pickedIso.compareTo(curStart) < 0) {
                setIsoDatesSilently(pickedIso, curStart);
            } else {
                setIsoDatesSilently(curStart, pickedIso);
            }
        }
        updatePickerModeButtons();
        errorText.setVisibility(View.GONE);
        refreshLivePeriodState(false);
        renderInteractiveCalendar();
    }

    private boolean refreshLivePeriodState(boolean showAllErrors) {
        String sIso = startInput.getText().toString().trim();
        String eIso = endInput.getText().toString().trim();
        boolean validStart = BudgetPeriodMath.isValidIsoDate(sIso);
        boolean validEnd = BudgetPeriodMath.isValidIsoDate(eIso);

        editExistingBtn.setVisibility(View.GONE);
        if (!readOnly) {
            alertBanner.setAlert(AlertBannerView.Tone.NONE, "", "");
        }

        if (!validStart || !validEnd) {
            topInfoBanner.setAlert(AlertBannerView.Tone.SUCCESS,
                    getString(R.string.budget_period_info_title),
                    getString(R.string.choisir_dates_debut_fin));
            daysBanner.setAlert(AlertBannerView.Tone.SUCCESS,
                    getString(R.string.choisir_dates_debut_fin),
                    getString(R.string.budget_pay_cycle_detail));
            if (showAllErrors) {
                showBannerError(getString(R.string.date_format_iso_error));
            }
            return false;
        }

        long sMillis = BudgetPeriodMath.parseIsoDateStartMillis(sIso);
        long eMillis = BudgetPeriodMath.parseIsoDateEndMillis(eIso);
        if (eMillis < sMillis) {
            topInfoBanner.setAlert(AlertBannerView.Tone.SUCCESS,
                    getString(R.string.budget_period_info_title),
                    getString(R.string.choisir_dates_debut_fin));
            daysBanner.setAlert(AlertBannerView.Tone.SUCCESS,
                    getString(R.string.choisir_dates_debut_fin),
                    getString(R.string.budget_pay_cycle_detail));
            if (showAllErrors) {
                showBannerError(getString(R.string.date_order_error));
            }
            return false;
        }

        int days = BudgetPeriodMath.daysBetweenInclusive(sMillis, eMillis);
        String rangeLabel = BudgetPeriodMath.formatRange(sMillis, eMillis, Locale.getDefault());
        topInfoBanner.setAlert(AlertBannerView.Tone.SUCCESS,
                getString(R.string.budget_period_info_title),
                getString(R.string.budget_period_days_format, rangeLabel, days));
        daysBanner.setAlert(AlertBannerView.Tone.SUCCESS,
                getString(R.string.budget_days_selected_title, days),
                getString(R.string.budget_pay_cycle_detail));

        if (readOnly) {
            return true;
        }

        if (editingBudget == null && BudgetPeriodMath.isPastPeriodEnd(eMillis, Calendar.getInstance())) {
            showBannerError(getString(R.string.past_period_budget_error));
            return false;
        }

        long excludeId = editingBudget == null ? 0L : editingBudget.id;
        BudgetEntity overlapping = null;
        for (BudgetEntity b : userBudgets) {
            if (excludeId > 0 && b.id == excludeId) continue;
            if (BudgetPeriodMath.periodsOverlap(sMillis, eMillis, b.periodStart, b.periodEnd)) {
                overlapping = b;
                break;
            }
        }
        if (overlapping != null) {
            String overlapRange = BudgetPeriodMath.formatRange(overlapping.periodStart, overlapping.periodEnd, Locale.getDefault());
            String bStart = BudgetPeriodMath.formatIsoDate(overlapping.periodStart);
            String bEnd = BudgetPeriodMath.formatIsoDate(overlapping.periodEnd);
            if (editingBudget == null && sIso.equals(bStart) && eIso.equals(bEnd)) {
                alertBanner.setAlert(AlertBannerView.Tone.WARNING,
                        getString(R.string.period_duplicate_title),
                        getString(R.string.period_overlap_error, overlapRange));
                long existingId = overlapping.id;
                editExistingBtn.setVisibility(View.VISIBLE);
                editExistingBtn.setOnClickListener(v -> {
                    startActivity(new Intent(this, BudgetPeriodActivity.class)
                            .putExtra("MODE", "edit")
                            .putExtra("BUDGET_ID", existingId));
                    finish();
                });
            } else {
                alertBanner.setAlert(AlertBannerView.Tone.DANGER,
                        getString(R.string.period_overlap_title),
                        getString(R.string.period_overlap_error, overlapRange));
            }
            return false;
        }
        return true;
    }

    private void showBannerError(String msg) {
        alertBanner.setAlert(AlertBannerView.Tone.DANGER, getString(R.string.verifier_ce_budget), msg);
        errorText.setText(msg);
        errorText.setVisibility(View.VISIBLE);
    }

    private void handleContinue() {
        if (readOnly && editingBudget != null) {
            startActivity(new Intent(this, AllocateBudgetActivity.class)
                    .putExtra("START", editingBudget.periodStart)
                    .putExtra("END", editingBudget.periodEnd)
                    .putExtra("TOTAL", editingBudget.monthlyLimitMilli)
                    .putExtra("BUDGET_ID", editingBudget.id)
                    .putExtra("READ_ONLY", true));
            return;
        }

        if (!refreshLivePeriodState(true)) {
            return;
        }

        String sIso = startInput.getText().toString().trim();
        String eIso = endInput.getText().toString().trim();
        long sMillis = BudgetPeriodMath.parseIsoDateStartMillis(sIso);
        long eMillis = BudgetPeriodMath.parseIsoDateEndMillis(eIso);

        long totalMilli;
        String rawTotalText = totalInput.getText() == null ? "" : totalInput.getText().toString().trim();
        if (rawTotalText.isEmpty()) {
            showBannerError(getString(R.string.budget_total_required));
            return;
        }
        try {
            totalMilli = BudgetPeriodMath.parsePositiveBudgetMilli(rawTotalText);
        } catch (Exception ex) {
            showBannerError(getString(R.string.budget_total_required));
            return;
        }
        if (totalMilli <= 0L) {
            showBannerError(getString(R.string.budget_total_required));
            return;
        }

        long excludeId = editingBudget == null ? 0L : editingBudget.id;
        continueBtn.setEnabled(false);
        viewModel.<BudgetEntity>request(
                repository -> repository.findOverlappingBudget(userId, sMillis, eMillis, excludeId),
                result -> {
                    continueBtn.setEnabled(true);
                    if (isFinishing() || isDestroyed()) return;
                    if (result.data != null) {
                        String range = BudgetPeriodMath.formatRange(
                                result.data.periodStart, result.data.periodEnd, Locale.getDefault());
                        showBannerError(getString(R.string.period_overlap_error, range));
                        return;
                    }
                    Intent next = new Intent(this, AllocateBudgetActivity.class)
                            .putExtra("START", sMillis)
                            .putExtra("END", eMillis)
                            .putExtra("TOTAL", totalMilli);
                    if (editingBudget != null) {
                        next.putExtra("BUDGET_ID", editingBudget.id);
                    }
                    startActivity(next);
                });
    }

    private void setIsoDatesSilently(String startIso, String endIso) {
        programmaticTextUpdate = true;
        startInput.setText(startIso);
        endInput.setText(endIso);
        programmaticTextUpdate = false;
    }

    private String localizedMonthName(int monthZeroBased) {
        String lang = Locale.getDefault().getLanguage();
        if ("fr".equalsIgnoreCase(lang)) {
            return new String[]{"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"}[monthZeroBased];
        }
        if ("ar".equalsIgnoreCase(lang)) {
            return new String[]{"جانفي", "فيفري", "مارس", "أفريل", "ماي", "جوان",
                    "جويلية", "أوت", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"}[monthZeroBased];
        }
        return new String[]{"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"}[monthZeroBased];
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static final class LoadedPeriodState {
        final List<BudgetEntity> all;
        final BudgetEntity target;

        LoadedPeriodState(List<BudgetEntity> all, BudgetEntity target) {
            this.all = all;
            this.target = target;
        }
    }
}
