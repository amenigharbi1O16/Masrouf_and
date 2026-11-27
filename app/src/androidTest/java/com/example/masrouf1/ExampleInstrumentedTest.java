/*
 * ============================================================================
 * FICHIER : ExampleInstrumentedTest.java
 * MISSION : Test instrumenté Android minimal vérifiant que le paquet exécuté sur appareil correspond à com.example.masrouf1.
 * IMPLÉMENTE : Test JUnit 4 Android exécuté sous AndroidJUnit4 runner.
 * EFFET À L'ÉCRAN : Aucun affichage utilisateur (test d'intégration système).
 * COMMUNICATIONS : Lit le contexte applicatif cible via InstrumentationRegistry.
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Test instrumenté de vérification de l'identifiant applicatif cible sur émulateur/téléphone.
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    /**
     * Vérifie que le contexte instrumenté porte exactement le packageId déclaré dans app/build.gradle.kts.
     */
    @Test
    public void useAppContext() {
        // Récupère le Context de l'application testée fourni par AndroidJUnitRunner.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        // Valide que l'applicationId est bien "com.example.masrouf1".
        assertEquals("com.example.masrouf1", appContext.getPackageName());
    }
}