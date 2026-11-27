/*
 * ============================================================================
 * FICHIER : MoneyUtilsTest.java
 * MISSION : Tests unitaires JVM vérifiant l'exactitude des conversions dinars <-> millimes entiers dans MoneyUtils.
 * IMPLÉMENTE : Classe de test JUnit 4 exécutée localement sans émulateur.
 * COMMUNICATIONS : Teste directement com.example.masrouf1.MoneyUtils.
 * ============================================================================
 */
package com.example.masrouf1;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Suite de tests unitaires JUnit 4 vérifiant le parsing et le formatage exact en millimes entiers.
 */
public final class MoneyUtilsTest {
    /** Vérifie que les montants saisis avec espace/virgule deviennent des millimes entiers exacts. */
    @Test public void parsesWholeAndMillimeAmountsWithoutFloatingPoint() {
        assertEquals(1_250_000L, MoneyUtils.toMilli("1 250,000"));
        assertEquals(25_750L, MoneyUtils.toMilli("25,750"));
        assertEquals(0L, MoneyUtils.toMilli("0,000"));
    }

    /** Vérifie le formatage français avec séparateur de milliers espace, virgule, 3 décimales et suffixe DT. */
    @Test public void formatsWithFrenchGroupingAndExactlyThreeDecimals() {
        assertEquals("1 250,000 DT", MoneyUtils.formatMilli(1_250_000L));
        assertEquals("25,750 DT", MoneyUtils.formatMilli(25_750L));
        assertEquals("0,000 DT", MoneyUtils.formatMilli(0L));
    }

    /** Vérifie le rejet de la 4e décimale (perte de millime) et du débordement Long.MAX_VALUE. */
    @Test public void rejectsPrecisionLossAndOverflow() {
        assertThrows(() -> MoneyUtils.toMilli("0,0001"));
        assertThrows(() -> MoneyUtils.toMilli("9223372036854775,808"));
    }

    private static void assertThrows(Runnable action) {
        try { action.run(); fail("Expected invalid money input to be rejected"); }
        catch (ArithmeticException | NumberFormatException expected) { }
    }
}
