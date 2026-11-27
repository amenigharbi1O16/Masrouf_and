/*
 * ============================================================================
 * FICHIER : MasroufViewModelFactory.java
 * MISSION : Fabrique AndroidX injectant la dépendance MasroufRepository dans MasroufViewModel.
 * IMPLÉMENTE : ViewModelProvider.Factory requise dès qu'un ViewModel possède un constructeur avec paramètres.
 * EFFET À L'ÉCRAN : Aucun rendu visuel ; permet à chaque Activity/Fragment d'obtenir son MasroufViewModel relié à Room.
 * COMMUNICATIONS : Utilisée avec new ViewModelProvider(owner, new MasroufViewModelFactory(context))
 *                  dans MainActivity, HomeFragment, SettingsFragment, DashboardSectionFragment et les écrans formulaires.
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/** Fabrique permettant d'injecter le Contexte applicatif lors de l'instanciation de MasroufViewModel. */
public final class MasroufViewModelFactory implements ViewModelProvider.Factory {
    // Contexte global d'application conservé sans risque de fuite d'Activity.
    private final Context applicationContext;

    /**
     * Construit la fabrique en extrayant getApplicationContext() du contexte fourni.
     */
    public MasroufViewModelFactory(Context context){applicationContext=context.getApplicationContext();}

    /**
     * Instancie MasroufViewModel avec un MasroufRepository initialisé sur la base Room locale.
     * @param modelClass classe de ViewModel demandée par ViewModelProvider.
     * @return instance typée de MasroufViewModel.
     */
    @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass){
        // Vérifie que la classe demandée est MasroufViewModel et lui injecte un nouveau MasroufRepository.
        if(modelClass.isAssignableFrom(MasroufViewModel.class))return modelClass.cast(new MasroufViewModel(new MasroufRepository(applicationContext)));
        // Lève une exception explicite si une autre classe inconnue est réclamée par erreur.
        throw new IllegalArgumentException("Unsupported ViewModel: "+modelClass.getName());
    }
}
