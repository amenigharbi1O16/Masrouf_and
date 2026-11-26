package com.masrouf.controller;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.masrouf1.AppRole;
import com.example.masrouf1.MainActivity;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.MasroufEntities.CategoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
import com.example.masrouf1.ui.AlertBannerView;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Full-featured multilingual Voice Transaction modal matching React Native VoiceTransactionModal.tsx:
 * - Language modes: Auto (default FR/AR/EN detection without manual choice) + FR / AR / EN pills
 * - Full microphone permission + listening/processing/result/error/cancelled states & animation
 * - Editable recognized phrase preserving Arabic letters, accents & numbers verbatim without translation
 * - Multi-language clickable quick test phrases (FR / AR / EN)
 * - Fully editable Transaction Preview Card (Purchase / Expense / Child Allowance) saving directly to Room
 * - Never auto-submits: user must explicitly tap "Confirmer et enregistrer"
 */
public final class VoiceTransactionDialogFragment extends DialogFragment {
    private static final String[] QUICK_EXAMPLES = {
            "شريت حليب وخبز ب 5 دينار",
            "J'ai acheté du lait et du pain pour 5 dinars",
            "I bought milk and bread for 5 dinars",
            "J'ai acheté des baskets à 21",
            "J'ai acheté des courses pour 45 dinars",
            "دفعت فاتورة الكهرباء 75 دينار",
            "I paid the internet bill 50 dinars",
            "أعطيت 20 دينار ليوسف مصروف",
            "J'ai donné 25 DT à mon enfant",
            "تبرعت ب 15 دينار للجمعية",
            "Donation of 10 dinars for charity"
    };

    private final SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private MasroufViewModel viewModel;
    private VoiceSpeechController speechController;
    private long userId;
    private boolean parentRole;

    private VoiceSpeechController.LangMode selectedLangMode = VoiceSpeechController.LangMode.AUTO;
    private String selectedOpType = "purchase";
    private long selectedCategoryId = 0L;
    private long selectedChildProfileId = 0L;
    private boolean suppressPhraseWatcher = false;

    private List<CategoryEntity> userCategories = Collections.emptyList();
    private List<ChildSpendingSummary> familyChildren = Collections.emptyList();

    private TextView langAutoPill, langFrPill, langArPill, langEnPill;
    private ImageButton micBtn;
    private View micRing;
    private TextView micStatus, cancelBtn, detectedLangText, opPurchase, opExpense, opChild, categoryReadout;
    private View detectedLangBadge, categorySection, purchaseExtra, childExtra;
    private AlertBannerView feedbackBanner;
    private EditText phraseInput, amountInput, labelInput, allocPctInput, dateInput, storeInput, qtyInput, unitInput;
    private LinearLayout examplesRow, categoryChips, childChips;
    private MaterialButton confirmSaveBtn;
    @Nullable
    private ObjectAnimator pulseAnimator;

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        userId = SessionStore.userId(requireContext());
        parentRole = SessionStore.role(requireContext()) == AppRole.PARENT;
        viewModel = new ViewModelProvider(this, new MasroufViewModelFactory(requireContext())).get(MasroufViewModel.class);
        speechController = new VoiceSpeechController(requireContext(), this, new VoiceSpeechController.Callback() {
            @Override
            public void onStateChanged(@NonNull VoiceSpeechController.State st, @Nullable String detailMessage, int errorCode) {
                renderSpeechState(st, errorCode);
            }

            @Override
            public void onPartialTranscript(@NonNull String partialText, @NonNull String detectedLang) {
                suppressPhraseWatcher = true;
                phraseInput.setText(partialText);
                phraseInput.setSelection(phraseInput.getText().length());
                suppressPhraseWatcher = false;
                updateDetectedLangBadge(detectedLang);
            }

            @Override
            public void onFinalTranscript(@NonNull String finalText, @NonNull String detectedLang) {
                // Insert exact recognized phrase without translation & never auto-submit!
                suppressPhraseWatcher = true;
                phraseInput.setText(finalText);
                phraseInput.setSelection(phraseInput.getText().length());
                suppressPhraseWatcher = false;
                applyParsedDraft(finalText, detectedLang);
            }
        });
    }

    @Override
    public void onDestroy() {
        stopPulseAnimation();
        if (speechController != null) speechController.destroy();
        super.onDestroy();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle state) {
        View root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_voice_transaction, null, false);

        langAutoPill = root.findViewById(R.id.voice_lang_auto);
        langFrPill = root.findViewById(R.id.voice_lang_fr);
        langArPill = root.findViewById(R.id.voice_lang_ar);
        langEnPill = root.findViewById(R.id.voice_lang_en);
        micRing = root.findViewById(R.id.voice_mic_ring);
        micBtn = root.findViewById(R.id.voice_mic_btn);
        micStatus = root.findViewById(R.id.voice_mic_status);
        cancelBtn = root.findViewById(R.id.voice_cancel_btn);
        feedbackBanner = root.findViewById(R.id.voice_feedback_banner);
        detectedLangBadge = root.findViewById(R.id.voice_detected_lang_badge);
        detectedLangText = root.findViewById(R.id.voice_detected_lang_text);
        phraseInput = root.findViewById(R.id.voice_phrase_input);
        examplesRow = root.findViewById(R.id.voice_examples_row);
        opPurchase = root.findViewById(R.id.voice_op_purchase);
        opExpense = root.findViewById(R.id.voice_op_expense);
        opChild = root.findViewById(R.id.voice_op_child);
        amountInput = root.findViewById(R.id.voice_amount_input);
        labelInput = root.findViewById(R.id.voice_label_input);
        categorySection = root.findViewById(R.id.voice_category_section);
        categoryChips = root.findViewById(R.id.voice_category_chips);
        categoryReadout = root.findViewById(R.id.voice_category_readout);
        allocPctInput = root.findViewById(R.id.voice_alloc_pct_input);
        dateInput = root.findViewById(R.id.voice_date_input);
        purchaseExtra = root.findViewById(R.id.voice_purchase_extra);
        storeInput = root.findViewById(R.id.voice_store_input);
        qtyInput = root.findViewById(R.id.voice_qty_input);
        unitInput = root.findViewById(R.id.voice_unit_input);
        childExtra = root.findViewById(R.id.voice_child_extra);
        childChips = root.findViewById(R.id.voice_child_chips);
        confirmSaveBtn = root.findViewById(R.id.voice_confirm_save);

        root.findViewById(R.id.voice_close).setOnClickListener(v -> dismiss());
        if (parentRole && opChild != null) {
            opChild.setVisibility(View.VISIBLE);
        }

        dateInput.setText(isoFormat.format(new Date()));
        dateInput.setOnClickListener(v -> openDatePicker());
        storeInput.setText("Commerce");
        qtyInput.setText("1");
        unitInput.setText("pièce");

        langAutoPill.setOnClickListener(v -> selectLangMode(VoiceSpeechController.LangMode.AUTO));
        langFrPill.setOnClickListener(v -> selectLangMode(VoiceSpeechController.LangMode.FR));
        langArPill.setOnClickListener(v -> selectLangMode(VoiceSpeechController.LangMode.AR));
        langEnPill.setOnClickListener(v -> selectLangMode(VoiceSpeechController.LangMode.EN));
        renderLangPills();

        micBtn.setOnClickListener(v -> {
            if (speechController.isListening()) {
                speechController.stopListening();
            } else {
                speechController.startListening(selectedLangMode);
            }
        });
        cancelBtn.setOnClickListener(v -> speechController.cancelListening());

        opPurchase.setOnClickListener(v -> switchOpType("purchase"));
        opExpense.setOnClickListener(v -> switchOpType("expense"));
        opChild.setOnClickListener(v -> switchOpType("child_allowance"));

        phraseInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (suppressPhraseWatcher) return;
                String current = s == null ? "" : s.toString();
                if (!current.trim().isEmpty()) {
                    applyParsedDraft(current, VoiceParser.detectLanguage(current));
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        buildQuickExampleChips();
        confirmSaveBtn.setOnClickListener(v -> confirmAndSave());

        loadContextData();
        renderOperationType();

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(root)
                .create();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(R.drawable.bg_card_form_beige);
        }
        return dialog;
    }

    private void loadContextData() {
        viewModel.<VoiceContextData>request(repository -> new VoiceContextData(
                repository.categories(userId),
                parentRole ? repository.familyChildren(userId) : Collections.emptyList()
        ), state -> {
            if (!isAdded() || state.loading || state.data == null) return;
            userCategories = state.data.categories == null ? Collections.emptyList() : state.data.categories;
            familyChildren = state.data.children == null ? Collections.emptyList() : state.data.children;
            renderCategoryChips();
            renderChildChips();
        });
    }

    private void selectLangMode(VoiceSpeechController.LangMode mode) {
        selectedLangMode = mode;
        renderLangPills();
    }

    private void renderLangPills() {
        bindPill(langAutoPill, selectedLangMode == VoiceSpeechController.LangMode.AUTO);
        bindPill(langFrPill, selectedLangMode == VoiceSpeechController.LangMode.FR);
        bindPill(langArPill, selectedLangMode == VoiceSpeechController.LangMode.AR);
        bindPill(langEnPill, selectedLangMode == VoiceSpeechController.LangMode.EN);
    }

    private void bindPill(TextView pill, boolean active) {
        if (pill == null) return;
        pill.setBackgroundResource(active ? R.drawable.bg_lang_selected : R.drawable.bg_chip_beige);
        pill.setTextColor(requireContext().getColor(active ? R.color.alabaster : R.color.black));
    }

    private void buildQuickExampleChips() {
        examplesRow.removeAllViews();
        for (String phrase : QUICK_EXAMPLES) {
            if (!parentRole && (phrase.contains("يوسف") || phrase.contains("mon enfant"))) continue;
            TextView chip = new TextView(requireContext());
            chip.setText(phrase);
            chip.setBackgroundResource(R.drawable.bg_chip_beige);
            chip.setTextColor(requireContext().getColor(R.color.black));
            chip.setTextSize(12f);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                suppressPhraseWatcher = true;
                phraseInput.setText(phrase);
                phraseInput.setSelection(phrase.length());
                suppressPhraseWatcher = false;
                String detected = VoiceParser.detectLanguage(phrase);
                applyParsedDraft(phrase, detected);
                if (feedbackBanner != null) {
                    showFeedback(AlertBannerView.Tone.SUCCESS, getString(R.string.voice_state_result), "");
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginEnd(dp(8));
            examplesRow.addView(chip, lp);
        }
    }

    private void applyParsedDraft(@NonNull String rawText, @NonNull String detectedLang) {
        VoiceParser.RichResult parsed = VoiceParser.parseRich(rawText, userCategories, familyChildren, parentRole);
        updateDetectedLangBadge(parsed.detectedLanguage.isEmpty() ? detectedLang : parsed.detectedLanguage);

        selectedOpType = parsed.operationType;
        if ("child_allowance".equals(selectedOpType) && !parentRole) {
            selectedOpType = "expense";
        }
        renderOperationType();

        amountInput.setText(parsed.amountMilli > 0L
                ? BigDecimal.valueOf(parsed.amountMilli, 3).stripTrailingZeros().toPlainString()
                : "");
        labelInput.setText(parsed.label);
        storeInput.setText(parsed.store);
        qtyInput.setText(parsed.quantity);
        unitInput.setText(parsed.unit);

        if (parsed.matchedCategoryId > 0L) {
            selectedCategoryId = parsed.matchedCategoryId;
            renderCategoryChips();
        }
        if (parsed.matchedChildId > 0L) {
            selectedChildProfileId = parsed.matchedChildId;
            renderChildChips();
        }
    }

    private void updateDetectedLangBadge(@NonNull String lang) {
        if (detectedLangBadge == null || detectedLangText == null) return;
        detectedLangBadge.setVisibility(View.VISIBLE);
        if ("ar".equals(lang)) detectedLangText.setText(R.string.voice_detected_ar);
        else if ("en".equals(lang)) detectedLangText.setText(R.string.voice_detected_en);
        else if ("mixed".equals(lang)) detectedLangText.setText(R.string.voice_detected_mixed);
        else detectedLangText.setText(R.string.voice_detected_fr);
    }

    private void switchOpType(@NonNull String opType) {
        selectedOpType = opType;
        renderOperationType();
        renderCategoryChips();
    }

    private void renderOperationType() {
        boolean isPurchase = "purchase".equals(selectedOpType);
        boolean isExpense = "expense".equals(selectedOpType);
        boolean isChild = "child_allowance".equals(selectedOpType) && parentRole;

        bindPill(opPurchase, isPurchase);
        bindPill(opExpense, isExpense);
        bindPill(opChild, isChild);

        categorySection.setVisibility(isChild ? View.GONE : View.VISIBLE);
        purchaseExtra.setVisibility(isPurchase ? View.VISIBLE : View.GONE);
        childExtra.setVisibility(isChild ? View.VISIBLE : View.GONE);
    }

    private void renderCategoryChips() {
        categoryChips.removeAllViews();
        String expectedType = "purchase".equals(selectedOpType) ? "product" : "expense";
        List<CategoryEntity> visible = new ArrayList<>();
        for (CategoryEntity c : userCategories) {
            if (expectedType.equalsIgnoreCase(c.type)) visible.add(c);
        }
        if (visible.isEmpty()) visible.addAll(userCategories);

        boolean currentValid = false;
        for (CategoryEntity c : visible) {
            if (c.id == selectedCategoryId) {
                currentValid = true;
                break;
            }
        }
        if (!currentValid && !visible.isEmpty()) {
            selectedCategoryId = visible.get(0).id;
        }

        CategoryEntity activeCat = null;
        for (CategoryEntity c : visible) {
            boolean selected = c.id == selectedCategoryId;
            if (selected) activeCat = c;
            TextView chip = new TextView(requireContext());
            chip.setText(c.name);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_green_tint : R.drawable.bg_chip_beige);
            chip.setTextColor(requireContext().getColor(R.color.black));
            chip.setTextSize(12.5f);
            chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            chip.setPadding(dp(12), dp(8), dp(12), dp(8));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                selectedCategoryId = c.id;
                renderCategoryChips();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginEnd(dp(6));
            categoryChips.addView(chip, lp);
        }
        if (activeCat != null) {
            categoryReadout.setText(activeCat.name);
            allocPctInput.setText(String.valueOf(Math.max(0, activeCat.allocationPercent)));
        } else {
            categoryReadout.setText(R.string.category_name);
            allocPctInput.setText("0");
        }
    }

    private void renderChildChips() {
        childChips.removeAllViews();
        if (familyChildren.isEmpty()) {
            selectedChildProfileId = 0L;
            return;
        }
        boolean found = false;
        for (ChildSpendingSummary ch : familyChildren) {
            if (ch.childProfileId == selectedChildProfileId) {
                found = true;
                break;
            }
        }
        if (!found) selectedChildProfileId = familyChildren.get(0).childProfileId;

        for (ChildSpendingSummary ch : familyChildren) {
            boolean selected = ch.childProfileId == selectedChildProfileId;
            TextView chip = new TextView(requireContext());
            chip.setText(ch.nickname);
            chip.setBackgroundResource(selected ? R.drawable.bg_chip_green_tint : R.drawable.bg_chip_beige);
            chip.setTextColor(requireContext().getColor(R.color.black));
            chip.setTextSize(13f);
            chip.setTypeface(null, selected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));
            chip.setClickable(true);
            chip.setFocusable(true);
            chip.setOnClickListener(v -> {
                selectedChildProfileId = ch.childProfileId;
                renderChildChips();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMarginEnd(dp(8));
            childChips.addView(chip, lp);
        }
    }

    private void showFeedback(AlertBannerView.Tone tone, String title, String detail) {
        if (feedbackBanner != null) {
            feedbackBanner.setVisibility(View.VISIBLE);
            feedbackBanner.setAlert(tone, title, detail);
        }
    }

    private void renderSpeechState(@NonNull VoiceSpeechController.State st, int errorCode) {
        if (!isAdded()) return;
        switch (st) {
            case LISTENING:
                micBtn.setBackgroundResource(R.drawable.bg_btn_select);
                micStatus.setText(R.string.voice_tap_to_stop);
                cancelBtn.setVisibility(View.VISIBLE);
                startPulseAnimation();
                showFeedback(AlertBannerView.Tone.SUCCESS,
                        getString(R.string.voice_tap_to_stop),
                        getString(R.string.voice_modal_subtitle));
                break;
            case PROCESSING:
                stopPulseAnimation();
                micStatus.setText(R.string.voice_recognizing);
                cancelBtn.setVisibility(View.VISIBLE);
                showFeedback(AlertBannerView.Tone.SUCCESS, getString(R.string.voice_recognizing), "");
                break;
            case RESULT:
                stopPulseAnimation();
                micBtn.setBackgroundResource(R.drawable.bg_circle_forest);
                micStatus.setText(R.string.voice_tap_to_speak);
                cancelBtn.setVisibility(View.GONE);
                showFeedback(AlertBannerView.Tone.SUCCESS, getString(R.string.voice_state_result), "");
                break;
            case CANCELLED:
                stopPulseAnimation();
                micBtn.setBackgroundResource(R.drawable.bg_circle_forest);
                micStatus.setText(R.string.voice_tap_to_speak);
                cancelBtn.setVisibility(View.GONE);
                showFeedback(AlertBannerView.Tone.WARNING, getString(R.string.voice_state_cancelled), "");
                break;
            case ERROR_PERMISSION:
                stopPulseAnimation();
                micBtn.setBackgroundResource(R.drawable.bg_circle_forest);
                micStatus.setText(R.string.voice_tap_to_speak);
                cancelBtn.setVisibility(View.GONE);
                showFeedback(AlertBannerView.Tone.WARNING, getString(R.string.voice_state_perm_denied), "");
                break;
            case ERROR:
                stopPulseAnimation();
                micBtn.setBackgroundResource(R.drawable.bg_circle_forest);
                micStatus.setText(R.string.voice_tap_to_speak);
                cancelBtn.setVisibility(View.GONE);
                int errRes = errorCode == SpeechRecognizer.ERROR_NO_MATCH
                        || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ? R.string.voice_state_no_match
                        : R.string.voice_recognition_error;
                showFeedback(AlertBannerView.Tone.DANGER, getString(errRes), "");
                break;
            case IDLE:
            default:
                stopPulseAnimation();
                micBtn.setBackgroundResource(R.drawable.bg_circle_forest);
                micStatus.setText(R.string.voice_tap_to_speak);
                cancelBtn.setVisibility(View.GONE);
                break;
        }
    }

    private void startPulseAnimation() {
        stopPulseAnimation();
        if (micRing == null) return;
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                micRing,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f, 1f));
        pulseAnimator.setDuration(900L);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.start();
    }

    private void stopPulseAnimation() {
        if (pulseAnimator != null) {
            pulseAnimator.cancel();
            pulseAnimator = null;
        }
        if (micRing != null) {
            micRing.setScaleX(1f);
            micRing.setScaleY(1f);
        }
    }

    private void openDatePicker() {
        Calendar cal = Calendar.getInstance();
        String existing = dateInput.getText() == null ? "" : dateInput.getText().toString().trim();
        try {
            Date parsed = isoFormat.parse(existing);
            if (parsed != null) cal.setTime(parsed);
        } catch (ParseException ignored) { }
        new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.set(year, month, dayOfMonth);
                    dateInput.setText(isoFormat.format(picked.getTime()));
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmAndSave() {
        if (feedbackBanner != null) feedbackBanner.setVisibility(View.GONE);

        final long amountMilli;
        try {
            amountMilli = MoneyInput.toMilli(amountInput.getText() == null ? "" : amountInput.getText().toString());
            if (amountMilli <= 0L) throw new NumberFormatException();
        } catch (NumberFormatException invalid) {
            showFeedback(AlertBannerView.Tone.WARNING, getString(R.string.money_invalid), "");
            amountInput.requestFocus();
            return;
        }

        final String cleanLabel = labelInput.getText() == null ? "" : labelInput.getText().toString().trim();
        if (cleanLabel.isEmpty()) {
            showFeedback(AlertBannerView.Tone.WARNING, getString(R.string.product_name_required), "");
            labelInput.requestFocus();
            return;
        }

        long occurredAt = System.currentTimeMillis();
        String dateRaw = dateInput.getText() == null ? "" : dateInput.getText().toString().trim();
        if (!dateRaw.isEmpty()) {
            try {
                Date d = isoFormat.parse(dateRaw);
                if (d != null) occurredAt = d.getTime();
            } catch (ParseException ignored) { }
        }
        final long finalOccurredAt = occurredAt;
        final long preferredBudgetId = SessionStore.selectedBudgetId(requireContext(), userId);

        if ("child_allowance".equals(selectedOpType) && parentRole) {
            if (selectedChildProfileId <= 0L) {
                showFeedback(AlertBannerView.Tone.WARNING, getString(R.string.children_empty), "");
                return;
            }
            confirmSaveBtn.setEnabled(false);
            final long childPid = selectedChildProfileId;
            viewModel.<Integer>request(
                    repository -> repository.giveMoney(userId, childPid, amountMilli, cleanLabel, finalOccurredAt),
                    state -> {
                        if (!isAdded() || state.loading) return;
                        confirmSaveBtn.setEnabled(true);
                        if (state.data != null && state.data == 0) {
                            Toast.makeText(requireContext(), R.string.family_gift_saved, Toast.LENGTH_SHORT).show();
                            notifySavedAndDismiss();
                        } else {
                            showFeedback(AlertBannerView.Tone.DANGER, getString(R.string.parent_budget_required), "");
                        }
                    });
            return;
        }

        if (selectedCategoryId <= 0L) {
            showFeedback(AlertBannerView.Tone.WARNING,
                    getString(R.string.select_category_first), "");
            return;
        }

        if ("purchase".equals(selectedOpType)) {
            long qtyMilli = 1_000L;
            try {
                String qStr = qtyInput.getText() == null ? "1" : qtyInput.getText().toString().trim();
                if (!qStr.isEmpty()) {
                    long parsedQty = MoneyInput.toMilli(qStr);
                    if (parsedQty > 0L) qtyMilli = parsedQty;
                }
            } catch (NumberFormatException ignored) { }
            final long finalQtyMilli = qtyMilli;
            final long unitMilli = Math.max(1L, Math.round((amountMilli * 1000.0) / finalQtyMilli));
            final String unitStr = (unitInput.getText() == null || unitInput.getText().toString().trim().isEmpty())
                    ? "pièce" : unitInput.getText().toString().trim();
            final String storeStr = storeInput.getText() == null ? "" : storeInput.getText().toString().trim();

            confirmSaveBtn.setEnabled(false);
            viewModel.<Long>request(repository -> {
                BudgetEntity targetBudget = repository.resolveBudgetForDateOrPreferred(userId, finalOccurredAt, preferredBudgetId);
                ProductEntity product = new ProductEntity();
                product.name = cleanLabel;
                product.brand = "";
                product.unit = unitStr;
                product.categoryId = selectedCategoryId;
                product.currentPriceMilli = unitMilli;
                product.currency = "TND";
                product.updatedAt = finalOccurredAt;

                PurchaseEntity purchase = new PurchaseEntity();
                purchase.userId = userId;
                purchase.occurredAt = finalOccurredAt;
                purchase.totalMilli = amountMilli;
                purchase.storeName = storeStr;

                PurchaseItemEntity item = new PurchaseItemEntity();
                item.description = cleanLabel;
                item.quantityMilli = (int) finalQtyMilli;
                item.unit = unitStr;
                item.unitPriceMilli = unitMilli;

                return repository.savePurchase(product, purchase, item, userId, targetBudget == null ? 0L : targetBudget.id);
            }, state -> {
                if (!isAdded() || state.loading) return;
                confirmSaveBtn.setEnabled(true);
                if (state.data != null && state.data > 0L) {
                    Toast.makeText(requireContext(), R.string.purchase_saved, Toast.LENGTH_SHORT).show();
                    notifySavedAndDismiss();
                } else {
                    showFeedback(AlertBannerView.Tone.DANGER, getString(R.string.budget_required_for_entry), "");
                }
            });
            return;
        }

        // Expense
        confirmSaveBtn.setEnabled(false);
        viewModel.<Long>request(repository -> {
            BudgetEntity targetBudget = repository.resolveBudgetForDateOrPreferred(userId, finalOccurredAt, preferredBudgetId);
            if (targetBudget == null) return -1L;
            TransactionEntity tx = new TransactionEntity();
            tx.userId = userId;
            tx.budgetId = targetBudget.id;
            tx.categoryId = selectedCategoryId;
            tx.kind = "expense";
            tx.label = cleanLabel;
            tx.note = phraseInput.getText() == null ? "" : phraseInput.getText().toString().trim();
            tx.amountMilli = amountMilli;
            tx.occurredAt = finalOccurredAt;
            return repository.saveExpense(tx, userId);
        }, state -> {
            if (!isAdded() || state.loading) return;
            confirmSaveBtn.setEnabled(true);
            if (state.data != null && state.data > 0L) {
                Toast.makeText(requireContext(), R.string.expense_saved, Toast.LENGTH_SHORT).show();
                notifySavedAndDismiss();
            } else {
                showFeedback(AlertBannerView.Tone.DANGER, getString(R.string.budget_required_for_entry), "");
            }
        });
    }

    private void notifySavedAndDismiss() {
        getParentFragmentManager().setFragmentResult("masrouf-voice-saved", Bundle.EMPTY);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).onDrawerDestination("home");
        }
        dismiss();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private static final class VoiceContextData {
        final List<CategoryEntity> categories;
        final List<ChildSpendingSummary> children;

        VoiceContextData(List<CategoryEntity> categories, List<ChildSpendingSummary> children) {
            this.categories = categories;
            this.children = children;
        }
    }
}
