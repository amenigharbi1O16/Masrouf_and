/*
 * ============================================================================
 * FICHIER : SafeClickListener.java
 * MISSION : Écouteur explicite abstrait filtrant les doubles clics involontaires par temporisation anti-rebond.
 * IMPLÉMENTE : View.OnClickListener utilisant l'horloge monotone SystemClock.elapsedRealtime().
 * EFFET À L'ÉCRAN : Empêche qu'un double appui rapide déclenche deux fois l'action cible.
 * COMMUNICATIONS : Classe utilitaire réutilisable disponible dans com.example.masrouf1 (prête pour tout View.setOnClickListener).
 * ============================================================================
 */
package com.example.masrouf1;

import android.os.SystemClock;
import android.view.View;

/**
 * Listener explicite abstrait implémentant View.OnClickListener avec protection anti-rebond (650 ms par défaut).
 */
public abstract class SafeClickListener implements View.OnClickListener {
    // Délai minimal par défaut entre deux appuis consécutifs autorisés (en millisecondes).
    private static final long DEFAULT_INTERVAL_MS = 650L;
    // Intervalle retenu pour cette instance d'écouteur.
    private final long intervalMs;
    // Horodatage du dernier clic accepté (base SystemClock).
    private long lastClickAt;

    /** Construit l'écouteur avec la temporisation standard de 650 ms. */
    protected SafeClickListener() { this(DEFAULT_INTERVAL_MS); }

    /**
     * Construit l'écouteur avec un délai anti-rebond personnalisé.
     * @param intervalMs délai minimum en millisecondes exigé entre deux clics valides.
     */
    protected SafeClickListener(long intervalMs) { this.intervalMs = intervalMs; }

    /**
     * Intercepte le clic système, ignore l'événement s'il survient trop vite, puis appelle onSafeClick().
     */
    @Override public final void onClick(View view) {
        // Mesure l'horloge système monotone insensible aux changements d'heure manuels.
        long now = SystemClock.elapsedRealtime();
        // Rejette silencieusement tout clic survenant avant l'expiration du délai anti-rebond.
        if (now - lastClickAt < intervalMs) return;
        // Mémorise l'instant du clic valide et exécute l'action métier dans la sous-classe.
        lastClickAt = now;
        onSafeClick(view);
    }

    /** Méthode à implémenter par l'écran appelant pour exécuter l'action sécurisée. */
    protected abstract void onSafeClick(View view);
}
