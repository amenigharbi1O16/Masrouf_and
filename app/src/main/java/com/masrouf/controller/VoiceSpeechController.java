/*
 * ============================================================================
 * FICHIER : VoiceSpeechController.java
 * MISSION : Contrôleur Android unifié de reconnaissance vocale multilingue
 *           (AUTO FR/AR/EN + mode forcé). Vérifie et demande la permission
 *           RECORD_AUDIO, pilote Android SpeechRecognizer avec repli automatique
 *           vers RecognizerIntent si indisponible ou occupé, et gère les états
 *           IDLE, LISTENING, PROCESSING, RESULT, CANCELLED, ERROR_PERMISSION, ERROR.
 * IMPLÉMENTE : RecognitionListener complet, détection automatique de langue
 *              API 33+ (EXTRA_ENABLE_LANGUAGE_DETECTION) et repli script/lexique
 *              via VoiceParser.detectLanguage(), sans jamais traduire la phrase.
 * EFFET À L'ÉCRAN : Aucun directement — transmet états, transcriptions partielles
 *                   et transcription finale verbatim via l'interface Callback.
 * COMMUNICATIONS :
 *   ← VoiceTransactionDialogFragment, VoiceFieldHelper
 *   → Android SpeechRecognizer / RecognizerIntent (API système vocale)
 *   → VoiceParser.detectLanguage (détection de langue de repli)
 * ============================================================================
 */
package com.masrouf.controller;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Contrôleur Android unifié pour la reconnaissance vocale : vérifie la permission RECORD_AUDIO,
 * gère Android SpeechRecognizer avec repli RecognizerIntent, suit l'état (IDLE/LISTENING/PROCESSING/RESULT/ERROR)
 * et supporte la détection automatique de langue (API 33+ ou heuristique de script).
 */
/**
 * Unified multilingual speech-recognition controller supporting French, English, and Arabic
 * with automatic language detection (API 33+ platform detection + multi-language hints + script/lexical fallback),
 * explicit permission handling, lifecycle states, and verbatim preservation without translation.
 */
public final class VoiceSpeechController {
    public enum LangMode {
        AUTO("auto"),
        FR("fr-FR"),
        AR("ar-TN"),
        EN("en-US");

        public final String bcp47;

        LangMode(String bcp47) {
            this.bcp47 = bcp47;
        }
    }

    public enum State {
        IDLE,
        LISTENING,
        PROCESSING,
        RESULT,
        CANCELLED,
        ERROR_PERMISSION,
        ERROR
    }

    public interface Callback {
        void onStateChanged(@NonNull State state, @Nullable String detailMessage, int errorCode);

        void onPartialTranscript(@NonNull String partialText, @NonNull String detectedLang);

        void onFinalTranscript(@NonNull String finalText, @NonNull String detectedLang);
    }

    private static final String EXTRA_ENABLE_LANGUAGE_DETECTION = "android.speech.extra.ENABLE_LANGUAGE_DETECTION";
    private static final String EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES = "android.speech.extra.LANGUAGE_DETECTION_ALLOWED_LANGUAGES";
    private static final String EXTRA_ADDITIONAL_LANGUAGES = "android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES";
    private static final String RESULT_DETECTED_LANGUAGE = "detected_language";

    private final Context appContext;
    private final Callback callback;
    private final ActivityResultLauncher<String> permissionLauncher;
    private final ActivityResultLauncher<Intent> intentFallbackLauncher;

    @Nullable
    private SpeechRecognizer recognizer;
    private LangMode pendingMode = LangMode.AUTO;
    private State currentState = State.IDLE;
    private boolean triedIntentFallback = false;

    public VoiceSpeechController(
            @NonNull Context context,
            @NonNull ActivityResultCaller caller,
            @NonNull Callback callback) {
        this.appContext = context.getApplicationContext();
        this.callback = callback;

        this.permissionLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (Boolean.TRUE.equals(granted)) {
                        beginListeningInternal(pendingMode);
                    } else {
                        emitState(State.ERROR_PERMISSION, null, -1);
                    }
                });

        this.intentFallbackLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        ArrayList<String> matches = result.getData()
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        String text = selectBestVerbatimCandidate(matches, null);
                        if (!text.isEmpty()) {
                            String detected = VoiceParser.detectLanguage(text);
                            emitState(State.RESULT, null, 0);
                            callback.onFinalTranscript(text, detected);
                            return;
                        }
                        emitState(State.ERROR, null, SpeechRecognizer.ERROR_NO_MATCH);
                    } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        emitState(State.CANCELLED, null, 0);
                    } else {
                        emitState(State.ERROR, null, SpeechRecognizer.ERROR_CLIENT);
                    }
                });
    }

    @NonNull
    public State getCurrentState() {
        return currentState;
    }

    public boolean isListening() {
        return currentState == State.LISTENING;
    }

    public void startListening(@NonNull LangMode mode) {
        this.pendingMode = mode;
        this.triedIntentFallback = false;
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            return;
        }
        beginListeningInternal(mode);
    }

    public void stopListening() {
        if (recognizer != null && currentState == State.LISTENING) {
            emitState(State.PROCESSING, null, 0);
            try {
                recognizer.stopListening();
            } catch (RuntimeException ignored) {
                // Keep state resilient across vendor implementations
            }
        }
    }

    public void cancelListening() {
        if (recognizer != null) {
            try {
                recognizer.cancel();
            } catch (RuntimeException ignored) { }
        }
        emitState(State.CANCELLED, null, 0);
    }

    public void destroy() {
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (RuntimeException ignored) { }
            recognizer = null;
        }
    }

    private void beginListeningInternal(@NonNull LangMode mode) {
        Intent intent = buildRecognitionIntent(mode);

        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            if (!launchRecognizerIntentFallback(intent)) {
                emitState(State.ERROR, null, SpeechRecognizer.ERROR_CLIENT);
            }
            return;
        }

        try {
            if (recognizer != null) {
                recognizer.cancel();
                recognizer.destroy();
            }
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext);
            recognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    emitState(State.LISTENING, null, 0);
                }

                @Override
                public void onBeginningOfSpeech() {
                    emitState(State.LISTENING, null, 0);
                }

                @Override
                public void onRmsChanged(float rmsdB) { }

                @Override
                public void onBufferReceived(byte[] buffer) { }

                @Override
                public void onEndOfSpeech() {
                    emitState(State.PROCESSING, null, 0);
                }

                @Override
                public void onError(int error) {
                    if (currentState == State.CANCELLED) {
                        return;
                    }
                    if (!triedIntentFallback
                            && (error == SpeechRecognizer.ERROR_CLIENT
                            || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
                            || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)) {
                        triedIntentFallback = true;
                        if (launchRecognizerIntentFallback(intent)) {
                            return;
                        }
                    }
                    if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        emitState(State.ERROR_PERMISSION, null, error);
                    } else {
                        emitState(State.ERROR, null, error);
                    }
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results == null
                            ? null
                            : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String platformLang = results == null ? null : results.getString(RESULT_DETECTED_LANGUAGE);
                    String text = selectBestVerbatimCandidate(matches, platformLang);
                    if (text.isEmpty()) {
                        emitState(State.ERROR, null, SpeechRecognizer.ERROR_NO_MATCH);
                        return;
                    }
                    String detected = resolveDetectedLang(text, platformLang, mode);
                    emitState(State.RESULT, null, 0);
                    callback.onFinalTranscript(text, detected);
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                    ArrayList<String> matches = partialResults == null
                            ? null
                            : partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    String partial = selectBestVerbatimCandidate(matches, null);
                    if (!partial.isEmpty()) {
                        String detected = VoiceParser.detectLanguage(partial);
                        callback.onPartialTranscript(partial, detected);
                    }
                }

                @Override
                public void onEvent(int eventType, Bundle params) { }
            });
            emitState(State.LISTENING, null, 0);
            recognizer.startListening(intent);
        } catch (RuntimeException failure) {
            if (!launchRecognizerIntentFallback(intent)) {
                emitState(State.ERROR, failure.getMessage(), SpeechRecognizer.ERROR_CLIENT);
            }
        }
    }

    private boolean launchRecognizerIntentFallback(@NonNull Intent intent) {
        if (intent.resolveActivity(appContext.getPackageManager()) == null) {
            return false;
        }
        try {
            emitState(State.PROCESSING, null, 0);
            intentFallbackLauncher.launch(intent);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @NonNull
    private Intent buildRecognitionIntent(@NonNull LangMode mode) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);

        if (mode == LangMode.AUTO) {
            String primary = appContext.getResources().getConfiguration().getLocales().size() > 0
                    ? appContext.getResources().getConfiguration().getLocales().get(0).toLanguageTag()
                    : "fr-FR";
            if (!primary.startsWith("fr") && !primary.startsWith("ar") && !primary.startsWith("en")) {
                primary = "fr-FR";
            }
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, primary);
            ArrayList<String> allowed = new ArrayList<>(Arrays.asList("fr-FR", "en-US", "ar-TN", "ar-SA"));
            if (Build.VERSION.SDK_INT >= 33) {
                intent.putExtra(EXTRA_ENABLE_LANGUAGE_DETECTION, true);
                intent.putStringArrayListExtra(EXTRA_LANGUAGE_DETECTION_ALLOWED_LANGUAGES, allowed);
            }
            intent.putStringArrayListExtra(EXTRA_ADDITIONAL_LANGUAGES, allowed);
        } else {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, mode.bcp47);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, mode.bcp47);
        }
        return intent;
    }

    @NonNull
    static String selectBestVerbatimCandidate(@Nullable List<String> matches, @Nullable String platformLang) {
        if (matches == null || matches.isEmpty()) {
            return "";
        }
        for (String item : matches) {
            if (item != null && !item.trim().isEmpty()) {
                return item.trim();
            }
        }
        return "";
    }

    @NonNull
    private static String resolveDetectedLang(@NonNull String text, @Nullable String platformLang, @NonNull LangMode mode) {
        String textDetected = VoiceParser.detectLanguage(text);
        if (!"mixed".equals(textDetected) && textDetected.length() == 2) {
            return textDetected;
        }
        if ("mixed".equals(textDetected)) {
            return "mixed";
        }
        if (platformLang != null) {
            String lower = platformLang.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("ar")) return "ar";
            if (lower.startsWith("en")) return "en";
            if (lower.startsWith("fr")) return "fr";
        }
        if (mode == LangMode.AR) return "ar";
        if (mode == LangMode.EN) return "en";
        return "fr";
    }

    private void emitState(@NonNull State state, @Nullable String detail, int errorCode) {
        this.currentState = state;
        callback.onStateChanged(state, detail, errorCode);
    }
}
