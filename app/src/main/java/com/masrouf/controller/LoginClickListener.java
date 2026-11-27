/*
 * ============================================================================
 * FICHIER : LoginClickListener.java
 * MISSION : Écouteur explicite nommé gérant le clic sur le bouton « Se connecter » de l'écran de connexion.
 * IMPLÉMENTE : View.OnClickListener découplé de LoginActivity.
 * EFFET À L'ÉCRAN : Lors du clic sur login_submit, lance la vérification des identifiants et l'ouverture de session.
 * COMMUNICATIONS : Instancié dans LoginActivity.onCreate() -> appelle activity.performLogin().
 * ============================================================================
 */
package com.masrouf.controller;

import android.view.View;

/**
 * Listener explicite nommé branché sur le bouton de connexion (login_submit) dans LoginActivity.
 */
public final class LoginClickListener implements View.OnClickListener {
    // Référence vers l'écran LoginActivity propriétaire du formulaire.
    private final LoginActivity activity;

    /**
     * Associe cet écouteur à l'instance LoginActivity courante.
     * @param activity écran de connexion qui exécutera performLogin() lors du clic.
     */
    public LoginClickListener(LoginActivity activity) { this.activity = activity; }

    /**
     * Méthode système déclenchée lorsque l'utilisateur appuie sur le bouton « Se connecter ».
     * @param view composant bouton ayant reçu le clic.
     */
    @Override public void onClick(View view) { activity.performLogin(); }
}
