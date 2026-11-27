/*
 * ============================================================================
 * FICHIER : VoiceParserTest.java
 * MISSION : Tests unitaires JVM vérifiant le parseur vocal déterministe multilingue (Darija, Arabe, Français, Anglais).
 * IMPLÉMENTE : Tests JUnit 4 sur VoiceParser.parse(), detectLanguage(), toArabicScript() et parseRich().
 * COMMUNICATIONS : Teste directement com.masrouf.controller.VoiceParser sans dépendance réseau.
 * ============================================================================
 */
package com.masrouf.controller;

import org.junit.Test;
import static org.junit.Assert.*;

import com.example.masrouf1.db.MasroufEntities.CategoryEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Suite de tests unitaires JUnit 4 vérifiant l'analyseur vocal multilingue VoiceParser.
 */
public final class VoiceParserTest {
    /** Vérifie l'extraction des nombres parlés en darija tunisienne ("alfin", "khamsa", "achra", "alf meyya"). */
    @Test
    public void parsesPurchaseWithSpokenTunisianAmount() {
        VoiceParser.Result result = VoiceParser.parse("chrit khobz b alfin");
        assertEquals("purchase", result.type);
        assertEquals("bread", result.productKey);
        assertEquals(2000L, result.amountMilli);
        assertEquals(5000L, VoiceParser.parse("khamsa dinars").amountMilli);
        assertEquals(10000L, VoiceParser.parse("achra dinars").amountMilli);
        assertEquals(1100L, VoiceParser.parse("alf meyya").amountMilli);
        assertEquals(2100L, VoiceParser.parse("alfin w mya").amountMilli);
    }

    /** Vérifie la détection des dons, des dotations nommées à un enfant et la translittération arabizi. */
    @Test
    public void parsesDonationAndChildAllowanceAndArabizi() {
        assertEquals("donation", VoiceParser.parse("sadaqa 2 DT").type);
        VoiceParser.Result child = VoiceParser.parse("a3tit flous l Sara 5 dt");
        assertEquals("child_allowance", child.type);
        assertEquals("Sara", child.childName);
        assertEquals(5000L, child.amountMilli);
        assertEquals("dfaعt 10.500 DT", VoiceParser.toArabicScript("dfa3t 10.500 DT"));
    }

    /** Vérifie le marquage d'ambiguïté lorsqu'aucun montant ou plusieurs montants concurrents sont énoncés. */
    @Test
    public void flagsMissingOrCompetingNumbersAsAmbiguous() {
        assertTrue(VoiceParser.parse("chrit khobz").ambiguous);
        assertTrue(VoiceParser.parse("chrit khobz 2 kg b 3 DT").ambiguous);
    }

    /** Vérifie la détection de langue (FR/AR/EN/mixed), la préservation verbatim du libellé et le ciblage catégorie. */
    @Test
    public void detectsMultilingualLanguagesWithoutTranslatingText() {
        assertEquals("fr", VoiceParser.detectLanguage("J'ai acheté des baskets à 21"));
        assertEquals("ar", VoiceParser.detectLanguage("شريت حليب وخبز ب 5 دينار"));
        assertEquals("en", VoiceParser.detectLanguage("I bought milk and bread for 5 dinars"));
        assertEquals("mixed", VoiceParser.detectLanguage("J'ai acheté خبز 4 dinars"));

        CategoryEntity clothing = new CategoryEntity();
        clothing.id = 2L;
        clothing.name = "Vêtements et chaussures";
        clothing.type = "product";
        clothing.allocationPercent = 8;

        CategoryEntity bills = new CategoryEntity();
        bills.id = 9L;
        bills.name = "Factures";
        bills.type = "expense";
        bills.allocationPercent = 10;

        List<CategoryEntity> cats = Arrays.asList(clothing, bills);

        VoiceParser.RichResult fr = VoiceParser.parseRich("J'ai acheté des baskets à 21", cats, Collections.emptyList(), false);
        assertEquals("fr", fr.detectedLanguage);
        assertEquals("purchase", fr.operationType);
        assertEquals(21000L, fr.amountMilli);
        assertEquals("J'ai acheté des baskets", fr.label);
        assertEquals(2L, fr.matchedCategoryId);

        VoiceParser.RichResult ar = VoiceParser.parseRich("دفعت فاتورة الكهرباء 75 دينار", cats, Collections.emptyList(), false);
        assertEquals("ar", ar.detectedLanguage);
        assertEquals("expense", ar.operationType);
        assertEquals(75000L, ar.amountMilli);
        assertTrue(ar.label.contains("فاتورة الكهرباء"));
        assertEquals(9L, ar.matchedCategoryId);

        VoiceParser.RichResult en = VoiceParser.parseRich("I paid the internet bill 50 dinars", cats, Collections.emptyList(), false);
        assertEquals("en", en.detectedLanguage);
        assertEquals("expense", en.operationType);
        assertEquals(50000L, en.amountMilli);
        assertEquals("I paid the internet bill", en.label);
        assertEquals(9L, en.matchedCategoryId);
    }
}
