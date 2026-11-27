/*
 * ============================================================================
 * FICHIER : PasswordHasher.java
 * MISSION : Sécuriser localement les mots de passe des comptes USER, PARENT et CHILD avant stockage SQLite.
 * IMPLÉMENTE : Dérivation de clé PBKDF2 avec sel aléatoire 16 octets (120 000 itérations, clé 256 bits)
 *              et comparaison à temps constant anti-timing attack.
 * EFFET À L'ÉCRAN : Aucun affichage direct ; empêche tout stockage de mot de passe en clair dans users.passwordHash.
 * COMMUNICATIONS : Appelé par MasroufRepository, RegisterActivity, LoginActivity, ChangePasswordActivity,
 *                  ChildrenManagementActivity, DevelopmentSeedManager et UserDao.
 * ============================================================================
 */
package com.masrouf.controller;

import android.util.Base64;
import android.os.Build;

import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Module cryptographique de hachage des mots de passe via PBKDF2 salé (120 000 itérations, clé 256 bits).
 */
public final class PasswordHasher {
    // Coût computationnel PBKDF2 ralentissant drastiquement les attaques par force brute hors ligne.
    private static final int ITERATIONS = 120_000;
    // Longueur de l'empreinte générée en bits (32 octets).
    private static final int KEY_BITS = 256;
    private PasswordHasher() { }

    /**
     * Génère un sel aléatoire de 16 octets et produit la chaîne "iterations$algorithme$saltBase64$hashBase64".
     * @param password mot de passe en clair fourni à l'inscription ou réinitialisation.
     * @return empreinte cryptographique prête à être stockée en base Room.
     */
    public static String hash(String password) {
        // Tire 16 octets cryptographiquement aléatoires propres à ce compte.
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        // Utilise SHA-256 à partir d'Android 8 (API 26+) et se replie sur SHA-1 sur API 24-25.
        String algorithm=Build.VERSION.SDK_INT>=26?"PBKDF2WithHmacSHA256":"PBKDF2WithHmacSHA1";
        byte[] derived = derive(password, salt, ITERATIONS,algorithm);
        return ITERATIONS + "$" + algorithm + "$" + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" + Base64.encodeToString(derived, Base64.NO_WRAP);
    }

    /**
     * Vérifie un mot de passe candidat face à l'empreinte stockée en SQLite, avec comparaison XOR en temps constant.
     * @param password saisie en clair ; @param encoded empreinte issue de users.passwordHash.
     * @return true si le mot de passe correspond exactement.
     */
    public static boolean verify(String password, String encoded) {
        if (password == null || encoded == null) return false;
        try {
            // Décode les 4 segments séparés par '$' : itérations, algorithme, sel, empreinte attendue.
            String[] parts = encoded.split("\\$");
            int iterations = Integer.parseInt(parts[0]);
            String algorithm=parts[1]; byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            byte[] actual = derive(password, salt, iterations,algorithm);
            if (actual.length != expected.length) return false;
            // Accumule les différences octet par octet sans retour anticipé (temps constant).
            int difference = 0;
            for (int i = 0; i < actual.length; i++) difference |= actual[i] ^ expected[i];
            return difference == 0;
        } catch (Exception ignored) { return false; }
    }

    /** Exécute PBKDF2 sur SecretKeyFactory puis efface immédiatement le tableau de caractères en mémoire. */
    private static byte[] derive(String password, byte[] salt, int iterations,String algorithm) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try { return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded(); }
        catch (Exception exception) { throw new IllegalStateException("Password hashing unavailable", exception); }
        finally { spec.clearPassword(); }
    }
}
