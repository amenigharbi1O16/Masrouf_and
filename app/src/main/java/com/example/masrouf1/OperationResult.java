/*
 * ============================================================================
 * FICHIER : OperationResult.java
 * MISSION : Encapsuler le résultat d'une écriture métier (succès avec valeur ou échec avec code d'erreur nommé).
 * IMPLÉMENTE : Patron Result<T> typé et immuable utilisé par MasroufRepository.
 * EFFET À L'ÉCRAN : Permet aux contrôleurs (RegisterActivity, LoginActivity, SettingsFragment, ChildrenManagementActivity)
 *                   d'afficher précisément le message localisé ("duplicate", "password", "save", etc.).
 * COMMUNICATIONS : Retourné par MasroufRepository -> transporté dans DataState -> traité par les observateurs UI.
 * ============================================================================
 */
package com.example.masrouf1;

/**
 * Résultat métier typé retourné par MasroufRepository lors des écritures (authentification, création enfant, etc.).
 * @param <T> type du résultat en cas de succès.
 */
public final class OperationResult<T> {
    // Valeur produite lorsque l'opération a réussi (null en cas d'erreur).
    public final T value;
    // Code d'erreur lisible par l'UI (null lorsque l'opération est un succès).
    public final String error;

    // Constructeur privé imposant l'usage des fabriques success() et failure().
    private OperationResult(T value,String error){this.value=value;this.error=error;}

    /** @return résultat positif encapsulant value avec error == null. */
    public static <T> OperationResult<T> success(T value){return new OperationResult<>(value,null);}

    /** @return résultat en échec contenant la clé d'erreur à traduire et afficher à l'écran. */
    public static <T> OperationResult<T> failure(String error){return new OperationResult<>(null,error);}

    /** @return true uniquement si aucune erreur métier ou SQLite n'est survenue. */
    public boolean succeeded(){return error==null;}
}
