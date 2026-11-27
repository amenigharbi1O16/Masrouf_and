/*
 * ============================================================================
 * FICHIER : ExampleUnitTest.java
 * MISSION : Test unitaire JUnit 4 minimal vérifiant le bon fonctionnement de
 *           la chaîne d'exécution des tests locaux JVM sous Gradle.
 * IMPLÉMENTE : Test unitaire simple {@link #addition_isCorrect()}.
 * EFFET À L'ÉCRAN : Aucun — exécuté en test unitaire JVM.
 * COMMUNICATIONS :
 *   ← Suite Gradle `test`
 * ============================================================================
 */
package com.example.masrouf1;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test unitaire JUnit 4 minimal généré avec le gabarit Android vérifiant l'environnement d'exécution JVM local.
 */
public class ExampleUnitTest {
    /** Vérifie que l'addition arithmétique de base fonctionne correctement dans la suite de tests host. */
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}