/*
 * ============================================================================
 * FICHIER : DataState.java
 * MISSION : Représenter de manière unifiée les 3 états d'un flux ou d'une requête asynchrone MVVM.
 * IMPLÉMENTE : Enveloppe générique immuable DataState<T> (loading, data, failed).
 * EFFET À L'ÉCRAN : Permet aux écrans d'afficher tour à tour le spinner de chargement, les données réelles ou le bandeau d'erreur.
 * COMMUNICATIONS : Produit dans MasroufViewModel -> observé par HomeFragment et tous les appels viewModel.request().
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Enveloppe d'état immuable exposée par MasroufViewModel aux Activities et Fragments.
 * @param <T> type de la charge utile métier (LiveData ou Data DTO).
 */
public final class DataState<T> {
    // Indique qu'une opération asynchrone est en cours sur le thread IO.
    public final boolean loading;
    // Charge utile retournée en cas de succès (null pendant le chargement ou en cas d'échec).
    public final T data;
    // Indique qu'une exception s'est produite pendant la requête SQLite/Repository.
    public final boolean failed;

    // Constructeur privé garantissant la cohérence entre loading, data et failed.
    private DataState(boolean loading,T data,boolean failed){this.loading=loading;this.data=data;this.failed=failed;}

    /** Construit l'état d'attente affiché (ProgressBar) avant la fin de l'exécuteur IO. */
    public static <T> DataState<T> loading(){return new DataState<>(true,null,false);}

    /** @return état réussi contenant les données lues depuis SQLite. */
    public static <T> DataState<T> success(T data){return new DataState<>(false,data,false);}

    /** @return état d'erreur permettant d'afficher un bandeau d'alerte sans faire planter l'écran. */
    public static <T> DataState<T> failure(){return new DataState<>(false,null,true);}
}
