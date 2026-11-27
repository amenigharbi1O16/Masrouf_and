/*
 * ============================================================================
 * FICHIER : VoiceFieldHelper.java
 * MISSION : Assistant réutilisable reliant un bouton micro inline (ImageButton)
 *           à n'importe quel champ EditText des formulaires (dépense, produit,
 *           recherche, ajout enfant, demande d'argent, catégorie personnalisée).
 * IMPLÉMENTE : Gestion du cycle d'écoute via VoiceSpeechController, permission
 *              RECORD_AUDIO, teinte verte émeraude sur le bouton actif, bannière
 *              d'état AlertBannerView et insertion intelligente (extraction
 *              numérique si champ chiffré, sinon texte verbatim au curseur).
 * EFFET À L'ÉCRAN : Colore le micro actif, affiche l'état d'écoute/erreur dans
 *                   la bannière ou un Toast, puis remplit le champ ciblé sans
 *                   jamais soumettre le formulaire automatiquement.
 * COMMUNICATIONS :
 *   ← ProductsFragment, AddExpenseActivity, AddProductActivity,
 *     ChildrenManagementActivity, MoneyRequestActivity, CustomCategoryDialogFragment
 *   → VoiceSpeechController (reconnaissance vocale Android SpeechRecognizer)
 *   → VoiceParser (extraction d'un montant numérique pour champs chiffrés)
 *   → AlertBannerView (affichage du statut d'écoute)
 * ============================================================================
 */
package com.masrouf.controller;

import android.content.Context;
import android.content.res.ColorStateList;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCaller;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.masrouf1.R;
import com.example.masrouf1.ui.AlertBannerView;

import java.util.HashMap;
import java.util.Map;

/**
 * Assistant réutilisable liant un bouton micro inline (ImageButton) à un champ EditText dans les formulaires
 * (dépense, produit, ajout enfant, demande d'argent, catégorie personnalisée).
 */
/**
 * Reusable per-form speech-to-field helper binding inline microphone buttons directly to inputs.
 * Handles runtime permission, listening/processing/result/cancelled/error visual feedback, and
 * preserves spoken French, English, Arabic, and mixed-language text verbatim without translation
 * or auto-submission.
 */
public final class VoiceFieldHelper {
    public interface OnDictatedListener {
        void onDictated(@NonNull EditText target, @NonNull String insertedText, @NonNull String detectedLang);
    }

    private final Context context;
    private final VoiceSpeechController controller;
    private final Map<ImageButton, EditText> bindings = new HashMap<>();
    @Nullable
    private AlertBannerView statusBanner;
    @Nullable
    private ImageButton activeMicButton;
    @Nullable
    private EditText activeTarget;
    @Nullable
    private OnDictatedListener customListener;

    public VoiceFieldHelper(@NonNull Context context, @NonNull ActivityResultCaller caller, @Nullable AlertBannerView banner) {
        this.context = context;
        this.statusBanner = banner;
        this.controller = new VoiceSpeechController(context, caller, new VoiceSpeechController.Callback() {
            @Override
            public void onStateChanged(@NonNull VoiceSpeechController.State state, @Nullable String detailMessage, int errorCode) {
                updateUiState(state, errorCode);
            }

            @Override
            public void onPartialTranscript(@NonNull String partialText, @NonNull String detectedLang) {
                showBanner(AlertBannerView.Tone.SUCCESS,
                        partialText,
                        context.getString(R.string.voice_recognizing));
            }

            @Override
            public void onFinalTranscript(@NonNull String finalText, @NonNull String detectedLang) {
                EditText target = activeTarget;
                if (target != null) {
                    applyTranscript(target, finalText);
                    if (customListener != null) {
                        customListener.onDictated(target, finalText, detectedLang);
                    }
                }
                resetMicTint();
            }
        });
    }

    public void setStatusBanner(@Nullable AlertBannerView banner) {
        this.statusBanner = banner;
    }

    public void bindField(@Nullable ImageButton micButton, @Nullable EditText target) {
        bindField(micButton, target, null);
    }

    public void bindField(@Nullable ImageButton micButton, @Nullable EditText target, @Nullable OnDictatedListener listener) {
        if (micButton == null || target == null) return;
        bindings.put(micButton, target);
        micButton.setOnClickListener(v -> {
            if (controller.isListening() && micButton == activeMicButton) {
                controller.stopListening();
                return;
            }
            if (controller.isListening()) {
                controller.cancelListening();
            }
            resetMicTint();
            activeMicButton = micButton;
            activeTarget = target;
            customListener = listener;
            micButton.setBackgroundResource(R.drawable.bg_chip_green_tint);
            micButton.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.emerald)));
            controller.startListening(VoiceSpeechController.LangMode.AUTO);
        });
    }

    public void destroy() {
        controller.destroy();
        bindings.clear();
        activeMicButton = null;
        activeTarget = null;
    }

    private void showBanner(AlertBannerView.Tone tone, String title, String detail) {
        if (statusBanner != null) {
            statusBanner.setVisibility(View.VISIBLE);
            statusBanner.setAlert(tone, title, detail);
        }
    }

    /** Insère la transcription vocale dans le champ cible (ou extrait le montant numérique si champ chiffré). */
    private void applyTranscript(@NonNull EditText target, @NonNull String finalText) {
        boolean numericOnly = (target.getInputType() & android.text.InputType.TYPE_MASK_CLASS)
                == android.text.InputType.TYPE_CLASS_NUMBER;
        String value = finalText.trim();
        if (numericOnly) {
            VoiceParser.Result parsed = VoiceParser.parse(value);
            if (parsed.amountMilli > 0L) {
                value = java.math.BigDecimal.valueOf(parsed.amountMilli, 3)
                        .stripTrailingZeros().toPlainString();
            } else {
                String digits = value.replace(',', '.').replaceAll("[^0-9.]", "");
                if (!digits.isEmpty()) value = digits;
            }
            target.setText(value);
            target.setSelection(target.getText().length());
            return;
        }
        Editable existing = target.getText();
        if (existing == null || existing.length() == 0) {
            target.setText(value);
        } else {
            int start = Math.max(0, target.getSelectionStart());
            int end = Math.max(start, target.getSelectionEnd());
            String prefixSpace = (start > 0 && !Character.isWhitespace(existing.charAt(start - 1))) ? " " : "";
            existing.replace(start, end, prefixSpace + value);
        }
        target.requestFocus();
        target.setSelection(target.getText().length());
    }

    private void updateUiState(@NonNull VoiceSpeechController.State state, int errorCode) {
        switch (state) {
            case LISTENING:
                if (statusBanner != null) {
                    showBanner(AlertBannerView.Tone.SUCCESS,
                            context.getString(R.string.voice_tap_to_stop),
                            context.getString(R.string.voice_modal_subtitle));
                } else {
                    Toast.makeText(context, R.string.voice_tap_to_stop, Toast.LENGTH_SHORT).show();
                }
                break;
            case PROCESSING:
                showBanner(AlertBannerView.Tone.SUCCESS,
                        context.getString(R.string.voice_recognizing),
                        context.getString(R.string.voice_modal_subtitle));
                break;
            case RESULT:
                showBanner(AlertBannerView.Tone.SUCCESS,
                        context.getString(R.string.voice_state_result),
                        "");
                break;
            case CANCELLED:
                resetMicTint();
                showBanner(AlertBannerView.Tone.WARNING,
                        context.getString(R.string.voice_state_cancelled),
                        "");
                break;
            case ERROR_PERMISSION:
                resetMicTint();
                if (statusBanner != null) {
                    showBanner(AlertBannerView.Tone.WARNING,
                            context.getString(R.string.voice_state_perm_denied),
                            "");
                } else {
                    Toast.makeText(context, R.string.voice_state_perm_denied, Toast.LENGTH_LONG).show();
                }
                break;
            case ERROR:
                resetMicTint();
                int msgRes = errorCode == SpeechRecognizer.ERROR_NO_MATCH
                        || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                        ? R.string.voice_state_no_match
                        : R.string.voice_recognition_error;
                if (statusBanner != null) {
                    showBanner(AlertBannerView.Tone.DANGER, context.getString(msgRes), "");
                } else {
                    Toast.makeText(context, msgRes, Toast.LENGTH_SHORT).show();
                }
                break;
            case IDLE:
            default:
                resetMicTint();
                break;
        }
    }

    private void resetMicTint() {
        if (activeMicButton != null) {
            activeMicButton.setBackgroundResource(R.drawable.bg_card_form_beige);
            activeMicButton.setImageTintList(ColorStateList.valueOf(context.getColor(R.color.forest)));
            activeMicButton = null;
        }
    }
}
