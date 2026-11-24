package com.masrouf.controller;

import android.util.Base64;
import android.os.Build;

import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Passwords are stored as salted PBKDF2 hashes; the original text is never persisted. */
public final class PasswordHasher {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private PasswordHasher() { }

    public static String hash(String password) {
        byte[] salt = new byte[16]; new SecureRandom().nextBytes(salt);
        String algorithm=Build.VERSION.SDK_INT>=26?"PBKDF2WithHmacSHA256":"PBKDF2WithHmacSHA1";
        byte[] derived = derive(password, salt, ITERATIONS,algorithm);
        return ITERATIONS + "$" + algorithm + "$" + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" + Base64.encodeToString(derived, Base64.NO_WRAP);
    }

    public static boolean verify(String password, String encoded) {
        if (password == null || encoded == null) return false;
        try {
            String[] parts = encoded.split("\\$");
            int iterations = Integer.parseInt(parts[0]);
            String algorithm=parts[1]; byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expected = Base64.decode(parts[3], Base64.NO_WRAP);
            byte[] actual = derive(password, salt, iterations,algorithm);
            if (actual.length != expected.length) return false;
            int difference = 0;
            for (int i = 0; i < actual.length; i++) difference |= actual[i] ^ expected[i];
            return difference == 0;
        } catch (Exception ignored) { return false; }
    }

    private static byte[] derive(String password, byte[] salt, int iterations,String algorithm) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try { return SecretKeyFactory.getInstance(algorithm).generateSecret(spec).getEncoded(); }
        catch (Exception exception) { throw new IllegalStateException("Password hashing unavailable", exception); }
        finally { spec.clearPassword(); }
    }
}
